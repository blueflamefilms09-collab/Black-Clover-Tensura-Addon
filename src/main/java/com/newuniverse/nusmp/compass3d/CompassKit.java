package com.newuniverse.nusmp.compass3d;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server half of the 3D Compass Magic (docs/magic/compass_3d.md). The owner's request: Compass Magic is ONE floating 3D compass like the anime's and every spell comes out of it as a 3D model.
 * The server decides everything and only reports it (rule 9): this class keeps ONE compass prop per caster ({@link PropKind#COMPASS_1}: it floats at the caster's right, follows, fades after
 * {@link CompassRules#LINGER} ticks and is never more than one per player), spawns the things the spells throw out of it ({@link PropKind#COMPASS_2}: arrow-needle projectiles that fly with the
 * page's own {@link SpellRuntime} bolt, seal discs that fly to a foe's feet, turn discs where a shot is bent back, route lines from the compass to the charted foe), and caps all of them per player
 * ({@link CompassRules#CAP_NEEDLES} and its siblings; decorative ones also take the shared effect budget). Gameplay stays in the pages (CompassBook): nothing here hurts anything; a prop that cannot
 * be spawned (cap, budget) just means the page's effect has no model that time. The client draws them ({@code client.prop.CompassPropPainter}) and only when {@code visual.compass3d} is on.
 * <p>
 * State is per player UUID, held in small maps that are only touched on the server thread and are cleaned when a prop ends or is found removed. Nothing here touches a client class.
 */
public final class CompassKit {
    private CompassKit() {}

    private static final ThemeKey THEME = ThemeKey.of("magic.COMPASS");
    private static final int GOLD = 0xFFC94A;
    /** The compass prop's own life (the lingering is the 'until' below, this is only the ceiling). */
    private static final int CEILING = 20 * 60 * 10;

    /** The state of one caster's compass. */
    private static final class Main {
        MagicPropEntity prop;
        long until, lastWord;
        int mode;
        UUID target;
    }

    private static final Map<UUID, Main> MAIN = new HashMap<>();
    /** The spell props of each caster, to enforce the caps (pruned of removed ones on every spawn). */
    private static final Map<UUID, List<MagicPropEntity>> LIVE = new HashMap<>();
    /** The bolt each arrow-needle prop follows. */
    private record Track(SpellRuntime.Bolt bolt, Vec3 last, int still) {}
    private static final Map<UUID, Track> BOLTS = new HashMap<>();

    // ================================================================ the caster's compass
    /**
     * Makes sure the caster has their compass and keeps it for 'ticks' more. mode: one of CompassRules.MODE_*, or -1 to keep the mode it has (a hold keeps it alive without changing what it
     * is doing); target: the foe the needle points at (null = the way the caster looks). Returns the compass prop, or null if the world refused.
     */
    public static MagicPropEntity ensure(ServerPlayer p, int ticks, int mode, LivingEntity target) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Main m = MAIN.get(p.getUUID());
        if (m == null || m.prop == null || m.prop.isRemoved() || m.prop.level() != level) {
            double[] at = CompassRules.mainPosition(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            MagicPropEntity e = MagicProps.spawn(level, PropKind.COMPASS_1, new Vec3(at[0], at[1], at[2]), CompassRules.mainYaw(p.yBodyRot), CompassRules.MAIN_SCALE, CEILING,
                    CompassRules.packMain(p.getYRot(), p.getXRot(), CompassRules.MODE_IDLE, 0), p);
            if (e == null) return null;
            m = new Main();
            m.prop = e;
            m.until = 0;
            MAIN.put(p.getUUID(), m);
            Fx.sprayTinted(level, THEME, FxSlot.SPARK, GOLD, 0.5f, at[0], at[1], at[2], 10, 0.45, 0.45, 0.2, 0.04);
        }
        m.until = Math.max(m.until, now + ticks);
        if (mode >= 0) {
            m.mode = mode;
            m.target = target == null ? null : target.getUUID();
        }
        return m.prop;
    }

    /** The compass centre (the prop's place if the caster has one, else where it would float). */
    public static Vec3 centre(ServerPlayer p) {
        Main m = MAIN.get(p.getUUID());
        if (m != null && m.prop != null && !m.prop.isRemoved() && m.prop.level() == p.level()) return m.prop.position();
        double[] at = CompassRules.mainPosition(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        return new Vec3(at[0], at[1], at[2]);
    }

    /** True while the caster has a living compass prop. */
    public static boolean hasMain(ServerPlayer p) {
        Main m = MAIN.get(p.getUUID());
        return m != null && m.prop != null && !m.prop.isRemoved();
    }

    /** The server behaviour of {@link PropKind#COMPASS_1}. */
    public static final MagicProps.Behavior MAIN_BEHAVIOR = new MagicProps.Behavior() {
        @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.7f, 0.7f); }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            Main m = e.ownerId() == null ? null : MAIN.get(e.ownerId());
            if (owner == null || m == null || m.prop != e) { e.expire(); return; }
            long left = m.until - sl.getGameTime();
            if (left <= 0) { e.expire(); return; }
            // follow: float at the caster's right, a little behind, turned outward; ease toward it so a sprint does not snap it
            double[] want = CompassRules.mainPosition(owner.getX(), owner.getY(), owner.getZ(), owner.yBodyRot);
            double dx = want[0] - e.getX(), dy = want[1] - e.getY(), dz = want[2] - e.getZ();
            double k = dx * dx + dy * dy + dz * dz > 64 ? 1.0 : 0.4;
            e.setPos(e.getX() + dx * k, e.getY() + dy * k, e.getZ() + dz * k);
            e.setYRot(CompassRules.approachDegrees(e.getYRot(), CompassRules.mainYaw(owner.yBodyRot), 0.3f));
            // what the needle points at: the charted foe, else the way the caster looks
            Entity t = m.target == null ? null : sl.getEntity(m.target);
            if (t instanceof LivingEntity le && le.isAlive() && le.distanceToSqr(owner) < 64 * 64) e.setTarget(le);
            else e.setTarget(null);
            Vec3 look = owner.getViewVector(1f);
            float aimYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z)), aimPitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(look.y, -1, 1)));
            int word = CompassRules.packMain(aimYaw, aimPitch, m.mode, left <= CompassRules.FADE ? (int) left : 0);
            long now = sl.getGameTime();
            if (CompassRules.shouldSetWord(e.param(), word, now, m.lastWord)) {            // 1.112.0: an aim-only change is sent at most every third tick
                e.setParam(word);
                m.lastWord = now;
            }
        }

        @Override public void end(MagicPropEntity e, ServerLevel sl) {
            if (e.ownerId() != null) {
                Main m = MAIN.get(e.ownerId());
                if (m != null && m.prop == e) MAIN.remove(e.ownerId());
            }
            Fx.sprayTinted(sl, THEME, FxSlot.MOTE, GOLD, 0.45f, e.getX(), e.getY(), e.getZ(), 6, 0.4, 0.4, 0.2, 0.03);
        }
    };

    // ================================================================ what comes out of it
    /**
     * Fires a page's bolt as an arrow-needle: the same {@link SpellRuntime} bolt (the hitbox, the homing, the damage callback are the page's own, untouched) plus a flying 3D arrow-needle prop
     * that rides the bolt. At the cap the bolt flies without a model. The prop ends with the bolt.
     */
    public static SpellRuntime.Bolt bolt(ServerPlayer p, Vec3 start, Vec3 vel, double radius, int life, boolean pierce, LivingEntity homing,
                                         SpellRuntime.OnHit hit, SpellRuntime.OnEnd end, int index) {
        MagicPropEntity prop = spawn(p, CompassRules.V_NEEDLE, index, start, 1.0f, life + 8, CompassRules.CAP_NEEDLES, false);
        SpellRuntime.Bolt bolt = SpellRuntime.bolt(p, start, vel, radius, life, pierce, homing, hit, (b, at) -> {
            if (prop != null) prop.expire();
            if (end != null) end.end(b, at);
        });
        if (prop != null) {
            BOLTS.put(prop.getUUID(), new Track(bolt, start, 0));
            orient(prop, vel);
            Fx.sprayTinted(p.serverLevel(), THEME, FxSlot.SPARK, GOLD, 0.4f, start.x, start.y, start.z, 3, 0.12, 0.12, 0.12, 0.03);
        }
        return bolt;
    }

    /** A seal disc flies from the compass to the foe's feet and turns there while 'life' lasts, following the foe. Null at the cap. */
    public static MagicPropEntity seal(ServerPlayer p, LivingEntity foe, int life, int index) {
        MagicPropEntity e = spawn(p, CompassRules.V_SEAL, index, centre(p), 1.0f, life + CompassRules.SEAL_FLIGHT, CompassRules.CAP_SEALS, true);
        if (e == null) return null;
        e.setTarget(foe);
        e.getPersistentData().putDouble("sx", e.getX());
        e.getPersistentData().putDouble("sy", e.getY());
        e.getPersistentData().putDouble("sz", e.getZ());
        return e;
    }

    /** A small turn disc where a shot is bent back (its needle swings toward the one who fired it). Null at the cap or the effect budget. */
    public static MagicPropEntity turnDisc(ServerPlayer p, Vec3 at, Entity shooter) {
        MagicPropEntity e = spawn(p, CompassRules.V_TURN, 0, at, 0.5f, 16, CompassRules.CAP_TURNS, true);
        if (e != null && shooter != null) e.setTarget(shooter);
        return e;
    }

    /** A route of marching 3D dashes from the compass to the foe, for 'ticks'. A new route replaces the oldest at the cap. */
    public static MagicPropEntity route(ServerPlayer p, LivingEntity foe, int ticks) {
        MagicPropEntity e = spawn(p, CompassRules.V_ROUTE, 0, centre(p), 1.0f, ticks, CompassRules.CAP_ROUTES, false);
        if (e != null) e.setTarget(foe);
        return e;
    }

    /** Spawns one COMPASS_2 prop of a variant under the caster's cap; the oldest of the same variant is dismissed for a route, a new one is refused for the rest. */
    private static MagicPropEntity spawn(ServerPlayer p, int variant, int index, Vec3 at, float scale, int life, int cap, boolean budgeted) {
        ServerLevel level = p.serverLevel();
        List<MagicPropEntity> list = LIVE.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        list.removeIf(x -> x.isRemoved() || x.level() != level);
        int same = 0;
        MagicPropEntity oldest = null;
        for (MagicPropEntity x : list) {
            if (CompassRules.variant(x.param()) != variant) continue;
            same++;
            if (oldest == null || x.life() > oldest.life()) oldest = x;
        }
        if (same >= cap) {
            if (variant != CompassRules.V_ROUTE || oldest == null) return null;
            oldest.expire();
            list.remove(oldest);
        }
        if (globalLive(variant, level.getGameTime()) >= CompassRules.globalCap(variant)) return null;          // 1.112.0: the server-wide cap
        if (budgeted && !PerfBudgets.effectPacket(level.getGameTime())) return null;
        MagicPropEntity e = MagicProps.spawn(level, PropKind.COMPASS_2, at, 0f, scale, Math.max(2, life), CompassRules.packProp(variant, index), p);
        if (e != null) {
            list.add(e);
            if (variant >= 0 && variant < COUNT.length) COUNT[variant]++;
        }
        return e;
    }

    /** Living spell props of each variant on the whole server, counted at most once per tick (and kept up to date within the tick by {@link #spawn}). */
    private static final int[] COUNT = new int[4];
    private static boolean registered;
    private static long countTick = Long.MIN_VALUE;

    private static int globalLive(int variant, long now) {
        if (variant < 0 || variant >= COUNT.length) return Integer.MAX_VALUE;
        if (now != countTick) {
            countTick = now;
            java.util.Arrays.fill(COUNT, 0);
            for (List<MagicPropEntity> l : LIVE.values()) {
                for (MagicPropEntity x : l) {
                    if (x.isRemoved()) continue;
                    int v = CompassRules.variant(x.param());
                    if (v >= 0 && v < COUNT.length) COUNT[v]++;
                }
            }
        }
        return COUNT[variant];
    }

    /** Server-wide living spell props of a variant (tests and {@code /multiverse perf}). */
    public static int liveEverywhere(int variant, long now) { return globalLive(variant, now); }

    /**
     * 1.112.0 (lag audit): the housekeeping the first version left out. The per-caster tables are keyed by player UUID and were only emptied by {@link #forget} (a self-test) or when the same
     * player cast again, so a player who left kept a list of finished props (and the level they were in) for the rest of the server's life. Now they go when the player logs out and when the
     * server stops. Called once from {@code prop.CompassProps.init}.
     */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> forget(e.getEntity().getUUID()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> {
            MAIN.clear();
            LIVE.clear();
            BOLTS.clear();
        });
    }

    private static void orient(MagicPropEntity e, Vec3 dir) {
        if (dir.lengthSqr() < 1e-6) return;
        Vec3 d = dir.normalize();
        e.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        e.setXRot((float) -Math.toDegrees(Math.asin(Mth.clamp(d.y, -1, 1))));
    }

    /** The server behaviour of {@link PropKind#COMPASS_2}. */
    public static final MagicProps.Behavior SPELL_BEHAVIOR = new MagicProps.Behavior() {
        @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.4f * e.scale(), 0.4f * e.scale()); }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) {
            switch (CompassRules.variant(e.param())) {
                case CompassRules.V_NEEDLE -> needle(e);
                case CompassRules.V_SEAL -> sealTick(e, sl);
                case CompassRules.V_ROUTE -> routeTick(e, sl);
                default -> { }                                      // the turn disc just stays where the shot was bent
            }
        }

        @Override public void end(MagicPropEntity e, ServerLevel sl) {
            BOLTS.remove(e.getUUID());
            int v = CompassRules.variant(e.param());
            if (v == CompassRules.V_NEEDLE) Fx.sprayTinted(sl, THEME, FxSlot.SPARK, GOLD, 0.45f, e.getX(), e.getY(), e.getZ(), 5, 0.15, 0.15, 0.15, 0.05);
            else if (v == CompassRules.V_SEAL) Fx.sprayTinted(sl, THEME, FxSlot.MOTE, GOLD, 0.5f, e.getX(), e.getY() + 0.2, e.getZ(), 6, 0.3, 0.1, 0.3, 0.03);
        }
    };

    private static void needle(MagicPropEntity e) {
        Track t = BOLTS.get(e.getUUID());
        if (t == null) { e.expire(); return; }
        Vec3 pos = t.bolt().pos;
        // a bolt that was dissolved (SpellRuntime.dissolveBolts) stops moving without calling its end: two still ticks end the model too
        int still = pos.distanceToSqr(t.last()) < 1e-8 ? t.still() + 1 : 0;
        if (still >= 2) { e.expire(); return; }
        BOLTS.put(e.getUUID(), new Track(t.bolt(), pos, still));
        e.setPos(pos.x, pos.y, pos.z);
        orient(e, t.bolt().vel);
    }

    private static void sealTick(MagicPropEntity e, ServerLevel sl) {
        LivingEntity owner = e.owner();
        Entity t = e.target();
        if (owner == null || !(t instanceof LivingEntity le) || !le.isAlive() || t.level() != sl) { e.expire(); return; }
        Vec3 dest = le.position().add(0, 0.12, 0);
        int age = e.life();
        if (age < CompassRules.SEAL_FLIGHT) {
            Vec3 s = new Vec3(e.getPersistentData().getDouble("sx"), e.getPersistentData().getDouble("sy"), e.getPersistentData().getDouble("sz"));
            double k = CompassRules.ease(age, CompassRules.SEAL_FLIGHT);
            Vec3 at = s.lerp(dest, k);
            e.setPos(at.x, at.y + Math.sin(k * Math.PI) * 0.8, at.z);
        } else {
            e.setPos(dest.x, dest.y, dest.z);
            if (age == CompassRules.SEAL_FLIGHT) {
                Fx.sprayTinted(sl, THEME, FxSlot.SPARK, GOLD, 0.5f, dest.x, dest.y + 0.1, dest.z, 8, 0.4, 0.05, 0.4, 0.04);
                if (CompassRules.index(e.param()) < 2)
                    sl.playSound(null, e.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        }
    }

    private static void routeTick(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !(e.target() instanceof LivingEntity le) || !le.isAlive()) { e.expire(); return; }
        Vec3 c = centre(p);
        e.setPos(c.x, c.y, c.z);
    }

    // ================================================================ testing and housekeeping
    /** Living spell props of a variant of this caster (the caps' counter). */
    public static int live(ServerPlayer p, int variant) {
        List<MagicPropEntity> list = LIVE.get(p.getUUID());
        if (list == null) return 0;
        int n = 0;
        for (MagicPropEntity x : list) if (!x.isRemoved() && CompassRules.variant(x.param()) == variant) n++;
        return n;
    }

    /** Forgets everything of one caster (a self-test cleans up after itself; a log out needs nothing, the props expire when their owner is gone). */
    public static void forget(UUID player) {
        Main m = MAIN.remove(player);
        if (m != null && m.prop != null && !m.prop.isRemoved()) m.prop.discard();
        List<MagicPropEntity> l = LIVE.remove(player);
        if (l != null) for (MagicPropEntity x : l) { BOLTS.remove(x.getUUID()); if (!x.isRemoved()) x.discard(); }
    }
}
