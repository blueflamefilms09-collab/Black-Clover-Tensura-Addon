package com.newuniverse.nusmp.relics.wave;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/** Wave 4: what a worn relic's act can see: the creature under the crosshair, and plain effects on the wearer. */
final class Wave4Look {
    private Wave4Look() {}

    /** A living thing an act may touch: alive, and never a creative or spectator player. */
    static boolean plain(LivingEntity e) {
        if (e == null || !e.isAlive() || e.isSpectator()) return false;
        return !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
    }

    /**
     * The living thing under the crosshair within {@code range}; with {@code wallsBlock} a wall in between hides it (Scrying does not
     * care, a mind-blast does).
     */
    static LivingEntity target(ServerPlayer p, double range, boolean wallsBlock, Predicate<LivingEntity> ok) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1f).scale(range));
        if (wallsBlock) {
            HitResult wall = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end,
                p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                e -> e instanceof LivingEntity le && e != p && plain(le) && ok.test(le));
        if (hit == null) return null;
        Entity found = hit.getEntity();
        return found instanceof LivingEntity le ? le : null;
    }

    /** An ambient, particle-free effect on a creature (a worn relic's steady effect); a missing effect id is skipped. */
    static void ambient(LivingEntity e, String effectId, int ticks, int amp) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (id == null) return;
        BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(h -> e.addEffect(new MobEffectInstance(h, ticks, amp, true, false, true)));
    }

    /** The creature's magicules as a percent of its maximum (a Tensura stat), or -1 when it has none to read. */
    static int magiculePercent(LivingEntity e) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex == null) return -1;
            double max = EnergyHelper.getMaxMagicule(e);
            return max <= 0 ? -1 : (int) Math.max(0, Math.min(999, Math.round(100.0 * ex.getMagicule() / max)));
        } catch (RuntimeException | LinkageError noTensura) {
            return -1;
        }
    }

    /** {@code preferred} (a Tensura damage type) when the game has it, else {@code fallback}: {@link com.newuniverse.nusmp.relics.Fx#damage} turns an unknown type into a plain blow. */
    static String damageType(ServerPlayer p, String preferred, String fallback) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(preferred);
            if (id != null && p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(id)) return preferred;
        } catch (RuntimeException unknown) {
            // fall through to the plain type
        }
        return fallback;
    }
}
