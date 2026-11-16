package com.newuniverse.nusmp.stones;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure numbers of the Infinity Stone slot: the gate, the scale, the factors, the never-lethal recoil, the caps, the away rule, the offerings. No Minecraft classes. */
class StoneRulesTest {
    private static final double G = StoneRules.GATE_EP;

    @Test
    void theGateIsAtTheSettingAndReadsBaseEp() {
        assertEquals(1.0e9, G);
        assertFalse(StoneRules.gateOk(G - 1, G));
        assertTrue(StoneRules.gateOk(G, G));
        assertFalse(StoneRules.gateOk(5 * G, 0), "a gate of zero or less opens nothing");
        assertFalse(StoneRules.gateOk(Double.NaN, G));
    }

    @Test
    void theScaleIsOneAtTheGateAndTwoAtTheCap() {
        assertEquals(1.0, StoneRules.stoneScale(G, G));
        assertEquals(1.0, StoneRules.stoneScale(G / 2, G));
        assertEquals(1.0 + Math.log10(3), StoneRules.stoneScale(3 * G, G), 1.0e-12);
        assertEquals(2.0, StoneRules.stoneScale(10 * G, G), 1.0e-9);
        assertEquals(2.0, StoneRules.stoneScale(1.0e6 * G, G));
        assertEquals(1.0, StoneRules.stoneScale(Double.NaN, G));
        assertEquals(1.0, StoneRules.stoneScale(3 * G, -1));
    }

    @Test
    void thePowerAndTimeStonesLendTheirFactorsAndNoFactorGoesUnderHalf() {
        assertEquals(0.85, StoneRules.costFactor(InfinityStone.POWER));
        assertEquals(1.0, StoneRules.costFactor(InfinityStone.SOUL));
        assertEquals(1.0, StoneRules.costFactor(null));
        assertEquals(0.80, StoneRules.cooldownFactor(InfinityStone.POWER));
        assertEquals(0.75, StoneRules.cooldownFactor(InfinityStone.TIME));
        assertEquals(1.0, StoneRules.cooldownFactor(InfinityStone.MIND));
        for (InfinityStone s : InfinityStone.values()) assertTrue(StoneRules.cooldownFactor(s) >= StoneRules.MIN_FACTOR);
    }

    @Test
    void theRecoilNeverKills() {
        assertEquals(5f, StoneRules.recoil(100f, 100f, 0.05), 1.0e-4f);
        assertEquals(2f, StoneRules.recoil(100f, 3f, 0.5), 1.0e-4f, "at 3 health only 2 can be taken");
        assertEquals(0f, StoneRules.recoil(100f, 1f, 0.5));
        assertEquals(0f, StoneRules.recoil(100f, 0.5f, 0.5));
        assertEquals(0f, StoneRules.recoil(100f, 50f, -1.0));
    }

    @Test
    void foresightAndTheSoulAnchorFireOnceUntilTheirCooldown() {
        assertTrue(StoneRules.foresightDodges(20f, 100f, 100, 100));
        assertFalse(StoneRules.foresightDodges(19.9f, 100f, 100, 100), "under a fifth of the maximum is not dodged");
        assertFalse(StoneRules.foresightDodges(50f, 100f, 99, 100), "not before the cooldown ends");
        assertTrue(StoneRules.soulAnchors(30f, 30f, 10, 0));
        assertFalse(StoneRules.soulAnchors(29f, 30f, 10, 0), "a blow that does not kill is no anchor");
        assertFalse(StoneRules.soulAnchors(99f, 30f, 10, 11));
        assertEquals(20f, StoneRules.anchorHealth(100f));
        assertEquals(1f, StoneRules.anchorHealth(2f));
    }

    @Test
    void controlAndHealingStayInsideTheBalanceLaw() {
        assertTrue(StoneRules.lockTicks(true, false) <= 20, "1 s boss");
        assertTrue(StoneRules.lockTicks(false, true) <= 60, "3 s player");
        assertTrue(StoneRules.lockTicks(false, false) <= 100, "5 s mob");
        for (double scale = 1.0; scale <= 2.0; scale += 0.25) assertTrue(StoneRules.soulworldPulse(scale) * 15 <= 0.5, "a Soulworld heals under half a bar in all");
    }

    @Test
    void passivesGrowWithTheScaleButNotPastTheCap() {
        assertEquals(0.3, StoneRules.passive(0.3, 1.0), 1.0e-12);
        assertEquals(0.6, StoneRules.passive(0.3, 2.0), 1.0e-12);
        assertEquals(0.6, StoneRules.passive(0.3, 99.0), 1.0e-12);
        assertEquals(0.3, StoneRules.passive(0.3, 0.1), 1.0e-12);
    }

    @Test
    void theAwayRuleOnlyTouchesAnOfflineHolderPastTheLimit() {
        assertTrue(StoneRules.awayExpired(true, 8, 7));
        assertFalse(StoneRules.awayExpired(true, 7, 7));
        assertFalse(StoneRules.awayExpired(false, Long.MAX_VALUE, 7), "an online holder is not away");
        assertFalse(StoneRules.awayExpired(true, Long.MAX_VALUE, 0), "a limit of zero switches the rule off");
    }

    @Test
    void theAltarTakesASkillOrAPetAndNeverAPlayerOrEp() {
        assertTrue(StoneRules.offeringAllowed("skill"));
        assertTrue(StoneRules.offeringAllowed("pet"));
        assertFalse(StoneRules.offeringAllowed("ep"));
        assertFalse(StoneRules.offeringAllowed("player"));
        assertFalse(StoneRules.offeringAllowed(null));
    }

    @Test
    void theStonesMapToTheirRelicsAndActKeys() {
        assertEquals(6, InfinityStone.values().length);
        for (InfinityStone s : InfinityStone.values()) {
            assertEquals(s, InfinityStone.byRelicId(s.relicId()));
            assertEquals(s, InfinityStone.byId(s.id().toUpperCase()));
            assertEquals(s, InfinityStone.ofActKey(s.actKey(1)));
            assertEquals(s, InfinityStone.ofActKey(s.actKey(2)));
            assertTrue(InfinityStone.isSlotAct(s.actKey(2)));
        }
        assertNull(InfinityStone.byRelicId("the_snap"));
        assertNull(InfinityStone.ofActKey("tesseract"));
        assertFalse(InfinityStone.isSlotAct(null));
    }
}
