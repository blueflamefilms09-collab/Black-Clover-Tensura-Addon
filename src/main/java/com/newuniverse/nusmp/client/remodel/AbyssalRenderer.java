package com.newuniverse.nusmp.client.remodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: the Labyrinth Guardian used to wear the Floor Warden's armour whatever boss it was, so the lich, the beholder and the five-headed dragon all looked like the same suit of armour. This is the
 * renderer that draws each boss as the creature it is: the look its profile names (docs/labyrinth/abyssal_bosses.md, tools/abyssal) in the phase it has reached, read from the entity's synced data
 * ({@link LabyrinthGuardianEntity#lookId}, {@link LabyrinthGuardianEntity#lookPhase}), on the pose clock the rest of the remodel trusts ({@link ZogratisPose}: idle, walk and run paced by the
 * ground covered, attack on the swing, cast on the boss's own cast cue, hurt, death). It IS the old renderer for a Guardian with no look (it extends {@link KitMobRenderer} with the Warden's
 * probe and calls it), so a profile without a {@code look} keeps exactly the look it had.
 *
 * <p>A humanoid look is drawn from the composite model of its phase (the vanilla player body with the look's 64x64 skin and its extras on it); the same skin and extras exist as separate files and
 * are listed in {@link AbyssalLooks} for the player-model renderer to take over. The body is scaled so that the model's reference height is the entity's hitbox height (which grows with the Guardian's
 * size attribute), times the look's boost; the glow layer follows the phase (brighter as the fight goes on) and the cast. An undead look crumbles to ash and soul fire while it dies.</p>
 */
public class AbyssalRenderer extends KitMobRenderer<LivingArmorEntity> {
    private final Map<ResourceLocation, KitClips> clipsOf = new HashMap<>();             // 1.99.0: by the animation file (the only thing a KitClips reads), not by a string built from the look, the animation and the phase on every frame
    private final Map<LivingArmorEntity, Integer> lastDust = new WeakHashMap<>();
    /** How long a cast cue shows the cast clip, in ticks. */
    private static final int CAST_TICKS = 24;

    public AbyssalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "floor_warden", Size.fit(), KitProbes.ARMOUR);
    }

    private static AbyssalLooks.Look lookOf(LivingArmorEntity e) {
        return e instanceof LabyrinthGuardianEntity g ? AbyssalLooks.get(g.lookId()) : null;
    }

    /** The geo files of one phase of a look: the model, the shared animation file, the atlas and its glow layer. */
    static GeoSpec spec(AbyssalLooks.Look look, int phase) {
        if (phase < 0 || phase >= PHASES) return build(look, phase);
        GeoSpec[] row = SPECS.get(look);
        if (row != null && row[phase] != null) return row[phase];
        GeoSpec made = build(look, phase);
        if (row == null) {
            if (SPECS.size() > 512) SPECS.clear();                                       // looks are a short fixed table; this only guards against a reload that makes new ones
            SPECS.put(look, row = new GeoSpec[PHASES]);
        }
        row[phase] = made;
        return made;
    }

    /**
     * 1.99.0 (performance pass, docs/performance_1_99.md): the spec of a look and phase is worked out once. It used to be rebuilt (four string concatenations and four locations) for every Guardian on every
     * frame, twice (the entity renderer asks for the texture, and the draw asks for the whole spec). Looks are the same objects every time ({@link AbyssalLooks#get}), so they are the key.
     */
    private static final int PHASES = 8;
    private static final Map<AbyssalLooks.Look, GeoSpec[]> SPECS = new java.util.IdentityHashMap<>();

    private static GeoSpec build(AbyssalLooks.Look look, int phase) {
        String m = look.model(phase);
        return new GeoSpec(rl("geo/entity/abyssal_" + m + ".geo.json"), rl("animations/entity/abyssal_" + look.animation() + ".animation.json"), rl("textures/entity/abyssal_" + m + ".png"),
                rl("textures/entity/abyssal_" + m + "_glow.png"));
    }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    @Override
    public ResourceLocation getTextureLocation(LivingArmorEntity e) {
        AbyssalLooks.Look look = lookOf(e);
        return look == null ? super.getTextureLocation(e) : spec(look, ((LabyrinthGuardianEntity) e).lookPhase()).texture();
    }

    @Override
    public boolean shouldRender(LivingArmorEntity e, Frustum frustum, double x, double y, double z) {
        AbyssalLooks.Look look = lookOf(e);
        if (look == null) return super.shouldRender(e, frustum, x, y, z);
        float s = Math.max(1f, scaleOf(e, look));
        return seen(e, frustum, x, y, z, look.growXz() * s, look.growUp() * s);
    }

    private static float scaleOf(LivingArmorEntity e, AbyssalLooks.Look look) {
        return e.getBbHeight() / Math.max(0.2f, look.height()) * look.boost();
    }

    @Override
    public void render(LivingArmorEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        AbyssalLooks.Look look = lookOf(e);
        if (look == null) {
            super.render(e, yaw, partial, pose, buffers, light);                         // no look: the Floor Warden's body, as before
            return;
        }
        LabyrinthGuardianEntity g = (LabyrinthGuardianEntity) e;
        int phase = g.lookPhase();
        GeoSpec spec = spec(look, phase);
        KitClips clips = clipsOf.computeIfAbsent(spec.animations(), k -> new KitClips(spec));
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        float scale = scaleOf(e, look);
        f.walkCycle = look.walkCycle() * scale;
        f.runCycle = look.runCycle() * scale;
        long since = e.level().getGameTime() - g.castAt();
        if (since >= 0 && since < CAST_TICKS && !f.dead) {                              // a cast cue from the boss: a move plays the cast clip, a change of phase the big one
            f.cast = g.castKind() == 2 ? 2 : 1;
            f.castProgress = Mth.clamp((since + partial) / CAST_TICKS, 0f, 1f);
        }
        clock.update(f, clips);

        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = Mth.clamp(look.glowLevel(phase) * (f.cast > 0 ? 1f : 0.86f + 0.14f * Mth.sin(f.age * 0.09f)), 0f, 1f) * fade;
        int glow = ZogratisGlow.scaled(look.glow(), level);
        int rgb = KitMath.multiply(bodyRgb(e), 0xFFFFFF);
        float smooth = clock.smoothScale(scale, f.age);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(f.cast > 0 || still) ? body : head, f.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(0f, look.lift() * smooth, 0f);
        pose.scale(smooth, smooth, smooth);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, rgb, fade, glow);
        pose.popPose();
        this.shadowRadius = look.shadow() * smooth * (fade < 0.5f ? 0f : 1f);
        if (f.dead && look.dust()) dust(e, look, smooth);
    }

    /** The dust death of an undead look: ash and soul fire rise off the body for as long as it dies, once per tick. */
    private void dust(LivingArmorEntity e, AbyssalLooks.Look look, float scale) {
        int t = e.deathTime;
        Integer last = lastDust.get(e);
        if (last != null && last == t) return;
        lastDust.put(e, t);
        var level = e.level();
        double h = e.getBbHeight(), w = Math.max(0.5, e.getBbWidth());
        for (int i = 0; i < 6; i++) {
            double x = e.getX() + (level.random.nextDouble() - 0.5) * w * 1.2, y = e.getY() + level.random.nextDouble() * h, z = e.getZ() + (level.random.nextDouble() - 0.5) * w * 1.2;
            level.addParticle(i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ASH, x, y, z, 0.0, 0.03, 0.0);
        }
    }
}
