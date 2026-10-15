package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the small shared helpers of the twelve weapons of wave 20 (which relic is in hand, who counts as a friend, the foe in front, a safe place to land,
 * the foes along a line), so the acts and the event handlers say the same thing the same way.
 */
final class Wave20Util {
    private Wave20Util() {}

    static String heldId(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() ? "" : Relics.of(s).map(RelicDef::id).orElse("");
    }

    /** The registry path of an entity's type ("ender_dragon"). */
    static String path(LivingEntity e) { return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(); }

    static String namespace(LivingEntity e) { return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace(); }

    static boolean isVoid(LivingEntity e) { return e != null && !(e instanceof Player) && Wave20Rules.isVoidCreature(namespace(e), path(e)); }

    static boolean isDraconic(LivingEntity e) { return e != null && !(e instanceof Player) && Wave20Rules.isDraconic(path(e)); }

    static boolean isBig(LivingEntity e) { return e.getMaxHealth() > Wave20Rules.BOSS_HEALTH; }

    /** The Labyrinth and the Living Dungeon are unbreakable: acts that would change blocks stay away from them. */
    static boolean protectedWorld(ServerLevel level) { return Labyrinth.in(level) || LivingDungeon.in(level); }

    /** The wielder, teammates, their pets and relic summons. */
    static boolean friend(ServerPlayer p, LivingEntity t) {
        if (t == p) return true;
        if (t instanceof Player o && (o.isSpectator() || !o.isAlive())) return false;
        if (t.isAlliedTo(p)) return true;
        if (t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())) return true;
        return t.getPersistentData().hasUUID("nusmp_relic_owner") && p.getUUID().equals(t.getPersistentData().getUUID("nusmp_relic_owner"));
    }

    /** The foe in front of the wielder: within the range and the narrow cone of the look direction. */
    static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestDot = 0.86;
        for (LivingEntity t : Fx.foesAround(p, range)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 0.1) continue;
            double dot = to.scale(1.0 / len).dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = t;
            }
        }
        return best;
    }

    /** The foes within {@code radius} of a point (at most 24, not the wielder, no friends, never creative or spectator players). */
    static List<LivingEntity> foesNear(ServerPlayer p, Vec3 c, double radius) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius), x -> x != p && x.isAlive() && !x.isSpectator() && x.distanceToSqr(c) <= radius * radius)) {
            if (found.size() >= 24) break;
            if (e instanceof Player o && (o.isCreative() || !p.canHarmPlayer(o))) continue;
            if (friend(p, e) || e.getPersistentData().hasUUID("nusmp_relic_owner")) continue;
            found.add(e);
        }
        return found;
    }

    /** The foes along the look line: within {@code width} of the ray and no farther than {@code range} (stopped by the first solid block), nearest first, at most {@code max}. */
    static List<LivingEntity> foesOnLine(ServerPlayer p, double range, double width, int max) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double len = lineLength(p, range);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, range)) {
            Vec3 c = t.getBoundingBox().getCenter();
            double d = Wave20Rules.distToRay(c.x, c.y, c.z, eye.x, eye.y, eye.z, look.x, look.y, look.z, len);
            if (d >= 0 && d <= width + t.getBbWidth() * 0.5) out.add(t);
            if (out.size() >= max) break;
        }
        return out;
    }

    /** How far the look line runs before a solid block stops it. */
    static double lineLength(ServerPlayer p, double range) {
        var hit = p.pick(range, 1.0f, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return Math.max(1.0, Math.sqrt(hit.getLocation().distanceToSqr(p.getEyePosition())));
        return range;
    }

    /** A place to stand near {@code at}: room for the player and ground within three blocks, no lava; null if there is none. */
    static Vec3 safeSpot(ServerLevel level, ServerPlayer p, Vec3 at) {
        for (int dy : new int[]{0, 1, -1, 2, -2, 3}) {
            double y = at.y + dy;
            AABB box = p.getBoundingBox().move(at.x - p.getX(), y - p.getY(), at.z - p.getZ());
            if (!level.noCollision(p, box)) continue;
            BlockPos feet = BlockPos.containing(at.x, y, at.z);
            boolean ground = false;
            for (int i = 1; i <= 3 && !ground; i++) {
                var below = level.getBlockState(feet.below(i));
                if (below.is(net.minecraft.world.level.block.Blocks.LAVA) || below.is(net.minecraft.world.level.block.Blocks.FIRE)) return null;
                ground = !below.getCollisionShape(level, feet.below(i)).isEmpty();
            }
            if (ground && !level.getFluidState(feet).is(net.minecraft.tags.FluidTags.LAVA)) return new Vec3(at.x, y, at.z);
        }
        return null;
    }

    /** Takes a mob effect off a creature by id (skipped if the id is unknown). */
    static void removeEffect(LivingEntity e, String id) {
        try {
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
            if (holder.isPresent()) e.removeEffect(holder.get());
        } catch (RuntimeException ignored) {
            // an unknown id: nothing to remove
        }
    }
}
