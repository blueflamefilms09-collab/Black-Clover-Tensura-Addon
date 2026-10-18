package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow.Kind;

import java.util.List;
import java.util.Locale;

/**
 * Why: every number of the Legendary Spear lives here and nowhere else (lengths, widths, delays, damage, cooldowns, costs), so the balance the owner set
 * (design.md section 5) can be checked by one test and an edit cannot drift between the skill code, its description and its row in the table. A technique's
 * description is built from the same constants, so the text a player reads in the Weapon Book can never disagree with what the code does. The damage is RAW
 * damage: the balance law scales it by the wielder's existence and the bond level (1.0 at level 0 to 2.2 at level 100). Distances are blocks; delays are
 * ticks (20 a second). Nothing here depends on the game, so the test pins the budget with plain JUnit.
 */
public final class SpearNumbers {
    private SpearNumbers() {}

    // ================================================================ lines of the spear
    /** Piercing Thrust: a straight line, struck a few ticks after the glint. */
    public static final double THRUST_LENGTH = 8.0, THRUST_WIDTH = 0.9, THRUST_RAW = 6.0;
    public static final int THRUST_DELAY = 3, THRUST_TARGETS = 6;
    /** The Longshaft passive makes the straight lines of the thrust, the Meteor Spear and Triaina this much longer (a throw and the grand beam are not stretched). */
    public static final double LONGSHAFT = 1.25;

    /** Dragoon Lunge. */
    public static final double LUNGE_DISTANCE = 9.0, LUNGE_WIDTH = 1.6, LUNGE_RAW = 6.0;
    public static final int LUNGE_TARGETS = 6;

    /** Pole Vault: the launch speed (blocks per tick, horizontal and up) and the Slow Falling that follows. The distance is the physics of the vanilla jump arc, about 7 forward, 2.5 up. */
    public static final double VAULT_FORWARD = 0.8, VAULT_UP = 0.62;
    public static final int VAULT_SLOW_FALL = 60;

    /** Javelin Throw: a bolt of this speed (blocks per tick) for this many ticks, so about 30 blocks. */
    public static final double JAVELIN_SPEED = 2.2, JAVELIN_RADIUS = 0.7, JAVELIN_RAW = 9.0;
    public static final int JAVELIN_LIFE = 14, JAVELIN_DELAY = 6, JAVELIN_TARGETS = 4;

    /** Meteor Spear. */
    public static final double METEOR_LENGTH = 16.0, METEOR_WIDTH = 1.4, METEOR_RAW = 14.0, METEOR_SPLASH = 4.0, METEOR_SPLASH_RADIUS = 2.5;
    public static final int METEOR_DELAY = 10, METEOR_TARGETS = 8;

    /** Triaina: the beam that grows into Brionac. */
    public static final double TRIAINA_LENGTH = 18.0, TRIAINA_WIDTH = 1.4, TRIAINA_RAW = 14.0;
    public static final int TRIAINA_DELAY = 12, TRIAINA_TARGETS = 10;

    /** Brionac, the ultimate beam. */
    public static final double BRIONAC_LENGTH = 26.0, BRIONAC_WIDTH = 2.6, BRIONAC_RAW = 26.0;
    public static final int BRIONAC_DELAY = 24;

    // ================================================================ rings and sweeps
    /** Windmill Sweep. */
    public static final double WHIRL_RADIUS = 4.8, WHIRL_RAW = 8.0, WHIRL_PUSH = 0.7;
    public static final int WHIRL_DELAY = 4;

    /** Minute Hand: the hand's reach, the steps of the turn (each lasts MINUTE_STEP_TICKS), and the half width of the hand in degrees. */
    public static final double MINUTE_RADIUS = 9.0, MINUTE_RAW = 16.0, MINUTE_HALF_WIDTH = 14.0;
    public static final int MINUTE_DELAY = 8, MINUTE_STEPS = 15, MINUTE_STEP_TICKS = 2;

    /** Chaos Spear: the lances, the damage of one, the range, and the flight time. */
    public static final double CHAOS_PER_LANCE = 2.4, CHAOS_RANGE = 14.0;
    public static final int CHAOS_LANCES = 5, CHAOS_DELAY = 5;

    // ================================================================ moves
    /** Hooked Pierce. */
    public static final double HOOK_RANGE = 12.0, HOOK_RAW = 5.0, HOOK_STOP = 2.2;
    public static final int HOOK_DELAY = 6;

    /** Portal Spear: how far the aim reaches, how far from it a footing is searched, the strike at the exit. */
    public static final double PORTAL_RANGE = 24.0, PORTAL_SEARCH = 3.0, PORTAL_EXIT_RADIUS = 2.8, PORTAL_RAW = 4.0;
    public static final int PORTAL_DELAY = 6, PORTAL_ECHO_TICKS = 60;

    /** Filo Rush: how long, how wide the trampling, the damage, the Speed amplifier (2 is Speed III) and how often foes are checked. */
    public static final double RUSH_RADIUS = 1.9, RUSH_RAW = 5.0;
    public static final int RUSH_TICKS = 50, RUSH_AMP = 2, RUSH_INTERVAL = 3, RUSH_TARGETS = 6;

    // ================================================================ defence
    /** Counter Thrust: the stance, the share of a melee blow that gets through, of a projectile, and the reach of the answer. */
    public static final double RIPOSTE_KEEP = 0.35, RIPOSTE_PROJECTILE_KEEP = 0.5, RIPOSTE_REACH = 7.0, RIPOSTE_RAW = 7.0;
    public static final int RIPOSTE_TICKS = 24;

    // ================================================================ time
    /** Time Reversal: look back this far, but not less than the minimum age, one snapshot per step, the buffer size, the search radius and the heal cap. */
    public static final int REWIND_WINDOW = 160, REWIND_MIN_AGE = 60, REWIND_STEP = 20, REWIND_CAPACITY = 10;
    public static final double REWIND_RADIUS = 3.0, REWIND_HEAL_CAP = 0.25;

    // ================================================================ passives
    /** Victor's Vigor: the share of maximum health a kill heals. */
    public static final double KILL_VIGOR = 0.02;
    /** Fervor of the Lust forms: stacks, the melee bonus of one stack (a share) and how long the stacks last after the last blow. */
    public static final int FERVOR_MAX = 5, FERVOR_TICKS = 120;
    public static final double FERVOR_PER_STACK = 0.06;
    /** Ravenous Aim: the reach and the cone (half angle, degrees) in which a technique snaps to a foe. */
    public static final double AIM_SNAP_RANGE = 9.0, AIM_SNAP_ANGLE = 75.0;

    // ================================================================ the Lust series
    /** Temptation: the radius, the pull per step (blocks per tick), the number of 2-tick steps, the damage to those who arrive, and how near counts as arrived. */
    public static final double TEMPT_RADIUS = 9.0, TEMPT_PULL = 0.3, TEMPT_RAW = 6.0, TEMPT_ARRIVE = 2.5;
    public static final int TEMPT_DELAY = 10, TEMPT_STEPS = 12, TEMPT_TARGETS = 8;

    /** Rapture: the reach, the damage, the share of the wielder's maximum health drunk, the corruption it costs on top of the curse's own, and the glint before the lunge. */
    public static final double RAPTURE_RANGE = 10.0, RAPTURE_RAW = 10.0, RAPTURE_HEAL = 0.05;
    public static final int RAPTURE_CORRUPTION = 12, RAPTURE_DELAY = 6;

    /** Thorn Chains: the circle, the pulses, the time between them, the damage of one, how far away it may be placed. */
    public static final double CHAINS_RADIUS = 4.0, CHAINS_RAW_PULSE = 2.4, CHAINS_RANGE = 14.0;
    public static final int CHAINS_PULSES = 5, CHAINS_INTERVAL = 10, CHAINS_DELAY = 10;

    /** Lust's Embrace: the radius, the pull steps (2 ticks each), the damage of the burst, the burst radius, the corruption it costs on top of the curse's own. */
    public static final double EMBRACE_RADIUS = 10.0, EMBRACE_RAW = 28.0, EMBRACE_BURST_RADIUS = 4.0;
    public static final int EMBRACE_PULL_STEPS = 10, EMBRACE_DELAY = 16, EMBRACE_CORRUPTION = 40;

    // ================================================================ the table
    /** A number for a description: no ".0" on a whole number. */
    static String n(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
    }

    /** Ticks as seconds for a description. */
    static String sec(int ticks) { return n(ticks / 20.0); }

    /** Ticks as "1 second" or "2.5 seconds" for a description. */
    static String secs(int ticks) {
        String s = sec(ticks);
        return s + (s.equals("1") ? " second" : " seconds");
    }

    public static final SkillRow PIERCING_THRUST = new SkillRow(SpearIds.PIERCING_THRUST, "Piercing Thrust",
            "Thrusts along a straight line " + n(THRUST_LENGTH) + " blocks long: after a " + THRUST_DELAY + " tick glint every foe in the line, up to " + THRUST_TARGETS
                    + ", takes " + n(THRUST_RAW) + " base damage. Walls stop it; step out of the line.",
            Kind.DAMAGE, Budget.ORDINARY, 60, 0.010, THRUST_RAW, THRUST_TARGETS);

    public static final SkillRow LUNGE = new SkillRow(SpearIds.LUNGE, "Dragoon Lunge",
            "Dashes up to " + n(LUNGE_DISTANCE) + " blocks forward, running through every foe in the way (up to " + LUNGE_TARGETS + ") for " + n(LUNGE_RAW)
                    + " base damage. The dash stops at walls and never ends over a gap.",
            Kind.DAMAGE, Budget.ORDINARY, 80, 0.010, LUNGE_RAW, LUNGE_TARGETS);

    public static final SkillRow VAULT = new SkillRow(SpearIds.VAULT, "Pole Vault",
            "Plants the spear and vaults about 7 blocks forward and 2 to 3 up. Slow Falling for " + secs(VAULT_SLOW_FALL) + " makes the landing soft.",
            Kind.UTILITY, Budget.ORDINARY, 100, 0.010, 0.0, 0);

    public static final SkillRow JAVELIN = new SkillRow(SpearIds.JAVELIN, "Javelin Throw",
            "Hurls the spear after a " + JAVELIN_DELAY + " tick wind-up: it flies about " + (int) (JAVELIN_SPEED * JAVELIN_LIFE) + " blocks and pierces up to " + JAVELIN_TARGETS
                    + " foes for " + n(JAVELIN_RAW) + " base damage each. Walls stop it.",
            Kind.DAMAGE, Budget.ORDINARY, 100, 0.015, JAVELIN_RAW, JAVELIN_TARGETS);

    public static final SkillRow WHIRLWIND = new SkillRow(SpearIds.WHIRLWIND, "Windmill Sweep",
            "Spins the long spear: after a " + WHIRL_DELAY + " tick ring of wind every foe within " + n(WHIRL_RADIUS) + " blocks (up to 12) takes " + n(WHIRL_RAW)
                    + " base damage and is thrown outward.",
            Kind.DAMAGE, Budget.ORDINARY, 100, 0.015, WHIRL_RAW, 12);

    public static final SkillRow RIPOSTE = new SkillRow(SpearIds.RIPOSTE, "Counter Thrust",
            "Raises a counter stance for " + secs(RIPOSTE_TICKS) + ". The next blow you take is cut by " + (int) Math.round((1 - RIPOSTE_KEEP) * 100)
                    + "% and, if the attacker is within " + n(RIPOSTE_REACH) + " blocks, answered with a thrust for " + n(RIPOSTE_RAW)
                    + " base damage. Arrows and other projectiles are only halved.",
            Kind.DAMAGE, Budget.ORDINARY, 140, 0.015, RIPOSTE_RAW, 1);

    public static final SkillRow CHAOS_SPEAR = new SkillRow(SpearIds.CHAOS_SPEAR, "Chaos Spear",
            "The spear splits into " + CHAOS_LANCES + " lances that fly at the nearest foes you can see within " + n(CHAOS_RANGE) + " blocks: " + n(CHAOS_PER_LANCE)
                    + " base damage each, so " + n(CHAOS_LANCES * CHAOS_PER_LANCE) + " on a lone foe. Cover and distance stop them.",
            Kind.DAMAGE, Budget.ORDINARY, 90, 0.015, CHAOS_LANCES * CHAOS_PER_LANCE, CHAOS_LANCES);

    public static final SkillRow HOOKED_PIERCE = new SkillRow(SpearIds.HOOKED_PIERCE, "Hooked Pierce",
            "Spears the foe under the crosshair within " + n(HOOK_RANGE) + " blocks for " + n(HOOK_RAW) + " base damage and hauls it to arm's length. A boss is not moved;"
                    + " you are pulled to it instead. Hard control: it shares the 12 second control lock.",
            Kind.DAMAGE, Budget.ORDINARY, 160, 0.020, HOOK_RAW, 1);

    public static final SkillRow METEOR_SPEAR = new SkillRow(SpearIds.METEOR_SPEAR, "Meteor Spear",
            "After a " + METEOR_DELAY + " tick glow the spear streaks " + n(METEOR_LENGTH) + " blocks like a falling star: up to " + METEOR_TARGETS + " foes in the lane take "
                    + n(METEOR_RAW) + " base damage, and the point where it ends bursts for " + n(METEOR_SPLASH) + " more within " + n(METEOR_SPLASH_RADIUS) + " blocks.",
            Kind.DAMAGE, Budget.HEAVY, 140, 0.025, METEOR_RAW + METEOR_SPLASH, METEOR_TARGETS);

    public static final SkillRow PORTAL_SPEAR = new SkillRow(SpearIds.PORTAL_SPEAR, "Portal Spear",
            "Opens a portal at the point you aim at within " + n(PORTAL_RANGE) + " blocks and steps through after " + PORTAL_DELAY + " ticks, landing on solid ground within "
                    + n(PORTAL_SEARCH) + " blocks of it. Foes within " + n(PORTAL_EXIT_RADIUS) + " blocks of the exit take " + n(PORTAL_RAW) + " base damage. Refused when there is no footing.",
            Kind.UTILITY, Budget.ORDINARY, 160, 0.020, PORTAL_RAW, 12);

    public static final SkillRow FILO_RUSH = new SkillRow(SpearIds.FILO_RUSH, "Filo Rush",
            "Runs like a filolial: Speed " + (RUSH_AMP + 1) + " for " + secs(RUSH_TICKS) + ", and every foe you run into (up to " + RUSH_TARGETS + ") takes "
                    + n(RUSH_RAW) + " base damage once and is knocked aside.",
            Kind.UTILITY, Budget.ORDINARY, 200, 0.020, RUSH_RAW, RUSH_TARGETS);

    public static final SkillRow TRIAINA = new SkillRow(SpearIds.TRIAINA, "Triaina",
            "A beam of energy: after a " + TRIAINA_DELAY + " tick wind-up it runs " + n(TRIAINA_LENGTH) + " blocks, " + n(TRIAINA_WIDTH) + " wide, striking up to "
                    + TRIAINA_TARGETS + " foes for " + n(TRIAINA_RAW) + " base damage. Walls stop it.",
            Kind.DAMAGE, Budget.HEAVY, 160, 0.025, TRIAINA_RAW, TRIAINA_TARGETS);

    public static final SkillRow MINUTE_HAND = new SkillRow(SpearIds.MINUTE_HAND, "Minute Hand",
            "After " + MINUTE_DELAY + " ticks a hand of light " + n(MINUTE_RADIUS) + " blocks long sweeps a full circle round you in "
                    + secs(MINUTE_STEPS * MINUTE_STEP_TICKS) + "; every foe it passes (up to 12) takes " + n(MINUTE_RAW)
                    + " base damage once. Break line of sight or turn with the hand to avoid it.",
            Kind.DAMAGE, Budget.HEAVY, 240, 0.030, MINUTE_RAW, 12);

    public static final SkillRow BRIONAC = new SkillRow(SpearIds.BRIONAC, "Brionac",
            "The spear's blazing light: a ring gathers for " + secs(BRIONAC_DELAY) + ", then a beam " + n(BRIONAC_LENGTH) + " blocks long and " + n(BRIONAC_WIDTH)
                    + " wide strikes up to 12 foes for " + n(BRIONAC_RAW) + " base damage and drives them back. Walls stop it; leave the line while the ring gathers.",
            Kind.ULTIMATE, Budget.ULTIMATE, 900, 0.060, BRIONAC_RAW, 12);

    public static final SkillRow TIME_REVERSAL = new SkillRow(SpearIds.TIME_REVERSAL, "Time Reversal",
            "Turns the minute hand back: you return to where you stood up to " + secs(REWIND_WINDOW) + " ago (the nearest safe place within " + n(REWIND_RADIUS)
                    + " blocks), regain up to " + (int) Math.round(REWIND_HEAL_CAP * 100) + "% of your maximum health if you were healthier then, and lose every harmful effect gained since."
                    + " The world and other people are untouched.",
            Kind.ULTIMATE, Budget.ULTIMATE, 2400, 0.060, 0.0, 0);

    public static final SkillRow TEMPTATION = new SkillRow(SpearIds.TEMPTATION, "Temptation",
            "A wave of allure: after " + TEMPT_DELAY + " ticks foes within " + n(TEMPT_RADIUS) + " blocks (up to " + TEMPT_TARGETS + ") are drawn towards you for "
                    + secs(TEMPT_STEPS * 2) + ", and those who arrive take " + n(TEMPT_RAW) + " base damage. Bosses are only slowed. Hard control: it shares the 12 second control lock.",
            Kind.DAMAGE, Budget.ORDINARY, 200, 0.025, TEMPT_RAW, TEMPT_TARGETS);

    public static final SkillRow RAPTURE = new SkillRow(SpearIds.RAPTURE, "Rapture",
            "Hunger for the nearest foe: a line fixes on it, and " + RAPTURE_DELAY + " ticks later you lunge up to " + n(RAPTURE_RANGE) + " blocks at it, run it through for " + n(RAPTURE_RAW) + " base damage and drink "
                    + (int) Math.round(RAPTURE_HEAL * 100) + "% of your maximum health from it. It adds " + RAPTURE_CORRUPTION + " corruption.",
            Kind.DAMAGE, Budget.HEAVY, 140, 0.025, RAPTURE_RAW, 1);

    public static final SkillRow THORN_CHAINS = new SkillRow(SpearIds.THORN_CHAINS, "Thorn Chains",
            "Chains and thorns burst from the ground within " + n(CHAINS_RADIUS) + " blocks of the point you aim at (up to " + n(CHAINS_RANGE) + " blocks away) and stay for "
                    + secs((CHAINS_PULSES - 1) * CHAINS_INTERVAL) + ": every " + secs(CHAINS_INTERVAL) + " foes inside take " + n(CHAINS_RAW_PULSE)
                    + " base damage and are slowed. Step out of the ring.",
            Kind.DAMAGE, Budget.ORDINARY, 180, 0.020, CHAINS_PULSES * CHAINS_RAW_PULSE, 12);

    public static final SkillRow LUSTS_EMBRACE = new SkillRow(SpearIds.LUSTS_EMBRACE, "Lust's Embrace",
            "Everything within " + n(EMBRACE_RADIUS) + " blocks (up to 12 foes) is dragged to you over " + secs(EMBRACE_PULL_STEPS * 2) + ", then the spear releases a crushing burst for "
                    + n(EMBRACE_RAW) + " base damage. Bosses are struck but not moved. It adds " + EMBRACE_CORRUPTION + " corruption.",
            Kind.ULTIMATE, Budget.ULTIMATE, 900, 0.060, EMBRACE_RAW, 12);

    /** Every row, in the order of {@link SpearIds#ALL}. */
    public static final List<SkillRow> ROWS = List.of(PIERCING_THRUST, LUNGE, VAULT, JAVELIN, WHIRLWIND, RIPOSTE, CHAOS_SPEAR, HOOKED_PIERCE, METEOR_SPEAR,
            PORTAL_SPEAR, FILO_RUSH, TRIAINA, MINUTE_HAND, BRIONAC, TIME_REVERSAL, TEMPTATION, RAPTURE, THORN_CHAINS, LUSTS_EMBRACE);

    /** The row with this skill id, or null. */
    public static SkillRow row(String id) {
        for (SkillRow r : ROWS) if (r.id().equals(id)) return r;
        return null;
    }
}
