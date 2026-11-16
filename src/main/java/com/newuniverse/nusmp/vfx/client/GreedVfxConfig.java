package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.GreedVfx;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * VFX wave 2: {@code greed.classicVfx} and {@code devil.classicVfx} in {@code config/nusmp-greed-devil-vfx-client.toml} (a CLIENT config: it only changes how things look). Off (the default): the Black Sword
 * Greed techniques and the devil combat hits draw their anime shapes (GreedLayer) on top of the old particles. On: the layer draws nothing for them, so only the old particles of 3.96.0 are seen. The values
 * are mirrored into the pure {@link GreedVfx} switches when the file loads or reloads; the dedicated server never registers this config.
 */
public final class GreedVfxConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue GREED, DEVIL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("greed");
        GREED = b.comment("true: the Black Sword Greed (forms and Secret Techniques) shows only its old particles. false: the anime effects are drawn too (ink crescents, black lightning, the hungry maw, the dome of scales). Looks only.")
                .define("classicVfx", false);
        b.pop();
        b.push("devil");
        DEVIL = b.comment("true: a devil's combat hit shows only the old tinted motes. false: each devil world's own hit is drawn (sigil crack, chainsaw rip, contract seal, blue flames, aura pulse, ghost fire, Brand and eclipse, curse runes). Looks only.")
                .define("classicVfx", false);
        b.pop();
        SPEC = b.build();
    }

    private GreedVfxConfig() {}

    /** Client side only: registers the file and keeps the pure switches in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-greed-devil-vfx-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    static void apply() {
        try { GreedVfx.setClassic(GREED.get(), DEVIL.get()); } catch (IllegalStateException notLoaded) { GreedVfx.setClassic(false, false); }
    }
}
