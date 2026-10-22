package com.newuniverse.nusmp.audio.voice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the decisions behind the spoken lines are pure and must hold on a dedicated server with several players: the catalogue is complete and well formed (120 lines, four roles, ids that
 * carry their role), the picker always answers a line that exists (every boss style lands on a family, every Djinn name on its line, forged names on nothing), and the limiter keeps its
 * promises per player (a cooldown per line, a gap between lines, the per-second budget, nothing shared between two players).
 */
class VoiceLogicTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes());

    @Test
    void theCatalogueHasTheExpectedShape() {
        assertEquals(10 + 30 + 8 + 72 + VoiceCatalog.GNOME_LINES.size(), VoiceCatalog.ids().size());
        assertEquals(36, VoiceCatalog.DJINN_NAMES.size());
        assertEquals(List.of("system", "boss", "npc", "djinn", "gnome"), VoiceCatalog.roles());
        assertEquals(10, VoiceCatalog.byRole().get("system").size());
        assertEquals(30, VoiceCatalog.byRole().get("boss").size());
        assertEquals(8, VoiceCatalog.byRole().get("npc").size());
        assertEquals(72, VoiceCatalog.byRole().get("djinn").size());
        for (String id : VoiceCatalog.ids()) {
            assertTrue(id.matches("[a-z0-9_]+") && id.length() < VoiceNet.MAX_ID, id);
            assertEquals(id.substring(0, id.indexOf('_')), VoiceCatalog.role(id));
            assertEquals("voice." + id, VoiceCatalog.eventId(id));
            assertEquals("voice/" + VoiceCatalog.role(id) + "/" + id, VoiceCatalog.filePath(id));
        }
        assertFalse(VoiceCatalog.has(null));
        assertFalse(VoiceCatalog.has("system_nothing"));
        assertNull(VoiceCatalog.role("zzz"));
    }

    @Test
    void slugsMatchTheEnumNamesOfTheDjinn() {
        assertEquals("glasya_labolas", VoiceCatalog.slug("Glasya-Labolas"));
        assertEquals("glasya_labolas", VoiceCatalog.slug("GLASYA_LABOLAS"));
        assertEquals("baal", VoiceCatalog.slug(" Baal "));
        assertEquals("", VoiceCatalog.slug(null));
    }

    @Test
    void everyBossStyleFindsAFamilyAndEveryEventALine() {
        for (String style : com.newuniverse.nusmp.boss.bars.BarStyleIds.ALL) {
            String family = VoicePicker.bossFamily(style);
            assertTrue(VoiceCatalog.BOSS_FAMILIES.contains(family), style + " -> " + family);
            for (String ev : VoiceCatalog.BOSS_EVENTS) assertNotNull(VoicePicker.boss(style, ev), style + " " + ev);
        }
        assertEquals(VoicePicker.DEFAULT_FAMILY, VoicePicker.bossFamily(null));
        assertEquals(VoicePicker.DEFAULT_FAMILY, VoicePicker.bossFamily("not_a_style"));
        assertEquals("boss_ice_intro", VoicePicker.boss("ice", "intro"));
        assertEquals("boss_devil_death", VoicePicker.boss("zogratis", "death"));
        assertEquals("boss_undead_phase2", VoicePicker.boss("lich", "phase2"));
        assertNull(VoicePicker.boss("ice", "not_an_event"));
        assertEquals("phase2", VoicePicker.bossPhaseEvent(0, 1));
        assertNull(VoicePicker.bossPhaseEvent(2, 2));
        assertNull(VoicePicker.bossPhaseEvent(3, 1));
    }

    @Test
    void everyDjinnAndNpcAndSystemLineResolves() {
        for (String name : VoiceCatalog.DJINN_NAMES) for (String ev : VoiceCatalog.DJINN_EVENTS) assertEquals("djinn_" + name + "_" + ev, VoicePicker.djinn(name.toUpperCase(), ev));
        assertEquals("djinn_glasya_labolas_attack", VoicePicker.djinn("Glasya-Labolas", "attack"));
        assertNull(VoicePicker.djinn("Nobody", "summon"));
        assertNull(VoicePicker.djinn("Baal", "dance"));
        for (String s : VoiceCatalog.SYSTEM_LINES) assertEquals("system_" + s, VoicePicker.system(s));
        assertNull(VoicePicker.system("nothing"));
        for (VoicePicker.NpcAct act : VoicePicker.NpcAct.values()) {
            assertNotNull(VoicePicker.npc(act, true), act + " merchant");
            assertNotNull(VoicePicker.npc(act, false), act + " other");
        }
        assertEquals("npc_merchant_greet", VoicePicker.npc(VoicePicker.NpcAct.GREET, true));
        assertEquals("npc_quest_done", VoicePicker.npc(VoicePicker.NpcAct.QUEST_DONE, false));
    }

    @Test
    void aCastSpeaksOnlyOnce() {
        assertTrue(VoicePicker.firstAttackOfCast(-1, 5));
        assertFalse(VoicePicker.firstAttackOfCast(5, 5));
        assertTrue(VoicePicker.firstAttackOfCast(5, 6));
    }

    @Test
    void lowHealthNeedsAQuarterOrLessAndAnAlivePlayer() {
        assertTrue(VoicePicker.lowHealth(4f, 20f));
        assertTrue(VoicePicker.lowHealth(5f, 20f));
        assertFalse(VoicePicker.lowHealth(6f, 20f));
        assertFalse(VoicePicker.lowHealth(0f, 20f));
        assertFalse(VoicePicker.lowHealth(3f, 0f));
    }

    @Test
    void theSameLineWaitsForItsCooldownPerPlayer() {
        VoiceLimiter l = new VoiceLimiter(100);
        assertTrue(l.tryAcquire(A, "system_floor_enter", 0, 5));
        assertFalse(l.tryAcquire(A, "system_floor_enter", 99, 5));
        assertTrue(l.tryAcquire(A, "system_floor_enter", 100, 5));
        assertTrue(l.tryAcquire(B, "system_floor_enter", 101, 5), "another player is not held back by the first player's cooldown");
    }

    @Test
    void theBudgetPerSecondAndTheGapBetweenLinesHold() {
        VoiceLimiter l = new VoiceLimiter(0);
        assertTrue(l.tryAcquire(A, "a_1", 0, 2));
        assertFalse(l.tryAcquire(A, "a_2", 2, 2), "two lines never start on top of each other");
        assertTrue(l.tryAcquire(A, "a_2", 5, 2));
        assertFalse(l.tryAcquire(A, "a_3", 10, 2), "a third line inside the same second is held back");
        assertTrue(l.tryAcquire(A, "a_3", 25, 2), "the next second has its own budget");
        assertFalse(l.tryAcquire(A, "a_4", 60, 0), "maxPerSecond 0 switches the voices off");
        assertFalse(l.tryAcquire(null, "a_4", 60, 2));
        assertFalse(l.tryAcquire(A, null, 60, 2));
    }

    @Test
    void aCooldownOverrideAndTheCleanUpWork() {
        VoiceLimiter l = new VoiceLimiter(100);
        assertTrue(l.tryAcquire(A, "x", 0, 5, 20));
        assertFalse(l.tryAcquire(A, "x", 19, 5, 20));
        assertTrue(l.tryAcquire(A, "x", 20, 5, 20));
        assertEquals(1, l.tracked());
        l.forget(A);
        assertEquals(0, l.tracked());
        assertTrue(l.tryAcquire(A, "x", 21, 5, 20), "a forgotten player starts fresh");
        l.prune(100_000);
        assertEquals(0, l.tracked());
        l.tryAcquire(B, "y", 0, 5);
        l.clear();
        assertEquals(0, l.tracked());
    }
}
