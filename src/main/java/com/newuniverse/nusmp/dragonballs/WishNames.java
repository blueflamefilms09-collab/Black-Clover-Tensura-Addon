package com.newuniverse.nusmp.dragonballs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: the overpowered wishes name a race, a skill, an item, a relic or a rank in free words ("I wish to become a high orc", {@code /dragonwish skill eye_of_truth}). This pure class turns a typed or spoken
 * sentence into ONE entry of a runtime list, fuzzily: case, spaces, underscores and a namespace do not matter, a partial word is enough when it is not ambiguous ("dem lord"), and a name with no spaces ("demonlord")
 * works. The lists themselves are read from the live registries at run time ({@link WishCatalog}), never copied here; this class only knows how to build unique display names and how to match. Pure: no Minecraft
 * type, so {@code WishNamesTest} runs without a game. Worst case per call: a few passes over one list (a few thousand entries) and a sentence of at most 512 characters.
 */
public final class WishNames {
    private WishNames() {}

    /** One thing that can be wished for. {@code id}: the registry id ("tensura:high_orc"). {@code name}: the unique display name. {@code aliases}: other names (a translated name). {@code tag}: a free label (a tier, a grade). */
    public record Entry(String id, String name, List<String> aliases, String tag) {
        public Entry(String id, String name) { this(id, name, List.of(), ""); }
    }

    public enum Status { OK, AMBIGUOUS, NONE }

    /** {@code entries}: the entries found (one for OK, several for AMBIGUOUS or for a multi-name resolve, none for NONE). */
    public record Result(Status status, List<Entry> entries) {
        public Entry first() { return entries.isEmpty() ? null : entries.get(0); }
    }

    /** Words a wish sentence has around the name; they never count as part of a name in the loose match. */
    private static final Set<String> FILLER = Set.of("i", "wish", "wishes", "to", "for", "a", "an", "the", "be", "become", "becoming", "me", "my", "myself", "into", "turn", "transform", "evolve", "reincarnate",
            "as", "change", "make", "give", "want", "would", "like", "please", "race", "skill", "skills", "ability", "abilities", "rank", "rate", "item", "items", "weapon", "learn", "know", "have", "get", "with",
            "and", "of", "x", "shenron", "dragon", "set", "pick", "choose", "name", "called", "named", "treasure", "gift", "rare", "stack", "stacks", "some", "many", "lots", "new", "random");

    public static final int MAX_CANDIDATES = 8;

    // ---------------------------------------------------------------- display names
    /** "high_orc" -> "High Orc" (the path of an id, title cased). */
    public static String titleOf(String id) {
        String path = id == null ? "" : id.substring(id.indexOf(':') + 1);
        StringBuilder b = new StringBuilder();
        for (String w : path.split("[_\\s/]+")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    /**
     * Builds the entries of a list of ids. The name is the title of the path ("High Orc"); when two ids share a path (two mods) the namespace is added ("Fire Resistance (othermod)"), so every name is unique.
     * {@code translated} (may be null) gives a second name per id (the language file's), added as an alias when it looks like a real name and not a language key. {@code tags} (may be null) gives the label.
     */
    public static List<Entry> build(List<String> ids, Map<String, String> translated, Map<String, String> tags) {
        Map<String, Integer> paths = new HashMap<>();
        for (String id : ids) paths.merge(titleOf(id).toLowerCase(Locale.ROOT), 1, Integer::sum);
        List<Entry> out = new ArrayList<>(ids.size());
        Map<String, Boolean> seen = new HashMap<>();
        for (String id : ids) {
            if (id == null || seen.put(id, true) != null) continue;
            String t = titleOf(id);
            String name = paths.get(t.toLowerCase(Locale.ROOT)) > 1 ? t + " (" + (id.contains(":") ? id.substring(0, id.indexOf(':')) : "?") + ")" : t;
            String tr = translated == null ? null : translated.get(id);
            List<String> alias = tr != null && looksTranslated(tr) && !norm(tr).equals(norm(t)) ? List.of(tr) : List.of();
            out.add(new Entry(id, name, alias, tags == null ? "" : tags.getOrDefault(id, "")));
        }
        return out;
    }

    /** A real name has a letter and is not a language key ("tensura.race.human", "item.tensura.x"). */
    public static boolean looksTranslated(String s) {
        if (s == null || s.isBlank() || s.length() > 80) return false;
        return s.contains(" ") || !s.matches("[a-z0-9_.:/-]+");
    }

    /** The id as written to a command: the path alone when it is unique in the list, else the full id. Always lower case with underscores, so it is one word. */
    public static List<String> commandNames(List<Entry> entries) {
        Map<String, Integer> paths = new HashMap<>();
        for (Entry e : entries) paths.merge(pathOf(e.id()), 1, Integer::sum);
        List<String> out = new ArrayList<>(entries.size());
        for (Entry e : entries) out.add(paths.get(pathOf(e.id())) > 1 ? e.id() : pathOf(e.id()));
        return out;
    }

    private static String pathOf(String id) { return id.substring(id.indexOf(':') + 1); }

    // ---------------------------------------------------------------- matching
    private static String norm(String s) { return DbRules.norm(s); }

    private static String[] tokens(String normalised) { return normalised.isEmpty() ? new String[0] : normalised.split(" "); }

    /** The phrases (normalised) an entry answers to: its name, its path, its full id, its aliases. */
    private static List<String[]> phrases(Entry e) {
        List<String[]> p = new ArrayList<>(4);
        p.add(tokens(norm(e.name())));
        p.add(tokens(norm(pathOf(e.id()))));
        if (e.id().contains(":")) p.add(tokens(norm(e.id())));
        for (String a : e.aliases()) p.add(tokens(norm(a)));
        return p;
    }

    private static int indexOf(String[] text, String[] phrase) {
        if (phrase.length == 0 || phrase.length > text.length) return -1;
        outer:
        for (int i = 0; i + phrase.length <= text.length; i++) {
            for (int j = 0; j < phrase.length; j++) if (!text[i + j].equals(phrase[j])) continue outer;
            return i;
        }
        return -1;
    }

    private static String compact(String[] t) { return String.join("", t); }

    private static String[] stripped(String[] t) {
        List<String> k = new ArrayList<>();
        for (String w : t) if (!FILLER.contains(w) && !w.chars().allMatch(Character::isDigit)) k.add(w);
        return k.toArray(new String[0]);
    }

    /** True when the sentence holds nothing but filler words and numbers: no name was given at all. */
    public static boolean noName(String text) { return stripped(tokens(norm(text))).length == 0; }

    /** What is left of the sentence when the filler words go: the part that should hold a name. */
    public static String nameOf(String text) { return String.join(" ", stripped(tokens(norm(text)))); }

    /** One entry for the sentence, fuzzily. Several equally good entries: AMBIGUOUS with up to {@value #MAX_CANDIDATES} of them (the wish is not used). Nothing: NONE. */
    public static Result resolve(List<Entry> entries, String text) {
        String[] t = tokens(norm(text));
        if (t.length == 0 || entries == null || entries.isEmpty()) return new Result(Status.NONE, List.of());
        String[] s = stripped(t);
        // A: the whole name typed (with or without filler, with or without spaces)
        List<Entry> exact = new ArrayList<>();
        String cs = compact(s), ct = compact(t);
        for (Entry e : entries) for (String[] p : phrases(e)) {
            String cp = compact(p);
            if (!cp.isEmpty() && (cp.equals(cs) || cp.equals(ct))) { exact.add(e); break; }
        }
        if (exact.size() == 1) return new Result(Status.OK, exact);
        if (exact.size() > 1) return new Result(Status.AMBIGUOUS, cap(exact));
        // B: a name inside the sentence, the longest one wins
        int best = 0;
        List<Entry> hits = new ArrayList<>();
        for (Entry e : entries) {
            int len = 0;
            for (String[] p : phrases(e)) if (compact(p).length() >= 3 && indexOf(t, p) >= 0) len = Math.max(len, compact(p).length());
            if (len == 0) continue;
            if (len > best) { best = len; hits.clear(); }
            if (len == best) hits.add(e);
        }
        if (hits.size() == 1) return new Result(Status.OK, hits);
        if (hits.size() > 1) return new Result(Status.AMBIGUOUS, cap(hits));
        // C: partial words: every word typed starts a word of the name; the closest name wins
        if (s.length == 0) return new Result(Status.NONE, List.of());
        int bestExtra = Integer.MAX_VALUE;
        List<Entry> near = new ArrayList<>();
        for (Entry e : entries) {
            int extra = Integer.MAX_VALUE;
            for (String[] p : phrases(e)) if (startsEveryWord(s, p)) extra = Math.min(extra, p.length - s.length);
            if (extra == Integer.MAX_VALUE) continue;
            if (extra < bestExtra) { bestExtra = extra; near.clear(); }
            if (extra == bestExtra) near.add(e);
        }
        if (near.size() == 1) return new Result(Status.OK, near);
        if (near.size() > 1) return new Result(Status.AMBIGUOUS, cap(near));
        return new Result(Status.NONE, List.of());
    }

    private static boolean startsEveryWord(String[] typed, String[] name) {
        boolean[] used = new boolean[name.length];
        for (String w : typed) {
            if (w.length() < 2) return false;
            boolean ok = false;
            for (int i = 0; i < name.length && !ok; i++) if (!used[i] && name[i].startsWith(w)) { used[i] = true; ok = true; }
            if (!ok) return false;
        }
        return true;
    }

    private static List<Entry> cap(List<Entry> l) { return l.size() <= MAX_CANDIDATES ? List.copyOf(l) : List.copyOf(l.subList(0, MAX_CANDIDATES)); }

    /**
     * Up to {@code max} DIFFERENT entries named in the sentence ("eye of truth and thought acceleration"): the longest full name found is taken, taken out of the sentence, and the search goes on. When no full name
     * is found at all the loose {@link #resolve} answers (OK with one entry, or AMBIGUOUS / NONE).
     */
    public static Result resolveAll(List<Entry> entries, String text, int max) {
        String[] t = tokens(norm(text));
        List<Entry> out = new ArrayList<>();
        for (int round = 0; round < Math.max(1, max) && t.length > 0 && entries != null; round++) {
            int best = 0;
            Entry pick = null;
            int at = -1, len = 0;
            boolean tie = false;
            for (Entry e : entries) {
                if (out.contains(e)) continue;
                for (String[] p : phrases(e)) {
                    int c = compact(p).length();
                    if (c < 3) continue;
                    int i = indexOf(t, p);
                    if (i < 0) continue;
                    if (c > best) { best = c; pick = e; at = i; len = p.length; tie = false; }
                    else if (c == best && pick != e) tie = true;
                }
            }
            if (pick == null || tie) break;
            out.add(pick);
            List<String> rest = new ArrayList<>(List.of(t));
            for (int k = 0; k < len; k++) rest.remove(at);
            t = rest.toArray(new String[0]);
        }
        if (!out.isEmpty()) return new Result(Status.OK, out);
        return resolve(entries, text);
    }

    // ---------------------------------------------------------------- spoken vocabulary
    /** The single words (normalised, 2+ letters) of a text, in order. */
    public static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        for (String w : tokens(norm(text))) if (w.length() >= 2 && !w.chars().allMatch(Character::isDigit)) out.add(w);
        return out;
    }

    /** Adds the words of every entry (name, path, aliases) to {@code into}, in order, until it holds {@code limit} words. */
    public static void addWords(List<Entry> entries, java.util.Collection<String> into, int limit) {
        for (Entry e : entries) {
            if (into.size() >= limit) return;
            for (String w : words(e.name())) if (into.size() < limit) into.add(w);
            for (String a : e.aliases()) for (String w : words(a)) if (into.size() < limit) into.add(w);
        }
    }
}
