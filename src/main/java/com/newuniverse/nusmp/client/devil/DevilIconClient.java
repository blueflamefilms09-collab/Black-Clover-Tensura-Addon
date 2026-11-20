package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.client.veldanava.AnimatedIconTexture;
import com.newuniverse.nusmp.devil.DevilAttribute;
import com.newuniverse.nusmp.devil.dual.DualRules;
import com.newuniverse.nusmp.skill.icons.EmblemComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the icons of the skills Devil Magic and Devil Bond (docs/devil_double_magic.md) animate and show WHICH devil and WHICH grimoire the player has, with no painted file per pair. The skills name the plain
 * icons {@code textures/skill/devil_magic.png} and {@code devil_bond.png}; while the player holds a contract this class registers an {@link AnimatedIconTexture} under those two paths (the way
 * {@code AnimatedIcons} does for the Ultimates), made from the emblem of the devil's attribute and the emblem of the grimoire's magic by {@link EmblemComposer}. A resource reload resets the paths, so a check
 * once a second puts the texture back; with no contract, or with {@code devil.animatedIcons} or {@code devil.classicLook} off, the paths are released and the plain PNGs show.
 *
 * <p>Cost (rule 11), all on the client: at most two textures (one per skill) and at most {@link EmblemComposer#MAX_CACHE} composed strips cached (8 frames of 32 x 32 each); a strip is composed only when the
 * pair (attribute, magic) changes, never per frame; emblem masks (16 x 16) are read once per key and kept (at most 122 of them); per tick the texture manager does one integer division per texture and, on a
 * frame change every {@link EmblemComposer#FRAME_TICKS} ticks, one 32 x 32 copy and upload. The once-a-second check is one lookup of the own contract, the grimoire (a few skill lookups) and two texture lookups.
 * Nothing is sent over the network and nothing is decided here. Not seen in a running game by its author.
 */
public final class DevilIconClient {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String NS = "anime_multiverse";
    private static final ResourceLocation MAGIC_ICON = ResourceLocation.fromNamespaceAndPath(NS, "textures/skill/devil_magic.png"), BOND_ICON = ResourceLocation.fromNamespaceAndPath(NS, "textures/skill/devil_bond.png");

    private static final Map<String, int[]> MASKS = new HashMap<>();                    // emblem path -> 16 x 16 ARGB (render thread only); null entry = unreadable, not tried again until a reload
    private static final Map<String, int[]> STRIPS = new HashMap<>();                   // pair key -> composed strip (bounded by MAX_CACHE)
    private static final Map<ResourceLocation, AnimatedIconTexture> MINE = new HashMap<>();
    private static int sinceCheck;
    private static String shownKey = "";

    private DevilIconClient() {}

    /** Called by {@link DevilClient#init}: the reload listener and the once-a-second check. */
    static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) rm -> {
            MASKS.clear();
            STRIPS.clear();
            sinceCheck = 1000;                                                         // check at the next tick
        }));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        if (++sinceCheck < 20) return;
        sinceCheck = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getResourceManager() == null) return;
        try {
            apply(mc, mc.getTextureManager(), mc.getResourceManager());
        } catch (RuntimeException | LinkageError failed) {
            LOG.warn("[anime_multiverse] the devil icon check failed", failed);
        }
    }

    private static void apply(Minecraft mc, TextureManager tm, ResourceManager rm) {
        var own = DevilClient.ownSync();
        DevilAttribute attr = own == null ? DevilAttribute.NONE : DevilAttribute.byOrdinal(own.attribute());
        if (own == null || own.tier() <= 0 || attr == DevilAttribute.NONE || !DevilClientConfig.animatedIcons() || DevilClientConfig.classicLook()) {
            release(tm, MAGIC_ICON);
            release(tm, BOND_ICON);
            shownKey = "";
            return;
        }
        MagicType magic = null;
        try {
            var g = GrimoirePages.grimoireOf(mc.player);
            if (g.isPresent()) magic = GrimoirePages.magicOf(g.get());
        } catch (RuntimeException noGrimoire) {
            // the devil alone
        }
        String magicKey = magic == null ? null : magic.name().toLowerCase(java.util.Locale.ROOT);
        String key = EmblemComposer.cacheKey(attr.key(), magicKey);
        boolean changed = !key.equals(shownKey);
        ensure(tm, rm, MAGIC_ICON, key, attr, magic, magicKey, changed);
        ensure(tm, rm, BOND_ICON, key, attr, magic, magicKey, changed);
        shownKey = key;
    }

    private static void ensure(TextureManager tm, ResourceManager rm, ResourceLocation at, String key, DevilAttribute attr, MagicType magic, String magicKey, boolean changed) {
        AbstractTexture now = tm.getTexture(at, null);
        AnimatedIconTexture mine = MINE.get(at);
        if (!changed && mine != null && now == mine) return;                         // ours is still there
        int[] strip = STRIPS.get(key);
        if (strip == null) {
            int[] devil = mask(rm, EmblemComposer.devilPath(attr.key()));
            if (devil == null) return;                                               // no emblem: the plain icon stays
            int[] mag = magicKey == null ? null : mask(rm, EmblemComposer.magicPath(magicKey));
            strip = EmblemComposer.strip(devil, DualRules.color(attr), mag, magic == null ? 0 : EmblemComposer.soulColor(magic.soulType()), EmblemComposer.FRAMES);
            if (STRIPS.size() >= EmblemComposer.MAX_CACHE) STRIPS.clear();           // a tiny cache: a change of pair is rare
            STRIPS.put(key, strip);
        }
        int n = EmblemComposer.SIZE;
        NativeImage img = new NativeImage(n, n * EmblemComposer.FRAMES, false);
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < n; x++) {
                int a = strip[y * n + x];
                img.setPixelRGBA(x, y, (a & 0xFF00FF00) | ((a >> 16) & 255) | ((a & 255) << 16));          // ARGB -> ABGR
            }
        AnimatedIconTexture t = AnimatedIconTexture.of(img, EmblemComposer.FRAME_TICKS, false);
        if (t == null) return;
        tm.register(at, t);                                                          // replaces (and releases) the old texture
        MINE.put(at, t);
    }

    private static void release(TextureManager tm, ResourceLocation at) {
        AnimatedIconTexture mine = MINE.remove(at);
        if (mine != null && tm.getTexture(at, null) == mine) tm.release(at);        // back to the plain PNG
    }

    /** The 16 x 16 ARGB mask of an emblem file, read once. */
    private static int[] mask(ResourceManager rm, String path) {
        if (MASKS.containsKey(path)) return MASKS.get(path);
        int[] out = null;
        Optional<Resource> res = rm.getResource(ResourceLocation.fromNamespaceAndPath(NS, path));
        if (res.isPresent()) {
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                if (img.getWidth() == EmblemComposer.EMB && img.getHeight() == EmblemComposer.EMB) {
                    out = new int[EmblemComposer.EMB * EmblemComposer.EMB];
                    for (int y = 0; y < EmblemComposer.EMB; y++)
                        for (int x = 0; x < EmblemComposer.EMB; x++) {
                            int p = img.getPixelRGBA(x, y);
                            out[y * EmblemComposer.EMB + x] = (p & 0xFF00FF00) | ((p >> 16) & 255) | ((p & 255) << 16);   // ABGR -> ARGB
                        }
                }
            } catch (IOException | RuntimeException unreadable) {
                LOG.warn("[anime_multiverse] could not read the emblem {}", path, unreadable);
            }
        }
        if (MASKS.size() >= 160) MASKS.clear();                                      // 122 emblems exist; this only guards against a runaway
        MASKS.put(path, out);
        return out;
    }
}
