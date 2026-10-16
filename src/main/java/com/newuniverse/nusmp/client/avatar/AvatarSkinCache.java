package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.AvatarRaceGroup;
import com.newuniverse.nusmp.avatar.appearance.SkinCompose;
import com.newuniverse.nusmp.avatar.appearance.TextureLru;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Why: an Avatar's body skin depends on three creator choices (body group, skin tone, outfit) and the eye colour, which is far too many combinations
 * for static files, so each combination is composed once from the grey body template and the outfit overlay (pure arithmetic in {@code SkinCompose})
 * and kept as a GPU texture. At most 64 live at a time (least recently drawn goes first and its texture is released, so nothing leaks); the templates
 * are read once and dropped on a resource reload and on logout. If anything fails the plain template is used, never an exception in the render thread.
 */
public final class AvatarSkinCache {
    private AvatarSkinCache() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int CAPACITY = 64;

    private record Key(AvatarRaceGroup group, int skin, int outfit, int eye) {}

    private static final TextureLru<Key, ResourceLocation> LRU = new TextureLru<>(CAPACITY, (k, rl) -> Minecraft.getInstance().getTextureManager().release(rl));
    private static final Map<String, int[]> TEMPLATES = new HashMap<>();
    private static int counter;

    private static ResourceLocation tex(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/avatar/" + path + ".png"); }

    /** The skin to draw an Avatar with this look and race with. */
    public static ResourceLocation skin(AppearanceData look, String raceId) {
        AvatarRaceGroup group = AvatarRaceGroup.of(raceId);
        Key key = new Key(group, SkinCompose.quantize(look.skinTone()), Math.max(0, Math.min(AppearanceData.OUTFIT_COUNT - 1, look.outfitId())), SkinCompose.quantize(look.eyeRgb()));
        ResourceLocation hit = LRU.get(key);
        if (hit != null) return hit;
        ResourceLocation made = make(key);
        if (made == null) return tex("base_" + group.file);
        LRU.put(key, made);
        return made;
    }

    private static ResourceLocation make(Key key) {
        try {
            int[] base = template("base_" + key.group().file), mask = template("base_" + key.group().file + "_mask"), outfit = template("outfit_" + key.outfit());
            if (base == null) return null;
            int[] px = SkinCompose.compose(base, mask, outfit, key.skin(), key.eye());
            NativeImage img = new NativeImage(64, 64, false);
            for (int y = 0; y < 64; y++)
                for (int x = 0; x < 64; x++) {
                    int a = px[y * 64 + x];
                    img.setPixelRGBA(x, y, (a & 0xFF00FF00) | ((a >> 16) & 255) | ((a & 255) << 16));   // ARGB -> the image's ABGR
                }
            DynamicTexture dt = new DynamicTexture(img);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_skin/s" + (counter++));
            Minecraft.getInstance().getTextureManager().register(rl, dt);
            return rl;
        } catch (Exception ex) {
            LOG.error("[nusmp] could not compose an avatar skin", ex);
            return null;
        }
    }

    /** A template image as ARGB ints (null if the file is missing); read once. */
    private static int[] template(String name) {
        if (TEMPLATES.containsKey(name)) return TEMPLATES.get(name);
        int[] out = null;
        try {
            Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(tex(name));
            if (r.isPresent()) {
                try (InputStream in = r.get().open(); NativeImage img = NativeImage.read(in)) {
                    if (img.getWidth() == 64 && img.getHeight() == 64) {
                        out = new int[64 * 64];
                        for (int y = 0; y < 64; y++)
                            for (int x = 0; x < 64; x++) {
                                int a = img.getPixelRGBA(x, y);                                 // ABGR
                                out[y * 64 + x] = (a & 0xFF00FF00) | ((a >> 16) & 255) | ((a & 255) << 16);
                            }
                    }
                }
            } else if (!name.startsWith("outfit_")) {
                LOG.warn("[nusmp] avatar template {} not found", name);
            }
        } catch (Exception ex) {
            LOG.error("[nusmp] avatar template {} failed to load", name, ex);
        }
        TEMPLATES.put(name, out);
        return out;
    }

    /** Resource reload, logout: releases every composed texture and forgets the templates. */
    public static void clear() {
        LRU.clear();
        TEMPLATES.clear();
    }
}
