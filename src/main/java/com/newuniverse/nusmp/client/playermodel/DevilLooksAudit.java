package com.newuniverse.nusmp.client.playermodel;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why: the devil looks (docs/devil_looks.md) are built by a generator and nobody can see them in a game from here, so what a machine can prove is proved here, from the jar the same way the client
 * reads it: every devil of the roster has a look (the ids come in as an argument so this stays free of game classes), the look has the 256 skin (and its glow map when it says it glows), no kit hair
 * (the cube hair of its extras replaces it), a model for every extras entry whose face rectangles all lie inside their atlas ({@link BcAudit#geoProblems}), only the {@code _fx} model glows, the scale keeps
 * the head in proportion, and the cost of the extras stays inside the budget written in the doc. Any opener ({@link PlayerModelAudit#classpath} for the jar and the CI self-test, a file for the unit test).
 */
public final class DevilLooksAudit {
    private DevilLooksAudit() {}

    /** Worst case of the parts of one look: quads at full detail (hair and fine layers included); glowing quads; the scale of the body (the head stays about 1.1x vanilla). */
    public static final int MAX_QUADS = 400, MAX_FX_QUADS = 60;
    public static final float MAX_SCALE = 1.15f, MIN_SCALE = 0.85f;

    public static List<String> run(Function<String, InputStream> open, List<String> devilIds) {
        List<String> bad = new ArrayList<>();
        Map<String, PlayerLook> looks = PlayerLookTables.load(open).looks();
        if (devilIds.isEmpty()) bad.add("no devil ids given");
        for (String devil : devilIds) {
            String id = DevilLooks.of(devil);
            if (id == null) {
                bad.add("DevilLooks has no look for the devil " + devil);
                continue;
            }
            PlayerLook l = looks.get(id);
            if (l == null) {
                bad.add("DevilLooks maps " + devil + " to " + id + " but looks_dv.json has no such look");
                continue;
            }
            if (l.hasHair()) bad.add(id + " still has a kit hair style (" + l.hair() + "): the cube hair of its extras replaces it");
            if (!l.skin().equals(id)) bad.add(id + ": its skin is '" + l.skin() + "', expected the skin of the same name");
            if (l.scale() > MAX_SCALE || l.scale() < MIN_SCALE) bad.add(id + ": scale " + l.scale() + " is outside " + MIN_SCALE + ".." + MAX_SCALE);
            String path = SkinPath.resolve(l.skin());
            check(open, path, id, "the skin", bad);
            if (l.glow()) check(open, SkinPath.glowOf(path), id, "the skin's glow map", bad);
            int quads = 0, fxQuads = 0, models = 0;
            for (PlayerLook.Extra x : l.extras()) {
                if (!x.model().startsWith("pmx_dv_")) {
                    bad.add(id + ": extras model " + x.model() + " is not a pmx_dv_ model");
                    continue;
                }
                models++;
                int[] q = BcAudit.geoProblems(id, x, open, bad);
                quads += q[0];
                if (x.model().endsWith("_fx")) fxQuads += q[0];
                if (x.model().endsWith("_fx") && q[1] > 0 && !x.glow()) bad.add(id + ": the fx model has a glow map but not the glow flag");
                if (!x.model().endsWith("_fx") && x.glow()) bad.add(id + ": only the fx model glows (the others would draw a second additive pass for nothing)");
            }
            if (models == 0) bad.add(id + " has no pmx_dv_ extras model");
            if (quads > MAX_QUADS) bad.add(id + ": " + quads + " quads of parts, over the budget of " + MAX_QUADS);
            if (fxQuads > MAX_FX_QUADS) bad.add(id + ": " + fxQuads + " glowing quads, over the budget of " + MAX_FX_QUADS);
        }
        for (String k : looks.keySet()) if (DevilLooks.isDevil(k) && !devilIds.contains(k.substring(DevilLooks.PREFIX.length()))) bad.add(k + " is a devil look of no devil in the roster");
        return bad;
    }

    /** A 256x256 png at {@code path}. */
    private static void check(Function<String, InputStream> open, String path, String id, String what, List<String> bad) {
        try (InputStream in = open.apply(path)) {
            java.awt.image.BufferedImage img = in == null ? null : ImageIO.read(in);
            if (img == null) bad.add(id + ": " + what + " " + path + " is missing");
            else if (img.getWidth() != 256 || img.getHeight() != 256) bad.add(id + ": " + what + " is " + img.getWidth() + "x" + img.getHeight() + ", not 256x256");
        } catch (Exception e) {
            bad.add(id + ": " + path + ": " + e);
        }
    }
}
