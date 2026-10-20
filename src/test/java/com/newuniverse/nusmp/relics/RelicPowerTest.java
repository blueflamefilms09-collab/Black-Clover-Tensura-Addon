package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.104.0: the attune lock as a pure rule (docs/relic_attune_lock.md): the factor table, the clamp, the no-attunement exemption, the mob wearer. */
class RelicPowerTest {
    private static final RelicPower.Config ON = RelicPower.Config.DEFAULT;

    @Test void defaultsAreTheOwnersFifth() {
        assertEquals(0.20, RelicPower.DEFAULT_UNATTUNED, 0.0);
        assertTrue(ON.lock());
        assertEquals(0.20, ON.unattunedPower(), 0.0);
    }

    @Test void factorTable() {
        //                                  attuned to wielder, needs attunement
        assertEquals(1.0, RelicPower.factor(true, true, ON), 0.0, "attuned to the wielder: full");
        assertEquals(0.20, RelicPower.factor(false, true, ON), 0.0, "needs it and is not attuned to the wielder: a fifth");
        assertEquals(1.0, RelicPower.factor(false, false, ON), 0.0, "needs no attunement: full for anyone");
        assertEquals(1.0, RelicPower.factor(true, false, ON), 0.0);
    }

    @Test void theLockCanBeSwitchedOff() {
        RelicPower.Config off = new RelicPower.Config(false, 0.20);
        assertEquals(1.0, RelicPower.factor(false, true, off), 0.0);
    }

    @Test void aNullConfigIsTheDefault() {
        assertEquals(0.20, RelicPower.factor(false, true, null), 0.0);
    }

    @Test void thePowerIsClamped() {
        assertEquals(0.05, new RelicPower.Config(true, 0.0).unattunedPower(), 0.0);
        assertEquals(0.05, new RelicPower.Config(true, -3.0).unattunedPower(), 0.0);
        assertEquals(1.0, new RelicPower.Config(true, 7.0).unattunedPower(), 0.0);
        assertEquals(0.5, new RelicPower.Config(true, 0.5).unattunedPower(), 0.0);
        assertEquals(0.20, RelicPower.clampPower(Double.NaN), 0.0);
        assertEquals(0.05, RelicPower.factor(false, true, new RelicPower.Config(true, 0.001)), 0.0);
        assertEquals(1.0, RelicPower.factor(false, true, new RelicPower.Config(true, 5.0)), 0.0);
    }

    @Test void aMobWearerCountsAsAttuned() {
        // Attune.isMine answers true for every non-player; the rule then reads "attuned to the wielder"
        assertEquals(1.0, RelicPower.factor(true, true, ON), 0.0);
    }

    @Test void noRelicIsExemptedById() {
        assertTrue(RelicPower.EXEMPT_IDS.isEmpty(), "the lock has no id exemption list: the only exemption is 'needs no attunement'");
        assertEquals(0.20, RelicPower.factor("elsdocia_like_id", false, true, ON), 0.0);
        assertEquals(0.20, RelicPower.factor((String) null, false, true, ON), 0.0);
    }

    @Test void scaleCutsOnlyAndNeverRaises() {
        assertEquals(20f, RelicPower.scale(100f, 0.20), 1e-4f);
        assertEquals(100f, RelicPower.scale(100f, 1.0), 0f);
        assertEquals(100f, RelicPower.scale(100f, 2.0), 0f, "a factor above 1 is never applied");
        assertEquals(0f, RelicPower.scale(0f, 0.2), 0f);
        assertEquals(-5f, RelicPower.scale(-5f, 0.2), 0f, "a non-positive amount is left alone");
        assertEquals(0f, RelicPower.scale(100f, -1.0), 0f, "a negative factor floors at zero damage");
    }

    @Test void nerfedAndPercentText() {
        assertTrue(RelicPower.nerfed(0.2));
        assertFalse(RelicPower.nerfed(1.0));
        assertEquals(20, RelicPower.percent(0.2));
        assertEquals(5, RelicPower.percent(0.05));
        assertEquals(100, RelicPower.percent(1.0));
        assertEquals("20%", RelicPower.label(0.2));
    }

    @Test void onlyTheWieldersOwnBlowIsMelee() {
        assertTrue(RelicPower.isMeleeBlow(true, true));
        assertFalse(RelicPower.isMeleeBlow(true, false), "a projectile or a spell credited to the player is not the weapon's blow");
        assertFalse(RelicPower.isMeleeBlow(false, true), "a magic damage type is not the weapon's blow");
    }

    @Test void theGradeRuleAndTheLockAgree() {
        // a relic below Legend never needs attunement, so the lock never touches it; Legend and above are cut when unattuned
        for (Grade g : Grade.values()) {
            double f = RelicPower.factor(false, AttuneRules.requires(true, g), ON);
            assertEquals(g.ordinal() >= Grade.LEGEND.ordinal() ? 0.20 : 1.0, f, 0.0, g.toString());
        }
    }
}
