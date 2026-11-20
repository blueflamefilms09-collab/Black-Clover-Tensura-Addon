package com.newuniverse.nusmp.client.heroweapon.bow;

import com.newuniverse.nusmp.heroweapon.weapon.bow.HeroArrowEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: one place registers the client renderer of the Legendary Bow's real arrow; NUSMP adds it to the mod bus inside its client-only block
 * ({@code modEventBus.addListener(BowArrowClient::register)}), so a dedicated server never loads it.
 */
public final class BowArrowClient {
    private BowArrowClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(HeroArrowEntities.HERO_ARROW.get(), HeroArrowRenderer::new);
    }
}
