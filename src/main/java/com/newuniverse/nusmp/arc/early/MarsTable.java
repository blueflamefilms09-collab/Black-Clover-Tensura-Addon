package com.newuniverse.nusmp.arc.early;

import java.util.List;

/**
 * Why: Mars's move table and numbers in one pure place (read by the boss class, checked by {@code EarlyTablesTest}). After the anime (the Dungeon
 * Exploration Arc): a Diamond Kingdom weapon with mage stones set in his body, and Crystal Magic: Harpe (crystal blades round him), Nemean Armor (crystal over
 * his body), crystal clones that multiply, the Talos Puppet Cluster and the Heavy Armored Titan. The fight is built on the things that set him apart: the
 * stones (the {@link StoneLedger}), the armor that has a front (the {@link FrontGuard}), the doubles that strike from three sides at once (the middle of
 * the triangle is the safe place), and the Titan, whose rings are jumped.
 */
public final class MarsTable {
    private MarsTable() {}

    public static final String HARPE = "harpe_blades", SLASH = "crystal_slash", LANCE = "crystal_lance", FIELD = "crystal_field", ARMOR = "nemean_armor",
            RUSH = "crystal_rush", DOUBLES = "crystal_doubles", NOVA = "shatter_nova", PUPPETS = "talos_puppets", TITAN = "heavy_armored_titan";

    /** Base damage of the moves. */
    public static final float HARPE_HIT = 5.5f, SLASH_HIT = 12f, LANCE_HIT = 11f, FIELD_HIT = 12f, RUSH_HIT = 10f, DOUBLE_HIT = 11f, NOVA_HIT = 10f,
            PUPPET_HIT = 9f, TITAN_HIT = 20f, TITAN_AFTER = 8f;

    /** The stones: how many, how long one shows, and the share of his maximum health that cracks it. A cracked stone staggers him for {@value #STONE_STAGGER} ticks. */
    public static final int STONES = 3, STONE_WINDOW = 240, STONE_STAGGER = 50;
    public static final double STONE_SHARE = 0.07;
    /** A staggered Mars takes this much more damage and starts nothing. */
    public static final float STAGGER_BONUS = 1.3f;

    /** The Nemean Armor stands this long. */
    public static final int ARMOR_TICKS = 140;

    /** Radii and reaches. */
    public static final double SLASH_REACH = 4.6, SLASH_HALF_ANGLE = 0.9, LANCE_LENGTH = 22, LANCE_HALF = 1.1, FIELD_RADIUS = 4.0, DOUBLES_RING = 4.2, DOUBLES_FANG = 2.2,
            NOVA_RADIUS = 7.0, PUPPET_BLAST = 2.2, TITAN_RADIUS = 9.0, TITAN_RINGS = 12.0, RUSH_LENGTH = 16, RUSH_HALF = 1.3;
    /** The Titan: ticks between its rings. */
    public static final int TITAN_GAP = 14;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(HARPE, "Harpe", 20, 18, 100, 4, 26, 6),
            MoveRow.of(SLASH, "Crystal Slash", 20, 16, 70, 0, 5, 6),
            MoveRow.of(LANCE, "Crystal Lance", 20, 20, 130, 5, 26, 5),
            MoveRow.of(FIELD, "Crystal Field", 20, 22, 150, 3, 26, 5),
            MoveRow.of(ARMOR, "Nemean Armor", 20, 18, 400, 0, 48, 2),
            MoveRow.of(RUSH, "Crystal Rush", 20, 18, 150, 7, 24, 3),
            MoveRow.crowd(DOUBLES, "Crystal Doubles", 40, 26, 240, 4, 26, 4),
            MoveRow.crowd(NOVA, "Shatter Nova", 40, 22, 200, 0, 8, 4),
            MoveRow.crowd(PUPPETS, "Talos Puppet Cluster", 60, 24, 320, 6, 30, 3),
            MoveRow.ultimate(TITAN, "Heavy Armored Titan", 60, 56, 1500, 0, 30, 5));

    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) { return MoveRow.find(ROWS, id); }

    /** Blades of the Harpe at a power's cast slots (1 to 4): three to six. */
    public static int harpe(int castSlots) { return 2 + Math.max(1, Math.min(4, castSlots)); }

    /** Puppets of the Cluster: three, four at the Gates. */
    public static int puppets(boolean gates) { return gates ? 4 : 3; }

    /** Fangs of the Doubles: three; four from 80% power. */
    public static int doubles(int stage) { return stage >= 3 ? 4 : 3; }

    /** Damage a showing stone takes to crack, for a boss of this maximum health. */
    public static double stoneDamage(double maxHealth) { return maxHealth * STONE_SHARE; }
}
