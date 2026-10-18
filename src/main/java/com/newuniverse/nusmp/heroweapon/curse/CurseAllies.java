package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyKey;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

import java.util.UUID;

/**
 * Why: despair comes from losing the people who matter, so the curse has to know who matters to whom: a tamed animal or other creature owned by the wielder, the
 * wielder's Avatar, a party or FTB team member, a friendly character of the arcs. The mod already has the pieces (the Avatar's creator, the dungeon party's key
 * built on FTB Teams through its soft bridge, the arc NPC's character), and this class only asks them, in one place, so the death handler, the betrayal handler,
 * the Overrun's grief rule and the self-test all use the same answer. Nothing here changes the world. It never throws: an unreadable party is "not a mate".
 */
final class CurseAllies {
    private CurseAllies() {}

    /** The kind of ally a dead creature was. */
    enum Kind { NONE, PET, AVATAR, PARTY, NPC }

    /** Who the dead creature was to others: its kind, and for a pet or an Avatar the uuid of the owner (null otherwise). */
    record Who(Kind kind, UUID owner) {
        static final Who NONE = new Who(Kind.NONE, null);
    }

    /** What a creature was to the people around it: a loyal Avatar, a party member (any player), a friendly arc character, an owned creature, or nothing. */
    static Who classify(LivingEntity e) {
        if (e instanceof AvatarEntity av) return av.isLoyal() ? new Who(Kind.AVATAR, av.creatorUuid()) : Who.NONE;
        if (e instanceof ServerPlayer) return new Who(Kind.PARTY, null);
        if (e instanceof ArcNpcEntity npc) return npc.character() != null ? new Who(Kind.NPC, null) : Who.NONE;
        if (e instanceof OwnableEntity pet && pet.getOwnerUUID() != null) return new Who(Kind.PET, pet.getOwnerUUID());
        return Who.NONE;
    }

    /** The despair cause of a kind of ally. */
    static Cause causeOf(Kind kind) {
        return switch (kind) {
            case PET -> Cause.PET_DEATH;
            case AVATAR -> Cause.AVATAR_DEATH;
            case PARTY -> Cause.PARTY_DEATH;
            case NPC -> Cause.NPC_DEATH;
            default -> Cause.OTHER;
        };
    }

    /** True when the dead creature (of this {@code who}) mattered to {@code mourner}. */
    static boolean mattersTo(Who who, LivingEntity dead, ServerPlayer mourner) {
        return switch (who.kind()) {
            case PET, AVATAR -> mourner.getUUID().equals(who.owner());
            case PARTY -> dead instanceof ServerPlayer dp && isPartyMate(mourner, dp);
            case NPC -> true;
            default -> false;
        };
    }

    /** True when the two are on the same scoreboard team, or in the same party or FTB team (a party of one is no party). Never throws. */
    static boolean isPartyMate(ServerPlayer a, ServerPlayer b) {
        if (a == null || b == null || a == b || a.getUUID().equals(b.getUUID())) return false;
        try {
            if (a.isAlliedTo(b)) return true;
            PartyKey ka = PartyResolver.keyOf(a), kb = PartyResolver.keyOf(b);
            return ka.team() && ka.equals(kb);
        } catch (RuntimeException | LinkageError unreadable) {
            return false;
        }
    }

    /**
     * True when {@code attacker} hurting {@code victim} is a betrayal: a party member, the victim's own Avatar (loyal or turned), a creature the victim owns (their
     * own summon or pet), or a friendly arc character.
     */
    static boolean isBetrayal(ServerPlayer victim, Entity attacker) {
        if (attacker == null || attacker == victim) return false;
        if (attacker instanceof ServerPlayer ap) return isPartyMate(victim, ap);
        if (attacker instanceof AvatarEntity av) return victim.getUUID().equals(av.creatorUuid());
        if (attacker instanceof ArcNpcEntity npc) return npc.character() != null;
        if (attacker instanceof OwnableEntity pet) return victim.getUUID().equals(pet.getOwnerUUID());
        return false;
    }
}
