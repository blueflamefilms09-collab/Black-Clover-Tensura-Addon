package com.newuniverse.nusmp.client.upgrade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the effect spec of one upgraded weapon (assets/nusmp/visual_upgrade/&lt;id&gt;.json, written by tools/visual_upgrade/god_melee_a/specs.py): which skins its mesh uses, the passes drawn on
 * top of the skin (core glow, flowing veins, halo shell), its idle animations, its ambient effect layers, its swing trail, and one recipe of effect components per act. Pure data (Gson only,
 * no Minecraft types) so the numbers can be checked headlessly; {@link UpgradeSpecs} loads them, {@code WeaponUpgrade} draws the passes, {@link UpgradeFx} runs the layers and the
 * trail, {@code UpgradeSignatureLayer} draws the recipes.
 */
public final class UpgradeSpec {
    /** One ambient effect layer: what it spawns (a vanilla particle or a coloured dust), where, how fast. All distances in blocks, rates per second. */
    public record Layer(String type, String anchor, String particle, int rgb, float rate, float radius, float speed, float size, float tilt, float rise, float height, float from, float to) {}

    /** One component of an act recipe (a ring, a column, a burst ...); fields a type does not use stay 0 / null. */
    public record Comp(String t, String at, String from, String to, String tex, int rgb, float r, float r0, float r1, float h, float w, float len, float size, float spin, float y, int n, int turns, int dur, int delay) {}

    /** The effect of one act: its signature id (crc32 of "item:act") and its components. */
    public record Act(String act, int id, List<Comp> recipe, int maxDur) {}

    /** The swing trail: a pattern name, the particle, two colours, how many particles per tick, their size, how far the trail reaches (blocks). */
    public record Swing(String pattern, String particle, int rgb, int rgb2, int count, float size, float reach) {}

    public final String id;
    public final int version;
    public final String skin, glow, veins, lodMesh, lodSkin, lodGlow;
    public final float lodNear, lodGlowOff;
    public final List<String> passes, idle;
    public final int veinsRgb, haloRgb;
    public final float veinsSpeed, veinsWavelength, veinsMin, veinsMax, haloScale, haloAlphaMin, haloAlphaMax, haloSpeed, flickerAmount, flickerSpeed;
    public final List<Layer> ambient;
    public final float handLength, handTilt;
    public final Swing swing;
    public final List<Act> acts;
    public final String ambientSound;
    public final int primary, secondary;

    private final Set<String> idleSet;

    private UpgradeSpec(JsonObject o) {
        id = str(o, "id", "");
        version = o.has("version") ? o.get("version").getAsInt() : 1;
        JsonObject maps = obj(o, "maps");
        skin = str(maps, "skin", "");
        glow = str(maps, "glow", "");
        veins = str(maps, "veins", "");
        JsonObject lod = obj(o, "lod");
        lodMesh = str(lod, "mesh", "");
        lodSkin = str(lod, "skin", "");
        lodGlow = str(lod, "glow", "");
        lodNear = flt(lod, "near", 12f);
        lodGlowOff = flt(lod, "glow_off", 24f);
        passes = strings(o.get("passes"));
        idle = strings(o.get("idle"));
        idleSet = Collections.unmodifiableSet(new HashSet<>(idle));
        JsonObject pc = obj(o, "pass_cfg");
        JsonObject vc = obj(pc, "veins"), hc = obj(pc, "halo"), fc = obj(pc, "flicker");
        veinsRgb = rgb(vc.get("rgb"), 0xFFFFFF);
        veinsSpeed = flt(vc, "speed", 0.5f);
        veinsWavelength = flt(vc, "wavelength", 26f);
        veinsMin = flt(vc, "min", 0.25f);
        veinsMax = flt(vc, "max", 1f);
        haloRgb = rgb(hc.get("rgb"), 0xFFFFFF);
        haloScale = flt(hc, "scale", 1.045f);
        haloAlphaMin = flt(hc, "alpha_min", 18f);
        haloAlphaMax = flt(hc, "alpha_max", 70f);
        haloSpeed = flt(hc, "speed", 0.09f);
        flickerAmount = flt(fc, "amount", 0.12f);
        flickerSpeed = flt(fc, "speed", 0.7f);
        List<Layer> layers = new ArrayList<>();
        if (o.has("ambient")) {
            for (JsonElement e : o.getAsJsonArray("ambient")) {
                JsonObject l = e.getAsJsonObject();
                layers.add(new Layer(str(l, "type", "motes"), str(l, "anchor", "tip"), str(l, "particle", "dust"), rgb(l.get("rgb"), 0xFFFFFF), flt(l, "rate", 8f), flt(l, "radius", 0.5f),
                        flt(l, "speed", 0.2f), flt(l, "size", 0.5f), flt(l, "tilt", 0f), flt(l, "rise", 0.02f), flt(l, "height", 1.5f), flt(l, "from", 0f), flt(l, "to", 1f)));
            }
        }
        ambient = Collections.unmodifiableList(layers);
        JsonObject hand = obj(o, "hand");
        handLength = flt(hand, "length", 1.5f);
        handTilt = flt(hand, "tilt", 15f);
        if (o.has("swing") && o.get("swing").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("swing");
            swing = new Swing(str(s, "pattern", "arc"), str(s, "particle", "dust"), rgb(s.get("rgb"), 0xFFFFFF), rgb(s.get("rgb2"), 0xFFFFFF), (int) flt(s, "count", 3f), flt(s, "size", 0.6f), flt(s, "reach", 1.5f));
        } else {
            swing = null;
        }
        List<Act> list = new ArrayList<>();
        if (o.has("acts")) {
            for (JsonElement e : o.getAsJsonArray("acts")) {
                JsonObject a = e.getAsJsonObject();
                List<Comp> comps = new ArrayList<>();
                int maxDur = 0;
                if (a.has("recipe")) {
                    for (JsonElement ce : a.getAsJsonArray("recipe")) {
                        JsonObject c = ce.getAsJsonObject();
                        Comp comp = new Comp(str(c, "t", ""), str(c, "at", "caster"), str(c, "from", "caster"), str(c, "to", "to"), str(c, "tex", "glow"), rgb(c.get("rgb"), 0xFFFFFF), flt(c, "r", 0f),
                                flt(c, "r0", 0f), flt(c, "r1", 0f), flt(c, "h", 0f), flt(c, "w", 0.5f), flt(c, "len", 2f), flt(c, "size", 1f), flt(c, "spin", 0f), flt(c, "y", 0f),
                                (int) flt(c, "n", 0f), (int) flt(c, "turns", 1f), (int) flt(c, "dur", 20f), (int) flt(c, "delay", 0f));
                        comps.add(comp);
                        maxDur = Math.max(maxDur, comp.dur() + comp.delay());
                    }
                }
                list.add(new Act(str(a, "act", ""), a.has("id") ? a.get("id").getAsInt() : 0, Collections.unmodifiableList(comps), maxDur));
            }
        }
        acts = Collections.unmodifiableList(list);
        ambientSound = str(o, "ambient_sound", "");
        JsonObject pal = obj(o, "palette");
        primary = rgb(pal.get("primary"), 0xFFFFFF);
        secondary = rgb(pal.get("secondary"), 0xFFFFFF);
    }

    public static UpgradeSpec parse(JsonObject o) { return new UpgradeSpec(o); }

    public static UpgradeSpec parse(Reader r) { return new UpgradeSpec(JsonParser.parseReader(r).getAsJsonObject()); }

    /** Does the item play this idle animation ("glow_breath", "orbit_parts", "flow_veins", "halo_breath", "rune_flicker")? */
    public boolean idles(String name) { return idleSet.contains(name); }

    /** The act whose key is `act`, or null. */
    public Act actByKey(String act) {
        for (Act a : acts) if (a.act().equals(act)) return a;
        return null;
    }

    // ---------------------------------------------------------------- json helpers
    private static JsonObject obj(JsonObject o, String k) { return o != null && o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : new JsonObject(); }

    private static String str(JsonObject o, String k, String d) { return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d; }

    private static float flt(JsonObject o, String k, float d) { return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsFloat() : d; }

    private static int rgb(JsonElement e, int d) {
        if (e == null || !e.isJsonArray()) return d;
        JsonArray a = e.getAsJsonArray();
        if (a.size() < 3) return d;
        return (clamp(a.get(0).getAsInt()) << 16) | (clamp(a.get(1).getAsInt()) << 8) | clamp(a.get(2).getAsInt());
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e != null && e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) out.add(x.getAsString());
        return Collections.unmodifiableList(out);
    }
}
