package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: how one character is drawn, as plain values in one record, so a look can be stored in a file, resolved by id, compared in a test and handed to the renderer without any game class: the
 * skin and whether its arms are slim, the glow map and its colour, the haircut and its colour, the extras (horns, wings, tails, a halo, a cape, floating weapons: GeoLite parts anchored on the
 * model's parts), the scale and the opacity. A skin or an extra is named, never loaded here; {@link SkinPath} and the client's resolver turn names into resources.
 *
 * <p>The kit's own convention for size: {@code scale} 1 draws the model at the vanilla player's own size (the player model drawn at 15/16, 1.875 blocks tall to the top of the head); use
 * {@link #scaleForHeight} to give a character a height in blocks.
 *
 * @param id       the id of the look (the key of the table; a character id, an entity id, optionally with a form or skin suffix)
 * @param skin     the skin name: a plain id ({@code asta}) or a path, see {@link SkinPath#resolve}; "" = the entity supplies it (an Avatar's composed skin)
 * @param slim     slim (3 pixel) arms
 * @param glow     the skin has a glow map ({@code <skin>_glow.png}, painted where the skin must shine; drawn additive and full bright)
 * @param glowRgb  the colour the glow map is tinted with (0xFFFFFF = as painted)
 * @param hair     the haircut ({@link HairStyleData} id, "" or "bald" = none)
 * @param hairRgb  the hair colour 0xRRGGBB
 * @param scale    see above
 * @param alpha    1 solid, less draws the body translucent
 * @param extras   GeoLite parts anchored on the body, in draw order
 * @param cape     a cape texture name (a plain id under the skin folder) or "" for none; drawn like the vanilla cape
 * @param holdR    radians the right arm is held raised forward (negative) while an extra is held in that hand, 0 = hangs
 * @param holdL    the same for the left arm
 */
public record PlayerLook(String id, String skin, boolean slim, boolean glow, int glowRgb, String hair, int hairRgb, float scale, float alpha, List<Extra> extras, String cape, float holdR, float holdL) {
    /** The height, in blocks, of the top of the head of a model at scale 1. */
    public static final float BASE_HEIGHT = 1.875f;
    public static final int WHITE = 0xFFFFFF;

    /** Where an extra is fixed on the model: it moves, turns and swings with that part. */
    public enum Anchor {
        HEAD("head"), BODY("body"), ARM_R("arm_r"), ARM_L("arm_l"), LEG_R("leg_r"), LEG_L("leg_l");

        /** The suffix of the root bone of an extras geo file that is drawn with this part: {@code x_<key>}. */
        public final String key;

        Anchor(String key) { this.key = key; }

        public static Anchor of(String s) {
            return switch (s == null ? "" : s.toLowerCase(Locale.ROOT)) {
                case "head" -> HEAD;
                case "arm_r", "right_arm" -> ARM_R;
                case "arm_l", "left_arm" -> ARM_L;
                case "leg_r", "right_leg" -> LEG_R;
                case "leg_l", "left_leg" -> LEG_L;
                default -> BODY;
            };
        }
    }

    /**
     * One GeoLite part set: {@code model} is the geo name ({@code geo/entity/<model>.geo.json}, animations {@code animations/entity/<model>.animation.json}), {@code texture} the texture name
     * under {@code textures/entity/} ("" = the model's own name), {@code tint} an ARGB multiplied into the part (0xFFFFFFFF = as painted), {@code glow} whether {@code <texture>_glow.png} is drawn
     * additive. Every root bone of the geo file is named {@code x_<anchor>} ({@code x_head}, {@code x_body}, {@code x_arm_r}, {@code x_arm_l}, {@code x_leg_r}, {@code x_leg_l}) and is drawn
     * with that part of the model.
     */
    public record Extra(String model, String texture, int tint, boolean glow) {
        public Extra {
            texture = texture == null || texture.isEmpty() ? model : texture;
        }

        public static Extra of(String model) { return new Extra(model, model, 0xFFFFFFFF, true); }
    }

    public PlayerLook {
        skin = skin == null ? "" : skin;
        hair = hair == null ? "" : hair;
        cape = cape == null ? "" : cape;
        scale = Float.isNaN(scale) || scale <= 0.05f ? 1f : Math.min(scale, 8f);
        alpha = Float.isNaN(alpha) ? 1f : Math.max(0f, Math.min(1f, alpha));
        extras = extras == null ? List.of() : List.copyOf(extras);
        holdR = Float.isNaN(holdR) ? 0f : Math.max(-2.5f, Math.min(1f, holdR));
        holdL = Float.isNaN(holdL) ? 0f : Math.max(-2.5f, Math.min(1f, holdL));
    }

    /** The scale that gives a character this height in blocks (the top of its head). */
    public static float scaleForHeight(float blocks) { return blocks / BASE_HEIGHT; }

    public boolean hasHair() { return !hair.isEmpty() && !hair.equals("bald"); }

    public PlayerLook withSkin(String s) { return new PlayerLook(id, s, slim, glow, glowRgb, hair, hairRgb, scale, alpha, extras, cape, holdR, holdL); }

    public PlayerLook withHair(String style, int rgb) { return new PlayerLook(id, skin, slim, glow, glowRgb, style, rgb, scale, alpha, extras, cape, holdR, holdL); }

    public PlayerLook withScale(float s) { return new PlayerLook(id, skin, slim, glow, glowRgb, hair, hairRgb, s, alpha, extras, cape, holdR, holdL); }

    public PlayerLook withAlpha(float a) { return new PlayerLook(id, skin, slim, glow, glowRgb, hair, hairRgb, scale, a, extras, cape, holdR, holdL); }

    public PlayerLook withGlowRgb(int rgb) { return new PlayerLook(id, skin, slim, glow, rgb, hair, hairRgb, scale, alpha, extras, cape, holdR, holdL); }

    public PlayerLook withExtras(List<Extra> e) { return new PlayerLook(id, skin, slim, glow, glowRgb, hair, hairRgb, scale, alpha, e, cape, holdR, holdL); }

    // ---------------------------------------------------------------- the table file
    /** Reads one look of {@code looks.json}. Unknown keys are ignored; a missing key takes its default. */
    public static PlayerLook parse(String id, JsonObject o) {
        List<Extra> extras = new ArrayList<>();
        if (o.has("extras")) {
            for (JsonElement e : o.getAsJsonArray("extras")) {
                if (e.isJsonPrimitive()) {
                    extras.add(Extra.of(e.getAsString()));
                } else {
                    JsonObject x = e.getAsJsonObject();
                    int tint = 0xFFFFFFFF;
                    if (x.has("tint")) tint = (int) Long.parseLong(x.get("tint").getAsString().replace("#", "").replace("0x", ""), 16);
                    extras.add(new Extra(x.get("model").getAsString(), x.has("texture") ? x.get("texture").getAsString() : "", tint, !x.has("glow") || x.get("glow").getAsBoolean()));
                }
            }
        }
        return new PlayerLook(id, str(o, "skin", ""), bool(o, "slim", false), bool(o, "glow", false), rgb(o, "glow_color", WHITE), str(o, "hair", ""), rgb(o, "hair_color", 0x3A281C),
                o.has("scale") ? o.get("scale").getAsFloat() : 1f, o.has("alpha") ? o.get("alpha").getAsFloat() : 1f, extras, str(o, "cape", ""),
                o.has("hold_r") ? o.get("hold_r").getAsFloat() : 0f, o.has("hold_l") ? o.get("hold_l").getAsFloat() : 0f);
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    private static boolean bool(JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }

    private static int rgb(JsonObject o, String k, int d) {
        if (!o.has(k)) return d;
        JsonElement e = o.get(k);
        if (e.getAsJsonPrimitive().isNumber()) return e.getAsInt() & 0xFFFFFF;
        return HairPalette.parse(e.getAsString(), d);
    }

    /** Every look of a {@code looks.json} document ({@code {"looks": {id: {...}}}}), in file order. */
    public static Map<String, PlayerLook> parseTable(JsonObject root) {
        Map<String, PlayerLook> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("looks").entrySet()) out.put(e.getKey(), parse(e.getKey(), e.getValue().getAsJsonObject()));
        return Collections.unmodifiableMap(out);
    }
}
