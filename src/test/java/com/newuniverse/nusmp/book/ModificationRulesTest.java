package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the five generic pages Modification Magic had keep their ids and order (saves and mastery bits), five new ones follow; Re-Structure's ledger is the rule that keeps the guard from being a free
 * wall (the kind that has been hurting is cut, every other kind finds a hole, the notes fade with a half-life, nothing leads on an empty ledger); the thread's pull and the theatre's ring never beat
 * a walker.
 */
class ModificationRulesTest {
    @Test
    void genericPagesKeepTheirIdsAndOrder() {
        assertEquals(List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard"), ModificationRules.OLD_IDS);
        assertEquals(List.of("specimen_thread", "spell_rewrite", "test_subjects", "dissection_theatre", "dissector_frame"), ModificationRules.NEW_IDS);
        var all = new HashSet<String>(ModificationRules.OLD_IDS);
        all.addAll(ModificationRules.NEW_IDS);
        assertEquals(10, all.size());
    }

    @Test
    void emptyLedgerChangesNothing() {
        var l = new ModificationRules.Ledger();
        assertNull(l.leading(0));
        for (var k : ModificationRules.Kind.values()) assertEquals(1f, l.multiplier(k, 5, 0.3f), 1e-6);
        l.note(ModificationRules.Kind.MELEE, 0.2, 0);
        assertNull(l.leading(0), "a scratch is not a pattern");
    }

    @Test
    void theLeadingKindIsCutAndTheOthersFindAHole() {
        var l = new ModificationRules.Ledger();
        l.note(ModificationRules.Kind.MELEE, 20, 100);
        l.note(ModificationRules.Kind.RANGED, 4, 100);
        assertEquals(ModificationRules.Kind.MELEE, l.leading(100));
        assertEquals(0.7f, l.multiplier(ModificationRules.Kind.MELEE, 100, 0.3f), 1e-6);
        assertEquals(ModificationRules.OTHER_KIND_PENALTY, l.multiplier(ModificationRules.Kind.RANGED, 100, 0.3f), 1e-6);
        assertEquals(ModificationRules.OTHER_KIND_PENALTY, l.multiplier(ModificationRules.Kind.MAGIC, 100, 0.3f), 1e-6);
    }

    @Test
    void rotatingTheKindBeatsStayingWithOne() {
        var kinds = ModificationRules.Kind.values();
        var stay = new ModificationRules.Ledger();
        var rotate = new ModificationRules.Ledger();
        double stayTaken = 0, rotateTaken = 0;
        long t = 0;
        for (int n = 0; n < 12; n++, t += 20) {
            stayTaken += 10 * stay.multiplier(ModificationRules.Kind.MELEE, t, 0.3f);
            stay.note(ModificationRules.Kind.MELEE, 10, t);
            var k = kinds[n % 3];
            rotateTaken += 10 * rotate.multiplier(k, t, 0.3f);
            rotate.note(k, 10, t);
        }
        assertTrue(rotateTaken > stayTaken, "rotating the kind of blow must beat repeating one (" + rotateTaken + " vs " + stayTaken + ")");
    }

    @Test
    void notesFadeWithTheHalfLife() {
        var l = new ModificationRules.Ledger();
        l.note(ModificationRules.Kind.MAGIC, 8, 0);
        assertEquals(4.0, l.total(ModificationRules.Kind.MAGIC, ModificationRules.ADAPT_HALF_LIFE), 1e-6);
        assertEquals(2.0, l.total(ModificationRules.Kind.MAGIC, 2L * ModificationRules.ADAPT_HALF_LIFE), 1e-6);
        assertNull(l.leading(6L * ModificationRules.ADAPT_HALF_LIFE), "an old pattern is forgotten");
        l.note(ModificationRules.Kind.MAGIC, -5, 10);
        assertEquals(8.0, l.total(ModificationRules.Kind.MAGIC, 0), 1e-9, "a negative amount is ignored");
    }

    @Test
    void countsAndCutsAreBounded() {
        assertEquals(3, ModificationRules.needleCount(0));
        assertEquals(5, ModificationRules.needleCount(9));
        assertEquals(2, ModificationRules.tendrilCount(0));
        assertEquals(4, ModificationRules.tendrilCount(4));
        assertEquals(2, ModificationRules.subjectCount(2));
        assertEquals(3, ModificationRules.subjectCount(3));
        assertEquals(0.25f, ModificationRules.adaptCut(1.0), 1e-6);
        assertEquals(0.40f, ModificationRules.adaptCut(50.0), 1e-6);
        assertTrue(ModificationRules.autoNeedleInterval(4) < ModificationRules.autoNeedleInterval(0));
        assertTrue(ModificationRules.autoNeedleInterval(99) >= 24);
    }

    @Test
    void theThreadAndTheTheatreNeverBeatAWalker() {
        double walker = 0.216;
        for (int a = 0; a <= ModificationRules.THREAD_TICKS; a += 5) assertTrue(ModificationRules.threadPull(a, ModificationRules.THREAD_TICKS) < walker);
        assertEquals(0.03, ModificationRules.threadPull(0, 140), 1e-9);
        assertEquals(0.10, ModificationRules.threadPull(140, 140), 1e-9);
        double prev = Double.NaN;
        for (int a = 0; a <= ModificationRules.THEATRE_TICKS; a++) {
            double r = ModificationRules.theatreRadius(a, ModificationRules.THEATRE_TICKS);
            if (!Double.isNaN(prev)) {
                assertTrue(r <= prev + 1e-9, "the ring only closes");
                assertTrue(prev - r < walker, "the ring closes slower than a walk per tick");
            }
            prev = r;
        }
        assertEquals(8.0, ModificationRules.theatreRadius(0, 100), 1e-9);
        assertEquals(3.0, ModificationRules.theatreRadius(100, 100), 1e-9);
    }
}
