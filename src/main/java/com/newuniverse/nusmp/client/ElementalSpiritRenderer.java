package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.ElementalSpiritLordEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.102: the Black Clover Spirit Lords in their true bodies, drawn with client.geo.GeoDraw from the models tools/gen_spirit_lords.py writes
 * (assets/nusmp/geo/entity/spirit_*.geo.json): the Salamander dragon, Undine and her water, Sylph and her wings, the Gnome. Undine's water and
 * Sylph's wings and bubbles are a second, translucent model drawn over the body. Clips: idle, move (walk or fly), cast, hit.
 *
 * <p>1.99.0 (Great Spirits, docs/rebuild/great_spirits.md): with {@link SpiritLooks#CANON} (the default) Sylph is drawn as the tiny winged fairy of the canon (honeycomb wings, hands on the
 * hips, an annoyed hop now and then), Undine as a giant elegant mermaid with very long wavy hair, and Salamander grows 1.3 times with a layer of flames over its body; the earth giant is
 * unchanged. The 0.102 models of Sylph and Undine stay on disk and are drawn when CANON is false. See {@link GreatSpiritModels}.</p>
 */
public class ElementalSpiritRenderer extends EntityRenderer<ElementalSpiritLordEntity> {
    private static final GeoSpec SALAMANDER = GeoSpec.of("spirit", "salamander"), UNDINE = GeoSpec.of("spirit", "undine"),
            UNDINE_WATER = GeoSpec.of("spirit", "undine_water"), SYLPH = GeoSpec.of("spirit", "sylph"),
            SYLPH_WINGS = GeoSpec.of("spirit", "sylph_wings"), GNOME = GeoSpec.of("spirit", "gnome");

    public ElementalSpiritRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6f;
        this.shadowStrength = 0.5f;
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.ELEMENTAL_SPIRIT_LORD.get(), ElementalSpiritRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(ElementalSpiritLordEntity e) {
        return switch (e.kind()) {
            case SALAMANDER -> SALAMANDER.texture();
            case UNDINE -> UNDINE.texture();
            case SYLPH -> SYLPH.texture();
            case GNOME -> GNOME.texture();
        };
    }

    @Override
    public void render(ElementalSpiritLordEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ElementalSpiritLordEntity.Kind k = e.kind();
        boolean canon = SpiritLooks.CANON;
        GeoSpec body = switch (k) {
            case SALAMANDER -> SALAMANDER;
            case UNDINE -> canon ? GreatSpiritModels.UNDINE_MERMAID : UNDINE;
            case SYLPH -> canon ? GreatSpiritModels.SYLPH_FAIRY : SYLPH;
            case GNOME -> GNOME;
        };
        if (k == ElementalSpiritLordEntity.Kind.UNDINE) {                                      // Undine (magic.undine): the tall woman of water; false = the old look below
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            boolean drawn = com.newuniverse.nusmp.client.undine.UndineEntityDraw.draw(e, partial, pose, buffers, light);
            pose.popPose();
            if (drawn) { super.render(e, yaw, partial, pose, buffers, light); return; }
        }
        float age = e.tickCount + partial;
        float castAge = e.tickCount - e.clientCastTick + partial;
        GeoAnim anims = GeoModels.animations(body.animations());
        String anim;
        float secs;
        GeoAnim.Clip castClip = anims == null ? null : anims.clip("cast");
        if (castClip != null && castAge >= 0 && castAge / 20f < castClip.length) { anim = "cast"; secs = castAge / 20f; }
        else if (e.hurtTime > 0) { anim = "hit"; secs = (e.hurtDuration - e.hurtTime + partial) / 20f; }
        else if (e.walkAnimation.speed() > 0.05f || e.getDeltaMovement().horizontalDistanceSqr() > 0.0006) { anim = "move"; secs = age / 20f; }
        else { anim = "idle"; secs = age / 20f; }
        if (canon && k == ElementalSpiritLordEntity.Kind.SYLPH && "idle".equals(anim) && anims != null && anims.clip("annoyed") != null) {      // 1.99.0: she stamps her foot now and then
            float hop = SpiritLooks.annoyedAt(secs, 0f);
            if (hop >= 0f) { anim = "annoyed"; secs = hop; }
        }
        this.shadowRadius = switch (k) { case SYLPH -> canon ? 0.12f : 0.6f; case UNDINE -> canon ? 0.8f : 0.6f; case SALAMANDER -> canon ? 0.78f : 0.6f; default -> 0.6f; };
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.12f);
        int g = (int) (255 * pulse);
        int glow = 0xFF000000 | (g << 16) | (g << 8) | g;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
        if (canon && k == ElementalSpiritLordEntity.Kind.SYLPH) {
            GreatSpiritModels.sylph(pose, buffers, anim, secs, age, light);
        } else if (canon && k == ElementalSpiritLordEntity.Kind.UNDINE) {
            GreatSpiritModels.undine(pose, buffers, anim, secs, age, light);
        } else if (canon && k == ElementalSpiritLordEntity.Kind.SALAMANDER) {
            GreatSpiritModels.salamander(pose, buffers, anim, secs, age, light);                       // MASSIVE: 1.3 times, with the flames of its mane, horns and wings over it
        } else {
            GeoDraw.paint(pose, buffers, body, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
            switch (k) {
                case UNDINE -> GeoDraw.paint(pose, buffers, UNDINE_WATER, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, 0xE6FFFFFF, glow);
                case SYLPH -> GeoDraw.paint(pose, buffers, SYLPH_WINGS, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, 0xE0FFFFFF, glow);
                default -> { }
            }
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                                  // the name tag
    }
}
