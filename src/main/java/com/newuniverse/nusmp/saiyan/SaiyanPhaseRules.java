package com.newuniverse.nusmp.saiyan;

/**
 * Why: the numbers of the Dragon Ball rework techniques (phases 3 and 4, and the Giant Ki Avatar) with no Minecraft type, so they are unit tested and so every count has one hard bound that no setting can pass
 * (rule 11: a technique can never cost more than a fixed amount of work). PHYSICAL: Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct. KI: Charged Ki Sphere, Ki Wave,
 * Ki Reinforcement. TRAINING: gravity. FORMS: the berserk flicker of the Berserk forms, the moon rule of the ape forms, the extended Kaio-ken strain. AVATAR: the Giant Ki Avatar.
 */
public final class SaiyanPhaseRules {
    private SaiyanPhaseRules() {}

    /** Hard bounds, whatever the settings say. */
    public static final int MAX_WAVE_TICKS = 100, WAVE_EVERY = 4, MAX_SPHERE_TRAVEL = 60, MAX_GRAVITY = 10, MAX_AVATAR_TICKS = 900, MAX_AVATAR_HITS = 6, MAX_SLAM_TARGETS = 8, MAX_RUSH_HITS = 3, MAX_GIFT_TARGETS = 8;
    public static final double MAX_VANISH = 16.0, MAX_SLAM_RADIUS = 8.0, MAX_AVATAR_SCALE = 12.0, MAX_COUNTER_REFLECT = 200.0;

    static double clamp(double x, double lo, double hi) { return Double.isNaN(x) ? lo : Math.max(lo, Math.min(hi, x)); }

    // ------------------------------------------------------------------ physical techniques
    /** How far a Vanishing Step goes: the setting, kept between 2 and {@link #MAX_VANISH} blocks. */
    public static double vanishDistance(double setting) { return clamp(setting, 2.0, MAX_VANISH); }

    /** The radius of a Ground Breaker shockwave: the base plus a quarter block per block fallen (at most 16 counted), never past {@link #MAX_SLAM_RADIUS}. */
    public static double slamRadius(double base, double fall) { return Math.min(MAX_SLAM_RADIUS, clamp(base, 1.0, MAX_SLAM_RADIUS) + 0.25 * clamp(fall, 0.0, 16.0)); }

    /** The damage factor of a Ground Breaker: 1 on the ground, up to 2 after a fall of 20 blocks or more. */
    public static double slamFactor(double fall) { return 1.0 + clamp(fall, 0.0, 20.0) / 20.0; }

    /** What a Warrior's Counter gives back: the blow's damage times the factor, never more than {@link #MAX_COUNTER_REFLECT}. */
    public static double counterReflect(double amount, double factor) { return Math.min(MAX_COUNTER_REFLECT, Math.max(0.0, amount) * clamp(factor, 0.0, 10.0)); }

    /** True while a counter stance is up. */
    public static boolean counterArmed(long now, long until) { return now >= 0 && now < until; }

    // ------------------------------------------------------------------ Ki techniques
    /** The ticks a Ki Wave channels for a charge of {@code charge} ticks: nothing below {@code minCharge}, else 10 + half the charge, at most {@code maxChannel} (and never past {@link #MAX_WAVE_TICKS}). */
    public static int waveTicks(int charge, int minCharge, int maxChannel) {
        if (charge < Math.max(0, minCharge)) return 0;
        return (int) clamp(10 + charge / 2, 10, Math.min(MAX_WAVE_TICKS, Math.max(10, maxChannel)));
    }

    /** True on the ticks a Ki Wave hits (every {@link #WAVE_EVERY} ticks since it began). */
    public static boolean waveBeat(long now, long begun) { return now > begun && (now - begun) % WAVE_EVERY == 0; }

    /** Ticks a Charged Ki Sphere flies over {@code distance} blocks at {@code speed} blocks a tick: at least 6, at most {@link #MAX_SPHERE_TRAVEL}. */
    public static int sphereTravel(double distance, double speed) { return (int) clamp(Math.ceil(Math.max(0.0, distance) / Math.max(0.2, speed)), 6, MAX_SPHERE_TRAVEL); }

    /** Share of the maximum Ki that Ki Reinforcement drains each second, kept non-negative. */
    public static double reinforceDrain(double setting) { return clamp(setting, 0.0, 20.0); }

    /** The outgoing damage factor of Ki Reinforcement (1 + the bonus, the bonus kept between 0 and 1.5) and the share of incoming damage that still goes through (1 / (1 + the defence bonus)). */
    public static double reinforceOut(double bonus) { return 1.0 + clamp(bonus, 0.0, 1.5); }

    public static double reinforceTaken(double defence) { return 1.0 / (1.0 + clamp(defence, 0.0, 1.5)); }

    // ------------------------------------------------------------------ gravity training
    /** The gravity level a Saiyan has adapted to: one level per {@code secondsPerLevel} trained seconds, at most {@link #MAX_GRAVITY}. */
    public static int adaptedLevel(int trainedSeconds, int secondsPerLevel) { return (int) clamp(Math.max(0, trainedSeconds) / Math.max(1, secondsPerLevel), 0, MAX_GRAVITY); }

    /** The level the next training starts at: one above the adapted level, at most the maximum. */
    public static int trainingLevel(int adapted) { return (int) clamp(adapted + 1, 1, MAX_GRAVITY); }

    /** The movement and jump penalty (a fraction, 0..0.35) of a level: 1% a level always, and 4% for every level the body has not adapted to yet. */
    public static double gravityPenalty(int level, int adapted) {
        int l = (int) clamp(level, 0, MAX_GRAVITY);
        return Math.min(0.35, 0.01 * l + 0.04 * Math.max(0, l - Math.max(0, adapted)));
    }

    /** Training points a second of gravity gives (the daily caps of {@link SaiyanTraining} still apply on top). */
    public static double gravityTrain(int level) { return 0.05 * clamp(level, 0, MAX_GRAVITY); }

    /** Stamina a second of gravity costs (the Saiyan's own stamina meter, never the food bar). */
    public static double gravityStamina(int level) { return 0.5 * clamp(level, 0, MAX_GRAVITY); }

    /** Idle training is worth nothing: a second counts only if the Saiyan moved or fought in the last 10 seconds. */
    public static boolean gravityCounts(boolean moved, long now, long lastCombat) { return moved || (now >= lastCombat && now - lastCombat <= 200); }

    // ------------------------------------------------------------------ forms
    /** The berserk forms lose control: the share of seconds that flicker (0.15 new, 0.05 mastered) and the Ki a flicker burns (percent of the maximum). Never input theft. */
    public static double berserkChance(double mastery) { return 0.15 - 0.10 * clamp(mastery, 0.0, 1.0); }

    public static double berserkFlicker() { return 2.0; }

    public static boolean isBerserk(String id) { return "super_saiyan_2_berserk".equals(id) || "super_saiyan_blue_berserk".equals(id); }

    public static boolean isApe(String id) { return "oozaru".equals(id) || "golden_great_ape".equals(id); }

    /** The ape forms need a full moon at night (the moon phase 0, the day time between 13000 and 23000), unless an artificial moon was granted. */
    public static boolean moonOk(int moonPhase, long dayTime, boolean artificialMoon) {
        if (artificialMoon) return true;
        long t = Math.floorMod(dayTime, 24000L);
        return moonPhase == 0 && t >= 13000L && t <= 23000L;
    }

    /** Kaio-ken stages 1..5 = x2, x3, x4, x10, x20. The strongest a Saiyan can enter: the first three by mastery as before, x10 from 85% mastery, x20 at full mastery (the last two only when {@code extended}). */
    public static int kaioMaxStage(double mastery, boolean extended) {
        int base = SaiyanForms.kaioMaxStage(mastery);
        if (!extended) return base;
        return mastery >= 1.0 ? 5 : mastery >= 0.85 ? 4 : base;
    }

    /** The damage, drain and strain multipliers of a stage 1..5 (the first three are the old table). */
    public static double kaioDamage(int stage) { return new double[] {1.0, 1.5, 2.0, 3.0, 4.0}[(int) clamp(stage, 1, 5) - 1]; }

    public static double kaioDrain(int stage) { return new double[] {1.0, 1.6, 2.4, 3.5, 5.0}[(int) clamp(stage, 1, 5) - 1]; }

    public static double kaioStrain(int stage) { return new double[] {1.0, 2.2, 4.0, 9.0, 18.0}[(int) clamp(stage, 1, 5) - 1]; }

    /** The mastered Super Saiyan drains nothing once it is fully mastered (the spec's "zero drain at 100% mastery"); every other form keeps the old law. */
    public static double drainFactor(String id, double mastery) { return "mastered_super_saiyan".equals(id) && mastery >= 1.0 ? 0.0 : SaiyanForms.drainFactor(mastery); }

    // ------------------------------------------------------------------ Giant Ki Avatar
    /** The ticks the avatar may stand: the setting in seconds (1..45), at most {@link #MAX_AVATAR_TICKS}. */
    public static int avatarTicks(double seconds) { return (int) clamp(Math.round(clamp(seconds, 1.0, 45.0) * 20.0), 20, MAX_AVATAR_TICKS); }

    /** The avatar's scale (blocks tall): the setting, kept between 3 and {@link #MAX_AVATAR_SCALE}. */
    public static double avatarScale(double setting) { return clamp(setting, 3.0, MAX_AVATAR_SCALE); }

    /** True on the ticks the avatar swings (once a second since it rose). */
    public static boolean avatarSwing(long now, long rose) { return now > rose && (now - rose) % 20 == 0; }

    /** Only the two highest Ultra Instinct forms can raise it (unless the server lifts the rule). */
    public static boolean avatarForm(String formId, boolean needsForm) { return !needsForm || "perfected_ultra_instinct".equals(formId) || "true_ultra_instinct".equals(formId); }

    /** True while the avatar still stands. */
    public static boolean avatarStands(long now, long until) { return now >= 0 && now < until; }
}
