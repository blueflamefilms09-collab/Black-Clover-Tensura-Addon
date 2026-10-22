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

    /** 1.111.0: the armour relics whose painted layer is known to be there (registered by {@link #registerAll}, or shipped as a PNG by a resource pack): the worn layer's failsafe asks it. */
    private static final java.util.Set<String> PAINTED_OK = new java.util.HashSet<>();
    /** The armour relics whose layer this class registered itself (what {@link #verify} checks for). */
    private static final java.util.Map<String, ResourceLocation> OWN_LAYERS = new java.util.LinkedHashMap<>();      // 1.112.0 (lag audit): id -> the outer layer's location, built once at registration (verify() used to build ~400 locations from strings every ten seconds)
    private static int generation, verifiedGeneration = -1, heals;
    private static long nextCheck;
    private static volatile String lastVerify = "not run yet";

    /** Is the painted layer of this armour relic registered and known to be non-empty (or supplied by a resource pack)? */
    public static boolean paintedOk(String relicId) { return PAINTED_OK.contains(relicId); }

    /** How many relic layers are known to be there, how often {@link #verify} had to register again, and what the last check found (for {@code /multiverse relic debug}). */
    public static int paintedCount() { return PAINTED_OK.size(); }

    public static int healCount() { return heals; }

    public static String lastVerify() { return lastVerify; }

    /**
     * 1.111.0: checks that the layers registered by {@link #registerAll} are really in the texture manager and not empty (a resource reload, a pack toggle or another mod's texture reset can
     * drop dynamic textures), and registers everything again when one is not. The first check after every registration looks at the pixels; later ones only at the presence.
     * Returns the number of layers that were missing or empty before the repair (0: fine).
     */
    public static int verify() {
        if (!painted) return 0;
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        boolean deep = verifiedGeneration != generation;
        int bad = 0;
        String first = "";
        for (java.util.Map.Entry<String, ResourceLocation> own : OWN_LAYERS.entrySet()) {
            String id = own.getKey();
            net.minecraft.client.renderer.texture.AbstractTexture t = tm.getTexture(own.getValue(), null);
            boolean ok = t instanceof DynamicTexture;
            if (ok && deep) {
                NativeImage img = ((DynamicTexture) t).getPixels();
                ok = img != null && hasVisiblePixel(img);
            }
            if (!ok) {
                bad++;
                if (first.isEmpty()) first = id;
            }
        }
        verifiedGeneration = generation;
        if (bad > 0) {
            heals++;
            lastVerify = bad + " armour layer(s) were missing or empty (first: " + first + "); painted again (repair " + heals + ")";
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] {}", lastVerify);
            registerAll();
            verifiedGeneration = generation;
        } else {
            lastVerify = OWN_LAYERS.size() + " armour layers present" + (deep ? " and not empty" : "");
        }
        return bad;
    }

    private static boolean hasVisiblePixel(NativeImage img) {
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) if ((img.getPixelRGBA(x, y) >>> 24) != 0) return true;
        return false;
    }

    /** Game bus, once per client tick: the first check as soon as a world is open after a (re)registration, then one every ten seconds; at most six repairs per session so a broken texture manager cannot loop. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !painted) return;
        long now = mc.level.getGameTime();
        if (verifiedGeneration == generation && now < nextCheck) return;
        nextCheck = now + 200;
        if (heals >= 6) return;
        try {
            verify();
        } catch (RuntimeException | LinkageError failed) {
            lastVerify = "the check failed: " + failed;
            com.mojang.logging.LogUtils.getLogger().error("[nusmp] the armour layer check failed", failed);
        }
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
            PAINTED_OK.clear();
            OWN_LAYERS.clear();
            for (RelicDef d : Relics.all()) {
                if (!d.slot().isArmor() || d.isAdopted()) continue;
                ResourceLocation outer = armorLayer(d.id(), false);
                if (mc.getResourceManager().getResource(outer).isPresent()) { PAINTED_OK.add(d.id()); continue; }      // an artist's PNG wins
                // 1.110.0: the painted layer is ALWAYS registered (it used to be a blank texture when a 3D model was expected to replace it, so a model that was not drawn left the body bare).
                // Whether the vanilla armour layer draws it or an empty model is decided on every frame ({@code ArmorSlotPlan}, {@code ArmorSlotClient}).
                // 1.111.0: every relic is painted on its own: one that fails (or paints nothing) gets a plain layer in its colours and the others are still registered
                // (before, the first failure ended the loop and every relic after it had no texture at all).
                try {
                    int[] argb = v2 ? ArmorPainterV2.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode())
                            : ArmorPainter.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode());
                    if (!com.newuniverse.nusmp.client.relics.worn.ArmorVisible.nonEmpty(argb)) throw new IllegalStateException("the painter returned a fully transparent layer");
                    int[] glowPx = v2 ? ArmorPainterV2.paintGlow(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode())
                            : ArmorPainter.paintGlow(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), d.id().hashCode());
                    tm.register(outer, new DynamicTexture(image(argb)));
                    tm.register(armorLayer(d.id(), true), new DynamicTexture(image(argb)));
                    tm.register(armorGlow(d.id()), new DynamicTexture(image(glowPx)));
                    GLOW.add(d.id());
                    OWN_LAYERS.put(d.id(), outer);
                    PAINTED_OK.add(d.id());
                } catch (RuntimeException | LinkageError failed) {
                    com.mojang.logging.LogUtils.getLogger().error("[nusmp] the armour layer of relic {} could not be painted; a plain layer in its colours is used", d.id(), failed);
                    try {
                        int[] plain = com.newuniverse.nusmp.client.relics.worn.ArmorVisible.solidLayer(d.paletteMain(), d.paletteTrim());
                        tm.register(outer, new DynamicTexture(image(plain)));
                        tm.register(armorLayer(d.id(), true), new DynamicTexture(image(plain)));
                        OWN_LAYERS.put(d.id(), outer);
                        PAINTED_OK.add(d.id());
                    } catch (RuntimeException | LinkageError worse) {
                        com.mojang.logging.LogUtils.getLogger().error("[nusmp] not even the plain armour layer of relic {} could be registered", d.id(), worse);
                    }
                }
            }
            generation++;
            verifiedGeneration = -1;
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
