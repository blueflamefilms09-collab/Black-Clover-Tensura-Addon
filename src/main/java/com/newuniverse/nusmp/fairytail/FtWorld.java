package com.newuniverse.nusmp.fairytail;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the Fairy Tail spells need the same few things from the world: who may be hurt (never the caster, an ally, a pet, a summon, a
 * creative or spectating player or an armour stand), who may be healed, where the crosshair lands, how to hit through the balance law and
 * how to run something a few ticks later (the inhale before a roar, the pulses of a rain, the fuse of a bomb). This class is that and
 * nothing else. Every area query is capped (24 creatures, 48 blocks) so no cast can scan or hurt an unbounded crowd; delayed work is a
 * bounded queue drained on the server tick (a failing task is logged once and dropped, a world that stops empties it). Server side only.
 */
public final class FtWorld {
    private FtWorld() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final int MAX_FOES = 24;
    private static final double MAX_RADIUS = 48.0;
    private static final String OWNER_KEY = "nusmp_owner", RELIC_OWNER_KEY = "nusmp_relic_owner";

    // ------------------------------------------------------------------------------------------------------------------ who
    static boolean isSummon(Entity e) {
        var data = e.getPersistentData();
        return data.hasUUID(OWNER_KEY) || data.hasUUID(RELIC_OWNER_KEY);
    }

    /** Anything a spell of this player may hurt by an area. */
    static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (isSummon(e)) return false;
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** Anything a deliberate aimed spell may hurt (also a neutral creature the player points at). */
    static boolean isAimable(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (isSummon(e)) return false;
        return !(e instanceof OwnableEntity own && own.getOwnerUUID() != null);
    }

    static List<LivingEntity> foesAround(ServerPlayer p, Vec3 center, double radius) {
        if (p == null || center == null || !p.isAlive()) return List.of();
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                e -> isFoe(p, e) && e.distanceToSqr(center) <= r * r);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    /** Foes in front of the caster within {@code range}, with cos(angle to the look direction) at least {@code minDot}. */
    static List<LivingEntity> foesInCone(ServerPlayer p, double range, double minDot) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1.0F);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : foesAround(p, p.position(), range + 1.5)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len > range + e.getBbWidth()) continue;
            if (len < 0.01 || to.scale(1.0 / len).dot(look) >= minDot) out.add(e);
        }
        return out;
    }

    /** Foes within {@code width} of the segment from-to. */
    static List<LivingEntity> foesAlong(ServerPlayer p, Vec3 from, Vec3 to, double width) {
        if (p == null || from == null || to == null || !p.isAlive()) return List.of();
        double w = Mth.clamp(width, 0.3, 6.0);
        AABB box = new AABB(from, to).inflate(w + 0.5);
        Vec3 seg = to.subtract(from);
        double len2 = Math.max(1.0e-6, seg.lengthSqr());
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!isFoe(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Mth.clamp(c.subtract(from).dot(seg) / len2, 0.0, 1.0);
            return from.add(seg.scale(t)).distanceTo(c) <= w + e.getBbWidth() * 0.5;
        });
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    /** Players round a point, and the caster's own pets and summons (at most 24). */
    static List<LivingEntity> alliesAround(ServerPlayer p, Vec3 center, double radius) {
        if (p == null || center == null) return List.of();
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) <= r * r
                        && (e instanceof Player || ownedBy(e, p)));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    private static boolean ownedBy(Entity e, ServerPlayer p) {
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return true;
        var data = e.getPersistentData();
        return data.hasUUID(OWNER_KEY) && p.getUUID().equals(data.getUUID(OWNER_KEY));
    }

    /** The first aimable living thing under the crosshair within range (blocks stop the search), or null. */
    static LivingEntity lookFoe(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition(), end = aimPoint(p, range);
        AABB box = p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.serverLevel(), p, start, end, box,
                e -> e instanceof LivingEntity le && le.isAlive() && !e.isSpectator() && e != p && isAimable(p, le));
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Where the crosshair ends: the first block within range, or the point at the full range. */
    static Vec3 aimPoint(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1.0F).scale(range));
        HitResult hit = p.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // ------------------------------------------------------------------------------------------------------------------ damage
    /** One hit from the caster through the balance law (EP scaling, mastery, the PvP cap). Returns the health actually taken. */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac, boolean magic) {
        if (caster == null || target == null || !target.isAlive() || raw <= 0 || !isAimable(caster, target)) return 0f;
        float dmg = BalanceLaw.damage(caster, target, (float) raw, masteryFrac);
        if (dmg <= 0f) return 0f;
        float before = target.getHealth();
        DamageSource src = magic ? caster.damageSources().indirectMagic(caster, caster) : caster.damageSources().playerAttack(caster);
        target.hurt(src, dmg);
        return Math.max(0f, before - target.getHealth());
    }

    /** Pushes the target away from a point (horizontal), a little up. */
    static void shove(LivingEntity target, Vec3 from, double strength) {
        Vec3 d = target.position().subtract(from);
        Vec3 h = new Vec3(d.x, 0, d.z);
        h = h.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : h.normalize();
        target.push(h.x * strength, 0.25 + 0.1 * strength, h.z * strength);
        target.hurtMarked = true;
    }

    // ------------------------------------------------------------------------------------------------------------------ effects
    static VfxShape shape(String name) {
        try {
            return VfxShape.valueOf(name);
        } catch (IllegalArgumentException missing) {
            return VfxShape.MAGIC_CIRCLE_EXPLOSION;
        }
    }

    /** One of the mod's effect shapes. A failure to send is never the spell's failure. */
    static void fx(ServerLevel level, String shapeName, Vec3 from, Vec3 to, int argb, int duration, float power) {
        if (level == null || shapeName == null || from == null || to == null) return;
        try {
            VfxSpawn.send(level, shape(shapeName), from, to, argb, duration, power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only
        }
    }

    static void fxFollow(ServerLevel level, String shapeName, Entity who, Vec3 to, int argb, int duration, float power) {
        if (level == null || shapeName == null || who == null || to == null) return;
        try {
            VfxSpawn.sendFollowing(level, shape(shapeName), who, to, argb, duration, power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only
        }
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        if (level == null || at == null || sound == null) return;
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ------------------------------------------------------------------------------------------------------------------ later
    private record Task(long due, long seq, Runnable run) {}

    private static final List<Task> QUEUE = new ArrayList<>();
    private static final int CAPACITY = 1024;
    private static long seq;
    private static boolean registered;

    /** Starts the server-tick listener (idempotent). Called by {@link FairyTailSystem#init}. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(FtWorld::onServerTick);
        NeoForge.EVENT_BUS.addListener(FtWorld::onServerStopping);
    }

    /** Runs {@code task} on the server thread {@code delayTicks} server ticks from now. False when the queue is full (the spell then just does not follow up). */
    static synchronized boolean later(ServerLevel level, int delayTicks, Runnable task) {
        if (task == null || QUEUE.size() >= CAPACITY) return false;
        QUEUE.add(new Task(level.getServer().getTickCount() + Math.max(0, delayTicks), seq++, task));
        return true;
    }

    /** How many follow-ups wait (the self-test uses it). */
    static synchronized int pending() { return QUEUE.size(); }

    private static void onServerTick(ServerTickEvent.Post event) {
        List<Task> due = new ArrayList<>();
        long now = event.getServer().getTickCount();
        synchronized (FtWorld.class) {
            for (Task t : QUEUE) if (t.due() <= now) due.add(t);
            if (due.isEmpty()) return;
            QUEUE.removeAll(due);
        }
        due.sort(Comparator.comparingLong(Task::due).thenComparingLong(Task::seq));
        for (Task t : due) {
            try {
                t.run().run();
            } catch (Throwable ex) {
                SafeCast.fault("fairytail.later", ex);
                LOG.warn("[nusmp] a Fairy Tail follow-up failed and was dropped: {}", ex.toString());
            }
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        synchronized (FtWorld.class) { QUEUE.clear(); }
    }
}
