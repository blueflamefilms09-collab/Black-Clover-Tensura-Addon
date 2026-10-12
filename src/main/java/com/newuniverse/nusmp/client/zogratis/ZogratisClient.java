package com.newuniverse.nusmp.client.zogratis;

import com.newuniverse.nusmp.entity.zogratis.DanteBoss;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.VanicaBoss;
import com.newuniverse.nusmp.entity.zogratis.ZenonBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderers of the Zogratis family: the four bosses, the Dark Disciple, the Blood Beast and Lucius's clone. */
public final class ZogratisClient {
    private ZogratisClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ZogratisEntities.DANTE.get(), ctx -> new ZogratisBossRenderer<DanteBoss>(ctx, ZogratisBossRenderer.DANTE));
        e.registerEntityRenderer(ZogratisEntities.VANICA.get(), ctx -> new ZogratisBossRenderer<VanicaBoss>(ctx, ZogratisBossRenderer.VANICA));
        e.registerEntityRenderer(ZogratisEntities.ZENON.get(), ctx -> new ZogratisBossRenderer<ZenonBoss>(ctx, ZogratisBossRenderer.ZENON));
        e.registerEntityRenderer(ZogratisEntities.LUCIUS.get(), ctx -> new ZogratisBossRenderer<LuciusBoss>(ctx, ZogratisBossRenderer.LUCIUS));
        e.registerEntityRenderer(ZogratisEntities.DARK_DISCIPLE.get(), ZogratisDiscipleRenderer::new);
        e.registerEntityRenderer(ZogratisEntities.BLOOD_BEAST.get(), ZogratisBeastRenderer::new);
        e.registerEntityRenderer(ZogratisEntities.ZOGRATIS_CLONE.get(), ZogratisCloneRenderer::new);
    }
}
