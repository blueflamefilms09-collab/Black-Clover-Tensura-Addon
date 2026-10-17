package com.newuniverse.nusmp.client.arc;

import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the arc renderers must ask "is this model there?" without writing a warning for every optional file (a boss has a second-form model only when its spec asks for one),
 * and must measure the stride of a model from its own clip instead of trusting a table. Answers are cached until the resources are reloaded; a reload is noticed because
 * {@link GeoModels} drops its caches then, which gives the always-present anchor model ({@link ArcModelKey#FALLBACK}) a new parsed instance.
 */
final class ArcModels {
    private ArcModels() {}

    private static final ResourceLocation ANCHOR = GeoSpec.of(ArcModelKey.KEY, ArcModelKey.FALLBACK).model();
    private static final Map<ResourceLocation, Boolean> HAS = new HashMap<>();
    private static final Map<String, float[]> STRIDES = new HashMap<>();
    private static GeoModelData token;

    private static void sync() {
        GeoModelData now = GeoModels.model(ANCHOR);
        if (now != token) {
            token = now;
            HAS.clear();
            STRIDES.clear();
        }
    }

    /** Does the resource exist? (A plain lookup in the resource manager: nothing is logged for a miss.) */
    static synchronized boolean has(ResourceLocation id) {
        sync();
        return HAS.computeIfAbsent(id, k -> {
            try {
                return Minecraft.getInstance().getResourceManager().getResource(k).isPresent();
            } catch (RuntimeException e) {
                return false;
            }
        });
    }

    /** Does the model of this model name ({@code heath_grice}) exist? */
    static boolean hasModel(String name) {
        return !name.isEmpty() && has(GeoSpec.of(ArcModelKey.KEY, name).model());
    }

    /** {walk, run} blocks of ground per cycle of this model's clips, measured from the leg swing of the clips and the hip height of the model. */
    static synchronized float[] strides(String name) {
        sync();
        return STRIDES.computeIfAbsent(name, n -> {
            GeoSpec spec = GeoSpec.of(ArcModelKey.KEY, n);
            GeoModelData m = GeoModels.model(spec.model());
            GeoModelData.Bone leg = m.bones.get("leg_l");
            float hip = leg == null ? 12.5f : leg.py;
            GeoAnim a = GeoModels.animations(spec.animations());
            return new float[]{ArcRenderMath.strideOr(ArcRenderMath.strideBlocks(hip, swing(a, "walk")), ArcRenderMath.DEFAULT_WALK_CYCLE),
                    ArcRenderMath.strideOr(ArcRenderMath.strideBlocks(hip, swing(a, "run")), ArcRenderMath.DEFAULT_RUN_CYCLE)};
        });
    }

    /** The largest forward or backward angle (degrees) the left thigh reaches in a clip; 0 when there is no such clip or track. */
    private static float swing(GeoAnim anim, String clip) {
        if (anim == null) return 0f;
        GeoAnim.Clip c = anim.clip(clip);
        if (c == null) return 0f;
        GeoAnim.BoneAnim leg = c.bones.get("leg_l");
        if (leg == null || leg.rotation == null) return 0f;
        float best = 0f;
        int steps = 24;
        for (int i = 0; i <= steps; i++) {
            float[] v = leg.rotation.sample(c.length * i / steps);
            if (v != null) best = Math.max(best, Math.abs(v[0]));
        }
        return best;
    }
}
