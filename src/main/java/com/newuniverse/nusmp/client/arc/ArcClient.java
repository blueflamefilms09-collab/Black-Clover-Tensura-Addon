package com.newuniverse.nusmp.client.arc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcItems;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Why: the client counterpart of {@code ArcSystem}: one renderer registration for every boss of the roster in a loop (each reads its {@link ArcLook}
 * from the entry the boss class stated next to its entity type, so a new boss needs no client code), one for the shared NPC type, and the tint of the
 * spawn egg in the character's colours. The renderers themselves are {@link ArcBossRenderer} and {@link ArcNpcRenderer} (the art kit's). A boss whose
 * registration fails is logged and skipped so one bad character cannot take the whole client down. Call {@link #init} from the mod constructor on the
 * client only.
 */
public final class ArcClient {
    private ArcClient() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    public static void init(IEventBus modBus) {
        modBus.addListener(ArcClient::registerRenderers);
        modBus.addListener(ArcClient::registerColors);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ArcEntities.NPC.get(), ArcNpcRenderer::new);
        for (ArcEntities.Entry b : ArcEntities.bosses()) {
            try {
                registerBoss(e, b);
            } catch (RuntimeException ex) {
                LOG.error("[nusmp] no renderer for the arc boss {}: {}", b.id(), ex.toString());
            }
        }
    }

    private static void registerBoss(EntityRenderersEvent.RegisterRenderers e, ArcEntities.Entry b) {
        EntityType<? extends ArcBoss> type = b.type().get();
        ArcLook look = b.look();
        EntityRendererProvider<ArcBoss> provider = ctx -> new ArcBossRenderer(ctx, look);
        e.registerEntityRenderer(type, provider);
    }

    private static void registerColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> {
            ArcCharacter c = ArcRoster.byId(ArcItems.characterOf(stack));
            int rgb = c == null ? 0x8A8A9A : tint == 0 ? c.color() : c.accent();
            return 0xFF000000 | (rgb & 0xFFFFFF);
        }, ArcItems.SPAWN_EGG.get());
    }
}
