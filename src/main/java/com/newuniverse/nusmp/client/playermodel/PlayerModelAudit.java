package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why: a look that names a skin that is not in the jar, a hair style with a box outside its 64x64 sheet, an extras model with no {@code x_<anchor>} root, or an arc boss with no look is drawn as
 * nothing (or with the default look) and nobody notices until a boss walks out wrong. This reads the kit's data files through any opener (the classpath in the unit test and in the CI server's
 * self-test {@code render.playermodel}, the resources folder in a tool) and returns every problem it finds, with no game class. Opener: a path under {@code assets/nusmp/} to a stream, or null
 * when the file does not exist.
 */
public final class PlayerModelAudit {
    private PlayerModelAudit() {}

    /** The classpath opener: {@code assets/nusmp/<path>} from the jar. */
    public static InputStream classpath(String path) { return PlayerModelAudit.class.getResourceAsStream("/assets/nusmp/" + path); }

    public static List<String> run(Function<String, InputStream> open) {
        List<String> bad = new ArrayList<>();
        Map<String, HairStyleData.Style> hair = null;
        try (InputStream in = open.apply("playermodel/hair_styles.json")) {
            if (in == null) bad.add("playermodel/hair_styles.json is missing");
            else hair = HairStyleData.parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception e) {
            bad.add("hair_styles.json: " + e);
        }
        if (hair != null) hairProblems(hair, open, bad);
        PlayerLookTables.Merged merged = PlayerLookTables.load(open);          // 1.99.0: looks.json, looks_abyssal.json and looks_dungeon.json, merged add-only
        bad.addAll(merged.problems());
        Map<String, PlayerLook> looks = merged.looks();
        lookProblems(looks, hair, open, bad);
        if (hair != null) {
            for (int i = 0; i < LegacyHair.COUNT; i++) {
                LegacyHair.Cut c = LegacyHair.of(i);
                if (!hair.containsKey(c.style())) bad.add("legacy hair id " + i + " maps to the unknown style " + c.style());
            }
        }
        return bad;
    }

    private static void hairProblems(Map<String, HairStyleData.Style> hair, Function<String, InputStream> open, List<String> bad) {
        for (HairStyleData.Style s : hair.values()) {
            String sheet = SkinPath.hairSheet(s.texture());
            String why = png(open, sheet, 64, 64);
            if (why != null) bad.add("hair " + s.id() + ": " + why);
            boolean hasCap = false;
            for (HairStyleData.Box b : s.boxes()) {
                int[] uv = b.uvSize();
                if (b.u() < 0 || b.v() < 0 || b.u() + 2 * (uv[0] + uv[2]) > HairStyleData.SHEET || b.v() + uv[2] + uv[1] > HairStyleData.SHEET)
                    bad.add("hair " + s.id() + " box " + b.name() + ": its UV box is outside the 64x64 sheet");
                if (b.name().equals("cap")) {
                    hasCap = true;
                    if (b.u() != HairStyleData.CAP_U || b.v() != HairStyleData.CAP_V) bad.add("hair " + s.id() + ": the cap is not on the hat layer's UV");
                    if (b.inflate() <= 0.55f) bad.add("hair " + s.id() + ": the cap is not larger than the hat layer (inflate " + b.inflate() + ")");
                }
            }
            if (!hasCap && !s.bald() && s.chains().isEmpty() && s.boxes().isEmpty()) bad.add("hair " + s.id() + ": nothing to draw");
            for (HairStyleData.Chain c : s.chains()) {
                int n = c.segments().size();
                if (n < 1 || n > 4) bad.add("hair " + s.id() + " chain " + c.name() + ": " + n + " segments (1 to 4)");
                for (HairStyleData.Segment g : c.segments()) {
                    int[] uv = g.uvSize();
                    if (g.u() < 0 || g.v() < 0 || g.u() + 2 * (uv[0] + uv[2]) > HairStyleData.SHEET || g.v() + uv[2] + uv[1] > HairStyleData.SHEET)
                        bad.add("hair " + s.id() + " chain " + c.name() + ": a segment's UV box is outside the sheet");
                }
            }
        }
    }

    private static void lookProblems(Map<String, PlayerLook> looks, Map<String, HairStyleData.Style> hair, Function<String, InputStream> open, List<String> bad) {
        for (PlayerLook l : looks.values()) {
            if (!l.skin().isEmpty()) {
                String path = SkinPath.resolve(l.skin());
                if (path.isEmpty()) bad.add("look " + l.id() + ": the skin name '" + l.skin() + "' is not valid");
                else {
                    int[] size = {SkinPath.BASE_SIZE};
                    String why = skinPng(open, path, size);
                    if (why != null) bad.add("look " + l.id() + ": " + why);
                    if (l.glow()) {
                        String g = png(open, SkinPath.glowOf(path), size[0], size[0]);              // the glow map has the size of its skin
                        if (g != null) bad.add("look " + l.id() + ": glow map: " + g);
                    }
                }
            }
            if (l.hasHair() && hair != null && !hair.containsKey(l.hair())) bad.add("look " + l.id() + ": the hair style '" + l.hair() + "' does not exist");
            for (PlayerLook.Extra x : l.extras()) {
                String geo = "geo/entity/" + x.model() + ".geo.json";
                try (InputStream in = open.apply(geo)) {
                    if (in == null) {
                        bad.add("look " + l.id() + ": the extras model " + x.model() + " is missing");
                        continue;
                    }
                    JsonObject g = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonArray bones = g.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
                    boolean root = false;
                    for (JsonElement e : bones) {
                        JsonObject b = e.getAsJsonObject();
                        if ((!b.has("parent") || b.get("parent").isJsonNull()) && b.get("name").getAsString().startsWith("x_")) root = true;
                    }
                    if (!root) bad.add("look " + l.id() + ": " + x.model() + " has no x_<anchor> root bone");
                } catch (Exception e) {
                    bad.add("look " + l.id() + ": " + geo + ": " + e);
                }
                String tex = SkinPath.extraTexture(x.texture());
                if (tex.isEmpty() || open.apply(tex) == null) bad.add("look " + l.id() + ": the extras texture " + x.texture() + " is missing");
            }
        }
        for (String id : PlayerModelTable.FIXED_LOOKS) if (!looks.containsKey(id)) bad.add("the look '" + id + "' that a body asks for by name is not in looks.json");
        for (String id : PlayerModelTable.ARC_BOSSES) if (!looks.containsKey(id)) bad.add("the arc boss " + id + " has no look");
    }

    /**
     * 3.38.0 (HD outfits): null when the skin exists and is square with a side that is a power-of-two multiple of 64 ({@link SkinPath#validSkinSize}: 64, 128, 256 ...), else why not.
     * {@code size[0]} receives the width when the file could be read (so the glow map can be checked against it), otherwise it keeps 64.
     */
    private static String skinPng(Function<String, InputStream> open, String path, int[] size) {
        try (InputStream in = open.apply(path)) {
            if (in == null) return path + " is missing";
            java.awt.image.BufferedImage img = ImageIO.read(in);
            if (img == null) return path + " is not a png";
            if (!SkinPath.validSkinSize(img.getWidth(), img.getHeight())) return path + " is " + img.getWidth() + "x" + img.getHeight() + ", not a square power-of-two multiple of 64 (64, 128, 256 ...)";
            size[0] = img.getWidth();
            return null;
        } catch (Exception e) {
            return path + ": " + e;
        }
    }

    /** null when the png exists and has this size, else why not. */
    private static String png(Function<String, InputStream> open, String path, int w, int h) {
        try (InputStream in = open.apply(path)) {
            if (in == null) return path + " is missing";
            java.awt.image.BufferedImage img = ImageIO.read(in);
            if (img == null) return path + " is not a png";
            if (img.getWidth() != w || img.getHeight() != h) return path + " is " + img.getWidth() + "x" + img.getHeight() + ", not " + w + "x" + h;
            return null;
        } catch (Exception e) {
            return path + ": " + e;
        }
    }
}
