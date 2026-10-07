package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 0.53: everything an {@link AuraPainter} needs. The pose is the player renderer's model space at the time layers draw: origin at
 * the feet (y points DOWN in model space: use {@code pose.translate(0, -h, 0)} to go h blocks up from the feet, in block units),
 * the model already posed for this frame ({@code model}), so drawing it again, or a copy of it, lines up with the body.
 *
 * @param t      ticks since the aura began (with the partial tick)
 * @param left   ticks until it ends
 * @param total  its whole length in ticks
 * @param style  the style number the spell passed to PlayerAuras.set
 * @param age    the renderer's animation clock (tickCount + partial tick), good for sine waves
 * @param fade   0..1: grows in over the first 8 ticks and out over the last 8
 */
public record AuraContext(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, PoseStack pose, MultiBufferSource buffers,
                          int light, float partial, float age, float t, float left, float total, int style, float limbSwing, float limbSwingAmount,
                          float netHeadYaw, float headPitch, float fade) {}
