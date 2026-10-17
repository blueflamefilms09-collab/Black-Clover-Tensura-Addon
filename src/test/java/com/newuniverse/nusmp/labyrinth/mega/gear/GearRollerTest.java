package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearSpec.Ench;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.GradeTable;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.Pool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the roller decides, for every kill in the Mega-Dungeon, whether a piece drops and what it is, and a wrong number here is invisible in play (a
 * rate that is half what was meant just feels stingy) so the numbers are pinned against the REAL shipped table: the chances at floor 1 are the
 * config defaults, Normal falls and Rare rises with depth while Special peaks in the middle, the total barely moves as weight shifts from one grade
 * to another, Looting changes how OFTEN but not WHAT, the caps hold when a config is absurd, one roll gives one decision, a seed replays the same
 * pieces, 100000 rolls land on the table's chances within five standard deviations (a statistical test whose random stream is a fixed seed, so it
 * either always passes or always fails), and every piece that comes out is made only of things the table offers: a slot the grade has items for, one of
 * its items, a bonus inside the grade's range, distinct enchantments at or below the level cap, engravings only on weapons and shields, a resistance
 * inside the grade's range for that kind of piece.
 */
class GearRollerTest {
    private static final GearRates R = GearRates.DEFAULT;
    private static final GearGrade[] G = GearGrade.values();

    private static double[] at(int floor, int looting) { return GearRoller.chances(R, GearFx.tables(), floor, looting); }

    @Test
    void floorOneChancesAreTheConfiguredBases() {
        double[] p = at(1, 0);
        assertEquals(0.05, p[0], 1e-12);
        assertEquals(0.015, p[1], 1e-12);
        assertEquals(0.004, p[2], 1e-12);
        assertEquals(0.069, GearRoller.total(p), 1e-12);
    }

    @Test
    void floorZeroAndTheGapsCountAsFloorOne() {
        assertEquals(GearRoller.total(at(1, 0)), GearRoller.total(at(0, 0)), 0.0);
        for (int g = 0; g < 3; g++) assertEquals(at(1, 0)[g], at(-1, 0)[g], 0.0, "the Abyssal road is as deep as the Holy one: " + g);
        for (int f = 1; f <= 80; f++) for (int g = 0; g < 3; g++) assertEquals(at(f, 0)[g], at(-f, 0)[g], 0.0);
    }

    @Test
    void normalFallsAndRareRisesWithDepth() {
        double[] prev = at(1, 0);
        for (int f = 2; f <= 80; f++) {
            double[] now = at(f, 0);
            assertTrue(now[0] <= prev[0] + 1e-12, "Normal must never rise: floor " + f);
            assertTrue(now[2] >= prev[2] - 1e-12, "Rare must never fall: floor " + f);
            prev = now;
        }
        assertTrue(at(51, 0)[0] < at(1, 0)[0] * 0.5, "Normal is at most half by floor 51");
        assertTrue(at(51, 0)[2] > at(1, 0)[2] * 4, "Rare is at least four times by floor 51");
    }

    @Test
    void specialPeaksInTheMiddle() {
        int best = 1;
        double top = 0;
        for (int f = 1; f <= 80; f++) {
            double s = at(f, 0)[1];
            if (s > top) { top = s; best = f; }
        }
        assertTrue(best >= 15 && best <= 40, "Special peaks about half-way along the curve, not at an end: floor " + best);
        assertTrue(top > at(1, 0)[1] * 1.5, "the peak is clearly above the start");
        assertTrue(top > at(80, 0)[1] * 1.2, "and clearly above where it settles");
        assertTrue(at(80, 0)[1] > at(1, 0)[1], "Special still ends above where it began");
    }

    @Test
    void depthMovesWeightInsteadOfAddingDrops() {
        double start = GearRoller.total(at(1, 0));
        for (int f = 1; f <= 80; f++) {
            double t = GearRoller.total(at(f, 0));
            assertTrue(Math.abs(t / start - 1.0) <= 0.10, "the total chance drifts more than 10 percent at floor " + f + ": " + t + " against " + start);
        }
    }

    @Test
    void lootingMakesDropsMoreOftenButNotDifferent() {
        double[] none = at(20, 0), three = at(20, 3);
        assertEquals(GearRoller.total(none) + 0.03, GearRoller.total(three), 1e-12, "+0.01 per level on the total");
        for (int g = 1; g < 3; g++) assertEquals(none[g] / none[0], three[g] / three[0], 1e-9, "the mix is the same: grade " + g);
        assertEquals(GearRoller.total(none), GearRoller.total(GearRoller.chances(R, GearFx.tables(), 20, -2)), 0.0, "a negative Looting level is nothing");
    }

    @Test
    void capsHoldWhateverTheConfigAsks() {
        GearTables t = GearFx.tables();
        GearRates silly = new GearRates(5.0, 5.0, 5.0, 0.2, 1.0);
        for (int f : new int[] {1, 10, 40, 200}) {
            for (int looting : new int[] {0, 3}) {
                double[] p = GearRoller.chances(silly, t, f, looting);
                assertTrue(GearRoller.total(p) <= t.totalCap() + 1e-12, "total cap at floor " + f);
                for (GearGrade g : G) assertTrue(p[g.ordinal()] <= t.maxChance(g) + 1e-12, g + " cap at floor " + f);
            }
        }
        GearRates none = new GearRates(0, 0, 0, 0.02, 0.5);
        assertEquals(0.0, GearRoller.total(GearRoller.chances(none, t, 10, 3)), 0.0, "no base chance, no drop, however much Looting");
    }

    @Test
    void floorShiftZeroTurnsTheShiftOff() {
        GearRates flat = new GearRates(0.05, 0.015, 0.004, 0.0, 0.01);
        for (int g = 0; g < 3; g++) assertEquals(GearRoller.chances(flat, GearFx.tables(), 1, 0)[g], GearRoller.chances(flat, GearFx.tables(), 70, 0)[g], 0.0);
        assertEquals(0.0, GearRoller.progress(flat, 70), 0.0);
        assertEquals(1.0, GearRoller.progress(R, 51), 1e-12);
        assertEquals(1.0, GearRoller.progress(R, 500), 0.0, "the progress stops at 1");
        assertEquals(0.5, GearRoller.progress(R, 26), 1e-12);
    }

    @Test
    void aGradeTheTablesDoNotOfferNeverDrops() {
        GearTables full = GearFx.tables();
        Map<GearGrade, GradeTable> only = new EnumMap<>(GearGrade.class);
        only.put(GearGrade.NORMAL, full.grade(GearGrade.NORMAL));
        GearTables normalOnly = new GearTables(only, Map.of(), Map.of(), 0.5, Map.of(GearSlot.SWORD, 1.0));
        double[] p = GearRoller.chances(R, normalOnly, 30, 0);
        assertEquals(0.0, p[1], 0.0);
        assertEquals(0.0, p[2], 0.0);
        assertTrue(p[0] > 0.0);
        assertFalse(GearRoller.make(GearFx.dice(1), GearGrade.RARE, null, normalOnly).isPresent());
        assertEquals(0.0, GearRoller.total(GearRoller.chances(R, GearTables.EMPTY, 30, 3)), 0.0);
        assertFalse(GearRoller.roll(GearFx.dice(2), 30, 3, GearTables.EMPTY, R).isPresent());
    }

    @Test
    void oneKillIsOneDecisionAndOneDraw() {
        AtomicInteger draws = new AtomicInteger();
        Dice counting = () -> { draws.incrementAndGet(); return 0.999999; };
        assertNull(GearRoller.pickGrade(counting, at(1, 0)));
        assertEquals(1, draws.get(), "the decision to drop is one draw");
        assertEquals(GearGrade.NORMAL, GearRoller.pickGrade(() -> 0.0, at(1, 0)));
        assertEquals(GearGrade.SPECIAL, GearRoller.pickGrade(() -> 0.0501, at(1, 0)));
        assertEquals(GearGrade.RARE, GearRoller.pickGrade(() -> 0.0651, at(1, 0)));
        assertNull(GearRoller.pickGrade(() -> 0.0691, at(1, 0)));
    }

    @Test
    void theGradeDistributionMatchesTheTable() {
        GearTables t = GearFx.tables();
        int n = 100_000;
        int seed = 0;
        for (int floor : new int[] {1, 26, 60}) {
            double[] p = GearRoller.chances(R, t, floor, 0);
            Dice d = GearFx.dice(1000 + seed++);
            int[] count = new int[3];
            int none = 0;
            for (int i = 0; i < n; i++) {
                Optional<GearSpec> s = GearRoller.roll(d, floor, 0, t, R);
                if (s.isPresent()) count[s.get().grade().ordinal()]++;
                else none++;
            }
            for (int g = 0; g < 3; g++) {
                double expect = n * p[g], sigma = Math.sqrt(n * p[g] * (1 - p[g]));
                assertTrue(Math.abs(count[g] - expect) <= 5 * sigma + 1, "floor " + floor + " grade " + G[g] + ": " + count[g] + " drops of " + n + ", expected " + expect + " +- " + 5 * sigma);
            }
            double pn = 1 - GearRoller.total(p), sigma = Math.sqrt(n * pn * (1 - pn));
            assertTrue(Math.abs(none - n * pn) <= 5 * sigma + 1, "floor " + floor + ": " + none + " kills without a drop");
        }
    }

    @Test
    void theShareOfNormalFallsAndTheShareOfRareRisesInTheRolls() {
        GearTables t = GearFx.tables();
        double[] shareNormal = new double[3], shareRare = new double[3];
        int i = 0;
        for (int floor : new int[] {1, 26, 60}) {
            Dice d = GearFx.dice(77 + floor);
            int normal = 0, rare = 0, all = 0;
            for (int k = 0; k < 200_000; k++) {
                Optional<GearSpec> s = GearRoller.roll(d, floor, 0, t, R);
                if (s.isEmpty()) continue;
                all++;
                if (s.get().grade() == GearGrade.NORMAL) normal++;
                if (s.get().grade() == GearGrade.RARE) rare++;
            }
            shareNormal[i] = normal / (double) all;
            shareRare[i] = rare / (double) all;
            i++;
        }
        assertTrue(shareNormal[0] > shareNormal[1] && shareNormal[1] > shareNormal[2], "Normal's share of the drops falls with depth");
        assertTrue(shareRare[0] < shareRare[1] && shareRare[1] < shareRare[2], "Rare's share of the drops rises with depth");
    }

    @Test
    void aSeedReplaysTheSamePieces() {
        GearTables t = GearFx.tables();
        List<GearSpec> a = new ArrayList<>(), b = new ArrayList<>();
        Dice da = GearFx.dice(424242), db = GearFx.dice(424242);
        for (int i = 0; i < 4000; i++) {
            GearRoller.roll(da, 30, 1, t, R).ifPresent(a::add);
            GearRoller.roll(db, 30, 1, t, R).ifPresent(b::add);
        }
        assertFalse(a.isEmpty());
        assertEquals(a, b);
        List<GearSpec> c = new ArrayList<>();
        Dice dc = GearFx.dice(424243);
        for (int i = 0; i < 4000; i++) GearRoller.roll(dc, 30, 1, t, R).ifPresent(c::add);
        assertNotEquals(a, c);
    }

    @Test
    void everyPieceIsMadeOnlyOfWhatTheTableOffers() {
        GearTables t = GearFx.tables();
        Dice d = GearFx.dice(9);
        for (GearGrade g : G) {
            GradeTable gt = t.grade(g);
            for (int i = 0; i < 30_000; i++) {
                Optional<GearSpec> o = GearRoller.make(d, g, null, t);
                assertTrue(o.isPresent(), "the shipped table offers every grade");
                GearSpec s = o.get();
                assertEquals(g, s.grade());
                assertNotNull(s.slot());
                assertTrue(gt.items(s.slot()).contains(s.item()), g + " " + s.slot() + ": " + s.item() + " is not in the table");
                if (s.slot().hasPowerStat()) {
                    assertTrue(s.powerPercent() >= gt.power().min() && s.powerPercent() <= gt.power().max(), "power " + s.powerPercent());
                } else assertEquals(0, s.powerPercent(), "bows, crossbows and shields have no percentage stat");
                assertTrue(s.durabilityPercent() >= gt.durability().min() && s.durabilityPercent() <= gt.durability().max());
                assertTrue(s.enchants().size() <= gt.enchantCount().max());
                assertTrue(s.enchants().size() >= Math.min(gt.enchantCount().min(), gt.enchantPool(s.slot()).size()));
                assertDistinctAndInPool(s.enchants(), gt.enchantPool(s.slot()), gt.enchantLevelCap());
                if (s.slot().engraves()) {
                    assertTrue(s.engravings().size() <= gt.engraveCount().max());
                    assertDistinctAndInPool(s.engravings(), gt.engravePool(s.slot()), Integer.MAX_VALUE);
                } else assertTrue(s.engravings().isEmpty(), "armour carries no engravings");
                GearTables.IntRange resist = gt.resist(s.slot());
                assertTrue(s.resistPercent() >= resist.min() && s.resistPercent() <= resist.max(), "resist " + s.resistPercent() + " of " + s.slot());
                assertEquals(new GearInfo(g, s.slot(), s.resistPercent()), s.info());
            }
        }
    }

    private static void assertDistinctAndInPool(List<Ench> got, List<Pool> pool, int cap) {
        Set<String> seen = new HashSet<>();
        for (Ench e : got) {
            assertTrue(seen.add(e.id()), "duplicate " + e.id());
            Pool p = null;
            for (Pool q : pool) if (q.id().equals(e.id())) p = q;
            assertNotNull(p, e.id() + " is not in the pool");
            assertTrue(e.level() >= 1 && e.level() <= Math.max(1, Math.min(p.max(), cap)), e.id() + " level " + e.level());
        }
    }

    @Test
    void theSlotWeightsShapeTheSlots() {
        GearTables t = GearFx.tables();
        int n = 100_000;
        Dice d = GearFx.dice(31);
        Map<GearSlot, Integer> count = new EnumMap<>(GearSlot.class);
        double sum = 0;
        for (GearSlot s : GearSlot.values()) sum += t.slotWeight(s);
        for (int i = 0; i < n; i++) count.merge(GearRoller.make(d, GearGrade.SPECIAL, null, t).orElseThrow().slot(), 1, Integer::sum);
        for (GearSlot s : GearSlot.values()) {
            double p = t.slotWeight(s) / sum, expect = n * p, sigma = Math.sqrt(n * p * (1 - p));
            assertTrue(Math.abs(count.getOrDefault(s, 0) - expect) <= 5 * sigma + 1, s + ": " + count.get(s) + " of " + n + ", expected " + expect);
        }
    }

    @Test
    void aForcedSlotIsHonouredAndAnUnofferedOneRefused() {
        GearTables t = GearFx.tables();
        for (GearSlot s : GearSlot.values()) {
            for (GearGrade g : G) assertEquals(s, GearRoller.make(GearFx.dice(5), g, s, t).orElseThrow().slot());
        }
        Map<GearSlot, List<String>> swordOnly = new EnumMap<>(GearSlot.class);
        swordOnly.put(GearSlot.SWORD, List.of("minecraft:iron_sword"));
        GradeTable gt = new GradeTable(swordOnly, GearTables.IntRange.ZERO, GearTables.IntRange.ZERO, GearTables.IntRange.ZERO, 1, Map.of(),
                GearTables.IntRange.ZERO, Map.of(), GearTables.IntRange.ZERO, GearTables.IntRange.ZERO);
        GearTables small = new GearTables(Map.of(GearGrade.NORMAL, gt), Map.of(), Map.of(), 0.5, Map.of());
        assertFalse(GearRoller.make(GearFx.dice(5), GearGrade.NORMAL, GearSlot.BOOTS, small).isPresent(), "the grade has no boots");
        assertEquals(GearSlot.SWORD, GearRoller.make(GearFx.dice(5), GearGrade.NORMAL, null, small).orElseThrow().slot(),
                "every slot weight is zero in this table, so the only offered slot is chosen");
    }

    @Test
    void theBossGuaranteeIsAlwaysARarePieceWithResistance() {
        GearTables t = GearFx.tables();
        Dice d = GearFx.dice(8);
        for (int i = 0; i < 20_000; i++) {
            GearSpec s = GearRoller.make(d, GearGrade.RARE, null, t).orElseThrow();
            assertEquals(GearGrade.RARE, s.grade());
            assertTrue(s.resistPercent() >= 4, "a Rare piece always resists magic: " + s);
            assertFalse(s.enchants().isEmpty(), "a Rare piece always has an enchantment, except where the slot has no pool");
            if (s.slot().engraves()) assertFalse(s.engravings().isEmpty(), "a Rare weapon or shield always has an engraving");
        }
    }

    @Test
    void enchantmentLevelsNeverPassTheGradesCap() {
        GearTables t = GearFx.tables();
        Dice d = GearFx.dice(44);
        for (int i = 0; i < 20_000; i++) {
            GearSpec special = GearRoller.make(d, GearGrade.SPECIAL, null, t).orElseThrow();
            for (Ench e : special.enchants()) assertEquals(1, e.level(), "Special enchantments are minor: level 1 (" + e.id() + ")");
            assertTrue(special.enchants().size() <= 1, "Special has at most one minor enchantment");
            assertTrue(special.engravings().size() <= 1, "Special has at most one light engraving");
            assertEquals(0, special.resistPercent(), "Special does not resist magic");
            GearSpec rare = GearRoller.make(d, GearGrade.RARE, null, t).orElseThrow();
            for (Ench e : rare.enchants()) assertTrue(e.level() <= 2, "Rare enchantments stay at level 2 or below (" + e.id() + ")");
            assertTrue(rare.enchants().size() <= 2 && rare.engravings().size() <= 2);
            GearSpec normal = GearRoller.make(d, GearGrade.NORMAL, null, t).orElseThrow();
            assertTrue(normal.enchants().isEmpty() && normal.engravings().isEmpty() && normal.powerPercent() == 0 && normal.durabilityPercent() == 0 && normal.resistPercent() == 0,
                    "Normal is plain: " + normal);
        }
    }
}
