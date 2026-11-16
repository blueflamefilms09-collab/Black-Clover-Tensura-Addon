package com.newuniverse.nusmp.skill.rules;

import com.newuniverse.nusmp.skill.rules.FaithWorshipRules.Recruit;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules.Worshipper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the worship model (docs/faith_fix.md) decides who may be recruited, what an offering and a gift cost and when an invitation lapses; none of that may drift or go negative, and the wiring must stay in place. */
class FaithWorshipRulesTest {
    private static final UUID GOD = UUID.randomUUID(), A = UUID.randomUUID(), B = UUID.randomUUID();

    @Test
    void recruitingChecksSelfDuplicateAndCapacity() {
        List<Worshipper> list = List.of();
        assertEquals(Recruit.SELF, FaithWorshipRules.canRecruit(list, GOD, GOD, 4));
        assertEquals(Recruit.OK, FaithWorshipRules.canRecruit(list, GOD, A, 4));
        List<Worshipper> one = FaithWorshipRules.add(list, GOD, A, "Alice", 4);
        assertEquals(1, one.size());
        assertEquals(Recruit.ALREADY, FaithWorshipRules.canRecruit(one, GOD, A, 4));
        assertEquals(one, FaithWorshipRules.add(one, GOD, A, "Alice again", 4), "a duplicate changes nothing");
        assertEquals(Recruit.FULL, FaithWorshipRules.canRecruit(one, GOD, B, 1));
        assertEquals(1, FaithWorshipRules.add(one, GOD, B, "Bob", 1).size(), "a full list stays full");
        assertEquals(Recruit.OK, FaithWorshipRules.canRecruit(null, GOD, A, 4), "a missing list is an empty one");
    }

    @Test
    void theCapIsClamped() {
        assertEquals(1, FaithWorshipRules.capOf(-3));
        assertEquals(64, FaithWorshipRules.capOf(5000));
        assertEquals(16, FaithWorshipRules.capOf(16));
    }

    @Test
    void worshippersCanLeaveAndKeepTheirOfferings() {
        List<Worshipper> l = FaithWorshipRules.add(FaithWorshipRules.add(List.of(), GOD, A, "Alice", 8), GOD, B, "Bob", 8);
        assertTrue(FaithWorshipRules.isWorshipper(l, A));
        List<Worshipper> grown = FaithWorshipRules.addWorship(l, A, 5);
        assertEquals(5, grown.get(0).worship());
        assertEquals(0, grown.get(1).worship());
        assertEquals(5, FaithWorshipRules.totalWorship(grown));
        assertEquals(grown, FaithWorshipRules.addWorship(grown, UUID.randomUUID(), 5), "a stranger's offering changes nothing");
        assertEquals(5, FaithWorshipRules.addWorship(grown, A, -9).get(0).worship(), "a negative offering is ignored");
        assertEquals(FaithWorshipRules.MAX_WORSHIP, FaithWorshipRules.addWorship(FaithWorshipRules.addWorship(l, A, Integer.MAX_VALUE), A, Integer.MAX_VALUE).get(0).worship());
        List<Worshipper> left = FaithWorshipRules.remove(grown, A);
        assertEquals(1, left.size());
        assertFalse(FaithWorshipRules.isWorshipper(left, A));
    }

    @Test
    void namesAreCut() {
        assertEquals("", FaithWorshipRules.shortName(null));
        assertEquals(FaithWorshipRules.NAME_MAX, FaithWorshipRules.shortName("x".repeat(100)).length());
        assertEquals("Bob", FaithWorshipRules.shortName("  Bob "));
    }

    @Test
    void onlyTheOfferedSizesAreAccepted() {
        for (int units : FaithWorshipRules.OFFER_STEPS) {
            assertTrue(FaithWorshipRules.isOfferStep(units));
            assertEquals(units * 1000.0, FaithWorshipRules.offerCost(units, 1000.0));
        }
        for (int bad : new int[]{0, -1, 2, 3, 11, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertFalse(FaithWorshipRules.isOfferStep(bad), "size " + bad);
            assertEquals(0.0, FaithWorshipRules.offerCost(bad, 1000.0), "size " + bad + " must not have a price to pay");
        }
        assertEquals(0.0, FaithWorshipRules.offerCost(5, 0.0));
    }

    @Test
    void theOfferingClockLimitsTheRate() {
        assertTrue(FaithWorshipRules.ready(-1, 0, 100), "never offered");
        assertFalse(FaithWorshipRules.ready(1000, 1050, 100));
        assertTrue(FaithWorshipRules.ready(1000, 1100, 100));
        assertTrue(FaithWorshipRules.ready(1000, 10, 100), "a clock that went back does not lock the player out");
    }

    @Test
    void prayerGrowsWithWorshippersButIsCapped() {
        assertEquals(0, FaithWorshipRules.prayBonus(0));
        assertEquals(0, FaithWorshipRules.prayBonus(3));
        assertEquals(1, FaithWorshipRules.prayBonus(4));
        assertEquals(FaithWorshipRules.MAX_PRAY_BONUS, FaithWorshipRules.prayBonus(64));
        assertEquals(0, FaithWorshipRules.prayBonus(-5));
    }

    @Test
    void aWorshipperPaysLessForAGiftButNeverNothing() {
        assertEquals(10, FaithWorshipRules.giftCost(10, false, 25));
        assertEquals(7, FaithWorshipRules.giftCost(10, true, 25));
        assertEquals(1, FaithWorshipRules.giftCost(1, true, 90));
        assertEquals(0, FaithWorshipRules.giftCost(0, true, 25), "a free gift stays free");
        assertEquals(1, FaithWorshipRules.giftCost(10, true, 500), "the discount is clamped to 90 percent");
        assertEquals(10, FaithWorshipRules.giftCost(10, true, -20), "a negative discount is no discount");
    }

    @Test
    void theSlowRegenerationIsBoundedAndNeedsWorshippers() {
        assertEquals(0, FaithWorshipRules.periods(5999, 6000));
        assertEquals(1, FaithWorshipRules.periods(6000, 6000));
        assertEquals(FaithWorshipRules.MAX_TRICKLE_PERIODS, FaithWorshipRules.periods(Long.MAX_VALUE, 6000), "a long absence is capped");
        assertEquals(0, FaithWorshipRules.periods(50_000, 0), "period 0 = off");
        assertEquals(0, FaithWorshipRules.periods(-50, 6000));
        assertEquals(0, FaithWorshipRules.trickleGain(5, 0), "no worshippers, no Faith");
        assertEquals(1, FaithWorshipRules.trickleGain(1, 1));
        assertEquals(1, FaithWorshipRules.trickleGain(1, 4));
        assertEquals(2, FaithWorshipRules.trickleGain(1, 5));
        assertEquals(3 * 16, FaithWorshipRules.trickleGain(3, 64), "64 worshippers: 1 + 63/4 = 16 per period");
    }

    @Test
    void invitationsLapse() {
        assertTrue(FaithWorshipRules.inviteLive(100, 100, 1200));
        assertTrue(FaithWorshipRules.inviteLive(100, 1300, 1200));
        assertFalse(FaithWorshipRules.inviteLive(100, 1301, 1200));
        assertFalse(FaithWorshipRules.inviteLive(100, 50, 1200), "a clock that went back closes it");
    }

    @Test
    void theWiringStaysInPlace() throws Exception {
        String root = "src/main/java/com/newuniverse/nusmp/";
        String nusmp = Files.readString(Path.of(root + "NUSMP.java"));
        assertTrue(nusmp.contains("FaithSelfTests.register()"), "self-tests registered");
        assertTrue(nusmp.contains("FaithWorship::onLogout") && nusmp.contains("FaithWorship.clear()"), "the invitation maps are cleaned on logout and server stop");
        String tests = Files.readString(Path.of(root + "skill/FaithSelfTests.java"));
        assertTrue(tests.contains("\"faith.rules\"") && tests.contains("\"faith.worship\""));
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"));
        assertTrue(smoke.contains(",faith,") && smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the smoke list expects the faith self-tests and keeps its tail");
        String cmd = Files.readString(Path.of(root + "command/FaithCommand.java"));
        assertTrue(cmd.contains("Commands.literal(\"faith\")") && cmd.contains("\"offer\"") && cmd.contains("\"gift\""), "the worship commands");
        String skill = Files.readString(Path.of(root + "skill/FaithSkill.java"));
        assertTrue(skill.contains("static final int BESTOW = 0, PRAY = 1, CHECK = 2;"), "the old mode numbers");
        assertTrue(skill.contains("protected int recruitMode() { return 3; }") && skill.contains("protected int giftMode() { return 4; }"));
        String goddess = Files.readString(Path.of(root + "skill/GoddessOfFaithSkill.java"));
        assertTrue(goddess.contains("BLESSING = 3, MIRACLE = 4") && goddess.contains("recruitMode() { return 5; }") && goddess.contains("giftMode() { return 6; }"), "Blessing and Miracle keep 3 and 4");
        String rules = Files.readString(Path.of(root + "skill/rules/FaithWorshipRules.java"));
        assertFalse(rules.contains("net.minecraft") || rules.contains("net.neoforged"), "the rules stay free of Minecraft classes");
        String worship = Files.readString(Path.of(root + "skill/FaithWorship.java"));
        assertFalse(worship.contains("net.minecraft.client") || worship.contains("Minecraft.getInstance"), "server only");
        assertTrue(worship.contains("MAX_INVITES") && worship.contains("MAX_CLOCKS"), "the state maps are bounded");
    }
}
