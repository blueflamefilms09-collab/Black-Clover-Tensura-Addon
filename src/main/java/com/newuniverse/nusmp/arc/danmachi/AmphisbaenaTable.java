package com.newuniverse.nusmp.arc.danmachi;

import java.util.List;

/**
 * Why: the Amphisbaena's moves are numbers first (range windows, telegraphs, shapes, damage) and the table must be checkable without a game: every move has a
 * telegraph of at least a second at the Gates, an attack exists at every distance, the ultimate is rare and leaves it exposed. This pure table is that;
 * {@code AmphisbaenaBoss} turns each row into a framework {@code Move}.
 *
 * <p>The shape of the fight (the two-headed water dragon of the 27th floor, see {@code docs/arc/amphisbaena.md}): the blue head breathes flame that burns
 * through steel (the Blue Flame Breath, the Scorch Field), the red head breathes a mist that dissipates magic (the Red Mist Breath), the body bites, lashes
 * and sweeps, the water answers (the Tidal Surge, the Waterspouts, the Dive Strike, the Water City Call), and the Twin Calamity breathes both at once. Cut
 * the red head and the mist moves are gone for good. The leveled form adds the Blue Flame Cyclone. New rows are only appended.
 */
public final class AmphisbaenaTable {
    private AmphisbaenaTable() {}

    public static final String TWIN_BITE = "twin_bite", TAIL_SWEEP = "tail_sweep", BLUE_FLAME_BREATH = "blue_flame_breath", RED_MIST_BREATH = "red_mist_breath",
            TIDAL_SURGE = "tidal_surge", WATERSPOUTS = "waterspouts", HEADS_LASH = "heads_lash", WATER_CITY_CALL = "water_city_call", DIVE_STRIKE = "dive_strike",
            SCORCH_FIELD = "scorch_field", TWIN_CALAMITY = "twin_calamity", BLUE_FLAME_CYCLONE = "blue_flame_cyclone";

    /** The moves that need the red head: gone once it is cut. */
    public static boolean needsRedHead(String id) { return RED_MIST_BREATH.equals(id) || TWIN_CALAMITY.equals(id); }

    private static final List<DmSpec> SPECS = List.of(
            DmSpec.move(TWIN_BITE, "Twin Bite").power(20).wind(18).cd(90).range(0, 7).weight(6).recovery(10).custom(6.5, 12),
            DmSpec.move(TAIL_SWEEP, "Tail Sweep").power(20).wind(22).cd(150).range(0, 10).weight(4).crowd().recovery(12).spin(9.0, 12, 1.6),
            DmSpec.move(BLUE_FLAME_BREATH, "Blue Flame Breath").power(20).wind(28).cd(240).range(4, 18).weight(5).recovery(14).breath(16, 28, 8, 6.5, 70),
            DmSpec.move(RED_MIST_BREATH, "Red Mist Breath").power(20).wind(28).cd(420).range(5, 30).weight(3).crowd().recovery(12).custom(6.5, 0),
            DmSpec.move(TIDAL_SURGE, "Tidal Surge").power(20).wind(24).cd(180).range(0, 12).weight(4).crowd().recovery(12).wave(12, 11, 1.4, 2, 14),
            DmSpec.move(WATERSPOUTS, "Waterspouts").power(20).wind(24).cd(210).range(5, 32).weight(4).recovery(10).marks(3, 2.3, 10, 1.2, 16),
            DmSpec.move(HEADS_LASH, "Heads' Lash").power(40).wind(22).cd(190).range(4, 22).weight(3).recovery(12).lane(2, 18, 2.0, 13, 1.0, 0),
            DmSpec.move(WATER_CITY_CALL, "Water City Call").power(40).wind(30).cd(720).range(0, 48).weight(2).recovery(10).custom(0, 0),
            DmSpec.move(DIVE_STRIKE, "Dive Strike").power(40).wind(28).cd(260).range(8, 34).weight(3).recovery(12).leap(8, 15, 1.3, 10),
            DmSpec.move(SCORCH_FIELD, "Scorch Field").power(60).wind(26).cd(330).range(5, 30).weight(3).crowd().recovery(12).custom(7, 4),
            DmSpec.move(TWIN_CALAMITY, "Twin Calamity").power(60).wind(64).cd(1500).range(0, 36).weight(5).crowd().ultimate().custom(16, 7),
            // the leveled form: appended, never reordered
            DmSpec.move(BLUE_FLAME_CYCLONE, "Blue Flame Cyclone").power(100).wind(30).cd(300).range(0, 14).weight(4).recovery(14).leveled().breath(14, 28, 12, 6.0, 360));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<DmSpec> specs() { return SPECS; }

    public static DmSpec byId(String id) {
        for (DmSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
