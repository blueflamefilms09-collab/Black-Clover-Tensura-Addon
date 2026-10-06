package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Helpers shared by the DanMachi god and magic skills. */
final class DMUtil {
    private DMUtil() {}

    static float dmg(double base) { return (float) (base * NUConfig.DM_DAMAGE_MULT.get()); }
    static double cost(double base) { return base * NUConfig.DM_COST_MULT.get(); }
    static int cooldown(int baseTicks) { return (int) Math.round(baseTicks * NUConfig.DM_COOLDOWN_MULT.get()); }

    /** Where the player's crosshair hits a block (or max range). */
    static Vec3 lookPoint(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
        HitResult hit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Living things (not the caster, not other players' allies logic) within radius of a point. */
    static List<LivingEntity> around(ServerPlayer player, Vec3 center, double radius) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) <= radius * radius);
    }
}
