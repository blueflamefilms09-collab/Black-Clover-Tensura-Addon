package com.newuniverse.nusmp.client.greed;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.greed.GreedNet;
import com.newuniverse.nusmp.greed.GreedRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Why: the client half of the Black Sword Greed is small and decides nothing: one rebindable key that only ASKS the server to switch form (R: the next open form, sneak + R: the previous), the cached state the server
 * sends (at most once a second, only on change) and two draws: the crimson starvation vignette and a one-line panel (satiation, tier, form). Everything on screen is the server's number. Cost: one fill gradient pair
 * and one string per frame, only while the local player is the wielder; nothing is allocated per frame beyond the line's text, rebuilt when the packet changes.
 */
public final class GreedClient {
    private GreedClient() {}

    public static final String CATEGORY = "key.categories.anime_multiverse";
    public static final KeyMapping FORM = new KeyMapping("key.anime_multiverse.greed_form", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "greed_hud");

    private static volatile GreedNet.Hud state;
    private static volatile String line = "";

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(FORM));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(LAYER, GreedClient::render));
        GreedNet.clientHud = GreedClient::onHud;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { state = null; line = ""; });
    }

    private static void onHud(GreedNet.Hud h) {
        state = (h.flags() & GreedNet.Hud.F_OWNER) != 0 ? h : null;
        if (state != null) {
            GreedRules.Form f = GreedRules.Form.byId(h.form());
            line = Component.translatableWithFallback("greed.hud", "Greed: %s%%  Tier %s  %s", h.satiation(), h.tier(), Component.translatableWithFallback(f.langKey(), f.english())).getString();
        } else line = "";
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.getConnection() == null) return;
        while (FORM.consumeClick()) {
            if (state != null) PacketDistributor.sendToServer(new GreedNet.SwitchForm(mc.player.isShiftKeyDown() ? -2 : -1));
            com.newuniverse.nusmp.client.lords.LordClient.switchForm(mc.player.isShiftKeyDown());       // the same key switches the form of a Lord weapon (the server decides)
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) { SafeCast.client("greed.hud", () -> draw(g, delta)); }

    private static void draw(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        GreedNet.Hud s = state;
        if (s == null || mc.player == null || mc.options.hideGui) return;
        boolean starving = (s.flags() & GreedNet.Hud.F_STARVING) != 0, half = (s.flags() & GreedNet.Hud.F_HALF) != 0;
        if (starving || half) {
            float pulse = (Mth.sin((mc.player.tickCount + delta.getGameTimeDeltaPartialTick(true)) * 0.25f) + 1.0f) * 0.5f;
            int alpha = (int) (starving ? Mth.lerp(pulse, 55, 135) : Mth.lerp(pulse, 30, 75));
            int edge = (alpha << 24) | 0x9E0412;
            int w = g.guiWidth(), h = g.guiHeight(), border = 26;
            RenderSystem.enableBlend();
            g.fillGradient(0, 0, w, border, edge, 0x009E0412);
            g.fillGradient(0, h - border, w, h, 0x009E0412, edge);
            g.fill(0, 0, border / 2, h, ((alpha / 2) << 24) | 0x9E0412);
            g.fill(w - border / 2, 0, w, h, ((alpha / 2) << 24) | 0x9E0412);
            RenderSystem.disableBlend();
        }
        String text = line;
        if (!text.isEmpty()) {
            int[] spot = com.newuniverse.nusmp.client.hud.HudDock.take(com.newuniverse.nusmp.client.hud.HudDock.Side.LEFT, g.guiWidth(), g.guiHeight(), mc.font.width(text), 10, new int[]{6, g.guiHeight() - 12});      // 5.0.0: the shared left column
            g.drawString(mc.font, text, spot[0], spot[1], starving ? 0xFFFF4040 : 0xFFE0E0E0, true);
        }
    }
}
