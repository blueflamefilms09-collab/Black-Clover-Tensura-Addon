package com.newuniverse.nusmp.client.shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

/** Preview stub: the Devil Unite overlays are GeoLite models; tools/mode_preview/preview.py renders them from the .geo.json files itself. */
public final class DevilOverlay {
    public static void draw(ModeArmor.Mode mode, PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> m, float now, float k, float swing) {}
}
