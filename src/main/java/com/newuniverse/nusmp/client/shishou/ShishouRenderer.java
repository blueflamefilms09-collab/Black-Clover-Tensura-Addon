package com.newuniverse.nusmp.client.shishou;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.ShishouMeshDraw;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.shishou.ShishouEntity;
import com.newuniverse.nusmp.shishou.ShishouRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: draws Shishou's body. The model is the same 3D sword mesh as the one in the hand ({@link ShishouMeshDraw}); the four animation states (idle, aggro, attack, dormant) are the clips of the mod's own
 * animation file {@code animations/entity/shishou_sword.animation.json} (the keyframe format and the parser {@link GeoAnim} of the mod's GeoLite system, which its spirits use; GeckoLib was NOT added: no new
 * dependency, nothing the server needs). The clip's single bone "sword" gives a rotation (degrees: x pitch down, y yaw, z roll about the blade) and a position (pixels: x right, y up, z forward) that are applied
 * to the sword on top of the entity's own yaw and pitch. Per frame: one clip lookup, two track samples into scratch arrays, no allocation; the glow pass only within {@link ShishouMeshDraw#GLOW_RANGE}, nothing
 * beyond {@link ShishouMeshDraw#CULL_RANGE}; a Stealth-veiled sword is drawn translucent.
 */
public final class ShishouRenderer extends EntityRenderer<ShishouEntity> {
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("nusmp", "animations/entity/shishou_sword.animation.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon/shishou.png");
    private static final float[] ROT = new float[3], POS = new float[3];

    public ShishouRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(ShishouEntity e) { return TEXTURE; }

    @Override
    protected boolean shouldShowName(ShishouEntity e) { return false; }

    @Override
    public void render(ShishouEntity e, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ShishouMeshDraw.available()) return;
        double d2 = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(e.getX(), e.getY(), e.getZ());
        if (d2 > ShishouMeshDraw.CULL_RANGE * ShishouMeshDraw.CULL_RANGE) return;
        int state = e.state();
        String clipName = ShishouRules.clip(state);
        float seconds = state == ShishouRules.STATE_ATTACK ? (e.stateAge() + partial) / 20f : (e.tickCount + partial) / 20f;
        float rx = 0f, ry = 0f, rz = 0f, px = 0f, py = 0f, pz = 0f;
        GeoAnim anim = GeoModels.animations(ANIMATION);
        GeoAnim.Clip clip = anim == null ? null : anim.clip(clipName);
        if (clip != null) {
            GeoAnim.BoneAnim bone = clip.bones.get("sword");
            if (bone != null) {
                float t = clip.time(seconds);
                if (bone.rotation != null && bone.rotation.sampleInto(t, ROT)) {
                    rx = ROT[0];
                    ry = ROT[1];
                    rz = ROT[2];
                }
                if (bone.position != null && bone.position.sampleInto(t, POS)) {
                    px = POS[0];
                    py = POS[1];
                    pz = POS[2];
                }
            }
        }
        boolean dormant = state == ShishouRules.STATE_DORMANT;
        float yaw = dormant ? e.getYRot() : Mth.rotLerp(partial, e.yRotO, e.getYRot());
        float pitch = dormant ? 0f : Mth.lerp(partial, e.xRotO, e.getXRot());
        pose.pushPose();
        pose.translate(0.0, dormant ? 0.0 : e.getBbHeight() * 0.5, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));                                          // the entity's yaw: local +z is where it looks
        pose.translate(px / 16f, py / 16f, pz / 16f);                                         // the clip's position (right, up, forward)
        pose.mulPose(Axis.YP.rotationDegrees(ry));
        pose.mulPose(Axis.XP.rotationDegrees(pitch + rx));                                    // pitch down is positive
        pose.mulPose(Axis.ZP.rotationDegrees(rz));                                            // roll about the blade
        pose.mulPose(Axis.XP.rotationDegrees(90f));                                           // lay the upright mesh (tip +y) along +z
        float alpha = e.isVeiled() ? 0.35f : 1f;
        float glow = d2 > ShishouMeshDraw.GLOW_RANGE * ShishouMeshDraw.GLOW_RANGE ? 0f : state == ShishouRules.STATE_DORMANT ? 0.35f : 1f;
        ShishouMeshDraw.draw(pose, buffers, light, alpha, glow, e.tickCount + partial);
        pose.popPose();
    }
}
