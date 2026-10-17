package com.newuniverse.nusmp.arc.danmachi;

import java.util.List;

/**
 * Why: the Minotaur's moves are numbers first (when it may use a move, how long the telegraph lasts, how big the shape is, how hard it hits), and the whole
 * table must be checkable without a game: that every move has a telegraph of at least a second at the Gates, that there is always an attack to throw at any
 * distance, that the ultimate is rare and leaves it exposed. This pure table is that; {@code MinotaurBoss} turns each row into a framework {@code Move}.
 *
 * <p>The shape of the fight (the Level 2 monster that hunts Bell on the 5th floor of the anime, see {@code docs/arc/minotaur.md}): a bull that wants to be
 * close (the horn, the fist, the slam, the sweep), runs at whoever it has marked (the charge, the leap, the rampage), shouts the weak into fear (the Howl),
 * and ends the first half of a fight with a run across the whole room (the Bull Run). When the party is strong enough it is reborn at the Gates as
 * Asterius, the one-horned, and the leveled move (the Wild Cross) joins. The first ten rows keep their places for the client; new rows are only appended.
 */
public final class MinotaurTable {
    private MinotaurTable() {}

    public static final String HORN_GORE = "horn_gore", HAMMER_FIST = "hammer_fist", GROUND_SLAM = "ground_slam", BULL_CHARGE = "bull_charge", HOWL = "howl",
            HUNTERS_LEAP = "hunters_leap", HORN_SWEEP = "horn_sweep", RUBBLE_THROW = "rubble_throw", RAMPAGE = "rampage", BULL_RUN = "bull_run",
            WILD_CROSS = "wild_cross";

    /** Base damages of the custom moves (before the damage law: power tuning, the boss's scale, the victim's EP, the balance law's caps). */
    public static final float RAMPAGE_DASH = 9f, BULL_RUN_HIT = 26f, BULL_RUN_QUAKE = 14f, CROSS_LANE = 16f, HOWL_SHOUT = 4f;

    private static final List<DmSpec> SPECS = List.of(
            DmSpec.move(HORN_GORE, "Horn Gore").power(20).wind(16).cd(80).range(0, 4.8).weight(6).recovery(10).cone(4.4, 55, 14, 1.2, true),
            DmSpec.move(HAMMER_FIST, "Hammer Fist").power(20).wind(18).cd(110).range(0, 7).weight(4).recovery(12).marks(1, 2.6, 17, 1.0, 0),
            DmSpec.move(GROUND_SLAM, "Ground Slam").power(20).wind(22).cd(160).range(0, 9.5).weight(4).crowd().recovery(12).wave(8.5, 10, 1.0, 1, 0),
            DmSpec.move(BULL_CHARGE, "Bull Charge").power(20).wind(22).cd(190).range(7, 30).weight(5).recovery(14).charge(24, 3.2, 18, 1.5, 1.5),
            DmSpec.move(HOWL, "Howl").power(20).wind(28).cd(420).range(0, 16).weight(2).crowd().recovery(12).custom(12, HOWL_SHOUT),
            DmSpec.move(HUNTERS_LEAP, "Hunter's Leap").power(40).wind(26).cd(240).range(8, 32).weight(3).recovery(12).leap(6.5, 12, 1.1, 8),
            DmSpec.move(HORN_SWEEP, "Horn Sweep").power(40).wind(20).cd(150).range(0, 6.5).weight(3).crowd().recovery(12).spin(5.6, 11, 1.3),
            DmSpec.move(RUBBLE_THROW, "Rubble Throw").power(40).wind(24).cd(210).range(8, 30).weight(3).recovery(10).marks(2, 2.6, 10, 0.9, 14),
            DmSpec.move(RAMPAGE, "Rampage").power(60).wind(30).cd(380).range(4, 26).weight(3).crowd().recovery(16).custom(11, RAMPAGE_DASH),
            DmSpec.move(BULL_RUN, "The Bull Run").power(60).wind(58).cd(1300).range(0, 40).weight(5).crowd().ultimate().custom(36, BULL_RUN_HIT),
            // the leveled form (Asterius): appended, never reordered
            DmSpec.move(WILD_CROSS, "Wild Cross").power(100).wind(26).cd(200).range(0, 9).weight(4).recovery(12).leveled().custom(9, CROSS_LANE));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<DmSpec> specs() { return SPECS; }

    public static DmSpec byId(String id) {
        for (DmSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
