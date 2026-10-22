import com.newuniverse.nusmp.client.mega.system.Op;
import com.newuniverse.nusmp.client.mega.system.OverlayLayout;
import com.newuniverse.nusmp.client.relics.djinn.gui.DjinnGuiPainter;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiSpec;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiState;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiTheme;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Place;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Rect;
import com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry;
import com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.Element;
import com.newuniverse.nusmp.relics.djinn.Djinn;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders GUI Move's edit mode without the game. What is REAL: the Djinn panel (the same DjinnGuiPainter the game calls), the System window (the same OverlayLayout.panel,
 * its marks replayed on a stand-in surface), and every position (GuiMoveMath, the registry's defaults, the saved places): the rectangles, the anchors, the snapping and the
 * clamp are the game's own code. What is a STAND-IN: the surface (alpha blended rectangles, a monospace font of 6 pixels a character), the world behind, the HUD elements
 * that need Minecraft to draw (shown as dimmed outlines, as the edit screen shows an element that is not on screen), and the edit screen's own drawing (redone here with the
 * same geometry and colours as client/ui/move/GuiMoveScreen). Run through tools/gui/preview_gui_move.py.
 */
public final class GuiMovePreview {
    static final int S = 3;
    static final int LIVE = 0xFF5FE0FF, GHOST = 0x905FE0FF, SELECTED = 0xFFFFB84D, HIDDEN = 0xFFFF5A5A, TEXT = 0xFFE8F4FF, TEXT_DIM = 0xFF9DB4C4, GUIDE = 0xFFFF7BD5;
    static Map<String, String> lang = new HashMap<>();

    static String tr(String key, String fallback) { return lang.getOrDefault(key, fallback); }

    /** One scene: where the elements are saved (null = never moved), who is selected, the guide lines, and whether the edit screen is on top. */
    static final class Scene {
        int guiW = 427, guiH = 240;
        Map<String, Place> placed = new HashMap<>();
        Map<String, Float> sizes = new HashMap<>();
        Set<String> hidden = new HashSet<>();
        Set<String> live = new HashSet<>(Set.of("system_hud", "djinn_panel"));
        String selected;
        double guideX = Double.NaN, guideY = Double.NaN;
        boolean edit;
        String caption;
    }

    // ------------------------------------------------------------ the real windows
    static GuiSpec spec() { return GuiSpec.of(Djinn.values()[0]); }

    static GuiState djinnState() {
        GuiState s = new GuiState();
        s.phase = 3;
        s.prev = 3;
        s.time = 160f;
        s.magoi = 72;
        s.assim = 380;
        s.trust = 42;
        s.timeMax = 1800;
        s.timeLeft = 1130;
        return s;
    }

    static OverlayLayout.Frame systemFrame(int guiW, int guiH) {
        OverlayLayout lay = new OverlayLayout(s -> s.length() * 6);
        OverlayLayout.Quest q = new OverlayLayout.Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Defeat 5 monsters", -1, false, "Unharmed", "Complete", "Failed", "", 2, 5, "Time", -1, 100, false);
        OverlayLayout.Stats st = new OverlayLayout.Stats(new String[]{"STR", "AGI", "VIT", "INT", "SENSE"}, new int[]{12, 9, 14, 7, 5});
        OverlayLayout.Panel p = new OverlayLayout.Panel("[ SYSTEM ]", "Floor", 3, 1, "Holy", "Threat", 3, false, "Boss floor", "No quest", q, st);
        return lay.panel(p, new OverlayLayout.View(guiW, guiH, 1f, 0, 0.67f, 40f));
    }

    // ------------------------------------------------------------ the rectangles (the logic of GuiMove.rectOf, on the same pure classes)
    static Rect rectOf(Element e, Scene sc) {
        int w = e.defW, h = e.defH;
        if (e.id.equals("djinn_panel")) { w = GuiLayout.W; h = GuiLayout.H; }
        OverlayLayout.Frame sf = e.id.equals("system_hud") ? systemFrame(sc.guiW, sc.guiH) : null;
        if (sf != null) { w = sf.w(); h = sf.h(); }
        float ms = sc.sizes.getOrDefault(e.id, 1f);
        Place p = sc.placed.get(e.id);
        if (p != null) return GuiMoveMath.rect(p, sc.guiW, sc.guiH, GuiMoveMath.total(ms, 1f), w, h, e.margin);
        if (sc.live.contains(e.id)) {
            if (sf != null) return new Rect(sf.x(), sf.y(), sf.w(), sf.h());
            if (e.id.equals("djinn_panel")) return new Rect(GuiLayout.originX(GuiLayout.Anchor.BOTTOM_CENTER, sc.guiW), GuiLayout.originY(GuiLayout.Anchor.BOTTOM_CENTER, sc.guiH), w, h);
        }
        return GuiMoveMath.rect(e.defPlace, sc.guiW, sc.guiH, 1f, w, h, e.margin);
    }

    // ------------------------------------------------------------ drawing
    static BufferedImage world(int w, int h) {
        BufferedImage img = new BufferedImage(w * S, h * S, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h * S; y++)
            for (int x = 0; x < w * S; x++) {
                int n = (x / S * 7 + y / S * 13) % 5;
                int c = y < h * S * 6 / 10 ? 0x20304A + n * 0x010101 : 0x2A4A2A + n * 0x010201;
                img.setRGB(x, y, c);
            }
        return img;
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.SrcOver);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8 * S - 2));
        return g;
    }

    static void fill(Graphics2D g, double x1, double y1, double x2, double y2, int argb) {
        g.setColor(new Color(argb, true));
        g.fillRect((int) Math.round(x1 * S), (int) Math.round(y1 * S), (int) Math.round((x2 - x1) * S), (int) Math.round((y2 - y1) * S));
    }

    static void text(Graphics2D g, String s, double x, double y, int argb, boolean shadow) {
        if (shadow) {
            g.setColor(new Color(0, 0, 0, (argb >>> 24) * 2 / 3));
            g.drawString(s, (int) Math.round(x * S) + S, (int) Math.round(y * S) + 7 * S + S);
        }
        g.setColor(new Color(argb, true));
        g.drawString(s, (int) Math.round(x * S), (int) Math.round(y * S) + 7 * S);
    }

    static void drawDjinn(Graphics2D g, Rect r, float t) {
        AffineTransform keep = g.getTransform();
        g.translate(r.x() * S, r.y() * S);
        g.scale(t, t);
        DjinnGuiPainter p = new DjinnGuiPainter();
        p.paint(new DjinnGuiPreviewCanvas(g), GuiTheme.of(spec(), false), djinnState(), 0, 0);
        g.setTransform(keep);
    }

    static void drawSystem(Graphics2D g, Rect r, float t, OverlayLayout.Frame f) {
        AffineTransform keep = g.getTransform();
        g.translate(r.x() * S, r.y() * S);
        g.scale(t, t);
        for (Op op : f.ops()) {
            if (op instanceof Op.Fill o) fill(g, o.x1() - f.x(), o.y1() - f.y(), o.x2() - f.x(), o.y2() - f.y(), o.argb());
            else if (op instanceof Op.Gradient o) fill(g, o.x1() - f.x(), o.y1() - f.y(), o.x2() - f.x(), o.y2() - f.y(), o.top());
            else if (op instanceof Op.Text o) text(g, o.text(), o.x() - f.x(), o.y() - f.y(), o.argb(), o.shadow());
        }
        g.setTransform(keep);
    }

    /** The canvas the Djinn painter draws on: fills are alpha rectangles, text a stand-in font. */
    static final class DjinnGuiPreviewCanvas implements com.newuniverse.nusmp.client.relics.djinn.gui.Canvas {
        final Graphics2D g;

        DjinnGuiPreviewCanvas(Graphics2D g) { this.g = g; }

        public void fill(int x1, int y1, int x2, int y2, int argb) {
            g.setColor(new Color(argb, true));
            g.fillRect(x1 * S, y1 * S, (x2 - x1) * S, (y2 - y1) * S);
        }

        public void text(String s, int x, int y, int argb) {
            g.setColor(new Color(0, 0, 0, (argb >>> 24) * 2 / 3));
            g.drawString(s, x * S + S, y * S + 7 * S + S);
            g.setColor(new Color(argb, true));
            g.drawString(s, x * S, y * S + 7 * S);
        }

        public int textWidth(String s) { return s.length() * 6; }
    }

    static void dashes(Graphics2D g, Rect r, int color, int gap) {
        int x1 = (int) Math.round(r.x()), y1 = (int) Math.round(r.y()), x2 = (int) Math.round(r.right()), y2 = (int) Math.round(r.bottom());
        for (int x = x1; x < x2; x += 4 + gap) { fill(g, x, y1, Math.min(x2, x + 4), y1 + 1, color); fill(g, x, y2 - 1, Math.min(x2, x + 4), y2, color); }
        for (int y = y1; y < y2; y += 4 + gap) { fill(g, x1, y, x1 + 1, Math.min(y2, y + 4), color); fill(g, x2 - 1, y, x2, Math.min(y2, y + 4), color); }
    }

    static BufferedImage render(Scene sc) {
        BufferedImage img = world(sc.guiW, sc.guiH);
        Graphics2D g = gfx(img);
        // the HUD, as the game draws it: the real Djinn panel and System window where they are not hidden, at their saved places
        for (Element e : GuiMoveRegistry.all()) {
            if (sc.hidden.contains(e.id) || !sc.live.contains(e.id)) continue;
            Rect r = rectOf(e, sc);
            float t = GuiMoveMath.total(sc.sizes.getOrDefault(e.id, 1f), 1f);
            if (e.id.equals("djinn_panel")) drawDjinn(g, r, sc.placed.containsKey(e.id) ? t : 1f);
            if (e.id.equals("system_hud")) drawSystem(g, r, sc.placed.containsKey(e.id) ? t : 1f, systemFrame(sc.guiW, sc.guiH));
        }
        if (sc.edit) {
            fill(g, 0, 0, sc.guiW, sc.guiH, 0x60000000);                                   // the flat dim of the edit screen
            // the buttons (stand-ins of ThemedButton), five in a row as the screen lays them out
            int bw = 80, bh = 16, gap = 4, total = 5 * bw + 4 * gap, bx = (sc.guiW - total) / 2, by = sc.guiH - bh - gap - 4;
            String on = tr("gui_move.on", "ON"), off = tr("gui_move.off", "OFF");
            String[] labels = {tr("gui_move.snap", "Snap: %s").replace("%s", on), tr("gui_move.show_all", "Show all: %s").replace("%s", on), tr("gui_move.panels", "Panels: %s").replace("%s", off), tr("gui_move.reset_all", "Reset all"), tr("gui_move.done", "Done")};
            for (int i = 0; i < 5; i++) {
                int x = bx + i * (bw + gap);
                fill(g, x, by, x + bw, by + bh, 0xC0101820);
                dashes(g, new Rect(x, by, bw, bh), 0xFF5FE0FF, -3);
                text(g, labels[i], x + (bw - labels[i].length() * 6) / 2.0, by + 4, TEXT, true);
            }
            int footTop = by;
            List<Element> shown = new ArrayList<>();
            for (Element e : GuiMoveRegistry.all()) if (e.kind == GuiMoveRegistry.Kind.HUD || sc.placed.containsKey(e.id)) shown.add(e);
            // the words go on a plain band at the top
            List<String> lines = new ArrayList<>();
            wrap(tr("gui_move.hint", ""), (sc.guiW - 24) / 6, lines);
            int nHint = lines.size();
            wrap(tr("gui_move.hint2", ""), (sc.guiW - 24) / 6, lines);
            fill(g, 0, 0, sc.guiW, 22 + lines.size() * 10, 0xA0000000);
            Element sel = sc.selected == null ? null : GuiMoveRegistry.byId(sc.selected);
            for (Element e : shown) if (e != sel) outline(g, e, sc, false);
            if (sel != null) outline(g, sel, sc, true);
            if (!Double.isNaN(sc.guideX)) fill(g, sc.guideX, 0, sc.guideX + 1, sc.guiH, GUIDE);
            if (!Double.isNaN(sc.guideY)) fill(g, 0, sc.guideY, sc.guiW, sc.guideY + 1, GUIDE);
            String title = tr("gui_move.title", "Move GUIs");
            text(g, title, (sc.guiW - title.length() * 6) / 2.0, 6, SELECTED, true);
            for (int i = 0; i < lines.size(); i++) text(g, lines.get(i), (sc.guiW - lines.get(i).length() * 6) / 2.0, 18 + i * 10, i < nHint ? TEXT : TEXT_DIM, true);
            if (sel != null) {
                Rect r = rectOf(sel, sc);
                String info = sel.english + "   x " + Math.round(r.x()) + "  y " + Math.round(r.y()) + "   size " + Math.round(sc.sizes.getOrDefault(sel.id, 1f) * 100) + "%";
                fill(g, (sc.guiW - info.length() * 6) / 2.0 - 4, footTop - 14, (sc.guiW + info.length() * 6) / 2.0 + 4, footTop - 3, 0xA0000000);
                text(g, info, (sc.guiW - info.length() * 6) / 2.0, footTop - 12, TEXT, true);
            }
        }
        if (sc.caption != null) {
            fill(g, 0, sc.guiH - 11, sc.guiW, sc.guiH, 0xC0000000);
            text(g, sc.caption, 3, sc.guiH - 10, 0xFFFFE8A0, false);
        }
        g.dispose();
        return img;
    }

    static void wrap(String text, int perLine, List<String> out) {
        String t = text;
        while (!t.isEmpty()) {
            int cut = Math.min(t.length(), Math.max(20, perLine));
            if (cut < t.length()) { int sp = t.lastIndexOf(' ', cut); if (sp > 0) cut = sp; }
            out.add(t.substring(0, cut).trim());
            t = t.substring(cut).trim();
        }
    }

    static void outline(Graphics2D g, Element e, Scene sc, boolean sel) {
        Rect r = rectOf(e, sc);
        boolean hidden = sc.hidden.contains(e.id), alive = sc.live.contains(e.id);
        int color = sel ? SELECTED : hidden ? HIDDEN : alive ? LIVE : GHOST;
        if (sel || alive) fill(g, r.x(), r.y(), r.right(), r.bottom(), (sel ? 0x28000000 : 0x14000000) | (color & 0xFFFFFF));
        dashes(g, r, color, alive || sel ? 3 : 6);
        String label = e.english + (hidden ? " (hidden)" : e.kind == GuiMoveRegistry.Kind.PANEL ? " (screen)" : "");
        int lw = label.length() * 6 + 6;
        double lx = Math.max(0, Math.min(sc.guiW - lw, r.x())), ly = r.y() >= 12 ? r.y() - 11 : r.y() + 1;
        fill(g, lx, ly, lx + lw, ly + 10, 0xB0000000);
        fill(g, lx, ly, lx + 2, ly + 10, color);
        text(g, label, lx + 4, ly + 1, sel ? SELECTED : hidden ? HIDDEN : TEXT, false);
    }

    static void save(BufferedImage img, File f) throws Exception {
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
    }

    /** A drag: the corner follows the mouse, snaps to the screen and the other GUIs (the real GuiMoveMath.snap), then the real placeFromGui makes the saved place. */
    static Place drop(Scene sc, Element e, double wantX, double wantY, Scene out) {
        Rect cur = rectOf(e, sc);
        Rect moving = new Rect(wantX, wantY, cur.w(), cur.h());
        List<Rect> others = new ArrayList<>();
        for (Element o : GuiMoveRegistry.all()) if (o != e) others.add(rectOf(o, sc));
        GuiMoveMath.Snapped s = GuiMoveMath.snap(moving, others.toArray(new Rect[0]), sc.guiW, sc.guiH, GuiMoveMath.SNAP_DISTANCE);
        out.guideX = s.guideX();
        out.guideY = s.guideY();
        float t = GuiMoveMath.total(sc.sizes.getOrDefault(e.id, 1f), 1f);
        int w = e.id.equals("djinn_panel") ? GuiLayout.W : e.defW, h = e.id.equals("djinn_panel") ? GuiLayout.H : e.defH;
        return GuiMoveMath.placeFromGui(sc.guiW, sc.guiH, t, w, h, e.margin, s.x(), s.y());
    }

    public static void main(String[] a) throws Exception {
        File dir = new File(a[0]);
        java.nio.file.Path langFile = java.nio.file.Path.of(a[1]);
        for (java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(java.nio.file.Files.readString(langFile)); m.find(); )
            lang.put(m.group(1), m.group(2).replace("\\\"", "\""));

        // 1. edit mode, nothing moved yet: the Djinn panel and the System window are live (drawn for real), the rest dimmed
        Scene s1 = new Scene();
        s1.edit = true;
        s1.selected = "djinn_panel";
        s1.caption = "edit mode (F9): live GUIs outlined in cyan, the selected one in amber, GUIs not on screen dimmed";
        save(render(s1), new File(dir, "edit_mode.png"));

        // 2. the Djinn panel dragged to the top, snapping to the middle of the screen (the guide line)
        Scene s2 = new Scene();
        s2.edit = true;
        s2.selected = "djinn_panel";
        Element djinn = GuiMoveRegistry.byId("djinn_panel");
        s2.placed.put("djinn_panel", drop(s1, djinn, (s1.guiW - GuiLayout.W) / 2.0 + 4, 74, s2));
        s2.caption = "dragging the Djinn panel: it snaps to the middle of the screen (pink guide) and is saved as an anchor + offset";
        save(render(s2), new File(dir, "djinn_dragged.png"));

        // 3. back in the game: the moved panel, everything else where it always was
        Scene s3 = new Scene();
        s3.placed.putAll(s2.placed);
        s3.caption = "back in the game: the Djinn panel at its new place";
        save(render(s3), new File(dir, "djinn_moved_play.png"));

        // 3b. the same, made bigger with the wheel (size 1.5) and moved to the right edge
        Scene s3b = new Scene();
        s3b.sizes.put("djinn_panel", 1.5f);
        s3b.placed.put("djinn_panel", GuiMoveMath.placeFromGui(s3b.guiW, s3b.guiH, 1.5f, GuiLayout.W, GuiLayout.H, 62, s3b.guiW - GuiLayout.W * 1.5 - 4, 90));
        s3b.edit = true;
        s3b.selected = "djinn_panel";
        s3b.caption = "wheel: size 150%, dropped at the right edge";
        save(render(s3b), new File(dir, "djinn_bigger.png"));

        // 4. reset: back where it was
        Scene s4 = new Scene();
        s4.caption = "reset (R, or Reset all): everything is back in its normal place";
        save(render(s4), new File(dir, "reset.png"));

        // 5. the same saved layout on three window sizes: the right-anchored Djinn panel and the bottom-left System window keep their edges
        Scene base = new Scene();
        base.live = new HashSet<>(Set.of("system_hud", "djinn_panel"));
        int[][] sizes = {{320, 180}, {427, 240}, {640, 360}};
        List<BufferedImage> parts = new ArrayList<>();
        for (int[] z : sizes) {
            Scene s = new Scene();
            s.guiW = z[0];
            s.guiH = z[1];
            OverlayLayout.Frame f = systemFrame(427, 240);
            s.placed.put("djinn_panel", GuiMoveMath.placeFromGui(427, 240, 1f, GuiLayout.W, GuiLayout.H, 62, 427 - GuiLayout.W - 6, 24));
            s.placed.put("system_hud", GuiMoveMath.placeFromGui(427, 240, 1f, f.w(), f.h(), 6, 6, 240 - f.h() - 6));
            s.caption = z[0] + " x " + z[1] + " GUI pixels: Djinn panel top-right, System window bottom-left (one saved layout)";
            parts.add(render(s));
        }
        int totalW = 0, maxH = 0;
        for (BufferedImage p : parts) { totalW += p.getWidth() + 12; maxH = Math.max(maxH, p.getHeight()); }
        BufferedImage sheet = new BufferedImage(totalW, maxH + 12, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = sheet.createGraphics();
        sg.setColor(new Color(0x101018));
        sg.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int x = 6;
        for (BufferedImage p : parts) { sg.drawImage(p, x, 6, null); x += p.getWidth() + 12; }
        sg.dispose();
        save(sheet, new File(dir, "window_sizes.png"));
    }
}
