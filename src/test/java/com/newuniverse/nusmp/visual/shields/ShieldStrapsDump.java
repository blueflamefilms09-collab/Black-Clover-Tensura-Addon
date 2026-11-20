package com.newuniverse.nusmp.visual.shields;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.72.0: writes the straps the game would build for shield meshes, so tools/item_preview/preview_shields.py draws the real geometry instead of a second implementation.
 * <pre>java -cp ... com.newuniverse.nusmp.visual.shields.ShieldStrapsDump out.txt meshId...</pre>
 * Output, per mesh and context (0..3 = left * 2 + raised, 4 = first person): {@code S meshId ctx trimRgb}, then {@code V part x y z nx ny nz} four lines per quad (part 0 leather, 1 trim),
 * on the item frame; {@code FIT meshId scale tx ty tz back <z> anchor <z>} gives the third-person fit and the back of the plate.
 */
public final class ShieldStrapsDump {
    public static void main(String[] a) throws Exception {
        ShieldStraps.Data d = ShieldStrapsTest.data();
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(a[0])))) {
            for (int i = 1; i < a.length; i++) {
                Path mesh = ShieldStrapsTest.ASSETS.resolve("weapon_meshes").resolve(a[i] + ".txt");
                List<float[]> body = new ArrayList<>();
                List<float[]> all = ShieldStrapsTest.meshVerts(mesh, body);
                float back = ShieldStraps.plateBackZ(body.isEmpty() ? all : body);
                float[] fit = ShieldStrapsTest.fitOf(all);
                out.println("FIT " + a[i] + " " + fit[0] + " " + fit[1] + " " + fit[2] + " " + fit[3] + " back " + back + " anchor " + ShieldStraps.anchorZ(fit, back));
                for (int ctx = 0; ctx < ShieldStraps.CONTEXTS; ctx++) {
                    List<float[]>[] set = ShieldStraps.build(d, ctx, ShieldStraps.anchorZ(fit, back));
                    out.println("S " + a[i] + " " + ctx + " " + Integer.toHexString(ShieldStraps.trimRgb(a[i])));
                    for (int part = 0; part < 2; part++)
                        for (float[] v : set[part]) out.println("V " + part + " " + v[0] + " " + v[1] + " " + v[2] + " " + v[5] + " " + v[6] + " " + v[7]);
                }
            }
        }
    }
}
