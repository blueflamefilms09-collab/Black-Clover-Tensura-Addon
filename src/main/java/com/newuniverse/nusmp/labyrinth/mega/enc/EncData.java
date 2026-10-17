package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
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
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: the encounter tables and the roster of wandering bosses are data packs' business (the owner will keep tuning what lives in which region),
 * so the game must read them again whenever data reloads and must never crash on a bad file. This class owns the two immutable snapshots the
 * rest of the package reads ({@link #table} for a theme, {@link #roster}) and the reload listener that replaces them. The files are
 * {@code data/nusmp/mega/encounters/<name>.json} (one per theme plus {@code default}; a theme file REPLACES the default for sectors of that
 * theme, a theme without a file borrows the table of its road, {@code radiant} or {@code abyss}, and only then the default answers) and
 * {@code data/nusmp/mega/wanderers.json}. Only the {@code nusmp} namespace is
 * read, so a datapack overrides a file by shipping the same path. When several packs ship a file the highest-priority one wins, as for any data
 * file. Every problem the pure parsers report is written to the log with the file's name; a file that is not JSON at all is skipped, and the
 * snapshot keeps working with what is left. Snapshots are swapped as a whole (volatile fields), so the server thread never sees a half-read table.
 */
public final class EncData {
    private EncData() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** {@code WANDERERS_ARC} (1.61.0) is an ADDITIVE file: the characters of the arcs that may walk the halls, merged after the main one by {@link WandererRoster#merge}. */
    private static final String ENCOUNTERS = "mega/encounters/", WANDERERS = "mega/wanderers.json", WANDERERS_ARC = "mega/wanderers_arc.json";

    private static volatile Map<String, EncounterTable> tables = Map.of();
    private static volatile WandererRoster roster = WandererRoster.EMPTY;
    private static volatile int version;

    /** The table for sectors of this theme on no particular road: its own file, else the default, else an empty table (no encounters). */
    public static EncounterTable table(RegionTheme theme) { return table(theme, null); }

    /**
     * The table for a sector of this theme on this road: its own file; else the road's table ({@code radiant} for the Holy road, {@code abyss} for the Abyssal
     * one); else the default; else an empty table (no encounters). The order is the pure rule {@link TableChoice#pick}.
     */
    public static EncounterTable table(RegionTheme theme, Polarity polarity) {
        Map<String, EncounterTable> t = tables;
        String name = TableChoice.pick(t.keySet(), theme == null ? null : theme.id(), polarity);
        EncounterTable chosen = name == null ? null : t.get(name);
        return chosen != null ? chosen : EncounterTable.EMPTY;
    }

    /** A table by file name (for the admin command), or null. */
    public static EncounterTable byName(String name) { return tables.get(name); }

    /** The names of all loaded tables, sorted. */
    public static Set<String> tableNames() { return new TreeSet<>(tables.keySet()); }

    public static WandererRoster roster() { return roster; }

    /** Counts up on every reload, so caches built from the tags and the tables know when to forget. */
    public static int version() { return version; }

    /** Replaces the snapshots (the reload listener, and the tests of the glue). */
    static void install(Map<String, EncounterTable> newTables, WandererRoster newRoster) {
        tables = Map.copyOf(newTables);
        roster = newRoster == null ? WandererRoster.EMPTY : newRoster;
        version++;
    }

    /** The listener the registry adds through {@code AddReloadListenerEvent}. */
    public static final class Reload implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            Map<String, EncounterTable> found = new LinkedHashMap<>();
            WandererRoster wanderers = WandererRoster.EMPTY, extra = WandererRoster.EMPTY;
            try {
                Map<ResourceLocation, List<Resource>> files = manager.listResourceStacks("mega", rl -> rl.getNamespace().equals("nusmp")
                        && (rl.getPath().equals(WANDERERS) || rl.getPath().equals(WANDERERS_ARC) || (rl.getPath().startsWith(ENCOUNTERS) && rl.getPath().endsWith(".json") && rl.getPath().indexOf('/', ENCOUNTERS.length()) < 0)));
                List<ResourceLocation> keys = new ArrayList<>(files.keySet());
                keys.sort(Comparator.comparing(ResourceLocation::toString));
                for (ResourceLocation key : keys) {
                    List<Resource> stack = files.get(key);
                    if (stack == null || stack.isEmpty()) continue;
                    String text = read(stack.get(stack.size() - 1), key);                       // the highest-priority pack wins
                    if (text == null) continue;
                    if (key.getPath().equals(WANDERERS) || key.getPath().equals(WANDERERS_ARC)) {
                        WandererRoster.Parsed p = WandererRoster.parse(text);
                        for (String problem : p.problems()) LOG.warn("[nusmp] {} ({})", problem, key.getPath());
                        if (p.roster() != null) {
                            if (key.getPath().equals(WANDERERS)) wanderers = p.roster();
                            else extra = p.roster();
                        }
                    } else {
                        String name = key.getPath().substring(ENCOUNTERS.length(), key.getPath().length() - 5);
                        EncounterTableParser.Parsed p = EncounterTableParser.parse(name, text);
                        for (String problem : p.problems()) LOG.warn("[nusmp] {}", problem);
                        if (p.table() != null) found.put(name, p.table());
                    }
                }
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] The Dungeon Master's data could not be listed: {}", e.toString());
            }
            install(found, WandererRoster.merge(wanderers, extra));
            LOG.info("[nusmp] Dungeon Master: {} encounter tables ({}), {} wandering boss kinds", found.size(), String.join(", ", new TreeSet<>(found.keySet())), roster.entries().size());
        }

        private static String read(Resource res, ResourceLocation key) {
            try (InputStream in = res.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException e) {
                LOG.warn("[nusmp] {} could not be read: {}", key, e.toString());
                return null;
            }
        }
    }
}
