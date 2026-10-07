package com.newuniverse.nusmp.client.geo;

import net.minecraft.resources.ResourceLocation;

/**
 * 0.53: where one animated model lives. By convention (tools/geo_builder.py writes exactly these):
 * <pre>
 *   assets/nusmp/geo/entity/&lt;key&gt;_&lt;name&gt;.geo.json                    the model (Blockbench / GeckoLib format)
 *   assets/nusmp/animations/entity/&lt;key&gt;_&lt;name&gt;.animation.json        its keyframe animations
 *   assets/nusmp/textures/entity/&lt;key&gt;_&lt;name&gt;.png                      its texture (any size the model's texture_width/height says)
 *   assets/nusmp/textures/entity/&lt;key&gt;_&lt;name&gt;_glow.png                 optional: emissive layer, drawn additive and full bright
 * </pre>
 * {@code GeoSpec.of("bronze", "shield")} names the four. The glow is only drawn when you pass a glow colour to {@link GeoDraw#paint}.
 */
public record GeoSpec(ResourceLocation model, ResourceLocation animations, ResourceLocation texture, ResourceLocation glow) {
    public static GeoSpec of(String key, String name) {
        String id = key + "_" + name;
        return new GeoSpec(rl("geo/entity/" + id + ".geo.json"), rl("animations/entity/" + id + ".animation.json"),
                rl("textures/entity/" + id + ".png"), rl("textures/entity/" + id + "_glow.png"));
    }

    /** The same model with another texture (and glow) of the same folder, e.g. a variant skin: {@code spec.skin("bronze_shield_red")}. */
    public GeoSpec skin(String textureName) {
        return new GeoSpec(model, animations, rl("textures/entity/" + textureName + ".png"), rl("textures/entity/" + textureName + "_glow.png"));
    }

    public GeoSpec noGlow() { return new GeoSpec(model, animations, texture, null); }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }
}
