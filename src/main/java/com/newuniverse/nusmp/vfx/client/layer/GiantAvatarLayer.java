package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: the Giant Ki Avatar of the Perfected / True Ultra Instinct forms (Dragon Ball rework, Saiyan techniques III): a translucent silver giant of Ki that stands behind the Saiyan and swings with him. It
 * is ONE effect that follows the Saiyan (the server sends one packet; the avatar is never an entity). The body is drawn from a few ribbons and glows with the Ki textures that KiLayer already owns (no new
 * texture file): torso, two legs, two arms that swing once a second (the same beat as the server's blow), a haloed head, a pulsing chest core, a ground ring and a few rising streaks. Looks only: it deals no
 * damage and costs nothing on the server. {@code saiyan.classicVfx = true} draws nothing (the server still sends it).
 * <p>Payload: from = the Saiyan's feet (follows), power = the height in blocks (3 to 12), duration = how long it stands, color = the ki colour of the form.
 * <p>Worst-case draw: at most 32 quads (128 vertices) per avatar at full detail, fewer at lower detail; one avatar per Saiyan.
 */
public final class GiantAvatarLayer extends AbstractVfxLayer {
    private static final int SILVER = 0xFFD8E4FF;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.GIANT_AVATAR); }

    @Override
    public int defaultDuration(VfxShape shape) { return 400; }

    @Override
    public int defaultColor(VfxShape shape) { return SILVER; }

    private static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0, 1)); }

    private static int hot(int argb, float t) { return VfxVertexBuffer.whiten(argb, t); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (KiVfx.classic()) return;
        float yaw = inst.followYaw(ctx);
        if (inst.payload.followEntity() >= 0 && Float.isNaN(yaw)) return;           // the Saiyan is gone: the avatar ends with him
        if (Float.isNaN(yaw)) yaw = 0f;
        float age = inst.ageTicks(ctx.partialTick);
        float H = Mth.clamp(inst.power, 3f, 12f);
        float fade = Math.min(Mth.clamp(age / 10f, 0, 1), Mth.clamp((inst.duration - age) / 12f, 0, 1));
        if (fade <= 0.01f) return;
        int col = hot(inst.color, 0.35f);
        float rad = yaw * Mth.DEG_TO_RAD;
        Vector3f fwd = new Vector3f(-Mth.sin(rad), 0, Mth.cos(rad)), right = new Vector3f(Mth.cos(rad), 0, Mth.sin(rad));
        Vector3f feet = ctx.rel(inst.from(ctx)).add(new Vector3f(fwd).mul(-H * 0.4f));         // it stands behind the Saiyan
        float swing = 0.5f + 0.5f * Mth.sin(age * 0.314f);                                       // one swing a second, the server's beat
        float pulse = 0.8f + 0.2f * VfxAnim.pulse(age, 1f);

        Vector3f hip = at(feet, right, fwd, H, 0.5f, 0f, 0f), neck = at(feet, right, fwd, H, 0.82f, 0f, 0f), head = at(feet, right, fwd, H, 0.93f, 0f, 0f);
        buf.beam(ctx, KiLayer.BEAM, VfxBlend.ADD, hip, neck, H * 0.30f, H * 0.34f, 1, -age * 0.02f, al(col, 0.30f * fade), al(col, 0.20f * fade));
        for (int side = -1; side <= 1; side += 2) {
            Vector3f hipSide = at(feet, right, fwd, H, 0.5f, 0.07f * side, 0f), foot = at(feet, right, fwd, H, 0.0f, 0.09f * side, 0f);
            buf.beam(ctx, KiLayer.BEAM, VfxBlend.ADD, hipSide, foot, H * 0.12f, H * 0.09f, 1, 0, al(col, 0.26f * fade), al(col, 0.10f * fade));
            float lift = side > 0 ? swing : 1f - swing;                                          // the arms swing against each other
            Vector3f shoulder = at(feet, right, fwd, H, 0.78f, 0.18f * side, 0f), hand = at(feet, right, fwd, H, 0.78f - 0.30f + 0.38f * lift, 0.24f * side, 0.26f * lift);
            buf.beam(ctx, KiLayer.BEAM, VfxBlend.ADD, shoulder, hand, H * 0.10f, H * 0.08f, 1, 0, al(col, 0.30f * fade), al(col, 0.12f * fade));
            buf.billboard(ctx, KiLayer.HALO, VfxBlend.ADD, hand, H * 0.16f * (0.8f + 0.5f * lift), 0, al(hot(col, 0.5f), 0.45f * fade));
        }
        buf.billboard(ctx, KiLayer.HALO, VfxBlend.ALPHA, head, H * 0.30f, 0, al(col, 0.45f * fade));
        buf.billboard(ctx, KiLayer.CORE, VfxBlend.ADD, head, H * 0.14f, 0, al(hot(col, 0.8f), 0.85f * fade));
        Vector3f chest = at(feet, right, fwd, H, 0.70f, 0f, 0.05f);
        buf.billboard(ctx, KiLayer.CORE, VfxBlend.ADD, chest, H * 0.22f * pulse, 0, al(hot(col, 0.8f), 0.7f * fade));
        buf.billboard(ctx, KiLayer.HALO, VfxBlend.ADD, chest, H * 0.45f, 0, al(col, 0.22f * fade));
        buf.ring(KiLayer.RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), H * 0.30f, H * 0.42f, ctx.seg(16, 8), 4, age * 0.02f, al(col, 0.5f * fade));
        for (int k = 0; k < 5; k++) {                                                          // streaks of Ki rising off the body
            float ph = Mth.frac(age * 0.03f + k * 0.2f), a = k * 1.256f + age * 0.01f;
            Vector3f b = at(feet, right, fwd, H, 0.15f + 0.7f * ph, Mth.cos(a) * 0.2f, Mth.sin(a) * 0.12f);
            buf.beam(ctx, KiLayer.STREAK, VfxBlend.ADD, b, new Vector3f(b).add(0, H * 0.12f, 0), H * 0.03f, 0.0f, 1, 0, al(col, 0.5f * (1f - ph) * fade), al(col, 0f));
        }
    }

    /** A point of the body: {@code h} the height (share of H), {@code s} to the side, {@code f} to the front (both shares of H), from the feet. */
    private static Vector3f at(Vector3f feet, Vector3f right, Vector3f fwd, float H, float h, float s, float f) {
        return new Vector3f(feet).add(0, H * h, 0).add(new Vector3f(right).mul(H * s)).add(new Vector3f(fwd).mul(H * f));
    }
}
