package com.newuniverse.nusmp.client.zogratis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What every renderer of the Zogratis family shares: the GeoLite models (tools/gen_zogratis_triad.py and tools/gen_zogratis_lucius.py
 * write assets/nusmp/geo|animations|textures/entity/zogratis_*), one {@link ZogratisPose} clock per entity (which clip, which second),
 * the red hit tint, the fade of a dying body, and a culling box that keeps wings, halos and capes from popping at the screen edge.
 * The animation and glow logic itself is in the pure classes {@link ZogratisPose} and {@link ZogratisGlow}.
 */
public abstract class ZogratisRenderer<T extends LivingEntity> extends EntityRenderer<T> {
    /** The geo key of every Zogratis asset: {@code GeoSpec.of(KEY, "dante_base")} is zogratis_dante_base.* */
    public static final String KEY = "zogratis";
    /** A dead mob is removed after 20 ticks, so a death clip is squeezed into them. */
    public static final float DEATH_TICKS = 20f;
    private static final int HURT_RGB = 0xFF9A9A, WHITE = 0xFFFFFF;

    private final Map<Entity, ZogratisPose> clocks = new WeakHashMap<>();

    protected ZogratisRenderer(EntityRendererProvider.Context ctx, float shadow) {
        super(ctx);
        this.shadowRadius = shadow;
        this.shadowStrength = 0.7f;
    }

    protected static GeoSpec spec(String name) { return GeoSpec.of(KEY, name); }

    /** The clock of one entity (made when first drawn, forgotten with the entity). */
    protected final ZogratisPose clock(Entity e) { return clocks.computeIfAbsent(e, k -> new ZogratisPose()); }

    /** A frame filled with what every living body gives: the time, the death, the swing, the last hit, the ground covered. */
    protected static ZogratisPose.Frame frame(LivingEntity e, float partial) {
        ZogratisPose.Frame f = new ZogratisPose.Frame();
        f.age = e.tickCount + partial;
        f.tick = e.tickCount;
        f.dead = e.isDeadOrDying();
        f.deathTicks = e.deathTime + partial;
        f.deathSpan = DEATH_TICKS;
        f.swinging = e.swinging;
        f.hurtTicks = e.hurtTime > 0 ? e.hurtDuration - e.hurtTime + partial : -1f;
        f.rawSpeed = (float) Math.hypot(e.getX() - e.xo, e.getZ() - e.zo);
        return f;
    }

    /** White, or a pale red for a body that was just hit or is dying (0xRRGGBB). */
    protected static int bodyRgb(LivingEntity e) { return e.hurtTime > 0 || e.deathTime > 0 ? HURT_RGB : WHITE; }

    /** The body: alpha-tested while solid, alpha-blended while it fades; then the glow layer additive. */
    protected static void paint(PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String clip, float seconds, int light, int rgb,
                                float alpha, int glowArgb) {
        GeoDraw.Layer layer = alpha < 0.995f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT;
        GeoDraw.paint(pose, buffers, spec, clip, seconds, GeoDraw.Space.PROP, layer, light, ZogratisGlow.withAlpha(rgb, alpha), glowArgb);
    }

    /** Culls by the hitbox grown by 'growXz' sideways and 'growUp' upward (and half a block down), not by the hitbox alone. */
    protected static boolean seen(Entity e, Frustum frustum, double x, double y, double z, double growXz, double growUp) {
        if (!e.shouldRender(x, y, z)) return false;
        if (e.noCulling) return true;
        AABB b = e.getBoundingBox();
        return frustum.isVisible(new AABB(b.minX - growXz, b.minY - 0.5, b.minZ - growXz, b.maxX + growXz, b.maxY + growUp, b.maxZ + growXz));
    }

    /** The lengths of the clips of one model, read from its animation file when asked (so a resource reload is picked up). */
    protected static final class GeoClips implements ZogratisPose.Clips {
        private final GeoSpec spec;

        protected GeoClips(GeoSpec spec) { this.spec = spec; }

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
