package com.newuniverse.nusmp.relics.djinn;

import java.util.List;

/**
 * Household Vessels and where Seals come from (pure). An owner may lend a fraction of the Djinn to two allies at the most; the ally gets only the
 * Lesser Summon and their assimilation rises with use (see {@link DjinnPhaseRules}). The Djinn still belongs to the owner: the one-per-type rule
 * is untouched, and an ally who already holds a Spirit or a Devil (and wears no multi-bond relic) cannot be lent it.
 */
public final class HouseholdRules {
    private HouseholdRules() {}

    public enum Verdict { OK, NOT_OWNER, SELF, FULL, ALREADY, ALLY_BONDED, ALLY_HAS_DJINN }

    public static Verdict invite(boolean isOwner, boolean self, int members, boolean alreadyMember, int allyHeldMask, boolean allyMulti) {
        if (!isOwner) return Verdict.NOT_OWNER;
        if (self) return Verdict.SELF;
        if (alreadyMember) return Verdict.ALREADY;
        if (members >= DjinnPhaseRules.HOUSEHOLD_MAX) return Verdict.FULL;
        if ((allyHeldMask & BondRules.DJINN) != 0) return Verdict.ALLY_HAS_DJINN;
        if (!BondRules.canBind(allyHeldMask, BondRules.DJINN, allyMulti)) return Verdict.ALLY_BONDED;
        return Verdict.OK;
    }

    /**
     * Which sleeping Djinn a boss reward or a vault hands out: the roll (uniform [0,1)) picks among those nobody holds and no Seal exists for.
     * Null if there is none left.
     */
    public static Djinn pickSleeping(List<Djinn> sleeping, double roll) {
        if (sleeping.isEmpty()) return null;
        int i = (int) Math.floor(Math.max(0.0, Math.min(0.999999, roll)) * sleeping.size());
        return sleeping.get(i);
    }

    /** The chance a Treasury / vault chest holds a Seal: 3 in 100 (and only if some Djinn still sleeps). */
    public static final double VAULT_CHANCE = 0.03;

    /** The floor bosses that drop a Seal the first time they fall in a world (the Nexus zone names). */
    public static boolean isSealBoss(String zoneName) {
        return "GOLIATH_PIT".equals(zoneName) || "CHARYBDIS_BASIN".equals(zoneName) || "UNDERWORLD_GATE".equals(zoneName) || "MONARCH_VAULT".equals(zoneName);
    }

    /** Server ticks a Seal may go unseen (not in an inventory, not an item entity) before its Djinn sleeps again: 7 days of running time. */
    public static final long SEAL_LOST_AFTER = 7L * 24L * 3600L * 20L;
}
