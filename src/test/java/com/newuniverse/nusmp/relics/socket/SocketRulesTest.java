package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.djinn.BondRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The socket count, the checks of putting a Core in, the cast trigger and the numbers of life steal and ability power. */
class SocketRulesTest {
    private static final SocketDef FIRE = SocketCatalog.byId("spirit:fire"), MEGICULA = SocketCatalog.byId("devil:megicula");

    @Test
    void slotsFollowTheTier() {
        for (int t = 1; t <= 7; t++) {
            int want = t >= 5 ? 2 : t >= 3 ? 1 : 0;
            assertEquals(want, SocketRules.slots(t, true, false), "tier " + t);
        }
        assertEquals(0, SocketRules.slots(7, false, false), "a curio relic has no socket unless opted in");
    }

    @Test
    void optInGivesOneSocketAtAnyTierAndTwoFromLegend() {
        assertEquals(1, SocketRules.slots(1, false, true));
        assertEquals(1, SocketRules.slots(2, true, true));
        assertEquals(2, SocketRules.slots(5, false, true));
    }

    @Test
    void insertChecksInOrder() {
        assertEquals(SocketRules.Outcome.NO_SLOTS, SocketRules.insert(SocketData.EMPTY, 0, FIRE, 0, false));
        assertEquals(SocketRules.Outcome.OK, SocketRules.insert(SocketData.EMPTY, 1, FIRE, 0, false));
        SocketData one = SocketData.EMPTY.with(new SocketData.Entry("spirit:fire", 5L));
        assertEquals(SocketRules.Outcome.DUPLICATE, SocketRules.insert(one, 2, FIRE, 0, false));
        assertEquals(SocketRules.Outcome.FULL, SocketRules.insert(one, 1, MEGICULA, 0, false));
        assertEquals(SocketRules.Outcome.OK, SocketRules.insert(one, 2, MEGICULA, 0, false));
    }

    @Test
    void neverMoreThanTwoWhateverTheSlotsSay() {
        SocketData two = SocketData.EMPTY.with(new SocketData.Entry("spirit:fire", 1L)).with(new SocketData.Entry("spirit:water", 2L));
        assertEquals(SocketRules.Outcome.FULL, SocketRules.insert(two, 9, MEGICULA, 0, false));
    }

    @Test
    void bondExclusivityIsTheDjinnSystemsOwn() {
        // a Spirit Core is refused to someone who holds a Devil or a Djinn, a Devil Core to someone who holds a Spirit
        assertEquals(SocketRules.Outcome.BOND_CONFLICT, SocketRules.insert(SocketData.EMPTY, 1, FIRE, BondRules.DEVIL, false));
        assertEquals(SocketRules.Outcome.BOND_CONFLICT, SocketRules.insert(SocketData.EMPTY, 1, FIRE, BondRules.DJINN, false));
        assertEquals(SocketRules.Outcome.BOND_CONFLICT, SocketRules.insert(SocketData.EMPTY, 1, MEGICULA, BondRules.SPIRIT, false));
        // the same kind is fine, and so is holding both with the multi-bond relic
        assertEquals(SocketRules.Outcome.OK, SocketRules.insert(SocketData.EMPTY, 1, FIRE, BondRules.SPIRIT, false));
        assertEquals(SocketRules.Outcome.OK, SocketRules.insert(SocketData.EMPTY, 1, MEGICULA, BondRules.SPIRIT | BondRules.DJINN, true));
        assertTrue(SocketRules.usable(SocketKind.SPIRIT, 0, false));
        assertFalse(SocketRules.usable(SocketKind.DEVIL, BondRules.SPIRIT, false));
    }

    @Test
    void castSlotTable() {
        assertEquals(-1, SocketRules.castSlot(false, 0f, 0, 2), "no sneak, no cast");
        assertEquals(-1, SocketRules.castSlot(true, 0f, 0, 0), "nothing bound");
        assertEquals(0, SocketRules.castSlot(true, 0f, 0, 1));
        assertEquals(0, SocketRules.castSlot(true, 10f, 1, 2));
        assertEquals(-1, SocketRules.castSlot(true, 0f, 2, 2), "a relic with two acts keeps plain sneak-use for its second act");
        assertEquals(1, SocketRules.castSlot(true, -70f, 2, 2), "looking steeply up fires the second socket");
        assertEquals(0, SocketRules.castSlot(true, -70f, 2, 1), "...or the only one");
        assertEquals(0, SocketRules.castSlot(true, -59f, 0, 2), "not steep enough: the first");
    }

    @Test
    void lifestealIsCappedPerHitAndPerSecond() {
        assertEquals(0f, SocketRules.lifesteal(0f, 0.08), 0f);
        assertEquals(0f, SocketRules.lifesteal(10f, 0.0), 0f);
        assertEquals(0.8f, SocketRules.lifesteal(10f, 0.08), 1e-4f);
        assertEquals(SocketRules.LIFESTEAL_HIT_CAP, SocketRules.lifesteal(1000f, 0.25), 0f);
        assertEquals(3f, SocketRules.lifestealLeft(20f, 0f), 1e-4f);
        assertEquals(0f, SocketRules.lifestealLeft(20f, 5f), 0f);
    }

    @Test
    void abilityPowerGrowsWithTierButIsBounded() {
        assertEquals(10.0, SocketRules.actAmount(10, 1), 1e-9);
        assertEquals(16.0, SocketRules.actAmount(10, 7), 1e-9);
        assertEquals(16.0, SocketRules.actAmount(10, 99), 1e-9);
        assertEquals(10.0, SocketRules.actAmount(10, -3), 1e-9);
    }

    @Test
    void resistancesMultiplyAndNeverGoBelowTenPercent() {
        assertEquals(0.85, SocketRules.keep(0.15), 1e-9);
        assertEquals(0.85 * 0.9, SocketRules.keep(0.15, 0.10), 1e-9);
        assertEquals(0.1, SocketRules.keep(0.9, 0.9, 0.9), 1e-9);
        assertEquals(1.0, SocketRules.keep(), 1e-9);
    }
}
