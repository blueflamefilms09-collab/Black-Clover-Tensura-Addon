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
        assertTrue(d.add("item:anime_multiverse:a"));
        assertFalse(d.add("item:anime_multiverse:a"));
        assertTrue(d.has("item:anime_multiverse:a"));
        assertFalse(d.has("item:anime_multiverse:b"));
        assertEquals(1, d.size());
    }

    @Test
    void invalidKeysAreRefused() {
        DiscoveryData d = new DiscoveryData();
        assertFalse(d.add(null));
        assertFalse(d.add(""));
        assertFalse(d.add("garbage"));
        assertFalse(d.add("item:anime_multiverse:with space"));
        assertTrue(d.isEmpty());
    }

    @Test
    void theSetIsCappedSoItCannotGrowWithoutEnd() {
        DiscoveryData d = new DiscoveryData();
        for (int i = 0; i < DiscoveryRules.MAX_KEYS + 50; i++) d.add("item:anime_multiverse:k" + i);
        assertEquals(DiscoveryRules.MAX_KEYS, d.size());
        assertFalse(d.add("item:anime_multiverse:one_more"));
        assertTrue(d.has("item:anime_multiverse:k0"), "what was found first is kept");
    }

    @Test
    void storedKeysLoadWithTheirVersionAndBadOnesAreDropped() {
        DiscoveryData d = new DiscoveryData(1, List.of("item:anime_multiverse:a", "junk", "page:b:c", "item:anime_multiverse:a"));
        assertEquals(1, d.version());
        assertEquals(List.of("item:anime_multiverse:a", "page:b:c"), d.snapshot());
    }

    @Test
    void mergeIsAUnionAndReportsWhatWasNew() {
        DiscoveryData a = new DiscoveryData(), b = new DiscoveryData();
        a.add("item:anime_multiverse:a");
        b.add("item:anime_multiverse:a");
        b.add("item:anime_multiverse:b");
        assertEquals(List.of("item:anime_multiverse:b"), a.merge(b));
        assertTrue(a.has("item:anime_multiverse:b"));
        assertEquals(List.of(), a.merge(b), "merging again changes nothing");
        assertEquals(List.of(), a.merge(null));
        assertFalse(b.has("item:anime_multiverse:x"), "the other set is not touched");
    }

    @Test
    void snapshotIsACopyAndKeysIsReadOnly() {
        DiscoveryData d = new DiscoveryData();
        d.add("item:anime_multiverse:a");
        List<String> snap = d.snapshot();
        snap.add("item:anime_multiverse:z");
        assertFalse(d.has("item:anime_multiverse:z"));
        try {
            d.keys().add("item:anime_multiverse:y");
            fail("the live view must be read-only");
        } catch (UnsupportedOperationException expected) {
            assertFalse(d.has("item:anime_multiverse:y"));
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
