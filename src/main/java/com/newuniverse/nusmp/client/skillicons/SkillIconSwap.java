package com.newuniverse.nusmp.client.skillicons;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.client.veldanava.AnimatedIconTexture;
import com.newuniverse.nusmp.client.veldanava.IconStrip;
import com.newuniverse.nusmp.skill.icons.SkillIconIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why: the repainted skill icons (docs/skill_icons_rework.md) live at the paths the skills already name ({@code textures/skill/<id>.png}), so the skill lists draw them with no change to any skill or
 * to ManasCore. Two things need the client: the setting {@code skillIcons.newStyle = false} must show the icons of 4.4.0 (kept under textures/skill/classic/v440), and the Ultimates' short loop is a vertical
 * strip that vanilla's {@code .mcmeta} animation cannot play (it only works for atlas textures, and a skill icon is blitted by its path). Both are done the way the Ultimate icons of 3.17.0 are
 * ({@link com.newuniverse.nusmp.client.veldanava.AnimatedIcons}): a small dynamic texture is registered UNDER THE SKILL'S ICON PATH, and a check once a second puts it back after a resource reload or
 * releases it when the setting asks for the plain picture. Cost: at most 256 map lookups a second for the check (none of them allocate); an animated icon copies and uploads one 32 x 32 square (4 KiB)
 * every frameTime ticks (at most 56 icons x 3.3 per second = 187 uploads, 750 KiB/s); a still icon costs nothing at all. Client only; the server never loads this class.
 */
public final class SkillIconSwap {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> BROKEN = new HashSet<>();                       // ids whose file could not be read (logged once; cleared on a reload)
    private static SkillIconIndex.Index index = SkillIconIndex.Index.EMPTY;
    private static Map<String, ResourceLocation> still = Map.of(), strip = Map.of(), classic = Map.of();   // built once per reload, so the check allocates nothing
    private static Set<String> animated = Set.of();
    private static int sinceCheck;

    private SkillIconSwap() {}

    /** Client block of the mod class: the config, the reload listener, the once-a-second check. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SkillIconOptions.SPEC, "nusmp-skillicons-client.toml");
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) SkillIconSwap::reload));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> onClientTick());
    }

    private static ResourceLocation loc(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    private static void reload(ResourceManager rm) {
        BROKEN.clear();
        SkillIconIndex.Index idx = SkillIconIndex.Index.EMPTY;
        Optional<Resource> res = rm.getResource(loc("skill_icons.json"));
        if (res.isPresent()) {
            try (var r = res.get().openAsReader()) {
                idx = SkillIconIndex.parse(r.lines().collect(Collectors.joining("\n")));
            } catch (IOException | RuntimeException unreadable) {
                LOG.warn("[anime_multiverse] could not read skill_icons.json", unreadable);
            }
        }
        Map<String, ResourceLocation> st = new HashMap<>(), sp = new HashMap<>(), cl = new HashMap<>();
        for (String id : idx.styled()) {
            st.put(id, loc(SkillIconIndex.stillPath(id)));
            sp.put(id, loc(SkillIconIndex.stripPath(id)));
            cl.put(id, loc(SkillIconIndex.classicPath(id)));
        }
        index = idx;
        still = Map.copyOf(st);
        strip = Map.copyOf(sp);
        classic = Map.copyOf(cl);
        animated = Set.copyOf(idx.animated());
        sinceCheck = 1000;                                                          // check at the next tick
    }

    private static void onClientTick() {
        if (index.isEmpty() || ++sinceCheck < 20) return;
        sinceCheck = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() == null) return;
        try {
            apply(mc.getTextureManager(), mc.getResourceManager(), SkillIconOptions.newStyle(), SkillIconOptions.animate());
        } catch (RuntimeException | LinkageError failed) {
            LOG.warn("[anime_multiverse] the skill icon check failed", failed);
        }
    }

    /** Marks a dynamic texture that holds a classic picture, so the check does not rebuild it every second. */
    private static final class ClassicTexture extends DynamicTexture {
        ClassicTexture(NativeImage image) { super(image); }
    }

    private static void apply(TextureManager tm, ResourceManager rm, boolean newStyle, boolean animate) {
        for (String id : index.styled()) {
            ResourceLocation at = still.get(id);
            AbstractTexture now = tm.getTexture(at, null);
            if (!newStyle) {
                if (now instanceof ClassicTexture || BROKEN.contains(id)) continue;
                ClassicTexture t = readClassic(rm, classic.get(id));
                if (t == null) BROKEN.add(id);
                else tm.register(at, t);
            } else if (animate && animated.contains(id)) {
                if (now instanceof AnimatedIconTexture || BROKEN.contains(id)) continue;
                AnimatedIconTexture t = readAnimated(rm, strip.get(id));
                if (t == null) BROKEN.add(id);
                else tm.register(at, t);
            } else if (now instanceof DynamicTexture) {
                tm.release(at);                                                      // back to the plain new picture
            }
        }
    }

    private static AnimatedIconTexture readAnimated(ResourceManager rm, ResourceLocation at) {
        Optional<Resource> png = rm.getResource(at);
        if (png.isEmpty()) return null;
        int frameTime = index.frameTime();
        Optional<Resource> meta = rm.getResource(at.withSuffix(".mcmeta"));
        if (meta.isPresent()) {
            try (var r = meta.get().openAsReader()) {
                frameTime = IconStrip.frameTime(r.lines().collect(Collectors.joining("\n")));
            } catch (IOException | RuntimeException unreadable) {
                // keep the index's frame time
            }
        }
        try (InputStream in = png.get().open()) {
            NativeImage img = NativeImage.read(in);
            int n = IconStrip.frames(img.getWidth(), img.getHeight());
            if (n < 1 || n > SkillIconIndex.MAX_FRAMES || img.getWidth() != 32) {                // the strip must be 32 px wide, 1..8 frames
                img.close();
                LOG.warn("[anime_multiverse] {} is not a 32 px strip of at most {} frames; the icon stays still", at, SkillIconIndex.MAX_FRAMES);
                return null;
            }
            return AnimatedIconTexture.of(img, frameTime, false);
        } catch (IOException | RuntimeException unreadable) {
            LOG.warn("[anime_multiverse] could not read {}", at, unreadable);
            return null;
        }
    }

    private static ClassicTexture readClassic(ResourceManager rm, ResourceLocation at) {
        Optional<Resource> png = rm.getResource(at);
        if (png.isEmpty()) return null;
        try (InputStream in = png.get().open()) {
            return new ClassicTexture(NativeImage.read(in));
        } catch (IOException | RuntimeException unreadable) {
            LOG.warn("[anime_multiverse] could not read {}", at, unreadable);
            return null;
        }
    }
}
