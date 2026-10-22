package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.blackclover.MagicType;

/**
 * Why: Storm Magic is a Fairy Tail magic of the Stormcaller Staff, so the old Storm grimoire must stop being handed out, but it must stay registered (old saves, old grimoires, the
 * creative tab and the Kaiser Granvorka captain book keep it). This is the one place that says whether the magic is hidden from the ROLLS (a soul's pool, the starter pool, the wizard
 * tower): the config {@code storm.staffOnly} (default true). false restores the old behaviour exactly. Nothing here removes a book, a page or an enum constant, and a grimoire that
 * already is a Storm grimoire is never touched.
 */
public final class StormGate {
    private StormGate() {}

    /** True when this magic must not be rolled for a soul, a starter pool or the wizard tower right now (server config {@code storm.staffOnly}). */
    public static boolean hidden(MagicType t) { return t != null && StormRules.hiddenName(t.name(), StormConfig.staffOnly()); }

    /** How many magics the staff-only setting takes out of the "every magic" pool (0 or 1), for the pools that compare their size to the enum's. */
    public static int hiddenCount() { return StormConfig.staffOnly() ? 1 : 0; }
}
