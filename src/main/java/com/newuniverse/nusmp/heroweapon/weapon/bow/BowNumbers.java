package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow.Kind;

import java.util.List;
import java.util.Locale;

/**
 * Why: every number of the Legendary Bow lives here and nowhere else (the draw, the range, the damage, the cooldowns, the costs), so the balance the owner set
 * (design.md section 5; the Bow is range and utility, its damage per second stays below the Sword's) can be checked by one test and an edit cannot drift
 * between the skill code, its description and its row in the table. A technique's description is built from the same constants, so the text a player reads
 * in the Weapon Book can never disagree with what the code does. The damage is RAW damage: the balance law scales it by the wielder's existence and the
 * bond level (1.0 at level 0 to 2.2 at level 100). Distances are blocks; delays are ticks (20 a second). Nothing here depends on the game.
 */
public final class BowNumbers {
    private BowNumbers() {}

    // ================================================================ the plain arrow of light
    /** Raw damage of a full draw, the ceiling any bonus may reach, the cost of a full draw (a share of the maximum magicules) and how wide the arrow is. */
    public static final double BASIC_RAW = 7.0, BASIC_MAX_RAW = 12.0, BASIC_COST = 0.004, BASIC_WIDTH = 0.45;
    /** The attack damage of the form counts as draw strength: each point is this much more damage (a share), on the plain arrow only. */
    public static final double STRENGTH_SCALE = 0.04;
    /** The range of the weakest and of a full draw, and what Farsight multiplies it by. */
    public static final double RANGE_MIN = 16.0, RANGE_MAX = 48.0, FARSIGHT = 1.3;
    /** Piercing Arrows: the second foe takes this share. Eagle Eye: foes farther than the distance take this much more. */
    public static final double PIERCE_FACTOR = 0.7, EAGLE_EYE_BONUS = 1.25, EAGLE_EYE_DISTANCE = 24.0;
    /** Law Fanatic makes plain arrows this much stronger while it lasts; the Pride arrows are this much stronger and this wide. */
    public static final double LAW_BONUS = 1.15, PRIDE_BONUS = 1.2, PRIDE_WIDTH = 1.0;
    /** Ticks an arming waits for its draw, and the pause after a shot before the next may leave the string. */
    public static final int ARMED_TICKS = 200, SHOT_LOCK = 4;

    // ================================================================ the arming techniques
    /** Eagle Piercing Shot: the range of the sniper arrow, its width, the damage, the foes it pierces. */
    public static final double SNIPER_RANGE = 64.0, SNIPER_WIDTH = 0.5, SNIPER_RAW = 14.0;
    public static final int SNIPER_TARGETS = 6;
    /** Bind Arrow: the range, the damage, and the root (ticks, before the control law shortens it). */
    public static final double NET_RANGE = 40.0, NET_RAW = 5.0;
    public static final int NET_ROOT = 30;
    /** Marking Arrow: the range, the damage, and how long the mark lasts. */
    public static final double MARK_RANGE = 40.0, MARK_RAW = 3.0;
    public static final int MARK_TICKS = 160;
    /** Burst Arrow: the range, the damage, the radius and the fuse after it lands. */
    public static final double BURST_RANGE = 40.0, BURST_RAW = 10.0, BURST_RADIUS = 3.5;
    public static final int BURST_FUSE = 10, BURST_TARGETS = 8;
    /** Seeker Arrow: the damage, the cone (half angle) and range in which it picks a foe, its speed (blocks per tick), life (ticks) and radius. */
    public static final double SEEK_RAW = 8.0, SEEK_CONE = 35.0, SEEK_RANGE = 40.0, SEEK_SPEED = 1.5, SEEK_RADIUS = 0.6;
    public static final int SEEK_LIFE = 40;
    /** Second Arrow: the share of the second arrow and the ticks between the two (not less than {@link SkillRow#HURT_IMMUNITY}, or the game would ignore the second blow). Together they never pass the ordinary budget. */
    public static final double SECOND_FACTOR = 0.6;
    public static final int SECOND_DELAY = 10;
    /** Pride's Command: the damage, how far from the victim the monsters that turn on it may stand, and how many. */
    public static final double COMMAND_RAW = 4.0, COMMAND_RANGE = 10.0, COMMAND_RANGE_SHOT = 40.0;
    public static final int COMMAND_RECRUITS = 3;

    // ================================================================ the direct techniques
    /** Arrow Squall: the arrows, the spread (degrees), the range, the damage of one and the wait. */
    public static final double SQUALL_SPREAD = 40.0, SQUALL_RANGE = 20.0, SQUALL_PER_ARROW = 1.7, SQUALL_WIDTH = 0.5;
    public static final int SQUALL_ARROWS = 7, SQUALL_DELAY = 6;
    /** Bird Hunting: the shafts, the reach, the damage and the wait. */
    public static final double BIRD_RANGE = 36.0, BIRD_RAW = 11.0, BIRD_CONE = 25.0;
    public static final int BIRD_SHAFTS = 5, BIRD_DELAY = 6;
    /** Backstep Shot: the spring, the damage, the reach for the foe it shoots. */
    public static final double RETREAT_DISTANCE = 6.0, RETREAT_RAW = 5.0, RETREAT_SEEK = 20.0;
    /** Meteor Bow: the reach, the radius, the damage and the fall time. */
    public static final double METEOR_RANGE = 48.0, METEOR_RADIUS = 4.0, METEOR_RAW = 16.0;
    public static final int METEOR_DELAY = 24;
    /** Arrow Rain: the reach, the circle, the number of waves, the time between them, the damage of one wave and the wait before the first. */
    public static final double RAIN_RANGE = 40.0, RAIN_RADIUS = 5.0, RAIN_RAW_WAVE = 2.0;
    public static final int RAIN_WAVES = 6, RAIN_INTERVAL = 10, RAIN_DELAY = 10;
    /** Frozen Rain: as the rain, with the slowing. */
    public static final double FROZEN_RANGE = 36.0, FROZEN_RADIUS = 6.0, FROZEN_RAW_WAVE = 2.0;
    public static final int FROZEN_WAVES = 5, FROZEN_INTERVAL = 10, FROZEN_DELAY = 10, FROZEN_SLOW_TICKS = 30;
    /** Starfall Volley, the ultimate rain. */
    public static final double STARFALL_RANGE = 40.0, STARFALL_RADIUS = 8.0, STARFALL_RAW_WAVE = 3.25;
    public static final int STARFALL_WAVES = 8, STARFALL_INTERVAL = 10, STARFALL_DELAY = 20;

    // ================================================================ the Pride series
    /** Law Fanatic: how long the armour of the aura lasts. */
    public static final int LAW_TICKS = 200;
    /** Bull of Phalaris: the reach, the damage, the root and the wait. */
    public static final double BULL_RANGE = 28.0, BULL_RAW = 10.0;
    public static final int BULL_ROOT = 30, BULL_DELAY = 10;
    /** Autocracy Justice: the reach, the circle, the waves, the time between them, the damage of one wave, the wait, and the corruption it adds. */
    public static final double AUTOCRACY_RANGE = 36.0, AUTOCRACY_RADIUS = 7.0, AUTOCRACY_RAW_WAVE = 7.0;
    public static final int AUTOCRACY_WAVES = 4, AUTOCRACY_INTERVAL = 10, AUTOCRACY_DELAY = 20, AUTOCRACY_CORRUPTION = 30;

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

    /** A share (0 to 1) as a whole percent for a description. */
    static String pct(double share) { return Math.round(share * 100) + "%"; }

    /** What the second arrow of Second Arrow adds to the first: a share of it, but together they never pass the ordinary budget. */
    public static double secondRaw(double first) { return Math.max(0.0, Math.min(first * SECOND_FACTOR, BASIC_MAX_RAW - first)); }

    public static final SkillRow PIERCING_SHOT = new SkillRow(BowIds.PIERCING_SHOT, "Eagle Piercing Shot",
            "Your next full draw (" + pct(ArmedMode.SNIPER.minFraction()) + " or more) looses a sniper arrow of light that runs up to " + n(SNIPER_RANGE) + " blocks and pierces up to "
                    + SNIPER_TARGETS + " foes for " + n(SNIPER_RAW) + " base damage each. A thin line shows where it will fly, so everyone can see it coming. It stays ready for "
                    + secs(ARMED_TICKS) + ".",
            Kind.DAMAGE, Budget.HEAVY, 140, 0.020, SNIPER_RAW, SNIPER_TARGETS);

    public static final SkillRow NET_ARROW = new SkillRow(BowIds.NET_ARROW, "Bind Arrow",
            "Your next draw of " + pct(ArmedMode.NET.minFraction()) + " or more pins the first foe it hits to the ground for up to " + secs(NET_ROOT) + " (" + n(NET_RAW)
                    + " base damage). Hard control: it shares the 12 second control lock, and a boss is only staggered. It stays ready for " + secs(ARMED_TICKS) + ".",
            Kind.DAMAGE, Budget.ORDINARY, 160, 0.020, NET_RAW, 1);

    public static final SkillRow MARK_ARROW = new SkillRow(BowIds.MARK_ARROW, "Marking Arrow",
            "Your next draw of " + pct(ArmedMode.MARK.minFraction()) + " or more marks the first foe it hits for " + secs(MARK_TICKS) + ": it glows through walls and is left frail, so you and your friends hurt it more ("
                    + n(MARK_RAW) + " base damage). It stays ready for " + secs(ARMED_TICKS) + ".",
            Kind.DAMAGE, Budget.ORDINARY, 140, 0.015, MARK_RAW, 1);

    public static final SkillRow EXPLOSIVE_ARROW = new SkillRow(BowIds.EXPLOSIVE_ARROW, "Burst Arrow",
            "Your next draw of " + pct(ArmedMode.BURST.minFraction()) + " or more looses an arrow that sticks in the first foe or wall within " + n(BURST_RANGE) + " blocks and bursts " + secs(BURST_FUSE)
                    + " later: every foe within " + n(BURST_RADIUS) + " blocks of it takes " + n(BURST_RAW) + " base damage and is pushed back. The ring on the ground shows the fuse. It breaks no blocks.",
            Kind.DAMAGE, Budget.HEAVY, 120, 0.025, BURST_RAW, BURST_TARGETS);

    public static final SkillRow TRACKING_SHOT = new SkillRow(BowIds.TRACKING_SHOT, "Seeker Arrow",
            "Your next draw of " + pct(ArmedMode.SEEK.minFraction()) + " or more looses an arrow that homes in on the foe nearest your crosshair (within " + n(SEEK_CONE) + " degrees and " + n(SEEK_RANGE)
                    + " blocks) for " + n(SEEK_RAW) + " base damage. Walls stop it, and it flies for " + secs(SEEK_LIFE) + ".",
            Kind.DAMAGE, Budget.ORDINARY, 120, 0.020, SEEK_RAW, 1);

    public static final SkillRow SECOND_ARROW = new SkillRow(BowIds.SECOND_ARROW, "Second Arrow",
            "Your next draw of " + pct(ArmedMode.SECOND.minFraction()) + " or more looses two arrows; the second follows " + secs(SECOND_DELAY) + " later at " + (int) Math.round(SECOND_FACTOR * 100)
                    + "% power. Together they never do more than " + n(BASIC_MAX_RAW) + " base damage to one foe.",
            Kind.DAMAGE, Budget.ORDINARY, 100, 0.010, BASIC_MAX_RAW, 2);

    public static final SkillRow ARROW_RAIN = new SkillRow(BowIds.ARROW_RAIN, "Arrow Rain",
            "You shoot into the sky; after " + RAIN_DELAY + " ticks arrows fall within " + n(RAIN_RADIUS) + " blocks of the point you aimed at (up to " + n(RAIN_RANGE) + " blocks away), "
                    + RAIN_WAVES + " waves " + secs(RAIN_INTERVAL) + " apart, " + n(RAIN_RAW_WAVE) + " base damage each wave. The circle is drawn on the ground.",
            Kind.DAMAGE, Budget.ORDINARY, 160, 0.025, RAIN_WAVES * RAIN_RAW_WAVE, 12);

    public static final SkillRow ARROW_SQUALL = new SkillRow(BowIds.ARROW_SQUALL, "Arrow Squall",
            SQUALL_ARROWS + " arrows fly in a " + n(SQUALL_SPREAD) + " degree fan up to " + n(SQUALL_RANGE) + " blocks after a " + SQUALL_DELAY + " tick draw; each arrow that strikes a foe deals "
                    + n(SQUALL_PER_ARROW) + " base damage, so a close foe can take all " + SQUALL_ARROWS + ". The fan is drawn first.",
            Kind.DAMAGE, Budget.ORDINARY, 80, 0.015, SQUALL_ARROWS * SQUALL_PER_ARROW, 12);

    public static final SkillRow BIRD_HUNTING = new SkillRow(BowIds.BIRD_HUNTING, "Bird Hunting",
            BIRD_SHAFTS + " shafts lock onto the foe under your crosshair within " + n(BIRD_RANGE) + " blocks and strike together after " + BIRD_DELAY + " ticks for " + n(BIRD_RAW)
                    + " base damage. Breaking line of sight saves the foe.",
            Kind.DAMAGE, Budget.ORDINARY, 100, 0.015, BIRD_RAW, 1);

    public static final SkillRow RETREAT_SHOT = new SkillRow(BowIds.RETREAT_SHOT, "Backstep Shot",
            "You spring up to " + n(RETREAT_DISTANCE) + " blocks backwards to safe ground and loose a quick arrow at the nearest foe in front of you for " + n(RETREAT_RAW) + " base damage.",
            Kind.UTILITY, Budget.ORDINARY, 100, 0.010, RETREAT_RAW, 1);

    public static final SkillRow METEOR_BOW = new SkillRow(BowIds.METEOR_BOW, "Meteor Bow",
            "A star falls where you aim (up to " + n(METEOR_RANGE) + " blocks) after " + secs(METEOR_DELAY) + "; the circle (" + n(METEOR_RADIUS) + " blocks from its centre) is drawn on the ground. Foes inside take "
                    + n(METEOR_RAW) + " base damage and are thrown back.",
            Kind.DAMAGE, Budget.HEAVY, 200, 0.030, METEOR_RAW, 12);

    public static final SkillRow FROZEN_RAIN = new SkillRow(BowIds.FROZEN_RAIN, "Frozen Rain",
            "An icy shower within " + n(FROZEN_RADIUS) + " blocks of the point you aim at (up to " + n(FROZEN_RANGE) + " blocks away): " + FROZEN_WAVES + " times, " + secs(FROZEN_INTERVAL)
                    + " apart, foes inside take " + n(FROZEN_RAW_WAVE) + " base damage and are slowed.",
            Kind.DAMAGE, Budget.ORDINARY, 180, 0.025, FROZEN_WAVES * FROZEN_RAW_WAVE, 12);

    public static final SkillRow STARFALL_VOLLEY = new SkillRow(BowIds.STARFALL_VOLLEY, "Starfall Volley",
            "A storm of light arrows rises and falls within " + n(STARFALL_RADIUS) + " blocks of the point you aim at (up to " + n(STARFALL_RANGE) + " blocks away): after " + secs(STARFALL_DELAY)
                    + ", " + STARFALL_WAVES + " waves " + secs(STARFALL_INTERVAL) + " apart, " + n(STARFALL_RAW_WAVE) + " base damage each wave to every foe inside.",
            Kind.ULTIMATE, Budget.ULTIMATE, 900, 0.060, STARFALL_WAVES * STARFALL_RAW_WAVE, 12);

    public static final SkillRow LAW_FANATIC = new SkillRow(BowIds.LAW_FANATIC, "Law Fanatic",
            "A sinister aura condenses into armour: Resistance and Speed for " + secs(LAW_TICKS) + ", and your plain arrows hit " + (int) Math.round((LAW_BONUS - 1) * 100) + "% harder.",
            Kind.UTILITY, Budget.ORDINARY, 300, 0.020, 0.0, 0);

    public static final SkillRow BULL_OF_PHALARIS = new SkillRow(BowIds.BULL_OF_PHALARIS, "Bull of Phalaris",
            "A brass bull closes round the foe under your crosshair (within " + n(BULL_RANGE) + " blocks): after " + BULL_DELAY + " ticks it is held for up to " + secs(BULL_ROOT)
                    + " and takes " + n(BULL_RAW) + " base damage in the furnace. Hard control: a boss is only staggered.",
            Kind.DAMAGE, Budget.HEAVY, 220, 0.025, BULL_RAW, 1);

    public static final SkillRow PRIDE_COMMAND = new SkillRow(BowIds.PRIDE_COMMAND, "Pride's Command",
            "Your next draw of " + pct(ArmedMode.COMMAND.minFraction()) + " or more turns the first monster it hits against its own side: it takes " + n(COMMAND_RAW) + " base damage and up to " + COMMAND_RECRUITS
                    + " monsters near it fight it for as long as their minds allow. Never a boss or a player. Hard control: it shares the 12 second control lock.",
            Kind.DAMAGE, Budget.ORDINARY, 240, 0.025, COMMAND_RAW, 1);

    public static final SkillRow AUTOCRACY_JUSTICE = new SkillRow(BowIds.AUTOCRACY_JUSTICE, "Autocracy Justice",
            "Orbs of light pour from the bow and form a lion that tears at everything within " + n(AUTOCRACY_RADIUS) + " blocks of the point you aim at (up to " + n(AUTOCRACY_RANGE) + " blocks away): after "
                    + secs(AUTOCRACY_DELAY) + ", " + AUTOCRACY_WAVES + " waves for " + n(AUTOCRACY_RAW_WAVE) + " base damage each. It adds " + AUTOCRACY_CORRUPTION + " corruption.",
            Kind.ULTIMATE, Budget.ULTIMATE, 900, 0.060, AUTOCRACY_WAVES * AUTOCRACY_RAW_WAVE, 12);

    /** Every row, in the order of {@link BowIds#ALL}. */
    public static final List<SkillRow> ROWS = List.of(PIERCING_SHOT, NET_ARROW, MARK_ARROW, EXPLOSIVE_ARROW, TRACKING_SHOT, SECOND_ARROW, ARROW_RAIN, ARROW_SQUALL,
            BIRD_HUNTING, RETREAT_SHOT, METEOR_BOW, FROZEN_RAIN, STARFALL_VOLLEY, LAW_FANATIC, BULL_OF_PHALARIS, PRIDE_COMMAND, AUTOCRACY_JUSTICE);

    /** The row with this skill id, or null. */
    public static SkillRow row(String id) {
        for (SkillRow r : ROWS) if (r.id().equals(id)) return r;
        return null;
    }
}
