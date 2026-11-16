package com.newuniverse.nusmp.beastwill;

import java.util.Map;

/**
 * Why (docs/beastwill_tails.md): the real tails and tentacles of the Beast Will forms are Blender meshes ({@code entity_meshes/bwtail_<stem>.txt}) cut into chains of parts named {@code sw<chain>_<segment>}.
 * This is everything about them that needs no game class, so a test can pin it: which forms have one, what kind of sway it plays, how a part name maps to the role number the client placer is given, and the
 * gentle sway itself (a wave that runs down the chain, bigger toward the tip). {@code tools/blender/beastwill_tails.py} mirrors {@link #angles} for its previews. Pure.
 */
public final class BeastTailRules {
    private BeastTailRules() {}

    /** The kinds of motion. */
    public static final int TAIL = 0, SNAKE = 1, TENTACLE = 2, FLUFF = 3;
    /** Role numbers of the chain parts start here (the eleven roles of {@code MeshPose} stay below): role = BASE + chain * 16 + segment. */
    public static final int BASE = 100, MAX_CHAIN = 16, MAX_SEG = 16;
    /** The most quad lines (body plus glow) one form's mesh may have, and the most players that get a mesh tail in one frame (the form budget of {@code NoelleFormRules}). */
    public static final int MAX_QUADS = 1500, MAX_PLAYERS_PER_FRAME = 4;
    /** The farthest a mesh tail is drawn (blocks); the far copy of the file takes over from 40 blocks, the glow stops at 48. */
    public static final double RANGE_BLOCKS = 48.0;

    /** The forms that have a mesh tail, their kind and how many chains the file has. {@code bodyNoTail}: the form's body piece also has tentacle bones, so a copy of it without them is worn instead. */
    public record Tail(int kind, int chains, boolean bodyNoTail) {}

    private static final Map<String, Tail> TAILS = Map.ofEntries(
            Map.entry("dragon", new Tail(TAIL, 1, false)),
            Map.entry("nidhoggr", new Tail(TAIL, 1, false)),
            Map.entry("jormungandr", new Tail(TAIL, 1, false)),
            Map.entry("serpent", new Tail(TAIL, 1, false)),
            Map.entry("apophis", new Tail(SNAKE, 2, false)),
            Map.entry("typhon", new Tail(SNAKE, 2, false)),
            Map.entry("kitsune", new Tail(FLUFF, 9, false)),
            Map.entry("kraken", new Tail(TENTACLE, 12, true)),
            Map.entry("scylla_charybdis", new Tail(TENTACLE, 6, true)));

    public static Tail of(String stem) { return TAILS.get(stem); }

    public static java.util.Set<String> stems() { return TAILS.keySet(); }

    public static String meshId(String stem) { return "bwtail_" + stem; }

    /** The role number of a part name {@code sw<chain>_<segment>}, or -1 for any other name. */
    public static int role(String part) {
        if (part == null || !part.startsWith("sw")) return -1;
        int u = part.indexOf('_');
        if (u < 3) return -1;
        try {
            int chain = Integer.parseInt(part.substring(2, u)), seg = Integer.parseInt(part.substring(u + 1));
            return chain < 0 || chain >= MAX_CHAIN || seg < 0 || seg >= MAX_SEG ? -1 : BASE + chain * MAX_SEG + seg;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean isRole(int role) { return role >= BASE && role < BASE + MAX_CHAIN * MAX_SEG; }

    public static int chainOf(int role) { return (role - BASE) / MAX_SEG; }

    public static int segOf(int role) { return (role - BASE) % MAX_SEG; }

    /**
     * The turn of one segment at {@code seconds}, radians about its own joint, written to {@code out} (pitch about x, yaw about y, roll 0; the mesh frame has the tail running toward -z). A wave runs down the
     * chain: each segment lags the one before it, the swing grows toward the tip, and chains are out of step with each other. Cheap: two sines, nothing allocated.
     */
    public static void angles(int kind, int chain, int seg, float seconds, float[] out) {
        float amp, w, lag, cross;
        switch (kind) {
            case SNAKE -> { amp = 0.09f; w = 1.4f; lag = 0.75f; cross = 0.35f; }
            case TENTACLE -> { amp = 0.10f; w = 0.9f; lag = 0.6f; cross = 0.9f; }
            case FLUFF -> { amp = 0.05f; w = 0.8f; lag = 0.5f; cross = 0.6f; }
            default -> { amp = 0.055f; w = 0.9f; lag = 0.55f; cross = 0.4f; }
        }
        float grow = 0.45f + 0.1f * seg;
        float ph = w * seconds * 6.2831855f - seg * lag + chain * 1.3f;
        out[0] = amp * cross * grow * (float) Math.sin(ph * 0.8f + 1.7f);
        out[1] = amp * grow * (float) Math.sin(ph);
        out[2] = 0f;
    }
}
