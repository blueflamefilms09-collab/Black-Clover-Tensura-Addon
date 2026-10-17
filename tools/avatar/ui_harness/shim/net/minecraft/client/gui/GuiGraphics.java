package net.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: harness shim of Minecraft's GuiGraphics: draws the same calls into a BufferedImage with Java2D, at an integer zoom, so the Soul Forge screen's own code
 * can be run and looked at without the game. It reproduces what the screen relies on: GUI-pixel coordinates, the pose stack (translate, scale), scissor
 * rectangles that ignore the pose, ARGB fills and gradients, textured blits with a tint from setColor, normal and additive blending, the vertex buffer the
 * dice is drawn into, Minecraft's glyph widths, and the vanilla look of a tooltip. It is NOT the game's renderer: fonts, filtering and depth are approximations.
 */
public class GuiGraphics {
    private final BufferedImage image;
    private final Graphics2D g2;
    private final int zoom;
    private final PoseStack pose = new PoseStack();
    private final Deque<java.awt.Rectangle> scissors = new ArrayDeque<>();
    private float cr = 1f, cg = 1f, cb = 1f, ca = 1f;
    private final List<float[]> quads = new ArrayList<>();           // x0 y0 x1 y1 x2 y2 x3 y3 argb(as float bits)
    private final List<Integer> quadColours = new ArrayList<>();
    private float[] pendingXY = new float[8];
    private int pendingCount, pendingColour;
    private final VertexConsumer consumer = new VertexConsumer() {
        @Override
        public VertexConsumer addVertex(Matrix4f m, float x, float y, float z) {
            Vector3f v = m.transformPosition(x, y, z, new Vector3f());
            if (pendingCount == 4) pendingCount = 0;
            pendingXY[pendingCount * 2] = v.x;
            pendingXY[pendingCount * 2 + 1] = v.y;
            pendingCount++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            if (pendingCount == 4) { quads.add(pendingXY.clone()); quadColours.add(argb); }
            return this;
        }
    };
    private final MultiBufferSource.BufferSource buffers = new MultiBufferSource.BufferSource(consumer);
    private final Map<String, BufferedImage> tintCache = new HashMap<>();
    private final Font fontForTooltip = new Font();

    public GuiGraphics(BufferedImage image, int zoom) {
        this.image = image;
        this.zoom = zoom;
        this.g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    public int guiWidth() { return image.getWidth() / zoom; }

    public int guiHeight() { return image.getHeight() / zoom; }

    public PoseStack pose() { return pose; }

    public MultiBufferSource.BufferSource bufferSource() { return buffers; }

    // ---------------------------------------------------------------- transforms
    private AffineTransform poseTransform() {
        Matrix4f m = pose.last().pose();
        AffineTransform at = new AffineTransform(zoom, 0, 0, zoom, 0, 0);
        at.concatenate(new AffineTransform(m.m00(), m.m01(), m.m10(), m.m11(), m.m30(), m.m31()));
        return at;
    }

    private void applyClip() {
        g2.setTransform(new AffineTransform());
        if (scissors.isEmpty()) { g2.setClip(null); return; }
        java.awt.Rectangle r = null;
        for (java.awt.Rectangle s : scissors) r = r == null ? new java.awt.Rectangle(s) : r.intersection(s);
        g2.setClip(new java.awt.Rectangle(r.x * zoom, r.y * zoom, r.width * zoom, r.height * zoom));
    }

    private Graphics2D begin() {
        applyClip();
        g2.setTransform(poseTransform());
        return g2;
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        scissors.push(new java.awt.Rectangle(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0)));
    }

    public void disableScissor() { if (!scissors.isEmpty()) scissors.pop(); }

    public void setColor(float r, float g, float b, float a) { cr = r; cg = g; cb = b; ca = a; }

    // ---------------------------------------------------------------- shapes
    private static Color color(int argb) {
        int a = (argb >>> 24) & 255;
        if (a == 0) a = 255;                                    // Minecraft treats a colour with no alpha as opaque in text, and fills pass real alpha
        return new Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, a);
    }

    private static Color fillColor(int argb) { return new Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255); }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        if (x0 > x1) { int t = x0; x0 = x1; x1 = t; }
        if (y0 > y1) { int t = y0; y0 = y1; y1 = t; }
        Graphics2D g = begin();
        g.setColor(fillColor(argb));
        g.fillRect(x0, y0, x1 - x0, y1 - y0);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int c1, int c2) {
        if (y1 <= y0 || x1 <= x0) return;
        Graphics2D g = begin();
        int h = y1 - y0;
        int steps = Math.min(h * zoom, 256);
        for (int i = 0; i < steps; i++) {
            float t = (i + 0.5f) / steps;
            int a = Math.round(((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t), r = Math.round(((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t),
                    gr = Math.round(((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t), b = Math.round((c1 & 255) * (1 - t) + (c2 & 255) * t);
            g.setColor(new Color(r, gr, b, a));
            g.fill(new Rectangle2D.Float(x0, y0 + h * (float) i / steps, x1 - x0, h / (float) steps + 0.02f));
        }
    }

    public void renderOutline(int x, int y, int w, int h, int argb) {
        fill(x, y, x + w, y + 1, argb);
        fill(x, y + h - 1, x + w, y + h, argb);
        fill(x, y + 1, x + 1, y + h - 1, argb);
        fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    // ---------------------------------------------------------------- text
    public int drawString(Font font, String s, int x, int y, int argb, boolean shadow) { return drawText(font, s, x, y, argb, shadow); }

    public int drawString(Font font, String s, int x, int y, int argb) { return drawText(font, s, x, y, argb, true); }

    public int drawString(Font font, Component c, int x, int y, int argb) { return drawText(font, c.getString(), x, y, argb, true); }

    public int drawString(Font font, Component c, int x, int y, int argb, boolean shadow) { return drawText(font, c.getString(), x, y, argb, shadow); }

    public void drawCenteredString(Font font, String s, int cx, int y, int argb) { drawText(font, s, cx - font.width(s) / 2, y, argb, true); }

    public void drawCenteredString(Font font, Component c, int cx, int y, int argb) { drawCenteredString(font, c.getString(), cx, y, argb); }

    private static final java.awt.Font AWT = new java.awt.Font("DejaVu Sans", java.awt.Font.PLAIN, 1).deriveFont(8.2f);

    private int drawText(Font font, String s, int x, int y, int argb, boolean shadow) {
        if (s == null || s.isEmpty()) return x;
        Graphics2D g = begin();
        g.setFont(AWT);
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int cw = Font.charWidth(c);
            String one = String.valueOf(c);
            int gw = g.getFontMetrics().stringWidth(one);
            float gx = cx + (cw - 1 - gw) / 2f;
            if (shadow) { g.setColor(new Color(0, 0, 0, Math.min(255, (int) (color(argb).getAlpha() * 0.6)))); g.drawString(one, gx + 1f, y + 7.4f + 1f); }
            g.setColor(color(argb));
            g.drawString(one, gx, y + 7.4f);
            cx += cw;
        }
        return cx;
    }

    // ---------------------------------------------------------------- textures
    private final Map<ResourceLocation, BufferedImage> textures = new HashMap<>();

    private BufferedImage texture(ResourceLocation rl) {
        return textures.computeIfAbsent(rl, k -> {
            try {
                var res = Minecraft.getInstance().getResourceManager().getResource(k);
                if (res.isEmpty()) return null;
                try (var in = res.get().open()) { return javax.imageio.ImageIO.read(in); }
            } catch (Exception e) { return null; }
        });
    }

    /** The 11-argument blit: the region (u, v, uw, vh) of the texture drawn into the box (x, y, w, h), tinted by setColor. */
    public void blit(ResourceLocation tex, int x, int y, int w, int h, float u, float v, int uw, int vh, int tw, int th) {
        BufferedImage src = texture(tex);
        if (src == null || w <= 0 || h <= 0) {
            fill(x, y, x + w, y + h, 0xFFFF00FF);                  // the game's missing texture: loud
            return;
        }
        int su = Math.max(0, Math.round(u * src.getWidth() / tw)), sv = Math.max(0, Math.round(v * src.getHeight() / th));
        int sw = Math.max(1, Math.round(uw * (float) src.getWidth() / tw)), sh = Math.max(1, Math.round(vh * (float) src.getHeight() / th));
        sw = Math.min(sw, src.getWidth() - su);
        sh = Math.min(sh, src.getHeight() - sv);
        BufferedImage part = src.getSubimage(su, sv, sw, sh);
        BufferedImage tinted = tint(part);
        if (!RenderSystem.additive) {
            Graphics2D g = begin();
            g.drawImage(tinted, x, y, w, h, null);
            return;
        }
        // additive: draw into a clear layer (only as big as the box the image lands in) with the same transform and clip, then add that layer to the picture
        java.awt.Rectangle box = poseTransform().createTransformedShape(new java.awt.Rectangle(x, y, w, h)).getBounds();
        box = box.intersection(new java.awt.Rectangle(0, 0, image.getWidth(), image.getHeight()));
        if (box.isEmpty()) return;
        BufferedImage layer = new BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lg = layer.createGraphics();
        lg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        lg.translate(-box.x, -box.y);
        applyClipTo(lg);
        lg.transform(poseTransform());
        lg.drawImage(tinted, x, y, w, h, null);
        lg.dispose();
        int[] add = ((java.awt.image.DataBufferInt) layer.getRaster().getDataBuffer()).getData();
        for (int yy = 0; yy < box.height; yy++) {
            for (int xx = 0; xx < box.width; xx++) {
                int sp = add[yy * box.width + xx];
                int sa = sp >>> 24;
                if (sa == 0) continue;
                int d = image.getRGB(box.x + xx, box.y + yy);
                int r = Math.min(255, ((d >> 16) & 255) + (((sp >> 16) & 255) * sa) / 255);
                int gg = Math.min(255, ((d >> 8) & 255) + (((sp >> 8) & 255) * sa) / 255);
                int b = Math.min(255, (d & 255) + ((sp & 255) * sa) / 255);
                image.setRGB(box.x + xx, box.y + yy, (d & 0xFF000000) | (r << 16) | (gg << 8) | b);
            }
        }
    }

    private void applyClipTo(Graphics2D g) {
        if (scissors.isEmpty()) return;
        java.awt.Rectangle r = null;
        for (java.awt.Rectangle s : scissors) r = r == null ? new java.awt.Rectangle(s) : r.intersection(s);
        g.setClip(new java.awt.Rectangle(r.x * zoom, r.y * zoom, r.width * zoom, r.height * zoom));
    }

    private BufferedImage tint(BufferedImage src) {
        if (cr == 1f && cg == 1f && cb == 1f && ca == 1f) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < src.getHeight(); yy++) {
            for (int xx = 0; xx < src.getWidth(); xx++) {
                int p = src.getRGB(xx, yy);
                int a = Math.round(((p >>> 24) & 255) * ca), r = Math.round(((p >> 16) & 255) * cr), g = Math.round(((p >> 8) & 255) * cg), b = Math.round((p & 255) * cb);
                out.setRGB(xx, yy, (Math.min(255, a) << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- the vertex buffer (the dice)
    public void flush() {
        if (quads.isEmpty()) return;
        applyClip();
        g2.setTransform(new AffineTransform(zoom, 0, 0, zoom, 0, 0));
        for (int i = 0; i < quads.size(); i++) {
            float[] q = quads.get(i);
            Path2D.Float path = new Path2D.Float();
            path.moveTo(q[0], q[1]);
            path.lineTo(q[2], q[3]);
            path.lineTo(q[4], q[5]);
            path.lineTo(q[6], q[7]);
            path.closePath();
            g2.setColor(fillColor(quadColours.get(i)));
            g2.fill(path);
        }
        quads.clear();
        quadColours.clear();
        pendingCount = 0;
    }

    // ---------------------------------------------------------------- tooltips
    public void renderComponentTooltip(Font font, List<Component> lines, int mx, int my) {
        if (lines == null || lines.isEmpty()) return;
        int w = 0;
        for (Component c : lines) w = Math.max(w, font.width(c.getString()));
        int h = lines.size() * (font.lineHeight + 1) + (lines.size() > 1 ? 2 : 0) - 1;
        int x = mx + 12, y = my - 12;
        if (x + w + 8 > guiWidth()) x = Math.max(4, mx - 16 - w);
        if (y + h + 6 > guiHeight()) y = guiHeight() - h - 6;
        if (y < 4) y = 4;
        PoseStack ps = pose();
        ps.pushPose();
        ps.translate(0f, 0f, 400f);
        int bg = 0xF0100010, edge1 = 0x505000FF, edge2 = 0x5028007F;
        fill(x - 3, y - 4, x + w + 3, y - 3, bg);
        fill(x - 3, y + h + 3, x + w + 3, y + h + 4, bg);
        fill(x - 3, y - 3, x + w + 3, y + h + 3, bg);
        fill(x - 4, y - 3, x - 3, y + h + 3, bg);
        fill(x + w + 3, y - 3, x + w + 4, y + h + 3, bg);
        fillGradient(x - 3, y - 3 + 1, x - 3 + 1, y + h + 3 - 1, edge1, edge2);
        fillGradient(x + w + 2, y - 3 + 1, x + w + 3, y + h + 3 - 1, edge1, edge2);
        fill(x - 3, y - 3, x + w + 3, y - 3 + 1, edge1);
        fill(x - 3, y + h + 2, x + w + 3, y + h + 3, edge2);
        int ty = y;
        for (int i = 0; i < lines.size(); i++) {
            Component c = lines.get(i);
            int rgb = c instanceof net.minecraft.network.chat.MutableComponent mc && mc.rgb() >= 0 ? mc.rgb() : 0xFFFFFF;
            drawText(font, c.getString(), x, ty, 0xFF000000 | rgb, true);
            ty += font.lineHeight + 1 + (i == 0 && lines.size() > 1 ? 2 : 0);
        }
        ps.popPose();
    }
}
