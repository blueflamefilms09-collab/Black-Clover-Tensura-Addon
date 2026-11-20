package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.lords.LordVfxCodes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: {@code lords.classicVfx} in {@code config/nusmp-lords-vfx-client.toml} (a CLIENT config: it only changes how things look). Off (the default): the Lord weapons (Black Axe of Sloth, Gunblade of Envy, Spear of
 * Vanity, Seifort) and the Black Sword Greed's Hunger of the Blade draw their own effect for every art, decree, state and level-up ({@code layer.LordLayer}). On: the layer draws nothing, so only the small particle puff
 * the old weapons also send is seen. The value is mirrored into the pure {@link LordVfxCodes} switch when the file loads or reloads; the dedicated server never registers this config.
 */
public final class LordVfxConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC;
    private static final ModConfigSpec.BooleanValue CLASSIC_MODELS;
    private static final ModConfigSpec.BooleanValue STATUS_BAR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("lords");
        CLASSIC = b.comment("true: the Lord weapons and Greed's hunger show no drawn effect (only the old particle puff). false: each weapon draws its own arts, decree, strained state and level-up. Looks only.")
                .define("classicVfx", false);
        CLASSIC_MODELS = b.comment("true: the four Lord weapons (Black Axe of Sloth, Gunblade of Envy, Spear of Vanity, Seifort) are drawn with their old 3D models and skins (weapon_meshes/classic). false: the new Blender models. Looks only.")
                .define("classicModels", false);
        STATUS_BAR = b.comment("true: the Tensura status menu shows a mastery bar for each Lord weapon you carry (and Greed's Half-Starved mastery) below the skill bars. Looks only.")
                .define("statusMasteryBar", true);
        b.pop();
        SPEC = b.build();
    }

    private LordVfxConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-lords-vfx-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    /** True when the four Lord weapons draw their old models (the file is not loaded yet: false). */
    public static boolean classicModels() {
        try { return CLASSIC_MODELS.get(); } catch (IllegalStateException | NullPointerException notLoaded) { return false; }
    }

    /** True when the status menu shows the mastery bars. */
    public static boolean statusBar() {
        try { return STATUS_BAR.get(); } catch (IllegalStateException | NullPointerException notLoaded) { return true; }
    }

    static void apply() {
        try { LordVfxCodes.setClassic(CLASSIC.get()); } catch (IllegalStateException notLoaded) { LordVfxCodes.setClassic(false); }
    }
}
