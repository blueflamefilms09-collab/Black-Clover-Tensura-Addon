import com.newuniverse.nusmp.client.mega.system.Op;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout.Panel;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout.Quest;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout.Stats;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout.ToastView;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout.View;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the owner wants to SEE the System window before the game can be started, and the only honest picture is one made by the same code that draws it in the
 * game. This runs the REAL {@code OverlayLayout} (the pure class the in-game layer replays) for a named scene and prints every mark it produces, one per line,
 * for tools/system/preview_overlay.py to paint. The text width comes from a table of the default font's glyph advances (written by the script from the game's
 * own font atlas), so wrapping and spacing match the game. Nothing here is part of the mod.
 *
 * Usage: java OverlayDump &lt;glyph table&gt; &lt;scene&gt; &lt;guiW&gt; &lt;guiH&gt; &lt;scale&gt; &lt;anchor&gt; &lt;alpha&gt; &lt;time&gt;
 * Output: "frame x y w h", then "F x1 y1 x2 y2 argb", "G x1 y1 x2 y2 top bottom", "T x y argb shadow text" (panel), then the same after "toasts".
 */
public final class OverlayDump {
    private static final int[] ADVANCE = new int[128];

    public static void main(String[] a) throws Exception {
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.trim().split(" ");
            if (p.length == 2) ADVANCE[Integer.parseInt(p[0])] = Integer.parseInt(p[1]);
        }
        String scene = a[1];
        int guiW = Integer.parseInt(a[2]), guiH = Integer.parseInt(a[3]);
        float scale = Float.parseFloat(a[4]), alpha = Float.parseFloat(a[6]), time = Float.parseFloat(a[7]);
        int anchor = Integer.parseInt(a[5]);
        OverlayLayout layout = new OverlayLayout(OverlayDump::width);
        View view = new View(guiW, guiH, scale, anchor, alpha, time);
        OverlayLayout.Frame f = layout.panel(panel(scene), view);
        System.out.println("frame " + f.x() + " " + f.y() + " " + f.w() + " " + f.h());
        dump(f.ops());
        System.out.println("toasts");
        int tx = 6, ty = anchor == 0 ? f.y() + f.h() + 8 : 8;
        dump(layout.toasts(toasts(scene), tx, ty));
    }

    static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += c < 128 && ADVANCE[c] > 0 ? ADVANCE[c] : 6;
        }
        return w;
    }

    static void dump(List<Op> ops) {
        for (Op op : ops) {
            if (op instanceof Op.Fill f) System.out.println(String.format(Locale.ROOT, "F %d %d %d %d %d", f.x1(), f.y1(), f.x2(), f.y2(), f.argb() & 0xFFFFFFFFL));
            else if (op instanceof Op.Gradient g) System.out.println(String.format(Locale.ROOT, "G %d %d %d %d %d %d", g.x1(), g.y1(), g.x2(), g.y2(), g.top() & 0xFFFFFFFFL, g.bottom() & 0xFFFFFFFFL));
            else if (op instanceof Op.Text t) System.out.println(String.format(Locale.ROOT, "T %d %d %d %d %s", t.x(), t.y(), t.argb() & 0xFFFFFFFFL, t.shadow() ? 1 : 0, t.text()));
        }
    }

    // ---------------------------------------------------------------- the scenes
    static Stats stats(int str, int agi, int vit, int in, int sense) {
        return new Stats(new String[]{"STR", "AGI", "VIT", "INT", "SENSE"}, new int[]{str, agi, vit, in, sense});
    }

    static Panel panel(String scene) {
        Stats st = stats(3, 1, 0, 2, 0);
        String clearTitle = "Clear 5 rooms without taking magic damage";
        return switch (scene) {
            case "count" -> holy(12, 12, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, clearTitle, -1, false, "", "COMPLETE", "FAILED", "", 3, 5, "TIME", -1, 0, false), st);
            case "typing" -> holy(13, 13, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Defeat 9 monsters on this floor", 17, true, "", "COMPLETE", "FAILED", "", 0, 9, "TIME", -1, 0, false), st);
            case "many" -> holy(31, 31, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Survive the ambush: defeat 12 of the creatures that hunt you", -1, false, "", "COMPLETE", "FAILED", "", 7, 12, "TIME", -1, 0, false), stats(12, 4, 9, 7, 2));
            case "speed" -> holy(7, 7, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_CLOCK, "Reach the next floor within 286 seconds", -1, false, "", "COMPLETE", "FAILED", "", 0, 286, "TIME", 4210, 5720, true), st);
            case "urgent" -> holy(7, 7, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_CLOCK, "Reach the next floor within 286 seconds", -1, false, "", "COMPLETE", "FAILED", "", 0, 286, "TIME", 310, 5720, true), st);
            case "unharmed" -> holy(4, 4, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_STATE, "Reach the next floor without taking any damage", -1, false, "UNHARMED", "COMPLETE", "FAILED", "", 0, 1, "TIME", -1, 0, false), st);
            case "done" -> holy(12, 12, new Quest(OverlayLayout.Q_DONE, OverlayLayout.MODE_COUNT, clearTitle, -1, false, "", "COMPLETE", "FAILED", "", 5, 5, "TIME", -1, 0, false), stats(4, 1, 0, 2, 0));
            case "failed" -> holy(12, 12, new Quest(OverlayLayout.Q_FAILED, OverlayLayout.MODE_COUNT, clearTitle, -1, false, "", "COMPLETE", "FAILED", "", 3, 5, "TIME", -1, 0, false), st);
            case "foreign" -> holy(2, 2, new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, clearTitle, -1, false, "", "COMPLETE", "FAILED", "QUEST OF FLOOR +3", 3, 5, "TIME", -1, 0, false), st);
            case "boss" -> new Panel("[ SYSTEM ]", "FLOOR", 10, 1, "HOLY", "THREAT", 10, true, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR", null, st);
            case "noquest" -> new Panel("[ SYSTEM ]", "FLOOR", 8, 1, "HOLY", "THREAT", 8, false, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR", null, st);
            case "abyssal" -> new Panel("[ SYSTEM ]", "FLOOR", -120, 2, "ABYSSAL", "THREAT", 120, false, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR",
                    new Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Defeat 20 monsters on this floor", -1, false, "", "COMPLETE", "FAILED", "", 11, 20, "TIME", -1, 0, false), stats(30, 30, 30, 30, 30));
            case "bare" -> new Panel("[ SYSTEM ]", "FLOOR", 1, 1, "HOLY", "THREAT", 1, false, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR", null, null);
            default -> throw new IllegalArgumentException("unknown scene " + scene);
        };
    }

    static Panel holy(int floor, int threat, Quest q, Stats st) {
        return new Panel("[ SYSTEM ]", "FLOOR", floor, 1, "HOLY", "THREAT", threat, false, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR", q, st);
    }

    static List<ToastView> toasts(String scene) {
        List<ToastView> t = new ArrayList<>();
        switch (scene) {
            case "count", "typing", "many" -> t.add(new ToastView("[SYSTEM]", "QUEST ARRIVED", 0, 30f));
            case "done" -> {
                t.add(new ToastView("[SYSTEM]", "QUEST COMPLETE", 1, 40f));
                t.add(new ToastView("[SYSTEM]", "REWARD: +1 STR", 3, 22f));
                t.add(new ToastView("[SYSTEM]", "EXTRACTION READY", 4, 8f));
            }
            case "failed" -> t.add(new ToastView("[SYSTEM]", "QUEST FAILED", 2, 40f));
            case "speed", "urgent" -> t.add(new ToastView("[SYSTEM]", "FLOOR +7 REACHED", 0, 12f));
            default -> {
            }
        }
        return t;
    }
}
