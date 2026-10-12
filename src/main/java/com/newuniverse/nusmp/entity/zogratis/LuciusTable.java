package com.newuniverse.nusmp.entity.zogratis;

import java.util.List;

/**
 * Lucius's move table (pure, unit tested): every move's numbers in one place, as {@link MoveSpec}s the scheduler can read without
 * Minecraft. {@code LuciusBoss} turns each entry into a {@link Move} by adding its telegraph and its code, looked up by id.
 * <p>
 * The shape of the fight: a calm, fast caster who stands at a distance and answers everything. Soul Magic is his own (motes, the
 * drain, Paladins, resurrection, the purge), the towers answer with any spell of any magic, the siblings' Body, Blood, Bone and
 * Gravity are borrowed, and Astaroth's Time bends the fight (light steps, the stasis, the rewind, acceleration, and precognition as a
 * passive). Four ultimates arrive as his power grows: the Soul Purge (40%), Sibling Assimilation (60%), the Time Stop and the
 * Grimoire Tower Storm (80%). Every move has a wind-up of at least eight ticks at every power and a mark or a line to read.
 */
public final class LuciusTable {
    private LuciusTable() {}

    public static final String SOUL_MOTES = "soul_motes", SOUL_DRAIN = "soul_drain", LIGHT_STEP = "light_step", TOWER_SPELL = "tower_spell",
            BONE_ARSENAL = "bone_arsenal", MIRROR_COURT = "mirror_court", BLOOD_TIDE = "blood_tide", GRAVITY_CRUSH = "gravity_crush",
            SWAP_STRIKE = "swap_strike", MEMORY_SCRAMBLE = "memory_scramble", TIME_HALT = "time_halt", ACCELERATE = "accelerate",
            PALADIN_CALL = "paladin_call", TIME_REVERSAL = "time_reversal", RESURRECTION = "resurrection", LIGHT_COMBO = "light_combo",
            SOUL_PURGE = "soul_purge", SIBLINGS = "siblings_assimilation", TIME_STOP = "time_stop", TOWER_STORM = "tower_storm";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float MOTE = 6.5f, DRAIN_TICK = 7f, STEP = 20f, HALT_BLOW = 24f, STOP_BLOW = 16f, PURGE = 18f, SWAP = 22f,
            BONE_SPEAR = 6.5f, BONE_BLADE = 8f, BLOOD_LANCE = 5.5f, SIBLING_SPEAR = 7.5f, SIBLING_LANCE = 6.0f,
            LASH = 5f, MEMORY_TOUCH = 4f, STORM_BOLT = 11f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as {@link Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(SOUL_MOTES, "Soul Motes", 20, 16, 80, 0, 38, 6, false, false, 6),
            new Spec(SOUL_DRAIN, "Soul Drain", 20, 26, 240, 3, 20, 4, false, false, 10),
            new Spec(LIGHT_STEP, "Beyond Light", 20, 16, 110, 5, 34, 5, false, false, 8),
            new Spec(TOWER_SPELL, "The Towers Answer", 20, 26, 130, 6, 40, 7, false, false, 8),
            new Spec(BONE_ARSENAL, "Bone Arsenal", 20, 22, 170, 4, 34, 4, false, false, 8),
            new Spec(MIRROR_COURT, "Mirror Court", 20, 28, 640, 0, 48, 2, false, true, 10),
            new Spec(BLOOD_TIDE, "Blood Tide", 40, 24, 240, 3, 30, 3, false, false, 8),
            new Spec(GRAVITY_CRUSH, "Lucifero's Weight", 40, 30, 290, 3, 34, 3, false, true, 10),
            new Spec(SWAP_STRIKE, "Swap Strike", 40, 26, 260, 6, 40, 3, false, false, 10),
            new Spec(MEMORY_SCRAMBLE, "Memory Manipulation", 40, 30, 520, 0, 40, 2, false, true, 10),
            new Spec(TIME_HALT, "Chrono Stasis", 40, 34, 420, 4, 30, 3, false, false, 12),
            new Spec(ACCELERATE, "Chrono Acceleration", 40, 20, 700, 0, 48, 2, false, false, 6),
            new Spec(PALADIN_CALL, "Soul Magic: Paladins", 40, 40, 900, 0, 48, 2, false, false, 10),
            new Spec(TIME_REVERSAL, "Time Reversal", 60, 36, 900, 0, 48, 2, false, false, 12),
            new Spec(RESURRECTION, "Resurrection", 60, 44, 800, 0, 48, 2, false, false, 10),
            new Spec(LIGHT_COMBO, "Beyond Light: Barrage", 60, 18, 380, 5, 30, 3, false, false, 14),
            new Spec(SOUL_PURGE, "Soul Purge", 40, 64, 1500, 0, 30, 4, true, true, 60),
            new Spec(SIBLINGS, "Sibling Assimilation", 60, 70, 1500, 0, 40, 5, true, true, 60),
            new Spec(TIME_STOP, "Time Stop", 80, 64, 1500, 0, 30, 5, true, true, 60),
            new Spec(TOWER_STORM, "Grimoire Tower Storm", 80, 72, 1500, 0, 44, 5, true, true, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
