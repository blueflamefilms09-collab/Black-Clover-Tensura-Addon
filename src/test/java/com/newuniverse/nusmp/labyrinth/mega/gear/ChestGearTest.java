package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@link ChestGear} is the whole chest decision, so its numbers are pinned against the REAL shipped table the way the kill rolls are: the chances of a standard
 * chest at floor 1 are the configured chest bases; a treasure chest multiplies ONLY Special and Rare; the depth shift moves weight from Normal to Special and Rare
 * as it does for kills; Luck adds to the total and nothing else; the caps hold when a config is absurd; a table that is no chest rolls nothing; one chest is one
 * decision and at most one piece; and a hundred thousand seeded chests land on the table within five standard deviations (a fixed seed, so a pass is a pass for
 * ever). A chest must also be more generous than one kill (the design: a chest is a deliberate reward), which is asserted, so someone retuning the defaults
 * the wrong way round hears about it.
 */
class ChestGearTest {
    private static final GearRates C = GearRates.CHEST_DEFAULT;
    private static final double F = GearRates.DEFAULT_TREASURE_FACTOR;
    private static final ChestRules.Kind STD = ChestRules.Kind.STANDARD, TRS = ChestRules.Kind.TREASURE, NONE = ChestRules.Kind.NONE;

    private static double[] at(ChestRules.Kind k, int floor, int luck) { return ChestGear.chances(k, C, F, GearFx.tables(), floor, luck); }

    private static void same(double[] a, double[] b, String why) {
        assertEquals(a.length, b.length, why);
        for (int i = 0; i < a.length; i++) assertEquals(a[i], b[i], 1e-12, why + " (index " + i + ")");
    }

    @Test
    void aStandardChestAtFloorOneHasTheConfiguredChances() {
        double[] p = at(STD, 1, 0);
        assertEquals(0.22, p[0], 1e-12);
        assertEquals(0.06, p[1], 1e-12);
        assertEquals(0.02, p[2], 1e-12);
        assertEquals(0.30, GearRoller.total(p), 1e-12);
    }

    @Test
    void aTreasureChestMultipliesOnlySpecialAndRare() {
        double[] s = at(STD, 1, 0), t = at(TRS, 1, 0);
        assertEquals(s[0], t[0], 1e-12, "Normal is not more likely in a treasure chest");
        assertEquals(s[1] * F, t[1], 1e-12);
        assertEquals(s[2] * F, t[2], 1e-12);
        GearRates r = ChestGear.ratesFor(TRS, C, 3.5);
        assertEquals(C.normal(), r.normal());
        assertEquals(C.special() * 3.5, r.special(), 1e-12);
        assertEquals(C.rare() * 3.5, r.rare(), 1e-12);
        assertEquals(C.floorShift(), r.floorShift());
        assertEquals(C.lootingBonus(), r.lootingBonus());
        assertSame(C, ChestGear.ratesFor(STD, C, 3.5), "a standard chest keeps the base rates");
        assertEquals(C.special(), ChestGear.ratesFor(TRS, C, 0.2).special(), 1e-12, "a factor below 1 never makes a treasure chest worse than a standard one");
    }

    @Test
    void aTableThatIsNoChestRollsNothing() {
        for (double d : at(NONE, 1, 0)) assertEquals(0.0, d);
        for (double d : ChestGear.chances(null, C, F, GearFx.tables(), 1, 3)) assertEquals(0.0, d);
        assertFalse(ChestGear.decide(GearFx.dice(1), NONE, C, F, GearFx.tables(), 1, 0).isPresent());
        assertFalse(ChestGear.decide(GearFx.dice(1), null, C, F, GearFx.tables(), 1, 0).isPresent());
        final int[] draws = {0};
        Dice counting = () -> { draws[0]++; return 0.0; };
        ChestGear.decide(counting, NONE, C, F, GearFx.tables(), 1, 0);
        assertEquals(0, draws[0], "a table that is no chest costs no random draw");
    }

    @Test
    void deeperChestsHoldBetterGradesAndTheTotalStaysPut() {
        double[] shallow = at(STD, 1, 0), mid = at(STD, 26, 0), deep = at(STD, 60, 0);
        assertTrue(shallow[0] > mid[0] && mid[0] > deep[0], "Normal falls with depth");
        assertTrue(shallow[2] < mid[2] && mid[2] < deep[2], "Rare rises with depth");
        assertTrue(mid[1] > shallow[1] && mid[1] > deep[1], "Special peaks in the middle");
        for (double[] p : new double[][] {mid, deep}) assertEquals(GearRoller.total(shallow), GearRoller.total(p), 0.05 * GearRoller.total(shallow), "the total drifts less than 5 %");
        same(at(STD, -26, 0), at(STD, 26, 0), "the Abyssal road is as deep as the Holy one");
        same(at(STD, 1, 0), at(STD, 0, 0), "floor 0 and the gaps between sectors count as floor 1");
    }

    @Test
    void luckAddsToTheTotalAndKeepsTheMix() {
        double[] none = at(STD, 1, 0), three = at(STD, 1, 3);
        assertEquals(GearRoller.total(none) + 3 * C.lootingBonus(), GearRoller.total(three), 1e-12);
        for (int g = 0; g < 3; g++) assertEquals(none[g] / GearRoller.total(none), three[g] / GearRoller.total(three), 1e-12, "the share of each grade is unchanged");
        same(at(STD, 1, 10), at(STD, 1, 400), "Luck above the ceiling counts as the ceiling");
        same(at(STD, 1, 0), at(STD, 1, -5), "negative Luck counts as none");
    }

    @Test
    void luckLevelsAreWholeAndBounded() {
        assertEquals(0, ChestGear.luckLevels(0f));
        assertEquals(0, ChestGear.luckLevels(-3f));
        assertEquals(0, ChestGear.luckLevels(Float.NaN));
        assertEquals(0, ChestGear.luckLevels(0.4f));
        assertEquals(1, ChestGear.luckLevels(0.5f));
        assertEquals(2, ChestGear.luckLevels(1.6f));
        assertEquals(ChestGear.MAX_LUCK_LEVELS, ChestGear.luckLevels(99f));
        assertEquals(ChestGear.MAX_LUCK_LEVELS, ChestGear.luckLevels(Float.POSITIVE_INFINITY));
    }

    @Test
    void theCapsHoldWhateverTheConfigAsks() {
        GearTables t = GearFx.tables();
        GearRates silly = new GearRates(5.0, 5.0, 5.0, 0.2, 0.25);
        for (ChestRules.Kind k : new ChestRules.Kind[] {STD, TRS}) {
            for (int floor : new int[] {1, 26, 80}) {
                double[] p = ChestGear.chances(k, silly, 10.0, t, floor, 10);
                double total = 0;
                for (GearGrade g : GearGrade.values()) {
                    assertTrue(p[g.ordinal()] <= t.maxChance(g) + 1e-12, k + " floor " + floor + " " + g + " " + p[g.ordinal()]);
                    total += p[g.ordinal()];
                }
                assertTrue(total <= t.totalCap() + 1e-12, k + " floor " + floor + " total " + total);
            }
        }
    }

    /**
     * The ceilings of the table are a safety net for a mistyped config, not part of the design: under the SHIPPED numbers (kills and chests, standard and
     * treasure, every depth, with and without Luck) a grade's chance is exactly base x factor x curve and the total is the plain sum. Found by drawing the chest odds:
     * the first ceilings (0.30 / 0.20 / 0.15 / 0.50) cut the deep treasure chest's curve flat, so they were raised.
     */
    @Test
    void noCeilingBitesUnderTheShippedDefaults() {
        GearTables t = GearFx.tables();
        for (ChestRules.Kind k : new ChestRules.Kind[] {STD, TRS}) {
            GearRates r = ChestGear.ratesFor(k, C, F);
            for (int floor = 1; floor <= 80; floor++) {
                double x = GearRoller.progress(r, floor), raw = 0.0;
                double[] got = ChestGear.chances(k, C, F, t, floor, 0);
                for (GearGrade g : GearGrade.values()) {
                    double want = r.base(g) * t.curve(g).at(x);
                    assertEquals(want, got[g.ordinal()], 1e-12, k + " floor " + floor + " " + g + ": a ceiling cut the chance");
                    raw += want;
                }
                assertTrue(raw < t.totalCap(), k + " floor " + floor + " total " + raw + " reaches the total ceiling");
            }
        }
        for (int floor = 1; floor <= 80; floor++) {
            double x = GearRoller.progress(GearRates.DEFAULT, floor);
            double[] got = GearRoller.chances(GearRates.DEFAULT, t, floor, 0);
            for (GearGrade g : GearGrade.values()) assertEquals(GearRates.DEFAULT.base(g) * t.curve(g).at(x), got[g.ordinal()], 1e-12, "kill floor " + floor + " " + g);
        }
    }

    @Test
    void aChestIsMoreGenerousThanAKill() {
        double kill = GearRoller.total(GearRoller.chances(GearRates.DEFAULT, GearFx.tables(), 1, 0));
        double chest = GearRoller.total(at(STD, 1, 0));
        assertTrue(chest > 2 * kill, "a chest (" + chest + ") pays much more often than one kill (" + kill + ")");
        assertTrue(GearRoller.total(at(TRS, 1, 0)) > chest, "a treasure chest pays more often than a standard one");
        assertTrue(at(TRS, 1, 0)[2] / GearRoller.total(at(TRS, 1, 0)) > at(STD, 1, 0)[2] / GearRoller.total(at(STD, 1, 0)), "and a bigger share of its pieces is Rare");
    }

    @Test
    void oneChestIsOneDecisionAndAtMostOnePiece() {
        GearTables t = GearFx.tables();
        final int[] draws = {0};
        Dice d = () -> { draws[0]++; return 0.999999; };           // above every chance: no piece, and exactly one draw was spent deciding
        assertFalse(ChestGear.decide(d, STD, C, F, t, 1, 0).isPresent());
        assertEquals(1, draws[0]);
        Dice low = GearFx.dice(5);
        for (int i = 0; i < 20_000; i++) {
            Optional<GearSpec> s = ChestGear.decide(low, TRS, C, F, t, 1 + i % 60, i % 4);
            if (s.isPresent()) {
                assertNotNull(t.grade(s.get().grade()).items(s.get().slot()));
                assertTrue(t.grade(s.get().grade()).items(s.get().slot()).contains(s.get().item()), "a piece is made only of what the table offers");
            }
        }
    }

    @Test
    void theDistributionOfAHundredThousandChestsMatchesTheTable() {
        GearTables t = GearFx.tables();
        int n = 100_000, seed = 0;
        for (ChestRules.Kind k : new ChestRules.Kind[] {STD, TRS}) {
            for (int floor : new int[] {1, 26, 60}) {
                double[] p = at(k, floor, 0);
                Dice d = GearFx.dice(9000 + 31 * seed++);
                int[] count = new int[3];
                for (int i = 0; i < n; i++) ChestGear.decide(d, k, C, F, t, floor, 0).ifPresent(s -> count[s.grade().ordinal()]++);
                for (int g = 0; g < 3; g++) {
                    double expect = n * p[g], sigma = Math.sqrt(n * p[g] * (1 - p[g]));
                    assertTrue(Math.abs(count[g] - expect) <= 5 * sigma + 1, k + " floor " + floor + " " + GearGrade.values()[g] + ": " + count[g] + " of " + n + ", expected " + expect + " +- " + 5 * sigma);
                }
            }
        }
    }

    @Test
    void aSeedReplaysTheSameChests() {
        GearTables t = GearFx.tables();
        Dice a = GearFx.dice(77), b = GearFx.dice(77);
        for (int i = 0; i < 3000; i++) assertEquals(ChestGear.decide(a, TRS, C, F, t, 1 + i % 50, 0), ChestGear.decide(b, TRS, C, F, t, 1 + i % 50, 0));
    }
}
