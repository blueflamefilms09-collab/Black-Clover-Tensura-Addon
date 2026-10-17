package com.newuniverse.nusmp.labyrinth.mega.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: the Rukh flow has to know whether a cast was Light or Dark, and the mod has 105 magic families, 500-odd spell pages, a Tensura skill list and
 * relics, written by many hands over many versions; no switch statement over any of them would survive the next update. So the answer is DATA
 * ({@code data/nusmp/mega/flow/alignments.json}) and this is the pure reader of it: a table of three things, applied in a fixed order that the
 * owner can read in one breath. (1) {@code exact}: an id with a deliberate entry (a family such as {@code lightning}, a page such as
 * {@code devil_union}, a Tensura skill) is exactly what the file says, and it beats every keyword, because only a person can know that Curse-Warding
 * is a ward and not a curse. (2) Keywords: the id is cut into whole words ({@code demon_light} is {@code demon}, {@code light}), and a word in
 * the {@code dark} list or the {@code light} list decides; WHOLE words, never substrings, so {@code lightning} and {@code lighten} are not "light". When both
 * lists hit, DARK wins: the safer reading, because a spell that is half demonic must not slip through the holy air as light. (3) Anything else
 * is NEUTRAL, which is what the flow ignores; a table that is empty or broken therefore makes the whole feature quietly do nothing instead
 * of boosting or punishing by accident. Several ids may be asked at once, most specific first (the page, then its family): the first id with an
 * exact entry decides alone, otherwise the keywords of all of them are weighed together. A merged magic ({@code merged:flame+light}) is asked
 * part by part and the strongest part (DARK, then LIGHT) speaks. {@link #parse} never throws: it returns the table it could build and the
 * problems it found, in plain English, for the loader to log. No Minecraft import, so JUnit feeds it the real file.
 */
public final class MagicAlignments {
    /** The table that classifies everything as NEUTRAL (before the first data load, or when the file is missing). */
    public static final MagicAlignments EMPTY = new MagicAlignments(Set.of(), Set.of(), Map.of());

    private static final String MERGED = "merged:";
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern ID = Pattern.compile("[a-z0-9_:./+]+");
    private static final Set<String> KNOWN_KEYS = Set.of("why", "light", "dark", "exact");

    /** Why: the answer and the reason together, so the {@code classify} command can say which rule spoke instead of just a colour. */
    public record Verdict(Alignment alignment, String rule) {}

    /** Why: the table AND the complaints together, so a loader can use what is usable and still report what is not. */
    public record Parsed(MagicAlignments table, List<String> problems) {}

    /** One id's answer before the ids are weighed together: {@code explicit} is true when it came from an exact entry. */
    private record V(Alignment alignment, boolean explicit, String rule) {}

    private static final V NOTHING = new V(Alignment.NEUTRAL, false, "no rule names it, so it is neutral");

    private final Set<String> light, dark;
    private final Map<String, Alignment> exact;

    private MagicAlignments(Set<String> light, Set<String> dark, Map<String, Alignment> exact) {
        this.light = light;
        this.dark = dark;
        this.exact = exact;
    }

    // ---------------------------------------------------------------- reading ids
    /** An id as the table compares it: trimmed, lower case, spaces and dashes as underscores ({@code "Demon-Light"} is {@code demon_light}); never null. */
    public static String normalize(String id) {
        if (id == null) return "";
        return id.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    /** The id without its namespace: {@code tensura:dark_cube} is {@code dark_cube}; an id with no colon is itself. */
    private static String path(String id) {
        int c = id.indexOf(':');
        return c < 0 ? id : id.substring(c + 1);
    }

    /** The whole words of an id: runs of lower-case letters and digits, split at everything else ({@code demon_light:strike} is demon, light, strike). */
    public static List<String> tokens(String id) {
        List<String> out = new ArrayList<>();
        String s = normalize(id);
        int start = -1;
        for (int i = 0; i <= s.length(); i++) {
            boolean word = i < s.length() && ((s.charAt(i) >= 'a' && s.charAt(i) <= 'z') || (s.charAt(i) >= '0' && s.charAt(i) <= '9'));
            if (word && start < 0) start = i;
            else if (!word && start >= 0) {
                out.add(s.substring(start, i));
                start = -1;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- classifying
    /** The alignment of a cast named by these ids, most specific first (a page id, then its family id). Null or blank ids are skipped; nothing named is NEUTRAL. */
    public Alignment classify(String... ids) { return explain(ids).alignment(); }

    /** Like {@link #classify} with the rule that decided, in plain English (for the {@code classify} command and the tests). */
    public Verdict explain(String... ids) {
        V best = null;
        if (ids != null) {
            for (String raw : ids) {
                V v = verdictOf(raw, true);
                if (v.explicit()) return new Verdict(v.alignment(), v.rule());
                best = best == null ? v : stronger(best, v);
            }
        }
        V r = best == null ? NOTHING : best;
        return new Verdict(r.alignment(), r.rule());
    }

    /** The answer for one id; {@code allowMerged} is false for the parts of a merged id (a part that is itself "merged:" is just a word). */
    private V verdictOf(String raw, boolean allowMerged) {
        String id = normalize(raw);
        if (id.isEmpty()) return NOTHING;
        if (allowMerged && id.startsWith(MERGED)) {
            V best = null;
            for (String part : id.substring(MERGED.length()).split("\\+")) {
                if (part.isBlank()) continue;
                V v = verdictOf(part, false);
                best = best == null ? v : stronger(best, v);
            }
            return best == null ? NOTHING : new V(best.alignment(), best.explicit(), "merged magic: " + best.rule());
        }
        String key = id;
        Alignment e = exact.get(key);
        if (e == null) {
            key = path(id);
            e = key.equals(id) ? null : exact.get(key);
        }
        if (e != null) return new V(e, true, "exact entry '" + key + "' is " + e.id());
        List<String> words = tokens(path(id));
        String darkHit = firstHit(words, dark), lightHit = firstHit(words, light);
        if (darkHit != null && lightHit != null) return new V(Alignment.DARK, false, "keywords '" + lightHit + "' (light) and '" + darkHit + "' (dark) both match, and dark wins");
        if (darkHit != null) return new V(Alignment.DARK, false, "keyword '" + darkHit + "' (dark)");
        if (lightHit != null) return new V(Alignment.LIGHT, false, "keyword '" + lightHit + "' (light)");
        return NOTHING;
    }

    private static String firstHit(List<String> words, Set<String> list) {
        for (String w : words) if (list.contains(w)) return w;
        return null;
    }

    private static int rank(Alignment a) { return a == Alignment.DARK ? 2 : a == Alignment.LIGHT ? 1 : 0; }

    /** DARK beats LIGHT beats NEUTRAL; on a tie an exact answer is kept over a keyword one, otherwise the earlier one stays. */
    private static V stronger(V a, V b) {
        int ra = rank(a.alignment()), rb = rank(b.alignment());
        if (rb > ra) return b;
        if (rb == ra && b.explicit() && !a.explicit()) return b;
        return a;
    }

    // ---------------------------------------------------------------- the table
    public boolean isEmpty() { return light.isEmpty() && dark.isEmpty() && exact.isEmpty(); }

    /** The words that make an id LIGHT (lower case, insertion order, read-only). */
    public Set<String> lightKeywords() { return light; }

    /** The words that make an id DARK (lower case, insertion order, read-only). */
    public Set<String> darkKeywords() { return dark; }

    /** The deliberate entries: normalised id to alignment (insertion order, read-only). */
    public Map<String, Alignment> exactEntries() { return exact; }

    // ---------------------------------------------------------------- parsing
    /**
     * Builds the table from the already-parsed JSON tree (maps, lists, strings: the loader converts Gson's tree into it, which keeps this class free
     * of any library). Never throws. A keyword must be ONE word of letters and digits; an exact key must be an id; an exact value must be
     * "light", "dark" or "neutral" (or "none"). Whatever is wrong is skipped and reported, whatever is right is kept.
     */
    public static Parsed parse(Map<String, Object> json) {
        List<String> problems = new ArrayList<>();
        if (json == null) {
            problems.add("the file is not a JSON object");
            return new Parsed(EMPTY, problems);
        }
        for (String key : json.keySet()) if (!KNOWN_KEYS.contains(key)) problems.add("unknown key '" + key + "' (ignored)");
        Set<String> light = words(json.get("light"), "light", problems);
        Set<String> dark = words(json.get("dark"), "dark", problems);
        for (String w : light) if (dark.contains(w)) problems.add("keyword '" + w + "' is in both lists (dark wins)");
        Map<String, Alignment> exact = new LinkedHashMap<>();
        Object ex = json.get("exact");
        if (ex instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String k = normalize(String.valueOf(e.getKey()));
                if (!ID.matcher(k).matches()) {
                    problems.add("exact: '" + e.getKey() + "' is not an id (lower-case letters, digits and _ : . / + only)");
                    continue;
                }
                Alignment a = e.getValue() instanceof String s ? Alignment.fromId(s) : null;
                if (a == null) {
                    problems.add("exact: '" + k + "' must be \"light\", \"dark\" or \"neutral\"");
                    continue;
                }
                if (exact.put(k, a) != null) problems.add("exact: '" + k + "' is listed twice (the later one wins)");
            }
        } else if (ex != null) problems.add("'exact' must be an object of id to alignment");
        return new Parsed(new MagicAlignments(Collections.unmodifiableSet(light), Collections.unmodifiableSet(dark), Collections.unmodifiableMap(exact)), problems);
    }

    private static Set<String> words(Object value, String list, List<String> problems) {
        Set<String> out = new LinkedHashSet<>();
        if (value == null) return out;
        if (!(value instanceof List<?> l)) {
            problems.add("'" + list + "' must be a list of words");
            return out;
        }
        for (Object o : l) {
            String w = o instanceof String s ? normalize(s) : null;
            if (w == null || !WORD.matcher(w).matches()) {
                problems.add(list + ": '" + o + "' is not one word of letters and digits (ignored)");
                continue;
            }
            if (!out.add(w)) problems.add(list + ": '" + w + "' is listed twice");
        }
        return out;
    }
}
