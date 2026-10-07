package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 0.53: draws one kind of {@link MagicPropEntity}. The pose is at the prop's feet (its position), already turned by its yaw; units
 * are blocks, y up. Scale yourself with e.scale(). Use {@link PropDraw} for boxes, spheres, cylinders, tori, crystals and tubes, and a
 * render type from {@link com.newuniverse.nusmp.client.aura.AuraRender} (translucent for glass and bubbles, additive for glows,
 * cutout for solid things).
 *
 * @param age  e.tickCount + partial tick: the animation clock
 */
@FunctionalInterface
public interface PropPainter {
    void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light);
}
