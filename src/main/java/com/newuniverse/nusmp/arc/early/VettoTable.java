package com.newuniverse.nusmp.arc.early;

import java.util.List;

/**
 * Why: Vetto's move table and numbers in one pure place (read by the boss class, checked by {@code EarlyTablesTest}). After the anime (the Seabed Temple
 * arc): an elf in a human body, tall and brutal, whose Beast Magic wraps animal auras round him (Bear Claw, Cheetah Charge ...), who closes mortal wounds,
 * and who in the end released all his mana to bring the temple down. The fight is the owner's reference for anti-magic against overwhelming physical stats:
 * the {@link BeastAuras} are what makes him overwhelming and what an anti-magic blow or a burst of damage tears off; the Mana Release is the ritual that
 * can be broken; the rest are heavy things to step out of. He is a brawler: he wants to be within a few blocks.
 */
public final class VettoTable {
    private VettoTable() {}

    public static final String CLAW = "bear_claw", FLURRY = "rending_flurry", CHARGE = "cheetah_charge", DIVE = "talon_dive", POUND = "ground_pound",
            ROAR = "beast_roar", SHIFT = "aura_shift", STAMPEDE = "stampede", CLAWS = "demon_beast_claws", RELEASE = "mana_release";

    /** Base damage of the moves. */
    public static final float CLAW_HIT = 14f, FLURRY_HIT = 5.5f, CHARGE_HIT = 11f, DIVE_HIT = 14f, POUND_HIT = 13f, ROAR_HIT = 6f, STAMPEDE_HIT = 14f,
            CLAWS_HIT = 10f, RELEASE_HIT = 24f;

    /** Reaches and radii. */
    public static final double CLAW_REACH = 5.2, CLAW_HALF_ANGLE = 0.9, FLURRY_REACH = 4.2, CHARGE_LENGTH = 18, CHARGE_HALF = 1.4, DIVE_RADIUS = 4.5,
            POUND_RADIUS = 6.5, ROAR_RADIUS = 9, STAMPEDE_LENGTH = 24, STAMPEDE_HALF = 2.0, CLAWS_RADIUS = 1.9, RELEASE_RADIUS = 15;
    /** The Mana Release: the share of his maximum health that breaks it, and the share of its blast that reaches the edge. */
    public static final double RELEASE_BREAK = 0.14, RELEASE_EDGE = 0.45;
    /** An aura shift is open to a blow for this long (the wind-up): he takes this much more while he changes. */
    public static final float SHIFT_BONUS = 1.3f;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(CLAW, "Bear Claw", 20, 16, 70, 0, 5.5, 6),
            MoveRow.of(FLURRY, "Rending Flurry", 20, 18, 110, 0, 5, 5),
            MoveRow.of(CHARGE, "Cheetah Charge", 20, 18, 130, 6, 22, 5),
            MoveRow.of(DIVE, "Talon Dive", 20, 22, 170, 7, 30, 4),
            MoveRow.crowd(POUND, "Ground Pound", 20, 20, 150, 0, 8, 4),
            MoveRow.crowd(ROAR, "Beast Roar", 20, 20, 200, 0, 10, 3),
            MoveRow.of(SHIFT, "Aura Shift", 20, 24, 380, 0, 48, 3),
            MoveRow.of(STAMPEDE, "Stampede", 40, 30, 280, 6, 26, 3),
            MoveRow.crowd(CLAWS, "Demon Beast Claws", 60, 24, 240, 3, 24, 4),
            MoveRow.ultimate(RELEASE, "Mana Release", 60, 70, 1500, 0, 40, 5));

    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) { return MoveRow.find(ROWS, id); }

    /** Foes the Demon Beast Claws mark: two more than the power's cast slots allow at the opening, at most five. */
    public static int clawMarks(int castSlots) { return Math.max(2, Math.min(5, castSlots + 1)); }
}
