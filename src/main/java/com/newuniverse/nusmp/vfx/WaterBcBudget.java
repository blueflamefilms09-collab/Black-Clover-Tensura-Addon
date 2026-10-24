package com.newuniverse.nusmp.vfx;

/**
 * Counts what the real scene builder ({@link WaterBcScene}) emits, tick by tick over the whole life of an effect, for a spread of sizes and lengths (the Roar of a short and of a long flight, the Lance of
 * the javelin and of the tide lance, the Cradle and the Nest of a small and of a big radius ...). Pure, so the unit test pins {@link WaterBc#meshCap} / {@link WaterBc#spriteCap} against the real geometry,
 * the CI self-test repeats it on the dedicated server, and the docs quote the numbers. Allocation of the counting object is fine here: this never runs in a frame.
 */
public final class WaterBcBudget {
    private WaterBcBudget() {}

    /** The most mesh and sprite quads of one effect, and which textures and pieces it touched. */
    public static final class Peak {
        public int mesh, sprites;
        public final boolean[] textures = new boolean[WaterBc.SPRITES.length], pieces = new boolean[WaterBc.PIECES.length];
    }

    private static final Object[][] SETS = {
            {WaterBc.ROAR, 0f, 1.5f, 6f, 0f, 1.5f, 18f, 1.2f, 24},
            {WaterBc.ROAR, 0f, 1.5f, 6f, 0f, 1.5f, 60f, 2.0f, 60},
            {WaterBc.ROAR, 0f, 1.5f, 6f, 0f, 1.5f, 6.01f, 0.6f, 10},
            {WaterBc.LANCE, 0f, 1.5f, 6f, 0f, 1.5f, 26f, 0.9f, 12},
            {WaterBc.LANCE, 0f, 1.5f, 6f, 0f, 1.5f, 40f, 1.3f, 16},
            {WaterBc.CRADLE, 0f, 0f, 6f, 0f, 1f, 6f, 2.2f, 80},
            {WaterBc.CRADLE, 0f, 0f, 6f, 0f, 1f, 6f, 4.4f, 160},
            {WaterBc.NEST, 0f, 0f, 6f, 0f, 1f, 6f, 2.4f, 80},
            {WaterBc.NEST, 0f, 0f, 6f, 0f, 1f, 6f, 6f, 200},
            {WaterBc.BURST, 0f, 0f, 6f, 0f, 1f, 6f, 1.4f, 28},
            {WaterBc.BURST, 0f, 0f, 6f, 0f, 1f, 6f, 2.4f, 34},
            {WaterBc.SPLASH, 0f, 0f, 4f, 0f, 0.8f, 4f, 1.2f, 20},
            {WaterBc.SPLASH, 0f, 0f, 4f, 0f, 0.8f, 4f, 0.6f, 14},
            {WaterBc.RING, 0f, 0f, 4f, 0f, 1f, 4f, 1.2f, 30},
            {WaterBc.RING, 0f, 0f, 4f, 0f, 1f, 4f, 2.0f, 18}};

    /** The camera faces +z (right = -x, up = +y); the effects stand in front of it, as in the preview. */
    public static Peak peak(int kind, int tier, boolean tinted) {
        Peak pk = new Peak();
        WaterBcScene scene = new WaterBcScene();
        scene.view.set(-1f, 0f, 0f, 0f, 1f, 0f);
        int[] cur = new int[2];
        WaterBcScene.Out out = new WaterBcScene.Out() {
            @Override public void sprite(int tex, boolean add, float u0, float u1, int argb, float[] q) {
                cur[1]++;
                pk.textures[tex] = true;
            }

            @Override public void mesh(WaterBcToon.Inst m) {
                cur[0] += WaterBcMeshes.get(m.piece, m.lo).quads;
                pk.pieces[m.piece] = true;
            }
        };
        for (Object[] s : SETS) {
            if ((Integer) s[0] != kind) continue;
            int dur = (Integer) s[8];
            for (int age = 0; age <= dur; age++) {
                for (int half = 0; half < 2; half++) {
                    cur[0] = 0;
                    cur[1] = 0;
                    scene.begin(out, (Float) s[1], (Float) s[2], (Float) s[3], (Float) s[4], (Float) s[5], (Float) s[6], (Float) s[7], age + half * 0.5f, dur, 12345L, tinted, 0xC02020).build(kind, tier);
                    pk.mesh = Math.max(pk.mesh, cur[0]);
                    pk.sprites = Math.max(pk.sprites, cur[1]);
                }
            }
        }
        return pk;
    }
}
