package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: Escanor's move numbers in one pure place, as {@link MoveSpec}s the framework's scheduler can read without Minecraft, so a test can play a
 * whole fight through the real scheduler (with the power held down by the sun, see {@link Sunshine#effectivePower}) and prove what he does at each
 * hour. {@code EscanorBoss} turns each row into a {@code Move} by adding its telegraph and its code, found by id.
 *
 * <p>The shape of the fight: Pride does not run and does not hide. He swings Rhitta (a wide arc, a ground-shaking smash, a spin), burns the air round
 * him (the flare), throws the axe's flame as waves, charges in a line, and from the middle of the morning lays heat hazes and columns of light on
 * the party. The minimum power of a row doubles as the Sunshine it needs: 20 is any hour, 40 dawn, 60 mid-morning, 80 the afternoon sun (Cruel
 * Sun), 100 only at noon with the Gates open (Zenith Cleave).
 */
public final class EscanorTable {
    private EscanorTable() {}

    public static final String SWING = "rhitta_swing", SMASH = "rhitta_smash", FLARE = "solar_flare", WAVE = "rhitta_wave", CHARGE = "pride_charge",
            HAZE = "heat_haze", PILLARS = "sunbeam_pillars", SPIN = "sun_spin", CRUEL_SUN = "cruel_sun", ZENITH = "zenith_cleave";

    /** Base damage of the moves (before the boss's damage law: power tuning, his own scale and the sun, the victim's EP, the balance law's caps). */
    public static final float SWING_DAMAGE = 17f, SMASH_DAMAGE = 21f, FLARE_DAMAGE = 11f, WAVE_DAMAGE = 11f, CHARGE_DAMAGE = 18f,
            CHARGE_RING_DAMAGE = 11f, HAZE_DAMAGE = 2.5f, PILLAR_DAMAGE = 15f, SPIN_DAMAGE = 9f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's {@code Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(SWING, "Rhitta Swing", 20, 16, 70, 0, 6.2, 6, false, false, 8),
            new Spec(SMASH, "Rhitta Smash", 20, 22, 150, 0, 9, 4, false, false, 12),
            new Spec(FLARE, "Flare", 20, 18, 180, 0, 8.5, 3, false, true, 10),
            new Spec(WAVE, "Rhitta Flame Wave", 20, 20, 130, 5, 32, 5, false, false, 8),
            new Spec(CHARGE, "Pride's Charge", 40, 18, 210, 8, 30, 3, false, false, 12),
            new Spec(HAZE, "Heat Haze", 40, 22, 300, 0, 40, 3, false, true, 10),
            new Spec(PILLARS, "Sunbeam Pillars", 60, 24, 230, 4, 36, 4, false, true, 12),
            new Spec(SPIN, "Sun Spin", 60, 22, 190, 0, 6.8, 4, false, true, 12),
            new Spec(CRUEL_SUN, "Cruel Sun", 80, 60, 1300, 0, 40, 5, true, true, 60),
            new Spec(ZENITH, "Zenith Cleave", 100, 62, 1400, 0, 40, 5, true, true, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index; only ever append). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
