package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The words of the relics' tooltips and action bar (docs/living_dungeon.md section 7): every relic says what it does, and the new rules are told. */
class RelicTextTest {
    @Test
    void everyRelicSaysWhatItDoesAndNamesItsCurseExactlyWhenItHasOne() {
        for (Relic r : Relic.values()) {
            String effect = RelicText.effect(r);
            assertNotNull(effect, r.name());
            assertFalse(effect.isBlank(), r.name() + " needs an effect line");
            if (r.curse == null) assertNull(RelicText.curse(r), r.name() + " has no curse, so it has no curse text");
            else assertFalse(RelicText.curse(r) == null || RelicText.curse(r).isBlank(), r.name() + " names the curse " + r.curse + " and must explain it");
        }
    }

    @Test
    void everyRelicIsToldToBeWornInADungeonRelicSlotAndThatThereAreTwo() {
        for (Relic r : Relic.values()) {
            String where = RelicText.where(r);
            assertTrue(where.contains("worn in a Dungeon Relic slot (you have two)"), where);
            assertFalse(where.toLowerCase().contains("charm") || where.toLowerCase().contains("belt") || where.toLowerCase().contains("ring slot"), where);
        }
        assertTrue(RelicText.where(Relic.GAMBLERS_GRIMOIRE_PAGE).startsWith("Artifact"));
        assertTrue(RelicText.where(Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS).startsWith("Wondrous item"));
    }

    @Test
    void theAmuletTellsItsBlessingAndItsCurse() {
        String effect = RelicText.effect(Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS), curse = RelicText.curse(Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS);
        assertTrue(effect.contains("mana signature") && effect.contains("Juggernaut"), effect);
        assertTrue(curse.contains("halved") && curse.contains("regeneration"), curse);
        assertEquals("Muffled Mana", Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS.curse);
    }

    @Test
    void thePagesCurseTellsTheImmuneSystemAndTheDay() {
        String curse = RelicText.curse(Relic.GAMBLERS_GRIMOIRE_PAGE);
        assertTrue(curse.contains("d20") && curse.contains("a tenth of your mana pool") && curse.contains("for the day"), curse);
        assertTrue(curse.contains("immune system"), curse);
        assertFalse(curse.contains("collapses"), "the old consequence is gone from the words: " + curse);
        String effect = RelicText.effect(Relic.GAMBLERS_GRIMOIRE_PAGE), hint = RelicText.hint(Relic.GAMBLERS_GRIMOIRE_PAGE);
        assertTrue(effect.contains("Use Relic key"), effect);
        assertTrue(hint.contains("off-hand"), hint);
    }

    @Test
    void theAttunementTellsThePlayerToStandStillWhateverBrokeIt() {
        for (AttuneClock.Why why : AttuneClock.Why.values()) {
            String text = RelicText.reset(why);
            assertTrue(text.contains("Stand still"), why + ": " + text);
        }
        assertTrue(RelicText.reset(AttuneClock.Why.MOVED).startsWith("You moved"));
        assertTrue(RelicText.reset(AttuneClock.Why.HURT).contains("blow"));
    }
}
