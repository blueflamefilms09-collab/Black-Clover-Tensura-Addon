package com.newuniverse.nusmp.client.remodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: every living mob of the mod that is not a framework boss (the Dark Disciple, the Blood Beast, the dungeon's creatures and armours, the spirits, the daemons, the painted constructs,
 * Rouge, the Tower Wizard, the cotton sheep and the legacy Zagred) used to have a renderer of its own around a hand-built cube model. The owner wants them all built like the Old Witch, as
 * player-proportioned characters of the art kit (tools/arc/char_*.py, docs/arc/remodel.md), so this ONE generic renderer draws them all from the kit's models ({@code arc_<model>}) on the pose clock
 * the rest of the mod trusts ({@link ZogratisPose}: idle, walk and run paced by the ground covered, attack on a swing, cast, hurt, death) and the family's glow and fade. What differs from mob to
 * mob is a {@link Probe}: which model and skin it wears now (a tier, a kind, a role, a form), what state it is in (casting, asleep, rising, sitting), how it is tinted, scaled and lifted. A
 * model that is missing is logged once by the geo loader and nothing is drawn, never an exception. Game logic is never touched: a probe only reads what the entity already says.
 */
public class KitMobRenderer<T extends LivingEntity> extends ZogratisRenderer<T> {
    /** Which clip to play over the clock's and at what second; filled by {@link Probe#pick}. */
    public static final class Pick {
        public String clip;
        public float seconds;
    }

    /** What one kind of mob tells the renderer; every method but {@link #model} has a neutral default. */
    public interface Probe<T extends LivingEntity> {
        /** The model (without {@code arc_}) it wears now: {@code spirit_undine}, {@code grimoire_daemon_arch} ... */
        String model(T e);

        /** The skin (a texture suffix, see {@link KitClips#spec}) or null. */
        default String skin(T e) { return null; }

        /** A colour (0xRRGGBB) multiplied into the body (the paint of a painted construct); white is no change. */
        default int tint(T e) { return 0xFFFFFF; }

        /** The colour the glow layer takes (0xRRGGBB); white keeps it as painted. */
        default int glowRgb(T e) { return 0xFFFFFF; }

        /** 1 solid; less draws the body translucent (a slime's gel, Undine's water, a cat of thread unravelling). */
        default float alpha(T e, float partial) { return 1f; }

        /** A factor on the body's size from the entity's state (a rise, a hop), 1 normally. */
        default float size(T e, float partial) { return 1f; }

        /** An extra non-uniform scale {sx, sy, sz} (a painted construct rising off the canvas), or null. */
        default float[] squash(T e, float partial) { return null; }

        /** Blocks the body floats above the ground. */
        default float lift(T e, float partial) { return 0f; }

        /** False when nothing should be drawn (Rouge resting on her summoner's head is drawn by the head layer). */
        default boolean visible(T e) { return true; }

        /** Writes what the entity says into the frame (a cast, a cast's progress, a swing) before the clock reads it. */
        default void fill(T e, ZogratisPose.Frame f, float partial) { }

        /** Picks a clip over the clock's; returns true when it did (then {@code out} says which and when). */
        default boolean pick(T e, float partial, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips, Pick out) { return false; }

        /** How bright the glow layer is, 0..1, before the fade of a dying body. */
        default float glowLevel(T e, ZogratisPose.Frame f) { return f.cast > 0 ? 1f : 0.72f + 0.12f * Mth.sin(f.age * 0.07f); }
    }

    /**
     * How a mob is scaled: {@code fit} draws the model at its hitbox (the entity's height over the model's own height, times {@code boost}), else at the constant {@code scale}.
     * A hitbox that grows (a slime, a Guardian's size attribute) grows the body with it.
     */
    public record Size(boolean fitted, float scale, float boost) {
        public static Size fixed(float scale) { return new Size(false, scale, 1f); }
        public static Size fit() { return new Size(true, 1f, 1f); }
        public static Size fit(float boost) { return new Size(true, 1f, boost); }
    }

    private final java.util.Map<String, KitClips> clipsOf = new java.util.HashMap<>();
    private final Probe<T> probe;
    private final Size size;

    public KitMobRenderer(EntityRendererProvider.Context ctx, String firstModel, Size size, Probe<T> probe) {
        super(ctx, RemodelLooks.of(firstModel).shadow());
        this.probe = probe;
        this.size = size;
    }

    @Override
    public ResourceLocation getTextureLocation(T e) { return KitClips.spec(probe.model(e), probe.skin(e), false).texture(); }

    /** Wings, tails and horns reach past the hitbox: cull by the model's own box, grown by the body's scale. */
    @Override
    public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) {
        RemodelLooks.Look n = RemodelLooks.of(probe.model(e));
        float s = Math.max(1f, scaleOf(e, n, 1f));
        return seen(e, frustum, x, y, z, n.growXz() * s, n.growUp() * s);
    }

    private float scaleOf(T e, RemodelLooks.Look n, float partial) {
        float base = size.fitted() ? (e.getBbHeight() / Math.max(0.2f, n.height())) * size.boost() : size.scale();
        return base * probe.size(e, partial);
    }

    /** Called inside the body's pose, after the body is drawn and before the pose is closed (the legacy Zagred's aura goes here). Does nothing by default. */
    protected void decorate(T e, PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String clip, float seconds, int light, float scale, float partial) { }

    /** Called after the body, at the entity's origin and before the name tag (a target ring on the floor goes here). Does nothing by default. */
    protected void afterBody(T e, PoseStack pose, MultiBufferSource buffers, float partial) { }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!probe.visible(e)) {
            super.render(e, yaw, partial, pose, buffers, light);                          // the name tag only
            return;
        }
        String model = probe.model(e);
        RemodelLooks.Look nums = RemodelLooks.of(model);
        GeoSpec spec = KitClips.spec(model, probe.skin(e), true);
        KitClips clips = clipsOf.computeIfAbsent(model, k -> new KitClips(GeoSpec.of("arc", k)));
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        float scale = scaleOf(e, nums, partial);
        f.walkCycle = nums.walkCycle() * scale;
        f.runCycle = nums.runCycle() * scale;
        probe.fill(e, f, partial);
        clock.update(f, clips);
        Pick pick = new Pick();
        if (!f.dead && probe.pick(e, partial, clock, f, clips, pick) && pick.clip != null) {
            clock.clip = pick.clip;
            clock.seconds = Math.max(0f, pick.seconds);
        }

        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float alpha = Math.min(fade, probe.alpha(e, partial));
        int glowRgb = probe.glowRgb(e);
        float level = Mth.clamp(probe.glowLevel(e, f), 0f, 1f) * fade;
        int glow = glowRgb == 0xFFFFFF ? ZogratisGlow.grey(level) : ZogratisGlow.scaled(glowRgb, level);
        int rgb = KitMath.multiply(bodyRgb(e), probe.tint(e));

        float smooth = clock.smoothScale(scale, f.age);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(f.cast > 0 || still) ? body : head, f.age);     // it faces what it looks at while it stands or casts, its way while it walks
        float[] sq = probe.squash(e, partial);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(0f, probe.lift(e, partial), 0f);
        if (sq != null) pose.scale(smooth * sq[0], smooth * sq[1], smooth * sq[2]);
        else pose.scale(smooth, smooth, smooth);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, rgb, alpha, glow);
        decorate(e, pose, buffers, spec, clock.clip, clock.seconds, light, smooth, partial);
        pose.popPose();
        afterBody(e, pose, buffers, partial);
        this.shadowRadius = nums.shadow() * smooth * (alpha < 0.5f ? 0f : 1f);
        super.render(e, yaw, partial, pose, buffers, light);                              // the name tag
    }
}
