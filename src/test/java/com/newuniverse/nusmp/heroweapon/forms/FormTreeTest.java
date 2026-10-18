package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: availability, status and list order decide what the player is offered and what the Weapon Book shows, on the server and on the client; the cascade (a form
 * learned now opens the next one) and the sealed curse forms are the edges that break quietly, so each has a case.
 */
class FormTreeTest {
    private static final Map<String, Set<String>> TAGS = Map.of("minecraft:leaves", Set.of("minecraft:oak_leaves", "minecraft:birch_leaves"),
            "c:ingots", Set.of("minecraft:iron_ingot", "minecraft:gold_ingot"));
    private static final FormTree.TagMatcher MATCH = (tag, item) -> TAGS.getOrDefault(tag, Set.of()).contains(item);

    private static HeroForm.Requirement item(String id, int n) { return new HeroForm.Requirement(id, "", n); }

    private static HeroForm.Requirement tag(String id, int n) { return new HeroForm.Requirement("", id, n); }

    private static HeroForm form(String id, int tier, List<String> parents, List<HeroForm.Requirement> req) {
        return new HeroForm(HeroKind.SHIELD, id, id, "", tier, parents, req, 100, Map.of(), Map.of(), Map.of(), List.of(), "base", false, null, 0);
    }

    private static HeroForm curse(String id, int tier, Sin sin, List<String> parents, List<HeroForm.Requirement> req) {
        return new HeroForm(HeroKind.SHIELD, id, id, "", tier, parents, req, 100, Map.of(), Map.of(), Map.of(), List.of(), "curse", true, sin, 3);
    }

    private static Map<String, Integer> absorbed(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    @Test
    void itemAndTagRequirementsCountWhatWasAbsorbed() {
        HeroForm.Requirement leaves = tag("minecraft:leaves", 32), berries = item("minecraft:glow_berries", 8);
        Map<String, Integer> got = absorbed("minecraft:oak_leaves", 20, "minecraft:birch_leaves", 15, "minecraft:glow_berries", 3, "minecraft:stick", 99);
        assertEquals(35, FormTree.raw(leaves, got, MATCH), "every item in the tag counts");
        assertEquals(32, FormTree.progress(leaves, got, MATCH), "progress is capped at the count");
        assertTrue(FormTree.met(leaves, got, MATCH));
        assertEquals(3, FormTree.raw(berries, got, MATCH));
        assertFalse(FormTree.met(berries, got, MATCH));
        assertEquals(0, FormTree.raw(tag("minecraft:planks", 4), got, MATCH), "an unknown tag matches nothing");
        assertEquals(0, FormTree.raw(berries, absorbed(), MATCH));
    }

    @Test
    void shareAveragesTheRequirementsAndIsOneWhenThereAreNone() {
        HeroForm f = form("a1", 1, List.of("base"), List.of(item("minecraft:leather", 10), item("minecraft:bone", 10)));
        assertEquals(0.25, FormTree.share(f, absorbed("minecraft:leather", 5), MATCH), 1e-9);
        assertEquals(1.0, FormTree.share(form("a1", 1, List.of("base"), List.of()), absorbed(), MATCH), 1e-9);
        assertEquals(1.0, FormTree.share(f, absorbed("minecraft:leather", 50, "minecraft:bone", 10), MATCH), 1e-9);
    }

    @Test
    void aFormIsAvailableWhenParentsAreLearnedAndRequirementsMetAndTheSeriesIsOpen() {
        HeroForm a1 = form("a1", 1, List.of("base"), List.of(item("minecraft:leather", 4)));
        HeroForm b1 = form("b1", 2, List.of("a1"), List.of(item("minecraft:bone", 4)));
        Map<String, Integer> got = absorbed("minecraft:leather", 4, "minecraft:bone", 4);
        assertTrue(FormTree.available(a1, Set.of("base"), got, MATCH, Set.of()));
        assertFalse(FormTree.available(b1, Set.of("base"), got, MATCH, Set.of()), "the parent is not learned");
        assertTrue(FormTree.available(b1, Set.of("base", "a1"), got, MATCH, Set.of()));
        assertFalse(FormTree.available(a1, Set.of("base", "a1"), got, MATCH, Set.of()), "already learned is not available");
        assertFalse(FormTree.available(a1, Set.of("base"), absorbed("minecraft:leather", 3), MATCH, Set.of()), "one short");
        HeroForm w = curse("w1", 1, Sin.WRATH, List.of("base"), List.of());
        assertFalse(FormTree.available(w, Set.of("base"), got, MATCH, Set.of()), "a curse form needs its series");
        assertFalse(FormTree.available(w, Set.of("base"), got, MATCH, Set.of("pride")), "another sin's series does not open it");
        assertTrue(FormTree.available(w, Set.of("base"), got, MATCH, Set.of("wrath")));
    }

    @Test
    void statusNamesEachKind() {
        HeroForm a1 = form("a1", 1, List.of("base"), List.of(item("minecraft:leather", 4)));
        HeroForm b1 = form("b1", 2, List.of("a1"), List.of(item("minecraft:bone", 4)));
        HeroForm w = curse("w1", 1, Sin.WRATH, List.of("base"), List.of());
        Set<String> learned = Set.of("base", "a1");
        Map<String, Integer> got = absorbed("minecraft:bone", 4);
        assertEquals(FormTree.Status.ACTIVE, FormTree.status(a1, "a1", learned, got, MATCH, Set.of()));
        assertEquals(FormTree.Status.LEARNED, FormTree.status(a1, "base", learned, got, MATCH, Set.of()));
        assertEquals(FormTree.Status.AVAILABLE, FormTree.status(b1, "base", learned, got, MATCH, Set.of()));
        assertEquals(FormTree.Status.LOCKED, FormTree.status(b1, "base", learned, absorbed(), MATCH, Set.of()));
        assertEquals(FormTree.Status.SEALED, FormTree.status(w, "base", learned, got, MATCH, Set.of()));
        assertEquals(FormTree.Status.AVAILABLE, FormTree.status(w, "base", learned, got, MATCH, Set.of("wrath")));
    }

    @Test
    void learnableCascadesThroughNewlyLearnedParentsInOrder() {
        List<HeroForm> forms = List.of(form("base", 0, List.of(), List.of()), form("a1", 1, List.of("base"), List.of(item("minecraft:leather", 4))),
                form("b1", 2, List.of("a1"), List.of(item("minecraft:bone", 4))), form("c1", 3, List.of("b1"), List.of(item("minecraft:bone", 8))));
        Map<String, Integer> got = absorbed("minecraft:leather", 4, "minecraft:bone", 8);
        assertEquals(List.of("a1", "b1", "c1"), FormTree.learnable(forms, Set.of("base"), got, MATCH, Set.of()),
                "requirements are never consumed, so one meal can open the whole chain");
        assertEquals(List.of("b1", "c1"), FormTree.learnable(forms, Set.of("base", "a1"), got, MATCH, Set.of()));
        assertEquals(List.of("a1"), FormTree.learnable(forms, Set.of("base"), absorbed("minecraft:leather", 4, "minecraft:bone", 3), MATCH, Set.of()));
        assertTrue(FormTree.learnable(forms, Set.of("base", "a1", "b1", "c1"), got, MATCH, Set.of()).isEmpty());
        List<HeroForm> backwards = new ArrayList<>(forms);
        java.util.Collections.reverse(backwards);
        assertEquals(3, FormTree.learnable(backwards, Set.of("base"), got, MATCH, Set.of()).size(), "the order of the list does not matter, only the result");
    }

    @Test
    void learnableLeavesSealedCurseFormsAloneAndOpensThemWithTheSeries() {
        List<HeroForm> forms = List.of(form("base", 0, List.of(), List.of()), curse("w1", 1, Sin.WRATH, List.of("base"), List.of()),
                curse("w2", 2, Sin.WRATH, List.of("w1"), List.of(item("minecraft:blaze_rod", 4))));
        Map<String, Integer> got = absorbed("minecraft:blaze_rod", 4);
        assertTrue(FormTree.learnable(forms, Set.of("base"), got, MATCH, Set.of()).isEmpty());
        assertEquals(List.of("w1", "w2"), FormTree.learnable(forms, Set.of("base"), got, MATCH, Set.of("wrath")));
    }

    @Test
    void rowsListTheOrdinaryTreeDepthFirstThenTheCurseSeriesByTheirSin() {
        List<HeroForm> forms = List.of(form("base", 0, List.of(), List.of()), form("a1", 1, List.of("base"), List.of(item("minecraft:leather", 4))),
                form("a2", 1, List.of("base"), List.of(item("minecraft:leather", 4))), form("b1", 2, List.of("a1"), List.of(item("minecraft:bone", 4))),
                form("b2", 2, List.of("a2"), List.of(item("minecraft:bone", 4))), form("c1", 3, List.of("b2", "b1"), List.of(item("minecraft:bone", 4))),
                curse("g1", 1, Sin.GREED, List.of("base"), List.of()), curse("l1", 1, Sin.GLUTTONY, List.of("base"), List.of()), curse("g2", 2, Sin.GREED, List.of("g1"), List.of()));
        List<FormTree.Row> rows = FormTree.rows(forms);
        List<String> ids = new ArrayList<>();
        for (FormTree.Row r : rows) ids.add(r.form().id());
        assertEquals(List.of("base", "a1", "b1", "a2", "b2", "c1", "g1", "g2", "l1"), ids, "c1 sits under its first parent b2; the series are grouped by sin");
        assertEquals(0, rows.get(0).indent());
        assertEquals(1, rows.get(1).indent());
        assertEquals(2, rows.get(2).indent());
        assertEquals(3, rows.get(5).indent());
        assertFalse(rows.get(5).curseHeading());
        assertTrue(rows.get(6).curseHeading(), "the first curse row asks for the heading");
        assertFalse(rows.get(7).curseHeading());
        assertEquals(0, rows.get(6).indent());
        assertEquals(1, rows.get(7).indent(), "g2 is one deeper than g1 inside the series");
    }

    @Test
    void rowsStillShowAFormWhoseParentIsMissing() {
        List<HeroForm> forms = List.of(form("base", 0, List.of(), List.of()), form("lost", 2, List.of("ghost"), List.of(item("minecraft:bone", 4))));
        List<FormTree.Row> rows = FormTree.rows(forms);
        assertEquals(2, rows.size());
        assertEquals("lost", rows.get(1).form().id());
    }

    @Test
    void rowsOfAnEmptyOrBaseOnlyListAreSafe() {
        assertTrue(FormTree.rows(List.of()).isEmpty());
        assertEquals(1, FormTree.rows(List.of(HeroForm.base(HeroKind.SHIELD))).size());
    }
}
