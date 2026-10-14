package com.newuniverse.nusmp.client.compat;

import com.newuniverse.nusmp.compat.CompatCodexLook;
import com.newuniverse.nusmp.compat.CompatItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client wiring of the Compatibility Codex item: the tint resolver of its grimoire-style model, and opening its screen. Call {@link #init} from the client branch of the mod constructor. */
public final class CompatCodexClient {
    private CompatCodexClient() {}

    /** Tint 0 = cover leather, 1 = frame metal, 2 = emblem (the grimoire's layers; see {@link CompatCodexLook}). */
    public static final ItemColor ITEM_COLOR = (stack, tintIndex) -> CompatCodexLook.argb(tintIndex);

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> event.register(ITEM_COLOR, CompatItems.CODEX.get()));
    }

    /** Opens the Codex screen (client only; the item calls this behind {@code level.isClientSide()}). */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new CompatCodexScreen(mc.screen));
    }
}
