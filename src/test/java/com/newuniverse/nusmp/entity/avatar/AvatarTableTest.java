package com.newuniverse.nusmp.entity.avatar;

import com.newuniverse.nusmp.entity.zogratis.MoveBook;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Devil Avatar's move table, its two variants, and played-out fights through the scheduler: what it does at each power and distance. */
class AvatarTableTest {
    static final List<AvatarTable.Spec> SPECS = AvatarTable.specs();

    @Test
    void theTableIsWellFormedForBothVariants() {
        for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA}) {
            List<AvatarTable.Spec> specs = AvatarTable.specs(v);
            assertTrue(specs.size() + 1 >= 10 && specs.size() + 1 <= 14, "ten to fourteen moves counting the plain blow: " + (specs.size() + 1));
            Set<String> ids = new HashSet<>();
            for (AvatarTable.Spec s : specs) {
                assertTrue(ids.add(s.id()), "unique id " + s.id());
                assertFalse(s.label().isBlank());
                assertTrue(s.minPower() >= 20 && s.minPower() <= 60, s.id() + " is available before the Gates");
                assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
                assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
                assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
                assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
                assertTrue(s.recovery() >= 8, "a beat to breathe after every move");
                assertTrue(s.manaCost() > 0);
            }
            assertSame(AvatarTable.byId(v, AvatarTable.ABYSSAL_COLLAPSE), specs.get(specs.size() - 1), "the ultimate closes the list");
            assertNull(AvatarTable.byId(v, "nope"));
        }
    }

    @Test
    void bothVariantsShareTheMoveBookAndOnlyTheWeightsDiffer() {
        List<AvatarTable.Spec> a = AvatarTable.specs(AvatarTable.LUCIFERO), b = AvatarTable.specs(AvatarTable.MEGICULA);
        assertEquals(a.size(), b.size());
        boolean anyDiffers = false;
        for (int i = 0; i < a.size(); i++) {
            AvatarTable.Spec x = a.get(i), y = b.get(i);
            assertEquals(x.id(), y.id(), "the same move at the same index (the client reads names by index)");
            assertEquals(x.label(), y.label());
            assertEquals(x.minPower(), y.minPower());
            assertEquals(x.windUpTicks(), y.windUpTicks());
            assertEquals(x.cooldownTicks(), y.cooldownTicks());
            assertEquals(x.minRange(), y.minRange());
            assertEquals(x.maxRange(), y.maxRange());
            assertEquals(x.ultimate(), y.ultimate());
            assertEquals(x.crowd(), y.crowd());
            assertEquals(x.recovery(), y.recovery());
            if (x.weight() != y.weight()) {
                anyDiffers = true;
                assertTrue(AvatarTable.isGravity(x.id()) || AvatarTable.isCurse(x.id()), x.id() + " is neither a gravity nor a curse move");
            }
        }
        assertTrue(anyDiffers);
    }

    @Test
    void theLuciferoAvatarLeansOnGravityAndTheMegiculaAvatarOnCurses() {
        int gravL = 0, gravM = 0, curseL = 0, curseM = 0;
        for (AvatarTable.Spec s : AvatarTable.specs(AvatarTable.LUCIFERO)) {
            if (AvatarTable.isGravity(s.id())) gravL += s.weight();
            if (AvatarTable.isCurse(s.id())) curseL += s.weight();
        }
        for (AvatarTable.Spec s : AvatarTable.specs(AvatarTable.MEGICULA)) {
            if (AvatarTable.isGravity(s.id())) gravM += s.weight();
            if (AvatarTable.isCurse(s.id())) curseM += s.weight();
        }
        assertTrue(gravL > gravM, "gravity weights " + gravL + " vs " + gravM);
        assertTrue(curseM > curseL, "curse weights " + curseM + " vs " + curseL);
        assertTrue(gravL > curseL, "the Lucifero Avatar prefers gravity to curses");
        assertTrue(curseM > gravM, "the Megicula Avatar prefers curses to gravity");
        assertFalse(AvatarTable.isGravity(AvatarTable.VOID_CLAWS) || AvatarTable.isCurse(AvatarTable.VOID_CLAWS));
        assertEquals(AvatarTable.byId(AvatarTable.VOID_CLAWS).weight(), AvatarTable.byId(AvatarTable.MEGICULA, AvatarTable.VOID_CLAWS).weight(), "the plain moves keep their weight");
        assertEquals(6, AvatarTable.weight(AvatarTable.LUCIFERO, AvatarTable.CEILING_PULL, 4));
        assertEquals(3, AvatarTable.weight(AvatarTable.MEGICULA, AvatarTable.CEILING_PULL, 4));
        assertEquals(1, AvatarTable.weight(AvatarTable.MEGICULA, AvatarTable.CEILING_PULL, 1), "never below 1");
    }

    @Test
    void theMixIsGravityCurseStepSummonAndOneUltimate() {
        assertEquals(1, SPECS.stream().filter(AvatarTable.Spec::ultimate).count());
        AvatarTable.Spec ult = AvatarTable.byId(AvatarTable.ABYSSAL_COLLAPSE);
        assertTrue(ult.ultimate() && ult.minPower() >= 60, "only usable at 60% and above");
        assertTrue(ult.windUpTicks() >= 40, "a ritual, not a flick");
        assertTrue(Math.round(ult.windUpTicks() * 0.75f) >= 30, "at the Gates it still winds up for the framework's ultimate minimum");
        assertTrue(ult.recovery() >= 40, "it is exposed afterwards (Move.EXPOSED_FROM)");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 10), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged");
        assertTrue(SPECS.stream().anyMatch(AvatarTable.Spec::crowd), "crowd moves");
        for (String id : new String[]{AvatarTable.CEILING_PULL, AvatarTable.FLOOR_CRUSH, AvatarTable.INVERSION_FIELD, AvatarTable.HEX_VOLLEY, AvatarTable.DREAD_BRAND,
                AvatarTable.SHADOW_STEP, AvatarTable.CALL_DISCIPLES, AvatarTable.VOID_CLAWS, AvatarTable.CURSE_NOVA})
            assertNotNull(AvatarTable.byId(id), id);
    }

    @Test
    void theMainMovesAreFastAndOnlyTheUltimateExposesTheBoss() {
        for (AvatarTable.Spec s : SPECS) {
            if (s.ultimate()) continue;
            assertTrue(s.recovery() < 40, s.id() + " must not expose the boss (recovery under 40 ticks)");
            if (!s.id().equals(AvatarTable.CALL_DISCIPLES)) assertTrue(s.windUpTicks() >= 14 && s.windUpTicks() <= 24, s.id() + " is a fast move: " + s.windUpTicks());
        }
        assertEquals(20, AvatarTable.byId(AvatarTable.CEILING_PULL).windUpTicks(), "the pull winds up for 20 ticks");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        // an attack, not a utility move (the Disciples work at any distance and would hide a gap)
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA})
                assertTrue(AvatarTable.specs(v).stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                                && !s.id().equals(AvatarTable.CALL_DISCIPLES)), "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (AvatarTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(AvatarTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.5 && all < PowerCurve.tuning(20).manaCap() * 3, "the opening pool pays for a few moves, not all");
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(AvatarTable.CLAWS > AvatarTable.CRUSH, "the close blow is the heavy one");
        assertTrue(AvatarTable.BOLT * AvatarMath.boltCount(4) < AvatarTable.ULT_CRUSH * 3, "a full volley is no crush");
        assertTrue(AvatarTable.BOLT < AvatarTable.NOVA, "a bolt is a small hit");
        assertTrue(AvatarTable.ULT_CRUSH > AvatarTable.CRUSH && AvatarTable.ULT_NOVA > 0, "the ultimate hits hardest");
    }

    @Test
    void namesAndVariants() {
        assertEquals("Avatar of Lucifero", AvatarTable.nameOf(AvatarTable.LUCIFERO));
        assertEquals("Avatar of Megicula", AvatarTable.nameOf(AvatarTable.MEGICULA));
        assertEquals(AvatarTable.LUCIFERO, AvatarTable.variantOf(0));
        assertEquals(AvatarTable.MEGICULA, AvatarTable.variantOf(1));
        assertEquals(AvatarTable.LUCIFERO, AvatarTable.variantOf(7), "anything unknown is the Lucifero Avatar");
        assertEquals(AvatarTable.LUCIFERO, AvatarTable.variantOf(-3));
    }

    /** Plays a fight: a decision every 5 ticks, the target wandering between 2 and 36 blocks. Returns how often each move was used. */
    static Map<String, Integer> play(int variant, long seed, int power, int ticks) {
        List<AvatarTable.Spec> specs = AvatarTable.specs(variant);
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(power);
        long busyUntil = 0;
        String prev = "";
        int run = 0;
        for (long t = 20; t < ticks; t += 5) {
            if (t < busyUntil) continue;
            double dist = 2 + 17 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1 + (int) (t / 300 % 3), t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(specs, s);
            if (idx < 0) continue;
            AvatarTable.Spec m = specs.get(idx);
            sch.commit(m, s, (int) Math.max(10, Math.round(m.cooldownTicks() * tu.cooldownMultiplier())));
            used.merge(m.id(), 1, Integer::sum);
            busyUntil = t + Math.max(8, Math.round(m.windUpTicks() * tu.windUpMultiplier())) + m.recovery();
            if (m.id().equals(prev)) run++;
            else { prev = m.id(); run = 1; }
            assertTrue(m.ultimate() || run <= MoveScheduler.REPEAT_LIMIT, "no move spammed: " + m.id());
        }
        return used;
    }

    @Test
    void atTheOpeningItUsesOnlyTheOpeningMovesAndNeverTheUltimate() {
        for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA}) {
            for (long seed = 1; seed <= 5; seed++) {
                Map<String, Integer> used = play(v, seed, 20, 20000);
                for (String id : used.keySet()) assertTrue(AvatarTable.byId(v, id).minPower() <= 20, id + " is not an opening move");
                assertNull(used.get(AvatarTable.ABYSSAL_COLLAPSE));
                assertNull(used.get(AvatarTable.INVERSION_FIELD), "the field waits for 40%");
                assertNull(used.get(AvatarTable.CURSE_NOVA), "the nova waits for 40%");
                assertTrue(used.size() >= 5, "variety at 20%: " + used.keySet());
            }
        }
    }

    @Test
    void theCollapseNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(AvatarTable.LUCIFERO, seed, 59, 20000).get(AvatarTable.ABYSSAL_COLLAPSE));
            assertNotNull(play(AvatarTable.LUCIFERO, seed, 60, 20000).get(AvatarTable.ABYSSAL_COLLAPSE), "it uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(AvatarTable.byId(AvatarTable.ABYSSAL_COLLAPSE), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA}) {
            for (long seed = 1; seed <= 3; seed++) {
                Map<String, Integer> used = play(v, seed, 100, 60000);
                for (AvatarTable.Spec s : AvatarTable.specs(v)) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (variant " + v + ", seed " + seed + ")");
                int ults = used.get(AvatarTable.ABYSSAL_COLLAPSE);
                assertTrue(ults >= 3, "several collapses in a long fight");
                assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
            }
        }
    }

    @Test
    void theVariantsPlayDifferentlyOverALongFight() {
        int gravityL = 0, gravityM = 0, curseL = 0, curseM = 0;
        for (long seed = 1; seed <= 12; seed++) {
            for (Map.Entry<String, Integer> e : play(AvatarTable.LUCIFERO, seed, 80, 40000).entrySet()) {
                if (AvatarTable.isGravity(e.getKey())) gravityL += e.getValue();
                if (AvatarTable.isCurse(e.getKey())) curseL += e.getValue();
            }
            for (Map.Entry<String, Integer> e : play(AvatarTable.MEGICULA, seed, 80, 40000).entrySet()) {
                if (AvatarTable.isGravity(e.getKey())) gravityM += e.getValue();
                if (AvatarTable.isCurse(e.getKey())) curseM += e.getValue();
            }
        }
        assertTrue(gravityL > gravityM, "the Lucifero Avatar bends gravity more: " + gravityL + " vs " + gravityM);
        assertTrue(curseM > curseL, "the Megicula Avatar curses more: " + curseM + " vs " + curseL);
    }

    @Test
    void upCloseTheClawsAreTheBreadAndButter() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 4000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(3.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            AvatarTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            used.merge(m.id(), 1, Integer::sum);
        }
        assertTrue(used.getOrDefault(AvatarTable.VOID_CLAWS, 0) >= 1, "close in it claws: " + used);
        assertNull(used.get(AvatarTable.HEX_VOLLEY), "no volley at 3 blocks");
        assertNull(used.get(AvatarTable.SHADOW_STEP), "no step at 3 blocks");
    }

    @Test
    void theMoveBookKeepsEveryMove() {
        for (int v : new int[]{AvatarTable.LUCIFERO, AvatarTable.MEGICULA}) {
            MoveBook<AvatarTable.Spec> book = MoveBook.of(AvatarTable.specs(v));
            assertEquals(AvatarTable.specs(v).size(), book.moves().size());
            assertTrue(book.dropped().isEmpty());
            assertEquals(AvatarTable.specs(v).size() - 1, book.indexOf(AvatarTable.ABYSSAL_COLLAPSE));
        }
    }
}
