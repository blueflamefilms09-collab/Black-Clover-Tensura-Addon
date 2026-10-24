import com.newuniverse.nusmp.mana.method.RuneElement;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Geometry;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneSpec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Preview helper (not part of the mod): reads scene lines from stdin and prints the world points of the REAL {@code RuneGeneratorUtil} for each, as one JSON object per line, so
 * the Python renderer (preview_runes.py) draws exactly what the game's client would spawn. A scene line is
 * {@code name element mods true progress lx ly lz cx cy cz spin flare}; a point is {@code [x, y, z, kind, ring]}.
 */
public final class DumpRunes {
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (String line; (line = in.readLine()) != null; ) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("\\s+");
            String name = t[0];
            RuneElement e = RuneElement.valueOf(t[1]);
            RuneSpec spec = new RuneSpec(e, Integer.parseInt(t[2]), Boolean.parseBoolean(t[3]));
            double progress = Double.parseDouble(t[4]);
            double lx = Double.parseDouble(t[5]), ly = Double.parseDouble(t[6]), lz = Double.parseDouble(t[7]);
            double cx = Double.parseDouble(t[8]), cy = Double.parseDouble(t[9]), cz = Double.parseDouble(t[10]);
            double spin = Double.parseDouble(t[11]);
            boolean flare = Boolean.parseBoolean(t[12]);
            Params p = RuneGeneratorUtil.paramsFor(spec);
            Geometry g = RuneGeneratorUtil.geometry(p);
            Frame f = RuneGeneratorUtil.frameFacing(cx, cy, cz, lx, ly, lz);
            int shown = RuneGeneratorUtil.visible(g, progress);
            double[] out = new double[3];
            StringBuilder sb = new StringBuilder();
            sb.append("{\"name\":\"").append(name).append("\",\"tint\":").append(p.tint()).append(",\"glyph\":\"").append(p.glyphParticle()).append("\",\"points\":[");
            boolean first = true;
            for (int i = 0; i < shown; i++) {
                RuneGeneratorUtil.toWorld(g, i, p.radius(), f, spin, out);
                if (!first) sb.append(',');
                first = false;
                sb.append(String.format(Locale.ROOT, "[%.5f,%.5f,%.5f,%d,%d]", out[0], out[1], out[2], g.kind[i], g.ring[i]));
            }
            if (flare) {
                for (int i = 0; i < RuneGeneratorUtil.flareCount(g); i++) {
                    RuneGeneratorUtil.toWorld(g, i, p.radius() * 1.35, f, spin, out);
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(String.format(Locale.ROOT, "[%.5f,%.5f,%.5f,3,0]", out[0], out[1], out[2]));
                }
            }
            sb.append("],\"normal\":[").append(String.format(Locale.ROOT, "%.5f,%.5f,%.5f", f.nx(), f.ny(), f.nz())).append("],\"radius\":").append(p.radius()).append("}");
            System.out.println(sb);
        }
    }
}
