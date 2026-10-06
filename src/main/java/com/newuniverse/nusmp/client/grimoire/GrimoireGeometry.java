package com.newuniverse.nusmp.client.grimoire;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.grimoire.GrimoireAppearance;
import com.newuniverse.nusmp.grimoire.GrimoireSprite;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.function.Function;

/**
 * The {@code "loader": "nusmp:grimoire"} geometry of assets/nusmp/models/item/grimoire.json. It has no elements of its own:
 * baking just resolves the 32 base sprites and hands them to a {@link GrimoireBakedModel}, which assembles the 3D book per item.
 * The "display" block of the JSON still supplies the gui / hand / ground / frame transforms.
 */
public final class GrimoireGeometry implements IUnbakedGeometry<GrimoireGeometry> {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The tint mapping the JSON documents ("tint_layers") must agree with the constants the planner and colour handler use. */
    private static final int[] EXPECTED_TINTS = {GrimoireAppearance.TINT_PRIMARY, GrimoireAppearance.TINT_TRIM,
            GrimoireAppearance.TINT_INSIGNIA, GrimoireAppearance.TINT_AURA};
    private static final String[] TINT_NAMES = {"cover_primary", "trim_metal", "insignia", "aura_glow"};

    public static final IGeometryLoader<GrimoireGeometry> LOADER = (json, context) -> {
        validateTintLayers(json);
        return new GrimoireGeometry();
    };

    private static void validateTintLayers(JsonObject json) {
        if (!json.has("tint_layers")) return;
        JsonObject layers = GsonHelper.getAsJsonObject(json, "tint_layers");
        for (int i = 0; i < TINT_NAMES.length; i++) {
            int declared = GsonHelper.getAsInt(layers, TINT_NAMES[i], EXPECTED_TINTS[i]);
            if (declared != EXPECTED_TINTS[i]) {
                LOGGER.warn("grimoire.json declares tint_layers.{}={} but the renderer uses {}; the code wins", TINT_NAMES[i], declared, EXPECTED_TINTS[i]);
            }
        }
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState, ItemOverrides overrides) {
        EnumMap<GrimoireSprite, TextureAtlasSprite> sprites = new EnumMap<>(GrimoireSprite.class);
        for (GrimoireSprite s : GrimoireSprite.values()) sprites.put(s, spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, s.texture())));
        return new GrimoireBakedModel(sprites, context.getTransforms());
    }
}
