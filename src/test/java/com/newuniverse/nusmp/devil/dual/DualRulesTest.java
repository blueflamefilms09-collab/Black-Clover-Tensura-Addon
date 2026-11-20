package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilAttribute;
import com.newuniverse.nusmp.devil.DevilTier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the numbers of Devil Magic, One of One and the immunities (docs/devil_double_magic.md) are pure, so they are pinned here without a game. */
class DualRulesTest {

    @Test
    void everyAttributeHasAnArtThatDoesMoreThanDamage() {
        for (DevilAttribute a : DevilAttribute.values()) {
            if (a == DevilAttribute.NONE) {
                assertFalse(DualRules.art(a).active());
                assertNull(DualRules.asMagic(a));
                continue;
            }
            assertTrue(DualRules.art(a).active(), a + " has an art that is only damage");
            assertTrue(DualRules.art(a).bonus() > 0, a + " has no damage bonus");
            assertNotNull(DualRules.asMagic(a));
            assertTrue(DualRules.color(a) != 0x888888, a + " has no colour of its own");
        }
    }

    @Test
    void theArtNeverUsesTheFireEffectIdOfTheBlowTable() {
        assertEquals("", DualRules.art(DevilAttribute.FLAME).effectId());
        assertEquals("burn", DualRules.art(DevilAttribute.FLAME).special());
    }

    @Test
    void damageAndCostGrowWithTheTierAndStayBounded() {
        double lastD = 0, lastC = 0;
        for (DevilTier t : new DevilTier[] {DevilTier.LOW, DevilTier.MID, DevilTier.HIGH, DevilTier.SUPREME}) {
            assertTrue(DualRules.baseDamage(t) > lastD);
            assertTrue(DualRules.cost(t, false) > lastC);
            assertTrue(DualRules.cost(t, true) > DualRules.cost(t, false));
            lastD = DualRules.baseDamage(t);
            lastC = DualRules.cost(t, false);
            assertTrue(DualRules.damage(t, 99.0, true, 99.0) <= DualRules.MAX_RAW);
            assertTrue(DualRules.damage(t, -5.0, false, -1.0) >= 0.0);
        }
        assertEquals(0.0, DualRules.baseDamage(DevilTier.NONE));
        assertTrue(DualRules.damage(DevilTier.HIGH, 0.3, true, 1.0) > DualRules.damage(DevilTier.HIGH, 0.3, false, 1.0));
        assertTrue(DualRules.cooldown(true) > DualRules.cooldown(false) && DualRules.cooldown(false) >= 160);
        assertTrue(DualRules.MAX_TARGETS <= 8);
    }

    @Test
    void theGrimoireHitsOfTheDoubleArtAreAllKnown() {
        for (String h : new String[] {"BURN", "FREEZE", "SHOCK", "SLOW", "POISON", "WITHER", "BLIND", "LEVITATE", "PULL", "PUSH", "WEAKEN", "PIERCE", "DRAIN", "NULLIFY"}) {
            DualRules.Hit hit = DualRules.hit(h);
            assertTrue(!hit.effectId().isEmpty() || !hit.special().isEmpty() || hit.bonus() > 0, h + " adds nothing");
        }
        DualRules.Hit none = DualRules.hit("nonsense");
        assertTrue(none.effectId().isEmpty() && none.special().isEmpty() && none.bonus() == 0.0);
        assertTrue(DualRules.hit(null).effectId().isEmpty());
    }

    @Test
    void onlyTheDevilsTimeCountsAsTimeMagic() {
        assertTrue(DualRules.isTime(DevilAttribute.TIME));
        assertFalse(DualRules.isTime(DevilAttribute.FLAME));
        assertEquals("TIME", DualRules.asMagic(DevilAttribute.TIME));
    }

    @Test
    void theGrantVerdictNeverGrantsTwice() {
        assertEquals(DualRules.Verdict.GRANT, DualRules.verdict(true, false));
        assertEquals(DualRules.Verdict.KEEP, DualRules.verdict(true, true));
        assertEquals(DualRules.Verdict.REVOKE, DualRules.verdict(false, true));
        assertEquals(DualRules.Verdict.KEEP, DualRules.verdict(false, false));
    }

    // ---------------------------------------------------------------- One of One
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a"), B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final HolderRules.Params ON = new HolderRules.Params(true, HolderRules.graceTicks(72), HolderRules.absentMillis(30));

    @Test
    void aHeldDevilIsTakenByNobodyElse() {
        HolderRules.Slot held = new HolderRules.Slot(A, 0, 0, null, 0);
        assertEquals(HolderRules.Verdict.SELF, HolderRules.check(held, A, 10, 10, ON));
        assertEquals(HolderRules.Verdict.TAKEN, HolderRules.check(held, B, 10, 10, ON));
        assertFalse(HolderRules.Verdict.TAKEN.mayTake);
        assertEquals(HolderRules.Verdict.FREE, HolderRules.check(null, B, 10, 10, ON));
        assertEquals(HolderRules.Verdict.FREE, HolderRules.check(held, B, 10, 10, new HolderRules.Params(false, 0, 0)));
    }

    @Test
    void theDeathLapseNeedsTheFullGraceAndStopsWhenTheDevilIsLetInAgain() {
        long grace = HolderRules.graceTicks(72);
        assertEquals(72_000L, grace);
        HolderRules.Slot died = new HolderRules.Slot(A, 1000, 0, null, 0);
        assertEquals(HolderRules.Verdict.TAKEN, HolderRules.check(died, B, 1000 + grace - 1, 0, ON));
        assertEquals(HolderRules.Verdict.LAPSED_DEATH, HolderRules.check(died, B, 1000 + grace, 0, ON));
        HolderRules.Slot renewed = new HolderRules.Slot(A, 0, 0, null, 0);
        assertEquals(HolderRules.Verdict.TAKEN, HolderRules.check(renewed, B, 1000 + 10 * grace, 0, ON));
        assertEquals(HolderRules.Verdict.TAKEN, HolderRules.check(died, B, 1000 + 10 * grace, 0, new HolderRules.Params(true, 0, 0)), "0 hours turns the death lapse off");
    }

    @Test
    void theAbsenceLapseUsesRealTime() {
        long thirty = HolderRules.absentMillis(30);
        HolderRules.Slot gone = new HolderRules.Slot(A, 0, 5_000, null, 0);
        assertEquals(HolderRules.Verdict.TAKEN, HolderRules.check(gone, B, 0, 5_000 + thirty - 1, ON));
        assertEquals(HolderRules.Verdict.LAPSED_ABSENT, HolderRules.check(gone, B, 0, 5_000 + thirty, ON));
        assertEquals(HolderRules.Verdict.TAKEN, HolderRules.check(gone, B, 0, 5_000 + thirty, new HolderRules.Params(true, 1, 0)), "0 days turns the absence lapse off");
    }

    @Test
    void aWonDuelReservesTheDevilForTheVictorAlone() {
        HolderRules.Slot reserved = new HolderRules.Slot(null, 0, 0, B, 5_000);
        assertEquals(HolderRules.Verdict.RESERVED_FOR_YOU, HolderRules.check(reserved, B, 100, 0, ON));
        assertEquals(HolderRules.Verdict.RESERVED_OTHER, HolderRules.check(reserved, A, 100, 0, ON));
        assertEquals(HolderRules.Verdict.FREE, HolderRules.check(reserved, A, 5_000, 0, ON), "the reservation runs out");
        assertTrue(HolderRules.duelWon(A, B, A, B, 900, 900));
        assertFalse(HolderRules.duelWon(A, B, A, B, 900, 901));
        assertFalse(HolderRules.duelWon(A, B, A, null, 900, 100), "a death with no player killer is not a win");
        assertFalse(HolderRules.duelWon(A, B, B, A, 900, 100), "the holder killing the challenger is not a win");
    }

    @Test
    void aDuelNeedsBothCloseAndFree() {
        assertTrue(HolderRules.duelMayStart(true, true, 10, 32, false, false, 0, 8));
        assertFalse(HolderRules.duelMayStart(false, true, 10, 32, false, false, 0, 8));
        assertFalse(HolderRules.duelMayStart(true, false, 10, 32, false, false, 0, 8));
        assertFalse(HolderRules.duelMayStart(true, true, 33, 32, false, false, 0, 8));
        assertFalse(HolderRules.duelMayStart(true, true, 10, 32, true, false, 0, 8));
        assertFalse(HolderRules.duelMayStart(true, true, 10, 32, false, true, 0, 8));
        assertFalse(HolderRules.duelMayStart(true, true, 10, 32, false, false, 8, 8));
    }

    @Test
    void theSettingsAreClamped() {
        assertEquals(0L, HolderRules.graceTicks(-4));
        assertEquals(720_000L, HolderRules.graceTicks(99_999));
        assertEquals(365L * 86_400_000L, HolderRules.absentMillis(99_999));
        assertEquals(1000L, HolderRules.reserveTicks(0));
        assertEquals(30 * 20L, HolderRules.duelTicks(0));
        assertEquals(1800 * 20L, HolderRules.duelTicks(99_999));
    }

    // ---------------------------------------------------------------- the immunities
    @Test
    void adaptabilityNeverAdaptsAndLawDominationNeverActsOnDevilMagic() {
        assertFalse(DevilMagicRules.mayAdaptTo(true, true));
        assertFalse(DevilMagicRules.lawDominationApplies(true, true));
        assertTrue(DevilMagicRules.mayAdaptTo(false, true));
        assertTrue(DevilMagicRules.lawDominationApplies(false, true));
        assertTrue(DevilMagicRules.mayAdaptTo(true, false), "the switch off restores the old behaviour");
        assertTrue(DevilMagicRules.lawDominationApplies(true, false));
        assertTrue(DevilMagicRules.isDevilMagic(true, false));
        assertTrue(DevilMagicRules.isDevilMagic(false, true));
        assertFalse(DevilMagicRules.isDevilMagic(false, false));
        assertFalse(DevilMagicRules.hostCarries(true, true, 19.9f));
        assertTrue(DevilMagicRules.hostCarries(true, true, 20f));
        assertFalse(DevilMagicRules.hostCarries(false, true, 100f));
        assertFalse(DevilMagicRules.hostCarries(true, false, 100f));
    }

    @Test
    void theTimeOriginsAreDistinctAndBothCountAsTime() {
        assertEquals(TimeOrigin.NONE, TimeOrigin.of(false, false));
        assertEquals(TimeOrigin.GRIMOIRE, TimeOrigin.of(true, false));
        assertEquals(TimeOrigin.DEVIL, TimeOrigin.of(false, true));
        assertEquals(TimeOrigin.BOTH, TimeOrigin.of(true, true));
        assertFalse(TimeOrigin.NONE.isTime());
        assertTrue(TimeOrigin.DEVIL.isTime() && TimeOrigin.GRIMOIRE.isTime() && TimeOrigin.BOTH.isTime());
    }
}
