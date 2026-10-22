package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.NoelleFormRules.Form;
import com.newuniverse.nusmp.book.NoelleFormRules.Lod;
import com.newuniverse.nusmp.book.NoelleFormRules.Recipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the pure rules of Noelle Silva's armour forms (docs/magic/noelle_forms.md): the style byte (form and tier), who may open which page, the level of detail by distance, the sprite budget
 * (the worst case of every form at every level stays under the cap and shrinks with the distance), the limits and the buffs. Pure: no game classes.
 */
class NoelleFormRulesTest {
    private static final Form[] FORMS = {Form.VALKYRIE, Form.MERMAID, Form.SEA_DRAGON, Form.BLACK_DRAGON};

    @Test
    void theStyleByteCarriesTheFormAndTheTier() {
        for (Form f : Form.values()) {
            assertEquals(f, NoelleFormRules.form(NoelleFormRules.style(f, false)));
            assertEquals(f, NoelleFormRules.form(NoelleFormRules.style(f, true)));
            assertFalse(NoelleFormRules.character(NoelleFormRules.style(f, false)));
            assertTrue(NoelleFormRules.character(NoelleFormRules.style(f, true)));
        }
        assertEquals(1, NoelleFormRules.style(Form.VALKYRIE, false), "the old Valkyrie Dress keeps its style number");
        assertEquals(0, NoelleFormRules.style(Form.ARMOUR, false), "Sea Dragon's Scale Mail keeps style 0");
        assertEquals(17, NoelleFormRules.style(Form.VALKYRIE, true));
        assertEquals(20, NoelleFormRules.style(Form.BLACK_DRAGON, true));
        assertFalse(NoelleFormRules.remade(0));
        assertTrue(NoelleFormRules.remade(1) && NoelleFormRules.remade(19));
    }

    @Test
    void aBadStyleByteNeverThrows() {
        for (int s : new int[]{-1, 5, 15, 31, 99, 127, 255, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertNotNull(NoelleFormRules.form(s));
            NoelleFormRules.character(s);
        }
        assertEquals(Form.ARMOUR, NoelleFormRules.form(15), "an unknown form reads as the plain armour");
    }

    @Test
    void theEnumOnlyGrewAtTheEnd() {
        assertEquals(Form.ARMOUR, Form.values()[0]);
        assertEquals(Form.VALKYRIE, Form.values()[1]);
        assertEquals(Form.MERMAID, Form.values()[2]);
        assertEquals(Form.SEA_DRAGON, Form.values()[3]);
        assertEquals(Form.BLACK_DRAGON, Form.values()[4]);
        assertEquals(5, Form.values().length);
        assertTrue(Form.values().length <= NoelleFormRules.FORM_MASK, "the form must fit the low four bits");
    }

    @Test
    void thePagesAndWhoMayOpenThem() {
        assertEquals(Form.VALKYRIE, NoelleFormRules.formOfPage("valkyrie_dress"));
        assertEquals(Form.MERMAID, NoelleFormRules.formOfPage("mermaid_form"));
        assertEquals(Form.SEA_DRAGON, NoelleFormRules.formOfPage("sea_dragon_god_form"));
        assertEquals(Form.BLACK_DRAGON, NoelleFormRules.formOfPage("black_dragon_form"));
        assertNull(NoelleFormRules.formOfPage("torrent"));
        assertNull(NoelleFormRules.formOfPage(null));
        assertTrue(NoelleFormRules.pageOpen("valkyrie_dress", false) && NoelleFormRules.pageOpen("mermaid_form", false), "every Water mage");
        assertFalse(NoelleFormRules.pageOpen("sea_dragon_god_form", false));
        assertFalse(NoelleFormRules.pageOpen("black_dragon_form", false));
        assertTrue(NoelleFormRules.pageOpen("sea_dragon_god_form", true) && NoelleFormRules.pageOpen("black_dragon_form", true));
        assertTrue(NoelleFormRules.pageOpen("torrent", false), "any other page is not decided here");
    }

    @Test
    void theLevelOfDetailFollowsTheDistance() {
        assertEquals(Lod.CHARACTER, NoelleFormRules.lod(0.0, true));
        assertEquals(Lod.CHARACTER, NoelleFormRules.lod(23.9 * 23.9, true));
        assertEquals(Lod.STANDARD, NoelleFormRules.lod(0.0, false), "a standard tier never reaches the character level");
        assertEquals(Lod.STANDARD, NoelleFormRules.lod(23.9 * 23.9, false));
        assertEquals(Lod.FAR, NoelleFormRules.lod(24.1 * 24.1, false));
        assertEquals(Lod.FAR, NoelleFormRules.lod(24.1 * 24.1, true));
        assertEquals(Lod.FAR, NoelleFormRules.lod(63.9 * 63.9, true));
        assertEquals(Lod.NONE, NoelleFormRules.lod(64.1 * 64.1, true));
        assertEquals(Lod.NONE, NoelleFormRules.lod(1e9, false));
    }

    @Test
    void aCrowdCostsABoundedNumberOfFigures() {
        assertEquals(Lod.CHARACTER, NoelleFormRules.budgeted(Lod.CHARACTER, 0, 0, 0));
        assertEquals(Lod.CHARACTER, NoelleFormRules.budgeted(Lod.CHARACTER, NoelleFormRules.MAX_CHARACTER_PER_FRAME - 1, 0, 0));
        assertEquals(Lod.STANDARD, NoelleFormRules.budgeted(Lod.CHARACTER, NoelleFormRules.MAX_CHARACTER_PER_FRAME, 0, 0), "the third character-tier player falls back one level");
        assertEquals(Lod.FAR, NoelleFormRules.budgeted(Lod.CHARACTER, NoelleFormRules.MAX_CHARACTER_PER_FRAME, NoelleFormRules.MAX_FULL_PER_FRAME, 0), "and then another");
        assertEquals(Lod.NONE, NoelleFormRules.budgeted(Lod.CHARACTER, NoelleFormRules.MAX_CHARACTER_PER_FRAME, NoelleFormRules.MAX_FULL_PER_FRAME, NoelleFormRules.MAX_FAR_PER_FRAME), "and past the last level: nothing");
        assertEquals(Lod.FAR, NoelleFormRules.budgeted(Lod.STANDARD, 0, NoelleFormRules.MAX_FULL_PER_FRAME, 0));
        assertEquals(Lod.NONE, NoelleFormRules.budgeted(Lod.NONE, 99, 99, 99), "nothing is never drawn");
        assertEquals(Lod.NONE, NoelleFormRules.budgeted(Lod.FAR, 0, 0, NoelleFormRules.MAX_FAR_PER_FRAME));
        int chars = 0, full = 0, far = 0, characters = 0, standards = 0, fars = 0, nones = 0;                 // 40 players in view, all character tier, all near
        for (int i = 0; i < 40; i++) {
            Lod l = NoelleFormRules.budgeted(Lod.CHARACTER, chars, full, far);
            if (l == Lod.CHARACTER) { chars++; full++; characters++; } else if (l == Lod.STANDARD) { full++; standards++; } else if (l == Lod.FAR) { far++; fars++; } else nones++;
        }
        assertEquals(NoelleFormRules.MAX_CHARACTER_PER_FRAME, characters);
        assertEquals(NoelleFormRules.MAX_FULL_PER_FRAME - NoelleFormRules.MAX_CHARACTER_PER_FRAME, standards);
        assertEquals(NoelleFormRules.MAX_FAR_PER_FRAME, fars);
        assertEquals(40 - NoelleFormRules.MAX_FULL_PER_FRAME - NoelleFormRules.MAX_FAR_PER_FRAME, nones, "the rest of the crowd is not drawn");
    }

    @Test
    void everyFormAtEveryLevelStaysUnderTheQuadCapAndShrinksWithTheDistance() {
        for (Form f : FORMS) {
            int prev = Integer.MAX_VALUE;
            for (Lod l : new Lod[]{Lod.CHARACTER, Lod.STANDARD, Lod.FAR, Lod.NONE}) {
                Recipe r = NoelleFormRules.recipe(f, l);
                int q = NoelleFormRules.quads(r);
                assertTrue(q <= NoelleFormRules.QUAD_CAP, f + " at " + l + " draws " + q + " quads");
                assertTrue(q <= prev, f + ": " + l + " draws more than the nearer level");
                prev = q;
            }
            assertEquals(0, NoelleFormRules.quads(NoelleFormRules.recipe(f, Lod.NONE)), "nothing beyond 64 blocks");
            Recipe far = NoelleFormRules.recipe(f, Lod.FAR);
            assertEquals(0, far.ribbons() + far.drops() + far.rings(), f + ": a distant player draws no ribbons, drops or rings");
            assertFalse(far.skin(), f + ": a distant player draws no skin shells");
        }
        assertEquals(0, NoelleFormRules.quads(NoelleFormRules.recipe(Form.ARMOUR, Lod.CHARACTER)), "the plain armour is the old painter's");
    }

    @Test
    void theBlackAndDragonFormsGetEyesTheOthersDoNot() {
        assertTrue(NoelleFormRules.recipe(Form.BLACK_DRAGON, Lod.STANDARD).eyes());
        assertTrue(NoelleFormRules.recipe(Form.SEA_DRAGON, Lod.STANDARD).eyes());
        assertFalse(NoelleFormRules.recipe(Form.VALKYRIE, Lod.CHARACTER).eyes());
        assertFalse(NoelleFormRules.recipe(Form.MERMAID, Lod.CHARACTER).eyes());
    }

    @Test
    void theLimitsAndTheExhaustion() {
        assertEquals(0L, NoelleFormRules.limitTicks(Form.VALKYRIE));
        assertEquals(0L, NoelleFormRules.limitTicks(Form.MERMAID));
        assertEquals(6000L, NoelleFormRules.limitTicks(Form.SEA_DRAGON));
        assertEquals(1800L, NoelleFormRules.limitTicks(Form.BLACK_DRAGON));
        assertFalse(NoelleFormRules.expired(Form.MERMAID, 1, 1_000_000L), "the Mermaid Form has no end of its own");
        assertFalse(NoelleFormRules.expired(Form.BLACK_DRAGON, 1000L, 1000L + 1799L));
        assertTrue(NoelleFormRules.expired(Form.BLACK_DRAGON, 1000L, 1000L + 1800L));
        assertFalse(NoelleFormRules.expired(Form.BLACK_DRAGON, 0L, 99_999L), "a form that never began (since 0) never expires");
        assertTrue(NoelleFormRules.expired(Form.SEA_DRAGON, 10L, 10L + 6000L));
        assertTrue(NoelleFormRules.blocked(5000L, 4999L));
        assertFalse(NoelleFormRules.blocked(5000L, 5000L));
        assertFalse(NoelleFormRules.blocked(0L, 0L));
        assertTrue(NoelleFormRules.BLACK_AFTERMATH_TICKS > NoelleFormRules.limitTicks(Form.BLACK_DRAGON), "the exhaustion is longer than the form");
    }

    @Test
    void theBuffsAreRealEffectsAndNoneTouchesHunger() {
        java.util.Set<String> known = java.util.Set.of("water_breathing", "dolphins_grace", "regeneration", "movement_slowdown", "damage_boost", "damage_resistance", "movement_speed", "night_vision");
        for (Form f : Form.values()) {
            for (NoelleFormRules.Buff b : NoelleFormRules.buffs(f)) {
                assertTrue(known.contains(b.effect()), f + ": unknown effect " + b.effect());
                assertTrue(b.amplifier() >= 0 && b.amplifier() <= 3, f + ": amplifier " + b.amplifier());
                assertFalse(b.waterOnly() && b.landOnly());
                assertFalse(b.effect().contains("hunger") || b.effect().contains("saturation"), "the forms never touch the food bar");
            }
        }
        assertEquals(0, NoelleFormRules.buffs(Form.VALKYRIE).length, "the Valkyrie Dress keeps the buffs of its own page");
        assertTrue(NoelleFormRules.buffs(Form.BLACK_DRAGON).length >= NoelleFormRules.buffs(Form.SEA_DRAGON).length);
    }

    @Test
    void theEntryWaveIsBounded() {
        assertEquals(0f, NoelleFormRules.entryDamage(Form.MERMAID), 0f);
        assertEquals(0.0, NoelleFormRules.entryRadius(Form.VALKYRIE), 0.0);
        assertTrue(NoelleFormRules.entryDamage(Form.BLACK_DRAGON) > NoelleFormRules.entryDamage(Form.SEA_DRAGON));
        assertTrue(NoelleFormRules.entryRadius(Form.BLACK_DRAGON) <= 8.0 && NoelleFormRules.entryRadius(Form.SEA_DRAGON) <= 8.0);
        assertTrue(NoelleFormRules.ENTRY_MAX_TARGETS <= 24);
    }

    @Test
    void theCharacterBitIsOnlyAddedToFormPagesByTheCharacterRule() {
        assertEquals(17, ArmourRules.style("character", 1, false, false, true));
        assertEquals(1, ArmourRules.style("character", 1, false, false, false));
        assertEquals(0, ArmourRules.style("character", 0, false, false, true), "style 0 (Scale Mail) is never raised");
        assertEquals(1, ArmourRules.style("none", 1, false, false, true), "only the character rule raises it");
        assertEquals(1, ArmourRules.style("sneak", 1, false, false, true));
        assertEquals(20, ArmourRules.style("character", 4, false, false, true));
    }

    @Test
    void everyFormHasItsPiecesAndKey() {
        for (Form f : FORMS) {
            assertFalse(NoelleFormRules.key(f).isEmpty());
            assertTrue(NoelleFormRules.standardPieces(f).length >= 9, f + " standard pieces");
            assertTrue(NoelleFormRules.characterPieces(f).length >= 3, f + " character pieces");
            java.util.Set<String> all = new java.util.HashSet<>(java.util.List.of(NoelleFormRules.standardPieces(f)));
            for (String n : NoelleFormRules.characterPieces(f)) assertTrue(all.add(n), f + ": " + n + " is listed twice");
        }
        assertEquals("", NoelleFormRules.key(Form.ARMOUR));
        assertEquals(0, NoelleFormRules.standardPieces(Form.ARMOUR).length);
        assertTrue(NoelleFormRules.standardPieces(Form.VALKYRIE).length >= 12);
    }
}
