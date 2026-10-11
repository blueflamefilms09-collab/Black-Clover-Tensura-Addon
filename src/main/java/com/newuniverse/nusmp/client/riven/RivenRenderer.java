package com.newuniverse.nusmp.client.riven;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.riven.RivenBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Riven Remake: the geo model (assets/nusmp/geo/entity/riven_remake.geo.json, built by tools/gen_riven_remake.py) drawn with client.geo.GeoDraw, plus
 * the emissive glow (eyes, emblem, lightning, grimoire). Phase 3 swaps to the Final Form texture on the same model, so the boss bar never changes
 * entity. The clip comes from RivenStatePayload (casts and phase changes); otherwise death, hit, walk or idle by what the entity is doing.
 */
public class RivenRenderer extends EntityRenderer<RivenBossEntity> {
    static final GeoSpec BASE = GeoSpec.of("riven", "remake");
    static final GeoSpec FINAL = BASE.skin("riven_remake_final");

    public RivenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.RIVEN_REMAKE.get(), RivenRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(RivenBossEntity e) { return BASE.texture(); }

    @Override
    public void render(RivenBossEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        GeoSpec spec = e.clientPhase() >= 3 ? FINAL : BASE;
        float age = e.tickCount + partial;
        String anim;
        float secs;
        GeoAnim anims = GeoModels.animations(spec.animations());
        if (e.deathTime > 0 || e.isDeadOrDying()) { anim = "death"; secs = (e.deathTime + partial) / 20f; }
        else if (!e.clientClip().isEmpty() && playing(anims, e.clientClip(), e.clientClipAge() + partial)) { anim = e.clientClip(); secs = (e.clientClipAge() + partial) / 20f; }
        else if (e.hurtTime > 0) { anim = "hit"; secs = (e.hurtDuration - e.hurtTime + partial) / 20f; }
        else if (e.walkAnimation.speed() > 0.05f) { anim = "walk"; secs = age / 20f; }
        else { anim = "idle"; secs = age / 20f; }
        float glow = 0.75f + 0.25f * Mth.sin(age * (0.1f + 0.05f * e.clientPhase()));
        int g = (int) (255 * glow);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
        GeoDraw.paint(pose, buffers, spec, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0xFF000000 | (g << 16) | (g << 8) | g);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** A one-shot clip is over when its length has passed; a looping one plays until the next payload. */
    private static boolean playing(GeoAnim anims, String name, float ageTicks) {
        if (anims == null) return false;
        GeoAnim.Clip c = anims.clip(name);
        return c != null && (c.loop || ageTicks / 20f < c.length + 0.05f);
    }
}
