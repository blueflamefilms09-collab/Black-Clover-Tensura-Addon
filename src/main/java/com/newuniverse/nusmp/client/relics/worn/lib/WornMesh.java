package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * One worn relic model, parsed from assets/nusmp/relic_worn/&lt;name&gt;.txt (written by tools/relics/gen_worn_*.py; the format is described at the top of
 * tools/relics/gen_worn_lib.py). Pure: no Minecraft types, so the files are parsed and checked in tests. The data sits in flat arrays so drawing allocates
 * nothing: quad {@code i} has its four corners at {@code pos[12 i ..]}, {@code uv[8 i ..]}, {@code nrm[12 i ..]}; quads are sorted by animation group, and
 * group {@code g} owns the quads {@code groupStart[g] .. groupStart[g + 1]} (group 0 is the static base).
 * <p>
 * Space: the model's own pixels in the layer space of the player model, relative to the pivot of its anchor part (y down, front is -z).
 */
public final class WornMesh {
    /**
     * Quad flags; every flag has its own bit (a test checks it). The first three are the library's own. The next three belong to the Charms / Genesis visual upgrade models (a model that
     * names its own skin, see {@link #skin}): GLOWMAP = the quad is redrawn additively with the glow map of the model, STAR = the quad belongs to the starfield / nebula pass (scrolling,
     * additive, drawn with the star texture instead of the skin), HALO = the quad belongs to the additive halo / light shaft / portal pass (drawn with the shared fx texture, tinted by its
     * colour slot). The last two belong to the God worn upgrade models (a {@code skin <id>} model, see {@link #tinted}): LIT = the quad also glows where its glow map says so, in the glow
     * slot colour; ADD = the quad is drawn only in the additive pass (a halo, a soft sprite), never in the lit solid pass. An older file never sets any of the new flags.
     */
    public static final int EMIT = 1, SHELL = 2, HOT = 4, GLOWMAP = 8, STAR = 16, HALO = 32, LIT = 64, ADD = 128;
    /** Colour slots. */
    public static final int MAIN = 0, TRIM = 1, GLOW = 2, DARK = 3, ACCENT = 4, NONE = 5, SLOTS = 6;

    /** How an animation group moves; its numbers are {@code a}, {@code b}, {@code c} (see {@link WornAnim}). */
    public enum Kind {
        BOB, SPIN, SWING, SWAY, PULSE, WAVE, ORBIT,
        /** The visual upgrade: turn about all three axes, a degrees a tick about x, b about z, c about y (an orbiting shard, a gyroscope ring). */
        TUMBLE;

        static Kind of(String s) {
            for (Kind k : values()) if (k.name().equalsIgnoreCase(s)) return k;
            throw new IllegalArgumentException("unknown animation kind " + s);
        }
    }

    /** An animation group: a name, how it moves, its pivot, and three numbers. */
    public record Group(String name, Kind kind, float px, float py, float pz, float a, float b, float c) {}

    public final String name;
    public final WornAnchor anchor;
    public final int quads;
    public final float[] pos, uv, nrm;
    public final byte[] slot, flags;
    public final int[] groupStart;
    /** Index 0 is null (the static base). */
    public final Group[] groups;
    public final int emitQuads, shellQuads, glowMapQuads, starQuads, haloQuads;
    /**
     * The visual upgrade: textures of this model under assets/nusmp/textures/ (null for the library models, which share the atlas). {@code skin} is the record {@code skin <v>} as written:
     * a texture path ending in .png (Charms / Genesis models: painted skin, drawn instead of the atlas, no palette tint) or a bare id (God worn models: the skin is
     * {@code relic_worn/god/<id>.png} and its glow map {@code relic_worn/god/<id>_glow.png}, a grey albedo tinted by the palette slots like the atlas, see {@link #tinted}).
     * {@code glowTex} (its emissive map, same UVs), {@code starTex} (the scrolling nebula of the STAR quads) and {@code lodName} (the model drawn instead of this one from far away, at most 15%
     * of the vertices) are the records of the same names.
     */
    public final String skin, glowTex, starTex, lodName;
    private WornMesh lodMesh;

    private WornMesh(String name, WornAnchor anchor, float[] pos, float[] uv, float[] nrm, byte[] slot, byte[] flags, int[] groupStart, Group[] groups) {
        this(name, anchor, pos, uv, nrm, slot, flags, groupStart, groups, null, null, null, null);
    }

    private WornMesh(String name, WornAnchor anchor, float[] pos, float[] uv, float[] nrm, byte[] slot, byte[] flags, int[] groupStart, Group[] groups,
                     String skin, String glowTex, String starTex, String lodName) {
        this.name = name;
        this.anchor = anchor;
        this.quads = slot.length;
        this.pos = pos;
        this.uv = uv;
        this.nrm = nrm;
        this.slot = slot;
        this.flags = flags;
        this.groupStart = groupStart;
        this.groups = groups;
        int e = 0, s = 0, gm = 0, st = 0, ha = 0;
        for (byte f : flags) {
            if ((f & (EMIT | LIT | ADD)) != 0) e++;                   // the quads the lit-glow pass draws (an older file sets only EMIT)
            if ((f & SHELL) != 0) s++;
            if ((f & GLOWMAP) != 0) gm++;
            if ((f & STAR) != 0) st++;
            if ((f & HALO) != 0) ha++;
        }
        this.emitQuads = e;
        this.shellQuads = s;
        this.glowMapQuads = gm;
        this.starQuads = st;
        this.haloQuads = ha;
        this.skin = skin;
        this.glowTex = glowTex;
        this.starTex = starTex;
        this.lodName = lodName;
    }

    /** Does this model carry its own skin (a visual upgrade model) instead of using the shared atlas? */
    public boolean custom() { return skin != null; }

    /** The God worn kind of skin (a bare id in the {@code skin} record): the albedo is grey and tinted by the palette slots like the atlas, and the LIT / ADD quads are drawn from the glow map. */
    public boolean tinted() { return skin != null && !skin.endsWith(".png"); }

    /** The albedo texture under assets/nusmp/textures/, or null for a library model. */
    public String skinPath() { return skin == null ? null : tinted() ? "relic_worn/god/" + skin + ".png" : skin; }

    /** The emissive map under assets/nusmp/textures/: the {@code glowtex} record, else (a tinted skin) {@code <id>_glow.png}, else null. */
    public String glowPath() { return glowTex != null ? glowTex : tinted() ? "relic_worn/god/" + skin + "_glow.png" : null; }

    /**
     * The model to draw from far away: the lod model named by this model's {@code lod} record once {@link WornModels} has read it (it attaches it with {@link #attachLod}), else this one.
     * Pure: the loading is the library's business, so the parser stays free of game classes.
     */
    public WornMesh lod() { return lodMesh == null ? this : lodMesh; }

    /** Called by the library when it has read the lod model this model names. */
    public void attachLod(WornMesh lod) { this.lodMesh = lod; }

    /**
     * The box of the player's second skin layer (hat, jacket, sleeves, pants) round an anchor part, in the part's own layer space: x0, x1, y0, y1, z0, z1. The outer layer is the
     * part's box grown by 0.5 pixel on the head and 0.25 on the body, arms and legs; a worn model that sinks into it is drawn under the skin's layer and seems to vanish.
     */
    public static float[] overlayBox(WornAnchor a) {
        return switch (a) {
            case HEAD -> new float[]{-4.5f, 4.5f, -8.5f, 0.5f, -4.5f, 4.5f};
            case RIGHT_ARM -> new float[]{-3.25f, 1.25f, -2.25f, 10.25f, -2.25f, 2.25f};
            case LEFT_ARM -> new float[]{-1.25f, 3.25f, -2.25f, 10.25f, -2.25f, 2.25f};
            case RIGHT_LEG, LEFT_LEG -> new float[]{-2.25f, 2.25f, -0.25f, 12.25f, -2.25f, 2.25f};
            default -> new float[]{-4.25f, 4.25f, -0.25f, 12.25f, -2.25f, 2.25f};
        };
    }

    /** How far past the skin layer a pushed-out vertex stands (pixels): enough to win the depth test, too little to see. */
    public static final float CLEARANCE = 0.06f;

    /**
     * A copy of this model whose static vertices never stand inside the second skin layer of {@code anchor} once drawn (1.71.0: relics were hidden under jackets, sleeves and
     * hat layers). The model is drawn as {@code translate(dx, dy, dz) * scale(k) * mirrorX?(p)}; a vertex whose drawn place lies inside the layer's box is moved out through the
     * nearest face (plus {@link #CLEARANCE}) and carried back to model space, so a model that already stands clear is returned unchanged (same instance) and one that sinks is
     * changed only where it sinks. Animated groups (orbiting motes, swarms) are left as they are.
     */
    public WornMesh clearOf(WornAnchor a, boolean mirror, float k, float dx, float dy, float dz) {
        if (a == null || k <= 0f) return this;
        float[] b = overlayBox(a);
        float[] np = null;
        int end = groupStart.length > 1 ? groupStart[1] : quads;
        for (int i = 0; i < end * 4; i++) {
            float x = pos[3 * i], y = pos[3 * i + 1], z = pos[3 * i + 2];
            float rx = k * (mirror ? -x : x) + dx, ry = k * y + dy, rz = k * z + dz;
            if (rx <= b[0] || rx >= b[1] || ry <= b[2] || ry >= b[3] || rz <= b[4] || rz >= b[5]) continue;
            float[] push = {rx - b[0], b[1] - rx, ry - b[2], b[3] - ry, rz - b[4], b[5] - rz};
            int best = 0;
            for (int j = 1; j < 6; j++) if (push[j] < push[best]) best = j;
            if (best >= 4) {                                                   // front or back: the faces the eye sees are the ones that must stay visible
                rz = best == 4 ? b[4] - CLEARANCE : b[5] + CLEARANCE;
            } else if (best >= 2) {
                ry = best == 2 ? b[2] - CLEARANCE : b[3] + CLEARANCE;
            } else {
                rx = best == 0 ? b[0] - CLEARANCE : b[1] + CLEARANCE;
            }
            if (np == null) np = pos.clone();
            float qx = (rx - dx) / k, qy = (ry - dy) / k, qz = (rz - dz) / k;
            np[3 * i] = mirror ? -qx : qx;
            np[3 * i + 1] = qy;
            np[3 * i + 2] = qz;
        }
        if (np == null) return this;
        WornMesh moved = new WornMesh(name, anchor, np, uv, nrm, slot, flags, groupStart, groups, skin, glowTex, starTex, lodName);
        moved.lodMesh = lodMesh;
        return moved;
    }

    /**
     * Which corner of a quad to write as the {@code j}-th vertex. A mirrored piece is drawn through a matrix with a negative determinant, which turns every quad inside out for a render
     * type that culls back faces (RenderType.eyes does); writing the corners in the opposite order turns it front again. Not mirrored: the order of the file.
     */
    public static int corner(boolean mirrored, int j) { return mirrored ? 3 - j : j; }

    private float centreY = Float.NaN;

    /** The middle height of the solid shape in the model's own space (layer pixels, y down), without halo and additive-only quads; 0 for an empty model. Computed once. */
    public float centreY() {
        if (!Float.isNaN(centreY)) return centreY;
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int q = 0; q < quads; q++) {
            if ((flags[q] & (HALO | ADD)) != 0) continue;
            for (int v = 0; v < 4; v++) {
                float y = pos[(q * 4 + v) * 3 + 1];
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        centreY = lo > hi ? 0f : (lo + hi) * 0.5f;
        return centreY;
    }

    /** Vertices of the solid pass (every quad), of the emissive pass and of the shell pass. */
    public int solidVertices() { return quads * 4; }

    public int emitVertices() { return emitQuads * 4; }

    public int shellVertices() { return shellQuads * 4; }

    /** The most vertices the model costs in one frame with all its passes. */
    public int totalVertices() { return solidVertices() + emitVertices() + glowMapQuads * 4; }

    /**
     * Parses a model file. Unknown record types are ignored (forward compatible); a malformed known record throws {@link IllegalArgumentException} with the line number.
     */
    public static WornMesh parse(String name, Iterable<String> lines) {
        WornAnchor anchor = WornAnchor.HEAD;
        List<Group> groups = new ArrayList<>();
        groups.add(null);
        List<float[]> quadData = new ArrayList<>();
        List<int[]> quadMeta = new ArrayList<>();
        String skin = null, glowTex = null, starTex = null, lod = null;
        int ln = 0;
        for (String raw : lines) {
            ln++;
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] v = line.split("\\s+");
            try {
                switch (v[0]) {
                    case "anchor" -> {
                        WornAnchor a = WornAnchor.byKey(v[1]);
                        if (a == null) throw new IllegalArgumentException("unknown anchor " + v[1]);
                        anchor = a;
                    }
                    case "skin" -> skin = v[1];
                    case "glowtex" -> glowTex = v[1];
                    case "startex" -> starTex = v[1];
                    case "lod" -> lod = v[1];
                    case "grp" -> {
                        int idx = Integer.parseInt(v[1]);
                        if (idx != groups.size()) throw new IllegalArgumentException("groups must be numbered 1, 2, 3 ... in order");
                        groups.add(new Group(v[2], Kind.of(v[3]), f(v[4]), f(v[5]), f(v[6]), f(v[7]), f(v[8]), f(v[9])));
                    }
                    case "q" -> {
                        if (v.length != 4 + 32) throw new IllegalArgumentException("a quad is q, 3 header numbers and 32 corner numbers; this has " + (v.length - 1) + " after q");
                        int g = Integer.parseInt(v[1]), s = Integer.parseInt(v[2]), fl = Integer.parseInt(v[3]);
                        if (g < 0 || g >= groups.size()) throw new IllegalArgumentException("group " + g + " is not defined before the quad");
                        if (s < 0 || s >= SLOTS) throw new IllegalArgumentException("slot " + s);
                        float[] d = new float[32];
                        for (int i = 0; i < 32; i++) d[i] = f(v[4 + i]);
                        quadData.add(d);
                        quadMeta.add(new int[]{g, s, fl});
                    }
                    default -> { }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException bad) {
                throw new IllegalArgumentException(name + " line " + ln + ": " + bad.getMessage(), bad);
            } catch (IllegalArgumentException bad) {
                throw new IllegalArgumentException(name + " line " + ln + ": " + bad.getMessage(), bad);
            }
        }
        int n = quadData.size();
        int[] order = new int[n];
        int[] start = new int[groups.size() + 1];
        for (int[] m : quadMeta) start[m[0] + 1]++;
        for (int g = 0; g < groups.size(); g++) start[g + 1] += start[g];
        int[] fill = start.clone();
        for (int i = 0; i < n; i++) order[fill[quadMeta.get(i)[0]]++] = i;
        float[] pos = new float[n * 12], uv = new float[n * 8], nrm = new float[n * 12];
        byte[] slot = new byte[n], flags = new byte[n];
        for (int k = 0; k < n; k++) {
            float[] d = quadData.get(order[k]);
            int[] m = quadMeta.get(order[k]);
            for (int c = 0; c < 4; c++) {
                int o = c * 8;
                pos[k * 12 + c * 3] = d[o];
                pos[k * 12 + c * 3 + 1] = d[o + 1];
                pos[k * 12 + c * 3 + 2] = d[o + 2];
                uv[k * 8 + c * 2] = d[o + 3];
                uv[k * 8 + c * 2 + 1] = d[o + 4];
                nrm[k * 12 + c * 3] = d[o + 5];
                nrm[k * 12 + c * 3 + 1] = d[o + 6];
                nrm[k * 12 + c * 3 + 2] = d[o + 7];
            }
            slot[k] = (byte) m[1];
            flags[k] = (byte) m[2];
        }
        return new WornMesh(name, anchor, pos, uv, nrm, slot, flags, start, groups.toArray(new Group[0]), skin, glowTex, starTex, lod);
    }

    private static float f(String s) {
        float x = Float.parseFloat(s);
        if (Float.isNaN(x) || Float.isInfinite(x)) throw new NumberFormatException("not a finite number: " + s);
        return x;
    }
}
