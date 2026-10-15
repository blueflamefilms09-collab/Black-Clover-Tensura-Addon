package com.newuniverse.nusmp.client.ui.codex;

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
 * Why: this is how the Codex updates itself. On every client resource reload (game start, F3+T, a pack switched in the menu) it scans
 * {@code assets/<any namespace>/codex/<section>/<name>.json}, parses each file ({@link CodexEntryParser}), merges the packs (lowest priority first,
 * the later one wins, see {@link CodexMerger}) and installs the result in {@link CodexData}. Because it asks the resource manager for the whole
 * stack of a path ({@code listResourceStacks}), not only the top pack, a resource pack can add an entry, overwrite one, or delete one ({@code
 * "remove": true}) without any Java. A broken file is logged and skipped; it never stops the other files or the reload. Registered once
 * through the client {@code RegisterClientReloadListenersEvent} (see {@link CodexClient}).
 */
public final class CodexReloadListener implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String ROOT = "codex";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<CodexEntryData> all = new ArrayList<>();
        try {
            Map<ResourceLocation, List<Resource>> found = manager.listResourceStacks(ROOT, rl -> rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(found.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                String path = rl.getPath().substring(ROOT.length() + 1, rl.getPath().length() - ".json".length());      // "rules/relics_exclusive"
                int slash = path.indexOf('/');
                if (slash <= 0) continue;                                                // a file straight under codex/ belongs to no section
                String section = path.substring(0, slash);
                String id = rl.getNamespace() + ":" + path.substring(slash + 1);
                for (Resource res : found.get(rl)) {                                    // lowest priority pack first
                    String json;
                    try (InputStream in = res.open()) {
                        json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException | RuntimeException e) {
                        LOG.warn("[nusmp] Codex file {} could not be read: {}", rl, e.toString());
                        continue;
                    }
                    CodexEntryParser.Result r = CodexEntryParser.parse(section, id, json);
                    if (r.ok()) all.add(r.entry());
                    else LOG.warn("[nusmp] Codex file {} skipped: {}", rl, r.error());
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The Codex could not scan its data folder", e);
        }
        CodexData.replace(CodexMerger.merge(all));
    }
}
