package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Fmt;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.newuniverse.nusmp.heroweapon.weapon.gauntlet.GauntletNumbers.*;

/**
 * Why: the names and descriptions of the gauntlet's techniques and passives, built from {@link GauntletNumbers} so that the numbers in the text are the
 * numbers in the table, free of game classes so a test can read them, and the source of the lang fragment the lead merges. Plain ASCII English.
 */
public final class GauntletText {
    private GauntletText() {}

    /** Skill id to {name, description}. */
    public static final Map<String, String[]> SKILLS = new LinkedHashMap<>();
    /** Passive id to {name, description}. */
    public static final Map<String, String[]> PASSIVES = new LinkedHashMap<>();

    private static String points(Rank r) { return "Spends up to " + r.maxSpend() + " skill points for " + Fmt.num(r.perPoint()) + " more damage each."; }

    static {
        SKILLS.put(RUSH.spec().id(), new String[]{"Air Strike Rush",
                "Lunge about " + Fmt.num(RUSH_BLOCKS) + " blocks along your look line and strike everything you pass through for " + Fmt.num(RUSH.spec().rawBase()) + " damage. " + points(RUSH)});
        SKILLS.put(MOONLIGHT_KICK.spec().id(), new String[]{"Moonlight Kick",
                "A rising crescent kick: the arc in front of you shows, and " + Fmt.secs(KICK_DELAY) + " s later every foe still in it takes " + Fmt.num(MOONLIGHT_KICK.spec().rawBase()) + " damage and a launch. " + points(MOONLIGHT_KICK)
                        + " A higher launch with each point."});
        SKILLS.put(AFTERIMAGE_PALM.spec().id(), new String[]{"Afterimage Palm",
                "Slip behind the nearest foe in front of you (up to " + Fmt.num(PALM_RANGE) + " blocks), leaving an afterimage, and strike with an open palm for "
                        + Fmt.num(AFTERIMAGE_PALM.spec().rawBase()) + " damage that breaks a raised guard. " + points(AFTERIMAGE_PALM)});
        SKILLS.put(QUAKING_FIST.spec().id(), new String[]{"Shaking Quaking Fist",
                "Gather for a moment, then drive a shock along a line " + Fmt.num(FIST_LENGTH) + " blocks long: " + Fmt.num(QUAKING_FIST.spec().rawBase()) + " damage, a shove and a slow. "
                        + points(QUAKING_FIST)});
        SKILLS.put(TIGER_LEAP.spec().id(), new String[]{"Tiger Leap",
                "Leap about " + Fmt.num(LEAP_BLOCKS) + " blocks; where you come down the ground breaks for " + Fmt.num(TIGER_LEAP.spec().rawBase()) + " damage and a shove within "
                        + Fmt.num(LEAP_RADIUS) + " blocks. " + points(TIGER_LEAP)});
        SKILLS.put(TURTLE_CRUSH.spec().id(), new String[]{"Turtle Crush",
                "Raise a heavy fist, then crush the ground in front of you (" + Fmt.num(CRUSH_RADIUS) + " blocks): " + Fmt.num(TURTLE_CRUSH.spec().rawBase())
                        + " damage that breaks guards and weakens. " + points(TURTLE_CRUSH)});
        SKILLS.put(IRON_GUARD.id(), new String[]{"Iron Guard",
                "Brace for " + Fmt.secs(GUARD_TICKS) + " s: you take " + Fmt.pct(1.0 - GUARD_TAKEN) + " percent less damage from blows and shots, every blow you take gives a skill point (up to "
                        + GUARD_POINTS_MAX + ") and the first one is answered with a counter of up to " + Fmt.num(COUNTER_RAW) + " damage."});
        SKILLS.put(SECOND_WIND.id(), new String[]{"Second Wind",
                "Breathe: heal " + Fmt.pct(SECOND_WIND.heal()) + " percent of your health, wash away one harmful effect and gain " + POINTS_WIND + " skill points."});
        SKILLS.put(DRAGON_SLAYER.spec().id(), new String[]{"Dragon Slaying Conflagration Fist",
                "Needs " + DRAGON_MIN + " skill points and spends them all. After " + Fmt.secs(DRAGON_DELAY) + " s of blazing warning a fist of flame strikes the first foe in front of you (up to "
                        + Fmt.num(DRAGON_LENGTH) + " blocks) for " + Fmt.num(DRAGON_SLAYER.spec().rawBase()) + " damage, " + Fmt.num(DRAGON_SLAYER.perPoint()) + " more for each point beyond "
                        + DRAGON_MIN + ", and burns foes within " + Fmt.num(DRAGON_SPLASH_RADIUS) + " blocks of it for " + Fmt.num(DRAGON_SPLASH_RAW) + "."});

        PASSIVES.put("rank_points", new String[]{"Skill Power-up Points",
                "Blows, techniques and the hits your guard takes fill a pool of up to " + PointPool.MAX + " skill points; the next technique spends them to hit harder. They fade after "
                        + Fmt.secs(PointPool.IDLE) + " idle seconds."});
        PASSIVES.put("hardened_fist", new String[]{"Hardened Fist",
                "You take " + Fmt.pct(1.0 - HARDENED_TAKEN) + " percent less damage and a full swing gives one more skill point."});
        PASSIVES.put("blossom_step", new String[]{"Blossom Step",
                "Afterimage Palm and Tiger Leap can be used again " + Fmt.pct(1.0 - BLOSSOM_COOLDOWN) + " percent sooner."});
        PASSIVES.put("spirit_bane", new String[]{"Spirit Bane",
                "Your techniques deal " + Fmt.num(SPIRIT_BONUS) + " more damage to undead foes."});
        PASSIVES.put("dragon_heart", new String[]{"Dragon Heart",
                "The Conflagration Fist counts " + DRAGON_HEART_BONUS + " skill point more than you hold: it opens at " + (DRAGON_MIN - DRAGON_HEART_BONUS) + " and hits harder with the same pool."});
    }

    public static String name(String skillId) { return SKILLS.get(skillId)[0]; }

    public static String desc(String skillId) { return SKILLS.get(skillId)[1]; }
}
