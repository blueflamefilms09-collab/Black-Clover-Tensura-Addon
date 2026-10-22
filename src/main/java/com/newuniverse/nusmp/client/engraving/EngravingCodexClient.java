package com.newuniverse.nusmp.client.engraving;

import com.newuniverse.nusmp.engraving.codex.CodexEntries;
import com.newuniverse.nusmp.engraving.codex.CodexGather;
import com.newuniverse.nusmp.engraving.codex.EngravingCodexItems;
import com.newuniverse.nusmp.engraving.codex.EngravingCodexLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client wiring of the Engravement Codex item: the tint resolver of its grimoire-style model, the one cached copy of the book, and opening its screen. Call {@link #init} from the client
 * branch of the mod constructor. The book (about a hundred and twenty entries) is built from the registries and the language ONCE, on the first open, and kept in a single reference; it is
 * dropped when the resources reload (a language change or F3+T also reloads them) and when the player leaves the world, so it is rebuilt with the new text on the next open. The memory
 * is bounded by the data (the entries of the mod), never by play time. No packet: the server is not involved.
 */
public final class EngravingCodexClient {
    private EngravingCodexClient() {}

    /** Tint 0 = cover leather, 1 = frame metal, 2 = emblem (the grimoire's layers; see {@link EngravingCodexLook}). */
    public static final ItemColor ITEM_COLOR = (stack, tintIndex) -> EngravingCodexLook.argb(tintIndex);

    private static volatile CodexEntries.Book cached;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> event.register(ITEM_COLOR, EngravingCodexItems.CODEX.get()));
        modBus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener((ResourceManagerReloadListener) manager -> cached = null));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> cached = null);
    }

    /** The book, built on first use from this client's registries and language. Client thread only. */
    static CodexEntries.Book book(Minecraft mc) {
        CodexEntries.Book b = cached;
        if (b == null && mc.level != null) {
            b = CodexGather.book(mc.level.registryAccess(), CodexGather.currentLanguage());
            cached = b;
        }
        return b;
    }

    /** Opens the Codex screen (client only; the item calls this behind {@code level.isClientSide()}). */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        CodexEntries.Book book = book(mc);
        if (book == null) return;
        mc.setScreen(new EngravingCodexScreen(mc.screen, book));
    }
}
