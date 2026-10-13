package com.newuniverse.nusmp.client.floor.charybdis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisSpawnBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Charybdis Spawn (tools/gen_charybdis.py writes assets/nusmp/geo|animations|textures/entity/charybdis_spawn*): the squid-whale leviathan
 * in its teal skin, and in its flushed skin once the Gates are open (the skin changes at the middle of the transformation, under a flash of cyan).
 * The clip, the glow and the fade of a dying body are the Zogratis family's own ({@link ZogratisPose}, {@link ZogratisGlow}); what is its own is
 * the pose: it faces where the server says it faces and pitches nose up or down with the way it swims, about the middle of its body, and its long
 * body and trailing tentacles are culled by a bigger box than its hitbox.
 */
public class CharybdisSpawnRenderer extends ZogratisRenderer<CharybdisSpawnBoss> {
    private static final GeoSpec BODY = GeoSpec.of("charybdis", "spawn"), FLUSHED = BODY.skin("charybdis_spawn_gates");
    private static final Clips CLIPS = new Clips(BODY);
    /** Blocks of water one cycle of the swim and of the fast swim covers; the shadow shows only close to the floor. */
    private static final float SWIM_CYCLE = 3.6f, DART_CYCLE = 5.5f;
    private static final int CYAN = 0x2FD6C8;

    public CharybdisSpawnRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.9f); }

    @Override
    public ResourceLocation getTextureLocation(CharybdisSpawnBoss e) { return BODY.texture(); }

    /** The body trails away behind and the tentacles hang below: a bigger box than the hitbox. */
    @Override
    public boolean shouldRender(CharybdisSpawnBoss e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        if (e.noCulling) return true;
        AABB b = e.getBoundingBox();
        return frustum.isVisible(new AABB(b.minX - 3.5, b.minY - 3.0, b.minZ - 3.5, b.maxX + 3.5, b.maxY + 1.5, b.maxZ + 3.5));
    }

    @Override
    public void render(CharybdisSpawnBoss e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, flushed = e.form() != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress();
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();
        f.transforming = transforming;
        f.transformProgress = phase;
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = SWIM_CYCLE;
        f.runCycle = DART_CYCLE;
        clock.update(f, CLIPS);

        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(e.power(), f.age, mode, phase) * fade;
        GeoSpec spec = flushed ? FLUSHED : BODY;

        float facing = Mth.rotLerp(partial, e.yRotO, e.getYRot()), pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float mid = e.getBbHeight() / 2f;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(0f, mid, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));                              // positive pitch looks down: the nose dips
        pose.translate(0f, -mid, 0f);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, ZogratisGlow.grey(level));
        float flash = transforming ? ZogratisGlow.flash(phase) : 0f;
        if (flash > 0.02f) {                                                  // cyan fire over the change of skin
            int rgb = ZogratisGlow.mix(CYAN, 0xFFFFFF, 0.5f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.85f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                   // the name tag
    }

    /** The lengths of the clips of the leviathan's animation file, read when asked (so a resource reload is picked up). */
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
