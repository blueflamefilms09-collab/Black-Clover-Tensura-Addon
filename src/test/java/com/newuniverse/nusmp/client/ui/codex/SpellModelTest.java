package com.newuniverse.nusmp.client.ui.codex;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The learned-spells tab: grouping by magic, sorting, de-duplication, sealed counts and the fallback lines. */
class SpellModelTest {
    private static SpellModel.Spell s(String magic, String label, String page, String name, int mode, int cost, int cd, int mastery) {
        return new SpellModel.Spell(magic, label, page, name, mode, cost, cd, mastery, "nusmp.page." + magic + "." + page);
    }

    @Test
    void groupsByMagicAlphabeticallyAndSpellsInPageOrder() {
        List<SpellModel.Spell> in = List.of(s("WATER", "Water Magic", "wave", "Wave", 2, 12, 8, 40), s("FLAME", "Flame Magic", "ball", "Fire Ball", 1, 8, 4, 20),
                s("WATER", "Water Magic", "drop", "Drop", 0, 0, 0, 0), s("FLAME", "Flame Magic", "bolt", "Bolt", 0, 5, 2, 0));
        List<SpellModel.Group> g = SpellModel.group(in, Map.of());
        assertEquals(2, g.size());
        assertEquals("Flame Magic", g.get(0).magicName());
        assertEquals("Water Magic", g.get(1).magicName());
        assertEquals("Bolt", g.get(0).spells().get(0).name());
        assertEquals("Fire Ball", g.get(0).spells().get(1).name());
        assertEquals("Drop", g.get(1).spells().get(0).name());
    }

    @Test
    void samePageFromTwoSourcesCountsOnce() {
        List<SpellModel.Spell> in = List.of(s("FLAME", "Flame Magic", "bolt", "Bolt", 0, 5, 2, 0), s("FLAME", "Flame Magic", "bolt", "Bolt (again)", 0, 5, 2, 0));
        List<SpellModel.Group> g = SpellModel.group(in, null);
        assertEquals(1, g.get(0).spells().size());
        assertEquals("Bolt", g.get(0).spells().get(0).name());
    }

    @Test
    void sealedPagesAreOnlyCountedAndAMagicWithOnlySealedPagesStillShows() {
        List<SpellModel.Spell> in = List.of(s("FLAME", "Flame Magic", "bolt", "Bolt", 0, 5, 2, 0));
        List<SpellModel.Group> g = SpellModel.group(in, Map.of("FLAME", 3, "ICE", 2, "WIND", 0));
        assertEquals(2, g.size());
        assertEquals(3, g.get(0).sealed());
        assertEquals("ICE", g.get(1).magicName());
        assertEquals(2, g.get(1).sealed());
        assertEquals(0, g.get(1).spells().size());
    }

    @Test
    void emptyInputGivesNoGroups() {
        assertTrue(SpellModel.group(new ArrayList<>(), Map.of()).isEmpty());
        assertTrue(SpellModel.group(List.of(), null).isEmpty());
    }

    @Test
    void brokenSpellsAreSkipped() {
        List<SpellModel.Spell> in = new ArrayList<>();
        in.add(null);
        in.add(new SpellModel.Spell(null, "x", "p", "n", 0, 0, 0, 0, null));
        in.add(new SpellModel.Spell("A", "x", null, "n", 0, 0, 0, 0, null));
        assertTrue(SpellModel.group(in, null).isEmpty());
    }

    @Test
    void aMagicWithoutALabelUsesItsId() {
        List<SpellModel.Group> g = SpellModel.group(List.of(s("merged:flame+water", "", "x", "X", 0, 1, 1, 0)), null);
        assertEquals("merged:flame+water", g.get(0).magicName());
    }

    @Test
    void generatedLinesAndStats() {
        SpellModel.Spell a = s("F", "F", "p", "Fire Ball", 1, 12, 8, 40);
        assertEquals("Fire Ball costs 12% of your magicule and rests for 8 s.", SpellModel.generatedLine(a));
        assertEquals("Cost 12%  Cooldown 8 s  Opens at 40% mastery", SpellModel.statsLine(a));
        SpellModel.Spell free = s("F", "F", "p", "Summon", 5, 0, 0, 0);
        assertEquals("Summon is free to use and needs no rest.", SpellModel.generatedLine(free));
        assertEquals("", SpellModel.statsLine(free));
        SpellModel.Spell noCd = s("F", "F", "p", "Wave", 5, 7, 0, -1);
        assertEquals("Wave costs 7% of your magicule and has no cooldown.", SpellModel.generatedLine(noCd));
        assertEquals("Cost 7%", SpellModel.statsLine(noCd));
        SpellModel.Spell noCost = s("F", "F", "p", "Rest", 5, 0, 3, 0);
        assertEquals("Rest costs no magicule and rests for 3 s.", SpellModel.generatedLine(noCost));
    }
}
