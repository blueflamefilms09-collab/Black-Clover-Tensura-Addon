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
 * Why: the redesigned Black Clover looks (docs/bc_playermodels.md) are built by a generator and nobody can see them in a game from here, so what a machine can prove is proved here, from the jar
 * the same way the client reads it: every old look that has a new one still exists (nothing was removed) and so does the new one; each new look has the 256 skin, no kit hair (the cube hair replaces it),
 * a model for every extras entry whose cubes all have their texture rectangles inside the atlas, and stays inside the cost budget written in the doc (cubes and quads of the new parts). No game
 * class, any opener ({@link PlayerModelAudit#classpath} for the jar and the CI self-test {@code bcmodel.looks}, a file for the unit test).
 */
public final class BcAudit {
    private BcAudit() {}

    /** Worst case of the NEW parts of one look (the old extras are not counted): quads drawn at full detail. */
    public static final int MAX_QUADS = 700, MAX_FX_QUADS = 120, MAX_ATLAS = 512;

    public static List<String> run(Function<String, InputStream> open) {
        List<String> bad = new ArrayList<>();
        PlayerLookTables.Merged merged = PlayerLookTables.load(open);
        Map<String, PlayerLook> looks = merged.looks();
        if (BcLooks.oldIds().isEmpty()) bad.add("BcLooks has no entries");
        for (String old : BcLooks.allOldIds()) {
            String id = BcLooks.of(old);
            if (!looks.containsKey(old)) bad.add("the old look '" + old + "' is gone: nothing is removed (rule 2)");
            PlayerLook l = looks.get(id);
            if (l == null) {
                bad.add("BcLooks maps " + old + " to " + id + " but looks_bc.json has no such look");
                continue;
            }
            if (BcLooks.theme(id) == null) bad.add(id + " has no shimmer theme in BcLooks");
            if (l.hasHair()) bad.add(id + " still has a kit hair style (" + l.hair() + "): the cube hair of its extras replaces it");
            if (!l.skin().equals(id)) bad.add(id + ": its skin is '" + l.skin() + "', expected the skin of the same name");
            String path = SkinPath.resolve(l.skin());
            try (InputStream in = open.apply(path)) {
                java.awt.image.BufferedImage img = in == null ? null : ImageIO.read(in);
                if (img == null) bad.add(id + ": " + path + " is missing");
                else if (img.getWidth() != 256 || img.getHeight() != 256) bad.add(id + ": the skin is " + img.getWidth() + "x" + img.getHeight() + ", not 256x256");
            } catch (Exception e) {
                bad.add(id + ": " + path + ": " + e);
            }
            int quads = 0, fxQuads = 0, models = 0;
            for (PlayerLook.Extra x : l.extras()) {
                if (!x.model().startsWith("pmx_bc_")) continue;
                models++;
                int[] q = geoProblems(id, x, open, bad);
                quads += q[0];
                if (x.model().endsWith("_fx")) fxQuads += q[0];
                if (x.model().endsWith("_fx") && q[1] > 0 && !x.glow()) bad.add(id + ": the fx model has a glow map but not the glow flag");
                if (!x.model().endsWith("_fx") && !x.model().contains("_keep") && x.glow()) bad.add(id + ": only the fx model glows (the others would draw a second additive pass for nothing; a kept old prop model may)");
            }
            if (models == 0) bad.add(id + " has no pmx_bc_ extras model");
            if (quads > MAX_QUADS) bad.add(id + ": " + quads + " quads of new parts, over the budget of " + MAX_QUADS);
            if (fxQuads > MAX_FX_QUADS) bad.add(id + ": " + fxQuads + " glowing quads, over the budget of " + MAX_FX_QUADS);
        }
        return bad;
    }

    /** {quads, 1 when the texture has a glow map} of one extras model, problems appended. */
    private static int[] geoProblems(String id, PlayerLook.Extra x, Function<String, InputStream> open, List<String> bad) {
        int quads = 0;
        String geo = "geo/entity/" + x.model() + ".geo.json";
        String tex = SkinPath.extraTexture(x.texture());
        int tw = 0, th = 0;
        try (InputStream in = open.apply(tex)) {
            java.awt.image.BufferedImage img = in == null ? null : ImageIO.read(in);
            if (img == null) bad.add(id + ": " + tex + " is missing");
            else {
                tw = img.getWidth();
                th = img.getHeight();
                if (tw > MAX_ATLAS || th > MAX_ATLAS) bad.add(id + ": the atlas " + tex + " is " + tw + "x" + th + ", over " + MAX_ATLAS);
            }
        } catch (Exception e) {
            bad.add(id + ": " + tex + ": " + e);
        }
        boolean glowMap = false;
        try (InputStream in = open.apply(SkinPath.extraTexture(x.texture()).replace(".png", "_glow.png"))) {
            glowMap = in != null;
        } catch (Exception ignored) {
            // a missing glow map is reported through the flag check below
        }
        try (InputStream in = open.apply(geo)) {
            if (in == null) {
                bad.add(id + ": " + geo + " is missing");
                return new int[]{0, 0};
            }
            JsonObject g = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject d = g.getAsJsonObject("description");
            int gw = d.get("texture_width").getAsInt(), gh = d.get("texture_height").getAsInt();
            if (tw > 0 && (gw != tw || gh != th)) bad.add(id + ": " + x.model() + " says its texture is " + gw + "x" + gh + " but " + tex + " is " + tw + "x" + th);
            for (JsonElement be : g.getAsJsonArray("bones")) {
                JsonObject b = be.getAsJsonObject();
                if (!b.has("cubes")) continue;
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    JsonObject c = ce.getAsJsonObject();
                    if (!c.has("uv") || !c.get("uv").isJsonObject()) {
                        bad.add(id + ": " + x.model() + " bone " + b.get("name").getAsString() + " has a cube without per-face uv");
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> f : c.getAsJsonObject("uv").entrySet()) {
                        quads++;
                        JsonObject fo = f.getValue().getAsJsonObject();
                        JsonArray uv = fo.getAsJsonArray("uv"), sz = fo.getAsJsonArray("uv_size");
                        float u = uv.get(0).getAsFloat(), v = uv.get(1).getAsFloat(), w = sz.get(0).getAsFloat(), h = sz.get(1).getAsFloat();
                        if (u < 0 || v < 0 || u + w > gw + 1e-3 || v + h > gh + 1e-3) bad.add(id + ": " + x.model() + " has a face rectangle outside the atlas (" + u + "," + v + " " + w + "x" + h + ")");
                    }
                }
            }
        } catch (Exception e) {
            bad.add(id + ": " + geo + ": " + e);
        }
        return new int[]{quads, glowMap ? 1 : 0};
    }
}
