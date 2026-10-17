package com.newuniverse.nusmp.avatar.party;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.cast.SimulatedPlayerCaster;
import com.newuniverse.nusmp.avatar.dnd.Stat;
import com.newuniverse.nusmp.avatar.naming.NamingMath;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is the act of an Avatar naming a wild monster, kept apart from the goal that walks up to it so that "which monsters may be named" and "what a naming
 * does" are read in one place. A monster is a candidate when it is one of Tensura's own, wild (no master, no name, nothing Tensura has already bound), not a boss, a
 * villager, a player or an Avatar, not in a fight, and within the strength window of {@link PartyRules#suitable}. Naming it costs the Avatar a share of its spell pool,
 * gives the monster a generated name ({@link NameGen}), Tensura's name and owner record (the Avatar is its master), the same kind of EP surge a named Avatar gets (luck
 * and INT of the Avatar, never above nine tenths of the Avatar's own EP) and a mark that puts it in the Avatar's {@link AvatarPartyManager} record. Evolution through
 * Tensura's own naming ({@code INameEvolution}) is NOT triggered: it is written for a player's naming ritual and nobody could check it for a mob namer.
 */
public final class PartyNaming {
    private PartyNaming() {}

    /** A member never becomes stronger than this share of its Avatar. */
    static final double MEMBER_EP_CAP = 0.9;

    /** May this Avatar name that creature right now? */
    public static boolean candidate(AvatarEntity a, LivingEntity e) {
        if (!(e instanceof Mob mob) || !e.isAlive() || e.isInvulnerable() || mob.isPassenger()) return false;
        if (e instanceof Player || e instanceof AvatarEntity || e instanceof AbstractVillager) return false;
        if (!TensuraBridge.isTensuraMob(e) || BossIdentity.isBoss(e)) return false;
        if (e.hasCustomName() || TensuraBridge.tensuraNamed(e) || !TensuraBridge.nameable(e)) return false;
        if (TensuraBridge.permanentOwner(e) != null || AvatarPartyManager.isMember(e)) return false;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;
        if (e instanceof TamableAnimal t && t.isTame()) return false;
        if (mob.getTarget() != null) return false;
        return PartyRules.suitable(TensuraBridge.maxEp(a), TensuraBridge.maxEp(e), PartyRules.DEFAULT_MIN_RATIO, PartyRules.DEFAULT_MAX_RATIO);
    }

    /** The nearest candidate within the configured radius of the Avatar, or null. */
    public static Mob nearest(AvatarEntity a) {
        double r = AvatarAiConfig.autoNamingRadius();
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        List<Mob> near = a.level().getEntitiesOfClass(Mob.class, a.getBoundingBox().inflate(r), m -> m != a && candidate(a, m));
        for (Mob m : near) {
            double d = m.distanceToSqr(a);
            if (d < bestD) { bestD = d; best = m; }
        }
        return best;
    }

    /** Facts for {@link PartyRules#mayName}. */
    public static PartyRules.Avatar facts(AvatarEntity a, AvatarPartyManager mgr) {
        double max = SimulatedPlayerCaster.poolMax(a);
        return new PartyRules.Avatar(AvatarConfig.enabled(), AvatarAiConfig.autoNaming(), a.isBetrayed(), mgr.count(a.getUUID()), AvatarAiConfig.partyMax(), a.level().getGameTime(),
                mgr.lastNamedAt(a.getUUID()), AvatarAiConfig.autoNamingCooldownTicks(), max <= 0 ? 0.0 : SimulatedPlayerCaster.poolNow(a) / max, AvatarAiConfig.autoNamingMinMagicule());
    }

    /**
     * Names the monster: pays, names, binds, marks, records. False (and nothing changed) when the Avatar cannot pay or the monster stopped being a candidate.
     * Server side; the Avatar must be within a few blocks of the monster (the goal walks it there).
     */
    public static boolean name(AvatarEntity a, Mob mob) {
        if (!(a.level() instanceof ServerLevel sl) || !candidate(a, mob)) return false;
        AvatarPartyManager mgr = AvatarPartyManager.get(sl.getServer());
        if (!PartyRules.mayName(facts(a, mgr))) return false;
        double cost = PartyRules.nameCost(SimulatedPlayerCaster.poolMax(a), AvatarAiConfig.autoNamingCostShare());
        if (!SimulatedPlayerCaster.spendPool(a, cost)) return false;

        Set<String> taken = new LinkedHashSet<>(mgr.namesOf(a.getUUID()));
        if (a.creatorUuid() != null) taken.addAll(AvatarManager.get(sl.getServer()).names(a.creatorUuid()));
        String name = NameGen.next(a.seeded(), taken);
        UUID avatarId = a.getUUID();

        // the strength: the surge a named Avatar gets, capped below the namer's own EP so a party never outgrows its Avatar
        int intScore = a.stats().length >= 6 ? a.stats()[Stat.INT.ordinal()] : 10;
        double boost = NamingMath.epBoost(a.storedLuck(), intScore, a.getRandom().nextDouble(), AvatarConfig.namingMinEpBoost(), AvatarConfig.namingMaxEpBoost());
        double ep = TensuraBridge.baseMaxEp(mob);
        double cap = TensuraBridge.maxEp(a) * MEMBER_EP_CAP;
        double factor = NamingMath.epMultiplier(boost);
        if (ep > 0.0 && ep * factor > cap) factor = Math.max(1.0, cap / ep);
        if (factor > 1.0) TensuraBridge.scaleEnergy(mob, factor);

        mob.setCustomName(Component.literal(name));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setTarget(null);
        mob.getPersistentData().putUUID(AvatarPartyManager.MARK, avatarId);
        TensuraBridge.setNaming(mob, name, avatarId, false);
        try {
            if (mob instanceof TamableAnimal t) {
                t.setOwnerUUID(avatarId);
                t.setTame(true, false);
            }
        } catch (RuntimeException soft) { /* the mark and Tensura's record already bind it */ }
        mgr.add(avatarId, mob.getUUID(), name, sl.getGameTime());

        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, mob.position().add(0, 0.08, 0), mob.position().add(0, 1.08, 0), 0xFF66CCFF, 50, 2.0f);
        VfxSpawn.sendFollowing(sl, VfxShape.SPIRIT_AURA, mob, mob.position(), 0xFF66CCFF, 50, 1.0f);
        ServerPlayer creator = a.creatorPlayer();
        if (creator != null && creator.level() == sl) {
            creator.sendSystemMessage(Component.literal(a.avatarName() + " named a " + mob.getType().getDescription().getString() + ": " + name + ".").withStyle(ChatFormatting.AQUA));
        }
        return true;
    }
}
