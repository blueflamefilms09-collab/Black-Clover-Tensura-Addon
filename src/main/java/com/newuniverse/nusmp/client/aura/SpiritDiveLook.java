package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;

/**
 * Spirit Dive remake: what THIS client chooses to draw (the values come from the CLIENT config {@code nusmp-spiritdive-client.toml}, see {@link DiveClientConfig}; plain flags so the painters read them
 * with no config lookup per frame). Only looks: nothing here changes what the server decides.
 * <ul>
 *   <li>{@code look}: which Spirit Dive look this client draws: {@code DJINN} (the default: a full set of plates in the style of the Djinn Full Equip), {@code V2} (the remade look of 3.0.0: wind halo, wing blades,
 *       flame mane, water ribbons, rock crown) or {@code CLASSIC} (the 1.99 look). The old option {@code classic} is the same as look CLASSIC ({@link #classic}).</li>
 *   <li>{@code classic}: the 1.99 Spirit Dive look (armour-like pieces, the old effects). True exactly when the look is CLASSIC; the old {@code classicLook} config key still switches it on.</li>
 *   <li>{@code characterEffects}: draw the character tier (Yuno, Fuegoleon, Noelle) as such; false draws every dive as the standard tier. Default true.</li>
 *   <li>{@code particles}: the ember / mote particle trail of a dive. Default true.</li>
 *   <li>{@code armView}: Fuegoleon's flaming arm: OFF, FLARE (only while he casts, dives or has the Salamander out) or ALWAYS (the default: also at rest, quieter).</li>
 * </ul>
 */
public final class SpiritDiveLook {
    private SpiritDiveLook() {}

    private static volatile boolean characterEffects = true, particles = true;
    private static volatile Look look = Look.DJINN;
    private static volatile FuegoleonArmRules.View armView = FuegoleonArmRules.View.ALWAYS;

    /** The look this client draws. */
    public static Look look() { return look; }

    /** True for the classic (1.99) look. */
    public static boolean classic() { return look == Look.CLASSIC; }

    /** True for the remade look of 3.0.0 (the one before the Djinn style). */
    public static boolean v2() { return look == Look.V2; }

    public static boolean characterEffects() { return characterEffects; }

    public static boolean particles() { return particles; }

    public static FuegoleonArmRules.View armView() { return armView; }

    /**
     * The old four-argument form (kept: the preview helpers and the 3.0.0 tests call it): {@code classicLook} true is CLASSIC, false is the remade look V2 (what false meant before the Djinn look existed).
     */
    public static void set(boolean classicLook, boolean characterFx, boolean particleTrail, FuegoleonArmRules.View arm) {
        set(classicLook ? Look.CLASSIC : Look.V2, characterFx, particleTrail, arm);
    }

    public static void set(Look which, boolean characterFx, boolean particleTrail, FuegoleonArmRules.View arm) {
        look = which == null ? Look.DJINN : which;
        characterEffects = characterFx;
        particles = particleTrail;
        armView = arm == null ? FuegoleonArmRules.View.ALWAYS : arm;
    }
}
