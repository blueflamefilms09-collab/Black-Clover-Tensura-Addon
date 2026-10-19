package com.newuniverse.nusmp.labyrinth.mega.lock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code /dungeon unstuck} must always be there for a player who is stuck, and must never be a free teleport out of a fight. These tests pin both halves: a cooldown, a cost
 * in experience levels and a refusal near hostile creatures for a player who is merely stuck somewhere odd, and none of the three for a player who is sealed in by a closed lock;
 * the party version has its own, longer cooldown; and the destination is the last safe room, else the nearest safe room, else the arrival stairs, else the forward stairs, else the Hub.
 */
class UnstuckRulesTest {
    private static final UnstuckRules.Config C = UnstuckRules.Config.DEFAULT;
    private static final long NEVER = Long.MIN_VALUE / 2;

    private static UnstuckRules.Refusal check(long now, long last, boolean inDungeon, boolean sealed, int levels, boolean hostile, boolean party) {
        return UnstuckRules.check(C, now, last, inDungeon, sealed, levels, hostile, party);
    }

    @Test void aPlayerWhoIsMerelyStuckPaysAndWaits() {
        assertEquals(UnstuckRules.Refusal.NONE, check(1000, NEVER, true, false, 3, false, false));
        assertEquals(UnstuckRules.Refusal.NO_XP, check(1000, NEVER, true, false, 2, false, false));
        assertEquals(UnstuckRules.Refusal.HOSTILE_NEAR, check(1000, NEVER, true, false, 30, true, false));
        assertEquals(UnstuckRules.Refusal.COOLDOWN, check(1000, 500, true, false, 30, false, false));
        assertEquals(UnstuckRules.Refusal.NONE, check(500 + 300 * 20, 500, true, false, 30, false, false), "ready again after the cooldown");
        assertEquals(UnstuckRules.Refusal.NOT_IN_DUNGEON, check(1000, NEVER, false, false, 30, false, false));
    }

    @Test void aPlayerSealedInPaysNothingAndWaitsForNothing() {
        assertEquals(UnstuckRules.Refusal.NONE, check(1000, 999, true, true, 0, true, false), "no cooldown, no cost, hostiles do not matter");
        assertEquals(0, UnstuckRules.cost(C, true));
        assertEquals(3, UnstuckRules.cost(C, false));
        assertEquals(UnstuckRules.Refusal.NOT_IN_DUNGEON, check(1000, NEVER, false, true, 0, false, false), "outside the dungeon there is nothing to be unstuck from");
    }

    @Test void thePartyVersionHasItsOwnLongerCooldown() {
        long used = 1000;
        assertEquals(UnstuckRules.Refusal.NONE, check(used + 300 * 20, NEVER, true, false, 30, false, true), "a party use is not blocked by a solo use that never happened");
        assertEquals(UnstuckRules.Refusal.COOLDOWN, check(used + 300 * 20, used, true, false, 30, false, true), "five minutes are not enough for the party version");
        assertEquals(UnstuckRules.Refusal.NONE, check(used + 600 * 20, used, true, false, 30, false, true));
        assertEquals(300, UnstuckRules.cooldownLeftSeconds(C, used + 300 * 20, used, true));
        assertEquals(0, UnstuckRules.cooldownLeftSeconds(C, used + 300 * 20, used, false));
        assertEquals(0, UnstuckRules.cooldownLeftSeconds(C, 5, NEVER, false));
        assertEquals(1, UnstuckRules.cooldownLeftSeconds(C, used + 300 * 20 - 1, used, false), "a part second rounds up");
    }

    @Test void zeroesSwitchTheCostTheCooldownAndTheHostileTestOff() {
        UnstuckRules.Config free = new UnstuckRules.Config(0, 0, 0, 0);
        assertEquals(UnstuckRules.Refusal.NONE, UnstuckRules.check(free, 10, 9, true, false, 0, true, false));
        assertEquals(0, UnstuckRules.cost(free, false));
    }

    @Test void aBadConfigIsClamped() {
        UnstuckRules.Config c = new UnstuckRules.Config(-5, 1000, 9999, Integer.MAX_VALUE).clamped();
        assertEquals(0, c.cooldownSeconds());
        assertEquals(100, c.xpLevels());
        assertEquals(64, c.hostileRadius());
        assertEquals(86400, c.partyCooldownSeconds());
    }

    @Test void theDestinationIsTheBestKnownPlaceInOrder() {
        assertEquals(UnstuckRules.Destination.LAST_SAFE, UnstuckRules.choose(true, true, true, true));
        assertEquals(UnstuckRules.Destination.NEAREST_SAFE, UnstuckRules.choose(false, true, true, true));
        assertEquals(UnstuckRules.Destination.ARRIVAL_STAIRS, UnstuckRules.choose(false, false, true, true));
        assertEquals(UnstuckRules.Destination.FORWARD_STAIRS, UnstuckRules.choose(false, false, false, true));
        assertEquals(UnstuckRules.Destination.HUB, UnstuckRules.choose(false, false, false, false));
        assertTrue(UnstuckRules.Destination.values().length == 5);
    }
}
