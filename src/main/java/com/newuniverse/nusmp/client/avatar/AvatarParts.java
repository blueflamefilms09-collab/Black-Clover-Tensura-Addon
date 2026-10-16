package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Why: the world renderer and the creation screen's live preview must show exactly the same Avatar, so the drawing of the 3D parts (hair, horns,
 * wings, the 3D pieces of an outfit, the aura ring) lives here once and both call it. Every part is a GeoLite model made by
 * tools/avatar/gen_avatar_client_assets.py, loaded once by GeoModels and shared; nothing is built per frame except pose transforms. Works in vanilla's
 * layer frame (y down, front -z, origin at the neck), the frame of the player model it is given.
 */
public final class AvatarParts {
    private AvatarParts() {}

    /** The outfits that have 3D pieces (must match EXTRA_OUTFITS of the generator). */
    private static final int[] EXTRA_OUTFITS = {1, 2, 3, 5, 8, 10, 11};
    private static final GeoSpec[] HAIR = new GeoSpec[AppearanceData.HAIR_COUNT], HORN = new GeoSpec[AppearanceData.HORN_COUNT], WING = new GeoSpec[AppearanceData.WING_COUNT],
            EXTRA = new GeoSpec[AppearanceData.OUTFIT_COUNT];
    public static final GeoSpec AURA = GeoSpec.of("avatar", "aura_ring");
    private static final int WHITE = 0xFFFFFFFF, FB = PropDraw.FULL_BRIGHT;

    static {
        for (int i = 0; i < HAIR.length; i++) HAIR[i] = GeoSpec.of("avatar", "hair_" + i);
        for (int i = 0; i < HORN.length; i++) HORN[i] = GeoSpec.of("avatar", "horn_" + i);
        for (int i = 0; i < WING.length; i++) WING[i] = GeoSpec.of("avatar", "wing_" + i);
        for (int n : EXTRA_OUTFITS) EXTRA[n] = GeoSpec.of("avatar", "extra_" + n);
    }

    /**
     * Draws hair, horns, outfit extras and wings on a posed humanoid model.
     * @param seconds the animation clock (age / 20), @param flap wing speed factor (1 on the ground, more in the air)
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, HumanoidModel<?> model, AppearanceData look, float seconds, float flap, float glowPulse) {
        int glow = ((int) (150 + 90 * glowPulse) << 24) | 0xFFFFFF;
        if (look.hairId() >= 0 && look.hairId() < HAIR.length) {
            pose.pushPose();
            model.head.translateAndRotate(pose);
            GeoDraw.paint(pose, buffers, HAIR[look.hairId()], "idle", seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, light, WHITE, 0);
            if (look.hornId() >= 0 && look.hornId() < HORN.length)
                GeoDraw.paint(pose, buffers, HORN[look.hornId()], null, seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, light, WHITE, glow);
            pose.popPose();
        } else if (look.hornId() >= 0 && look.hornId() < HORN.length) {
            pose.pushPose();
            model.head.translateAndRotate(pose);
            GeoDraw.paint(pose, buffers, HORN[look.hornId()], null, seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, light, WHITE, glow);
            pose.popPose();
        }
        boolean extra = look.outfitId() >= 0 && look.outfitId() < EXTRA.length && EXTRA[look.outfitId()] != null;
        boolean wings = look.wingId() >= 0 && look.wingId() < WING.length;
        if (extra || wings) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            if (extra) GeoDraw.paint(pose, buffers, EXTRA[look.outfitId()], "idle", seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, light, WHITE, glow);
            if (wings) GeoDraw.paint(pose, buffers, WING[look.wingId()], "idle", seconds * flap, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, light, WHITE, glow);
            pose.popPose();
        }
    }

    /**
     * The aura ring on the ground: the pose's origin is the Avatar's feet (y up, blocks). The ring is white and tinted by the aura colour, drawn
     * additive and full bright; it grows with the avatar's scale (the caller scales the pose).
     */
    public static void drawAura(PoseStack pose, MultiBufferSource buffers, int auraRgb, float alpha, float seconds) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        if (a < 4) return;
        GeoDraw.paint(pose, buffers, AURA, "idle", seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FB, (a << 24) | (auraRgb & 0xFFFFFF), 0);
    }
}
