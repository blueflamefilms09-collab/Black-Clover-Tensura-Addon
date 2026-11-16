package com.newuniverse.nusmp.client.playermodel;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why: the redesigned Black Clover NPC and Midnight Sun looks (docs/npcs_bc.md, {@link BcNpcLooks}) are generator-built and nobody sees them in a game from here, so what a machine can prove is proved here
 * the same way {@link BcAudit} does it for the bosses: the old look is still there (rule 2), the new look exists with a 256 skin, no kit hair, every {@code pmx_bc_} model with all its cube faces inside
 * its atlas, and the quads inside {@link BcAudit}'s budgets. Reuses BcAudit's geometry check. No game class, any opener (classpath in the CI self-test {@code bcnpc.looks}, a file in the unit test).
 */
public final class BcNpcAudit {
    private BcNpcAudit() {}

    public static List<String> run(Function<String, InputStream> open) {
        List<String> bad = new ArrayList<>();
        Map<String, PlayerLook> looks = PlayerLookTables.load(open).looks();
        if (BcNpcLooks.oldIds().isEmpty()) bad.add("BcNpcLooks has no entries");
        for (String old : BcNpcLooks.oldIds()) {
            String id = BcNpcLooks.of(old);
            if (!looks.containsKey(old)) bad.add("the old look '" + old + "' is gone: nothing is removed (rule 2)");
            PlayerLook l = looks.get(id);
            if (l == null) {
                bad.add("BcNpcLooks maps " + old + " to " + id + " but looks_bc_npc.json has no such look");
                continue;
            }
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
                int[] q = BcAudit.geoProblems(id, x, open, bad);
                quads += q[0];
                boolean fx = x.model().endsWith("_fx");
                if (fx) fxQuads += q[0];
                if (fx && q[1] > 0 && !x.glow()) bad.add(id + ": the fx model has a glow map but not the glow flag");
                if (!fx && x.glow()) bad.add(id + ": only the fx model glows");
            }
            if (models == 0) bad.add(id + " has no pmx_bc_ extras model");
            if (quads > BcAudit.MAX_QUADS) bad.add(id + ": " + quads + " quads of new parts, over the budget of " + BcAudit.MAX_QUADS);
            if (fxQuads > BcAudit.MAX_FX_QUADS) bad.add(id + ": " + fxQuads + " glowing quads, over the budget of " + BcAudit.MAX_FX_QUADS);
        }
        return bad;
    }
}
