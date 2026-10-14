package com.newuniverse.nusmp.client.relics.worn;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a {@link WornRenderer} is given for one relic on one player in one frame.
 *
 * @param stack        the worn relic's stack (its def is {@code Relics.defOf(stack)})
 * @param pose         the layer's pose stack (vanilla layer space: 16 model units to a block, y down, front is -z)
 * @param buffers      where to draw
 * @param light        the packed light of the wearer
 * @param model        the parent player model, already posed (head, body and arm parts carry this frame's rotation, crouch offset and swing)
 * @param player       the wearer
 * @param partialTick  the partial tick of this frame
 * @param ageInTicks   the wearer's age in ticks with the partial tick (the layer's own clock)
 * @param netHeadYaw   head yaw relative to the body, degrees
 * @param headPitch    head pitch, degrees
 * @param firstPerson  true if this is the local player seen from their own eyes (renderers draw nothing then)
 */
public record WornCtx(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> model,
                      AbstractClientPlayer player, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, boolean firstPerson) {
    /** Does the wearer have the slim (Alex) arms? */
    public boolean slim() { return player.getSkin().model() == PlayerSkin.Model.SLIM; }
}
