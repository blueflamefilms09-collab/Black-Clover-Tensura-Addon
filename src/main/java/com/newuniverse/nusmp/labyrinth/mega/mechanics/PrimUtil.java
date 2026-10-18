package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the small helpers every primitive group shares, so each primitive reads as its description and a bug (a damage that kills a full-health player in one blow, a push that does
 * not reach the client, a block id that does not exist) is fixed once. Damage per firing is capped at 70 % of the target's maximum health: a mechanic is a hazard, not an execution.
 */
final class PrimUtil {
    private PrimUtil() {}

    static boolean addEffect(LivingEntity e, String id, int ticks, int amp) {
        try {
            var h = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
            if (h.isEmpty()) return false;
            e.addEffect(new MobEffectInstance(h.get(), Math.max(1, ticks), Math.max(0, amp), false, true));
            return true;
        } catch (RuntimeException bad) {
            return false;
        }
    }

    static void removeEffect(LivingEntity e, String id) {
        try {
            BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).ifPresent(e::removeEffect);
        } catch (RuntimeException ignored) {
            // an unknown effect id: nothing to remove
        }
    }

    static DamageSource source(ServerLevel l, String kind) {
        var d = l.damageSources();
        return switch (kind) {
            case "fire" -> d.onFire();
            case "freeze" -> d.freeze();
            case "fall" -> d.fall();
            case "drown" -> d.drown();
            case "wither" -> d.wither();
            case "lightning" -> d.lightningBolt();
            case "magic" -> d.magic();
            default -> d.generic();
        };
    }

    /** Hurts a creature, capped so a single firing never takes more than 70 % of its maximum health. */
    static void hurt(ServerLevel l, LivingEntity t, double amount, String kind) {
        if (amount <= 0 || !t.isAlive()) return;
        double cap = Math.max(1.0, t.getMaxHealth() * 0.7);
        t.hurt(source(l, kind), (float) Math.min(amount, cap));
    }

    /** Adds to a creature's velocity and makes the client hear about it. */
    static void push(Entity t, double dx, double dy, double dz) {
        t.setDeltaMovement(t.getDeltaMovement().add(dx, dy, dz));
        t.hurtMarked = true;
    }

    /** The horizontal unit direction from a point to an entity (a random one when they coincide). */
    static Vec3 flat(Entity e, double fx, double fz, java.util.SplittableRandom rng) {
        double dx = e.getX() - fx, dz = e.getZ() - fz, d = Math.hypot(dx, dz);
        if (d < 0.01) {
            double a = rng.nextDouble() * Math.PI * 2;
            return new Vec3(Math.cos(a), 0, Math.sin(a));
        }
        return new Vec3(dx / d, 0, dz / d);
    }

    static void teleport(LivingEntity e, double x, double y, double z) {
        if (e instanceof ServerPlayer sp && sp.connection != null) sp.connection.teleport(x, y, z, sp.getYRot(), sp.getXRot());
        else e.teleportTo(x, y, z);
        e.fallDistance = 0;
    }

    /** The default state of a block id; air when the id is unknown (a primitive that places air places nothing). */
    static BlockState block(String id) {
        try {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            return b == null ? Blocks.AIR.defaultBlockState() : b.defaultBlockState();
        } catch (RuntimeException e) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    /** Does a living entity stand in this cell (a block must not be put on someone's head)? */
    static boolean occupied(ServerLevel l, BlockPos p) {
        return !l.getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(0.05), LivingEntity::isAlive).isEmpty();
    }
}
