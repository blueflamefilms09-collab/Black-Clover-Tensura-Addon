package com.newuniverse.nusmp.grimoire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Unique Skill's name carries the bound character: the pure helper, for every character of CanonBook. */
class CanonNamingTest {
    @Test
    void theOwnersExampleNames() {
        assertEquals("Rades Spirito's Soul Corpse Magic", CanonNaming.named("rades", "Soul Corpse Magic"));
        assertEquals("Yami Sukehiro's Dark Magic", CanonNaming.named("yami", "Dark Magic"));
        assertEquals("Asta's Anti-Magic", CanonNaming.named("asta", "Anti-Magic"));
    }

    @Test
    void aBookWithNoCharacterKeepsThePlainName() {
        assertEquals("Soul Corpse Magic", CanonNaming.named(null, "Soul Corpse Magic"));
        assertEquals("Soul Corpse Magic", CanonNaming.named("", "Soul Corpse Magic"));
        assertEquals("Soul Corpse Magic", CanonNaming.named("not_a_character", "Soul Corpse Magic"));
        assertEquals("Soul Corpse Magic", CanonNaming.namedFor(null, "Soul Corpse Magic"));
        assertEquals("Soul Corpse Magic", CanonNaming.namedFor("  ", "Soul Corpse Magic"));
    }

    @Test
    void thePossessive() {
        assertEquals("Yami Sukehiro's", CanonNaming.possessive("Yami Sukehiro"));
        assertEquals("Mars'", CanonNaming.possessive("Mars"), "a name that ends in s keeps a bare apostrophe");
        assertEquals("", CanonNaming.possessive(null));
        assertEquals("", CanonNaming.possessive(" "));
        assertEquals("Grey's", CanonNaming.possessive("Grey"));
    }

    @Test
    void everyCharacterOfCanonBookGetsItsNameInFrontOfItsMagic() {
        for (CanonBook b : CanonBook.values()) {
            assertEquals(b.owner, CanonNaming.ownerOf(b.id()), b.name());
            assertTrue(CanonNaming.known(b.id()), b.name());
            String name = CanonNaming.named(b.id(), "Some Magic");
            assertTrue(name.startsWith(CanonNaming.possessive(b.owner) + " "), b.name() + ": " + name);
            assertTrue(name.endsWith(" Some Magic"), name);
            assertEquals(name, CanonNaming.namedFor(b.owner, "Some Magic"));
            assertEquals(name, CanonNaming.named(b.id().toUpperCase(), "Some Magic"), "the id lookup ignores case, as CanonBook.byId does");
        }
    }

    @Test
    void anInstanceCarriesTheCharacterOfTheSlotOnlyForTheMagicOfTheSlot() {
        assertEquals("rades", CanonNaming.wanted("SOUL_CORPSE", "rades", "SOUL_CORPSE"));
        assertEquals("", CanonNaming.wanted("SOUL_CORPSE", "rades", "FLAME"), "a second book the player holds belongs to no character");
        assertEquals("", CanonNaming.wanted("SOUL_CORPSE", "", "SOUL_CORPSE"), "a book with no character has none");
        assertEquals("", CanonNaming.wanted("SOUL_CORPSE", "not_a_character", "SOUL_CORPSE"));
        assertEquals("", CanonNaming.wanted("", "rades", "SOUL_CORPSE"), "an empty slot gives no character");
        assertEquals("", CanonNaming.wanted(null, "rades", "SOUL_CORPSE"));
        assertEquals("", CanonNaming.wanted("SOUL_CORPSE", "rades", null));
    }

    @Test
    void everyCharactersMagicIsAKnownMagicName() {
        for (CanonBook b : CanonBook.values()) assertFalse(b.magic.isBlank(), b.name());
    }
}
