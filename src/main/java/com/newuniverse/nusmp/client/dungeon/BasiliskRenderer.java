package com.newuniverse.nusmp.client.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.dungeon.BasiliskEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Basilisk of the Living Dungeon in its own model (tools/gen_dungeon_mobs.py -> assets/nusmp/geo/entity/dungeon_basilisk.geo.json, its animations
 * and textures), drawn with client.geo.GeoDraw: a low, heavy armoured lizard, forest-green and near-black scales, a bone crest down the back, a wedge
 * head and splayed legs. The venom-green eyes and a faint green glow along the crest are the additive glow layer, pulsing. Clips: idle (slow breaths,
 * a tongue flick, a swaying tail), walk (alternating splayed legs, the body undulating; driven by the distance walked), attack (it rears up and snaps,
 * while it hunts), sleep (flat on the ground, the tail coiled), hurt (a flinch). It turns with its body yaw.
 */
public class BasiliskRenderer extends EntityRenderer<BasiliskEntity> {
    private static final GeoSpec BASILISK = GeoSpec.of("dungeon", "basilisk");
    private static final float WALK_SECONDS = 1.4f;                          // the length of one stride cycle of the walk clip
    private static final float CYCLE = 9.4248f;                              // the walk-animation position that makes one stride (2 pi / 0.6662, vanilla's leg swing)

    public BasiliskRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.1f;
        this.shadowStrength = 0.75f;
    }

    @Override
    public ResourceLocation getTextureLocation(BasiliskEntity e) {
        return BASILISK.texture();
    }

    @Override
    public void render(BasiliskEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        boolean moving = e.walkAnimation.speed() > 0.05f;
        float stride = e.walkAnimation.position(partial) % CYCLE;
        String anim;
        float secs;
        if (e.deathTime > 0) { anim = "idle"; secs = age / 20f; }
        else if (e.hurtTime > 0) { anim = "hurt"; secs = (e.hurtDuration - e.hurtTime + partial) / 20f; }
        else if (e.asleep()) { anim = "sleep"; secs = age / 20f; }
        else if (e.isAggressive() && !moving) { anim = "attack"; secs = age / 20f; }
        else if (moving) { anim = "walk"; secs = stride / CYCLE * WALK_SECONDS; }
        else { anim = "idle"; secs = age / 20f; }
        float pulse = (e.isAggressive() ? 0.85f : 0.7f) + 0.2f * Mth.sin(age * 0.13f);       // a slow venomous pulse
        int g = Math.min(255, (int) (255 * pulse));
        int glow = 0xFF000000 | (g << 16) | (g << 8) | g;
        int tint = e.hurtTime > 0 || e.deathTime > 0 ? 0xFFFF9A9A : 0xFFFFFFFF;       // a red flash when it is hit
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
        if (e.deathTime > 0) {                                                       // it falls onto its side, like any dying mob
            float f = (float) Math.sqrt(Math.max(0f, (e.deathTime + partial - 1f) / 20f * 1.6f));
            pose.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1f) * 90f));
        }
        GeoDraw.paint(pose, buffers, BASILISK, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, tint, glow);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                          // the name tag
    }
}
