package com.newuniverse.nusmp.arc.midnight;

import java.util.List;

/**
 * Why: Fana is the Third Eye's Hatred, an elf soul in the body of a Diamond Kingdom orphan, who burns forests when she is angry and calls the fire spirit
 * Salamander to fight beside her. Her fight is a lesson in fire that can be read: Salamander's breath and claw for the middle, a fireball that the story
 * showed being struck back by an anti-magic sword, pillars and rains of embers for the crowd, a robe of phoenix feathers that only water and ice go through,
 * and, when the Gates open, the self-destruction that her Third Eye drives and that the party can end by breaking the Eye. This table is where those numbers
 * live (pure, unit tested), so {@code FanaBoss} only has to turn each row into a telegraph and a strike.
 */
public final class FanaTable {
    private FanaTable() {}

    public static final String FIREBALL = "fireball", BREATH = "salamander_breath", CLAW = "salamander_claw", CHARGE = "salamander_charge",
            SPIRAL = "spiral_flames", COLUMNS = "ignis_columna", ROAR = "salamander_roar", FIRE_MAGIC = "fire_magic", ERUPTION = "crimson_eruption",
            ROBE = "phoenix_robe", ARMS = "flame_burial", EMBERS = "ember_rain", EVIL_HEART = "evil_heart";

    /** Base damage of the moves, before the boss's damage law (power tuning, the victim's EP, the balance law's caps). */
    public static final float FIREBALL_HIT = 14f, BREATH_HIT = 10f, CLAW_HIT = 13f, CHARGE_HIT = 14f, SPIRAL_HIT = 11f, COLUMN_HIT = 11f, ROAR_HIT = 5f,
            ERUPTION_HIT = 10f, ARMS_HIT = 12f, EMBER_HIT = 8f, DETONATION = 32f, FALLBACK = 8f;

    /** The moves in the order the boss lists them (the client reads names by index: new moves are only ever appended). */
    private static final List<MidnightSpec> SPECS = List.of(
            new MidnightSpec(FIREBALL, "Maximum Exploding Fireball", 20, 22, 120, 8, 40, 5, false, false, 8),
            new MidnightSpec(BREATH, "Salamander's Breath", 20, 18, 100, 2, 16, 5, false, false, 8),
            new MidnightSpec(CLAW, "Salamander's Claw", 20, 16, 70, 0, 6.5, 6, false, false, 10),
            new MidnightSpec(CHARGE, "Salamander's Charge", 20, 24, 200, 6, 32, 4, false, false, 10),
            new MidnightSpec(SPIRAL, "Profound Spiral Flames", 20, 22, 150, 8, 36, 4, false, false, 8),
            new MidnightSpec(COLUMNS, "Ignis Columna", 20, 26, 190, 3, 36, 4, false, true, 10),
            new MidnightSpec(ROAR, "Salamander's Roar", 20, 20, 300, 0, 11, 2, false, true, 12),
            new MidnightSpec(FIRE_MAGIC, "Fire Magic", 20, 18, 240, 6, 32, 2, false, false, 8),
            new MidnightSpec(ERUPTION, "Crimson Eruption", 40, 28, 230, 0, 14, 4, false, true, 14),
            new MidnightSpec(ROBE, "Phoenix Feathers Robe", 40, 24, 760, 0, 48, 1, false, false, 10),
            new MidnightSpec(ARMS, "Calidus Brachium: Flame Burial", 60, 30, 260, 0, 36, 3, false, false, 10),
            new MidnightSpec(EMBERS, "Rain of Embers", 60, 32, 340, 0, 34, 3, false, true, 12),
            new MidnightSpec(EVIL_HEART, "Evil Heart", 100, 60, 1500, 0, 48, 6, true, true, 60));

    public static List<MidnightSpec> specs() { return SPECS; }

    public static MidnightSpec byId(String id) {
        for (MidnightSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
