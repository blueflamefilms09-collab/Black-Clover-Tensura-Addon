package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 0.52: the player-side half of the Grimoire Sword Draw (docs/grimoire_sword_draw.md): 24 ticks of keyframed pose for the arms,
 * torso and head, the first-person hand motion, and the particles round the grimoire. The server ({@code anim.SwordDraw}) hands
 * the weapon over at tick 12 (the hilt leaves the pages).
 * <ul>
 *   <li><b>Third person / other players:</b> {@code mixin.client.PlayerModelMixin} calls {@link #pose} after vanilla has posed the
 *       model. Arms are replaced by the keyframes (blended in over 3 ticks, out over ticks 20 to 24); the head adds its offset to
 *       the player's own look so the head still tracks.</li>
 *   <li><b>First person:</b> {@link #onRenderHand} moves the main hand down to the book and up again with the sword.</li>
 *   <li><b>Particles:</b> {@link #onTick}: Anti-Magic is black and crimson wisps, Sword Magic is white light and runes.</li>
 * </ul>
 */
public final class SwordDrawClient {
    private SwordDrawClient() {}

    public static final int DURATION = 24, GIVE_TICK = 12;
    /** entity id -> {the entity's tickCount when it began, style}. */
    private static final Map<Integer, int[]> ANIMS = new HashMap<>();

    /** Keyframes: {tick, x, y, z (degrees), ease (0 in-out quad, 1 out cubic, 2 linear)}. Arms are in model rotations (negative x lifts forward). */
    static final float[][] RIGHT_ARM = {
            {0, 0, 0, 0, 0}, {4, -15, 0, 14, 0}, {8, -40, -6, 20, 0}, {12, -48, -8, 18, 0}, {14, -70, -6, 12, 1},
            {18, -125, -8, 6, 1}, {22, -95, 6, 8, 0}, {24, -70, 8, 8, 0}};
    static final float[][] LEFT_ARM = {
            {0, 0, 0, 0, 0}, {4, -10, 0, -12, 0}, {10, -30, 10, -18, 0}, {18, -45, 20, -10, 0}, {24, -25, 25, -8, 0}};
    static final float[][] BODY = {
            {0, 0, 0, 0, 0}, {6, 10, 8, 3, 0}, {12, 14, 10, 4, 0}, {16, 4, 0, 0, 1}, {20, -5, -6, -2, 0}, {24, 0, 0, 0, 0}};
    static final float[][] HEAD = {
            {0, 0, 0, 0, 0}, {4, 22, 14, 0, 0}, {12, 20, 12, 0, 0}, {18, -4, 0, 0, 0}, {24, 0, 0, 0, 0}};

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SwordDrawClient::onTick);
        NeoForge.EVENT_BUS.addListener(SwordDrawClient::onRenderHand);
    }

    public static void start(int entityId, int style) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(entityId);
        if (e != null) ANIMS.put(entityId, new int[]{e.tickCount, style});
    }

    // ---------------------------------------------------------------- easing and keyframes
    static float ease(int type, float u) {
        u = Mth.clamp(u, 0f, 1f);
        return switch (type) {
            case 1 -> 1f - (1f - u) * (1f - u) * (1f - u);                                  // easeOutCubic
            case 2 -> u;
            default -> u < 0.5f ? 2f * u * u : 1f - (float) Math.pow(-2f * u + 2f, 2) / 2f;  // easeInOutQuad
        };
    }

    /** One channel (0 x, 1 y, 2 z) of a keyframe track at tick t, in radians. */
    static float sample(float[][] kf, float t, int ch) {
        if (t <= kf[0][0]) return kf[0][1 + ch] * Mth.DEG_TO_RAD;
        for (int i = 0; i < kf.length - 1; i++) {
            if (t > kf[i + 1][0]) continue;
            float u = (t - kf[i][0]) / Math.max(1e-4f, kf[i + 1][0] - kf[i][0]);
            return Mth.lerp(ease((int) kf[i + 1][4], u), kf[i][1 + ch], kf[i + 1][1 + ch]) * Mth.DEG_TO_RAD;
        }
        return kf[kf.length - 1][1 + ch] * Mth.DEG_TO_RAD;
    }

    /** How much of the draw pose shows: in over 3 ticks, out over the last 4. */
    static float weight(float t) { return Math.min(1f, t / 3f) * (1f - Mth.clamp((t - 20f) / 4f, 0f, 1f)); }

    /** True while this entity's sword draw / store is playing (the cast animations leave the arms alone then). */
    public static boolean isDrawing(LivingEntity e) {
        int[] a = ANIMS.get(e.getId());
        return a != null && e.tickCount - a[0] >= 0 && e.tickCount - a[0] <= DURATION;
    }

    // ---------------------------------------------------------------- the model pose (called by the mixin)
    public static void pose(PlayerModel<?> m, LivingEntity e, float age) {
        int[] a = ANIMS.get(e.getId());
        if (a == null) return;
        float t = age - a[0];
        if (t < 0 || t > DURATION) return;
        float w = weight(t);
        float tt = (a[1] & 16) != 0 ? DURATION - t : t;                                  // 0.54: a stored weapon plays the draw backwards
        m.rightArm.xRot = Mth.lerp(w, m.rightArm.xRot, sample(RIGHT_ARM, tt, 0));
        m.rightArm.yRot = Mth.lerp(w, m.rightArm.yRot, sample(RIGHT_ARM, tt, 1));
        m.rightArm.zRot = Mth.lerp(w, m.rightArm.zRot, sample(RIGHT_ARM, tt, 2));
        m.leftArm.xRot = Mth.lerp(w, m.leftArm.xRot, sample(LEFT_ARM, tt, 0));
        m.leftArm.yRot = Mth.lerp(w, m.leftArm.yRot, sample(LEFT_ARM, tt, 1));
        m.leftArm.zRot = Mth.lerp(w, m.leftArm.zRot, sample(LEFT_ARM, tt, 2));
        m.body.xRot = Mth.lerp(w, m.body.xRot, sample(BODY, tt, 0));
        m.body.yRot = Mth.lerp(w, m.body.yRot, sample(BODY, tt, 1));
        m.body.zRot = Mth.lerp(w, m.body.zRot, sample(BODY, tt, 2));
        m.head.xRot += sample(HEAD, tt, 0) * w;
        m.head.yRot += sample(HEAD, tt, 1) * w;
        m.head.zRot += sample(HEAD, tt, 2) * w;
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.jacket.copyFrom(m.body);
        m.hat.copyFrom(m.head);
    }

    // ---------------------------------------------------------------- first person
    static void onRenderHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        int[] a = ANIMS.get(p.getId());
        if (a == null) return;
        float t = p.tickCount + e.getPartialTick() - a[0];
        if (t < 0 || t > DURATION) return;
        float tt = (a[1] & 16) != 0 ? DURATION - t : t;
        float down = ease(0, tt / 8f) - ease(1, (tt - 10f) / 10f);                              // 0 -> 1 reaching the book, back to 0 with the sword
        PoseStack ps = e.getPoseStack();
        ps.translate(0.12f * down, -0.65f * down, 0.1f * down);
        ps.mulPose(Axis.XP.rotationDegrees(28f * down));
        ps.mulPose(Axis.ZP.rotationDegrees(-14f * down));
    }

    // ---------------------------------------------------------------- particles round the grimoire
    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ANIMS.isEmpty()) return;
        for (Iterator<Map.Entry<Integer, int[]>> it = ANIMS.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity p = mc.level.getEntity(en.getKey());
            int[] a = en.getValue();
            if (p == null || p.tickCount - a[0] > DURATION + 2 || p.tickCount < a[0]) { it.remove(); continue; }
            int t = p.tickCount - a[0];
            double yaw = Math.toRadians(p instanceof LivingEntity l ? l.yBodyRot : p.getYRot());
            double hx = p.getX() - Math.cos(yaw) * 0.5 - Math.sin(yaw) * -0.15, hy = p.getY() + 0.95, hz = p.getZ() - Math.sin(yaw) * 0.5 + Math.cos(yaw) * 0.15;
            var r = p.level().random;
            int st = a[1] & 15;
            boolean am = st == 0 || st == 2;                                              // 0.54: Anti-Magic and Yami's Dark Magic are dark, the rest are bright
            if (t <= GIVE_TICK + 2) {
                for (int k = 0; k < 3; k++) {
                    double ox = (r.nextDouble() - 0.5) * 0.4, oz = (r.nextDouble() - 0.5) * 0.4;
                    if (am) {
                        boolean red = r.nextBoolean();
                        p.level().addParticle(new DustParticleOptions(red ? (st == 2 ? new Vector3f(0.45f, 0.12f, 0.75f) : new Vector3f(0.78f, 0.04f, 0.12f)) : new Vector3f(0.04f, 0f, 0.07f), 1.3f),
                                hx + ox, hy + 0.1, hz + oz, 0, 0.05 + 0.02 * t / 4.0, 0);
                    } else {
                        p.level().addParticle(r.nextInt(3) == 0 ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD, hx + ox, hy + 0.1, hz + oz, 0, 0.06, 0);
                    }
                }
            }
            if (t == GIVE_TICK) {                                                         // the hilt leaves the pages
                for (int k = 0; k < 24; k++) {
                    double ang = r.nextDouble() * Math.PI * 2, sp = 0.08 + r.nextDouble() * 0.12;
                    p.level().addParticle(am ? ParticleTypes.LARGE_SMOKE : ParticleTypes.END_ROD, hx, hy + 0.5, hz, Math.cos(ang) * sp, 0.05 + r.nextDouble() * 0.1, Math.sin(ang) * sp);
                }
                if (am) p.level().addParticle(ParticleTypes.EXPLOSION, hx, hy + 0.5, hz, 0, 0, 0);
            }
        }
    }
}
