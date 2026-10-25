package com.newuniverse.nusmp.entity.veldanava;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The three boons of the Will of the Creator: the choice is validated, the table is complete and bounded, and the menu names all three. */
class VeldanavaBoonsTest {
    @Test
    void onlyOneToThreeIsAChoice() {
        assertFalse(VeldanavaBoons.valid(0));
        assertFalse(VeldanavaBoons.valid(-1));
        assertFalse(VeldanavaBoons.valid(4));
        assertFalse(VeldanavaBoons.valid(Integer.MAX_VALUE));
        for (int i = 1; i <= VeldanavaBoons.COUNT; i++) assertTrue(VeldanavaBoons.valid(i));
        assertEquals(3, VeldanavaBoons.COUNT);
    }

    @Test
    void theTableHasThreeDistinctBoonsWithBoundedChanges() {
        List<VeldanavaBoons.Boon> all = VeldanavaBoons.all();
        assertEquals(3, all.size());
        Set<String> names = new HashSet<>();
        Set<Integer> ids = new HashSet<>();
        for (VeldanavaBoons.Boon b : all) {
            assertTrue(names.add(b.name()) && ids.add(b.id()));
            assertFalse(b.changes().isEmpty());
            assertEquals(b, VeldanavaBoons.boon(b.id()));
            for (VeldanavaBoons.Change c : b.changes()) {
                assertTrue(Set.of("max_health", "armor_toughness", "attack_damage", "movement_speed", "attack_speed", "armor", "luck").contains(c.attribute()), c.attribute());
                assertTrue(c.amount() > 0, "a boon never takes anything away");
                if (c.multiply()) assertTrue(c.amount() <= 0.25, "a speed boon stays under 25%");
                else assertTrue(c.amount() <= 24.0, "a flat boon stays under 24");
            }
        }
        assertTrue(names.contains("Magic King Solomon") && names.contains("Martial King Hercules") && names.contains("Wisdom King Raphael"));
        assertEquals(1, VeldanavaBoons.boon(0).id(), "an invalid number falls back to the first, it never throws");
    }

    @Test
    void theMenuNamesAllThreeAndMarksTheRecommendedOne() {
        List<String> magic = VeldanavaBoons.menu(true), other = VeldanavaBoons.menu(false);
        assertEquals(3, magic.size());
        assertEquals(3, other.size());
        assertTrue(magic.get(0).startsWith("* "));
        assertTrue(other.get(1).startsWith("* "));
        assertEquals(1, VeldanavaBoons.recommended(true));
        assertEquals(2, VeldanavaBoons.recommended(false));
        for (String line : magic) assertTrue(line.contains(":"));
    }
}
