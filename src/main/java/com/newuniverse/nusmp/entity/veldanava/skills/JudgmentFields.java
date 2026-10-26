package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Why: Verdict of Ruin is a field of true damage that follows its target, and the skills that come after it (Israfil's cage and Nihility Collapse, Jibril's sanctuary, Solomon's realm) are fields
 * too. This is the one manager of them: a small list, ticked once per server tick, with hard limits so no number of players can make it expensive. A field has a {@link Behavior} (what one
 * pulse does and how often it pulses); the manager owns the list, the limits and the cleanup.
 *
 * <p>Limits: at most {@code fieldsPerPlayer} per owner of all kinds together (a new one replaces the owner's oldest) and {@code fieldsTotal} on the server (a new one over that is refused); a field
 * lasts at most {@link #MAX_TICKS}; it pulses on its own period and each pulse touches at most the number of creatures its behavior allows (all of them use one bounding-box query); it sends no packets
 * (the client draws the field from the one effect packet the skill sent when it was raised). A field ends when its time is up, when its owner dies, logs out or leaves the dimension, when
 * the creature it follows dies or leaves, and when the server stops; the behavior is told ({@link Behavior#end}). With no field, the tick returns on its first line.
 */
public final class JudgmentFields {
    private JudgmentFields() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("s.veldanava.fields");

    /** The longest any field lives (ticks), whatever a skill asks for. */
    public static final int MAX_TICKS = 1200;

    /** What a kind of field does: how often it pulses, what a pulse does, and what happens when it ends. */
    interface Behavior {
        /** Ticks between two pulses (at least 1). */
        int period();

        /** One pulse: touches at most a bounded number of creatures. Runs on the server thread. */
        void pulse(ServerPlayer owner, ServerLevel level, Field f);

        /** The field is over (time up, owner gone, edict, server stop). */
        default void end(ServerLevel level, Field f) {}
    }

    /** One live field. */
    static final class Field {
        final Behavior behavior;
        final String kind;
        final UUID owner;
        final ResourceKey<Level> dimension;
        final UUID follow;                                    // the creature it follows, or null for a field fixed in the ground
        Vec3 center;
        final double radius;
        long endsAt;

        Field(Behavior behavior, String kind, UUID owner, ResourceKey<Level> dimension, UUID follow, Vec3 center, double radius, long endsAt) {
            this.behavior = behavior;
            this.kind = kind;
            this.owner = owner;
            this.dimension = dimension;
            this.follow = follow;
            this.center = center;
            this.radius = radius;
            this.endsAt = endsAt;
        }
    }

    private static final List<Field> FIELDS = new ArrayList<>();

    public static int count() { return FIELDS.size(); }

    public static int countOf(UUID owner) {
        int n = 0;
        for (Field f : FIELDS) if (f.owner.equals(owner)) n++;
        return n;
    }

    /** Raises a Verdict of Ruin field. {@code follow} may be null. Returns false when the server-wide limit refuses it. Server thread only. */
    public static boolean add(ServerPlayer owner, ServerLevel level, LivingEntity follow, Vec3 center, double radius, int ticks) {
        return add(owner, level, follow, center, radius, Math.min(ticks, JudgmentRules.VERDICT_TICKS), VERDICT, "verdict");
    }

    /** Raises a field of any kind. Returns false when the server-wide limit refuses it. The field's life is cut to {@link #MAX_TICKS}. Server thread only. */
    static boolean add(ServerPlayer owner, ServerLevel level, LivingEntity follow, Vec3 center, double radius, int ticks, Behavior behavior, String kind) {
        int perPlayer = Math.max(1, JudgmentConfig.fieldsPerPlayer());
        while (countOf(owner.getUUID()) >= perPlayer) removeOldestOf(level.getServer(), owner.getUUID());
        if (FIELDS.size() >= Math.max(1, JudgmentConfig.fieldsTotal())) return false;
        FIELDS.add(new Field(behavior, kind, owner.getUUID(), level.dimension(), follow == null ? null : follow.getUUID(), center, radius, level.getGameTime() + Math.max(1, Math.min(ticks, MAX_TICKS))));
        return true;
    }

    private static void removeOldestOf(MinecraftServer server, UUID owner) {
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            if (f.owner.equals(owner)) { it.remove(); finish(server, f); return; }
        }
    }

    /** Tells the behavior its field is over. A behavior that throws must not break the tick. */
    private static void finish(MinecraftServer server, Field f) {
        if (server == null) return;
        try {
            ServerLevel l = server.getLevel(f.dimension);
            if (l != null) f.behavior.end(l, f);
        } catch (RuntimeException ignored) {
            // an ending is best effort
        }
    }

    /** The first live field of this owner and kind, or null. */
    static Field find(UUID owner, String kind) {
        for (Field f : FIELDS) if (f.owner.equals(owner) && f.kind.equals(kind)) return f;
        return null;
    }

    /** Keeps a field alive until {@code until} (a held domain renews itself every tick it is still held). */
    static void renew(Field f, long until) { f.endsAt = Math.min(until, f.endsAt + MAX_TICKS); }

    /** Ends the owner's fields of one kind. Returns how many. */
    static int clearKind(MinecraftServer server, UUID owner, String kind) {
        int n = 0;
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            if (f.owner.equals(owner) && f.kind.equals(kind)) { it.remove(); finish(server, f); n++; }
        }
        return n;
    }

    /** The default behavior: Verdict of Ruin. */
    static final Behavior VERDICT = new Behavior() {
        @Override public int period() { return JudgmentRules.FIELD_PULSE_TICKS; }
        @Override public void pulse(ServerPlayer owner, ServerLevel level, Field f) { pulseVerdict(owner, level, f); }
        @Override public void end(ServerLevel level, Field f) { tollVerdict(level, f); }
    };

    /** Ends every field that touches the sphere (the Supreme Edict cancels domains). Returns how many were ended. */
    public static int clearAround(ServerLevel level, Vec3 center, double radius) { return clearAround(level, center, radius, null); }

    /** The same, but the fields of {@code keep} stay (a caster's own edict does not end the caster's own field). */
    public static int clearAround(ServerLevel level, Vec3 center, double radius, UUID keep) {
        int n = 0;
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            if (keep != null && f.owner.equals(keep)) continue;
            if (f.dimension.equals(level.dimension()) && f.center.distanceTo(center) <= radius + f.radius) { it.remove(); finish(level.getServer(), f); n++; }
        }
        return n;
    }

    /**
     * Solomon (3.18.0): ends every field that touches the sphere whose owner {@code keep} does not name (the owner's own and the owner's allies' fields stay), at most {@code max}; {@code owners} is told the owner of
     * each field that ended. Returns how many.
     */
    static int clearHostile(ServerLevel level, Vec3 center, double radius, java.util.function.Predicate<UUID> keep, int max, java.util.function.Consumer<UUID> owners) {
        int n = 0;
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext() && n < max; ) {
            Field f = it.next();
            if (keep.test(f.owner)) continue;
            if (f.dimension.equals(level.dimension()) && f.center.distanceTo(center) <= radius + f.radius) {
                it.remove();
                finish(level.getServer(), f);
                if (owners != null) owners.accept(f.owner);
                n++;
            }
        }
        return n;
    }

    public static void clearOf(UUID owner) {
        FIELDS.removeIf(f -> f.owner.equals(owner));
    }

    public static void clearAll() { FIELDS.clear(); }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        if (FIELDS.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            MinecraftServer server = e.getServer();
            long now = server.overworld().getGameTime();
            for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
                Field f = it.next();
                if (!step(server, f, now)) { it.remove(); finish(server, f); }
            }
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** Runs one field for this tick; false = it is over. */
    private static boolean step(MinecraftServer server, Field f, long now) {
        if (now >= f.endsAt || !JudgmentConfig.enabled()) return false;
        ServerPlayer owner = server.getPlayerList().getPlayer(f.owner);
        if (owner == null || !owner.isAlive()) return false;
        ServerLevel level = server.getLevel(f.dimension);
        if (level == null || owner.serverLevel() != level) return false;
        if (f.follow != null) {
            net.minecraft.world.entity.Entity e = level.getEntity(f.follow);
            if (!(e instanceof LivingEntity target) || !target.isAlive()) return false;
            f.center = target.position().add(0.0, 0.1, 0.0);
        }
        long left = f.endsAt - now;
        if (left % Math.max(1, f.behavior.period()) != 0) return true;
        f.behavior.pulse(owner, level, f);
        return true;
    }

    private static void pulseVerdict(ServerPlayer owner, ServerLevel level, Field f) {
        AABB box = new AABB(f.center, f.center).inflate(f.radius, 4.0, f.radius);
        double r2 = f.radius * f.radius;
        int hit = 0;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !x.isSpectator() && x != owner && !x.isAlliedTo(owner))) {
            if (hit >= JudgmentRules.MAX_FIELD_TARGETS) break;
            double dx = t.getX() - f.center.x, dz = t.getZ() - f.center.z;
            if (dx * dx + dz * dz > r2) continue;
            hit++;
            JudgmentEffects.silence(t, 30);
            t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, 30, 0, false, true, true));
            JudgmentDamage.share(owner, t, JudgmentRules.VERDICT_PULSE_SHARE);
        }
    }

    /**
     * 3.21.0 (setting sandalphonUpgrades): the last toll. When a Verdict of Ruin runs out by itself (its time is up and its owner is still here, not when an edict, a death, a logout or a newer field ended it) it takes
     * {@link JudgmentRules#VERDICT_TOLL_SHARE} of the maximum health of every foe still inside, one bounding-box query, at most {@link JudgmentRules#MAX_FIELD_TARGETS} creatures, one sound and no packet.
     */
    private static void tollVerdict(ServerLevel level, Field f) {
        if (!f.kind.equals("verdict") || !JudgmentConfig.sandalphonUpgrades() || level.getGameTime() < f.endsAt) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(f.owner);
        if (owner == null || !owner.isAlive() || owner.serverLevel() != level) return;
        AABB box = new AABB(f.center, f.center).inflate(f.radius, 4.0, f.radius);
        double r2 = f.radius * f.radius;
        int hit = 0;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !x.isSpectator() && x != owner && !x.isAlliedTo(owner))) {
            if (hit >= JudgmentRules.MAX_FIELD_TARGETS) break;
            double dx = t.getX() - f.center.x, dz = t.getZ() - f.center.z;
            if (dx * dx + dz * dz > r2) continue;
            hit++;
            JudgmentDamage.share(owner, t, JudgmentRules.VERDICT_TOLL_SHARE);
        }
        if (hit > 0) JudgmentFx.toll(level, f.center);
    }

    public static void onLogout(UUID owner) { clearOf(owner); }
}
