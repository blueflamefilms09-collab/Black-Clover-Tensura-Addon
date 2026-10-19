package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** What a Half Equip blow is worth: the share and cap each special reads off the target and the bearer, the player clamp, and the blows-in-a-row table. */
class DjinnTraitRulesTest {
    private static TraitRules.Facts facts(boolean undead, int armor, double health, boolean invisible, int ailments, double speed, int streak) {
        return new TraitRules.Facts(undead, armor, health, invisible, ailments, speed, streak);
    }

    private static final TraitRules.Facts PLAIN = facts(false, 0, 1.0, false, 0, 0.0, 0);

    @Test
    void aSpecialThatReadsNothingKeepsTheRowsShare() {
        for (Djinn.Special s : Djinn.Special.values()) {
            switch (s) {
                case SMITE, CRUSH, HUNT, TRAMPLE, REVEAL, CARRION, BLOODLUST, DRUMBEAT -> { }
                default -> {
                    assertEquals(1.0, TraitRules.fracFactor(s, PLAIN), 1e-12, s.name());
                    assertEquals(1.0, TraitRules.capFactor(s, PLAIN), 1e-12, s.name());
                }
            }
        }
    }

    @Test
    void theOriginalSmiteAndCrushAreExactlyWhatTheyWere() {
        assertEquals(2.0, TraitRules.fracFactor(Djinn.Special.SMITE, facts(true, 0, 1, false, 0, 0, 0)), 1e-12);
        assertEquals(2.0, TraitRules.capFactor(Djinn.Special.SMITE, facts(true, 0, 1, false, 0, 0, 0)), 1e-12, "smite doubled the cap too");
        assertEquals(1.0, TraitRules.fracFactor(Djinn.Special.SMITE, PLAIN), 1e-12);
        assertEquals(1.0 + 10.0 / 20.0, TraitRules.fracFactor(Djinn.Special.CRUSH, facts(false, 10, 1, false, 0, 0, 0)), 1e-12);
        assertEquals(2.0, TraitRules.fracFactor(Djinn.Special.CRUSH, facts(false, 30, 1, false, 0, 0, 0)), 1e-12, "armour counts to 20");
        assertEquals(1.0, TraitRules.capFactor(Djinn.Special.CRUSH, facts(false, 30, 1, false, 0, 0, 0)), 1e-12, "crush raised the share only");
    }

    @Test
    void huntReadsTheTargetsHealth() {
        assertEquals(1.5, TraitRules.fracFactor(Djinn.Special.HUNT, facts(false, 0, 0.49, false, 0, 0, 0)), 1e-12);
        assertEquals(1.0, TraitRules.fracFactor(Djinn.Special.HUNT, facts(false, 0, 0.5, false, 0, 0, 0)), 1e-12);
        assertEquals(1.0, TraitRules.fracFactor(Djinn.Special.HUNT, PLAIN), 1e-12);
    }

    @Test
    void trampleReadsTheBearersSpeed() {
        double stand = TraitRules.fracFactor(Djinn.Special.TRAMPLE, facts(false, 0, 1, false, 0, 0.0, 0));
        double walk = TraitRules.fracFactor(Djinn.Special.TRAMPLE, facts(false, 0, 1, false, 0, 0.2, 0));
        double sprint = TraitRules.fracFactor(Djinn.Special.TRAMPLE, facts(false, 0, 1, false, 0, 0.28, 0));
        double absurd = TraitRules.fracFactor(Djinn.Special.TRAMPLE, facts(false, 0, 1, false, 0, 9.0, 0));
        assertEquals(1.0, stand, 1e-12);
        assertTrue(walk > stand && sprint > walk, "faster is harder");
        assertEquals(1.8, absurd, 1e-12, "capped at +80 percent");
    }

    @Test
    void revealAndCarrionReadWhatTheTargetIs() {
        assertEquals(1.5, TraitRules.fracFactor(Djinn.Special.REVEAL, facts(false, 0, 1, true, 0, 0, 0)), 1e-12);
        assertEquals(1.0, TraitRules.fracFactor(Djinn.Special.REVEAL, PLAIN), 1e-12);
        assertEquals(1.5, TraitRules.fracFactor(Djinn.Special.CARRION, facts(false, 0, 1, false, 1, 0, 0)), 1e-12);
        assertEquals(1.5, TraitRules.fracFactor(Djinn.Special.CARRION, facts(false, 0, 1, false, 5, 0, 0)), 1e-12, "half again, not more, however many ailments");
        assertEquals(1.0, TraitRules.fracFactor(Djinn.Special.CARRION, PLAIN), 1e-12);
    }

    @Test
    void bloodlustClimbsToFortyPercentAndDrumbeatIsEveryThird() {
        double prev = 0;
        for (int streak = 0; streak < 10; streak++) {
            double f = TraitRules.fracFactor(Djinn.Special.BLOODLUST, facts(false, 0, 1, false, 0, 0, streak));
            assertTrue(f >= prev, "it never falls inside a streak");
            assertTrue(f <= 1.4 + 1e-12, "forty percent at the most");
            prev = f;
        }
        assertEquals(1.0, TraitRules.fracFactor(Djinn.Special.BLOODLUST, facts(false, 0, 1, false, 0, 0, 0)), 1e-12);
        assertEquals(1.4, TraitRules.fracFactor(Djinn.Special.BLOODLUST, facts(false, 0, 1, false, 0, 0, 4)), 1e-12);
        for (int streak = 0; streak < 12; streak++) {
            boolean third = streak % 3 == 2;
            assertEquals(third, TraitRules.drumbeat(streak), "blow " + streak);
            assertEquals(third ? 2.0 : 1.0, TraitRules.fracFactor(Djinn.Special.DRUMBEAT, facts(false, 0, 1, false, 0, 0, streak)), 1e-12);
        }
        assertFalse(TraitRules.drumbeat(-1));
    }

    @Test
    void noSpecialExceedsTheCeilingOnTheShare() {
        for (Djinn d : Djinn.values()) {
            double worst = 0;
            TraitRules.Facts[] cases = {PLAIN, facts(true, 40, 0.1, true, 9, 9.0, 4), facts(true, 40, 0.1, true, 9, 9.0, 2)};
            for (TraitRules.Facts f : cases) worst = Math.max(worst, TraitRules.fracFactor(d.trait().special(), f));
            assertTrue(worst <= TraitRules.MAX_FACTOR + 1e-12, d.name() + " factor " + worst);
        }
    }

    @Test
    void aRiderOnAPlayerIsNeverMoreThanSixWhatEverTheCapSays() {
        for (Djinn d : Djinn.values()) {
            for (double dark : new double[]{1.0, 1.5}) {                      // a dark Djinn's cap x1.5
                for (float blow : new float[]{1f, 6f, 20f, 60f, 200f}) {
                    float onPlayer = TraitRules.rider(blow, d.trait().bonusFrac() * dark, d.trait().bonusCap() * dark, true);
                    assertTrue(onPlayer <= TraitRules.PLAYER_RIDER_CAP, d.name() + " blow " + blow);
                    float onMob = TraitRules.rider(blow, d.trait().bonusFrac() * dark, d.trait().bonusCap() * dark, false);
                    assertTrue(onMob <= d.trait().bonusCap() * dark + 1e-4, d.name());
                    assertTrue(onPlayer <= onMob + 1e-6);
                }
            }
        }
        assertEquals(0f, TraitRules.rider(-5f, 0.3, 7.5, false), 0f);
        assertEquals(3.0f, TraitRules.rider(10f, 0.3, 7.5, true), 1e-6f);
        assertEquals(7.5f, TraitRules.rider(100f, 0.3, 7.5, false), 1e-6f);
        assertEquals(6.0f, TraitRules.rider(100f, 0.3, 7.5, true), 1e-6f);
    }

    // ---------------------------------------------------------------- the blows in a row
    @Test
    void aStreakCountsBlowsThatComeSoonAfterEachOther() {
        BlowStreak s = new BlowStreak();
        UUID a = UUID.nameUUIDFromBytes("a".getBytes()), b = UUID.nameUUIDFromBytes("b".getBytes());
        assertEquals(0, s.hit(a, 100));
        assertEquals(1, s.hit(a, 120));
        assertEquals(2, s.hit(a, 120 + TraitRules.STREAK_WINDOW));
        assertEquals(0, s.hit(a, 120 + 2 * TraitRules.STREAK_WINDOW + 5), "too long a pause starts over");
        assertEquals(0, s.hit(b, 130), "another bearer has a streak of their own");
        assertEquals(0, s.hit(a, 10), "a clock that went backwards starts over");
        s.forget(a);
        assertEquals(0, s.hit(a, 11));
        assertEquals(2, s.size());
        s.clear();
        assertEquals(0, s.size());
    }

    @Test
    void theStreakTableNeverGrowsWithoutEnd() {
        BlowStreak s = new BlowStreak();
        for (int i = 0; i < 5000; i++) {
            s.hit(new UUID(i, i * 31L), i);
            assertTrue(s.size() <= BlowStreak.MAX, "size " + s.size());
        }
        for (int i = 0; i < 1000; i++) {                                    // all fresh at once: the table is cleared rather than grown
            s.hit(new UUID(77, i), 100000);
            assertTrue(s.size() <= BlowStreak.MAX);
        }
    }
}
