package com.newuniverse.nusmp.heroweapon.weapon.chain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the heavy links are felt in every fight, so their counting (a link per blow, the weight taking hold at the third, forgetting with time, a size limit) is proved exactly. */
class WeightLedgerTest {
    private static UUID id(int n) { return UUID.nameUUIDFromBytes(("w" + n).getBytes()); }

    @Test
    void eachBlowAddsALinkAndTheThirdTakesHoldAndClearsThem() {
        WeightLedger w = new WeightLedger();
        assertEquals(1, w.add(id(1), 100));
        assertEquals(2, w.add(id(1), 120));
        assertEquals(2, w.stacks(id(1), 130));
        assertEquals(WeightLedger.MAX_STACKS, w.add(id(1), 140), "the third blow: the weight takes hold");
        assertEquals(0, w.stacks(id(1), 141), "and the links are used up");
        assertEquals(1, w.add(id(1), 150), "the next blow starts again");
    }

    @Test
    void linksFallOffAfterSixtyTicksWithoutABlow() {
        WeightLedger w = new WeightLedger();
        w.add(id(1), 0);
        assertEquals(1, w.stacks(id(1), 59));
        assertEquals(0, w.stacks(id(1), 60));
        assertEquals(1, w.add(id(1), 100), "an old link does not count towards the third");
    }

    @Test
    void creaturesAreCountedApart() {
        WeightLedger w = new WeightLedger();
        w.add(id(1), 0);
        w.add(id(1), 10);
        assertEquals(1, w.add(id(2), 20));
        assertEquals(2, w.stacks(id(1), 25));
    }

    @Test
    void theTableIsBoundedAndForgetsTheOldestFirst() {
        WeightLedger w = new WeightLedger();
        for (int i = 0; i < WeightLedger.MAX_TARGETS + 4; i++) w.add(id(i), i);
        assertEquals(WeightLedger.MAX_TARGETS, w.size(30));
        assertEquals(0, w.stacks(id(0), 30), "the first creature was forgotten");
        assertEquals(1, w.stacks(id(WeightLedger.MAX_TARGETS + 3), 30));
    }

    @Test
    void clearForgetsEverything() {
        WeightLedger w = new WeightLedger();
        w.add(id(1), 0);
        w.clear();
        assertEquals(0, w.size(1));
    }
}
