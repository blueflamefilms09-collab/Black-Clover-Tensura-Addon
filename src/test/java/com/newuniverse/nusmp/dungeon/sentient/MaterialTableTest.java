package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The rare synthesis materials: four in a hundred a kill, two more per Looting level, one item per kind of mob. */
class MaterialTableTest {
    @Test
    void theMappingIsTheOwnersList() {
        assertEquals("goblin_dagger", MaterialTable.materialFor("goblin", false));
        assertEquals("kobold_fang", MaterialTable.materialFor("hound_dog", false));
        assertEquals("slime_core", MaterialTable.materialFor("slime", false));
        assertEquals("aqua_slime_gel", MaterialTable.materialFor("slime", true));
        assertEquals("direwolf_pelt", MaterialTable.materialFor("direwolf", false));
        assertEquals("lizardman_scale", MaterialTable.materialFor("lizardman", false));
        assertEquals("water_spirit_essence", MaterialTable.materialFor("undine", false));
        assertEquals("megalodon_tooth", MaterialTable.materialFor("megalodon", false));
        assertEquals("devil_horn", MaterialTable.materialFor("lesser_daemon", false));
        assertEquals("disciple_sigil", MaterialTable.materialFor("dark_disciple", false));
        for (String bear : new String[]{"giant_bear", "horned_bear", "orc", "war_gnome"}) assertEquals("beast_hide", MaterialTable.materialFor(bear, false), bear);
        assertNull(MaterialTable.materialFor("basilisk", false));
        assertNull(MaterialTable.materialFor("", false));
        assertNull(MaterialTable.materialFor("zombie", true), "only a slime can be an aqua slime");
    }

    @Test
    void everyMaterialIsAKnownItemAndEveryItemIsDropped() {
        Set<String> all = new HashSet<>(java.util.List.of(MaterialTable.ALL));
        assertEquals(12, MaterialTable.ALL.length);
        assertEquals(12, all.size(), "no id twice");
        Set<String> reached = new HashSet<>();
        for (String mob : new String[]{"goblin", "hound_dog", "slime", "direwolf", "lizardman", "undine", "megalodon", "lesser_daemon", "dark_disciple", "giant_bear"}) {
            String m = MaterialTable.materialFor(mob, false);
            assertTrue(all.contains(m), mob + " -> " + m);
            reached.add(m);
        }
        reached.add(MaterialTable.materialFor("slime", true));
        reached.add(MaterialTable.HEART);
        assertEquals(all, reached, "every material has a source");
    }

    @Test
    void theChanceIsFourPercentPlusTwoPerLooting() {
        assertEquals(0.04, MaterialTable.chance(0), 1e-9);
        assertEquals(0.06, MaterialTable.chance(1), 1e-9);
        assertEquals(0.10, MaterialTable.chance(3), 1e-9);
        assertEquals(0.04, MaterialTable.chance(-2), 1e-9);
        assertTrue(MaterialTable.chance(1000) <= MaterialTable.CEILING);
    }

    @Test
    void theRatesHoldOverManyKills() {
        for (int looting = 0; looting <= 3; looting++) {
            Dice d = new Random(40 + looting)::nextDouble;
            int n = 60000, hits = 0;
            for (int i = 0; i < n; i++) {
                String m = MaterialTable.roll("goblin", false, looting, d);
                if (m != null) {
                    assertEquals("goblin_dagger", m);
                    hits++;
                }
            }
            assertEquals(MaterialTable.chance(looting), hits / (double) n, 0.006, "looting " + looting);
        }
    }

    @Test
    void aMobWithNoMaterialNeverLeavesOne() {
        Dice d = new Random(5)::nextDouble;
        for (int i = 0; i < 5000; i++) assertNull(MaterialTable.roll("basilisk", false, 3, d));
    }

    @Test
    void everyMaterialSaysWhereItComesFrom() {
        for (String m : MaterialTable.ALL) {
            assertNotNull(MaterialTable.source(m));
            assertNotEquals("the Dungeon", MaterialTable.source(m), m);
        }
        assertEquals("the Dungeon", MaterialTable.source("something_else"));
    }
}
