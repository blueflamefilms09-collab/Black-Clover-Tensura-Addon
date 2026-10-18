package com.newuniverse.nusmp.client.labyrinth.foundry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.arc.ArcRenderMath;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: four hundred mobs must not mean four hundred renderers, and the art kit's characters (docs/arc/art_kit.md) already have everything a fighting body needs, so this ONE class
 * draws every {@link FoundryMobEntity} with the machinery the Arc renderers and the Zogratis family already trust: the pose clock {@link ZogratisPose} (idle, walk and run paced by
 * the ground covered so the feet do not slide, attack on a swing, cast and cast_big while an ability winds up, hurt, death), the glow layer, the clean fade of a dying body, the red
 * hit tint. What is different from {@code ArcNpcRenderer} and {@code ArcBossRenderer} is only where the files are: a foundry mob has its OWN geometry and texture
 * ({@code foundry_<id>}) but SHARES its clips with the other mobs of its animation class ({@code foundry_anim_<class>}, named by the asset index): a {@link GeoSpec} is a record, so the
 * spec is built from the three files without any engine change. The model is drawn at the size the index gives (the hitbox follows by the SCALE attribute on the server). The
 * glow brightens with the tier. A mob with no index entry or no model is skipped (logged once), never an exception.
 */
public class FoundryRenderer extends ZogratisRenderer<FoundryMobEntity> {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String KEY = "foundry";

    private final Map<String, GeoSpec> specs = new HashMap<>();
    private final Map<String, Clips> clips = new HashMap<>();
    private final Map<String, float[]> strides = new HashMap<>();
    private final Set<String> warned = new HashSet<>();

    public FoundryRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.45f); }

    /** The files of a mob: its own model and textures, the shared clips of its class. Cached per id (the index is re-read on a resource reload, which clears nothing here because the
     *  entries hold only resource names). */
    private GeoSpec spec(String id, FoundryIndex.Entry en) {
        return specs.computeIfAbsent(id + "|" + en.anim(), k -> {
            GeoSpec own = GeoSpec.of(KEY, id);
            ResourceLocation anim = ResourceLocation.fromNamespaceAndPath("nusmp", "animations/entity/foundry_anim_" + en.anim() + ".animation.json");
            return new GeoSpec(own.model(), anim, own.texture(), own.glow());
        });
    }

    @Override
    public ResourceLocation getTextureLocation(FoundryMobEntity e) { return GeoSpec.of(KEY, e.foundryId().isEmpty() ? "none" : e.foundryId()).texture(); }

    @Override
    public boolean shouldRender(FoundryMobEntity e, Frustum frustum, double x, double y, double z) {
        FoundryIndex.Entry en = FoundryIndex.get(e.foundryId());
        double s = en == null ? 1.0 : en.size();
        return seen(e, frustum, x, y, z, 1.4 * s, 0.9 * s);          // wings, horns, halos and weapons reach past the hitbox
    }

    /** {walk, run} blocks of ground per cycle of the class's clips, from the thigh swing and the model's hip height (the maths of the arc renderers, for these file names). */
    private float[] stride(GeoSpec spec) {
        return strides.computeIfAbsent(spec.model().toString() + spec.animations(), k -> {
            GeoModelData m = GeoModels.model(spec.model());
            GeoModelData.Bone leg = m.bones.get("leg_l");
            float hip = leg == null ? 12.5f : leg.py;
            GeoAnim a = GeoModels.animations(spec.animations());
            return new float[]{ArcRenderMath.strideOr(ArcRenderMath.strideBlocks(hip, swing(a, "walk")), ArcRenderMath.DEFAULT_WALK_CYCLE),
                    ArcRenderMath.strideOr(ArcRenderMath.strideBlocks(hip, swing(a, "run")), ArcRenderMath.DEFAULT_RUN_CYCLE)};
        });
    }

    private static float swing(GeoAnim anim, String clip) {
        if (anim == null) return 0f;
        GeoAnim.Clip c = anim.clip(clip);
        if (c == null) return 0f;
        GeoAnim.BoneAnim leg = c.bones.get("leg_l");
        if (leg == null || leg.rotation == null) return 0f;
        float best = 0f;
        for (int i = 0; i <= 24; i++) {
            float[] v = leg.rotation.sample(c.length * i / 24f);
            if (v != null) best = Math.max(best, Math.abs(v[0]));
        }
        return best;
    }

    @Override
    public void render(FoundryMobEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        String id = e.foundryId();
        FoundryIndex.Entry en = id.isEmpty() ? null : FoundryIndex.get(id);
        if (en == null) {
            if (!id.isEmpty() && warned.add(id)) LOG.warn("[nusmp] the foundry mob '{}' has no entry in the asset index: it is not drawn", id);
            super.render(e, yaw, partial, pose, buffers, light);
            return;
        }
        GeoSpec spec = spec(id, en);
        Clips reader = clips.computeIfAbsent(spec.animations().toString(), k -> new Clips(spec));
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        float[] stride = stride(spec);
        f.walkCycle = stride[0] * en.size();
        f.runCycle = stride[1] * en.size();
        boolean casting = e.casting();
        if (casting) {
            f.cast = e.castMax() >= 20 ? 2 : 1;
            f.castProgress = 1f - e.castLeft() / (float) Math.max(1, e.castMax());
        }
        clock.update(f, reader);
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = (0.50f + 0.06f * en.tier() + 0.10f * Mth.sin(f.age * 0.07f) + (casting ? 0.25f * f.castProgress : 0f)) * fade;
        int glow = ZogratisGlow.grey(Mth.clamp(level, 0f, 1f));
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(casting || still) ? body : head, f.age);       // it faces what it looks at while it casts or stands, its way while it walks
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(en.size(), en.size(), en.size());
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                                       // the name tag of a custom-named mob
    }

    /** The lengths and looping of the shared clips (asked every time, so a resource reload is picked up). */
    private static final class Clips implements ZogratisPose.Clips {
        private final GeoSpec spec;

        Clips(GeoSpec spec) { this.spec = spec; }

        private GeoAnim.Clip find(String clip) {
            GeoAnim a = GeoModels.animations(spec.animations());
            return a == null ? null : a.clip(clip);
        }

        @Override
        public float length(String clip) {
            GeoAnim.Clip c = find(clip);
            return c == null ? 0f : c.length;
        }

        @Override
        public boolean loops(String clip) {
            GeoAnim.Clip c = find(clip);
            return c == null || c.loop;
        }
    }
}
