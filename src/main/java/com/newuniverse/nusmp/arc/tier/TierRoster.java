package com.newuniverse.nusmp.arc.tier;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the plain list of the tier-list characters that are built, with no game class behind it, so the client's player-model table (and the tests that read it) can know which ids the kit draws
 * without loading the entity code. {@link TierBosses#all()} is this list; a new tier is one more line here.
 */
public final class TierRoster {
    private TierRoster() {}

    /** Every light boss of the list that is built, tier F first (the order is the roster order and never changes; later tiers are appended). */
    public static List<TierRow> all() {
        List<TierRow> out = new ArrayList<>();
        out.addAll(TierRosterF.rows());
        out.addAll(TierRosterD.rows());
        return out;
    }

    /** The ids of every light boss that is built. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (TierRow r : all()) out.add(r.id());
        return out;
    }
}
