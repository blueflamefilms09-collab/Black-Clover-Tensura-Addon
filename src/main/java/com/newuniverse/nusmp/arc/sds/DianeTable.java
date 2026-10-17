package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Diane's numbers in one pure place (the same shape as the Zogratis tables) so the scheduler can be played against them in a unit test. Diane, the Serpent's Sin of
 * Envy, is a giant of the Giant Clan with the hammer Gideon and the earth-shaping power Creation (see docs/arc/diane.md for the sources), and her fight is built on SIZE: she
 * opens at human size, grows into a giant with a quake you can see coming, is slow, wide and tough as a giant, and is CUT DOWN TO SIZE by a burst of damage, which gives the
 * party ten seconds of a smaller, faster, easier Diane before she grows again. Each size has its own moves (a move has a group: 0 either, 1 giant, 2 human); the two moves
 * that change her size are not drawn by the scheduler, the boss starts them when the clock says so. Drole's Dance is her ultimate: a rhythm of five stomps whose rings can be
 * jumped, that makes her stronger for every beat it completes and that a burst interrupts. The order of the rows is the index the client reads move names by: only ever append.
 */
public final class DianeTable {
    private DianeTable() {}

    public static final String GIDEON_SMASH = "gideon_smash", GIDEON_SWEEP = "gideon_sweep", QUEEN_EMBRACE = "queen_embrace", RISING_METEOR = "rising_meteor",
            DROLE_DANCE = "drole_dance", RUSH_ROCK = "rush_rock", HEAVY_METAL = "heavy_metal", DIAMOND_SHIELD = "diamond_shield", HAMMER_SWING = "hammer_swing",
            GROW = "giants_pride", SHRINK = "human_size";

    /** The size groups of a move. */
    public static final int ANY = 0, GIANT = 1, HUMAN = 2;

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float SMASH = 17f, SWEEP = 14f, QUEEN = 15f, METEOR = 12f, DANCE_WAVE = 10f, ROCK = 5f, SWING = 11f, GROW_QUAKE = 10f;

    public static final double SMASH_RADIUS = 4.2, SWEEP_RANGE = 8.5, SWEEP_HALF = 1.1, QUEEN_HALF = 3.2, QUEEN_LENGTH = 26, QUEEN_SPEED = 0.9, METEOR_RADIUS = 2.4,
            DANCE_RADIUS = 10, DANCE_SPEED = 0.55, DANCE_RING_HEIGHT = 0.6, SWING_RANGE = 4.6, SWING_HALF = 0.9, GROW_RADIUS = 7;
    /** The size attribute of a giant (her hitbox is 1.95 times as big in every direction: 0.7 x 2.0 becomes about 1.4 x 3.9). */
    public static final double SCALE_GIANT = 1.95;
    /** Heavy Metal: how long, what each blow loses (a share of her maximum health) and how much fire melts it (a share, inside {@link MetalSkin#HEAT_SPAN} ticks). */
    public static final int METAL_TICKS = 120;
    public static final float METAL_FLAT_FRACTION = 0.004f, METAL_MELT_FRACTION = 0.04f;
    /** The Diamond Shield: how long it stands and what it takes before it breaks (a share of her maximum health). */
    public static final int CRYSTAL_TICKS = 160;
    public static final float CRYSTAL_FRACTION = 0.06f;
    /** Drole's Dance: the ticks of a beat, how long before a stomp the foot rises (the tell), and the beats (more at the Gates). */
    public static final int BEAT_TICKS = 20, PREBEAT = 8, BEATS = 5, BEATS_GATES = 7;
    /** Cutting her down to size: the share of her health inside the span that does it (more at the Gates), the span and the time before it can happen again. */
    public static final float CUT_FRACTION = 0.09f, CUT_FRACTION_GATES = 0.14f;
    public static final int CUT_SPAN = 80, CUT_COOLDOWN = 600;
    /** What she takes as a giant (she is tough), cut down to size (the window the party earned), and dazed (a shield or a dance broken). */
    public static final float GIANT_TAKES = 0.9f, CUT_TAKES = 1.15f, DAZED_TAKES = 1.15f;
    public static final int DAZE_TICKS = 40;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(GIDEON_SMASH, "Gideon: Smash", 20, 26, 150, 0, 10, 5, 14).inGroup(GIANT),
            MoveRow.of(GIDEON_SWEEP, "Gideon: Sweep", 20, 24, 130, 0, 8, 4, 14).inGroup(GIANT),
            MoveRow.of(QUEEN_EMBRACE, "Queen Embrace", 40, 30, 360, 6, 36, 3, 12).inGroup(ANY),
            MoveRow.of(RISING_METEOR, "Rising Meteor", 20, 28, 260, 4, 36, 4, 10).inGroup(ANY).crowdMove(),
            MoveRow.of(DROLE_DANCE, "Drole's Dance", 60, 60, 1300, 0, 48, 5, 50).inGroup(GIANT).ult(),
            MoveRow.of(RUSH_ROCK, "Rush Rock", 20, 16, 90, 4, 32, 5, 8).inGroup(HUMAN),
            MoveRow.of(HEAVY_METAL, "Heavy Metal", 20, 20, 480, 0, 48, 2, 10).inGroup(HUMAN),
            MoveRow.of(DIAMOND_SHIELD, "Diamond Shield", 40, 22, 420, 0, 48, 2, 10).inGroup(HUMAN),
            MoveRow.of(HAMMER_SWING, "Gideon: Swing", 20, 14, 70, 0, 5.5, 5, 8).inGroup(HUMAN),
            MoveRow.of(GROW, "Giant's Pride", 20, 30, 400, 0, 48, 1, 14).inGroup(HUMAN).noMana(),
            MoveRow.of(SHRINK, "Human Size", 20, 24, 400, 0, 48, 1, 10).inGroup(GIANT).noMana());

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) {
        for (MoveRow r : ROWS) if (r.id().equals(id)) return r;
        return null;
    }

    /** The two moves that change her size: started by the boss's clock, never drawn by the scheduler. */
    public static boolean isSizeChange(String id) { return GROW.equals(id) || SHRINK.equals(id); }

    /** The moves the scheduler may draw at the given size: those of the group (or of either), without the two size changes. */
    public static List<MoveRow> drawable(boolean giant) {
        List<MoveRow> out = new ArrayList<>();
        for (MoveRow r : ROWS) if (!isSizeChange(r.id()) && (r.group() == ANY || r.group() == (giant ? GIANT : HUMAN))) out.add(r);
        return out;
    }

    /** Rocks of one Rush Rock: two and the volley slots (3, 3, 4, 5, 6), at most seven. */
    public static int rockCount(int castSlots) { return Math.min(7, 2 + Math.max(1, castSlots)); }

    /** Pillars of one Rising Meteor: two and the volley slots (3, 3, 4, 5, 6), at most seven. */
    public static int meteorCount(int castSlots) { return Math.min(7, 2 + Math.max(1, castSlots)); }

    public static int danceBeats(boolean gates) { return gates ? BEATS_GATES : BEATS; }

    public static float cutFraction(boolean gates) { return gates ? CUT_FRACTION_GATES : CUT_FRACTION; }
}
