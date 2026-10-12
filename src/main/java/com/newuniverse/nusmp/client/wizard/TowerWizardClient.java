package com.newuniverse.nusmp.client.wizard;

import com.newuniverse.nusmp.entity.NUEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The Tower Wizard is drawn with his own geo model by {@link TowerWizardRenderer} (an old wizard with a green orb), no longer as the game's witch. */
public final class TowerWizardClient {
    private TowerWizardClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.TOWER_WIZARD.get(), TowerWizardRenderer::new);
    }
}
