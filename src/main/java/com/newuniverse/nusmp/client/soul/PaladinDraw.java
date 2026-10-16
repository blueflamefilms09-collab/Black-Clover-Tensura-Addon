package com.newuniverse.nusmp.client.soul;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.soul.PaladinEffects;
import com.newuniverse.nusmp.soul.PaladinNet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the halo and the wings of a Paladin are the same picture whichever way they get drawn (a render layer on a vanilla-style
 * renderer, or the free-standing overlay for creatures whose renderer has no layers, such as the GeckoLib-driven Tensura mobs), so
 * the picture is made in one place. It works in the layer frame every vanilla layer works in: y down, the creature's front on -z,
 * the feet {@value #FEET} blocks below the origin, in units of whatever scale the renderer has already applied (read back from the
 * pose, so a slime that is drawn big or a baby that is drawn small still gets a halo of the right size). The size of the creature
 * comes from its bounding box, not from its model: halo and wings are drawn for a human-sized wearer and scaled by
 * {@code (height + width) / 2.4}, so a spider, a ravager and a player each get a halo that fits and wings that match. Two GeoLite
 * models (tools/gen_paladin_assets.py), loaded once by {@code GeoModels} and shared by every Paladin; nothing is built per frame
 * except the pose transforms. Everything is full bright and additive-glowing, pulsing gently, because it is light.
 */
final class PaladinDraw {
    private PaladinDraw() {}

    /** Where the feet are in layer space (vanilla's translate of 1.501 blocks). */
    static final float FEET = 1.501f;
    private static final GeoSpec HALO = GeoSpec.of("paladin", "halo"), WINGS = GeoSpec.of("paladin", "wings");
    private static final int FB = PropDraw.FULL_BRIGHT, WHITE = 0xFFFFFFFF;

    /** Does this creature wear the halo and wings now? The marker effect (synced by vanilla) or the server's own announcement. */
    static boolean dressed(LivingEntity e) {
        if (e.isInvisible() || e.isSpectator()) return false;
        return PaladinEffects.marked(e) || PaladinNet.marked(e.getId(), e.level().getGameTime());
    }

    /** The scale the pose already carries: the length of its y axis (rotation and the layer flips do not change it). */
    private static float poseScale(PoseStack pose) {
        float x = pose.last().pose().m10(), y = pose.last().pose().m11(), z = pose.last().pose().m12();
        float s = Mth.sqrt(x * x + y * y + z * z);
        return s < 1e-3f ? 1f : s;
    }

    /**
     * Draws the halo and the wings of 'e' in the layer frame. 'age' is the entity's age in ticks with the partial tick, so the models
     * animate smoothly and out of step with each other creature (the phase includes the id).
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, LivingEntity e, float age) {
        float sc = poseScale(pose);
        float h = e.getBbHeight(), w = e.getBbWidth();
        float ref = Mth.clamp((h + w) / 2.4f, 0.35f, 4.0f);              // blocks: 1 = a human
        float m = ref / sc;                                                // local units per model block
        float phase = e.getId() * 0.73f;
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.11f + phase);
        int glow = (Mth.clamp((int) (150 + 90 * pulse), 0, 255) << 24) | 0xFFFFFF;
        int bloom = (Mth.clamp((int) (30 + 34 * pulse), 0, 255) << 24) | 0xFFE9A8;

        // the halo: centred above the top of the bounding box, the gap scales with the wearer
        float top = FEET - h / sc;
        pass(pose, buffers, HALO, top - 0.30f * m, 0f, m, (age + phase) / 20f, glow, bloom);

        // the wings: between the shoulder blades of a tall creature; on top of the back of a low, wide one (a spider, a pig)
        boolean wide = w >= h * 0.95f;
        float attach = FEET - (wide ? 0.86f : 0.68f) * h / sc;
        float back = wide ? 0f : Math.max(0f, 0.30f * w - 0.125f) / sc;
        float flap = e.onGround() ? 1f : 2.2f;                              // faster in the air
        pass(pose, buffers, WINGS, attach, back, m, (age + phase) / 20f * flap, glow, bloom);
    }

    /** One model: placed with its file origin at (0, y, z), scaled by m about it; skin, glow map, then a faint wider additive bloom. */
    private static void pass(PoseStack pose, MultiBufferSource buffers, GeoSpec spec, float y, float z, float m, float seconds, int glow, int bloom) {
        pose.pushPose();
        pose.translate(0f, y - 1.5f * m, z);                                // GeoDraw puts the file origin 1.5 model blocks below the frame's origin
        pose.scale(m, m, m);
        GeoDraw.paint(pose, buffers, spec, "idle", seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, FB, WHITE, glow);
        pose.translate(0f, 1.5f, 0f);                                       // scale the bloom about the file origin
        pose.scale(1.09f, 1.09f, 1.09f);
        pose.translate(0f, -1.5f, 0f);
        GeoDraw.paint(pose, buffers, spec, "idle", seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.ADDITIVE, FB, bloom, 0);
        pose.popPose();
    }
}
