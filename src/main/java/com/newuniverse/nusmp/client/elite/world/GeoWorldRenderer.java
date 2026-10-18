package com.newuniverse.nusmp.client.elite.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: ONE renderer for everything the world engine draws as a GeoLite model (the mod's own model format, see {@code client/geo}): the two calamity bosses, the Lich's phylactery, the Gaia
 * spike and the void bolt. The arc kit's characters are humanoids with faces and hair painted by a generator for named characters; a golem of grave soil, a robed lich and a spike of earth
 * are not that, so their models are built by {@code tools/elite/gen_world_models.py} with the same GeoLite builder the Zogratis and Foundry models use, and drawn here. What differs between
 * the entities is a small {@link Look} (which clip at which second, the size, a lift, the glow), so the class itself is shared.
 */
public class GeoWorldRenderer<T extends Entity> extends EntityRenderer<T> {
    /** How one kind of entity is drawn. All methods are called every frame on the client thread. */
    public interface Look<E extends Entity> {
        /** The clip name of the model's animation file, or null for the rest pose. */
        String clip(E e, float partial);

        /** The clock of that clip in seconds. */
        float seconds(E e, float partial);

        /** False = skip the frame (a spike that has not risen yet). */
        default boolean visible(E e, float partial) { return true; }

        /** A uniform scale of the model (1 = as built). */
        default float scale(E e, float partial) { return 1f; }

        /** Blocks the model is lifted (a floating lich). */
        default float lift(E e, float partial) { return 0f; }

        /** Degrees the model is turned (default: the body yaw of a living entity, 0 otherwise). */
        default float yaw(E e, float partial) {
            return e instanceof LivingEntity le ? Mth.rotLerp(partial, le.yBodyRotO, le.yBodyRot) : 0f;
        }

        /** The glow layer tint, ARGB; alpha 0 = no glow. */
        default int glow(E e, float partial) { return 0xFFFFFFFF; }

        /** An extra additive shell (a shield), ARGB with alpha; 0 = none. */
        default int shell(E e, float partial) { return 0; }

        default float spin(E e, float partial) { return 0f; }
    }

    private final GeoSpec spec;
    private final Look<T> look;

    public GeoWorldRenderer(EntityRendererProvider.Context ctx, GeoSpec spec, Look<T> look, float shadow) {
        super(ctx);
        this.spec = spec;
        this.look = look;
        this.shadowRadius = shadow;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return spec.texture(); }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (look.visible(e, partial)) {
            int tint = e instanceof LivingEntity le && (le.hurtTime > 0 || le.deathTime > 0) ? 0xFFFF9A9A : 0xFFFFFFFF;
            float s = look.scale(e, partial);
            pose.pushPose();
            pose.translate(0f, look.lift(e, partial), 0f);
            pose.mulPose(Axis.YP.rotationDegrees(-look.yaw(e, partial) + look.spin(e, partial)));
            pose.scale(s, s, s);
            GeoDraw.paint(pose, buffers, spec, look.clip(e, partial), look.seconds(e, partial), GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, tint, look.glow(e, partial));
            int shell = look.shell(e, partial);
            if ((shell >>> 24) != 0) {
                pose.scale(1.07f, 1.04f, 1.07f);
                GeoDraw.paint(pose, buffers, spec.noGlow(), look.clip(e, partial), look.seconds(e, partial), GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT, shell, 0);
            }
            pose.popPose();
        }
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(T e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return e.shouldRender(x, y, z);
    }
}
