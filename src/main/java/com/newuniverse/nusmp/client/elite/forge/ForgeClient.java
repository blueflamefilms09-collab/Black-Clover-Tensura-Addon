package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.elite.forge.ForgeComponents;
import com.newuniverse.nusmp.elite.forge.net.ForgeClientBridge;
import com.newuniverse.nusmp.elite.forge.net.ForgeNet;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Why: the client's wiring of the forge: it replaces the {@link ForgeClientBridge} consumers (so a packet opens the right screen), keeps the last progress the server
 * synced (for anything that wants it), and adds the tooltip lines of a forged item (its quality, who forged it, how many engravings the forge put on it). It is only
 * loaded on the client (the registry calls it behind a dist check), and the bridge keeps the common packet code free of client classes.
 */
public final class ForgeClient {
    private ForgeClient() {}

    private static volatile ForgeNet.Progress progress = ForgeNet.Progress.empty();

    /** The last progress the server sent. */
    public static ForgeNet.Progress progress() { return progress; }

    public static void init(IEventBus modBus) {
        ForgeClientBridge.open = ForgeClient::onOpen;
        ForgeClientBridge.start = ForgeClient::onStart;
        ForgeClientBridge.done = ForgeClient::onDone;
        ForgeClientBridge.sync = s -> progress = s.progress();
        NeoForge.EVENT_BUS.addListener(ForgeClient::onTooltip);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> progress = ForgeNet.Progress.empty());
    }

    private static void onOpen(ForgeNet.OpenStation p) {
        progress = p.progress();
        Minecraft mc = Minecraft.getInstance();
        Screen cur = mc.screen;
        if (cur instanceof ForgeMinigameScreen m) m.setPending(p);
        else if (cur instanceof ForgeStationScreen s) s.update(p);
        else if (!p.refresh()) mc.setScreen(new ForgeStationScreen(p));
    }

    private static void onStart(ForgeNet.Start p) {
        Minecraft.getInstance().setScreen(new ForgeMinigameScreen(p));
    }

    private static void onDone(ForgeNet.Done d) {
        Minecraft mc = Minecraft.getInstance();
        Screen cur = mc.screen;
        if (cur instanceof ForgeMinigameScreen m) m.setDone(d);
        else if (cur instanceof ForgeStationScreen s) s.status(Component.translatable(d.message()), d.ok() ? 0xFF7CF0B0 : 0xFFFF6A6A);
        else mc.gui.setOverlayMessage(Component.translatable(d.message()), false);
    }

    private static void onTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        Quality q = ForgeComponents.qualityOf(stack);
        if (q == null) return;
        List<Component> lines = e.getToolTip();
        int at = Math.min(1, lines.size());
        lines.add(at, Component.translatable("nusmp.eforge.tip.quality", Component.translatable("nusmp.eforge.quality." + q.id()).withStyle(s -> s.withColor(q.rgb()))).withStyle(ChatFormatting.GRAY));
        String smith = stack.get(ForgeComponents.SMITH.get());
        if (smith != null && !smith.isEmpty()) lines.add(at + 1, Component.translatable("nusmp.eforge.tip.smith", smith).withStyle(ChatFormatting.DARK_GRAY));
        int engraved = ForgeComponents.engravedCount(stack);
        if (engraved > 0) lines.add(Math.min(at + 2, lines.size()), Component.translatable("nusmp.eforge.tip.engraved", engraved).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
