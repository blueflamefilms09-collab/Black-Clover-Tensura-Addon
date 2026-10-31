package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: reads one will file ({@code data/nusmp/beast_will/<id>.json}) into a {@link BeastWillDef} and checks it against {@link BeastWillRules} (3.45.0). The game's loader and the unit tests call the same
 * function, so a file that passes the test is a file the game accepts. A file with any problem is rejected as a whole (a half-read will would be a will with the wrong cost), and the problem is reported in
 * words. Pure: Gson only.
 */
public final class BeastWillSchema {
    private BeastWillSchema() {}

    /** {@code def} is null when the file was rejected; {@code problems} lists every reason. */
    public record Parsed(BeastWillDef def, List<String> problems) {
        public boolean ok() { return def != null && problems.isEmpty(); }
    }

    public static Parsed parse(String fileId, String text) {
        List<String> bad = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) return new Parsed(null, List.of(fileId + ": the file is not a JSON object"));
            JsonObject o = root.getAsJsonObject();
            String id = str(o, "id", fileId, bad);
            if (!id.equals(fileId)) bad.add(fileId + ": the id \"" + id + "\" must equal the file name");
            BeastRank rank = BeastRank.byName(str(o, "minRank", "A", bad));
            if (rank == null) bad.add(fileId + ": unknown minRank");
            BeastWillDef def = new BeastWillDef(id, str(o, "name", id, bad), str(o, "beast", "", bad), str(o, "affinity", "", bad), bool(o, "legendary", false),
                    rank, strings(o, "sources", bad), strings(o, "keywords", bad), num(o, "dropChance", 0.01, bad), num(o, "requireEp", 5000, bad), strings(o, "hostRaces", bad),
                    num(o, "otherHostCompat", 0.6, bad), num(o, "assimilationSeconds", 7200, bad), num(o, "masterySeconds", 3600, bad), num(o, "assimilationDayCap", 0.25, bad),
                    num(o, "masteryDayCap", 0.25, bad), (int) num(o, "coreCooldownDays", 3, bad), (int) num(o, "tint", 0xFFFFFF, bad), stats(o, "passive", bad),
                    phase(obj(o, "acquire", bad), bad), phase(obj(o, "integrate", bad), bad));
            if (bad.isEmpty()) {
                String p = def.problem();
                if (p != null) bad.add(p);
            }
            return new Parsed(bad.isEmpty() ? def : null, bad);
        } catch (RuntimeException e) {
            bad.add(fileId + ": " + e);
            return new Parsed(null, bad);
        }
    }

    private static BeastWillDef.Phase phase(JsonObject o, List<String> bad) {
        if (o == null) return null;
        List<BeastWillDef.Ability> abilities = new ArrayList<>();
        JsonArray arr = array(o, "abilities", bad);
        if (arr != null) for (JsonElement e : arr) {
            if (!e.isJsonObject()) { bad.add("an ability is not an object"); continue; }
            JsonObject a = e.getAsJsonObject();
            abilities.add(new BeastWillDef.Ability(str(a, "id", "", bad), str(a, "kind", "", bad), (int) num(a, "slot", 1, bad), num(a, "costPct", 0, bad), (int) num(a, "cooldownTicks", 200, bad),
                    num(a, "strain", 0, bad), num(a, "range", 0, bad), num(a, "radius", 0, bad), (int) num(a, "durationTicks", 0, bad), num(a, "power", 1, bad), (int) num(a, "maxTargets", 1, bad),
                    num(a, "bossFactor", 0.5, bad)));
        }
        return new BeastWillDef.Phase(num(o, "unlockAssimilation", 0.1, bad), num(o, "unlockMastery", 0, bad), num(o, "drainPct", 0.5, bad), num(o, "strainPerSecond", 0.2, bad), num(o, "regen", 0, bad), stats(o, "stats", bad), abilities);
    }

    private static List<BeastWillDef.Stat> stats(JsonObject o, String key, List<String> bad) {
        List<BeastWillDef.Stat> out = new ArrayList<>();
        JsonArray arr = array(o, key, bad);
        if (arr != null) for (JsonElement e : arr) {
            if (!e.isJsonObject()) { bad.add(key + ": a stat is not an object"); continue; }
            JsonObject s = e.getAsJsonObject();
            String op = str(s, "op", "add", bad).toLowerCase(Locale.ROOT);
            if (!op.equals("add") && !op.equals("mult")) bad.add(key + ": op must be add or mult");
            out.add(new BeastWillDef.Stat(str(s, "attribute", "", bad), op.equals("mult") ? BeastWillDef.Op.MULT : BeastWillDef.Op.ADD, num(s, "amount", 0, bad)));
        }
        return out;
    }

    // ---------------------------------------------------------------- small readers that report instead of throwing
    private static String str(JsonObject o, String k, String def, List<String> bad) {
        JsonElement e = o.get(k);
        if (e == null || e.isJsonNull()) return def;
        if (!e.isJsonPrimitive()) { bad.add(k + " must be text"); return def; }
        return e.getAsString();
    }

    private static double num(JsonObject o, String k, double def, List<String> bad) {
        JsonElement e = o.get(k);
        if (e == null || e.isJsonNull()) return def;
        try { return e.getAsDouble(); } catch (RuntimeException ex) { bad.add(k + " must be a number"); return def; }
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? def : e.isJsonPrimitive() && e.getAsBoolean();
    }

    private static JsonObject obj(JsonObject o, String k, List<String> bad) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonObject()) { bad.add(k + " must be an object"); return null; }
        return e.getAsJsonObject();
    }

    private static JsonArray array(JsonObject o, String k, List<String> bad) {
        JsonElement e = o.get(k);
        if (e == null || e.isJsonNull()) return null;
        if (!e.isJsonArray()) { bad.add(k + " must be a list"); return null; }
        return e.getAsJsonArray();
    }

    private static List<String> strings(JsonObject o, String k, List<String> bad) {
        List<String> out = new ArrayList<>();
        JsonArray arr = array(o, k, bad);
        if (arr != null) for (JsonElement e : arr) {
            if (e.isJsonPrimitive()) out.add(e.getAsString().toLowerCase(Locale.ROOT).trim()); else bad.add(k + ": every entry must be text");
        }
        return out;
    }
}
