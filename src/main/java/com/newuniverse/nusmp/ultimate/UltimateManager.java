package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.anim.CastAnim;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.mana.Heritage;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.multiverse.WizardKing;
import com.newuniverse.nusmp.perf.PerfParticles;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.ultimate.UltimateDef.Debuff;
import com.newuniverse.nusmp.ultimate.UltimateRules.Cancel;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: this is the whole runtime of Ultimate Magic (docs/magic/ultimate_magic.md), server side only. The grimoire page of an Ultimate ({@link UltimatePages}) calls {@link #begin}; from then on ONE tracked
 * {@link Session} per caster (a map keyed by the player's UUID, server thread only) runs the cast:
 * <ol>
 *   <li><b>Wind-up</b> ({@code windupTicks}, 100..400): the caster is held in place by two transient attribute modifiers (movement speed, jump strength) and a charge animation plays; a circle follows the caster and
 *       the element's ground picture is re-sent every 40 ticks (at most 6 times); two small budgeted particle packets go out every 10 ticks. Nothing is paid yet.</li>
 *   <li><b>Cancel</b>: the caster dies, logs out, changes dimension, is sealed, takes {@code interruptFraction} of max health in damage, lets go (sneaks 15 ticks) or no longer holds 90 percent of the mana. Nothing was
 *       spent, so nothing is refunded; the only price is a 10 second lockout ({@link UltimateRules#FAIL_LOCKOUT_TICKS}).</li>
 *   <li><b>Release</b>: ALL the current magicule is taken, the long cooldown starts, and three pulses (0, 10, 20 ticks) hit at most 24 foes each inside the radius: damage through the grimoire's own balance law
 *       ({@code GrimoireBook.hurt}, which is clamped again by the PvP hit cap), the debuffs of the row, a push or pull. Foes are chosen by {@code Foes.isFoe} (never the caster, allies, teams, pets, summons, creative
 *       players or spectators; players only when {@code canHarmPlayer} says the PvP setting allows it).</li>
 * </ol>
 * Cost: while nobody casts the server tick returns on {@code SESSIONS.isEmpty()}; per active cast one O(1) check per tick, one particle emission per 10 ticks, one area query per pulse (at most 96 candidates looked at,
 * 24 hit). At most {@code maxSimultaneous} casts exist on the whole server (default 2). Effect packets of a cast are capped at {@link UltimateRules#MAX_EFFECT_PACKETS} and go through VfxSpawn's per-tick budget.
 */
public final class UltimateManager {
    private UltimateManager() {}

    /** The one tracked entry of a cast. Server thread only. */
    static final class Session {
        final UUID owner;
        final ServerPlayer player;
        final UltimateDef def;
        final GrimoireBook book;
        final ManasSkillInstance instance;
        final int mode, windup;
        final double radius;
        final ResourceKey<Level> dim;
        final long startTick;
        final int tint;
        final ParticleOptions particle;
        final boolean creative;
        boolean released, test;
        UltimateAnim.Clips clips = UltimateAnim.DEFAULT;       // the body animation of this caster (by kingdom, or the old single loop), chosen once at the start
        long releaseTick;
        int nextPulse, sneakTicks, packets;
        double damageTaken, cx, cy, cz;

        Session(ServerPlayer p, UltimateDef def, GrimoireBook book, ManasSkillInstance instance, int mode, int windup, double radius, long now) {
            this.owner = p.getUUID();
            this.player = p;
            this.def = def;
            this.book = book;
            this.instance = instance;
            this.mode = mode;
            this.windup = windup;
            this.radius = radius;
            this.dim = p.level().dimension();
            this.startTick = now;
            this.tint = book == null ? 0xFFFFFFFF : (book.colorNow() | 0xFF000000);
            this.particle = particleOf(def);
            this.creative = p.isCreative();
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final List<Session> SCRATCH = new ArrayList<>();
    private static final Map<String, Holder<MobEffect>> EFFECTS = new HashMap<>();
    private static final EnumMap<Cancel, Integer> CANCELS = new EnumMap<>(Cancel.class);
    private static int released;

    private static final ResourceLocation LOCK_SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "ultimate_lock_speed"), LOCK_JUMP = ResourceLocation.fromNamespaceAndPath("nusmp", "ultimate_lock_jump");
    private static final PerfProbe.Section PROBE = PerfProbe.section("s.ultimate.tick");

    // ---------------------------------------------------------------- queries
    /** Casts gathering or releasing right now on the whole server. */
    public static int activeCount() { return SESSIONS.size(); }

    public static boolean casting(Player p) { return !p.level().isClientSide && SESSIONS.containsKey(p.getUUID()); }

    /** Casts released since the server started (for the self-test and /multiverse reports). */
    public static int releasedCount() { return released; }

    public static int cancelCount(Cancel why) { return CANCELS.getOrDefault(why, 0); }

    // ---------------------------------------------------------------- the gate
    /** The player's progress against the gate of the page: the mastery of THIS book, the rank step, the EP. */
    public static UltimateGate.Progress progress(ServerPlayer p, GrimoireBook book, ManasSkillInstance inst) {
        double mastery = 0, ep = 0;
        try { mastery = book == null || inst == null ? 0 : book.masteryFrac(inst); } catch (Throwable ignored) { /* no skill data */ }
        try { ep = EnergyHelper.getMaxEP(p); } catch (Throwable ignored) { /* a stand-in player has no Tensura data */ }
        int step = 0;
        boolean king = false;
        try { step = MultiverseProfile.step(p); } catch (Throwable ignored) { /* no rank data */ }
        try { king = WizardKing.isKing(p); } catch (Throwable ignored) { /* no king data */ }
        return new UltimateGate.Progress(mastery, step, ep, king);
    }

    public static UltimateGate.Verdict verdict(ServerPlayer p, GrimoireBook book, ManasSkillInstance inst) {
        return UltimateGate.evaluate(UltimateConfig.gateStrictness(), progress(p, book, inst));
    }

    // ---------------------------------------------------------------- starting a cast (called by the page)
    /** The page's cast: true when a wind-up began (the grimoire then counts a cast), false when it was refused (a message says why; nothing is spent, no cooldown). */
    public static boolean begin(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, UltimateDef def) {
        if (def == null || p.level().isClientSide) return false;
        long now = p.level().getGameTime();
        double current = 0, max = 0;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null) {
            current = ex.getMagicule();
            max = EnergyHelper.getMaxMagicule(p);
        }
        UltimateGate.Verdict gate = verdict(p, book, inst);
        UltimateRules.Start verdict = UltimateRules.start(UltimateConfig.enabled(), gate.open(), now, now + UltimatePlayer.cooldownLeft(p, now), UltimatePlayer.lockUntil(p),
                SESSIONS.containsKey(p.getUUID()), SESSIONS.size(), UltimateConfig.maxSimultaneous(), current, max, p.isCreative());
        if (verdict != UltimateRules.Start.OK) {
            refuse(p, verdict, gate, now);
            return false;
        }
        return start(p, def, book, inst, mode, now);
    }

    /** Starts a cast for a CI self-test or an admin: no gate, no magicule rule, no cooldown check (the global cap and "already casting" still hold). */
    public static boolean startForTest(ServerPlayer p, UltimateDef def, GrimoireBook book, ManasSkillInstance inst, int windupTicks) {
        long now = p.level().getGameTime();
        if (SESSIONS.containsKey(p.getUUID()) || SESSIONS.size() >= UltimateRules.simultaneousCap(UltimateConfig.maxSimultaneous())) return false;
        return start(p, def, book, inst, -1, now, windupTicks, true);
    }

    private static boolean start(ServerPlayer p, UltimateDef def, GrimoireBook book, ManasSkillInstance inst, int mode, long now) {
        return start(p, def, book, inst, mode, now, UltimateRules.windupTicks(def.windupTicks(), UltimateConfig.windupScale()), false);
    }

    private static boolean start(ServerPlayer p, UltimateDef def, GrimoireBook book, ManasSkillInstance inst, int mode, long now, int windupTicks, boolean test) {
        ServerLevel sl = p.serverLevel();
        int windup = UltimateRules.clampInt(windupTicks, 1, UltimateRules.MAX_WINDUP_TICKS);
        double radius = UltimateRules.radius(def.radius(), UltimateConfig.radiusScale());
        Session s = new Session(p, def, book, inst, mode, windup, radius, now);
        s.test = test;
        s.cx = p.getX();
        s.cy = p.getY();
        s.cz = p.getZ();
        SESSIONS.put(p.getUUID(), s);
        lock(p);
        s.clips = UltimateAnim.choose(UltimateConfig.kingdomAnimations(), kingdomOf(p, inst));
        CastAnim.play(p, s.clips.first());      // the wind-up pose of the caster's kingdom (the old looping Mana Zone pose when there is none)
        sendCircle(sl, p, s);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0f, 0.5f);
        warnNearby(sl, p, s);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.ultimate.gathering", "You gather everything you are... (sneak to let go)").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** The caster's kingdom for the animation: the one they hail from (Heritage), else the kingdom of their grimoire's cover. Null when neither is known (a stand-in player, a grimoire without a cover). */
    static Kingdom kingdomOf(ServerPlayer p, ManasSkillInstance inst) {
        Kingdom heritage = null, cover = null;
        try { heritage = Heritage.kingdom(p); } catch (Throwable ignored) { /* no heritage data */ }
        try { cover = inst == null ? null : GrimoireBook.cover(inst).kingdom; } catch (Throwable ignored) { /* no cover data */ }
        return UltimateAnim.resolveKingdom(heritage, cover);
    }

    /**
     * The grimoire plays the page's own release clip right after a cast that began (GrimoireBook.onRelease); for an Ultimate page that must not replace the wind-up pose the manager just chose, so the clip of the
     * running cast is returned instead (the client ignores a clip that already plays). Anything else gets {@code clip} back unchanged.
     */
    public static String pageClip(Player p, String pageId, String clip) {
        if (SESSIONS.isEmpty() || p == null) return clip;
        Session s = SESSIONS.get(p.getUUID());
        return s != null && !s.released && s.def.id().equals(pageId) ? s.clips.first() : clip;
    }

    /** The clips the running cast of this player plays (null when none): for the self-test. */
    static UltimateAnim.Clips clipsOf(Player p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null ? null : s.clips;
    }

    private static void refuse(ServerPlayer p, UltimateRules.Start why, UltimateGate.Verdict gate, long now) {
        String text = UltimateRules.reason(why);
        String arg = switch (why) {
            case GATE -> gate.summary();
            case COOLDOWN -> minutes(UltimatePlayer.cooldownLeft(p, now));
            case LOCKOUT -> minutes(Math.max(0, UltimatePlayer.lockUntil(p) - now));
            default -> "";
        };
        GrimoireBook.fail(p, text.contains("%s") ? text.replace("%s", arg) : text);
    }

    static String minutes(long ticks) {
        long s = (ticks + 19) / 20;
        return s >= 60 ? (s / 60) + " min " + (s % 60) + " s" : s + " s";
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            SCRATCH.clear();
            SCRATCH.addAll(SESSIONS.values());
            for (Session s : SCRATCH) step(s);
            SCRATCH.clear();
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    private static void step(Session s) {
        ServerPlayer p = s.player;
        long now = p.level().getGameTime();
        if (!s.released) {
            if (p.isRemoved()) { cancel(s, Cancel.LOGOUT); return; }
            if (!p.isAlive()) { cancel(s, Cancel.DEATH); return; }
            if (!UltimateConfig.enabled()) { cancel(s, Cancel.DISABLED); return; }
            if (p.level().dimension() != s.dim) { cancel(s, Cancel.DIMENSION); return; }
            if (sealed(p, now)) { cancel(s, Cancel.SEALED); return; }
            int age = (int) (now - s.startTick);
            s.sneakTicks = p.isShiftKeyDown() ? s.sneakTicks + 1 : 0;
            if (UltimateRules.sneakCancels(UltimateConfig.sneakCancel(), age, s.sneakTicks)) { cancel(s, Cancel.SNEAK); return; }
            if (age % 10 == 0 && !s.creative && !s.test && !reserveHeld(p)) { cancel(s, Cancel.LOW_MANA); return; }
            if (age % 20 == 0) lock(p);
            if (age % UltimateRules.FX_EMIT_PERIOD == 0) emitCharge(s, age);
            if (UltimateRules.chargeFxDue(age, s.windup) && age > 0) sendCharge(p.serverLevel(), s);
            if (s.clips.loopDue(age)) CastAnim.play(p, s.clips.loop());      // the wind-up pose is over: the long hold begins (one packet, the existing cast-animation payload)
            if (age >= s.windup) release(s, now);
        } else {
            if (p.isRemoved() || !p.isAlive() || p.level().dimension() != s.dim) { finish(s); return; }       // the magicule is already spent: the remaining pulses are simply lost
            int t = (int) (now - s.releaseTick);
            while (s.nextPulse < UltimateRules.PULSES && t >= UltimateRules.pulseTick(s.nextPulse)) pulse(s, s.nextPulse++);
            if (s.nextPulse >= UltimateRules.PULSES) finish(s);
        }
    }

    private static boolean sealed(ServerPlayer p, long now) {
        try {
            return com.newuniverse.nusmp.entity.zogratis.SpellSeal.sealed(p) || p.getPersistentData().getLong("nusmp_sealed_until") > now;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean reserveHeld(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex == null || UltimateRules.reserveHeld(ex.getMagicule(), EnergyHelper.getMaxMagicule(p));
    }

    // ---------------------------------------------------------------- cancelling
    /** Ends a cast before its release. Nothing was spent, so there is nothing to give back; the caster gets the short lockout (not on a server stop or an admin end). */
    public static void cancel(Player player, Cancel why) {
        Session s = SESSIONS.get(player.getUUID());
        if (s != null) cancel(s, why);
    }

    private static void cancel(Session s, Cancel why) {
        if (s.released) { finish(s); return; }
        CANCELS.merge(why, 1, Integer::sum);
        ServerPlayer p = s.player;
        long lock = UltimateRules.lockoutUntil(p.level().getGameTime(), why);
        if (lock > 0) UltimatePlayer.setLockUntil(p, lock);
        finish(s);
        if (why != Cancel.LOGOUT && why != Cancel.SHUTDOWN && why != Cancel.DEATH && !p.isRemoved()) {
            CastAnim.play(p, CastAnim.FAIL);
            p.displayClientMessage(Component.translatableWithFallback("nusmp.ultimate.cancel." + why.name().toLowerCase(java.util.Locale.ROOT), UltimateRules.reason(why)).withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Removes the entry and everything it put on the caster (the movement hold, the animation). Idempotent. */
    private static void finish(Session s) {
        if (SESSIONS.remove(s.owner) == null) return;
        unlock(s.player);
        if (!s.player.isRemoved() && s.player.isAlive()) CastAnim.stop(s.player);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (SESSIONS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        cancel(p, Cancel.DEATH);
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (SESSIONS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        cancel(p, Cancel.DIMENSION);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (SESSIONS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        cancel(p, Cancel.LOGOUT);
    }

    /** A blow during the wind-up: adds up, and breaks the cast at the configured fraction of max health. */
    public static void onDamaged(LivingDamageEvent.Post e) {
        if (SESSIONS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || s.released) return;
        s.damageTaken += Math.max(0f, e.getNewDamage());
        if (UltimateRules.interrupts(s.damageTaken, p.getMaxHealth(), UltimateConfig.interruptFraction())) cancel(s, Cancel.DAMAGE);
    }

    /** The server is closing: every cast is dropped (nothing was spent, so nothing is lost) and the maps are cleared. */
    public static void onServerStopping() {
        SCRATCH.clear();
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            if (!s.released) CANCELS.merge(Cancel.SHUTDOWN, 1, Integer::sum);
            finish(s);
        }
        SESSIONS.clear();
    }

    /** Admin / self-test: ends every cast without a lockout. */
    public static void clearAll() {
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            if (!s.released) CANCELS.merge(Cancel.ADMIN, 1, Integer::sum);
            finish(s);
        }
    }

    // ---------------------------------------------------------------- the movement hold
    private static void lock(ServerPlayer p) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED), jump = p.getAttribute(Attributes.JUMP_STRENGTH);
        if (speed != null && !speed.hasModifier(LOCK_SPEED)) speed.addTransientModifier(new AttributeModifier(LOCK_SPEED, -0.95, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (jump != null && !jump.hasModifier(LOCK_JUMP)) jump.addTransientModifier(new AttributeModifier(LOCK_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        p.setSprinting(false);
    }

    private static void unlock(ServerPlayer p) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED), jump = p.getAttribute(Attributes.JUMP_STRENGTH);
        if (speed != null) speed.removeModifier(LOCK_SPEED);
        if (jump != null) jump.removeModifier(LOCK_JUMP);
    }

    // ---------------------------------------------------------------- the release
    private static void release(Session s, long now) {
        ServerPlayer p = s.player;
        ServerLevel sl = p.serverLevel();
        if (!s.creative && !s.test && !reserveHeld(p)) { cancel(s, Cancel.LOW_MANA); return; }
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null && !s.creative) {            // ALL the current magicule, taken now and not before
            double take = UltimateRules.release(ex.getMagicule());
            ex.setMagicule(Math.max(0.0, ex.getMagicule() - take));
            ex.markDirty();
        }
        s.released = true;
        s.releaseTick = now;
        s.cx = p.getX();
        s.cy = p.getY();
        s.cz = p.getZ();
        released++;
        UltimatePlayer.countRelease(p);
        int cd = UltimateConfig.cooldownTicks();
        UltimatePlayer.setCooldownUntil(p, now + cd);
        try {
            if (s.instance != null && s.mode >= 0) s.instance.setCoolDown(NUSMP.ticksToSeconds(cd), s.mode);      // the grimoire's own cooldown display
        } catch (Throwable ignored) { /* the instance may be gone */ }
        CastAnim.play(p, s.clips.release());      // the release clip of the caster's kingdom (the Signature finisher for the default)
        Vec3 c = new Vec3(s.cx, s.cy, s.cz);
        send(sl, s, VfxShape.MAGIC_CIRCLE_EXPLOSION, c.add(0, 0.1, 0), c.add(0, 1.1, 0), 30, (float) (s.radius / 5.0));
        send(sl, s, shape(s.def.burstShape()), c, c.add(0, 1, 0), burstTicks(s.def), s.def.burstPower());
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0f, 0.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0f, 0.5f);
        p.displayClientMessage(Component.literal(s.def.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        if (UltimateConfig.exhaustAfter() && !s.creative) {
            try { ManaLaw.exhaust(p); } catch (Throwable ignored) { /* the mana law is optional for a stand-in */ }
        }
    }

    private static void pulse(Session s, int index) {
        ServerPlayer p = s.player;
        ServerLevel sl = p.serverLevel();
        Vec3 c = new Vec3(s.cx, s.cy, s.cz);
        if (index > 0) send(sl, s, shape(s.def.echoShape()), c, c.add(0, 1, 0), 40, s.def.echoPower());
        List<LivingEntity> foes = foes(p, c, s.radius);
        double damageScale = UltimateConfig.damageScale();
        for (LivingEntity t : foes) {
            double dist = Math.sqrt(t.distanceToSqr(c.x, c.y, c.z));
            float raw = UltimateRules.pulseRaw(s.def.damage(), damageScale, index, dist, s.radius);
            if (raw > 0f && s.book != null && s.instance != null) s.book.hurt(s.instance, p, t, s.mode, raw);
            if (index == 0) afflict(s, t, c);
        }
    }

    /** The foes of the caster inside the sphere, nearest first: at most 96 are looked at, at most 24 are hit. */
    static List<LivingEntity> foes(ServerPlayer p, Vec3 c, double radius) {
        boolean players = UltimateConfig.hitPlayers();
        int[] seen = {0};
        List<LivingEntity> list = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius),
                e -> seen[0] < UltimateRules.MAX_CANDIDATES && e.distanceToSqr(c.x, c.y, c.z) <= radius * radius && (players || !(e instanceof Player)) && Foes.isFoe(p, e) && ++seen[0] > 0));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c.x, c.y, c.z)));
        return list.size() > UltimateRules.MAX_TARGETS ? new ArrayList<>(list.subList(0, UltimateRules.MAX_TARGETS)) : list;
    }

    private static void afflict(Session s, LivingEntity t, Vec3 c) {
        boolean boss = false;
        try { boss = BalanceLaw.isBoss(t); } catch (Throwable ignored) { /* no balance data */ }
        UltimateDef d = s.def;
        for (Debuff db : d.debuffs()) {
            Holder<MobEffect> eff = effect(db.effect());
            if (eff != null) t.addEffect(new MobEffectInstance(eff, UltimateRules.debuffTicks(db.ticks(), boss), UltimateRules.debuffAmplifier(db.amplifier())));
        }
        if (d.igniteSeconds() > 0) t.setRemainingFireTicks(Math.min(UltimateRules.MAX_IGNITE_SECONDS, d.igniteSeconds()) * 20);
        if (d.freezeTicks() > 0) t.setTicksFrozen(Math.min(UltimateRules.MAX_FREEZE_TICKS, d.freezeTicks()));
        double push = Math.max(-UltimateRules.MAX_PUSH, Math.min(UltimateRules.MAX_PUSH, d.push()));
        if (push != 0) {
            Vec3 away = t.position().subtract(c.x, t.getY(), c.z);
            double len = Math.max(0.1, Math.sqrt(away.x * away.x + away.z * away.z));
            t.knockback(Math.abs(push) * (boss ? 0.5 : 1.0), -(away.x / len) * Math.signum(push), -(away.z / len) * Math.signum(push));
        }
        if (d.lift() > 0) {
            Vec3 m = t.getDeltaMovement();
            t.setDeltaMovement(m.x, Math.max(m.y, Math.min(UltimateRules.MAX_LIFT, d.lift()) * (boss ? 0.5 : 1.0)), m.z);
            t.hurtMarked = true;
        }
    }

    /** The potion effect of a row, resolved once and kept (a missing id is kept as null and skipped). */
    private static Holder<MobEffect> effect(String id) {
        return EFFECTS.computeIfAbsent(id, UltimateManager::resolve);
    }

    private static Holder<MobEffect> resolve(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
    }

    // ---------------------------------------------------------------- pictures (all capped: see UltimateRules.MAX_EFFECT_PACKETS)
    private static VfxShape shape(String name) {
        try { return VfxShape.valueOf(name); } catch (IllegalArgumentException e) { return VfxShape.MAGIC_CIRCLE_EXPLOSION; }
    }

    private static ParticleOptions particleOf(UltimateDef def) {
        try { return MagicType.valueOf(def.magic()).particle(); } catch (RuntimeException | LinkageError e) { return null; }
    }

    /** One effect packet of this cast, if the cast still has some left (VfxSpawn adds the per-tick budget and the nearby-players rule). */
    private static void send(ServerLevel sl, Session s, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        if (s.packets >= UltimateRules.MAX_EFFECT_PACKETS) return;
        s.packets++;
        VfxSpawn.send(sl, shape, from, to, s.tint, ticks, power);
    }

    /** The long pictures (the black moon, the tornado) are drawn for 100 ticks, the others for 60. */
    private static int burstTicks(UltimateDef d) { return d.burstShape().equals("DARK_BLACK_MOON") || d.burstShape().equals("WIND_TORNADO") ? 100 : 60; }

    /** The wind-up picture is drawn as big as the area the release will hit (the configured radius scale included). */
    private static float chargePower(Session s) { return (float) Math.max(0.5, Math.min(24.0, s.def.chargePower() * (s.radius / Math.max(1.0, s.def.radius())))); }

    private static void sendCircle(ServerLevel sl, ServerPlayer p, Session s) {
        if (s.packets >= UltimateRules.MAX_EFFECT_PACKETS) return;
        s.packets++;
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.1, 0), s.tint, Math.min(s.windup, 200), (float) (s.radius / 6.0));
        sendCharge(sl, s);
    }

    private static void sendCharge(ServerLevel sl, Session s) {
        Vec3 c = new Vec3(s.cx, s.cy, s.cz);
        send(sl, s, shape(s.def.chargeShape()), c, c.add(0, 1, 0), Math.min(80, s.windup), chargePower(s));
    }

    /** Two budgeted particle packets every 10 ticks: a swirl that tightens as the wind-up ends, and a rising column. */
    private static void emitCharge(Session s, int age) {
        if (s.particle == null) return;
        ServerLevel sl = s.player.serverLevel();
        double k = Math.min(1.0, age / (double) Math.max(1, s.windup)), r = s.radius * 0.5 * (1.0 - k) + 1.0;
        PerfParticles.send(sl, s.particle, s.player.getX(), s.player.getY() + 0.3, s.player.getZ(), UltimateRules.CHARGE_PARTICLES, r, 0.15, r, 0.02);
        PerfParticles.send(sl, s.particle, s.player.getX(), s.player.getY() + 1.0, s.player.getZ(), 8, 0.3, 1.2, 0.3, 0.05);
    }

    private static void warnNearby(ServerLevel sl, ServerPlayer p, Session s) {
        double range = s.radius + UltimateRules.WARN_EXTRA_RANGE;
        Component line = Component.translatableWithFallback("nusmp.ultimate.warning", "%s gathers an Ultimate Magic! Get away or break their concentration!", p.getName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        int sent = 0;
        for (ServerPlayer o : sl.players()) {
            if (o == p || o.distanceToSqr(p) > range * range) continue;
            o.displayClientMessage(line, true);
            if (++sent >= UltimateRules.MAX_WARNED_PLAYERS) break;
        }
    }

    // ---------------------------------------------------------------- wiring
    /** For the self-test: the session of a player (null when none). */
    static Session session(Player p) { return SESSIONS.get(p.getUUID()); }

    /** For the self-test: ends the wind-up now and releases (as if the last tick had come). */
    static void forceRelease(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null && !s.released) release(s, p.level().getGameTime());
    }

    /** For the self-test: runs the remaining pulses at once. */
    static void forcePulses(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || !s.released) return;
        while (s.nextPulse < UltimateRules.PULSES) pulse(s, s.nextPulse++);
        finish(s);
    }
}
