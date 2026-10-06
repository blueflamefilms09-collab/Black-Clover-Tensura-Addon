package com.newuniverse.nusmp.grimoire;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.grimoire.GrimoireModelPlan.PlannedQuad;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Developer tool, not a rule: renders grimoires with a tiny software rasteriser (same quads, same textures, same tints and the
 * model's own display transforms) to PNG contact sheets. Skipped unless GRIMOIRE_PREVIEW_DIR is set:
 * <pre>GRIMOIRE_PREVIEW_DIR=build/preview ./gradlew test --tests '*GrimoirePreviewTest'</pre>
 */
class GrimoirePreviewTest {
    private static final File TEXTURES = new File("src/main/resources/assets/nusmp/textures/item/grimoire3d");
    private static final File MODEL = new File("src/main/resources/assets/nusmp/models/item/grimoire.json");
    private static final Map<GrimoireSprite, BufferedImage> IMAGES = new EnumMap<>(GrimoireSprite.class);

    private record Transform(float[] rot, float[] trans, float[] scale) {}

    private static Transform display(String context) throws Exception {
        try (FileReader r = new FileReader(MODEL)) {
            JsonObject d = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("display").getAsJsonObject(context);
            return new Transform(arr(d, "rotation", 0), arr(d, "translation", 0), arr(d, "scale", 1));
        }
    }

    private static float[] arr(JsonObject o, String k, float def) {
        float[] f = {def, def, def};
        if (o.has(k)) { JsonArray a = o.getAsJsonArray(k); for (int i = 0; i < 3; i++) f[i] = a.get(i).getAsFloat(); }
        return f;
    }

    private static BufferedImage tex(GrimoireSprite s) throws Exception {
        BufferedImage b = IMAGES.get(s);
        if (b == null) { b = ImageIO.read(new File(TEXTURES, s.file + ".png")); IMAGES.put(s, b); }
        return b;
    }

    /** vanilla ItemTransform.apply then translate(-0.5): p' = R*S*(p-0.5) + t,  R = Rx*Ry*Rz (JOML rotationXYZ). */
    private static float[] xform(float x, float y, float z, Transform t) {
        x = (x - 0.5f) * t.scale[0]; y = (y - 0.5f) * t.scale[1]; z = (z - 0.5f) * t.scale[2];
        float rx = (float) Math.toRadians(t.rot[0]), ry = (float) Math.toRadians(t.rot[1]), rz = (float) Math.toRadians(t.rot[2]);
        float x1 = x * (float) Math.cos(rz) - y * (float) Math.sin(rz), y1 = x * (float) Math.sin(rz) + y * (float) Math.cos(rz), z1 = z;   // Rz
        float x2 = x1 * (float) Math.cos(ry) + z1 * (float) Math.sin(ry), z2 = -x1 * (float) Math.sin(ry) + z1 * (float) Math.cos(ry), y2 = y1; // Ry
        float y3 = y2 * (float) Math.cos(rx) - z2 * (float) Math.sin(rx), z3 = y2 * (float) Math.sin(rx) + z2 * (float) Math.cos(rx), x3 = x2;   // Rx
        return new float[]{x3 + t.trans[0] / 16f, y3 + t.trans[1] / 16f, z3 + t.trans[2] / 16f};
    }

    static BufferedImage render(GrimoireAppearance look, Transform t, int size, Color bg) throws Exception { return render(look, t, size, bg, false); }

    static BufferedImage render(GrimoireAppearance look, Transform t, int size, Color bg, boolean held) throws Exception {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(bg); g.fillRect(0, 0, size, size); g.dispose();
        float[] zbuf = new float[size * size];
        java.util.Arrays.fill(zbuf, -1e9f);
        List<PlannedQuad> plan = GrimoireModelPlan.build(look.renderKey().withHeld(held));
        for (PlannedQuad q : plan) {
            float[][] v = new float[4][];
            for (int i = 0; i < 4; i++) v[i] = xform(q.pos()[i * 3], q.pos()[i * 3 + 1], q.pos()[i * 3 + 2], t);
            // screen: x right, y up; GUI +z = towards the viewer
            float area = 0;
            for (int i = 0; i < 4; i++) { float[] a = v[i], b = v[(i + 1) % 4]; area += a[0] * b[1] - b[0] * a[1]; }
            if (area <= 0) continue;                                    // back face (counter-clockwise = front)
            // shade from the transformed normal
            float[] e1 = {v[1][0] - v[0][0], v[1][1] - v[0][1], v[1][2] - v[0][2]}, e2 = {v[3][0] - v[0][0], v[3][1] - v[0][1], v[3][2] - v[0][2]};
            float nx = e1[1] * e2[2] - e1[2] * e2[1], ny = e1[2] * e2[0] - e1[0] * e2[2], nz = e1[0] * e2[1] - e1[1] * e2[0];
            float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nl; ny /= nl; nz /= nl;
            float lamb = Math.max(0, nx * 0.25f + ny * 0.75f + nz * 0.6f) / 1.0f;
            float shade = q.shade() ? Math.min(1f, 0.52f + 0.5f * lamb) : 1f;
            int tint = q.tint() >= 0 ? look.tint(q.tint()) : -1;
            BufferedImage tx = tex(q.sprite());
            int[][] idx = {{0, 1, 2}, {0, 2, 3}};
            for (int[] tri : idx) raster(img, zbuf, size, v[tri[0]], v[tri[1]], v[tri[2]],
                    new float[]{q.uv()[tri[0] * 2], q.uv()[tri[0] * 2 + 1]}, new float[]{q.uv()[tri[1] * 2], q.uv()[tri[1] * 2 + 1]}, new float[]{q.uv()[tri[2] * 2], q.uv()[tri[2] * 2 + 1]},
                    tx, tint, shade, q.emissive());
        }
        return img;
    }

    private static void raster(BufferedImage img, float[] zbuf, int size, float[] a, float[] b, float[] c, float[] ua, float[] ub, float[] uc,
                               BufferedImage tx, int tint, float shade, boolean emissive) {
        float s = size;                                                  // 1 model unit = `size` pixels, centred
        float[] ax = {a[0] * s + s / 2, -a[1] * s + s / 2}, bx = {b[0] * s + s / 2, -b[1] * s + s / 2}, cx = {c[0] * s + s / 2, -c[1] * s + s / 2};
        int minX = Math.max(0, (int) Math.floor(Math.min(ax[0], Math.min(bx[0], cx[0])))), maxX = Math.min(size - 1, (int) Math.ceil(Math.max(ax[0], Math.max(bx[0], cx[0]))));
        int minY = Math.max(0, (int) Math.floor(Math.min(ax[1], Math.min(bx[1], cx[1])))), maxY = Math.min(size - 1, (int) Math.ceil(Math.max(ax[1], Math.max(bx[1], cx[1]))));
        float den = (bx[1] - cx[1]) * (ax[0] - cx[0]) + (cx[0] - bx[0]) * (ax[1] - cx[1]);
        if (Math.abs(den) < 1e-9f) return;
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                float x = px + 0.5f, y = py + 0.5f;
                float w0 = ((bx[1] - cx[1]) * (x - cx[0]) + (cx[0] - bx[0]) * (y - cx[1])) / den;
                float w1 = ((cx[1] - ax[1]) * (x - cx[0]) + (ax[0] - cx[0]) * (y - cx[1])) / den;
                float w2 = 1 - w0 - w1;
                if (w0 < -1e-4f || w1 < -1e-4f || w2 < -1e-4f) continue;
                float z = w0 * a[2] + w1 * b[2] + w2 * c[2];
                if (z < zbuf[py * size + px] - 1e-6f) continue;
                float u = (w0 * ua[0] + w1 * ub[0] + w2 * uc[0]) / 16f, vv = (w0 * ua[1] + w1 * ub[1] + w2 * uc[1]) / 16f;
                int tu = Math.min(tx.getWidth() - 1, Math.max(0, (int) (u * tx.getWidth()))), tv = Math.min(tx.getHeight() - 1, Math.max(0, (int) (vv * tx.getHeight())));
                int argb = tx.getRGB(tu, tv);
                float al = ((argb >>> 24) & 255) / 255f;
                if (al < 0.1f) continue;                                  // the translucent shader discards these
                float r = ((argb >> 16) & 255) / 255f, gg = ((argb >> 8) & 255) / 255f, bb = (argb & 255) / 255f;
                if (tint >= 0) { r *= ((tint >> 16) & 255) / 255f; gg *= ((tint >> 8) & 255) / 255f; bb *= (tint & 255) / 255f; }
                r *= shade; gg *= shade; bb *= shade;
                int dst = img.getRGB(px, py);
                float dr = ((dst >> 16) & 255) / 255f, dg = ((dst >> 8) & 255) / 255f, db = (dst & 255) / 255f;
                float a2 = emissive ? al : 1f;
                int or = clamp(r * a2 + dr * (1 - a2)), og = clamp(gg * a2 + dg * (1 - a2)), ob = clamp(bb * a2 + db * (1 - a2));
                img.setRGB(px, py, 0xFF000000 | (or << 16) | (og << 8) | ob);
                zbuf[py * size + px] = z;
            }
        }
    }

    private static int clamp(float v) { return Math.max(0, Math.min(255, Math.round(v * 255))); }

    private static boolean heldSheet = false;

    private static void sheet(File out, String title, List<GrimoireAppearance> looks, List<String> labels, Transform t, int cell, int cols) throws Exception {
        int rows = (looks.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cell, rows * (cell + 18), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x3A3E46)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i < looks.size(); i++) {
            BufferedImage im = render(looks.get(i), t, cell, new Color(0x4A4F5A), heldSheet);
            int x = (i % cols) * cell, y = (i / cols) * (cell + 18);
            g.drawImage(im, x, y, null);
            g.setColor(Color.WHITE); g.drawString(labels.get(i), x + 4, y + cell + 13);
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out.getPath() + "  (" + title + ")");
    }

    @Test
    void renderContactSheets() throws Exception {
        String dir = System.getenv("GRIMOIRE_PREVIEW_DIR");
        Assumptions.assumeTrue(dir != null && !dir.isBlank(), "set GRIMOIRE_PREVIEW_DIR to render previews");
        File base = new File(dir);
        Transform gui = display("gui");

        // 1) the grimoires the mod really hands out
        List<GrimoireAppearance> real = new ArrayList<>();
        List<String> realLabels = new ArrayList<>();
        MagicType[] mags = {MagicType.FLAME, MagicType.WATER, MagicType.WIND, MagicType.EARTH, MagicType.DARK, MagicType.LIGHT, MagicType.TIME, MagicType.ANTI_MAGIC};
        GrimoireCover[] covers = GrimoireCover.values();
        for (int i = 0; i < covers.length; i++) {
            MagicType m = mags[i % mags.length];
            real.add(GrimoireAppearance.fromCover(covers[i], m));
            realLabels.add(covers[i].name().toLowerCase() + " / " + m.name().toLowerCase());
        }
        sheet(new File(base, "real_covers.png"), "the 12 real covers", real, realLabels, gui, 220, 4);

        heldSheet = true;
        sheet(new File(base, "real_covers_held.png"), "the same covers while held (slight glow)", real, realLabels, gui, 220, 4);
        heldSheet = false;

        // 2) the generator: 12 consecutive-ish ids
        List<GrimoireAppearance> gen = new ArrayList<>();
        List<String> genLabels = new ArrayList<>();
        int[] ids = {0, 1, 2, 3, 4, 5, 6, 7, 51_000, 77_777, 102_998, 102_999};
        for (int id : ids) { gen.add(GrimoireAppearance.fromGrimoireId(id)); genLabels.add("id " + id); }
        sheet(new File(base, "generated_ids.png"), "generated ids", gen, genLabels, gui, 220, 4);

        // 3) showcase: new crests / thickness / clasps
        List<GrimoireAppearance> show = new ArrayList<>();
        List<String> showLabels = new ArrayList<>();
        for (var e : GrimoireShowcase.entries()) { show.add(e.look()); showLabels.add(e.look().insignia().displayName + " " + e.look().thickness().displayName + " " + e.look().clasp().displayName); }
        sheet(new File(base, "showcase.png"), "creative tab showcase", show, showLabels, gui, 220, 4);

        // 4) one grimoire from four angles (front, GUI, back, spine) to prove it is real 3D
        GrimoireAppearance hero = GrimoireAppearance.fromCover(GrimoireCover.FIVE_LEAF, MagicType.ANTI_MAGIC);
        List<GrimoireAppearance> heroes = new ArrayList<>();
        List<String> heroLabels = new ArrayList<>();
        float[][] rots = {{0, 0, 0}, {20, 35, 0}, {20, 150, 0}, {10, -75, 0}, {55, 20, 0}, {-30, 200, 0}};
        String[] names = {"front", "gui", "back-ish", "spine", "from above", "from below"};
        BufferedImage[] imgs = new BufferedImage[rots.length];
        int cell = 220;
        BufferedImage sheet = new BufferedImage(3 * cell, 2 * (cell + 18), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x3A3E46)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i < rots.length; i++) {
            imgs[i] = render(hero, new Transform(rots[i], new float[]{0, 0, 0}, new float[]{1, 1, 1}), cell, new Color(0x4A4F5A));
            g.drawImage(imgs[i], (i % 3) * cell, (i / 3) * (cell + 18), null);
            g.setColor(Color.WHITE); g.drawString("five-leaf anti-magic: " + names[i], (i % 3) * cell + 4, (i / 3) * (cell + 18) + cell + 13);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(base, "angles.png"));
        System.out.println("wrote " + new File(base, "angles.png").getPath());
    }
}
