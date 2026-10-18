package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.n;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.pct;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.sec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the names and descriptions of the Scythe's techniques and passives are read in two places that must agree, the skill objects (the tooltip and the Weapon
 * Book read them) and the language file ({@code hero.skill.<id>.name}, {@code .desc}, {@code hero.passive.scythe.<id>} and {@code .desc}), so they are written
 * once, here, with no game imports, and the numbers in the sentences are the constants the skills use. A test holds them to the house rules and a small program
 * writes the language fragment from this table.
 */
public final class ScytheText {
    private ScytheText() {}

    /** {id, name, description} of every skill, in the order the weapon lists them. */
    private static final String[][] SKILLS = {
            {ScytheNumbers.FULL_MOON_ARMY, "Full Moon Army",
                    "A full circle with the long scythe: up to " + ScytheNumbers.MOON_TARGETS + " foes within " + n(ScytheNumbers.MOON_RADIUS) + " blocks take "
                            + n(ScytheNumbers.MOON_RAW) + " damage and are nudged away. Counter: keep beyond " + n(ScytheNumbers.MOON_RADIUS) + " blocks."},
            {ScytheNumbers.FLYING_CIRCLE, "Flying Circle",
                    "Throws a spinning disc of energy " + n(ScytheNumbers.CIRCLE_RANGE) + " blocks forward; it cuts up to " + ScytheNumbers.CIRCLE_TARGETS + " foes for "
                            + n(ScytheNumbers.CIRCLE_RAW_OUT) + " damage on the way out and returns to cut them for " + n(ScytheNumbers.CIRCLE_RAW_BACK) + " more. Counter: step out of its line."},
            {ScytheNumbers.WIND_SLICE, "Wind Slice",
                    "A whirling cut in front of you: up to " + ScytheNumbers.SLICE_TARGETS + " foes within " + n(ScytheNumbers.SLICE_RANGE) + " blocks take "
                            + n(ScytheNumbers.SLICE_RAW) + " damage and are pushed back hard. It makes room when you are crowded."},
            {ScytheNumbers.REAP, "Soul Reap",
                    "After a short swing-up the scythe reaps a cone " + n(ScytheNumbers.REAP_RANGE) + " blocks long for " + n(ScytheNumbers.REAP_BASE)
                            + " damage; a mob at or under " + pct(ScytheNumbers.REAP_THRESHOLD) + " percent health (never a boss or a player) takes double. Each foe it kills heals you "
                            + pct(ScytheNumbers.REAP_HEAL_PER_FOE) + " percent, at most " + pct(ScytheNumbers.REAP_HEAL_CAP) + " percent. Counter: leave the cone during the swing-up."},
            {ScytheNumbers.PROTECTION_BREAK, "Protection Break",
                    "After a short gathering a spirit-edged chop hits the foe on your crosshair for " + n(ScytheNumbers.BREAK_BASE) + " damage plus " + n(ScytheNumbers.BREAK_PER_ARMOR)
                            + " per point of its armour (up to " + n(ScytheNumbers.BREAK_BONUS_CAP) + " more). Counter: sidestep it or raise a shield."},
            {ScytheNumbers.DEADLY_DANCE, "Deadly Dance",
                    ScytheNumbers.DANCE_SWINGS + " swings in a row in front of you, each cutting foes within " + n(ScytheNumbers.DANCE_RANGE) + " blocks for " + n(ScytheNumbers.DANCE_RAW)
                            + " damage (" + n(ScytheNumbers.DANCE_RAW * ScytheNumbers.DANCE_SWINGS) + " in all). Counter: leave the arc."},
            {ScytheNumbers.FLOATING_SCYTHE, "Floating Scythe",
                    "A second scythe floats at your side for " + sec(ScytheNumbers.FLOAT_STRIKES * ScytheNumbers.FLOAT_INTERVAL) + " seconds and cuts the nearest foe within "
                            + n(ScytheNumbers.FLOAT_RANGE) + " blocks for " + n(ScytheNumbers.FLOAT_RAW) + " damage every second. You cannot conjure another while it floats."},
            {ScytheNumbers.SOUL_HARVEST, "Soul Harvest",
                    "Ultimate. Souls gather round you for a moment, then a huge reaping circle of " + n(ScytheNumbers.HARVEST_RADIUS) + " blocks cuts every foe for "
                            + n(ScytheNumbers.HARVEST_BASE) + " damage plus " + n(ScytheNumbers.HARVEST_PER_SOUL) + " per soul collected, " + pct(ScytheNumbers.HARVEST_WEAK_FACTOR - 1.0)
                            + " percent more on the weak, and heals you " + pct(ScytheNumbers.HARVEST_HEAL_PER_FOE) + " percent per foe reaped. Counter: leave the circle."},
            {ScytheNumbers.SCYTHE_ZERO, "Scythe 0",
                    "A cut that deals no damage: it severs the beneficial effects (speed, strength, regeneration and the like) from up to " + ScytheNumbers.ZERO_TARGETS
                            + " foes in front of you. Counter: none, it only takes."},
    };

    /** {id, name, description} of every passive a form of the Scythe may list. */
    private static final String[][] PASSIVES = {
            {ScytheNumbers.P_SOUL_TITHE, "Soul Tithe",
                    "Every kill with the scythe in hand gathers a soul (up to " + ScytheNumbers.SOUL_CAP + ") that fades " + sec(ScytheNumbers.SOUL_FADE_TICKS)
                            + " seconds after the last one; Soul Harvest spends them. Putting the scythe away loses them."},
            {ScytheNumbers.P_DRAGON_SOUL, "Dragon Soul", "The scythe can hold up to " + ScytheNumbers.SOUL_CAP_DRAGON + " souls."},
            {ScytheNumbers.P_NULL_EDGE, "Null Edge",
                    "A melee hit strips one beneficial effect from the foe; the same foe is not stripped again for " + sec(ScytheNumbers.NULL_EDGE_GAP) + " seconds."},
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

    /** The language entries of the Scythe: skill and passive names and descriptions, keyed as the lang file wants them. */
    public static Map<String, String> lang() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String[] r : SKILLS) {
            out.put("hero.skill." + r[0] + ".name", r[1]);
            out.put("hero.skill." + r[0] + ".desc", r[2]);
        }
        for (String[] r : PASSIVES) {
            out.put("hero.passive.scythe." + r[0], r[1]);
            out.put("hero.passive.scythe." + r[0] + ".desc", r[2]);
        }
        return out;
    }
}
