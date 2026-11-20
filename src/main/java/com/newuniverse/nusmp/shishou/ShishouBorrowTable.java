package com.newuniverse.nusmp.shishou;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Why: Shishou can USE the skills and spells of other mods (base Tensura Reincarnated, TR Nightmares, Tensura Mysticism) and of this mod's own Black Clover grimoire magics, with the SWORD's mana. Which ones, at which
 * level, at which price and cooldown, and from which monsters Skill Eater may learn them, is ONE bounded data table ({@code data/anime_multiverse/shishou/borrowed_skills.json}) read by this pure class (no game
 * classes), so a server owner or a modpack author can change it and the tests can pin it. An entry is only a NAME: nothing here links another mod's classes, a skill that is not in the registry is simply not
 * there ({@link ShishouBorrow} looks it up by id at the moment of the cast).
 *
 * <p>An entry's {@code idx} is its bit in the saved mask of learned entries: stable, 0..63, never reused (a removed entry leaves a hole). At most {@link #MAX} entries.</p>
 */
public final class ShishouBorrowTable {
    private ShishouBorrowTable() {}

    public static final String RESOURCE = "/data/anime_multiverse/shishou/borrowed_skills.json";
    public static final int MAX = 64;
    public static final int MANA_MAX = 200, COOLDOWN_MIN = 20, COOLDOWN_MAX = 6000, HOLD_MAX = 40, LEVEL_MAX = ShishouLevels.MAX_LEVEL;
    /** The sources: the mod a skill belongs to (a switch each in the server config). */
    public static final String TENSURA = "tensura", NIGHTMARES = "nightmares", MYSTICISM = "mysticism", MULTIVERSE = "multiverse";
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern WORD = Pattern.compile("[a-z0-9_]{2,24}");

    public enum Kind {
        /** A registered skill cast by the body the way a player's key does (through the Tensura skill system). */
        SKILL,
        /** An echo of one of this mod's Black Clover grimoire magics ({@code echo:flame}): the magic's own particle and hit effect, fired by the sword. */
        ECHO
    }

    /** One entry. {@code level} is the sword level that opens it by leveling (0 = Skill Eater only); {@code steal} are words of an entity id that may teach it, {@code percent} the base chance. */
    public record Entry(int idx, String source, Kind kind, String id, String label, int level, int mana, int cooldown, int hold, List<String> steal, int percent) {
        public boolean stealable() { return !steal.isEmpty() && percent > 0; }
    }

    // ---------------------------------------------------------------- parse
    /** Parses the table; an entry with a bad field is skipped (never the whole table), the list is cut at {@link #MAX}, the order of the file is kept. */
    public static List<Entry> parse(String json) {
        List<Entry> out = new ArrayList<>();
        long used = 0L;
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException bad) {
            return out;
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("skills") || !root.getAsJsonObject().get("skills").isJsonArray()) return out;
        for (JsonElement e : root.getAsJsonObject().getAsJsonArray("skills")) {
            if (out.size() >= MAX || !e.isJsonObject()) continue;
            Entry en = entry(e.getAsJsonObject());
            if (en == null || (used >> en.idx() & 1L) != 0L) continue;
            used |= 1L << en.idx();
            out.add(en);
        }
        return out;
    }

    private static Entry entry(JsonObject o) {
        try {
            int idx = o.get("idx").getAsInt();
            String source = o.get("source").getAsString().toLowerCase(Locale.ROOT), id = o.get("id").getAsString().toLowerCase(Locale.ROOT), label = o.get("label").getAsString();
            Kind kind = Kind.valueOf(o.get("kind").getAsString().toUpperCase(Locale.ROOT));
            if (idx < 0 || idx >= MAX || !ID.matcher(id).matches() || label.isBlank() || label.length() > 40) return null;
            if (!(source.equals(TENSURA) || source.equals(NIGHTMARES) || source.equals(MYSTICISM) || source.equals(MULTIVERSE))) return null;
            if (kind == Kind.ECHO != id.startsWith("echo:")) return null;
            int level = clamp(o.get("level").getAsInt(), 0, LEVEL_MAX), mana = clamp(o.get("mana").getAsInt(), 1, MANA_MAX), cd = clamp(o.get("cooldown").getAsInt(), COOLDOWN_MIN, COOLDOWN_MAX);
            int hold = o.has("hold") ? clamp(o.get("hold").getAsInt(), 0, HOLD_MAX) : 6, pct = o.has("percent") ? clamp(o.get("percent").getAsInt(), 0, 100) : 0;
            List<String> words = new ArrayList<>();
            if (o.has("steal")) {
                JsonArray a = o.getAsJsonArray("steal");
                for (JsonElement w : a) {
                    String s = w.getAsString().toLowerCase(Locale.ROOT);
                    if (WORD.matcher(s).matches() && words.size() < 6) words.add(s);
                }
            }
            if (level == 0 && (words.isEmpty() || pct == 0)) return null;                  // an entry that can never be had is a mistake
            return new Entry(idx, source, kind, id, label, level, mana, cd, hold, List.copyOf(words), pct);
        } catch (RuntimeException bad) {
            return null;
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // ---------------------------------------------------------------- the shipped table
    private static volatile List<Entry> table;

    /** The shipped table (read once from the jar; an unreadable file is an empty table, never a crash). */
    public static List<Entry> entries() {
        List<Entry> t = table;
        if (t == null) {
            t = List.of();
            try (InputStream in = ShishouBorrowTable.class.getResourceAsStream(RESOURCE)) {
                if (in != null) t = List.copyOf(parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException | RuntimeException none) {
                t = List.of();
            }
            table = t;
        }
        return t;
    }

    public static Entry byIdx(int idx) {
        for (Entry e : entries()) if (e.idx() == idx) return e;
        return null;
    }

    // ---------------------------------------------------------------- availability and learning
    /** Is the entry open for a sword of this level with these learned bits? By leveling (level above 0) or because Skill Eater took it. */
    public static boolean available(Entry e, int swordLevel, long mask) {
        return e.level() > 0 && swordLevel >= e.level() || (mask >> e.idx() & 1L) != 0L;
    }

    /** The entries that are open, in table order. */
    public static List<Entry> availableList(List<Entry> all, int swordLevel, long mask) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) if (available(e, swordLevel, mask)) out.add(e);
        return out;
    }

    /** The first entry that Skill Eater may take from a creature of this type id (a part of its path matches a word, the entry is not open yet); null when none. {@code path} is the id without the namespace. */
    public static Entry stealMatch(List<Entry> all, String typeId, int swordLevel, long mask) {
        if (typeId == null) return null;
        String id = typeId.toLowerCase(Locale.ROOT);
        String path = id.substring(id.indexOf(':') + 1);
        for (Entry e : all) {
            if (!e.stealable() || available(e, swordLevel, mask)) continue;
            for (String w : e.steal()) if (path.contains(w)) return e;
        }
        return null;
    }

    /** Does a roll in [0, 1) take it? The percent is scaled by the server's steal percentage. */
    public static boolean steals(Entry e, int scalePercent, double roll) {
        if (e == null || scalePercent <= 0) return false;
        return roll >= 0.0 && roll < Math.min(1.0, e.percent() / 100.0 * scalePercent / 100.0);
    }

    /** The mask bit of an entry. */
    public static long bit(Entry e) { return 1L << e.idx(); }

    /** The entry after {@code cur} among the open ones (wraps); the first when {@code cur} is -1 or no longer open; null when none is open. */
    public static Entry cycle(List<Entry> all, int curIdx, int swordLevel, long mask) {
        List<Entry> a = availableList(all, swordLevel, mask);
        if (a.isEmpty()) return null;
        int at = -1;
        for (int i = 0; i < a.size(); i++) if (a.get(i).idx() == curIdx) at = i;
        return a.get((at + 1) % a.size());
    }

    /** The picked entry when it is still open, else the first open one; null when none is open. */
    public static Entry current(List<Entry> all, int pickIdx, int swordLevel, long mask) {
        List<Entry> a = availableList(all, swordLevel, mask);
        if (a.isEmpty()) return null;
        for (Entry e : a) if (e.idx() == pickIdx) return e;
        return a.get(0);
    }

    // ---------------------------------------------------------------- the worst case, for the docs and the tests
    /** The most entries a body looks at to find the one to cast: the whole table, once a gap (never per tick). */
    public static int lookupsPerCast() { return MAX; }
}
