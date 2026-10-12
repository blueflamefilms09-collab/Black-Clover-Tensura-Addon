package com.newuniverse.nusmp.client.wizard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.wizard.TowerWizardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Tower Wizard in his own model (tools/gen_tower_wizard.py -> assets/nusmp/geo/entity/tower_wizard.geo.json, its animations and textures),
 * drawn with client.geo.GeoDraw like the Spirit Lords: an old wizard in a wide-brimmed hat holding a green orb. The green orb and the glint in
 * his eyes are the additive glow layer, pulsing with the orb. Clips: idle (breathing, beard and hat sway, orb bob) and talk (a nod and a hand
 * gesture), played while a player stands close. He turns with the entity's yaw (he looks at whoever speaks to him).
 */
public class TowerWizardRenderer extends EntityRenderer<TowerWizardEntity> {
    private static final GeoSpec WIZARD = GeoSpec.of("tower", "wizard");
    private static final double TALK_RANGE = 4.0;

    public TowerWizardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.55f;
        this.shadowStrength = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(TowerWizardEntity e) {
        return WIZARD.texture();
    }

    @Override
    public void render(TowerWizardEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // a stationary entity: the clock comes from the world, wrapped on a multiple of the clips' lengths (4 s) so floats stay precise
        float age = (float) (e.level().getGameTime() % 4000L) + partial;
        boolean talking = e.level().getNearestPlayer(e, TALK_RANGE) != null;
        String anim = talking ? "talk" : "idle";
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.157f);                     // the orb's 2 s pulse
        int g = (int) (255 * pulse);
        int glow = 0xFF000000 | (g << 16) | (g << 8) | g;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        GeoDraw.paint(pose, buffers, WIZARD, anim, age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                      // the name tag
    }
}
