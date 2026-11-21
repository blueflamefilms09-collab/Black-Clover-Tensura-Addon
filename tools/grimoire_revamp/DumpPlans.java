import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;
import com.newuniverse.nusmp.grimoire.GrimoireRevampPlan;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * Dumps the classic and the revamped plan of a few looks as JSON for tools/grimoire_revamp/preview.py (the software preview). Not part of the mod.
 * javac -proc:none -d OUT -cp "$(cat cp_full2.txt)" -sourcepath src/main/java tools/grimoire_revamp/DumpPlans.java && java -cp OUT:CP:src/main/resources DumpPlans plans.json
 */
public final class DumpPlans {
    record Look(String name, String cover, String magic, String canon) {}

    public static void main(String[] args) throws Exception {
        Look[] looks = {
                new Look("flame_three_leaf", "THREE_LEAF", "FLAME", null),
                new Look("wind_four_leaf", "FOUR_LEAF", "WIND", null),
                new Look("water_heart", "HEART", "WATER", null),
                new Look("anti_five_leaf_tattered", "FIVE_LEAF", "ANTI_MAGIC", null),
                new Look("time_spade", "SPADE", "TIME", null),
                new Look("god_tier_light", "GOD_TIER", "LIGHT", null),
        };
        try (PrintWriter out = new PrintWriter(args[0], "UTF-8")) {
            out.println("[");
            boolean first = true;
            for (Look l : looks) {
                BookLook look = BookLook.resolve(l.cover(), l.magic(), l.canon(), 0);
                for (boolean held : new boolean[]{false, true}) {
                    for (int open : new int[]{0, 1, 2, 3}) {
                        BookLook.Key key = look.key(held, open);
                        if (!first) out.println(",");
                        first = false;
                        out.print("{\"look\":\"" + l.name() + "\",\"held\":" + held + ",\"open\":" + open + ",\"magic\":\"" + l.magic() + "\",\"tattered\":" + look.tattered()
                                + ",\"tint\":[" + look.tint(0) + "," + look.tint(1) + "," + look.tint(2) + "],\"glow\":" + look.glowColor() + ",");
                        quads(out, "classic", GrimoireBookPlan.build(key));
                        out.print(",");
                        quads(out, "revamp", GrimoireRevampPlan.build(key));
                        out.print("}");
                    }
                }
            }
            out.println("\n]");
        }
    }

    static void quads(PrintWriter out, String name, List<GrimoireBookPlan.Quad> quads) {
        out.print("\"" + name + "\":[");
        for (int i = 0; i < quads.size(); i++) {
            GrimoireBookPlan.Quad q = quads.get(i);
            if (i > 0) out.print(",");
            StringBuilder sb = new StringBuilder("[\"" + q.texture() + "\"," + q.tint() + "," + (q.emissive() ? 1 : 0) + ",[");
            for (int k = 0; k < 12; k++) sb.append(k > 0 ? "," : "").append(String.format(Locale.ROOT, "%.4f", q.pos()[k]));
            sb.append("],[");
            for (int k = 0; k < 8; k++) sb.append(k > 0 ? "," : "").append(String.format(Locale.ROOT, "%.4f", q.uv()[k]));
            sb.append("]]");
            out.print(sb);
        }
        out.print("]");
    }
}
