package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of relics_wear: the wear check, the sync rules, the right-click equip rules, the once-only causes. */
class RelicWearRulesTest {
    private static RelicWear.Data complete() {
        return new RelicWear.Data(
                Map.of("necklace", 1, "back", 1, "belt", 1, "hands", 2, "charm", 1, "dungeon_relic", 2, "relic_head", 1, "relic_feet", 1),
                Map.of("necklace", Set.of("anime_multiverse:a"), "back", Set.of("anime_multiverse:b"), "belt", Set.of("anime_multiverse:c"), "hands", Set.of("anime_multiverse:d"), "charm", Set.of("anime_multiverse:e"),
                        "dungeon_relic", Set.of(), "relic_head", Set.of("anime_multiverse:h"), "relic_feet", Set.of("anime_multiverse:f")),
                Set.of("necklace", "back", "belt", "hands", "charm", "dungeon_relic", "relic_head", "relic_feet"));
    }

    @Test
    void completeDataHasNoProblem() {
        assertEquals(List.of(), RelicWear.checkSlots(complete()));
        assertEquals(List.of(), RelicWear.checkRelics(Map.of("anime_multiverse:a", "NECK", "anime_multiverse:b", "BACK", "anime_multiverse:c", "BELT", "anime_multiverse:d", "HANDS", "anime_multiverse:e", "CHARM",
                "anime_multiverse:h", "HEAD", "anime_multiverse:c1", "CHEST", "anime_multiverse:f", "FEET", "anime_multiverse:w", "MAIN_HAND", "anime_multiverse:p", "CONSUMABLE"), complete()));
    }

    @Test
    void aSlotTheModDoesNotDefineIsNamedEvenWhenCuriosIsKnownToShipIt() {
        RelicWear.Data d = new RelicWear.Data(Map.of("back", 1), Map.of("necklace", Set.of("anime_multiverse:a")), Set.of("necklace"));
        List<String> p = RelicWear.checkRelics(Map.of("anime_multiverse:a", "NECK"), d);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("relies on Curios shipping it"));
    }

    @Test
    void aSlotNobodyDefinesIsAHole() {
        assertTrue(RelicWear.CURIOS_SHIPPED.containsAll(RelicWear.RELIC_SLOTS), "the five relic slots are the ones Curios ships");
        assertTrue(RelicWear.CURIOS_SHIPPED.contains("ring") && RelicWear.CURIOS_SHIPPED.contains("curio"));
    }

    @Test
    void aMissingTagEntityOrSizeIsFound() {
        RelicWear.Data noTag = new RelicWear.Data(Map.of("necklace", 1), Map.of("necklace", Set.of()), Set.of("necklace"));
        assertTrue(RelicWear.checkRelics(Map.of("anime_multiverse:a", "NECK"), noTag).get(0).contains("not listed in data/curios/tags/item/necklace.json"));
        RelicWear.Data noEntity = new RelicWear.Data(Map.of("necklace", 1), Map.of("necklace", Set.of("anime_multiverse:a")), Set.of());
        assertTrue(RelicWear.checkRelics(Map.of("anime_multiverse:a", "NECK"), noEntity).get(0).contains("not given to the player"));
        RelicWear.Data zero = new RelicWear.Data(Map.of("necklace", 0), Map.of("necklace", Set.of("anime_multiverse:a")), Set.of("necklace"));
        assertTrue(RelicWear.checkRelics(Map.of("anime_multiverse:a", "NECK"), zero).get(0).contains("not defined by this mod"));
        assertTrue(RelicWear.checkSlots(new RelicWear.Data(Map.of(), Map.of(), Set.of())).size() >= 3 * 7);
    }

    @Test
    void theSlotIdsFollowTheEnum() {
        assertEquals("necklace", RelicWear.curiosSlot("NECK"));
        assertEquals("charm", RelicWear.curiosSlot("CHARM"));
        assertNull(RelicWear.curiosSlot("HEAD"));
        assertEquals("FEET", RelicWear.armourEquipSlot("FEET"));
        assertNull(RelicWear.armourEquipSlot("NECK"));
        for (String s : RelicWear.RELIC_SLOTS) {
            boolean found = false;
            for (RelicSlot r : RelicSlot.values()) if (s.equals(r.curiosSlot())) found = true;
            assertTrue(found, s);
        }
    }

    @Test
    void theReaderParsesSlotsTagsWithRequiredFlagsAndEntities() throws IOException {
        Map<String, String> files = Map.of(
                "data/anime_multiverse/curios/slots/necklace.json", "{\"size\": 3}",
                "data/curios/tags/item/necklace.json", "{\"values\": [\"anime_multiverse:a\", {\"id\": \"anime_multiverse:b\", \"required\": false}]}",
                "data/anime_multiverse/curios/entities/relics.json", "{\"entities\": [\"player\"], \"slots\": [\"necklace\"]}",
                "data/anime_multiverse/curios/entities/dungeon_relic.json", "{\"entities\": [\"zombie\"], \"slots\": [\"dungeon_relic\"]}");
        RelicWear.Data d = RelicWear.read(path -> files.containsKey(path) ? new ByteArrayInputStream(files.get(path).getBytes(StandardCharsets.UTF_8)) : null);
        assertEquals(3, (int) d.slotSizes().get("necklace"));
        assertEquals(Set.of("anime_multiverse:a", "anime_multiverse:b"), d.tags().get("necklace"));
        assertEquals(Set.of("necklace"), d.assigned(), "a slot given only to another entity is not given to the player");
    }

    // ---------------------------------------------------------------- sync rules
    @Test
    void theServerLooksEveryTenTicksStaggeredAndAtOnceAfterALoginRespawnOrDimensionChange() {
        int hits = 0;
        for (int t = 0; t < 100; t++) if (WornSyncRules.looksNow(t, 3, false)) hits++;
        assertEquals(10, hits);
        assertTrue(WornSyncRules.looksNow(1, 1, true));
        assertFalse(WornSyncRules.forcesResend(WornSyncRules.Moment.TICK));
        for (WornSyncRules.Moment m : new WornSyncRules.Moment[]{WornSyncRules.Moment.LOGIN, WornSyncRules.Moment.RESPAWN, WornSyncRules.Moment.DIMENSION}) assertTrue(WornSyncRules.forcesResend(m), m.name());
    }

    @Test
    void aListGoesOutOnlyWhenItDiffersFromTheOneSentLast() {
        assertFalse(WornSyncRules.shouldSend("", ""));
        assertFalse(WornSyncRules.shouldSend(null, ""));
        assertTrue(WornSyncRules.shouldSend("", "a"));
        assertTrue(WornSyncRules.shouldSend("a", ""));
        assertFalse(WornSyncRules.shouldSend("a|b", "a|b"));
        assertFalse(WornSyncRules.shouldSend("a", null));
    }

    @Test
    void theFastLookIsShortWhileAScreenIsOpenAndRunsOutAfterIt() {
        assertEquals(WornSyncRules.FAST_TTL, WornSyncRules.lookTtl(true, 10));
        assertEquals(10, WornSyncRules.lookTtl(false, 10));
        assertEquals(1, WornSyncRules.lookTtl(true, 1), "never longer than the normal time");
        int hold = WornSyncRules.nextFastHold(true, 0);
        assertEquals(WornSyncRules.FAST_HOLD, hold);
        int ticks = 0;
        while (hold > 0) { hold = WornSyncRules.nextFastHold(false, hold); ticks++; }
        assertEquals(WornSyncRules.FAST_HOLD, ticks);
        assertFalse(WornSyncRules.keepListAfterLeave());
    }

    // ---------------------------------------------------------------- equip rules
    @Test
    void rightClickEquipsOnlyACuriosRelicNotWhileSneaking() {
        assertTrue(CurioEquipRules.tryOnUse(true, false, false));
        assertFalse(CurioEquipRules.tryOnUse(true, true, false), "sneak-use stays the ritual");
        assertFalse(CurioEquipRules.tryOnUse(false, false, false));
        assertFalse(CurioEquipRules.tryOnUse(true, false, true), "something already took the stack on this click");
    }

    @Test
    void theFirstFreePlaceOfTheRightSlotTypeIsPicked() {
        List<CurioEquipRules.Slot> slots = List.of(new CurioEquipRules.Slot("charm", List.of(false)), new CurioEquipRules.Slot("hands", List.of(false, true)),
                new CurioEquipRules.Slot("necklace", List.of(true)));
        assertEquals(new CurioEquipRules.Pick("hands", 1), CurioEquipRules.pick("hands", slots));
        assertEquals(new CurioEquipRules.Pick("necklace", 0), CurioEquipRules.pick("necklace", slots));
        assertNull(CurioEquipRules.pick("charm", slots), "full");
        assertNull(CurioEquipRules.pick("belt", slots), "the player has no such slot type");
        assertNull(CurioEquipRules.pick(null, slots));
    }

    // ---------------------------------------------------------------- once-only causes
    @Test
    void aCauseIsSaidOncePerRelicAndKeptForTheReport() {
        WornOnce.clear();
        assertTrue(WornOnce.first(WornOnce.Cause.NO_MODEL, "ring_a"));
        assertFalse(WornOnce.first(WornOnce.Cause.NO_MODEL, "ring_a"));
        assertTrue(WornOnce.first(WornOnce.Cause.NO_MODEL, "ring_b"));
        assertTrue(WornOnce.first(WornOnce.Cause.SYNCED_ONLY, "ring_a"), "another cause for the same relic is another line");
        assertEquals(3, WornOnce.count());
        assertTrue(WornOnce.causes().get(0).startsWith("ring_a has no worn model"));
        for (int i = 0; i < 500; i++) WornOnce.first(WornOnce.Cause.MODELS_OFF, "r" + i);
        assertTrue(WornOnce.count() <= WornOnce.MAX, "a flood of different causes cannot grow the table");
        WornOnce.clear();
        assertEquals(0, WornOnce.count());
        assertTrue(WornOnce.first(WornOnce.Cause.NO_MODEL, "ring_a"));
        WornOnce.clear();
    }
}
