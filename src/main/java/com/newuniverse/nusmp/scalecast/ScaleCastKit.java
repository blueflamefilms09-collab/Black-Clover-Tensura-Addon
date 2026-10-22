package com.newuniverse.nusmp.scalecast;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server half of Scale Magic's new execution (docs/magic/scale_casting.md). Scale Magic (Damnatio Kira) is carried out through ONE floating balance scale beside the caster
 * ({@link PropKind#SCALE_BALANCE}) that actively weighs a foe, and a passive SUPPRESSION AURA that presses the weighed foes' blows and spell power down by what the Scale measured. The server
 * decides everything (rule 9): the weights, the foes, the reductions and the caps live here and in the pure {@link ScaleCastRules}; the client only draws the synced prop word.
 * <p>
 * <b>The Scale</b>: one per player UUID ({@link State}), at most {@link ScaleCastRules#GLOBAL_SCALES} on the server. It appears when the Scale grimoire is summoned (floating), when a Scale page
 * is cast and while one is held, stays {@link ScaleCastRules#LINGER} ticks after the last of those and fades; a cast also starts the weighing animation (the pulse bits of the prop word).
 * <p>
 * <b>The aura</b>: while the Scale is shown or the grimoire floats, every {@link ScaleCastRules#SCAN_EVERY} ticks (staggered, at most {@link ScaleCastRules#SCANS_PER_TICK} scans on the whole
 * server in a tick) the caster's surroundings are scanned once for foes within the radius; the nearest {@link ScaleCastRules#MAX_FOES} are weighed (weight = Tensura EP and magicule load,
 * {@link #weightOf}) against the caster and each gets a reduction ({@link ScaleCastRules#reduction}: at most 35 percent of its damage, half for a boss) stored in the per-foe table
 * {@link #SUPP} for {@link ScaleCastRules#ENTRY_LIFE} ticks. The damage event only reads that table (one hash lookup, no allocation); the spell-power hook ({@link #magicMult}, called by
 * {@code ManaLaw.powerMult}) reads the same entry with its own lower cap. A foe held by several casters takes the strongest reduction, never the sum.
 * <p>
 * State is per player UUID in maps touched on the server thread only, cleaned on logout, death, respawn, dimension change and server stop. Nothing here touches a client class.
 */
public final class ScaleCastKit {
    private ScaleCastKit() {}

    private static final ThemeKey THEME = ThemeKey.of("magic.SCALE");
    private static final int TEAL = 0x63BDB5;
    /** The prop's own life: the ceiling only, the Scale ends at its {@code shownUntil}. */
    private static final int CEILING = 20 * 60 * 10;
    private static final PerfProbe.Section PROBE_TICK = PerfProbe.section("s.scale.tick"), PROBE_HIT = PerfProbe.section("s.scale.hit");
    /** The shared allowance of aura scans per tick (the scan is the only per-tick cost with any size). */
    private static final PerfBudget SCANS = new PerfBudget("scale-scans", ScaleCastRules.SCANS_PER_TICK);

    /** One caster's Scale and aura. */
    private static final class State {
        MagicPropEntity prop;
        long shownUntil, auraUntil, lastScan, lastWord, foeSince;
        boolean wasFloating;
        UUID foe;
        int casterQ, foeQ, percent, pulse;
        boolean hasFoe;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim;
        /** The foes this caster currently presses (to clean their entries up), and the scratch the next scan fills (swapped, so a scan allocates no array). */
        UUID[] foes = new UUID[ScaleCastRules.MAX_FOES], next = new UUID[ScaleCastRules.MAX_FOES];
        int nFoes;
    }

    /** What the aura currently does to one foe: who holds it, the damage and spell-power reductions, until when, and the tick a spell reduction was last applied. */
    private static final class Entry {
        UUID by;
        float red, magicRed;
        long until, magicStamp = Long.MIN_VALUE;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, Entry> SUPP = new HashMap<>();
    /** Scratch of the nearest-foes pick (server thread only; cleared after every scan so no entity is kept alive by it). */
    private static final LivingEntity[] PICK = new LivingEntity[ScaleCastRules.MAX_FOES];
    private static final double[] PICK_D = new double[ScaleCastRules.MAX_FOES];
    private static boolean registered;

    // ================================================================ wiring
    /** Called once by {@code prop.ScaleProps.init}: the damage hook, the player tick, the clean-ups. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        AttributeEvents.incoming(ScaleCastKit::onIncoming);
        AttributeEvents.playerTick(ScaleCastKit::tickPlayer);
        AttributeEvents.logout(p -> forget(p.getUUID()));
        AttributeEvents.death(e -> {
            Entity dead = e.getEntity();
            if (dead instanceof ServerPlayer p) forget(p.getUUID());
            if (!SUPP.isEmpty()) SUPP.remove(dead.getUUID());
        });
        AttributeEvents.serverTick(server -> { if (server.getTickCount() % 100 == 0) prune(server.overworld().getGameTime()); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> { STATES.clear(); SUPP.clear(); });
    }

    // ================================================================ the weight
    /**
     * The weight of a creature as the Scale reads it: Tensura's maximum existence points ({@code EnergyHelper.getMaxEP}) are its body, the share of its magicule pool that is loaded
     * ({@code TensuraStorages.getExistenceFrom(e).getMagicule()} over {@code EnergyHelper.getMaxMagicule}) is how much of it it brings to bear ({@link ScaleCastRules#weight}).
     * A creature that Tensura has no data for falls back to ten times its max health.
     */
    public static double weightOf(LivingEntity e) {
        double ep = 0, mp = 0, maxMp = 0;
        try { ep = EnergyHelper.getMaxEP(e); } catch (Throwable ignored) { }
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex != null) mp = ex.getMagicule();
            maxMp = EnergyHelper.getMaxMagicule(e);
        } catch (Throwable ignored) { }
        return ScaleCastRules.weight(ep, e.getMaxHealth(), mp, maxMp);
    }

    // ================================================================ the Scale
    /**
     * Makes sure the caster has a Scale and keeps it for {@code ticks} more (the aura with it). Null when the feature is off, when the server already holds the most Scales it allows, or
     * when the world refused the prop.
     */
    public static MagicPropEntity show(ServerPlayer p, int ticks) {
        if (!ScaleCastConfig.enabled()) return null;
        long now = p.level().getGameTime();
        State st = state(p, now, true);
        if (st == null) return null;
        st.shownUntil = Math.max(st.shownUntil, now + ticks);
        st.auraUntil = Math.max(st.auraUntil, st.shownUntil);
        ServerLevel level = p.serverLevel();
        if (st.prop == null || st.prop.isRemoved() || st.prop.level() != level) {
            double[] at = ScaleCastRules.position(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            MagicPropEntity e = MagicProps.spawn(level, PropKind.SCALE_BALANCE, new Vec3(at[0], at[1], at[2]), ScaleCastRules.yaw(p.yBodyRot), ScaleCastRules.MAIN_SCALE, CEILING, 0, p);
            if (e == null) return null;
            st.prop = e;
            st.lastWord = Long.MIN_VALUE;
            Fx.sprayTinted(level, THEME, FxSlot.SPARK, TEAL, 0.5f, at[0], at[1], at[2], 8, 0.4, 0.4, 0.2, 0.04);
            level.playSound(null, at[0], at[1], at[2], SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 0.85f);
        }
        return st.prop;
    }

    /** A Scale page was cast (hook of {@code GrimoireBook.castPage}): the Scale shows, the weighing animation starts, the foe is weighed at once (within the scan allowance). */
    public static void onCast(ServerPlayer p) {
        if (!ScaleCastConfig.enabled()) return;
        if (show(p, ScaleCastRules.LINGER) == null) return;
        State st = STATES.get(p.getUUID());
        if (st == null) return;
        st.pulse = (st.pulse + 1) & 7;
        long now = p.level().getGameTime();
        if (now - st.lastScan >= 4) st.lastScan = now - ScaleCastRules.SCAN_EVERY;        // weigh now rather than at the next scan, but never faster than every four ticks
    }

    /** A Scale page is being held (hook of {@code GrimoireBook.onHeld}): the Scale stays out. */
    public static void onHeld(ServerPlayer p) { show(p, 40); }

    /** The Scale's centre if the caster has one, else {@code fallback}: where a page starts its projectiles from (Scale Volley). */
    public static Vec3 origin(ServerPlayer p, Vec3 fallback) {
        State st = STATES.get(p.getUUID());
        if (st != null && st.prop != null && !st.prop.isRemoved() && st.prop.level() == p.level() && ScaleCastConfig.enabled()) return st.prop.position();
        return fallback;
    }

    /** True while the caster has a living Scale prop. */
    public static boolean hasScale(ServerPlayer p) {
        State st = STATES.get(p.getUUID());
        return st != null && st.prop != null && !st.prop.isRemoved();
    }

    /** True while the caster's aura is on. */
    public static boolean auraOn(ServerPlayer p) {
        State st = STATES.get(p.getUUID());
        return st != null && p.level().getGameTime() < st.auraUntil;
    }

    private static State state(ServerPlayer p, long now, boolean create) {
        State st = STATES.get(p.getUUID());
        if (st != null) {
            if (st.dim != null && st.dim != p.level().dimension()) { forget(p.getUUID()); st = null; }
            else return st;
        }
        if (!create || STATES.size() >= ScaleCastRules.GLOBAL_SCALES) return null;
        st = new State();
        st.dim = p.level().dimension();
        st.lastScan = now - (Math.floorMod(p.getUUID().hashCode(), ScaleCastRules.SCAN_EVERY));        // staggered between players
        STATES.put(p.getUUID(), st);
        return st;
    }

    // ================================================================ the tick (every player, every tick: returns at once unless the player has a Scale or the ten-tick check is due)
    private static void tickPlayer(ServerPlayer p) {
        long t0 = PerfProbe.begin();
        try {
            tick0(p);
        } finally {
            PerfProbe.end(PROBE_TICK, t0);
        }
    }

    private static void tick0(ServerPlayer p) {
        State st = STATES.get(p.getUUID());
        boolean due = p.tickCount % 10 == 3;
        if (st == null && !due) return;
        if (!ScaleCastConfig.enabled()) { if (st != null) forget(p.getUUID()); return; }
        long now = p.level().getGameTime();
        if (due && GrimoireSummon.isFloating(p, MagicType.SCALE)) {         // the grimoire is bound: the Scale shows when it was just summoned, the aura holds while it floats
            State s = state(p, now, true);
            if (s != null) {
                if (!s.wasFloating) show(p, ScaleCastRules.LINGER);
                s.wasFloating = true;
                s.auraUntil = Math.max(s.auraUntil, now + 30);
                st = s;
            }
        } else if (due && st != null) {
            st.wasFloating = false;
        }
        if (st == null) return;
        if (st.dim != null && st.dim != p.level().dimension()) { forget(p.getUUID()); return; }
        if (!p.isAlive()) { forget(p.getUUID()); return; }
        if (now < st.auraUntil) {
            if (now - st.lastScan >= ScaleCastRules.SCAN_EVERY && SCANS.tryTake(now, 1)) scan(p, st, now);
        } else if (st.nFoes > 0 || st.hasFoe) {
            release(p.getUUID(), st);
        }
        if (now >= st.shownUntil + 2 && now >= st.auraUntil && (st.prop == null || st.prop.isRemoved())) forget(p.getUUID());
    }

    /** One scan: weigh the caster, find the foes within the radius, keep the nearest few, weigh them, store their reductions, choose the foe the beam shows. */
    private static void scan(ServerPlayer p, State st, long now) {
        st.lastScan = now;
        ServerLevel level = p.serverLevel();
        double radius = ScaleCastConfig.radius(), r2 = radius * radius, cap = ScaleCastConfig.damageCap(), magicCap = ScaleCastConfig.magicCap();
        int cq = ScaleCastRules.quantize(weightOf(p));
        st.casterQ = cq;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> isFoe(p, e));
        int n = 0;
        for (LivingEntity e : near) {
            double d = e.distanceToSqr(p);
            if (d > r2) continue;
            if (n < ScaleCastRules.MAX_FOES) { n = insert(e, d, n); continue; }
            if (d < PICK_D[n - 1]) insert(e, d, n - 1);
        }
        LivingEntity weighed = chooseWeighed(p, st, n, now);
        UUID me = p.getUUID();
        UUID[] fresh = st.next;
        int kept = 0;
        for (int i = 0; i < n; i++) {
            LivingEntity f = PICK[i];
            int fq = ScaleCastRules.quantize(weightOf(f));
            double dec = ScaleCastRules.decades(cq, fq);
            double red = ScaleCastRules.reduction(dec, BalanceLaw.isBoss(f), cap);
            double mred = ScaleCastRules.magicReduction(red, magicCap);
            UUID id = f.getUUID();
            Entry en = SUPP.get(id);
            if (en == null) { en = new Entry(); SUPP.put(id, en); }
            if (en.by == null || me.equals(en.by) || now > en.until || red > en.red) {         // a foe held by several casters takes the strongest reduction, never the sum
                en.by = me;
                en.red = (float) red;
                en.magicRed = (float) mred;
                en.until = now + ScaleCastRules.ENTRY_LIFE;
            }
            fresh[kept++] = id;
            if (f == weighed) { st.foeQ = fq; st.percent = (int) Math.round(red * 100.0); }
        }
        // foes that left the radius stop being pressed at once (the entry is only dropped if this caster holds it)
        for (int i = 0; i < st.nFoes; i++) {
            UUID old = st.foes[i];
            boolean still = false;
            for (int k = 0; k < kept && !still; k++) still = old.equals(fresh[k]);
            if (!still) dropEntry(old, me);
        }
        st.next = st.foes;
        st.foes = fresh;
        st.nFoes = kept;
        Arrays.fill(st.next, null);
        st.hasFoe = weighed != null;
        st.foe = weighed == null ? null : weighed.getUUID();
        Arrays.fill(PICK, null);
    }

    /** Inserts into the nearest-first scratch; returns the new count (at most MAX_FOES). */
    private static int insert(LivingEntity e, double d, int n) {
        int at = Math.min(n, ScaleCastRules.MAX_FOES - 1);
        while (at > 0 && PICK_D[at - 1] > d) {
            PICK[at] = PICK[at - 1];
            PICK_D[at] = PICK_D[at - 1];
            at--;
        }
        PICK[at] = e;
        PICK_D[at] = d;
        return Math.min(ScaleCastRules.MAX_FOES, n + 1);
    }

    /** The foe the beam shows: the one already weighed while it stays in reach (so the beam does not flicker), else the one the caster looks at, else the nearest. */
    private static LivingEntity chooseWeighed(ServerPlayer p, State st, int n, long now) {
        if (n == 0) return null;
        if (st.foe != null && now - st.foeSince < ScaleCastRules.FOE_STICKY) {
            for (int i = 0; i < n; i++) if (st.foe.equals(PICK[i].getUUID())) return PICK[i];
        }
        LivingEntity look = GrimoireBook.target(p, ScaleCastConfig.radius());
        LivingEntity pick = PICK[0];
        if (look != null) for (int i = 0; i < n; i++) if (PICK[i] == look) { pick = look; break; }
        st.foeSince = now;
        return pick;
    }

    private static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return false;
        if (e instanceof Player other) return !other.isCreative() && p.getServer() != null && p.getServer().isPvpAllowed();
        if (e instanceof Enemy) return true;
        return e instanceof Mob m && m.getTarget() != null && (m.getTarget() == p || m.getTarget().isAlliedTo(p));
    }

    private static void dropEntry(UUID foe, UUID by) {
        Entry en = SUPP.get(foe);
        if (en != null && by.equals(en.by)) SUPP.remove(foe);
    }

    private static void release(UUID player, State st) {
        for (int i = 0; i < st.nFoes; i++) { dropEntry(st.foes[i], player); st.foes[i] = null; }
        st.nFoes = 0;
        st.hasFoe = false;
        st.foe = null;
        st.percent = 0;
    }

    private static void prune(long now) {
        if (SUPP.isEmpty()) return;
        SUPP.values().removeIf(en -> now > en.until + 20);
    }

    // ================================================================ the damage and the spell power
    /** The damage event: a suppressed foe's blow at the caster (or at their allies and pets) is cut by its reduction, never below the floor, never more than the cap. */
    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (SUPP.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            incoming(e);
        } finally {
            PerfProbe.end(PROBE_HIT, t0);
        }
    }

    private static void incoming(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof LivingEntity att)) return;
        Entry en = SUPP.get(att.getUUID());
        if (en == null) return;
        long now = att.level().getGameTime();
        if (now > en.until || en.red <= 0f || en.magicStamp == now || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;        // a spell reduced this very tick is not cut twice
        LivingEntity victim = e.getEntity();
        if (victim == att || !protects(victim, en.by)) return;
        float cut = ScaleCastRules.apply(e.getAmount(), en.red);
        if (cut < e.getAmount()) e.setAmount(cut);
    }

    /** Whether the aura of caster {@code by} protects {@code victim}: the caster, their pets and their team. */
    private static boolean protects(LivingEntity victim, UUID by) {
        if (by == null) return false;
        if (by.equals(victim.getUUID())) return true;
        if (!(victim.level() instanceof ServerLevel sl)) return false;
        Entity caster = sl.getEntity(by);
        return caster != null && victim.isAlliedTo(caster);
    }

    /**
     * The spell power of a caster inside someone's Scale aura: 1 when nothing presses it, else {@code 1 - magic reduction} (at most 30 percent). Called by {@code ManaLaw.powerMult} for
     * every spell's damage; stamps the tick so the damage event does not cut the same hit a second time.
     */
    public static double magicMult(ServerPlayer p) {
        if (SUPP.isEmpty()) return 1.0;
        Entry en = SUPP.get(p.getUUID());
        if (en == null) return 1.0;
        long now = p.level().getGameTime();
        if (now > en.until || en.magicRed <= 0f) return 1.0;
        en.magicStamp = now;
        return ScaleCastRules.magicMultiplier(en.magicRed);
    }

    /** The reduction the aura currently puts on a foe's damage (0 when none): for the HUD of the Scale and the tests. */
    public static float reductionOf(Entity foe, long now) {
        Entry en = SUPP.get(foe.getUUID());
        return en == null || now > en.until ? 0f : en.red;
    }

    // ================================================================ the prop
    /** The server behaviour of {@link PropKind#SCALE_BALANCE}. */
    public static final MagicProps.Behavior BEHAVIOR = new MagicProps.Behavior() {
        @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.8f, 0.8f); }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            State st = e.ownerId() == null ? null : STATES.get(e.ownerId());
            if (owner == null || st == null || st.prop != e || !ScaleCastConfig.enabled()) { e.expire(); return; }
            long now = sl.getGameTime();
            long left = st.shownUntil - now;
            if (left <= 0) { e.expire(); return; }
            // follow: float at the caster's left; ease toward the place so a sprint does not snap it; no write (no packet) when it is already there or on an odd tick
            double[] want = ScaleCastRules.position(owner.getX(), owner.getY(), owner.getZ(), owner.yBodyRot);
            double dx = want[0] - e.getX(), dy = want[1] - e.getY(), dz = want[2] - e.getZ(), d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > 1e-4 && ((now & 1L) == 0L || d2 > 64)) {                        // every second tick only (the client smooths between updates): half the position packets
                double k = d2 > 64 ? 1.0 : 0.6;
                e.setPos(e.getX() + dx * k, e.getY() + dy * k, e.getZ() + dz * k);
            }
            float wantYaw = ScaleCastRules.yaw(owner.yBodyRot);
            if (Math.abs(ScaleCastRules.approachDegrees(e.getYRot(), wantYaw, 1f) - e.getYRot()) > 0.5f) e.setYRot(ScaleCastRules.approachDegrees(e.getYRot(), wantYaw, 0.3f));
            Entity foe = st.foe == null ? null : sl.getEntity(st.foe);
            e.setTarget(foe instanceof LivingEntity le && le.isAlive() ? le : null);
            int word = ScaleCastRules.pack(st.casterQ, st.foeQ, st.hasFoe && foe != null, st.percent, ScaleCastConfig.radius(), now < st.auraUntil, st.pulse, left <= ScaleCastRules.FADE);
            if (ScaleCastRules.shouldSetWord(e.param(), word, now, st.lastWord)) {
                e.setParam(word);
                st.lastWord = now;
            }
        }

        @Override public void end(MagicPropEntity e, ServerLevel sl) {
            if (e.ownerId() != null) {
                State st = STATES.get(e.ownerId());
                if (st != null && st.prop == e) st.prop = null;
            }
            Fx.sprayTinted(sl, THEME, FxSlot.MOTE, TEAL, 0.45f, e.getX(), e.getY(), e.getZ(), 6, 0.4, 0.4, 0.2, 0.03);
        }
    };

    // ================================================================ housekeeping and testing
    /** Forgets everything of one caster: the Scale, the aura, the entries it holds. Safe to call at any time (logout, death, respawn, dimension change, the config turned off). */
    public static void forget(UUID player) {
        State st = STATES.remove(player);
        if (st == null) return;
        release(player, st);
        if (st.prop != null && !st.prop.isRemoved()) st.prop.discard();
    }

    /** Scales and aura entries on the whole server (tests and {@code /multiverse perf}). */
    public static int liveScales() { return STATES.size(); }

    public static int liveEntries() { return SUPP.size(); }

    /** The number of foes this caster presses right now. */
    public static int foesOf(ServerPlayer p) {
        State st = STATES.get(p.getUUID());
        return st == null ? 0 : st.nFoes;
    }

    /** Runs one scan now, whatever the throttles say (the self-test; the real tick never calls it). */
    public static void scanNow(ServerPlayer p) {
        long now = p.level().getGameTime();
        State st = state(p, now, true);
        if (st != null) { st.auraUntil = Math.max(st.auraUntil, now + 40); scan(p, st, now); }
    }
}
