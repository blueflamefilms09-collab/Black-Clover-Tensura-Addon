package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The catalog's check of the waves' defs: duplicates, taken ids, vessels below LEGEND. */
class RelicChecksTest {
    static RelicDef d(String id, Grade g) { return RelicDef.of(id, id, g, RelicSlot.NECK); }

    @Test
    void aRepeatedIdIsDropped() {
        RelicChecks.Checked c = RelicChecks.check(List.of(d("a", Grade.RARE), d("b", Grade.RARE), d("a", Grade.GOD)), Set.of());
        assertEquals(2, c.defs().size());
        assertEquals(Grade.RARE, c.defs().get(0).grade());
        assertEquals(1, c.problems().size());
    }

    @Test
    void anIdThatAnItemAlreadyHasIsDroppedUnlessAdopted() {
        RelicChecks.Checked c = RelicChecks.check(List.of(d("old", Grade.RARE), d("fresh", Grade.RARE), d("old2", Grade.RARE).adopt(() -> null)), Set.of("old", "old2"));
        assertEquals(2, c.defs().size());
        assertEquals("fresh", c.defs().get(0).id());
        assertEquals("old2", c.defs().get(1).id());
        assertEquals(1, c.problems().size());
    }

    @Test
    void aVesselBelowLegendLosesTheFlagAndALegendKeepsIt() {
        RelicChecks.Checked c = RelicChecks.check(List.of(d("low", Grade.UNIQUE).vessel(), d("high", Grade.LEGEND).vessel()), Set.of());
        assertFalse(c.defs().get(0).isVessel());
        assertTrue(c.defs().get(1).isVessel());
        assertEquals(1, c.problems().size());
    }

    @Test
    void duplicateActKeysAndEmptyConsumablesAreReported() {
        Act a = Act.of("k", "K", null);
        RelicChecks.Checked c = RelicChecks.check(List.of(d("x", Grade.RARE).act(a).act(a),
                RelicDef.of("potion", "P", Grade.NORMAL, RelicSlot.CONSUMABLE)), Set.of());
        assertEquals(2, c.problems().size());
        assertEquals(2, c.defs().size());
    }

    @Test
    void aNullIsReportedNotThrown() {
        assertEquals(1, RelicChecks.check(java.util.Arrays.asList((RelicDef) null), Set.of()).problems().size());
    }
}
