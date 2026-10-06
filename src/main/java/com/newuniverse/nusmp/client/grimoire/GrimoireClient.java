package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.blackclover.BlackCloverRegistry;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client wiring for the grimoire's look: the geometry loader and the tint resolver. Call {@link #init} from the client branch of the mod constructor. */
public final class GrimoireClient {
    public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "grimoire");

    /** Item colour handler: tintindex 0 = cover leather, 1 = frame / ornament metal, 2 = emblem; anything else untinted. */
    public static final ItemColor ITEM_COLOR = (stack, tintIndex) -> {
        int rgb = GrimoireItem.look(stack).tint(tintIndex);
        return rgb < 0 ? -1 : 0xFF000000 | rgb;
    };

    private GrimoireClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(GrimoireClient::registerLoaders);
        modBus.addListener(GrimoireClient::registerColors);
    }

    private static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(LOADER_ID, GrimoireGeometry.LOADER);
    }

    private static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register(ITEM_COLOR, BlackCloverRegistry.GRIMOIRE.get());
    }
}
