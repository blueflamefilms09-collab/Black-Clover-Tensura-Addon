package com.newuniverse.nusmp.client.aura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Spirit Dive remake: the one client entry point (a single line of the client block of the mod constructor): the client config {@code nusmp-spiritdive-client.toml} ({@link DiveClientConfig}), the
 * frame counter of the painters ({@link SpiritDiveFrame}), the first person flaming arm ({@link FireArmFirstPerson}), the first person gauntlets of the Djinn-style dive ({@link DiveFirstPerson}) and the particle trail ({@link DiveParticles}). The painters themselves are
 * registered by {@code AuraRegistry} ({@link SpiritDiveAura}, {@link FuegoleonArmAura}). Safe to call twice.
 */
public final class DiveClient {
    private DiveClient() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, DiveClientConfig.SPEC, "nusmp-spiritdive-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> DiveClientConfig.apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> DiveClientConfig.apply());
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> { if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) SpiritDiveFrame.begin(); });
        NeoForge.EVENT_BUS.addListener(FireArmFirstPerson::onRenderArm);
        NeoForge.EVENT_BUS.addListener(FireArmFirstPerson::onRenderHand);
        NeoForge.EVENT_BUS.addListener(DiveFirstPerson::onRenderArm);                       // the Djinn-style dive: the gauntlets in first person
        NeoForge.EVENT_BUS.addListener(DiveFirstPerson::onRenderHand);
        NeoForge.EVENT_BUS.addListener(DiveParticles::onClientTick);
    }
}
