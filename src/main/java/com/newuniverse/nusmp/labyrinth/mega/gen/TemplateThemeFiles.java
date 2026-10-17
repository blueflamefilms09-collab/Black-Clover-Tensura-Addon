package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: {@link TemplateThemes} is the rule that lets a room belong to a theme; this is how the rule is filled from the data pack. Each theme's builder ships ONE file,
 * {@code data/nusmp/mega/template_themes/<theme>.json} ({@code {"theme": "holy_church", "templates": ["nusmp:mega/hall/holy_church_"], "relative": {"mega/hall/holy_church_": 8}}}),
 * and a mistake in such a file must be loud, not silent: a theme id that names no theme would make the rooms unreachable (no sector ever wears it), a prefix with
 * the wrong spelling would leave the rooms free for every theme (a Holy Church nave in a Spider Warren), and a typo in a key would be ignored. So the pure parser
 * takes the files as plain Java (the game-side loader turns the JSON into maps), builds the rule set and returns what it did not understand as sentences, which the
 * loader logs and {@code mega_check.py} reports at build time. The {@code relative} weights are not read here: they belong to the pool generator
 * ({@code gen_mega_pools.py}), which writes them into the pools; the game never needs them at run time.
 */
public final class TemplateThemeFiles {
    private TemplateThemeFiles() {}

    /** The namespace a prefix gets when the file does not write one (all Mega templates live in {@code nusmp}). */
    private static final String NAMESPACE = "nusmp:";

    /**
     * The rule set the files describe. {@code docs} is one parsed JSON object per file (each may be a {@code Map<String,Object>}; anything else is reported and
     * skipped); {@code problems} receives one sentence per thing that was not understood (unknown key, missing or unknown theme, empty or non-text prefix list).
     * A file with a problem still contributes what is valid in it.
     */
    public static TemplateThemes parse(List<?> docs, List<String> problems) {
        TemplateThemes rules = TemplateThemes.none();
        if (docs == null) return rules;
        int n = 0;
        for (Object doc : docs) {
            n++;
            if (!(doc instanceof Map<?, ?> m)) {
                problems.add("template_themes file #" + n + " is not a JSON object");
                continue;
            }
            for (Object key : m.keySet()) {
                String k = String.valueOf(key);
                if (!k.equals("theme") && !k.equals("templates") && !k.equals("relative") && !k.startsWith("_")) problems.add("template_themes file #" + n + ": unknown key '" + k + "'");
            }
            Object themeObj = m.get("theme");
            String theme = themeObj instanceof String s ? s.trim().toLowerCase(Locale.ROOT) : "";
            if (theme.isEmpty()) {
                problems.add("template_themes file #" + n + " names no theme");
                continue;
            }
            if (!isTheme(theme)) {
                problems.add("template_themes file #" + n + ": '" + theme + "' is not a theme id (no sector would ever wear it, so its rooms would never be drawn)");
                continue;
            }
            Object list = m.get("templates");
            List<String> prefixes = new ArrayList<>();
            if (list instanceof List<?> l) {
                for (Object o : l) {
                    if (o instanceof String p && !p.isBlank()) prefixes.add(normalise(p));
                    else problems.add("template_themes file for '" + theme + "': a prefix is not a non-empty text");
                }
            } else {
                problems.add("template_themes file for '" + theme + "' has no 'templates' list");
            }
            if (prefixes.isEmpty()) problems.add("template_themes file for '" + theme + "' claims no template");
            rules = rules.with(theme, prefixes);
        }
        return rules;
    }

    /** A prefix as the pool entries spell templates: with the namespace ({@code mega/hall/x_} becomes {@code nusmp:mega/hall/x_}). */
    public static String normalise(String prefix) {
        String p = prefix.trim();
        return p.indexOf(':') >= 0 ? p : NAMESPACE + p;
    }

    /** Is this the id of a {@link RegionTheme}? */
    public static boolean isTheme(String id) {
        if (id == null) return false;
        for (RegionTheme t : RegionTheme.values()) if (t.id().equalsIgnoreCase(id.trim())) return true;
        return false;
    }
}
