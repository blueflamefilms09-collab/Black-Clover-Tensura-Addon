package com.newuniverse.nusmp.elite.records;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Why: holds the loaded definitions (records, titles, synergies) as ONE immutable {@link Catalog} snapshot that is swapped in at every data reload, so the server thread never sees half a reload, and
 * reads the data files: {@code data/<namespace>/elite/records/*.json}, {@code .../elite/titles/*.json} and {@code .../elite/titles/synergies/*.json} (every namespace, so a datapack adds and a later pack
 * replaces a record, title or synergy with the same id). Problems (a broken entry, an unknown reference) are logged once per reload and kept for {@code /records problems}; they never stop the
 * load. After a reload on a running server every player is told the new definitions.
 */
public final class RecordsCatalog {
    private RecordsCatalog() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final String RECORDS = "elite/records", TITLES = "elite/titles";

    private static volatile Catalog current = Catalog.EMPTY;
    private static volatile List<String> problems = List.of();

    public static Catalog get() { return current; }

    public static List<String> problems() { return problems; }

    /** Swaps the snapshot (used by the reload and by the self-test). */
    public static void install(Catalog c, List<String> found) {
        current = c;
        problems = List.copyOf(found);
    }

    /** The data reload listener: register it with {@code AddReloadListenerEvent}. */
    public static final class Reload implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            List<Catalog.Source> records = read(manager, RECORDS, false), titles = read(manager, TITLES, false), synergies = read(manager, TITLES, true);
            Catalog.Loaded l = Catalog.load(records, titles, synergies);
            install(l.catalog(), l.problems());
            LOG.info("[nusmp] Records: {} records, {} titles, {} synergies loaded ({} problem(s))", l.catalog().records().size(), l.catalog().titles().size(), l.catalog().synergies().size(), l.problems().size());
            for (int i = 0; i < Math.min(20, l.problems().size()); i++) LOG.warn("[nusmp] Records data: {}", l.problems().get(i));
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(() -> RecordsSync.resyncAll(server));
        }
    }

    private static List<Catalog.Source> read(ResourceManager manager, String root, boolean synergies) {
        List<Catalog.Source> out = new ArrayList<>();
        try {
            Map<ResourceLocation, List<Resource>> found = manager.listResourceStacks(root, rl -> rl.getPath().endsWith(".json") && rl.getPath().contains("/synergies/") == synergies);
            List<ResourceLocation> keys = new ArrayList<>(found.keySet());
            keys.sort(Comparator.comparing((ResourceLocation rl) -> rl.getPath()).thenComparing(ResourceLocation::getNamespace));
            for (ResourceLocation rl : keys) {
                for (Resource res : found.get(rl)) {                                 // lowest priority pack first: later packs replace by id
                    try (InputStream in = res.open()) {
                        out.add(new Catalog.Source(rl.toString(), new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                    } catch (IOException | RuntimeException e) {
                        LOG.warn("[nusmp] Records data file {} could not be read: {}", rl, e.toString());
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Records data folder {} could not be scanned", root, e);
        }
        return out;
    }
}
