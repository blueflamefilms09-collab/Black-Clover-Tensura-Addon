package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a data file the owner edits by hand WILL contain a typo sooner or later ("hrad" for "hard", a faction nobody defined, a count of
 * "2--4"), and in a game that cannot be run here the worst outcome is silence: the entry is dropped and nobody knows. So this parser is strict
 * about what it understands and loud about what it does not: every problem becomes one line of text (naming the file and the entry) that the
 * loader writes to the log and the shipped-file test turns into a failing build. A problem that makes an entry meaningless (no id, a bad band,
 * no monsters) drops the entry; a problem with a detail (a weight outside its range, an unknown key) keeps the entry with the value clamped and
 * still reports it. One bad entry never costs the rest of the file. Uses Gson (the repo's JSON library) but no Minecraft type.
 */
public final class EncounterTableParser {
    private EncounterTableParser() {}

    /** The result: the table (null when the file is not even a JSON object) and every problem found, entries dropped or not. */
    public record Parsed(EncounterTable table, List<String> problems) {
        public boolean clean() { return table != null && problems.isEmpty(); }
    }

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,48}");
    private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SOUND_OR_PARTICLE = Pattern.compile("([a-z0-9_.-]+:)?[a-z0-9_./-]+");
    private static final Set<String> TABLE_KEYS = Set.of("id", "description", "comment", "rollBonus", "budgetScale", "defaultFaction", "entries");
    private static final Set<String> ENTRY_KEYS = Set.of("id", "weight", "band", "minThreat", "maxThreat", "faction", "mobs", "needs", "scaleWithParty",
            "minParty", "message", "range", "cells", "hunt", "cue", "comment");
    private static final Set<String> MOB_KEYS = Set.of("type", "id", "faction", "count");
    private static final Set<String> NEEDS_KEYS = Set.of("dark", "lit", "noisy");
    private static final Set<String> CUE_KEYS = Set.of("sound", "volume", "pitch", "particle", "count");
    /** A table of this size or bigger is not an encounter table. */
    public static final int MAX_ENTRIES = 64, MAX_SPECS = 6;

    public static Parsed parse(String name, String json) {
        List<String> problems = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement e = JsonParser.parseString(json == null ? "" : json);
            if (!e.isJsonObject()) {
                problems.add(name + ": the file is not a JSON object");
                return new Parsed(null, problems);
            }
            root = e.getAsJsonObject();
        } catch (RuntimeException bad) {
            problems.add(name + ": not valid JSON (" + String.valueOf(bad.getMessage()).replace('\n', ' ') + ")");
            return new Parsed(null, problems);
        }
        unknownKeys(name, root, TABLE_KEYS, problems);
        String description = string(name, root, "description", "", problems);
        int rollBonus = integer(name, root, "rollBonus", 0, -3, 3, problems);
        double budgetScale = number(name, root, "budgetScale", 1.0, 0.25, 4.0, problems);
        String defaultFaction = string(name, root, "defaultFaction", "", problems);
        if (!defaultFaction.isEmpty() && !EncounterTable.FACTIONS.contains(defaultFaction)) {
            problems.add(name + ": defaultFaction '" + defaultFaction + "' is not a faction of the contract");
            defaultFaction = "";
        }
        List<EncounterTable.Entry> entries = new ArrayList<>();
        JsonElement list = root.get("entries");
        if (list == null || !list.isJsonArray()) {
            problems.add(name + ": 'entries' must be an array");
        } else {
            JsonArray arr = list.getAsJsonArray();
            if (arr.size() > MAX_ENTRIES) problems.add(name + ": more than " + MAX_ENTRIES + " entries, the rest are ignored");
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < arr.size() && i < MAX_ENTRIES; i++) {
                if (!arr.get(i).isJsonObject()) {
                    problems.add(name + ": entry #" + i + " is not an object");
                    continue;
                }
                EncounterTable.Entry entry = entry(name, i, arr.get(i).getAsJsonObject(), defaultFaction, problems);
                if (entry == null) continue;
                if (!seen.add(entry.id())) {
                    problems.add(name + ": entry id '" + entry.id() + "' is used twice, the second one is dropped");
                    continue;
                }
                entries.add(entry);
            }
        }
        return new Parsed(new EncounterTable(name, description, rollBonus, budgetScale, defaultFaction.isEmpty() ? null : defaultFaction, entries), problems);
    }

    private static EncounterTable.Entry entry(String file, int index, JsonObject o, String tableFaction, List<String> problems) {
        String id = string(file, o, "id", "", problems);
        String where = file + " entry " + (id.isEmpty() ? "#" + index : "'" + id + "'");
        if (!ID.matcher(id).matches()) {
            problems.add(where + ": 'id' must be 1-48 characters of a-z, 0-9 and _");
            return null;
        }
        unknownKeys(where, o, ENTRY_KEYS, problems);
        Band band = Band.byName(string(where, o, "band", "", problems));
        if (band == null || band == Band.NOTHING) {
            problems.add(where + ": 'band' must be minor, standard, hard or ambush");
            return null;
        }
        int weight = integer(where, o, "weight", 10, 1, 1000, problems);
        int minThreat = integer(where, o, "minThreat", 1, 1, 100000, problems);
        int maxThreat = integer(where, o, "maxThreat", 100000, 1, 100000, problems);
        if (maxThreat < minThreat) {
            problems.add(where + ": maxThreat " + maxThreat + " is below minThreat " + minThreat + ", the window is ignored");
            maxThreat = 100000;
        }
        String faction = string(where, o, "faction", "", problems);
        if (!faction.isEmpty() && !EncounterTable.FACTIONS.contains(faction)) {
            problems.add(where + ": faction '" + faction + "' is not a faction of the contract");
            faction = "";
        }
        List<EncounterTable.MobSpec> mobs = new ArrayList<>();
        JsonElement ml = o.get("mobs");
        if (ml == null || !ml.isJsonArray() || ml.getAsJsonArray().isEmpty()) {
            problems.add(where + ": 'mobs' must be a non-empty array");
            return null;
        }
        JsonArray marr = ml.getAsJsonArray();
        if (marr.size() > MAX_SPECS) problems.add(where + ": more than " + MAX_SPECS + " kinds of monster, the rest are ignored");
        for (int i = 0; i < marr.size() && i < MAX_SPECS; i++) {
            if (!marr.get(i).isJsonObject()) {
                problems.add(where + ": mob #" + i + " is not an object");
                return null;
            }
            EncounterTable.MobSpec spec = mob(where + " mob #" + i, marr.get(i).getAsJsonObject(), problems);
            if (spec == null) return null;
            // a faction monster with no faction anywhere could never be drawn (the room's native faction is only a preference), so it is a data error, not a silent skip
            if (spec.kind() == EncounterTable.Kind.FACTION && spec.faction().isEmpty() && faction.isEmpty() && tableFaction.isEmpty()) {
                problems.add(where + " mob #" + i + ": a faction monster needs a faction: set 'faction' on the monster or on the entry, or 'defaultFaction' on the table");
                return null;
            }
            mobs.add(spec);
        }
        EncounterTable.Needs needs = EncounterTable.Needs.NONE;
        JsonElement ne = o.get("needs");
        if (ne != null) {
            if (!ne.isJsonObject()) problems.add(where + ": 'needs' must be an object");
            else needs = needs(where + " needs", ne.getAsJsonObject(), problems);
        }
        boolean scale = bool(where, o, "scaleWithParty", true, problems);
        int minParty = integer(where, o, "minParty", 1, 1, 64, problems);
        String message = string(where, o, "message", "", problems);
        double minRange = 0, maxRange = 0;
        JsonElement re = o.get("range");
        if (re != null) {
            if (!re.isJsonArray() || re.getAsJsonArray().size() != 2 || !isNumber(re.getAsJsonArray().get(0)) || !isNumber(re.getAsJsonArray().get(1))) {
                problems.add(where + ": 'range' must be [min, max] in blocks");
            } else {
                minRange = re.getAsJsonArray().get(0).getAsDouble();
                maxRange = re.getAsJsonArray().get(1).getAsDouble();
                if (!(minRange >= 8.0) || !(maxRange <= 96.0) || maxRange < minRange + 4.0) {
                    problems.add(where + ": 'range' must lie inside 8..96 blocks and be at least 4 wide, the default is used");
                    minRange = 0;
                    maxRange = 0;
                }
            }
        }
        int cells = integer(where, o, "cells", 2, 1, 4, problems);
        int hunt = integer(where, o, "hunt", -1, -1, 12000, problems);
        EncounterTable.Cue cue = EncounterTable.Cue.NONE;
        JsonElement ce = o.get("cue");
        if (ce != null) {
            if (!ce.isJsonObject()) problems.add(where + ": 'cue' must be an object");
            else cue = cue(where + " cue", ce.getAsJsonObject(), problems);
        }
        return new EncounterTable.Entry(id, weight, band, minThreat, maxThreat, faction, mobs, needs, scale, minParty, message, minRange, maxRange, cells, hunt, cue);
    }

    private static EncounterTable.MobSpec mob(String where, JsonObject o, List<String> problems) {
        unknownKeys(where, o, MOB_KEYS, problems);
        String type = string(where, o, "type", "", problems);
        CountRange count = CountRange.ONE;
        if (o.has("count")) {
            JsonElement c = o.get("count");
            count = c.isJsonPrimitive() ? CountRange.parse(c.getAsString()) : null;
            if (count == null) {
                problems.add(where + ": 'count' must be a number or a range such as \"2-4\" (0 to " + CountRange.LIMIT + ")");
                return null;
            }
        }
        if (type.equals("entity")) {
            String id = string(where, o, "id", "", problems);
            if (!ENTITY_ID.matcher(id).matches()) {
                problems.add(where + ": 'id' must be an entity id such as minecraft:zombie");
                return null;
            }
            if (o.has("faction")) problems.add(where + ": an entity mob has no 'faction'");
            return new EncounterTable.MobSpec(EncounterTable.Kind.ENTITY, id, "", count);
        }
        if (type.equals("faction")) {
            String faction = string(where, o, "faction", "", problems);
            if (!faction.isEmpty() && !EncounterTable.FACTIONS.contains(faction)) {
                problems.add(where + ": faction '" + faction + "' is not a faction of the contract");
                return null;
            }
            if (o.has("id")) problems.add(where + ": a faction mob has no 'id'");
            return new EncounterTable.MobSpec(EncounterTable.Kind.FACTION, "", faction, count);
        }
        problems.add(where + ": 'type' must be \"entity\" or \"faction\"");
        return null;
    }

    private static EncounterTable.Needs needs(String where, JsonObject o, List<String> problems) {
        unknownKeys(where, o, NEEDS_KEYS, problems);
        return new EncounterTable.Needs(tri(where, o, "dark", problems), tri(where, o, "lit", problems), tri(where, o, "noisy", problems));
    }

    private static EncounterTable.Cue cue(String where, JsonObject o, List<String> problems) {
        unknownKeys(where, o, CUE_KEYS, problems);
        String sound = string(where, o, "sound", "", problems), particle = string(where, o, "particle", "", problems);
        if (!sound.isEmpty() && !SOUND_OR_PARTICLE.matcher(sound).matches()) {
            problems.add(where + ": 'sound' is not a sound id");
            sound = "";
        }
        if (!particle.isEmpty() && !SOUND_OR_PARTICLE.matcher(particle).matches()) {
            problems.add(where + ": 'particle' is not a particle id");
            particle = "";
        }
        float volume = (float) number(where, o, "volume", 0.7, 0.0, 4.0, problems), pitch = (float) number(where, o, "pitch", 0.8, 0.5, 2.0, problems);
        int count = integer(where, o, "count", 12, 0, 64, problems);
        return new EncounterTable.Cue(sound, volume, pitch, particle, count);
    }

    // ---------------------------------------------------------------- typed readers (a wrong type is a problem, never an exception)
    private static void unknownKeys(String where, JsonObject o, Set<String> known, List<String> problems) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) if (!known.contains(e.getKey())) problems.add(where + ": unknown key '" + e.getKey() + "'");
    }

    private static boolean isNumber(JsonElement e) { return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber(); }

    private static String string(String where, JsonObject o, String key, String def, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            problems.add(where + ": '" + key + "' must be a string");
            return def;
        }
        return e.getAsString().trim();
    }

    private static boolean bool(String where, JsonObject o, String key, boolean def, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
            problems.add(where + ": '" + key + "' must be true or false");
            return def;
        }
        return e.getAsBoolean();
    }

    private static Boolean tri(String where, JsonObject o, String key, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return null;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
            problems.add(where + ": '" + key + "' must be true or false");
            return null;
        }
        return e.getAsBoolean();
    }

    private static int integer(String where, JsonObject o, String key, int def, int lo, int hi, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (!isNumber(e) || e.getAsDouble() != Math.rint(e.getAsDouble())) {
            problems.add(where + ": '" + key + "' must be a whole number");
            return def;
        }
        double v = e.getAsDouble();
        if (v < lo || v > hi) {
            problems.add(where + ": '" + key + "' " + e.getAsString() + " is outside " + lo + ".." + hi + ", clamped");
            return (int) Math.max(lo, Math.min(hi, v));
        }
        return (int) v;
    }

    private static double number(String where, JsonObject o, String key, double def, double lo, double hi, List<String> problems) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (!isNumber(e)) {
            problems.add(where + ": '" + key + "' must be a number");
            return def;
        }
        double v = e.getAsDouble();
        if (Double.isNaN(v) || v < lo || v > hi) {
            problems.add(where + ": '" + key + "' " + e.getAsString() + " is outside " + lo + ".." + hi + ", clamped");
            return Double.isNaN(v) ? def : Math.max(lo, Math.min(hi, v));
        }
        return v;
    }
}
