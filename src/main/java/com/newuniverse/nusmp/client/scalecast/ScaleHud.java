package com.newuniverse.nusmp.client.scalecast;

import com.newuniverse.nusmp.scalecast.ScaleVisualConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the small plate over the hotbar that shows what the local player's own Scale measured (docs/magic/scale_casting.md): the two weights and what the aura takes off the weighed foe. The
 * numbers are decided by the server and arrive in the Scale prop's synced word; the painter ({@code client.prop.ScaleBalancePainter}) decodes them into two strings when they change and hands
 * them here each frame it draws the player's own Scale, and this layer only draws them while they are fresh (the plate vanishes by itself when the Scale fades, the foe is gone or the Scale
 * is out of sight). It holds no state of the game, only two strings and a tick stamp, and registers the CLIENT config {@code nusmp-scale-casting-client.toml}.
 */
public final class ScaleHud {
    private ScaleHud() {}

    private static String line1 = "", line2 = "";
    private static long seen = Long.MIN_VALUE;
    private static boolean registered;

    /** Called once from the client block of the mod constructor. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (registered) return;
        registered = true;
        container.registerConfig(ModConfig.Type.CLIENT, ScaleVisualConfig.SPEC, "nusmp-scale-casting-client.toml");
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "scale_weights"), ScaleHud::render));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { seen = Long.MIN_VALUE; line1 = ""; line2 = ""; });
    }

    /** The painter reports the readout of the local player's own Scale (strings built only when the word changes: a reference copy here, no allocation). */
    public static void note(String first, String second, long gameTime) {
        line1 = first;
        line2 = second;
        seen = gameTime;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!ScaleVisualConfig.readout()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        if (mc.level.getGameTime() - seen > 6L) return;
        Font font = mc.font;
        int w = Math.max(font.width(line1), font.width(line2)) + 12, h = 24;
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() - 80;
        g.fill(x, y, x + w, y + h, 0x88101418);
        g.fill(x, y, x + w, y + 1, 0xCCD7B25A);
        g.drawCenteredString(font, line1, g.guiWidth() / 2, y + 4, 0xFFE8B0);
        g.drawCenteredString(font, line2, g.guiWidth() / 2, y + 14, 0x9CF0E4);
    }
}
