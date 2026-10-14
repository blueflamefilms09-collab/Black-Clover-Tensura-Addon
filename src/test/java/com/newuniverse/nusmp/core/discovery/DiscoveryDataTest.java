package com.newuniverse.nusmp.core.discovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A player's found set: adding, the guards, the union on clone, and the order. */
class DiscoveryDataTest {
    @Test
    void addIsTrueOnlyTheFirstTime() {
        DiscoveryData d = new DiscoveryData();
        assertTrue(d.isEmpty());
        assertTrue(d.add("item:nusmp:a"));
        assertFalse(d.add("item:nusmp:a"));
        assertTrue(d.has("item:nusmp:a"));
        assertFalse(d.has("item:nusmp:b"));
        assertEquals(1, d.size());
    }

    @Test
    void invalidKeysAreRefused() {
        DiscoveryData d = new DiscoveryData();
        assertFalse(d.add(null));
        assertFalse(d.add(""));
        assertFalse(d.add("garbage"));
        assertFalse(d.add("item:nusmp:with space"));
        assertTrue(d.isEmpty());
    }

    @Test
    void theSetIsCappedSoItCannotGrowWithoutEnd() {
        DiscoveryData d = new DiscoveryData();
        for (int i = 0; i < DiscoveryRules.MAX_KEYS + 50; i++) d.add("item:nusmp:k" + i);
        assertEquals(DiscoveryRules.MAX_KEYS, d.size());
        assertFalse(d.add("item:nusmp:one_more"));
        assertTrue(d.has("item:nusmp:k0"), "what was found first is kept");
    }

    @Test
    void storedKeysLoadWithTheirVersionAndBadOnesAreDropped() {
        DiscoveryData d = new DiscoveryData(1, List.of("item:nusmp:a", "junk", "page:b:c", "item:nusmp:a"));
        assertEquals(1, d.version());
        assertEquals(List.of("item:nusmp:a", "page:b:c"), d.snapshot());
    }

    @Test
    void mergeIsAUnionAndReportsWhatWasNew() {
        DiscoveryData a = new DiscoveryData(), b = new DiscoveryData();
        a.add("item:nusmp:a");
        b.add("item:nusmp:a");
        b.add("item:nusmp:b");
        assertEquals(List.of("item:nusmp:b"), a.merge(b));
        assertTrue(a.has("item:nusmp:b"));
        assertEquals(List.of(), a.merge(b), "merging again changes nothing");
        assertEquals(List.of(), a.merge(null));
        assertFalse(b.has("item:nusmp:x"), "the other set is not touched");
    }

    @Test
    void snapshotIsACopyAndKeysIsReadOnly() {
        DiscoveryData d = new DiscoveryData();
        d.add("item:nusmp:a");
        List<String> snap = d.snapshot();
        snap.add("item:nusmp:z");
        assertFalse(d.has("item:nusmp:z"));
        try {
            d.keys().add("item:nusmp:y");
            fail("the live view must be read-only");
        } catch (UnsupportedOperationException expected) {
            assertFalse(d.has("item:nusmp:y"));
        }
    }

    @Test
    void grandfatheringStampIsTheCurrentVersion() {
        DiscoveryData d = new DiscoveryData();
        assertTrue(DiscoveryRules.needsMigration(d.version()), "a fresh or pre-update player is migrated");
        d.setVersion(DiscoveryRules.VERSION);
        assertFalse(DiscoveryRules.needsMigration(d.version()));
    }
}
