package com.newuniverse.nusmp.arc.tier;

/**
 * Why: a light boss had five moves cut from nine generic shapes, so Foxy, Ao and Code played alike. A trait is the ONE passive or phase rule from the character's anime that makes
 * the fight different (docs/bosses/tier_powers.md, rule 2 of docs/bosses/RULES.md): a slow field, Hierro armour, the Balance, a Karma mark. Pure data and constants (no Minecraft types):
 * the game side is {@code TierTraits}, the price list and the numbers a test can check are here.
 *
 * <p>Every trait has a price in points (a tier may only spend {@link TierLetter#traitBudget()}), a counter the player can learn and a window in which it is off, so none is a wall. Only
 * ever APPEND entries (the ordinal is not saved, but the docs and the tests list them in this order).
 */
public enum TierTrait {
    SLOW_FIELD("Slow field", 2, false, "players within a few blocks are slowed", "stay out of the field and use range", "the field is only 6 blocks (9 at the Gates)"),
    STEAM("Steam", 2, false, "melee attackers close to him catch fire", "range, water, fire resistance", "only melee hits within 4.5 blocks ignite, once per 2 seconds"),
    ARMOUR("Hierro armour", 3, false, "melee (non-magic) damage is cut by 35%", "magic and ranged damage go round it", "the armour is off while he is exposed after his ultimate"),
    BALANCE("The Balance", 3, true, "when the bound move falls he weighs the damage dealt while he raised it: whoever dealt over half of it is weakened, a lone fighter also takes part of it back", "share the damage, or stop hitting while he winds the move up", "the weighing is only the wind-up of that one move, which is telegraphed like any other"),
    SOUL_MARK("Soul marks", 3, false, "his hits add marks; three marks weaken the player and make him hit harder", "break contact, let the marks run out (15 s)", "marks expire and a mark is cleared when it fires"),
    MIRROR("Mirror stance", 2, true, "after one move he reflects part of the damage he takes for 6 s", "stop hitting for 6 s, or chip from range", "only 6 s after the move, and the reflection is capped per hit"),
    SYSTEM_LOCK("System lock", 3, false, "his hits strip a beneficial effect", "re-apply the effect after the hit", "at most one effect per victim every 2 seconds"),
    PULL("Pull", 2, true, "the target of one move is dragged toward him", "break line of sight, knockback resistance", "about 1.2 seconds, after the move's wind-up"),
    SPORE_POISON("Spore poison", 2, false, "his hits poison (never lethal)", "milk, regeneration", "poison stops at 1 health and is short"),
    FUSION("Fusion", 2, false, "at 66% and 33% health he grows stronger for good", "burst him down before the fusion", "two steps only, announced"),
    SIGHT("Sight", 2, false, "invisible and sneaking players near him are lit up", "break line of sight with blocks", "the outline lasts 2 s and has a 14 block reach"),
    KARMA("Karma mark", 2, true, "one move marks the target; the mark takes 20% more from his hits", "milk clears the mark", "the mark lasts 15 s"),
    ECHO("Echo", 2, true, "one move repeats once after 1.5 s", "keep moving after the first one", "the repeat has the same telegraph and no bonus"),
    UNTOUCHABLE("Untouchable", 2, true, "35% less damage for 4 s after one move (his guards)", "dodge the guards, hit after the window", "only 4 s, and only after that move"),
    CLIMB("Climb", 2, false, "each power step makes him faster and harder-hitting (max 3 steps)", "kill pace: he is slowest at the start", "it is earned by the fight getting longer, never instant"),
    SHIELD_BREAK("Shield break", 1, false, "his hits disable a raised shield for 5 s", "dodge instead of blocking", "the shield comes back after 5 s");

    private final String label, what, counter, window;
    private final int price;
    private final boolean needsMove;

    TierTrait(String label, int price, boolean needsMove, String what, String counter, String window) {
        this.label = label;
        this.price = price;
        this.needsMove = needsMove;
        this.what = what;
        this.counter = counter;
        this.window = window;
    }

    public String label() { return label; }

    /** Points of the tier's budget this trait costs. */
    public int price() { return price; }

    /** Does the trait hang on one named move of the row (the trigger or the target of that move)? */
    public boolean needsMove() { return needsMove; }

    /** One sentence: what it does. */
    public String what() { return what; }

    /** What the player does against it (never blank: a trait without a counter is refused by the test). */
    public String counter() { return counter; }

    /** When it is off or weak (never blank). */
    public String window() { return window; }

    // ---------------------------------------------------------------- the numbers (strength numbers are multiplied by the server setting tierPowerScale, durations are not)

    public static final double SLOW_RADIUS = 6.0, SLOW_RADIUS_GATES = 9.0, MAX_RADIUS = 16.0;
    public static final int SLOW_TICKS = 40;
    public static final double STEAM_REACH = 4.5;
    public static final int STEAM_FIRE_TICKS = 60, STEAM_GAP_TICKS = 40;
    public static final double ARMOUR_CUT = 0.35, UNTOUCHABLE_CUT = 0.35, MAX_CUT = 0.6;
    public static final int UNTOUCHABLE_TICKS = 80;
    public static final int BALANCE_WEAKEN_TICKS = 120, BALANCE_SLACK_TICKS = 40;
    /** The top dealer is weakened only when they dealt at least this share of the window (so sharing the damage is a real answer). */
    public static final double BALANCE_TOP_SHARE = 0.5;
    /** A lone fighter takes this share of what they dealt in the window back, at most {@code BALANCE_BACK_CAP_ATTACKS} times the boss's attack (as the base of one strike). */
    public static final double BALANCE_BACK = 0.4, BALANCE_BACK_CAP_ATTACKS = 1.0;
    public static final int MARK_MAX = 3, MARK_TICKS = 300, FRENZY_TICKS = 160;
    public static final double FRENZY_DAMAGE = 1.10;
    public static final int MIRROR_TICKS = 120;
    public static final double MIRROR_SHARE = 0.25, MIRROR_CAP_HP = 6.0;
    public static final int LOCK_GAP_TICKS = 40;
    public static final int PULL_IMPULSES = 4, PULL_GAP_TICKS = 8;
    public static final double PULL_STRENGTH = 0.4;
    public static final int POISON_TICKS = 60;
    public static final double FUSION_DAMAGE = 1.15, FUSION_SPEED = 0.10;
    public static final double[] FUSION_AT = {0.66, 0.33};
    public static final double SIGHT_REACH = 14.0;
    public static final int SIGHT_TICKS = 40;
    public static final int KARMA_TICKS = 300;
    public static final double KARMA_DAMAGE = 1.20;
    public static final int ECHO_DELAY_TICKS = 30;
    public static final int CLIMB_MAX_STEPS = 3;
    public static final double CLIMB_DAMAGE = 0.08, CLIMB_SPEED = 0.08;
    public static final int SHIELD_TICKS = 100;
    /** At most this many marks / karma entries are held per boss (the fight is for a handful of players; the map is pruned every second). */
    public static final int MAX_TRACKED = 24;

    /** Climb steps at a power value (20 -> 0, 40 -> 1, 60 -> 2, 80 and 100 -> 3). */
    public static int climbSteps(int power) { return Math.max(0, Math.min(CLIMB_MAX_STEPS, (power - 20) / 20)); }

    /** Fusion steps reached at a health fraction (0, 1 under 66%, 2 under 33%). */
    public static int fusionSteps(double healthFraction) {
        int n = 0;
        for (double at : FUSION_AT) if (healthFraction < at) n++;
        return n;
    }

    /** The damage multiplier the boss's own traits give at this power and health ({@code scale} = the tierPowerScale setting). */
    public static double selfDamage(java.util.Collection<TierTraitSpec> traits, int power, double healthFraction, double scale, boolean frenzy) {
        double m = 1.0;
        for (TierTraitSpec s : traits) {
            switch (s.trait()) {
                case CLIMB -> m *= 1.0 + CLIMB_DAMAGE * scale * climbSteps(power);
                case FUSION -> m *= Math.pow(1.0 + (FUSION_DAMAGE - 1.0) * scale, fusionSteps(healthFraction));
                case SOUL_MARK -> { if (frenzy) m *= 1.0 + (FRENZY_DAMAGE - 1.0) * scale; }
                default -> { }
            }
        }
        return m;
    }

    /** The speed bonus (a fraction, 0.1 = +10%) the boss's own traits give at this power and health. */
    public static double selfSpeed(java.util.Collection<TierTraitSpec> traits, int power, double healthFraction, double scale) {
        double v = 0;
        for (TierTraitSpec s : traits) {
            switch (s.trait()) {
                case CLIMB -> v += CLIMB_SPEED * scale * climbSteps(power);
                case FUSION -> v += FUSION_SPEED * scale * fusionSteps(healthFraction);
                default -> { }
            }
        }
        return v;
    }
}
