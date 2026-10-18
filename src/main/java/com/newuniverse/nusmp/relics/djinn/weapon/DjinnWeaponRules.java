package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Djinn weapons as numbers (pure, so DjinnWeaponRulesTest can check every row): the twelve of 1.34.0 (buffed in 1.93.0) and the twenty-four added then. A weapon is the temporary item a Half or Full Djinn Equip
 * puts in the bearer's hand: melee stats, two acts (right-click, sneak + right-click) and its engravings. The Half Equip opens the first act; the Full
 * Equip (Maso) opens the second and makes both stronger. A dark Djinn's weapon hits harder and drinks more Magoi.
 *
 * <p>All costs are shares of the maximum Magoi (Tensura magicules), multiplied by the same drain factor the Djinn phases pay; every power is a raw
 * damage that goes through the balance law and the 45% player cap ({@code Fx.damageHeavy}); every cooldown is in ticks.
 */
public final class DjinnWeaponRules {
    private DjinnWeaponRules() {}

    public static final int ACT_ONE = 0, ACT_TWO = 1;
    public static final int HALF = 2, FULL = 3;

    /** Stronger numbers in a Full Equip (power x1.35, cooldown x0.8) and for a dark Djinn (power x1.2, Magoi x1.25). */
    public static final double FULL_POWER = 1.35, DARK_POWER = 1.20, FULL_COOLDOWN = 0.80, DARK_COST = 1.25;
    /** A right-click is answered at most this often (ticks): holding the button never repeats an act. */
    public static final int USE_GUARD = 8;
    /** No act hits more foes than this (the relics' own cap is 24). */
    public static final int MAX_TARGETS = 12;
    /** The shortest time a pending strike (Storm Pillars) waits, and the most it may wait, ticks. */
    public static final int STRIKE_DELAY = 24;

    /** One act: its lang key suffix, the Magoi share, the cooldown, the raw damage (or heal) and the reach in blocks. */
    public record Act(String key, double cost, int cooldown, double power, double range) {}

    /** One weapon: the class of weapon (for the lang and the audit), melee damage and total attack speed, extra reach, and the two acts. */
    public record Spec(Djinn djinn, String kind, double damage, double speed, double reach, Act one, Act two) {
        public Act act(int which) { return which == ACT_TWO ? two : one; }
    }

    private static final Map<Djinn, Spec> TABLE = new EnumMap<>(Djinn.class);

    private static void row(Djinn d, String kind, double damage, double speed, double reach, Act one, Act two) { TABLE.put(d, new Spec(d, kind, damage, speed, reach, one, two)); }

    static {
        // 1.93.0 buff of the twelve: melee damage +1, act power x1.15, cooldown x0.9, reach x1.1 (costs, speeds and the Full / dark multipliers are unchanged)
        row(Djinn.BAAL, "lance", 10.0, 1.4, 1.0, new Act("thunder_throw", 0.030, 108, 10.5, 22), new Act("judgement_step", 0.050, 234, 12.5, 9));
        row(Djinn.AMON, "greatsword", 13.0, 1.0, 0.5, new Act("sun_cleave", 0.030, 100, 12.5, 6.5), new Act("ember_wall", 0.050, 250, 9, 7.5));
        row(Djinn.VINEA, "trident", 10.0, 1.5, 0.8, new Act("tide_spear", 0.025, 90, 9, 12), new Act("frost_surge", 0.050, 234, 10.5, 7.5));
        row(Djinn.FOCALOR, "glaive", 9.0, 1.6, 1.0, new Act("cyclone_sweep", 0.030, 100, 9, 5), new Act("updraft_leap", 0.040, 200, 7, 5.5));
        row(Djinn.ZAGAN, "staff", 7.0, 1.6, 0.5, new Act("bloom_heal", 0.040, 162, 7, 10), new Act("thorn_snare", 0.040, 216, 8, 13));
        row(Djinn.ZEPAR, "axe", 14.0, 0.9, 0.3, new Act("titan_slam", 0.040, 144, 13.5, 6), new Act("rock_breaker", 0.040, 216, 13.5, 5));
        row(Djinn.FURFUR, "horn", 8.0, 1.2, 0.3, new Act("resonance_wave", 0.030, 108, 8, 10), new Act("dissonance", 0.045, 234, 7, 7));
        row(Djinn.MARCHOSIAS, "claws", 8.5, 2.4, 0.0, new Act("ember_rush", 0.030, 108, 9, 9), new Act("brand", 0.035, 180, 6, 15));
        row(Djinn.SITRI, "bow", 7.0, 1.8, 0.5, new Act("dawn_arrow", 0.025, 54, 11.5, 35), new Act("radiant_burst", 0.050, 234, 9, 7));
        row(Djinn.BIFRON, "glaive", 10.0, 1.5, 1.0, new Act("chain_lightning", 0.030, 108, 9, 13), new Act("storm_pillars", 0.050, 234, 11.5, 15));
        row(Djinn.PAIMON, "glaive", 10.0, 1.5, 1.0, new Act("crescent_wind", 0.030, 100, 9, 11), new Act("heavenly_dash", 0.040, 200, 10.5, 10));
        row(Djinn.VASSAGO, "scythe", 11.0, 1.1, 0.8, new Act("abyssal_reap", 0.035, 126, 10.5, 6), new Act("drowning_pull", 0.045, 234, 7, 10));

        // 1.93.0: the twenty-four weapons of the new Djinn, appended in the roster order. Their acts are ActLibrary's.
        row(Djinn.BAEL, "claymore", 12.0, 1.0, 0.5, new Act("royal_decree", 0.030, 110, 11, 6.5), new Act("throne_quake", 0.050, 270, 10, 6.5));
        row(Djinn.AGARES, "harpoon", 9.5, 1.3, 1.0, new Act("tidal_lunge", 0.030, 120, 9.5, 8), new Act("undertow", 0.045, 260, 7.5, 8));
        row(Djinn.VALEFOR, "daggers", 7.0, 2.2, 0.0, new Act("spark_snatch", 0.028, 90, 8, 12), new Act("storm_heist", 0.040, 220, 9, 10));
        row(Djinn.BARBATOS, "bow", 6.5, 1.7, 0.5, new Act("wild_arrow", 0.025, 60, 10, 30), new Act("green_ambush", 0.045, 250, 8, 14));
        row(Djinn.GUSION, "rapier", 8.0, 1.7, 0.8, new Act("mirror_thrust", 0.028, 90, 9, 10), new Act("tide_mirror", 0.040, 240, 7, 6));
        row(Djinn.VEPAR, "anchor", 12.0, 0.9, 0.3, new Act("anchor_drop", 0.035, 130, 11, 5), new Act("shipwreck", 0.045, 260, 8, 8));
        row(Djinn.SHAX, "fans", 6.5, 2.0, 0.0, new Act("wing_hush", 0.028, 90, 7.5, 7), new Act("muted_flurry", 0.045, 250, 7, 6));
        row(Djinn.BUER, "staff", 6.0, 1.5, 0.5, new Act("wheel_mend", 0.040, 170, 7, 9), new Act("cleansing_spokes", 0.045, 260, 8, 11));
        row(Djinn.ANDREALPHUS, "lance", 8.5, 1.4, 1.0, new Act("star_spear", 0.028, 70, 9.5, 26), new Act("peacock_fan", 0.045, 250, 8.5, 8));
        row(Djinn.KIMARIS, "lance", 10.5, 1.2, 1.2, new Act("charge_lance", 0.035, 130, 11, 10), new Act("dark_gallop", 0.048, 250, 12, 13));
        row(Djinn.AMDUSIAS, "rapier", 7.5, 1.5, 0.6, new Act("horn_note", 0.028, 80, 8, 16), new Act("grand_chord", 0.045, 260, 8, 7.5));
        row(Djinn.BELIAL, "greatsword", 12.5, 0.95, 0.4, new Act("hollow_cleave", 0.032, 120, 11.5, 6.5), new Act("throne_ash", 0.048, 270, 9, 6.5));
        row(Djinn.DECARABIA, "pincers", 7.5, 1.6, 0.2, new Act("pincer_snap", 0.030, 100, 10, 5), new Act("star_shell", 0.040, 240, 6, 6));
        row(Djinn.SEERE, "sabre", 7.0, 2.0, 0.4, new Act("swift_slash", 0.026, 70, 8, 7), new Act("gale_step", 0.040, 200, 8.5, 12));
        row(Djinn.DANTALION, "staff", 6.0, 1.6, 0.5, new Act("forbidden_page", 0.028, 90, 8, 14), new Act("many_faces", 0.045, 260, 8, 14));
        row(Djinn.ANDROMALIUS, "spear", 9.0, 1.5, 1.0, new Act("seeker_bolt", 0.028, 80, 9, 24), new Act("unveil", 0.042, 240, 7.5, 9));
        row(Djinn.GLASYA_LABOLAS, "claws", 8.0, 2.2, 0.0, new Act("savage_rend", 0.028, 80, 9, 4.5), new Act("bloodhound_rush", 0.042, 220, 10, 9));
        row(Djinn.HAAGENTI, "cleaver", 11.5, 1.1, 0.5, new Act("bull_gore", 0.034, 120, 11, 7), new Act("great_work", 0.045, 260, 8, 6.5));
        row(Djinn.CROCELL, "staff", 6.5, 1.5, 0.5, new Act("frost_bolt", 0.026, 70, 8, 20), new Act("rime_field", 0.045, 260, 7.5, 7.5));
        row(Djinn.FURCAS, "halberd", 10.0, 1.2, 1.0, new Act("ice_wind_sweep", 0.032, 120, 9.5, 6.5), new Act("knights_gale", 0.045, 240, 10, 9));
        row(Djinn.BALAM, "trident", 9.0, 1.4, 0.8, new Act("triple_flame", 0.030, 100, 9, 11), new Act("prophecy", 0.048, 260, 10, 14));
        row(Djinn.ALLOCES, "longsword", 10.0, 1.3, 0.6, new Act("lion_slash", 0.030, 100, 10.5, 6), new Act("lions_roar", 0.045, 250, 7, 8.5));
        row(Djinn.CAMIO, "staff", 6.5, 1.7, 0.5, new Act("crow_flock", 0.028, 80, 7.5, 12), new Act("murder_of_crows", 0.045, 250, 8.5, 14));
        row(Djinn.MURMUR, "mace", 11.0, 1.1, 0.3, new Act("death_drum", 0.034, 120, 9.5, 5.5), new Act("funeral_march", 0.047, 270, 8.5, 9));
    }

    public static Spec spec(Djinn d) { return TABLE.get(d); }

    // ---------------------------------------------------------------- the numbers of a use
    /** Is this act open in this phase? The first from the Half Equip, the second from the Full Equip. */
    public static boolean open(int act, int phase) { return act == ACT_ONE ? phase >= HALF : phase >= FULL; }

    /** The raw damage (or heal) of an act in a phase, for a light or dark Djinn. */
    public static double power(Spec s, int act, int phase, boolean dark) {
        double p = s.act(act).power();
        if (phase >= FULL) p *= FULL_POWER;
        if (dark) p *= DARK_POWER;
        return p;
    }

    /** The Magoi share an act costs before the drain factor. */
    public static double cost(Spec s, int act, boolean dark) { return s.act(act).cost() * (dark ? DARK_COST : 1.0); }

    /** The cooldown of an act in ticks (never less than 20). */
    public static int cooldown(Spec s, int act, int phase) {
        double c = s.act(act).cooldown() * (phase >= FULL ? FULL_COOLDOWN : 1.0);
        return Math.max(20, (int) Math.round(c));
    }

    // ---------------------------------------------------------------- the item ids
    public static final String PREFIX = "djinn_weapon_", DARK_SUFFIX = "_dark";

    /** The registry path of a Djinn's weapon: {@code djinn_weapon_baal}, or {@code djinn_weapon_baal_dark}. */
    public static String itemPath(Djinn d, boolean dark) { return PREFIX + d.id() + (dark ? DARK_SUFFIX : ""); }

    /** The Djinn a weapon path belongs to, or null. */
    public static Djinn djinnOfPath(String path) {
        if (path == null || !path.startsWith(PREFIX)) return null;
        String rest = path.substring(PREFIX.length());
        if (rest.endsWith(DARK_SUFFIX)) rest = rest.substring(0, rest.length() - DARK_SUFFIX.length());
        return Djinn.byName(rest);
    }

    public static boolean isDarkPath(String path) { return path != null && path.startsWith(PREFIX) && path.endsWith(DARK_SUFFIX) && djinnOfPath(path) != null; }

    // ---------------------------------------------------------------- the engravings
    /** One Tensura engraving (an enchantment of the {@code #tensura:engraving} tag) and its level. */
    public record Engraving(String id, int level) {}

    private static final Map<Djinn, List<Engraving>> ENGRAVINGS = new EnumMap<>(Djinn.class);

    private static void eng(Djinn d, Engraving... e) { ENGRAVINGS.put(d, List.of(e)); }

    private static Engraving e(String id, int level) { return new Engraving(id, level); }

    static {
        eng(Djinn.BAAL, e("elemental_boost", 2), e("barrier_piercing", 1), e("swift", 1));                  // a lance of thunder that goes through wards
        eng(Djinn.AMON, e("elemental_boost", 2), e("severance", 2), e("sturdy", 1));                        // a sun-forged cleaver
        eng(Djinn.VINEA, e("elemental_boost", 2), e("magic_weapon", 1), e("magic_protection", 1));          // the queen's trident, water that wards
        eng(Djinn.FOCALOR, e("swift", 2), e("elemental_boost", 1), e("magic_weapon", 1));                   // a gale that is always a beat ahead
        eng(Djinn.ZAGAN, e("vitality", 2), e("restoration", 1), e("elemental_boost", 1));                   // life and mending
        eng(Djinn.ZEPAR, e("crushing", 3), e("sturdy", 2), e("severance", 1));                              // breaker of giants
        eng(Djinn.FURFUR, e("magic_interference", 2), e("elemental_boost", 1), e("enervation", 1));         // sound that unravels spells and strength
        eng(Djinn.MARCHOSIAS, e("swift", 2), e("energy_steal", 1), e("severance", 1));                      // the hunter's claws
        eng(Djinn.SITRI, e("holy_weapon", 2), e("elemental_boost", 2), e("barrier_piercing", 1));           // dawn light
        eng(Djinn.BIFRON, e("elemental_boost", 2), e("barrier_piercing", 1), e("swift", 1));                // storm fang
        eng(Djinn.PAIMON, e("swift", 2), e("severance", 2), e("elemental_boost", 1));                       // wind-thin cuts
        eng(Djinn.VASSAGO, e("soul_eater", 1), e("energy_steal", 1), e("enervation", 2), e("severance", 1)); // the abyss takes what it cuts

        // 1.93.0: the new Djinn (Tensura's engravings, CLAUDE.md rule 6)
        eng(Djinn.BAEL, e("crushing", 2), e("severance", 2), e("sturdy", 2), e("vitality", 1));              // a king's claymore: it cuts, it crushes, it does not break
        eng(Djinn.AGARES, e("elemental_boost", 2), e("swift", 1), e("magic_weapon", 1));                     // a harpoon of the tide
        eng(Djinn.VALEFOR, e("swift", 2), e("energy_steal", 1), e("elemental_boost", 1), e("barrier_piercing", 1)); // a thief's daggers take what they touch
        eng(Djinn.BARBATOS, e("swift", 1), e("elemental_boost", 2), e("vitality", 1), e("magic_weapon", 1)); // the hunter's bow of green wood
        eng(Djinn.GUSION, e("elemental_boost", 2), e("magic_protection", 2), e("swift", 1));                 // mirrored water that wards
        eng(Djinn.VEPAR, e("crushing", 2), e("sturdy", 2), e("elemental_boost", 1), e("severance", 1));      // an anchor that wrecks
        eng(Djinn.SHAX, e("magic_interference", 2), e("swift", 2), e("enervation", 1));                     // the dove's silence unravels spells
        eng(Djinn.BUER, e("restoration", 2), e("vitality", 2), e("elemental_boost", 1));                    // the healer's wheel
        eng(Djinn.ANDREALPHUS, e("holy_weapon", 1), e("elemental_boost", 2), e("barrier_piercing", 1), e("magic_weapon", 1)); // starlight through wards
        eng(Djinn.KIMARIS, e("crushing", 2), e("swift", 2), e("severance", 1), e("sturdy", 1));              // a cavalry lance
        eng(Djinn.AMDUSIAS, e("magic_interference", 1), e("elemental_boost", 2), e("swift", 1), e("enervation", 1)); // the unicorn's note
        eng(Djinn.BELIAL, e("severance", 2), e("elemental_boost", 2), e("soul_eater", 1), e("enervation", 1)); // a hollow throne takes what sits on it
        eng(Djinn.DECARABIA, e("sturdy", 2), e("holy_coat", 1), e("elemental_boost", 2), e("magic_protection", 1)); // a shell of stars
        eng(Djinn.SEERE, e("swift", 3), e("elemental_boost", 1), e("severance", 1));                        // always a beat ahead
        eng(Djinn.DANTALION, e("magic_interference", 2), e("magic_weapon", 1), e("elemental_boost", 1), e("magic_capacity", 1)); // the scholar's staff
        eng(Djinn.ANDROMALIUS, e("barrier_piercing", 2), e("elemental_boost", 2), e("swift", 1), e("magic_interference", 1)); // it finds what is warded
        eng(Djinn.GLASYA_LABOLAS, e("severance", 2), e("energy_steal", 1), e("swift", 2), e("crushing", 1)); // the hound's fangs
        eng(Djinn.HAAGENTI, e("crushing", 2), e("sturdy", 1), e("vitality", 1), e("elemental_boost", 1));    // the alchemist's cleaver
        eng(Djinn.CROCELL, e("elemental_boost", 2), e("lethargy", 1), e("magic_weapon", 1), e("magic_protection", 1)); // frost that slows
        eng(Djinn.FURCAS, e("elemental_boost", 2), e("stagnation", 1), e("sturdy", 1), e("swift", 1));       // an old knight's halberd on an ice wind
        eng(Djinn.BALAM, e("elemental_boost", 2), e("severance", 1), e("swift", 1), e("magic_weapon", 1));   // three flames
        eng(Djinn.ALLOCES, e("severance", 2), e("elemental_boost", 2), e("sturdy", 1), e("swift", 1));       // the lion-eyed knight's sword
        eng(Djinn.CAMIO, e("magic_interference", 1), e("swift", 2), e("elemental_boost", 1), e("soul_eater", 1)); // crows feed on the dead
        eng(Djinn.MURMUR, e("soul_eater", 1), e("enervation", 2), e("magic_interference", 1), e("crushing", 1)); // the drum that ends things
    }

    /** The engravings of a Djinn's weapon: its own, and for the dark one a Ruination on top. */
    public static List<Engraving> engravings(Djinn d, boolean dark) {
        List<Engraving> base = ENGRAVINGS.get(d);
        if (!dark) return base;
        java.util.ArrayList<Engraving> out = new java.util.ArrayList<>(base);
        out.add(new Engraving("ruination", 1));
        return List.copyOf(out);
    }
}
