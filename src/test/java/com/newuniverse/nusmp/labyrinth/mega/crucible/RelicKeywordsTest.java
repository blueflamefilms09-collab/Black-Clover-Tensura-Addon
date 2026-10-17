package com.newuniverse.nusmp.labyrinth.mega.crucible;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The word-prefix match that makes a sector's keywords pick its own relics, with real relic names. */
class RelicKeywordsTest {
    @Test
    void wordsSplitOnEverythingThatIsNotALetterOrDigit() {
        assertEquals(List.of("staff", "of", "moonlight"), RelicKeywords.words("Staff of Moonlight"));
        assertEquals(List.of("staff", "of", "moonlight"), RelicKeywords.words("staff_of_moonlight"));
        assertEquals(List.of("silver", "wind", "mountain", "glaive"), RelicKeywords.words("Silver-Wind Mountain Glaive"));
        assertEquals(List.of("scroll", "of", "firball", "bargain", "bin", "scrolls", "2"), RelicKeywords.words("Scroll of Firball (Bargain Bin Scrolls #2)"));
        assertEquals(List.of(), RelicKeywords.words(null));
        assertEquals(List.of(), RelicKeywords.words("--"));
    }

    @Test
    void aKeywordMatchesTheStartOfAWordOfTheIdOrTheName() {
        assertTrue(RelicKeywords.matches("frostfang", "Frostfang", List.of("frost")));
        assertTrue(RelicKeywords.matches("staff_of_moonlight", "Staff of Moonlight", List.of("moon")));
        assertTrue(RelicKeywords.matches("starstudded_leathers", "Starstudded Leathers", List.of("star")));
        assertTrue(RelicKeywords.matches("amulet_of_the_sea_of_stars", "Amulet of the Sea of Stars", List.of("star")));
        assertTrue(RelicKeywords.matches("mjolnir", "Mjolnir", List.of("storm", "mjolnir")));
        assertTrue(RelicKeywords.matches("greaves_of_the_wind_walker", "Greaves of the Wind Walker", List.of("wind")));
        assertTrue(RelicKeywords.matches("some_id", "Mountainguard's Greataxe", List.of("mountain")), "the card name counts as well as the id");
        assertTrue(RelicKeywords.matches("bracers_of_phasing", "Bracers of Phasing", List.of("phas")), "a stem of a word works");
    }

    @Test
    void aKeywordInsideAWordDoesNotMatch() {
        assertFalse(RelicKeywords.matches("voice_of_the_mountain", "Voice of the Mountain", List.of("ice")), "ice is inside Voice, not the start of a word");
        assertTrue(RelicKeywords.matches("voice_of_the_mountain", "Voice of the Mountain", List.of("mountain")));
        assertFalse(RelicKeywords.matches("sunder", "Sunder", List.of("under")));
        assertFalse(RelicKeywords.matches("frostfang", "Frostfang", List.of("fang")));
    }

    @Test
    void noKeywordsMatchNothing() {
        assertFalse(RelicKeywords.matches("frostfang", "Frostfang", List.of()));
        assertFalse(RelicKeywords.matches("frostfang", "Frostfang", null));
        assertFalse(RelicKeywords.matches("frostfang", "Frostfang", List.of("", "  ")));
        assertFalse(RelicKeywords.matches(null, null, List.of("frost")));
    }

    @Test
    void keywordsAreCaseInsensitive() {
        assertTrue(RelicKeywords.matches("Frostfang", "FROSTFANG", List.of("FROST")));
        assertTrue(RelicKeywords.matches("x", "Frostfang", List.of(" Frost ")));
    }
}
