package com.newuniverse.nusmp.requip;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: Instant Requip changes the whole worn set in one press; it must be all or nothing, must say what was missing, and must never lose or double a stack. */
class LoadoutRulesTest {
    private static final ArmouryRulesTest.St NONE = ArmouryRulesTest.OPS.empty();
    private static final ArmouryRules.Ops<ArmouryRulesTest.St> OPS = ArmouryRulesTest.OPS;

    /** An exact match scores 2, the same item before a '#' (a damaged copy) scores 1. */
    private static final LoadoutRules.Matcher<ArmouryRulesTest.St> M = (t, c) -> {
        if (OPS.isEmpty(t) || OPS.isEmpty(c)) return 0;
        if (t.item().equals(c.item())) return 2;
        return t.item().split("#")[0].equals(c.item().split("#")[0]) ? 1 : 0;
    };

    private static ArmouryRulesTest.St s(String item) { return new ArmouryRulesTest.St(item, 1); }

    private static List<ArmouryRulesTest.St> list(int n) { return ArmouryRulesTest.cells(n); }

    private static List<ArmouryRulesTest.St> template(String... slotItems) {
        List<ArmouryRulesTest.St> t = list(LoadoutRules.SLOTS);
        for (int i = 0; i < slotItems.length; i++) if (slotItems[i] != null) t.set(i, s(slotItems[i]));
        return t;
    }

    private static long total(LoadoutRules.Plan<ArmouryRulesTest.St> p) {
        return ArmouryRules.total(p.worn(), OPS) + ArmouryRules.total(p.rest(), OPS) + ArmouryRules.total(p.armoury(), OPS);
    }

    @Test
    void theNumbersOfTheSlots() {
        assertEquals(14, LoadoutRules.SLOTS);
        assertEquals(6, LoadoutRules.MAX_LOADOUTS);
        assertTrue(LoadoutRules.isArmourSlot(3));
        assertFalse(LoadoutRules.isArmourSlot(4));
        assertTrue(LoadoutRules.validSlot(13));
        assertFalse(LoadoutRules.validSlot(14));
    }

    @Test
    void theCooldownIsOneSecondAndInstantChangeShortensIt() {
        assertEquals(20, LoadoutRules.cooldown(0.0, false));
        assertEquals(20, LoadoutRules.cooldown(0.74, false));
        assertEquals(LoadoutRules.INSTANT_COOLDOWN_TICKS, LoadoutRules.cooldown(0.75, false));
        assertEquals(LoadoutRules.INSTANT_COOLDOWN_TICKS, LoadoutRules.cooldown(0.0, true));
        assertTrue(LoadoutRules.INSTANT_COOLDOWN_TICKS > 0);
    }

    @Test
    void aSwapFromTheArmouryPutsTheOldSetBackInTheCellsTheNewOnesLeft() {
        List<ArmouryRulesTest.St> worn = list(LoadoutRules.SLOTS);
        worn.set(0, s("iron_helm"));
        worn.set(1, s("iron_chest"));
        worn.set(5, s("wood_sword"));
        List<ArmouryRulesTest.St> armoury = list(ArmouryRules.MAX_SLOTS);
        armoury.set(7, s("gold_helm"));
        armoury.set(9, s("gold_chest"));
        armoury.set(30, s("fire_sword"));
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template("gold_helm", "gold_chest", null, null, null, "fire_sword"), worn, list(27), armoury, 27, OPS, M);
        assertEquals(LoadoutRules.Status.OK, p.status());
        assertEquals(3, p.moved());
        assertEquals(0, p.missing());
        assertEquals(s("gold_helm"), p.worn().get(0));
        assertEquals(s("gold_chest"), p.worn().get(1));
        assertEquals(s("fire_sword"), p.worn().get(5));
        assertEquals(s("iron_helm"), p.armoury().get(7));
        assertEquals(s("iron_chest"), p.armoury().get(9));
        assertEquals(s("wood_sword"), p.armoury().get(30));
        assertEquals(6, total(p));
        // the inputs are never changed
        assertEquals(s("iron_helm"), worn.get(0));
        assertEquals(s("gold_helm"), armoury.get(7));
    }

    @Test
    void aSlotThatAlreadyMatchesAndAnEmptyTemplateSlotAreLeftAlone() {
        List<ArmouryRulesTest.St> worn = list(LoadoutRules.SLOTS);
        worn.set(0, s("gold_helm"));
        worn.set(3, s("my_boots"));                                   // the template says nothing about the feet
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template("gold_helm"), worn, list(27), list(54), 27, OPS, M);
        assertEquals(LoadoutRules.Status.UNCHANGED, p.status());
        assertFalse(p.applies());
        assertEquals(s("my_boots"), p.worn().get(3));
    }

    @Test
    void anItemThatNoLongerExistsIsSkippedAndCounted() {
        List<ArmouryRulesTest.St> worn = list(LoadoutRules.SLOTS);
        worn.set(0, s("iron_helm"));
        List<ArmouryRulesTest.St> armoury = list(54);
        armoury.set(2, s("gold_chest"));                              // the helm of the loadout is gone, the chest is there
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template("gold_helm", "gold_chest"), worn, list(27), armoury, 27, OPS, M);
        assertEquals(LoadoutRules.Status.PARTIAL, p.status());
        assertTrue(p.applies());
        assertEquals(1, p.moved());
        assertEquals(1, p.missing());
        assertEquals(s("iron_helm"), p.worn().get(0));                // the missing piece left the slot as it was
        assertEquals(s("gold_chest"), p.worn().get(1));
        // nothing at all found: no change
        LoadoutRules.Plan<ArmouryRulesTest.St> none = LoadoutRules.plan(template("gold_helm"), worn, list(27), list(54), 27, OPS, M);
        assertEquals(LoadoutRules.Status.NOTHING_FOUND, none.status());
        assertEquals(1, none.missing());
        assertFalse(none.applies());
    }

    @Test
    void aFullArmouryChangesNothing() {
        List<ArmouryRulesTest.St> worn = list(LoadoutRules.SLOTS);
        worn.set(0, s("iron_helm"));
        List<ArmouryRulesTest.St> armoury = list(54);
        List<ArmouryRulesTest.St> rest = list(27);
        rest.set(4, s("gold_helm"));                                  // the wanted helm is in the inventory, so the old helm needs a free armoury cell
        for (int i = 0; i < 3; i++) armoury.set(i, s("filler_sword_" + i));
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template("gold_helm"), worn, rest, armoury, 3, OPS, M);
        assertEquals(LoadoutRules.Status.ARMOURY_FULL, p.status());
        assertFalse(p.applies());
        assertEquals(s("iron_helm"), p.worn().get(0));
        assertEquals(s("gold_helm"), p.rest().get(4));
        // with one free cell it goes through, and the inventory slot is the one left empty
        armoury.set(2, NONE);
        LoadoutRules.Plan<ArmouryRulesTest.St> ok = LoadoutRules.plan(template("gold_helm"), worn, rest, armoury, 3, OPS, M);
        assertEquals(LoadoutRules.Status.OK, ok.status());
        assertEquals(s("gold_helm"), ok.worn().get(0));
        assertEquals(s("iron_helm"), ok.armoury().get(2));
        assertTrue(OPS.isEmpty(ok.rest().get(4)));
    }

    @Test
    void anEmptyLoadoutIsRefused() {
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template(), list(LoadoutRules.SLOTS), list(27), list(54), 27, OPS, M);
        assertEquals(LoadoutRules.Status.EMPTY_LOADOUT, p.status());
    }

    @Test
    void theExactCopyIsPreferredToTheDamagedOne() {
        List<ArmouryRulesTest.St> armoury = list(54);
        armoury.set(1, s("sword#damaged"));
        armoury.set(2, s("sword"));
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template(null, null, null, null, null, "sword"), list(LoadoutRules.SLOTS), list(27), armoury, 27, OPS, M);
        assertEquals(s("sword"), p.worn().get(5));
        assertEquals(s("sword#damaged"), p.armoury().get(1));
        // a damaged copy is found when it is all there is
        armoury.set(2, NONE);
        LoadoutRules.Plan<ArmouryRulesTest.St> q = LoadoutRules.plan(template(null, null, null, null, null, "sword"), list(LoadoutRules.SLOTS), list(27), armoury, 27, OPS, M);
        assertEquals(s("sword#damaged"), q.worn().get(5));
    }

    @Test
    void aWornItemInTheWrongSlotMovesWithoutBeingTakenFromASlotThatNeedsIt() {
        List<ArmouryRulesTest.St> worn = list(LoadoutRules.SLOTS);
        worn.set(5, s("axe"));                                        // hotbar 1 holds the axe, the loadout wants it in hotbar 2 and a sword in hotbar 1
        worn.set(6, s("sword"));
        LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(template(null, null, null, null, null, "sword", "axe"), worn, list(27), list(54), 27, OPS, M);
        assertTrue(p.applies());
        assertEquals(s("sword"), p.worn().get(5));
        assertEquals(s("axe"), p.worn().get(6));
        assertEquals(2, total(p));
    }

    @Test
    void cyclingGoesRoundTheSavedLoadouts() {
        boolean[] saved = {true, false, true, false, false, true};
        assertEquals(0, LoadoutRules.cycle(saved, -1, false));
        assertEquals(2, LoadoutRules.cycle(saved, 0, false));
        assertEquals(5, LoadoutRules.cycle(saved, 2, false));
        assertEquals(0, LoadoutRules.cycle(saved, 5, false));
        assertEquals(5, LoadoutRules.cycle(saved, 0, true));
        assertEquals(2, LoadoutRules.cycle(saved, 5, true));
        assertEquals(5, LoadoutRules.cycle(saved, -1, true));
        assertEquals(-1, LoadoutRules.cycle(new boolean[6], 2, false));
        assertEquals(3, LoadoutRules.cycle(new boolean[]{false, false, false, true, false, false}, 3, false));
    }

    /** Random worlds: whatever the plan does, it never changes the total, and a plan that does not apply returns exactly what it was given. */
    @Test
    void randomSwapsNeverLoseOrDoubleAnything() {
        Random rnd = new Random(7);
        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"};
        for (int run = 0; run < 400; run++) {
            List<ArmouryRulesTest.St> worn = list(LoadoutRules.SLOTS), rest = list(27), ar = list(54), tpl = list(LoadoutRules.SLOTS);
            int n = 0;
            for (int i = 0; i < worn.size(); i++) if (rnd.nextInt(3) > 0) { worn.set(i, s(names[rnd.nextInt(names.length)] + "#" + (n++))); }
            for (int i = 0; i < rest.size(); i++) if (rnd.nextInt(4) == 0) rest.set(i, s(names[rnd.nextInt(names.length)] + "#" + (n++)));
            for (int i = 0; i < ar.size(); i++) if (rnd.nextInt(3) == 0) ar.set(i, s(names[rnd.nextInt(names.length)] + "#" + (n++)));
            for (int i = 0; i < tpl.size(); i++) if (rnd.nextInt(2) == 0) tpl.set(i, s(names[rnd.nextInt(names.length)] + "#0"));
            int cap = 18 + rnd.nextInt(37);
            long before = ArmouryRules.total(worn, OPS) + ArmouryRules.total(rest, OPS) + ArmouryRules.total(ar, OPS);
            LoadoutRules.Plan<ArmouryRulesTest.St> p = LoadoutRules.plan(tpl, worn, rest, ar, cap, OPS, M);
            assertEquals(before, total(p), "run " + run + " " + p.status());
            if (!p.applies()) { assertSame(worn, p.worn()); assertSame(rest, p.rest()); assertSame(ar, p.armoury()); }
            // the multiset of items is the same (nothing turned into something else)
            List<String> a = items(worn, rest, ar), b = items(p.worn(), p.rest(), p.armoury());
            java.util.Collections.sort(a); java.util.Collections.sort(b);
            assertEquals(a, b, "run " + run);
        }
    }

    @SafeVarargs
    private static List<String> items(List<ArmouryRulesTest.St>... lists) {
        List<String> out = new ArrayList<>();
        for (List<ArmouryRulesTest.St> l : lists) for (ArmouryRulesTest.St x : l) if (!OPS.isEmpty(x)) out.add(x.item() + "x" + x.n());
        return out;
    }
}
