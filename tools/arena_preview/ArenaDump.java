import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaThemes;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dumps the REAL plan of a boss hall (the very object the game builds from) as one JSON file, so tools/arena_preview.py can draw it without a game.
 * Usage: java ArenaDump <out dir> [theme id ...]   (no theme ids = every hall)
 */
public final class ArenaDump {
    private ArenaDump() {}

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        List<String> ids = args.length > 1 ? List.of(java.util.Arrays.copyOfRange(args, 1, args.length)) : ArenaThemes.ids();
        for (String id : ids) {
            BossArenaStructure a = ArenaThemes.build(id);
            if (a == null) throw new IllegalArgumentException("unknown hall " + id);
            ArenaThemes.Def d = ArenaThemes.def(id);
            StringBuilder b = new StringBuilder(1 << 20);
            b.append("{\"id\":").append(q(id)).append(",\"title\":").append(q(d.title())).append(",\"side\":").append(q(d.side())).append(",\"size\":").append(a.size)
                    .append(",\"height\":").append(a.height).append(",\"foundation\":").append(BossArenaStructure.FOUNDATION).append(",\"palette\":[");
            List<String> pal = a.palette();
            for (int i = 0; i < pal.size(); i++) b.append(i > 0 ? "," : "").append(q(pal.get(i)));
            b.append("],\"rle\":[");
            short[] raw = a.raw();
            int run = 1;
            boolean first = true;
            for (int i = 1; i <= raw.length; i++) {
                if (i < raw.length && raw[i] == raw[i - 1]) {
                    run++;
                    continue;
                }
                b.append(first ? "" : ",").append('[').append(run).append(',').append(raw[i - 1]).append(']');
                first = false;
                run = 1;
            }
            b.append("],\"elements\":[");
            boolean fe = true;
            for (BossArenaStructure.Element e : a.elements()) {
                b.append(fe ? "" : ",").append("{\"id\":").append(e.id).append(",\"kind\":").append(q(e.kind.name())).append(",\"effect\":").append(q(e.effect.name())).append(",\"adds\":").append(e.adds)
                        .append(",\"locked\":").append(e.locked).append(",\"share\":").append(e.bossShare).append(",\"cells\":[");
                for (int i = 0; i < e.cells.size(); i++) {
                    BossArenaStructure.Placed c = e.cells.get(i);
                    b.append(i > 0 ? "," : "").append('[').append(c.x()).append(',').append(c.y()).append(',').append(c.z()).append(']');
                }
                b.append("]}");
                fe = false;
            }
            b.append("],\"zones\":[");
            for (int i = 0; i < a.zones().size(); i++) {
                BossArenaStructure.Zone z = a.zones().get(i);
                b.append(i > 0 ? "," : "").append('[').append(q(z.kind())).append(',').append(z.x()).append(',').append(z.z()).append(',').append(z.r()).append(']');
            }
            b.append("],\"spots\":[");
            for (int i = 0; i < a.spots().size(); i++) b.append(i > 0 ? "," : "").append('[').append(a.spots().get(i).x()).append(',').append(a.spots().get(i).z()).append(']');
            b.append("],\"seal\":[");
            for (int i = 0; i < a.sealCells().size(); i++) {
                BossArenaStructure.Placed c = a.sealCells().get(i);
                b.append(i > 0 ? "," : "").append('[').append(c.x()).append(',').append(c.y()).append(',').append(c.z()).append(']');
            }
            b.append("],\"gate\":[").append(a.gate()[0]).append(',').append(a.gate()[1]).append("],\"entry\":[").append(a.entry()[0]).append(',').append(a.entry()[1]).append("]}");
            Files.writeString(out.resolve(id + ".json"), b.toString(), StandardCharsets.UTF_8);
            System.out.println(id + " " + a.size + "x" + a.size + "x" + a.height + " elements=" + a.elements().size() + " zones=" + a.zones().size());
        }
    }

    private static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.append('"').toString();
    }
}
