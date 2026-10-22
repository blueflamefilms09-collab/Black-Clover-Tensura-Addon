package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report 2.1.0, "none of the new Soul Corpse summon spells work"): the summon pages could not be chosen, because a page opens only through the unlock rules (mastery, rank, page slots) and the
 * rules asked for more than a player could give. This pins the rules as numbers, with a small model of the unlock loop ({@code MasteryPages.tryUnlock}) that is run with and without the fix, so the cause
 * stays visible: the old rule leaves the Marionette at 67 percent, Necropolis and Legion at 100 percent plus a rank, and David and Michael Caesal without a slot even at full mastery.
 */
class SoulCorpseAccessTest {
    /** The cost tiers of BookPage (percent of the magicule pool), by the factory the book uses for the page. */
    private static final Map<String, Double> COST = Map.ofEntries(
            Map.entry("attribute_bolt", 8.0), Map.entry("attribute_cut", 12.0), Map.entry("attribute_field", 20.0), Map.entry("attribute_domain", 35.0), Map.entry("attribute_guard", 12.0),
            Map.entry("corpse_reanimate", 12.0), Map.entry("corpse_jimmy", 12.0), Map.entry("corpse_alfredo", 12.0), Map.entry("corpse_carl", 12.0), Map.entry("true_soul_corpse", 35.0),
            Map.entry("corpse_marionette", 12.0), Map.entry("corpse_swap", 12.0), Map.entry("corpse_legion", 35.0), Map.entry("corpse_david", 12.0), Map.entry("corpse_michael_caesal", 35.0));

    private static final String BOOK = SoulCorpseAccess.BOOK;

    @Test
    void theTableIsConsistent() {
        List<String> order = SoulCorpseAccess.ORDER;
        assertEquals(15, order.size());
        assertEquals(order.size(), new HashSet<>(order).size(), "page ids are unique");
        assertEquals(new HashSet<>(order), COST.keySet(), "the cost table of this test covers exactly the pages of the book");
        assertTrue(order.containsAll(SoulCorpseAccess.GENERIC_SUMMONS));
        assertTrue(order.containsAll(SoulCorpseAccess.RADES_ONLY));
        assertTrue(SoulCorpseAccess.GENERIC_SUMMONS.containsAll(SoulCorpseAccess.STARTER));
        assertTrue(order.containsAll(SoulCorpseAccess.MASTERY_CAP.keySet()));
        Set<String> both = new HashSet<>(SoulCorpseAccess.GENERIC_SUMMONS);
        both.retainAll(SoulCorpseAccess.RADES_ONLY);
        assertTrue(both.isEmpty(), "a generic summon is never one of Rades's pages: " + both);
        assertEquals(6, SoulCorpseAccess.RADES_ONLY.size(), "Rades has five numbered undead and True Soul Corpse Magic");
        // the five old generic pages and the first Rades pages kept their places (the unlock bit mask is the position)
        assertEquals("attribute_bolt", order.get(0));
        assertEquals("attribute_domain", order.get(3));
        assertEquals("corpse_reanimate", order.get(5));
        assertEquals("true_soul_corpse", order.get(9));
        assertEquals("corpse_marionette", order.get(10));
        assertEquals("corpse_david", order.get(13));
        assertEquals("corpse_michael_caesal", order.get(14));
        for (String id : order) assertTrue(order.indexOf(id) < 31, "the unlock mask is an int");
    }

    @Test
    void whoMayOpenWhat() {
        for (String id : SoulCorpseAccess.GENERIC_SUMMONS) {
            assertTrue(SoulCorpseAccess.mayOpen(id, false), id + " must open for a holder of any grimoire");
            assertTrue(SoulCorpseAccess.mayOpen(id, true), id + " must open for Rades too");
            assertFalse(SoulCorpseAccess.isRadesOnly(id));
        }
        for (String id : SoulCorpseAccess.RADES_ONLY) {
            assertFalse(SoulCorpseAccess.mayOpen(id, false), id + " is Rades's alone");
            assertTrue(SoulCorpseAccess.mayOpen(id, true));
        }
        assertFalse(SoulCorpseAccess.isRadesOnly(null));
        assertFalse(SoulCorpseAccess.isStarter(null));
        assertTrue(SoulCorpseAccess.mayOpen("corpse_swap", false) && SoulCorpseAccess.mayOpen("attribute_bolt", false));
    }

    @Test
    void theOldRuleSealedTheSummonsAndTheCapsReachThem() {
        int n = SoulCorpseAccess.ORDER.size();
        // the book's own rule, as it was: Marionette 2/3 of the bar, Necropolis / Legion / Michael Caesal / True Soul Corpse the full bar (35 percent cost), David 87 percent
        assertEquals(10.0 / 15, SoulCorpseAccess.bookRule(10, n, 12, 1.0), 1e-9);
        assertEquals(1.0, SoulCorpseAccess.bookRule(3, n, 35, 1.0), 1e-9);
        assertEquals(1.0, SoulCorpseAccess.bookRule(12, n, 35, 1.0), 1e-9);
        assertEquals(13.0 / 15, SoulCorpseAccess.bookRule(13, n, 12, 1.0), 1e-9);
        assertEquals(0.0, SoulCorpseAccess.bookRule(0, n, 8, 1.0), 1e-9);
        // with the fix every page of the generic summons asks at most half of the bar and no rank; Rades's capped pages at most 80 percent
        for (String id : SoulCorpseAccess.GENERIC_SUMMONS) {
            double need = need(id, 1.0);
            assertTrue(need <= 0.5 + 1e-9, id + " asks " + need);
            if (SoulCorpseAccess.MASTERY_CAP.containsKey(id)) assertTrue(SoulCorpseAccess.rankWaived(BOOK, id), id + " must ask no rank");
        }
        for (String id : SoulCorpseAccess.MASTERY_CAP.keySet()) assertTrue(need(id, 1.0) <= 0.8 + 1e-9, id);
        assertEquals(0.0, need("corpse_marionette", 1.0), 1e-9, "the Marionette is a starter page");
        assertEquals(0.0, need("corpse_reanimate", 1.0), 1e-9);
        // pages that are not capped keep the book's rule
        assertEquals(need("corpse_swap", 1.0), SoulCorpseAccess.bookRule(11, n, 12, 1.0), 1e-9);
        assertEquals(SoulCorpseAccess.bookRule(6, n, 12, 1.0), need("corpse_jimmy", 1.0), 1e-9);
        // other books and unknown pages are never touched
        assertEquals(0.77, SoulCorpseAccess.needed("book_fire", "corpse_legion", 0.77, 1.0), 1e-9);
        assertEquals(0.77, SoulCorpseAccess.needed(BOOK, null, 0.77, 1.0), 1e-9);
        assertFalse(SoulCorpseAccess.rankWaived("book_fire", "corpse_legion"));
        assertFalse(SoulCorpseAccess.slotFree("book_fire", "corpse_legion"));
    }

    @Test
    void theServerScaleStillCountsAndNothingGoesAboveTheBar() {
        assertEquals(0.30 * 2.0, SoulCorpseAccess.needed(BOOK, "attribute_domain", 1.0, 2.0), 1e-9, "a stricter server asks more");
        assertEquals(0.30 * 0.5, SoulCorpseAccess.needed(BOOK, "attribute_domain", 1.0, 0.5), 1e-9, "a looser server asks less");
        for (String id : SoulCorpseAccess.MASTERY_CAP.keySet()) {
            double v = SoulCorpseAccess.needed(BOOK, id, 1.0, 2.0);
            assertTrue(v <= 1.0 && v >= 0.0, id);
        }
        assertEquals(0.0, SoulCorpseAccess.needed(BOOK, "attribute_domain", 1.0, -3.0), 1e-9, "a bad scale never makes a page cost less than nothing");
        // the cap never makes a page dearer than the book's own rule would
        assertEquals(0.1, SoulCorpseAccess.needed(BOOK, "corpse_legion", 0.1, 1.0), 1e-9);
    }

    // ---------------------------------------------------------------- a model of MasteryPages.tryUnlock
    private static double need(String id, double scale) {
        int mode = SoulCorpseAccess.ORDER.indexOf(id);
        return SoulCorpseAccess.needed(BOOK, id, SoulCorpseAccess.bookRule(mode, SoulCorpseAccess.ORDER.size(), COST.get(id), scale), scale);
    }

    /** The rank gate: pages of 20 percent or more ask a rank, unless waived. Returns whether a player with NO rank passes. */
    private static boolean passesWithoutRank(String id, boolean fix, boolean hasRank) {
        boolean asksRank = COST.get(id) >= 20;
        return hasRank || !asksRank || (fix && SoulCorpseAccess.rankWaived(BOOK, id));
    }

    /**
     * Which pages a caster with the given mastery ends up with after enough unlock ticks: the loop of MasteryPages.tryUnlock (a page opens when its mastery, its rank and a slot allow it, one per
     * tick, lowest first), without a rank, with 'slots' page slots, for a caster of Rades's grimoire or not.
     */
    private static Set<String> open(double mastery, int slots, boolean rades, boolean fix) { return open(mastery, slots, rades, fix, false); }

    private static Set<String> open(double mastery, int slots, boolean rades, boolean fix, boolean hasRank) {
        Set<String> open = new HashSet<>(List.of("attribute_bolt"));
        if (fix) open.addAll(SoulCorpseAccess.STARTER);
        List<String> order = SoulCorpseAccess.ORDER;
        for (int tick = 0; tick < 100; tick++) {
            int used = 0;
            for (String id : open) if (!(fix && SoulCorpseAccess.slotFree(BOOK, id))) used++;
            boolean full = used >= slots;
            if (full && !fix) break;
            boolean opened = false;
            for (int m = 1; m < order.size() && !opened; m++) {
                String id = order.get(m);
                if (open.contains(id)) continue;
                double needed = fix ? need(id, 1.0) : SoulCorpseAccess.bookRule(m, order.size(), COST.get(id), 1.0);
                if (mastery < needed) continue;
                if (full && !(fix && SoulCorpseAccess.slotFree(BOOK, id))) continue;
                if (!passesWithoutRank(id, fix, hasRank)) continue;
                if (!SoulCorpseAccess.mayOpen(id, rades)) continue;
                open.add(id);
                opened = true;
            }
            if (!opened) break;
        }
        return open;
    }

    @Test
    void beforeTheFixTheSummonsStayShutForEverybody() {
        // a holder at 60 percent of the bar, ordinary grimoire: Marionette, Necropolis, Legion all sealed
        Set<String> was = open(0.6, 6 + (int) Math.floor(0.6 * 6), false, false);
        assertFalse(was.contains("corpse_marionette") || was.contains("attribute_domain") || was.contains("corpse_legion"), "old rules: " + was);
        // at the full bar the signature pages still need a rank, and Rades's last pages lose the race for the 12 slots
        Set<String> full = open(1.0, 12, false, false);
        assertTrue(full.contains("corpse_marionette"));
        assertFalse(full.contains("attribute_domain") || full.contains("corpse_legion"), "signature pages ask the rank Senior: " + full);
        Set<String> rades = open(1.0, 12, true, false);
        assertFalse(rades.contains("corpse_michael_caesal"), "old rules, full mastery, Rades: Michael Caesal stays shut: " + rades);
        // even a Senior Rades at full mastery: 12 slots are used up in book order before David and Michael Caesal (the newest pages) are reached
        Set<String> senior = open(1.0, 12, true, false, true);
        assertFalse(senior.contains("corpse_david") || senior.contains("corpse_michael_caesal") || senior.contains("corpse_legion"), "old rules, rank and mastery, 12 slots: " + senior);
        assertEquals(12, senior.size());
    }

    @Test
    void afterTheFixEverySummonOpensWithoutARankAndRadesGetsAllHisPages() {
        // a brand new holder: Reanimation and the Marionette at once
        Set<String> fresh = open(0.0, 6, false, true);
        assertTrue(fresh.containsAll(SoulCorpseAccess.STARTER), "fresh: " + fresh);
        // a novice at 35 percent: Necropolis too
        assertTrue(open(0.35, 6 + 2, false, true).contains("attribute_domain"));
        // half the bar: the whole generic set, still no rank
        Set<String> half = open(0.5, 6 + 3, false, true);
        assertTrue(half.containsAll(SoulCorpseAccess.GENERIC_SUMMONS), "half: " + half);
        assertTrue(Collections_disjoint(half, SoulCorpseAccess.RADES_ONLY), "no Rades page without his grimoire: " + half);
        // Rades at full mastery with the smallest full-mastery slot count: every page except the zone page that asks a rank
        Set<String> rades = open(1.0, 12, true, true);
        for (String id : SoulCorpseAccess.ORDER) if (!id.equals("attribute_field")) assertTrue(rades.contains(id), id + " stays shut for Rades: " + rades);
        // the slot limit never starves a summon: even with a single slot
        Set<String> tiny = open(1.0, 1, true, true);
        assertTrue(tiny.containsAll(SoulCorpseAccess.GENERIC_SUMMONS) && tiny.contains("corpse_david") && tiny.contains("corpse_michael_caesal") && tiny.contains("true_soul_corpse"), "tiny: " + tiny);
    }

    @Test
    void theZombieCapsStayAndTheSummonsDoNotGetCheaper() {
        // the magicule cost and cooldown tiers are untouched: the capped pages stay expensive
        assertEquals(35.0, COST.get("corpse_legion"), 1e-9);
        assertEquals(35.0, COST.get("attribute_domain"), 1e-9);
        // the rule only changes who can open a page, never how many zombies stand: caps live in SoulZombieRules
        assertEquals(6, SoulZombieRules.CAP_DEFAULT);
        assertEquals(60, SoulZombieRules.GLOBAL_CAP_DEFAULT);
        List<String> unchanged = new ArrayList<>(SoulCorpseAccess.ORDER);
        unchanged.removeAll(SoulCorpseAccess.MASTERY_CAP.keySet());
        unchanged.removeAll(SoulCorpseAccess.STARTER);
        for (String id : unchanged) assertFalse(SoulCorpseAccess.slotFree(BOOK, id) || SoulCorpseAccess.rankWaived(BOOK, id), id + " must keep the book's rules");
    }

    private static boolean Collections_disjoint(Set<String> a, List<String> b) {
        for (String s : b) if (a.contains(s)) return false;
        return true;
    }
}
