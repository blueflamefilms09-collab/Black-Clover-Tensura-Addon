package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's rule is "mega-rare roughly one rift in a few hundred, with a pity counter per player": it is a number, so it is pinned here. The table must sum to one for every input, the
 * mega-rare share must rise with depth, with luck and with pity (and never fall), the pity must make the next rift certain at its hard limit, and a long simulated run must see about
 * one mega-rare rift in as many as the config says, never a gap longer than the hard limit.
 */
class RiftTiersTest {
    private static final RiftTiers.Config C = RiftTiers.Config.defaults();

    @Test
    void theTableSumsToOneEverywhere() {
        Random r = new Random(7);
        for (int i = 0; i < 2000; i++) {
            double s = r.nextDouble() * 0.5, p = r.nextDouble() * 0.2;
            double[] t = RiftTiers.table(C, s, p, r.nextInt(1500) - 300, r.nextDouble() * 20 - 5, r.nextInt(900));
            assertEquals(3, t.length);
            assertEquals(1.0, t[0] + t[1] + t[2], 1e-9, "sums to one");
            for (double w : t) assertTrue(w >= 0.0 && w <= 1.0, "a weight " + w);
        }
        double[] none = RiftTiers.table(C, 0, 0, 0, 0, 0);
        assertEquals(1.0, none[0] + none[1] + none[2], 1e-9, "no shortcut and no pocket chance still sums to one");
    }

    @Test
    void theOldChancesKeepTheirProportion() {
        double[] t = RiftTiers.table(C, 0.18, 0.06, 0, 0, 0);
        assertEquals(0.18 / 0.24, t[0] / (t[0] + t[1]), 1e-9, "common:rare stays shortcut:pocket");
    }

    @Test
    void theBaseChanceIsOneInTheConfiguredNumber() {
        assertEquals(1.0 / 250.0, RiftTiers.megaChance(C, 0, 0, 0), 1e-12);
        RiftTiers.Config one100 = new RiftTiers.Config(100, 60, 300, 1.0, 0.08);
        assertEquals(0.01, RiftTiers.megaChance(one100, 0, 0, 0), 1e-12);
        assertEquals(1.0 / RiftTiers.MIN_ONE_IN, RiftTiers.megaChance(new RiftTiers.Config(1, 10, 20, 0, 0), 0, 0, 0), 1e-12, "a config under the minimum is raised to it");
    }

    @Test
    void depthLuckAndPityOnlyRaiseTheChance() {
        double prev = -1;
        for (int floor = 0; floor <= 1200; floor += 50) {
            double c = RiftTiers.megaChance(C, floor, 0, 0);
            assertTrue(c >= prev, "depth " + floor);
            prev = c;
        }
        assertEquals(2.0 / 250.0, RiftTiers.megaChance(C, 1000, 0, 0), 1e-12, "twice as likely at floor 1000");
        assertEquals(RiftTiers.megaChance(C, 1000, 0, 0), RiftTiers.megaChance(C, -1000, 0, 0), 1e-12, "the Abyssal road counts the same");
        prev = -1;
        for (double luck = -5; luck <= 30; luck += 1) {
            double c = RiftTiers.megaChance(C, 0, luck, 0);
            assertTrue(c >= prev, "luck " + luck);
            prev = c;
        }
        assertTrue(RiftTiers.megaChance(C, 0, 100, 0) <= 3.0 / 250.0 + 1e-12, "luck helps at most threefold");
        assertTrue(RiftTiers.megaChance(C, 0, -100, 0) >= 0.5 / 250.0 - 1e-12, "bad luck hurts at most by half");
        prev = -1;
        for (int pity = 0; pity <= 700; pity++) {
            double c = RiftTiers.megaChance(C, 0, 0, pity);
            assertTrue(c >= prev - 1e-15, "pity " + pity);
            prev = c;
        }
    }

    @Test
    void pityIsCertainAtTheHardLimitAndNeverBefore() {
        assertEquals(RiftTiers.megaChance(C, 0, 0, 0), RiftTiers.megaChance(C, 0, 0, C.pityStart()), 1e-12, "no pity up to its start");
        assertTrue(RiftTiers.megaChance(C, 0, 0, C.pityHard() - 1) < 1.0, "not certain a rift early");
        assertEquals(1.0, RiftTiers.megaChance(C, 0, 0, C.pityHard()), 0.0);
        assertEquals(1.0, RiftTiers.megaChance(C, 0, 0, 100000), 0.0);
        assertTrue(RiftTiers.isMega(C, 0, 0, C.pityHard(), 0.999999));
        assertTrue(!RiftTiers.isMega(C, 0, 0, 0, 0.5));
    }

    @Test
    void theCounterResetsOnAMegaRareAndGrowsOtherwise() {
        assertEquals(0, RiftTiers.nextPity(77, RiftTier.MEGA_RARE));
        assertEquals(78, RiftTiers.nextPity(77, RiftTier.RARE));
        assertEquals(1, RiftTiers.nextPity(0, RiftTier.COMMON));
        assertEquals(1, RiftTiers.nextPity(-5, RiftTier.COMMON), "a corrupted negative counter is mended");
    }

    @Test
    void aLongRunSeesAboutOneInTheConfiguredNumberAndNeverAGapPastTheHardLimit() {
        Random r = new Random(12345);
        int megas = 0, since = 0, longest = 0, rifts = 400_000;
        int pity = 0;
        for (int i = 0; i < rifts; i++) {
            RiftTier t = RiftTiers.roll(RiftTiers.table(C, 0.18, 0.06, 100, 0, pity), r.nextDouble());
            if (t == RiftTier.MEGA_RARE) {
                megas++;
                longest = Math.max(longest, since);
                since = 0;
            } else since++;
            pity = RiftTiers.nextPity(pity, t);
        }
        double oneIn = rifts / (double) megas;
        assertTrue(oneIn > 100 && oneIn < 250, "one in " + oneIn + " (config 250, depth 100, pity on)");
        assertTrue(longest <= C.pityHard(), "the longest dry spell was " + longest);
    }

    @Test
    void aRollLandsOnTheRightTier() {
        double[] t = {0.6, 0.39, 0.01};
        assertEquals(RiftTier.COMMON, RiftTiers.roll(t, 0.0));
        assertEquals(RiftTier.COMMON, RiftTiers.roll(t, 0.5999));
        assertEquals(RiftTier.RARE, RiftTiers.roll(t, 0.6));
        assertEquals(RiftTier.RARE, RiftTiers.roll(t, 0.9899));
        assertEquals(RiftTier.MEGA_RARE, RiftTiers.roll(t, 0.99));
        assertEquals(RiftTier.MEGA_RARE, RiftTiers.roll(t, 0.9999));
        assertEquals(RiftTier.COMMON, RiftTiers.roll(t, 1.5), "a roll past the table is the plain tear, never the rarest");
        assertEquals(RiftTier.MEGA_RARE, RiftTier.byId("mega_rare"));
        assertEquals(RiftTier.COMMON, RiftTier.byId("from-the-future"));
        assertEquals(RiftTier.MEGA_RARE, RiftTier.byOrdinal(99));
        assertEquals(RiftTier.COMMON, RiftTier.byOrdinal(-4));
    }
}
