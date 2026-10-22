package com.newuniverse.nusmp.book;

/**
 * 1.99.0: the pure rules behind {@link ArmourState} (no game classes, unit tested by ArmourPolicyTest). What the armour layer does when a page is cast, when it ends, how often it
 * is refreshed on the clients and what it costs.
 *
 * <p>The armour is a render layer ({@code aura.Aura}) whose duration the server states in ticks. A worn armour has no end, so the server keeps it alive with a LEASE: it tells the
 * clients "this armour has {@link #LEASE_TICKS} more ticks" every {@link #REFRESH_EVERY} ticks (the client keeps the start of a refreshed armour of the same style, so nothing is
 * forged again). Taking it off tells the clients "{@link #SHATTER_TICKS} more ticks", which is inside the shatter window of {@code EquipAnimation} (18 ticks), so the pieces
 * shatter out exactly as they forged in.
 */
public final class ArmourRules {
    private ArmourRules() {}

    /** The duration the clients are told on every refresh. */
    public static final int LEASE_TICKS = 200;
    /** How often the lease is renewed (always well inside the lease, and never inside the 18 tick shatter window). */
    public static final int REFRESH_EVERY = 100;
    /** The duration told when the armour is taken off: the clients shatter it out over this time (EquipAnimation.SHATTER_WINDOW is 18). */
    public static final int SHATTER_TICKS = 20;
    /** The armour's upkeep and ambient intake are paid once a second. */
    public static final int SECOND = 20;
    /** Health the Silver-Guardian-style poison never takes the wearer below. */
    public static final float HP_FLOOR = 6f;

    // ------------------------------------------------------------------ what a worn armour is
    /** The armour a player wears: the magic, the style, and whether it belongs to a FORM (Spirit Dive) rather than to an armour page. {@code aura} is the render layer ("" = drawn by an older system). */
    public record Held(String magic, int style, boolean form, String aura) {}

    public enum Outcome {
        /** Nothing is worn: put it on. */
        PUT_ON,
        /** The same armour (same style) is worn: take it off. */
        TAKE_OFF,
        /** The magic's armour is worn in another style: forge the other style. */
        RESTYLE,
        /** Another magic's armour is worn: take it off and put this one on. */
        SWAP,
        /** The magic's FORM is on: the armour page changes nothing (the form's look is the one that shows). */
        KEEP
    }

    /** A cast of an armour page of {@code magic} with {@code style}, with {@code held} worn (null = nothing). */
    public static Outcome armourPage(Held held, String magic, int style) {
        if (held == null) return Outcome.PUT_ON;
        if (!held.magic().equals(magic)) return Outcome.SWAP;
        if (held.form()) return Outcome.KEEP;
        return held.style() == style ? Outcome.TAKE_OFF : Outcome.RESTYLE;
    }

    /** The FORM page of {@code magic} is cast with {@code held} worn: PUT_ON (nothing), SWAP (another magic's armour goes), or RESTYLE (the magic's own armour page is worn: it becomes the form). */
    public static Outcome formStart(Held held, String magic) {
        if (held == null) return Outcome.PUT_ON;
        return held.magic().equals(magic) ? Outcome.RESTYLE : Outcome.SWAP;
    }

    /** Does a cast of this armour page only take the armour off (so the caster pays nothing and no spell runs)? */
    public static boolean onlyTakesOff(Held held, String magic, int style) { return armourPage(held, magic, style) == Outcome.TAKE_OFF; }

    // ------------------------------------------------------------------ style
    /** The page's table style raised by its rule: "sneak" = 1 while sneaking, "mastery" = 1 from three quarters mastery (or a devil state), otherwise the table style. */
    public static int style(String styleBy, int base, boolean sneaking, boolean mastered) {
        if ("sneak".equals(styleBy) && sneaking) return Math.max(base, 1);
        if ("mastery".equals(styleBy) && mastered) return Math.max(base, 1);
        return base;
    }

    /** The same with the character tier (Noelle's forms, {@link NoelleFormRules}): "character" raises every armour page whose table style is above 0 by the character bit when the caster is the canon character. */
    public static int style(String styleBy, int base, boolean sneaking, boolean mastered, boolean character) {
        int s = style(styleBy, base, sneaking, mastered);
        return "character".equals(styleBy) && character && base > 0 ? s | NoelleFormRules.CHARACTER_BIT : s;
    }

    // ------------------------------------------------------------------ timing
    public static boolean refreshDue(long now) { return now % REFRESH_EVERY == 0; }

    public static boolean secondDue(long now) { return now % SECOND == 0; }

    // ------------------------------------------------------------------ why an armour ends
    public enum Drop { NONE, DEATH, KNOCKED_OUT, NO_MANA, UNKNOWN_MAGIC }

    /**
     * Whether a worn armour must come off this tick. {@code frozen}: held in stopped time or stunned (knocked out); {@code mana}: the magicules the wearer has; {@code upkeepDue}: this
     * tick pays the upkeep; {@code upkeep}: the magicules the upkeep takes; {@code creative}: no upkeep for creative players.
     */
    public static Drop drop(boolean alive, boolean frozen, boolean knownMagic, boolean creative, boolean upkeepDue, double mana, double upkeep) {
        if (!alive) return Drop.DEATH;
        if (!knownMagic) return Drop.UNKNOWN_MAGIC;
        if (frozen) return Drop.KNOCKED_OUT;
        if (upkeepDue && !creative && upkeep > 0.0 && mana < upkeep) return Drop.NO_MANA;
        return Drop.NONE;
    }

    /** The mana after one second: upkeep paid, ambient intake added, never above the pool or below zero. */
    public static double manaAfterSecond(double mana, double max, double upkeep, double absorb) {
        double m = mana - upkeep + absorb;
        return Math.max(0.0, Math.min(max, m));
    }

    /** Silver-Guardian-style poison: the wearer may pay {@code hpUpkeep} only above the floor and never in creative. */
    public static boolean hpUpkeepAllowed(float health, boolean creative, double hpUpkeep) { return hpUpkeep > 0.0 && !creative && health - hpUpkeep >= HP_FLOOR; }

    // ------------------------------------------------------------------ the armour's boost to spells
    /** Half of the armour's attack bonus also strengthens the spells of the magic the armour belongs to (an ordinary cast may USE the armour's boost, it never puts the armour on). */
    public static float spellBoost(float attackBonus) { return 1f + Math.max(0f, attackBonus) * 0.5f; }

    // ------------------------------------------------------------------ Devil Union (Asta and Liebe): the strict limit and the price
    /** Default limit of Devil Union (Black Form): five minutes. */
    public static final int DEVIL_LIMIT_SECONDS = 300;
    /** Default time the user is incapacitated afterwards: thirty minutes. */
    public static final int DEVIL_AFTERMATH_SECONDS = 1800;

    public static long devilLimitTicks(int configSeconds) { return Math.max(20, configSeconds) * 20L; }

    public static long devilAftermathTicks(int configSeconds) { return Math.max(0, configSeconds) * 20L; }

    /** The union is over: it began at {@code since} (game time, 0 = not running) and has lasted the limit. */
    public static boolean devilExpired(long since, long now, long limitTicks) { return since > 0 && now - since >= limitTicks; }

    /** The user may not enter the union again while the aftermath lasts. */
    public static boolean devilBlocked(long aftermathUntil, long now) { return now < aftermathUntil; }

    // ------------------------------------------------------------------ Spirit Dive stages
    /** The first dives of a mage are HALF dives (Yuno: only the left half transforms); the strength of the bonus grows to full after {@link #HALF_DIVES} dives or at mastery. */
    public static final int HALF_DIVES = 3;
    public static final float HALF_SCALE = 0.6f;

    public static boolean halfDive(int divesDone, boolean mastered) { return !mastered && divesDone < HALF_DIVES; }

    public static float diveScale(int divesDone, boolean mastered) { return halfDive(divesDone, mastered) ? HALF_SCALE : 1f; }

    /** A bonus value scaled by the stage and the configured strength (never negative). */
    public static float scaled(float value, float stage, double config) { return (float) Math.max(0.0, value * stage * config); }
}
