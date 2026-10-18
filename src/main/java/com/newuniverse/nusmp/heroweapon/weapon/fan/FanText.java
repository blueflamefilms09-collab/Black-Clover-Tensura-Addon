package com.newuniverse.nusmp.heroweapon.weapon.fan;

import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.n;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.pct;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.sec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the names and descriptions of the Fan's techniques and passives are read in two places that must agree, the skill objects (the tooltip and the Weapon
 * Book read them) and the language file ({@code hero.skill.<id>.name}, {@code .desc}, {@code hero.passive.fan.<id>} and {@code .desc}), so they are written once,
 * here, with no game imports. A test holds them to the house rules (a name of at most 28 characters, a description of one or two sentences that fits a tooltip,
 * plain ASCII) and a small program writes the language fragment from this table, so the two cannot drift apart.
 */
public final class FanText {
    private FanText() {}

    /** {id, name, description} of every skill, in the order the weapon lists them. The numbers in the sentences are the constants the skills use. */
    private static final String[][] SKILLS = {
            {FanNumbers.FLOWER_WIND, "Flower Wind",
                    "A wide sweep of cutting wind and petals: up to " + FanNumbers.FLOWER_TARGETS + " foes within " + n(FanNumbers.FLOWER_RANGE) + " blocks in front take "
                            + n(FanNumbers.FLOWER_RAW) + " damage and are nudged back, and a sweep that cuts two or more gathers magic residue. Counter: stay out of the arc."},
            {FanNumbers.MOON_BREAK, "Moon Break",
                    "Flings a crescent of wind " + n(FanNumbers.MOON_RANGE) + " blocks forward that cuts up to " + FanNumbers.MOON_TARGETS + " foes in its path for "
                            + n(FanNumbers.MOON_RAW) + " damage. Counter: step aside; walls stop it."},
            {FanNumbers.REPEL_GUST, "Repelling Gust",
                    "A ring of wind pushes every foe within " + n(FanNumbers.GUST_RADIUS) + " blocks away and turns arrows back on their archers. It deals no damage. Heavy foes barely move."},
            {FanNumbers.GALE_PULL, "Gale Pull",
                    "Draws up to " + FanNumbers.PULL_TARGETS + " foes in front of you toward a point just ahead (heavy foes less). It is hard control, so it shares the 12 second "
                            + "control rest, and bosses are not pulled. Counter: stay beyond " + n(FanNumbers.PULL_RANGE) + " blocks."},
            {FanNumbers.WIND_DANCE, "Wind Dance",
                    "A gliding dash of about " + n(FanNumbers.DANCE_LENGTH) + " blocks that brushes aside foes in the way for " + n(FanNumbers.DANCE_RAW)
                            + " damage; slow falling follows for " + sec(FanNumbers.DANCE_SLOW_FALL_TICKS) + " seconds."},
            {FanNumbers.MIST_VEIL, "Mist Veil",
                    "Wraps you in mist for " + sec(FanNumbers.VEIL_TICKS) + " seconds: mobs hunting you lose their target and you turn invisible. Attacking ends the veil. "
                            + "Foes right beside you still notice you."},
            {FanNumbers.SNOW_PLUM, "Snow Plum",
                    "Plum blossoms bless you and your friends within " + n(FanNumbers.PLUM_RADIUS) + " blocks: speed for " + sec(FanNumbers.PLUM_TICKS)
                            + " seconds and a small heal (never more than " + pct(FanNumbers.PLUM_HEAL_CAP) + " percent of their health)."},
            {FanNumbers.TORTOISE_SHELL_CRACKER, "Tortoise Shell Cracker",
                    "The wind coils at the fan tip, then a thrust cracks the foe on your crosshair for " + n(FanNumbers.TORTOISE_BASE) + " damage plus half its armour (up to "
                            + n(FanNumbers.TORTOISE_ARMOR_BONUS_CAP) + " more). Counter: leave its line of sight or raise a shield."},
            {FanNumbers.SNOW_MOON_FLOWER, "Reverse Snow Moon Flower",
                    "Ultimate. After a gathering of red petals a tornado of petals stands where you aim for " + sec(FanNumbers.SNOW_MOON_PULSES * FanNumbers.SNOW_MOON_INTERVAL)
                            + " seconds and cuts foes inside it " + FanNumbers.SNOW_MOON_PULSES + " times; every residue stack burns for more damage. Counter: leave the red circle."},
    };

    /** {id, name, description} of every passive a form of the Fan may list. */
    private static final String[][] PASSIVES = {
            {FanNumbers.P_FEATHERFALL, "Featherfall", "The fan's wind cushions you: falls hurt " + pct(1.0 - FanNumbers.FALL_FACTOR) + " percent less, and holding sneak while you fall lets you glide down slowly."},
            {FanNumbers.P_RESIDUE, "Magic Residue",
                    "The fan drinks the magic that leaks from foes: a kill, a sweep that cuts two or more, and every " + FanNumbers.SWINGS_PER_RESIDUE
                            + " full swings each add a stack (up to " + FanNumbers.RESIDUE_MAX + ") that the petal tornado burns. Putting the fan away loses them."},
            {FanNumbers.P_DRAGON_SURGE, "Dragon Surge",
                    "At " + FanNumbers.RESIDUE_MAX + " residue the sweep of Flower Wind reaches " + n(FanNumbers.SURGE_RANGE) + " blocks farther and cuts for " + n(FanNumbers.SURGE_RAW) + " more."},
            {FanNumbers.P_SEALING_TIDE, "Sealing Tide", "The gust and the pull of this sealing fan reach " + n(FanNumbers.TIDE_EXTRA) + " blocks farther."},
            {FanNumbers.P_TIGER_POUNCE, "Tiger Pounce", "Wind Dance carries you a quarter faster."},
            {FanNumbers.P_KEYSTONE_WARD, "Keystone Ward",
                    "While the Mist Veil holds, projectiles hurt you " + pct(1.0 - FanNumbers.VEIL_PROJECTILE_FACTOR) + " percent less."},
    };

    private static final Map<String, String[]> SKILL_MAP = table(SKILLS), PASSIVE_MAP = table(PASSIVES);

    public static List<String> skillIds() { return List.copyOf(SKILL_MAP.keySet()); }

    public static List<String> passiveIds() { return List.copyOf(PASSIVE_MAP.keySet()); }

    public static String skillName(String id) { return SKILL_MAP.get(id)[0]; }

    public static String skillDesc(String id) { return SKILL_MAP.get(id)[1]; }

    public static String passiveName(String id) { return PASSIVE_MAP.get(id)[0]; }

    public static String passiveDesc(String id) { return PASSIVE_MAP.get(id)[1]; }

    private static Map<String, String[]> table(String[][] rows) {
        Map<String, String[]> m = new LinkedHashMap<>();
        for (String[] r : rows) m.put(r[0], new String[] {r[1], r[2]});
        return m;
    }

    /** The language entries of the Fan: skill and passive names and descriptions, keyed as the lang file wants them. */
    public static Map<String, String> lang() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String[] r : SKILLS) {
            out.put("hero.skill." + r[0] + ".name", r[1]);
            out.put("hero.skill." + r[0] + ".desc", r[2]);
        }
        for (String[] r : PASSIVES) {
            out.put("hero.passive.fan." + r[0], r[1]);
            out.put("hero.passive.fan." + r[0] + ".desc", r[2]);
        }
        return out;
    }
}
