package com.newuniverse.nusmp.relics.djinn.dark;

import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.relics.djinn.dark.DarkRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Dark Djinn's numbers: what the black rukh adds, what it costs, and when it can be taken, refused and cleansed. */
class DarkRulesTest {
    // ---------------------------------------------------------------- the drink
    private static Drink ok() { return new Drink(true, true, false, 0, false, true, 50, 0, false, true, false); }

    private static Drink with(Drink d, String what) {
        return switch (what) {
            case "sneak" -> new Drink(false, d.owns(), d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "owner" -> new Drink(d.sneaking(), false, d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "lent" -> new Drink(d.sneaking(), false, true, d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "house" -> new Drink(d.sneaking(), d.owns(), d.lent(), 1, d.dark(), d.vesselInHand(), d.trust(), d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "dark" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), true, d.vesselInHand(), d.trust(), d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "vessel" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), d.dark(), false, d.trust(), d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "trust" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), 49, d.phase(), d.collapse(), d.hasShard(), d.creative());
            case "phase" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), 2, d.collapse(), d.hasShard(), d.creative());
            case "collapse" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), d.phase(), true, d.hasShard(), d.creative());
            case "shard" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), d.phase(), d.collapse(), false, d.creative());
            case "creative" -> new Drink(d.sneaking(), d.owns(), d.lent(), d.householdMembers(), d.dark(), d.vesselInHand(), d.trust(), d.phase(), d.collapse(), d.hasShard(), true);
            default -> throw new IllegalArgumentException(what);
        };
    }

    @Test
    void anOwnerWithTrustFiftyShardAndVesselMayDrink() {
        assertEquals(Verdict.OK, drink(ok()));
        assertEquals(50, DRINK_TRUST);
    }

    @Test
    void everyConditionRefusesWithItsOwnReason() {
        assertEquals(Verdict.NOT_SNEAKING, drink(with(ok(), "sneak")));
        assertEquals(Verdict.NOT_OWNER, drink(with(ok(), "owner")));
        assertEquals(Verdict.HOUSEHOLD_LENT, drink(with(ok(), "lent")), "a household member never gets a dark Djinn");
        assertEquals(Verdict.HOUSEHOLD_MEMBERS, drink(with(ok(), "house")), "a dark Djinn cannot be lent");
        assertEquals(Verdict.ALREADY_DARK, drink(with(ok(), "dark")));
        assertEquals(Verdict.NO_VESSEL, drink(with(ok(), "vessel")));
        assertEquals(Verdict.NEED_TRUST, drink(with(ok(), "trust")));
        assertEquals(Verdict.PHASE_ON, drink(with(ok(), "phase")));
        assertEquals(Verdict.COLLAPSE, drink(with(ok(), "collapse")));
        assertEquals(Verdict.NO_SHARD, drink(with(ok(), "shard")));
    }

    @Test
    void trustFortyNineIsNotEnoughAndFiftyIs() {
        assertEquals(Verdict.NEED_TRUST, drink(with(ok(), "trust")));
        assertEquals(Verdict.OK, drink(ok()));
    }

    @Test
    void creativePlayersSkipTrustAndCollapseButNotTheShardOrTheVessel() {
        assertEquals(Verdict.OK, drink(with(with(ok(), "trust"), "creative")));
        assertEquals(Verdict.OK, drink(with(with(ok(), "collapse"), "creative")));
        assertEquals(Verdict.NO_SHARD, drink(with(with(ok(), "shard"), "creative")));
        assertEquals(Verdict.NO_VESSEL, drink(with(with(ok(), "vessel"), "creative")));
    }

    // ---------------------------------------------------------------- the unforced fall
    @Test
    void theFallNeedsAShardAFullEquipAndNinetyPercentAssimilation() {
        double lucky = 0.0;                                           // the lowest roll always wins when everything else holds
        assertTrue(falls(lucky, true, false, false, 3, 90f, true, false));
        assertFalse(falls(lucky, true, false, false, 3, 89.9f, true, false), "below 90%");
        assertFalse(falls(lucky, true, false, false, 2, 95f, true, false), "only in a Full Equip");
        assertFalse(falls(lucky, true, false, false, 3, 95f, false, false), "never without a shard in the pack");
        assertFalse(falls(lucky, true, false, true, 3, 95f, true, false), "never twice");
        assertFalse(falls(lucky, true, true, false, 3, 95f, true, false), "never for a household member");
        assertFalse(falls(lucky, false, false, false, 3, 95f, true, false), "never for a player with no Djinn");
        assertFalse(falls(lucky, true, false, false, 3, 95f, true, true), "never in a collapse");
    }

    @Test
    void theFallChanceAddsUpToTenPercentAMinute() {
        double perSecond = fallPerSecond();
        assertTrue(perSecond > 0.0 && perSecond < 0.01);
        assertEquals(1.0 - FALL_PER_MINUTE, Math.pow(1.0 - perSecond, 60), 1.0e-12);
        assertFalse(falls(perSecond, true, false, false, 3, 100f, true, false), "a roll at the threshold does not win");
        assertTrue(falls(perSecond * 0.99, true, false, false, 3, 100f, true, false));
    }

    // ---------------------------------------------------------------- Half and Full Equip
    @Test
    void theRiderGrowsBy35PercentAndItsCapGoesFromSixToNine() {
        assertEquals(0.25, riderFrac(0.25, false));
        assertEquals(0.3375, riderFrac(0.25, true), 1.0e-12);
        assertEquals(6.0, riderCap(6.0, false));
        assertEquals(9.0, riderCap(6.0, true), 1.0e-12);
        assertEquals(7.5, riderCap(5.0, true), 1.0e-12);
    }

    @Test
    void masoGainsTenPercentSpeedTwoAttackAndHalfAnElementBoost() {
        assertEquals(0.30, masoSpeed(DjinnPhaseRules.MASO_SPEED, false), 1.0e-12);
        assertEquals(0.40, masoSpeed(DjinnPhaseRules.MASO_SPEED, true), 1.0e-12);
        assertEquals(5.0, masoAttack(DjinnPhaseRules.MASO_ATTACK, false));
        assertEquals(7.0, masoAttack(DjinnPhaseRules.MASO_ATTACK, true));
        assertEquals(0.30, masoElement(DjinnPhaseRules.MASO_ELEMENT, false), 1.0e-12);
        assertEquals(0.50, masoElement(DjinnPhaseRules.MASO_ELEMENT, true), 1.0e-12);
        assertEquals(3.0, masoElementFlat(3.0, DjinnPhaseRules.MASO_ELEMENT, false));
        assertEquals(5.0, masoElementFlat(3.0, DjinnPhaseRules.MASO_ELEMENT, true), 1.0e-9, "a Strength Djinn's flat attack scales like the boost");
        assertEquals(10.0, masoElementFlat(6.0, DjinnPhaseRules.MASO_ELEMENT, true), 1.0e-9);
    }

    // ---------------------------------------------------------------- Extreme Magic
    @Test
    void extremeMagicHitsAQuarterHarderAndCostsAFifthLess() {
        assertEquals(26.0, extremeDamage(26.0, false));
        assertEquals(32.5, extremeDamage(26.0, true), 1.0e-12);
        assertEquals(0.60, extremePrice(DjinnPhaseRules.entryFraction(DjinnPhaseRules.EXTREME), false), 1.0e-12);
        assertEquals(0.48, extremePrice(DjinnPhaseRules.entryFraction(DjinnPhaseRules.EXTREME), true), 1.0e-12);
        assertTrue(extremePrice(0.60, true) < DjinnPhaseRules.requiredFraction(DjinnPhaseRules.EXTREME), "the bar still has to be 60% full to begin it");
    }

    // ---------------------------------------------------------------- the price
    @Test
    void upkeepIsThirtyPercentMoreAndAssimilationRisesTwiceAsFast() {
        assertEquals(0.007, upkeep(0.007, false));
        assertEquals(0.0091, upkeep(0.007, true), 1.0e-12);
        assertEquals(0.06f, assimilationRate(0.06f, false));
        assertEquals(0.12f, assimilationRate(0.06f, true), 1.0e-6f);
        assertEquals(4.0f, assimilationRate(DjinnPhaseRules.EXTREME_ASSIMILATION, true), 1.0e-6f);
    }

    @Test
    void trustCannotRiseAboveSixtyButMayFallAndKeepsWhatItAlreadyHad() {
        assertEquals(55, trustAfter(54, 55, true));
        assertEquals(60, trustAfter(60, 61, true), "a dark Djinn's trust stops at 60");
        assertEquals(60, trustAfter(58, 70, true), "a big jump is cut at the cap");
        assertEquals(40, trustAfter(60, 40, true), "it may fall freely");
        assertEquals(90, trustAfter(90, 90, true), "a trust above the cap from before is left alone");
        assertEquals(90, trustAfter(90, 95, true), "but does not rise");
        assertEquals(95, trustAfter(90, 95, false), "a plain Djinn is not capped");
        assertEquals(100, trustAfter(60, 100, false));
    }

    @Test
    void theExtremeGateOpensAtTheTrustCapForADarkDjinnOnly() {
        assertEquals(70, gateTrust(60, DjinnPhaseRules.EXTREME_TRUST, true), "60 is counted as 70");
        assertEquals(59, gateTrust(59, DjinnPhaseRules.EXTREME_TRUST, true));
        assertEquals(60, gateTrust(60, DjinnPhaseRules.EXTREME_TRUST, false), "a plain Djinn still needs 70");
        assertEquals(88, gateTrust(88, DjinnPhaseRules.EXTREME_TRUST, true), "more trust is never turned into less");
        assertEquals(60, extremeTrustNeeded(DjinnPhaseRules.EXTREME_TRUST, true));
        assertEquals(70, extremeTrustNeeded(DjinnPhaseRules.EXTREME_TRUST, false));
        assertEquals(DjinnPhaseRules.Verdict.OK, DjinnPhaseRules.request(extremeAt(gateTrust(60, 70, true)), 4));
        assertEquals(DjinnPhaseRules.Verdict.NEED_TRUST, DjinnPhaseRules.request(extremeAt(gateTrust(59, 70, true)), 4));
    }

    private static DjinnPhaseRules.Snapshot extremeAt(int trust) {
        return new DjinnPhaseRules.Snapshot(DjinnPhaseRules.FULL, true, false, true, trust, 10f, 1.0, 1000L, 0L, 0L, 0L, false);
    }

    @Test
    void theBacklashIsFifteenPointsHeavierOnlyWhenTheEndWasDeliberate() {
        assertEquals(0.45, backlash(0.30, true, true), 1.0e-12);
        assertEquals(0.15, backlash(0.0, true, true), 1.0e-12, "even a clean early exit leaves a mild mark");
        assertEquals(1.0, backlash(0.95, true, true), 1.0e-12, "never past 1");
        assertEquals(0.30, backlash(0.30, false, true), 1.0e-12);
        assertEquals(0.0, backlash(0.0, true, false), 1.0e-12, "death, logout, an admin or a lost vessel stay free");
        DjinnPhaseRules.Backlash plain = DjinnPhaseRules.backlash(0.30), dark = DjinnPhaseRules.backlash(backlash(0.30, true, true));
        assertTrue(dark.weakTicks() > plain.weakTicks() && dark.hungerTicks() > plain.hungerTicks());
    }

    @Test
    void theElementalCalmIsHalved() {
        assertEquals(0.8, calm(0.8, false));
        assertEquals(0.4, calm(0.8, true), 1.0e-12);
        assertEquals(0.5, calm(1.0, true), 1.0e-12);
    }

    // ---------------------------------------------------------------- skills, aura
    @Test
    void theDarkSkillsComeInWithTheEquipAndAreVerifiedTensuraIds() {
        assertTrue(skills(1).isEmpty());
        assertEquals(java.util.List.of("tensura:darkness_attack_resistance"), skills(2));
        assertEquals(3, skills(3).size());
        for (String id : skills(3)) assertTrue(id.matches("tensura:[a-z_]+"), id);
        assertTrue(skills(3).containsAll(skills(2)));
    }

    @Test
    void theAuraIsBounded() {
        assertEquals(6.0, AURA_RADIUS);
        assertTrue(MAX_FRIGHTENED <= 16 && MAX_UNEASY <= 8);
        assertTrue(FEAR_TICKS <= 100 && UNEASE_TICKS <= 60, "short effects, renewed each second");
    }

    // ---------------------------------------------------------------- shards
    @Test
    void shardsDropAtFifteenAndTwoPercent() {
        assertEquals(0.15, BOSS_CHANCE);
        assertEquals(0.02, VAULT_CHANCE);
        assertTrue(wins(0.149999, BOSS_CHANCE));
        assertFalse(wins(0.15, BOSS_CHANCE));
        assertTrue(wins(0.0199, VAULT_CHANCE));
        assertFalse(wins(0.02, VAULT_CHANCE));
        assertFalse(wins(0.0, 0.0), "a chance of zero never wins");
        assertTrue(wins(0.999999, 1.0));
        int hits = 0;
        java.util.Random rng = new java.util.Random(7);
        for (int i = 0; i < 100000; i++) if (wins(rng.nextDouble(), BOSS_CHANCE)) hits++;
        assertTrue(Math.abs(hits / 100000.0 - 0.15) < 0.01, "about 15% of kills: " + hits);
    }

    @Test
    void onlyTheCommanderAndTheAvatarPayAShardAtTheirCollapse() {
        assertTrue(isShardZone("MONARCH_VAULT"));
        assertTrue(isShardZone("UNDERWORLD_GATE"));
        assertFalse(isShardZone("GOLIATH_PIT"));
        assertFalse(isShardZone("CHARYBDIS_BASIN"));
        assertFalse(isShardZone("TEMPEST_MIRAGE"));
        assertFalse(isShardZone(null));
        assertEquals("the Devil Avatar", bossName("UNDERWORLD_GATE"));
        assertEquals("the Blood-Red Commander", bossName("MONARCH_VAULT"));
    }

    @Test
    void severityIsAlwaysClampedToOne() {
        assertEquals(1.0, backlash(5.0, true, true), 1.0e-12);
        assertEquals(0.15, backlash(-3.0, true, true), 1.0e-12, "a negative severity is read as none, then the 0.15 is added");
    }
}
