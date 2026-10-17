package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.avatar.menu.DiceSpin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Why: the 3D dice of the Identity page. {@link DiceSpin} (pure, tested) decides where each triangle of the twenty-sided die is on the screen and how bright it
 * is; this only paints those triangles into the GUI's own vertex buffer, then the facet edges, then the number on the face that looks at the player. A GUI has no
 * triangle call, so each triangle is a quad whose last corner is repeated, submitted once with each winding order, so it can never be thrown away by back-face
 * culling whichever way the corners come out on screen. The buffer is flushed before and after, so everything drawn earlier stays under the die and everything
 * drawn later stays over it.
 */
final class ForgeDice3D {
    private ForgeDice3D() {}

    /** @param tintRgb the die's colour (0xRRGGBB); @param alpha 0..1 for the whole die; @param numberAlpha 0..1 for the number on the front face */
    static void draw(GuiGraphics g, Font font, DiceSpin.View v, int tintRgb, float alpha, float numberAlpha, float radius) {
        if (alpha <= 0.01f) return;
        g.flush();
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        for (DiceSpin.Tri t : v.tris()) {
            int col = (a << 24) | shaded(tintRgb, t.shade());
            triangle(vc, m, t.x0(), t.y0(), t.x1(), t.y1(), t.x2(), t.y2(), col);
        }
        int edge = (Math.round(a * 0.85f) << 24) | 0xE1D7FF;
        float w = Math.max(0.8f, radius / 38f);
        for (DiceSpin.Seg s : v.edges()) line(vc, m, s.x0(), s.y0(), s.x1(), s.y1(), w, edge);
        g.flush();
        if (numberAlpha > 0.02f) {
            String n = Integer.toString(v.frontNumber());
            float scale = Math.max(0.6f, radius / 15f) * Math.max(0.55f, v.labelScale());
            g.pose().pushPose();
            g.pose().translate(v.labelX(), v.labelY(), 0f);
            g.pose().scale(scale, scale, 1f);
            int tc = (Math.round(255f * Math.max(0f, Math.min(1f, numberAlpha))) << 24) | 0xFFFFFF;
            g.drawString(font, n, -font.width(n) / 2, -font.lineHeight / 2 + 1, tc, true);
            g.pose().popPose();
        }
    }

    /** The tint scaled by a facet's brightness (0..1): never fully black, so a dark facet is still a facet. */
    static int shaded(int rgb, float shade) {
        float k = 0.28f + 0.72f * Math.max(0f, Math.min(1f, shade));
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k + 14 * shade));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * k + 14 * shade));
        int b = Math.min(255, Math.round((rgb & 255) * k + 14 * shade));
        return (r << 16) | (g << 8) | b;
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        vc.addVertex(m, x0, y0, 0f).setColor(argb);
        vc.addVertex(m, x1, y1, 0f).setColor(argb);
        vc.addVertex(m, x2, y2, 0f).setColor(argb);
        vc.addVertex(m, x3, y3, 0f).setColor(argb);
    }

    /** A filled triangle, submitted with both windings so culling cannot hide it. */
    private static void triangle(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
        quad(vc, m, x0, y0, x1, y1, x2, y2, x2, y2, argb);
        quad(vc, m, x0, y0, x2, y2, x1, y1, x1, y1, argb);
    }

    /** A line of the given width as a thin quad (both windings). */
    private static void line(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len * width * 0.5f, ny = dx / len * width * 0.5f;
        quad(vc, m, x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, argb);
        quad(vc, m, x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, argb);
    }
}
