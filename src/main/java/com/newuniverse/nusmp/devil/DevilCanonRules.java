package com.newuniverse.nusmp.devil;

/**
 * Why: the pure numbers behind the canon looks of the devils (docs/devil_trial_model.md, "Canon looks"): which devils have their own model, where its files are, how big a rank draws it, and how the summoning
 * figure ({@link DevilNpc}) appears, merges and dissolves. No Minecraft type, so it is unit tested without a game; the client renderers ({@code DevilTrialGeoRenderer}, {@code DevilNpcGeoRenderer}) and the
 * figure's server-side appearance effect read it.
 *
 * <p>The ids are the roster of {@link WorldDevil} (the model of a devil is named after its id); an id that is not here (a pack devil, a future devil) keeps the generic horned brute of the trial. Cost: arrays and
 * arithmetic only.
 */
public final class DevilCanonRules {
    private DevilCanonRules() {}

    /** The devils that have their own canon model, in roster order (appended only, like the roster). */
    public static final String[] IDS = {
            "liebe", "lucifero", "beelzebub", "astaroth", "megicula", "lucifugus",
            "fox_devil", "curse_devil", "future_devil", "ghost_devil",
            "sebastian",
            "kin_of_iblis", "kin_of_egyn", "kin_of_amaimon", "kin_of_astaroth", "kin_of_lucifer",
            "daemon_noir", "daemon_blanc", "daemon_jaune", "daemon_violet", "daemon_rouge", "daemon_vert", "daemon_bleu",
            "hall_demon", "god_hand", "etherious",
    };

    /** The prefix of every canon asset: {@code geo/entity/devil_canon_<id>.geo.json}, {@code animations/entity/devil_canon_<id>.animation.json}, {@code textures/entity/devil_canon_<id>.png} (+ {@code _glowmask.png}). */
    public static final String PREFIX = "devil_canon_";

    public static boolean has(String id) {
        if (id != null) for (String s : IDS) if (s.equals(id)) return true;
        return false;
    }

    public static String geoPath(String id) { return "geo/entity/" + PREFIX + id + ".geo.json"; }

    public static String animationPath(String id) { return "animations/entity/" + PREFIX + id + ".animation.json"; }

    public static String texturePath(String id) { return "textures/entity/" + PREFIX + id + ".png"; }

    public static String glowPath(String id) { return "textures/entity/" + PREFIX + id + "_glowmask.png"; }

    /** The folder name of the 4.71.0 versions of the canon models, kept for the client option {@code bossLooks.devilCanonClassic} (same file names, one folder deeper). */
    public static final String CLASSIC_DIR = "classic_devil_canon/";

    public static String classicGeoPath(String id) { return "geo/entity/" + CLASSIC_DIR + PREFIX + id + ".geo.json"; }

    public static String classicAnimationPath(String id) { return "animations/entity/" + CLASSIC_DIR + PREFIX + id + ".animation.json"; }

    public static String classicTexturePath(String id) { return "textures/entity/" + CLASSIC_DIR + PREFIX + id + ".png"; }

    public static String classicGlowPath(String id) { return "textures/entity/" + CLASSIC_DIR + PREFIX + id + "_glowmask.png"; }

    /** The clips every devil model has (the generic brute and all canon models): the three of the trial plus the two of the summoning figure. Append only. */
    public static final String CLIP_IDLE = "animation.devil_trial.idle", CLIP_WALK = "animation.devil_trial.walk", CLIP_ATTACK = "animation.devil_trial.attack",
            CLIP_RISE = "animation.devil_trial.rise", CLIP_ROAR = "animation.devil_trial.roar";

    // ---------------------------------------------------------------- the size
    /** The scale a rank draws a devil at: a Supreme devil looms, a Low one is smaller; 1 for an unknown id. The hitbox of the trial devil does not change. The trial and the summoning figure share it. */
    public static float rankScale(String id) {
        WorldDevil d = WorldDevil.byId(id);
        if (d == null) return 1.0f;
        return switch (d.tier()) { case SUPREME -> 1.08f; case HIGH -> 1.0f; case MID -> 0.95f; default -> 0.9f; };
    }

    // ---------------------------------------------------------------- how the summoning figure is drawn (client reads these every frame; no allocation)
    /** How deep (blocks) the figure starts below the circle and rises out of it. */
    public static final float SINK_BLOCKS = 1.4f;

    /** The share (0..1) of the state's own length that has passed after {@code t} ticks. */
    public static float progress(int state, float t) {
        int len = switch (state) {
            case DevilNpcRules.RISING -> DevilNpcRules.RISE_TICKS;
            case DevilNpcRules.OFFER -> DevilNpcRules.ROAR_TICKS;
            case DevilNpcRules.MERGE -> DevilNpcRules.MERGE_TICKS;
            case DevilNpcRules.DISSOLVE -> DevilNpcRules.DISSOLVE_TICKS;
            default -> 0;
        };
        if (len <= 0) return 1f;
        return t <= 0f ? 0f : t >= len ? 1f : t / len;
    }

    /** Opacity of the figure: it fades in during the first half of the rise, out while it merges or dissolves. */
    public static float fade(int state, float t) {
        float p = progress(state, t);
        return switch (state) {
            case DevilNpcRules.RISING -> Math.min(1f, p * 2f);
            case DevilNpcRules.MERGE, DevilNpcRules.DISSOLVE -> 1f - p;
            default -> 1f;
        };
    }

    /** How far below its place the figure stands (it climbs out of the circle during the rise). */
    public static float sink(int state, float t) { return state == DevilNpcRules.RISING ? (1f - progress(state, t)) * SINK_BLOCKS : 0f; }

    /** The size factor while merging into the signer (down to 40 percent); 1 otherwise. */
    public static float shrink(int state, float t) { return state == DevilNpcRules.MERGE ? 1f - 0.6f * progress(state, t) : 1f; }

    /** How far (0..1, eased) of the way to the signer's chest the figure has slid while merging; 0 otherwise. */
    public static float slide(int state, float t) {
        if (state != DevilNpcRules.MERGE) return 0f;
        float p = progress(state, t);
        return p * p;
    }

    /** The opacity as one of {@code steps + 1} levels, so the renderer can use a table of colours instead of making one per frame. */
    public static int fadeLevel(float fade, int steps) {
        int n = Math.round(fade * steps);
        return n < 0 ? 0 : Math.min(n, steps);
    }

    // ---------------------------------------------------------------- the appearance effect of the figure (server side)
    /** One burst every this many ticks of the rise, of this many dust particles (the haze is half of it); the last burst, as the figure stands up, is the larger. */
    public static final int FX_EVERY = 3, FX_BURST = 6, FX_LAND = 14;

    /** The particles (of the main kind) the figure sends on tick {@code stateTicks} of its rise: 0 on most ticks. */
    public static int riseBurst(int stateTicks) {
        if (stateTicks <= 0 || stateTicks > DevilNpcRules.RISE_TICKS || stateTicks % FX_EVERY != 0) return 0;
        return stateTicks + FX_EVERY > DevilNpcRules.RISE_TICKS ? FX_LAND : FX_BURST;
    }

    /** The most particles of the main kind one figure sends in the whole rise (bursts only, the haze is half of it); the server budget caps whatever a crowd of rituals asks for. */
    public static int riseParticlesTotal() {
        int n = 0;
        for (int t = 0; t <= DevilNpcRules.RISE_TICKS; t++) n += riseBurst(t);
        return n;
    }
}
