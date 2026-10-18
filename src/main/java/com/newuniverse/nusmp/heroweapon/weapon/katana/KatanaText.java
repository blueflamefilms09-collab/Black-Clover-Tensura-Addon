package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.n;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.pct;
import static com.newuniverse.nusmp.heroweapon.weapon.glassworld.Fmt.sec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the names and descriptions of the Katana's techniques and passives are read in two places that must agree, the skill objects (the tooltip and the Weapon
 * Book read them) and the language file ({@code hero.skill.<id>.name}, {@code .desc}, {@code hero.passive.katana.<id>} and {@code .desc}), so they are written
 * once, here, with no game imports, and the numbers in the sentences are the constants the skills use. A test holds them to the house rules and a small program
 * writes the language fragment from this table.
 */
public final class KatanaText {
    private KatanaText() {}

    /** {id, name, description} of every skill, in the order the weapon lists them. */
    private static final String[][] SKILLS = {
            {KatanaNumbers.CRESCENT_MOON, "Crescent Moon Sword",
                    "A long thin slash: a crescent of light cuts up to " + KatanaNumbers.CRESCENT_TARGETS + " foes in a line " + n(KatanaNumbers.CRESCENT_RANGE) + " blocks long for "
                            + n(KatanaNumbers.CRESCENT_RAW) + " damage, a little more with tempo. Counter: step out of the line."},
            {KatanaNumbers.INSTANT_BLADE, "Instant Blade: Mist",
                    "Iaido. After a breath (a glint at your hip) you dash up to " + n(KatanaNumbers.INSTANT_DASH) + " blocks and cut everything on the line for "
                            + n(KatanaNumbers.INSTANT_RAW) + " damage, a little more with tempo. Counter: leave the line during the breath."},
            {KatanaNumbers.COUNTER_STANCE, "Counter Stance",
                    "Raises a parry for " + sec(KatanaNumbers.PARRY_TICKS) + " seconds: the first blow from a living foe is stopped (at most " + pct(KatanaNumbers.PARRY_NEGATE_SHARE)
                            + " percent of your health) and answered with a cut for " + n(KatanaNumbers.COUNTER_MIN) + " to " + n(KatanaNumbers.COUNTER_MAX)
                            + " damage, and your tempo jumps to full. Counter: it stops one blow, so wait it out."},
            {KatanaNumbers.RELEASE_EVADE, "Release Evade",
                    "A flick of the sword and a step away from the nearest foe: for " + sec(KatanaNumbers.EVADE_TICKS) + " seconds the next blow from a living foe misses you, "
                            + "and your tempo rises. Counter: attacks that arrive after the step."},
            {KatanaNumbers.TRIPLE_THRUST, "Triple Thrust",
                    "After a short draw you step forward and thrust " + KatanaNumbers.THRUSTS + " times along a line " + n(KatanaNumbers.THRUST_LENGTH) + " blocks long, "
                            + n(KatanaNumbers.THRUST_RAW) + " damage each, a little more with tempo. Counter: sidestep during the draw."},
            {KatanaNumbers.STARDUST_BLADE, "Stardust Blade",
                    KatanaNumbers.STARDUST_CUTS + " quick cuts in a flurry on the foe on your crosshair within " + n(KatanaNumbers.STARDUST_RANGE) + " blocks, "
                            + n(KatanaNumbers.STARDUST_RAW) + " damage each (" + n(KatanaNumbers.STARDUST_RAW * KatanaNumbers.STARDUST_CUTS) + " in all), with stardust where the blade lands. Counter: get out of reach."},
            {KatanaNumbers.DOUBLE_SWORD, "Double Sword",
                    "A phantom second blade follows yours for " + sec(KatanaNumbers.DOUBLE_TICKS) + " seconds: your next " + KatanaNumbers.ECHO_MAX
                            + " melee hits are each doubled by a phantom cut for " + n(KatanaNumbers.ECHO_RAW) + " damage."},
            {KatanaNumbers.SOUL_SLICE, "Soul Slice",
                    "A cut that goes through the body to the spirit: " + n(KatanaNumbers.SLICE_RAW) + " damage and " + pct(KatanaNumbers.SLICE_DRAIN)
                            + " percent of the foe's magicules drained; you take back " + pct(KatanaNumbers.SLICE_TAKE_BACK) + " percent of it, at most "
                            + pct(KatanaNumbers.SLICE_GAIN_CAP) + " percent of your own. Foes without magicules lose nothing."},
            {KatanaNumbers.SAKURA_DANCE, "Kagura Dance of Sakura",
                    "Ultimate. Three formations in a row (blossom, beginning blossom and a final line), together " + n(KatanaNumbers.SAKURA_BASE) + " damage plus "
                            + n(KatanaNumbers.SAKURA_PER_TEMPO) + " for every level of tempo you hold (up to " + n(KatanaNumbers.sakuraTotal(KatanaNumbers.TEMPO_MAX))
                            + " in all); the tempo is spent. Counter: leave the circle, then the cone, then the line."},
    };

    /** {id, name, description} of every passive a form of the Katana may list. */
    private static final String[][] PASSIVES = {
            {KatanaNumbers.P_IAI_TEMPO, "Iaido Tempo",
                    "Using different katana techniques one after another, each within " + sec(KatanaNumbers.TEMPO_WINDOW) + " seconds of the last, builds tempo (up to "
                            + KatanaNumbers.TEMPO_MAX + "): every level adds damage to the cuts and thrusts and the dance of sakura grows with it."},
            {KatanaNumbers.P_KEEN_OPENING, "Keen Opening",
                    "The first full swing after " + sec(KatanaNumbers.OPENING_IDLE) + " seconds without a katana action strikes again for " + n(KatanaNumbers.OPENING_RAW) + " damage, as a draw cut should."},
            {KatanaNumbers.P_RIPOSTE, "Riposte",
                    "After a successful parry the next full swing within " + sec(KatanaNumbers.RIPOSTE_TICKS) + " seconds is followed by a riposte for " + n(KatanaNumbers.RIPOSTE_RAW) + " damage."},
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

    /** The language entries of the Katana: skill and passive names and descriptions, keyed as the lang file wants them. */
    public static Map<String, String> lang() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String[] r : SKILLS) {
            out.put("hero.skill." + r[0] + ".name", r[1]);
            out.put("hero.skill." + r[0] + ".desc", r[2]);
        }
        for (String[] r : PASSIVES) {
            out.put("hero.passive.katana." + r[0], r[1]);
            out.put("hero.passive.katana." + r[0] + ".desc", r[2]);
        }
        return out;
    }
}
