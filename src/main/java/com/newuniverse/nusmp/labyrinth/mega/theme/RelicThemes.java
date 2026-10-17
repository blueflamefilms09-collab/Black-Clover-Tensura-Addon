package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.crucible.RelicKeywords;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: in the Mega-Dungeon the relic a party chooses in a Crucible decides what the NEXT region looks and plays like ("the chosen relic dictates the
 * palette and ruleset of the next ten floors"), so a relic has to be an IDENTITY with a place in the world, not a random number: taking Frostfang
 * leads to the Frost road, the Bloodbound Katana to the Blood road, and the choice among three pedestals is a choice of where to go next. A mapping
 * that important must be explainable to a player and tunable by the owner without a mod update, so it is a pure function of three things the owner
 * can see: (a) an EXPLICIT table, {@code data/nusmp/mega/relic_themes.json}, that says "this relic id belongs to that theme" and always wins (the
 * famous relics are placed by hand, by their lore); (b) otherwise the KEYWORDS the themes already carry for this purpose ({@code relics.keywords} in
 * each theme file): the theme with the most distinct keywords found at the start of a word of the relic's id or card name wins, a tie going to the
 * theme that comes first in {@link RegionTheme}; (c) otherwise a stable HASH of the relic id over the themes, so a relic nobody has described yet (one
 * a later wave adds) still leads somewhere, always to the same place, and the world never depends on a dice roll. Pure (no Minecraft types, no JSON
 * library): the rules are pinned by {@code RelicThemesTest}, the shipped file by its data test, and the game glue ({@code RelicThemeMap},
 * {@code NextTheme}) only gathers the inputs.
 * <p>
 * {@link RegionTheme#GRAVITY} is the baseline the first region is drawn in, so it is reachable only by an explicit entry (a relic of weights, hammers
 * and anchors, listed by hand): neither the keywords nor the hash ever return it, which is also what stops a generic word such as "stone" from sending
 * every Infinity Stone to the heavy road. Adding a theme to the enum re-deals only the relics that have neither an explicit entry nor a keyword hit.
 * <p>
 * 1.55.0: a relic leads to a {@link Road}, a theme AND a ruleset id ("the chosen relic dictates the palette and ruleset of the next ten floors"). The
 * ruleset is the name other systems may key their own data on when a theme alone is too coarse (an encounter table, a faction mix, a trap set for one
 * relic's road). It defaults to the theme's own id, so a system that knows only themes keeps working unchanged, and a relic may name a variant in the
 * {@code rulesets} table of {@code relic_themes.json}. Nothing about the ruleset is stored: the relic that was taken is saved in the Crucible of the
 * region it was taken in, so the ruleset of any region is a pure function of saved data ({@code NextTheme.rulesetOf}).
 */
public final class RelicThemes {
    private RelicThemes() {}

    /**
     * The result of {@link #parse}: relic key to theme id (both normalised, every theme id a real theme), relic key to ruleset id (1.55.0, optional), ruleset
     * id to display name (1.55.0, optional) and the warnings found on the way.
     */
    public record Parsed(Map<String, String> explicit, Map<String, String> rulesets, Map<String, String> names, List<String> warnings) {
        /** The pre-1.55.0 shape (no rulesets): kept so every older caller and test still reads. */
        public Parsed(Map<String, String> explicit, List<String> warnings) { this(explicit, Map.of(), Map.of(), warnings); }
    }

    /** What a relic leads to: the theme the next region wears and the id of the ruleset its ten floors play by (the theme's id unless the relic names a variant). */
    public record Road(RegionTheme theme, String ruleset) {}

    private static final java.util.regex.Pattern RULESET_ID = java.util.regex.Pattern.compile("[a-z0-9_]{1,40}");

    /** Is this text usable as a ruleset id (lower case letters, digits and underscores, at most 40)? */
    public static boolean validRuleset(String id) { return id != null && RULESET_ID.matcher(id).matches(); }

    /** A relic id as the tables key it: trimmed, lower case, without a namespace ({@code nusmp:Frostfang} and {@code frostfang} are the same relic). Never null. */
    public static String key(String relicId) {
        if (relicId == null) return "";
        String s = relicId.trim().toLowerCase(Locale.ROOT);
        int colon = s.lastIndexOf(':');
        return colon >= 0 ? s.substring(colon + 1) : s;
    }

    /** The theme with this id (any case), or null: unlike {@code RegionTheme.fromId} an unknown id is not read as the baseline. */
    public static RegionTheme themeById(String id) {
        if (id == null) return null;
        String s = id.trim();
        for (RegionTheme t : RegionTheme.values()) if (t.id().equalsIgnoreCase(s)) return t;
        return null;
    }

    /**
     * The theme the region after this relic's Crucible gets. Never throws and never returns null.
     * @param relicId the relic's registry path ({@code frostfang}); null or empty is allowed and gives a stable hash like any other id
     * @param relicDisplayName the card name ({@code Frostfang}); null or empty is allowed (then only the id is read)
     * @param explicit relic key to theme id, from {@code relic_themes.json}; null is allowed; an entry naming an unknown theme is ignored
     * @param keywords each theme's relic keywords; null is allowed; the entry of {@link RegionTheme#GRAVITY} is never read
     */
    public static RegionTheme themeOf(String relicId, String relicDisplayName, Map<String, String> explicit, Map<RegionTheme, List<String>> keywords) {
        String key = key(relicId);
        RegionTheme byTable = explicitTheme(key, explicit);
        if (byTable != null) return byTable;
        RegionTheme byWords = byKeywords(relicId, relicDisplayName, keywords);
        if (byWords != null) return byWords;
        return byHash(key);
    }

    /**
     * 1.55.0: the road a relic leads to: its theme ({@link #themeOf}) and its ruleset id, which is the relic's entry in {@code rulesets} when that is a valid
     * id and the theme's own id otherwise. Never throws and never returns null.
     * @param rulesets relic key to ruleset id, from {@code relic_themes.json}; null is allowed; an entry that is not a valid id is ignored
     */
    public static Road roadOf(String relicId, String relicDisplayName, Map<String, String> explicit, Map<String, String> rulesets, Map<RegionTheme, List<String>> keywords) {
        RegionTheme theme = themeOf(relicId, relicDisplayName, explicit, keywords);
        String named = rulesetFor(key(relicId), rulesets);
        return new Road(theme, named != null ? named : theme.id());
    }

    /** The ruleset id the table names for this relic key, or null (no entry, or an entry that is not a valid id). */
    static String rulesetFor(String key, Map<String, String> rulesets) {
        if (rulesets == null || rulesets.isEmpty() || key.isEmpty()) return null;
        String direct = normalRuleset(rulesets.get(key));
        if (direct != null) return direct;
        for (Map.Entry<String, String> e : rulesets.entrySet()) {                    // a caller's map may be keyed "nusmp:Frostfang"
            if (e.getKey() == null || !key.equals(key(e.getKey()))) continue;
            String id = normalRuleset(e.getValue());
            if (id != null) return id;
        }
        return null;
    }

    private static String normalRuleset(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        return validRuleset(s) ? s : null;
    }

    /** Rule (a): the theme the table gives this relic key, or null (no entry, or an entry that does not name a theme). */
    static RegionTheme explicitTheme(String key, Map<String, String> explicit) {
        if (explicit == null || explicit.isEmpty() || key.isEmpty()) return null;
        RegionTheme direct = themeById(explicit.get(key));
        if (direct != null) return direct;
        for (Map.Entry<String, String> e : explicit.entrySet()) {                    // a caller's map may be keyed "nusmp:Frostfang"
            if (e.getKey() == null || !key.equals(key(e.getKey()))) continue;
            RegionTheme t = themeById(e.getValue());
            if (t != null) return t;
        }
        return null;
    }

    /**
     * Rule (b): the non-baseline theme with the most distinct keywords that begin a word of the relic's id or card name; null when none has a hit. A tie
     * goes to the theme that comes first in the enum, so the answer never depends on a map's iteration order.
     */
    static RegionTheme byKeywords(String relicId, String relicDisplayName, Map<RegionTheme, List<String>> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        List<String> words = RelicKeywords.words(relicId);
        words.addAll(RelicKeywords.words(relicDisplayName));
        if (words.isEmpty()) return null;
        RegionTheme best = null;
        int bestHits = 0;
        for (RegionTheme t : RegionTheme.values()) {
            if (t == RegionTheme.GRAVITY) continue;
            int h = hits(words, keywords.get(t));
            if (h > bestHits) {
                best = t;
                bestHits = h;
            }
        }
        return best;
    }

    /** How many DISTINCT keywords begin one of the words (a keyword listed twice, or hitting two words, counts once). */
    static int hits(List<String> words, List<String> keywords) {
        if (keywords == null || keywords.isEmpty() || words.isEmpty()) return 0;
        Set<String> seen = new HashSet<>();
        int n = 0;
        for (String k : keywords) {
            String key = k == null ? "" : k.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty() || !seen.add(key)) continue;
            for (String w : words) {
                if (w.startsWith(key)) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    /** Rule (c): a stable theme for any key, over every theme but the baseline. */
    static RegionTheme byHash(String key) {
        List<RegionTheme> pool = new ArrayList<>();
        for (RegionTheme t : RegionTheme.values()) if (t != RegionTheme.GRAVITY) pool.add(t);
        if (pool.isEmpty()) return RegionTheme.GRAVITY;
        return pool.get((int) Math.floorMod(hash(key), (long) pool.size()));
    }

    /**
     * FNV-1a over the key's UTF-16 units, then splitmix64's finaliser. Written out here instead of {@code String.hashCode} so the value is documented,
     * has no weak low bits for names that share a prefix, and is pinned by a test: changing it would move every undescribed relic to another theme.
     */
    static long hash(String key) {
        long h = 0xCBF29CE484222325L;
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0x100000001B3L;
        }
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    // ---------------------------------------------------------------- the file
    /**
     * Reads {@code relic_themes.json} from a tree of maps, lists, strings and numbers: {@code {"explicit": {"frostfang": "frost", ...}}}, and, since 1.55.0,
     * the optional {@code "rulesets": {"frostfang": "the_long_winter"}} (relic key to ruleset id) and {@code "ruleset_names": {"the_long_winter": "The Long
     * Winter"}} (ruleset id to the name a player reads). Never throws: an entry that is not a string, names no theme (or no valid ruleset id), or has an empty
     * key becomes a warning and is skipped, so one typo costs one relic its hand-made place, not the file. Keys are normalised with {@link #key}; a repeated
     * key keeps the last entry and warns.
     */
    public static Parsed parse(Object tree) { return parse(tree, themeIds()); }

    /** The same against an explicit list of theme ids instead of the enum (a test checks the shipped file against the ids the mod ships, whatever the enum on its class path holds). */
    static Parsed parse(Object tree, Collection<String> knownThemes) {
        List<String> warn = new ArrayList<>();
        Map<String, String> out = new LinkedHashMap<>();
        if (!(tree instanceof Map<?, ?> root)) {
            warn.add("relic_themes.json must be a JSON object");
            return new Parsed(out, warn);
        }
        Object table = root.get("explicit");
        if (!(table instanceof Map<?, ?> entries)) {
            warn.add(table == null ? "no 'explicit' object: every relic is placed by its keywords or its hash" : "explicit must be an object of relic id to theme id");
            return new Parsed(out, warn);
        }
        for (Map.Entry<?, ?> e : entries.entrySet()) {
            String key = key(String.valueOf(e.getKey()));
            if (key.isEmpty()) {
                warn.add("an entry has an empty relic id");
                continue;
            }
            if (!(e.getValue() instanceof String name)) {
                warn.add(key + ": the theme must be a theme id (a string)");
                continue;
            }
            String theme = name.trim().toLowerCase(Locale.ROOT);
            if (!knownThemes.contains(theme)) {
                warn.add(key + ": '" + name + "' is not a theme (" + String.join(", ", knownThemes) + ")");
                continue;
            }
            if (out.put(key, theme) != null) warn.add(key + ": listed twice, the last entry wins");
        }
        Map<String, String> rulesets = new LinkedHashMap<>();
        Object rs = root.get("rulesets");
        if (rs instanceof Map<?, ?> rsEntries) {
            for (Map.Entry<?, ?> e : rsEntries.entrySet()) {
                String key = key(String.valueOf(e.getKey()));
                if (key.isEmpty()) {
                    warn.add("rulesets: an entry has an empty relic id");
                    continue;
                }
                String id = e.getValue() instanceof String text ? normalRuleset(text) : null;
                if (id == null) {
                    warn.add("rulesets." + key + ": '" + e.getValue() + "' is not a ruleset id (lower case letters, digits and underscores, at most 40)");
                    continue;
                }
                if (rulesets.put(key, id) != null) warn.add("rulesets." + key + ": listed twice, the last entry wins");
            }
        } else if (rs != null) {
            warn.add("rulesets must be an object of relic id to ruleset id");
        }
        Map<String, String> names = new LinkedHashMap<>();
        Object nm = root.get("ruleset_names");
        if (nm instanceof Map<?, ?> nmEntries) {
            for (Map.Entry<?, ?> e : nmEntries.entrySet()) {
                String id = normalRuleset(String.valueOf(e.getKey()));
                String text = e.getValue() instanceof String t ? t.trim() : "";
                if (id == null || text.isEmpty() || text.length() > 60) {
                    warn.add("ruleset_names." + e.getKey() + ": needs a valid ruleset id and a name of 1 to 60 characters");
                    continue;
                }
                names.put(id, text);
            }
        } else if (nm != null) {
            warn.add("ruleset_names must be an object of ruleset id to name");
        }
        return new Parsed(out, rulesets, names, warn);
    }

    private static List<String> themeIds() {
        List<String> ids = new ArrayList<>();
        for (RegionTheme t : RegionTheme.values()) ids.add(t.id());
        return ids;
    }
}
