package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Spirit Dive remake: the CLIENT config {@code nusmp-spiritdive-client.toml} (it only changes what THIS client draws; the dedicated server never registers it). The values are mirrored into the plain
 * flags of {@link SpiritDiveLook} when the file loads or reloads, so the painters read a field, not a config, every frame.
 * <ul>
 *   <li>{@code spiritDive.classicLook} (default false): draw the 1.99 Spirit Dive instead of the remade look.</li>
 *   <li>{@code spiritDive.characterEffects} (default true): draw the character tier of Yuno, Fuegoleon and Noelle; false draws every dive as the standard tier (cheaper).</li>
 *   <li>{@code spiritDive.particles} (default true): the ember and mote particle trail of a dive.</li>
 *   <li>{@code fuegoleonArm.view} (default ALWAYS): OFF, FLARE (only while casting, diving or with the Salamander out) or ALWAYS (also at rest).</li>
 * </ul>
 */
public final class DiveClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC, CHARACTER_FX, PARTICLES;
    private static final ModConfigSpec.EnumValue<FuegoleonArmRules.View> ARM_VIEW;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spiritDive");
        CLASSIC = b.comment("true: the Spirit Dive is drawn as in 1.99 (armour-like pieces) instead of the remade look.").define("classicLook", false);
        CHARACTER_FX = b.comment("The character tier of the dive (Yuno's, Fuegoleon's and Noelle's own look). false = every dive is drawn as the standard tier.").define("characterEffects", true);
        PARTICLES = b.comment("The ember / mote particle trail of a Spirit Dive.").define("particles", true);
        b.pop();
        b.push("fuegoleonArm");
        ARM_VIEW = b.comment("Fuegoleon's flaming right arm: OFF, FLARE (only while he casts, dives or has the Salamander out) or ALWAYS (also at rest, quieter).").defineEnum("view", FuegoleonArmRules.View.ALWAYS);
        b.pop();
        SPEC = b.build();
    }

    private DiveClientConfig() {}

    /** Mirrors the file into {@link SpiritDiveLook} (the defaults stand in until the config is loaded). */
    public static void apply() {
        try {
            SpiritDiveLook.set(CLASSIC.get(), CHARACTER_FX.get(), PARTICLES.get(), ARM_VIEW.get());
        } catch (IllegalStateException notLoaded) {
            SpiritDiveLook.set(false, true, true, FuegoleonArmRules.View.ALWAYS);
        }
    }
}
