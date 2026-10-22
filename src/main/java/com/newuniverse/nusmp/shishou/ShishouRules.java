package com.newuniverse.nusmp.shishou;

/**
 * Why: every number of Shishou in one pure place, so a balance change is one edit, the tests pin it, and the worst-case cost of each skill in docs/items/shishou.md is read from here. No game classes. The
 * server config ({@link ShishouConfig}) may lower or raise a few of these; the defaults are these.
 */
public final class ShishouRules {
    private ShishouRules() {}

    // ---------------------------------------------------------------- identity
    public static final String ID = "shishou";
    /** Anti-theft: the grace before a non-wielder's grip is torn open, in ticks (10 seconds), the check rate (once a second) and the effects (Slowness X = amplifier 9, Wither I). */
    public static final int THEFT_TICKS = 200, THEFT_CHECK = 20, THEFT_SLOW_AMP = 9, THEFT_WITHER_AMP = 0, THEFT_EFFECT_TICKS = 60;
    public static final String THEFT_LINE = "Let go of me, you aren't my wielder.";

    // ---------------------------------------------------------------- mana
    public static final int MAX_STEPS = 20;
    public static final double MANA_BASE = 100.0, MANA_PER_LEVEL = 8.0, MANA_PER_STEP = 6.0;

    /** The mana pool: 100 + 8 per level + 6 per absorbed step. Level 1: 108; level 100 with all steps: 1020. */
    public static double maxMana(int level, int manaSteps) {
        return MANA_BASE + MANA_PER_LEVEL * clampLevel(level) + MANA_PER_STEP * Math.max(0, Math.min(MAX_STEPS, manaSteps));
    }

    /** Mana back per tick: 0.6 per second plus 0.04 per level per second (level 1: 0.64 a second, level 100: 4.6): slow. */
    public static double regenPerTick(int level) { return (0.6 + 0.04 * clampLevel(level)) / 20.0; }

    /** What a swing of the sword costs in mana (the upkeep of Ultra-Speed Regeneration: the sword never wears, mana pays). */
    public static final double HIT_MANA = 1.0;
    /** Below this share of the pool the sword is "low": a warning once, and the Vibration / Black Flame / Spatial Blade extras stop. */
    public static final double LOW_MANA_SHARE = 0.15;

    public static int clampLevel(int level) { return Math.max(1, Math.min(ShishouLevels.MAX_LEVEL, level)); }

    // ---------------------------------------------------------------- strikes
    public static final double STRIKE_CAP = 60.0;

    /** The damage of a telekinetic charge strike: 6 + 0.45 per level + 0.5 per attack step + the element bonus, never above {@code cap}. */
    public static double strikeDamage(int level, int attackSteps, double elementBonus, double cap) {
        double d = 6.0 + 0.45 * clampLevel(level) + 0.5 * Math.max(0, Math.min(MAX_STEPS, attackSteps)) + Math.max(0.0, elementBonus);
        return Math.min(Math.max(1.0, cap), d);
    }

    /** The bonus of Vibration Magic: a share of the hit that bypasses armour, capped (default 20% of the hit, never above {@code pctCap}). */
    public static double vibrationBonus(double hit, double pct, double pctCap) {
        return Math.max(0.0, hit) * Math.max(0.0, Math.min(pct, pctCap));
    }

    /** One element's bonus damage on a strike: +1 per learned element, up to +4. */
    public static double elementBonus(int learnedElements) { return Math.min(4, Math.max(0, learnedElements)); }

    // ---------------------------------------------------------------- Black Flame (the effect)
    public static final int BLACK_FLAME_TICKS = 100, BLACK_FLAME_PERIOD = 10;

    /** Damage of one Black Flame pulse (every 10 ticks for 5 seconds): 1 + 0.04 per level, capped at {@code cap} (default 5). */
    public static double blackFlamePulse(int level, double cap) { return Math.min(Math.max(0.5, cap), 1.0 + 0.04 * clampLevel(level)); }

    /** The amplifier carried by the effect: the level band 0..4 (it only scales the particles, the damage comes from the level stored on the sword). */
    public static int blackFlameAmp(int level) { return Math.min(4, clampLevel(level) / 25); }

    // ---------------------------------------------------------------- the spells
    public static final int BLACK_LIGHTNING_RANGE = 25, BLACK_LIGHTNING_SPLASH_TARGETS = 6;
    public static final double BLACK_LIGHTNING_SPLASH_RADIUS = 3.0, BLACK_LIGHTNING_SPLASH_SHARE = 0.4;

    public static double blackLightning(int level, double cap) { return Math.min(cap, 20.0 + 0.7 * clampLevel(level)); }

    public static final double LEAP_RANGE = 12.0, LEAP_RANGE_SHORT = 6.0;
    public static final int LEAP_IFRAMES = 20, LEAP_STEPS_MAX = 40;

    public static final double HAKI_RADIUS = 10.0, MAKA_MAX_HEALTH = 20.0;
    public static final int MAKA_PULSE = 20, MAKA_TARGETS = 16, MAKA_EFFECT_TICKS = 60;

    public static final int BARRIER_REFRESH = 100, BARRIER_COOLDOWN = 600, BARRIER_DEFLECT_PER_SECOND = 3;
    public static final double BARRIER_DEFLECT_MANA = 12.0;

    /** Absorption hearts of the Multilayer Barrier: 4 at level 10 growing to 8 at level 100 (a whole heart count). */
    public static int barrierHearts(int level) { return Math.max(4, Math.min(8, 4 + (clampLevel(level) - 10) / 22)); }

    /** The amplifier of Absorption for that many hearts (one level = four points = two hearts): 4 hearts = 1, 8 hearts = 3. */
    public static int barrierAmplifier(int hearts) { return Math.max(0, Math.min(3, hearts / 2 - 1)); }

    public static final int SENSE_RADIUS = 35, SENSE_TARGETS = 64, SENSE_TICKS = 200, SENSE_REFRESH = 40, SENSE_GLOW_TICKS = 60;
    public static final double SENSE_MANA_PER_REFRESH = 2.0;

    public static final int DIVINE_TARGETS = 16, DIVINE_RANGE = 20, DIVINE_RELEASE_TICKS = 160;
    public static final double DIVINE_RADIUS = 8.0, DIVINE_CAP = 60.0, DIVINE_RELEASE_BONUS = 0.25;

    public static double divine(int level, double cap) { return Math.min(cap, 12.0 + 0.5 * clampLevel(level)); }

    public static final int CLONES_PER_CAST = 4, CLONES_GLOBAL = 6, CLONE_LIFE = 100, CLONE_PERIOD = 10;
    public static final double CLONE_SHARE = 0.35;

    public static final int SPELL_TARGETS = 12;
    public static final double SPELL_RADIUS = 7.0, SPELL_RANGE = 24.0;

    public static double spellDamage(int level, double base, double perLevel, double cap) { return Math.min(cap, base + perLevel * clampLevel(level)); }

    public static final int STEALTH_TICKS = 200;
    public static final int STOMACH_RADIUS = 5, STOMACH_ITEMS = 16, STOMACH_AUTO_ITEMS = 8, STOMACH_AUTO_PERIOD = 20;
    public static final int STORAGE_SLOTS = 54, STORAGE_ROWS_DEFAULT = 3;

    // ---------------------------------------------------------------- the wild phase
    public static final int DORMANT_KILLS = 25, DORMANT_DAYS = 7, DAY_TICKS = 24000, WILD_STRIKE_MANA = 4;
    public static final double WILD_MANA = 100.0;

    /** Should a wild sword settle into its pedestal? After {@code kills} kills, after {@code days} days, or when its wild mana is spent. */
    public static boolean shouldGoDormant(int wildKills, long wildTicks, double wildMana, int killLimit, int dayLimit) {
        return wildKills >= Math.max(1, killLimit) || wildTicks >= (long) Math.max(1, dayLimit) * DAY_TICKS || wildMana < WILD_STRIKE_MANA;
    }

    /** Hovering height above the ground the wild sword keeps, the reach of its strike, its flight speed. */
    public static final double REACH = 2.4, FLIGHT_SPEED = 0.7, CHARGE_SPEED = 1.9;
    public static final int STRIKE_COOLDOWN = 24, ATTACK_ANIM_TICKS = 14, TARGET_RANGE = 24, MINION_LEASH = 16, MINION_RETURN = 24, MINION_TELEPORT = 64;
    public static final int GROUND_SEARCH = 40, GROUND_SIDE_TRIES = 8, GROUND_SIDE_RADIUS = 12;

    // ---------------------------------------------------------------- animation states (synced as a byte)
    public static final int STATE_IDLE = 0, STATE_AGGRO = 1, STATE_ATTACK = 2, STATE_DORMANT = 3;
    public static final String[] STATE_CLIPS = {"idle", "aggro", "attack", "dormant"};

    /** The clip of a state; an unknown state is the idle clip. */
    public static String clip(int state) { return state >= 0 && state < STATE_CLIPS.length ? STATE_CLIPS[state] : STATE_CLIPS[0]; }

    /** The blade is 1.7 blocks long: where the hilt (the end of the grip, the Rukh trail starts here) lies behind the entity's centre along its look direction. */
    public static final double BLADE_LENGTH = 1.7, HILT_BACK = 0.65;

    // ---------------------------------------------------------------- sword arts (attribute numbers, applied only while held)
    public static final double BASE_ATTACK = 7.0, ATTACK_PER_LEVEL = 0.12, ATTACK_BONUS_CAP = 12.0, SPEED_ARTS_BONUS = 0.4, ARTS_DAMAGE_SHARE = 0.10, SHARING_SPEED_PER_SKILL = 0.004, SHARING_SPEED_CAP = 0.04;

    /** The attack damage the sword shows: 7 + 0.12 per level, never more than 7 + 12. */
    public static double heldAttack(int level) { return BASE_ATTACK + Math.min(ATTACK_BONUS_CAP, ATTACK_PER_LEVEL * clampLevel(level)); }

    /** The Skill Sharing walk-speed boost of the holder: a small share per absorbed skill, capped at 4%. */
    public static double sharingSpeed(int absorbedCount) { return Math.min(SHARING_SPEED_CAP, SHARING_SPEED_PER_SKILL * Math.max(0, absorbedCount)); }

    // ---------------------------------------------------------------- the one-per-world cap
    public static final int RESERVE_DAYS = 7, RECALL_GAP = 20, CAST_GAP = 4, APPRAISE_GAP = 20, SYNC_PERIOD = 20, FAR_DISTANCE = 96;
}
