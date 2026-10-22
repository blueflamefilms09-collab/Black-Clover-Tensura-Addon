import com.newuniverse.nusmp.engraving.codex.EngravingCodexLook;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;

import java.io.FileWriter;
import java.util.Locale;

/** Dumps the real geometry (GrimoireBookPlan) of the Engravement Codex look as JSON for preview_codex_book.py. args: out.json. */
public final class EngravingPreview {
    public static void main(String[] a) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        boolean[] helds = {false, true};
        for (int h = 0; h < 2; h++) {
            var look = EngravingCodexLook.LOOK;
            if (h > 0) sb.append(',');
            sb.append("{\"label\":\"engraving ").append(helds[h] ? "held" : "closed").append("\",\"tints\":[").append(look.tint(0)).append(',').append(look.tint(1)).append(',').append(look.tint(2))
              .append("],\"glow\":").append(look.glowColor()).append(",\"quads\":[");
            boolean first = true;
            for (GrimoireBookPlan.Quad q : GrimoireBookPlan.build(look.key(helds[h]))) {
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"t\":\"").append(q.texture()).append("\",\"tint\":").append(q.tint()).append(",\"e\":").append(q.emissive())
                  .append(",\"f\":\"").append(q.face()).append("\",\"n\":[").append(String.format(Locale.ROOT, "%.3f,%.3f,%.3f", q.normal()[0], q.normal()[1], q.normal()[2])).append("],\"p\":[");
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
