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

    private static volatile boolean whiteRegistered;

    /** Was the white pixel of the plain Curios boxes registered with the texture manager (set by {@link #registerAll}; for {@code /multiverse relic debug}). */
    public static boolean whiteRegistered() { return whiteRegistered; }

    private static volatile boolean paintedUpgrade, painted;

    /** Were the armour layers painted by the 1.110.0 painter (true) or the older one (false)? For {@code /nusmp debug worn}. */
    public static boolean paintedWithUpgrade() { return paintedUpgrade; }

    /** Called by the worn layer once per frame: repaints the layers once when {@code visual.upgrade} was flipped since they were painted (one boolean read otherwise). */
    public static void refreshIfOptionChanged() {
        if (!painted) return;
        boolean now = com.newuniverse.nusmp.client.relics.fx.RelicFxOptions.upgrade();
        if (now != paintedUpgrade) registerAll();
    }

    /** The ids of the armour relics whose glow channel is registered (drawn by the worn layer over the armour). */
    private static final java.util.Set<String> GLOW = new java.util.HashSet<>();

    /** The glow channel of an armour relic's worn layer (the pixels the painter drew in the glow colour), drawn full bright by {@code ArmorGlow}. */
    public static ResourceLocation armorGlow(String relicId) {
        return GLOW_LOCATIONS.get(relicId, id -> ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "textures/models/armor/" + RelicArmorMaterials.name(id) + "_glow.png"));
    }

    /** 1.99.0 (performance pass): the glow location of a relic, built once (the worn layer asked for it, with two string concatenations, for every armour relic of every wearer on every frame). */
    private static final com.newuniverse.nusmp.perf.BoundedCache<String, ResourceLocation> GLOW_LOCATIONS = new com.newuniverse.nusmp.perf.BoundedCache<>(1024);

    /** Was a glow channel registered for this relic (not when a resource pack supplies the armour texture, nor when a 3D model replaces the armour box)? */
    public static boolean hasArmorGlow(String relicId) { return GLOW.contains(relicId); }

    /** Where vanilla looks for the worn layer of this relic id. */
    public static ResourceLocation armorLayer(String relicId, boolean inner) {
        return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "textures/models/armor/" + RelicArmorMaterials.name(relicId) + "_layer_" + (inner ? 2 : 1) + ".png");
    }

    /** Paints and registers everything. Call after the texture manager has reloaded; calling it again just repaints. */
    public static void registerAll() {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        boolean v2 = com.newuniverse.nusmp.client.relics.fx.RelicFxOptions.upgrade();                 // 1.110.0: the cleaner layers while visual.upgrade is on, the older painter when it is off
        try {
            NativeImage white = new NativeImage(4, 4, true);
            for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) white.setPixelRGBA(x, y, 0xFFFFFFFF);
            tm.register(WHITE, new DynamicTexture(white));
            whiteRegistered = true;
            com.newuniverse.nusmp.client.relics.worn.lib.WornModels.clear();                   // the catalog decides below which armour boxes a 3D model replaces
            com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary.clear();
            GLOW.clear();
            for (RelicDef d : Relics.all()) {
                if (!d.slot().isArmor() || d.isAdopted()) continue;
                ResourceLocation outer = armorLayer(d.id(), false);
                if (mc.getResourceManager().getResource(outer).isPresent()) continue;      // an artist's PNG wins
                // 1.110.0: the painted layer is ALWAYS registered (it used to be a blank texture when a 3D model was expected to replace it, so a model that was not drawn left the body bare).
                // Whether the vanilla armour layer draws it or an empty model is decided on every frame ({@code ArmorSlotPlan}, {@code ArmorSlotClient}).
                int[] argb = v2 ? ArmorPainterV2.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode())
                        : ArmorPainter.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode());
                int[] glowPx = v2 ? ArmorPainterV2.paintGlow(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode())
                        : ArmorPainter.paintGlow(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode());
                tm.register(outer, new DynamicTexture(image(argb)));
                tm.register(armorLayer(d.id(), true), new DynamicTexture(image(argb)));
                tm.register(armorGlow(d.id()), new DynamicTexture(image(glowPx)));
                GLOW.add(d.id());
            }
            paintedUpgrade = v2;
            painted = true;
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
