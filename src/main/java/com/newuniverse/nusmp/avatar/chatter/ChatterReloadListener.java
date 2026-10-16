package com.newuniverse.nusmp.avatar.chatter;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Why: loads {@code data/<ns>/avatar_chatter/*.json} once per data reload into the immutable {@link ChatterDictionary} snapshot, so speaking in
 * game never touches the disk. Registered by {@code AvatarAi.registerEvents} through AddReloadListenerEvent; a broken file is skipped with a log
 * line, never fatal.
 */
public final class ChatterReloadListener implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String ROOT = "avatar_chatter";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<ChatterDictionary.Parsed> files = new ArrayList<>();
        try {
            Map<ResourceLocation, List<Resource>> found = manager.listResourceStacks(ROOT, rl -> rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(found.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                for (Resource res : found.get(rl)) {                                    // lowest priority pack first, later packs add lines
                    try (InputStream in = res.open()) {
                        ChatterDictionary.Parsed p = ChatterDictionary.parseFile(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        if (p.ok()) files.add(p);
                        else LOG.warn("[nusmp] Avatar chatter file {} skipped: {}", rl, p.error());
                    } catch (IOException | RuntimeException e) {
                        LOG.warn("[nusmp] Avatar chatter file {} could not be read: {}", rl, e.toString());
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Avatar chatter could not scan its data folder", e);
        }
        ChatterDictionary.install(ChatterDictionary.build(files));
    }
}
