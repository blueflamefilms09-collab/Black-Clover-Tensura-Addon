package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.bar.BossBarNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * The boss bar overlay of the mod. On {@code RenderGuiLayerEvent.Pre} of vanilla's boss layer it draws every boss bar itself, in vanilla's
 * layout (centred, 182 wide, the name 9 px above, 19 px between bars, stopping at a third of the screen height): bars the mod did not
 * style look exactly like vanilla's, and the mod bosses' bars (those with a {@link BossBarNet.Style} from the server) get the animated
 * bar of {@link BossBarPainter}. The vanilla layer is cancelled only after the client's bar list was read successfully (see
 * {@link VanillaBossBars}); otherwise, or when {@code bossbar.animations} is off, or if drawing ever throws, vanilla draws as it always did.
 * Shake, glitch and shatter run on the {@link BossBarAnim} of each bar; at most {@value #SHATTERS_PER_FRAME} bars shatter in one frame.
 */
public final class ModBossBarOverlay {
    private ModBossBarOverlay() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SHATTERS_PER_FRAME = 2;
    private static boolean failed;
    private static long lastPurge = Long.MIN_VALUE;

    /** Registers the overlay and the logout cleanup on the game bus (call once from the client section of the mod constructor). */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ModBossBarOverlay::onLayer);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> BossBarClientState.clear());
    }

    private static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (failed || !VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName()) || !BossBarOptions.animations()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gui == null) return;
        Map<UUID, LerpingBossEvent> events = VanillaBossBars.events(mc.gui.getBossOverlay());
        if (events == null) return;                                           // unreadable: leave vanilla alone
        long now = mc.level.getGameTime();
        if (now != lastPurge && now % 20 == 0) {
            lastPurge = now;
            BossBarClientState.purge(events, now);
        }
        if (events.isEmpty()) return;                                         // nothing to draw: vanilla's layer has nothing either
        try {
            GuiGraphics gg = event.getGuiGraphics();
            boolean sized = com.newuniverse.nusmp.client.ui.GuiScale.begin(gg, "boss_bar", gg.guiWidth() / 2f, 0f);     // GUI Size: grows from the top centre
            try {
                draw(gg, mc, events, now, now + event.getPartialTick().getGameTimeDeltaPartialTick(false));
            } finally {
                if (sized) gg.pose().popPose();
            }
            event.setCanceled(true);
        } catch (RuntimeException e) {
            failed = true;                                                    // draw nothing of ours again; whatever was half drawn stays for this frame
            LOGGER.warn("Animated boss bar disabled: drawing failed. The vanilla boss bar is used.", e);
        }
    }

    private static void draw(GuiGraphics g, Minecraft mc, Map<UUID, LerpingBossEvent> events, long now, double time) {
        Font font = mc.font;
        boolean effects = BossBarOptions.shake();
        boolean percent = BossBarOptions.percent();
        int centre = g.guiWidth() / 2, height = g.guiHeight();
        int y = BossBarLayout.START_Y, shatters = SHATTERS_PER_FRAME;
        for (LerpingBossEvent ev : events.values()) {
            int x = centre - BossBarLayout.BAR_W / 2;
            BossBarClientState.Entry en = BossBarClientState.get(ev.getId());
            if (en != null) {
                BossBarNet.Style st = en.style;
                en.anim.update(now, ev.getProgress(), st.phase(), st.primary(), st.secondary(), st.rage());
                en.anim.frame(time);
                boolean mayShatter = shatters > 0 && en.anim.shattering();
                if (mayShatter) shatters--;
                BossBarPainter.paintMod(g, font, ev, en, x, y, time, effects, percent, mayShatter);
            } else {
                BossBarPainter.paintVanilla(g, ev, x, y);
                g.drawString(font, ev.getName(), centre - font.width(ev.getName()) / 2, y - 9, 0xFFFFFF);
            }
            y += BossBarLayout.step(en != null);
            if (!BossBarLayout.continues(y, height)) break;
        }
    }
}
