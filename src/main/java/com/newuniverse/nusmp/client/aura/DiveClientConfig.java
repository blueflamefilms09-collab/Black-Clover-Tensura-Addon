package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Spirit Dive remake: the CLIENT config {@code nusmp-spiritdive-client.toml} (it only changes what THIS client draws; the dedicated server never registers it). The values are mirrored into the plain
 * flags of {@link SpiritDiveLook} when the file loads or reloads, so the painters read a field, not a config, every frame.
 * <ul>
 *   <li>{@code spiritDive.look} (default REEL since 3.3): which Spirit Dive look this client draws. REEL = the V2 design executed like the showcase videos (glowing cube crown with a gold ring halo and white lights, cube gauntlets, a mosaic of block patches, a leaf or comb wing, a spiral-ribbon entry, the character's weapon), ANIME = the look of the anime (the wearer's own body bathed in the energy of the spirit: a flowing shell, glowing hair and eyes, a crown halo, plume sheets), DJINN = a full set of plates in the style of the Djinn Full Equip (forged on piece by piece, engraved glow veins),
 *       V2 = the remade look of 3.0.0 (wind halo and blades, flame mane, water ribbons, rock crown), CLASSIC = the 1.99 look.</li>
 *   <li>{@code spiritDive.windLook} (default PACK, 3.4): which Wind the REEL look draws: PACK = the owner's reference pack of Yuno's Spirit Dive, REEL = the Wind of 3.3.</li>
 *   <li>{@code spiritDive.classicLook} (default false, kept from 3.0.0): true is the same as look CLASSIC (it wins over {@code look}).</li>
 *   <li>{@code spiritDive.characterEffects} (default true): draw the character tier of Yuno, Fuegoleon and Noelle; false draws every dive as the standard tier (cheaper).</li>
 *   <li>{@code spiritDive.particles} (default true): the ember and mote particle trail of a dive.</li>
 *   <li>{@code fuegoleonArm.view} (default ALWAYS): OFF, FLARE (only while casting, diving or with the Salamander out) or ALWAYS (also at rest).</li>
 *   <li>{@code fuegoleonArm.look} (default DJINN, 3.1): DJINN (the Djinn-Equip flame limb), V3 (the 3.0.0 arm) or CLASSIC (the plain molten model).</li>
 * </ul>
 */
public final class DiveClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC, CHARACTER_FX, PARTICLES;
    private static final ModConfigSpec.EnumValue<Look> LOOK;
    private static final ModConfigSpec.EnumValue<WindLook> WIND_LOOK;
    private static final ModConfigSpec.EnumValue<FuegoleonArmRules.View> ARM_VIEW;
    private static final ModConfigSpec.EnumValue<FuegoleonArmDjinnRules.Look> ARM_LOOK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spiritDive");
        LOOK = b.comment("The Spirit Dive look this client draws: REEL (default: the V2 design like the showcase videos: a crown of glowing cubes with a gold ring halo, cube gauntlets, a mosaic of block patches on the body, a leaf / comb wing, a ribbon-and-flash entry, the character's own weapon), ANIME (what the anime shows: the wearer bathed in the energy of the spirit, glowing hair and eyes, a crown halo, big plume sheets; Fuegoleon, Yuno and Noelle get more), DJINN (a full set of plates in the style of the Djinn Full Equip), V2 (the 3.0.0 remake: wind halo and blades, flame mane, water ribbons, rock crown) or CLASSIC (1.99).")
                .defineEnum("look", Look.REEL);
        WIND_LOOK = b.comment("Which Wind the REEL look draws: PACK (default: the Wind of the owner's reference pack of Yuno's Spirit Dive: a pale green-white light over the body, a crown of tall bars with arrow tips on a thin ring, one huge smooth leaf wing with an ornate cross, a circle of connected crosses round the wrist, Yuno's sword of Zephyr and bow of Boreas) or REEL (the Wind of 3.3: cube crown, cube gauntlets, hex-membrane wings). Only changes what this client draws.")
                .defineEnum("windLook", WindLook.PACK);
        CLASSIC = b.comment("true: the Spirit Dive is drawn as in 1.99 (armour-like pieces), whatever 'look' says (the same as look = CLASSIC).").define("classicLook", false);
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
            SpiritDiveLook.set(CLASSIC.get() ? Look.CLASSIC : LOOK.get(), CHARACTER_FX.get(), PARTICLES.get(), ARM_VIEW.get());
            SpiritDiveLook.setWindLook(WIND_LOOK.get());
            FuegoleonArmLook.set(ARM_LOOK.get());
        } catch (IllegalStateException notLoaded) {
            SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
            SpiritDiveLook.setWindLook(WindLook.PACK);
            FuegoleonArmLook.set(FuegoleonArmDjinnRules.DEFAULT_LOOK);
        }
    }
}
