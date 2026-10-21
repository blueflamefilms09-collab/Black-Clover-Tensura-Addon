package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Why: the telegraph table ({@code data/nusmp/mega/poise/telegraphs.json}) is a data pack's business like the foundry's own library, so it is read again on every data reload and a bad file
 * never crashes the game: this class owns the one immutable snapshot ({@link #library()}) everything reads, and the reload listener that replaces it. A datapack overrides the file by shipping
 * the same path. Every parse problem is logged with its text; with no file at all the table is empty (no telegraph, no ultimate: the foundry mobs fight as they did).
 */
public final class TelegraphData {
    private TelegraphData() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/poise/telegraphs.json");

    private static volatile TelegraphLibrary library = TelegraphLibrary.EMPTY;

    public static TelegraphLibrary library() { return library; }

    /** Replaces the snapshot (the reload listener, and the tests of the glue). */
    public static void install(TelegraphLibrary lib) { library = lib == null ? TelegraphLibrary.EMPTY : lib; }

    /** The listener the registry adds through {@code AddReloadListenerEvent}. */
    public static final class Reload implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            TelegraphLibrary lib = TelegraphLibrary.EMPTY;
            try {
                Optional<Resource> res = manager.getResource(FILE);
                if (res.isEmpty()) {
                    LOG.warn("[nusmp] The telegraph table {} is missing: foundry mobs get no floor warning and no ultimate", FILE);
                } else {
                    try (InputStream in = res.get().open()) {
                        TelegraphLibrary.Parsed p = TelegraphLibrary.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        for (String problem : p.problems()) LOG.warn("[nusmp] telegraphs: {}", problem);
                        lib = p.library();
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.warn("[nusmp] The telegraph table could not be read: {}", e.toString());
            }
            install(lib);
            LOG.info("[nusmp] Poise: {} telegraph kinds, {} overrides, {} ultimates", lib.kinds().size(), lib.overrides().size(), lib.ultimateIds().size());
        }
    }
}
