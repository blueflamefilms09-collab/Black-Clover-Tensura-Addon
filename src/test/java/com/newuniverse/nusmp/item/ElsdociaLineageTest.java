package com.newuniverse.nusmp.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ElsdociaLineageTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes()),
            C = UUID.nameUUIDFromBytes("c".getBytes()), D = UUID.nameUUIDFromBytes("d".getBytes());
    private static final int JUNIOR = 0, SENIOR = 14, GRAND = 15, KING = 16;

    @Test
    void damageFormulaIsFourHundredPlusTwentyFive() {
        assertEquals(400, ElsdociaMath.damage(0));
        assertEquals(425, ElsdociaMath.damage(1));
        assertEquals(450, ElsdociaMath.damage(2));
        assertEquals(400, ElsdociaMath.damage(-3));
        assertEquals(399.0, ElsdociaMath.attributeAmount(0), 1e-9);
        assertTrue(ElsdociaMath.isGrand(GRAND) && ElsdociaMath.isGrand(KING) && !ElsdociaMath.isGrand(SENIOR));
    }

    @Test
    void firstOwnerClaimsWithoutBonusEvenIfGrand() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY;
        assertEquals(ElsdociaLineage.Verdict.CLAIM, l.verdict(A));
        l = l.take(A, "A", "FLAME", GRAND);
        assertEquals(400, l.damage());
        assertEquals(A, l.current().id());
        assertEquals(ElsdociaLineage.Verdict.SAME, l.verdict(A));
    }

    @Test
    void passToNewGrandOwnerAddsTwentyFiveEachTime() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "FLAME", JUNIOR).take(B, "B", "ICE", GRAND);
        assertEquals(425, l.damage());
        l = l.take(C, "C", "WIND", KING);
        assertEquals(450, l.damage());
    }

    @Test
    void nonGrandHolderPassesItOnWithoutBonus() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "FLAME", GRAND).take(B, "B", "ICE", SENIOR);
        assertEquals(400, l.damage());
        assertEquals(B, l.current().id());
        l = l.take(C, "C", "WIND", GRAND);
        assertEquals(425, l.damage());
    }

    @Test
    void neverBackToTheLastOwnerButFurtherBackMay() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "FLAME", JUNIOR).take(B, "B", "ICE", GRAND);
        assertEquals(ElsdociaLineage.Verdict.BLOCKED, l.verdict(A));
        assertSame(l, l.take(A, "A", "FLAME", GRAND));
        l = l.take(C, "C", "WIND", JUNIOR);
        assertEquals(ElsdociaLineage.Verdict.BLOCKED, l.verdict(B));
        assertEquals(ElsdociaLineage.Verdict.PASS, l.verdict(A));
        l = l.take(A, "A", "FLAME", GRAND);
        assertEquals(425, l.damage(), "an earlier owner coming back gives no second bonus");
        assertEquals(A, l.current().id());
        assertEquals(ElsdociaLineage.Verdict.BLOCKED, l.verdict(C));
    }

    @Test
    void sameOwnerKeepingItChangesNothingButFillsAMissingMagic() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "", JUNIOR);
        assertTrue(l.learned().isEmpty());
        ElsdociaLineage same = l.take(A, "A", "", JUNIOR);
        assertEquals(l, same);
        l = l.take(A, "A", "storm", JUNIOR);
        assertEquals(List.of("STORM"), l.learned());
        assertEquals("STORM", l.current().magic());
    }

    @Test
    void everyDistinctMagicIsLearnedOnceAndKept() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "FLAME", JUNIOR).take(B, "B", "ICE", JUNIOR).take(C, "C", "FLAME", JUNIOR).take(D, "D", "", JUNIOR);
        assertEquals(List.of("FLAME", "ICE"), l.learned());
        assertEquals(4, l.slots());
        assertEquals("ICE", l.learnedAt(3));
        assertNull(l.learnedAt(1));
        assertNull(l.learnedAt(4));
    }

    @Test
    void cyclingWrapsThroughTheBaseSlotsAndTheLearnedOnes() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "FLAME", JUNIOR).take(B, "B", "ICE", JUNIOR);
        assertEquals(0, l.activeSlot());
        l = l.cycled();
        assertEquals(1, l.activeSlot());
        l = l.cycled().cycled();
        assertEquals(3, l.activeSlot());
        l = l.cycled();
        assertEquals(0, l.activeSlot());
        assertEquals(0, l.withActive(99).activeSlot(), "an out-of-range slot falls back to the first");
    }

    @Test
    void adminForceIgnoresTheNoReturnRule() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "A", "FLAME", JUNIOR).take(B, "B", "ICE", GRAND);
        l = l.force(A, "A", "FLAME", GRAND);
        assertEquals(A, l.current().id());
        assertEquals(425, l.damage(), "A held it before: no second bonus");
        assertEquals(B, l.last().id());
    }

    @Test
    void signaturePageIsTheDearestUsableOneLaterWinningTies() {
        assertEquals(2, ElsdociaLineage.pickSignature(new double[]{8, 12, 35, 20}, new boolean[]{true, true, true, true}));
        assertEquals(3, ElsdociaLineage.pickSignature(new double[]{8, 35, 20, 35}, new boolean[]{true, true, true, true}));
        assertEquals(2, ElsdociaLineage.pickSignature(new double[]{8, 35, 20}, new boolean[]{true, false, true}));
        assertEquals(-1, ElsdociaLineage.pickSignature(new double[]{8}, new boolean[]{false}));
        assertEquals(-1, ElsdociaLineage.pickSignature(new double[0], new boolean[0]));
    }

    @Test
    void savedFormRoundTripsAndSurvivesDamage() {
        ElsdociaLineage l = ElsdociaLineage.EMPTY.take(A, "Asta", "ANTI_MAGIC", GRAND).take(B, "Yu|no", "WIND", GRAND).take(C, "Noelle", "WATER", JUNIOR).withActive(3);
        List<String> lines = l.encode();
        ElsdociaLineage back = ElsdociaLineage.decode(lines);
        assertEquals(l.owners().size(), back.owners().size());
        assertEquals(l.learned(), back.learned());
        assertEquals(l.damage(), back.damage());
        assertEquals(3, back.activeSlot());
        assertEquals("Yu_no", back.owners().get(1).name(), "a pipe in a name cannot break the line format");
        assertEquals(l.encode(), back.encode());
        List<String> broken = new java.util.ArrayList<>(lines);
        broken.add("O|not-a-uuid|x|y|z|1");
        broken.add("garbage");
        broken.add(null);
        assertEquals(l.encode(), ElsdociaLineage.decode(broken).encode());
        assertEquals(ElsdociaLineage.EMPTY, ElsdociaLineage.decode(null));
        assertEquals(ElsdociaLineage.EMPTY, ElsdociaLineage.decode(List.of()));
    }

    @Test
    void theTooltipCountsAgainstEveryMagic() {
        assertEquals(105, ElsdociaMath.TOTAL_MAGICS);
        assertTrue(ElsdociaMath.MAX_MOTES >= 1);
    }
}
