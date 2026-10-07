package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 0.54: the player-side half of the spell-casting body animations (docs/cast_animation_guide.md). The server ({@code anim.CastAnim})
 * says which clip a player plays; this class plays it on the vanilla player model from {@code mixin.client.PlayerModelMixin}.
 * The clips are Blockbench keyframes (assets/nusmp/animations/player/cast.animation.json, bones body / left_arm / right_arm /
 * left_leg / right_leg, degrees in the model's own rotation convention, position y in pixels, up positive):
 * <ul>
 *   <li>arms are <b>replaced</b> by the clip (so the hand really goes out, up or to the side);</li>
 *   <li>body and legs <b>add</b> to the vanilla walk / sneak pose; the head keeps tracking the player's look;</li>
 *   <li>a new clip fades in over 3 ticks from whatever was playing (a release flows out of the chant), a stopped or finished
 *       clip fades out over 6 ticks.</li>
 * </ul>
 */
public final class CastAnimClient {
    private CastAnimClient() {}

    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "animations/player/cast.animation.json");
    private static final float FADE_IN = 3f, FADE_OUT = 6f, HOLD_AFTER_END = 0.25f * 20f;

    private static final class State {
        String clip, prev;
        float start = Float.NaN, prevStart, stopAt = Float.NaN;
        int entityTick;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    /** The server says: this entity plays this clip now (an empty name = stop with a fade out). */
    public static void receive(int entityId, String clip) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Iterator<Integer> it = STATES.keySet().iterator(); it.hasNext(); ) if (mc.level.getEntity(it.next()) == null) it.remove();
        Entity e = mc.level.getEntity(entityId);
        if (e == null) return;
        float now = e.tickCount;
        State s = STATES.computeIfAbsent(entityId, k -> new State());
        if (clip == null || clip.isEmpty()) {
            if (s.clip != null && Float.isNaN(s.stopAt)) s.stopAt = now;
            return;
        }
        if (s.clip != null && Float.isNaN(s.stopAt)) { s.prev = s.clip; s.prevStart = s.start; } else s.prev = null;
        s.clip = clip;
        s.start = now;
        s.stopAt = Float.NaN;
    }

    private static GeoAnim.Clip clip(GeoAnim a, String name) { return a == null || name == null ? null : a.clip(name); }

    /** The pose of one clip at an age: [bone 0 body, 1 left arm, 2 right arm, 3 left leg, 4 right leg][x, y, z rotation in radians, position y in pixels]. */
    private static final String[] BONES = {"body", "left_arm", "right_arm", "left_leg", "right_leg"};

    private static float[][] sample(GeoAnim.Clip c, float ticksSince) {
        float[][] out = new float[BONES.length][4];
        if (c == null) return out;
        float t = c.time(Math.max(0f, ticksSince) / 20f);
        for (int i = 0; i < BONES.length; i++) {
            GeoAnim.BoneAnim b = c.bones.get(BONES[i]);
            if (b == null) continue;
            if (b.rotation != null) {
                float[] r = b.rotation.sample(t);
                if (r != null) { out[i][0] = r[0] * Mth.DEG_TO_RAD; out[i][1] = r[1] * Mth.DEG_TO_RAD; out[i][2] = r[2] * Mth.DEG_TO_RAD; }
            }
            if (b.position != null) {
                float[] p = b.position.sample(t);
                if (p != null) out[i][3] = p[1];
            }
        }
        return out;
    }

    /** True once a non-looping clip has finished and held its last frame long enough. */
    private static boolean finished(GeoAnim.Clip c, float since) { return c != null && !c.loop && since > c.length * 20f + HOLD_AFTER_END; }

    // ---------------------------------------------------------------- the model pose (called by the mixin)
    public static void pose(PlayerModel<?> m, LivingEntity e, float age) {
        State s = STATES.isEmpty() ? null : STATES.get(e.getId());
        if (s == null || s.clip == null) return;
        if (SwordDrawClient.isDrawing(e)) return;                                         // the sword draw owns the arms while it plays
        GeoAnim anims = GeoModels.animations(FILE);
        GeoAnim.Clip cur = clip(anims, s.clip);
        if (cur == null) { STATES.remove(e.getId()); return; }
        float since = age - s.start;
        if (since < -2f) { STATES.remove(e.getId()); return; }                            // the player respawned / the tick counter reset
        float stopAt = Float.isNaN(s.stopAt) && finished(cur, since) ? s.start + cur.length * 20f + HOLD_AFTER_END : s.stopAt;
        float out = Float.isNaN(stopAt) ? 1f : 1f - Mth.clamp((age - stopAt) / FADE_OUT, 0f, 1f);
        if (out <= 0f) { STATES.remove(e.getId()); return; }
        float in = Mth.clamp(since / FADE_IN, 0f, 1f);
        float[][] v = sample(cur, since);
        if (s.prev != null && in < 1f) {                                                  // the previous clip is the base while the new one fades in
            float[][] pv = sample(clip(anims, s.prev), age - s.prevStart);
            for (int i = 0; i < v.length; i++) for (int k = 0; k < 4; k++) v[i][k] = Mth.lerp(in, pv[i][k], v[i][k]);
            in = 1f;
        }
        float w = in * out;

        // arms: replaced
        blendArm(m.leftArm, v[1], w);
        blendArm(m.rightArm, v[2], w);
        // body and legs: added to vanilla
        m.body.xRot += v[0][0] * w; m.body.yRot += v[0][1] * w; m.body.zRot += v[0][2] * w;
        m.leftLeg.xRot += v[3][0] * w; m.leftLeg.yRot += v[3][1] * w; m.leftLeg.zRot += v[3][2] * w;
        m.rightLeg.xRot += v[4][0] * w; m.rightLeg.yRot += v[4][1] * w; m.rightLeg.zRot += v[4][2] * w;
        // the body position moves the whole figure up / down (the model's y points down)
        float dy = -v[0][3] * w;
        if (dy != 0f) {
            m.head.y += dy; m.body.y += dy; m.leftArm.y += dy; m.rightArm.y += dy; m.leftLeg.y += dy; m.rightLeg.y += dy;
        }
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        m.jacket.copyFrom(m.body);
        m.hat.copyFrom(m.head);
    }

    private static void blendArm(net.minecraft.client.model.geom.ModelPart arm, float[] v, float w) {
        arm.xRot = Mth.lerp(w, arm.xRot, v[0]);
        arm.yRot = Mth.lerp(w, arm.yRot, v[1]);
        arm.zRot = Mth.lerp(w, arm.zRot, v[2]);
    }
}
