package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The relics and what names them: the enum (its ordinals are bits of a mask and part of saved worlds), the pedestal's property enum, and the
 * datapack that gives them exactly two Curios slots (docs/living_dungeon.md section 7).
 */
class RelicEnumTest {
    @Test
    void theOrdinalsOfTheOldFiveAreUnchangedAndTheAmuletIsAppended() {
        assertEquals(0, Relic.GLUTTONS_CHALICE.ordinal());
        assertEquals(1, Relic.MANTLE_OF_THE_SCAVENGER.ordinal());
        assertEquals(2, Relic.BAND_OF_THE_SPATIAL_ANCHOR.ordinal());
        assertEquals(3, Relic.DEVIL_BOUND_SHEATH.ordinal());
        assertEquals(4, Relic.GAMBLERS_GRIMOIRE_PAGE.ordinal());
        assertEquals(5, Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS.ordinal());
        assertEquals(6, Relic.values().length, "six relics: add new ones at the end");
        assertEquals(1 << 5, Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS.bit());
    }

    @Test
    void theAmuletHasItsContractNames() {
        Relic a = Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS;
        assertEquals("amulet_of_the_juggernauts_blindness", a.id);
        assertEquals("Amulet of the Juggernaut's Blindness", a.label);
        assertEquals(Relic.Slot.RELIC, a.slot);
        assertEquals("Muffled Mana", a.curse);
        assertSame(a, Relic.byName("amulet_of_the_juggernauts_blindness"));
        assertSame(a, Relic.byName("nusmp:amulet_of_the_juggernauts_blindness"));
        assertSame(a, Relic.byName("AMULET_OF_THE_JUGGERNAUTS_BLINDNESS"));
        assertTrue(Relic.ids().contains("amulet_of_the_juggernauts_blindness"), "the give command suggests it");
    }

    @Test
    void theRelicSlotIsTheLastSlotKindAndEveryRelicUsesIt() {
        Relic.Slot[] slots = Relic.Slot.values();
        assertEquals(Relic.Slot.RELIC, slots[slots.length - 1]);
        assertEquals("dungeon_relic", Relic.Slot.RELIC.label);
        assertEquals(Relic.Slot.OFF_HAND, slots[4], "the older entries keep their places");
        for (Relic r : Relic.values()) assertEquals(Relic.Slot.RELIC, r.slot, r.name());
    }

    @Test
    void everyRelicHasAnItemIdAndAPedestalValueAndTheOtherWayRound() {
        Set<String> ids = new HashSet<>();
        for (Relic r : Relic.values()) {
            assertNotNull(r.id);
            assertFalse(r.id.isEmpty(), r.name());
            assertTrue(r.id.matches("[a-z0-9_]+"), "a legal registry path: " + r.id);
            assertTrue(ids.add(r.id), "no two relics share an id: " + r.id);
            RelicSlot s = RelicSlot.of(r);
            assertNotEquals(RelicSlot.EMPTY, s, r.name() + " needs a pedestal value");
            assertSame(r, s.relic());
            assertEquals(r.id, s.getSerializedName(), "the pedestal names the relic by its item id");
        }
        for (RelicSlot s : RelicSlot.values()) {
            if (s == RelicSlot.EMPTY) {
                assertNull(s.relic());
                assertEquals("empty", s.getSerializedName());
            } else {
                assertNotNull(s.relic(), s.name());
            }
        }
        assertEquals(Relic.values().length + 1, RelicSlot.values().length, "every relic plus the empty pedestal");
        assertSame(RelicSlot.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS, RelicSlot.values()[RelicSlot.values().length - 1], "the new value is the last one");
    }

    @Test
    void theTreasuryHoldsTheFourRelicsOfTheDesignInItsOrder() {
        Relic[] t = Relic.treasury();
        assertEquals(4, t.length);
        assertArrayEquals(new Relic[]{Relic.BAND_OF_THE_SPATIAL_ANCHOR, Relic.DEVIL_BOUND_SHEATH, Relic.GAMBLERS_GRIMOIRE_PAGE, Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS}, t);
        assertEquals(4, EnumSet.of(t[0], t[1], t[2], t[3]).size(), "four distinct relics");
        for (Relic r : t) assertNotEquals(RelicSlot.EMPTY, RelicSlot.of(r), r.name());
        Set<Relic> left = EnumSet.allOf(Relic.class);
        left.removeAll(Set.of(t));
        assertEquals(EnumSet.of(Relic.GLUTTONS_CHALICE, Relic.MANTLE_OF_THE_SCAVENGER), left, "only the two of the removed food web are kept out");
    }

    @Test
    void theTreasuryIsACopyNobodyCanChange() {
        Relic[] t = Relic.treasury();
        t[0] = Relic.GLUTTONS_CHALICE;
        assertSame(Relic.BAND_OF_THE_SPATIAL_ANCHOR, Relic.treasury()[0]);
        Relic[] old = Relic.legacyTreasury();
        old[0] = Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS;
        assertSame(Relic.GLUTTONS_CHALICE, Relic.legacyTreasury()[0]);
    }

    @Test
    void withTheSentientRulesOffTheTreasuriesHoldWhatTheyAlwaysDid() {
        assertArrayEquals(new Relic[]{Relic.GLUTTONS_CHALICE, Relic.MANTLE_OF_THE_SCAVENGER, Relic.BAND_OF_THE_SPATIAL_ANCHOR, Relic.DEVIL_BOUND_SHEATH,
                Relic.GAMBLERS_GRIMOIRE_PAGE}, Relic.legacyTreasury(), "the five of 1.21.0 in their old order");
        assertArrayEquals(Relic.treasury(), Relic.pool(true));
        assertArrayEquals(Relic.legacyTreasury(), Relic.pool(false));
        for (Relic r : Relic.legacyTreasury()) assertTrue(r.ordinal() < 5, "no relic newer than 1.21.0 in the old pool: " + r.name());
        assertEquals(5, Relic.legacyTreasury().length);
    }

    // ---------------------------------------------------------------- the datapack: exactly two relic slots, and no other slot takes a relic
    private static String resource(String path) throws IOException {
        try (InputStream in = RelicEnumTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String compact(String json) { return json.replaceAll("\\s+", ""); }

    @Test
    void theSlotDataGivesExactlyTwoDungeonRelicSlots() throws IOException {
        String slot = compact(resource("/data/nusmp/curios/slots/dungeon_relic.json"));
        assertTrue(slot.contains("\"size\":2"), slot);
        assertTrue(slot.contains("\"validators\":[\"curios:tag\"]"), slot);
        assertTrue(slot.contains("\"icon\":\"curios:slot/empty_curio_slot\""), slot);
        // Worn counts the first two relics in Curios order (slot order, ties by id); the generic "curio" slot is order 20 and sorts before
        // "dungeon_relic" on a tie, so the relic slots must be strictly lower for them to win the two places.
        assertTrue(slot.contains("\"order\":10"), slot);
        String entity = compact(resource("/data/nusmp/curios/entities/dungeon_relic.json"));
        assertTrue(entity.contains("\"entities\":[\"player\"]"), entity);
        assertTrue(entity.contains("\"slots\":[\"dungeon_relic\"]"), entity);
    }

    @Test
    void theDungeonRelicTagListsEveryRelicAndNothingElse() throws IOException {
        String tag = compact(resource("/data/curios/tags/item/dungeon_relic.json"));
        for (Relic r : Relic.values()) assertTrue(tag.contains("\"nusmp:" + r.id + "\""), "the dungeon_relic tag lists " + r.id);
        assertEquals(Relic.values().length, tag.split("nusmp:", -1).length - 1, "six items and no others: " + tag);
    }

    @Test
    void noOtherCuriosSlotTakesARelic() throws IOException {
        for (String slot : new String[]{"ring", "charm", "back", "belt", "curio", "hands"}) {
            String tag = resource("/data/curios/tags/item/" + slot + ".json");
            for (Relic r : Relic.values()) assertFalse(tag.contains("nusmp:" + r.id + "\""), r.id + " must not fit the " + slot + " slot");
        }
    }

    @Test
    void theWeaponsKeepTheirSlots() throws IOException {
        assertTrue(resource("/data/curios/tags/item/belt.json").contains("nusmp:demon_slayer_sword"));
        assertTrue(resource("/data/curios/tags/item/back.json").contains("nusmp:severing_greatsword"));
        assertTrue(resource("/data/curios/tags/item/charm.json").contains("nusmp:brooch_pin"));
        assertTrue(resource("/data/curios/tags/item/hands.json").contains("nusmp:hand_of_providence"));
    }
}
