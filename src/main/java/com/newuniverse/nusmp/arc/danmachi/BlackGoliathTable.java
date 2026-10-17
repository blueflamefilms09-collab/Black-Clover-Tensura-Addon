package com.newuniverse.nusmp.arc.danmachi;

import java.util.List;

/**
 * Why: the Black Goliath's moves are numbers first (range windows, telegraphs, shapes, damage) and the table must be checkable without a game: every move
 * has a telegraph that is still 12 ticks (an ultimate 42) at the Gates, an attack exists at every distance, the ultimate is rare and leaves it exposed. This pure table is that;
 * {@code BlackGoliathBoss} turns each row into a framework {@code Move}.
 *
 * <p>The shape of the fight (the Dungeon's own answer to a god's power, the irregular Goliath of the 18th floor, see {@code docs/arc/black_goliath.md}): a
 * giant that is faster than the stone Goliath, with long reach and slow telegraphs. Fists and sweeps up close, boulders from afar, a shoulder rush, spokes
 * of cracks that leave gaps to stand in, a roar that calls monsters and another that frightens, and the Dungeon's Wrath, which sends rings and cracks across
 * the whole room. The leveled form adds the Titan Slam. New rows are only appended.
 */
public final class BlackGoliathTable {
    private BlackGoliathTable() {}

    public static final String BLACK_FIST = "black_fist", GIANTS_SWEEP = "giants_sweep", DUNGEON_STOMP = "dungeon_stomp", BOULDER_TOSS = "boulder_toss",
            WAILING_HOWL = "wailing_howl", SHOULDER_RUSH = "shoulder_rush", FISSURE_SPOKES = "fissure_spokes", MONSTER_CALL = "monster_call",
            DUNGEONS_WRATH = "dungeons_wrath", TITAN_SLAM = "titan_slam";

    /** Base damages of the custom moves. */
    public static final float WRATH_RING = 14f, WRATH_SPOKE = 15f, HOWL_SHOUT = 5f;

    private static final List<DmSpec> SPECS = List.of(
            DmSpec.move(BLACK_FIST, "Black Fist").power(20).wind(20).cd(90).range(0, 6).weight(6).recovery(12).cone(5.4, 40, 20, 1.6, true),
            DmSpec.move(GIANTS_SWEEP, "Giant's Sweep").power(20).wind(22).cd(140).range(0, 8.5).weight(4).crowd().recovery(12).cone(8.0, 100, 14, 1.4, false),
            DmSpec.move(DUNGEON_STOMP, "Dungeon Stomp").power(20).wind(24).cd(170).range(0, 11).weight(4).crowd().recovery(12).wave(11, 13, 1.1, 2, 12),
            DmSpec.move(BOULDER_TOSS, "Boulder Toss").power(20).wind(26).cd(200).range(8, 34).weight(4).recovery(10).marks(2, 2.8, 12, 1.0, 16),
            DmSpec.move(WAILING_HOWL, "Wailing Howl").power(20).wind(30).cd(450).range(0, 18).weight(2).crowd().recovery(12).custom(14, HOWL_SHOUT),
            DmSpec.move(SHOULDER_RUSH, "Shoulder Rush").power(20).wind(22).cd(210).range(6, 28).weight(4).recovery(14).charge(18, 3.6, 19, 1.4, 1.2),
            DmSpec.move(FISSURE_SPOKES, "Fissure Spokes").power(40).wind(26).cd(240).range(0, 22).weight(3).crowd().recovery(12).spokes(3, 18, 2.4, 15, 1.0),
            DmSpec.move(MONSTER_CALL, "Monster Call").power(40).wind(30).cd(720).range(0, 48).weight(2).recovery(10).custom(0, 0),
            DmSpec.move(DUNGEONS_WRATH, "Dungeon's Wrath").power(60).wind(60).cd(1400).range(0, 40).weight(5).crowd().ultimate().custom(14, WRATH_RING),
            // the leveled form: appended, never reordered
            DmSpec.move(TITAN_SLAM, "Titan Slam").power(100).wind(28).cd(260).range(6, 34).weight(4).recovery(14).leveled().leap(8, 18, 1.3, 9));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<DmSpec> specs() { return SPECS; }

    public static DmSpec byId(String id) {
        for (DmSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
