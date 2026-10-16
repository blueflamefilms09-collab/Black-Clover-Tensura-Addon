package com.newuniverse.nusmp.song;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a song reaches everyone around the singer, so who counts as a friend and who as a foe has to be decided in one place: the
 * singer, their team, other players without a team (the mod's usual rule, see the Time book) and tamed animals of those are allies;
 * hostile monsters and anything already hunting an ally are foes; peaceful animals and armour stands are left alone.
 */
public final class SongTargets {
    private SongTargets() {}

    public static boolean isAlly(ServerPlayer singer, LivingEntity e) {
        if (e == singer || singer.isAlliedTo(e)) return true;
        if (e instanceof Player other) return singer.getTeam() == null && other.getTeam() == null;
        if (e instanceof TamableAnimal pet && pet.getOwner() != null) return pet.getOwner() == singer || singer.isAlliedTo(pet.getOwner());
        return false;
    }

    public static boolean isFoe(ServerPlayer singer, LivingEntity e) {
        if (!e.isAlive() || e.isSpectator() || e instanceof ArmorStand || isAlly(singer, e)) return false;
        if (e instanceof Player other) return !other.isCreative() && singer.getServer() != null && singer.getServer().isPvpAllowed();
        if (e instanceof Enemy) return true;
        return e instanceof Mob m && m.getTarget() != null && isAlly(singer, m.getTarget());
    }

    public static List<LivingEntity> allies(ServerPlayer singer, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>(singer.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && e.distanceToSqr(c) <= r * r && isAlly(singer, e)));
        return out;
    }

    public static List<LivingEntity> foes(ServerPlayer singer, Vec3 c, double r) {
        return new ArrayList<>(singer.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.distanceToSqr(c) <= r * r && isFoe(singer, e)));
    }
}
