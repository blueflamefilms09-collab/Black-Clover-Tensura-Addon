package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Summoner's Hell Mode creatures (docs/summoner_creatures.md): the card-to-creature table, the append-only order, the tier power and the model files every species needs must not regress. */
class SummonSpeciesTest {
    private static final Path RES = Path.of("src/main/resources/assets/anime_multiverse");

    @Test
    void theCardsCallTheirCreaturesAndRanksUpgradeThem() {
        assertEquals(SummonSpecies.HAYATE, SummonSpecies.forCard(SummonRules.Card.BEAST, 0));
        assertEquals(SummonSpecies.CERORIN, SummonSpecies.forCard(SummonRules.Card.BEAST, 2));
        assertEquals(SummonSpecies.CERORIN, SummonSpecies.forCard(SummonRules.Card.LEGION, 5));
        assertEquals(SummonSpecies.ROCANEL, SummonSpecies.forCard(SummonRules.Card.STONE, 1));
        assertEquals(SummonSpecies.MIRA, SummonSpecies.forCard(SummonRules.Card.STONE, 2));
        assertEquals(SummonSpecies.ELLIE, SummonSpecies.forCard(SummonRules.Card.WRAITH, 0));
        assertEquals(SummonSpecies.OKIYOSAN, SummonSpecies.forCard(SummonRules.Card.WRAITH, 1));
        for (SummonRules.Card c : SummonRules.Card.values()) for (int r = 0; r < 12; r++) assertNotNull(SummonSpecies.forCard(c, r));
    }

    @Test
    void theOrderIsAppendOnlyAndAStoredIndexNeverCrashes() {
        assertEquals(0, SummonSpecies.HAYATE.ordinal());
        assertEquals(11, SummonSpecies.MERUS.ordinal());
        assertEquals(12, SummonSpecies.values().length);
        assertNull(SummonSpecies.byIndex(-1));
        assertNull(SummonSpecies.byIndex(99));
        assertEquals(SummonSpecies.DRADRA, SummonSpecies.byIndex(SummonSpecies.DRADRA.ordinal()));
        assertEquals(SummonSpecies.GENBU, SummonSpecies.byId("genbu"));
        assertNull(SummonSpecies.byId("nope"));
    }

    @Test
    void theUltimateIsTwiceTheUniqueAndEveryNumberIsSane() {
        assertEquals(1.0, SummonSpecies.power(Tier.BASE));
        assertTrue(SummonSpecies.power(Tier.MASTERED) > SummonSpecies.power(Tier.BASE));
        assertEquals(2.0, SummonSpecies.power(Tier.ULTIMATE));
        assertTrue(SummonSpecies.power(Tier.ULTIMATE_MASTERED) > SummonSpecies.power(Tier.ULTIMATE));
        for (SummonSpecies s : SummonSpecies.values()) {
            assertTrue(s.hpMul > 0 && s.hpMul <= 5 && s.dmgMul > 0 && s.dmgMul <= 5, s + " multipliers");
            assertTrue(s.speed > 0.1 && s.speed < 0.6, s + " speed");
            assertTrue(s.width >= 0.4f && s.width <= 2.0f && s.height >= 0.4f && s.height <= 2.5f, s + " box");
        }
    }

    @Test
    void aPerformerIsShortAndBounded() {
        for (Tier t : Tier.values()) {
            int ticks = SummonSpecies.performerTicks(t);
            assertTrue(ticks >= 20 && ticks <= 120, "life " + ticks);
            assertTrue(SummonSpecies.performerScale(t) >= 1.0f && SummonSpecies.performerScale(t) <= 2.0f);
        }
        assertTrue(SummonSpecies.performerTicks(Tier.ULTIMATE) > SummonSpecies.performerTicks(Tier.BASE));
    }

    @Test
    void everySpeciesHasItsModelSheetGlowAndFourClips() throws Exception {
        for (SummonSpecies s : SummonSpecies.values()) {
            String n = "summon_" + s.id;
            Path geo = RES.resolve("geo/entity/" + n + ".geo.json"), anim = RES.resolve("animations/entity/" + n + ".animation.json");
            assertTrue(Files.isRegularFile(geo), n + " geo");
            assertTrue(Files.isRegularFile(RES.resolve("textures/entity/" + n + ".png")), n + " sheet");
            assertTrue(Files.isRegularFile(RES.resolve("textures/entity/" + n + "_glowmask.png")), n + " glow mask");
            assertTrue(Files.readString(geo).contains("\"geometry." + n + "\""), n + " geometry id");
            String a = Files.readString(anim);
            for (String clip : new String[]{"idle", "walk", "attack", "ability"}) assertTrue(a.contains("\"animation." + n + "." + clip + "\""), n + " clip " + clip);
        }
    }
}
