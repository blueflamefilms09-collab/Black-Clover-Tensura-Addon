package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.world.item.Rarity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Why: every Djinn Seal and Djinn weapon was EPIC, so a nameless minor Djinn read the same as Baal. The rarity now follows the Djinn: the Djinn of the main cast of Magi are EPIC, the rest are RARE when
 * their Extreme Magic hits as hard as the median of the roster or harder and UNCOMMON when it hits softer. Pure and bounded (one pass over the 36 rows, cached); it is read when the item is registered (item rarity is a registered default in 1.21), so it has no switch.
 */
public final class DjinnRarity {
    private DjinnRarity() {}

    /** The Djinn of the main cast (the Dungeon Capturers and their rivals); never lowered by the damage rule. */
    static final Set<Djinn> MAIN_CAST = Set.of(Djinn.BAAL, Djinn.AMON, Djinn.VINEA, Djinn.FOCALOR, Djinn.ZAGAN, Djinn.ZEPAR, Djinn.FURFUR, Djinn.PAIMON, Djinn.AGARES, Djinn.BELIAL);

    private static volatile Map<Djinn, Rarity> table;

    public static Rarity of(Djinn d) {
        Map<Djinn, Rarity> t = table;
        if (t == null) table = t = build();
        Rarity r = t.get(d);
        return r == null ? Rarity.EPIC : r;
    }

    /** The whole table from the Extreme damages (pure: used by the test, too). */
    static Map<Djinn, Rarity> build() {
        double[] dmg = new double[Djinn.values().length];
        for (Djinn d : Djinn.values()) dmg[d.ordinal()] = d.extreme().damage();
        double[] sorted = dmg.clone();
        java.util.Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];
        Map<Djinn, Rarity> m = new EnumMap<>(Djinn.class);
        for (Djinn d : Djinn.values()) m.put(d, rarity(MAIN_CAST.contains(d), dmg[d.ordinal()], median));
        return m;
    }

    static Rarity rarity(boolean mainCast, double extremeDamage, double median) {
        if (mainCast) return Rarity.EPIC;
        return extremeDamage >= median ? Rarity.RARE : Rarity.UNCOMMON;
    }
}
