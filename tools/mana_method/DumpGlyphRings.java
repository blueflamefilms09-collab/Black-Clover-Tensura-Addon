import com.newuniverse.nusmp.mana.method.RuneElement;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout.Layout;
import com.newuniverse.nusmp.mana.method.RuneSpec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Preview helper (not part of the mod): reads scene lines from stdin and prints the quads of the REAL {@code RuneGlyphLayout} for each as one JSON object per line.
 * A scene line is {@code name element mods true age charge lx ly lz cx cy cz camx camy camz detail}.
 */
public final class DumpGlyphRings {
    private static String arr(float[] a, int n) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "%.4f", a[i]));
        }
        return sb.append(']').toString();
    }

    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Layout lay = new Layout();                               // one layout, reused for every scene, as the game reuses one per rune
        for (String line; (line = in.readLine()) != null; ) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("\\s+");
            RuneSpec spec = new RuneSpec(RuneElement.valueOf(t[1]), Integer.parseInt(t[2]), Boolean.parseBoolean(t[3]));
            double age = Double.parseDouble(t[4]);
            int charge = Integer.parseInt(t[5]);
            double[] v = new double[9];
            for (int i = 0; i < 9; i++) v[i] = Double.parseDouble(t[6 + i]);
            float detail = Float.parseFloat(t[15]);
            Params p = RuneGeneratorUtil.paramsFor(spec);
            Frame f = RuneGeneratorUtil.frameFacing(v[3], v[4], v[5], v[0], v[1], v[2]);
            int level = RuneGlyphLayout.levelFor(detail);
            RuneGlyphLayout.fill(lay, spec, p, f, age, charge, level, v[6], v[7], v[8]);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"name\":\"").append(t[0]).append("\",\"level\":").append(level).append(",\"quads\":").append(lay.quads()).append(",\"expected\":").append(RuneGlyphLayout.quadsAt(p, level));
            sb.append(",\"glyphs\":[");
            for (int i = 0; i < lay.glyphs; i++) {
                if (i > 0) sb.append(',');
                sb.append("{\"c\":").append(arr(new float[]{lay.gc[i * 3], lay.gc[i * 3 + 1], lay.gc[i * 3 + 2]}, 3));
                sb.append(",\"r\":").append(arr(new float[]{lay.gr[i * 3], lay.gr[i * 3 + 1], lay.gr[i * 3 + 2]}, 3));
                sb.append(",\"u\":").append(arr(new float[]{lay.gu[i * 3], lay.gu[i * 3 + 1], lay.gu[i * 3 + 2]}, 3));
                sb.append(String.format(Locale.ROOT, ",\"light\":%.3f,\"cell\":%d,\"ring\":%d}", lay.light[i], lay.cell[i], lay.ring[i]));
            }
            sb.append("],\"arcs\":[");
            for (int i = 0; i < lay.arcQuads; i++) {
                if (i > 0) sb.append(',');
                float[] q = new float[12];
                System.arraycopy(lay.aq, i * 12, q, 0, 12);
                sb.append("{\"q\":").append(arr(q, 12)).append(String.format(Locale.ROOT, ",\"light\":%.3f}", lay.arcLight[i]));
            }
            sb.append("],\"book\":").append(lay.bookQuads > 0 ? arr(lay.bq, 36) : "null").append(String.format(Locale.ROOT, ",\"bookLight\":%.3f}", lay.bookLight));
            System.out.println(sb);
        }
    }
}
