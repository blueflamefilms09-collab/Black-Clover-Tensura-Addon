package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.newuniverse.nusmp.arc.tier.TierMove.Kind.*;
import static com.newuniverse.nusmp.arc.tier.TierMove.of;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 3.39.0 (tier boss powers, docs/bosses/tier_powers.md): the trait catalogue is complete (a counter, a window, a price), the budget grows by letter, the pure balance model is deterministic and
 * reacts to the right knobs, every rule of {@link TierBalance#problems} fires on a hand-built bad row (and a clean row passes), the trait helpers and constants are sane, and the REAL rosters
 * obey all of it. The last group (marked below) depends on the roster rows (TierRosterF / TierRosterD), which are tuned in the same release: it is the one that must be green before shipping.
 */
class TierBalanceTest {
    private static final double EPS = 1e-9;
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    // ---------------------------------------------------------------- hand-built rows (no rosters involved)

    private static TierMove.Builder jab() { return of("jab", "Jab", CLEAVE).wind(16).cd(70).range(0, 6).weight(4).dmg(1.0).size(4.5); }

    private static TierMove.Builder bolt() { return of("bolt", "Bolt", BOLT).wind(18).cd(80).range(4, 32).weight(4).dmg(1.0); }

    private static TierMove.Builder burst() { return of("burst", "Burst", SLAM).wind(22).cd(120).range(0, 8).weight(3).dmg(1.1).size(4.0); }

    private static TierMove.Builder ult() { return of("ult", "Ult", WAVE).wind(46).range(0, 20).size(10).dmg(1.2).ult(); }

    /** Ordinary moves only (power 20), so the plain pressure is the same at every power and a trait is the only thing that can change it. */
    private static TierRow.Builder plain(TierLetter l) { return TierRow.of(l, "t", "T", "x").move(jab()).move(bolt()).move(burst()); }

    /** A full kit with the one ultimate. */
    private static TierRow.Builder kit(TierLetter l) { return plain(l).move(ult()); }

    private static boolean anyContains(List<String> problems, String... words) {
        for (String p : problems) {
            boolean all = true;
            for (String w : words) if (!p.contains(w)) { all = false; break; }
            if (all) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- (a) the catalogue

    @Test
    void everyTraitHasAWhatACounterAWindowALabelAndAPriceOfOneToThree() {
        assertTrue(TierTrait.values().length >= 16, "the catalogue of tier_powers.md has 16 traits, entries are only ever appended");
        Set<String> labels = new HashSet<>();
        for (TierTrait t : TierTrait.values()) {
            assertNotNull(t.what(), t + " what");
            assertNotNull(t.counter(), t + " counter");
            assertNotNull(t.window(), t + " window");
            assertNotNull(t.label(), t + " label");
            assertFalse(t.what().isBlank(), t + " has no description");
            assertFalse(t.counter().isBlank(), t + " has no counter: a trait without a counter is refused");
            assertFalse(t.window().isBlank(), t + " has no window: a trait without a window is a wall");
            assertFalse(t.label().isBlank(), t + " has no label");
            assertTrue(t.price() >= 1 && t.price() <= 3, t + " costs " + t.price() + ", prices are 1..3");
            assertTrue(labels.add(t.label()), "the label '" + t.label() + "' is used twice");
        }
    }

    @Test
    void everyTraitCanBeBoughtByTheSmallestBudget() {
        for (TierTrait t : TierTrait.values()) assertTrue(t.price() <= TierLetter.F.traitBudget(), t + " could never be bought");
    }

    // ---------------------------------------------------------------- (b) the budget

    @Test
    void theTraitBudgetIsFiveSevenNineAndOnlyGoesUp() {
        assertEquals(5, TierLetter.F.traitBudget());
        assertEquals(7, TierLetter.D.traitBudget());
        assertEquals(9, TierLetter.C.traitBudget());
        assertEquals(11, TierLetter.B.traitBudget(), "tier_powers.md: C 9, B 11, A 13");
        assertEquals(13, TierLetter.A.traitBudget());
        TierLetter[] l = TierLetter.values();
        for (int i = 0; i < l.length; i++) {
            assertEquals(5 + 2 * i, l[i].traitBudget(), l[i].label());
            if (i > 0) assertTrue(l[i].traitBudget() > l[i - 1].traitBudget(), l[i].label() + " must have a bigger budget than " + l[i - 1].label());
        }
    }

    // ---------------------------------------------------------------- (c) TierBalance on hand-built rows

    @Test
    void aCleanRowHasNoProblem() {
        TierRow r = kit(TierLetter.F).trait(TierTrait.SLOW_FIELD).trait(TierTrait.CLIMB).build();
        assertEquals(4, r.traitPoints());
        assertEquals(List.of(), TierBalance.problems(r));
        TierRow none = kit(TierLetter.D).build();
        assertEquals(List.of(), TierBalance.problems(none), "a row without traits is fine");
        TierRow full = kit(TierLetter.D).trait(TierTrait.ARMOUR).trait(TierTrait.SPORE_POISON).build();
        assertEquals(5, full.traitPoints());
        assertEquals(List.of(), TierBalance.problems(full));
    }

    @Test
    void aTraitBudgetOverflowIsAProblem() {
        TierRow r = kit(TierLetter.F).trait(TierTrait.ARMOUR).trait(TierTrait.SOUL_MARK).build();            // 3 + 3 = 6 > 5
        assertEquals(6, r.traitPoints());
        List<String> bad = TierBalance.problems(r);
        assertTrue(anyContains(bad, "6 trait points", "has 5"), bad.toString());
        assertEquals(1, bad.size(), "only the budget is wrong: " + bad);
        // the same two traits are fine in tier D (budget 7)
        assertEquals(List.of(), TierBalance.problems(kit(TierLetter.D).trait(TierTrait.ARMOUR).trait(TierTrait.SOUL_MARK).build()));
        // exactly the budget is allowed
        assertEquals(List.of(), TierBalance.problems(kit(TierLetter.F).trait(TierTrait.ARMOUR).trait(TierTrait.SPORE_POISON).build()));
    }

    @Test
    void aDuplicateTraitIsAProblem() {
        TierRow r = kit(TierLetter.D).trait(TierTrait.CLIMB).trait(TierTrait.CLIMB).build();            // 4 points, inside the budget
        List<String> bad = TierBalance.problems(r);
        assertTrue(anyContains(bad, "CLIMB", "twice"), bad.toString());
    }

    @Test
    void moreThanTwoTraitsIsAProblemEvenInsideTheBudget() {
        TierRow r = kit(TierLetter.F).trait(TierTrait.SHIELD_BREAK).trait(TierTrait.CLIMB).trait(TierTrait.STEAM).build();            // 1 + 2 + 2 = 5 = the F budget
        assertEquals(TierLetter.F.traitBudget(), r.traitPoints());
        List<String> bad = TierBalance.problems(r);
        assertTrue(anyContains(bad, "3 traits", "at most " + TierBalance.MAX_TRAITS), bad.toString());
        assertFalse(anyContains(bad, "trait points"), "the budget itself is not exceeded: " + bad);
    }

    @Test
    void aTraitThatNeedsAMoveMustNameOneOfTheRowAndAStandaloneTraitMustNot() {
        int needing = 0, standalone = 0;
        for (TierTrait t : TierTrait.values()) {
            if (t.needsMove()) {
                needing++;
                List<String> none = TierBalance.problems(kit(TierLetter.D).trait(t).build());
                assertTrue(anyContains(none, t.name(), "must name a move"), t + " names no move but is not refused: " + none);
                List<String> wrong = TierBalance.problems(kit(TierLetter.D).trait(t, "no_such_move").build());
                assertTrue(anyContains(wrong, t.name(), "must name a move", "no_such_move"), t + " names a missing move but is not refused: " + wrong);
                List<String> right = TierBalance.problems(kit(TierLetter.D).trait(t, "bolt").build());
                assertFalse(anyContains(right, "must name a move"), t + " names a real move but is refused: " + right);
                assertFalse(anyContains(right, "stands alone"), t + ": " + right);
            } else {
                standalone++;
                List<String> named = TierBalance.problems(kit(TierLetter.D).trait(t, "bolt").build());
                assertTrue(anyContains(named, t.name(), "stands alone"), t + " is standalone but may name a move: " + named);
                List<String> bare = TierBalance.problems(kit(TierLetter.D).trait(t).build());
                assertFalse(anyContains(bare, "must name a move"), t + ": " + bare);
                assertFalse(anyContains(bare, "stands alone"), t + ": " + bare);
            }
        }
        assertTrue(needing >= 1 && standalone >= 1, "both kinds of trait exist");
        // the explicit examples of tier_powers.md
        assertTrue(TierTrait.MIRROR.needsMove() && TierTrait.PULL.needsMove() && TierTrait.KARMA.needsMove() && TierTrait.ECHO.needsMove() && TierTrait.UNTOUCHABLE.needsMove());
        assertFalse(TierTrait.CLIMB.needsMove() || TierTrait.FUSION.needsMove() || TierTrait.STEAM.needsMove() || TierTrait.SLOW_FIELD.needsMove());
    }

    /** An ultimate already leaves the boss exposed (no armour or guard then) and an echoed ultimate would double the biggest hit, so these three may not hang on it. */
    @Test
    void echoMirrorAndUntouchableCannotHangOnTheUltimate() {
        for (TierTrait t : new TierTrait[] {TierTrait.ECHO, TierTrait.MIRROR, TierTrait.UNTOUCHABLE}) {
            List<String> onUlt = TierBalance.problems(kit(TierLetter.D).trait(t, "ult").build());
            assertTrue(anyContains(onUlt, t.name(), "cannot hang on the ultimate", "ult"), t + " on the ultimate must be refused: " + onUlt);
            assertEquals(List.of(), TierBalance.problems(kit(TierLetter.D).trait(t, "bolt").build()), t + " on an ordinary move is fine");
        }
        for (TierTrait t : new TierTrait[] {TierTrait.BALANCE, TierTrait.KARMA, TierTrait.PULL})
            assertEquals(List.of(), TierBalance.problems(kit(TierLetter.D).trait(t, "ult").build()), t + " may hang on the ultimate");
    }

    @Test
    void aMoveOverTheCapIsAProblemAndTheCapItselfIsFine() {
        double over = TierBalance.MAX_ORDINARY + 0.01;
        List<String> bad = TierBalance.problems(plain(TierLetter.F).move(of("big", "Big", SLAM).range(0, 6).dmg(over)).move(ult()).build());
        assertTrue(anyContains(bad, "t/big", "cap"), bad.toString());
        assertEquals(1, bad.size(), bad.toString());
        // exactly 1.6x is fine for an ordinary move, and an ordinary move may not borrow the ultimate's 2.0x
        assertEquals(List.of(), TierBalance.problems(plain(TierLetter.F).move(of("big", "Big", SLAM).range(0, 6).dmg(TierBalance.MAX_ORDINARY)).move(ult()).build()));
        assertFalse(TierBalance.problems(plain(TierLetter.F).move(of("big", "Big", SLAM).range(0, 6).dmg(1.8)).build()).isEmpty());
        // the ultimate: 2.0x is fine, over it is not, and 1.8x (over the ordinary cap) is fine for an ultimate
        assertEquals(List.of(), TierBalance.problems(plain(TierLetter.F).move(of("u", "U", WAVE).range(0, 20).dmg(TierBalance.MAX_ULTIMATE).ult()).build()));
        assertEquals(List.of(), TierBalance.problems(plain(TierLetter.F).move(of("u", "U", WAVE).range(0, 20).dmg(1.8).ult()).build()));
        List<String> badUlt = TierBalance.problems(plain(TierLetter.F).move(of("u", "U", WAVE).range(0, 20).dmg(TierBalance.MAX_ULTIMATE + 0.01).ult()).build());
        assertTrue(anyContains(badUlt, "t/u", "cap"), badUlt.toString());
    }

    @Test
    void theCapsAreTheOnesOfTheDoc() {
        assertEquals(1.6, TierBalance.MAX_ORDINARY, EPS);
        assertEquals(2.0, TierBalance.MAX_ULTIMATE, EPS);
        assertEquals(2, TierBalance.MAX_TRAITS);
        assertEquals(25, TierBalance.BAND_PERCENT);
        assertEquals(0.25, TierBalance.BAND, EPS);
        assertEquals(60, TierBalance.REF_POWER);
        assertTrue(TierBalance.HIT_RATE > 0 && TierBalance.HIT_RATE < 1 && TierBalance.ULTIMATE_HIT_RATE > 0 && TierBalance.ULTIMATE_HIT_RATE <= 1);
    }

    @Test
    void aBossWithoutAttackIsAProblem() {
        TierRow r = TierRow.of(TierLetter.F, "t", "T", "x").scale(1.0, 0.0).move(jab()).move(ult()).build();
        assertTrue(anyContains(TierBalance.problems(r), "no attack"), TierBalance.problems(r).toString());
    }

    @Test
    void thePressureModelIsDeterministic() {
        TierRow r = kit(TierLetter.D).trait(TierTrait.CLIMB).trait(TierTrait.STEAM).build();
        for (int power : new int[] {0, 20, 40, 60, 80, 100})
            assertEquals(TierBalance.pressure(r, power), TierBalance.pressure(r, power), "power " + power);
        TierRow twin = kit(TierLetter.D).trait(TierTrait.CLIMB).trait(TierTrait.STEAM).build();
        assertEquals(TierBalance.pressure(r, 60), TierBalance.pressure(twin, 60), EPS, "two equal rows give the same number");
        assertEquals(TierBalance.table(List.of(r, twin)), TierBalance.table(List.of(r, twin)));
    }

    @Test
    void thePressureIsNeverNegativeAndAnEmptyKitIsZero() {
        for (TierLetter l : new TierLetter[] {TierLetter.F, TierLetter.D})
            for (int power = 0; power <= 100; power += 10) {
                assertTrue(TierBalance.pressure(kit(l).build(), power) >= 0, l + " power " + power);
                assertTrue(TierBalance.pressure(kit(l).trait(TierTrait.STEAM).build(), power) >= 0, l + " power " + power);
            }
        TierRow empty = TierRow.of(TierLetter.F, "t", "T", "x").build();
        assertEquals(0.0, TierBalance.pressure(empty, 60), EPS);
        // only an ultimate: nothing to throw before 60% power
        TierRow onlyUlt = TierRow.of(TierLetter.F, "t", "T", "x").move(ult()).build();
        assertEquals(0.0, TierBalance.pressure(onlyUlt, 20), EPS);
        assertTrue(TierBalance.pressure(onlyUlt, 60) > 0);
    }

    @Test
    void aBiggerHitMakesMorePressureAndItScalesLinearlyWithDamageAndAttack() {
        TierRow weak = TierRow.of(TierLetter.F, "t", "T", "x").move(jab().dmg(1.0)).move(bolt().dmg(1.0)).move(ult().dmg(1.0)).build();
        TierRow strong = TierRow.of(TierLetter.F, "t", "T", "x").move(jab().dmg(1.5)).move(bolt().dmg(1.5)).move(ult().dmg(1.5)).build();
        for (int power : new int[] {20, 60, 100}) {
            double a = TierBalance.pressure(weak, power), b = TierBalance.pressure(strong, power);
            assertTrue(b > a, "power " + power + ": " + b + " should be more than " + a);
            assertEquals(1.5, b / a, 1e-9, "the schedule does not depend on the damage, so the pressure is linear in it (power " + power + ")");
        }
        // a bigger hit on ONE move also raises it
        TierRow oneUp = TierRow.of(TierLetter.F, "t", "T", "x").move(jab().dmg(1.0)).move(bolt().dmg(1.3)).move(ult().dmg(1.0)).build();
        assertTrue(TierBalance.pressure(oneUp, 60) > TierBalance.pressure(weak, 60));
        // a bigger attack stat too
        TierRow stronger = TierRow.of(TierLetter.F, "t", "T", "x").scale(1.0, 1.5).move(jab()).move(bolt()).move(ult()).build();
        TierRow normal = TierRow.of(TierLetter.F, "t", "T", "x").move(jab()).move(bolt()).move(ult()).build();
        assertEquals(1.5, TierBalance.pressure(stronger, 60) / TierBalance.pressure(normal, 60), 1e-6);
    }

    @Test
    void moreBoltsInAVolleyMakeMorePressureAndALongerCooldownMakesLess() {
        TierRow one = TierRow.of(TierLetter.F, "t", "T", "x").move(of("v", "V", VOLLEY).range(4, 30).count(1).dmg(0.8)).build();
        TierRow five = TierRow.of(TierLetter.F, "t", "T", "x").move(of("v", "V", VOLLEY).range(4, 30).count(5).dmg(0.8)).build();
        assertEquals(5.0, TierBalance.pressure(five, 20) / TierBalance.pressure(one, 20), 1e-9);
        TierRow quick = TierRow.of(TierLetter.F, "t", "T", "x").move(of("m", "M", SLAM).range(0, 8).cd(60).dmg(1.0)).build();
        TierRow slow = TierRow.of(TierLetter.F, "t", "T", "x").move(of("m", "M", SLAM).range(0, 8).cd(300).dmg(1.0)).build();
        assertTrue(TierBalance.pressure(quick, 20) > TierBalance.pressure(slow, 20));
    }

    @Test
    void hitsCountsEveryBoltCircleAndWispOnceAndTheOtherShapesOnce() {
        assertEquals(3, TierBalance.hits(of("v", "V", VOLLEY).count(3).build()));
        assertEquals(4, TierBalance.hits(of("m", "M", MARKED).count(4).build()));
        assertEquals(2, TierBalance.hits(of("s", "S", SWARM).count(2).build()));
        for (TierMove.Kind k : new TierMove.Kind[] {CLEAVE, SLAM, WAVE, BOLT, RUSH, BEAM})
            assertEquals(1, TierBalance.hits(of("x", "X", k).count(5).build()), k + " is one hit however its count is set");
    }

    @Test
    void healthAtFollowsThePowerCurveAndIsNeverRaisedByPower() {
        assertEquals(0.15, TierBalance.healthAt(100), EPS);
        assertEquals(0.35, TierBalance.healthAt(80), EPS);
        assertEquals(0.55, TierBalance.healthAt(60), EPS);
        assertEquals(0.75, TierBalance.healthAt(40), EPS);
        assertEquals(0.92, TierBalance.healthAt(20), EPS);
        assertEquals(0.92, TierBalance.healthAt(0), EPS);
        double last = 2;
        for (int p = 0; p <= 100; p += 5) {
            double h = TierBalance.healthAt(p);
            assertTrue(h > 0 && h < 1 && h <= last, "power " + p);
            last = h;
        }
    }

    @Test
    void climbAddsPressureAsThePowerRisesAndOnlyWithTheTrait() {
        TierRow plain = plain(TierLetter.D).build();
        TierRow climb = plain(TierLetter.D).trait(TierTrait.CLIMB).build();
        assertEquals(TierBalance.pressure(plain, 20), TierBalance.pressure(plain, 100), EPS, "without the trait the ordinary kit is the same at every power");
        assertTrue(TierBalance.pressure(climb, 100) > TierBalance.pressure(climb, 20), "a CLIMB row hits harder at 100% power than at 20%");
        assertEquals(TierBalance.pressure(plain, 20), TierBalance.pressure(climb, 20), EPS, "no step is climbed at 20%");
        assertEquals(1.0 + TierTrait.CLIMB_DAMAGE * TierTrait.CLIMB_MAX_STEPS, TierBalance.pressure(climb, 100) / TierBalance.pressure(plain, 100), 1e-9);
        assertEquals(1.0 + TierTrait.CLIMB_DAMAGE * 2, TierBalance.pressure(climb, 60) / TierBalance.pressure(plain, 60), 1e-9);
        assertEquals(TierBalance.traitMultiplier(climb, 100), 1.0 + TierTrait.CLIMB_DAMAGE * 3, EPS);
        double last = 0;
        for (int p = 20; p <= 100; p += 20) {
            double now = TierBalance.pressure(climb, p);
            assertTrue(now >= last - EPS, "climb never lowers the pressure (power " + p + ")");
            last = now;
        }
    }

    @Test
    void fusionAddsPressureOnceTheBossIsHurtButNotAtTheStart() {
        TierRow plain = plain(TierLetter.D).build();
        TierRow fusion = plain(TierLetter.D).trait(TierTrait.FUSION).build();
        assertTrue(TierBalance.pressure(fusion, 60) > TierBalance.pressure(plain, 60), "FUSION row beats the same row without it at 60%");
        assertEquals(TierBalance.pressure(plain, 20), TierBalance.pressure(fusion, 20), EPS, "no fusion yet at 20% power (92% health)");
        assertEquals(TierTrait.FUSION_DAMAGE, TierBalance.pressure(fusion, 60) / TierBalance.pressure(plain, 60), 1e-9, "one step at 55% health");
        assertEquals(TierTrait.FUSION_DAMAGE * TierTrait.FUSION_DAMAGE, TierBalance.pressure(fusion, 100) / TierBalance.pressure(plain, 100), 1e-9, "two steps at 15% health");
    }

    @Test
    void steamAndSporePoisonAddTheirOwnConstantDamagePerSecond() {
        TierRow plain = kit(TierLetter.D).build();
        TierRow steam = kit(TierLetter.D).trait(TierTrait.STEAM).build();
        TierRow poison = kit(TierLetter.D).trait(TierTrait.SPORE_POISON).build();
        TierRow both = kit(TierLetter.D).trait(TierTrait.STEAM).trait(TierTrait.SPORE_POISON).build();
        for (int power : new int[] {20, 60, 100}) {
            double base = TierBalance.pressure(plain, power);
            assertEquals(base + TierBalance.STEAM_DPS, TierBalance.pressure(steam, power), 1e-9, "steam at " + power);
            assertEquals(base + TierBalance.POISON_DPS, TierBalance.pressure(poison, power), 1e-9, "poison at " + power);
            assertEquals(base + TierBalance.STEAM_DPS + TierBalance.POISON_DPS, TierBalance.pressure(both, power), 1e-9, "both at " + power);
        }
        assertEquals(0.0, TierBalance.traitDps(plain), EPS);
        assertEquals(TierBalance.STEAM_DPS, TierBalance.traitDps(steam), EPS);
        assertEquals(TierBalance.POISON_DPS, TierBalance.traitDps(poison), EPS);
        assertTrue(TierBalance.STEAM_DPS > 0 && TierBalance.POISON_DPS > 0);
    }

    @Test
    void soulMarkAndKarmaRaiseTheDamageByTheirUptimeShare() {
        TierRow plain = plain(TierLetter.D).build();
        TierRow marks = plain(TierLetter.D).trait(TierTrait.SOUL_MARK).build();
        TierRow karma = plain(TierLetter.D).trait(TierTrait.KARMA, "bolt").build();
        double frenzy = 1.0 + (TierTrait.FRENZY_DAMAGE - 1.0) * TierBalance.FRENZY_UPTIME;
        double karmaMul = 1.0 + (TierTrait.KARMA_DAMAGE - 1.0) * TierBalance.KARMA_UPTIME;
        assertEquals(frenzy, TierBalance.pressure(marks, 60) / TierBalance.pressure(plain, 60), 1e-9);
        assertEquals(karmaMul, TierBalance.pressure(karma, 60) / TierBalance.pressure(plain, 60), 1e-9);
        assertEquals(1.0, TierBalance.traitMultiplier(plain, 60), EPS);
        assertTrue(frenzy > 1.0 && karmaMul > 1.0);
    }

    @Test
    void defensiveAndControlTraitsDoNotChangeThePressure() {
        TierRow plain = kit(TierLetter.D).build();
        TierTrait[] standalone = {TierTrait.SLOW_FIELD, TierTrait.ARMOUR, TierTrait.SYSTEM_LOCK, TierTrait.SIGHT, TierTrait.SHIELD_BREAK};
        for (TierTrait t : standalone)
            assertEquals(TierBalance.pressure(plain, 60), TierBalance.pressure(kit(TierLetter.D).trait(t).build(), 60), 1e-9, t + " is defence / control, not damage");
    }

    @Test
    void theMeanOfATierIgnoresTheOtherTiersAndIsZeroForNoRows() {
        TierRow f1 = kit(TierLetter.F).build();
        TierRow f2 = kit(TierLetter.F).scale(1.0, 2.0).build();
        TierRow d1 = kit(TierLetter.D).build();
        List<TierRow> rows = List.of(f1, f2, d1);
        double expectF = (TierBalance.pressure(f1, 60) + TierBalance.pressure(f2, 60)) / 2;
        assertEquals(expectF, TierBalance.mean(rows, TierLetter.F, 60), 1e-9);
        assertEquals(TierBalance.pressure(d1, 60), TierBalance.mean(rows, TierLetter.D, 60), 1e-9);
        assertEquals(0.0, TierBalance.mean(rows, TierLetter.C, 60), EPS);
        assertEquals(0.0, TierBalance.mean(List.of(), TierLetter.F, 60), EPS);
    }

    private static List<TierRow> normals(int n) {
        List<TierRow> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) rows.add(TierRow.of(TierLetter.F, "norm" + i, "N" + i, "x").move(jab()).move(bolt()).move(ult()).build());
        return rows;
    }

    @Test
    void theBandFlagsAnOutlierOfATierOfThreeOrMoreAndNotATinyTier() {
        assertEquals(List.of(), TierBalance.bandProblems(normals(3)), "three equal bosses are inside their own band");
        // six normal bosses and one that deals 1.8x: it lies above +25% of the mean, the six lie inside
        List<TierRow> rows = normals(6);
        rows.add(TierRow.of(TierLetter.F, "spiky", "S", "x").move(jab().dmg(1.8)).move(bolt().dmg(1.8)).move(ult().dmg(1.8)).build());
        List<String> bad = TierBalance.bandProblems(rows);
        assertEquals(1, bad.size(), bad.toString());
        assertTrue(anyContains(bad, "spiky", "outside"), bad.toString());
        // a boss that deals almost nothing is flagged below the band
        List<TierRow> low = normals(6);
        low.add(TierRow.of(TierLetter.F, "meek", "M", "x").move(jab().dmg(0.05)).move(bolt().dmg(0.05)).move(ult().dmg(0.05)).build());
        List<String> badLow = TierBalance.bandProblems(low);
        assertEquals(1, badLow.size(), badLow.toString());
        assertTrue(anyContains(badLow, "meek", "outside"), badLow.toString());
        // the very same spread in a tier of two is not compared at all
        assertEquals(List.of(), TierBalance.bandProblems(List.of(rows.get(0), rows.get(6))));
        assertEquals(List.of(), TierBalance.bandProblems(List.of()));
        // tiers are compared separately: a spread among two D bosses does not disturb the three F bosses
        List<TierRow> mixed = normals(3);
        mixed.add(TierRow.of(TierLetter.D, "d1", "D1", "x").move(jab()).move(bolt()).move(ult()).build());
        mixed.add(TierRow.of(TierLetter.D, "d2", "D2", "x").move(jab().dmg(1.5)).move(bolt().dmg(1.5)).move(ult().dmg(1.5)).build());
        assertEquals(List.of(), TierBalance.bandProblems(mixed));
    }

    @Test
    void theTableHasOneLinePerRowAndNamesTheTraits() {
        TierRow a = TierRow.of(TierLetter.F, "alpha", "A", "x").move(jab()).move(bolt()).move(ult()).trait(TierTrait.MIRROR, "bolt").trait(TierTrait.CLIMB).build();
        TierRow b = TierRow.of(TierLetter.D, "beta", "B", "x").move(jab()).move(bolt()).move(ult()).build();
        String table = TierBalance.table(List.of(a, b));
        String[] lines = table.strip().split("\\R");
        assertEquals(3, lines.length, table);
        assertTrue(lines[0].contains("boss") && lines[0].contains("p@60"), lines[0]);
        assertTrue(lines[1].startsWith("alpha") && lines[1].contains("MIRROR(bolt)") && lines[1].contains("CLIMB"), lines[1]);
        assertTrue(lines[2].startsWith("beta"), lines[2]);
        assertEquals(1, TierBalance.table(List.of()).strip().split("\\R").length, "an empty table still has its header");
    }

    // ---------------------------------------------------------------- the row and the spec

    @Test
    void theRowKeepsTheOldFourArgumentConstructorAndHasTheTraitHelpers() {
        TierRow r = kit(TierLetter.D).trait(TierTrait.MIRROR, "jab").trait(TierTrait.CLIMB).build();
        assertTrue(r.has(TierTrait.MIRROR) && r.has(TierTrait.CLIMB) && !r.has(TierTrait.STEAM));
        assertEquals("jab", r.moveOf(TierTrait.MIRROR));
        assertNull(r.moveOf(TierTrait.CLIMB));
        assertNull(r.moveOf(TierTrait.STEAM));
        assertEquals(TierTrait.MIRROR.price() + TierTrait.CLIMB.price(), r.traitPoints());
        TierRow old = new TierRow(r.character(), r.letter(), r.moves(), r.height());
        assertEquals(List.of(), old.traits());
        assertEquals(0, old.traitPoints());
        assertFalse(old.has(TierTrait.CLIMB));
        TierRow nul = new TierRow(r.character(), r.letter(), r.moves(), r.height(), null);
        assertEquals(List.of(), nul.traits(), "a null trait list is an empty one");
        assertThrows(UnsupportedOperationException.class, () -> r.traits().add(TierTraitSpec.of(TierTrait.STEAM)), "the trait list is immutable");
        assertEquals(r, new TierRow(r.character(), r.letter(), r.moves(), r.height(), r.traits()));
    }

    @Test
    void aTraitSpecNeedsATraitAndKeepsItsMove() {
        assertThrows(IllegalArgumentException.class, () -> new TierTraitSpec(null, "x"));
        assertNull(TierTraitSpec.of(TierTrait.CLIMB).moveId());
        assertEquals("bolt", TierTraitSpec.on(TierTrait.ECHO, "bolt").moveId());
        assertEquals(TierTrait.ECHO, TierTraitSpec.on(TierTrait.ECHO, "bolt").trait());
    }

    // ---------------------------------------------------------------- (d) the helpers of TierTrait

    @Test
    void climbStepsFollowThePowerCurve() {
        assertEquals(0, TierTrait.climbSteps(0));
        assertEquals(0, TierTrait.climbSteps(20));
        assertEquals(1, TierTrait.climbSteps(40));
        assertEquals(2, TierTrait.climbSteps(60));
        assertEquals(3, TierTrait.climbSteps(80));
        assertEquals(3, TierTrait.climbSteps(100));
        assertEquals(0, TierTrait.climbSteps(-50), "never negative");
        assertEquals(TierTrait.CLIMB_MAX_STEPS, TierTrait.climbSteps(500), "never above the cap");
        assertEquals(0, TierTrait.climbSteps(39));
        assertEquals(1, TierTrait.climbSteps(59));
        int last = 0;
        for (int p = 0; p <= 120; p++) {
            int s = TierTrait.climbSteps(p);
            assertTrue(s >= last && s >= 0 && s <= TierTrait.CLIMB_MAX_STEPS, "power " + p);
            last = s;
        }
    }

    @Test
    void fusionStepsFollowTheHealthFraction() {
        assertEquals(0, TierTrait.fusionSteps(0.9));
        assertEquals(1, TierTrait.fusionSteps(0.6));
        assertEquals(2, TierTrait.fusionSteps(0.2));
        assertEquals(0, TierTrait.fusionSteps(1.0));
        assertEquals(2, TierTrait.fusionSteps(0.0));
        assertEquals(0, TierTrait.fusionSteps(0.66), "the step is under 66%, not at it");
        assertEquals(1, TierTrait.fusionSteps(0.65));
        assertEquals(1, TierTrait.fusionSteps(0.33));
        assertEquals(2, TierTrait.fusionSteps(0.32));
        double[] at = TierTrait.FUSION_AT;
        assertEquals(2, at.length, "two steps only, announced");
        assertTrue(at[0] > at[1] && at[0] < 1 && at[1] > 0);
    }

    private static List<TierTraitSpec> specs(TierTrait... ts) {
        List<TierTraitSpec> out = new ArrayList<>();
        for (TierTrait t : ts) out.add(TierTraitSpec.of(t));
        return out;
    }

    @Test
    void selfDamageForTheDamageTraitsAtScaleOneAndTwo() {
        assertEquals(1.0, TierTrait.selfDamage(List.of(), 100, 0.1, 1.0, true), EPS, "no trait, no bonus");
        // CLIMB: +8% per step, 3 steps at 100% power, the scale multiplies the percentage
        assertEquals(1.24, TierTrait.selfDamage(specs(TierTrait.CLIMB), 100, 1.0, 1.0, false), 1e-9);
        assertEquals(1.48, TierTrait.selfDamage(specs(TierTrait.CLIMB), 100, 1.0, 2.0, false), 1e-9);
        assertEquals(1.0, TierTrait.selfDamage(specs(TierTrait.CLIMB), 20, 1.0, 2.0, true), EPS, "no step at 20% power");
        assertEquals(1.16, TierTrait.selfDamage(specs(TierTrait.CLIMB), 60, 1.0, 1.0, false), 1e-9);
        assertEquals(1.12, TierTrait.selfDamage(specs(TierTrait.CLIMB), 100, 1.0, 0.5, false), 1e-9, "the lowest setting (0.5) halves the percentage");
        // FUSION: +15% per step, compounded
        assertEquals(1.0, TierTrait.selfDamage(specs(TierTrait.FUSION), 100, 0.9, 1.0, false), EPS);
        assertEquals(1.15, TierTrait.selfDamage(specs(TierTrait.FUSION), 100, 0.6, 1.0, false), 1e-9);
        assertEquals(1.15 * 1.15, TierTrait.selfDamage(specs(TierTrait.FUSION), 100, 0.2, 1.0, false), 1e-9);
        assertEquals(1.3, TierTrait.selfDamage(specs(TierTrait.FUSION), 100, 0.6, 2.0, false), 1e-9);
        assertEquals(1.3 * 1.3, TierTrait.selfDamage(specs(TierTrait.FUSION), 100, 0.2, 2.0, false), 1e-9);
        // SOUL_MARK: +10% only while the frenzy is on
        assertEquals(1.10, TierTrait.selfDamage(specs(TierTrait.SOUL_MARK), 20, 1.0, 1.0, true), 1e-9);
        assertEquals(1.20, TierTrait.selfDamage(specs(TierTrait.SOUL_MARK), 20, 1.0, 2.0, true), 1e-9);
        assertEquals(1.0, TierTrait.selfDamage(specs(TierTrait.SOUL_MARK), 20, 1.0, 1.0, false), EPS);
        assertEquals(1.0, TierTrait.selfDamage(specs(TierTrait.SOUL_MARK), 20, 1.0, 2.0, false), EPS);
        // frenzy changes nothing for the traits that have no frenzy
        assertEquals(TierTrait.selfDamage(specs(TierTrait.CLIMB, TierTrait.FUSION), 80, 0.5, 1.0, false),
                TierTrait.selfDamage(specs(TierTrait.CLIMB, TierTrait.FUSION), 80, 0.5, 1.0, true), EPS);
        // they stack by multiplying
        assertEquals(1.24 * 1.15 * 1.10, TierTrait.selfDamage(specs(TierTrait.CLIMB, TierTrait.FUSION, TierTrait.SOUL_MARK), 100, 0.6, 1.0, true), 1e-9);
        // scale 0 switches every bonus off
        assertEquals(1.0, TierTrait.selfDamage(specs(TierTrait.CLIMB, TierTrait.FUSION, TierTrait.SOUL_MARK), 100, 0.1, 0.0, true), EPS);
        // a bigger scale never makes the boss weaker
        double last = 0;
        for (double s = 0.5; s <= 2.0; s += 0.25) {
            double d = TierTrait.selfDamage(specs(TierTrait.CLIMB, TierTrait.FUSION), 100, 0.2, s, false);
            assertTrue(d >= last, "scale " + s);
            last = d;
        }
    }

    @Test
    void theOtherTraitsGiveNoSelfDamageOrSpeed() {
        for (TierTrait t : TierTrait.values()) {
            if (t == TierTrait.CLIMB || t == TierTrait.FUSION || t == TierTrait.SOUL_MARK) continue;
            assertEquals(1.0, TierTrait.selfDamage(specs(t), 100, 0.1, 2.0, true), EPS, t + " damage");
            assertEquals(0.0, TierTrait.selfSpeed(specs(t), 100, 0.1, 2.0), EPS, t + " speed");
        }
        assertEquals(0.0, TierTrait.selfSpeed(specs(TierTrait.SOUL_MARK), 100, 0.1, 2.0), EPS, "the frenzy is damage only");
    }

    @Test
    void selfSpeedForTheSpeedTraitsAtScaleOneAndTwo() {
        assertEquals(0.0, TierTrait.selfSpeed(List.of(), 100, 0.1, 1.0), EPS);
        assertEquals(0.24, TierTrait.selfSpeed(specs(TierTrait.CLIMB), 100, 1.0, 1.0), 1e-9);
        assertEquals(0.48, TierTrait.selfSpeed(specs(TierTrait.CLIMB), 100, 1.0, 2.0), 1e-9);
        assertEquals(0.0, TierTrait.selfSpeed(specs(TierTrait.CLIMB), 20, 1.0, 2.0), EPS);
        assertEquals(0.08, TierTrait.selfSpeed(specs(TierTrait.CLIMB), 40, 1.0, 1.0), 1e-9);
        assertEquals(0.0, TierTrait.selfSpeed(specs(TierTrait.FUSION), 100, 0.9, 1.0), EPS);
        assertEquals(0.10, TierTrait.selfSpeed(specs(TierTrait.FUSION), 100, 0.6, 1.0), 1e-9);
        assertEquals(0.20, TierTrait.selfSpeed(specs(TierTrait.FUSION), 100, 0.2, 1.0), 1e-9);
        assertEquals(0.40, TierTrait.selfSpeed(specs(TierTrait.FUSION), 100, 0.2, 2.0), 1e-9);
        assertEquals(0.24 + 0.20, TierTrait.selfSpeed(specs(TierTrait.CLIMB, TierTrait.FUSION), 100, 0.2, 1.0), 1e-9, "the speed bonuses add up");
        assertEquals(0.0, TierTrait.selfSpeed(specs(TierTrait.CLIMB, TierTrait.FUSION), 100, 0.2, 0.0), EPS);
        // sanity: at the shipped caps a boss never gets more than +100% speed even at the top setting
        assertTrue(TierTrait.selfSpeed(specs(TierTrait.CLIMB, TierTrait.FUSION), 100, 0.0, 2.0) < 1.0);
    }

    // ---------------------------------------------------------------- (e) constants

    @Test
    void reductionsAndRadiiAreInsideTheirCapsAndTicksArePositive() {
        assertTrue(TierTrait.ARMOUR_CUT > 0 && TierTrait.ARMOUR_CUT <= TierTrait.MAX_CUT, "armour cut");
        assertTrue(TierTrait.UNTOUCHABLE_CUT > 0 && TierTrait.UNTOUCHABLE_CUT <= TierTrait.MAX_CUT, "untouchable cut");
        assertTrue(TierTrait.MAX_CUT > 0 && TierTrait.MAX_CUT < 1, "a cut can never make the boss immune");
        assertTrue(TierTrait.SLOW_RADIUS > 0 && TierTrait.SLOW_RADIUS <= TierTrait.MAX_RADIUS, "slow radius");
        assertTrue(TierTrait.SLOW_RADIUS_GATES >= TierTrait.SLOW_RADIUS && TierTrait.SLOW_RADIUS_GATES <= TierTrait.MAX_RADIUS, "slow radius at the Gates");
        assertTrue(TierTrait.STEAM_REACH > 0 && TierTrait.STEAM_REACH <= TierTrait.MAX_RADIUS, "steam reach");
        assertTrue(TierTrait.SIGHT_REACH > 0 && TierTrait.SIGHT_REACH <= TierTrait.MAX_RADIUS, "sight reach");
        assertEquals(24, TierTrait.MAX_TRACKED, "the framework looks at 24 foes at most, so do the marks");
        assertEquals(3, TierTrait.MARK_MAX);
        assertEquals(3, TierTrait.CLIMB_MAX_STEPS);

        int[] ticks = {TierTrait.SLOW_TICKS, TierTrait.STEAM_FIRE_TICKS, TierTrait.STEAM_GAP_TICKS, TierTrait.UNTOUCHABLE_TICKS, TierTrait.BALANCE_SLACK_TICKS,
                TierTrait.BALANCE_WEAKEN_TICKS, TierTrait.MARK_TICKS, TierTrait.FRENZY_TICKS, TierTrait.MIRROR_TICKS, TierTrait.LOCK_GAP_TICKS, TierTrait.PULL_IMPULSES,
                TierTrait.PULL_GAP_TICKS, TierTrait.POISON_TICKS, TierTrait.SIGHT_TICKS, TierTrait.KARMA_TICKS, TierTrait.ECHO_DELAY_TICKS, TierTrait.SHIELD_TICKS};
        for (int t : ticks) assertTrue(t > 0, "a tick count or impulse count is not positive: " + t);
        assertTrue(TierTrait.PULL_IMPULSES <= 20, "the pull is a handful of impulses, not a loop");
        assertTrue(TierTrait.POISON_TICKS <= TierMove.MAX_EFFECT_TICKS && TierTrait.SLOW_TICKS <= TierMove.MAX_EFFECT_TICKS, "statuses stay short");
    }

    @Test
    void percentagesAreFractionsAndMultipliersAreAboveOne() {
        assertTrue(TierTrait.BALANCE_BACK > 0 && TierTrait.BALANCE_BACK < 1);
        assertTrue(TierTrait.BALANCE_TOP_SHARE > 0 && TierTrait.BALANCE_TOP_SHARE <= 1, "sharing the damage must be a real answer to the balance");
        assertTrue(TierTrait.MIRROR_SHARE > 0 && TierTrait.MIRROR_SHARE < 1);
        assertTrue(TierTrait.BALANCE_BACK_CAP_ATTACKS > 0 && TierTrait.MIRROR_CAP_HP > 0, "the give-back is capped per hit / window");
        assertTrue(TierTrait.FRENZY_DAMAGE > 1 && TierTrait.FUSION_DAMAGE > 1 && TierTrait.KARMA_DAMAGE > 1);
        assertTrue(TierTrait.FUSION_SPEED > 0 && TierTrait.CLIMB_SPEED > 0 && TierTrait.CLIMB_DAMAGE > 0 && TierTrait.PULL_STRENGTH > 0);
        // a boss carries two traits at most: even the worst pair at the top of the setting range (2.0) stays under x3 of its own damage
        TierTrait[] dmg = {TierTrait.CLIMB, TierTrait.FUSION, TierTrait.SOUL_MARK};
        double worst = 0;
        for (TierTrait a : dmg) for (TierTrait b : dmg) if (a != b) worst = Math.max(worst, TierTrait.selfDamage(specs(a, b), 100, 0.0, 2.0, true));
        System.out.println("worst pair of self-damage traits at tierPowerScale 2.0 and 100% power: x" + worst);
        assertTrue(worst < 3.0, "two self bonuses at the top setting give x" + worst);
    }

    /** The numbers in the table of docs/bosses/tier_powers.md section 2 (seconds are ticks / 20): when a number is retuned, retune the doc line too. */
    @Test
    void theConstantsAreTheNumbersOfTheDocTable() {
        assertEquals(6.0, TierTrait.SLOW_RADIUS, EPS);
        assertEquals(9.0, TierTrait.SLOW_RADIUS_GATES, EPS);
        assertEquals(3 * 20, TierTrait.STEAM_FIRE_TICKS, "steam: fire for 3 s");
        assertEquals(2 * 20, TierTrait.STEAM_GAP_TICKS, "steam: once per 2 s");
        assertEquals(0.35, TierTrait.ARMOUR_CUT, EPS);
        assertEquals(0.35, TierTrait.UNTOUCHABLE_CUT, EPS);
        assertEquals(4 * 20, TierTrait.UNTOUCHABLE_TICKS, "untouchable: 4 s");
        assertEquals(6 * 20, TierTrait.BALANCE_WEAKEN_TICKS, "balance: weakened 6 s");
        assertEquals(15 * 20, TierTrait.MARK_TICKS, "soul marks: 15 s each");
        assertEquals(8 * 20, TierTrait.FRENZY_TICKS, "soul marks: 8 s of frenzy");
        assertEquals(1.10, TierTrait.FRENZY_DAMAGE, EPS);
        assertEquals(6 * 20, TierTrait.MIRROR_TICKS, "mirror: 6 s");
        assertEquals(0.25, TierTrait.MIRROR_SHARE, EPS);
        assertEquals(6.0, TierTrait.MIRROR_CAP_HP, EPS);
        assertEquals(2 * 20, TierTrait.LOCK_GAP_TICKS, "system lock: one effect per victim every 2 s");
        assertEquals(0.4, TierTrait.PULL_STRENGTH, EPS);
        assertEquals(1.15, TierTrait.FUSION_DAMAGE, EPS);
        assertEquals(0.10, TierTrait.FUSION_SPEED, EPS);
        assertArrayEquals(new double[] {0.66, 0.33}, TierTrait.FUSION_AT, EPS);
        assertEquals(14.0, TierTrait.SIGHT_REACH, EPS);
        assertEquals(2 * 20, TierTrait.SIGHT_TICKS, "sight: the outline lasts 2 s");
        assertEquals(15 * 20, TierTrait.KARMA_TICKS, "karma: 15 s");
        assertEquals(1.20, TierTrait.KARMA_DAMAGE, EPS);
        assertEquals(30, TierTrait.ECHO_DELAY_TICKS, "echo: after 1.5 s");
        assertEquals(0.08, TierTrait.CLIMB_DAMAGE, EPS);
        assertEquals(0.08, TierTrait.CLIMB_SPEED, EPS);
        assertEquals(5 * 20, TierTrait.SHIELD_TICKS, "shield break: 5 s");
        assertEquals(3 * 20, TierTrait.POISON_TICKS, "poison is short");
    }

    /**
     * The setting tierPowerScale goes up to 2.0 and multiplies strengths: ARMOUR_CUT * 2.0 = 0.70 is over MAX_CUT (0.6) and SLOW_RADIUS_GATES * 2.0 = 18 is over MAX_RADIUS (16), so the game
     * class that applies a trait must clamp with the caps, and the per-boss maps must be bounded by MAX_TRACKED. These guards only run once the files exist.
     */
    @Test
    void theGameSideClampsStrengthsToTheCapsAndBoundsItsMaps() throws Exception {
        Path traits = SRC.resolve("arc/tier/TierTraits.java"), state = SRC.resolve("arc/tier/TierState.java");
        if (Files.isRegularFile(traits)) {
            String src = Files.readString(traits);
            assertTrue(src.contains("MAX_CUT"), "TierTraits must clamp reductions with TierTrait.MAX_CUT (the scale can reach 2.0: 0.35 * 2.0 = 0.70)");
            assertTrue(src.contains("MAX_RADIUS"), "TierTraits must clamp radii with TierTrait.MAX_RADIUS (9 * 2.0 = 18 > 16)");
        }
        if (Files.isRegularFile(state)) assertTrue(Files.readString(state).contains("MAX_TRACKED"), "TierState must bound its maps with TierTrait.MAX_TRACKED");
    }

    // ---------------------------------------------------------------- (f) the REAL rosters (depend on the roster tuning of TierRosterF / TierRosterD)

    private static List<TierRow> sixteen() {
        List<TierRow> out = new ArrayList<>(TierRosterF.rows());
        out.addAll(TierRosterD.rows());
        return out;
    }

    @Test
    void theRealRosterIsTheSixteenPlusTheGagBoss() {
        assertEquals(6, TierRosterF.rows().size());
        assertEquals(10, TierRosterD.rows().size());
        assertEquals(sixteen().size() + TierRosterGag.rows().size(), TierRoster.all().size());
    }

    /** ROSTER-DEPENDENT: every row's traits and hit sizes (traits are added to the rows by the roster tuning). */
    @Test
    void everyRealRowPassesTheBalanceRulesAndTheHouseRules() {
        List<String> bad = new ArrayList<>();                                      // every row is checked so one run shows all the problems at once
        for (TierRow row : TierRoster.all()) {
            for (String p : TierBalance.problems(row)) bad.add("TierBalance: " + p);
            for (String p : TierRules.problems(row)) bad.add("TierRules: " + p);
        }
        assertEquals(List.of(), bad);
    }

    /**
     * ROSTER-DEPENDENT: the +-25% band of the sixteen bosses of tiers F and D. Light Yagami (the gag boss, 28 health, a pen for a weapon) is deliberately not in the band: he is rank F by the ladder
     * but not a fight, so he would sit far under the F mean (see {@link #theGagBossIsOutsideTheBandOnPurpose}).
     */
    @Test
    void theSixteenLieInsideTheirTierBand() {
        List<TierRow> all = sixteen();
        System.out.println("tier pressure of the sixteen (expected damage per second, 50% hit rate):");
        System.out.println(TierBalance.table(all));
        for (TierLetter l : new TierLetter[] {TierLetter.F, TierLetter.D})
            System.out.printf(java.util.Locale.ROOT, "mean pressure at %d%% power, tier %s: %.2f%n", TierBalance.REF_POWER, l.label(), TierBalance.mean(all, l, TierBalance.REF_POWER));
        assertEquals(List.of(), TierBalance.bandProblems(all));
    }

    /** The gag boss is rank F by the ladder but is not a fight: counting him would drag the F mean down and flag the real F bosses, which is why the band is taken over the sixteen only. */
    @Test
    void theGagBossIsNotPartOfTheBandAndGetsNoPowers() {
        System.out.println(TierBalance.table(TierRoster.all()));
        List<TierRow> withLight = new ArrayList<>(sixteen());
        withLight.addAll(TierRosterGag.rows());
        List<String> bad = TierBalance.bandProblems(withLight);
        boolean lightFlagged = false;
        for (String s : bad) if (s.startsWith(TierRosterGag.LIGHT)) lightFlagged = true;
        assertTrue(lightFlagged, "the gag boss would be flagged under the F band if he were counted: " + bad);
        TierRow light = TierRosterGag.rows().get(0);
        assertTrue(TierBalance.pressure(light, TierBalance.REF_POWER) < 1.0, "the gag boss is not a fight");
        assertTrue(light.traits().isEmpty(), "the gag boss gets no powers (tier_powers.md: Light Yagami is not touched)");
    }

    /** ROSTER-DEPENDENT: tier_powers.md section 4 says all 16 fit their budget: F 2-3 of 5, D 4-7 of 7. */
    @Test
    void everyRealBossSpendsAtLeastOneTraitAndStaysInItsBudget() {
        for (TierRow row : sixteen()) {
            assertFalse(row.traits().isEmpty(), row.id() + " has no trait");
            assertTrue(row.traitPoints() >= 1 && row.traitPoints() <= row.letter().traitBudget(), row.id() + " spends " + row.traitPoints() + " of " + row.letter().traitBudget());
            assertTrue(row.traits().size() <= TierBalance.MAX_TRAITS, row.id());
        }
    }

    /** ROSTER-DEPENDENT: no real move is bigger than the caps, and the biggest ordinary / ultimate hits are printed so a retune can see the room left. */
    @Test
    void theBiggestRealHitsStayUnderTheCaps() {
        double maxOrdinary = 0, maxUltimate = 0;
        for (TierRow row : sixteen())
            for (TierMove m : row.moves()) {
                if (m.ultimate()) maxUltimate = Math.max(maxUltimate, m.damage()); else maxOrdinary = Math.max(maxOrdinary, m.damage());
            }
        System.out.println("biggest ordinary hit " + maxOrdinary + "x (cap " + TierBalance.MAX_ORDINARY + "x), biggest ultimate " + maxUltimate + "x (cap " + TierBalance.MAX_ULTIMATE + "x)");
        assertTrue(maxOrdinary <= TierBalance.MAX_ORDINARY);
        assertTrue(maxUltimate <= TierBalance.MAX_ULTIMATE);
    }
}
