package com.newuniverse.nusmp.corpse;

import java.util.List;
import java.util.Map;

/**
 * Why (owner report 2.1.0, "none of the new Soul Corpse summon spells work", docs/magic/soul_corpse_fix.md): the summon pages of Soul Corpse Magic were appended at the END of a 15 page book,
 * and a grimoire page opens only when the caster's mastery reaches its place in the book (page k of n) or its spell cost, and, for a signature page, the rank Senior as well. The
 * Marionette sat at 67 percent of the mastery bar, Necropolis and Legion (signature pages) at the full bar and the rank Senior, David at 87 percent, so nobody who held the grimoire could ever
 * choose them and "nothing happened" (the 1.46.0 fix that opened Reanimation for everyone had the same cause, and it never reached the pages that came after it). This pure class (no game types)
 * is the one written rule of who may open what and how early:
 * <ul>
 *   <li>{@link #GENERIC_SUMMONS}: the zombie pages every Soul Corpse holder may use (Reanimation, Marionette, Necropolis, Legion). Never character-locked.</li>
 *   <li>{@link #RADES_ONLY}: Rades Spirito's own pages (the five numbered undead and True Soul Corpse Magic); the lock lines are in {@code book.CharacterSpells} (kept as they are) and a test pins
 *       that the two lists agree.</li>
 *   <li>{@link #STARTER}: opened at once for every holder of the grimoire ({@code Corpses.openStarterPage}); Reanimation was the only one, the Marionette joins it.</li>
 *   <li>{@link #MASTERY_CAP}: the pages whose mastery cost is capped (a fraction of the bar, before the server's masteryScale) and whose rank gate is waived, so the strong summons are still
 *       earned by casting but are reachable: Necropolis 30 percent, Legion 50, David 60, True Soul Corpse 70, Michael Caesal 80. Their magicule cost, long cooldowns and the zombie caps are unchanged.
 *       A rank written for a page in the server config ({@code multiverse.pageGates}) still wins.</li>
 *   <li>{@link #slotFree}: those same pages (the starters and the capped ones) take no page slot. A grimoire has 6 slots growing to 12 with mastery (16 with a contract) and opens pages in
 *       book order, so in a 15 page book the LAST pages (David and Michael Caesal, the newest) were starved of a slot even at full mastery; the summon pages no longer count against the
 *       slots and are not stopped by them.</li>
 * </ul>
 */
public final class SoulCorpseAccess {
    private SoulCorpseAccess() {}

    /** The registry path of the Soul Corpse grimoire book (the key of {@code CharacterSpells} and of the page gates). */
    public static final String BOOK = "book_soul_corpse";

    public static final String REANIMATE = "corpse_reanimate", MARIONETTE = "corpse_marionette", SWAP = "corpse_swap", LEGION = "corpse_legion", NECROPOLIS = "attribute_domain";
    public static final String JIMMY = "corpse_jimmy", ALFREDO = "corpse_alfredo", CARL = "corpse_carl", DAVID = "corpse_david", MICHAEL = "corpse_michael_caesal", TRUE_PACT = "true_soul_corpse";

    /** Every family page of the Soul Corpse book in mode order, as shipped (modes never move: the unlock bit mask is the position). */
    public static final List<String> ORDER = List.of("attribute_bolt", "attribute_cut", "attribute_field", NECROPOLIS, "attribute_guard",
            REANIMATE, JIMMY, ALFREDO, CARL, TRUE_PACT, MARIONETTE, SWAP, LEGION, DAVID, MICHAEL);

    /** The pages that raise zombies for any Soul Corpse holder. */
    public static final List<String> GENERIC_SUMMONS = List.of(REANIMATE, MARIONETTE, NECROPOLIS, LEGION);

    /** Rades Spirito's exclusive pages (character-locked in {@code book.CharacterSpells}). */
    public static final List<String> RADES_ONLY = List.of(JIMMY, DAVID, ALFREDO, CARL, MICHAEL, TRUE_PACT);

    /** Opened at once for every holder of the Soul Corpse grimoire. */
    public static final List<String> STARTER = List.of(REANIMATE, MARIONETTE);

    /** Highest share of the mastery bar these pages ever ask for (before the server's masteryScale); their rank gate is waived. */
    public static final Map<String, Double> MASTERY_CAP = Map.of(NECROPOLIS, 0.30, LEGION, 0.50, DAVID, 0.60, TRUE_PACT, 0.70, MICHAEL, 0.80);

    /** True for the book of this registry path ("book_soul_corpse"). */
    public static boolean isBook(String registryPath) { return BOOK.equals(registryPath); }

    /** True when this page opens at once for everyone who holds the Soul Corpse grimoire. */
    public static boolean isStarter(String pageId) { return pageId != null && STARTER.contains(pageId); }

    /** True when the page is Rades Spirito's own (only his grimoire may open it). */
    public static boolean isRadesOnly(String pageId) { return pageId != null && RADES_ONLY.contains(pageId); }

    /** True when any Soul Corpse holder may open this page by mastery (every page that is not Rades's). */
    public static boolean openToEveryone(String pageId) { return pageId != null && !isRadesOnly(pageId); }

    /** Whether a caster may open this page: a Rades page needs Rades's grimoire, every other page is open to all. */
    public static boolean mayOpen(String pageId, boolean radesGrimoire) { return openToEveryone(pageId) || radesGrimoire; }

    /** True when the rank gate (Senior for a signature page, Intermediate for a zone page) does not apply to this page of this book. */
    public static boolean rankWaived(String registryPath, String pageId) { return isBook(registryPath) && pageId != null && MASTERY_CAP.containsKey(pageId); }

    /** True when the page takes no page slot of the grimoire and is not stopped when the slots are full (the starters and the capped summons of the Soul Corpse book). */
    public static boolean slotFree(String registryPath, String pageId) { return isBook(registryPath) && pageId != null && (isStarter(pageId) || MASTERY_CAP.containsKey(pageId)); }

    /**
     * The mastery a page asks for: 'base' (the book's own rule, 0..1, already times the server scale) for a page with no cap, else the smaller of 'base' and the cap times 'scale'
     * (never above 1). A starter page asks nothing. Pages of other books are returned as they are.
     */
    public static double needed(String registryPath, String pageId, double base, double scale) {
        if (!isBook(registryPath) || pageId == null) return base;
        if (isStarter(pageId)) return 0.0;
        Double cap = MASTERY_CAP.get(pageId);
        if (cap == null) return base;
        return Math.min(base, Math.min(1.0, cap * Math.max(0.0, scale)));
    }

    /** The mastery rule of the book itself (MasteryPages.masteryCost): the larger of the place k/n and the spell cost / 35, times the scale, never above 1. Mode 0 is free. */
    public static double bookRule(int mode, int pages, double costPercent, double scale) {
        if (mode <= 0) return 0.0;
        double byPlace = (double) mode / Math.max(1, pages);
        double byCost = Math.min(1.0, costPercent / 35.0);
        return Math.min(1.0, Math.max(byPlace, byCost) * scale);
    }
}
