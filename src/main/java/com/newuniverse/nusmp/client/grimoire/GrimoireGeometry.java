package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The {@code "loader": "nusmp:grimoire"} geometry of assets/nusmp/models/item/grimoire.json. It has no elements of its own: baking
 * resolves the grimoire_book textures and hands them to a {@link GrimoireBakedModel}, which builds each book's 3D model from its
 * look. The JSON's "display" block still supplies the gui / hand / ground / frame transforms.
 */
public final class GrimoireGeometry implements IUnbakedGeometry<GrimoireGeometry> {
    public static final IGeometryLoader<GrimoireGeometry> LOADER = (json, context) -> new GrimoireGeometry();

    static ResourceLocation texture(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "item/grimoire_book/" + name); }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState, ItemOverrides overrides) {
        Map<String, TextureAtlasSprite> sprites = new HashMap<>();
        for (String t : GrimoireBookPlan.textures()) sprites.put(t, spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texture(t))));
        return new GrimoireBakedModel(sprites, context.getTransforms());
    }
}
