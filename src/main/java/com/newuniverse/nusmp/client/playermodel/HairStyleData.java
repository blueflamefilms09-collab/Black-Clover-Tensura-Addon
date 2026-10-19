package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the haircuts of the kit are data (assets/nusmp/playermodel/hair_styles.json, written with their painted sheets by tools/playermodel/gen_hair.py), and the Java side has to read them without
 * a game class so the table can be tested (every style resolves, every box has a UV region inside its 64x64 sheet, every chain has 1 to 4 segments, the cap sits on the hat layer's UV).
 * Coordinates are the player model's own, in pixels: y DOWN, the head box spans x -4..4, y -8..0, z -4..4 and the neck line is y 0; a box with a {@code part} of {@code head} or {@code body} is
 * posed with that part (both pivots are at the neck, so one set of numbers serves both).
 *
 * <p>A {@link Box} is a static piece (the scalp cap, a spike, a bun). A {@link Chain} is a lock that hangs from {@code root} in the space of its first segment's part: segment {@code i} hangs from
 * the end of segment {@code i - 1}, turned by the spring angles, and a chain may change part (head to body) so its lower end follows the neck and back instead of the head's nod.
 */
public final class HairStyleData {
    private HairStyleData() {}

    /** The side of the square sheet every style uses (a skin's size). */
    public static final int SHEET = 64;
    /** The scalp cap sits where a skin's hat layer is. */
    public static final int CAP_U = 32, CAP_V = 0;

    /** What a piece does under a helmet: {@code HIDE} it is not drawn, {@code KEEP} it is drawn, {@code TAIL} (chains) only the segments that end below the helmet's lower edge are drawn. */
    public enum Helm {
        HIDE, KEEP, TAIL;

        static Helm of(String s, Helm fallback) {
            if (s == null) return fallback;
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "hide" -> HIDE;
                case "keep" -> KEEP;
                case "tail" -> TAIL;
                default -> fallback;
            };
        }
    }

    /** The part a piece hangs from. */
    public enum Part { HEAD, BODY }

    /** A static box: its min corner {@code from}, extent {@code size}, {@code inflate} on every side, UV origin and UV box size, tone (see {@link HairPalette.Tint#tone}), optional rotation about {@code pivot}. */
    public record Box(String name, Part part, float[] from, float[] size, float inflate, int u, int v, int[] uvSize, int tone, float[] rot, float[] pivot, Helm helm) {
        public boolean rotated() { return rot != null; }
    }

    /** One segment of a chain: a box of {@code size} hanging from its top-centre pivot, inflated, with its UV and tone, hung from {@code part}. */
    public record Segment(Part part, float[] size, float inflate, int u, int v, int[] uvSize, int tone) {}

    /** A hanging lock. {@code tuning} is {@code head} (reacts to the head), {@code body} or {@code stiff}; {@code params} is {@code default}, {@code stiff} or {@code heavy}. */
    public record Chain(String name, String tuning, String params, float phase, float[] root, Helm helm, List<Segment> segments) {
        public float length() {
            float t = 0f;
            for (Segment s : segments) t += s.size()[1];
            return t;
        }
    }

    public record Style(String id, String texture, List<Box> boxes, List<Chain> chains) {
        public boolean bald() { return boxes.isEmpty() && chains.isEmpty(); }
    }

    /** Every style of a hair_styles.json, in file order. */
    public static Map<String, Style> parse(JsonObject root) {
        Map<String, Style> out = new LinkedHashMap<>();
        JsonObject styles = root.getAsJsonObject("styles");
        for (Map.Entry<String, JsonElement> e : styles.entrySet()) out.put(e.getKey(), parseStyle(e.getKey(), e.getValue().getAsJsonObject()));
        return Collections.unmodifiableMap(out);
    }

    private static Style parseStyle(String id, JsonObject o) {
        List<Box> boxes = new ArrayList<>();
        if (o.has("boxes")) for (JsonElement b : o.getAsJsonArray("boxes")) boxes.add(parseBox(b.getAsJsonObject()));
        List<Chain> chains = new ArrayList<>();
        if (o.has("chains")) for (JsonElement c : o.getAsJsonArray("chains")) chains.add(parseChain(c.getAsJsonObject()));
        String tex = o.has("texture") ? o.get("texture").getAsString() : "hair/" + id;
        return new Style(id, tex, List.copyOf(boxes), List.copyOf(chains));
    }

    private static Box parseBox(JsonObject o) {
        float[] size = floats(o, "size", 3);
        int[] uvSize = o.has("uvsize") ? ints(o.getAsJsonArray("uvsize")) : roundAll(size);
        int[] uv = ints(o.getAsJsonArray("uv"));
        return new Box(str(o, "name", "box"), part(o), floats(o, "from", 3), size, o.has("inflate") ? o.get("inflate").getAsFloat() : 0f, uv[0], uv[1], uvSize,
                o.has("tone") ? o.get("tone").getAsInt() : 0, o.has("rot") ? floats(o, "rot", 3) : null, o.has("pivot") ? floats(o, "pivot", 3) : null,
                Helm.of(str(o, "helm", "hide"), Helm.HIDE));
    }

    private static Chain parseChain(JsonObject o) {
        List<Segment> segs = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("segments")) {
            JsonObject s = e.getAsJsonObject();
            float[] size = floats(s, "size", 3);
            int[] uv = ints(s.getAsJsonArray("uv"));
            segs.add(new Segment(part(s), size, s.has("inflate") ? s.get("inflate").getAsFloat() : 0f, uv[0], uv[1], s.has("uvsize") ? ints(s.getAsJsonArray("uvsize")) : roundAll(size),
                    s.has("tone") ? s.get("tone").getAsInt() : 0));
        }
        return new Chain(str(o, "name", "chain"), str(o, "tuning", "head"), str(o, "params", "default"), o.has("phase") ? o.get("phase").getAsFloat() : 0f, floats(o, "root", 3),
                Helm.of(str(o, "helm", "tail"), Helm.TAIL), List.copyOf(segs));
    }

    private static Part part(JsonObject o) { return "body".equalsIgnoreCase(str(o, "part", "head")) ? Part.BODY : Part.HEAD; }

    private static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    private static float[] floats(JsonObject o, String k, int n) {
        JsonArray a = o.getAsJsonArray(k);
        if (a == null || a.size() < n) throw new IllegalArgumentException("hair: '" + k + "' needs " + n + " numbers");
        float[] f = new float[n];
        for (int i = 0; i < n; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    private static int[] ints(JsonArray a) {
        int[] r = new int[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = Math.round(a.get(i).getAsFloat());
        return r;
    }

    private static int[] roundAll(float[] f) {
        int[] r = new int[f.length];
        for (int i = 0; i < r.length; i++) r[i] = Math.max(1, Math.round(f[i]));
        return r;
    }

    // ---------------------------------------------------------------- helmet rule (pure, tested)
    /** The lower edge, in head-space pixels, of a helmet's outer layer: a 10 x 10 x 10 box round the head reaches y = 1. */
    public static final float HELMET_BOTTOM = 1.2f;

    /** Is a static box drawn under a helmet? */
    public static boolean boxShownUnderHelmet(Box b) { return b.helm() == Helm.KEEP; }

    /**
     * Is segment {@code index} of a chain drawn under a helmet? A {@code TAIL} chain draws the segments whose lower end is under the helmet's lower edge (the part of the lock that hangs out
     * of it); {@code KEEP} draws all; {@code HIDE} none. The nominal (unswung) geometry decides, so a lock does not flicker as it swings.
     */
    public static boolean segmentShownUnderHelmet(Chain c, int index) {
        if (c.helm() == Helm.KEEP) return true;
        if (c.helm() == Helm.HIDE) return false;
        float y = c.root()[1];
        for (int i = 0; i < index; i++) y += c.segments().get(i).size()[1];
        return y + c.segments().get(index).size()[1] > HELMET_BOTTOM;
    }

    /** The nominal pivot of segment {@code index} in its part's space: the root plus the lengths of the segments above it. */
    public static float[] pivotOf(Chain c, int index) {
        float y = c.root()[1];
        for (int i = 0; i < index; i++) y += c.segments().get(i).size()[1];
        return new float[]{c.root()[0], y, c.root()[2]};
    }
}
