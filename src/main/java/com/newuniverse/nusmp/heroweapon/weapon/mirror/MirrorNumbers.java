package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;

import java.util.List;
import java.util.Set;

/**
 * Why: every number the Vassal Mirror casts with (damage, cooldown, cost, reach, counts, timings) lives in this one pure table, so the balance budget of
 * design.md section 5 can be pinned by a test ({@code MirrorNumbersTest}) and a later edit that breaks the budget fails the build. The logic builds its skills
 * from the {@link Row}s below (so the table and the game cannot differ) and reads every other constant from here. The small pure rules that decide an outcome
 * (what a ray is worth with so much gleam, how big an absorption shield to give, how much of a spell is turned back) are methods here for the same reason.
 * Raw damage is before the level factor (1.0 to 2.2) and the balance law. Design, not canon: the figures are the author's.
 */
public final class MirrorNumbers {
    private MirrorNumbers() {}

    // ------------------------------------------------------------------ the budget rows (one per skill)
    public static final Row BASH = Row.damage("mirror.mirror_bash", Kind.ORDINARY, 6, 9, 6, 60, 0.01);
    public static final Row GLASS_SHIELD = Row.damage("mirror.glass_shield", Kind.UTILITY, 4, 4, 6, 120, 0.02);
    public static final Row PRISM_RAY = Row.damage("mirror.prism_ray", Kind.HEAVY, 13, 19.5, 6, 100, 0.02);
    public static final Row STARDUST = Row.utility("mirror.stardust_mirror", 160, 0.02).reaching(3);
    public static final Row REBOUND = Row.damage("mirror.release_rebound", Kind.HEAVY, 12, 20, 6, 200, 0.02);
    public static final Row MOVEMENT = Row.utility("mirror.movement_mirror", 120, 0.02);
    public static final Row CHAIN_BINDING = Row.damage("mirror.chain_binding", Kind.UTILITY, 4, 4, 4, 240, 0.02);
    public static final Row KALEIDOSCOPE = Row.damage("mirror.kaleidoscope", Kind.ULTIMATE, 5, 30, 8, 700, 0.06);
    public static final Row COVET = Row.utility("mirror.covet", 160, 0.02).reaching(1);
    public static final Row RESSENTIMENT = Row.utility("mirror.ressentiment", 280, 0.03).reaching(6);

    public static final List<Row> ALL = List.of(BASH, GLASS_SHIELD, PRISM_RAY, STARDUST, REBOUND, MOVEMENT, CHAIN_BINDING, KALEIDOSCOPE, COVET, RESSENTIMENT);

    /** The ids of the ultimates (one per form at most). */
    public static final Set<String> ULTIMATES = Set.of(KALEIDOSCOPE.id());

    // ------------------------------------------------------------------ Mirror Bash
    public static final double BASH_RANGE = 3.6, BASH_HALF_ANGLE = 55.0, BASH_KNOCK = 0.9;
    /** The stagger: Slowness IV for this many ticks (a soft slowing, not hard control, so the shared control lock is not used). */
    public static final int BASH_STAGGER_TICKS = 12;

    // ------------------------------------------------------------------ Glass Shield (panes)
    public static final int PANE_MAX = 2, PANE_LIFE = 120, PANE_CHARGES = 3;
    public static final double PANE_RADIUS = 1.7, PANE_THICK = 0.5, PANE_DISTANCE = 2.6, SHARD_RADIUS = 3.5;

    // ------------------------------------------------------------------ gleam (light stored by reflecting)
    public static final int GLEAM_MAX = 100, GLEAM_PER_CATCH = 12, GLEAM_PER_BASH_HIT = 4, GLEAM_PER_SHARD_HIT = 3, GLEAM_PER_BIND = 5, GLEAM_PER_PULSE_HIT = 2;
    /** Gleam fades one point every {@code GLEAM_DECAY_EVERY} ticks once {@code GLEAM_DECAY_AFTER} ticks have passed without a gain. */
    public static final int GLEAM_DECAY_AFTER = 200, GLEAM_DECAY_EVERY = 10;

    // ------------------------------------------------------------------ Prism Ray
    public static final double RAY_BASE = 7.0, RAY_GLEAM_BONUS = 6.0, RAY_RANGE = 22.0, RAY_WIDTH = 0.7, RAY_BOUNCE_RANGE = 14.0, RAY_BOUNCE_SCALE = 0.8;
    public static final int RAY_WINDUP = 7;

    // ------------------------------------------------------------------ Stardust Mirror
    public static final double STARDUST_RANGE = 20.0, STARDUST_GROUP_RADIUS = 4.0, STARDUST_CATCH_RADIUS = 2.4;
    public static final int STARDUST_TICKS = 160, STARDUST_CATCHES = 2;

    // ------------------------------------------------------------------ Release Rebound
    public static final int REBOUND_TICKS = 80, REBOUND_CLEANSE = 2, REBOUND_RETURN_TICKS = 120, REBOUND_RETURN_AMP = 1;
    /** Half of the spell damage taken is turned back, at most {@code REBOUND_BLOW_CAP} per blow and {@code REBOUND_BUDGET} in all for one cast. */
    public static final double REBOUND_SHARE = 0.5, REBOUND_BLOW_CAP = 12.0, REBOUND_BUDGET = 20.0, REBOUND_RETURN_RANGE = 14.0;

    // ------------------------------------------------------------------ Movement Mirror and the image
    public static final double MOVE_RANGE = 9.0, IMAGE_SOAK_SHARE = 0.35;
    public static final int IMAGE_LIFE = 240;

    // ------------------------------------------------------------------ passives
    /** The Spell Ward: a Mirror in hand cuts the magic damage the wielder takes by this share. */
    public static final double SPELL_WARD = 0.15;
    /**
     * The Afterimage: the image repeats the next Mirror Bash or Prism Ray at this share of its strength, once, {@code AFTERIMAGE_DELAY} ticks later. The delay is above ten
     * ticks on purpose: a creature that was just hurt ignores a second, smaller blow for ten ticks (the game's hurt-resistance window), so an earlier echo would do nothing.
     */
    public static final double AFTERIMAGE_SCALE = 0.5;
    public static final int AFTERIMAGE_DELAY = 12;

    // ------------------------------------------------------------------ Chain Binding Mirror
    public static final double CHAIN_BIND_RADIUS = 5.0;
    public static final int CHAIN_BIND_TICKS = 60, CHAIN_BIND_MAX = 4;

    // ------------------------------------------------------------------ Kaleidoscope
    public static final double KALEIDO_RADIUS = 6.0, KALEIDO_RANGE = 18.0, KALEIDO_RING = 5.0;
    /** Six pulses, twelve ticks apart: a pulse at least ten ticks after the last, or the target's hurt-resistance window would swallow it. */
    public static final int KALEIDO_PULSES = 6, KALEIDO_EVERY = 12, KALEIDO_TELL = 14;

    // ------------------------------------------------------------------ the Envy series
    public static final double COVET_RANGE = 16.0, COVET_EXTRA_COST = 0.02;
    public static final int COVET_MAX = 3, COVET_TICKS = 200, COVET_AMP_CAP = 2, COVET_CORRUPTION = 35;
    public static final double RESSENT_RADIUS = 9.0;
    public static final int RESSENT_TICKS = 80, RESSENT_EVERY = 10, RESSENT_MAX = 6, RESSENT_CORRUPTION = 40;

    // ------------------------------------------------------------------ small pure rules
    /** What a Prism Ray is worth: seven, plus up to six more as the gleam fills (0 to 100). */
    public static double rayDamage(int gleam) {
        int g = Math.max(0, Math.min(GLEAM_MAX, gleam));
        return RAY_BASE + RAY_GLEAM_BONUS * g / GLEAM_MAX;
    }

    /** The Absorption amplifier for a shield worth about a fifth of the target's health: level I (4 health) for most, level II (8) from 40 maximum health. */
    public static int absorptionAmp(double maxHealth) {
        return maxHealth >= 40.0 ? 1 : 0;
    }

    /** The share of a spell the Spell Ward lets through. */
    public static float wardCut(float amount) {
        return (float) (amount * (1.0 - SPELL_WARD));
    }

    /** How much of a blow the image soaks: the whole blow up to 35 percent of the wielder's maximum health, the rest passes through. */
    public static float imageSoak(float amount, float maxHealth) {
        return Math.max(0f, Math.min(amount, (float) (maxHealth * IMAGE_SOAK_SHARE)));
    }

    /** What a rebound turns back of a spell of {@code amount}: half of it, but not above the blow cap or what is left of the budget (never negative). */
    public static float reboundBack(float amount, double budgetLeft) {
        return (float) Math.max(0.0, Math.min(Math.min(amount * REBOUND_SHARE, REBOUND_BLOW_CAP), budgetLeft));
    }

    /**
     * Whether a harmful effect may be sent back to a foe: only the plain damaging or weakening ones of the base game, and Slowness no stronger than level II.
     * Hard-control effects (paralysis, petrification, stun and the like of other mods) are cleansed from the wielder but never sent on, so the shared control rule holds.
     */
    public static boolean returnable(String effectPath, int amplifier) {
        return switch (effectPath) {
            case "poison", "wither", "weakness", "hunger", "mining_fatigue", "unluck" -> true;
            case "slowness" -> amplifier <= 1;
            default -> false;
        };
    }
}
