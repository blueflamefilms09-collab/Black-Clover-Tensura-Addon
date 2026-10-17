package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Why: flat slices are exact but hard to read as a building; the owner asked to SEE the Hub. This is a tiny perspective ray-caster over the blocks of the pure
 * {@link HubModel}: the part of the world a picture needs is baked once into a byte array (one {@code BlockKind} ordinal per block), then every pixel walks a
 * ray through it with the classic voxel traversal (Amanatides and Woo) and takes the colour of the first solid block it meets, lit by the direction of the face it
 * hit and faded into the sky with distance; glass and water are see-through. A "cutaway" predicate removes blocks (half of a tower, for example) so the inside
 * can be seen. It draws flat colours without textures, so it shows form (the spiral, the collars, the pit), not material. A preview tool only: it is in the test
 * sources and the game never loads it.
 */
final class VoxelView {
    private final int x0, y0, z0, sx, sy, sz;
    private final byte[] cells;
    private static final BlockKind[] KINDS = BlockKind.values();

    /** Bakes the box [x0, x0+sx) x [y0, y0+sy) x [z0, z0+sz) of the model. */
    VoxelView(HubModel m, int x0, int y0, int z0, int sx, int sy, int sz) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.cells = new byte[sx * sy * sz];
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        for (int z = 0; z < sz; z++)
            for (int x = 0; x < sx; x++) {
                m.fillColumn(x0 + x, z0 + z, col);
                for (int y = 0; y < sy; y++) cells[(z * sy + y) * sx + x] = (byte) col[y0 + y - HubModel.MIN_Y].ordinal();
            }
    }

    BlockKind at(int x, int y, int z) {
        int lx = x - x0, ly = y - y0, lz = z - z0;
        if (lx < 0 || ly < 0 || lz < 0 || lx >= sx || ly >= sy || lz >= sz) return BlockKind.AIR;
        return KINDS[cells[(lz * sy + ly) * sx + lx]];
    }

    /** Which blocks the picture leaves out (false = keep). */
    interface Clip { boolean cut(int x, int y, int z); }

    private static final double[] FACE = {0.80, 0.62, 1.00, 0.45, 0.72, 0.55};

    /**
     * Renders a perspective picture. The camera looks from {@code eye} at {@code target} with the given vertical field of view (degrees); {@code fog}
     * is the distance at which blocks have faded completely into the sky (0 = no fog).
     */
    BufferedImage render(double[] eye, double[] target, double fovDeg, int w, int h, Clip clip, double fog) {
        double fx = target[0] - eye[0], fy = target[1] - eye[1], fz = target[2] - eye[2];
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl;
        fy /= fl;
        fz /= fl;
        // right = forward x up (world up = +y), then true up = right x forward
        double rx = -fz, ry = 0, rz = fx;
        double rl = Math.sqrt(rx * rx + rz * rz);
        if (rl < 1e-9) { rx = 1; rz = 0; rl = 1; }
        rx /= rl;
        rz /= rl;
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        double tanHalf = Math.tan(Math.toRadians(fovDeg) / 2.0), aspect = (double) w / h;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                double sxn = (2.0 * (px + 0.5) / w - 1.0) * tanHalf * aspect;
                double syn = (1.0 - 2.0 * (py + 0.5) / h) * tanHalf;
                double dx = fx + rx * sxn + ux * syn, dy = fy + ry * sxn + uy * syn, dz = fz + rz * sxn + uz * syn;
                img.setRGB(px, py, trace(eye, dx, dy, dz, clip, fog));
            }
        }
        return img;
    }

    private static int sky(double dy) {
        double t = Math.max(0, Math.min(1, 0.5 + dy * 0.8));
        return mix(0xDCE8F4, 0x6FA0DC, t);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private int trace(double[] eye, double dx, double dy, double dz, Clip clip, double fog) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= len;
        dy /= len;
        dz /= len;
        int x = (int) Math.floor(eye[0]), y = (int) Math.floor(eye[1]), z = (int) Math.floor(eye[2]);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDx = dx == 0 ? 1e30 : Math.abs(1.0 / dx), tDy = dy == 0 ? 1e30 : Math.abs(1.0 / dy), tDz = dz == 0 ? 1e30 : Math.abs(1.0 / dz);
        double tMx = dx == 0 ? 1e30 : (dx > 0 ? (x + 1 - eye[0]) : (eye[0] - x)) * tDx;
        double tMy = dy == 0 ? 1e30 : (dy > 0 ? (y + 1 - eye[1]) : (eye[1] - y)) * tDy;
        double tMz = dz == 0 ? 1e30 : (dz > 0 ? (z + 1 - eye[2]) : (eye[2] - z)) * tDz;
        int face = 2;
        double t = 0;
        double maxT = fog > 0 ? fog : 600;
        int glass = -1;
        double glassMix = 0;
        for (int i = 0; i < 1400 && t < maxT; i++) {
            int lx = x - x0, ly = y - y0, lz = z - z0;
            if (lx >= 0 && ly >= 0 && lz >= 0 && lx < sx && ly < sy && lz < sz && !(clip != null && clip.cut(x, y, z))) {
                BlockKind k = KINDS[cells[(lz * sy + ly) * sx + lx]];
                if (k != BlockKind.AIR) {
                    Color c = MacroPreview.colour(k);
                    if (k == BlockKind.GLASS || k == BlockKind.WATER) {
                        if (glass < 0) { glass = c.getRGB() & 0xFFFFFF; }
                        glassMix = Math.min(0.7, glassMix + 0.3);
                    } else if (k == BlockKind.CHAIN_Y || k == BlockKind.RAIL_LIGHT || k == BlockKind.RAIL_DARK || k == BlockKind.RAIL_CHAIN || k == BlockKind.LANTERN
                            || k == BlockKind.LANTERN_HANGING || k == BlockKind.MOSS_CARPET || k == BlockKind.GLOW_LICHEN) {
                        // thin things: draw them as a 55 percent blend so a railing does not hide the stair behind it
                        glass = c.getRGB() & 0xFFFFFF;
                        glassMix = Math.min(0.55, glassMix + 0.25);
                    } else {
                        double light = FACE[face];
                        // a faint checkerboard so single blocks can be told apart
                        if (((x + y + z) & 1) == 0) light *= 0.97;
                        int rgb = MacroPreview.shaded(c, light).getRGB() & 0xFFFFFF;
                        if (fog > 0) rgb = mix(rgb, sky(dy), Math.min(1.0, Math.pow(t / fog, 1.6)));
                        if (glass >= 0) rgb = mix(rgb, glass, glassMix);
                        return rgb;
                    }
                }
            }
            if (tMx < tMy && tMx < tMz) { x += stepX; t = tMx; tMx += tDx; face = stepX > 0 ? 1 : 0; }
            else if (tMy < tMz) { y += stepY; t = tMy; tMy += tDy; face = stepY > 0 ? 3 : 2; }
            else { z += stepZ; t = tMz; tMz += tDz; face = stepZ > 0 ? 5 : 4; }
            if (y < y0 - 2 && dy < 0) break;
            if (y > y0 + sy + 2 && dy > 0) break;
        }
        int bg = sky(dy);
        return glass >= 0 ? mix(bg, glass, glassMix) : bg;
    }

    /** Convenience for a predicate on x only (cut away everything with x greater than a plane). */
    static Clip cutX(int plane, boolean keepLess) { return (x, y, z) -> keepLess ? x > plane : x < plane; }

    static Clip cutZ(int plane, boolean keepLess) { return (x, y, z) -> keepLess ? z > plane : z < plane; }

    /** Cuts away everything above a height (the roof taken off), so a floor plan of one storey can be seen from an angle. */
    static Clip cutAbove(int y0) { return (x, y, z) -> y > y0; }

    /** Keeps only the rows from lo to hi: one storey taken out of the tower, to be looked at from above as a floor plan. */
    static Clip band(int lo, int hi) { return (x, y, z) -> y < lo || y > hi; }

}
