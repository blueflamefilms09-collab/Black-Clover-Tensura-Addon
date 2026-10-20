package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Why: which exclusive pocket dungeons exist, and everything that makes each one itself, is DATA ({@code data/nusmp/mega/pockets/<id>.json}, written by {@code tools/rift/gen_pockets.py} from
 * its roster), so a server owner can retune a tomb or add a fourteenth with a datapack and no rebuild, the same reason the Decennial boss profiles are JSON. A definition is a plain record with no
 * Minecraft type, parsed by a strict reader that says in plain words what is wrong with a file (a mob roster of two, an unknown element, a colour that is not a colour) and clamps every
 * number, so one bad file costs one tomb its place in the pool and never the whole load. What a definition holds: the LOOK (the colour of the tear and of its skybox core, the biome: sky, fog,
 * water, ambient particles, vanilla sound ids, the soft ids of the mod's own sounds, the weather, the gravity rules), the LAYOUT (grid, room count, cycles, how many zones of inverted and
 * negated gravity and ceiling rivers, the elements of its trap rooms, the block palette), the EXCLUSIVE MOBS (three to five profiles: a body from the Mob Foundry or a vanilla/Tensura type,
 * a name, attribute multipliers, a size, an aura), the MEGA-BOSS (the id of a D&amp;D boss profile in {@code mega/pockets/bosses}), the SEAL of the treasury (the Black Clover magics that unravel
 * it, the force it takes to break, the Sigil), the RELICS (which ones drop from the boss, from the treasury, and which one is the primary artifact whose removal collapses the tomb) and the
 * ancient magic that resonates with a mage's grimoire.
 */
public record PocketDef(String id, String name, String series, String lore, int accent, int sky, Biome biome, Gravity gravity, Palette palette, Layout layout, List<PocketElement> traps,
                        List<Mob> mobs, String boss, String theme, Seal seal, Relics relics, List<String> resonance, int collapseSeconds, int weight, int minDepth, int floorMin) {

    public PocketDef {
        traps = List.copyOf(traps);
        mobs = List.copyOf(mobs);
        resonance = List.copyOf(resonance);
    }

    public static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{2,40}");
    private static final Pattern RES = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    /** The look of the pocket's biome: the colours, the ambient particle, the vanilla sound ids of the biome JSON and the soft (mod) ids played by code with a vanilla fallback. */
    public record Biome(int sky, int fog, int water, int waterFog, String particle, double particleChance, String ambient, String mood, String additions, String music, String weather,
                        String softAmbient, String softCrackle) {}

    /** The gravity rules: how many rooms of inverted gravity (ceiling walkers), of negated (weightless) gravity and with a river on the ceiling, and the pocket-wide gravity as a share of normal. */
    public record Gravity(int inverted, int negated, int rivers, double base) {}

    /** The blocks the pocket is built of (registry ids); a block the registry does not know is replaced by stone bricks at build time and reported by the self-test. */
    public record Palette(String wall, String floor, String ceiling, String pillar, String accent, String light, String glass, String liquid) {}

    /** The generator's knobs: grid side (odd, 5 or 7), room count, share of extra loop doors, and the boss lair's half width. */
    public record Layout(int grid, int rooms, double cycles, int bossHalf) {}

    /**
     * One exclusive mob. {@code base} is {@code foundry:<id>} (a body of the Mob Foundry: the look) or an entity type id; {@code fallback} is the entity type used when the base is not
     * available. The multipliers scale the stats the base would have on that floor; {@code scale} the size; {@code glow} the outline colour; the aura is a vanilla particle in a colour.
     */
    public record Mob(String id, String name, String base, String fallback, String habitat, String role, double hp, double damage, double armor, double speed, double scale, double knockback,
                      int glow, String aura, int auraRgb, int weight, int groupMin, int groupMax, boolean elite) {
        public boolean air() { return "air".equals(habitat); }

        public boolean foundryBase() { return base.startsWith("foundry:"); }

        /** The foundry id of a foundry base ("" for any other). */
        public String foundryId() { return foundryBase() ? base.substring("foundry:".length()) : ""; }
    }

    /** The seal of the treasury: the Black Clover magics that unravel it, its name, the force (points of attack damage) that breaks it, and the name of the Sigil that opens it. */
    public record Seal(List<String> magic, String label, int forceHp, String sigil) {
        public Seal {
            magic = List.copyOf(magic);
        }
    }

    /** The relics: those the mega-boss drops, those the treasury's pedestals give, and the primary artifact (one of the treasury's) whose removal starts the collapse. */
    public record Relics(List<String> boss, List<String> treasury, String primary) {
        public Relics {
            boss = List.copyOf(boss);
            treasury = List.copyOf(treasury);
        }

        /** Every relic of the pocket, boss first. */
        public List<String> all() {
            List<String> out = new ArrayList<>(boss);
            for (String t : treasury) if (!out.contains(t)) out.add(t);
            return out;
        }
    }

    // ---------------------------------------------------------------- limits
    public static final int MOBS_MIN = 3, MOBS_MAX = 5, ROOMS_MIN = 10, ROOMS_MAX = 26, COLLAPSE_MIN = 60, COLLAPSE_MAX = 900;

    /** The id of the biome JSON this pocket's look is written to ({@code nusmp:pocket_<id>}). */
    public String biomeId() { return "nusmp:pocket_" + id; }

    /** The id of the boss profile file ({@code mega/pockets/bosses/<boss>.json}). */
    public String bossFile() { return "bosses/" + boss; }

    // ---------------------------------------------------------------- the reader
    /** Reads a file; throws {@link IllegalArgumentException} with a plain sentence when it is wrong. */
    public static PocketDef parse(String fileId, JsonObject o) {
        String id = str(o, "id");
        if (!ID.matcher(id).matches()) throw new IllegalArgumentException(fileId + ": id '" + id + "' is not a pocket id (lower-case words, digits and underscores, 3 to 41 characters)");
        if (!id.equals(fileId)) throw new IllegalArgumentException(fileId + ": the id '" + id + "' must be the file name");
        JsonObject b = obj(o, "biome", id);
        Biome biome = new Biome(color(b, "sky", id), color(b, "fog", id), color(b, "water", id), color(b, "water_fog", id), res(b, "particle", id), clampD(num(b, "particle_chance", 0.01), 0.0, 0.2),
                res(b, "ambient", id), res(b, "mood", id), res(b, "additions", id), res(b, "music", id), weather(b, id), res(b, "soft_ambient", id), res(b, "soft_crackle", id));
        JsonObject g = obj(o, "gravity", id);
        Gravity gravity = new Gravity(clampI((int) num(g, "inverted", 0), 0, 3), clampI((int) num(g, "negated", 0), 0, 2), clampI((int) num(g, "rivers", 0), 0, 3), clampD(num(g, "base", 1.0), 0.25, 1.0));
        JsonObject p = obj(o, "palette", id);
        Palette palette = new Palette(res(p, "wall", id), res(p, "floor", id), res(p, "ceiling", id), res(p, "pillar", id), res(p, "accent", id), res(p, "light", id), res(p, "glass", id), res(p, "liquid", id));
        JsonObject l = obj(o, "layout", id);
        int grid = (int) num(l, "grid", 7);
        if (grid != 5 && grid != 7) throw new IllegalArgumentException(id + ": layout.grid must be 5 or 7, not " + grid);
        int rooms = (int) num(l, "rooms", 18);
        if (rooms < ROOMS_MIN || rooms > ROOMS_MAX || rooms > grid * grid - 6) throw new IllegalArgumentException(id + ": layout.rooms " + rooms + " is outside " + ROOMS_MIN + ".." + Math.min(ROOMS_MAX, grid * grid - 6));
        Layout layout = new Layout(grid, rooms, clampD(num(l, "cycles", 0.35), 0.0, 1.0), clampI((int) num(l, "boss_half", 10), 8, 11));
        List<PocketElement> traps = new ArrayList<>();
        for (String t : strings(o, "traps")) {
            PocketElement e = PocketElement.byId(t);
            if (e == null) throw new IllegalArgumentException(id + ": unknown trap element '" + t + "'");
            if (!traps.contains(e)) traps.add(e);
        }
        if (traps.isEmpty() || traps.size() > 3) throw new IllegalArgumentException(id + ": traps needs one to three elements, not " + traps.size());
        List<Mob> mobs = new ArrayList<>();
        if (!o.has("mobs") || !o.get("mobs").isJsonArray()) throw new IllegalArgumentException(id + ": missing list 'mobs'");
        for (JsonElement el : o.getAsJsonArray("mobs")) mobs.add(mob(id, el.getAsJsonObject()));
        if (mobs.size() < MOBS_MIN || mobs.size() > MOBS_MAX) throw new IllegalArgumentException(id + ": mobs needs " + MOBS_MIN + " to " + MOBS_MAX + " exclusive mobs, not " + mobs.size());
        for (int i = 0; i < mobs.size(); i++) for (int j = i + 1; j < mobs.size(); j++) if (mobs.get(i).id().equals(mobs.get(j).id())) throw new IllegalArgumentException(id + ": two mobs are both called '" + mobs.get(i).id() + "'");
        boolean ground = false;
        for (Mob m : mobs) if (!m.air()) ground = true;
        if (!ground) throw new IllegalArgumentException(id + ": at least one mob must walk (habitat ground)");
        String boss = str(o, "boss");
        if (!ID.matcher(boss).matches()) throw new IllegalArgumentException(id + ": boss '" + boss + "' is not a profile id");
        String theme = str(o, "theme");
        if (!theme.matches("[a-z][a-z_]*")) throw new IllegalArgumentException(id + ": theme '" + theme + "' is not a region theme id");
        JsonObject s = obj(o, "seal", id);
        List<String> magic = new ArrayList<>();
        for (String m : strings(s, "magic")) magic.add(m.toUpperCase(Locale.ROOT));
        if (magic.isEmpty()) throw new IllegalArgumentException(id + ": seal.magic needs at least one Black Clover magic");
        Seal seal = new Seal(magic, str(s, "label"), clampI((int) num(s, "force", 2400), 400, 20000), str(s, "sigil"));
        JsonObject r = obj(o, "relics", id);
        List<String> rb = relicIds(r, "boss", id), rt = relicIds(r, "treasury", id);
        if (rb.isEmpty() || rb.size() > 2) throw new IllegalArgumentException(id + ": relics.boss needs one or two relic ids");
        if (rt.isEmpty() || rt.size() > 2) throw new IllegalArgumentException(id + ": relics.treasury needs one or two relic ids");
        String primary = str(r, "primary");
        if (!rt.contains(primary)) throw new IllegalArgumentException(id + ": relics.primary '" + primary + "' is not one of relics.treasury");
        for (String x : rb) if (rt.contains(x)) throw new IllegalArgumentException(id + ": the relic '" + x + "' is in both relics.boss and relics.treasury");
        List<String> resonance = new ArrayList<>();
        for (String m : strings(o, "resonance")) resonance.add(m.toUpperCase(Locale.ROOT));
        return new PocketDef(id, str(o, "name"), str(o, "series"), str(o, "lore"), color(o, "accent", id), color(o, "sky", id), biome, gravity, palette, layout, traps, mobs, boss, theme, seal,
                new Relics(rb, rt, primary), resonance, clampI((int) num(o, "collapse_seconds", 150), COLLAPSE_MIN, COLLAPSE_MAX), clampI((int) num(o, "weight", 10), 1, 1000),
                clampI((int) num(o, "min_depth", 0), 0, 1000), clampI((int) num(o, "floor_min", 1), 1, 1000));
    }

    private static Mob mob(String pocket, JsonObject m) {
        String id = str(m, "id");
        if (!ID.matcher(id).matches()) throw new IllegalArgumentException(pocket + ": mob id '" + id + "' is not an id");
        String base = str(m, "base");
        if (!base.startsWith("foundry:") && !RES.matcher(base).matches()) throw new IllegalArgumentException(pocket + "/" + id + ": base '" + base + "' is neither foundry:<id> nor an entity type id");
        if (base.startsWith("foundry:") && !ID.matcher(base.substring(8)).matches()) throw new IllegalArgumentException(pocket + "/" + id + ": foundry base '" + base + "' is not an id");
        String fallback = str(m, "fallback");
        if (!RES.matcher(fallback).matches()) throw new IllegalArgumentException(pocket + "/" + id + ": fallback '" + fallback + "' is not an entity type id");
        String habitat = str(m, "habitat");
        if (!habitat.equals("ground") && !habitat.equals("air")) throw new IllegalArgumentException(pocket + "/" + id + ": habitat must be ground or air, not '" + habitat + "'");
        String role = str(m, "role");
        if (!ROLES.contains(role)) throw new IllegalArgumentException(pocket + "/" + id + ": unknown role '" + role + "' (known: " + ROLES + ")");
        int gmin = clampI((int) num(m, "group_min", 1), 1, 8), gmax = clampI((int) num(m, "group_max", gmin), gmin, 8);
        JsonObject aura = obj(m, "aura", pocket + "/" + id);
        return new Mob(id, str(m, "name"), base, fallback, habitat, role, clampD(num(m, "hp", 1.0), 0.3, 6.0), clampD(num(m, "damage", 1.0), 0.3, 4.0), clampD(num(m, "armor", 0.0), 0.0, 12.0),
                clampD(num(m, "speed", 1.0), 0.5, 1.8), clampD(num(m, "scale", 1.0), 0.5, 2.5), clampD(num(m, "knockback", 0.0), 0.0, 1.0), color(m, "glow", pocket + "/" + id), res(aura, "particle", pocket + "/" + id),
                color(aura, "color", pocket + "/" + id), clampI((int) num(m, "weight", 3), 1, 20), gmin, gmax, m.has("elite") && m.get("elite").getAsBoolean());
    }

    /** The roles a mob profile may name. */
    public static final List<String> ROLES = List.of("melee", "ranged", "caster", "tank", "skirmisher", "brute");

    private static List<String> relicIds(JsonObject o, String key, String pocket) {
        List<String> out = new ArrayList<>();
        for (String s : strings(o, key)) {
            if (!s.matches("[a-z0-9_]+")) throw new IllegalArgumentException(pocket + ": relic id '" + s + "' is not snake_case");
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }

    private static String weather(JsonObject o, String id) {
        String w = o.has("weather") ? str(o, "weather") : "clear";
        if (!List.of("clear", "ash", "snow", "rain", "spores", "storm", "mist").contains(w)) throw new IllegalArgumentException(id + ": unknown weather '" + w + "'");
        return w;
    }

    // ---------------------------------------------------------------- field helpers
    static String str(JsonObject o, String k) {
        if (!o.has(k) || !o.get(k).isJsonPrimitive()) throw new IllegalArgumentException("missing text field '" + k + "'");
        String s = o.get(k).getAsString();
        if (s.isBlank()) throw new IllegalArgumentException("text field '" + k + "' is empty");
        return s;
    }

    static String res(JsonObject o, String k, String where) {
        String s = str(o, k);
        if (!RES.matcher(s).matches()) throw new IllegalArgumentException(where + ": '" + k + "' = '" + s + "' is not a namespaced id");
        return s;
    }

    static JsonObject obj(JsonObject o, String k, String where) {
        if (!o.has(k) || !o.get(k).isJsonObject()) throw new IllegalArgumentException(where + ": missing object '" + k + "'");
        return o.getAsJsonObject(k);
    }

    static double num(JsonObject o, String k, double dflt) {
        if (!o.has(k)) return dflt;
        if (!o.get(k).isJsonPrimitive() || !o.get(k).getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("'" + k + "' is not a number");
        double v = o.get(k).getAsDouble();
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new IllegalArgumentException("'" + k + "' is not finite");
        return v;
    }

    static int color(JsonObject o, String k, String where) {
        String s = str(o, k);
        if (!COLOR.matcher(s).matches()) throw new IllegalArgumentException(where + ": '" + k + "' = '" + s + "' is not a #rrggbb colour");
        return Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
    }

    static List<String> strings(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k)) {
            if (!o.get(k).isJsonArray()) throw new IllegalArgumentException("'" + k + "' is not a list");
            JsonArray a = o.getAsJsonArray(k);
            for (JsonElement el : a) out.add(el.getAsString());
        }
        return out;
    }

    static int clampI(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static double clampD(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
