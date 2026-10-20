package com.newuniverse.nusmp.relics;

import java.util.Locale;
import java.util.Set;

/**
 * 1.104.0, the attune lock (docs/relic_attune_lock.md): a relic's full power belongs to the player it is attuned to. A relic that needs attunement
 * ({@link RelicDef#needsAttunement()}) and is held by someone it is NOT attuned to (never attuned, or attuned to another player) deals only a fraction
 * of its damage (default 20%). Pure: no Minecraft types, so the whole decision is unit-tested; {@link RelicPowerEvents} applies it on the server.
 */
public final class RelicPower {
    private RelicPower() {}

    /** The owner's number: an unattuned relic deals about a fifth of its power. */
    public static final double DEFAULT_UNATTUNED = 0.20;
    public static final double MIN_UNATTUNED = 0.05, MAX_UNATTUNED = 1.0;

    /**
     * The relic ids that are exempt from the lock. Empty on purpose: the only relics that keep full power unattuned are the ones that need no
     * attunement at all (everything below Legend, and the cards written without {@code .attune()}); Elsdocia is not a relic card (it has no attunement
     * ritual), so it is outside the rule by what it is, not by a list.
     */
    public static final Set<String> EXEMPT_IDS = Set.of();

    /** The two settings (config {@code relics.attuneLock} and {@code relics.unattunedPower}). */
    public record Config(boolean lock, double unattunedPower) {
        public static final Config DEFAULT = new Config(true, DEFAULT_UNATTUNED);

        public Config {
            unattunedPower = clampPower(unattunedPower);
        }
    }

    /** The unattuned fraction forced into 0.05 .. 1.0 (a NaN reads as the default). */
    public static double clampPower(double p) {
        if (Double.isNaN(p)) return DEFAULT_UNATTUNED;
        return Math.max(MIN_UNATTUNED, Math.min(MAX_UNATTUNED, p));
    }

    /**
     * The damage factor of a relic in the hands of a wielder: 1.0 when the relic needs no attunement, when it is attuned to the wielder (a mob that wears a
     * relic counts as attuned: the caller passes {@code true}), or when the lock is switched off; otherwise the unattuned power.
     */
    public static double factor(boolean attunedToWielder, boolean needsAttunement, Config config) {
        Config c = config == null ? Config.DEFAULT : config;
        if (!c.lock() || !needsAttunement || attunedToWielder) return 1.0;
        return c.unattunedPower();
    }

    /** The same with the id: an exempt id keeps full power (the list is empty, see {@link #EXEMPT_IDS}). */
    public static double factor(String relicId, boolean attunedToWielder, boolean needsAttunement, Config config) {
        if (relicId != null && EXEMPT_IDS.contains(relicId)) return 1.0;
        return factor(attunedToWielder, needsAttunement, config);
    }

    /** Is a hit with this factor cut down? */
    public static boolean nerfed(double factor) { return factor < 0.9999; }

    /** The factor as a whole percent for text ("20"). */
    public static int percent(double factor) { return (int) Math.round(Math.max(0.0, Math.min(1.0, factor)) * 100.0); }

    /** The damage a hit deals once the factor is applied (never below zero, never raised). */
    public static float scale(float amount, double factor) {
        if (!(amount > 0f) || !nerfed(factor)) return amount;
        return (float) (amount * Math.max(0.0, Math.min(1.0, factor)));
    }

    /**
     * Is this damage the wielder's own melee blow (so the weapon in the main hand is what struck)? {@code attackType} is whether the damage type is
     * the player attack or a mob attack, {@code directIsAttacker} that the entity that hit is the one the damage is credited to (a projectile or a
     * spell is not).
     */
    public static boolean isMeleeBlow(boolean attackType, boolean directIsAttacker) { return attackType && directIsAttacker; }

    /** The persistent-data key an arrow, bolt or thrown weapon carries when it left an unattuned relic (its factor). */
    public static final String PROJECTILE_TAG = "nusmp_relic_power";

    /** A readable factor for the command ("20%"). */
    public static String label(double factor) { return String.format(Locale.ROOT, "%d%%", percent(factor)); }
}
