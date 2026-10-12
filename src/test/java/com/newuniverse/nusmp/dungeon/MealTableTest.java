package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What each cooked monster meal adds beside its buff, and how the tooltip and the message on eating word it. */
class MealTableTest {
    @Test
    void everyCookedMealButThePlainOneHasAPerk() {
        Set<String> meals = new HashSet<>();
        for (MealTable.Perk p : MealTable.ALL) meals.add(p.meal());
        assertEquals(Set.of("candied_slime_jelly", "grilled_armor_flank", "roast_basilisk_fillet", "roast_direwolf_haunch"), meals);
        assertEquals(4, MealTable.ALL.size(), "no meal appears twice");
        assertNull(MealTable.forMeal("seared_monster_meat"), "the plain meal stays plain");
        assertNull(MealTable.forMeal("raw_monster_meat"));
        assertSame(MealTable.CANDIED_SLIME_JELLY, MealTable.forMeal("candied_slime_jelly"));
        assertSame(MealTable.ROAST_DIREWOLF_HAUNCH, MealTable.forMeal("roast_direwolf_haunch"));
    }

    @Test
    void theSlimeJellyLendsMagicSense() {
        MealTable.Perk p = MealTable.CANDIED_SLIME_JELLY;
        assertEquals("tensura:magic_sense", p.skill());
        assertEquals(6000, p.skillTicks());
        assertEquals(6000, p.magicTicks());
    }

    @Test
    void skillsAreTensuraIdsAndMagicsAreEnumNames() {
        Set<String> skills = new HashSet<>(), magics = new HashSet<>();
        for (MealTable.Perk p : MealTable.ALL) {
            assertTrue(p.skill().matches("tensura:[a-z_]+"), p.skill());
            assertTrue(p.magic().matches("[A-Z_]+"), p.magic());
            assertFalse(p.skillName().isBlank());
            assertTrue(p.skillTicks() > 0 && p.magicTicks() > 0, p.meal());
            skills.add(p.skill());
            magics.add(p.magic());
        }
        assertEquals(4, skills.size(), "each meal lends its own skill");
        assertEquals(4, magics.size(), "and attunes to its own magic");
    }

    @Test
    void durationsReadAsMinutesOrSeconds() {
        assertEquals("5 min", MealTable.duration(6000));
        assertEquals("4 min", MealTable.duration(4800));
        assertEquals("3 min", MealTable.duration(3600));
        assertEquals("1 min", MealTable.duration(1200));
        assertEquals("45 s", MealTable.duration(900));
        assertEquals("5 s", MealTable.duration(100));
        assertEquals("0 s", MealTable.duration(0));
        assertEquals("0 s", MealTable.duration(-40));
        assertEquals("90 s", MealTable.duration(1800));
    }

    @Test
    void everyRiderThatDoesSomethingIsDescribed() {
        for (String hit : List.of("BURN", "FREEZE", "SHOCK", "SLOW", "POISON", "WITHER", "BLIND", "LEVITATE", "PULL", "PUSH", "WEAKEN", "DRAIN"))
            assertNotEquals(MealTable.verb("PIERCE"), MealTable.verb(hit), hit);
        assertEquals("slow what they hit", MealTable.verb("SLOW"));
        assertEquals(MealTable.verb("PUSH"), MealTable.verb("PULL"));
        assertEquals("carry its touch", MealTable.verb("NULLIFY"));
    }

    @Test
    void theLinesReadAsSentences() {
        assertEquals("Attuned to Gel Magic (5 min): your spells also slow what they hit", MealTable.attuneLine("Gel Magic", "SLOW", 6000));
        assertEquals("Lends Magic Sense (5 min)", MealTable.skillLine("Magic Sense", 6000));
        assertEquals("Gel Magic: your spells also slow what they hit. Magic Sense lent.", MealTable.eatLine("Gel Magic", "SLOW", "Magic Sense"));
        assertEquals("Blood Magic: your spells also give a little life back to you.", MealTable.eatLine("Blood Magic", "DRAIN", null));
        assertEquals("Danger Sense lent.", MealTable.eatLine(null, null, "Danger Sense"));
        assertEquals("", MealTable.eatLine(null, null, null));
    }

    @Test
    void theMessageOnEatingFitsTheActionBar() {
        Map<String, String> label = Map.of("GEL", "Gel Magic", "IRON", "Iron Magic", "POISON", "Poison Magic", "BLOOD", "Blood Magic");
        Map<String, String> hit = Map.of("GEL", "SLOW", "IRON", "PUSH", "POISON", "POISON", "BLOOD", "DRAIN");
        for (MealTable.Perk p : MealTable.ALL) {
            String line = MealTable.eatLine(label.get(p.magic()), hit.get(p.magic()), p.skillName());
            assertTrue(line.length() <= 100, line);
        }
    }
}
