package com.newuniverse.nusmp.client.celestial;

import com.newuniverse.nusmp.celestial.CelestialEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The client side of Celestial Spirit Magic: the renderer of the Celestial Spirit King (the spirits and the Gate are props, see client.prop.CelestialPropPainter). */
public final class CelestialClient {
    private CelestialClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CelestialEntities.SPIRIT_KING.get(), SpiritKingRenderer::new);
    }
}
