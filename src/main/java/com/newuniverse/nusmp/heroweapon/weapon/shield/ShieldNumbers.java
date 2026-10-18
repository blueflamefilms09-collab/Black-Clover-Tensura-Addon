package com.newuniverse.nusmp.heroweapon.weapon.shield;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner's balance law for the hero weapons (design.md section 5) is a list of numbers (raw damage per class of technique, the shortest cooldown, the
 * cost in magicules, at most 12 targets), and a later edit of one skill must not be able to break it without a test noticing. So every number of the Legendary
 * Shield lives HERE, in plain constants and one table with a row per skill, and the skills read their cooldown, cost and damage from the table; the unit test
 * walks the table against the budget and the in-game self-test checks that the skill objects really carry the table's values. This class has no Minecraft import,
 * so the test runs anywhere. Raw damage is the base number before the bond level's factor (1.0 to 2.2) and the balance law's existence scaling; a Shield is a
 * defender, so its damage rows are the lowest of the set and most of its power is in the defensive numbers below.
 */
public final class ShieldNumbers {
    private ShieldNumbers() {}

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

    // ------------------------------------------------------------------ the table
    private static final Map<String, Spec> TABLE = new LinkedHashMap<>();

    private static Spec row(String id, String name, Kind kind, int cooldown, double cost, double raw, int hits, boolean curse) {
        Spec s = new Spec(id, name, kind, cooldown, cost, raw, hits, curse);
        TABLE.put(id, s);
        return s;
    }

    // the ordinary tree (14) ...
    public static final Spec CHANGE_ATTACK = row("shield.change_attack", "Change Shield (Attack)", Kind.DAMAGE, 80, 0.015, 12, 1, false);
    public static final Spec AIR_STRIKE = row("shield.air_strike", "Air Strike Shield", Kind.UTILITY, 140, 0.02, 0, 0, false);
    public static final Spec SECOND_SHIELD = row("shield.second_shield", "Second Shield", Kind.UTILITY, 200, 0.02, 0, 0, false);
    public static final Spec COVER = row("shield.cover", "Protect", Kind.UTILITY, 400, 0.03, 0, 0, false);
    public static final Spec PROVOKE = row("shield.provoke", "Provoke", Kind.UTILITY, 160, 0.015, 0, 0, false);
    public static final Spec HERB_MEND = row("shield.herb_mend", "Herb Mend", Kind.UTILITY, 400, 0.03, 0, 0, false);
    public static final Spec PRISON = row("shield.prison", "Shield Prison", Kind.UTILITY, 300, 0.03, 0, 0, false);
    public static final Spec CHAIN = row("shield.chain", "Chain Shield", Kind.DAMAGE, 260, 0.025, 3, 4, false);
    public static final Spec HOOK = row("shield.hook", "Hook", Kind.DAMAGE, 140, 0.02, 4, 1, false);
    public static final Spec IRON_MAIDEN = row("shield.iron_maiden", "Iron Maiden", Kind.HEAVY, 300, 0.03, 20, 1, false);
    public static final Spec NEEDLE_BURST = row("shield.needle_burst", "Needle Burst", Kind.DAMAGE, 100, 0.02, 10, 1, false);
    public static final Spec SHOOTING_STAR = row("shield.shooting_star", "Shooting Star Shield", Kind.DAMAGE, 120, 0.02, 9, 1, false);
    public static final Spec SOUL_AEGIS = row("shield.soul_aegis", "Soul Aegis", Kind.UTILITY, 500, 0.03, 0, 0, false);
    public static final Spec UNDEAD_CONTROL = row("shield.undead_control", "Undead Control", Kind.UTILITY, 400, 0.03, 0, 0, false);
    public static final Spec ABSOLUTE_DEFENCE = row("shield.absolute_defence", "Absolute Defence", Kind.ULTIMATE, 1200, 0.07, 0, 0, false);
    // ... and the Wrath series (4)
    public static final Spec RAGE_BASH = row("shield.rage_bash", "Rage Bash", Kind.DAMAGE, 80, 0.02, 10, 1, true);
    public static final Spec RAGE_ROAR = row("shield.rage_roar", "Rage Roar", Kind.DAMAGE, 300, 0.02, 4, 1, true);
    public static final Spec BLOOD_SACRIFICE = row("shield.blood_sacrifice", "Blood Sacrifice", Kind.HEAVY, 600, 0.03, 18, 1, true);
    public static final Spec MEGIDDO = row("shield.megiddo", "Megiddo Burst", Kind.ULTIMATE, 1200, 0.08, 28, 1, true);

    /** Every row in the order the logic lists the skills. */
    public static List<Spec> all() { return List.copyOf(TABLE.values()); }

    /** The row of a skill id, or null. */
    public static Spec spec(String id) { return TABLE.get(id); }

    // ------------------------------------------------------------------ the passives (ids the logic implements; the forms file lists them)
    public static final Set<String> PASSIVES = Set.of("herb_regen", "balloon_bounce", "hatchling_guard", "gel_absorb", "rotting_endurance", "venom_fang",
            "needle_counter", "bulwark", "soul_eater", "drain_null", "rage_fury");

    // ------------------------------------------------------------------ the guard (the raised shield)
    /** The vanilla delay before a raised shield blocks, in ticks of use. */
    public static final int BLOCK_DELAY = 5;
    /** The guard's Resolve, the damage a raised shield can soak before it breaks, as a multiple of the wielder's maximum health; the Light Metal form's passive raises it. */
    public static final double GUARD_CAPACITY = 1.5, BULWARK_CAPACITY = 2.25;
    /** A block in the first ticks after the guard becomes active is a parry: no Resolve is spent. The window in ticks, wider for the Light Metal form. */
    public static final int PARRY_WINDOW = 8, PARRY_WINDOW_BULWARK = 12;
    /** A second parry counts only this many ticks after the last one, so lowering and raising the guard again and again cannot make every blow a parry. */
    public static final int PARRY_PAUSE = 20;
    /** A broken guard cannot be raised for this long, then comes back with a quarter of its Resolve. */
    public static final int BREAK_TICKS = 60;
    public static final double AFTER_BREAK = 0.25;
    /** Resolve comes back after this many quiet ticks, by this share of the capacity per tick (10 percent a second). */
    public static final int REGEN_DELAY = 30;
    public static final double REGEN_PER_TICK = 0.005;
    /** The Counter charge, 0 to 100: blocked damage as a share of the maximum health, times this; doubled by a parry. It fades slowly after a quiet spell. */
    public static final double COUNTER_MAX = 100, COUNTER_RATE = 1.0, PARRY_COUNTER_FACTOR = 2.0;
    public static final int COUNTER_HOLD = 160;
    public static final double COUNTER_DECAY_PER_TICK = 0.2;

    // ------------------------------------------------------------------ counters on a block (the needle and the viper forms)
    /** The needle counter's final damage may be at most this share of the blow it answers, with this raw cap, at most once per attacker per this many ticks. */
    public static final double NEEDLE_SHARE = 0.5, NEEDLE_RAW_CAP = 5;
    public static final int NEEDLE_ICD = 10, VENOM_TICKS = 80;

    // ------------------------------------------------------------------ party support
    public static final double COVER_SHARE = 0.40, COVER_FLOOR = 0.25, COVER_RADIUS = 12, COVER_KEEP_RADIUS = 20;
    public static final int COVER_TICKS = 160, COVER_MAX_ALLIES = 8;
    public static final double WARD_BANK_SHARE = 0.15, WARD_BANK_CAP = 30, WARD_RANGE = 24;
    public static final int WARD_TICKS = 200, MAX_WARDS = 3;
    public static final double HERB_RADIUS = 5, HERB_SHARE = 0.10;
    public static final int HERB_REGEN_TICKS = 100, HERB_DELAY = 10;
    public static final double AEGIS_RADIUS = 6;
    public static final int AEGIS_TICKS = 240;

    // ------------------------------------------------------------------ the floating shields
    public static final double PANEL_RANGE = 12, PANEL_HALF_WIDTH = 1.6, PANEL_HALF_HEIGHT = 1.6, PANEL_THICKNESS = 0.5, PANEL_PUSH = 0.4;
    public static final int PANEL_HP = 6, PANEL_TICKS = 160, MAX_PANELS = 2;

    // ------------------------------------------------------------------ control
    public static final double PROVOKE_RADIUS = 10;
    public static final int PROVOKE_TICKS = 80;
    public static final double PRISON_RANGE = 14, PRISON_RADIUS = 2.6, PRISON_PULL = 0.35;
    public static final int PRISON_TICKS = 80;
    public static final double CHAIN_RADIUS = 6, CHAIN_PULL = 0.25;
    public static final int CHAIN_TARGETS = 4, CHAIN_PULSES = 4, CHAIN_INTERVAL = 20;
    public static final double HOOK_RANGE = 14, HOOK_STOP = 2.0;
    public static final double UNDEAD_RADIUS = 12;
    public static final int UNDEAD_TICKS = 120;

    // ------------------------------------------------------------------ attacks that spend the Counter
    public static final double CHANGE_RANGE = 3.6, CHANGE_HALF_ANGLE = 55, CHANGE_BASE = 6, CHANGE_SPAN = 6;
    public static final int CHANGE_DELAY = 6;
    public static final double IRON_RANGE = 16, IRON_BASE = 16, IRON_SPAN = 4;
    public static final int IRON_DELAY = 14, CAGE_MARK_TICKS = 100;
    public static final double NEEDLE_BURST_RADIUS = 4.5, NEEDLE_BURST_BASE = 4, NEEDLE_BURST_SPAN = 6, NEEDLE_BURST_SPEND = 0.5;
    public static final int NEEDLE_BURST_DELAY = 6;
    public static final double STAR_SPEED = 1.6, STAR_RADIUS = 1.0, STAR_PUSH = 0.8;
    public static final int STAR_LIFE = 14, STAR_DELAY = 4;

    // ------------------------------------------------------------------ the ultimate of the ordinary tree
    public static final double DEFENCE_RADIUS = 6.5;
    public static final int DEFENCE_TICKS = 100, DEFENCE_ALLY_LEVEL = 2, DEFENCE_SELF_LEVEL = 3, DEFENCE_SLOW_LEVEL = 2;

    // ------------------------------------------------------------------ souls (the Soul Eater form)
    public static final int SOULS_MAX = 5, SOUL_HOLD = 600;

    // ------------------------------------------------------------------ the Wrath series
    public static final double RAGE_BASH_LUNGE = 4, RAGE_BASH_RADIUS = 2.2;
    public static final int RAGE_BASH_FIRE_SECONDS = 3, RAGE_BASH_DELAY = 6;
    public static final double ROAR_RANGE = 8, ROAR_HALF_ANGLE = 60;
    public static final int ROAR_SLOW_TICKS = 60, ROAR_FRENZY_TICKS = 120, ROAR_DELAY = 10;
    /** Blood Sacrifice: the share of the maximum health the wielder gives, the share of it that must remain, the wind-up, the radius, the splash damage, the root and the after-curse. */
    public static final double BLOOD_HEALTH_SHARE = 0.25, BLOOD_HEALTH_FLOOR = 0.30, BLOOD_RADIUS = 3, BLOOD_SPLASH = 8;
    public static final int BLOOD_DELAY = 20, BLOOD_ROOT_TICKS = 30, BLOOD_CURSE_TICKS = 300, BLOOD_CORRUPTION = 80;
    /** Megiddo Burst: the same kind of price, a bigger wind-up and radius. */
    public static final double MEGIDDO_HEALTH_SHARE = 0.30, MEGIDDO_HEALTH_FLOOR = 0.25, MEGIDDO_RADIUS = 9;
    public static final int MEGIDDO_DELAY = 40, MEGIDDO_CORRUPTION = 150;
    /** Rage Fury: the strength a hit taken gives, in ticks, and the most levels it stacks to. */
    public static final int FURY_TICKS = 80, FURY_MAX_LEVEL = 1;

    // ------------------------------------------------------------------ the form passives
    public static final double GEL_SHARE = 0.25, GEL_CAP = 0.08;
    public static final int ENDURANCE_COOLDOWN = 1200, ENDURANCE_REGEN_TICKS = 100;
    public static final double ENDURANCE_BELOW = 0.30;
    public static final double HATCHLING_RADIUS = 12;
    public static final double SOUL_KILL_RADIUS = 16;
}
