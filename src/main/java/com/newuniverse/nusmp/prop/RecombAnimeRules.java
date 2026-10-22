package com.newuniverse.nusmp.prop;

/**
 * Recombination Magic, the "anime look" (client switch {@code visual.recombAnime}): the numbers and decisions of how the house-golem (the Raging Black Bull drawn as the giant of the anime:
 * a humanoid built of houses with tower horns and brick fists) moves and where its dust and impact flashes go. Pure (no game classes), so JUnit pins it; the painter
 * ({@code client/prop/RecombAnimePainter}) and the effect helper ({@code vfx/client/RecombAnimeFx}) only call it. Nothing here changes the server: the Bull's hit box, its ram and its cannons are
 * those of {@link RecombinationProps}; the look follows the same synced state word (bits 0-1 state, 2-17 start time, 18 Ultra, 19-29 Ultra start).
 *
 * <p>Model numbers are in model pixels (16 per block) and come from {@code tools/recomb/golem_numbers.json}, written by {@code tools/recomb/gen_recomb_anime.py}; a test compares them with the file.
 * The model is 156 px (9.75 blocks) tall at scale 1; {@link #RENDER_FACTOR} brings it to about 8 blocks at the Bull's usual prop scale of 1.1 to 1.3.</p>
 */
public final class RecombAnimeRules {
    private RecombAnimeRules() {}

    public static final float PX = 1f / 16f;

    // ------------------------------------------------------------------ the model (tools/recomb/golem_numbers.json)
    public static final float HEIGHT_PX = 156f;
    public static final float RENDER_FACTOR = 0.70f;
    public static final float FOOT_X_PX = 14f, FOOT_FRONT_PX = 17f, FOOT_BACK_PX = 13f, LEG_LENGTH_PX = 46f, LEG_SWING_DEG = 30f;
    public static final float FIST_X_PX = 42f, PUNCH_FIST_FORWARD_PX = 62.5f, PUNCH_FIST_FRONT_PX = 77.5f, PUNCH_FIST_HEIGHT_PX = 20.5f;
    public static final float WALK_CYCLE_S = 1.0f, PUNCH_IMPACT_S = 0.60f, PUNCH_LENGTH_S = 1.4f, STOMP_LAND_S = 0.52f, STOMP_LENGTH_S = 0.9f, ASSEMBLE_S = 1.3f, COLLAPSE_S = 1.2f, FIRE_S = 0.6f;
    /** Chimney tops (x, y, z in model px, z negative = front): the left shoulder house and the right one (the tree stands there). */
    public static final float[] CHIMNEY_PX = {27.5f, 119f, -5f};
    public static final float[] EYE_PX = {5.4f, 108f, -10f};

    /** The models (assets/nusmp/geo/entity/recombanime_*.geo.json) and the clips each must carry; a test and the self-test read them. */
    public static final String[] MODELS = {"golem", "house", "ring", "drill", "golem_lod"};
    public static final String[][] CLIPS = {{"idle", "walk", "punch", "fire", "assemble", "collapse", "stomp"}, {"idle", "rise", "swap"}, {"idle"}, {"idle"},
            {"idle", "walk", "punch", "fire", "assemble", "collapse", "stomp"}};
    /** The most cubes a model may have (perf budget of a prop: at most about 130 x 5 faces x 4 vertices per pass, twice with the glow pass); the low-poly golem has about a third. */
    public static final int[] MAX_CUBES = {140, 30, 24, 28, 40};

    /** The custom particles (particle/fx catalog ids) the anime look spawns; nothing else is ever spawned by it. */
    public static final String[] PARTICLE_IDS = {"smoke_wisp", "soot_puff", "sand_grain", "earth_chip", "ash_flake", "impact_burst", "shock_ring", "spark", "ember", "flame_tongue"};

    // ------------------------------------------------------------------ the ticks of the server's states (RecombinationProps)
    public static final int ASSEMBLE_TICKS = 26, COLLAPSE_TICKS = 24, RAM_TICKS = 28, FIRE_TICKS = 12, PUNCH_IMPACT_TICK = 12, STOMP_AT_TICK = ASSEMBLE_TICKS + 10, STOMP_TICKS = 18;
    public static final int RAM = 1, FIRE = 2, ULTRA_BIT = 1 << 18;

    public enum Clip {
        IDLE("idle"), WALK("walk"), PUNCH("punch"), FIRE_VOLLEY("fire"), ASSEMBLE("assemble"), COLLAPSE("collapse"), STOMP("stomp");

        public final String id;

        Clip(String id) { this.id = id; }
    }

    // ------------------------------------------------------------------ the synced state word
    public static int state(int param) { return param & 3; }
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }
    public static boolean ultra(int param) { return (param & ULTRA_BIT) != 0; }
    public static int ultraSince(int param, long gameTime) { return ultra(param) ? (int) ((gameTime - ((param >> 19) & 0x7FF)) & 0x7FF) : 0; }

    /** Which clip the golem plays: the end of its life first, then the rise out of the ground, the blow, the volley, the settling stomp after the rise, the walk, else idle. */
    public static Clip clip(int state, int sinceTicks, float ageTicks, float maxLifeTicks, boolean moving) {
        if (ageTicks > maxLifeTicks - COLLAPSE_TICKS) return Clip.COLLAPSE;
        if (ageTicks < ASSEMBLE_TICKS) return Clip.ASSEMBLE;
        if (state == RAM && sinceTicks < RAM_TICKS) return Clip.PUNCH;
        if (state == FIRE && sinceTicks < FIRE_TICKS) return Clip.FIRE_VOLLEY;
        if (ageTicks < ASSEMBLE_TICKS + STOMP_TICKS) return Clip.STOMP;
        return moving ? Clip.WALK : Clip.IDLE;
    }

    /** The seconds into the clip: the ones that run once count from their own start, the loops from the age (a walk is driven by {@link #walkSeconds}). */
    public static float clipSeconds(Clip c, float sinceTicks, float ageTicks, float maxLifeTicks, float phaseCycles) {
        switch (c) {
            case COLLAPSE: return (ageTicks - (maxLifeTicks - COLLAPSE_TICKS)) / 20f;
            case ASSEMBLE: return ageTicks / 20f;
            case PUNCH: case FIRE_VOLLEY: return sinceTicks / 20f;
            case STOMP: return (ageTicks - ASSEMBLE_TICKS) / 20f;
            case WALK: return walkSeconds(phaseCycles);
            default: return ageTicks / 20f;
        }
    }

    // ------------------------------------------------------------------ size
    /** The scale the painter draws the model at: the prop's scale (at least 0.3) times the render factor, and 1.6 times that when the Ultra form has fully grown ({@code grow} 0..1). */
    public static float renderScale(float propScale, float grow) {
        return Math.max(0.3f, propScale) * RENDER_FACTOR * (1f + 0.6f * Math.max(0f, Math.min(1f, grow)));
    }

    // ------------------------------------------------------------------ the stride (feet that do not slide)
    /** Blocks the body travels in one walk cycle (two steps) at a render scale: each step is the swing of a leg, 2 * leg * sin(swing). */
    public static float cycleBlocks(float renderScale) {
        return 2f * 2f * LEG_LENGTH_PX * (float) Math.sin(Math.toRadians(LEG_SWING_DEG)) * PX * renderScale;
    }

    /** The walk phase (in cycles, kept in [0, 1)) after the body moved {@code blocks} more. */
    public static float advancePhase(float phase, double blocks, float renderScale) {
        float p = phase + (float) (blocks / Math.max(0.05f, cycleBlocks(renderScale)));
        p = p - (float) Math.floor(p);
        return p < 0f ? p + 1f : p;
    }

    /** Seconds into the walk clip for a phase in cycles. */
    public static float walkSeconds(float phaseCycles) { return phaseCycles * WALK_CYCLE_S; }

    /**
     * Footfalls between two phases (both in [0, 1), the walk moving on): bit 0 = the left foot lands (at 0.25 of the cycle: the left leg is forward and starts back), bit 1 = the right foot
     * (at 0.75). {@code prev == next} gives none.
     */
    public static int footfalls(float prev, float next) {
        int out = 0;
        if (crossedPhase(prev, next, 0.25f)) out |= 1;
        if (crossedPhase(prev, next, 0.75f)) out |= 2;
        return out;
    }

    private static boolean crossedPhase(float prev, float next, float mark) {
        if (next >= prev) return prev < mark && next >= mark;
        return prev < mark || next >= mark;                                      // wrapped past 1.0
    }

    /** True when a counter that now reads {@code now} and moved on by {@code elapsed} since the last look passed {@code mark} (and starts a fresh count at 0 when it is smaller than elapsed). */
    public static boolean crossed(int now, int elapsed, int mark) {
        if (elapsed <= 0) return false;
        return now - elapsed < mark && now >= mark;
    }

    // ------------------------------------------------------------------ where things are (world offsets from the prop's feet, blocks)
    /** The world x, z offset of a point {@code lx} to the model's left (+x in the file) and {@code forward} blocks ahead, for an entity yaw in degrees (Minecraft: forward = (-sin, cos)). */
    public static double[] toWorld(double yawDeg, double lx, double forward) {
        double r = Math.toRadians(yawDeg), s = Math.sin(r), c = Math.cos(r);
        return new double[]{-s * forward + c * lx, c * forward + s * lx};
    }

    /** Where the striking (right) fist lands: {@code {left offset, forward, height}} in blocks at a render scale. */
    public static double[] fistLanding(float renderScale) {
        return new double[]{-FIST_X_PX * PX * renderScale, PUNCH_FIST_FRONT_PX * PX * renderScale, 0.1 * renderScale};
    }

    /** Where a foot comes down: {@code {left offset, forward}} in blocks; {@code left} true = the left foot. */
    public static double[] footLanding(boolean left, float renderScale) {
        double reach = LEG_LENGTH_PX * Math.sin(Math.toRadians(LEG_SWING_DEG)) + 2.0;
        return new double[]{(left ? FOOT_X_PX : -FOOT_X_PX) * PX * renderScale, reach * PX * renderScale};
    }

    /** The chimney top (left of the model, height, forward) in blocks. */
    public static double[] chimneyTop(float renderScale) {
        return new double[]{CHIMNEY_PX[0] * PX * renderScale, CHIMNEY_PX[1] * PX * renderScale, -CHIMNEY_PX[2] * PX * renderScale};
    }

    // ------------------------------------------------------------------ the effects (counts before the quality scaling)
    public static int footfallDust(float renderScale) { return clampInt(Math.round(5 + 3 * renderScale), 4, 14); }

    public static int slamDust(float renderScale) { return clampInt(Math.round(8 + 5 * renderScale), 6, 28); }

    public static int slamSparks(float renderScale) { return clampInt(Math.round(10 + 6 * renderScale), 8, 30); }

    public static int slamChips(float renderScale) { return clampInt(Math.round(4 + 3 * renderScale), 3, 14); }

    /** Ticks between two small puffs of chimney smoke (a puff roughly every {@code n} ticks). */
    public static int chimneyPuffEvery() { return 8; }

    private static int clampInt(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    // ------------------------------------------------------------------ no lag: level of detail and the draw budget of a frame (client)
    public static final int LOD_FULL = 0, LOD_LOW = 1, LOD_CULL = 2;
    /** At most this many golems are drawn with the full model in one frame (the rest use the low-poly one): with N golems in view the cost is bounded, not N times the full model. */
    public static final int FULL_GOLEMS_PER_FRAME = 3;
    /** A golem's tick events (dust, flash, smoke) are worked out only within 96 blocks of the camera (squared); beyond that the particles would be dropped by their distance detail anyway. */
    public static final double EVENT_RANGE_SQ = 96.0 * 96.0;
    /** Quads of one pass of the full golem and of the low-poly golem (the glow pass is drawn for the full one only): the numbers the doc quotes; a test pins them to the files. */
    public static final int FULL_QUADS_MAX = 480, LOW_QUADS_MAX = 170;

    /**
     * Which model a golem is drawn with at a squared distance from the camera: the full one up to {@code near * quality * max(1, renderScale)} blocks (near is the aura level-of-detail distance, 48 by
     * default; quality is the video quality's detail factor 1, 0.75 or 0.5), the low-poly one up to 3.2 times that (and never less than 96 blocks times the quality), nothing beyond.
     */
    public static int lod(double distSq, double near, float quality, float renderScale) {
        double q = Math.max(0.25f, Math.min(1f, quality));
        double nearEff = Math.max(8.0, near) * q * Math.max(1f, renderScale);
        double cull = Math.max(96.0 * q, nearEff * 3.2);
        if (distSq > cull * cull) return LOD_CULL;
        return distSq <= nearEff * nearEff ? LOD_FULL : LOD_LOW;
    }

    /** True when a small construct (a house, the ring, the drill) is too far to draw: beyond {@code far * quality} blocks. */
    public static boolean cullSmall(double distSq, double far, float quality) {
        double d = Math.max(16.0, far) * Math.max(0.25f, Math.min(1f, quality));
        return distSq > d * d;
    }

    /** The count of full-model golems drawn in the current frame; the key is anything that differs from frame to frame (the game time and the partial tick). Render thread only, no allocation. */
    public static final class FrameBudget {
        private long key = Long.MIN_VALUE;
        private int used;

        public boolean take(long frameKey, int cap) {
            if (frameKey != key) {
                key = frameKey;
                used = 0;
            }
            if (used >= cap) return false;
            used++;
            return true;
        }
    }

    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    // ------------------------------------------------------------------ caps and housekeeping
    /** The most golems whose walk state the painter keeps (a golem that was not drawn for {@link #STATE_STALE_TICKS} ticks is forgotten). */
    public static final int STATE_CAP = 48, STATE_STALE_TICKS = 400;

    /** The tick budget of one painted frame's effects: every golem asks for its events once per game tick, never per frame. */
    public static boolean newTick(int lastTick, int tick) { return tick != lastTick; }
}
