package com.newuniverse.nusmp.core.discovery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the discovery system: keys, visibility, labels, the slot diff, chunking, the grimoire bitmask. */
class DiscoveryRulesTest {
    @Test
    void keysHaveTheirPrefixAndOnlyTheModIsTracked() {
        assertEquals("item:nusmp:heart_of_words", DiscoveryRules.itemKey("nusmp", "heart_of_words"));
        assertEquals("page:fire_book:attribute_bolt", DiscoveryRules.pageKey("fire_book", "attribute_bolt"));
        assertEquals("skill:nusmp:grimoire_fire", DiscoveryRules.skillKey("nusmp", "grimoire_fire"));
        assertEquals("magic:poison_plant", DiscoveryRules.magicKey("POISON_PLANT"));
        assertEquals("spiritlord:salamander", DiscoveryRules.spiritLordKey("SALAMANDER"));
        assertEquals("mob:nusmp:zagred", DiscoveryRules.mobKey("nusmp", "zagred"));
        assertEquals("boss:dante", DiscoveryRules.bossKey("Dante"));
        assertTrue(DiscoveryRules.tracksNamespace("nusmp"));
        assertFalse(DiscoveryRules.tracksNamespace("tensura"));
        assertFalse(DiscoveryRules.tracksNamespace("minecraft"));
        assertFalse(DiscoveryRules.tracksNamespace(null));
    }

    @Test
    void validKeysAreShortPrefixedAndClean() {
        assertTrue(DiscoveryRules.validKey("item:nusmp:a"));
        assertTrue(DiscoveryRules.validKey("boss:dante"));
        assertFalse(DiscoveryRules.validKey(null));
        assertFalse(DiscoveryRules.validKey(""));
        assertFalse(DiscoveryRules.validKey("item:"));                       // a prefix alone
        assertFalse(DiscoveryRules.validKey("nusmp:thing"));                 // no known prefix
        assertFalse(DiscoveryRules.validKey("item:nusmp:has space"));
        assertFalse(DiscoveryRules.validKey("item:nusmp:tab\there"));
        assertFalse(DiscoveryRules.validKey("item:" + "x".repeat(DiscoveryRules.MAX_KEY_LENGTH)));
        assertTrue(DiscoveryRules.validKey("item:" + "x".repeat(DiscoveryRules.MAX_KEY_LENGTH - 5)));
    }

    @Test
    void visibleOnlyWhenKnownOrShowAll() {
        Set<String> known = Set.of("item:nusmp:a");
        assertTrue(DiscoveryRules.visible(known, false, "item:nusmp:a"));
        assertFalse(DiscoveryRules.visible(known, false, "item:nusmp:b"));
        assertTrue(DiscoveryRules.visible(known, true, "item:nusmp:b"), "show-all (creative) sees everything");
        assertFalse(DiscoveryRules.visible(known, false, null), "no key is never visible unless show-all");
        assertFalse(DiscoveryRules.visible(Set.of(), false, "item:nusmp:a"));
        assertFalse(DiscoveryRules.visible(null, false, "item:nusmp:a"));
    }

    @Test
    void anyOfSeveralKeysShowsAnEntryAndNoKeysNeverGates() {
        Set<String> known = Set.of("item:nusmp:b");
        assertTrue(DiscoveryRules.visibleAny(known, false, List.of("item:nusmp:a", "item:nusmp:b")));
        assertFalse(DiscoveryRules.visibleAny(known, false, List.of("item:nusmp:a", "item:nusmp:c")));
        assertTrue(DiscoveryRules.visibleAny(known, false, List.of()), "an entry with no keys is not gated");
        assertTrue(DiscoveryRules.visibleAny(known, true, List.of("item:nusmp:z")));
        assertEquals(List.of("a", "b", "c"), DiscoveryRules.splitKeys("a|b| c |"));
        assertEquals(List.of(), DiscoveryRules.splitKeys(null));
        assertEquals(List.of(), DiscoveryRules.splitKeys(""));
    }

    @Test
    void anUndiscoveredLabelIsAlwaysTheSameThreeQuestionMarks() {
        Set<String> known = Set.of("item:nusmp:a");
        assertEquals("Sword", DiscoveryRules.label(known, false, "item:nusmp:a", "Sword"));
        assertEquals("???", DiscoveryRules.label(known, false, "item:nusmp:b", "Sword"));
        assertEquals("???", DiscoveryRules.label(known, false, "item:nusmp:b", "A Much Longer Secret Name"), "the length of the name must not leak");
        assertEquals("A Much Longer Secret Name", DiscoveryRules.label(known, true, "item:nusmp:b", "A Much Longer Secret Name"));
    }

    @Test
    void slotDiffReportsOnlyWhatChangedAndStoresIt() {
        String[] prev = new String[5];
        String[] now = {"sword", null, "bow", "sword", null};
        List<String> found = new ArrayList<>();
        assertEquals(3, DiscoveryRules.diffSlots(prev, now, found::add));
        assertEquals(List.of("sword", "bow", "sword"), found);
        found.clear();
        assertEquals(0, DiscoveryRules.diffSlots(prev, now, found::add), "an unchanged inventory finds nothing and changes nothing");
        assertTrue(found.isEmpty());
        now[0] = null;                                                         // a slot emptied: a change, but nothing new to find
        assertEquals(1, DiscoveryRules.diffSlots(prev, now, found::add));
        assertTrue(found.isEmpty());
        assertNull(prev[0], "the previous scan was updated");
        now[1] = "bow";
        assertEquals(1, DiscoveryRules.diffSlots(prev, now, found::add));
        assertEquals(List.of("bow"), found);
    }

    @Test
    void slotDiffUsesIdentityNotEquality() {
        String a = new String("same"), b = new String("same");
        Object[] prev = {a};
        List<Object> found = new ArrayList<>();
        assertEquals(1, DiscoveryRules.diffSlots(prev, new Object[]{b}, found::add), "a different object in the slot is a change");
        assertEquals(0, DiscoveryRules.diffSlots(prev, new Object[]{b}, found::add));
    }

    @Test
    void grimoireBitmaskOpensModeZeroAlwaysAndTheSetBits() {
        assertEquals(List.of(0), DiscoveryRules.unlockedModes(0, 6));
        assertEquals(List.of(0, 1, 3), DiscoveryRules.unlockedModes((1 << 1) | (1 << 3), 6));
        assertEquals(List.of(0, 1), DiscoveryRules.unlockedModes((1 << 1) | (1 << 4), 3), "bits past the family count are not pages of the family");
        assertEquals(List.of(), DiscoveryRules.unlockedModes(-1, 0));
        assertEquals(31, DiscoveryRules.unlockedModes(-1, 99).size(), "an int mask holds at most 31 modes");
    }

    @Test
    void chunksNeverExceedThePacketSizeAndNeverVanish() {
        assertEquals(1, DiscoveryRules.chunks(List.of()).size(), "an empty set still sends one (empty) packet so the client is told");
        assertTrue(DiscoveryRules.chunks(List.of()).get(0).isEmpty());
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < DiscoveryRules.CHUNK * 2 + 1; i++) keys.add("item:nusmp:k" + i);
        List<List<String>> chunks = DiscoveryRules.chunks(keys);
        assertEquals(3, chunks.size());
        int total = 0;
        for (List<String> c : chunks) { assertTrue(c.size() <= DiscoveryRules.CHUNK); total += c.size(); }
        assertEquals(keys.size(), total);
        assertEquals(keys.get(DiscoveryRules.CHUNK), chunks.get(1).get(0), "order is kept");
    }

    @Test
    void migrationRunsUntilTheVersionIsStamped() {
        assertTrue(DiscoveryRules.needsMigration(0));
        assertFalse(DiscoveryRules.needsMigration(DiscoveryRules.VERSION));
        assertFalse(DiscoveryRules.needsMigration(DiscoveryRules.VERSION + 1), "data from a newer build is never migrated down");
    }
}
