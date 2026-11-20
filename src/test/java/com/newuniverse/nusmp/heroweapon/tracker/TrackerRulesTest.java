package com.newuniverse.nusmp.heroweapon.tracker;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Filter;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Row;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Settings;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Status;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Table;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Viewer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Weapon Tracker's whole answer is these pure rules, so every status, the hidden names, the unique settings, the viewer's own row and the filters are pinned without Minecraft. */
class TrackerRulesTest {
    private static final String ME = "00000000-0000-0000-0000-00000000000a", A = "00000000-0000-0000-0000-00000000000b", B = "00000000-0000-0000-0000-00000000000c";
    private static final Settings DEFAULT = new Settings(true, true, false, true, 1);          // the shipped defaults: holyUnique on, vassalUnique off, names shown, one bond

    private static Table build(Map<String, List<String>> reg, Settings s, String me, HeroKind bond) {
        return TrackerRules.build(reg, s, new Viewer(me, bond), id -> id.equals(A) ? "Alice" : id.equals(B) ? "Bob" : "Me", id -> id.equals(A) || id.equals(ME));
    }

    private static Row row(Table t, HeroKind k) {
        for (Row r : t.rows()) if (r.kind() == k) return r;
        throw new AssertionError("no row for " + k);
    }

    @Test
    void oneRowPerWeaponInCatalogueOrderAndAnEmptyRegisterIsAllFree() {
        Table t = build(Map.of(), DEFAULT, ME, null);
        assertEquals(21, t.rows().size());
        assertEquals(HeroKind.values().length, t.rows().size());
        for (int i = 0; i < 21; i++) {
            assertEquals(HeroKind.values()[i], t.rows().get(i).kind());
            assertEquals(Status.FREE, t.rows().get(i).status());
            assertEquals(0, t.rows().get(i).holders());
            assertEquals("", t.rows().get(i).holder());
            assertFalse(t.rows().get(i).online());
        }
        assertNull(t.own());
        assertEquals(21, TrackerRules.countFree(t.rows()));
    }

    @Test
    void aUniqueWeaponHeldBySomeoneElseIsTakenAndNamesTheHolder() {
        Table t = build(Map.of(HeroKind.SHIELD.itemId(), List.of(A)), DEFAULT, ME, null);
        Row r = row(t, HeroKind.SHIELD);
        assertEquals(Status.TAKEN, r.status());
        assertEquals(1, r.holders());
        assertEquals("Alice", r.holder());
        assertTrue(r.online());
        assertTrue(r.unique());
        assertEquals(20, TrackerRules.countFree(t.rows()));
    }

    @Test
    void anOfflineHolderIsReportedOffline() {
        Row r = row(build(Map.of(HeroKind.SWORD.itemId(), List.of(B)), DEFAULT, ME, null), HeroKind.SWORD);
        assertEquals("Bob", r.holder());
        assertFalse(r.online());
    }

    @Test
    void theViewersOwnWeaponIsYoursAndBecomesTheOwnWeapon() {
        Table t = build(Map.of(HeroKind.BOW.itemId(), List.of(ME)), DEFAULT, ME, HeroKind.BOW);
        assertEquals(Status.YOURS, row(t, HeroKind.BOW).status());
        assertEquals(HeroKind.BOW, t.own());
        assertEquals(1, t.settings().maxBonds());
        assertEquals(1, TrackerRules.filter(t.rows(), Filter.YOURS).size());
    }

    @Test
    void theRegisterAloneIsEnoughForYoursAndTheBondAloneIsEnoughToo() {
        Table byRegister = build(Map.of(HeroKind.AXE.itemId(), List.of(ME)), DEFAULT, ME, null);
        assertEquals(Status.YOURS, row(byRegister, HeroKind.AXE).status());
        assertEquals(HeroKind.AXE, byRegister.own());
        Table byBond = build(Map.of(), DEFAULT, ME, HeroKind.AXE);
        assertEquals(Status.YOURS, row(byBond, HeroKind.AXE).status());
        assertEquals(1, row(byBond, HeroKind.AXE).holders());
    }

    @Test
    void withVassalUniqueOffAWeaponHeldByOthersIsSharedAndCountsEveryHolder() {
        Table t = build(Map.of(HeroKind.HAMMER.itemId(), List.of(A, B), HeroKind.FAN.itemId(), List.of(B)), DEFAULT, ME, null);
        Row hammer = row(t, HeroKind.HAMMER);
        assertEquals(Status.SHARED, hammer.status());
        assertEquals(2, hammer.holders());
        assertEquals("Alice", hammer.holder());                       // the first online holder is the one named
        assertFalse(hammer.unique());
        assertEquals(Status.SHARED, row(t, HeroKind.FAN).status());
        assertEquals("Bob", row(t, HeroKind.FAN).holder());
    }

    @Test
    void withVassalUniqueOnTheSevenStarAndVassalWeaponsAreTakenToo() {
        Settings strict = new Settings(true, true, true, true, 1);
        Table t = build(Map.of(HeroKind.HAMMER.itemId(), List.of(A), HeroKind.CHAIN.itemId(), List.of(B)), strict, ME, null);
        assertEquals(Status.TAKEN, row(t, HeroKind.HAMMER).status());
        assertEquals(Status.TAKEN, row(t, HeroKind.CHAIN).status());
        assertTrue(row(t, HeroKind.HAMMER).unique());
    }

    @Test
    void withHolyUniqueOffALegendaryWeaponIsSharedToo() {
        Settings loose = new Settings(true, false, false, true, 1);
        Table t = build(Map.of(HeroKind.SPEAR.itemId(), List.of(A)), loose, ME, null);
        assertEquals(Status.SHARED, row(t, HeroKind.SPEAR).status());
        assertFalse(row(t, HeroKind.SPEAR).unique());
    }

    @Test
    void holderNamesAreHiddenWhenTheSettingIsOffButStatusCountAndOnlineStay() {
        Settings hidden = new Settings(true, true, false, false, 1);
        Table t = build(Map.of(HeroKind.SHIELD.itemId(), List.of(A), HeroKind.HAMMER.itemId(), List.of(A, B)), hidden, ME, null);
        Row shield = row(t, HeroKind.SHIELD);
        assertEquals(Status.TAKEN, shield.status());
        assertEquals("", shield.holder());
        assertTrue(shield.online());
        assertEquals(2, row(t, HeroKind.HAMMER).holders());
        assertEquals("", row(t, HeroKind.HAMMER).holder());
    }

    @Test
    void theNameLookupIsNeverCalledWhenNamesAreHiddenAndOnlyOncePerWeapon() {
        Set<String> asked = new HashSet<>();
        TrackerRules.build(Map.of(HeroKind.SHIELD.itemId(), List.of(A)), new Settings(true, true, false, false, 1), new Viewer(ME, null), id -> { asked.add(id); return "x"; }, id -> false);
        assertTrue(asked.isEmpty(), "a hidden name must not even be looked up");
        int[] calls = {0};
        TrackerRules.build(Map.of(HeroKind.HAMMER.itemId(), List.of(A, B, "00000000-0000-0000-0000-00000000000d")), DEFAULT, new Viewer(ME, null), id -> { calls[0]++; return "n"; }, id -> false);
        assertEquals(1, calls[0], "one name lookup per weapon however many holders it has");
    }

    @Test
    void theViewersOwnNameIsNeverNeededOnTheirOwnRow() {
        Table t = build(Map.of(HeroKind.KATANA.itemId(), List.of(ME, A)), DEFAULT, ME, HeroKind.KATANA);
        Row r = row(t, HeroKind.KATANA);
        assertEquals(Status.YOURS, r.status());
        assertEquals(2, r.holders());
        assertEquals("", r.holder());
    }

    @Test
    void anUnknownNameStaysEmptyAndNamesAreCappedAndCleaned() {
        Table t = TrackerRules.build(Map.of(HeroKind.SHIELD.itemId(), List.of(A)), DEFAULT, new Viewer(ME, null), id -> "", id -> false);
        assertEquals("", row(t, HeroKind.SHIELD).holder());
        String longName = "A".repeat(80);
        assertEquals(TrackerRules.MAX_NAME, TrackerRules.clean(longName).length());
        assertEquals("ab", TrackerRules.clean("a\nb\u0000"));
        assertEquals("", TrackerRules.clean(null));
    }

    @Test
    void aViewerWithNoUuidStillGetsATable() {
        Table t = TrackerRules.build(Map.of(HeroKind.SHIELD.itemId(), List.of(A)), DEFAULT, null, id -> "Alice", id -> true);
        assertEquals(21, t.rows().size());
        assertEquals(Status.TAKEN, row(t, HeroKind.SHIELD).status());
        assertNull(t.own());
    }

    @Test
    void theSettingsTravelWithTheTable() {
        Settings off = new Settings(false, true, false, true, 0);
        Table t = build(Map.of(), off, ME, null);
        assertFalse(t.settings().enabled());
        assertEquals(0, t.settings().maxBonds());
    }

    @Test
    void theFiltersSplitTheTwentyOneWeaponsBy4_7_10AndByStatus() {
        Table t = build(Map.of(HeroKind.SHIELD.itemId(), List.of(A), HeroKind.HAMMER.itemId(), List.of(A), HeroKind.FAN.itemId(), List.of(ME)), DEFAULT, ME, HeroKind.FAN);
        assertEquals(21, TrackerRules.filter(t.rows(), Filter.ALL).size());
        assertEquals(4, TrackerRules.filter(t.rows(), Filter.LEGENDARY).size());
        assertEquals(7, TrackerRules.filter(t.rows(), Filter.SEVEN_STAR).size());
        assertEquals(10, TrackerRules.filter(t.rows(), Filter.VASSAL).size());
        assertEquals(18, TrackerRules.filter(t.rows(), Filter.FREE).size());
        assertEquals(2, TrackerRules.filter(t.rows(), Filter.TAKEN).size());                  // TAKEN covers TAKEN and SHARED
        assertEquals(1, TrackerRules.filter(t.rows(), Filter.YOURS).size());
        for (Row r : TrackerRules.filter(t.rows(), Filter.FREE)) assertEquals(Status.FREE, r.status());
    }

    @Test
    void chipsWrapAndScrollStaysInsideTheList() {
        assertEquals(Filter.YOURS, TrackerRules.step(Filter.ALL, -1));
        assertEquals(Filter.ALL, TrackerRules.step(Filter.YOURS, 1));
        assertEquals(Filter.LEGENDARY, TrackerRules.step(Filter.ALL, 1));
        assertEquals(0, TrackerRules.clampScroll(-5, 21, 7));
        assertEquals(14, TrackerRules.clampScroll(99, 21, 7));
        assertEquals(0, TrackerRules.clampScroll(3, 4, 7));
        assertEquals(5, TrackerRules.clampScroll(5, 21, 7));
    }

    @Test
    void classLabelsAreTheThreeFamilies() {
        Set<String> labels = new HashSet<>();
        for (HeroKind k : HeroKind.values()) labels.add(TrackerRules.classLabel(k.heroClass()));
        assertEquals(Set.of("Legendary", "Seven Star", "Vassal"), labels);
    }

    @Test
    void aTableIsAtMostOneRowPerWeaponWhateverTheRegisterHolds() {
        Map<String, List<String>> reg = new java.util.HashMap<>();
        for (HeroKind k : HeroKind.values()) reg.put(k.itemId(), List.of(A, B, ME));
        reg.put("not_a_weapon", List.of(A));
        Table t = build(reg, DEFAULT, UUID.randomUUID().toString(), null);
        assertEquals(TrackerRules.maxRows(), t.rows().size());
    }
}
