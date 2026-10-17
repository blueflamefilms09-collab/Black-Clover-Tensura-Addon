package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.charter.CharterTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Charter laws applied to a grimoire an arc boss declares, and how it joins the table without disturbing the seven. */
class ArcCharterRulesTest {
    private static ArcCharacter heath() {
        return ArcCharacter.boss("heath_grice", "Heath Grice").title("the Death Knight").source("Black Clover").tier(2).colors(0x203040, 0x90B0D0)
                .charter(0.3, "darkness", "tensura:curse", "shadow_bind").build();
    }

    @Test
    void sanitizeKeepsLawfulIdsOnly() {
        List<String> in = List.of("tensura:Gravity_Field", " tensura:curse ", "tensura:curse", "tensura:usurper", "nonsense", "", "a:b c", "x:y");
        assertEquals(List.of("tensura:gravity_field", "tensura:curse", "x:y"), ArcCharterRules.sanitize(in));
        assertEquals(List.of("tensura:usurper", "nonsense", "", "a:b c"), ArcCharterRules.dropped(in));
        assertEquals(List.of(), ArcCharterRules.sanitize(null));
    }

    @Test
    void theCapIsTheCharterCap() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) many.add("tensura:skill_" + i);
        List<String> kept = ArcCharterRules.sanitize(many);
        assertEquals(CharterTable.MAX_SKILLS, kept.size());
        assertEquals("tensura:skill_0", kept.get(0));
    }

    @Test
    void theGradeFollowsTheTier() {
        for (int t = 1; t <= 4; t++) assertEquals("LEGEND", ArcCharterRules.gradeFor(t));
        assertEquals("GOD", ArcCharterRules.gradeFor(5));
    }

    @Test
    void aBossBecomesACharterRow() {
        CharterTable.Boss b = ArcCharterRules.toBoss(heath());
        assertNotNull(b);
        assertEquals("heath_grice", b.id());
        assertEquals("heath_grice", b.entityPath());
        assertEquals("Heath Grice", b.name());
        assertEquals("LEGEND", b.grade());
        assertEquals(0.3, b.chance(), 1e-9);
        assertEquals(0x203040, b.cover());
        assertEquals(0x90B0D0, b.trim());
        assertEquals(List.of("tensura:darkness", "tensura:curse", "tensura:shadow_bind"), b.skills());
        assertEquals("charter_grimoire_heath_grice", b.itemId());
        assertEquals("has_dropped_charter_grimoire_heath_grice", b.flag());
        assertTrue(b.lore().contains("Heath Grice, the Death Knight"));
        assertTrue(b.lore().contains("Black Clover"));
    }

    @Test
    void noGrimoireWhenThereIsNoneToGive() {
        assertNull(ArcCharterRules.toBoss(null));
        assertNull(ArcCharterRules.toBoss(ArcCharacter.boss("plain", "Plain").build()), "no skills declared");
        assertNull(ArcCharterRules.toBoss(ArcCharacter.boss("thief", "Thief").charter(0.5, "usurper").build()), "only a forbidden skill is left");
        assertNull(ArcCharterRules.toBoss(ArcCharacter.npc("friend", "Friend").charter(0.5, "darkness").build()), "an NPC has no grimoire");
    }

    @Test
    void anArcGrimoireJoinsTheTableAndTheSevenAreUntouched() {
        List<String> before = CharterTable.ids();
        try {
            CharterTable.Boss b = CharterTable.addArc(heath());
            assertNotNull(b);
            assertSame(b, CharterTable.addArc(heath()), "the same character again is harmless");
            assertSame(b, CharterTable.byId("heath_grice"));
            assertSame(b, CharterTable.byId("HEATH_GRICE"));
            assertSame(b, CharterTable.byEntity("nusmp", "heath_grice"));
            assertNull(CharterTable.byEntity("minecraft", "heath_grice"), "only this mod's namespace");
            assertNull(CharterTable.byEntity("tensura", "heath_grice"));
            assertSame(b, CharterTable.byItemPath("charter_grimoire_heath_grice"));
            assertEquals(8, CharterTable.all().size());
            assertEquals("heath_grice", CharterTable.ids().get(7));
            assertEquals(before, CharterTable.ids().subList(0, 7));
            assertEquals("dante", CharterTable.all().get(0).id());

            assertNull(CharterTable.addArc(ArcCharacter.boss("goliath", "Another Goliath").charter(0.5, "darkness").build()), "an old boss's id is taken");
            assertNull(CharterTable.addArc(ArcCharacter.boss("dante", "Another Dante").charter(0.5, "darkness").build()));
            assertNull(CharterTable.addArc(ArcCharacter.boss("plain", "Plain").build()));
            assertEquals(8, CharterTable.all().size());
            assertEquals("Goliath", CharterTable.byId("goliath").name());
        } finally {
            CharterTable.clearArcForTests();
        }
        assertEquals(before, CharterTable.ids());
        assertNull(CharterTable.byId("heath_grice"));
        assertNull(CharterTable.byEntity("nusmp", "heath_grice"));
    }
}
