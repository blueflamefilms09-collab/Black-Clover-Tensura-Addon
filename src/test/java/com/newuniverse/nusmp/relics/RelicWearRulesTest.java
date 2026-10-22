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
                Map.of("necklace", 1, "back", 1, "belt", 1, "hands", 2, "charm", 1, "dungeon_relic", 2, "celestial_key", 12),
                Map.of("necklace", Set.of("nusmp:a"), "back", Set.of("nusmp:b"), "belt", Set.of("nusmp:c"), "hands", Set.of("nusmp:d"), "charm", Set.of("nusmp:e"),
                        "dungeon_relic", Set.of(), "celestial_key", Set.of()),
                Set.of("necklace", "back", "belt", "hands", "charm", "dungeon_relic", "celestial_key"));
    }

    @Test
    void completeDataHasNoProblem() {
        assertEquals(List.of(), RelicWear.checkSlots(complete()));
        assertEquals(List.of(), RelicWear.checkRelics(Map.of("nusmp:a", "NECK", "nusmp:b", "BACK", "nusmp:c", "BELT", "nusmp:d", "HANDS", "nusmp:e", "CHARM",
                "nusmp:h", "HEAD", "nusmp:c1", "CHEST", "nusmp:f", "FEET", "nusmp:w", "MAIN_HAND", "nusmp:p", "CONSUMABLE"), complete()));
    }

    @Test
    void aSlotTheModDoesNotDefineIsNamedEvenWhenCuriosIsKnownToShipIt() {
        RelicWear.Data d = new RelicWear.Data(Map.of("back", 1), Map.of("necklace", Set.of("nusmp:a")), Set.of("necklace"));
        List<String> p = RelicWear.checkRelics(Map.of("nusmp:a", "NECK"), d);
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
        assertTrue(RelicWear.checkRelics(Map.of("nusmp:a", "NECK"), noTag).get(0).contains("not listed in data/curios/tags/item/necklace.json"));
        RelicWear.Data noEntity = new RelicWear.Data(Map.of("necklace", 1), Map.of("necklace", Set.of("nusmp:a")), Set.of());
        assertTrue(RelicWear.checkRelics(Map.of("nusmp:a", "NECK"), noEntity).get(0).contains("not given to the player"));
        RelicWear.Data zero = new RelicWear.Data(Map.of("necklace", 0), Map.of("necklace", Set.of("nusmp:a")), Set.of("necklace"));
        assertTrue(RelicWear.checkRelics(Map.of("nusmp:a", "NECK"), zero).get(0).contains("not defined by this mod"));
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
                "data/nusmp/curios/slots/necklace.json", "{\"size\": 3}",
                "data/curios/tags/item/necklace.json", "{\"values\": [\"nusmp:a\", {\"id\": \"nusmp:b\", \"required\": false}]}",
                "data/nusmp/curios/entities/relics.json", "{\"entities\": [\"player\"], \"slots\": [\"necklace\"]}",
                "data/nusmp/curios/entities/dungeon_relic.json", "{\"entities\": [\"zombie\"], \"slots\": [\"dungeon_relic\"]}");
        RelicWear.Data d = RelicWear.read(path -> files.containsKey(path) ? new ByteArrayInputStream(files.get(path).getBytes(StandardCharsets.UTF_8)) : null);
        assertEquals(3, (int) d.slotSizes().get("necklace"));
        assertEquals(Set.of("nusmp:a", "nusmp:b"), d.tags().get("necklace"));
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
