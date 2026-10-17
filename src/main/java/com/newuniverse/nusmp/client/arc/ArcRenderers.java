package com.newuniverse.nusmp.client.arc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcEntities;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: one call registers every renderer of the arcs: the shared NPC entity gets {@link ArcNpcRenderer}, and every boss registered with {@link ArcEntities#boss} gets an
 * {@link ArcBossRenderer} built from the {@code ArcLook} that was stated next to its type. A boss whose type or look is broken is logged and skipped, so one bad character never
 * stops the others (or the game) from loading. Called from the client's renderer registration event, next to the Zogratis family's.
 */
public final class ArcRenderers {
    private ArcRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ArcEntities.NPC.get(), ArcNpcRenderer::new);
        for (ArcEntities.Entry b : ArcEntities.bosses()) {
            try {
                EntityType<? extends ArcBoss> type = b.type().get();
                e.registerEntityRenderer(type, ctx -> new ArcBossRenderer(ctx, b.look()));
            } catch (RuntimeException ex) {
                LogUtils.getLogger().error("[nusmp] the renderer of the arc boss {} could not be registered: {}", b.id(), ex.toString());
            }
        }
    }
}
