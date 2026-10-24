import com.newuniverse.nusmp.client.companion.TalkPainter;
import com.newuniverse.nusmp.client.companion.gs.BondStory;
import com.newuniverse.nusmp.client.companion.gs.BubbleStyle;
import com.newuniverse.nusmp.client.companion.gs.FaceInfo;
import com.newuniverse.nusmp.client.companion.gs.GiftMemory;
import com.newuniverse.nusmp.client.companion.gs.GsCanvas;
import com.newuniverse.nusmp.client.companion.gs.GsHit;
import com.newuniverse.nusmp.client.companion.gs.GsItemRef;
import com.newuniverse.nusmp.client.companion.gs.GsSprite;
import com.newuniverse.nusmp.client.companion.gs.GsTalkFrame;
import com.newuniverse.nusmp.client.companion.gs.GsTalkLayout;
import com.newuniverse.nusmp.client.companion.gs.GsTalkPainter;
import com.newuniverse.nusmp.client.companion.gs.Mood;
import com.newuniverse.nusmp.client.companion.gs.PouchRing;
import com.newuniverse.nusmp.client.companion.gs.TypeWriter;
import com.newuniverse.nusmp.client.ui.gs.GsCompile;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Previews the Great Spirit companion talk screen and its speech bubble without the game (docs/gui_redesign/companion_gui.md). It runs the SAME GsTalkPainter over the SAME GsTalkLayout the game uses, with
 * the REAL texture files of skins B and C (filigree corners, clover frames, ripple, glow) and the real skin of the companion for the portrait, and the real English texts of tools/lang/companions.json. Only
 * the drawing surface is a stand-in: text is a 6 pixel monospace (so widths are approximate), items are coloured tiles with a letter, and the bubble is a drawn mock-up of BubbleStyle's numbers. Run through
 * tools/gui/preview_companion_gs.py; output goes to docs/previews/companions/gs/.
 */
public final class CompanionGsPreview {
    static final int S = 3;
    static final File ROOT = new File(".");
    static final File RES = new File("src/main/resources/assets/nusmp");

    // ================================================================ the lang fragment (real texts)
    static final Map<String, String> LANG = new HashMap<>();

    static void loadLang() throws Exception {
        Pattern p = Pattern.compile("^\\s*\"([^\"]+)\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        for (String line : Files.readAllLines(new File("tools/lang/companions.json").toPath(), StandardCharsets.UTF_8)) {
            Matcher m = p.matcher(line);
            if (m.find()) LANG.put(m.group(1), m.group(2).replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\"));
        }
    }

    static String t(String key, Object... args) {
        String v = LANG.getOrDefault(key, key);
        for (Object a : args) v = v.replaceFirst("%s", Matcher.quoteReplacement(String.valueOf(a)));
        return v;
    }

    // ================================================================ items (stand-ins)
    record Item(String name, int color, char letter, int count) {}

    static final Map<Integer, Item> ITEMS = new HashMap<>();

    static void items() {
        ITEMS.put(GsItemRef.pouch(0), new Item("Bread", 0xC8A060, 'B', 5));
        ITEMS.put(GsItemRef.pouch(1), new Item("Golden Apple", 0xF0C020, 'G', 2));
        ITEMS.put(GsItemRef.pouch(4), new Item("Potion of Healing", 0xE0507A, 'P', 1));
        Item[] hot = {new Item("Sword", 0x9AA8C0, 'S', 1), new Item("Pickaxe", 0x8A7A5A, 'X', 1), new Item("Apple", 0xD03030, 'A', 12), new Item("Bread", 0xC8A060, 'B', 16),
                new Item("Cooked Beef", 0x8A4A2A, 'C', 8), new Item("Cake", 0xF0E0E0, 'K', 1), new Item("Golden Apple", 0xF0C020, 'G', 3), new Item("Rotten Flesh", 0x6A7A3A, 'R', 4),
                new Item("Torch", 0xE0A040, 'T', 32)};
        for (int i = 0; i < 9; i++) ITEMS.put(GsItemRef.inventory(i), hot[i]);
        String[] names = {"Stone", "Dirt", "Oak Log", "Iron Ingot", "Spider Eye", "Cookie", "Sugar", "Milk Bucket", "Diamond"};
        int[] cols = {0x808080, 0x7A5A3A, 0x6A4A28, 0xD0D0D0, 0x7A2A3A, 0xB08040, 0xF0F0F0, 0xA0B0D0, 0x40E0E0};
        for (int i = 0; i < 9; i++) ITEMS.put(GsItemRef.inventory(9 + i), new Item(names[i], cols[i], names[i].charAt(0), 1 + i * 7 % 40));
        ITEMS.put(GsItemRef.inventory(14), new Item("Cake", 0xF0E0E0, 'K', 1));
        ITEMS.put(GsItemRef.taste(0), new Item("Cake", 0xF0E0E0, 'K', 1));
        ITEMS.put(GsItemRef.taste(1), new Item("Rotten Flesh", 0x6A7A3A, 'R', 1));
        ITEMS.put(GsItemRef.taste(2), new Item("Apple", 0xD03030, 'A', 1));
        ITEMS.put(GsItemRef.taste(3), new Item("Spider Eye", 0x7A2A3A, 'E', 1));
        ITEMS.put(GsItemRef.taste(4), new Item("Iron Ingot", 0xD0D0D0, 'I', 1));
    }

    // ================================================================ the canvas
    static final class Canvas implements GsCanvas {
        final BufferedImage img;
        final Graphics2D g;
        final Font plain = new Font(Font.MONOSPACED, Font.BOLD, 8 * S), italic = new Font(Font.MONOSPACED, Font.BOLD | Font.ITALIC, 8 * S);
        final Map<String, BufferedImage> textures = new HashMap<>();
        final BufferedImage face;
        int[] clip;
        final Map<Integer, Item> held;

        Canvas(BufferedImage img, BufferedImage face, Map<Integer, Item> held) {
            this.img = img;
            this.face = face;
            this.held = held;
            g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setFont(plain);
        }

        @Override public void fill(int x1, int y1, int x2, int y2, int argb) {
            g.setColor(new Color(argb, true));
            g.fillRect(x1 * S, y1 * S, (x2 - x1) * S, (y2 - y1) * S);
        }

        @Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
            int H = (y2 - y1) * S;
            for (int r = 0; r < H; r++) {
                float f = H <= 1 ? 0f : r / (float) (H - 1);
                g.setColor(new Color(lerp(top, bottom, f), true));
                g.fillRect(x1 * S, y1 * S + r, (x2 - x1) * S, 1);
            }
        }

        static int lerp(int a, int b, float f) {
            int out = 0;
            for (int sh = 24; sh >= 0; sh -= 8) out |= (int) (((a >>> sh) & 255) * (1 - f) + ((b >>> sh) & 255) * f) << sh;
            return out;
        }

        @Override public void clip(int x1, int y1, int x2, int y2) {
            clip = new int[]{x1 * S, y1 * S, x2 * S, y2 * S};
            g.setClip(x1 * S, y1 * S, (x2 - x1) * S, (y2 - y1) * S);
        }

        @Override public void unclip() {
            clip = null;
            g.setClip((Shape) null);
        }

        @Override public void text(String s, int x, int y, int argb) { draw(s, x, y, argb, plain); }

        void draw(String s, int x, int y, int argb, Font font) {
            g.setFont(font);
            g.setColor(new Color(argb, true));
            for (int i = 0; i < s.length(); i++) g.drawString(String.valueOf(s.charAt(i)), (x + i * 6) * S, y * S + 7 * S);
        }

        @Override public int textWidth(String s) { return s == null ? 0 : s.length() * 6; }

        BufferedImage tex(GsSkin skin, String name) {
            String key = skin + name;
            return textures.computeIfAbsent(key, k -> {
                try {
                    return ImageIO.read(new File(RES, "textures/gui/gs/" + (skin == GsSkin.B ? "b" : "c") + "/" + name + ".png"));
                } catch (Exception e) {
                    return null;
                }
            });
        }

        @Override
        public void sprite(GsSprite s, GsSkin skin, int x, int y, int w, int h, int u, int v, int uw, int vh, int tint, boolean additive) {
            BufferedImage t = switch (s) {
                case CORNER -> tex(skin, "corner");
                case CLOVER3 -> tex(skin, "clover3_frame");
                case CLOVER4 -> tex(skin, "clover4_frame");
                case CLOVER5 -> tex(skin, "clover5_frame");
                case GLOW4 -> tex(skin, "clover4_glow");
                case GLOW5 -> tex(skin, "clover5_glow");
                case DRIPS -> tex(skin, "clover5_drips");
                case RIPPLE -> tex(skin, "ripple");
                default -> face;
            };
            if (t == null) return;
            int W = w * S, H = h * S;
            int ta = tint >>> 24, tr = (tint >> 16) & 255, tg = (tint >> 8) & 255, tb = tint & 255;
            for (int py = 0; py < H; py++) {
                int dy = y * S + py;
                if (dy < 0 || dy >= img.getHeight() || (clip != null && (dy < clip[1] || dy >= clip[3]))) continue;
                int iy = (int) Math.floor(v + (py + 0.5) / H * vh);
                if (iy < 0 || iy >= t.getHeight()) continue;
                for (int px = 0; px < W; px++) {
                    int dx = x * S + px;
                    if (dx < 0 || dx >= img.getWidth() || (clip != null && (dx < clip[0] || dx >= clip[2]))) continue;
                    int ix = (int) Math.floor(u + (px + 0.5) / W * uw);
                    if (ix < 0 || ix >= t.getWidth()) continue;
                    int c = t.getRGB(ix, iy);
                    int a = (c >>> 24) * ta / 255;
                    if (a == 0) continue;
                    int r = ((c >> 16) & 255) * tr / 255, gg = ((c >> 8) & 255) * tg / 255, b = (c & 255) * tb / 255;
                    int d = img.getRGB(dx, dy);
                    int dr = (d >> 16) & 255, dg = (d >> 8) & 255, db = d & 255;
                    if (additive) {
                        dr = Math.min(255, dr + r * a / 255);
                        dg = Math.min(255, dg + gg * a / 255);
                        db = Math.min(255, db + b * a / 255);
                    } else {
                        dr = (r * a + dr * (255 - a)) / 255;
                        dg = (gg * a + dg * (255 - a)) / 255;
                        db = (b * a + db * (255 - a)) / 255;
                    }
                    img.setRGB(dx, dy, 0xFF000000 | (dr << 16) | (dg << 8) | db);
                }
            }
        }

        @Override public void item(int ref, int x, int y) {
            Item it = held.get(ref);
            if (it == null) return;
            g.setColor(new Color(it.color));
            g.fillRect((x + 2) * S, (y + 2) * S, 12 * S, 12 * S);
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect((x + 2) * S, (y + 13) * S, 12 * S, S);
            g.fillRect((x + 13) * S, (y + 2) * S, S, 12 * S);
            g.setColor(new Color(255, 255, 255, 110));
            g.fillRect((x + 2) * S, (y + 2) * S, 12 * S, S);
            g.setFont(plain);
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(String.valueOf(it.letter), (x + 5) * S, (y + 11) * S);
            if (it.count > 1) {
                String n = String.valueOf(it.count);
                g.setColor(Color.BLACK);
                g.drawString(n, (x + 17 - n.length() * 6) * S + S, (y + 16) * S + S);
                g.setColor(Color.WHITE);
                g.drawString(n, (x + 17 - n.length() * 6) * S, (y + 16) * S);
            }
        }

        static int scaleA(int argb, float f) { return (Math.max(0, Math.min(255, (int) ((argb >>> 24) * f))) << 24) | (argb & 0xFFFFFF); }

        @Override
        public void compiled(String[] lines, int count, int x, int y, int lineH, float age, int ink, int settled, int data) {
            int n = Math.min(count, lines.length);
            if (GsCompile.done(age)) {
                for (int i = 0; i < n; i++) text(lines[i], x, y + i * lineH, settled);
                return;
            }
            float p = GsCompile.progress(age), h = n * lineH;
            int step = (int) (age * 0.6f);
            for (int i = 0; i < n; i++) {
                float mix = GsCompile.lineMix(i * lineH, p, h);
                int ly = y + i * lineH;
                switch (GsCompile.layers(mix)) {
                    case 0 -> draw(lines[i], x, ly, ink, italic);
                    case 1 -> text(lines[i], x, ly, settled);
                    default -> {
                        draw(lines[i], x, ly, scaleA(ink, 1f - mix), italic);
                        StringBuilder sb = new StringBuilder();
                        for (int k = 0; k < lines[i].length(); k++) {
                            char ch = lines[i].charAt(k);
                            sb.append(ch == ' ' ? ' ' : GsCompile.scrambled(k + i * 37, step, GsCompile.scrambleFrac(mix)) ? GsCompile.scrambleGlyph(k + i * 37, step) : ch);
                        }
                        text(sb.toString(), x, ly, scaleA(data, Math.min(1f, mix * 1.6f)));
                        text(lines[i], x, ly, scaleA(settled, Math.max(0f, mix * 2f - 1f)));
                    }
                }
            }
        }
    }

    // ================================================================ the scene
    static BufferedImage portraitSkin;
    static FaceInfo faceInfo = FaceInfo.NONE;

    static void loadFace() throws Exception {
        portraitSkin = ImageIO.read(new File(RES, "textures/entity/playermodel/charmy_pappitson.png"));
        int w = portraitSkin.getWidth(), h = portraitSkin.getHeight();
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) argb[y * w + x] = portraitSkin.getRGB(x, y);
        faceInfo = FaceInfo.from(argb, w, h);
    }

    static BufferedImage world(int w, int h) {
        BufferedImage img = new BufferedImage(w * S, h * S, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h * S; y++) for (int x = 0; x < w * S; x++) {
            int n = (x / S * 7 + y / S * 13) % 5;
            img.setRGB(x, y, y < h * S * 6 / 10 ? 0x20304A + n * 0x010101 : 0x2A4A2A + n * 0x010201);
        }
        Graphics2D g = img.createGraphics();                           // the screen's transparent background
        g.setPaint(new GradientPaint(0, 0, new Color(0x10, 0x10, 0x10, 0xC0), 0, h * S, new Color(0x10, 0x10, 0x10, 0xD0)));
        g.fillRect(0, 0, w * S, h * S);
        g.dispose();
        return img;
    }

    /** Everything one preview state needs (mirrors what GsTalkScreen fills when a view arrives). */
    static final class Scene {
        GsSkin skin = GsSkin.B;
        int tab, w = 427, h = 240;
        int rapport = 14;
        Mood mood = Mood.HAPPY;
        float moodAge = 8f;
        int hover = GsHit.NONE;
        boolean picker, typedAll = true, locked;
        int heldIndex = 14;
        float time = 120f, storyAge = 1000f, typeAge = 1000f;
        float rise = 1000f;
        int rows = 3;
        float rippleAt = -1000f;
        int stance = 0;
    }

    static GsTalkFrame frame(Scene sc, Canvas canvas, GsTalkLayout L) {
        GsTalkFrame f = new GsTalkFrame();
        f.skin = sc.skin;
        f.fancy = true;
        f.time = sc.time;
        f.tab = sc.tab;
        f.name = "Charmy Pappitson";
        f.sub = t("nusmp.companion.charmy.sub");
        f.color = 0xF4EAD0;
        String[] tabs = {"talk", "interact", "bond", "pouch"};
        for (int i = 0; i < 4; i++) f.tabNames[i] = t("nusmp.companion.tab." + tabs[i]);
        String[] lb = f.labels;
        lb[GsTalkFrame.L_CLOSE] = t("nusmp.companion.close");
        lb[GsTalkFrame.L_GIVE] = t("nusmp.companion.gs.give");
        lb[GsTalkFrame.L_LIKES] = t("nusmp.companion.gs.likes");
        lb[GsTalkFrame.L_FIVE] = t("nusmp.companion.bond.five");
        lb[GsTalkFrame.L_FOLLOW] = t("nusmp.companion.gs.follow");
        lb[GsTalkFrame.L_WAIT] = t("nusmp.companion.gs.wait");
        lb[GsTalkFrame.L_PANEL] = t("nusmp.companion.gs.panel");
        lb[GsTalkFrame.L_USE_ON] = t("nusmp.companion.gs.use_on");
        lb[GsTalkFrame.L_USE_OFF] = t("nusmp.companion.gs.use_off");
        lb[GsTalkFrame.L_PUT] = t("nusmp.companion.gs.put");
        lb[GsTalkFrame.L_TAKE_ALL] = t("nusmp.companion.gs.take_all");
        lb[GsTalkFrame.L_TASTES] = t("nusmp.companion.gs.tastes");
        lb[GsTalkFrame.L_STORY] = t("nusmp.companion.gs.story");
        lb[GsTalkFrame.L_PICKER] = t("nusmp.companion.gs.picker");
        lb[GsTalkFrame.L_GIFT_HINT] = t("nusmp.companion.gs.gift_hint");
        lb[GsTalkFrame.L_HAND] = t("nusmp.companion.gs.hand");
        lb[GsTalkFrame.L_NO_STORY] = t("nusmp.companion.gs.story.none");
        lb[GsTalkFrame.L_RING_READY] = t("nusmp.companion.gs.ring.ready");
        lb[GsTalkFrame.L_RING_EMPTY] = t("nusmp.companion.gs.ring.empty");
        lb[GsTalkFrame.L_RING_OFF] = t("nusmp.companion.gs.ring.off");
        lb[GsTalkFrame.L_RING_LOCKED] = t("nusmp.companion.gs.ring.locked");
        String[] st = {"guard", "aggressive", "passive"};
        for (int i = 0; i < 3; i++) lb[GsTalkFrame.L_STANCE0 + i] = t("nusmp.companion.stance." + st[i]);
        lb[GsTalkFrame.L_MOOD] = t("nusmp.companion.gs.mood");
        lb[GsTalkFrame.L_NO_TASTES] = t("nusmp.companion.gs.tastes.none");
        lb[GsTalkFrame.L_FOLLOWING] = t("nusmp.companion.gs.state.following");
        lb[GsTalkFrame.L_WAITING] = t("nusmp.companion.gs.state.waiting");
        lb[GsTalkFrame.L_PUT_HINT] = t("nusmp.companion.gs.put_hint");
        String[] moods = {"neutral", "happy", "shy", "angry", "hungry"};
        f.moodName = t("nusmp.companion.gs.mood." + moods[sc.mood.ordinal()]);

        f.face = faceInfo;
        f.mood = sc.mood;
        f.moodAge = sc.moodAge;
        int tier = sc.rapport >= 25 ? 3 : sc.rapport >= 10 ? 2 : sc.rapport >= 3 ? 1 : 0;
        String[] tn = {"stranger", "acquaintance", "friend", "close"};
        f.rapport = sc.rapport;
        f.rapportShown = sc.rapport - (sc.rise < 24 ? 2 * (1f - sc.rise / 24f) : 0f);
        f.rapportGlow = sc.rise < 48 ? (1f - sc.rise / 48f) * (1f - sc.rise / 48f) : 0f;
        f.sinceRise = sc.rise;
        f.litLeaf = -1;
        f.tierName = t("nusmp.companion.tier." + tn[tier]);
        f.tierLine = f.tierName;
        f.rapportText = Integer.toString(sc.rapport);
        int[] at = {3, 10, 25};
        f.toNext = 0;
        for (int a : at) if (sc.rapport < a) {
            f.toNext = a - sc.rapport;
            break;
        }
        f.nextText = f.toNext > 0 ? "+" + f.toNext : "";

        String text = switch (sc.tab) {
            case 0 -> t("nusmp.companion.charmy.greet", "Steve");
            case 1 -> t("nusmp.companion.charmy.a.join");
            case 2 -> t("nusmp.companion.charmy.gift.love", "Cake");
            default -> t("nusmp.companion.bond.pouch.put", "Bread", 3);
        };
        List<String> wrapped = TalkPainter.wrap(canvas, text, L.dialogue[2] - 18, L.dialogueLines);
        f.dialogue = wrapped.toArray(new String[0]);
        int total = TypeWriter.total(f.dialogue);
        f.typed = sc.typedAll ? Integer.MAX_VALUE : TypeWriter.shown(total, sc.typeAge, true);
        f.dialogueDone = sc.typedAll;
        f.hover = sc.hover;
        if (sc.hover != GsHit.NONE) {
            int[] r = L.rectOf(sc.hover, f);
            if (r != null) {
                f.rippleX = r[0] + r[2] / 2f;
                f.rippleY = r[1] + r[3] / 2f;
                f.rippleAt = sc.time - 6f;
            }
        }
        if (sc.rippleAt > -999f) f.rippleAt = sc.rippleAt;

        // rows
        if (sc.tab == 0) {
            f.rows.add(new GsTalkFrame.Row(t("nusmp.companion.charmy.q.self"), false));
            f.rows.add(new GsTalkFrame.Row(t("nusmp.companion.charmy.q.food"), false));
            f.rows.add(new GsTalkFrame.Row(t("nusmp.companion.charmy.q.magic"), false));
        }
        // interact / bond widgets
        f.hasFive = f.hasPanel = f.hasFollow = true;
        f.following = true;
        f.hasGift = f.hasLikes = true;
        f.stanceOffered = true;
        f.stance = sc.stance;
        f.hasPut = f.hasTakeAll = f.hasUse = true;
        f.useIt = 1;
        f.followBlocked = sc.locked;
        f.putBlocked = sc.locked;
        f.heldIndex = sc.heldIndex;
        f.heldCount = 1;
        f.heldReaction = GiftMemory.Reaction.LOVED;
        GiftMemory.Reaction[] tr = {GiftMemory.Reaction.LOVED, GiftMemory.Reaction.HATED, GiftMemory.Reaction.LIKED, GiftMemory.Reaction.HATED, GiftMemory.Reaction.REFUSED};
        for (GiftMemory.Reaction r : tr) f.tasteReactions.add(r);
        long filled = 0;
        for (int i = 0; i < 36; i++) if (i != 20 && i != 27 && i != 31) filled |= 1L << i;
        f.inventoryFilled = filled;
        f.inventoryReactions[5] = GiftMemory.Reaction.LOVED;
        f.inventoryReactions[7] = GiftMemory.Reaction.HATED;
        f.inventoryReactions[2] = GiftMemory.Reaction.LIKED;
        f.inventoryReactions[14] = GiftMemory.Reaction.LOVED;
        f.pickerOpen = sc.picker;
        f.story = new String[]{t("nusmp.companion.gs.ev.loved", "Cake"), t("nusmp.companion.gs.ev.rapport", 2), t("nusmp.companion.gs.ev.five"), t("nusmp.companion.gs.ev.hated", "Spider Eye"), t("nusmp.companion.gs.ev.joined")};
        f.storyKinds = new BondStory.Kind[]{BondStory.Kind.LOVED, BondStory.Kind.RAPPORT, BondStory.Kind.FIVE, BondStory.Kind.HATED, BondStory.Kind.JOINED};
        f.storyAge = sc.storyAge;
        for (int i = 0; i < 9; i++) f.pouchCounts[i] = i == 0 || i == 1 || i == 4 ? "x" : "";
        f.ring = sc.locked ? PouchRing.State.LOCKED : PouchRing.State.READY;
        f.ringSlot = sc.locked ? -1 : 4;
        if (sc.locked) f.useIt = -1;
        f.hint = sc.tab == 2 ? t("nusmp.companion.gs.hint.bond") : "";
        return f;
    }

    static BufferedImage render(Scene sc) {
        BufferedImage img = world(sc.w, sc.h);
        GsTalkLayout L = GsTalkLayout.compute(sc.w, sc.h, sc.tab);
        Map<Integer, Item> held = ITEMS;
        Canvas c = new Canvas(img, portraitSkin, held);
        GsTalkFrame f = frame(sc, c, L);
        GsTalkPainter.paint(c, L, f);
        c.g.dispose();
        return img;
    }

    static void save(BufferedImage img, File dir, String name) throws Exception {
        ImageIO.write(img, "png", new File(dir, name));
    }

    // ================================================================ the bubble mock-up
    static BufferedImage bubbles(GsSkin skin) {
        int w = 330, h = 120;
        BufferedImage img = world(w, h);
        Canvas c = new Canvas(img, portraitSkin, ITEMS);
        Graphics2D g = c.g;
        String line = t("nusmp.companion.charmy.gift.love", "Cake");
        int[] cx = {50, 125, 200, 275};
        float[] age = {2f, 20f, 20f, 20f}, left = {100f, 100f, 4f, 100f};
        boolean[] far = {false, false, false, true};
        String[] caption = {"pops in", "full", "fading", "far: 2 lines"};
        for (int k = 0; k < 4; k++) {
            g.setColor(new Color(0xF4EAD0));
            g.fillRect((cx[k] - 8) * S, 84 * S, 16 * S, 14 * S);                  // a stand-in head
            g.setColor(new Color(0x6A4A8A));
            g.fillRect((cx[k] - 11) * S, 98 * S, 22 * S, 20 * S);
            float sc = BubbleStyle.pop(age[k]), op = BubbleStyle.fade(left[k]);
            List<String> ls = TalkPainter.wrap(c, line + " " + t("nusmp.companion.charmy.a.food"), 110 * 2 / 3, 4);
            int n = BubbleStyle.lines(far[k] ? 20 * 20 : 100, ls.size());
            g.translate(cx[k] * S, 80 * S);
            g.scale(sc, sc);
            g.translate(-cx[k] * S, -80 * S);
            int tail = BubbleStyle.tailColor(skin, op);
            int y0 = 80 - n * 10;
            String name = "Charmy Pappitson";
            int nw = c.textWidth(name);
            c.fill(cx[k] - nw / 2 - 2, y0 - 11, cx[k] + nw / 2 + 2, y0 - 1, BubbleStyle.nameCardColor(skin, op));
            c.draw(name, cx[k] - nw / 2, y0 - 10, BubbleStyle.nameColor(skin, op), c.italic);
            for (int i = 0; i < n; i++) {
                int tw = c.textWidth(ls.get(i));
                c.fill(cx[k] - tw / 2 - 2, y0 + i * 10 - 1, cx[k] + tw / 2 + 2, y0 + i * 10 + 9, BubbleStyle.cardColor(skin, op));
                c.text(ls.get(i), cx[k] - tw / 2, y0 + i * 10, BubbleStyle.textColor(skin, op));
            }
            c.text("v", cx[k] - 3, 80, tail);
            g.setTransform(new java.awt.geom.AffineTransform());
            c.draw(caption[k], cx[k] - caption[k].length() * 3, 108, 0xFF9A9AB0, c.plain);
        }
        c.draw("mock-up of BubbleStyle's numbers, not a game capture", 6, 2, 0xFF9A9AB0, c.plain);
        g.dispose();
        return img;
    }

    // ================================================================ the expression strip
    static BufferedImage moods(GsSkin skin) {
        Mood[] ms = Mood.values();
        int cell = 112, w = cell * ms.length, h = 96;
        BufferedImage strip = new BufferedImage(w * S / 2, h * S / 2 + 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = strip.createGraphics();
        sg.setColor(new Color(12, 12, 16));
        sg.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        for (int i = 0; i < ms.length; i++) {
            Scene sc = new Scene();
            sc.skin = skin;
            sc.tab = 0;
            sc.mood = ms[i];
            sc.moodAge = 6f;
            sc.time = 20f + i * 11f;
            sc.rapport = ms[i] == Mood.SHY ? 1 : 14;
            BufferedImage full = render(sc);
            GsTalkLayout L = GsTalkLayout.compute(sc.w, sc.h, 0);
            int[] p = L.portrait;
            BufferedImage crop = full.getSubimage((p[0] - 6) * S, (p[1] - 4) * S, (p[2] + 12) * S, (p[3] + 6 + 14 + 14 + 28 + 12) * S);
            sg.drawImage(crop, i * cell * S / 2, 0, cell * S / 2, crop.getHeight() * (cell * S / 2) / crop.getWidth(), null);
        }
        sg.dispose();
        return strip;
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "docs/previews/companions/gs");
        out.mkdirs();
        loadLang();
        items();
        loadFace();
        for (GsSkin skin : GsSkin.values()) {
            String k = skin == GsSkin.B ? "b" : "c";
            Scene a = new Scene();
            a.skin = skin;
            a.tab = 0;
            a.hover = GsHit.ROW + 1;
            save(render(a), out, "talk_" + k + "_talk.png");
            Scene b = new Scene();
            b.skin = skin;
            b.tab = 1;
            b.hover = GsHit.FOLLOW;
            b.mood = Mood.NEUTRAL;
            save(render(b), out, "talk_" + k + "_interact.png");
            Scene c = new Scene();
            c.skin = skin;
            c.tab = 2;
            c.hover = GsHit.GIVE;
            c.storyAge = 1000f;
            c.rise = 10f;
            save(render(c), out, "talk_" + k + "_bond.png");
            Scene d = new Scene();
            d.skin = skin;
            d.tab = 3;
            d.hover = GsHit.POUCH;
            d.mood = Mood.NEUTRAL;
            save(render(d), out, "talk_" + k + "_pouch.png");
            Scene e = new Scene();
            e.skin = skin;
            e.tab = 2;
            e.picker = true;
            e.hover = GsHit.PICKER + 14;
            save(render(e), out, "picker_" + k + ".png");
            Scene f = new Scene();
            f.skin = skin;
            f.tab = 2;
            f.storyAge = 14f;
            f.rise = 3f;
            f.mood = Mood.HAPPY;
            save(render(f), out, "compile_" + k + ".png");
            Scene g = new Scene();
            g.skin = skin;
            g.tab = 0;
            g.typedAll = false;
            g.typeAge = 14f;
            g.mood = Mood.SHY;
            g.rapport = 2;
            save(render(g), out, "typing_" + k + ".png");
            Scene h = new Scene();
            h.skin = skin;
            h.tab = 3;
            h.w = 320;
            h.h = 180;
            h.hover = GsHit.HOTBAR + 6;
            save(render(h), out, "small_pouch_" + k + ".png");
            Scene i = new Scene();
            i.skin = skin;
            i.tab = 2;
            i.w = 320;
            i.h = 180;
            save(render(i), out, "small_bond_" + k + ".png");
            Scene j = new Scene();
            j.skin = skin;
            j.tab = 3;
            j.rapport = 1;
            j.locked = true;
            j.mood = Mood.SHY;
            save(render(j), out, "locked_" + k + ".png");
            save(moods(skin), out, "moods_" + k + ".png");
            save(bubbles(skin), out, "bubble_" + k + ".png");
        }
        System.out.println("wrote " + out);
    }
}
