package com.newuniverse.nusmp.mana.zone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A Grand Magic Zone (also called a Strong Magic Region): a PLACE of exceptionally dense, unstable natural mana, read from
 * {@code data/<ns>/grand_magic_zones/*.json} (pure: no Minecraft types, unit tested). It works like a dungeon through magical pressure:
 * weak players are hurt, strong players are fed, and the weather and terrain behave strangely. See docs/mana/mana_zone_redesign.md.
 *
 * <p>JSON fields: {@code name}, {@code dimension} (default minecraft:overworld), {@code shape} ("sphere" with {@code centre} and
 * {@code radius}, or "box" with {@code min} and {@code max}), {@code theme}, {@code density} (1..3, as ManaLaw.density),
 * {@code recommended_ep}, {@code effects} (list), {@code tint} ("#RRGGBB" or "0xAARRGGBB"), optional {@code tp} (x,y,z the teleport
 * command uses) and {@code border} (true for the neutral-zone barricades). The id is the file name.
 */
public record GrandZoneRegion(String id, String name, String dimension, boolean box, double cx, double cy, double cz, double radius,
                              double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                              Theme theme, double density, double recommendedEp, Set<Effect> effects, int tint, double[] tp, boolean border) {

    /** The look of a zone. The ordinal travels in the sync packet: ADD AT THE END only. */
    public enum Theme { SEABED, FOREST, VOLCANO, GRAVITO, HAVEN, WILD }

    /** What a zone does. The bit (1 << ordinal) travels in the sync packet: ADD AT THE END only. */
    public enum Effect {
        CHAOTIC_GRAVITY,      // random low-gravity pulses and pushes (Gravito Rock Zone)
        FLOATING_BOULDERS,    // client-only: massive rocks drifting in the air
        VIOLENT_CURRENTS,     // swirling currents in water (Seabed Temple)
        MAGMA_HEAT,           // heat haze, fire damage to the weak (Ultime-Volcano Mountain Trail)
        MANA_STORM,           // sky tint, fog, lightning
        WITCH_GLOW,           // drifting spores, disorientation (Witches' Forest)
        CALM,                 // safe haven: nothing here harms you, the weak are healed (Elysia)
        PRESSURE;             // slowness and fatigue on the weak

        public int bit() { return 1 << ordinal(); }

        public static Effect byName(String n) {
            if (n == null) return null;
            String s = n.trim().toUpperCase(Locale.ROOT);
            for (Effect e : values()) if (e.name().equals(s)) return e;
            return null;
        }
    }

    public static Theme themeByName(String n) {
        if (n != null) for (Theme t : Theme.values()) if (t.name().equalsIgnoreCase(n.trim())) return t;
        return Theme.WILD;
    }

    public boolean has(Effect e) { return effects.contains(e); }

    public int effectMask() {
        int m = 0;
        for (Effect e : effects) m |= e.bit();
        return m;
    }

    public static Set<Effect> fromMask(int mask) {
        Set<Effect> s = EnumSet.noneOf(Effect.class);
        for (Effect e : Effect.values()) if ((mask & e.bit()) != 0) s.add(e);
        return s;
    }

    /** True inside the zone. Spheres compare squared distances; no allocation. */
    public boolean contains(double x, double y, double z) {
        if (box) return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /** Distance to the border from the inside, in blocks (negative outside); used for the fade of the environment at the edge. */
    public double depth(double x, double y, double z) {
        if (box) return Math.min(Math.min(Math.min(x - minX, maxX - x), Math.min(y - minY, maxY - y)), Math.min(z - minZ, maxZ - z));
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return radius - Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** The radius the client uses to scale its effects (the box's half-extent for a box). */
    public double effectiveRadius() { return box ? Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY) * 0.5 : radius; }

    /** A copy with a new id and a new area: how the dungeon turns a zone's theme into the environment of one floor or room. */
    public GrandZoneRegion withBox(String newId, String dim, double x0, double y0, double z0, double x1, double y1, double z1) {
        double ax = Math.min(x0, x1), bx = Math.max(x0, x1), ay = Math.min(y0, y1), by = Math.max(y0, y1), az = Math.min(z0, z1), bz = Math.max(z0, z1);
        return new GrandZoneRegion(newId, name, dim, true, (ax + bx) / 2, (ay + by) / 2, (az + bz) / 2, Math.max(bx - ax, bz - az) / 2, ax, ay, az, bx, by, bz,
                theme, density, recommendedEp, effects, tint, null, border);
    }

    // ---------------------------------------------------------------- parsing
    /** Parses one file. Throws IllegalArgumentException with a readable reason when the file cannot make a zone. */
    public static GrandZoneRegion parse(String id, String json) {
        JsonElement e;
        try { e = JsonParser.parseString(json); } catch (RuntimeException ex) { throw new IllegalArgumentException(id + ": not valid JSON"); }
        if (!e.isJsonObject()) throw new IllegalArgumentException(id + ": not an object");
        return parse(id, e.getAsJsonObject());
    }

    public static GrandZoneRegion parse(String id, JsonObject o) {
        String shape = str(o, "shape", "sphere").toLowerCase(Locale.ROOT);
        boolean box = shape.equals("box");
        double[] c = vec(o, "centre", 0), mn = vec(o, "min", 0), mx = vec(o, "max", 0);
        double radius = num(o, "radius", 0);
        if (!box && radius <= 0) throw new IllegalArgumentException(id + ": a sphere needs a positive radius");
        if (box && (mx[0] <= mn[0] || mx[1] <= mn[1] || mx[2] <= mn[2])) throw new IllegalArgumentException(id + ": a box needs max above min on every axis");
        if (box) {
            c = new double[]{(mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2, (mn[2] + mx[2]) / 2};
            radius = Math.max(mx[0] - mn[0], mx[2] - mn[2]) / 2;
        }
        Set<Effect> fx = EnumSet.noneOf(Effect.class);
        JsonElement list = o.get("effects");
        if (list != null && list.isJsonArray()) {
            JsonArray a = list.getAsJsonArray();
            for (JsonElement x : a) {
                Effect ef = x.isJsonPrimitive() ? Effect.byName(x.getAsString()) : null;
                if (ef == null) throw new IllegalArgumentException(id + ": unknown effect " + x);
                fx.add(ef);
            }
        }
        double[] tp = o.has("tp") ? vec(o, "tp", 0) : null;
        return new GrandZoneRegion(id, str(o, "name", id), str(o, "dimension", "minecraft:overworld"), box, c[0], c[1], c[2], radius,
                mn[0], mn[1], mn[2], mx[0], mx[1], mx[2], themeByName(str(o, "theme", "wild")),
                Math.max(1.0, Math.min(3.0, num(o, "density", 2.0))), Math.max(0, num(o, "recommended_ep", 1_000_000)), fx,
                colour(str(o, "tint", "#8C6BFF")), tp, o.has("border") && o.get("border").isJsonPrimitive() && o.get("border").getAsBoolean());
    }

    /** Problems that make a zone unusable or silly (used by the data test and the self-test). */
    public List<String> problems() {
        List<String> p = new ArrayList<>();
        if (!has(Effect.CALM) && effects.isEmpty()) p.add(id + ": a zone with no effects does nothing");
        if (has(Effect.CALM) && (has(Effect.MAGMA_HEAT) || has(Effect.CHAOTIC_GRAVITY) || has(Effect.VIOLENT_CURRENTS) || has(Effect.PRESSURE)))
            p.add(id + ": a safe haven cannot also hurt");
        if (density < 1.0 || density > 3.0) p.add(id + ": density outside 1..3");
        if (recommendedEp < 1000) p.add(id + ": recommended EP below 1,000 is no zone");
        if (effectiveRadius() < 16) p.add(id + ": smaller than 16 blocks");
        return p;
    }

    // ---------------------------------------------------------------- json helpers
    private static String str(JsonObject o, String k, String d) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : d;
    }

    private static double num(JsonObject o, String k, double d) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return d;
        double v = e.getAsDouble();
        return Double.isNaN(v) || Double.isInfinite(v) ? d : v;
    }

    private static double[] vec(JsonObject o, String k, double d) {
        double[] v = {d, d, d};
        JsonElement e = o.get(k);
        if (e != null && e.isJsonArray() && e.getAsJsonArray().size() == 3) {
            for (int i = 0; i < 3; i++) {
                JsonElement x = e.getAsJsonArray().get(i);
                if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isNumber()) v[i] = x.getAsDouble();
            }
        }
        return v;
    }

    /** "#RRGGBB", "0xAARRGGBB" or "RRGGBB" to an opaque ARGB int. */
    public static int colour(String s) {
        try {
            String t = s.trim();
            if (t.startsWith("#")) t = t.substring(1);
            if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
            long v = Long.parseLong(t, 16);
            return t.length() > 6 ? (int) v : (int) (0xFF000000L | v);
        } catch (RuntimeException e) {
            return 0xFF8C6BFF;
        }
    }
}
