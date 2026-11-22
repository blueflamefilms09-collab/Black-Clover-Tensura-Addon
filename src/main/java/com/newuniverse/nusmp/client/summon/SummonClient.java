package com.newuniverse.nusmp.client.summon;

import com.newuniverse.nusmp.skill.summon.SummonEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client half of the Summoner creatures (docs/summoner_creatures.md): registers the renderer. Called by the client setup only; no common class names it. */
public final class SummonClient {
    private SummonClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(SummonEntities.BEAST.get(), SummonBeastRenderer::new));
    }
}
