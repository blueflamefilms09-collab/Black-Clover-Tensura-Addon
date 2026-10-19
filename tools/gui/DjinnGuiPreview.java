import com.newuniverse.nusmp.client.relics.djinn.gui.Canvas;
import com.newuniverse.nusmp.client.relics.djinn.gui.DjinnGuiPainter;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiSpec;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiState;
import com.newuniverse.nusmp.client.relics.djinn.gui.GuiTheme;
import com.newuniverse.nusmp.relics.djinn.Affinity;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.Djinn.Look;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the REAL active-Djinn panel code (client/relics/djinn/gui, the same DjinnGuiPainter the game calls) into PNG files, so the look can be judged
 * without the game. Only the Canvas is different: fills become alpha-blended rectangles, text a stand-in font of 6 pixels a character.
 * Run through tools/gui/preview_djinn_gui.py (it compiles this file against src/main/java and writes docs/previews/djinn_gui/).
 *
 * Output: roster_full.png (every Djinn in a Full Equip), affinities.png, phases_NAME.png (sealed, lesser, half, full, extreme, dark full),
 * anim_NAME_NN.png frames (for a GIF), intro_NAME.png (the summon), stand_ins.png (synthetic Djinn for the 24 not yet in the table).
 */
public final class DjinnGuiPreview {
    static final int S = 3;

    static final class ImgCanvas implements Canvas {
        final Graphics2D g;
        final int ox, oy;
        final FontMetrics fm;

        ImgCanvas(Graphics2D g, int ox, int oy) {
            this.g = g;
            this.ox = ox;
            this.oy = oy;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8 * S - 2));
            fm = g.getFontMetrics();
        }

        public void fill(int x1, int y1, int x2, int y2, int argb) {
            g.setColor(new Color(argb, true));
            g.fillRect((ox + x1) * S, (oy + y1) * S, (x2 - x1) * S, (y2 - y1) * S);
        }

        public void text(String s, int x, int y, int argb) {
            g.setColor(new Color(0, 0, 0, (argb >>> 24) * 2 / 3));
            g.drawString(s, (ox + x) * S + S, (oy + y) * S + 7 * S + S);
            g.setColor(new Color(argb, true));
            g.drawString(s, (ox + x) * S, (oy + y) * S + 7 * S);
        }

        public int textWidth(String s) { return s.length() * 6; }
    }

    static BufferedImage newImage(int w, int h) {
        BufferedImage img = new BufferedImage(w * S, h * S, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int y = 0; y < h * S; y++)                              // a stand-in world: dusk sky over grass, so translucency can be judged
            for (int x = 0; x < w * S; x++) {
                int n = (x / S * 7 + y / S * 13) % 5;
                int c = y < h * S * 6 / 10 ? 0x20304A + n * 0x010101 : 0x2A4A2A + n * 0x010201;
                img.setRGB(x, y, c);
            }
        g.dispose();
        return img;
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.SrcOver);
        return g;
    }

    static GuiState state(int phase, float time) {
        GuiState s = new GuiState();
        s.phase = phase;
        s.prev = phase;
        s.time = time;
        s.magoi = 72;
        s.assim = 380;
        s.trust = 42;
        s.timeMax = phase == 1 ? 400 : phase == 3 ? 1800 : 60;
        s.timeLeft = phase == 1 ? 260 : phase == 3 ? 1130 : 22;
        return s;
    }

    static void panel(Graphics2D g, DjinnGuiPainter p, GuiTheme th, GuiState st, int x, int y) {
        p.paint(new ImgCanvas(g, x, y), th, st, 0, 0);
    }

    /** Hand made specs standing in for Djinn that are not in the table yet: proves the panel is data driven. */
    static List<GuiSpec> standIns() {
        record R(String id, Affinity a, int color, int glow) {}
        R[] rows = {
                new R("bael", Affinity.STRENGTH, 0xC0522A, 0xFFC9A0), new R("agares", Affinity.WATER, 0x2E86AB, 0xB8ECFF),
                new R("valefor", Affinity.LIGHTNING, 0xE0B020, 0xFFF3A8), new R("barbatos", Affinity.LIFE, 0x4C9A3A, 0xD6FFB0),
                new R("gusion", Affinity.WATER, 0x1E5AA8, 0xA8D0FF), new R("vepar", Affinity.WATER, 0x20A090, 0xB0FFF0),
                new R("shax", Affinity.SOUND, 0x9A5AD0, 0xE4C8FF), new R("buer", Affinity.LIFE, 0x78B040, 0xEAFFC0),
                new R("andrealphus", Affinity.LIGHT, 0xE8C860, 0xFFFFE0), new R("kimaris", Affinity.STRENGTH, 0x8A3A3A, 0xFFB8B8),
                new R("amdusias", Affinity.SOUND, 0xD070A0, 0xFFD0E8), new R("belial", Affinity.HEAT, 0xB82010, 0xFF9060),
                new R("decarabia", Affinity.LIGHT, 0x60A0E0, 0xE8F8FF), new R("seere", Affinity.WIND, 0x50C0A0, 0xD8FFF0),
                new R("dantalion", Affinity.LIGHT, 0xB0A0C8, 0xF8F0FF), new R("andromalius", Affinity.LIGHTNING, 0x6050C0, 0xCEC8FF),
                new R("glasya_labolas", Affinity.STRENGTH, 0x6A2A2A, 0xFF9A80), new R("haagenti", Affinity.STRENGTH, 0xC08030, 0xFFE0A0),
                new R("crocell", Affinity.WATER, 0x30708A, 0xA0E0F0), new R("furcas", Affinity.WIND, 0x80A8C0, 0xE8F4FF),
                new R("balam", Affinity.HEAT, 0xD05010, 0xFFC080), new R("alloces", Affinity.HEAT, 0xA03010, 0xFFA878),
                new R("camio", Affinity.SOUND, 0x7050B0, 0xDCC8FF), new R("murmur", Affinity.SOUND, 0x503070, 0xC8A8F0)};
        Look.Horns[] hs = Look.Horns.values();
        Look.Wings[] ws = Look.Wings.values();
        Look.Tail[] ts = Look.Tail.values();
        Look.Halo[] hl = Look.Halo.values();
        List<GuiSpec> out = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            R r = rows[i];
            Look look = new Look(hs[1 + i % 5], ws[1 + (i / 2) % 5], ts[i % 5], hl[(i / 3) % 4], i % 3 != 0);
            String name = r.id().substring(0, 1).toUpperCase() + r.id().substring(1);
            out.add(new GuiSpec(r.id(), name, "Stand-in title", "Stand-in Maso", r.color(), r.glow(), r.a(), look));
        }
        return out;
    }

    static void sheet(File out, List<GuiSpec> specs, int cols, int phase, float time, boolean dark) throws Exception {
        int rows = (specs.size() + cols - 1) / cols, cw = GuiLayout.W + 8, ch = GuiLayout.H + 8;
        BufferedImage img = newImage(cols * cw + 8, rows * ch + 8);
        Graphics2D g = gfx(img);
        DjinnGuiPainter p = new DjinnGuiPainter();
        for (int i = 0; i < specs.size(); i++) {
            GuiTheme th = GuiTheme.of(specs.get(i), dark);
            GuiState st = state(phase, time + i * 7);
            panel(g, p, th, st, 8 + (i % cols) * cw, 8 + (i / cols) * ch);
        }
        g.dispose();
        ImageIO.write(img, "png", out);
    }

    public static void main(String[] a) throws Exception {
        File dir = new File(a[0]);
        dir.mkdirs();
        List<GuiSpec> real = new ArrayList<>();
        for (Djinn d : Djinn.values()) real.add(GuiSpec.of(d));
        sheet(new File(dir, "roster_full.png"), real, 2, 3, 120f, false);
        sheet(new File(dir, "roster_dark_full.png"), real, 2, 3, 120f, true);
        sheet(new File(dir, "stand_ins.png"), standIns(), 3, 3, 90f, false);
        List<GuiSpec> aff = new ArrayList<>();
        for (Affinity af : Affinity.values()) for (GuiSpec s : real) if (s.affinity() == af) { aff.add(s); break; }
        sheet(new File(dir, "affinities.png"), aff, 2, 3, 150f, false);
        for (GuiSpec s : real) {
            int cw = GuiLayout.W + 8, ch = GuiLayout.H + 8;
            BufferedImage img = newImage(2 * cw + 8, 3 * ch + 8);
            Graphics2D g = gfx(img);
            DjinnGuiPainter p = new DjinnGuiPainter();
            int[] phases = {0, 1, 2, 3, 4, 3};
            for (int i = 0; i < 6; i++) {
                GuiTheme th = GuiTheme.of(s, i == 5);
                GuiState st = state(phases[i], 100 + i * 3);
                if (phases[i] == 0) { st.lockLeft = 3200; st.coolLeft = 5400; }
                if (phases[i] == 4) st.pulse = 0.0f;
                panel(g, p, th, st, 8 + (i % 2) * cw, 8 + (i / 2) * ch);
            }
            g.dispose();
            ImageIO.write(img, "png", new File(dir, "phases_" + s.id() + ".png"));
            // animation frames, one every 2 ticks, in a Full Equip
            File fr = new File(dir, "frames_" + s.id());
            fr.mkdirs();
            for (int f = 0; f < 48; f++) {
                BufferedImage im = newImage(GuiLayout.W + 8, GuiLayout.H + 8);
                Graphics2D gg = gfx(im);
                panel(gg, new DjinnGuiPainter(), GuiTheme.of(s, false), state(3, 200 + f * 2f), 4, 4);
                gg.dispose();
                ImageIO.write(im, "png", new File(fr, String.format("f%02d.png", f)));
            }
            // the summon intro
            BufferedImage in = newImage(4 * cw + 8, 2 * ch + 8);
            Graphics2D g2 = gfx(in);
            float[] ages = {1, 5, 9, 13, 17, 21, 25, 29};
            for (int i = 0; i < 8; i++) {
                GuiState st = state(3, 300 + ages[i]);
                st.introAge = ages[i];
                st.phaseAge = ages[i];
                st.prev = 1;
                panel(g2, new DjinnGuiPainter(), GuiTheme.of(s, false), st, 8 + (i % 4) * cw, 8 + (i / 4) * ch);
            }
            g2.dispose();
            ImageIO.write(in, "png", new File(dir, "intro_" + s.id() + ".png"));
        }
    }
}
