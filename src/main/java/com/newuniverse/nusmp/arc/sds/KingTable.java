package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;

import java.util.List;

/**
 * Why: King's numbers in one pure place (the same shape as the Zogratis tables) so the scheduler can be played against them in a unit test. King, the Grizzly's Sin
 * of Sloth and Fairy King Harlequin, fights from the air with the Spirit Spear Chastiefol, and the fight is built from the canon of that spear (the forms Guardian,
 * Fossilization, Sunflower, Increase and Pollen Garden, and the power Disaster: see docs/arc/king.md for the sources and for what is unverified). Three rules make it a
 * fight that is more than a stat check: the Guardian turns blades and arrows aside and is undone by ice; the Pollen Garden / Disaster field strikes everyone who is
 * not standing in a garden (and a garden is always within reach); the Sunflower is a flower of lanes in which the safe place is between two petals. Everything is
 * telegraphed, and the air is the fourth rule: he hovers out of a sword's reach, comes down to dive, and is exposed when he lands. The order of the rows is the index
 * the client reads move names by: only ever append.
 */
public final class KingTable {
    private KingTable() {}

    public static final String CHASTIEFOL_THROW = "chastiefol_throw", INCREASE_RAIN = "increase_rain", DIVE_STRIKE = "dive_strike", WING_GUST = "wing_gust",
            POLLEN_GARDEN = "pollen_garden", GUARDIAN = "guardian", GUARDIAN_SLAM = "guardian_slam", SUNFLOWER = "sunflower", FOSSILIZATION = "fossilization",
            DISASTER = "disaster";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float THROW = 11f, THROW_BURST = 6f, RAIN = 7f, DIVE = 14f, GUST = 4f, POLLEN_PULSE = 8f, DISASTER_PULSE = 14f, SLAM = 16f, PETAL = 11f,
            FOSSIL = 9f;

    public static final double THROW_BURST_RADIUS = 2.4, RAIN_RADIUS = 1.7, DIVE_RADIUS = 3.0, GUST_RANGE = 7.0, GUST_HALF = 0.9, POLLEN_RADIUS = 9.0,
            DISASTER_RADIUS = 15.0, GARDEN_RADIUS = 2.2, SLAM_RADIUS = 3.8, PETAL_HALF = 0.9, PETAL_LENGTH = 11.0, FOSSIL_RANGE = 5.5, FOSSIL_HALF = 0.7;
    /** How far the spear reaches (it is thrown too fast to be a point: the lane in the telegraph is the whole flight). */
    public static final double THROW_REACH = 30.0;
    /**
     * The farthest the CENTRE of the nearest garden is from a foe, in blocks. The foe only has to reach the garden's edge (this less the garden's radius, 2.3 blocks); a player
     * who is walking and takes half a second to react makes 2.6 blocks of the 24 ticks of warning, so nobody needs to sprint to be safe.
     */
    public static final double MAX_WALK = 4.5;
    public static final double GARDEN_GAP = 3.2;

    /** The rhythm of the fields: the Pollen Garden (three beats) and the Disaster (five beats, six at the Gates). The warning is 24 ticks in all of them. */
    public static final DisasterBeats.Spec POLLEN = new DisasterBeats.Spec(3, 36, 24), DISASTER_SPEC = new DisasterBeats.Spec(5, 32, 24),
            DISASTER_SPEC_GATES = new DisasterBeats.Spec(6, 32, 24);

    /** How long the Guardian stands, how long a foe is held by Fossilization (before the balance law's cap), and how long ice leaves him staggered. */
    public static final int GUARDIAN_TICKS = 160, ROOT_TICKS = 40, THAW_DAZE = 40;
    /** The burst that breaks his concentration while the Disaster stands: a share of his maximum health inside the span. */
    public static final float BREAK_FRACTION = 0.08f;
    public static final int BREAK_SPAN = 60;
    /** How much more he takes while dazed (ice on the Guardian, a broken Disaster, a stumble after the dive). */
    public static final float DAZED_TAKES = 1.15f;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(CHASTIEFOL_THROW, "Chastiefol: Spirit Spear", 20, 18, 110, 6, 30, 5, 8),
            MoveRow.of(INCREASE_RAIN, "Chastiefol: Increase", 20, 24, 230, 6, 40, 4, 10).crowdMove(),
            MoveRow.of(DIVE_STRIKE, "Fairy Dive", 20, 22, 260, 6, 40, 3, 58),     // exposed from the start of the dive for 58 ticks: 40 to 52 of them on the ground (a dive takes 6 to 18)
            MoveRow.of(WING_GUST, "Fairy Gust", 20, 16, 170, 0, 7, 3, 8),
            MoveRow.of(POLLEN_GARDEN, "Pollen Garden", 40, 28, 420, 0, 44, 3, 10).crowdMove(),
            MoveRow.of(GUARDIAN, "Chastiefol: Guardian", 40, 20, 520, 0, 48, 2, 10),
            MoveRow.of(GUARDIAN_SLAM, "Guardian: Four Arms", 40, 24, 200, 0, 12, 4, 12),
            MoveRow.of(SUNFLOWER, "Chastiefol: Sunflower", 40, 26, 300, 8, 44, 3, 10),
            MoveRow.of(FOSSILIZATION, "Chastiefol: Fossilization", 40, 18, 280, 0, 7, 3, 12),
            MoveRow.of(DISASTER, "Disaster", 60, 60, 1300, 0, 44, 5, 60).ult());

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) {
        for (MoveRow r : ROWS) if (r.id().equals(id)) return r;
        return null;
    }

    /** Spears of one Increase: three and the volley slots (4, 4, 5, 6, 7), at most eight. */
    public static int rainCount(int castSlots) { return Math.min(8, 3 + Math.max(1, castSlots)); }

    /** Petals of one Sunflower: six, eight at the Gates (the gap between two petals stays wide enough to stand in, see {@link SunflowerMath}). */
    public static int petalCount(boolean gates) { return gates ? 8 : 6; }

    /** How many Sunflower volleys: one, two at the Gates (the second turned half a petal so the first one's safe place is not the second one's). */
    public static int sunflowerVolleys(boolean gates) { return gates ? 2 : 1; }

    public static DisasterBeats.Spec disasterSpec(boolean gates) { return gates ? DISASTER_SPEC_GATES : DISASTER_SPEC; }

    /** Gardens in a Pollen Garden: a foe and two over, at least three; in the Disaster: a foe and one over, at least two. */
    public static int pollenGardens(int foes) { return SafeGardens.gardenCount(foes, 2, 3); }

    public static int disasterGardens(int foes) { return SafeGardens.gardenCount(foes, 1, 2); }

    /** The pulse of the Disaster grows a little with the power steps (14, 14, 14, 15, 16 at the Gates). */
    public static float disasterPulse(boolean gates) { return gates ? DISASTER_PULSE + 2f : DISASTER_PULSE; }
}
