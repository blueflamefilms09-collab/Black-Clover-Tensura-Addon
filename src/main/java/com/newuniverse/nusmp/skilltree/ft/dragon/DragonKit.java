package com.newuniverse.nusmp.skilltree.ft.dragon;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Why: both Dragon Slayer routes need the same small geometry and effect helpers (a flat look direction, the mouth and the chest, the first wall on a ray, a dash that stops at a
 * wall, the nearest foe on a lane, a knock-back that also reaches a player's client, a stun that goes through the balance law, a magicule refill through Tensura's own gain
 * path), and writing them once is what keeps the rules of the balance law (one hard control per 12 s, capped durations, no teleport through a wall) the same in all fifteen
 * techniques. Everything here runs on the server thread, holds no state, and uses only the mod's neutral APIs ({@link AbilityKit}, {@link BalanceLaw}, {@link Tells}, Tensura's
 * energy helper): nothing of the Black Clover grimoire, which is the isolation the owner asked for. Hits deliberately do NOT reset the target's hurt-frames: a technique that
 * lands twice has to space its pulses by the vanilla ten ticks, the same economy every other attack in the game obeys, so a barrage cannot out-damage the balance law's cap per hit.
 */
public final class DragonKit {
    private DragonKit() {}

    // ------------------------------------------------------------------ the numbers
    /**
     * The tuning number {@code key} of {@code node} at {@code level}: what the loaded route says now (so a datapack retunes a technique), else the shipped default in
     * {@code tables} (so a deleted key or a missing route never leaves a hole in a cast or a passive).
     */
    public static double value(DragonTables tables, String node, String key, int level) {
        return RouteRegistry.value(tables.route(), node, key, level, tables.def(node, key, level));
    }

    // ------------------------------------------------------------------ where things are
    /** The horizontal look direction (unit), built from the yaw so that a caster looking straight up or down still has one. */
    public static Vec3 flatLook(Entity e) {
        double yaw = Math.toRadians(e.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    /** {@code v} turned about the vertical axis by {@code degrees}. */
    public static Vec3 yawed(Vec3 v, double degrees) {
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    /** The horizontal unit vector pointing to the caster's right-hand side when facing {@code flatDir}. */
    public static Vec3 rightOf(Vec3 flatDir) { return new Vec3(-flatDir.z, 0.0, flatDir.x); }

    /** The horizontal unit vector from {@code origin} to {@code t} (the way a blast pushes it), or {@code fallback} when they are on top of each other. */
    public static Vec3 awayFrom(Vec3 origin, Entity t, Vec3 fallback) {
        Vec3 d = t.position().subtract(origin);
        double len = Math.sqrt(d.x * d.x + d.z * d.z);
        return len < 1.0e-3 ? fallback : new Vec3(d.x / len, 0.0, d.z / len);
    }

    /** Where a breath leaves the caster: just in front of the eyes, a little low. */
    public static Vec3 mouth(ServerPlayer p) { return p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0.0, -0.18, 0.0); }

    /** Where a punch or a thrown thing leaves the caster: in front of the chest. */
    public static Vec3 fist(ServerPlayer p) { return p.position().add(0.0, p.getBbHeight() * 0.62, 0.0).add(flatLook(p).scale(0.7)); }

    public static Vec3 center(Entity e) { return e.getBoundingBox().getCenter(); }

    /** The end of the segment {@code from} to {@code to}, or the first solid block on it. */
    public static Vec3 wallEnd(ServerPlayer p, Vec3 from, Vec3 to) {
        BlockHitResult hit = p.serverLevel().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /**
     * Foes in the cone in front of the caster that the caster can SEE: a blow or a breath that comes out of the body does not pass through a wall, which
     * {@link AbilityKit#cone} alone would allow (it measures angle and distance only). Uses the game's own eye-to-eye line-of-sight test.
     */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double halfAngleDeg) { return sight(p, AbilityKit.cone(p, range, halfAngleDeg)); }

    /** The foes of {@code foes} that {@code p} has a clear line to (the game's eye-to-eye test); the rest are behind a wall. */
    public static List<LivingEntity> sight(ServerPlayer p, List<LivingEntity> foes) {
        List<LivingEntity> seen = new ArrayList<>(foes.size());
        for (LivingEntity e : foes) if (p.hasLineOfSight(e)) seen.add(e);
        return seen;
    }

    /** The nearest foe within {@code width} of the segment {@code a} to {@code b}, or null. */
    public static LivingEntity nearestAlong(ServerPlayer p, Vec3 a, Vec3 b, double width) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : AbilityKit.along(p, a, b, width)) {
            double d = e.distanceToSqr(a);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    /**
     * What the caster is pointing at: the first foe on the look ray, else the first wall, else (the ray found nothing within the range) the ground below the end of the ray, so
     * an area technique aimed at the sky still lands where foes can stand.
     */
    public static Vec3 aimPoint(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 far = eye.add(p.getViewVector(1f).scale(range));
        BlockHitResult wall = p.serverLevel().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        boolean blocked = wall.getType() != HitResult.Type.MISS;
        Vec3 end = blocked ? wall.getLocation() : far;
        LivingEntity foe = nearestAlong(p, eye, end, 0.7);
        if (foe != null) return center(foe);
        return blocked ? end : wallEnd(p, end, end.subtract(0.0, 24.0, 0.0));
    }

    /** The first solid surface within four blocks below {@code v} (a ring of warning dust or a strike point belongs on the ground, not at a foe's waist), else {@code v} itself. */
    public static Vec3 groundBelow(ServerPlayer p, Vec3 v) {
        BlockHitResult hit = p.serverLevel().clip(new ClipContext(v.add(0.0, 0.5, 0.0), v.subtract(0.0, 4.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? v : hit.getLocation();
    }

    /**
     * The farthest point up to {@code max} blocks along {@code dir} that the caster's body fits in without touching a block: the dash stops at the first wall, so a charge or
     * a blink can never put the player inside a block or through a wall. Returns the caster's own position when nothing is free.
     */
    public static Vec3 clearDash(ServerPlayer p, Vec3 dir, double max) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        Vec3 best = from;
        for (double d = 0.5; d <= max + 1.0e-6; d += 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (!level.noCollision(p, p.getBoundingBox().move(c.subtract(from)))) break;
            best = c;
        }
        return best;
    }

    // ------------------------------------------------------------------ hitting
    /** 0..1 how practised the skill is, for the balance law. */
    public static double mastery(ManasSkillInstance i) { return Math.min(1.0, i.getMastery() / (double) Math.max(1, i.getMaxMastery())); }

    /** Whether the caster can still follow up a delayed technique: alive, connected and in the level where the technique began. */
    public static boolean ready(ServerPlayer p, ServerLevel level) { return p.isAlive() && !p.hasDisconnected() && !p.isRemoved() && p.serverLevel() == level; }

    /**
     * Hits up to {@code max} of the foes, the nearest to {@code from} first, each through the balance law. {@code rider} runs on every foe that was hit (burn, numbing,
     * knock-back). Returns the foes that were hit.
     */
    public static List<LivingEntity> strike(ServerPlayer p, Vec3 from, List<LivingEntity> foes, int max, float raw, double mastery, Consumer<LivingEntity> rider) {
        List<LivingEntity> sorted = new ArrayList<>(foes);
        sorted.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity t : sorted) {
            if (hit.size() >= Math.max(1, max)) break;
            if (AbilityKit.hit(p, t, raw, mastery)) {
                hit.add(t);
                if (rider != null) rider.accept(t);
            }
        }
        return hit;
    }

    /** Pushes {@code t} along {@code dir} (horizontal) and makes sure a player's client hears of it. */
    public static void knock(LivingEntity t, Vec3 dir, double strength) {
        if (strength <= 0.0 || dir.x * dir.x + dir.z * dir.z < 1.0e-6) return;
        t.knockback(strength, -dir.x, -dir.z);
        t.hurtMarked = true;
    }

    /** Throws the target straight up by {@code power} (a kick that launches). */
    public static void launch(LivingEntity t, double power) {
        if (power <= 0.0) return;
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x, Math.max(v.y, power), v.z);
        t.hurtMarked = true;
    }

    /** A short numbing slowdown (not hard control: it only drags the target to a crawl). */
    public static void numb(LivingEntity t, double seconds, int amplifier) {
        int ticks = (int) Math.round(seconds * 20.0);
        if (ticks > 0) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier));
    }

    /**
     * Hard control: Tensura's paralysis for the time the balance law allows this target (capped, shorter on a boss, halved when repeated), or a root of the same length if
     * Tensura has no such effect. Call it ONLY after {@link AbilityKit#control} returned true; that is the shared one-per-12-seconds gate.
     */
    public static void stun(LivingEntity t, int baseTicks) {
        int ticks = BalanceLaw.controlTicks(t, baseTicks);
        Optional<? extends Holder<MobEffect>> paralysis = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", "paralysis"));
        if (paralysis.isPresent()) t.addEffect(new MobEffectInstance(paralysis.get(), ticks, 0));
        else t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
    }

    /** Gives back a share (0..1) of the caster's maximum magicules through Tensura's own gain path, which respects its gain multipliers. */
    public static void gainMagicule(ServerPlayer p, double share) {
        if (share > 0.0) EnergyHelper.gainMagicule(p, EnergyHelper.getMaxMagicule(p) * share, EnergyHelper.GainType.NORMAL);
    }

    /** Effects are sent for at most this many of the foes a technique hit: a full pack would otherwise flood every nearby client with packets. */
    public static final int MAX_FOE_EFFECTS = 6;

    /** One effect on each of the first {@link #MAX_FOE_EFFECTS} foes that were hit (a burst on the body). */
    public static void burstOn(ServerLevel level, VfxShape shape, List<LivingEntity> hit, int ticks, float size) {
        for (int i = 0; i < hit.size() && i < MAX_FOE_EFFECTS; i++) {
            Vec3 c = center(hit.get(i));
            AbilityKit.fx(level, shape, c, c, 0, ticks, size);
        }
    }

    /** A lightning arc from {@code from} to each of the first {@link #MAX_FOE_EFFECTS} foes that were hit. */
    public static void arcsTo(ServerLevel level, Vec3 from, List<LivingEntity> hit, int ticks, float size) {
        for (int i = 0; i < hit.size() && i < MAX_FOE_EFFECTS; i++) AbilityKit.fx(level, VfxShape.FX_LIGHTNING_ARC, from, center(hit.get(i)), 0, ticks, size);
    }

    /** A sound at a point of the world (an impact), not at the caster. */
    public static void soundAt(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, BlockPos.containing(at), sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ------------------------------------------------------------------ tells (the warning dust of the zogratis tells, server side only)
    public static void tellLine(ServerLevel level, Vec3 a, Vec3 b, int rgb) { Tells.line(level, a, b, rgb); }

    public static void tellRing(ServerLevel level, Vec3 center, double radius, int rgb) { Tells.ring(level, center, radius, rgb); }

    public static void tellColumn(ServerLevel level, Vec3 base, double height, int rgb) { Tells.column(level, base, height, rgb); }

    public static void tellGather(ServerLevel level, Vec3 at, double radius, int rgb, int count) { Tells.gather(level, at, radius, rgb, count); }

    /** The two edges of a cone, drawn as two lines of warning dust from the caster outwards. */
    public static void tellCone(ServerLevel level, Vec3 origin, Vec3 flatDir, double range, double halfAngleDeg, int rgb) {
        tellLine(level, origin, origin.add(yawed(flatDir, halfAngleDeg).scale(range)), rgb);
        tellLine(level, origin, origin.add(yawed(flatDir, -halfAngleDeg).scale(range)), rgb);
    }
}
