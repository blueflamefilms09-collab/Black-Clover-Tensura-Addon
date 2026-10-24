package com.newuniverse.nusmp.client.companion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.companion.gs.BubbleStyle;
import com.newuniverse.nusmp.client.companion.gs.CompanionStyle;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;
import com.newuniverse.nusmp.client.ui.gs.GsUser;
import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.companion.talk.BubbleRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Why: draws the speech bubbles the server sends ({@code Bubble.say}) over heads. It is a client-only holder (the common code reaches it through {@code CompanionNet.clientBubble}) and it is cheap by construction: at
 * most {@link BubbleRules#MAX_SHOWN} bubbles are kept (the oldest goes first), each is wrapped to at most four lines ONCE, only those within {@link BubbleRules#DRAW_RANGE} blocks of the camera and not on an invisible
 * entity are drawn, one text batch per line (a name-tag-like line with a dark background), and expired ones are dropped as they are met and every two seconds. Switched off by the client option
 * {@code companions.bubbles}. The worst case per frame is 24 bubbles of 4 lines each, 96 short text draws, and in practice a handful. In the Great Spirit look (client option {@code companions.screenStyle}) a bubble adds its speaker's name and a pointer (6 draws at most), shows 2 lines instead of 4 beyond 12 blocks, and skips itself while it is nearly faded out: 144 draws at the very worst.
 */
public final class BubbleClient {
    private BubbleClient() {}

    private static final class Line {
        final Component line;
        final long until, from;
        List<FormattedCharSequence> wrapped;
        /** The speaker's name in italics (Great Spirit look only), built once with the wrapped lines. */
        FormattedCharSequence name;

        Line(Component line, long until, long from) {
            this.line = line;
            this.until = until;
            this.from = from;
        }
    }

    private static final Map<Integer, Line> SHOWN = new HashMap<>();
    private static final int WIDTH = 110, MAX_LINES = 4;

    static void onLine(CompanionNet.BubbleLine p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !CompanionClientConfig.bubbles()) return;
        long until = mc.level.getGameTime() + Math.max(1, Math.min(p.ticks(), BubbleRules.MAX_LIFE));
        if (SHOWN.size() >= BubbleRules.MAX_SHOWN && !SHOWN.containsKey(p.entityId())) {
            int oldest = -1;
            long soonest = Long.MAX_VALUE;
            for (Map.Entry<Integer, Line> en : SHOWN.entrySet()) if (en.getValue().until < soonest) {
                soonest = en.getValue().until;
                oldest = en.getKey();
            }
            SHOWN.remove(oldest);
        }
        SHOWN.put(p.entityId(), new Line(p.line(), until, mc.level.getGameTime()));
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (SHOWN.isEmpty() || mc.level == null || mc.level.getGameTime() % 40 != 0) return;
        long now = mc.level.getGameTime();
        SHOWN.values().removeIf(b -> now >= b.until);
    }

    static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { SHOWN.clear(); }

    static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SHOWN.isEmpty() || !CompanionClientConfig.bubbles()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        long now = level.getGameTime();
        Camera cam = e.getCamera();
        Vec3 camPos = cam.getPosition();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;
        boolean any = false;
        boolean gs = CompanionScreens.style() == CompanionStyle.GREAT_SPIRIT;
        GsSkin skin = gs ? GsUser.skin() : GsSkin.B;
        for (Iterator<Map.Entry<Integer, Line>> it = SHOWN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Line> en = it.next();
            Line b = en.getValue();
            if (now >= b.until) {
                it.remove();
                continue;
            }
            Entity ent = level.getEntity(en.getKey());
            if (ent == null || ent.isInvisible() || !ent.isAlive()) continue;
            Vec3 at = ent.getPosition(pt);
            double dx = at.x - camPos.x, dy = at.y + ent.getBbHeight() + 0.75 - camPos.y, dz = at.z - camPos.z;
            double dist2 = dx * dx + dy * dy + dz * dz;
            if (!BubbleRules.inDrawRange(dist2)) continue;
            if (b.wrapped == null) {
                List<FormattedCharSequence> all = font.split(Component.literal(BubbleRules.cut(b.line.getString())), WIDTH);
                b.wrapped = all.size() > MAX_LINES ? all.subList(0, MAX_LINES) : all;
                b.name = Component.literal(BubbleRules.cut(ent.getName().getString())).withStyle(ChatFormatting.ITALIC).getVisualOrderText();
            }
            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(cam.rotation());
            pose.scale(-0.025f, -0.025f, 0.025f);
            if (gs) {
                float tNow = now + pt;
                float opacity = BubbleStyle.fade(b.until - tNow);
                if (BubbleStyle.invisible(opacity)) {
                    pose.popPose();
                    continue;
                }
                float sc = BubbleStyle.pop(tNow - b.from);
                pose.scale(sc, sc, sc);
                drawGs(font, b, pose.last().pose(), buffers, skin, opacity, dist2);
                pose.popPose();
                any = true;
                continue;
            }
            Matrix4f m = pose.last().pose();
            int n = b.wrapped.size();
            for (int i = 0; i < n; i++) {
                FormattedCharSequence l = b.wrapped.get(i);
                font.drawInBatch(l, -font.width(l) / 2f, (i - n) * 10f, 0xFFFFFFFF, false, m, buffers, Font.DisplayMode.NORMAL, 0xC0101018, 0xF000F0);
            }
            pose.popPose();
            any = true;
        }
        if (any) buffers.endBatch();
    }

    /**
     * The Great Spirit look of one bubble: the speaker's name in italics on a darker card, the words on a parchment (skin B) or dark glass (skin C) card, a small pointer under it, popping in and fading out.
     * Two lines instead of four beyond 12 blocks. At most {@code MAX_LINES + 2} text draws per bubble.
     */
    private static void drawGs(Font font, Line b, Matrix4f m, MultiBufferSource.BufferSource buffers, GsSkin skin, float opacity, double dist2) {
        int n = BubbleStyle.lines(dist2, b.wrapped.size());
        int text = BubbleStyle.textColor(skin, opacity), card = BubbleStyle.cardColor(skin, opacity);
        for (int i = 0; i < n; i++) {
            FormattedCharSequence l = b.wrapped.get(i);
            font.drawInBatch(l, -font.width(l) / 2f, (i - n) * 10f, text, false, m, buffers, Font.DisplayMode.NORMAL, card, 0xF000F0);
        }
        if (b.name != null) font.drawInBatch(b.name, -font.width(b.name) / 2f, (-n - 1) * 10f, BubbleStyle.nameColor(skin, opacity), false, m, buffers, Font.DisplayMode.NORMAL, BubbleStyle.nameCardColor(skin, opacity), 0xF000F0);
        font.drawInBatch("v", -font.width("v") / 2f, -1f, BubbleStyle.tailColor(skin, opacity), false, m, buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
    }

    /** How many bubbles are kept (the preview and the tests). */
    public static int shown() { return SHOWN.size(); }
}
