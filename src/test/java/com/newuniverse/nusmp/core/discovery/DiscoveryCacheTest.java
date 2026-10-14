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
        assertTrue(DiscoveryCache.visible("item:nusmp:anything"));
        assertEquals("Name", DiscoveryCache.label("item:nusmp:x", "Name"));
    }

    @Test
    void afterTheFirstSyncOnlyKnownKeysShow() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("item:nusmp:a", "page:b:c"));
        assertTrue(DiscoveryCache.synced());
        assertTrue(DiscoveryCache.visible("item:nusmp:a"));
        assertTrue(DiscoveryCache.visible("page:b:c"));
        assertFalse(DiscoveryCache.visible("item:nusmp:z"));
        assertEquals("???", DiscoveryCache.label("item:nusmp:z", "Secret"));
        assertEquals("Secret", DiscoveryCache.label("item:nusmp:a", "Secret"));
        assertEquals(2, DiscoveryCache.count());
        DiscoveryCache.clear();
    }

    @Test
    void deltasAddAndAReplaceReplaces() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("item:nusmp:a"));
        DiscoveryCache.receive(false, false, true, List.of("item:nusmp:b"));
        assertTrue(DiscoveryCache.visible("item:nusmp:a"));
        assertTrue(DiscoveryCache.visible("item:nusmp:b"));
        DiscoveryCache.receive(true, false, false, List.of("item:nusmp:c"));
        assertFalse(DiscoveryCache.visible("item:nusmp:a"), "a full sync replaces the old set");
        assertTrue(DiscoveryCache.visible("item:nusmp:c"));
        DiscoveryCache.clear();
    }

    @Test
    void showAllMakesEverythingVisibleAndTheKeysStillCount() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, true, false, List.of("item:nusmp:a"));
        assertTrue(DiscoveryCache.showsAll());
        assertTrue(DiscoveryCache.visible("item:nusmp:never_found"));
        DiscoveryCache.receive(false, false, false, List.of());                // creative left: only the state changes
        assertFalse(DiscoveryCache.showsAll());
        assertFalse(DiscoveryCache.visible("item:nusmp:never_found"));
        assertTrue(DiscoveryCache.visible("item:nusmp:a"));
        DiscoveryCache.clear();
    }

    @Test
    void multipleKeysShowAnEntryIfAnyIsKnown() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("item:nusmp:b"));
        assertTrue(DiscoveryCache.visibleAny("item:nusmp:a|item:nusmp:b"));
        assertFalse(DiscoveryCache.visibleAny("item:nusmp:a|item:nusmp:c"));
        assertTrue(DiscoveryCache.visibleAny(""), "no keys: not gated");
        DiscoveryCache.clear();
    }

    @Test
    void invalidKeysFromTheWireAreIgnored() {
        DiscoveryCache.clear();
        DiscoveryCache.receive(true, false, false, List.of("junk", "item:nusmp:ok", "item:nusmp:bad key"));
        assertEquals(1, DiscoveryCache.count());
        DiscoveryCache.clear();
    }

    @Test
    void listenersHearOnlyAnnouncedNewKeys() {
        DiscoveryCache.clear();
        List<List<String>> heard = new ArrayList<>();
        DiscoveryCache.addListener(heard::add);
        DiscoveryCache.receive(true, false, false, List.of("item:nusmp:a"));            // the login sync: silent
        assertTrue(heard.isEmpty());
        DiscoveryCache.receive(false, false, true, List.of("item:nusmp:b", "item:nusmp:a"));
        assertEquals(1, heard.size());
        assertEquals(List.of("item:nusmp:b"), heard.get(0), "only the keys that were new");
        DiscoveryCache.receive(false, false, true, List.of("item:nusmp:b"));            // nothing new: silent
        assertEquals(1, heard.size());
        DiscoveryCache.clear();
    }

    @Test
    void clearGoesBackToNotSynced() {
        DiscoveryCache.receive(true, false, false, List.of("item:nusmp:a"));
        DiscoveryCache.clear();
        assertFalse(DiscoveryCache.synced());
        assertEquals(0, DiscoveryCache.count());
        assertTrue(DiscoveryCache.visible("item:nusmp:whatever"));
    }
}
