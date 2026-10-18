package com.newuniverse.nusmp.client.remodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the framework bosses (the Zogratis four, the Devil Avatar, the God of Order, the Goliath, the Charybdis Spawn: everything that extends {@link ZogratisBoss}) were each drawn by a
 * renderer of their own around hand-built cube models, and the owner wants every mob of the mod built like the Old Witch: a player-proportioned character of the art kit (tools/arc/char_*.py,
 * docs/arc/remodel.md). This ONE class draws them all from the kit's models ({@code arc_<model>}, optionally {@code arc_<model>_form2}), on exactly the machinery the old renderers (and
 * {@code ArcBossRenderer}) used: the pose clock {@link ZogratisPose} (idle, walk and run paced by the ground covered, attack on a swing, cast while winding up and cast_big for an ultimate, hurt,
 * transform, death), the glow curve {@link ZogratisGlow} (brighter with the power drawn, swelling over a cast, starving while exposed), the flash of the devil's colour over the model swap, and the
 * clean fade of a dying body. What differs from boss to boss is a {@link Look} (the models, how the transformation is split, how it scales, its flash colour) and optional {@link Hooks} (a skin, a
 * hover, a rise from the floor, the visit clips of the God of Order). The game logic of a boss is untouched: only what it looks like changed. A missing model is tolerated: the geo loader logs it once
 * and nothing is drawn, never an exception.
 */
public class KitBossRenderer<T extends ZogratisBoss> extends ZogratisRenderer<T> {
    /** What a boss can say about its own look beyond the defaults; every method has a neutral default. */
    public interface Hooks<T extends ZogratisBoss> {
        /** The skin (a texture suffix, see {@link KitClips#spec}) the boss wears, or null for the plain one. */
        default String skin(T e, boolean second) { return null; }

        /** Blocks the body floats above the ground (Lucius's divine form). */
        default float lift(T e, boolean transforming, float phase, int form) { return 0f; }

        /** 0..1: how far the boss has risen out of the floor (the Goliath); below 1 the model is sunk and plays its {@code spawn} clip at that time. */
        default float rise(T e, float partial) { return 1f; }

        /** 0..1: how far it has grown to full size after appearing (the God of Order steps out of its column). */
        default float appear(T e, float partial) { return 1f; }

        /** The glow's ARGB when the boss lights in a colour of its own (Lucius while Julius holds the body), else 0 for the usual grey ramp. */
        default int glowColor(T e, float level) { return 0; }

        /**
         * Picks a clip of the boss's own over the clock's (the God of Order's read, beam and bow): returns 0 when it does not, 1 when it does and the light should swell as in a cast, 2 when
         * it does and the light stays as it was. Only asked while the boss is neither dying, transforming nor casting.
         */
        default int clip(T e, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips) { return 0; }
    }

    /** One boss's wardrobe and proportions. */
    public static final class Look<T extends ZogratisBoss> {
        final String first, second;
        final boolean splitTransform, secondFromTransform, followsHitbox, pitches;
        final float scale;
        final int flash;
        final double growXz, growUp, growDown;
        final Hooks<T> hooks;

        /**
         * @param second            the second form's model name (e.g. {@code dante_zogratis_form2}), or null
         * @param splitTransform    the two models each play half of the transformation (the swap is at the middle); false: the second model plays all of it
         * @param secondFromTransform the second model is worn for the whole transformation, not only after it (the Triad's hybrid bodies burst out of the first frame)
         * @param followsHitbox     grows with the hitbox (a form with a bigger body), false when the model was made for it
         * @param flash             the colour that burns over the swap (0xRRGGBB), or -1 for the devil's own
         * @param pitches           tilts nose up or down with its swimming (the Charybdis Spawn)
         */
        public Look(String first, String second, boolean splitTransform, boolean secondFromTransform, boolean followsHitbox, float scale, int flash, boolean pitches,
                    double growXz, double growUp, double growDown, Hooks<T> hooks) {
            this.first = first;
            this.second = second;
            this.splitTransform = splitTransform;
            this.secondFromTransform = secondFromTransform;
            this.followsHitbox = followsHitbox;
            this.scale = scale;
            this.flash = flash;
            this.pitches = pitches;
            this.growXz = growXz;
            this.growUp = growUp;
            this.growDown = growDown;
            this.hooks = hooks == null ? new Hooks<>() {} : hooks;
        }
    }

    private final Look<T> look;
    private final GeoSpec baseSpec;
    private final KitClips firstClips, secondClips;
    private final RemodelLooks.Look numbers, numbers2;

    public KitBossRenderer(EntityRendererProvider.Context ctx, Look<T> look) {
        super(ctx, RemodelLooks.of(look.first).shadow());
        this.look = look;
        this.baseSpec = GeoSpec.of("arc", look.first);
        this.firstClips = new KitClips(GeoSpec.of("arc", look.first));
        this.secondClips = new KitClips(GeoSpec.of("arc", look.second == null ? look.first : look.second));
        this.numbers = RemodelLooks.of(look.first);
        this.numbers2 = RemodelLooks.of(look.second == null ? look.first : look.second);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) { return baseSpec.texture(); }

    /** Wings, halos, horns and a long tail reach far past the hitbox: cull by the bigger box. */
    @Override
    public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        if (e.noCulling) return true;
        net.minecraft.world.phys.AABB b = e.getBoundingBox();
        return frustum.isVisible(new net.minecraft.world.phys.AABB(b.minX - look.growXz, b.minY - look.growDown, b.minZ - look.growXz, b.maxX + look.growXz, b.maxY + look.growUp, b.maxZ + look.growXz));
    }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int form = e.form();
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, newForm = form != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress();
        boolean hasSecond = look.second != null;
        boolean useSecond = hasSecond && (look.splitTransform ? newForm : newForm || (look.secondFromTransform && transforming));
        String model = useSecond ? look.second : look.first;
        KitClips clips = useSecond ? secondClips : firstClips;
        RemodelLooks.Look nums = useSecond ? numbers2 : numbers;
        GeoSpec spec = KitClips.spec(model, look.hooks.skin(e, useSecond), true);
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();

        float target = look.scale * (look.followsHitbox ? e.getBbHeight() / e.getType().getHeight() : 1f) * look.hooks.appear(e, partial);
        f.transforming = transforming;
        f.transformProgress = !hasSecond ? phase : look.splitTransform ? (useSecond ? (phase - 0.5f) * 2f : phase * 2f) : phase;
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = nums.walkCycle() * target;
        f.runCycle = nums.runCycle() * target;
        clock.update(f, clips);

        int visit = 0;
        if (!f.dead && !transforming && !casting) visit = look.hooks.clip(e, clock, f, clips);
        float rise = look.hooks.rise(e, partial);
        if (rise < 1f && !f.dead && clips.length(ZogratisPose.SPAWN) > 0f) {      // the rise out of the floor: the clip's time is the rise itself
            clock.clip = ZogratisPose.SPAWN;
            clock.seconds = Math.max(0f, rise) * clips.length(ZogratisPose.SPAWN);
        }

        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : visit == 1 ? ZogratisGlow.CASTING : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(Math.max(e.power(), visit == 1 ? 60 : 0), f.age, mode, visit == 1 ? 1f : phase) * fade;
        int custom = look.hooks.glowColor(e, level);
        int glow = custom != 0 ? custom : ZogratisGlow.grey(level);

        float size = clock.smoothScale(target, f.age);
        float lift = look.hooks.lift(e, transforming, phase, form);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float facing;
        if (look.pitches) {
            facing = Mth.rotLerp(partial, e.yRotO, e.getYRot());                                   // it faces where the server says it faces
        } else {
            float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
            facing = clock.smoothYaw(f.dead || !(casting || still || visit != 0) ? body : head, f.age);   // it faces its target while it casts or stands, its way while it walks
        }
        float sink = rise < 1f ? (1f - rise) * (nums.height() * size + 0.4f) : 0f;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        if (look.pitches) {
            float mid = e.getBbHeight() / 2f;
            pose.translate(0f, mid, 0f);
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));      // positive pitch looks down: the nose dips
            pose.translate(0f, -mid, 0f);
        }
        pose.translate(0f, lift - sink, 0f);
        pose.scale(size, size, size);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        // the flash is at the model swap: the middle of the transformation; the Triad's hybrid body is worn from the first frame, so it also bursts there
        float flash = !transforming ? 0f : look.secondFromTransform ? Math.max(ZogratisGlow.flash(phase), ZogratisGlow.flash(0.5f + phase)) : ZogratisGlow.flash(phase);
        if (flash > 0.02f) {                                                          // the boss's colour burns over the model swap
            int base = look.flash >= 0 ? look.flash : e.styleColor();
            int rgb = ZogratisGlow.mix(base, 0xFFFFFF, 0.5f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.85f), 0);
        }
        pose.popPose();
        this.shadowRadius = rise < 1f ? 0f : nums.shadow() * size;
        super.render(e, yaw, partial, pose, buffers, light);                          // the name tag
    }
}
