package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the profile of one Decennial boss, as a plain record with no Minecraft type, read from {@code data/nusmp/mega/decennial/bosses/<side>_<nnn>.json} by a strict parser that says in plain words
 * what is wrong with a file and clamps every number through {@link DecennialRules}. It is the same idea as the D&D profile of the Guardian ({@code guard.DndProfile}), one level up: the D&D profile
 * says which legendary actions and resistances a boss has; this one says WHO the boss of a level is (name, title, lore), WHERE it fights (the arena theme), what it is made of (creature type,
 * damage resistances, signature mechanic), how the fight unfolds (two to four phases, each with a health threshold, abilities, a speed and damage change and adds), what the hall itself does
 * (one or two lair actions), what it pays (a loot hook at the level's grade) and how it looks and sounds (a look of the 22 abyssal looks with a tint and a size, and a sound set key). Keeping it as
 * data is what makes 200 bosses a table and not 200 classes: the runtime ({@code DecennialLayer}) is one piece of code that reads whichever profile its Guardian was given.
 *
 * @param band   0, or the gate band (1..7) of the Underworld Root this level belongs to (Evil levels only)
 * @param devil  "" or the name of the {@code blackclover.Devil} this boss is (the Qliphoth's stationed devils)
 * @param famous "" or the id of the famous D&D profile whose creature this boss is (the 17 famous villains each stand as a level's boss)
 */
public record DecennialProfile(String id, int level, String side, String name, String title, String lore, String archetype, String theme, String creature, List<String> resist, String signature,
                               List<Phase> phases, List<LairRef> lairs, AddsSpec adds, Loot loot, ManaCore manaCore, String look, int tint, double scale, String soundSet, String famous, Dnd dnd,
                               int band, String devil) {

    /**
     * One phase. {@code at} is the share of health under which the phase begins (the first is 1.0); {@code speed} and {@code damage} are multipliers of the boss's own numbers while the phase lasts;
     * {@code abilities} are the legendary actions it may spend points on in this phase (ids of {@code DndRules.LEGENDARY_IDS}); {@code addCount} adds of one of {@code addKinds} step out when it begins.
     */
    public record Phase(String name, double at, List<String> abilities, double speed, double damage, int addCount, List<String> addKinds, String shout) {}

    /** A lair action the boss owns: the id of a {@link DecennialVocab.Lair}, how often it comes (seconds), from which phase, and a power factor on its damage (0.5 to 1.5). */
    public record LairRef(String id, int every, int fromPhase, double power) {}

    /** The add budget: the kinds, the most alive at once, the most the whole fight may spawn, and (optionally) a wave of {@code wave} every {@code waveEvery} seconds while few are alive. */
    public record AddsSpec(List<String> pool, int maxAlive, int fightCap, int wave, int waveEvery) {}

    /** The loot hook: the grade of the level, the kind (weapon, relic or armour), the weapon type of {@code GradeDamage} and the relic keywords that suit the boss. */
    public record Loot(String grade, String kind, String type, List<String> keywords) {}

    /** The Mana Core it pays at the seal: its title and tier (1..5, from the level). */
    public record ManaCore(String title, int tier) {}

    /** The D&D layer numbers of the boss (the same fields the Guardian's own profile has). */
    public record Dnd(int resistances, int legendaryPoints, boolean antiMagicAura, boolean frightfulAura, boolean absorbMagic, String breath, boolean reflective) {}

    public boolean evil() { return level < 0; }

    public int depth() { return Math.abs(level); }

    public int floor() { return DecennialRules.floorOf(level); }

    public Phase phase(int index) { return phases.get(Math.max(1, Math.min(phases.size(), index)) - 1); }

    public int phaseCount() { return phases.size(); }

    /** The abilities usable in phase {@code index}: everything the phases up to it list, in first-seen order. */
    public List<String> abilitiesUpTo(int index) {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= Math.min(index, phases.size()); i++) for (String a : phases.get(i - 1).abilities()) if (!out.contains(a)) out.add(a);
        return out;
    }

    // ================================================================ parsing
    /** Reads a file; throws {@link IllegalArgumentException} with a plain sentence when it is wrong. */
    public static DecennialProfile parse(String id, JsonObject o) {
        int level = DecennialRules.levelOfId(id);
        if (level == 0) throw new IllegalArgumentException(id + ": the id must be holy_001..holy_100 or evil_001..evil_100");
        if (o.has("level") && o.get("level").getAsInt() != level) throw new IllegalArgumentException(id + ": 'level' says " + o.get("level").getAsInt() + " but the id says " + level);
        String side = str(o, "side", id);
        if (!side.equals(DecennialRules.side(level))) throw new IllegalArgumentException(id + ": side '" + side + "' does not fit the level");
        String name = str(o, "name", id), title = str(o, "title", id), lore = str(o, "lore", id);
        if (name.trim().length() < 3 || name.contains("TODO") || name.toLowerCase(java.util.Locale.ROOT).contains("placeholder")) throw new IllegalArgumentException(id + ": the name '" + name + "' is a placeholder");
        String theme = str(o, "theme", id);
        if (!theme.matches("[a-z][a-z0-9_]{2,31}")) throw new IllegalArgumentException(id + ": theme '" + theme + "' is not an arena id");
        String creature = str(o, "creature", id);
        if (!DecennialVocab.CREATURES.contains(creature)) throw new IllegalArgumentException(id + ": unknown creature type '" + creature + "' (known: " + DecennialVocab.CREATURES + ")");
        List<String> resist = DecennialVocab.strings(o, "resist");
        for (String r : resist) if (!DecennialVocab.RESIST.contains(r)) throw new IllegalArgumentException(id + ": unknown resistance '" + r + "' (known: " + DecennialVocab.RESIST + ")");
        if (resist.size() > 3) throw new IllegalArgumentException(id + ": at most three damage resistances");
        String signature = str(o, "signature", id);
        if (DecennialVocab.signature(signature) == null) throw new IllegalArgumentException(id + ": unknown signature mechanic '" + signature + "'");

        List<Phase> phases = new ArrayList<>();
        JsonArray pa = o.has("phases") && o.get("phases").isJsonArray() ? o.getAsJsonArray("phases") : new JsonArray();
        for (JsonElement el : pa) {
            JsonObject p = el.getAsJsonObject();
            List<String> abilities = DecennialVocab.strings(p, "abilities");
            for (String a : abilities) if (!KNOWN_ABILITIES.contains(a)) throw new IllegalArgumentException(id + ": unknown ability '" + a + "' (known: " + KNOWN_ABILITIES + ")");
            List<String> kinds = DecennialVocab.strings(p, "add_kinds");
            for (String k : kinds) if (!DecennialVocab.ADD_KINDS.contains(k)) throw new IllegalArgumentException(id + ": unknown add kind '" + k + "' (known: " + DecennialVocab.ADD_KINDS + ")");
            int addCount = (int) DecennialRules.clamp(num(p, "add_count", 0), 0, DecennialRules.MAX_ADDS_PER_WAVE);
            if (addCount > 0 && kinds.isEmpty()) throw new IllegalArgumentException(id + ": a phase brings adds but names no kind");
            phases.add(new Phase(str(p, "name", id), DecennialRules.clamp(num(p, "at", 1.0), 0.05, 1.0), List.copyOf(abilities),
                    DecennialRules.clamp(num(p, "speed", 1.0), DecennialRules.MIN_SPEED_MULT, DecennialRules.MAX_SPEED_MULT),
                    DecennialRules.clamp(num(p, "damage", 1.0), 0.5, DecennialRules.MAX_DAMAGE_MULT), addCount, List.copyOf(kinds), p.has("shout") ? p.get("shout").getAsString() : ""));
        }
        if (phases.size() < DecennialRules.MIN_PHASES || phases.size() > DecennialRules.MAX_PHASES) throw new IllegalArgumentException(id + ": a boss has " + DecennialRules.MIN_PHASES + " to " + DecennialRules.MAX_PHASES
                + " phases, not " + phases.size());
        if (phases.get(0).at() != 1.0) throw new IllegalArgumentException(id + ": the first phase must start at 1.0");
        for (int i = 1; i < phases.size(); i++) {
            if (phases.get(i - 1).at() - phases.get(i).at() < DecennialRules.MIN_PHASE_GAP - 1e-9) throw new IllegalArgumentException(id + ": phase " + (i + 1) + " starts too close under phase " + i
                    + " (at least " + DecennialRules.MIN_PHASE_GAP + " of health apart)");
        }

        List<LairRef> lairs = new ArrayList<>();
        JsonArray la = o.has("lair_actions") && o.get("lair_actions").isJsonArray() ? o.getAsJsonArray("lair_actions") : new JsonArray();
        for (JsonElement el : la) {
            JsonObject l = el.getAsJsonObject();
            String lid = str(l, "id", id);
            if (DecennialVocab.lair(lid) == null) throw new IllegalArgumentException(id + ": unknown lair action '" + lid + "'");
            lairs.add(new LairRef(lid, (int) DecennialRules.clamp(num(l, "every", 16), DecennialRules.MIN_LAIR_SECONDS, DecennialRules.MAX_LAIR_SECONDS),
                    (int) DecennialRules.clamp(num(l, "from_phase", 1), 1, phases.size()), DecennialRules.clamp(num(l, "power", 1.0), 0.5, 1.5)));
        }
        if (lairs.size() < DecennialRules.MIN_LAIRS || lairs.size() > DecennialRules.MAX_LAIRS) throw new IllegalArgumentException(id + ": a boss has " + DecennialRules.MIN_LAIRS + " to " + DecennialRules.MAX_LAIRS
                + " lair actions, not " + lairs.size());

        JsonObject ad = o.has("adds") ? o.getAsJsonObject("adds") : new JsonObject();
        List<String> pool = DecennialVocab.strings(ad, "pool");
        for (String k : pool) if (!DecennialVocab.ADD_KINDS.contains(k)) throw new IllegalArgumentException(id + ": unknown add kind '" + k + "'");
        AddsSpec adds = new AddsSpec(List.copyOf(pool), (int) DecennialRules.clamp(num(ad, "max_alive", 6), 0, DecennialRules.MAX_ADDS_ALIVE),
                (int) DecennialRules.clamp(num(ad, "fight_cap", 24), 0, DecennialRules.MAX_ADDS_PER_FIGHT), (int) DecennialRules.clamp(num(ad, "wave", 0), 0, DecennialRules.MAX_ADDS_PER_WAVE),
                (int) DecennialRules.clamp(num(ad, "wave_every", 0), 0, 300));
        if (adds.maxAlive() > 0 && adds.pool().isEmpty()) throw new IllegalArgumentException(id + ": adds are allowed but the pool is empty");

        JsonObject lo = o.has("loot") ? o.getAsJsonObject("loot") : new JsonObject();
        String grade = str(lo, "grade", id);
        if (!grade.equals(DecennialRules.gradeOf(level))) throw new IllegalArgumentException(id + ": loot grade '" + grade + "' is not the grade of level " + level + " (" + DecennialRules.gradeOf(level) + ")");
        String kind = lo.has("kind") ? lo.get("kind").getAsString() : "weapon";
        if (!List.of("weapon", "relic", "armour").contains(kind)) throw new IllegalArgumentException(id + ": loot kind must be weapon, relic or armour");
        Loot loot = new Loot(grade, kind, lo.has("type") ? lo.get("type").getAsString() : "longsword", DecennialVocab.strings(lo, "keywords"));

        JsonObject mo = o.has("mana_core") ? o.getAsJsonObject("mana_core") : new JsonObject();
        ManaCore core = new ManaCore(mo.has("title") ? mo.get("title").getAsString() : name + "'s Core", (int) DecennialRules.clamp(num(mo, "tier", DecennialRules.tierOf(level)), 1, 5));

        String look = o.has("look") ? o.get("look").getAsString() : "";
        if (!look.isEmpty() && !look.matches("[a-z][a-z0-9_]{0,31}")) throw new IllegalArgumentException(id + ": look '" + look + "' is not a look id");
        int tint = 0xFFFFFF;
        if (o.has("tint")) {
            String t = o.get("tint").getAsString();
            if (!t.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException(id + ": tint '" + t + "' is not #RRGGBB");
            tint = Integer.parseInt(t.substring(1), 16);
        }
        double scale = DecennialRules.clamp(num(o, "scale", 1.0), 0.7, 1.5);
        String sound = o.has("sound_set") ? o.get("sound_set").getAsString() : "";
        if (!sound.isEmpty() && !sound.matches("[a-z][a-z0-9_]{1,40}")) throw new IllegalArgumentException(id + ": sound_set '" + sound + "' is not a key");
        String famous = o.has("famous") ? o.get("famous").getAsString() : "";

        JsonObject dn = o.has("dnd") ? o.getAsJsonObject("dnd") : new JsonObject();
        String breath = dn.has("breath") ? dn.get("breath").getAsString() : "fire";
        if (!List.of("fire", "frost", "lightning", "poison", "acid").contains(breath)) throw new IllegalArgumentException(id + ": unknown breath '" + breath + "'");
        Dnd dnd = new Dnd((int) DecennialRules.clamp(num(dn, "resistances", 3), 0, 5), (int) DecennialRules.clamp(num(dn, "legendary_points", 3), 1, 5), flag(dn, "anti_magic_aura"), flag(dn, "frightful_aura"),
                flag(dn, "absorb_magic"), breath, flag(dn, "reflective"));

        int band = (int) DecennialRules.clamp(num(o, "band", 0), 0, 7);
        String devil = o.has("devil") ? o.get("devil").getAsString() : "";
        return new DecennialProfile(id, level, side, name, title, lore, o.has("archetype") ? o.get("archetype").getAsString() : "", theme, creature, List.copyOf(resist), signature, List.copyOf(phases),
                List.copyOf(lairs), adds, loot, core, look, tint, scale, sound, famous, dnd, band, devil);
    }

    /** The abilities a phase may list: the legendary actions of the D&D layer (kept as text so this class stays free of the game; a test pins the list against {@code DndRules.LEGENDARY_IDS}). */
    public static final List<String> KNOWN_ABILITIES = List.of("tail_sweep", "blink", "cantrip", "wing_buffet", "frightful_presence", "magic_drain", "counterspell", "life_drain", "haki_pulse", "gravity_crush",
            "dragon_breath", "eye_rays", "mind_blast", "death_touch", "vampiric_bite");

    private static String str(JsonObject o, String k, String id) {
        if (!o.has(k) || !o.get(k).isJsonPrimitive()) throw new IllegalArgumentException(id + ": missing text field '" + k + "'");
        return o.get(k).getAsString();
    }

    private static double num(JsonObject o, String k, double dflt) { return o.has(k) ? o.get(k).getAsDouble() : dflt; }

    private static boolean flag(JsonObject o, String k) { return o.has(k) && o.get(k).getAsBoolean(); }
}
