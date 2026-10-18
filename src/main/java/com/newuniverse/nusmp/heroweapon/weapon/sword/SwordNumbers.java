package com.newuniverse.nusmp.heroweapon.weapon.sword;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner's balance law for the hero weapons (design.md section 5) is a list of numbers, and the Legendary Sword is the weapon that hits most often and most
 * fast, which is exactly where one careless edit (a cooldown of 20, a fifth pulse) makes it break the law. So every number of the Sword lives HERE, in plain constants
 * and one table with a row per technique; the techniques read their cooldown, cost and damage from the table, the unit test walks the table against the budget, and the
 * in-game self-test checks that the skill objects really carry the table's values. No Minecraft import, so the test runs anywhere. Raw damage is the base number before
 * the bond level's factor (1.0 to 2.2) and the balance law's existence scaling.
 */
public final class SwordNumbers {
    private SwordNumbers() {}

    /** The budget class of a technique (design.md section 5): what the test holds its row to. */
    public enum Kind { UTILITY, DAMAGE, HEAVY, ULTIMATE }

    /**
     * One technique's row. {@code raw} is the raw damage of one hit at its strongest and {@code hits} how many such hits one target can take from one cast, so
     * {@link #total()} is the most one target can take from one cast. Cooldown in ticks before the level scaling, cost a share of the maximum magicules.
     */
    public record Spec(String id, String name, Kind kind, int cooldown, double cost, double raw, int hits, boolean curse) {
        public double total() { return raw * hits; }
    }

    // ------------------------------------------------------------------ the budget (design.md section 5)
    public static final double ORDINARY_MAX = 12, HEAVY_MAX = 20, ULTIMATE_MAX = 32;
    /** The smallest raw damage one hit of a technique may have (design.md section 5: 3 to 12 for an ordinary technique). */
    public static final double MIN_HIT = 3;
    public static final int MAX_TARGETS = 12;
    public static final int MIN_DAMAGE_COOLDOWN = 60, MIN_UTILITY_COOLDOWN = 100, MIN_ULTIMATE_COOLDOWN = 600;
    public static final double MIN_COST = 0.01, MAX_COST = 0.03, MIN_ULTIMATE_COST = 0.05, MAX_ULTIMATE_COST = 0.08;
    /** The longest any repeating task of a technique may run (20 s) and the most entities one technique may make. */
    public static final int MAX_TASK_TICKS = 400, MAX_SPAWNS = 6;
    /** The bonus strike of a passive or of the Flow is at most this much raw damage (a bonus is always a small addition to a blow). */
    public static final double BONUS_MAX = 6;

    // ------------------------------------------------------------------ the table
    private static final Map<String, Spec> TABLE = new LinkedHashMap<>();

    private static Spec row(String id, String name, Kind kind, int cooldown, double cost, double raw, int hits, boolean curse) {
        Spec s = new Spec(id, name, kind, cooldown, cost, raw, hits, curse);
        TABLE.put(id, s);
        return s;
    }

    // the ordinary tree (14) ...
    public static final Spec BLITZ = row("sword.blitz", "Blitz Strike", Kind.DAMAGE, 70, 0.015, 8, 1, false);
    public static final Spec TEMPEST = row("sword.tempest", "Tempest Dance", Kind.DAMAGE, 90, 0.02, 5, 2, false);
    public static final Spec AFTERIMAGE = row("sword.afterimage", "Afterimage Step", Kind.UTILITY, 100, 0.015, 0, 0, false);
    public static final Spec FLASHING = row("sword.flashing", "Flashing Sword", Kind.UTILITY, 100, 0.015, 4, 1, false);
    public static final Spec WIND_CUTTER = row("sword.wind_cutter", "Wind Cutter Whistle", Kind.DAMAGE, 90, 0.015, 4, 3, false);
    public static final Spec METEOR = row("sword.meteor", "Meteor Sword", Kind.DAMAGE, 160, 0.025, 3, 4, false);
    public static final Spec ASSASSIN = row("sword.assassin", "Assassin Sword", Kind.DAMAGE, 160, 0.02, 12, 1, false);
    public static final Spec EAGLE_BLADE = row("sword.eagle_blade", "Eagle Blade", Kind.HEAVY, 200, 0.025, 16, 1, false);
    public static final Spec GRAVITY = row("sword.gravity", "Gravity Sword", Kind.DAMAGE, 280, 0.025, 3, 3, false);
    public static final Spec CLEAVE = row("sword.cleave", "Skyfall Cleave", Kind.HEAVY, 260, 0.03, 16, 1, false);
    public static final Spec PHOENIX_GALE = row("sword.phoenix_gale", "Phoenix Gale Blade", Kind.HEAVY, 300, 0.03, 9, 2, false);
    public static final Spec FLOAT_BLADES = row("sword.float_blades", "Float Sword", Kind.DAMAGE, 400, 0.03, 3, 4, false);
    public static final Spec HUNDRED_SWORDS = row("sword.hundred_swords", "Hundred Swords", Kind.HEAVY, 260, 0.03, 4, 4, false);
    public static final Spec LIGHTNING_HUNDRED = row("sword.lightning_hundred", "Lightning Hundred Swords", Kind.ULTIMATE, 900, 0.07, 3.6, 8, false);
    // ... and the Greed and Gluttony series (5)
    public static final Spec GUILLOTINE = row("sword.guillotine", "Guillotine", Kind.HEAVY, 360, 0.03, 16, 1, true);
    public static final Spec GOLD_REBELLION = row("sword.gold_rebellion", "Gold Rebellion", Kind.DAMAGE, 300, 0.025, 10, 1, true);
    public static final Spec MANEATER = row("sword.maneater", "Maneater", Kind.HEAVY, 200, 0.025, 14, 1, true);
    public static final Spec STRONG_DECLINE = row("sword.strong_decline", "Strong Decline", Kind.HEAVY, 320, 0.03, 16, 1, true);
    public static final Spec INSATIABLE_MAW = row("sword.insatiable_maw", "Insatiable Maw", Kind.ULTIMATE, 1200, 0.08, 9.6, 3, true);

    /** Every row in the order the logic lists the skills. */
    public static List<Spec> all() { return List.copyOf(TABLE.values()); }

    /** The row of a skill id, or null. */
    public static Spec spec(String id) { return TABLE.get(id); }

    // ------------------------------------------------------------------ the passives (ids the logic implements; the forms file lists them)
    public static final Set<String> PASSIVES = Set.of("quick_draw", "wind_walker", "shadow_blade", "star_shower", "gravity_edge", "ember_edge", "carapace_guard",
            "hundred_edge", "thunder_edge", "plunder", "fang_maw");

    // ------------------------------------------------------------------ Tempo (the combo) and the Flow
    /** A blow within this many ticks of the last one keeps the Tempo; the Tempo goes up to {@link #TEMPO_MAX}, and a full one turns into a Flow strike. */
    public static final int TEMPO_WINDOW = 40, TEMPO_MAX = 10;
    /** Tempo at which the wielder runs faster (Speed I, Speed II); the Wind Walker form reaches them one blow earlier. */
    public static final int SPEED_ONE_AT = 3, SPEED_TWO_AT = 7, SPEED_TICKS = 30;
    public static final double FLOW_RAW = 6, GHOST_RAW = 4;
    public static final int GHOST_TICKS = 60;

    // ------------------------------------------------------------------ the form passives
    public static final double QUICK_DRAW_RAW = 3, SHADOW_RAW = 3, STAR_RAW = 4, HUNDRED_EDGE_RAW = 3, THUNDER_RAW = 3, FANG_RAW = 3, GILDED_RAW = 3;
    public static final int QUICK_DRAW_IDLE = 60, STAR_EVERY = 5, STAR_PAUSE = 40, HUNDRED_EDGE_EVERY = 10, THUNDER_PAUSE = 20;
    public static final double STAR_RADIUS = 1.5, HUNDRED_EDGE_RADIUS = 2.5, THUNDER_RANGE = 5, FANG_BELOW = 0.30;
    public static final int GRAVITY_EDGE_TICKS = 30, EMBER_FIRE_SECONDS = 2;
    public static final int CARAPACE_COOLDOWN = 300;
    public static final double CARAPACE_SHARE = 0.5, CARAPACE_THRESHOLD = 0.20, WIND_WALKER_FALL = 0.5;
    /** Plunder (Greed): a blow takes this share of the foe's maximum magicules, of which the wielder keeps at most the second share of their own maximum; at most once per pause. */
    public static final double PLUNDER_TAKE = 0.003, PLUNDER_KEEP = 0.01;
    public static final int PLUNDER_PAUSE = 10, GILDED_TICKS = 120;
    public static final int MARK_CAP = 24;

    // ------------------------------------------------------------------ the techniques
    public static final double BLITZ_DASH = 7, BLITZ_WIDTH = 1.2;
    public static final int BLITZ_DELAY = 3;
    public static final double TEMPEST_RADIUS = 4.5, TEMPEST_PUSH = 0.5;
    public static final int TEMPEST_DELAY = 6, TEMPEST_GAP = 6;
    public static final double STEP_DASH = 5;
    public static final int STEP_SPEED_TICKS = 60, STEP_SPEED_LEVEL = 1, STEP_GUARD_TICKS = 20;
    public static final double FLASH_RANGE = 7, FLASH_HALF_ANGLE = 70;
    public static final int FLASH_BLIND_TICKS = 40, FLASH_GLOW_TICKS = 60, FLASH_SPEED_TICKS = 60;
    public static final double CUTTER_SPEED = 1.5, CUTTER_RADIUS = 0.9, CUTTER_SPREAD = 12;
    public static final int CUTTER_LIFE = 8, CUTTER_COUNT = 3;
    public static final double METEOR_RANGE = 16, METEOR_RING = 1.6, METEOR_RADIUS = 1.8;
    public static final int METEOR_DELAY = 12, METEOR_GAP = 3, METEOR_STARS = 4;
    public static final double ASSASSIN_RANGE = 12, ASSASSIN_SLACK = 4, ASSASSIN_BEHIND = 1.5, ASSASSIN_BASE = 8, ASSASSIN_BACKSTAB = 4;
    public static final int ASSASSIN_DELAY = 5;
    public static final double EAGLE_DASH = 5, EAGLE_BASE = 10, EAGLE_PIERCE = 0.6;
    public static final int EAGLE_DELAY = 8;
    public static final double GRAVITY_RANGE = 14, GRAVITY_RADIUS = 3.5;
    public static final int GRAVITY_PULSES = 3, GRAVITY_GAP = 20, GRAVITY_SLOW_TICKS = 25;
    public static final double CLEAVE_LEAP = 5, CLEAVE_RADIUS = 3.5, CLEAVE_PUSH = 1.0;
    public static final int CLEAVE_DELAY = 12;
    public static final double PHOENIX_RANGE = 7, PHOENIX_HALF_ANGLE = 55, PHOENIX_BIRD_SPEED = 1.4, PHOENIX_BIRD_RADIUS = 1.2, PHOENIX_STORM = 9, PHOENIX_BIRD = 9;
    public static final int PHOENIX_DELAY = 18, PHOENIX_BIRD_LIFE = 11, PHOENIX_FIRE_SECONDS = 4;
    public static final int FLOAT_TICKS = 160, FLOAT_GAP = 20, FLOAT_COUNT = 4, FLOAT_BLOCKS = 3;
    public static final double FLOAT_RADIUS = 3.5, FLOAT_GUARD_RADIUS = 3.0;
    public static final double HUNDRED_RANGE = 14, HUNDRED_RADIUS = 5;
    public static final int HUNDRED_DELAY = 10, HUNDRED_PULSES = 4, HUNDRED_GAP = 10;
    public static final double LIGHTNING_RANGE = 16, LIGHTNING_RADIUS = 7;
    public static final int LIGHTNING_DELAY = 18, LIGHTNING_PULSES = 8, LIGHTNING_GAP = 5;

    // ------------------------------------------------------------------ the Greed and Gluttony series
    public static final double GUILLOTINE_RANGE = 12, STEAL_TAKE = 0.04, STEAL_KEEP = 0.02;
    public static final int GUILLOTINE_DELAY = 16, GUILLOTINE_CORRUPTION = 40;
    public static final double REBELLION_RADIUS = 5;
    public static final int REBELLION_DELAY = 6;
    public static final double MANEATER_RANGE = 4.5, MANEATER_HALF_ANGLE = 50, MANEATER_HEAL = 0.04;
    public static final int MANEATER_BITES = 3, MANEATER_DELAY = 8;
    public static final double DECLINE_LENGTH = 10, DECLINE_HALF_WIDTH = 1.2;
    public static final int DECLINE_DELAY = 14, DECLINE_SLOW_TICKS = 40, DECLINE_CORRUPTION = 30;
    public static final double MAW_RADIUS = 7, MAW_HEAL = 0.06;
    public static final int MAW_DELAY = 30, MAW_WAVES = 3, MAW_GAP = 10, MAW_CORRUPTION = 100;
}
