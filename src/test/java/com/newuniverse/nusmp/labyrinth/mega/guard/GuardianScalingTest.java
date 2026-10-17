package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling.Trait;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling.TraitSpec;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Labyrinth Guardian's health, damage, armour and size are formulas of the sector's threat and its trait is chosen by the sector's theme, so a
 * curve that overflows, a size that outgrows its cap, a theme without a trait of its own, a second phase that weakens a trait or a lost core that can
 * never be forged again would each be found by a player first; this test pins the curves, the caps, the theme-to-trait table and the re-forge rule of
 * {@link GuardianScaling} with no game running.
 */
class GuardianScalingTest {
    private static final double B = GuardianScaling.BASE_HEALTH, P = GuardianScaling.HEALTH_PER_THREAT;

    @Test
    void healthFollowsTheFormulaUntilTheAttributeCeiling() {
        assertEquals(400.0, GuardianScaling.health(B, P, 1), 1e-9);
        assertEquals(400.0 * 1.35, GuardianScaling.health(B, P, 2), 1e-9);
        assertEquals(400.0 * (1 + 0.35 * 4), GuardianScaling.health(B, P, 5), 1e-9);
        // threat 8: the formula asks for 1380, the attribute stops at 1024, the rest becomes a damage divisor
        assertEquals(1380.0, GuardianScaling.rawHealth(B, P, 8), 1e-9);
        assertEquals(1024.0, GuardianScaling.health(B, P, 8), 1e-9);
        assertEquals(1380.0 / 1024.0, GuardianScaling.damageDivisor(B, P, 8), 1e-9);
        assertEquals(1.0, GuardianScaling.damageDivisor(B, P, 3), 1e-9, "below the ceiling nothing is divided");
        double last = 0;
        for (int t = 1; t <= 60; t++) {
            double effective = GuardianScaling.health(B, P, t) * GuardianScaling.damageDivisor(B, P, t);
            assertTrue(effective >= last, "effective health never falls with threat (threat " + t + ")");
            assertEquals(GuardianScaling.rawHealth(B, P, t), effective, 1e-6, "ceiling times divisor is the formula again (threat " + t + ")");
            last = effective;
        }
    }

    @Test
    void aBadThreatIsClampedNotTrusted() {
        assertEquals(GuardianScaling.health(B, P, 1), GuardianScaling.health(B, P, 0), 1e-9);
        assertEquals(GuardianScaling.health(B, P, 1), GuardianScaling.health(B, P, -7), 1e-9);
        double huge = GuardianScaling.rawHealth(B, P, Integer.MAX_VALUE);
        assertTrue(huge > 0 && !Double.isInfinite(huge) && !Double.isNaN(huge), "no overflow: " + huge);
        assertEquals(1.0, GuardianScaling.rawHealth(0.0, P, 5), 1e-9, "never below 1 health");
        assertEquals(400.0, GuardianScaling.rawHealth(B, -3.0, 9), 1e-9, "a negative rate does not make it weaker");
    }

    @Test
    void damageAndArmourGrowSlowerThanHealth() {
        double d = GuardianScaling.DAMAGE_PER_THREAT;
        assertEquals(14.0, GuardianScaling.attackDamage(d, 1), 1e-9);
        assertEquals(14.0 * (1 + 0.12 * 9), GuardianScaling.attackDamage(d, 10), 1e-9);
        int t = 10;
        double healthRatio = GuardianScaling.rawHealth(B, P, t) / B;
        double damageRatio = GuardianScaling.attackDamage(d, t) / GuardianScaling.attackDamage(d, 1);
        double armourRatio = GuardianScaling.armor(t) / GuardianScaling.armor(1);
        assertTrue(healthRatio > damageRatio, "health outgrows damage: " + healthRatio + " vs " + damageRatio);
        assertTrue(damageRatio > armourRatio, "damage outgrows armour: " + damageRatio + " vs " + armourRatio);
        assertEquals(GuardianScaling.MAX_DAMAGE, GuardianScaling.attackDamage(5.0, 500), 1e-9);
        assertEquals(12.0, GuardianScaling.armor(1), 1e-9);
        assertEquals(GuardianScaling.MAX_ARMOR, GuardianScaling.armor(1000), 1e-9);
    }

    @Test
    void theGuardianIsColossalFromTheFirstSectorAndGrowsALittle() {
        double base = GuardianScaling.BASE_SCALE;
        assertEquals(1.8, base, 1e-12, "the brief's colossal boss: 1.8 times a Floor Warden");
        assertEquals(base, GuardianScaling.scale(base, 1), 1e-9, "threat 1 is the base size");
        assertEquals(base * (1.0 + 0.02 * 9), GuardianScaling.scale(base, 10), 1e-9, "2 % of the base more per threat");
        assertEquals(base * GuardianScaling.MAX_SCALE_GROWTH, GuardianScaling.scale(base, 500), 1e-9, "growth stops at a quarter more");
        double last = 0;
        for (int t = 1; t <= 80; t++) {
            double sc = GuardianScaling.scale(base, t);
            assertTrue(sc >= last, "size never shrinks with threat (threat " + t + ")");
            assertTrue(sc >= base && sc <= base * 1.25 + 1e-9, "size stays within a quarter of the base (threat " + t + "): " + sc);
            last = sc;
        }
        assertEquals(GuardianScaling.scale(base, 1), GuardianScaling.scale(base, -5), 1e-9, "a bad threat is clamped");
        // the config range is a hard fence (0.5 to 4): a hand-edited file can neither shrink the boss below half a Warden nor grow it past 4
        assertEquals(0.5, GuardianScaling.MIN_BASE_SCALE, 1e-12, "the config range starts at half a Warden");
        assertEquals(4.0, GuardianScaling.MAX_SCALE, 1e-12, "and ends at four times one");
        assertEquals(0.5, GuardianScaling.scale(0.2, 1), 1e-9);
        assertEquals(0.5, GuardianScaling.scale(0.5, 1), 1e-9, "half a Warden is a legal setting");
        assertEquals(1.0, GuardianScaling.scale(1.0, 1), 1e-9, "a plain Warden's size is a legal setting");
        assertEquals(GuardianScaling.MAX_SCALE, GuardianScaling.scale(9.0, 1), 1e-9);
        assertEquals(GuardianScaling.MAX_SCALE, GuardianScaling.scale(GuardianScaling.MAX_SCALE, 500), 1e-9, "even the largest base never passes the ceiling");
        assertEquals(base, GuardianScaling.scale(Double.NaN, 1), 1e-9, "a NaN reads as the default");
        assertTrue(GuardianScaling.scale(Double.POSITIVE_INFINITY, 1) <= GuardianScaling.MAX_SCALE);
        assertTrue(GuardianScaling.scale(Double.NEGATIVE_INFINITY, 1) >= GuardianScaling.MIN_BASE_SCALE);
    }

    @Test
    void theSecondPhaseIsStrictlyUnderHalfHealth() {
        assertFalse(GuardianScaling.phaseTwo(200.0, 400.0), "exactly half is still the first phase");
        assertTrue(GuardianScaling.phaseTwo(199.9, 400.0));
        assertFalse(GuardianScaling.phaseTwo(400.0, 400.0));
        assertFalse(GuardianScaling.phaseTwo(0.0, 0.0), "no max health, no phase");
        assertTrue(GuardianScaling.phaseTwo(0.0, 400.0));
    }

    /** The themes that borrow a trait: the two roads of 1.55.0 and the themes of 1.59.0. */
    private static final java.util.Map<String, Trait> BORROWED = java.util.Map.ofEntries(
            java.util.Map.entry("radiant", Trait.ASTRAL), java.util.Map.entry("abyss", Trait.GRAVITY),
            java.util.Map.entry("feywild_grove", Trait.VERDANT), java.util.Map.entry("dwarven_forgehold", Trait.FLAME),
            java.util.Map.entry("crystal_geode", Trait.ASTRAL), java.util.Map.entry("sunken_temple", Trait.FROST),
            java.util.Map.entry("holy_church", Trait.ASTRAL), java.util.Map.entry("celestial_court", Trait.ASTRAL),
            java.util.Map.entry("sky_citadel", Trait.STORM), java.util.Map.entry("mountain_monastery", Trait.FROST),
            java.util.Map.entry("cursed_archive", Trait.VERDANT), java.util.Map.entry("necropolis", Trait.FROST),
            java.util.Map.entry("vampire_castle", Trait.BLOOD), java.util.Map.entry("haunted_manor", Trait.SHADOW));

    @Test
    void everyThemeHasItsOwnTrait() {
        Set<Trait> seen = new HashSet<>();
        for (RegionTheme th : RegionTheme.values()) {
            Trait t = GuardianScaling.traitOf(th);
            Trait borrowed = BORROWED.get(th.id());                                    // by id: the same test passes with and without the later themes in the enum
            if (borrowed != null) assertEquals(borrowed, t, th.id() + " borrows its trait");
            else assertEquals(th.name(), t.name(), "the nine original themes carry their own name as the trait");
            seen.add(t);
        }
        assertEquals(9, seen.size(), "every one of the nine traits is some theme's trait");
        assertEquals(Trait.GRAVITY, GuardianScaling.traitOf(null));
        assertEquals(9, Trait.values().length, "nine traits: a new trait needs a table row and a test");
    }

    @Test
    void everyTraitStaysInsideItsHardCapsInBothPhases() {
        for (Trait t : Trait.values()) {
            for (boolean phaseTwo : new boolean[]{false, true}) {
                TraitSpec s = GuardianScaling.spec(t, phaseTwo);
                String at = t + (phaseTwo ? " phase 2" : " phase 1");
                assertTrue(s.cooldown() == 0 || s.cooldown() >= GuardianScaling.MIN_ACTIVE_COOLDOWN, at + ": cooldown " + s.cooldown());
                assertTrue(s.power() >= 0 && s.power() <= GuardianScaling.MAX_POWER, at + ": power " + s.power());
                assertTrue(s.range() >= 0 && s.duration() >= 0 && s.count() >= 0, at + ": no negative numbers");
                if (t == Trait.SHADOW) {
                    assertTrue(s.duration() <= GuardianScaling.SHADOW_MAX_TICKS && s.duration() <= 40, at + ": blindness two seconds at most, was " + s.duration());
                    assertTrue(s.range() <= 6.0, at + ": only within 6 blocks, was " + s.range());
                }
                if (t == Trait.VERDANT) assertTrue(s.duration() <= 20, at + ": a root lasts one second at most, was " + s.duration());
                if (t == Trait.SPATIAL) assertTrue(s.cooldown() >= GuardianScaling.BLINK_MIN_COOLDOWN && s.range() <= GuardianScaling.BLINK_MAX_RANGE, at + ": a short blink, not too often");
            }
        }
        assertEquals(160, GuardianScaling.spec(Trait.SPATIAL, false).cooldown(), "the brief's 8 second blink");
    }

    @Test
    void theSecondPhaseNeverWeakensATrait() {
        for (Trait t : Trait.values()) {
            TraitSpec a = GuardianScaling.spec(t, false), b = GuardianScaling.spec(t, true);
            assertTrue(b.power() >= a.power(), t + ": power");
            assertTrue(b.range() >= a.range(), t + ": range");
            assertTrue(b.count() >= a.count(), t + ": count");
            if (a.cooldown() > 0) assertTrue(b.cooldown() <= a.cooldown(), t + ": cooldown");
            assertTrue(b.duration() >= a.duration(), t + ": duration");
        }
        assertTrue(GuardianScaling.spec(Trait.STORM, true).count() > GuardianScaling.spec(Trait.STORM, false).count(), "more hops in the second phase");
        assertTrue(GuardianScaling.spec(Trait.ASTRAL, true).count() > GuardianScaling.spec(Trait.ASTRAL, false).count(), "more stars in the second phase");
    }

    @Test
    void themeColoursAreDistinctRgb() {
        Set<Integer> seen = new HashSet<>();
        for (RegionTheme th : RegionTheme.values()) {
            int c = GuardianScaling.themeColor(th);
            assertTrue(c >= 0 && c <= 0xFFFFFF, th + " is a 24 bit colour");
            assertTrue(seen.add(c), th + " has its own colour");
        }
        assertTrue(GuardianScaling.themeColor(null) >= 0);
    }

    @Test
    void aLostCoreMayBeForgedAgainOnlyWhenNothingElseHoldsTheSeal() {
        long now = 100_000L;
        long fiveMinutes = 5L * 60 * 20;
        // everything in order: spent altar, sealed door, no core, wait over
        assertTrue(GuardianScaling.canReforge(true, true, false, now - fiveMinutes, now, 5));
        assertFalse(GuardianScaling.canReforge(true, true, false, now - fiveMinutes + 1, now, 5), "one tick early");
        assertFalse(GuardianScaling.canReforge(false, true, false, -1, now, 5), "the altar is not spent: the Guardian lives");
        assertFalse(GuardianScaling.canReforge(true, false, false, -1, now, 5), "the door was opened already: nothing to forge for");
        assertFalse(GuardianScaling.canReforge(true, true, true, -1, now, 5), "a core exists: never two");
        assertTrue(GuardianScaling.canReforge(true, true, false, -1, now, 5), "it never forged: ready");
        assertEquals(0L, GuardianScaling.reforgeWaitTicks(now - 10 * fiveMinutes, now, 5));
        assertEquals(fiveMinutes, GuardianScaling.reforgeWaitTicks(now, now, 5));
        assertEquals(300L, GuardianScaling.reforgeWaitTicks(now - fiveMinutes + 300, now, 5));
        assertEquals(0L, GuardianScaling.reforgeWaitTicks(now, now, 0), "a zero wait is ready at once");
        assertEquals(0L, GuardianScaling.reforgeWaitTicks(now, now, -4), "a negative wait is read as zero");
    }
}
