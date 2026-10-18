package com.newuniverse.nusmp.client.heroweapon.carriage;

import com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Why: the client half of the Vassal Carriage is one renderer for one entity type, and it has to be attached to the mod event bus during start-up, before the
 * renderers are registered. The carriage's logic calls {@link #init} from its constructor on the client (it is the one place that knows the weapon exists),
 * and the lead may also list this class in {@code HeroClient.init}; the call is idempotent, so both are safe. It registers the renderer for the carriage
 * entity type and drops the baked mesh when the resources reload.
 */
public final class CarriageClient {
    private CarriageClient() {}

    private static boolean done;

    /** Attaches the renderer registration and the mesh reload hook to the mod bus; safe to call twice. */
    public static synchronized void init(IEventBus modBus) {
        if (done || modBus == null) return;
        done = true;
        modBus.addListener(CarriageClient::registerRenderers);
        modBus.addListener((ModelEvent.BakingCompleted e) -> CarriageMesh.clear());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CarriageEntities.CARRIAGE.get(), CarriageRenderer::new);
    }
}
