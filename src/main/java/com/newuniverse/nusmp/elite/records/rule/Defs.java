package com.newuniverse.nusmp.elite.records.rule;

import java.util.List;
import java.util.Set;

/**
 * Why: the immutable definitions the data files describe: a record (what to do), a title (what it needs, what it gives) and a synergy (which held titles together give more). Plain records so the
 * parsers, the catalog, the sync packets and the tests all pass the same small objects around. Pure.
 */
public final class Defs {
    private Defs() {}

    /** One record: an achievement with a rarity, a category, a hidden flag (shown as ??? until unlocked) and the one condition that unlocks it. */
    public record RecordDef(String id, String name, String description, Category category, Rarity rarity, boolean hidden, Condition condition) {}

    /** One title: earned when ALL requirements hold, kept for good, one of the held ones is active. {@code tags} feed the synergy matcher ({@code rarity:<id>} is implicit). */
    public record TitleDef(String id, String name, String description, Rarity rarity, Set<String> tags, List<Requirement> requirements, Bonuses bonuses) {}

    /**
     * One synergy: applies while the HELD titles contain all of {@code requires} AND (when {@code tag} is not empty) at least {@code tagCount} held titles carry {@code tag}
     * ({@code "*"} counts every held title). At least one of the two parts must be present.
     */
    public record SynergyDef(String id, String name, String description, List<String> requires, String tag, int tagCount, Bonuses bonuses) {}

    public static boolean validId(String id) {
        if (id == null || id.length() < 2 || id.length() > 64) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.')) return false;
        }
        return true;
    }
}
