package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Why: nothing about the Hub can be played by the author (the project has no game to run), but the owner wants to SEE it. This is not a test; it is a
 * program with a {@code main} that asks the pure {@link HubModel} what is at every position and writes PNG pictures: vertical slices through the middle of the
 * Spire and the Chasm (x and z), horizontal slices at several heights, a top-down map of the whole Hub, side elevations, the unrolled path of each staircase and
 * a perspective ray-cast of the landmarks from the plaza and from inside. The colours are flat per {@link BlockKind} (no textures), so the pictures show shape,
 * not material; they are what the generator will build, block for block, because they read the very function the generator reads.
 *
 * <p>Run (from the repository root; the commands are also in docs/labyrinth/mega_macro.md):
 * <pre>
 *   javac -d build/macro_preview -sourcepath src/main/java:src/test/java src/test/java/com/newuniverse/nusmp/labyrinth/mega/macro/MacroPreview.java
 *   java -Djava.awt.headless=true -cp build/macro_preview com.newuniverse.nusmp.labyrinth.mega.macro.MacroPreview OUT_DIR [seed] [only]
 * </pre>
 * {@code only} limits the output to file names that contain that text.
 */
public final class MacroPreview {
    private MacroPreview() {}

    static final Map<BlockKind, Color> COLOURS = new EnumMap<>(BlockKind.class);

    static {
        c(BlockKind.AIR, 0xDDE9F5);
        c(BlockKind.GRASS, 0x6BAA3C);
        c(BlockKind.DIRT, 0x8A6540);
        c(BlockKind.STONE, 0x8C8C8C);
        c(BlockKind.DEEPSLATE, 0x4A4A52);
        c(BlockKind.BEDROCK, 0x1E1E1E);
        c(BlockKind.PATH, 0xA3A39A);
        c(BlockKind.POST, 0x777777);
        c(BlockKind.LANTERN, 0xFFC857);
        c(BlockKind.SOUL_LANTERN, 0x4FD6E0);
        c(BlockKind.LANTERN_HANGING, 0xFFC857);
        c(BlockKind.CHAIN_Y, 0x555555);
        c(BlockKind.QUARTZ_BRICKS, 0xE3D9C6);
        c(BlockKind.SMOOTH_QUARTZ, 0xFAF8F4);
        c(BlockKind.CALCITE, 0xCDD0CB);
        c(BlockKind.GOLD_TRIM, 0xF2B91B);
        c(BlockKind.GLASS, 0x9FD8F0);
        c(BlockKind.QUARTZ_PILLAR, 0xE0D9CC);
        c(BlockKind.CHISELED_QUARTZ, 0xD6CFC0);
        c(BlockKind.QUARTZ_SLAB, 0xEFE8DC);
        c(BlockKind.SMOOTH_QUARTZ_SLAB, 0xFFFFFF);
        c(BlockKind.QUARTZ_STAIR_N, 0xEFE8DC);
        c(BlockKind.QUARTZ_STAIR_E, 0xEFE8DC);
        c(BlockKind.QUARTZ_STAIR_S, 0xEFE8DC);
        c(BlockKind.QUARTZ_STAIR_W, 0xEFE8DC);
        c(BlockKind.RAMP_BLOCK, 0x5C5C66);
        c(BlockKind.RAMP_SLAB, 0x7A7A86);
        c(BlockKind.RAIL_LIGHT, 0xE9C46A);
        c(BlockKind.RAIL_DARK, 0x6B2E2E);
        c(BlockKind.RAIL_CHAIN, 0x303030);
        c(BlockKind.BLACKSTONE, 0x2B2730);
        c(BlockKind.BASALT, 0x3C3C42);
        c(BlockKind.COBBLED_DEEPSLATE, 0x56565E);
        c(BlockKind.DRIPSTONE_BLOCK, 0x866B5A);
        c(BlockKind.SCULK, 0x0E3B4A);
        c(BlockKind.MOSS, 0x5B7D2F);
        c(BlockKind.MOSS_CARPET, 0x6C9A35);
        c(BlockKind.GLOW_LICHEN, 0x9BD7A5);
        c(BlockKind.POINTED_DRIPSTONE_UP, 0x9A7B63);
        c(BlockKind.POINTED_DRIPSTONE_DOWN, 0x9A7B63);
        c(BlockKind.WATER, 0x3D6FD6);
        c(BlockKind.GATE_FRAME, 0xFFD23F);
        c(BlockKind.GATE, 0xB44BFF);
        c(BlockKind.ABYSS_FRAME, 0x5B1FA3);
        c(BlockKind.PLATFORM_DARK, 0x38343F);
        c(BlockKind.PLATFORM_DARK_TRIM, 0x4B4558);
        c(BlockKind.DEEPSLATE_TILES, 0x33333A);
        c(BlockKind.POLISHED_DEEPSLATE, 0x424249);
        c(BlockKind.NATIVE, 0xFF00FF);
    }

    private static void c(BlockKind k, int rgb) { COLOURS.put(k, new Color(rgb)); }

    static Color colour(BlockKind k) {
        Color c = COLOURS.get(k);
        return c != null ? c : Color.MAGENTA;
    }

    // ---- drawing helpers

    interface Cells { BlockKind at(int u, int v); }

    /** Draws a rectangle of cells; (u0..u1) goes to the right, (v0..v1) goes UP the picture. */
    static BufferedImage grid(Cells cells, int u0, int u1, int v0, int v1, int scale, String title) {
        int w = (u1 - u0 + 1) * scale, h = (v1 - v0 + 1) * scale;
        int head = 22;
        BufferedImage img = new BufferedImage(w, h + head, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x20242C));
        g.fillRect(0, 0, w, h + head);
        for (int v = v1; v >= v0; v--) {
            for (int u = u0; u <= u1; u++) {
                BlockKind k = cells.at(u, v);
                Color col = colour(k);
                g.setColor(col);
                g.fillRect((u - u0) * scale, head + (v1 - v) * scale, scale, scale);
                if (scale >= 6 && k != BlockKind.AIR) {
                    g.setColor(new Color(0, 0, 0, 40));
                    g.drawRect((u - u0) * scale, head + (v1 - v) * scale, scale - 1, scale - 1);
                }
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString(title + "   (u " + u0 + ".." + u1 + ", v " + v0 + ".." + v1 + ", " + scale + " px per block)", 6, 15);
        g.dispose();
        return img;
    }

    static void save(BufferedImage img, File dir, String name, String only) throws IOException {
        if (only != null && !only.isEmpty() && !name.contains(only)) return;
        File f = new File(dir, name);
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f.getPath() + " (" + img.getWidth() + "x" + img.getHeight() + ")");
    }

    // ---- the pictures

    /** A vertical slice: plane z = fixed (u = x) or plane x = fixed (u = z). */
    static BufferedImage vertical(HubModel m, boolean alongX, int fixed, int u0, int u1, int y0, int y1, int scale, String title) {
        return grid((u, y) -> alongX ? m.kindAt(u, y, fixed) : m.kindAt(fixed, y, u), u0, u1, y0, y1, scale, title);
    }

    /** A horizontal slice at height y (u = x to the right, v = z UP the picture; z grows to the south, so north is down). */
    static BufferedImage plan(HubModel m, int y, int x0, int x1, int z0, int z1, int scale, String title) {
        return grid((x, z) -> m.kindAt(x, y, z), x0, x1, z0, z1, scale, title);
    }

    /** Top-down map: the colour of the highest block, shaded by height, with the roads and landmarks visible. */
    static BufferedImage topDown(HubModel m, int x0, int x1, int z0, int z1, int scale) {
        int w = (x1 - x0 + 1) * scale, h = (z1 - z0 + 1) * scale, head = 22;
        BufferedImage img = new BufferedImage(w, h + head, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x20242C));
        g.fillRect(0, 0, w, h + head);
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        for (int z = z0; z <= z1; z++)
            for (int x = x0; x <= x1; x++) {
                m.fillColumn(x, z, col);
                int top = HubModel.MAX_Y;
                while (top > HubModel.MIN_Y && col[top - HubModel.MIN_Y] == BlockKind.AIR) top--;
                BlockKind k = col[top - HubModel.MIN_Y];
                Color base = colour(k);
                // hillshade from the height difference with the north-west neighbour
                int hn = height(m, x - 1, z - 1);
                double shade = 1.0 + Math.max(-0.35, Math.min(0.35, (top - hn) * 0.10));
                // landmarks are tall: darken the sky-high parts a little less than the ground so the tower reads
                double tall = Math.min(1.0, Math.max(0.0, (top - HubLayout.SURFACE_Y) / 160.0));
                shade += tall * 0.15;
                g.setColor(shaded(base, shade));
                g.fillRect((x - x0) * scale, head + (z - z0) * scale, scale, scale);
            }
        // orientation marks: the plaza's flat disc and the footprint of its template, the three gates, and what is what
        int ox = -x0 * scale, oz = head - z0 * scale;
        g.setColor(new Color(255, 255, 255, 140));
        g.drawOval(ox - HubLayout.PLAZA_RADIUS * scale, oz - HubLayout.PLAZA_RADIUS * scale, 2 * HubLayout.PLAZA_RADIUS * scale, 2 * HubLayout.PLAZA_RADIUS * scale);
        g.drawRect(ox - 16 * scale, oz - 16 * scale, 33 * scale, 33 * scale);
        g.setColor(new Color(255, 255, 255, 200));
        for (int[] gt : new int[][] {{HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Z}, {HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Z}, {HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Z}})
            g.drawOval((gt[0] - x0) * scale - 4, head + (gt[1] - z0) * scale - 4, 8, 8);
        label(g, "PLAZA: safe, buildable (flat disc r 40, template 33 x 33)", ox - 150, oz + HubLayout.PLAZA_RADIUS * scale + 16);
        label(g, "home gate", (HubLayout.HOME_GATE_X - x0) * scale - 24, head + (HubLayout.HOME_GATE_Z - z0) * scale - 8);
        label(g, "HOLY SPIRE: Ascension (+1, +2 ...)", (HubLayout.SPIRE_X - x0) * scale - 110, head + (HubLayout.SPIRE_Z - z0 + HubLayout.SPIRE_BASE_RADIUS + 8) * scale);
        label(g, "ABYSSAL CHASM: Descent (-1, -2 ...)", (HubLayout.CHASM_X - x0) * scale - 110, head + (HubLayout.CHASM_Z - z0 + HubLayout.CHASM_RIM_RADIUS + 8) * scale);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString("Top-down map of the Hub (x " + x0 + ".." + x1 + ", z " + z0 + ".." + z1 + " downwards = south; " + scale + " px per block)", 6, 15);
        g.dispose();
        return img;
    }

    private static int height(HubModel m, int x, int z) { return m.topY(x, z); }

    static Color shaded(Color c, double f) {
        int r = (int) Math.max(0, Math.min(255, c.getRed() * f));
        int gr = (int) Math.max(0, Math.min(255, c.getGreen() * f));
        int b = (int) Math.max(0, Math.min(255, c.getBlue() * f));
        return new Color(r, gr, b);
    }

    private static boolean want(String only, String name) { return only == null || only.isEmpty() || name.contains(only); }

    /** Perspective pictures of the two landmarks (see {@link VoxelView}); a baked box is only built when one of its pictures is wanted. */
    static void renders(HubModel m, File dir, String only) throws IOException {
        int sx = HubLayout.SPIRE_X, sz = HubLayout.SPIRE_Z, cx = HubLayout.CHASM_X, cz = HubLayout.CHASM_Z;
        String[] spire = {"spire_render_exterior", "spire_render_door", "spire_render_cutaway", "spire_render_cutaway_low", "spire_render_shaft_up", "spire_render_crown",
                "spire_render_landing", "spire_render_gallery", "spire_render_gallery_plan"};
        String[] chasm = {"chasm_render_rim", "chasm_render_cutaway", "chasm_render_from_door", "chasm_render_floor", "chasm_render_ramp", "chasm_render_landing",
                "chasm_render_cave_mouth", "chasm_render_caves_cut"};
        boolean wantSpire = false, wantChasm = false;
        for (String n : spire) wantSpire |= want(only, n);
        for (String n : chasm) wantChasm |= want(only, n);
        if (wantSpire) {
            VoxelView v = new VoxelView(m, sx - 110, 55, sz - 110, 220, 180, 220);
            Spiral st = m.spire().stair();
            // from the plaza side, as a player walking up the road sees it
            if (want(only, spire[0])) save(v.render(new double[] {sx - 120, 100, sz + 30}, new double[] {sx, 140, sz}, 62, 900, 700, null, 400), dir, spire[0] + ".png", null);
            if (want(only, spire[1])) save(v.render(new double[] {sx - 62, 72, sz - 12}, new double[] {sx - 40, 72, sz}, 75, 900, 700, null, 0), dir, spire[1] + ".png", null);
            // the south half removed: the inside of the tower seen from the south
            if (want(only, spire[2])) save(v.render(new double[] {sx, 146, sz + 140}, new double[] {sx, 140, sz}, 58, 900, 760, VoxelView.cutZ(sz + 1, true), 0), dir, spire[2] + ".png", null);
            if (want(only, spire[3])) save(v.render(new double[] {sx, 100, sz + 70}, new double[] {sx, 100, sz}, 60, 900, 700, VoxelView.cutZ(sz + 1, true), 0), dir, spire[3] + ".png", null);
            // inside, looking up the shaft from the hall floor
            if (want(only, spire[4])) save(v.render(new double[] {sx + 8, 70, sz + 6}, new double[] {sx, 170, sz}, 80, 800, 900, null, 0), dir, spire[4] + ".png", null);
            // the crown from above
            if (want(only, spire[5])) save(v.render(new double[] {sx + 30, 250, sz + 40}, new double[] {sx, 205, sz}, 60, 900, 700, null, 0), dir, spire[5] + ".png", null);
            // standing on the second landing, looking at the doorway into the trial chambers
            if (want(only, spire[6])) {
                int[] ab = st.landingSteps(48);
                int mid = (ab[0] + ab[1]) / 2;
                double ang = st.path.angleOf(mid) + st.path.dir * 0.5 / st.path.radius(mid), r = st.path.radius(mid);
                double ex = sx + (r - 6.5) * Math.cos(ang - 0.12), ez = sz + (r - 6.5) * Math.sin(ang - 0.12);
                double tx = sx + (r + 4) * Math.cos(ang), tz = sz + (r + 4) * Math.sin(ang);
                save(v.render(new double[] {ex, 68 + 24 + 2.6, ez}, new double[] {tx, 68 + 24 + 1.5, tz}, 80, 900, 700, null, 0), dir, spire[6] + ".png", null);
            }
            // a gallery with the roof taken off: the ring of trial chambers seen from above at an angle
            if (want(only, spire[7])) save(v.render(new double[] {sx + 20, 140, sz + 70}, new double[] {sx, 94, sz}, 55, 900, 700, VoxelView.cutAbove(68 + 24 + 12), 0), dir, spire[7] + ".png", null);
            if (want(only, spire[8])) save(v.render(new double[] {sx + 0.5, 150, sz + 0.6}, new double[] {sx + 0.5, 92, sz + 0.5}, 62, 900, 900, VoxelView.band(68 + 24 - 1, 68 + 24 + 4), 0), dir, spire[8] + ".png", null);
        }
        if (wantChasm) {
            VoxelView v = new VoxelView(m, cx - 110, -60, cz - 110, 220, 160, 220);
            Spiral ramp = m.chasm().ramp();
            if (want(only, chasm[0])) save(v.render(new double[] {cx + 62, 80, cz + 40}, new double[] {cx - 10, 30, cz}, 70, 900, 700, null, 0), dir, chasm[0] + ".png", null);
            if (want(only, chasm[1])) save(v.render(new double[] {cx, 150, cz + 150}, new double[] {cx, -10, cz}, 60, 900, 760, VoxelView.cutZ(cz + 1, true), 0), dir, chasm[1] + ".png", null);
            if (want(only, chasm[2])) save(v.render(new double[] {cx + 54, 71, cz + 2}, new double[] {cx - 30, 44, cz - 6}, 75, 900, 700, null, 0), dir, chasm[2] + ".png", null);
            if (want(only, chasm[3])) save(v.render(new double[] {cx + 20, -38, cz + 6}, new double[] {cx - 10, -44, cz}, 75, 900, 700, null, 0), dir, chasm[3] + ".png", null);
            // the ramp seen from across the pit
            if (want(only, chasm[4])) save(v.render(new double[] {cx - 34, 56, cz - 20}, new double[] {cx + 28, 20, cz + 12}, 80, 900, 700, null, 0), dir, chasm[4] + ".png", null);
            // standing on a landing, and looking into the cave mouth behind it
            int[] lv = m.chasm().landingLevels();
            if (lv.length > 2 && (want(only, chasm[5]) || want(only, chasm[6]))) {
                int cell = m.params().cell();
                int stand = ChasmModel.CAVE_TOP - cell * (lv[1] + 1) + 2;
                int[] ab = ramp.landingSteps(2 * (HubLayout.SURFACE_Y - stand));
                int mid = (ab[0] + ab[1]) / 2;
                double ang = ramp.path.angleOf(mid) + ramp.path.dir * 0.5 / ramp.path.radius(mid), r = ramp.path.radius(mid);
                double ex = cx + (r - 3) * Math.cos(ang), ez = cz + (r - 3) * Math.sin(ang);
                double tx = cx + (r + 12) * Math.cos(ang), tz = cz + (r + 12) * Math.sin(ang);
                if (want(only, chasm[5])) save(v.render(new double[] {cx + (r - 14) * Math.cos(ang + 0.25), stand + 8, cz + (r - 14) * Math.sin(ang + 0.25)}, new double[] {cx + r * Math.cos(ang), stand + 1, cz + r * Math.sin(ang)}, 65, 900, 700, null, 0), dir, chasm[5] + ".png", null);
                if (want(only, chasm[6])) save(v.render(new double[] {ex, stand + 1.6, ez}, new double[] {tx, stand + 1.2, tz}, 85, 900, 700, null, 0), dir, chasm[6] + ".png", null);
            }
            // the rock behind the wall opened by a vertical cut through the rim door's axis, seen from the side
            if (want(only, chasm[7])) save(v.render(new double[] {cx + 20, 20, cz + 105}, new double[] {cx + 20, 20, cz}, 52, 900, 700, VoxelView.cutZ(cz + 30, true), 0), dir, chasm[7] + ".png", null);
        }
    }

    /**
     * The side view of a staircase or ramp, unrolled: the arc of the path runs to the right (one column per block of arc), the height of the walking surface
     * up the picture. Landings are gold, the railing-free final stop light blue, ordinary steps white (a slab step is drawn half a block lower), and a strip under
     * the picture shows the angle of every step round the axis, so the number of turns can be counted.
     */
    static BufferedImage pathProfile(Spiral sp, String title, int yLow, int yHigh, int[] marks, String[] markNames) {
        int stepPx = 2, rowPx = 4, head = 24, strip = 26;
        int w = sp.path.steps() * stepPx + 60, h = (yHigh - yLow + 1) * rowPx + head + strip;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x20242C));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < marks.length; i++) {
            int y = head + (yHigh - marks[i]) * rowPx;
            g.setColor(new Color(255, 255, 255, 50));
            g.drawLine(0, y, w, y);
            label(g, markNames[i], 4, y - 3);
        }
        for (int i = 0; i < sp.path.steps(); i++) {
            int x = 50 + i * stepPx;
            double top = sp.topHalf(i) / 2.0;
            int y = head + (int) ((yHigh + 1 - top) * rowPx);
            Color c = sp.path.isOpen(i) ? new Color(0x9FD8F0) : sp.path.isLanding(i) ? new Color(0xF2B91B) : new Color(0xFAF8F4);
            g.setColor(c);
            g.fillRect(x, y, stepPx, 3 * rowPx);
            if (sp.railed(i)) {
                g.setColor(new Color(0xE9C46A));
                g.fillRect(x, y - 1 * rowPx, stepPx, rowPx);
            }
            double deg = Math.toDegrees(Geo.wrapPi(sp.path.angleOf(i)));
            g.setColor(Color.getHSBColor((float) ((deg + 180.0) / 360.0), 0.8f, 0.9f));
            g.fillRect(x, h - strip + 4, stepPx, 14);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString(title + "   (" + sp.path.steps() + " blocks of arc, " + String.format("%.2f", sp.path.thetaEnd / Geo.TWO_PI) + " turns; strip = compass angle of the step)", 6, 15);
        g.dispose();
        return img;
    }

    /** Draws a label with a dark outline so it reads on any background. */
    static void label(Graphics2D g, String text, int x, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(text, x + 1, y + 1);
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "build/macro_preview_out");
        dir.mkdirs();
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 12345L;
        String only = args.length > 2 ? args[2] : null;
        HubModel m = HubModel.of(seed);
        System.out.println("model built in " + m.buildNanos() / 1_000_000 + " ms, wfc stats (restarts, fallbacks, collapses, backtracks, nanos) = " + java.util.Arrays.toString(m.wfcStats()));
        int sx = HubLayout.SPIRE_X, sz = HubLayout.SPIRE_Z, cx = HubLayout.CHASM_X, cz = HubLayout.CHASM_Z;

        save(topDown(m, -260, 260, -100, 100, 2), dir, "hub_topdown.png", only);

        renders(m, dir, only);
        // the Spire
        save(vertical(m, true, sz, sx - 48, sx + 48, 60, 225, 5, "Spire, vertical slice z=" + sz + " (west left, east right)"), dir, "spire_slice_x.png", only);
        save(vertical(m, false, sx, sz - 48, sz + 48, 60, 225, 5, "Spire, vertical slice x=" + sx), dir, "spire_slice_z.png", only);
        for (int y : new int[] {68, 70, 80, 100, 130, 160, 190, 209, 212, 216})
            save(plan(m, y, sx - 48, sx + 48, sz - 48, sz + 48, 8, "Spire, plan at y=" + y), dir, "spire_plan_y" + y + ".png", only);

        // the Chasm
        int yb = HubLayout.SURFACE_Y - HubLayout.CHASM_DEPTH - 6;
        save(vertical(m, true, cz, cx - 70, cx + 70, yb, 80, 6, "Chasm, vertical slice z=" + cz + " (west left, east right)"), dir, "chasm_slice_x.png", only);
        save(vertical(m, false, cx, cz - 70, cz + 70, yb, 80, 6, "Chasm, vertical slice x=" + cx), dir, "chasm_slice_z.png", only);
        for (int y : new int[] {66, 50, 20, -10, -30, -44, -46})
            save(plan(m, y, cx - 70, cx + 70, cz - 70, cz + 70, 6, "Chasm, plan at y=" + y), dir, "chasm_plan_y" + y + ".png", only);
        // the side view of each road, unrolled
        int[] sm = new int[SpireModel.GALLERIES + 1];
        String[] smn = new String[SpireModel.GALLERIES + 1];
        for (int g = 0; g < SpireModel.GALLERIES; g++) { sm[g] = m.spire().gallery(g).standY(); smn[g] = "gallery " + g + " landing y=" + sm[g]; }
        sm[SpireModel.GALLERIES] = HubLayout.HOLY_GATE_Y - 2;
        smn[SpireModel.GALLERIES] = "crown / Holy gate y=" + sm[SpireModel.GALLERIES];
        save(pathProfile(m.spire().stair(), "Spire stair, side view", HubLayout.SURFACE_Y - 2, HubLayout.HOLY_GATE_Y + 2, sm, smn), dir, "spire_path_side.png", only);
        int[] lv = m.chasm().landingLevels();
        int[] cm = new int[lv.length + 2];
        String[] cmn = new String[lv.length + 2];
        cm[0] = HubLayout.SURFACE_Y;
        cmn[0] = "rim door y=" + cm[0];
        for (int i = 0; i < lv.length; i++) { cm[i + 1] = ChasmModel.CAVE_TOP - m.params().cell() * (lv[i] + 1) + 2; cmn[i + 1] = "landing " + (i + 1) + " + cave mouth, y=" + cm[i + 1]; }
        cm[lv.length + 1] = ChasmModel.FLOOR_STAND;
        cmn[lv.length + 1] = "floor / Abyssal gate y=" + cm[lv.length + 1];
        save(pathProfile(m.chasm().ramp(), "Chasm ramp, side view", ChasmModel.FLOOR_STAND - 2, HubLayout.SURFACE_Y + 4, cm, cmn), dir, "chasm_path_side.png", only);
    }
}
