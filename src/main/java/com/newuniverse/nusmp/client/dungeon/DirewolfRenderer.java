package com.newuniverse.nusmp.client.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.dungeon.DirewolfEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Direwolf of the Living Dungeon in its own model (tools/gen_dungeon_mobs.py -> assets/nusmp/geo/entity/dungeon_direwolf.geo.json, its animations
 * and textures), drawn with client.geo.GeoDraw like the Tower Wizard: a huge scarred dire wolf, blue-grey fur, pale paws, a dark mane along the spine,
 * lower fangs and glowing amber eyes (the additive glow layer, pulsing). Clips: idle, walk, run (while it hunts and moves), attack (a lunge with open
 * jaws, while it hunts and stands), sleep (a full apex predator lies curled up), hurt (a flinch). The walk and run clips are driven by the distance the
 * wolf has walked, so its legs never slide. It turns with its body yaw.
 */
public class DirewolfRenderer extends EntityRenderer<DirewolfEntity> {
    private static final GeoSpec WOLF = GeoSpec.of("dungeon", "direwolf");
    private static final float WALK_SECONDS = 1.0f, RUN_SECONDS = 0.55f;     // the length of one stride cycle of the walk and run clips
    private static final float CYCLE = 9.4248f;                              // the walk-animation position that makes one stride (2 pi / 0.6662, vanilla's leg swing)

    public DirewolfRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.9f;
        this.shadowStrength = 0.7f;
    }

    @Override
    public ResourceLocation getTextureLocation(DirewolfEntity e) {
        return WOLF.texture();
    }

    @Override
    public void render(DirewolfEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        boolean moving = e.walkAnimation.speed() > 0.05f;
        float stride = e.walkAnimation.position(partial) % CYCLE;
        String anim;
        float secs;
        if (e.deathTime > 0) { anim = "idle"; secs = age / 20f; }
        else if (e.hurtTime > 0) { anim = "hurt"; secs = (e.hurtDuration - e.hurtTime + partial) / 20f; }
        else if (e.asleep()) { anim = "sleep"; secs = age / 20f; }
        else if (e.isAggressive() && moving) { anim = "run"; secs = stride / CYCLE * RUN_SECONDS; }
        else if (e.isAggressive()) { anim = "attack"; secs = age / 20f; }
        else if (moving) { anim = "walk"; secs = stride / CYCLE * WALK_SECONDS; }
        else { anim = "idle"; secs = age / 20f; }
        float pulse = (e.isAggressive() ? 0.9f : 0.75f) + 0.2f * Mth.sin(age * 0.19f);
        int g = Math.min(255, (int) (255 * pulse));
        int glow = 0xFF000000 | (g << 16) | (g << 8) | g;
        int tint = e.hurtTime > 0 || e.deathTime > 0 ? 0xFFFF9A9A : 0xFFFFFFFF;       // a red flash when it is hit
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
        if (e.deathTime > 0) {                                                       // it falls onto its side, like any dying mob
            float f = (float) Math.sqrt(Math.max(0f, (e.deathTime + partial - 1f) / 20f * 1.6f));
            pose.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1f) * 90f));
        }
        GeoDraw.paint(pose, buffers, WOLF, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, tint, glow);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                          // the name tag
    }
}
