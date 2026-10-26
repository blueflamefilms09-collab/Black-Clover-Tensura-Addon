package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Kind;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Mode;

import java.util.List;
import java.util.Locale;

/**
 * Why: the numbers of Hercules, Lord of Martial Prowess (docs/veldanava_skills.md) in one place that names no Minecraft type, so the skill, the events, the client's picture, the unit tests and the CI
 * self-test read the same table. Ten modes like every Veldanava skill: four toggles (Adamantine Body, God of Strength, Unstoppable Momentum, Colossal Vigor), five actives (Earth-Shattering Tremor, Invictus,
 * Titan's Grip, Devastating Throw, Battle Roar) and the mastery active World-Breaking Strike. Costs are in Magicule. The order of the modes is the order of the owner's skill file.
 */
public final class HerculesRules {
    private HerculesRules() {}

    public static final JudgmentRules.Spec HERCULES = new JudgmentRules.Spec("hercules", "Hercules", "Lord of Martial Prowess", List.of(
            new Mode(0, "adamantine_body", "Adamantine Body", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(1, "god_of_strength", "God of Strength", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(2, "unstoppable_momentum", "Unstoppable Momentum", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(3, "colossal_vigor", "Colossal Vigor", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(4, "earth_shattering_tremor", "Earth-Shattering Tremor", Kind.PRESS, 3000, 2000, 25, 15, false),
            new Mode(5, "invictus", "Invictus", Kind.CROUCH_PRESS, 8000, 5500, 90, 60, false),
            new Mode(6, "titans_grip", "Titan's Grip", Kind.PRESS, 2500, 1500, 20, 12, false),
            new Mode(7, "devastating_throw", "Devastating Throw", Kind.CROUCH_PRESS, 4000, 2500, 30, 18, false),
            new Mode(8, "battle_roar", "Battle Roar", Kind.PRESS, 5000, 3200, 45, 30, false),
            new Mode(9, "world_breaking_strike", "World-Breaking Strike", Kind.PRESS, 15000, 15000, 180, 180, false)));

    /** Hercules' skill code in the player's mask (bits 5..7), see {@link JudgmentRules#code}. */
    public static final int CODE = 5;

    // ---------------------------------------------------------------- Adamantine Body (toggle 0)
    /** The share of a physical / magical blow that is taken (10% / 50%: 90% / 50% less). */
    public static final double PHYSICAL_TAKEN = 0.10, MAGIC_TAKEN = 0.50;
    /** The least damage taken off a blow for the picture of the plates to be shown, and the gap between two pictures (ticks). */
    public static final float BODY_FX_MIN = 3.0f;
    public static final int BODY_FX_GAP_TICKS = 10;

    /** The damage left of a blow on Adamantine Body: a tenth of a physical blow, half of a magical one. Never negative. */
    public static float adamantine(float amount, boolean magic) {
        if (amount <= 0f) return 0f;
        return (float) (amount * (magic ? MAGIC_TAKEN : PHYSICAL_TAKEN));
    }

    private static final String[] FIRE_WORDS = {"fire", "flame", "lava", "burn", "magma", "hot_floor", "inferno", "fireball"};

    /** True for a damage type path of fire (flames, lava, burning, a fireball): Adamantine Body takes none of it. */
    public static boolean fire(String damageTypePath) {
        if (damageTypePath == null) return false;
        String s = damageTypePath.toLowerCase(Locale.ROOT);
        for (String w : FIRE_WORDS) if (s.contains(w)) return true;
        return false;
    }

    // ---------------------------------------------------------------- God of Strength (toggle 1)
    /** The multiplier of a melee blow (normal / mastered), before the Balance Law. */
    public static final double STRENGTH = 3.0, STRENGTH_MASTERED = 4.5;
    /** The hardest block the hand breaks in one hit (obsidian is 50; bedrock and the like are -1 and are never touched). */
    public static final double BREAK_MAX_HARDNESS = 50.0;
    /** The shockwave of a melee blow: the reach (blocks), the most enemies it hurts, the share of the blow they take, the gap between two shockwaves of one player (ticks). */
    public static final double SHOCK_RADIUS = 3.0, SHOCK_SHARE = 0.35;
    public static final int SHOCK_MAX = 6, SHOCK_GAP_TICKS = 6;

    /** The damage of a melee blow of God of Strength: the blow times the multiplier. */
    public static float strength(float amount, boolean mastered) {
        return amount <= 0f ? 0f : (float) (amount * (mastered ? STRENGTH_MASTERED : STRENGTH));
    }

    /** True when a block of this hardness breaks in one hit (it exists, it can be broken at all and is not harder than {@link #BREAK_MAX_HARDNESS}). */
    public static boolean instantBreak(float hardness) { return hardness >= 0f && hardness <= BREAK_MAX_HARDNESS; }

    /** The break speed that makes a block of this hardness break at once: more than the 100 per point of hardness the game needs for a block the tool cannot harvest. */
    public static float instantSpeed(float hardness) { return Math.max(1f, hardness) * 100f + 1f; }

    /** What a shockwave hurts for: a share of the blow that caused it. */
    public static float shockDamage(float blow) { return blow <= 0f ? 0f : (float) (blow * SHOCK_SHARE); }

    // ---------------------------------------------------------------- Unstoppable Momentum (toggle 2)
    /** The blocks the owner must cover in a second to count as moving, the seconds of movement before the first stage, the seconds each next stage needs, the highest stage. */
    public static final double MOVING_BLOCKS_PER_SECOND = 2.0;
    public static final int STAGE_START_SECONDS = 3, STAGE_STEP_SECONDS = 3, STAGE_MAX = 3, SECONDS_MAX = 99;
    /** What each stage adds: attack speed, movement speed (shares of the base) and the share of the damage the armour took off that comes back (normal / mastered). */
    public static final double ATTACK_SPEED_PER_STAGE = 0.12, MOVE_SPEED_PER_STAGE = 0.07, PIERCE_PER_STAGE = 0.15, PIERCE_PER_STAGE_MASTERED = 0.20;

    /** The seconds of continuous movement after one more second: one more when the owner covered the distance, none when they stood or crept. */
    public static int momentumSeconds(int before, double blocksMoved) {
        if (blocksMoved < MOVING_BLOCKS_PER_SECOND) return 0;
        return Math.min(SECONDS_MAX, Math.max(0, before) + 1);
    }

    /** The stage (0..3) after this many seconds of moving: 3 s = 1, 6 s = 2, 9 s = 3. */
    public static int momentumStage(int seconds) {
        if (seconds < STAGE_START_SECONDS) return 0;
        return Math.min(STAGE_MAX, 1 + (seconds - STAGE_START_SECONDS) / STAGE_STEP_SECONDS);
    }

    /** The share of the armour's saving a blow takes back at this stage: the damage the armour took off (original - after armour) times the pierce share, never more than what was taken off. */
    public static float armourPierce(float original, float afterArmour, int stage, boolean mastered) {
        if (stage <= 0 || original <= afterArmour) return 0f;
        double share = Math.min(1.0, stage * (mastered ? PIERCE_PER_STAGE_MASTERED : PIERCE_PER_STAGE));
        return (float) ((original - afterArmour) * share);
    }

    // ---------------------------------------------------------------- Colossal Vigor (toggle 3)
    /** The share of the maximum health a second heals out of combat (normal / mastered), the ticks without a blow given or taken that count as out of combat. */
    public static final double REGEN_SHARE = 0.02, REGEN_SHARE_MASTERED = 0.04;
    public static final int CALM_TICKS = 100;
    /** The share of a blow dealt that heals the owner in combat (normal / mastered) and the most one blow heals (share of the maximum health). */
    public static final double LIFESTEAL = 0.15, LIFESTEAL_MASTERED = 0.25, LIFESTEAL_CAP = 0.08;

    /** True when the owner neither struck nor was struck for {@link #CALM_TICKS}. */
    public static boolean calm(long now, long lastHurt, long lastStruck) { return now - Math.max(lastHurt, lastStruck) >= CALM_TICKS; }

    /** The Hit Points a second of calm heals. */
    public static float regen(float maxHealth, boolean mastered) { return (float) (Math.max(0f, maxHealth) * (mastered ? REGEN_SHARE_MASTERED : REGEN_SHARE)); }

    /** The Hit Points a blow of {@code dealt} damage heals: a share of it, never above {@link #LIFESTEAL_CAP} of the maximum. */
    public static float lifesteal(float dealt, float maxHealth, boolean mastered) {
        if (dealt <= 0f) return 0f;
        return (float) Math.min(dealt * (mastered ? LIFESTEAL_MASTERED : LIFESTEAL), Math.max(0f, maxHealth) * LIFESTEAL_CAP);
    }

    // ---------------------------------------------------------------- Earth-Shattering Tremor (4)
    /** The leap (blocks per tick, forward and up), how long the landing is waited for (ticks), the reach of the shockwave, the creatures it hurts at most, the base damage, the knock-up (blocks per tick), the Movement Interference (ticks). */
    public static final double LEAP_FORWARD = 1.35, LEAP_UP = 1.0, TREMOR_RADIUS = 14.0, TREMOR_DAMAGE = 140.0, KNOCK_UP = 0.95;
    public static final int LEAP_WINDOW_TICKS = 120, TREMOR_MAX_CREATURES = 24, TREMOR_INTERFERENCE_TICKS = 60;
    /** The ground the tremor breaks: the radius of the bowl, its depth, the most blocks one tremor removes. */
    public static final double TREMOR_BOWL_RADIUS = 6.0;
    public static final int TREMOR_BOWL_DEPTH = 2, TREMOR_MAX_BLOCKS = 240;

    /** The share of the tremor's damage a creature at {@code distance} takes: all at the centre, 40% at the rim. */
    public static double tremorFalloff(double distance, double radius) {
        if (radius <= 0.0) return 0.0;
        double d = Math.max(0.0, Math.min(distance, radius)) / radius;
        return 1.0 - 0.6 * d;
    }

    // ---------------------------------------------------------------- Invictus (5)
    /** How long it lasts (ticks), the second between two checks, how many seconds pass between two pictures, the most the damage grows (normal / mastered: x3 / x4 at the last Hit Point). */
    public static final int INVICTUS_TICKS = 200, INVICTUS_PULSE_TICKS = 20, INVICTUS_FX_EVERY_PULSES = 4;
    public static final double INVICTUS_BONUS = 2.0, INVICTUS_BONUS_MASTERED = 3.0;

    /** The damage a blow may do to someone under Invictus: all of it unless it would leave less than one Hit Point (health and absorption count), then what leaves exactly one. */
    public static float invictusFloor(float damage, float health, float absorption) {
        if (damage <= 0f) return 0f;
        float room = Math.max(0f, health + Math.max(0f, absorption) - 1f);
        return Math.min(damage, room);
    }

    /** The multiplier of a melee blow under Invictus: 1 at full health, up to 1 + the bonus at none. */
    public static double invictusMultiplier(float health, float maxHealth, boolean mastered) {
        if (maxHealth <= 0f) return 1.0;
        double missing = 1.0 - Math.max(0.0, Math.min(1.0, health / maxHealth));
        return 1.0 + missing * (mastered ? INVICTUS_BONUS_MASTERED : INVICTUS_BONUS);
    }

    // ---------------------------------------------------------------- Titan's Grip (6)
    /** The reach and width of the grip, the distance in front of the owner the creature is put at, the Action Lock (ticks; the Balance Law limits it per target). */
    public static final double GRIP_RANGE = 40.0, GRIP_WIDTH = 1.4, GRIP_STOP = 2.2;
    public static final int GRIP_LOCK_TICKS = 40;

    // ---------------------------------------------------------------- Devastating Throw (7)
    /** How far a creature can be picked up (blocks), how far the ground can (blocks), how many blocks a lifted chunk is at most. */
    public static final double THROW_PICK_RANGE = 12.0, CHUNK_REACH = 8.0;
    public static final int CHUNK_BLOCKS = 12;
    /** The speed of the throw (blocks per tick), the pulse (ticks), the longest it flies (ticks) and so the longest path, the width of what it hits on the way, the most creatures it hits on the way. */
    public static final double THROW_SPEED = 2.4, THROW_HIT_RADIUS = 2.0;
    public static final int THROW_PULSE_TICKS = 2, THROW_MAX_TICKS = 18, THROW_MAX_HITS = 8;
    /** The damage on the way, to the thrown creature on landing, and of the blast where it lands; the blast's reach, the creatures it hurts at most, the crater's size and the blocks it may remove. */
    public static final double THROW_PATH_DAMAGE = 90.0, THROW_IMPACT_DAMAGE = 200.0, THROW_BLAST_DAMAGE = 130.0, THROW_BLAST_RADIUS = 4.5, CRATER_RADIUS = 3.0;
    public static final int THROW_BLAST_MAX = 12, THROW_CRATER_BLOCKS = 60;

    /** The length of a throw's path: the speed times the ticks it flies, never more than {@link #THROW_SPEED} x {@link #THROW_MAX_TICKS}. */
    public static double throwPath(int ticks) { return THROW_SPEED * Math.max(0, Math.min(ticks, THROW_MAX_TICKS)); }

    // ---------------------------------------------------------------- Battle Roar (8)
    /** The reach, the enemies it touches at most, the Fear and the Temporal Stutter (ticks; the Balance Law limits both per target), the debuffs it clears from the owner at most. */
    public static final double ROAR_RADIUS = 30.0;
    public static final int ROAR_MAX = 32, ROAR_FEAR_TICKS = 100, ROAR_STUTTER_TICKS = 60, ROAR_CLEANSE_MAX = 24;

    // ---------------------------------------------------------------- World-Breaking Strike (9)
    /** How far ahead the fist lands (blocks), the width of the aim, the radius of the blow, the creatures it hurts at most, the base damage (the other creatures take half), the Temporal Debt, the Shattered Armour (ticks). */
    public static final double STRIKE_REACH = 7.0, STRIKE_WIDTH = 1.6, STRIKE_RADIUS = 6.0, STRIKE_DAMAGE = 600.0, STRIKE_SIDE_SHARE = 0.5;
    public static final int STRIKE_MAX = 8, STRIKE_DEBT = 60, ARMOR_BREAK_TICKS = 400;
    /** The reach in which the Strike shatters the spatial barriers that stand: the pockets of the labyrinths and the fields of the enemy. */
    public static final double STRIKE_SHATTER_RADIUS = 24.0;

    /** How long the armour stays broken: the full time on an ordinary creature, half of it on a player or a boss. */
    public static int armorBreakTicks(boolean player, boolean boss) { return (player || boss) ? ARMOR_BREAK_TICKS / 2 : ARMOR_BREAK_TICKS; }

    /** The damage of the Strike on the creature of rank {@code index} of the ones it hits (0 = the one the fist lands on): all of it, then half. */
    public static double strikeDamage(int index) { return index <= 0 ? STRIKE_DAMAGE : STRIKE_DAMAGE * STRIKE_SIDE_SHARE; }
}
