package com.newuniverse.nusmp.core.discovery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The client's copy: shows everything until synced, then exactly what the server said; deltas add; replace replaces; clear resets. */
class DiscoveryCacheTest {
    @Test
    void notSyncedShowsEverythingSoTheOldBehaviourSurvives() {
        DiscoveryCache.clear();
        assertFalse(DiscoveryCache.synced());
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:anything"));
        assertEquals("Name", DiscoveryCache.label("item:anime_multiverse:x", "Name"));
    }

    @Test
    void afterTheFirstSyncOnlyKnownKeysShow() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("item:anime_multiverse:a", "page:b:c"));
        assertTrue(DiscoveryCache.synced());
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:a"));
        assertTrue(DiscoveryCache.visible("page:b:c"));
        assertFalse(DiscoveryCache.visible("item:anime_multiverse:z"));
        assertEquals("???", DiscoveryCache.label("item:anime_multiverse:z", "Secret"));
        assertEquals("Secret", DiscoveryCache.label("item:anime_multiverse:a", "Secret"));
        assertEquals(2, DiscoveryCache.count());
        DiscoveryCache.clear();
    }

    @Test
    void deltasAddAndAReplaceReplaces() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("item:anime_multiverse:a"));
        DiscoveryCache.receive(false, false, true, List.of("item:anime_multiverse:b"));
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:a"));
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:b"));
        DiscoveryCache.receive(true, false, false, List.of("item:anime_multiverse:c"));
        assertFalse(DiscoveryCache.visible("item:anime_multiverse:a"), "a full sync replaces the old set");
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:c"));
        DiscoveryCache.clear();
    }

    @Test
    void showAllMakesEverythingVisibleAndTheKeysStillCount() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, true, false, List.of("item:anime_multiverse:a"));
        assertTrue(DiscoveryCache.showsAll());
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:never_found"));
        DiscoveryCache.receive(false, false, false, List.of());                // creative left: only the state changes
        assertFalse(DiscoveryCache.showsAll());
        assertFalse(DiscoveryCache.visible("item:anime_multiverse:never_found"));
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:a"));
        DiscoveryCache.clear();
    }

    @Test
    void multipleKeysShowAnEntryIfAnyIsKnown() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("item:anime_multiverse:b"));
        assertTrue(DiscoveryCache.visibleAny("item:anime_multiverse:a|item:anime_multiverse:b"));
        assertFalse(DiscoveryCache.visibleAny("item:anime_multiverse:a|item:anime_multiverse:c"));
        assertTrue(DiscoveryCache.visibleAny(""), "no keys: not gated");
        DiscoveryCache.clear();
    }

    @Test
    void invalidKeysFromTheWireAreIgnored() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("junk", "item:anime_multiverse:ok", "item:anime_multiverse:bad key"));
        assertEquals(1, DiscoveryCache.count());
        DiscoveryCache.clear();
    }

    @Test
    void listenersHearOnlyAnnouncedNewKeys() {
        DiscoveryCache.clear();
        List<List<String>> heard = new ArrayList<>();
        DiscoveryCache.addListener(heard::add);
        DiscoveryCache.receive(true, false, false, List.of("item:anime_multiverse:a"));            // the login sync: silent
        assertTrue(heard.isEmpty());
        DiscoveryCache.receive(false, false, true, List.of("item:anime_multiverse:b", "item:anime_multiverse:a"));
        assertEquals(1, heard.size());
        assertEquals(List.of("item:anime_multiverse:b"), heard.get(0), "only the keys that were new");
        DiscoveryCache.receive(false, false, true, List.of("item:anime_multiverse:b"));            // nothing new: silent
        assertEquals(1, heard.size());
        DiscoveryCache.clear();
    }

    @Test
    void clearGoesBackToNotSynced() {
        DiscoveryCache.receive(true, false, false, List.of("item:anime_multiverse:a"));
        DiscoveryCache.clear();
        assertFalse(DiscoveryCache.synced());
        assertEquals(0, DiscoveryCache.count());
        assertTrue(DiscoveryCache.visible("item:anime_multiverse:whatever"));
    }
}
