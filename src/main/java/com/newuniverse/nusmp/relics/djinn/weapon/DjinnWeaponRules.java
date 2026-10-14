package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The twelve Djinn weapons as numbers (pure, so DjinnWeaponRulesTest can check every row). A weapon is the temporary item a Half or Full Djinn Equip
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
        row(Djinn.BAAL, "lance", 9.0, 1.4, 1.0, new Act("thunder_throw", 0.030, 120, 9, 20), new Act("judgement_step", 0.050, 260, 11, 8));
        row(Djinn.AMON, "greatsword", 12.0, 1.0, 0.5, new Act("sun_cleave", 0.030, 110, 11, 6), new Act("ember_wall", 0.050, 280, 8, 7));
        row(Djinn.VINEA, "trident", 9.0, 1.5, 0.8, new Act("tide_spear", 0.025, 100, 8, 11), new Act("frost_surge", 0.050, 260, 9, 7));
        row(Djinn.FOCALOR, "glaive", 8.0, 1.6, 1.0, new Act("cyclone_sweep", 0.030, 110, 8, 4.5), new Act("updraft_leap", 0.040, 220, 6, 5));
        row(Djinn.ZAGAN, "staff", 6.0, 1.6, 0.5, new Act("bloom_heal", 0.040, 180, 6, 9), new Act("thorn_snare", 0.040, 240, 7, 12));
        row(Djinn.ZEPAR, "axe", 13.0, 0.9, 0.3, new Act("titan_slam", 0.040, 160, 12, 5.5), new Act("rock_breaker", 0.040, 240, 12, 4.5));
        row(Djinn.FURFUR, "horn", 7.0, 1.2, 0.3, new Act("resonance_wave", 0.030, 120, 7, 9), new Act("dissonance", 0.045, 260, 6, 6.5));
        row(Djinn.MARCHOSIAS, "claws", 7.5, 2.4, 0.0, new Act("ember_rush", 0.030, 120, 8, 8), new Act("brand", 0.035, 200, 5, 14));
        row(Djinn.SITRI, "bow", 6.0, 1.8, 0.5, new Act("dawn_arrow", 0.025, 60, 10, 32), new Act("radiant_burst", 0.050, 260, 8, 6.5));
        row(Djinn.BIFRON, "glaive", 9.0, 1.5, 1.0, new Act("chain_lightning", 0.030, 120, 8, 12), new Act("storm_pillars", 0.050, 260, 10, 14));
        row(Djinn.PAIMON, "glaive", 9.0, 1.5, 1.0, new Act("crescent_wind", 0.030, 110, 8, 10), new Act("heavenly_dash", 0.040, 220, 9, 9));
        row(Djinn.VASSAGO, "scythe", 10.0, 1.1, 0.8, new Act("abyssal_reap", 0.035, 140, 9, 5.5), new Act("drowning_pull", 0.045, 260, 6, 9));
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
