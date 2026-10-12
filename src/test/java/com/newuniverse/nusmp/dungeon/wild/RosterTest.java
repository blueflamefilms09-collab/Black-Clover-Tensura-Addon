package com.newuniverse.nusmp.dungeon.wild;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Living Dungeon's roster: tier arithmetic and weighted choosing (docs/living_dungeon.md section 1), without the game. */
class RosterTest {
    private static Roster.Entry e(String id, int tier) { return new Roster.Entry(id, tier, false, false, false); }

    private static Roster.Entry water(String id, int tier) { return new Roster.Entry(id, tier, false, true, false); }

    private static Roster sample() {
        List<Roster.Entry> l = new ArrayList<>();
        l.add(new Roster.Entry("iceandfire:fire_dragon", 3, true, false, false));
        l.add(new Roster.Entry("iceandfire:ice_dragon", 3, true, false, false));
        l.add(new Roster.Entry("iceandfire:lightning_dragon", 3, true, false, false));
        l.add(e("tensura:direwolf", 3));
        l.add(e("tensura:basilisk", 3));
        l.add(water("tensura:megalodon", 3));
        l.add(e("tensura:armorsaurus", 2));
        l.add(e("iceandfire:troll", 2));
        l.add(e("tensura:slime", 1));
        l.add(e("tensura:horned_rabbit", 1));
        l.add(e("tensura:salamander", 2));
        return new Roster(l);
    }

    @Test
    void tierComesFromTheTagsAndTheVetoWins() {
        assertEquals(0, Roster.tierFor(true, true, true, true, true), "excluded beats every list");
        assertEquals(3, Roster.tierFor(false, false, false, true, false), "legendary counts as 3");
        assertEquals(3, Roster.tierFor(true, true, true, false, false), "the highest listed tier wins");
        assertEquals(2, Roster.tierFor(true, true, false, false, false));
        assertEquals(1, Roster.tierFor(true, false, false, false, false));
        assertEquals(0, Roster.tierFor(false, false, false, false, false), "in no list = not a member");
    }

    @Test
    void countsAndLookups() {
        Roster r = sample();
        assertEquals(3, r.count(3), "legendary entries are counted apart");
        assertEquals(3, r.legendCount());
        assertEquals(3, r.count(2));
        assertEquals(2, r.count(1));
        assertEquals(7, r.countNamespace("tensura"));
        assertEquals(4, r.countNamespace("iceandfire"));
        assertTrue(r.hasAquatic(3));
        assertFalse(r.hasAquatic(2));
        assertTrue(r.contains(2, "tensura:armorsaurus"));
        assertFalse(r.contains(1, "tensura:armorsaurus"));
        assertFalse(r.contains(3, "iceandfire:fire_dragon"), "a legendary is not an ordinary member");
    }

    @Test
    void pickIsDeterministicAndRespectsTierAndWater() {
        Roster r = sample();
        for (double roll = 0; roll < 1.0; roll += 0.01) {
            Roster.Entry a = r.pick(3, Roster.UPPER, false, roll);
            assertEquals(a, r.pick(3, Roster.UPPER, false, roll), "same roll, same creature");
            assertEquals(3, a.tier());
            assertFalse(a.aquatic(), "land births never pick a sea creature");
            assertFalse(a.legendary(), "ordinary births never pick a dragon");
            Roster.Entry w = r.pick(3, Roster.DEEP, true, roll);
            assertEquals("tensura:megalodon", w.id(), "the only sea creature of tier 3");
        }
        assertNull(r.pick(2, Roster.UPPER, true, 0.5), "tier 2 has nothing that lives in water");
        assertNull(new Roster(List.of()).pick(1, 0, false, 0.5));
    }

    @Test
    void rollsOutsideTheRangeStillPick() {
        Roster r = sample();
        assertNotNull(r.pick(1, -1, false, -3.0));
        assertNotNull(r.pick(1, -1, false, 1.0));
        assertNotNull(r.pick(1, -1, false, 7.0));
    }

    @Test
    void commonThingsAreCommonAndAHomeTierIsFavoured() {
        Roster r = sample();
        int slimes = 0, rabbits = 0;
        for (int i = 0; i < 1000; i++) {
            String id = r.pick(1, -1, false, i / 1000.0).id();
            if (id.equals("tensura:slime")) slimes++; else rabbits++;
        }
        assertTrue(slimes > rabbits, "slime weighs 30 against 14: " + slimes + " vs " + rabbits);
        int atHome = 0, away = 0;
        for (int i = 0; i < 1000; i++) {
            if (r.pick(2, Roster.VENTS, false, i / 1000.0).id().equals("tensura:salamander")) atHome++;
            if (r.pick(2, Roster.UPPER, false, i / 1000.0).id().equals("tensura:salamander")) away++;
        }
        assertTrue(atHome > away * 2, "a salamander is likelier in the Volcanic Vents: " + atHome + " vs " + away);
        assertTrue(away > 0, "but it is not forbidden elsewhere");
    }

    @Test
    void weightTable() {
        assertEquals(10, Roster.baseWeight("something_from_a_modpack"));
        assertTrue(Roster.baseWeight("slime") > Roster.baseWeight("direwolf"));
        assertTrue(Roster.baseWeight("direwolf") > Roster.baseWeight("sissie"));
        assertEquals(-1, Roster.affinity("armorsaurus"));
        assertEquals(Roster.VENTS, Roster.affinity("salamander"));
        assertEquals(Roster.DEEP, Roster.affinity("sea_serpent"));
        Roster.Entry s = e("tensura:salamander", 2);
        assertEquals(5.0 * 4.0, Roster.weightAt(s, Roster.VENTS), 1e-9);
        assertEquals(5.0 * 0.5, Roster.weightAt(s, Roster.CITY), 1e-9);
        assertEquals(5.0, Roster.weightAt(s, -1), 1e-9);
    }

    @Test
    void legendsLiveWhereTheirElementDoes() {
        assertEquals(Roster.VENTS, Roster.legendHome("fire_dragon"));
        assertEquals(Roster.DEEP, Roster.legendHome("ice_dragon"));
        assertEquals(Roster.CITY, Roster.legendHome("lightning_dragon"));
        assertEquals(Roster.CITY, Roster.legendHome("a_dragon_from_a_modpack"));
        Roster r = sample();
        boolean fire = false, ice = false, lightning = false;
        for (int i = 0; i < 30; i++) {
            Roster.Entry l = r.pickLegend(i / 30.0);
            assertTrue(l.legendary());
            fire |= l.path().equals("fire_dragon");
            ice |= l.path().equals("ice_dragon");
            lightning |= l.path().equals("lightning_dragon");
        }
        assertTrue(fire && ice && lightning, "every dragon can be chosen");
        assertNull(new Roster(List.of(e("tensura:slime", 1))).pickLegend(0.3));
    }

    @Test
    void entryPartsOfTheId() {
        Roster.Entry x = e("iceandfire:troll", 2);
        assertEquals("iceandfire", x.namespace());
        assertEquals("troll", x.path());
        assertEquals("minecraft", e("pig", 1).namespace());
        assertEquals("pig", e("pig", 1).path());
    }
}
