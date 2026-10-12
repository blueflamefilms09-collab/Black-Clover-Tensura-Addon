package com.newuniverse.nusmp.client.wizard;

import com.newuniverse.nusmp.entity.NUEntities;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The Tower Wizard is drawn as the game's own witch: a pointed hat and a robe. */
public final class TowerWizardClient {
    private TowerWizardClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.TOWER_WIZARD.get(), WitchRenderer::new);
    }
}
