package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a faction file is hand-written (and a datapack may add or retune one), so reading it must never throw and never silently accept a typo.
 * This turns the already-parsed JSON tree (maps, lists, strings, numbers, booleans: the loader converts Gson's tree into it, which keeps this
 * class free of any library so JUnit can feed it the real files) into a {@link FactionDef}, clamps every number into a safe range, fills
 * defaults, and returns a list of plain-English problems instead of exceptions: an unknown key, a bad colour, a rival that lists itself, a
 * theme that does not exist. The loader logs each problem once at reload, so the author sees "{@code goblin: unknown key 'rival'}" instead
 * of a faction with no enemies and no explanation. Whether a rival id names a real faction needs the whole set, so that check lives in
 * {@link FactionGraph}. Pure (no Minecraft types).
 */
public final class FactionParser {
    private FactionParser() {}

    /** Why: the reader returns the faction AND the complaints together, so a caller can use what is usable and still report what is not: the parsed faction (null only when the file name is not a usable id or the file is not an object) and everything that looked wrong. */
    public record Result(FactionDef def, List<String> problems) {}

    public static final int DEFAULT_COLOR = 0xA0A0A0;
    public static final double DEFAULT_MEAT_CHANCE = 0.40, MAX_WEIGHT = 100.0;
    public static final String DEFAULT_CUT = "raw_monster_meat";

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Pattern LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> KNOWN_KEYS = Set.of("why", "id", "name", "color", "tag", "rivals", "allies", "aggro", "themeWeights", "drops", "territorial", "oneWay");

    /** Is this string usable as a faction id (it becomes a file name, a tag path, a lang key and a command argument)? */
    public static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /** Parses the file {@code <fileId>.json} whose content was read into {@code json}. */
    public static Result parse(String fileId, Map<String, Object> json) {
        List<String> problems = new ArrayList<>();
        String id = fileId == null ? "" : fileId.trim().toLowerCase(Locale.ROOT);
        if (!validId(id)) {
            problems.add("file name '" + fileId + "' is not a faction id (a lower-case letter, then lower-case letters, digits or underscores)");
            return new Result(null, problems);
        }
        if (json == null) {
            problems.add(id + ": the file is not a JSON object");
            return new Result(null, problems);
        }
        for (String key : json.keySet()) if (!KNOWN_KEYS.contains(key)) problems.add(id + ": unknown key '" + key + "' (ignored)");
        if (json.get("id") instanceof String declared && !declared.trim().equalsIgnoreCase(id))
            problems.add(id + ": the file declares id '" + declared + "' but the file name wins");

        String name = text(json, "name", "", id, problems);
        if (name.isBlank()) name = Character.toUpperCase(id.charAt(0)) + id.substring(1);

        int color = DEFAULT_COLOR;
        String colorText = text(json, "color", "", id, problems);
        if (!colorText.isEmpty()) {
            int c = parseColor(colorText);
            if (c < 0) problems.add(id + ": color '" + colorText + "' is not #RRGGBB; using grey");
            else color = c;
        }

        String tag = text(json, "tag", "", id, problems);
        if (tag.isEmpty()) tag = "nusmp:mega/" + id;
        else if (!LOCATION.matcher(tag).matches()) {
            problems.add(id + ": tag '" + tag + "' is not a resource location; using nusmp:mega/" + id);
            tag = "nusmp:mega/" + id;
        }

        List<String> rivals = idList(json.get("rivals"), "rivals", id, problems);
        List<String> allies = idList(json.get("allies"), "allies", id, problems);

        int range = FactionDef.UNSET, bonus = FactionDef.UNSET;
        Object aggro = json.get("aggro");
        if (aggro instanceof Map<?, ?> a) {
            range = whole(a.get("range"), FactionDef.MAX_RANGE, "aggro.range", id, problems);
            bonus = whole(a.get("nativeBonus"), FactionDef.MAX_BONUS, "aggro.nativeBonus", id, problems);
        } else if (aggro != null) problems.add(id + ": 'aggro' must be an object with range and nativeBonus");

        Map<String, Double> weights = new LinkedHashMap<>();
        Object tw = json.get("themeWeights");
        if (tw instanceof Map<?, ?> t) {
            for (Map.Entry<?, ?> e : t.entrySet()) {
                String theme = String.valueOf(e.getKey()).trim().toLowerCase(Locale.ROOT);
                if (!isTheme(theme)) {
                    problems.add(id + ": themeWeights names unknown theme '" + e.getKey() + "' (ignored)");
                    continue;
                }
                if (!(e.getValue() instanceof Number n)) {
                    problems.add(id + ": themeWeights." + theme + " must be a number (ignored)");
                    continue;
                }
                double w = n.doubleValue();
                if (Double.isNaN(w) || w < 0.0) w = 0.0;
                weights.put(theme, Math.min(MAX_WEIGHT, w));
            }
        } else if (tw != null) problems.add(id + ": 'themeWeights' must be an object of theme id to number");

        String meat = DEFAULT_CUT;
        double meatChance = DEFAULT_MEAT_CHANCE;
        Object drops = json.get("drops");
        if (drops instanceof Map<?, ?> d) {
            Object cut = d.get("meat");
            if (cut instanceof String s) {
                String c = s.trim().toLowerCase(Locale.ROOT);
                if (c.startsWith("nusmp:")) c = c.substring(6);
                if (FactionDef.CUTS.contains(c)) meat = c;
                else problems.add(id + ": drops.meat '" + s + "' is not one of " + FactionDef.CUTS + "; using " + DEFAULT_CUT);
            } else if (cut != null) problems.add(id + ": drops.meat must be a string");
            Object chance = d.get("meatChance");
            if (chance instanceof Number n) meatChance = n.doubleValue() < 0.0 || Double.isNaN(n.doubleValue()) ? 0.0 : Math.min(1.0, n.doubleValue());
            else if (chance != null) problems.add(id + ": drops.meatChance must be a number");
        } else if (drops != null) problems.add(id + ": 'drops' must be an object with meat and meatChance");

        boolean territorial = flag(json, "territorial", id, problems), oneWay = flag(json, "oneWay", id, problems);
        return new Result(new FactionDef(id, name, color, tag, rivals, allies, range, bonus, territorial, oneWay, weights, meat, meatChance), problems);
    }

    /** {@code #RRGGBB} or {@code RRGGBB} as an int, or -1 when it is neither. */
    static int parseColor(String text) {
        String h = text.startsWith("#") ? text.substring(1) : text;
        if (h.length() != 6) return -1;
        try {
            int v = Integer.parseInt(h, 16);
            return v < 0 ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isTheme(String id) {
        for (RegionTheme t : RegionTheme.values()) if (t.id().equals(id)) return true;
        return false;
    }

    private static String text(Map<String, Object> m, String key, String def, String id, List<String> problems) {
        Object o = m.get(key);
        if (o == null) return def;
        if (o instanceof String s) return s.trim();
        problems.add(id + ": '" + key + "' must be a string");
        return def;
    }

    private static boolean flag(Map<String, Object> m, String key, String id, List<String> problems) {
        Object o = m.get(key);
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        problems.add(id + ": '" + key + "' must be true or false");
        return false;
    }

    /** A whole number in [0, max], or {@link FactionDef#UNSET} when absent or not a number. */
    private static int whole(Object o, int max, String what, String id, List<String> problems) {
        if (o == null) return FactionDef.UNSET;
        if (!(o instanceof Number n) || Double.isNaN(n.doubleValue())) {
            problems.add(id + ": " + what + " must be a number; using the server config value");
            return FactionDef.UNSET;
        }
        long v = Math.round(n.doubleValue());
        if (v < 0 || v > max) problems.add(id + ": " + what + " " + v + " is outside 0.." + max + "; clamped");
        return (int) Math.max(0, Math.min(max, v));
    }

    private static List<String> idList(Object o, String key, String self, List<String> problems) {
        List<String> out = new ArrayList<>();
        if (o == null) return out;
        if (!(o instanceof List<?> list)) {
            problems.add(self + ": '" + key + "' must be a list of faction ids");
            return out;
        }
        for (Object x : list) {
            if (!(x instanceof String s) || s.isBlank()) {
                problems.add(self + ": '" + key + "' holds something that is not a faction id (ignored)");
                continue;
            }
            String v = s.trim().toLowerCase(Locale.ROOT);
            if (v.equals(self)) problems.add(self + ": '" + key + "' lists the faction itself (ignored)");
            else if (!out.contains(v)) out.add(v);
        }
        return out;
    }
}
