import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Riven Remake's assets (player proportions: 8x8 head, 8x12 body, 4x12 arms and legs), written without Python:
 *
 *     java tools/GenRiven.java
 *
 * writes under src/main/resources/assets/nusmp/:
 *   geo/entity/riven_remake.geo.json, riven_remake_final.geo.json         (final adds the page-wings and the bull-skull crown)
 *   animations/entity/riven_remake.animation.json (+ _final copy)          every clip of the design sheet
 *   textures/entity/riven_remake.png / _glow.png, riven_remake_final.png / _glow.png
 *   textures/item/black_bull_bard_relic.png
 * File frame (see tools/geo_builder.py): pixels, y up, feet at the origin, front on -z, +x the model's left. Animation rotations are
 * degrees added to the bone; positive x rotation swings a limb forward.
 */
public class GenRiven {
    static final Path ROOT = Path.of("src", "main", "resources", "assets", "nusmp");
    static final int TEX = 128;

    interface Painter { int argb(int face, int x, int y, int w, int h); }   // face: 0 top 1 bottom 2 west 3 north(front) 4 east 5 south

    static final class Cube {
        String bone; double ox, oy, oz; int sx, sy, sz; double inflate; double[] rot, pivot; int u, v; Painter color, glow;
    }

    static final class Model {
        final String name; final boolean fin;
        final Map<String, double[]> pivots = new LinkedHashMap<>();
        final Map<String, String> parents = new LinkedHashMap<>();
        final List<Cube> cubes = new ArrayList<>();
        int sx, sy, rowH;
        Model(String name, boolean fin) { this.name = name; this.fin = fin; }

        void bone(String n, String parent, double px, double py, double pz) { pivots.put(n, new double[]{px, py, pz}); parents.put(n, parent); }

        Cube cube(String bone, double ox, double oy, double oz, int sx, int sy, int sz, double inflate, double[] rot, Painter color, Painter glow) {
            Cube c = new Cube();
            c.bone = bone; c.ox = ox; c.oy = oy; c.oz = oz; c.sx = sx; c.sy = sy; c.sz = sz; c.inflate = inflate; c.color = color; c.glow = glow;
            if (rot != null) { c.rot = rot; c.pivot = new double[]{ox + sx / 2.0, oy + sy / 2.0, oz + sz / 2.0}; }
            int w = 2 * (sx + sz), h = sy + sz;
            if (this.sx + w > TEX) { this.sx = 0; this.sy += rowH; rowH = 0; }
            if (this.sy + h > TEX) throw new IllegalStateException("atlas full " + name);
            c.u = this.sx; c.v = this.sy;
            this.sx += w; rowH = Math.max(rowH, h);
            cubes.add(c);
            return c;
        }
    }

    // ================================================================ colours
    static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }
    static int rgba(int r, int g, int b, int a) { return (a << 24) | (r << 16) | (g << 8) | b; }
    static int hash(int a, int b, int c) { int h = a * 73856093 ^ b * 19349663 ^ c * 83492791; h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15; return h & 0x7fffffff; }
    static int shade(int argb, double k) {
        int r = (int) Math.min(255, ((argb >> 16) & 255) * k), g = (int) Math.min(255, ((argb >> 8) & 255) * k), b = (int) Math.min(255, (argb & 255) * k);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
    static final int SKIN = rgb(226, 204, 196), HAIR = rgb(16, 14, 24), HAIR_HI = rgb(52, 42, 96), COAT = rgb(15, 13, 24), COAT_HI = rgb(30, 26, 48),
            SILVER = rgb(176, 182, 208), SHIRT = rgb(44, 30, 72), PANTS = rgb(24, 21, 36), BOOT = rgb(11, 9, 16), VIOLET = rgb(140, 108, 255);

    static double faceShade(int face) { return face == 0 ? 1.12 : face == 1 ? 0.6 : face == 3 ? 1.0 : face == 5 ? 0.82 : 0.9; }

    static Painter solid(int base, double noise) {
        return (f, x, y, w, h) -> shade(base, faceShade(f) * (1 - noise + noise * 2 * (hash(x, y, f) % 100) / 100.0));
    }

    // ---------------------------------------------------------------- head
    static Painter head(boolean fin) {
        return (f, x, y, w, h) -> {
            double k = faceShade(f);
            if (f == 0 || f == 5) return shade((hash(x, y, f) % 7 == 0) ? HAIR_HI : HAIR, k);
            if (f == 1) return shade(SKIN, k);
            boolean side = f == 2 || f == 4;
            if (f == 3) {
                if (y <= 1 || (y == 2 && (x < 2 || x > 5 || hash(x, y, 9) % 3 == 0)) || ((x == 0 || x == 7) && y <= 4)) return shade((hash(x, y, 3) % 5 == 0) ? HAIR_HI : HAIR, k);
                if (y == 3 && (x == 1 || x == 2 || x == 5 || x == 6)) return rgb(20, 16, 24);                  // brows
                if (y == 4 && (x == 1 || x == 6)) return fin ? rgb(120, 96, 255) : rgb(232, 232, 255);        // eye whites
                if (y == 4 && (x == 2 || x == 5)) return rgb(110, 84, 255);                                    // violet-blue irises
                if (y == 6 && (x == 3 || x == 4)) return rgb(150, 92, 104);                                    // mouth
                return shade(SKIN, k * (0.97 + 0.03 * (hash(x, y, 5) % 3)));
            }
            if (side) return y <= 2 || x >= w - 2 && y <= 4 ? shade(HAIR, k) : shade(SKIN, k);
            return 0;
        };
    }
    static Painter headGlow(boolean fin) {
        return (f, x, y, w, h) -> {
            if (f != 3 || y != 4) return 0;
            if (x == 2 || x == 5 || fin && (x == 1 || x == 6)) return rgb(150, 120, 255);
            return 0;
        };
    }

    // ---------------------------------------------------------------- coat pieces
    static Painter coatBody() {
        return (f, x, y, w, h) -> {
            int base = hash(x, y, f) % 11 == 0 ? COAT_HI : COAT;
            boolean edge = x == 0 || x == w - 1;
            if (f == 3) {                                                  // the open front: shirt shows in the centre, silver filigree down both lapels
                if (x >= 3 && x <= 4 && y >= 3) return 0;
                if (x == 2 || x == 5) return SILVER;
                if (y == h - 1) return SILVER;
                if (y % 4 == 1 && (x == 1 || x == 6)) return shade(SILVER, 0.8);
            }
            if (f == 2 || f == 4) { if (y == h - 1 || x == 0 && y > 1) return shade(SILVER, 0.7); }
            if (f == 5) { if (x == w / 2 || x == w / 2 - 1) return shade(SILVER, 0.55); }
            return shade(base, faceShade(f));
        };
    }
    static Painter coatGlow() {
        return (f, x, y, w, h) -> {
            if (f == 3 && (x == 2 || x == 5) && y % 3 != 0) return rgba(140, 108, 255, 150);
            return 0;
        };
    }
    static Painter shirt() { return (f, x, y, w, h) -> shade(hash(x, y, f) % 9 == 0 ? rgb(66, 48, 110) : SHIRT, faceShade(f)); }

    static Painter silverTrimmed(int base, boolean lowerBand) {
        return (f, x, y, w, h) -> {
            if (lowerBand && y >= h - 2 && f >= 2) return shade(SILVER, faceShade(f) * 0.85);
            return shade(hash(x, y, f) % 10 == 0 ? COAT_HI : base, faceShade(f));
        };
    }

    static Painter legs() {
        return (f, x, y, w, h) -> {
            if (f >= 2 && y >= h - 4) return shade(y == h - 4 ? SILVER : (hash(x, y, f) % 8 == 0 ? rgb(28, 24, 40) : BOOT), faceShade(f));
            return shade(hash(x, y, f) % 9 == 0 ? rgb(36, 31, 52) : PANTS, faceShade(f));
        };
    }

    static Painter handSkin() { return (f, x, y, w, h) -> shade(SKIN, faceShade(f) * (0.96 + 0.04 * (hash(x, y, f) % 2))); }

    static final String[] SKULL = {" XX ", "XXXX", "X..X", " XX "};
    static Painter emblem(boolean fin) {
        return (f, x, y, w, h) -> {
            if (f != 3) return rgb(30, 26, 44);
            boolean on = x < 4 && y < 4 && SKULL[y].charAt(x) == 'X';
            return on ? rgb(236, 236, 246) : rgb(20, 17, 32);
        };
    }
    static Painter emblemGlow() {
        return (f, x, y, w, h) -> f == 3 && y == 2 && (x == 1 || x == 2) ? rgb(150, 120, 255) : 0;
    }

    static Painter hairCube() {
        return (f, x, y, w, h) -> shade(hash(x, y, f) % 6 == 0 ? HAIR_HI : HAIR, faceShade(f));
    }

    /** The hair shell over the head: top, back and sides, and a fringe over the forehead; the face stays open. */
    static Painter hairOverlay() {
        return (f, x, y, w, h) -> {
            boolean hair;
            if (f == 0 || f == 5) hair = true;
            else if (f == 1) hair = false;
            else if (f == 3) hair = y <= 1 || (y == 2 && hash(x, y, 9) % 3 == 0) || ((x == 0 || x == w - 1) && y <= 3);
            else hair = y <= 3 || ((x == 0 || x == w - 1) && y <= 5);
            return hair ? shade(hash(x, y, f) % 6 == 0 ? HAIR_HI : HAIR, faceShade(f)) : 0;
        };
    }
    static Painter hairTipGlow() {
        return (f, x, y, w, h) -> (f == 0 && hash(x, y, 4) % 3 == 0) ? rgba(150, 120, 255, 200) : 0;
    }

    static Painter page(boolean glow) {
        return (f, x, y, w, h) -> {
            boolean line = y % 3 == 1 && x > 0 && x < w - 1 && hash(x / 2, y, 8) % 4 != 0;
            if (glow) return line ? rgba(150, 120, 255, 210) : 0;
            if (line) return rgb(112, 84, 190);
            return shade(rgb(236, 226, 204), faceShade(f) * (0.95 + 0.05 * (hash(x, y, 2) % 3)));
        };
    }

    static Painter bone() {
        return (f, x, y, w, h) -> {
            if (hash(x, y, f) % 13 == 0) return shade(rgb(90, 84, 90), faceShade(f));
            return shade(rgb(226, 220, 208), faceShade(f) * (0.94 + 0.06 * (hash(x, y, 3) % 2)));
        };
    }

    // ================================================================ model
    static Model build(boolean fin) {
        Model m = new Model(fin ? "riven_remake_final" : "riven_remake", fin);
        m.bone("root", null, 0, 0, 0);
        m.bone("legR", "root", -2, 12, 0);
        m.bone("legL", "root", 2, 12, 0);
        m.bone("torso", "root", 0, 12, 0);
        m.bone("collar", "torso", 0, 24, 0);
        m.bone("tailBack", "torso", 0, 12, 2);
        m.bone("tailR", "torso", -4.5, 12, 0);
        m.bone("tailL", "torso", 4.5, 12, 0);
        m.bone("head", "torso", 0, 24, 0);
        m.bone("hair", "head", 0, 32, 0);
        m.bone("armR", "torso", -5, 22, 0);
        m.bone("armL", "torso", 5, 22, 0);
        if (fin) {
            m.bone("wingR", "torso", -3, 22, 2.5);
            m.bone("wingL", "torso", 3, 22, 2.5);
            m.bone("crown", "head", 0, 32, 0);
        }
        m.cube("legR", -4, 0, -2, 4, 12, 4, 0, null, legs(), null);
        m.cube("legL", 0, 0, -2, 4, 12, 4, 0, null, legs(), null);
        m.cube("torso", -4, 12, -2, 8, 12, 4, 0, null, shirt(), null);
        m.cube("torso", -4, 12, -2, 8, 12, 4, 0.5, null, coatBody(), coatGlow());
        m.cube("torso", -2, 16, -3, 4, 4, 1, 0, null, emblem(fin), emblemGlow());
        m.cube("collar", -4.5, 24, -2.5, 9, 3, 5, 0, null, silverTrimmed(COAT, false), null);
        m.cube("tailBack", -4.5, 1, 1.5, 9, 11, 1, 0, null, silverTrimmed(COAT, true), null);
        m.cube("tailR", -5, 3, -2.5, 1, 9, 5, 0, null, silverTrimmed(COAT, true), null);
        m.cube("tailL", 4, 3, -2.5, 1, 9, 5, 0, null, silverTrimmed(COAT, true), null);
        m.cube("head", -4, 24, -4, 8, 8, 8, 0, null, head(fin), headGlow(fin));
        m.cube("head", -4, 24, -4, 8, 8, 8, 0.5, null, hairOverlay(), null);
        double[][] tufts = {{-3, 32.5, -2, 3, 3, 3, 10, 0, 15}, {0, 33, -3, 3, 3, 3, -10, 10, -12}, {2, 32, 0, 3, 4, 3, 0, 20, 20},
                {-4, 32, 1, 3, 3, 3, 15, -15, 10}, {-1, 32, 2, 4, 3, 3, 20, 0, -8}};
        for (double[] t : tufts) m.cube("hair", t[0], t[1], t[2], (int) t[3], (int) t[4], (int) t[5], 0, new double[]{t[6], t[7], t[8]}, hairCube(), hairTipGlow());
        m.cube("armR", -8, 12, -2, 4, 12, 4, 0, null, handSkin(), null);
        m.cube("armR", -8, 15, -2, 4, 9, 4, 0.5, null, silverTrimmed(COAT, true), null);
        m.cube("armL", 4, 12, -2, 4, 12, 4, 0, null, handSkin(), null);
        m.cube("armL", 4, 15, -2, 4, 9, 4, 0.5, null, silverTrimmed(COAT, true), null);
        if (fin) {
            double[][] wing = {{12, 1, 7, 35}, {10, 1, 6, 55}, {8, 1, 5, 15}};
            for (double[] w : wing) {
                m.cube("wingL", 3, 22, 2.5, (int) w[0], (int) w[1], (int) w[2], 0, new double[]{0, 0, w[3]}, page(false), page(true));
                m.cube("wingR", -3 - w[0], 22, 2.5, (int) w[0], (int) w[1], (int) w[2], 0, new double[]{0, 0, -w[3]}, page(false), page(true));
            }
            m.cube("crown", 3, 32, -2, 2, 5, 2, 0, new double[]{0, 0, -20}, bone(), null);
            m.cube("crown", -5, 32, -2, 2, 5, 2, 0, new double[]{0, 0, 20}, bone(), null);
            m.cube("crown", -2.5, 33, -4.5, 5, 3, 1, 0, null, bone(), (f, x, y, w, h) -> f == 3 && y == 1 && (x == 1 || x == 3) ? rgb(150, 120, 255) : 0);
        }
        return m;
    }

    // ================================================================ output
    static String num(double d) { return String.format(Locale.ROOT, "%s", d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d)); }
    static String arr(double... v) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < v.length; i++) s.append(i > 0 ? "," : "").append(num(v[i])); return s.append("]").toString(); }

    static String geoJson(Model m) {
        StringBuilder s = new StringBuilder();
        s.append("{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry.").append(m.name)
                .append("\",\"texture_width\":").append(TEX).append(",\"texture_height\":").append(TEX)
                .append(",\"visible_bounds_width\":4,\"visible_bounds_height\":4,\"visible_bounds_offset\":[0,1,0]},\"bones\":[\n");
        boolean firstBone = true;
        for (var e : m.pivots.entrySet()) {
            if (!firstBone) s.append(",\n");
            firstBone = false;
            s.append("{\"name\":\"").append(e.getKey()).append("\"");
            if (m.parents.get(e.getKey()) != null) s.append(",\"parent\":\"").append(m.parents.get(e.getKey())).append("\"");
            s.append(",\"pivot\":").append(arr(e.getValue())).append(",\"cubes\":[");
            boolean firstCube = true;
            for (Cube c : m.cubes) {
                if (!c.bone.equals(e.getKey())) continue;
                if (!firstCube) s.append(",");
                firstCube = false;
                s.append("{\"origin\":").append(arr(c.ox, c.oy, c.oz)).append(",\"size\":").append(arr(c.sx, c.sy, c.sz)).append(",\"uv\":").append(arr(c.u, c.v));
                if (c.inflate != 0) s.append(",\"inflate\":").append(num(c.inflate));
                if (c.rot != null) s.append(",\"rotation\":").append(arr(c.rot)).append(",\"pivot\":").append(arr(c.pivot));
                s.append("}");
            }
            s.append("]}");
        }
        return s.append("\n]}]}\n").toString();
    }

    static void paint(Model m, BufferedImage img, BufferedImage glow) {
        for (Cube c : m.cubes) {
            int[][] rect = {                                  // face -> {x, y, w, h} in the box layout
                    {c.u + c.sz, c.v, c.sx, c.sz}, {c.u + c.sz + c.sx, c.v, c.sx, c.sz}, {c.u, c.v + c.sz, c.sz, c.sy},
                    {c.u + c.sz, c.v + c.sz, c.sx, c.sy}, {c.u + c.sz + c.sx, c.v + c.sz, c.sz, c.sy}, {c.u + 2 * c.sz + c.sx, c.v + c.sz, c.sx, c.sy}};
            for (int f = 0; f < 6; f++) {
                int[] r = rect[f];
                for (int y = 0; y < r[3]; y++) for (int x = 0; x < r[2]; x++) {
                    int col = c.color.argb(f, x, y, r[2], r[3]);
                    if ((col >>> 24) != 0) img.setRGB(r[0] + x, r[1] + y, col);
                    if (c.glow != null) { int g = c.glow.argb(f, x, y, r[2], r[3]); if ((g >>> 24) != 0) glow.setRGB(r[0] + x, r[1] + y, g); }
                }
            }
        }
    }

    // ================================================================ animations
    static final class Anim {
        final StringBuilder out = new StringBuilder("{\"format_version\":\"1.8.0\",\"animations\":{\n");
        boolean firstClip = true;
        StringBuilder cur; boolean firstBone; String clipName;

        void clip(String name, double len, boolean loop) {
            end();
            if (!firstClip) out.append(",\n");
            firstClip = false;
            out.append("\"").append(name).append("\":{\"loop\":").append(loop).append(",\"animation_length\":").append(num(len)).append(",\"bones\":{");
            firstBone = true; clipName = name;
        }
        private boolean open;
        void end() { if (clipName != null) { closeBone(); out.append("}}"); clipName = null; } }
        private String boneName;
        private final List<String> tracks = new ArrayList<>();
        void closeBone() {
            if (boneName == null) return;
            if (!firstBone) out.append(",");
            firstBone = false;
            out.append("\"").append(boneName).append("\":{").append(String.join(",", tracks)).append("}");
            boneName = null; tracks.clear();
        }
        /** kind: rotation / position / scale. frames: "t:x,y,z;t:x,y,z". */
        void t(String bone, String kind, String frames) {
            if (!bone.equals(boneName)) { closeBone(); boneName = bone; }
            StringBuilder s = new StringBuilder("\"" + kind + "\":{");
            String[] fr = frames.split(";");
            for (int i = 0; i < fr.length; i++) {
                String[] kv = fr[i].trim().split(":");
                s.append(i > 0 ? "," : "").append("\"").append(String.format(Locale.ROOT, "%.2f", Double.parseDouble(kv[0]))).append("\":[").append(kv[1]).append("]");
            }
            tracks.add(s.append("}").toString());
        }
        void rot(String bone, String frames) { t(bone, "rotation", frames); }
        void pos(String bone, String frames) { t(bone, "position", frames); }
        void scale(String bone, String frames) { t(bone, "scale", frames); }
        String json() { end(); return out.append("\n}}\n").toString(); }
    }

    static String animations(boolean fin) {
        Anim a = new Anim();
        String flap = "0:0,0,0;0.5:0,0,10;1:0,0,0;1.5:0,0,-6;2:0,0,0";
        a.clip("idle", 4.0, true);
        a.rot("torso", "0:0,0,0;2:2,0,0;4:0,0,0");
        a.rot("head", "0:0,0,0;1:-2,6,0;2:0,0,0;3:2,-6,0;4:0,0,0");
        a.rot("armR", "0:4,0,0;2:-3,0,0;4:4,0,0");
        a.rot("armL", "0:-4,0,0;2:6,0,0;4:-4,0,0");
        a.rot("tailBack", "0:2,0,0;2:6,0,0;4:2,0,0");
        a.rot("hair", "0:0,0,0;1:0,0,5;2:0,0,0;3:0,0,-5;4:0,0,0");
        a.pos("root", "0:0,0,0;2:0,0.4,0;4:0,0,0");
        if (fin) a.rot("wingL", "0:0,0,0;1:0,0,8;2:0,0,0;3:0,0,-5;4:0,0,0");
        if (fin) a.rot("wingR", "0:0,0,0;1:0,0,-8;2:0,0,0;3:0,0,5;4:0,0,0");
        a.clip("walk", 1.0, true);
        a.rot("legR", "0:30,0,0;0.5:-30,0,0;1:30,0,0");
        a.rot("legL", "0:-30,0,0;0.5:30,0,0;1:-30,0,0");
        a.rot("armR", "0:-26,0,0;0.5:26,0,0;1:-26,0,0");
        a.rot("armL", "0:26,0,0;0.5:-26,0,0;1:26,0,0");
        a.rot("torso", "0:0,3,0;0.5:0,-3,0;1:0,3,0");
        a.rot("tailBack", "0:14,0,0;0.5:22,0,0;1:14,0,0");
        a.rot("tailR", "0:-10,0,0;0.5:10,0,0;1:-10,0,0");
        a.rot("tailL", "0:10,0,0;0.5:-10,0,0;1:10,0,0");
        a.rot("hair", "0:6,0,0;0.5:-4,0,0;1:6,0,0");
        a.pos("root", "0:0,0,0;0.25:0,0.8,0;0.5:0,0,0;0.75:0,0.8,0;1:0,0,0");
        a.clip("talk", 1.2, false);
        a.rot("head", "0:0,0,0;0.2:-8,10,0;0.5:4,-8,0;0.9:-6,6,0;1.2:0,0,0");
        a.rot("armR", "0:0,0,0;0.2:50,10,0;0.5:70,-10,0;0.9:40,10,0;1.2:0,0,0");
        a.rot("armL", "0:0,0,0;0.3:25,0,0;0.9:25,0,0;1.2:0,0,0");
        a.rot("torso", "0:0,0,0;0.4:0,-6,0;0.9:0,6,0;1.2:0,0,0");
        a.clip("cast_grimoire", 0.8, false);
        a.rot("armL", "0:0,0,0;0.15:70,0,0;0.6:70,0,0;0.8:20,0,0");
        a.rot("armR", "0:0,0,0;0.2:-30,0,0;0.4:100,-20,0;0.6:110,10,0;0.8:30,0,0");
        a.rot("head", "0:0,0,0;0.2:10,0,0;0.6:6,0,0;0.8:0,0,0");
        a.rot("torso", "0:0,0,0;0.2:-6,0,0;0.45:8,0,0;0.8:0,0,0");
        a.rot("hair", "0:0,0,0;0.3:-20,0,0;0.8:0,0,0");
        a.clip("cast_song", 1.6, false);
        a.rot("armR", "0:0,0,0;0.3:50,0,-50;0.8:70,0,-60;1.3:50,0,-50;1.6:0,0,0");
        a.rot("armL", "0:0,0,0;0.3:50,0,50;0.8:70,0,60;1.3:50,0,50;1.6:0,0,0");
        a.rot("head", "0:0,0,0;0.3:14,0,0;1.3:10,0,0;1.6:0,0,0");
        a.rot("torso", "0:0,0,0;0.4:-6,0,3;0.8:-4,0,-3;1.2:-6,0,3;1.6:0,0,0");
        a.pos("root", "0:0,0,0;0.4:0,1.2,0;0.8:0,0,0;1.2:0,1.2,0;1.6:0,0,0");
        if (fin) a.rot("wingL", flap.replace("1.5", "1.4"));
        if (fin) a.rot("wingR", "0:0,0,0;0.5:0,0,-10;1:0,0,0;1.5:0,0,6;2:0,0,0");
        a.clip("eldritch_blast", 0.45, false);
        a.rot("armR", "0:0,0,0;0.1:95,0,0;0.35:90,0,0;0.45:40,0,0");
        a.rot("armL", "0:0,0,0;0.1:-25,0,0;0.45:0,0,0");
        a.rot("torso", "0:0,0,0;0.1:-12,0,0;0.3:-4,0,0;0.45:0,0,0");
        a.rot("head", "0:0,0,0;0.1:8,0,0;0.45:0,0,0");
        a.clip("shadow_step", 0.35, false);
        a.rot("torso", "0:0,0,0;0.12:24,0,0;0.25:24,0,0;0.35:0,0,0");
        a.pos("root", "0:0,0,0;0.12:0,-3,0;0.25:0,-3,0;0.35:0,0,0");
        a.scale("root", "0:1,1,1;0.2:1,1,1;0.26:0.05,0.05,0.05;0.3:1,1,1;0.35:1,1,1");
        a.clip("manifest_weapon", 0.6, false);
        a.rot("armR", "0:0,0,0;0.2:120,0,-30;0.4:100,0,10;0.6:50,0,0");
        a.rot("armL", "0:0,0,0;0.2:120,0,30;0.4:100,0,-10;0.6:50,0,0");
        a.rot("head", "0:0,0,0;0.2:12,0,0;0.6:0,0,0");
        a.rot("torso", "0:0,0,0;0.2:-8,0,0;0.4:6,0,0;0.6:0,0,0");
        a.clip("manifest_shield", 0.5, false);
        a.rot("armL", "0:0,0,0;0.15:90,0,20;0.35:90,0,0;0.5:40,0,0");
        a.rot("armR", "0:0,0,0;0.2:30,0,-20;0.5:0,0,0");
        a.rot("torso", "0:0,0,0;0.2:0,12,0;0.5:0,0,0");
        a.clip("soul_bond", 0.7, false);
        a.rot("armR", "0:0,0,0;0.2:85,0,0;0.5:85,0,0;0.7:30,0,0");
        a.rot("armL", "0:0,0,0;0.2:40,0,0;0.7:10,0,0");
        a.rot("head", "0:0,0,0;0.2:-12,0,0;0.7:0,0,0");
        a.rot("torso", "0:0,0,0;0.25:8,0,0;0.7:0,0,0");
        for (int n = 1; n <= 3; n++) {
            a.clip("sword_combo_" + n, 0.4, false);
            int sgn = n == 2 ? -1 : 1;
            a.rot("armR", n == 3 ? "0:0,0,0;0.1:-90,0,0;0.25:120,0,0;0.4:30,0,0" : "0:0,0,0;0.1:-40,0," + (sgn * 40) + ";0.22:100,0," + (-sgn * 20) + ";0.4:20,0,0");
            a.rot("armL", "0:0,0,0;0.15:-30,0,0;0.4:0,0,0");
            a.rot("torso", "0:0,0,0;0.1:0," + (-sgn * 25) + ",0;0.22:0," + (sgn * 25) + ",0;0.4:0,0,0");
            a.rot("head", "0:0,0,0;0.1:0," + (sgn * 12) + ",0;0.22:0," + (-sgn * 12) + ",0;0.4:0,0,0");
            a.rot("legR", "0:0,0,0;0.12:22,0,0;0.4:0,0,0");
            a.rot("legL", "0:0,0,0;0.12:-18,0,0;0.4:0,0,0");
        }
        a.clip("hit", 0.3, false);
        a.rot("torso", "0:0,0,0;0.08:-14,0,0;0.3:0,0,0");
        a.rot("head", "0:0,0,0;0.08:-12,0,0;0.3:0,0,0");
        a.rot("armR", "0:0,0,0;0.08:-20,0,-10;0.3:0,0,0");
        a.rot("armL", "0:0,0,0;0.08:-20,0,10;0.3:0,0,0");
        a.clip("stagger", 0.8, false);
        a.rot("torso", "0:0,0,0;0.15:22,0,0;0.6:18,0,5;0.8:0,0,0");
        a.rot("head", "0:0,0,0;0.15:-26,0,0;0.6:-20,0,0;0.8:0,0,0");
        a.rot("armR", "0:0,0,0;0.2:-15,0,-12;0.8:0,0,0");
        a.rot("armL", "0:0,0,0;0.2:-15,0,12;0.8:0,0,0");
        a.rot("legR", "0:0,0,0;0.2:-12,0,0;0.8:0,0,0");
        a.rot("legL", "0:0,0,0;0.2:14,0,0;0.8:0,0,0");
        a.clip("phase2", 1.4, false);
        a.rot("armR", "0:0,0,0;0.4:40,0,-65;1.0:40,0,-65;1.4:0,0,0");
        a.rot("armL", "0:0,0,0;0.4:40,0,65;1.0:40,0,65;1.4:0,0,0");
        a.rot("torso", "0:0,0,0;0.4:-10,0,0;1.0:-10,0,0;1.4:0,0,0");
        a.rot("head", "0:0,0,0;0.4:20,0,0;1.0:20,0,0;1.4:0,0,0");
        a.pos("root", "0:0,0,0;0.5:0,3,0;1.0:0,3,0;1.4:0,0,0");
        a.rot("hair", "0:0,0,0;0.4:-25,0,0;1.0:-25,0,0;1.4:0,0,0");
        a.clip("final_form", 2.2, false);
        a.rot("armR", "0:0,0,0;0.6:30,0,-70;1.6:30,0,-80;2.2:10,0,-10");
        a.rot("armL", "0:0,0,0;0.6:30,0,70;1.6:30,0,80;2.2:10,0,10");
        a.rot("torso", "0:0,0,0;0.6:-14,0,0;1.6:-14,0,0;2.2:-4,0,0");
        a.rot("head", "0:0,0,0;0.6:24,0,0;1.6:26,0,0;2.2:8,0,0");
        a.pos("root", "0:0,0,0;0.8:0,6,0;1.8:0,6,0;2.2:0,1,0");
        a.rot("hair", "0:0,0,0;0.6:-30,0,0;2.2:-12,0,0");
        if (fin) a.rot("wingL", "0:0,0,0;0.6:0,0,25;1.2:0,0,10;1.8:0,0,25;2.2:0,0,8");
        if (fin) a.rot("wingR", "0:0,0,0;0.6:0,0,-25;1.2:0,0,-10;1.8:0,0,-25;2.2:0,0,-8");
        a.clip("death", 2.0, false);
        a.rot("root", "0:0,0,0;0.7:-35,0,0;1.4:-90,0,0;2:-90,0,0");
        a.pos("root", "0:0,0,0;1.4:0,1,0;2:0,1,0");
        a.rot("head", "0:0,0,0;0.7:10,0,0;2:14,0,0");
        a.rot("armR", "0:0,0,0;0.7:-40,0,-30;2:-10,0,-40");
        a.rot("armL", "0:0,0,0;0.7:-40,0,30;2:-10,0,40");
        a.rot("legR", "0:0,0,0;1:8,0,0;2:4,0,0");
        a.rot("legL", "0:0,0,0;1:-6,0,0;2:-2,0,0");
        if (fin) a.rot("wingL", "0:0,0,0;1:0,0,-20;2:0,0,-35");
        if (fin) a.rot("wingR", "0:0,0,0;1:0,0,20;2:0,0,35");
        return a.json();
    }

    // ================================================================ item
    static void relic(Path out) throws IOException {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 1; y < 15; y++) for (int x = 3; x < 13; x++) {
            boolean edge = x == 3 || x == 12 || y == 1 || y == 14;
            im.setRGB(x, y, edge ? rgb(120, 92, 200) : (hash(x, y, 1) % 5 == 0 ? rgb(214, 204, 182) : rgb(238, 228, 206)));
        }
        for (int y = 4; y < 13; y += 2) for (int x = 5; x < 11; x++) if (hash(x, y, 3) % 4 != 0) im.setRGB(x, y, rgb(112, 84, 190));
        for (int[] p : new int[][]{{7, 2}, {8, 2}, {6, 3}, {9, 3}}) im.setRGB(p[0], p[1], rgb(150, 120, 255));
        Files.createDirectories(out.getParent());
        ImageIO.write(im, "png", out.toFile());
    }

    static void write(Path p, String s) throws IOException { Files.createDirectories(p.getParent()); Files.writeString(p, s); }

    public static void main(String[] args) throws IOException {
                for (boolean fin : new boolean[]{false, true}) {
            Model m = build(fin);
            write(ROOT.resolve("geo/entity/" + m.name + ".geo.json"), geoJson(m));
            write(ROOT.resolve("animations/entity/" + m.name + ".animation.json"), animations(fin));
            BufferedImage img = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB), glow = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
            paint(m, img, glow);
            Path tp = ROOT.resolve("textures/entity/" + m.name + ".png");
            Files.createDirectories(tp.getParent());
            ImageIO.write(img, "png", tp.toFile());
            ImageIO.write(glow, "png", ROOT.resolve("textures/entity/" + m.name + "_glow.png").toFile());
            System.out.println(m.name + ": " + m.cubes.size() + " cubes, atlas rows to y=" + (m.sy + m.rowH));
        }
        relic(ROOT.resolve("textures/item/black_bull_bard_relic.png"));
        System.out.println("done");
    }
}
