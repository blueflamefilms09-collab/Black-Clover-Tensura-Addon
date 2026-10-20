package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: three small pure rules sit between the data and the game and a mistake in any of them is invisible until a boss fails to pay or a band of floors wears the wrong colour: the translation of a
 * boss profile into the Guardian's own D&D profile, the choice of the item a boss pays, and the themes the Underworld Root gives the floors after a gate.
 */
class DecennialGlueRulesTest {
    @Test
    void everyBossTranslatesIntoADndProfileTheGuardianCanRun() {
        DecennialRoster.loadShipped();
        for (DecennialProfile p : DecennialRoster.all()) {
            for (int phase = 1; phase <= p.phaseCount(); phase++) {
                DndProfile d = DecennialDnd.of(p, phase);
                assertTrue(DecennialDnd.isDecennial(d.id()), p.id());
                assertEquals(3, d.forms().size(), p.id());
                assertEquals(d.legendary().size(), d.costs().size(), p.id());
                assertTrue(d.lair().isEmpty(), "the hall's lair actions are the Decennial layer's own: " + p.id());
                assertFalse(d.mythic(), p.id() + " has phases, not a mythic third life");
                assertFalse(d.execution(), p.id());
                assertEquals(p.look(), d.look(), p.id());
                assertEquals(p.side(), d.side(), p.id());
                for (int c : d.costs()) assertTrue(c == 1 || c == 2, p.id());
            }
            assertTrue(DecennialDnd.of(p, 1).legendary().size() <= DecennialDnd.of(p, p.phaseCount()).legendary().size(), "a phase never takes a move away: " + p.id());
        }
        assertFalse(DecennialDnd.isDecennial("default"));
        assertFalse(DecennialDnd.isDecennial(null));
    }

    private static List<DecennialLoot.Candidate> fakeCatalog() {
        List<DecennialLoot.Candidate> pool = new ArrayList<>();
        for (int g = 0; g <= 6; g++) {
            pool.add(new DecennialLoot.Candidate("sword_" + g, "Plain Sword " + g, "weapon", "sword", g));
            pool.add(new DecennialLoot.Candidate("spear_" + g, "Dark Spear " + g, "weapon", "spear", g));
            pool.add(new DecennialLoot.Candidate("ring_" + g, "Gilded Ring " + g, "relic", "", g));
            pool.add(new DecennialLoot.Candidate("helm_" + g, "Iron Helm " + g, "armour", "", g));
            pool.add(new DecennialLoot.Candidate("potion_" + g, "Potion " + g, "", "", g));
        }
        return pool;
    }

    @Test
    void theItemAPaysIsOfTheLevelsGradeAndKindAndNeverOneAlreadyHeld() {
        DecennialRoster.loadShipped();
        List<DecennialLoot.Candidate> pool = fakeCatalog();
        for (DecennialProfile p : DecennialRoster.all()) {
            DecennialLoot.Candidate c = DecennialLoot.pick(p, pool, Set.of());
            assertNotNull(c, p.id());
            assertEquals(DecennialLoot.gradeOrdinal(p.loot().grade()), c.grade(), p.id());
            assertFalse(c.kind().isEmpty(), p.id() + " never pays a consumable");
            assertEquals(p.loot().kind(), c.kind(), p.id());
            Set<String> taken = new HashSet<>();
            taken.add(c.id());
            DecennialLoot.Candidate second = DecennialLoot.pick(p, pool, taken);
            assertNotNull(second, p.id());
            assertNotEquals(c.id(), second.id());
            assertEquals(c.id(), DecennialLoot.pick(p, pool, Set.of()).id(), "the same boss makes the same choice every time: " + p.id());
        }
    }

    @Test
    void aGradeWithNothingLeftPaysNothingAndAKeywordWins() {
        DecennialRoster.loadShipped();
        DecennialProfile p = DecennialRoster.forLevel(-50);
        List<DecennialLoot.Candidate> only = new ArrayList<>();
        only.add(new DecennialLoot.Candidate("wrong_grade", "Spare", p.loot().kind(), p.loot().type(), 0));
        assertNull(DecennialLoot.pick(p, only, Set.of()));
        assertNull(DecennialLoot.pick(p, fakeCatalog(), allIds(fakeCatalog())));
        List<DecennialLoot.Candidate> two = new ArrayList<>();
        int g = DecennialLoot.gradeOrdinal(p.loot().grade());
        two.add(new DecennialLoot.Candidate("aaa", "Plain", p.loot().kind(), p.loot().type(), g));
        String key = p.loot().keywords().isEmpty() ? "zzkey" : p.loot().keywords().get(0);
        two.add(new DecennialLoot.Candidate("bbb", "Item of " + key, p.loot().kind(), p.loot().type(), g));
        if (!p.loot().keywords().isEmpty()) assertEquals("bbb", DecennialLoot.pick(p, two, Set.of()).id());
        assertEquals("weapon", DecennialLoot.kindOfSlot("MAIN_HAND"));
        assertEquals("armour", DecennialLoot.kindOfSlot("CHEST"));
        assertEquals("relic", DecennialLoot.kindOfSlot("NECK"));
        assertEquals("", DecennialLoot.kindOfSlot("CONSUMABLE"));
    }

    private static Set<String> allIds(List<DecennialLoot.Candidate> pool) {
        Set<String> s = new HashSet<>();
        for (DecennialLoot.Candidate c : pool) s.add(c.id());
        return s;
    }

    @Test
    void theUnderworldRootDarkensTheFloorsPastEachGateAndOnlyOnTheAbyssalRoad() {
        for (String id : UnderworldRoot.allThemeIds()) assertEquals(id, RegionTheme.fromId(id).id(), id + " is a real region theme");
        assertNull(UnderworldRoot.themeIdAfter(100), "the Holy road is untouched");
        assertNull(UnderworldRoot.themeIdAfter(0));
        assertNull(UnderworldRoot.themeIdAfter(-45), "not a boss floor");
        assertNull(UnderworldRoot.themeIdAfter(-10), "the first band is the ordinary Abyss");
        assertNull(UnderworldRoot.themeIdAfter(-130));
        assertNotNull(UnderworldRoot.themeIdAfter(-140), "after the first gate the floors change");
        assertNotNull(UnderworldRoot.themeIdAfter(-1000), "even after the throne the table answers (the last level is clamped)");
        for (int level = 14; level <= 100; level++) {
            String id = UnderworldRoot.themeIdAfter(-10 * level);
            assertNotNull(id, "level " + level);
            assertTrue(UnderworldRoot.allThemeIds().contains(id));
        }
    }
}
