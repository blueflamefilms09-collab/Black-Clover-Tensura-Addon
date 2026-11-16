package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Boss rework wave 3, part 3 (docs/bosses/rework_wave3.md): the tier families of the 200 Decennial bosses. A family is a side (evil or holy) and a D&D tier of
 * {@link DecennialRules#tierOf} (levels 1..20 are tier 1 and so on), so there are ten of them. Each family has ONE visual signature (the name of a {@code VfxShape} and a colour,
 * drawn at the boss's feet now and then and as the move lands) and ONE extra move (a shape on the floor: a circle, a donut with a safe middle, or a line through the foe), all as
 * data in {@code data/anime_multiverse/mega/decennial/tier_families.json}, written by {@code tools/gen_decennial_roster.py}. Pure: no game types (the shape is a name, checked
 * against the enum by the tests); every number is clamped so a bad file cannot make a move that is not bounded.
 */
public final class DecennialFamilies {
    private DecennialFamilies() {}

    /** The floor shapes a family move takes. */
    public enum Pattern { CIRCLE, DONUT, LINE }

    /**
     * @param radius  circle and donut: the outer radius; line: the length
     * @param period  seconds between casts
     * @param warn    ticks the shape is shown before it lands
     * @param share   the part of the boss's base attack one hit takes (after the player's hit cap)
     */
    public record Family(String id, Pattern pattern, DecennialVocab.Effect effect, String shape, int rgb, double radius, int period, int warn, double share, int effectTicks,
                         String label) {}

    public static final double MIN_RADIUS = 2.0, MAX_RADIUS = 18.0, MAX_SHARE = 0.12, MIN_SHARE = 0.02;
    public static final int MIN_PERIOD = 8, MAX_PERIOD = 60, MIN_WARN = 20, MAX_WARN = 60, MAX_EFFECT_TICKS = 60;
    /** The width of a line move (blocks from its axis). */
    public static final double LINE_HALF_WIDTH = 1.6;

    private static final String RESOURCE = "/data/anime_multiverse/mega/decennial/tier_families.json";
    private static volatile Map<String, Family> families;

    /** The family id of a level: {@code evil_1} .. {@code holy_5}. Pure. */
    public static String idOf(int level) { return DecennialRules.side(level) + "_" + DecennialRules.tierOf(level); }

    /** The family of a level, or null when the data has none. */
    public static Family of(int level) {
        if (!DecennialRules.validLevel(level)) return null;
        return families().get(idOf(level));
    }

    public static Map<String, Family> families() {
        Map<String, Family> m = families;
        if (m == null) {
            synchronized (DecennialFamilies.class) {
                m = families;
                if (m == null) {
                    m = parse(read());
                    families = m;
                }
            }
        }
        return m;
    }

    /** Reads the table (keys starting with an underscore are notes). A row naming an unknown pattern or effect is refused; every number is clamped. */
    public static Map<String, Family> parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Family> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            JsonObject o = e.getValue().getAsJsonObject();
            Pattern pattern = Pattern.valueOf(o.get("pattern").getAsString().toUpperCase(Locale.ROOT));
            DecennialVocab.Effect effect = DecennialVocab.Effect.valueOf(o.get("effect").getAsString().toUpperCase(Locale.ROOT));
            int rgb = Integer.parseInt(o.get("color").getAsString().replace("#", ""), 16) & 0xFFFFFF;
            out.put(e.getKey(), new Family(e.getKey(), pattern, effect, o.get("shape").getAsString(), rgb,
                    DecennialRules.clamp(o.get("radius").getAsDouble(), MIN_RADIUS, MAX_RADIUS),
                    (int) DecennialRules.clamp(o.get("period").getAsInt(), MIN_PERIOD, MAX_PERIOD),
                    (int) DecennialRules.clamp(o.get("warn").getAsInt(), MIN_WARN, MAX_WARN),
                    DecennialRules.clamp(o.get("share").getAsDouble(), MIN_SHARE, MAX_SHARE),
                    (int) DecennialRules.clamp(o.get("effectTicks").getAsInt(), 0, MAX_EFFECT_TICKS),
                    o.get("label").getAsString()));
        }
        return out;
    }

    /**
     * Is the point {@code (px, pz)} inside the shape? Circle and donut are centred on {@code (cx, cz)}; a line runs from {@code (cx, cz)} along the unit direction {@code (dx, dz)}
     * for {@code radius} blocks. Pure.
     */
    public static boolean inside(Pattern pattern, double radius, double cx, double cz, double dx, double dz, double px, double pz) {
        double rx = px - cx, rz = pz - cz;
        switch (pattern) {
            case CIRCLE:
                return rx * rx + rz * rz <= radius * radius;
            case DONUT:
                double d2 = rx * rx + rz * rz;
                return d2 <= radius * radius && d2 >= radius * radius * 0.2025;                  // a safe middle of 45 percent of the radius
            default:
                double along = rx * dx + rz * dz, across = Math.abs(rx * dz - rz * dx);
                return along >= 0.0 && along <= radius && across <= LINE_HALF_WIDTH;
        }
    }

    private static String read() {
        try (InputStream in = DecennialFamilies.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("missing resource " + RESOURCE);
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                StringBuilder b = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) b.append(buf, 0, n);
                return b.toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RESOURCE + ": " + e.getMessage());
        }
    }
}
