package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.SolomonModels;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3D models of the Solomon ultimate: {@code visual.solomonModels} in {@code config/nusmp-solomon-client.toml} (a CLIENT config: it only changes how things look). On (the default): Solomon draws its real 3D models (the ring
 * of the 72 Pillars, the floating Grimoire, the links and the sigil head of the Spell Chain, the signet ring over the marked target, the rune crystals of the Convergence and the glass shards of the Erasure). Off: exactly what
 * the version before drew (the flat sprites). The value is mirrored into the pure {@link SolomonModels} switch when the file loads or reloads, the same way {@link HerculesModelsConfig} does; the dedicated server never
 * registers this config.
 */
public final class SolomonModelsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue MODELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        MODELS = b.comment("Draw the 3D models of the Solomon ultimate (the pillars of the 72, the Grimoire, the spell chain links, the signet ring, the rune crystals and glass shards). Off = the flat sprites of the older versions.")
                .define("solomonModels", true);
        b.pop();
        SPEC = b.build();
    }

    private SolomonModelsConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-solomon-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { SolomonModels.setEnabled(MODELS.get()); } catch (IllegalStateException notLoaded) { SolomonModels.setEnabled(true); }
    }
}
