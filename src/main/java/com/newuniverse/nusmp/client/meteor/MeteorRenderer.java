package com.newuniverse.nusmp.client.meteor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.meteor.MeteorEntity;
import com.newuniverse.nusmp.meteor.MeteorRules;
import com.newuniverse.nusmp.meteor.MeteorStyle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * Draws the shared meteor (docs/meteor_fall.md): a tumbling rock of rotated cubes (tools/blockbench/gen_meteor.py writes {@code geo/entity/meteor_rock.geo.json} and one texture + glow per style:
 * {@code textures/entity/meteor_rock_<style>.png}, {@code _glow.png}) and a streak of nested diamond cubes behind it, along the line it came in on (additive, {@code meteor_tail_<style>.png}).
 * The rock is always full bright and its glow layer is added on top.
 *
 * <p>Cost per meteor per frame: the rock (14 cubes, at most 84 quads) twice (body and glow) plus the tail (at most 8 cubes, 40 visible quads), so at most about 210 quads; at most
 * {@code maxGlobal} (24 by default) meteors exist, none is drawn beyond {@link MeteorRules#DRAW_DISTANCE} blocks, and nothing is allocated per frame (the turn is a scratch quaternion).
 */
public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    private static final MeteorStyle[] STYLES = MeteorStyle.values();
    private static final GeoSpec[] ROCK = new GeoSpec[STYLES.length], TAIL = new GeoSpec[STYLES.length];
    private static final GeoSpec ROCK_BASE = GeoSpec.of("meteor", "rock"), TAIL_BASE = GeoSpec.of("meteor", "tail");
    private static final Quaternionf TURN = new Quaternionf();                     // render thread only

    static {
        for (MeteorStyle s : STYLES) {
            ROCK[s.ordinal()] = ROCK_BASE.skin("meteor_rock_" + s.key);
            TAIL[s.ordinal()] = TAIL_BASE.skin("meteor_tail_" + s.key).noGlow();
        }
    }

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity e) { return ROCK[e.style().ordinal()].texture(); }

    @Override
    public void render(MeteorEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        int i = e.style().ordinal();
        float size = e.size();
        double dx = e.getX() - e.xo, dy = e.getY() - e.yo, dz = e.getZ() - e.zo, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        pose.pushPose();
        pose.translate(0f, e.getBbHeight() * 0.5f, 0f);                           // the entity's feet are half its height under the centre of the rock
        // the tail points back along the way the meteor came: local +y is turned onto -velocity
        pose.pushPose();
        if (len > 1.0E-4) pose.mulPose(TURN.rotationTo(0f, 1f, 0f, (float) (-dx / len), (float) (-dy / len), (float) (-dz / len)));
        pose.scale(size, size, size);
        GeoDraw.paint(pose, buffers, TAIL[i], null, 0f, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT, 0xFFFFFFFF, 0);
        pose.popPose();
        // the rock tumbles about a fixed slanted axis; the model's centre is 12 px (0.75 block) above its origin
        pose.pushPose();
        pose.mulPose(Axis.of(AXIS).rotationDegrees((e.tickCount + partial) * MeteorRules.spinPerTick(size)));
        pose.scale(size, size, size);
        pose.translate(0f, -0.75f, 0f);
        GeoDraw.paint(pose, buffers, ROCK[i], null, 0f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, PropDraw.FULL_BRIGHT, 0xFFFFFFFF, 0xFFFFFFFF);
        pose.popPose();
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static final org.joml.Vector3f AXIS = new org.joml.Vector3f(0.55f, 0.75f, 0.35f).normalize();
}
