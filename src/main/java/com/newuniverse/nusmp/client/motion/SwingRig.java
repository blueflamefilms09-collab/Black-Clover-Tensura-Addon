package com.newuniverse.nusmp.client.motion;

/**
 * The swing rig of one weapon mesh, read from the two directives tools/weaponmotion/build_swing.py writes into the mesh file (docs/vassal_forms_v2.md):
 * <pre>
 * bone &lt;i&gt; &lt;parent&gt; &lt;px py pz&gt; &lt;ax ay az&gt; &lt;bx by bz&gt; &lt;gain&gt; &lt;w&gt; &lt;zeta&gt; &lt;maxDeg&gt;
 * skin &lt;layer&gt; &lt;first vertex&gt; &lt;count&gt; &lt;code&gt; ...        code = bone * 64 + round(63 * weight towards bone + 1)
 * </pre>
 * Pure data and parsing (no Minecraft class), so a test can read every mesh file. The bones form one chain (parent = i - 1); a vertex follows the blend of its bone and the next one.
 * Parsing is lenient about nothing: {@link #valid} says whether the rig is safe to draw, and the renderer drops an invalid rig and draws the mesh rigid.
 */
public final class SwingRig {
    public static final int LAYERS = 3;

    public int n;
    public final float[] px = new float[SwingMath.MAX_BONES], py = new float[SwingMath.MAX_BONES], pz = new float[SwingMath.MAX_BONES];
    public final float[] ax = new float[SwingMath.MAX_BONES], ay = new float[SwingMath.MAX_BONES], az = new float[SwingMath.MAX_BONES];
    public final float[] bx = new float[SwingMath.MAX_BONES], by = new float[SwingMath.MAX_BONES], bz = new float[SwingMath.MAX_BONES];
    public final float[] gain = new float[SwingMath.MAX_BONES], w = new float[SwingMath.MAX_BONES], zeta = new float[SwingMath.MAX_BONES], maxRad = new float[SwingMath.MAX_BONES];
    /** Per layer: the index of the first skinned vertex, and the code of every vertex from there on (null = the layer is rigid). */
    public final int[] skinFrom = new int[LAYERS];
    public final short[][] skin = new short[LAYERS][];
    private boolean broken;

    /** One {@code bone} line (the tokens after the keyword split already: {@code v[0]} is "bone"). */
    public void addBone(String[] v) {
        try {
            int i = Integer.parseInt(v[1]);
            if (i != n || i >= SwingMath.MAX_BONES || v.length < 16 || Integer.parseInt(v[2]) != i - 1) {
                broken = true;
                return;
            }
            float[] f = new float[13];
            for (int k = 0; k < 13; k++) f[k] = Float.parseFloat(v[3 + k]);
            px[i] = f[0]; py[i] = f[1]; pz[i] = f[2];
            ax[i] = f[3]; ay[i] = f[4]; az[i] = f[5];
            bx[i] = f[6]; by[i] = f[7]; bz[i] = f[8];
            gain[i] = f[9]; w[i] = f[10]; zeta[i] = f[11];
            maxRad[i] = (float) Math.toRadians(f[12]);
            n = i + 1;
        } catch (RuntimeException e) {
            broken = true;
        }
    }

    /** One {@code skin} line. */
    public void addSkin(String[] v) {
        try {
            int layer = Integer.parseInt(v[1]);
            int from = Integer.parseInt(v[2]);
            int count = Integer.parseInt(v[3]);
            if (layer < 0 || layer >= LAYERS || count < 0 || from < 0 || v.length != 4 + count || skin[layer] != null) {
                broken = true;
                return;
            }
            short[] codes = new short[count];
            for (int k = 0; k < count; k++) codes[k] = (short) Integer.parseInt(v[4 + k]);
            skinFrom[layer] = from;
            skin[layer] = codes;
        } catch (RuntimeException e) {
            broken = true;
        }
    }

    public static int bone(short code) {
        return code >> 6;
    }

    /** Weight (0..1) of the next bone in a vertex's blend. */
    public static float weight(short code) {
        return (code & 63) / 63f;
    }

    /**
     * Whether the rig can be drawn: at least one bone, every code names a bone (and the next one when it blends), every skinned layer ends exactly where the layer ends
     * ({@code vertexCount[layer]}), every bone has a non-degenerate axis, a positive frequency and a limit.
     */
    public boolean valid(int[] vertexCount) {
        if (broken || n < 1) return false;
        for (int i = 0; i < n; i++) {
            if (len(ax[i], ay[i], az[i]) < 0.5f || len(bx[i], by[i], bz[i]) < 0.5f || !(w[i] > 0f) || !(zeta[i] >= 0f) || !(maxRad[i] > 0f) || !(gain[i] >= 0f)) return false;
        }
        boolean any = false;
        for (int l = 0; l < LAYERS; l++) {
            short[] c = skin[l];
            if (c == null) continue;
            any = true;
            if (vertexCount == null || l >= vertexCount.length || skinFrom[l] + c.length != vertexCount[l] || skinFrom[l] % 4 != 0) return false;
            for (short code : c) {
                int b = bone(code);
                if (code < 0 || b >= n || (weight(code) > 0f && b + 1 >= n)) return false;
            }
        }
        return any;
    }

    private static float len(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /** Skinned vertices over all layers (what a frame spends on the blend). */
    public int skinnedVertices() {
        int s = 0;
        for (short[] c : skin) if (c != null) s += c.length;
        return s;
    }
}
