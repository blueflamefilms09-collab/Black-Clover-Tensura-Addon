package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Why: every custom particle of the library behaves differently (an ember rises and flickers, a leaf tumbles, a ring just widens), but they all run through ONE particle class, so the
 * difference lives in this immutable record: gravity, drag, spin, swirl, sway, jitter, the size and alpha curves, the lifetime range, whether it glows (full-bright) and blends additively,
 * and how its animation frames advance. The numbers are authored next to the textures in {@code tools/particles} and shipped as {@code data/nusmp/particle_catalog.json}; this class only
 * reads them. Pure (no game classes), so the parsing and the clamps are unit-tested. A record is created once per particle type, never per particle.
 *
 * <ul>
 *   <li>{@code gravity}: positive falls, negative rises; the velocity changes by {@code 0.04 * gravity} per tick.</li>
 *   <li>{@code drag}: the velocity is multiplied by this each tick (0.9 stops fast, 0.99 glides).</li>
 *   <li>{@code spin}: roll of the quad in radians per tick. {@code swirl}: radians per tick the horizontal velocity turns. {@code sway}: sinusoidal sideways push, blocks per tick.</li>
 *   <li>{@code sizeStart}/{@code sizeEnd}: full width of the quad in blocks at birth and death; {@link Curve} shapes the way between them.</li>
 *   <li>{@code fadeIn}/{@code fadeOut}: the fractions of the life over which the alpha rises from 0 and falls to 0.</li>
 * </ul>
 */
public record FxProfile(float gravity, float drag, float spin, float swirl, float sway, float jitter, float sizeStart, float sizeEnd, Curve curve,
                        float fadeIn, float fadeOut, int lifeMin, int lifeMax, Light light, Blend blend, Anim anim, int loopTicks, boolean collide) {

    public enum Curve { LINEAR, POP, EASE_OUT, PULSE, HOLD }

    public enum Light { NORMAL, FULL }

    public enum Blend { TRANSLUCENT, ADD }

    public enum Anim { AGE, LOOP, RANDOM }

    /** The hardest limits: whatever the file says, a particle never lives longer than this many ticks nor grows past this width in blocks. */
    public static final int MAX_LIFE = 240;
    public static final float MAX_SIZE = 8.0f;

    public static final FxProfile DEFAULT = new FxProfile(0f, 0.96f, 0f, 0f, 0f, 0f, 0.2f, 0.1f, Curve.LINEAR, 0.1f, 0.4f, 16, 28, Light.FULL, Blend.ADD, Anim.LOOP, 3, false);

    public FxProfile {
        drag = clamp(drag, 0.5f, 1.0f);
        sizeStart = clamp(sizeStart, 0f, MAX_SIZE);
        sizeEnd = clamp(sizeEnd, 0f, MAX_SIZE);
        fadeIn = clamp(fadeIn, 0f, 1f);
        fadeOut = clamp(fadeOut, 0f, 1f);
        lifeMin = Math.max(1, Math.min(MAX_LIFE, lifeMin));
        lifeMax = Math.max(lifeMin, Math.min(MAX_LIFE, lifeMax));
        loopTicks = Math.max(1, loopTicks);
        if (curve == null) curve = Curve.LINEAR;
        if (light == null) light = Light.NORMAL;
        if (blend == null) blend = Blend.TRANSLUCENT;
        if (anim == null) anim = Anim.LOOP;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Reads the {@code "profile"} object of a catalog entry; every missing key keeps the default, a wrong key type or word keeps the default too. */
    public static FxProfile fromJson(JsonObject o) {
        if (o == null) return DEFAULT;
        FxProfile d = DEFAULT;
        float[] size = pair(o.get("size"), d.sizeStart, d.sizeEnd);
        float[] fade = pair(o.get("fade"), d.fadeIn, d.fadeOut);
        float[] life = pair(o.get("life"), d.lifeMin, d.lifeMax);
        return new FxProfile(num(o, "gravity", d.gravity), num(o, "drag", d.drag), num(o, "spin", d.spin), num(o, "swirl", d.swirl), num(o, "sway", d.sway), num(o, "jitter", d.jitter),
                size[0], size[1], word(o, "curve", Curve.values(), d.curve), fade[0], fade[1], Math.round(life[0]), Math.round(life[1]),
                word(o, "light", Light.values(), d.light), word(o, "blend", Blend.values(), d.blend),
                word(o, "anim", Anim.values(), d.anim), Math.round(num(o, "loop", d.loopTicks)), o.has("collide") && o.get("collide").isJsonPrimitive() && o.get("collide").getAsBoolean());
    }

    private static float num(JsonObject o, String key, float fallback) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return fallback;
        return e.getAsFloat();
    }

    private static float[] pair(JsonElement e, float a, float b) {
        if (e != null && e.isJsonArray()) {
            JsonArray arr = e.getAsJsonArray();
            if (arr.size() == 2 && arr.get(0).isJsonPrimitive() && arr.get(1).isJsonPrimitive()) return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()};
        }
        return new float[]{a, b};
    }

    private static <E extends Enum<E>> E word(JsonObject o, String key, E[] all, E fallback) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return fallback;
        String s = e.getAsString().trim().toUpperCase(Locale.ROOT);
        for (E v : all) if (v.name().equals(s)) return v;
        return fallback;
    }
}
