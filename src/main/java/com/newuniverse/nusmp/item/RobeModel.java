package com.newuniverse.nusmp.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Picks the robe's 3D shape (robe / mantle / coat / simple / devil) and its set texture. */
public class RobeModel extends GeoModel<RobeItem> {
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath("nusmp", "animations/armor/robe.animation.json");

    @Override public ResourceLocation getModelResource(RobeItem item) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "geo/armor/" + item.kind.style + ".geo.json");
    }
    @Override public ResourceLocation getTextureResource(RobeItem item) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/armor/" + item.setId + ".png");
    }
    @Override public ResourceLocation getAnimationResource(RobeItem item) { return ANIM; }
}
