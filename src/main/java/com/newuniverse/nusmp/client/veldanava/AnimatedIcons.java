package com.newuniverse.nusmp.client.veldanava;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Why: the five Ultimate skill icons animate (3.17.0). The skill names a static icon path ({@code textures/skill/veldanava/<id>.png}); ManasCore's lists draw that path, so at run time we register
 * an {@link AnimatedIconTexture} UNDER THAT PATH and every list animates with no change to the skill or to ManasCore. A resource reload (or a resource pack) resets the path to the plain PNG, so a
 * check once a second puts the animation back (five map lookups). The 128 px strips are registered under their own paths only while the card screen is open and released when it closes, so
 * nothing ticks off screen. {@link AnimatedIconOptions} picks ANIMATED / STILL / CLASSIC; STILL releases our texture (the plain hero PNG shows), CLASSIC registers the old sprite at the path.
 * Per tick cost: five integer divisions (plus one 32 x 32 copy and upload per icon every two ticks); no allocation. Client only.
 */
public final class AnimatedIcons {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> BROKEN = new HashSet<>();                 // ids whose strip could not be read (logged once; cleared on a resource reload)
    private static final Set<String> HD_OPEN = new HashSet<>();                // the HD strips registered for the card screen right now
    private static int sinceCheck;
    // the locations are built once (the check below runs every second and the card screen asks every frame)
    private static final Map<String, ResourceLocation> STILL = locations(IconStrip::stillPath), STRIP = locations(IconStrip::stripPath),
            HD = locations(IconStrip::hdStripPath), CLASSIC = locations(IconStrip::classicPath);

    private AnimatedIcons() {}

    /** Client block of the mod class: the config, the reload listener, the once-a-second check. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AnimatedIconOptions.SPEC, "nusmp-icons-client.toml");
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) rm -> {
            BROKEN.clear();
            sinceCheck = 1000;                                                 // check at the next tick
        }));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> onClientTick());
    }

    private static ResourceLocation loc(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    private static Map<String, ResourceLocation> locations(Function<String, String> path) {
        Map<String, ResourceLocation> m = new HashMap<>();
        for (String id : IconStrip.IDS) m.put(id, loc(path.apply(id)));
        return Map.copyOf(m);
    }

    /** Once a second: every skill-list icon has the texture the chosen style asks for. */
    private static void onClientTick() {
        if (++sinceCheck < 20) return;
        sinceCheck = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() == null) return;
        try {
            apply(mc.getTextureManager(), mc.getResourceManager(), AnimatedIconOptions.style());
        } catch (RuntimeException | LinkageError failed) {
            LOG.warn("[nusmp] the Ultimate icon check failed", failed);
        }
    }

    private static void apply(TextureManager tm, ResourceManager rm, AnimatedIconOptions.Style style) {
        for (String id : IconStrip.IDS) {
            ResourceLocation at = STILL.get(id);
            AbstractTexture now = tm.getTexture(at, null);
            switch (style) {
                case ANIMATED -> {
                    if (now instanceof AnimatedIconTexture || BROKEN.contains(id)) continue;
                    AnimatedIconTexture t = readAnimated(rm, STRIP.get(id), false);
                    if (t == null) BROKEN.add(id);
                    else tm.register(at, t);
                }
                case STILL -> {
                    if (now instanceof DynamicTexture) tm.release(at);        // back to the plain hero PNG
                }
                case CLASSIC -> {
                    if (!IconStrip.WITH_CLASSIC.contains(id)) {
                        if (now instanceof DynamicTexture) tm.release(at);
                        continue;
                    }
                    if (isClassic(now) || BROKEN.contains(id)) continue;
                    ClassicTexture t = readClassic(rm, CLASSIC.get(id));
                    if (t == null) BROKEN.add(id);
                    else tm.register(at, t);
                }
            }
        }
    }

    /** Marks a dynamic texture that holds an old sprite, so the check does not rebuild it every second. */
    private static final class ClassicTexture extends DynamicTexture {
        ClassicTexture(NativeImage image) {
            super(image);
        }
    }

    private static boolean isClassic(AbstractTexture t) { return t instanceof ClassicTexture; }

    private static AnimatedIconTexture readAnimated(ResourceManager rm, ResourceLocation at, boolean linear) {
        Optional<Resource> png = rm.getResource(at);
        if (png.isEmpty()) return null;
        int frameTime = IconStrip.DEFAULT_FRAMETIME;
        Optional<Resource> meta = rm.getResource(at.withSuffix(".mcmeta"));
        if (meta.isPresent()) {
            try (var r = meta.get().openAsReader()) {
                frameTime = IconStrip.frameTime(r.lines().collect(Collectors.joining("\n")));
            } catch (IOException | RuntimeException unreadable) {
                // keep the default
            }
        }
        try (InputStream in = png.get().open()) {
            AnimatedIconTexture t = AnimatedIconTexture.of(NativeImage.read(in), frameTime, linear);
            if (t == null) LOG.warn("[nusmp] {} is not a strip of square frames; the icon stays still", at);
            return t;
        } catch (IOException | RuntimeException unreadable) {
            LOG.warn("[nusmp] could not read {}", at, unreadable);
            return null;
        }
    }

    private static ClassicTexture readClassic(ResourceManager rm, ResourceLocation at) {
        Optional<Resource> png = rm.getResource(at);
        if (png.isEmpty()) return null;
        try (InputStream in = png.get().open()) {
            return new ClassicTexture(NativeImage.read(in));
        } catch (IOException | RuntimeException unreadable) {
            LOG.warn("[nusmp] could not read {}", at, unreadable);
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------ the card screen

    /** What the card screen draws for an icon: the texture and its size in pixels (128 for the animated HD strip, 32 for a still). */
    public record Pick(ResourceLocation texture, int size) {}

    /** Called by the card screen as it opens: registers the HD strips for the ANIMATED style (the other styles need nothing). */
    public static void openCards() {
        if (AnimatedIconOptions.style() != AnimatedIconOptions.Style.ANIMATED) return;
        Minecraft mc = Minecraft.getInstance();
        for (String id : IconStrip.IDS) {
            if (HD_OPEN.contains(id)) continue;
            AnimatedIconTexture t = readAnimated(mc.getResourceManager(), HD.get(id), true);
            if (t != null) {
                mc.getTextureManager().register(HD.get(id), t);
                HD_OPEN.add(id);
            }
        }
    }

    /** Called when the card screen closes: the HD strips stop ticking and free their memory. */
    public static void closeCards() {
        if (HD_OPEN.isEmpty()) return;
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        for (String id : HD_OPEN) tm.release(HD.get(id));
        HD_OPEN.clear();
    }

    /** The texture to draw for this skill on a card: the HD animation when it is open, else the classic sprite (CLASSIC style) or the hero frame. */
    public static Pick cardIcon(String id) {
        AnimatedIconOptions.Style style = AnimatedIconOptions.style();
        if (style == AnimatedIconOptions.Style.ANIMATED && HD_OPEN.contains(id)) {
            ResourceLocation hd = HD.get(id);
            if (Minecraft.getInstance().getTextureManager().getTexture(hd, null) instanceof AnimatedIconTexture) return new Pick(hd, 128);   // a reload resets it to the plain strip until the screen is opened again
        }
        if (style == AnimatedIconOptions.Style.CLASSIC && IconStrip.WITH_CLASSIC.contains(id)) return new Pick(CLASSIC.get(id), 32);
        return new Pick(STILL.get(id), 32);
    }
}
