import com.newuniverse.nusmp.client.companion.TalkCanvas;
import com.newuniverse.nusmp.client.companion.TalkFrame;
import com.newuniverse.nusmp.client.companion.TalkLayout;
import com.newuniverse.nusmp.client.companion.TalkPainter;

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
 * Previews the companion talk screen without the game (docs/companions/companions.md). It draws the SAME TalkPainter over the SAME TalkLayout the game uses; only the drawing surface is a stand-in
 * (alpha blended rectangles, a monospace font of 6 pixels a character, items as coloured squares with a count), so the layout, colours and states are the real ones while the text metrics are approximate.
 * Run through tools/gui/preview_companion_gui.py (it compiles this file against src/main/java and writes docs/previews/companions/).
 *
 * Output: talk_talk.png, talk_interact.png, talk_bond.png, talk_pouch.png (Charmy, a friend, at the default GUI size 427 x 240), talk_bond_stranger.png (rapport 1, the join and pouch choices closed with
 * their reasons), talk_small.png (a 320 x 180 window), bubble_mock.png (a drawn mock-up of the speech bubble layout, not a game capture).
 */
public final class CompanionTalkPreview {
    static final int S = 3;

    static final class ImgCanvas implements TalkCanvas {
        final Graphics2D g;
        final FontMetrics fm;

        ImgCanvas(Graphics2D g) {
            this.g = g;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8 * S - 2));
            fm = g.getFontMetrics();
        }

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

    static BufferedImage world(int w, int h) {
        BufferedImage img = new BufferedImage(w * S, h * S, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h * S; y++)                              // a stand-in world: dusk sky over grass, so translucency can be judged
            for (int x = 0; x < w * S; x++) {
                int n = (x / S * 7 + y / S * 13) % 5;
                img.setRGB(x, y, y < h * S * 6 / 10 ? 0x20304A + n * 0x010101 : 0x2A4A2A + n * 0x010201);
            }
        return img;
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.SrcOver);
        return g;
    }

    static final String[] TABS = {"Talk", "Interact", "Bond", "Pouch"};
    static final int CREAM = 0xF4EAD0;

    static List<TalkFrame.Row> rows(int tab, boolean stranger) {
        List<TalkFrame.Row> r = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                r.add(new TalkFrame.Row("Who are you?", false));
                r.add(new TalkFrame.Row("What do you like to eat?", false));
                r.add(new TalkFrame.Row("Tell me about your magic.", false));
            }
            case 1 -> {
                r.add(new TalkFrame.Row("Walk with me.", stranger));
                r.add(new TalkFrame.Row("High five", false));
            }
            case 2 -> {
                r.add(new TalkFrame.Row("Give what I am holding", false));
                r.add(new TalkFrame.Row("What do you like?", false));
                r.add(new TalkFrame.Row("Guard: answer whoever hurts you", false));
                r.add(new TalkFrame.Row("Aggressive: go for anything hostile", false));
                r.add(new TalkFrame.Row("Passive: never start a fight", false));
            }
            default -> {
                r.add(new TalkFrame.Row("Put what I am holding in the pouch", stranger));
                if (!stranger) {
                    r.add(new TalkFrame.Row("Take back Bread (x5)", false));
                    r.add(new TalkFrame.Row("Take back Golden Apple (x2)", false));
                    r.add(new TalkFrame.Row("Take everything back", false));
                    r.add(new TalkFrame.Row("Do not let her use the pouch on me", false));
                }
            }
        }
        return r;
    }

    static TalkFrame frame(int tab, boolean stranger, int hoverRow, int hoverTab) {
        int rapport = stranger ? 1 : 14;
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) slots.add(!stranger && (i == 0 || i == 1) ? "x" : "");
        return new TalkFrame("Charmy Pappitson", "Cotton and Food Mage of the Black Bulls", CREAM,
                tab == 2 ? "Thanks! Bread is not bad at all." : "Oh, Steve! Did you bring snacks? ...I mean, hello!", TABS, tab, rows(tab, stranger), 0, hoverRow, hoverTab, false,
                rapport, stranger ? "a stranger" : "a friend", stranger ? "Stance: Guard" : "Stance: Guard   Looks after you: on", 0, stranger ? -1 : 1, slots, -1, "Close", "");
    }

    static void items(Graphics2D g, TalkLayout L, boolean stranger) {
        if (L.tab != 3 || stranger) return;
        int[][] colours = {{0xC8A060, 5}, {0xF0C020, 2}};
        for (int i = 0; i < 2; i++) {
            int[] r = L.slotRect(i);
            g.setColor(new Color(colours[i][0]));
            g.fillRect((r[0] + 3) * S, (r[1] + 3) * S, 10 * S, 10 * S);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(colours[i][1]), (r[0] + 11) * S - 6 * S, (r[1] + 16) * S);
        }
    }

    static void shot(String name, File out, int w, int h, int tab, boolean stranger, int hoverRow, int hoverTab) throws Exception {
        BufferedImage img = world(w, h);
        Graphics2D g = gfx(img);
        TalkLayout L = TalkLayout.compute(w, h, tab);
        TalkPainter.paint(new ImgCanvas(g), L, frame(tab, stranger, hoverRow, hoverTab));
        items(g, L, stranger);
        g.dispose();
        ImageIO.write(img, "png", new File(out, name));
    }

    /** A drawn mock-up of the speech bubble layout: a stand-in figure and the wrapped lines in dark boxes over its head (not a game capture). */
    static void bubble(File out) throws Exception {
        int w = 200, h = 130;
        BufferedImage img = world(w, h);
        Graphics2D g = gfx(img);
        ImgCanvas c = new ImgCanvas(g);
        g.setColor(new Color(0xF4EAD0));
        g.fillRect(90 * S, 70 * S, 20 * S, 12 * S);                  // head
        g.setColor(new Color(0x6A4A8A));
        g.fillRect(86 * S, 82 * S, 28 * S, 34 * S);                  // body
        String[] lines = {"Oh, Steve! Did you bring", "snacks? ...I mean, hello!"};
        for (int i = 0; i < lines.length; i++) {
            int tw = c.textWidth(lines[i]);
            int x = 100 - tw / 2, y = 38 + i * 10;
            c.fill(x - 2, y - 1, x + tw + 2, y + 9, 0xC0101018);
            c.text(lines[i], x, y, 0xFFFFFFFF);
        }
        g.setColor(new Color(1, 1, 1, 0.6f));
        c.text("mock-up: layout of the bubble, not a capture", 6, 120, 0xFF9A9AB0);
        g.dispose();
        ImageIO.write(img, "png", new File(out, "bubble_mock.png"));
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "docs/previews/companions");
        out.mkdirs();
        shot("talk_talk.png", out, 427, 240, 0, false, 1, -1);
        shot("talk_interact.png", out, 427, 240, 1, false, -1, 2);
        shot("talk_bond.png", out, 427, 240, 2, false, 3, -1);
        shot("talk_pouch.png", out, 427, 240, 3, false, 1, -1);
        shot("talk_bond_stranger.png", out, 427, 240, 1, true, -1, -1);
        shot("talk_small.png", out, 320, 180, 2, false, -1, -1);
        bubble(out);
        System.out.println("wrote " + out);
    }
}
