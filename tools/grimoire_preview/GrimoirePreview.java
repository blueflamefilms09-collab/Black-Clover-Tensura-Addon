import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;

import java.io.FileWriter;
import java.util.Locale;

/**
 * Dumps the real grimoire geometry (GrimoireBookPlan) and resolved tints (BookLook) for a list of looks as JSON, for preview.py.
 * args: out.json then entries "label|cover|magic|canon|seed|held"
 */
public final class GrimoirePreview {
    public static void main(String[] a) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i < a.length; i++) {
            String[] p = a[i].split("\\|", -1);
            BookLook look = BookLook.resolve(p[1], p[2], p[3], Integer.parseInt(p[4]));
            boolean held = Boolean.parseBoolean(p[5]);
            if (i > 1) sb.append(',');
            sb.append("{\"label\":\"").append(p[0]).append("\",\"tints\":[").append(look.tint(0)).append(',').append(look.tint(1)).append(',').append(look.tint(2))
              .append("],\"glow\":").append(look.glowColor()).append(",\"quads\":[");
            boolean first = true;
            for (GrimoireBookPlan.Quad q : GrimoireBookPlan.build(look.key(held))) {
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"t\":\"").append(q.texture()).append("\",\"tint\":").append(q.tint()).append(",\"e\":").append(q.emissive())
                  .append(",\"f\":\"").append(q.face()).append("\",\"p\":[");
                for (int k = 0; k < 12; k++) sb.append(k > 0 ? "," : "").append(String.format(Locale.ROOT, "%.3f", q.pos()[k]));
                sb.append("],\"uv\":[");
                for (int k = 0; k < 8; k++) sb.append(k > 0 ? "," : "").append(String.format(Locale.ROOT, "%.3f", q.uv()[k]));
                sb.append("]}");
            }
            sb.append("]}");
        }
        sb.append(']');
        try (FileWriter w = new FileWriter(a[0])) { w.write(sb.toString()); }
    }
}
