import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.engraving.codex.CodexEntries;
import com.newuniverse.nusmp.engraving.codex.CodexFilter;
import com.newuniverse.nusmp.engraving.codex.CodexLayout;
import com.newuniverse.nusmp.engraving.codex.TensuraNotes;
import com.newuniverse.nusmp.franchise.FranchiseData;
import com.newuniverse.nusmp.franchise.FranchiseMap;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless layout of the Engravement Codex screen for tools/engraving_codex/preview_screen.py: builds the real book from the shipped data (the real pure classes), measures text with the
 * widths of Minecraft's default font, lays the entries out with the real {@link CodexLayout}, filters and paginates with the real {@link CodexFilter}, and writes the pages as JSON for the
 * renderer. Run from the repository root. args: out.json.
 */
public final class ScreenPreview {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/";
    private static final int STRIP_W = 132, GAP = 8, FOUND_MAX = 6;

    /** Width in pixels of Minecraft's default font for a character (glyph width plus the one pixel of spacing). */
    static int cw(char c) {
        if (c == ' ') return 4;
        if ("i!.,:;|'".indexOf(c) >= 0) return c == '\'' ? 3 : 2;
        if ("lI[]t".indexOf(c) >= 0) return c == 'l' ? 3 : (c == 'I' || c == '[' || c == ']') ? 4 : 4;
        if ("(){}fk<>\"*".indexOf(c) >= 0) return c == '"' ? 4 : 5;
        if (c == '@' || c == '~') return 7;
        return 6;
    }

    static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += cw(s.charAt(i));
        return w;
    }

    static Map<String, String> lang() throws Exception {
        Map<String, String> out = new HashMap<>();
        for (String f : new String[]{"src/main/resources/assets/nusmp/lang/en_us.json", "tools/lang/franchise_engravings.json", "tools/lang/engraving_codex.json"}) {
            JsonObject o = JsonParser.parseString(Files.readString(new File(f).toPath())).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    static Map<String, List<String>> specs(String file) throws Exception {
        String src = Files.readString(new File(SRC + file).toPath());
        Map<String, List<String>> out = new HashMap<>();
        Matcher entry = Pattern.compile("Map\\.entry\\(\"([a-z0-9_]+)\", List\\.of\\(((?:e\\(\"[a-z_:]+\", \\d\\)(?:, )?)+)\\)\\)").matcher(src);
        while (entry.find()) {
            List<String> ids = new ArrayList<>();
            Matcher id = Pattern.compile("e\\(\"([a-z_:]+)\", \\d\\)").matcher(entry.group(2));
            while (id.find()) ids.add(id.group(1));
            out.put(entry.group(1), ids);
        }
        return out;
    }

    static String esc(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static String strs(List<String> l) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) sb.append(i > 0 ? "," : "").append(esc(l.get(i)));
        return sb.append("]").toString();
    }

    public static void main(String[] a) throws Exception {
        Map<String, String> lang = lang();
        FranchiseData.Table table = FranchiseData.table();
        Map<String, TreeSet<String>> franchise = new HashMap<>();
        for (Map.Entry<String, String> e : FranchiseMap.map().items().entrySet())
            franchise.computeIfAbsent(e.getValue(), k -> new TreeSet<>()).add(lang.getOrDefault("item.nusmp." + e.getKey(), CodexEntries.pretty(e.getKey())));
        Map<String, TreeSet<String>> carriers = new HashMap<>();
        for (String f : new String[]{"item/WeaponEngravings.java", "item/ArmorEngravings.java"})
            for (Map.Entry<String, List<String>> w : specs(f).entrySet())
                for (String id : w.getValue()) carriers.computeIfAbsent(id.contains(":") ? id : "tensura:" + id, k -> new TreeSet<>()).add(lang.getOrDefault("item.nusmp." + w.getKey(), CodexEntries.pretty(w.getKey())));
        List<CodexEntries.Own> own = new ArrayList<>();
        File[] files = new File("src/main/resources/data/nusmp/enchantment").listFiles((d, n) -> n.endsWith(".json") && !n.startsWith("fr_"));
        java.util.Arrays.sort(files);
        for (File f : files) {
            String id = f.getName().replace(".json", "");
            own.add(new CodexEntries.Own(id, "main", 1, carriers.getOrDefault("nusmp:" + id, new TreeSet<>())));
        }
        List<CodexEntries.TensuraRef> tensura = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> e : new java.util.TreeMap<>(carriers).entrySet())
            if (e.getKey().startsWith("tensura:")) tensura.add(new CodexEntries.TensuraRef(e.getKey().substring(8), null, 3, e.getValue()));
        TensuraNotes.Table notes = TensuraNotes.table();
        CodexEntries.Book book = CodexEntries.build(new CodexEntries.Input(table, franchise, own, tensura, notes, lang::get));

        String[][] scenarios = {
                {"first page: all engravings", "", ""},
                {"section: Black Clover", "black_clover", ""},
                {"section: Tensura Reincarnated engravings", CodexEntries.GROUP_TENSURA, ""},
                {"section: this mod's own enchantments", CodexEntries.GROUP_MOD, ""},
                {"search: 'fire'", "", "fire"},
                {"search: 'demon slayer'", "", "demon slayer"},
                {"search: no match", "", "zzzzqq"},
        };
        int[][] windows = {{640, 360}, {427, 240}};
        StringBuilder sb = new StringBuilder("[");
        boolean firstScenario = true;
        for (int[] win : windows) {
            int width = win[0], height = win[1];
            int bw = Math.min(width - 16, 600), bh = Math.min(height - 44, 380);
            int bx = (width - bw) / 2, by = Math.max(4, (height - bh - 22) / 2);
            int pagesX = bx + 8 + STRIP_W + 10, pw = (bx + bw - 8 - pagesX - 14) / 2, spineX = pagesX + pw + 7, pageH = bh - 24 - 22;
            float ts = CodexLayout.textScale(pageH);
            int vWidth = Math.round((pw - 8) / ts), vPageH = Math.round(pageH / ts), vGap = Math.round(GAP / ts);
            CodexLayout.Laid[] laid = new CodexLayout.Laid[book.entries().size()];
            for (int i = 0; i < laid.length; i++) laid[i] = CodexLayout.lay(i, book.entries().get(i), vWidth, vPageH, FOUND_MAX, new CodexLayout.Words("Found on", "+%d more", "no item of this mod carries it yet", "..."), ScreenPreview::width);
            for (String[] sc : scenarios) {
                int[] shown = CodexFilter.select(book.entries(), sc[1], sc[2]);
                int[] heights = new int[shown.length];
                for (int i = 0; i < shown.length; i++) heights[i] = laid[shown[i]].height();
                List<int[]> pages = CodexFilter.paginate(heights, vPageH, vGap);
                if (!firstScenario) sb.append(',');
                firstScenario = false;
                sb.append("{\"title\":").append(esc(sc[0] + " (" + width + "x" + height + ")")).append(",\"w\":").append(width).append(",\"h\":").append(height)
                  .append(",\"bx\":").append(bx).append(",\"by\":").append(by).append(",\"bw\":").append(bw).append(",\"bh\":").append(bh).append(",\"pagesX\":").append(pagesX).append(",\"pw\":").append(pw)
                  .append(",\"spineX\":").append(spineX).append(",\"pageH\":").append(pageH).append(",\"ts\":").append(String.format(Locale.ROOT, "%.3f", ts)).append(",\"vGap\":").append(vGap).append(",\"group\":").append(esc(sc[1])).append(",\"query\":").append(esc(sc[2]))
                  .append(",\"shown\":").append(shown.length).append(",\"total\":").append(book.entries().size()).append(",\"pageCount\":").append(pages.size()).append(",\"rows\":[");
                sb.append("{\"id\":\"\",\"label\":\"All engravings\",\"count\":").append(book.entries().size()).append(",\"colour\":").append(0x5CD8FF).append("}");
                for (CodexEntries.Group g : book.groups())
                    sb.append(",{\"id\":").append(esc(g.id())).append(",\"label\":").append(esc(g.shortName())).append(",\"count\":").append(g.count()).append(",\"colour\":").append(g.colour()).append("}");
                sb.append("],\"pages\":[");
                for (int p = 0; p < Math.min(pages.size(), 4); p++) {
                    if (p > 0) sb.append(',');
                    sb.append("[");
                    for (int k = 0; k < pages.get(p)[1]; k++) {
                        CodexLayout.Laid l = laid[shown[pages.get(p)[0] + k]];
                        if (k > 0) sb.append(',');
                        sb.append("{\"name\":").append(esc(l.name())).append(",\"group\":").append(esc(l.groupLine())).append(",\"colour\":").append(l.colour()).append(",\"scopeInline\":").append(esc(l.scopeInline())).append(",\"scopeX\":").append(l.scopeX()).append(",\"scope\":").append(strs(l.scope()))
                          .append(",\"text\":").append(strs(l.text())).append(",\"facts\":").append(strs(l.facts())).append(",\"cap\":[");
                        for (int c = 0; c < l.factCap().length; c++) sb.append(c > 0 ? "," : "").append(l.factCap()[c]);
                        sb.append("],\"found\":").append(strs(l.found())).append(",\"h\":").append(l.height()).append("}");
                    }
                    sb.append("]");
                }
                sb.append("]}");
            }
        }
        sb.append("]");
        try (FileWriter w = new FileWriter(a[0])) { w.write(sb.toString()); }
        System.out.println(book.entries().size() + " entries, " + book.groups().size() + " groups");
    }
}
