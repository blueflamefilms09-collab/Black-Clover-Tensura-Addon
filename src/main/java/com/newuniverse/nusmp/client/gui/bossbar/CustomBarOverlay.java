package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.bars.BarLayoutMath;
import com.newuniverse.nusmp.client.ui.GuiScale;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the custom boss bars are drawn on {@code RenderGuiLayerEvent.Pre} of vanilla's boss layer, ahead of {@link ModBossBarOverlay} (priority HIGH): when at least one visible boss bar has a custom
 * style (a {@link CustomBarState} whose style is loaded), or one is dying or previewed, this class lays out ALL the bars in one stack, draws the custom ones itself and hands every other bar
 * (vanilla's, other mods', the animated ones of 1.25.0) to {@link ModBossBarOverlay#drawFallback}, then cancels the layer; otherwise it does nothing and the animated overlay (or vanilla) draws as
 * before. {@code bossbar.custom=false} switches it off; if drawing ever throws it disables itself and the old overlay takes over. The stack is scaled by the GUI Size entry {@code boss_bar} and by
 * {@code bossbar.scale}; three or more custom bars at once are drawn compact. A bar whose boss died stays for its death animation after the vanilla event is gone.
 */
final class CustomBarOverlay {
    private CustomBarOverlay() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean failed;
    private static long lastPurge = Long.MIN_VALUE;
    private static final int START_Y = 4;

    static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CustomBarOverlay::onLayer);
    }

    private static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (failed || !VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName()) || CustomBarClient.isEmpty() || !BossBarOptions.custom()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gui == null) return;
        Map<UUID, LerpingBossEvent> events = VanillaBossBars.events(mc.gui.getBossOverlay());
        if (events == null) return;                                           // unreadable: leave everything to the old overlay
        long now = mc.level.getGameTime();
        if (now != lastPurge && now % 10 == 0) {
            lastPurge = now;
            purge(events, now);
            BossBarClientState.purge(events, now);
        }
        // is there anything of ours to draw, and how many bars will it be?
        int custom = 0;
        for (LerpingBossEvent ev : events.values()) {
            CustomBarState st = CustomBarClient.get(ev.getId());
            if (st != null && resolve(st)) custom++;
        }
        for (Iterator<Map.Entry<UUID, CustomBarState>> it = CustomBarClient.entries(); it.hasNext(); ) {
            CustomBarState st = it.next().getValue();
            if ((st.dying || st.preview) && !events.containsKey(st.id) && resolve(st)) custom++;
        }
        if (custom == 0) return;
        if (GuiMove.hidden("boss_bar")) {                                     // GUI Move: the player switched the boss bars off; vanilla's layer must not draw them either
            event.setCanceled(true);
            return;
        }
        try {
            GuiGraphics gg = event.getGuiGraphics();
            float cfg = BossBarOptions.scale();
            // GUI Move + GUI Size: the default rectangle is the stack as the config scale draws it, from the top centre; moved, the stack is drawn at the saved place
            int bw = Math.round(BarLayoutMath.WIDTH * cfg), bh = Math.round(BarLayoutMath.step(BarLayoutMath.compact(custom)) * custom * cfg);
            boolean sized = GuiMove.begin(gg, "boss_bar", gg.guiWidth() / 2 - bw / 2, Math.round(START_Y * cfg), bw, bh, gg.guiWidth() / 2f, 0f);
            boolean scaled = Math.abs(cfg - 1f) > 0.01f;
            if (scaled) GuiScale.around(gg, gg.guiWidth() / 2f, 0f, cfg);
            try {
                draw(gg, mc, events, now, now + event.getPartialTick().getGameTimeDeltaPartialTick(false), custom, GuiScale.hud("boss_bar") * cfg);
            } finally {
                if (scaled) gg.pose().popPose();
                GuiMove.end(gg, sized);
            }
            event.setCanceled(true);
        } catch (RuntimeException e) {
            failed = true;
            LOGGER.warn("Custom boss bars disabled: drawing failed. The animated and vanilla boss bars are used.", e);
        }
    }

    /** True when the state's style is loaded (resolving it on the first look and when the server changes it). */
    private static boolean resolve(CustomBarState st) {
        String id = st.s.style();
        if (!id.equals(st.styleId) || st.style == null) {
            st.styleId = id;
            st.style = BarStyles.get(id);
        }
        return st.style != null;
    }

    private static void draw(GuiGraphics g, Minecraft mc, Map<UUID, LerpingBossEvent> events, long now, double time, int custom, float scale) {
        boolean compact = BarLayoutMath.compact(custom);
        boolean motion = BossBarOptions.shake();
        int centre = g.guiWidth() / 2;
        int height = Math.round(g.guiHeight() / Math.max(0.5f, scale));
        int y = START_Y;
        for (LerpingBossEvent ev : events.values()) {
            if (!BarLayoutMath.fits(y, height)) return;
            CustomBarState st = CustomBarClient.get(ev.getId());
            if (st != null && resolve(st)) {
                st.lastEventName = ev.getName();
                st.lastProgress = ev.getProgress();
                CustomBarPainter.paint(g, mc.font, mc, st, ev.getName(), ev.getProgress(), centre - BarLayoutMath.WIDTH / 2, y, time, now, compact, motion);
                y += BarLayoutMath.step(compact);
            } else {
                y += ModBossBarOverlay.drawFallback(g, mc, ev, y + 9, now, time);
            }
        }
        for (Iterator<Map.Entry<UUID, CustomBarState>> it = CustomBarClient.entries(); it.hasNext(); ) {
            CustomBarState st = it.next().getValue();
            if (events.containsKey(st.id) || !(st.dying || st.preview) || !resolve(st)) continue;
            if (!BarLayoutMath.fits(y, height)) return;
            float hp = st.preview ? st.previewHp : st.dying ? 0f : st.lastProgress;
            Component name = st.lastEventName;
            CustomBarPainter.paint(g, mc.font, mc, st, name, hp, centre - BarLayoutMath.WIDTH / 2, y, time, now, compact, motion);
            y += BarLayoutMath.step(compact);
        }
    }

    /** Forgets a bar whose vanilla event is gone (after a grace period, a dying bar after its animation, a preview after its time). */
    private static void purge(Map<UUID, LerpingBossEvent> events, long now) {
        for (Iterator<Map.Entry<UUID, CustomBarState>> it = CustomBarClient.entries(); it.hasNext(); ) {
            CustomBarState st = it.next().getValue();
            boolean present = events.containsKey(st.id);
            if (st.preview) {
                if (now > st.previewUntil) it.remove();
            } else if (st.dying) {
                if (!Double.isNaN(st.deathTime) && now - st.deathTime > BarLayoutMath.DEATH_TICKS + 4) it.remove();
                else if (!present && Double.isNaN(st.deathTime) && now - st.arrivedAt > BarLayoutMath.DYING_KEEP_TICKS) it.remove();
            } else if (present) {
                st.arrivedSeen = now;
            } else if (now - Math.max(st.arrivedAt, st.arrivedSeen) > 200) {
                it.remove();                                                  // its bar is gone and no clear came: a boss that unloaded
            }
        }
    }
}
