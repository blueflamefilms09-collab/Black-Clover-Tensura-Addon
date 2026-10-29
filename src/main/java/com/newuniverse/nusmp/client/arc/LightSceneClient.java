package com.newuniverse.nusmp.client.arc;

import com.newuniverse.nusmp.arc.tier.LightFrame;
import com.newuniverse.nusmp.arc.tier.LightNet;
import com.newuniverse.nusmp.arc.tier.LightScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the CLIENT half of the Death Note gag: draws the anime-style cinematic (black bars, speed lines, the red eyes, panel bursts, the rising pen, the green pool of light, the page with the name
 * being written, a screen shake) from the plan of {@link LightFrame}, which is pure and bounded (at most {@link LightFrame#MAX_QUADS} rectangles a frame, no allocation: the plan object, the two cached
 * text strings and the texts are built once). Nothing is decided here: the server's packet ({@link LightNet}) starts, cancels or ends the drawing, and the scene also stops by itself when the
 * viewer is more than {@link #MAX_VIEW} blocks from Light, Light is gone, the world changed, or {@link LightScene#CLIENT_HARD_LIMIT} ticks passed (a lost packet can never leave bars on the screen).
 * A bystander gets the bars and the captions only; the target gets the whole cinematic. Client only; the common packet class reaches this through its {@link LightNet#CLIENT} hook.
 */
public final class LightSceneClient {
    private LightSceneClient() {}

    /** The viewer is farther than this from Light: nothing is drawn (the effect of a far player is skipped). */
    public static final double MAX_VIEW = 40.0;
    private static final int FADE_TICKS = 10;

    private static final LightFrame FRAME = new LightFrame();
    private static final Component CAPTION_CHIP = Component.translatable("nusmp.light_yagami.caption.chip");
    private static final Component CAPTION_WRITING = Component.translatable("nusmp.light_yagami.caption.writing");
    private static final Component CARD_CHIP = Component.translatable("nusmp.light_yagami.card.chip");
    private static final Component CARD_PEN = Component.translatable("nusmp.light_yagami.card.pen");
    private static final Component CARD_NOTE = Component.translatable("nusmp.light_yagami.card.note");

    private static boolean active, target;
    private static int light, targetId, age, out;
    private static String name = "", typed = "";
    private static int typedCount = -1;
    private static float shakeX;

    public static void init(IEventBus modBus) {
        LightNet.CLIENT = LightSceneClient::onScene;
        NeoForge.EVENT_BUS.addListener(LightSceneClient::onTick);
        NeoForge.EVENT_BUS.addListener(LightSceneClient::onGui);
        NeoForge.EVENT_BUS.addListener(LightSceneClient::onCamera);
    }

    /** The server's packet (already validated by the common side). */
    private static void onScene(LightNet.Scene s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (s.op() == LightNet.OP_START) {
            active = true;
            light = s.light();
            targetId = s.target();
            target = s.role() == LightNet.ROLE_TARGET || mc.player.getId() == s.target();
            age = 0;
            out = 0;
            typedCount = -1;
            Entity who = mc.level.getEntity(targetId);
            name = who == null ? "" : who.getName().getString();
        } else if (active && s.light() == light && out == 0) {
            out = 1;                                                       // cancel or end: the bars slide away
        }
    }

    private static void onTick(ClientTickEvent.Post e) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stop();
            return;
        }
        if (mc.isPaused()) return;
        if (out > 0) {
            if (++out > FADE_TICKS) stop();
            return;
        }
        Entity l = mc.level.getEntity(light);
        if (l == null || !l.isAlive() || mc.player.distanceToSqr(l) > MAX_VIEW * MAX_VIEW || ++age > LightScene.CLIENT_HARD_LIMIT) out = 1;
    }

    private static void stop() {
        active = false;
        out = 0;
        shakeX = 0f;
    }

    private static void onGui(RenderGuiLayerEvent.Post e) {
        if (!active || !VanillaGuiLayers.CAMERA_OVERLAYS.equals(e.getName())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        float fade = out == 0 ? 1f : Math.max(0f, 1f - out / (float) FADE_TICKS);
        LightFrame f = FRAME.plan(Math.min(age, LightScene.TOTAL_TICKS) + e.getPartialTick().getGameTimeDeltaPartialTick(false), w, h, target, fade);
        shakeX = f.shakeX;
        int sx = Math.round(f.shakeX), sy = Math.round(f.shakeY);
        for (int i = 0; i < f.n; i++) {
            int dx = i < 2 ? 0 : sx, dy = i < 2 ? 0 : sy;                   // the two bars never shake (the screen would show a gap)
            g.fill(f.x0[i] + dx, f.y0[i] + dy, f.x1[i] + dx, f.y1[i] + dy, f.argb[i]);
        }
        int barBottom = f.n > 0 ? f.y1[0] : 0;
        var font = mc.font;
        if (f.caption != LightFrame.CAPTION_NONE) {
            Component c = f.caption == LightFrame.CAPTION_CHIP ? CAPTION_CHIP : CAPTION_WRITING;
            g.drawCenteredString(font, c, w / 2, Math.max(2, barBottom / 2 - 4), 0xFFFFFF);
        }
        if (target && f.card != LightFrame.CARD_NONE) {
            Component c = f.card == LightFrame.CARD_CHIP ? CARD_CHIP : f.card == LightFrame.CARD_PEN ? CARD_PEN : CARD_NOTE;
            float s = 1.5f + 3.0f * f.cardGrow;
            g.pose().pushPose();
            g.pose().translate(w / 2f + sx, h * 0.30f + sy, 0f);
            g.pose().scale(s, s, 1f);
            g.drawCenteredString(font, c, 0, 0, f.card == LightFrame.CARD_CHIP ? 0xFFE9A0 : 0xFF3030);
            g.pose().popPose();
        }
        if (target && f.page && !name.isEmpty()) {
            int n = LightFrame.charsWritten(f.write, name.length());
            if (n != typedCount) {
                typedCount = n;
                typed = name.substring(0, n);
            }
            g.pose().pushPose();
            g.pose().translate((f.pageX0 + f.pageX1) / 2f + sx, (f.pageY0 + f.pageY1) / 2f - 4f + sy, 0f);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, typed, 0, 0, 0x101010);
            g.pose().popPose();
        }
    }

    /** The view shakes with the cinematic (the target only, and only while the scene is drawn). */
    private static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (!active || !target || shakeX == 0f) return;
        e.setRoll(e.getRoll() + shakeX * 0.12f);
        e.setYaw(e.getYaw() + shakeX * 0.05f);
    }
}
