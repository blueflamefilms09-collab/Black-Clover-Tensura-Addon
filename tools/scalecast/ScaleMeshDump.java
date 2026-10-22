import com.newuniverse.nusmp.scalecast.ScaleCastRules;
import com.newuniverse.nusmp.scalecast.ScaleMesh;

import java.util.Locale;

/**
 * Dumps the Scale mesh (the real in-game geometry, scalecast/ScaleMesh) and a table of the real rules (scalecast/ScaleCastRules) for tools/scalecast/preview_scale.py.
 * Mesh lines: part, layer, 4 x (x y z u v), normal (nx ny nz). Rule lines: RULE decades tilt strength reduction magic boss.
 */
public final class ScaleMeshDump {
    public static void main(String[] args) {
        for (String part : ScaleMesh.PART_NAMES) {
            float[] q = ScaleMesh.raw(part);
            int layer = ScaleMesh.layerOf(part);
            for (int o = 0; o + ScaleMesh.STRIDE <= q.length; o += ScaleMesh.STRIDE) {
                StringBuilder sb = new StringBuilder(part).append(' ').append(layer);
                for (int k = 0; k < ScaleMesh.STRIDE; k++) sb.append(String.format(Locale.ROOT, " %.5f", q[o + k]));
                System.out.println(sb);
            }
        }
        for (int lod = 0; lod < ScaleMesh.LODS; lod++) System.out.println("QUADS " + lod + " " + ScaleMesh.quads(lod));
        for (double d = -3.0; d <= 3.01; d += 0.25) {
            double red = ScaleCastRules.reduction(d, false, ScaleCastRules.DAMAGE_CAP);
            System.out.println(String.format(Locale.ROOT, "RULE %.2f %.4f %.4f %.4f %.4f %.4f", d, ScaleCastRules.tilt(d), ScaleCastRules.strength(d), red,
                    ScaleCastRules.magicReduction(red), ScaleCastRules.reduction(d, true, ScaleCastRules.DAMAGE_CAP)));
        }
    }
}
