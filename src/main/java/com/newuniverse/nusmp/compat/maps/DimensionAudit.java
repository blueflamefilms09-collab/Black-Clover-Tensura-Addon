package com.newuniverse.nusmp.compat.maps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: map mods read a few fields of a dimension type (does it have a ceiling, a sky, how tall is it, how are coordinates scaled) and key their data by the dimension id,
 * and the game itself refuses a dimension type whose height is not a multiple of 16 or whose logical height is above its height. This class checks the mod's own
 * dimension JSONs against those rules and names each dimension's map profile ({@link Profile}), so a later edit cannot break the maps unnoticed. It only READS the
 * JSON given to it; the loader at the bottom reads the mod's own resources from the class path. PURE apart from that loader. Documented in docs/compat/maps.md section 3.
 */
public final class DimensionAudit {
    private DimensionAudit() {}

    /** The dimensions of the mod, by path in {@code nusmp:}. A unit test compares this list with the folder, so a new dimension has to be added here. */
    public static final List<String> NAMES = List.of("mega_dungeon", "sanctuary", "labyrinth", "living_dungeon", "grimoire_library", "arc_arena", "zogratis_arena", "dream");

    /** How a map mod will most likely see the dimension (cave style layer under a roof, or a plain top-down surface). */
    public enum Profile {
        SURFACE("surface (sky, no ceiling): plain top-down map"),
        ROOFED("roofed, no sky (Nether-like): maps use a cave style layer under the player"),
        ROOFED_SKYLIT("roofed with a sky: maps use a cave style layer"),
        OPEN_DARK("no sky and no ceiling: top-down map, dark");

        public final String text;

        Profile(String text) { this.text = text; }
    }

    /** The result for one dimension. {@code problems} is empty when every rule holds. */
    public record Report(String id, Profile profile, int minY, int height, int logicalHeight, double scale, String effects, List<String> problems) {
        public boolean ok() { return problems.isEmpty(); }
    }

    public static Profile profileOf(boolean skylight, boolean ceiling) {
        if (ceiling) return skylight ? Profile.ROOFED_SKYLIT : Profile.ROOFED;
        return skylight ? Profile.SURFACE : Profile.OPEN_DARK;
    }

    /**
     * Checks a dimension type JSON ({@code type}) and the dimension JSON that uses it ({@code dimension}, may be null) for the dimension {@code id}.
     * {@code knownTypes} are the dimension type ids that exist in this mod ({@code nusmp:...}); a {@code minecraft:} type is accepted without a lookup.
     */
    public static Report audit(String id, JsonObject type, JsonObject dimension, Set<String> knownTypes) {
        List<String> p = new ArrayList<>();
        if (type == null) {
            p.add("no dimension type JSON");
            return new Report(id, Profile.SURFACE, 0, 0, 0, 1.0, "", p);
        }
        for (String f : List.of("has_skylight", "has_ceiling", "min_y", "height", "logical_height", "coordinate_scale", "ambient_light", "effects"))
            if (!type.has(f)) p.add("missing field " + f);
        boolean sky = bool(type, "has_skylight"), ceil = bool(type, "has_ceiling");
        int minY = num(type, "min_y"), height = num(type, "height"), logical = num(type, "logical_height");
        double scale = type.has("coordinate_scale") ? type.get("coordinate_scale").getAsDouble() : 1.0;
        String effects = type.has("effects") ? type.get("effects").getAsString() : "";
        if (minY % 16 != 0) p.add("min_y " + minY + " is not a multiple of 16");
        if (height % 16 != 0) p.add("height " + height + " is not a multiple of 16");
        if (height < 16 || height > 4064) p.add("height " + height + " is outside 16..4064");
        if (minY < -2032 || minY > 2031) p.add("min_y " + minY + " is outside -2032..2031");
        if (minY + height > 2032) p.add("min_y + height is " + (minY + height) + ", over 2032");
        if (logical < 0 || logical > height) p.add("logical_height " + logical + " is not within 0.." + height);
        if (!(scale >= 0.00001 && scale <= 3.0E7)) p.add("coordinate_scale " + scale + " is outside 0.00001..30000000");
        if (!effects.isEmpty() && effects.indexOf(':') <= 0) p.add("effects '" + effects + "' is not a namespaced id");
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) p.add("dimension id '" + id + "' is not a valid lower-case id (map mods key their data by it)");
        if (dimension != null) {
            String t = dimension.has("type") && dimension.get("type").isJsonPrimitive() ? dimension.get("type").getAsString() : "";
            if (t.isEmpty()) p.add("the dimension JSON has no type");
            else if (t.startsWith("nusmp:") && !knownTypes.contains(t)) p.add("the dimension JSON names type " + t + " which does not exist");
        }
        return new Report(id, profileOf(sky, ceil), minY, height, logical, scale, effects, p);
    }

    private static boolean bool(JsonObject o, String k) { return o.has(k) && o.get(k).getAsBoolean(); }

    private static int num(JsonObject o, String k) {
        if (!o.has(k)) return 0;
        JsonElement e = o.get(k);
        return e.getAsInt();
    }

    // ---------------------------------------------------------------- loader (class path)
    /** Reads {@code data/nusmp/<folder>/<name>.json} from the mod's resources; null when it is not there or not JSON. */
    public static JsonObject load(ClassLoader cl, String folder, String name) {
        String path = "data/nusmp/" + folder + "/" + name + ".json";
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) return null;
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement e = JsonParser.parseReader(r);
                return e.isJsonObject() ? e.getAsJsonObject() : null;
            }
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    /** Audits every dimension of {@link #NAMES} from the class path. */
    public static List<Report> auditAll(ClassLoader cl) {
        Set<String> types = new java.util.HashSet<>();
        for (String n : NAMES) if (load(cl, "dimension_type", n) != null) types.add("nusmp:" + n);
        List<Report> out = new ArrayList<>();
        for (String n : NAMES) out.add(audit("nusmp:" + n, load(cl, "dimension_type", n), load(cl, "dimension", n), types));
        return out;
    }
}
