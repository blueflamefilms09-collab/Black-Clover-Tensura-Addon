package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmRules;

/**
 * Spirit Dive remake: what THIS client chooses to draw (the values come from the CLIENT config {@code nusmp-spiritdive-client.toml}, see {@link DiveClientConfig}; plain flags so the painters read them
 * with no config lookup per frame). Only looks: nothing here changes what the server decides.
 * <ul>
 *   <li>{@code classic}: the 1.99 Spirit Dive look (armour-like pieces, the old effects) instead of the remade look. Default false.</li>
 *   <li>{@code characterEffects}: draw the character tier (Yuno, Fuegoleon, Noelle) as such; false draws every dive as the standard tier. Default true.</li>
 *   <li>{@code particles}: the ember / mote particle trail of a dive. Default true.</li>
 *   <li>{@code armView}: Fuegoleon's flaming arm: OFF, FLARE (only while he casts, dives or has the Salamander out) or ALWAYS (the default: also at rest, quieter).</li>
 * </ul>
 */
public final class SpiritDiveLook {
    private SpiritDiveLook() {}

    private static volatile boolean classic, characterEffects = true, particles = true;
    private static volatile FuegoleonArmRules.View armView = FuegoleonArmRules.View.ALWAYS;

    public static boolean classic() { return classic; }

    public static boolean characterEffects() { return characterEffects; }

    public static boolean particles() { return particles; }

    public static FuegoleonArmRules.View armView() { return armView; }

    public static void set(boolean classicLook, boolean characterFx, boolean particleTrail, FuegoleonArmRules.View arm) {
        classic = classicLook;
        characterEffects = characterFx;
        particles = particleTrail;
        armView = arm == null ? FuegoleonArmRules.View.ALWAYS : arm;
    }
}
