package com.newuniverse.nusmp.client.ui.codex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the one place that decides what the book shows when several packs write to the same section. Entries arrive in load order (lowest
 * priority pack first, as the resource manager lists them); an entry with the same section and id as an earlier one replaces it, and a
 * {@code remove} entry deletes it, so a server pack can retire or rewrite a stock rule without editing the mod. The result is sorted (order, then
 * title, then id) so the tabs look the same on every client and in every test. Pure, no Minecraft types.
 */
public final class CodexMerger {
    private CodexMerger() {}

    /** Sections by name (sorted alphabetically, case-insensitive); sections left with no entry are dropped. */
    public static Map<String, List<CodexEntryData>> merge(List<CodexEntryData> loadOrder) {
        Map<String, CodexEntryData> byKey = new LinkedHashMap<>();
        for (CodexEntryData e : loadOrder) {
            if (e == null || e.section().isEmpty() || e.id().isEmpty()) continue;
            String key = e.section() + "\u0000" + e.id();
            if (e.remove()) byKey.remove(key);
            else byKey.put(key, e);
        }
        Map<String, List<CodexEntryData>> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CodexEntryData e : byKey.values()) out.computeIfAbsent(e.section(), s -> new ArrayList<>()).add(e);
        Comparator<CodexEntryData> cmp = Comparator.comparingInt(CodexEntryData::order)
                .thenComparing(e -> e.title().toLowerCase(Locale.ROOT)).thenComparing(CodexEntryData::id);
        for (List<CodexEntryData> l : out.values()) l.sort(cmp);
        return out;
    }
}
