package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.client.playermodel.LegacyHair;
import com.newuniverse.nusmp.client.playermodel.PlayerAssets;
import com.newuniverse.nusmp.client.playermodel.PlayerBody;
import com.newuniverse.nusmp.client.playermodel.PlayerLook;
import com.newuniverse.nusmp.client.playermodel.PlayerModelRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Why: the player-made Avatar on the player-model kit (1.94.0). It was already drawn on a player model with a skin composed from the creator's choices ({@link AvatarSkinCache}); what changes
 * is that the kit's renderer now draws it, so it gets the vanilla animations, armour, items and elytra from one place, and its hair is the kit's hair system: the creator's old hair id
 * ({@code AppearanceData.hairId}, saved in worlds and unchanged) picks the haircut and its colour through {@link LegacyHair}, the hair has springs and no longer clips. The skin stays the
 * composed texture (the look names none), the horns, wings and outfit pieces stay the 3D parts of {@link AvatarFeatureLayer}, the iris stays glowing ({@link AvatarEyesLayer}) and the aura ring
 * still turns at its feet. The size is the vanilla scale attribute the server sets from the look (the renderer scales by it by itself).
 */
public final class AvatarPlayerBody implements PlayerBody<AvatarEntity> {
    private static final PlayerLook[] LOOKS = new PlayerLook[LegacyHair.COUNT];

    /** The look an Avatar with this old hair id wears: no skin of its own (the composed one is supplied), the hair of the old id. */
    public static synchronized PlayerLook lookOf(int hairId) {
        int i = Math.max(0, Math.min(LegacyHair.COUNT - 1, hairId));
        if (LOOKS[i] == null) {
            LegacyHair.Cut cut = LegacyHair.of(i);
            LOOKS[i] = new PlayerLook("avatar_hair_" + i, "", false, false, PlayerLook.WHITE, cut.style(), cut.rgb(), 1f, 1f, List.of(), "", 0f, 0f);
        }
        return LOOKS[i];
    }

    @Override public PlayerLook look(AvatarEntity e) { return lookOf(AvatarLooks.of(e).hairId()); }

    @Override public ResourceLocation skin(AvatarEntity e) { return AvatarSkinCache.skin(AvatarLooks.of(e), e.raceId()); }

    @Override
    public float cullGrow(AvatarEntity e, PlayerLook look) { return 1.4f * Math.max(1f, e.getScale()); }

    @Override
    public void layers(PlayerModelRenderer.Body<AvatarEntity> body, EntityRendererProvider.Context ctx) {
        body.addLayer(new AvatarFeatureLayer(body));
        body.addLayer(new AvatarEyesLayer(body));
    }

    @Override
    public void decorate(AvatarEntity e, float partial, Decoration deco) {
        if (e.isInvisible()) return;
        AppearanceData look = AvatarLooks.of(e);
        float age = e.tickCount + partial, s = e.getScale();
        boolean frenzy = e.frenzied();
        var pose = deco.pose();
        pose.pushPose();
        pose.scale(s, s, s);
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.12f + e.getId());
        int rgb = frenzy ? 0x7A0A14 : look.auraRgb();
        AvatarParts.drawAura(pose, deco.buffers(), rgb, (frenzy ? 0.85f : 0.40f) + 0.20f * pulse, age / 20f);
        pose.popPose();
    }

    /** Is there a haircut for this old id (for the self-test)? */
    public static boolean hairOk(int hairId) { return PlayerAssets.hair(LegacyHair.of(hairId).style()) != null || LegacyHair.of(hairId).style().equals("bald"); }
}
