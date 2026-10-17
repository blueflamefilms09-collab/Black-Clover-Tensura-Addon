package com.newuniverse.nusmp.labyrinth.mega.theme;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The theme parser: every field, every way a datapack can get it wrong, and that a broken field never throws. */
class ThemeDefTest {
    private static ThemeDef.Parsed parse(String id, String json) { return ThemeDef.parse(id, MiniJson.parse(json)); }

    @Test
    void aCompleteFileParsesWithoutWarnings() {
        ThemeDef.Parsed p = parse("blood", "{\"id\": \"blood\", \"name\": \"Blood\", \"color\": \"#9A1B1B\", \"families\": {\"brick\": \"minecraft:nether_bricks\", \"wood\": \"crimson_planks\"},"
                + " \"explicit\": {\"minecraft:lantern\": \"minecraft:soul_lantern\"}, \"relics\": {\"keywords\": [\"Blood\", \"crimson\", \"blood\"]},"
                + " \"catastrophes\": {\"collapse\": 2, \"flood\": 1, \"spirit\": 3}, \"ambient\": \"crimson_spore\"}");
        assertEquals(List.of(), p.warnings());
        ThemeDef d = p.def();
        assertEquals("blood", d.id());
        assertEquals("Blood", d.name());
        assertEquals(0x9A1B1B, d.color());
        assertEquals("minecraft:nether_bricks", d.families().get("brick"));
        assertEquals("minecraft:crimson_planks", d.families().get("wood"), "a bare id is a minecraft id");
        assertEquals("minecraft:soul_lantern", d.explicit().get("minecraft:lantern"));
        assertEquals(List.of("blood", "crimson"), d.keywords(), "lower case, no duplicates");
        assertEquals(3, d.weight("spirit"));
        assertEquals(0, d.weight("nonsense"));
        assertEquals("minecraft:crimson_spore", d.ambient(), "the namespace is made explicit");
    }

    @Test
    void theIdFallsBackToTheFileNameAndTheNameToTheId() {
        ThemeDef d = parse("storm", "{\"catastrophes\": {\"flood\": 1}}").def();
        assertEquals("storm", d.id());
        assertEquals("Storm", d.name());
        assertEquals(0xAAAAAA, d.color());
        ThemeDef weird = parse("verdant", "{\"id\": \"Verdant Ruins!\"}").def();
        assertEquals("verdant", weird.id(), "an id that is not snake case is replaced by the file name");
        assertNull(parse("", "{}").def(), "no id anywhere: no theme");
    }

    @Test
    void aBrokenFieldBecomesAWarningNotAnException() {
        ThemeDef.Parsed p = parse("x", "{\"color\": \"red\", \"families\": {\"marble\": \"minecraft:stone\", \"brick\": 7, \"tile\": \"Not A Block\", \"rock\": \"minecraft:stone\"},"
                + " \"explicit\": {\"a\": 1}, \"relics\": {\"keywords\": \"frost\"}, \"catastrophes\": {\"meteor\": 3, \"flood\": \"lots\"}, \"ambient\": \"not a particle\"}");
        assertNotNull(p.def());
        assertTrue(p.warnings().size() >= 8, p.warnings().toString());
        assertEquals(0xAAAAAA, p.def().color());
        assertEquals(1, p.def().families().size(), "only the good slot survives");
        assertEquals("minecraft:stone", p.def().families().get("rock"));
        assertEquals(0, p.def().explicit().size());
        assertEquals("", p.def().ambient());
        assertEquals(4, p.def().weight("collapse"), "no usable weights: the default table");
    }

    @Test
    void aRetiredGradeShiftInAnOldFileIsIgnoredWithoutAWarning() {
        // 1.54.0: every vault draws from the same grades, so the per-theme shift of the first draft no longer exists; a file that still has it must load
        ThemeDef.Parsed p = parse("x", "{\"relics\": {\"keywords\": [\"frost\"], \"gradeBias\": 2}, \"catastrophes\": {\"flood\": 1}}");
        assertEquals(List.of(), p.warnings());
        assertEquals(List.of("frost"), p.def().keywords());
    }

    @Test
    void weightsAreClampedToZeroToOneHundred() {
        ThemeDef d = parse("x", "{\"catastrophes\": {\"collapse\": -5, \"flood\": 1000, \"spirit\": 2}}").def();
        assertEquals(0, d.weight("collapse"));
        assertEquals(100, d.weight("flood"));
        assertEquals(2, d.weight("spirit"));
    }

    @Test
    void aFileThatIsNotAnObjectGivesNoTheme() {
        ThemeDef.Parsed p = ThemeDef.parse("x", "just text");
        assertNull(p.def());
        assertEquals(1, p.warnings().size());
        assertNull(ThemeDef.parse("x", null).def());
    }

    @Test
    void theBakedBaselineIsTheDeepslatePaletteAndAPlainThemeHasNoSwap() {
        ThemeDef g = ThemeDef.gravity();
        assertEquals("gravity", g.id());
        assertEquals("minecraft:deepslate_bricks", g.families().get("brick"));
        assertEquals("minecraft:dark_oak_planks", g.families().get("wood"));
        assertEquals(9, g.families().size());
        assertTrue(g.explicit().isEmpty());
        ThemeDef plain = ThemeDef.plain("blood");
        assertEquals("blood", plain.id());
        assertEquals(g.families(), plain.families(), "a theme without a file looks like the baseline");
        assertEquals("Blood", plain.name());
        assertTrue(plain.keywords().isEmpty());
    }

    @Test
    void theLightTheSoundAndTheDangerAreRead() {
        ThemeDef.Parsed p = parse("radiant", "{\"catastrophes\": {\"flood\": 1}, \"light\": 3, \"danger\": 0.8, \"sound\": {\"id\": \"Block.Bell.Resonate\", \"volume\": 0.3, \"pitch\": 0.7, \"every\": 8}}");
        assertEquals(List.of(), p.warnings());
        ThemeDef d = p.def();
        assertEquals(3, d.lightShift());
        assertEquals(0.8, d.danger(), 1e-9);
        assertEquals("minecraft:block.bell.resonate", d.mood().sound(), "case folded, namespace made explicit");
        assertEquals(0.3f, d.mood().volume(), 1e-6f);
        assertEquals(0.7f, d.mood().pitch(), 1e-6f);
        assertEquals(8, d.mood().every());
        assertTrue(d.mood().any());
    }

    @Test
    void aFileWithoutThemIsTheBaselinesLightSilenceAndDanger() {
        ThemeDef d = parse("x", "{\"catastrophes\": {\"flood\": 1}}").def();
        assertEquals(0, d.lightShift());
        assertEquals(1.0, d.danger(), 1e-9);
        assertFalse(d.mood().any());
        assertEquals(ThemeDef.Mood.NONE, d.mood());
        ThemeDef g = ThemeDef.gravity();
        assertEquals(0, g.lightShift());
        assertEquals(1.0, g.danger(), 1e-9);
        assertFalse(g.mood().any());
        assertEquals(0, ThemeDef.plain("blood").lightShift());
        assertEquals(1.0, ThemeDef.plain("blood").danger(), 1e-9);
    }

    @Test
    void theLightTheSoundAndTheDangerAreClampedWithAWarningAndNeverThrow() {
        ThemeDef.Parsed p = parse("x", "{\"catastrophes\": {\"flood\": 1}, \"light\": 99, \"danger\": 17, \"sound\": {\"id\": \"minecraft:ambient.cave\", \"volume\": 9, \"pitch\": 0.01, \"every\": 0}}");
        assertEquals(5, p.warnings().size(), p.warnings().toString());
        assertEquals(ThemeDef.MAX_LIGHT_SHIFT, p.def().lightShift());
        assertEquals(ThemeDef.MAX_DANGER, p.def().danger(), 1e-9);
        assertEquals(1.0f, p.def().mood().volume(), 1e-6f);
        assertEquals(0.5f, p.def().mood().pitch(), 1e-6f);
        assertEquals(1, p.def().mood().every());
        ThemeDef.Parsed low = parse("x", "{\"catastrophes\": {\"flood\": 1}, \"light\": -99, \"danger\": 0}");
        assertEquals(-ThemeDef.MAX_LIGHT_SHIFT, low.def().lightShift());
        assertEquals(ThemeDef.MIN_DANGER, low.def().danger(), 1e-9);
        ThemeDef.Parsed wrong = parse("x", "{\"catastrophes\": {\"flood\": 1}, \"light\": \"bright\", \"danger\": \"high\", \"sound\": \"cave\"}");
        assertEquals(3, wrong.warnings().size(), wrong.warnings().toString());
        assertEquals(0, wrong.def().lightShift());
        assertEquals(1.0, wrong.def().danger(), 1e-9);
        assertFalse(wrong.def().mood().any());
        ThemeDef.Parsed badSound = parse("x", "{\"catastrophes\": {\"flood\": 1}, \"sound\": {\"id\": \"not a sound\", \"volume\": \"loud\"}, \"light\": 2}");
        assertFalse(badSound.def().mood().any(), "a sound without a usable id is silence");
        assertEquals(2, badSound.def().lightShift(), "and costs nothing else");
        assertEquals(1, badSound.warnings().size(), badSound.warnings().toString());
    }

    @Test
    void theRecordIsImmutable() {
        ThemeDef g = ThemeDef.gravity();
        boolean threw = false;
        try { g.families().put("brick", "minecraft:stone"); } catch (UnsupportedOperationException expected) { threw = true; }
        assertTrue(threw);
        threw = false;
        try { g.keywords().add("x"); } catch (UnsupportedOperationException expected) { threw = true; }
        assertTrue(threw);
    }
}
