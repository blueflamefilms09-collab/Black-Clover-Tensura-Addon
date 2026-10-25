package com.newuniverse.nusmp.client.corpse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.corpse.CorpseDecay;
import com.newuniverse.nusmp.corpse.CorpseEffects;
import com.newuniverse.nusmp.corpse.CorpseLookRules;
import com.newuniverse.nusmp.corpse.NamedCorpseEntity;
import com.newuniverse.nusmp.corpse.NamedCorpses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * Why: Rades Spirito's numbered undead have their own custom looks that follow the series art: No.4 Jimmy a bandaged crouching figure with golden spiral eyes, No.3 David a huge stitched
 * patchwork giant with a belt ring, No.2 Alfredo an armoured, belted, hooded figure with a blue-white spirit aura, No.1 Carl a tall figure in white ornate robes and a box mitre, No.0 Michael
 * Caesal a lean figure in a white featureless mask with a grey ponytail and a red sash. They are GeoLite models built from boxes by {@code tools/corpse/gen_rades_undead.py} (assets
 * {@code geo|animations|textures/entity/rades_*}); each has four skins (the decay stage the marker effect carries: later stages are the first plus more dirt and tears) and a glow map per skin.
 * The glow is drawn by the geo drawer's additive pass, which asks the safe additive helper ({@code client.SafeAdditive}) for its render type (the geo drawer never builds the vanilla eyes type itself). The
 * animation clock, hurt tint and culling box come from the Zogratis renderer base; this class picks the model, the skin and the clip (idle, walk, run, attack, and "cast" while the body's
 * signature move or throw winds up: {@link NamedCorpseEntity#castLeft()}). The older renderer {@link NamedCorpseRenderer} and its assets stay for {@code CorpseClientConfig.classicLooks}.
 * Client only; every number it reads is synced entity data. Draw cost is bounded: nothing is drawn beyond {@link #DRAW_RANGE} blocks (the entity type also tracks only 8 chunks), the glow
 * pass (half of the vertices) is skipped beyond {@link #GLOW_RANGE} blocks, the pose frame is one reused object (no allocation per frame) and the models are 28 to 47 boxes (at most 2256
 * vertices with the glow pass, see docs/exclusive/rades_undead.md).
 * <p>
 * 3.7.1 (docs/magic/rades_cast_fix.md): a model or texture that fails to load used to draw NOTHING (the geo drawer returns at once for an empty model), which left only the name tag and the
 * magic circle. {@link CorpseLookRules#draw} now decides once per kind (and again after a resource reload): the geo look, or a tinted zombie stand-in (the vanilla humanoid, the name tag stays),
 * and one client-log line says which it is and which file failed. {@code corpse.fallbackLook = false} (client config) restores the old "draw nothing".
 */
public class RadesUndeadRenderer extends ZogratisRenderer<NamedCorpseEntity> {
    private static final int STAGES = CorpseDecay.MAX_STAGE + 1;
    private static final GeoSpec[][] SKINS = new GeoSpec[NamedCorpses.values().length][STAGES];
    private static final Clips[] CLIPS = new Clips[NamedCorpses.values().length];
    private static final float THROW_TICKS = NamedCorpseEntity.THROW_TICKS;
    /** Blocks: nothing is drawn beyond DRAW_RANGE, the additive glow pass only within GLOW_RANGE. */
    public static final double DRAW_RANGE = 96.0, GLOW_RANGE = 40.0;
    private static final double DRAW_RANGE_SQ = DRAW_RANGE * DRAW_RANGE, GLOW_RANGE_SQ = GLOW_RANGE * GLOW_RANGE;
    /** The one pose frame this renderer fills for every body it draws (the render thread draws one body at a time). */
    private final ZogratisPose.Frame scratch = new ZogratisPose.Frame();

    private static final Logger LOG = LogUtils.getLogger();
    /** What each kind is drawn as, decided against the model object that was loaded (a resource reload gives a new object, which decides again). Render thread only. */
    private static final CorpseLookRules.Draw[] DRAW = new CorpseLookRules.Draw[NamedCorpses.values().length];
    private static final GeoModelData[] DECIDED_FOR = new GeoModelData[NamedCorpses.values().length];
    private static final ResourceLocation STAND_IN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    /** The tint of the stand-in of each kind (0xRRGGBB), in the colours of its series art: Jimmy golden-brown, David blue-grey, Alfredo steel blue, Carl pale, Michael white. */
    private static final int[] STAND_IN_RGB = {0xD9B26A, 0x8FA0C0, 0x7C93C9, 0xE8E4D8, 0xF4F4F4};
    private final HumanoidModel<LivingEntity> standIn;

    static {
        for (NamedCorpses k : NamedCorpses.values()) {
            String name = modelName(k);
            GeoSpec base = GeoSpec.of("rades", name);
            for (int s = 0; s < STAGES; s++) SKINS[k.ordinal()][s] = s == 0 ? base : base.skin("rades_" + name + "_s" + s);
            CLIPS[k.ordinal()] = new Clips(base);
        }
    }

    public RadesUndeadRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, 0.6f);
        HumanoidModel<LivingEntity> m = null;
        try {
            m = new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE));
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] numbered undead: the zombie stand-in model could not be built, a failed geo model will draw nothing: {}", ex.toString());
        }
        this.standIn = m;
    }

    /**
     * What this kind is drawn as: decided from the loaded model (has parts) and the base texture (exists), again whenever a resource reload gave a new model object, with one line in the client log
     * each time the decision is made (info when the geo look is used, error when it is not and which file is the cause).
     */
    private static CorpseLookRules.Draw decide(NamedCorpses k) {
        int i = k.ordinal();
        GeoSpec spec = SKINS[i][0];
        GeoModelData m = GeoModels.model(spec.model());
        if (m == DECIDED_FOR[i] && DRAW[i] != null) return DRAW[i];
        DECIDED_FOR[i] = m;
        boolean parts = m != null && !m.roots.isEmpty();
        boolean tex = textureExists(spec.texture());
        CorpseLookRules.Draw d = CorpseLookRules.draw(parts, tex, CorpseClientConfig.fallbackLook());
        DRAW[i] = d;
        String what = k.id + " is drawn as " + CorpseLookRules.describe(d) + " (model " + spec.model() + ": " + (parts ? "ok" : "EMPTY or missing") + ", texture " + spec.texture() + ": " + (tex ? "ok" : "MISSING") + ")";
        if (d == CorpseLookRules.Draw.GEO) LOG.info("[nusmp] numbered undead look: {}", what); else LOG.error("[nusmp] numbered undead look: {}", what);
        return d;
    }

    private static boolean textureExists(ResourceLocation tex) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** The file name part of a kind's model: rades_jimmy, rades_david, rades_alfredo, rades_carl, rades_michael. */
    public static String modelName(NamedCorpses k) {
        return k == NamedCorpses.MICHAEL_CAESAL ? "michael" : k.id.substring("corpse_".length());
    }

    /** The clip lengths of one model, read from its animation file when asked. */
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

    private static GeoSpec skin(NamedCorpseEntity e) {
        MobEffectInstance mark = e.getEffect(CorpseEffects.holder());
        return SKINS[e.kind().ordinal()][mark == null ? 0 : CorpseDecay.stageOf(mark.getAmplifier())];
    }

    /** The base class's frame, written into the reused object (every field it or this class reads is set here). */
    private ZogratisPose.Frame fill(NamedCorpseEntity e, float partial) {
        ZogratisPose.Frame f = scratch;
        f.age = e.tickCount + partial;
        f.tick = e.tickCount;
        f.dead = e.isDeadOrDying();
        f.deathTicks = e.deathTime + partial;
        f.deathSpan = DEATH_TICKS;
        f.transforming = false;
        f.transformProgress = 0f;
        f.gemOut = false;
        f.swinging = e.swinging;
        f.hurtTicks = e.hurtTime > 0 ? e.hurtDuration - e.hurtTime + partial : -1f;
        f.rawSpeed = (float) Math.hypot(e.getX() - e.xo, e.getZ() - e.zo);
        return f;
    }

    @Override
    public ResourceLocation getTextureLocation(NamedCorpseEntity e) { return skin(e).texture(); }

    @Override
    public boolean shouldRender(NamedCorpseEntity e, Frustum frustum, double x, double y, double z) {
        if (entityRenderDispatcher.distanceToSqr(e) > DRAW_RANGE_SQ) return false;
        boolean big = e.kind() == NamedCorpses.DAVID;
        return seen(e, frustum, x, y, z, big ? 1.8 : 1.1, big ? 1.0 : 0.7);
    }

    @Override
    public void render(NamedCorpseEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = fill(e, partial);
        int cast = e.castLeft();
        f.cast = cast > 0 ? 1 : 0;
        f.castProgress = cast > 0 ? 1f - Math.min(cast, THROW_TICKS) / THROW_TICKS : 0f;
        f.walkCycle = switch (e.kind()) { case DAVID -> 2.6f; case ALFREDO -> 2.0f; default -> 1.5f; };
        f.runCycle = switch (e.kind()) { case JIMMY -> 2.4f; case MICHAEL_CAESAL -> 2.2f; default -> 2.8f; };
        clock.update(f, CLIPS[e.kind().ordinal()]);

        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float glow = (0.65f + 0.25f * Mth.sin(f.age * 0.1f)) * fade;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(!f.dead && (cast > 0 || clock.speed() < ZogratisPose.WALK_ON) ? head : body, f.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        int glowArgb = entityRenderDispatcher.distanceToSqr(e) > GLOW_RANGE_SQ ? 0 : ZogratisGlow.grey(glow);          // no glow pass far away
        CorpseLookRules.Draw how = decide(e.kind());
        if (how == CorpseLookRules.Draw.GEO) paint(pose, buffers, skin(e), clock.clip, clock.seconds, light, bodyRgb(e), fade, glowArgb);
        else if (how == CorpseLookRules.Draw.FALLBACK) standIn(e, partial, pose, buffers, light, head - body);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** The stand-in of a body whose geo look did not load: the vanilla humanoid in the zombie skin, tinted by kind, scaled to the body's height and width. The pose is already turned to the body's facing. */
    private void standIn(NamedCorpseEntity e, float partial, PoseStack pose, MultiBufferSource buffers, int light, float headYaw) {
        HumanoidModel<LivingEntity> m = standIn;
        if (m == null) return;
        float h = e.kind().height / 1.95f, w = e.kind().width / 0.6f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        if (e.deathTime > 0) {                                                       // lies down like any dying mob (vanilla: a quarter turn over about a second)
            float f = Mth.sqrt(Math.min(1f, (e.deathTime + partial - 1f) / 20f * 1.6f));
            pose.mulPose(Axis.ZP.rotationDegrees(f * 90f));
        }
        pose.scale(-w, -h, w);
        pose.translate(0f, -1.501f, 0f);
        m.setupAnim(e, e.walkAnimation.position(partial), e.walkAnimation.speed(partial), e.tickCount + partial, Mth.wrapDegrees(headYaw), e.getXRot());
        int rgb = e.hurtTime > 0 || e.deathTime > 0 ? bodyRgb(e) : STAND_IN_RGB[e.kind().ordinal()];
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(STAND_IN_TEXTURE));
        m.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, 0xFF000000 | rgb);
        pose.popPose();
    }
}
