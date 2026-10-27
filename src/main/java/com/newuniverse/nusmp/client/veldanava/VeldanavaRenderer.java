package com.newuniverse.nusmp.client.veldanava;

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
import com.newuniverse.nusmp.entity.veldanava.VeldanavaBoss;
import com.newuniverse.nusmp.entity.veldanava.VeldanavaRules;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws Veldanava in the body his synced state names: the Star King Dragon (drawn 1.6 times the size of its model), the Creator God, the Dark Avatar of God. All the
 * animation clock work is the pure {@link ZogratisPose} the Zogratis bosses use; this class only chooses the model and the clip:
 * <ul>
 *   <li><b>Change of body</b>: in the first half of the 4 seconds the old body holds its big cast (cast_big) and grows brighter; at the middle the entity swaps the body, a
 *       flash covers it, and the new body plays its own {@code transform} clip.</li>
 *   <li><b>Clip</b>: idle / walk / run by the ground covered, attack on a swing, cast while winding up a move (cast_big for the ultimate and the Gravity Pull), hurt, death.</li>
 *   <li><b>Cost</b> (rule 11): one boss per world, at most 1784 quads per pass on two passes (the dragon), no allocation per frame beyond the pose stack, nothing is drawn
 *       further than {@value #MAX_DISTANCE} blocks from the camera, and the dragon is culled by a grown box so wings do not pop at the screen edge.</li>
 * </ul>
 */
public class VeldanavaRenderer extends ZogratisRenderer<VeldanavaBoss> {
    private static final int FORMS = VeldanavaRules.FORMS;
    private static final GeoSpec[] SPECS = new GeoSpec[FORMS];
    private static final Clips[] CLIPS = new Clips[FORMS];
    /** How far from the camera he is still drawn, in blocks. */
    public static final double MAX_DISTANCE = 220.0;
    private static final int FLASH_RGB = 0x9FF4EA;
    /** Per body: how far its wings, horns and cloak reach past the hitbox (sideways, upward). */
    private static final double[] GROW_XZ = {18.0, 2.5, 3.5}, GROW_UP = {10.0, 2.0, 3.0};

    static {
        for (int f = 0; f < FORMS; f++) {
            SPECS[f] = GeoSpec.of("veldanava", VeldanavaRules.geo(f));
            CLIPS[f] = new Clips(SPECS[f]);
        }
    }

    public VeldanavaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, 0.9f);
    }

    @Override
    public ResourceLocation getTextureLocation(VeldanavaBoss e) { return SPECS[e.form()].texture(); }

    @Override
    public boolean shouldRender(VeldanavaBoss e, Frustum frustum, double camX, double camY, double camZ) {
        if (e.distanceToSqr(camX, camY, camZ) > MAX_DISTANCE * MAX_DISTANCE) return false;
        int f = e.form();
        return seen(e, frustum, camX, camY, camZ, GROW_XZ[f], GROW_UP[f]);
    }

    @Override
    public void render(VeldanavaBoss e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame fr = frame(e, partial);
        int form = e.form();
        GeoSpec spec = SPECS[form];
        boolean change = e.transforming();
        boolean casting = e.state() == VeldanavaRules.STATE_CASTING;
        float phase = e.phaseProgress();
        boolean newBody = change && phase >= 0.5f;                       // the entity swapped the body at the middle of the change
        boolean oldBody = change && !newBody;
        boolean big = casting && VeldanavaRules.bigCast(e.castingMove());                       // the wide or heavy moves play the big cast clip (3.30.0: the new ones too)

        fr.transforming = newBody;
        fr.transformProgress = (phase - 0.5f) * 2f;
        fr.cast = oldBody ? 2 : casting ? (big ? 2 : 1) : 0;
        fr.castProgress = oldBody ? phase * 2f : phase;
        float scale = VeldanavaRules.modelScale(form);
        fr.walkCycle = (form == VeldanavaRules.DRAGON ? 5.0f : 1.4f) * scale;
        fr.runCycle = (form == VeldanavaRules.DRAGON ? 8.0f : 2.4f) * scale;
        clock.update(fr, CLIPS[form]);

        int mode = fr.dead ? ZogratisGlow.NORMAL : change ? ZogratisGlow.TRANSFORMING : casting ? ((e.castingMove() == VeldanavaRules.GENESIS || e.castingMove() == VeldanavaRules.MEGIDDO) ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = fr.dead ? ZogratisGlow.deathFade(fr.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(form == VeldanavaRules.AVATAR ? 100 : form == VeldanavaRules.HUMANOID ? 70 : 55, fr.age, mode, phase) * fade;
        int glow = ZogratisGlow.grey(level);

        float size = clock.smoothScale(scale, fr.age);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(fr.dead || !(casting || still) ? body : head, fr.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(size, size, size);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        if (e.guarding() && !fr.dead) {                                  // Justice King Michael: the whole body is wrapped in gold light
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(0xF2D27A, 0.30f + 0.10f * Mth.sin(fr.age * 0.4f)), 0);
        }
        float flash = change ? ZogratisGlow.flash(phase) : 0f;
        if (flash > 0.02f) {                                              // the light that covers the swap of the body
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(FLASH_RGB, flash * 0.85f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);             // the name tag
    }

    /** The lengths of the clips of one model, read when asked (so a resource reload is picked up). */
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
