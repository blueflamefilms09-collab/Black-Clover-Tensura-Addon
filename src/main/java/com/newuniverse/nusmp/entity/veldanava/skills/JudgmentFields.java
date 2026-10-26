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
 * Why: Verdict of Ruin is a field of true damage that follows its target, and the skills that come after it (Spatial Collapse, Realm of Magic, Kingdom of God) are fields too. This is the one
 * manager of them: a small list, ticked once per server tick, with hard limits so no number of players can make it expensive.
 *
 * <p>Limits: at most {@code fieldsPerPlayer} per owner (a new one replaces the owner's oldest) and {@code fieldsTotal} on the server (a new one over that is refused); a field lasts at most
 * {@link JudgmentRules#VERDICT_TICKS}; it pulses every {@link JudgmentRules#FIELD_PULSE_TICKS} ticks and each pulse touches at most {@link JudgmentRules#MAX_FIELD_TARGETS} creatures with one
 * bounding-box query; it sends no packets (the client draws the field from the one effect packet the skill sent when it was raised). A field ends when its time is up, when its owner dies, logs out or leaves the dimension, when
 * the creature it follows dies or leaves, and when the server stops. With no field, the tick returns on its first line.
 */
public final class JudgmentFields {
    private JudgmentFields() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("s.veldanava.fields");

    /** One live field. */
    static final class Field {
        final UUID owner;
        final ResourceKey<Level> dimension;
        final UUID follow;                                    // the creature it follows, or null for a field fixed in the ground
        Vec3 center;
        final double radius;
        final long endsAt;

        Field(UUID owner, ResourceKey<Level> dimension, UUID follow, Vec3 center, double radius, long endsAt) {
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

    /** Raises a field. {@code follow} may be null. Returns false when the server-wide limit refuses it. Server thread only. */
    public static boolean add(ServerPlayer owner, ServerLevel level, LivingEntity follow, Vec3 center, double radius, int ticks) {
        int perPlayer = Math.max(1, JudgmentConfig.fieldsPerPlayer());
        while (countOf(owner.getUUID()) >= perPlayer) removeOldestOf(owner.getUUID());
        if (FIELDS.size() >= Math.max(1, JudgmentConfig.fieldsTotal())) return false;
        FIELDS.add(new Field(owner.getUUID(), level.dimension(), follow == null ? null : follow.getUUID(), center, radius, level.getGameTime() + Math.min(ticks, JudgmentRules.VERDICT_TICKS)));
        return true;
    }

    private static void removeOldestOf(UUID owner) {
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
            if (it.next().owner.equals(owner)) { it.remove(); return; }
        }
    }

    /** Ends every field that touches the sphere (the Supreme Edict cancels domains). Returns how many were ended. */
    public static int clearAround(ServerLevel level, Vec3 center, double radius) { return clearAround(level, center, radius, null); }

    /** The same, but the fields of {@code keep} stay (a caster's own edict does not end the caster's own field). */
    public static int clearAround(ServerLevel level, Vec3 center, double radius, UUID keep) {
        int n = 0;
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            if (keep != null && f.owner.equals(keep)) continue;
            if (f.dimension.equals(level.dimension()) && f.center.distanceTo(center) <= radius + f.radius) { it.remove(); n++; }
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
                if (!step(server, f, now)) it.remove();
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
        if (left % JudgmentRules.FIELD_PULSE_TICKS != 0) return true;
        pulse(owner, level, f);
        return true;
    }

    private static void pulse(ServerPlayer owner, ServerLevel level, Field f) {
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

    public static void onLogout(UUID owner) { clearOf(owner); }
}
