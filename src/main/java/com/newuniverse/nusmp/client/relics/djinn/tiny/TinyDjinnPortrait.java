package com.newuniverse.nusmp.client.relics.djinn.tiny;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The head of the Tiny Djinn in the circle of the Djinn panel during a Lesser Summon, in place of the silhouette: the emblem is covered by a dark disc with the Djinn's
 * rim, and the head of the same model (its hair, crown, horns, ears, brow; the lids blink) turns gently in it. It draws only while the config
 * {@code djinn.tinyPortrait} is true and the model exists; any failure leaves the old silhouette (the panel's own emblem) as it was.
 */
public final class TinyDjinnPortrait {
    private TinyDjinnPortrait() {}

    private static final float RADIUS = 23f, HEAD_SCALE = 40f, HEAD_CENTRE_BLOCKS = 0.70f;
    private static final GeoModelData[] SOURCE = new GeoModelData[Djinn.values().length], VIEW = new GeoModelData[Djinn.values().length];

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** The head-only view of a Djinn's model (the head bone and everything on it), rebuilt when the resources are reloaded. */
    private static GeoModelData head(Djinn d) {
        GeoModelData full = GeoModels.model(rl(TinyDjinnAssets.model(d)));
        int o = d.ordinal();
        if (VIEW[o] == null || SOURCE[o] != full) {
            GeoModelData.Bone head = full.bones.get("head");
            if (head == null) return null;
            GeoModelData v = full.view(b -> false);
            v.roots.add(head);
            SOURCE[o] = full;
            VIEW[o] = v;
        }
        return VIEW[o];
    }

    /**
     * @param cx,cy the centre of the circle in the GUI's (scaled) pixels; {@code ticks} the panel's clock
     * @return true if the head was drawn (the disc covered the silhouette), false if nothing changed
     */
    public static boolean draw(GuiGraphics g, Djinn d, boolean dark, int cx, int cy, float ticks) {
        if (!TinyDjinnConfig.enabled() || !TinyDjinnConfig.portrait()) return false;
        GeoModelData head = head(d);
        if (head == null || head.roots.isEmpty()) return false;
        try {
            disc(g, cx, cy, d, dark);
            PoseStack ps = g.pose();
            ps.pushPose();
            try {
                ps.translate(cx, cy + 5f, 120f);
                ps.scale(HEAD_SCALE, -HEAD_SCALE, HEAD_SCALE);
                ps.mulPose(Axis.YP.rotationDegrees(16f * (float) Math.sin(ticks * 0.045)));
                ps.mulPose(Axis.XP.rotationDegrees(-6f));
                ps.translate(0f, -HEAD_CENTRE_BLOCKS, 0f);
                GeoAnim anims = GeoModels.animations(rl(TinyDjinnAssets.ANIMATION));
                GeoAnim.Clip clip = anims == null ? null : anims.clip("idle");
                float seconds = ticks / 20f;
                MultiBufferSource.BufferSource buffers = g.bufferSource();
                GeoDraw.draw(ps, buffers.getBuffer(AuraRender.translucent(rl(TinyDjinnAssets.texture(d, dark)))), head, clip, seconds, GeoDraw.Space.PROP,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                GeoDraw.draw(ps, buffers.getBuffer(AuraRender.additive(rl(TinyDjinnAssets.glow(d, dark)))), head, clip, seconds, GeoDraw.Space.PROP,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xD0FFFFFF);
                g.flush();
            } finally {
                ps.popPose();
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** A dark disc with a rim in the Djinn's light, drawn row by row (the panel draws everything as rectangles). */
    private static void disc(GuiGraphics g, int cx, int cy, Djinn d, boolean dark) {
        int light = dark ? d.glow() : d.color();
        int rim = 0xB0000000 | light;
        int fill = 0xF2000000 | scaleRgb(light, 0.16f);
        for (int pass = 0; pass < 2; pass++) {
            float r = pass == 0 ? RADIUS + 1.5f : RADIUS;
            int col = pass == 0 ? rim : fill;
            int ri = (int) Math.ceil(r);
            for (int dy = -ri; dy <= ri; dy++) {
                float hw = (float) Math.sqrt(Math.max(0f, r * r - (dy + 0.5f) * (dy + 0.5f)));
                if (hw <= 0f) continue;
                g.fill(cx - (int) hw, cy + dy, cx + (int) hw, cy + dy + 1, col);
            }
        }
    }

    private static int scaleRgb(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k), gr = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        return (r << 16) | (gr << 8) | b;
    }
}
