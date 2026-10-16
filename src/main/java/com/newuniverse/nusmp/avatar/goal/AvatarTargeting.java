package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.multiverse.Squads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Why: one rule decides who an Avatar may hurt, so the goals, the signature moves and the damage-event guard cannot disagree: never its creator (unless it
 * has betrayed them), never the creator's FTB squadmates, never another Avatar of the same creator, never the creator's pets. The squad lookup uses the
 * mod's reflective Squads helper, so FTB Teams stays a soft dependency.
 */
public final class AvatarTargeting {
    private AvatarTargeting() {}

    /** May this Avatar hurt that entity? */
    public static boolean canHarm(AvatarEntity a, LivingEntity victim) {
        if (victim == null || victim == a || !victim.isAlive()) return false;
        UUID c = a.creatorUuid();
        if (c == null) return true;
        if (victim.getUUID().equals(c)) return a.isBetrayed();                                  // only a betrayed Avatar may touch its creator
        if (a.isBetrayed()) return true;                                                          // a betrayed one is nobody's ally
        if (victim instanceof AvatarEntity other && c.equals(other.creatorUuid())) return false;
        if (victim instanceof OwnableEntity pet && c.equals(pet.getOwnerUUID())) return false;
        if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return !isSquadmate(a, victim);
    }

    /** True when the entity is a player in the creator's FTB party (false without FTB Teams). */
    public static boolean isSquadmate(AvatarEntity a, LivingEntity victim) {
        if (!(victim instanceof ServerPlayer)) return false;
        ServerPlayer owner = a.creatorPlayer();
        if (owner == null) return false;
        try {
            return Squads.partyMembers(owner).contains(victim.getUUID());
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
