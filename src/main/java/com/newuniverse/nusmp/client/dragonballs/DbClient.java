package com.newuniverse.nusmp.client.dragonballs;

import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.DbBoot;
import com.newuniverse.nusmp.dragonballs.DbSky;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the Dragon Balls: the Shenron renderer, and the dark sky of a summoning. The darkening is a screen tint (a translucent dark quad before the HUD) plus a darker fog colour, driven by
 * {@link DbSky} (which the server's small packet writes and which expires by itself). It costs one quad and one float compare per frame while a dragon is out and nothing otherwise. A purely visual
 * setting, so it is a CLIENT config ({@code nusmp-dragonballs-client.toml}: {@code dragonBalls.skyDarkening}).
 */
public final class DbClient {
    private DbClient() {}

    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SKY_DARKENING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("dragonBalls");
        SKY_DARKENING = b.comment("Darken the screen and the fog while Shenron is summoned near you. false = the sky stays as it is (the dragon is still drawn).").define("skyDarkening", true);
        b.pop();
        SPEC = b.build();
    }

    private static boolean darkening() { try { return SKY_DARKENING.get(); } catch (Throwable t) { return true; } }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-dragonballs-client.toml");
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(DbBoot.SHENRON.get(), ShenronRenderer::new));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> DbSky.clientTick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> DbSky.reset());
        NeoForge.EVENT_BUS.addListener(DbClient::onGui);
        NeoForge.EVENT_BUS.addListener(DbClient::onFog);
    }

    private static void onGui(RenderGuiEvent.Pre e) {
        float f = DbSky.level();
        if (f <= 0.01f || !darkening()) return;
        GuiGraphics g = e.getGuiGraphics();
        int rgb = DbSky.set() == BallSet.SUPER.ordinal() ? 0x0C0418 : 0x020A08;
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (f * 0.62f * 255f) << 24) | rgb);
    }

    private static void onFog(ViewportEvent.ComputeFogColor e) {
        float f = DbSky.level();
        if (f <= 0.01f || !darkening()) return;
        float k = 1f - 0.85f * f;
        e.setRed(e.getRed() * k);
        e.setGreen(e.getGreen() * k);
        e.setBlue(e.getBlue() * k);
    }
}
