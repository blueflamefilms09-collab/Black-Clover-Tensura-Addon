package com.newuniverse.nusmp.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

import java.util.List;

/** 0.100: shared drawing helpers of the Codex: palette, wrapped text, section headers and the posed player figure. */
public final class UiDraw {
    private UiDraw() {}

    public static final int COVER = 0xFF241636, COVER_EDGE = 0xFF120A1C, GOLD = 0xFFE8C468, PAPER = 0xFFE9DCBB, PAPER_SHADE = 0xFFD9C79A,
            INK = 0xFF3A2A1A, INK_DIM = 0xFF6B5A44, ACCENT = 0xFF7A5CFF, WARN = 0xFF9A2A2A, RULE = 0xFFB7A47C;

    public static int withAlpha(int argb, int a) { return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, a)) << 24); }

    /** Draws wrapped text and returns the y just below it. */
    public static int wrapped(GuiGraphics g, Font font, String text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), Math.max(20, width));
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, x, y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    /** The height wrapped text would take. */
    public static int wrappedHeight(Font font, String text, int width) {
        return font.split(Component.literal(text), Math.max(20, width)).size() * (font.lineHeight + 1);
    }

    /** A section header: small gold-on-ink title with a rule under it. Returns the y below it. */
    public static int header(GuiGraphics g, Font font, String title, int x, int y, int width) {
        g.drawString(font, title, x, y, GOLD_INK, false);
        g.hLine(x, x + width, y + font.lineHeight + 1, RULE);
        return y + font.lineHeight + 5;
    }

    public static final int GOLD_INK = 0xFF8A5E10;

    /**
     * Draws the player standing (or sitting, bowing, floating ...) at the centre (cx, cy) of a box of the given height, turned by
     * 'yaw' degrees. The pose is whatever {@link UiPoses} is blending to; the figure is the real, fully dressed player model.
     */
    public static void player(GuiGraphics g, LivingEntity e, int cx, int cy, int height, float yaw) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        float scale = height / 2.35f;
        float bb = e.getBbHeight();
        float bodyO = e.yBodyRot, bodyOO = e.yBodyRotO, yRot = e.getYRot(), yRotO = e.yRotO, xRot = e.getXRot(), xRotO = e.xRotO,
                headO = e.yHeadRot, headOO = e.yHeadRotO;
        e.yBodyRot = 180f; e.yBodyRotO = 180f; e.setYRot(180f); e.yRotO = 180f; e.setXRot(0f); e.xRotO = 0f; e.yHeadRot = 180f; e.yHeadRotO = 180f;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 50.0);
        ps.scale(scale, scale, -scale);
        ps.translate(0.0, bb / 2.0 - UiPoses.lift(), 0.0);
        ps.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupForEntityInInventory();
        dispatcher.setRenderShadow(false);
        UiPoses.subject = e;
        try {
            dispatcher.render(e, 0.0, 0.0, 0.0, 0.0f, 1.0f, ps, g.bufferSource(), 0xF000F0);
            g.flush();
        } finally {
            UiPoses.subject = null;
            dispatcher.setRenderShadow(true);
            ps.popPose();
            Lighting.setupFor3DItems();
            e.yBodyRot = bodyO; e.yBodyRotO = bodyOO; e.setYRot(yRot); e.yRotO = yRotO; e.setXRot(xRot); e.xRotO = xRotO; e.yHeadRot = headO; e.yHeadRotO = headOO;
        }
    }
}
