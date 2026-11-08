package com.newuniverse.nusmp.beastwill;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: reads every {@code data/nusmp/beast_will/<id>.json} at each data reload (3.45.0), hands the text to {@link BeastWillSchema} (the validator the unit test runs too) and installs the result into
 * {@link BeastWillCatalog} in one step. A rejected file is logged with its reasons and left out; nothing here is ever fatal. A data pack can add a will or replace one of the four by shipping a file
 * with the same name. Registered through {@code AddReloadListenerEvent} by {@link BeastWillBoot}.
 */
public final class BeastWillLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String ROOT = "beast_will";
    /** 3.60.0: the 27 wills as they were before the remake, kept whole; the server setting {@code classicWills} makes the game read this folder instead of {@link #ROOT}. */
    public static final String CLASSIC_ROOT = "beast_will_classic";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, BeastWillDef> out = new LinkedHashMap<>();
        List<String> rejected = new ArrayList<>();
        try {
            String root = BeastWillConfig.classicWills() ? CLASSIC_ROOT : ROOT;
            Map<ResourceLocation, Resource> files = manager.listResources(root, rl -> rl.getNamespace().equals("nusmp") && rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(files.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                String id = idOf(root, rl.getPath());
                if (id == null) continue;
                BeastWillSchema.Parsed p;
                try (InputStream in = files.get(rl).open()) {
                    p = BeastWillSchema.parse(id, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException | RuntimeException e) {
                    p = new BeastWillSchema.Parsed(null, List.of(id + ": could not be read: " + e));
                }
                if (p.ok()) out.put(id, p.def());
                else {
                    rejected.add(id);
                    LOG.warn("[nusmp] beast will file {} was REJECTED and the will is left out:\n  {}", rl, String.join("\n  ", p.problems()));
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] beast wills could not scan their data folder", e);
        }
        BeastWillCatalog.install(out);
        BeastWillManager.onReload();
        LOG.info("[nusmp] beast wills: {} loaded, {} rejected", out.size(), rejected.size());
    }

    /** {@code beast_will/lion_will.json} gives {@code lion_will}; a file in a sub-folder is not a will's. */
    static String idOf(String path) { return idOf(ROOT, path); }

    static String idOf(String root, String path) {
        String p = path.startsWith(root + "/") ? path.substring(root.length() + 1) : path;
        if (!p.endsWith(".json")) return null;
        p = p.substring(0, p.length() - 5);
        return p.isEmpty() || p.contains("/") ? null : p;
    }
}
