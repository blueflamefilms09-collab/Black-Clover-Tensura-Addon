package com.newuniverse.nusmp.client.ui.codex;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Why: the holder of the data-driven half of the Codex. {@link CodexReloadListener} scans the resource packs on every reload (F3+T, a pack
 * toggled in the menu) and hands the merged result to {@link #replace}; the pages read it through {@link #get}. The snapshot is replaced
 * whole, never edited, so a render thread reading it while a reload lands on another thread sees one consistent book. This class is the pure
 * core (no Minecraft types): it also owns the small text rules of the format (tab titles, glyphs, lang keys, the "???" rule) so they are tested.
 */
public final class CodexData {
    private CodexData() {}

    /** An immutable view of the loaded sections; {@code generation} grows by one on every reload so a page can tell its cache is stale. */
    public record Snapshot(Map<String, List<CodexEntryData>> sections, int generation) {
        public static final Snapshot EMPTY = new Snapshot(Map.of(), 0);
    }

    private static volatile Snapshot current = Snapshot.EMPTY;

    public static Snapshot get() { return current; }

    /** Installs a freshly merged book (see {@link CodexMerger#merge}). */
    public static void replace(Map<String, List<CodexEntryData>> merged) {
        Map<String, List<CodexEntryData>> copy = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<CodexEntryData>> e : merged.entrySet()) copy.put(e.getKey(), List.copyOf(e.getValue()));
        current = new Snapshot(java.util.Collections.unmodifiableMap(copy), current.generation() + 1);
    }

    // ---------------------------------------------------------------- the small text rules of the format

    /** "spirit_lords" -> "Spirit Lords"; dashes and slashes count as spaces. */
    public static String prettify(String name) {
        if (name == null) return "";
        StringBuilder b = new StringBuilder();
        for (String part : name.replace('-', '_').replace('/', '_').split("_")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    /** The tab glyph: the first letter of the title, upper case; "?" for an empty title. */
    public static String glyph(String title) {
        if (title == null) return "?";
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (Character.isLetterOrDigit(c)) return String.valueOf(Character.toUpperCase(c));
        }
        return "?";
    }

    private static final Pattern LANG_KEY = Pattern.compile("[a-z0-9_-]+(\\.[a-z0-9_/-]+)+");

    /** Does this text look like a lang key ("nusmp.codex.rule.x.title": lower case, dots, no spaces)? The caller still checks that the key exists. */
    public static boolean looksLikeLangKey(String s) { return s != null && LANG_KEY.matcher(s).matches(); }

    /** The lang key of a section's tab title. */
    public static String sectionKey(String section) { return "nusmp.codex.section." + section; }

    /**
     * The discovery rule: an entry with no keys is always shown; one with keys is shown only if the asker says any one of them is visible.
     * {@code visibleAny} is the game's discovery cache ({@code DiscoveryCache::visibleAny}); a test passes a fake.
     */
    public static boolean shown(String keys, Predicate<String> visibleAny) {
        return keys == null || keys.isBlank() || visibleAny.test(keys);
    }

    /** "item:nusmp:foo" -> "nusmp:foo"; anything else (or an empty icon) -> null. */
    public static String iconItemId(String icon) {
        if (icon == null || !icon.toLowerCase(Locale.ROOT).startsWith("item:")) return null;
        String id = icon.substring(5).trim();
        return id.isEmpty() ? null : id;
    }
}
