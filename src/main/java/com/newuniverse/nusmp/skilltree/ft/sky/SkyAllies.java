package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: a support route is only as good as its answer to "who is my friend", and the answer has to match the rest of the mod: the caster; the people who play with them (the read-only
 * party seam the dungeon quests already use, {@code SystemParty}, so a Sky Dragon Slayer heals exactly the group the quests count); players on the same scoreboard team (a server's own
 * teams still work); and tamed pets of any of those, because a healer who ignores the party's wolf is a bad healer. Everything else is not an ally, so a heal never reaches a stranger's
 * pet or a monster. The decision is made from facts the game already keeps (the party list, the team, the pet's owner id) and nothing is stored, so there is no new state to keep in sync
 * or to clean up when the player leaves. Only creatures that are alive and not spectating count, and only those inside the radius, measured from the point the technique names (the caster
 * for a circle, the end of a cone for a breath that carries a cure).
 */
public final class SkyAllies {
    private SkyAllies() {}

    /** The allies of {@code caster} within {@code radius} of {@code centre}, the caster included when inside the circle. A new list, in no particular order. */
    public static List<LivingEntity> around(ServerPlayer caster, Vec3 centre, double radius) {
        Set<UUID> friends = FtWorld.friendIds(caster);
        double r2 = radius * radius;
        List<LivingEntity> out = new ArrayList<>();
        if (caster.isAlive() && !caster.isSpectator() && caster.distanceToSqr(centre) <= r2) out.add(caster);        // the caster is always their own ally, found or not by the level's entity lookup
        for (LivingEntity e : caster.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius), x -> x.isAlive() && !x.isSpectator())) {
            if (e != caster && e.distanceToSqr(centre) <= r2 && isAlly(caster, e, friends)) out.add(e);
        }
        return out;
    }

    /** Is {@code e} a friend of {@code caster}: themselves, a party member, a teammate, or a tamed pet of one of those? */
    public static boolean isAlly(ServerPlayer caster, LivingEntity e, Set<UUID> friends) {
        if (friends.contains(e.getUUID())) return true;
        if (e instanceof Player && caster.isAlliedTo(e)) return true;
        if (e instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            if (friends.contains(pet.getOwnerUUID())) return true;
            LivingEntity owner = pet.getOwner();
            return owner != null && caster.isAlliedTo(owner);
        }
        return false;
    }
}
