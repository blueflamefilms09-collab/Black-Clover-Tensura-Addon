package com.newuniverse.nusmp.client.relics;

import com.mojang.blaze3d.platform.NativeImage;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.relics.RelicArmorMaterials;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * The textures the relics paint at run time, registered with the texture manager (after every resource reload, since a reload wipes them):
 * the worn layer of each new HEAD and CHEST relic ({@link ArmorPainter}) at the path vanilla's armour renderer asks for
 * (textures/models/armor/relic_&lt;id&gt;_layer_1.png), unless a resource pack ships a PNG there; and a white pixel for the Curios worn models.
 */
public final class RelicTextures {
    private RelicTextures() {}

    /** The white texture the worn models are tinted from. */
    public static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "relic_white");

    /** Where vanilla looks for the worn layer of this relic id. */
    public static ResourceLocation armorLayer(String relicId, boolean inner) {
        return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "textures/models/armor/" + RelicArmorMaterials.name(relicId) + "_layer_" + (inner ? 2 : 1) + ".png");
    }

    /** Paints and registers everything. Call after the texture manager has reloaded; calling it again just repaints. */
    public static void registerAll() {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        try {
            NativeImage white = new NativeImage(4, 4, true);
            for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) white.setPixelRGBA(x, y, 0xFFFFFFFF);
            tm.register(WHITE, new DynamicTexture(white));
            for (RelicDef d : Relics.all()) {
                if (!d.slot().isArmor() || d.isAdopted()) continue;
                ResourceLocation outer = armorLayer(d.id(), false);
                if (mc.getResourceManager().getResource(outer).isPresent()) continue;      // an artist's PNG wins
                int[] argb = ArmorPainter.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode());
                tm.register(outer, new DynamicTexture(image(argb)));
                tm.register(armorLayer(d.id(), true), new DynamicTexture(image(argb)));
            }
        } catch (RuntimeException | LinkageError failed) {
            com.mojang.logging.LogUtils.getLogger().error("[nusmp] relic textures could not be painted", failed);
        }
    }

    private static NativeImage image(int[] argb) {
        NativeImage img = new NativeImage(ArmorPainter.W, ArmorPainter.H, true);
        for (int y = 0; y < ArmorPainter.H; y++) {
            for (int x = 0; x < ArmorPainter.W; x++) {
                int p = argb[y * ArmorPainter.W + x];
                int a = p >>> 24, r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
                img.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);                  // NativeImage keeps its pixels as ABGR
            }
        }
        return img;
    }
}
