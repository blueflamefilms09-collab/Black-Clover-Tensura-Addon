package com.newuniverse.nusmp.client.mode;

import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.ArmStyle;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Look;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-modearmor-client.toml} (it only changes what THIS client draws; the dedicated server never registers it): {@code modeArmor.windLook}, which overlay Yuno's Wind Spirit
 * Dive wears: V2 (the default: the layered 3D coat, the arrow crown and the leaf wings, {@link ModeArmorWindV2}) or CLASSIC (the 73-quad overlay of {@link ModeArmorLayer}). Every other Wind user wears
 * the classic overlay either way. {@code modeArmor.windArms} picks the arms of V2: BLOCKY (default) or SMOOTH. The value is mirrored into {@link ModeArmorWindV2#setLook} when the file loads or reloads, so the layer reads a field, not a config, every frame.
 */
public final class ModeArmorWindV2Config {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<Look> LOOK;
    private static final ModConfigSpec.EnumValue<ArmStyle> ARMS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("modeArmor");
        LOOK = b.comment("The Wind Spirit Dive overlay of Yuno (the mage whose bound grimoire is Yuno's): V2 (default: a layered 3D coat with a standing fur collar, shoulder mantles, sash and coat tails, a small crown of arrow blades and two leaf wings) or CLASSIC (the old overlay of flat cards). Every other Wind user wears the classic overlay. Only changes what this client draws.")
                .defineEnum("windLook", Look.V2);
        ARMS = b.comment("The arms of the V2 overlay: BLOCKY (default: the classic overlay's square box sleeves with the chunky fur cuffs and a square shoulder mantle, in the v2 cloth) or SMOOTH (the rounded sleeves, cuffs and domed mantles of the first v2). Only changes what this client draws; windLook = CLASSIC ignores it.")
                .defineEnum("windArms", ArmStyle.BLOCKY);
        b.pop();
        SPEC = b.build();
    }

    private ModeArmorWindV2Config() {}

    private static boolean done;

    /** One line of the client block of the mod constructor. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-modearmor-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            ModeArmorWindV2.setLook(LOOK.get());
            ModeArmorWindV2.setArms(ARMS.get());
        } catch (IllegalStateException notLoaded) {
            ModeArmorWindV2.setLook(Look.V2);
            ModeArmorWindV2.setArms(ArmStyle.BLOCKY);
        }
    }
}
