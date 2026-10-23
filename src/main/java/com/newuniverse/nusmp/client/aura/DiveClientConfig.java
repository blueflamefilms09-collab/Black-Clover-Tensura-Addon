package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Spirit Dive remake: the CLIENT config {@code nusmp-spiritdive-client.toml} (it only changes what THIS client draws; the dedicated server never registers it). The values are mirrored into the plain
 * flags of {@link SpiritDiveLook} when the file loads or reloads, so the painters read a field, not a config, every frame.
 * <ul>
 *   <li>{@code spiritDive.look} (default DJINN): which Spirit Dive look this client draws. DJINN = a full set of plates in the style of the Djinn Full Equip (forged on piece by piece, engraved glow veins),
 *       V2 = the remade look of 3.0.0 (wind halo and blades, flame mane, water ribbons, rock crown), CLASSIC = the 1.99 look.</li>
 *   <li>{@code spiritDive.classicLook} (default false, kept from 3.0.0): true is the same as look CLASSIC (it wins over {@code look}).</li>
 *   <li>{@code spiritDive.characterEffects} (default true): draw the character tier of Yuno, Fuegoleon and Noelle; false draws every dive as the standard tier (cheaper).</li>
 *   <li>{@code spiritDive.particles} (default true): the ember and mote particle trail of a dive.</li>
 *   <li>{@code fuegoleonArm.view} (default ALWAYS): OFF, FLARE (only while casting, diving or with the Salamander out) or ALWAYS (also at rest).</li>
 * </ul>
 */
public final class DiveClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC, CHARACTER_FX, PARTICLES;
    private static final ModConfigSpec.EnumValue<Look> LOOK;
    private static final ModConfigSpec.EnumValue<FuegoleonArmRules.View> ARM_VIEW;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spiritDive");
        LOOK = b.comment("The Spirit Dive look this client draws: DJINN (a full set of plates in the style of the Djinn Full Equip), V2 (the 3.0.0 remake: wind halo and blades, flame mane, water ribbons, rock crown) or CLASSIC (1.99).")
                .defineEnum("look", Look.DJINN);
        CLASSIC = b.comment("true: the Spirit Dive is drawn as in 1.99 (armour-like pieces), whatever 'look' says (the same as look = CLASSIC).").define("classicLook", false);
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
            SpiritDiveLook.set(CLASSIC.get() ? Look.CLASSIC : LOOK.get(), CHARACTER_FX.get(), PARTICLES.get(), ARM_VIEW.get());
        } catch (IllegalStateException notLoaded) {
            SpiritDiveLook.set(Look.DJINN, true, true, FuegoleonArmRules.View.ALWAYS);
        }
    }
}
