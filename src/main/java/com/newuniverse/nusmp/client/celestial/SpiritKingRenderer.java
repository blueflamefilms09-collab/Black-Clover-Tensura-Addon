package com.newuniverse.nusmp.client.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.celestial.SpiritKingEntity;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Celestial Spirit King in his own models: one GeoLite file per phase ({@code celestial_king_p1} the Sovereign of the Gate in a white-and-gold robe with a stepped crown,
 * {@code _p2} the Star-Crowned with a halo of twelve star shards and a mantle of night, {@code _p3} the Celestial Ascendant in gold plate with the twelve keys floating round him
 * and eyes ablaze), each with an emissive glow map pulsing with his health. Clips follow what the server says he is doing: idle, raise (Star Fall), slam (Gate Slam), point
 * (the Bind and the Zodiac Beams), judge (the Judgement) and death. Under him a circle of the zodiac wheel turns; while he casts the Judgement a second, larger ring opens.
 */
public class SpiritKingRenderer extends EntityRenderer<SpiritKingEntity> {
    private static final GeoSpec[] PHASES = {GeoSpec.of("celestial", "king_p1"), GeoSpec.of("celestial", "king_p2"), GeoSpec.of("celestial", "king_p3")};
    private static final int GOLD = 0xFFE27A, WHITE = 0xFFFFFF, FB = 0xF000F0;
    private static final float SCALE = 1.45f;
    private static ResourceLocation circle, ring, glint;

    public SpiritKingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.2f;
        this.shadowStrength = 0.5f;
    }

    private static void textures() {
        if (circle != null) return;
        circle = AuraRender.tex("particle/celestial_circle");
        ring = AuraRender.tex("particle/celestial_ring");
        glint = AuraRender.tex("particle/celestial_glint");
    }

    @Override public ResourceLocation getTextureLocation(SpiritKingEntity e) { return PHASES[Mth.clamp(e.phase(), 1, 3) - 1].texture(); }

    /** A boss with a long beam of light should not blink out when its centre leaves the screen. */
    @Override public boolean shouldRender(SpiritKingEntity e, Frustum frustum, double x, double y, double z) { return true; }

    @Override
    public void render(SpiritKingEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        textures();
        float age = e.tickCount + partial;
        long now = e.level().getGameTime();
        int phase = Mth.clamp(e.phase(), 1, 3);
        String clip = "idle";
        float clock = age / 20f;
        if (e.deathTime > 0) {
            clip = "death";
            clock = (e.deathTime + partial) / 20f;
        } else if (e.castKind() != SpiritKingEntity.CAST_NONE && now < e.castAt() + e.castLen()) {
            clip = switch (e.castKind()) {
                case SpiritKingEntity.CAST_STAR_FALL -> "raise";
                case SpiritKingEntity.CAST_GATE_SLAM -> "slam";
                case SpiritKingEntity.CAST_JUDGEMENT -> "judge";
                default -> "point";
            };
            clock = (now - e.castAt() + partial) / 20f;
        }
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.15f);
        float hp = Mth.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f);
        int glow = glowArgb(pulse * (0.8f + 0.4f * (1f - hp)), 0xFFFFFF);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
        pose.scale(SCALE, SCALE, SCALE);
        GeoDraw.paint(pose, buffers, PHASES[phase - 1], clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
        pose.popPose();

        // the circle under him: the zodiac wheel, and in the Judgement a second ring opening round the whole arena
        if (e.deathTime == 0) {
            pose.pushPose();
            pose.translate(0f, 0.05f, 0f);
            com.newuniverse.nusmp.client.prop.KeyDraw.flat(pose, buffers.getBuffer(AuraRender.additive(circle)), 4.4f, age * 0.6f, argb(0.8f, GOLD), FB);
            pose.translate(0f, 0.01f, 0f);
            com.newuniverse.nusmp.client.prop.KeyDraw.flat(pose, buffers.getBuffer(AuraRender.additive(ring)), 2.9f, -age * 1.3f, argb(0.6f * pulse, mix(GOLD, WHITE, 0.4f)), FB);
            if (e.castKind() == SpiritKingEntity.CAST_JUDGEMENT && now < e.castAt() + e.castLen()) {
                float open = Mth.clamp((now - e.castAt() + partial) / 40f, 0f, 1f);
                pose.translate(0f, 0.01f, 0f);
                com.newuniverse.nusmp.client.prop.KeyDraw.flat(pose, buffers.getBuffer(AuraRender.additive(circle)), 30f * open, age * 0.2f, argb(0.45f * open, GOLD), FB);
            }
            pose.popPose();
        }
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static int glowArgb(float a, int rgb) { return argb(Mth.clamp(a, 0f, 1f), rgb); }

    private static int argb(float a, int rgb) { return (Mth.clamp((int) (a * 255f + 0.5f), 0, 255) << 24) | (rgb & 0xFFFFFF); }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
