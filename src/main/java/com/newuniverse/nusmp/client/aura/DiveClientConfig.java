package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
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
 *   <li>{@code fuegoleonArm.look} (default DJINN, 3.1): DJINN (the Djinn-Equip flame limb), V3 (the 3.0.0 arm) or CLASSIC (the plain molten model).</li>
 * </ul>
 */
public final class DiveClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC, CHARACTER_FX, PARTICLES;
    private static final ModConfigSpec.EnumValue<FuegoleonArmRules.View> ARM_VIEW;
    private static final ModConfigSpec.EnumValue<FuegoleonArmDjinnRules.Look> ARM_LOOK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spiritDive");
        CLASSIC = b.comment("true: the Spirit Dive is drawn as in 1.99 (armour-like pieces) instead of the remade look.").define("classicLook", false);
        CHARACTER_FX = b.comment("The character tier of the dive (Yuno's, Fuegoleon's and Noelle's own look). false = every dive is drawn as the standard tier.").define("characterEffects", true);
        PARTICLES = b.comment("The ember / mote particle trail of a Spirit Dive.").define("particles", true);
        b.pop();
        b.push("fuegoleonArm");
        ARM_VIEW = b.comment("Fuegoleon's flaming right arm: OFF, FLARE (only while he casts, dives or has the Salamander out) or ALWAYS (also at rest, quieter).").defineEnum("view", FuegoleonArmRules.View.ALWAYS);
        ARM_LOOK = b.comment("Fuegoleon's flame arm, which look: DJINN (default: a sculpted limb of flame forged in piece by piece, as the Djinn Equip is), V3 (the 3.0.0 arm: molten model, flame sheets and a mane of strands) or CLASSIC (the plain molten model of 3.0.0 without sheets or mane).").defineEnum("look", FuegoleonArmDjinnRules.Look.DJINN);
        b.pop();
        SPEC = b.build();
    }

    private DiveClientConfig() {}

    /** Mirrors the file into {@link SpiritDiveLook} (the defaults stand in until the config is loaded). */
    public static void apply() {
        try {
            SpiritDiveLook.set(CLASSIC.get(), CHARACTER_FX.get(), PARTICLES.get(), ARM_VIEW.get());
            FuegoleonArmLook.set(ARM_LOOK.get());
        } catch (IllegalStateException notLoaded) {
            SpiritDiveLook.set(false, true, true, FuegoleonArmRules.View.ALWAYS);
            FuegoleonArmLook.set(FuegoleonArmDjinnRules.DEFAULT_LOOK);
        }
    }
}
