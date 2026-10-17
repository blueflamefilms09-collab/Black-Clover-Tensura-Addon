package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Why: loads {@code data/<ns>/mega/compat/*.json} once per data reload into the immutable {@link CompatTable} that {@link ModCompatProcessor}
 * reads while a sector is being built, so placing a piece never touches the disk. Reload-safe: every reload builds a fresh table and swaps one
 * volatile reference, so a processor running on the server thread during a {@code /reload} sees either the old table or the new one, never a
 * half-built one. Files are read in sorted name order (the result must not depend on pack or file-system order), a file whose mod is not loaded
 * is dropped here (so with neither compat mod installed the table is EMPTY and the processor's first check ends the work), and a broken file is
 * logged and skipped: a typo in one datapack must never stop the dungeon from generating, it only means no swap. A datapack that wants to fix
 * or extend a table replaces the file of the same name (the highest-priority pack wins, as for any data file).
 */
public final class CompatLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String ROOT = "mega/compat";

    private static volatile CompatTable table = CompatTable.EMPTY;
    private static volatile int filesSeen, filesUsed;

    /** The current table (EMPTY until the first data reload, and whenever no compat mod is loaded). */
    public static CompatTable table() { return table; }

    /**
     * Replaces the table and returns the previous one. For tests and the in-game self-tests, which swap in a table that points at blocks that always
     * exist and restore the old one afterwards; a data reload replaces it again.
     */
    public static CompatTable install(CompatTable replacement) {
        CompatTable before = table;
        table = replacement == null ? CompatTable.EMPTY : replacement;
        return before;
    }

    /** How many compat files the last data reload found, and how many of them were used (their mod was loaded): the self-test proves the data path with it. */
    public static int filesSeen() { return filesSeen; }

    public static int filesUsed() { return filesUsed; }

    /** Is the mod with this id loaded? Safe before mod loading finished. */
    static boolean modLoaded(String modId) {
        ModList list = ModList.get();
        return list != null && modId != null && list.isLoaded(modId);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<CompatTable.Source> sources = new ArrayList<>();
        int seen = 0;
        try {
            Map<ResourceLocation, Resource> found = manager.listResources(ROOT, rl -> rl.getPath().endsWith(".json"));
            List<ResourceLocation> keys = new ArrayList<>(found.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation rl : keys) {
                seen++;
                try (Reader reader = new InputStreamReader(found.get(rl).open(), StandardCharsets.UTF_8)) {
                    CompatTable.Source source = CompatJson.parse(JsonParser.parseReader(reader));
                    if (source == null) {
                        LOG.warn("[nusmp] Mod-compat file {} skipped: it needs a \"mod\" and a \"swaps\" object", rl);
                    } else if (modLoaded(source.mod())) {
                        sources.add(source);
                    }
                } catch (IOException | RuntimeException e) {
                    LOG.warn("[nusmp] Mod-compat file {} could not be read and was skipped: {}", rl, e.toString());
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Mod-compat data could not be scanned; no furniture will be swapped", e);
        }
        table = CompatTable.of(sources);
        filesSeen = seen;
        filesUsed = sources.size();
        if (!table.isEmpty()) LOG.info("[nusmp] Mod compat for the Safe Zones: {} furniture hints are available ({})", table.hints().size(), table.hints());
    }
}
