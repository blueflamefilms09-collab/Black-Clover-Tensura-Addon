package com.newuniverse.nusmp.client.summon;

import com.newuniverse.nusmp.skill.rules.SummonSpecies;
import com.newuniverse.nusmp.skill.summon.SummonBeast;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Why: draws the Hell Mode summons (docs/summoner_creatures.md): the species of the entity picks the GeckoLib model, the painted sheet and the animation file ({@code summon_<id>}), and the auto-glow layer
 * lights the glow mask (eyes, gems, the Dragon's mouth, the halo). The model files come from {@code tools/blockbench/summoner/gen_summons.py}.
 *
 * <p>Cost (rule 11): client draw only. At most the owner's cap of creatures (default 64 hard) plus one performer; each is 90 to 410 quads and one glow pass; nothing beyond 64 blocks is drawn; the resource
 * locations are built once per species (a table), no allocation per frame. Not seen in a running game by its author.
 */
public final class SummonBeastRenderer extends GeoEntityRenderer<SummonBeast> {
    private static final double CULL = 64.0;
    private static final ResourceLocation[] MODEL = new ResourceLocation[SummonSpecies.values().length], TEXTURE = new ResourceLocation[MODEL.length], ANIM = new ResourceLocation[MODEL.length];

    static {
        for (SummonSpecies s : SummonSpecies.values()) {
            int i = s.ordinal();
            MODEL[i] = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "geo/entity/summon_" + s.id + ".geo.json");
            TEXTURE[i] = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/summon_" + s.id + ".png");
            ANIM[i] = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "animations/entity/summon_" + s.id + ".animation.json");
        }
    }

    public SummonBeastRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(SummonBeast e) { return MODEL[e.species().ordinal()]; }
            @Override public ResourceLocation getTextureResource(SummonBeast e) { return TEXTURE[e.species().ordinal()]; }
            @Override public ResourceLocation getAnimationResource(SummonBeast e) { return ANIM[e.species().ordinal()]; }
        });
        this.shadowRadius = 0.6f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** Far creatures are skipped; the models can be taller than their box (wings, halo). */
    @Override
    public boolean shouldRender(SummonBeast e, Frustum frustum, double x, double y, double z) { return e.distanceToSqr(x, y, z) < CULL * CULL; }
}
