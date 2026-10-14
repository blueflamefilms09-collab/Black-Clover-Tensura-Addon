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
    /** Quad flags. */
    public static final int EMIT = 1, SHELL = 2, HOT = 4;
    /** Colour slots. */
    public static final int MAIN = 0, TRIM = 1, GLOW = 2, DARK = 3, ACCENT = 4, NONE = 5, SLOTS = 6;

    /** How an animation group moves; its numbers are {@code a}, {@code b}, {@code c} (see {@link WornAnim}). */
    public enum Kind {
        BOB, SPIN, SWING, SWAY, PULSE, WAVE, ORBIT;

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
    public final int emitQuads, shellQuads;

    private WornMesh(String name, WornAnchor anchor, float[] pos, float[] uv, float[] nrm, byte[] slot, byte[] flags, int[] groupStart, Group[] groups) {
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
        int e = 0, s = 0;
        for (byte f : flags) {
            if ((f & EMIT) != 0) e++;
            if ((f & SHELL) != 0) s++;
        }
        this.emitQuads = e;
        this.shellQuads = s;
    }

    /** Vertices of the solid pass (every quad), of the emissive pass and of the shell pass. */
    public int solidVertices() { return quads * 4; }

    public int emitVertices() { return emitQuads * 4; }

    public int shellVertices() { return shellQuads * 4; }

    /** The most vertices the model costs in one frame with all its passes. */
    public int totalVertices() { return solidVertices() + emitVertices(); }

    /**
     * Parses a model file. Unknown record types are ignored (forward compatible); a malformed known record throws {@link IllegalArgumentException} with the line number.
     */
    public static WornMesh parse(String name, Iterable<String> lines) {
        WornAnchor anchor = WornAnchor.HEAD;
        List<Group> groups = new ArrayList<>();
        groups.add(null);
        List<float[]> quadData = new ArrayList<>();
        List<int[]> quadMeta = new ArrayList<>();
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
        return new WornMesh(name, anchor, pos, uv, nrm, slot, flags, start, groups.toArray(new Group[0]));
    }

    private static float f(String s) {
        float x = Float.parseFloat(s);
        if (Float.isNaN(x) || Float.isInfinite(x)) throw new NumberFormatException("not a finite number: " + s);
        return x;
    }
}
