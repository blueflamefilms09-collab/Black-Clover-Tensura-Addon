package com.newuniverse.nusmp.boss.bars;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Why: a style is data ({@code assets/nusmp/boss_bars/<id>.json}): which emblem, which fill effect, how it enters and dies, and the colours. This is the strict reader (pure, so the tests and the
 * game read the files with the same code): it says in plain words what is wrong with a file, and it refuses an unknown emblem, effect, entrance or death so a typo cannot silently draw nothing.
 * The art (the frame atlas) is not in the json: it is {@code textures/gui/boss_bar/frame_<id>.png}, named by the id.
 */
public record BarStyle(String id, String family, String emblem, String fx, String entrance, String death, Map<String, Integer> colors, float shimmerSpeed, float shimmerStrength) {
    public static final List<String> FAMILIES = List.of("theme", "arc", "boss");
    public static final List<String> ENTRANCES = List.of("slide", "rise", "fade", "flash");
    public static final List<String> DEATHS = List.of("shatter", "dissolve");
    public static final List<String> FX = List.of("shimmer", "drip", "embers", "wave", "sparks", "frost", "souls", "leaves", "static", "glitch", "veins", "runes", "shield");
    /** The emblems of {@code emblems.png}, in cell order (8 per row). Appended only. */
    public static final List<String> EMBLEMS = List.of("wing", "thorn_crown", "skull", "flame", "snowflake", "bolt", "void_eye", "leaf", "wave", "blood_drop", "gear", "mirror", "root", "gate", "horns",
            "crown_horns", "dragon", "feather", "halo", "lich", "broken_crown", "ankh", "plague", "triad", "eclipse", "sins", "spade", "knight", "labyrinth", "crown", "scales", "anvil", "whirlpool",
            "key", "spirit", "rune");
    /** The colours every style gives (all {@code #RRGGBB}). */
    public static final List<String> COLOR_KEYS = List.of("fill_top", "fill_bottom", "glow", "accent", "ghost", "name", "title", "shadow", "frame", "shield", "invulnerable", "cast", "lair", "marker");

    private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}");

    public int color(String key) {
        Integer c = colors.get(key);
        return c == null ? 0xFFFFFF : c;
    }

    public int emblemIndex() { return EMBLEMS.indexOf(emblem); }

    public int fxIndex() { return FX.indexOf(fx); }

    /** Reads a file; throws {@link IllegalArgumentException} with a plain sentence when it is wrong. */
    public static BarStyle parse(String json) {
        JsonObject o;
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) throw new IllegalArgumentException("not a json object");
            o = e.getAsJsonObject();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("unreadable json: " + ex.getMessage());
        }
        String id = str(o, "id", null);
        if (!id.matches("[a-z][a-z0-9_]{1,31}")) throw new IllegalArgumentException(id + ": the id must be lower case words and underscores");
        String family = str(o, "family", id);
        if (!FAMILIES.contains(family)) throw new IllegalArgumentException(id + ": unknown family '" + family + "' (known: " + FAMILIES + ")");
        String emblem = str(o, "emblem", id);
        if (!EMBLEMS.contains(emblem)) throw new IllegalArgumentException(id + ": unknown emblem '" + emblem + "' (known: " + EMBLEMS + ")");
        String fx = str(o, "fx", id);
        if (!FX.contains(fx)) throw new IllegalArgumentException(id + ": unknown fill effect '" + fx + "' (known: " + FX + ")");
        String entrance = str(o, "entrance", id);
        if (!ENTRANCES.contains(entrance)) throw new IllegalArgumentException(id + ": unknown entrance '" + entrance + "' (known: " + ENTRANCES + ")");
        String death = str(o, "death", id);
        if (!DEATHS.contains(death)) throw new IllegalArgumentException(id + ": unknown death '" + death + "' (known: " + DEATHS + ")");
        if (!o.has("colors") || !o.get("colors").isJsonObject()) throw new IllegalArgumentException(id + ": missing 'colors'");
        JsonObject co = o.getAsJsonObject("colors");
        Map<String, Integer> colors = new LinkedHashMap<>();
        for (String k : COLOR_KEYS) {
            String v = str(co, k, id + " colors");
            if (!HEX.matcher(v).matches()) throw new IllegalArgumentException(id + ": colour '" + k + "' is '" + v + "', not #RRGGBB");
            colors.put(k, Integer.parseInt(v.substring(1), 16));
        }
        float speed = 1f, strength = 0.5f;
        if (o.has("shimmer") && o.get("shimmer").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("shimmer");
            if (s.has("speed")) speed = Math.max(0f, Math.min(4f, s.get("speed").getAsFloat()));
            if (s.has("strength")) strength = Math.max(0f, Math.min(1f, s.get("strength").getAsFloat()));
        }
        return new BarStyle(id, family, emblem, fx, entrance, death, Map.copyOf(colors), speed, strength);
    }

    private static String str(JsonObject o, String k, String who) {
        if (!o.has(k) || !o.get(k).isJsonPrimitive()) throw new IllegalArgumentException((who == null ? "style" : who) + ": missing text field '" + k + "'");
        return o.get(k).getAsString();
    }
}
