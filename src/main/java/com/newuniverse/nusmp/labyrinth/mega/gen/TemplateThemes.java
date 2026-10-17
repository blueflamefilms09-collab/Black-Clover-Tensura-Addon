package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: until 1.58.0 every template of the Mega-Dungeon was theme-neutral (a theme only swapped the block palette), so a Holy Church could never have a
 * nave of its own and a Cursed Archive could never have a reading hall of its own: a pool drew the same rooms in every sector. This is the one rule that
 * lets a template belong to a theme: a template whose id starts with a registered prefix may be drawn ONLY in sectors of the themes registered for that
 * prefix; a template that matches no prefix stays free for every theme (so the 122 rooms that exist keep working exactly as before). It is pure data
 * (no game types), loaded from {@code data/nusmp/mega/template_themes/<theme>.json} ({@code {"theme": "holy_church", "templates": ["nusmp:mega/hall/holy_church_"]}})
 * by the generator's data loader, and consulted by the assembler when it draws a pool entry for a sector. Longest matching prefix wins, so a theme can claim
 * one folder and a more specific prefix can hand a single room to another theme.
 */
public final class TemplateThemes {
    private final Map<String, Set<String>> byPrefix;

    private TemplateThemes(Map<String, Set<String>> byPrefix) {
        this.byPrefix = byPrefix;
    }

    /** No restriction at all: every template is allowed in every theme. */
    public static TemplateThemes none() { return new TemplateThemes(Map.of()); }

    /** A rule set from prefix -> theme ids (copied, lower-cased; an empty set or a blank prefix is dropped). */
    public static TemplateThemes of(Map<String, ? extends Iterable<String>> rules) {
        Map<String, Set<String>> out = new HashMap<>();
        if (rules != null) {
            for (Map.Entry<String, ? extends Iterable<String>> e : rules.entrySet()) {
                String prefix = e.getKey() == null ? "" : e.getKey().trim();
                if (prefix.isEmpty() || e.getValue() == null) continue;
                Set<String> themes = new HashSet<>();
                for (String t : e.getValue()) if (t != null && !t.isBlank()) themes.add(t.trim().toLowerCase(Locale.ROOT));
                if (!themes.isEmpty()) out.put(prefix, themes);
            }
        }
        return new TemplateThemes(out);
    }

    /** Adds one theme file's claim: every prefix in {@code prefixes} now also allows {@code theme}. Returns a new rule set. */
    public TemplateThemes with(String theme, List<String> prefixes) {
        Map<String, Set<String>> out = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : byPrefix.entrySet()) out.put(e.getKey(), new HashSet<>(e.getValue()));
        if (theme != null && !theme.isBlank() && prefixes != null) {
            String t = theme.trim().toLowerCase(Locale.ROOT);
            for (String p : prefixes) if (p != null && !p.isBlank()) out.computeIfAbsent(p.trim(), k -> new HashSet<>()).add(t);
        }
        return new TemplateThemes(out);
    }

    /** May this template be drawn in a sector of this theme? True when no prefix claims it, or the longest claiming prefix lists the theme. */
    public boolean allows(String templateId, String themeId) {
        if (templateId == null) return false;
        String best = null;
        for (String prefix : byPrefix.keySet()) {
            if (templateId.startsWith(prefix) && (best == null || prefix.length() > best.length())) best = prefix;
        }
        if (best == null) return true;
        return themeId != null && byPrefix.get(best).contains(themeId.toLowerCase(Locale.ROOT));
    }

    /** Is this template reserved for some theme (so it is NOT free for every sector)? */
    public boolean restricted(String templateId) {
        if (templateId == null) return false;
        for (String prefix : byPrefix.keySet()) if (templateId.startsWith(prefix)) return true;
        return false;
    }

    /** The themes that may draw this template, or an empty list when it is free for all. */
    public List<String> themesOf(String templateId) {
        String best = null;
        if (templateId != null) for (String prefix : byPrefix.keySet()) if (templateId.startsWith(prefix) && (best == null || prefix.length() > best.length())) best = prefix;
        if (best == null) return List.of();
        List<String> out = new ArrayList<>(byPrefix.get(best));
        java.util.Collections.sort(out);
        return out;
    }

    public int size() { return byPrefix.size(); }
}
