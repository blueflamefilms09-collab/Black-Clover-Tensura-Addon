package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 0.53: parsed keyframe animations (the .animation.json files of Blockbench / GeckoLib, format 1.8.0): per bone rotation (degrees,
 * ADDED to the bone's own rotation), position (pixels, added) and scale (multiplied) tracks, linearly interpolated between keyframes.
 * Keyframe values may be [x, y, z], a single number, or {"pre"/"post": [x, y, z]} (post wins); numeric strings are read, other molang is 0.
 * Pure data (no game classes), see {@link GeoDraw} for how it is applied.
 */
public final class GeoAnim {
    public static final class Track {
        final float[] time;
        final float[][] value;

        Track(float[] time, float[][] value) { this.time = time; this.value = value; }

        /** The value at t seconds (clamped to the first / last keyframe). */
        public float[] sample(float t) {
            int n = time.length;
            if (n == 0) return null;
            if (n == 1 || t <= time[0]) return value[0];
            if (t >= time[n - 1]) return value[n - 1];
            int i = 0;
            while (i + 2 < n && t >= time[i + 1]) i++;
            float span = time[i + 1] - time[i], k = span <= 1e-6f ? 0f : (t - time[i]) / span;
            float[] a = value[i], b = value[i + 1];
            return new float[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k};
        }
    }

    public static final class BoneAnim {
        public Track rotation, position, scale;
    }

    public static final class Clip {
        public final String name;
        public float length;
        public boolean loop = true;
        public final Map<String, BoneAnim> bones = new HashMap<>();

        Clip(String name) { this.name = name; }

        /** Animation time for a clock in seconds: looped, or held on the last frame. */
        public float time(float seconds) {
            if (length <= 1e-6f) return 0f;
            return loop ? seconds % length : Math.min(seconds, length);
        }
    }

    private final Map<String, Clip> clips = new LinkedHashMap<>();

    public Clip clip(String name) {
        if (name == null) return null;
        Clip c = clips.get(name);
        if (c == null) c = clips.get("animation." + name);
        if (c == null) for (Map.Entry<String, Clip> e : clips.entrySet()) if (e.getKey().endsWith("." + name)) return e.getValue();
        return c;
    }

    public List<String> names() { return Collections.unmodifiableList(new ArrayList<>(clips.keySet())); }

    public static GeoAnim parse(JsonObject root) {
        GeoAnim a = new GeoAnim();
        if (!root.has("animations") || !root.get("animations").isJsonObject()) return a;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("animations").entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject co = e.getValue().getAsJsonObject();
            Clip c = new Clip(e.getKey());
            c.length = co.has("animation_length") ? co.get("animation_length").getAsFloat() : 0f;
            if (co.has("loop")) {
                JsonElement l = co.get("loop");
                c.loop = !(l.isJsonPrimitive() && (l.getAsJsonPrimitive().isBoolean() ? !l.getAsBoolean() : "hold_on_last_frame".equals(l.getAsString())));
            }
            if (co.has("bones") && co.get("bones").isJsonObject()) {
                for (Map.Entry<String, JsonElement> be : co.getAsJsonObject("bones").entrySet()) {
                    if (!be.getValue().isJsonObject()) continue;
                    JsonObject bo = be.getValue().getAsJsonObject();
                    BoneAnim ba = new BoneAnim();
                    ba.rotation = track(bo.get("rotation"), 0f);
                    ba.position = track(bo.get("position"), 0f);
                    ba.scale = track(bo.get("scale"), 1f);
                    c.bones.put(be.getKey(), ba);
                    if (c.length <= 0f) c.length = Math.max(c.length, lastTime(ba));
                }
            }
            a.clips.put(e.getKey(), c);
        }
        return a;
    }

    private static float lastTime(BoneAnim b) {
        float t = 0f;
        for (Track tr : new Track[]{b.rotation, b.position, b.scale}) if (tr != null && tr.time.length > 0) t = Math.max(t, tr.time[tr.time.length - 1]);
        return t;
    }

    private static Track track(JsonElement e, float fill) {
        if (e == null) return null;
        if (e.isJsonObject() && !isConstant(e.getAsJsonObject())) {
            List<float[]> keys = new ArrayList<>();                 // {time, x, y, z}
            for (Map.Entry<String, JsonElement> k : e.getAsJsonObject().entrySet()) {
                float t;
                try { t = Float.parseFloat(k.getKey()); } catch (NumberFormatException ex) { continue; }
                float[] v = value(k.getValue(), fill);
                keys.add(new float[]{t, v[0], v[1], v[2]});
            }
            if (keys.isEmpty()) return null;
            keys.sort((p, q) -> Float.compare(p[0], q[0]));
            float[] time = new float[keys.size()];
            float[][] val = new float[keys.size()][];
            for (int i = 0; i < time.length; i++) { time[i] = keys.get(i)[0]; val[i] = new float[]{keys.get(i)[1], keys.get(i)[2], keys.get(i)[3]}; }
            return new Track(time, val);
        }
        return new Track(new float[]{0f}, new float[][]{value(e, fill)});
    }

    private static boolean isConstant(JsonObject o) { return o.has("post") || o.has("pre") || o.has("vector"); }

    private static float[] value(JsonElement e, float fill) {
        if (e == null) return new float[]{fill, fill, fill};
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            float[] r = {fill, fill, fill};
            for (int i = 0; i < 3 && i < a.size(); i++) r[i] = num(a.get(i), fill);
            return r;
        }
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("post")) return value(o.get("post"), fill);
            if (o.has("pre")) return value(o.get("pre"), fill);
            if (o.has("vector")) return value(o.get("vector"), fill);
            return new float[]{fill, fill, fill};
        }
        float v = num(e, fill);
        return new float[]{v, v, v};
    }

    private static float num(JsonElement e, float def) {
        if (e == null || !e.isJsonPrimitive()) return def;
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isNumber()) return p.getAsFloat();
        try { return Float.parseFloat(p.getAsString().trim()); } catch (NumberFormatException ex) { return def; }
    }
}
