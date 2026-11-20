package com.newuniverse.nusmp.client.playermodel;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why: Kakashi's four looks (docs/bosses/kakashi.md) are built by tools/bc_playermodels/gen_kakashi.py on the Black Clover kit, and nobody can see them in a game from here, so what a machine can
 * prove is proved here, from the jar the same way the client reads it: each of the four looks exists in the merged tables, has the 256 skin (and its glow map when it says it glows), no kit hair
 * (the cube hair of its extras replaces it), a pmx_bc_ model for every extras entry whose face rectangles all lie inside their atlas ({@link BcAudit#geoProblems}), only the {@code _fx} model glows,
 * a shimmer theme, and the cost stays inside the budget of the doc. It reuses the checks of {@link BcAudit} and does not change them: the Black Clover audit still walks only its own looks. No game
 * class; any opener ({@link PlayerModelAudit#classpath} for the jar and the CI self-test {@code bcmodel.looks}, a file for the unit test).
 */
public final class KakashiLooksAudit {
    private KakashiLooksAudit() {}

    /** Worst case of one look: quads at full detail (main plus fine layers) and glowing quads. The Susanoo is a glow-only model, so its allowance is bigger than the Black Clover bosses'. */
    public static final int MAX_QUADS = 700, MAX_FX_QUADS = 360;

    public static List<String> run(Function<String, InputStream> open) {
        List<String> bad = new ArrayList<>();
        Map<String, PlayerLook> looks = PlayerLookTables.load(open).looks();
        for (String id : KakashiLooks.ids()) {
            PlayerLook l = looks.get(id);
            if (l == null) {
                bad.add("KakashiLooks names " + id + " but looks_bc_kakashi.json has no such look");
                continue;
            }
            if (KakashiLooks.theme(id) == null || BcLooks.theme(id) == null) bad.add(id + " has no shimmer theme");
            if (l.hasHair()) bad.add(id + " still has a kit hair style (" + l.hair() + "): the cube hair of its extras replaces it");
            if (!l.skin().equals(id)) bad.add(id + ": its skin is '" + l.skin() + "', expected the skin of the same name");
            String path = SkinPath.resolve(l.skin());
            check(open, path, id, "the skin", true, bad);
            if (l.glow()) check(open, SkinPath.glowOf(path), id, "the skin's glow map", false, bad);
            int quads = 0, fxQuads = 0, models = 0;
            for (PlayerLook.Extra x : l.extras()) {
                if (!x.model().startsWith("pmx_bc_kakashi")) {
                    bad.add(id + ": extras model " + x.model() + " is not a pmx_bc_kakashi model");
                    continue;
                }
                models++;
                int[] q = BcAudit.geoProblems(id, x, open, bad);
                quads += q[0];
                if (x.model().endsWith("_fx")) fxQuads += q[0];
                if (x.model().endsWith("_fx") && q[1] > 0 && !x.glow()) bad.add(id + ": the fx model has a glow map but not the glow flag");
                if (!x.model().endsWith("_fx") && x.glow()) bad.add(id + ": only the fx model glows (the others would draw a second additive pass for nothing)");
            }
            if (models == 0) bad.add(id + " has no pmx_bc_kakashi extras model");
            if (quads > MAX_QUADS) bad.add(id + ": " + quads + " quads of parts, over the budget of " + MAX_QUADS);
            if (fxQuads > MAX_FX_QUADS) bad.add(id + ": " + fxQuads + " glowing quads, over the budget of " + MAX_FX_QUADS);
            boolean susanoo = KakashiLooks.SUSANOO.equals(id);
            if (susanoo && fxQuads == 0) bad.add(id + " has no glowing Susanoo model");
            if (!susanoo && fxQuads > 0) bad.add(id + " has a glowing model but is not the Susanoo look");
        }
        return bad;
    }

    private static void check(Function<String, InputStream> open, String path, String id, String what, boolean skin, List<String> bad) {
        try (InputStream in = open.apply(path)) {
            java.awt.image.BufferedImage img = in == null ? null : ImageIO.read(in);
            if (img == null) bad.add(id + ": " + what + " " + path + " is missing");
            else if (skin && (img.getWidth() != 256 || img.getHeight() != 256)) bad.add(id + ": " + what + " is " + img.getWidth() + "x" + img.getHeight() + ", not 256x256");
        } catch (Exception e) {
            bad.add(id + ": " + what + " " + path + ": " + e);
        }
    }
}
