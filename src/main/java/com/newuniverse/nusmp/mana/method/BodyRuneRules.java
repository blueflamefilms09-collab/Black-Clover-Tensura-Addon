package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;

/**
 * The Body Rune page (pure, tested). Canon (Black Clover Wiki, Gadjah): the Heart Kingdom's Spirit Guardian of lightning writes runes ON HIS OWN BODY with the Mana Method and so takes very powerful
 * attacks unharmed; he also hovers in the air without magic. The page writes runes on the CASTER'S body for a limited time. What the runes give is small, temporary attribute changes (never creative flight; since 3.7.0 a FULL hold also turns away {@value #REDUCTION_FULL_PERCENT} % of each hit), scaled by the hold:
 * <table>
 *   <caption>At a tap (level 0) and at a full hold (level 1)</caption>
 *   <tr><th>attribute</th><th>how</th><th>tap</th><th>full</th><th>hard cap</th></tr>
 *   <tr><td>movement speed</td><td>+ percent (multiplied total)</td><td>+6 %</td><td>+20 %</td><td>+25 %</td></tr>
 *   <tr><td>attack damage</td><td>+ percent (multiplied total)</td><td>+5 %</td><td>+20 %</td><td>+25 %</td></tr>
 *   <tr><td>jump strength</td><td>+ value (0.42 is a normal jump)</td><td>+0.04</td><td>+0.12</td><td>+0.15</td></tr>
 *   <tr><td>step height</td><td>+ value (0.6 is normal), from the second ring tier</td><td>0</td><td>+0.4</td><td>+0.4</td></tr>
 *   <tr><td>safe fall distance</td><td>+ blocks (3 is normal)</td><td>+1</td><td>+5</td><td>+5</td></tr>
 *   <tr><td>knockback resistance</td><td>+ value</td><td>+0.1</td><td>+0.4</td><td>+0.4</td></tr>
 * </table>
 * A full hold (and only that) also lets the caster glide: slow falling for {@value #GLIDE_TICKS} ticks, once, at the cast. It is not flight: it works only while falling.
 * The duration is {@code bodySeconds} (default {@value #DEFAULT_SECONDS}, 5..{@value #MAX_SECONDS}) x ({@value #MIN_TIME_SHARE} at a tap .. 1 at a full hold). Every modifier has its own fixed id
 * ({@link #IDS}), is transient (never saved: a crash cannot leave one behind) and is removed before it is added (a second cast refreshes, it never stacks).
 */
public final class BodyRuneRules {
    private BodyRuneRules() {}

    // ---------------------------------------------------------------- the page
    public static final double COST_PERCENT = 10.0, COST_FLOOR = 200.0;
    public static final int COOLDOWN = 600;
    public static final int DEFAULT_SECONDS = 15, MIN_SECONDS = 5, MAX_SECONDS = 60;
    public static final double MIN_TIME_SHARE = 0.5;
    public static final int GLIDE_TICKS = 60;
    /** At most this many bodies carry runes at once on the server (one per player; the ceiling only guards a runaway). */
    public static final int MAX_SESSIONS = 64;

    /** The fixed ids (paths in the {@code nusmp} namespace) of the six modifiers, in the order of {@link Mods}. */
    public static final String[] IDS = {"mana_method_body_speed", "mana_method_body_attack", "mana_method_body_jump", "mana_method_body_step", "mana_method_body_fall", "mana_method_body_knockback"};

    public static final double SPEED_MAX = 0.25, ATTACK_MAX = 0.25, JUMP_MAX = 0.15, STEP_MAX = 0.4, FALL_MAX = 5.0, KNOCKBACK_MAX = 0.4;

    /** The damage the runes turn away (3.7.0, owner decision: "sure, a little"): a FULL hold only, 15 % of every hit that is not void or a kill command; never more than {@value #REDUCTION_CAP}. */
    public static final double REDUCTION_FULL = 0.15, REDUCTION_CAP = 0.20;
    static final int REDUCTION_FULL_PERCENT = 15;

    /** The share of a hit a body with this charge turns away: {@link #REDUCTION_FULL} for a measured full hold, 0 for a tap, a half hold and a cast without a measured hold. */
    public static double reduction(Charge c) {
        boolean full = c != null && !c.legacy() && c.stage() == RuneCharge.Stage.FULL;
        return full ? Math.min(REDUCTION_CAP, REDUCTION_FULL) : 0.0;
    }

    /** The amount left of a hit after {@code reduction} (clamped to 0..{@link #REDUCTION_CAP}); never negative, never more than the hit. */
    public static float reduced(float amount, double reduction) {
        double r = Math.max(0.0, Math.min(REDUCTION_CAP, reduction));
        return (float) Math.max(0.0, amount * (1.0 - r));
    }

    public static int seconds(int configured) { return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, configured)); }

    /** What the runes give: the six amounts (0 = not given), the glide in ticks and the duration in ticks. */
    public record Mods(double speed, double attack, double jump, double step, double fall, double knockback, int glideTicks, int ticks, int tier) {
        /** The amount of modifier {@code i} in the order of {@link #IDS}. */
        public double amount(int i) {
            return switch (i) {
                case 0 -> speed;
                case 1 -> attack;
                case 2 -> jump;
                case 3 -> step;
                case 4 -> fall;
                default -> knockback;
            };
        }

        /** True for the modifiers that are percentages of the total (speed, attack); the others add a plain value. */
        public static boolean multiplied(int i) { return i <= 1; }
    }

    /** The modifiers and the duration of a charge. The cast without a measured hold is the middle of the range, ring tier 3, no glide. */
    public static Mods modsFor(Charge c, int configuredSeconds) {
        double l = RuneCost.level(c);
        int tier = c == null ? 1 : c.tier();
        boolean full = c != null && !c.legacy() && c.stage() == RuneCharge.Stage.FULL;
        double speed = Math.min(SPEED_MAX, 0.06 + 0.14 * l), attack = Math.min(ATTACK_MAX, 0.05 + 0.15 * l);
        double jump = Math.min(JUMP_MAX, 0.04 + 0.08 * l);
        double step = tier >= 2 ? Math.min(STEP_MAX, 0.2 + 0.2 * l) : 0.0;
        double fall = Math.min(FALL_MAX, 1.0 + 4.0 * l), kb = Math.min(KNOCKBACK_MAX, 0.1 + 0.3 * l);
        int ticks = (int) Math.round(seconds(configuredSeconds) * 20 * (MIN_TIME_SHARE + (1.0 - MIN_TIME_SHARE) * l));
        return new Mods(speed, attack, jump, step, fall, kb, full ? GLIDE_TICKS : 0, Math.max(20, ticks), tier);
    }

    /** How many rune pictures the body wears: the chest rune, and from the second ring tier the rune at the feet. */
    public static int pictures(int tier) { return tier >= 2 ? 2 : 1; }

    /** Has a session that ends at {@code until} run out at {@code now}? */
    public static boolean expired(long now, long until) { return now >= until; }
}
