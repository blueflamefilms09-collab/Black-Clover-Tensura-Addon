package com.newuniverse.nusmp.shishou;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: who may Shishou hurt? One answer for his strikes, his spells, the Demon Lord's Haki and the Doppelganger: a hostile mob (an {@link Enemy} or a monster by category, which includes the Tensura and
 * Black Clover mobs that are monsters), that is NEVER a player, a villager or other NPC, a tamed or owned creature (any {@link OwnableEntity} with an owner), another Shishou, or an ally of the wielder
 * (same team). And ONE bounded way to find them: {@link #near} is a single box query around a point, filtered, cut at a cap, nearest first, so a use costs one scan and never more than {@code cap} results.
 */
public final class ShishouTargets {
    private ShishouTargets() {}

    /** May Shishou (with this wielder, or none) hurt this creature? */
    public static boolean hostileFor(Player wielder, LivingEntity t) {
        if (t == null || !t.isAlive() || t instanceof Player || t instanceof Npc || t instanceof AbstractVillager || t instanceof com.newuniverse.nusmp.shishou.ShishouEntity) return false;
        if (!(t instanceof Mob)) return false;
        if (!(t instanceof Enemy) && t.getType().getCategory() != MobCategory.MONSTER) return false;
        if (t instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;
        if (t instanceof TamableAnimal ta && ta.isTame()) return false;
        if (wielder != null && (wielder.isAlliedTo(t) || t.isAlliedTo(wielder))) return false;
        return true;
    }

    /** A creature the pet sword must never strike: a player, an NPC or villager, another Shishou, a tamed or owned creature, an ally of the wielder. */
    public static boolean protectedKind(Player wielder, LivingEntity t) {
        if (t == null || t instanceof Player || t instanceof Npc || t instanceof AbstractVillager || t instanceof com.newuniverse.nusmp.shishou.ShishouEntity) return true;
        if (t instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (t instanceof TamableAnimal ta && ta.isTame()) return true;
        return wielder != null && (wielder.isAlliedTo(t) || t.isAlliedTo(wielder));
    }

    /** May the pet sword attack this creature for this reason ({@link ShishouPet#mayAttack})? */
    public static boolean petMayAttack(ShishouPet.Reason reason, Player wielder, LivingEntity t) {
        if (t == null || !t.isAlive() || !(t instanceof Mob m)) return false;
        boolean hostile = t instanceof Enemy || t.getType().getCategory() == MobCategory.MONSTER;
        return ShishouPet.mayAttack(reason, hostile, m.getTarget() == wielder && wielder != null, protectedKind(wielder, t));
    }

    /** Hostile or neutral (Magic Sense's overlay): any non-player, non-NPC, non-owned living thing. */
    public static boolean markable(Player wielder, LivingEntity t, boolean neutrals) {
        if (t == null || !t.isAlive() || t instanceof Player || t instanceof Npc || t instanceof AbstractVillager || t instanceof com.newuniverse.nusmp.shishou.ShishouEntity) return false;
        if (t instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;
        if (t instanceof TamableAnimal ta && ta.isTame()) return false;
        if (wielder != null && (wielder.isAlliedTo(t) || t.isAlliedTo(wielder))) return false;
        boolean hostile = t instanceof Enemy || t.getType().getCategory() == MobCategory.MONSTER;
        if (hostile) return true;
        return neutrals && t instanceof Mob m && t.getType().getCategory() != MobCategory.MISC && m.getTarget() != null;
    }

    /**
     * One box query: the creatures of {@code type} within {@code radius} of {@code center} that pass {@code filter}, nearest first, at most {@code cap}. The only scan of a skill use.
     */
    public static <T extends Entity> List<T> near(ServerLevel level, Class<T> type, Vec3 center, double radius, int cap, Predicate<? super T> filter) {
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        double r2 = radius * radius;
        List<T> found = level.getEntitiesOfClass(type, box, e -> e.distanceToSqr(center) <= r2 && filter.test(e));
        if (found.size() > 1) found.sort((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)));
        if (found.size() > cap) return new ArrayList<>(found.subList(0, Math.max(0, cap)));
        return found;
    }

    /** The hostile creatures near a point (the usual use), nearest first, at most {@code cap}. */
    public static List<Mob> hostilesNear(ServerLevel level, Player wielder, Vec3 center, double radius, int cap) {
        return near(level, Mob.class, center, radius, cap, m -> hostileFor(wielder, m));
    }

    /** The creature nearest to the wielder's look line within 32 blocks that the sword may hunt, or null. */
    public static net.minecraft.world.entity.Mob aimedPrey(net.minecraft.server.level.ServerPlayer p, ShishouEntity sword, Vec3 look) {
        net.minecraft.world.phys.AABB box = p.getBoundingBox().expandTowards(look.scale(32.0)).inflate(3.0);
        Vec3 eye = p.getEyePosition();
        net.minecraft.world.entity.Mob best = null;
        double bestAngle = 0.12;                                                              // about seven degrees off the crosshair
        for (net.minecraft.world.entity.Mob m : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, m -> m.isAlive() && sword.isValidTarget(m) && !sword.isIgnored(m))) {
            Vec3 to = m.position().add(0.0, m.getBbHeight() * 0.5, 0.0).subtract(eye);
            double len = to.length();
            if (len < 1.0e-3 || len > 32.0) continue;
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, to.dot(look) / len)));
            if (angle < bestAngle) { bestAngle = angle; best = m; }
        }
        return best;
    }
}
