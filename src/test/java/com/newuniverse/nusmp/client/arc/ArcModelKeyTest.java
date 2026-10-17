package com.newuniverse.nusmp.client.arc;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Which model an arc character is drawn with: clean ids only, the character's own model first, the plain villager next, nothing last. */
class ArcModelKeyTest {
    @Test
    void aPlainIdIsKeptAndShoutedIdsAreLowered() {
        assertEquals("heath_grice", ArcModelKey.clean("heath_grice"));
        assertEquals("heath_grice", ArcModelKey.clean("  Heath_Grice "));
        assertEquals("asta2", ArcModelKey.clean("ASTA2"));
    }

    @Test
    void anythingThatCouldBeAPathOrIsEmptyIsRefused() {
        assertEquals("", ArcModelKey.clean(null));
        assertEquals("", ArcModelKey.clean(""));
        assertEquals("", ArcModelKey.clean("   "));
        assertEquals("", ArcModelKey.clean("../etc/passwd"));
        assertEquals("", ArcModelKey.clean("a/b"));
        assertEquals("", ArcModelKey.clean("nusmp:asta"));
        assertEquals("", ArcModelKey.clean("a.b"));
        assertEquals("", ArcModelKey.clean("9lives"), "an id starts with a letter, like the roster's ids");
        assertEquals("", ArcModelKey.clean("x".repeat(65)), "a very long id is a bad save, not a model");
    }

    @Test
    void theStemAndTheSecondFormFollowTheFileNamesTheKitWrites() {
        assertEquals("arc_heath_grice", ArcModelKey.stem("heath_grice"));
        assertEquals("heath_grice_form2", ArcModelKey.form2("heath_grice"));
        assertEquals("", ArcModelKey.stem("../x"));
        assertEquals("", ArcModelKey.form2(null));
        assertEquals("arc", ArcModelKey.KEY);
    }

    @Test
    void theOwnModelWinsTheFallbackIsSecondAndNothingIsLast() {
        Set<String> both = Set.of("yami_sukehiro", ArcModelKey.FALLBACK);
        assertEquals("yami_sukehiro", ArcModelKey.resolve("yami_sukehiro", both::contains));
        assertEquals(ArcModelKey.FALLBACK, ArcModelKey.resolve("asta", both::contains), "an unknown character is a villager");
        assertEquals(ArcModelKey.FALLBACK, ArcModelKey.resolve("", both::contains), "a blank NPC is a villager");
        assertEquals(ArcModelKey.FALLBACK, ArcModelKey.resolve("../hack", both::contains), "a bad id is a villager, never a path");
        Set<String> onlyHim = Set.of("yami_sukehiro");
        assertEquals("", ArcModelKey.resolve("asta", onlyHim::contains), "no model and no fallback: nothing is drawn");
        assertEquals("yami_sukehiro", ArcModelKey.resolve("yami_sukehiro", onlyHim::contains));
    }

    @Test
    void theResolverIsAskedOnlyForCleanNames() {
        StringBuilder asked = new StringBuilder();
        ArcModelKey.resolve("Black Bull", n -> { asked.append(n).append(';'); return false; });
        assertEquals(ArcModelKey.FALLBACK + ";", asked.toString(), "a name with a space is never offered to the resource lookup");
    }
}
