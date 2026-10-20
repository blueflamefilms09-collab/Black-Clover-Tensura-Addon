package com.newuniverse.nusmp.mana.zone;

import java.util.Locale;
import java.util.Set;

/**
 * The numbers of the True Magic page (Shin Maho), pure and tested. An INTERPRETATION of the canon the owner gave: Mana Method is the Heart Kingdom's art of shaping
 * ambient mana into runes and magic circles that instruct the spell; with enough control (Stage 3 or higher) it makes TRUE MAGIC, the real physical element instead
 * of the imitation of it, fuelled mostly by the environment's dense natural mana rather than the caster's pool, hugely stronger, and devils are highly susceptible to it.
 * It is easiest for the natural attributes (fire, water, wind, earth, lightning, plant) and hard for abstract or unnatural ones, so it is only offered to those.
 */
public final class TrueMagicRules {
    private TrueMagicRules() {}

    /** The natural attributes (MagicType names) True Magic is easy for. */
    public static final Set<String> NATURAL = Set.of("FLAME", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT");

    /** The best Stage number that still counts as "Stage 3 or higher" control (stages run 9 lowest to 0 highest). */
    public static final int MAX_STAGE_NUMBER = 3;
    /** Raw damage before the balance law, the radius, the delay while the rune array draws itself, the cost (percent of the pool) and the cooldown (ticks). */
    public static final double RAW_DAMAGE = 40.0, RADIUS = 5.0, COST_PERCENT = 12.0;
    public static final int DELAY_TICKS = 24, COOLDOWN_TICKS = 600, REACH = 40, MAX_TARGETS = 12;

    public static boolean isNatural(String magicName) { return magicName != null && NATURAL.contains(magicName.toUpperCase(Locale.ROOT)); }

    /** Control: Stage 3 or higher (the number 3, 2, 1, 0), or an Arcane or Saint stage whatever its number. */
    public static boolean controlEnough(int stageNumber, boolean arcaneOrSaint) { return arcaneOrSaint || stageNumber <= MAX_STAGE_NUMBER; }

    /** The share of the spell's cost the environment's mana pays (refunded to the caster): 20% in ordinary mana, 40% at density 2, 60% at 3, never over 80%. */
    public static double fuelShare(double density) { return Math.max(0.0, Math.min(0.8, 0.2 + 0.2 * (density - 1.0))); }

    /** Power from the dense mana: up to +35% at density 3 ("hugely stronger" comes from the real element and the base damage; the environment adds to it). */
    public static double powerMult(double density) { return 1.0 + 0.35 * Math.max(0.0, Math.min(1.0, (density - 1.0) / 2.0)); }

    /** Devils are highly susceptible: x1.6, but only x1.3 for a boss. */
    public static double devilMult(boolean devil, boolean boss) { return !devil ? 1.0 : boss ? 1.3 : 1.6; }

    /** The raw damage of one strike on one target. */
    public static double damage(double density, boolean devil, boolean boss) { return RAW_DAMAGE * powerMult(density) * devilMult(devil, boss) * (boss ? 0.6 : 1.0); }

    /** True when an entity type path names a devil (the devils of the Devil Arc, Zagred's kind). */
    public static boolean devilPath(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(Locale.ROOT);
        return p.contains("devil") || p.contains("daemon") || p.contains("demon") || p.contains("zagred");
    }
}
