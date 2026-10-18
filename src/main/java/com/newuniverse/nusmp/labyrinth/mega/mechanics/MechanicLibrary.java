package com.newuniverse.nusmp.labyrinth.mega.mechanics;

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
 * Why: the library is data (a datapack may add or replace a file), so the game reads it again whenever data reloads and must never crash on a bad file. This class owns the one
 * immutable snapshot everything else reads (the catalogue, every definition sorted by id, an index by id) and the reload listener that swaps it. Files are
 * {@code data/nusmp/mega/mechanics/ops.json} (the catalogue) and every other {@code *.json} in that folder (a category file written by the generator, or a datapack's own). A
 * mechanic whose id repeats is skipped with a warning (the first, in file-name order, wins); every parser problem is logged with the file's name. Snapshots are swapped as a
 * whole (volatile), so the server thread never sees a half-read library.
 */
public final class MechanicLibrary {
    private MechanicLibrary() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "mega/mechanics/";

    private static volatile OpCatalog catalog;
    private static volatile List<MechanicDef> all = List.of();
    private static volatile Map<String, MechanicDef> byId = Map.of();
    private static volatile List<String> problems = List.of();
    private static volatile int version;

    /** The catalogue, or null before the first load. */
    public static OpCatalog catalog() { return catalog; }

    /** Every mechanic, sorted by id. */
    public static List<MechanicDef> all() { return all; }

    public static MechanicDef get(String id) { return id == null ? null : byId.get(id); }

    public static int size() { return all.size(); }

    /** The problems found by the last load (for the verify command and the self-test). */
    public static List<String> problems() { return problems; }

    /** Counts up on every reload, so caches built from the library know when to forget. */
    public static int version() { return version; }

    public static List<MechanicDef> of(MechCategory c) {
        List<MechanicDef> out = new ArrayList<>();
        for (MechanicDef d : all) if (d.category() == c) out.add(d);
        return out;
    }

    /** Installs a snapshot (the reload listener, and the tests / self-tests that build a library by hand). */
    public static void install(OpCatalog cat, List<MechanicDef> defs, List<String> probs) {
        List<MechanicDef> sorted = new ArrayList<>(defs);
        sorted.sort(Comparator.comparing(MechanicDef::id));
        Map<String, MechanicDef> idx = new LinkedHashMap<>();
        for (MechanicDef d : sorted) idx.putIfAbsent(d.id(), d);
        catalog = cat;
        all = List.copyOf(sorted);
        byId = Map.copyOf(idx);
        problems = List.copyOf(probs);
        version++;
    }

    /** The listener the registry adds through {@code AddReloadListenerEvent}. */
    public static final class Reload implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            List<String> probs = new ArrayList<>();
            OpCatalog cat = null;
            List<MechanicDef> defs = new ArrayList<>();
            try {
                Map<ResourceLocation, List<Resource>> files = manager.listResourceStacks("mega", rl -> rl.getNamespace().equals("nusmp") && rl.getPath().startsWith(DIR) && rl.getPath().endsWith(".json")
                        && rl.getPath().indexOf('/', DIR.length()) < 0);
                List<ResourceLocation> keys = new ArrayList<>(files.keySet());
                keys.sort(Comparator.comparing(ResourceLocation::toString));
                ResourceLocation opsKey = ResourceLocation.fromNamespaceAndPath("nusmp", DIR + "ops.json");
                List<Resource> opsStack = files.get(opsKey);
                if (opsStack != null && !opsStack.isEmpty()) {
                    String text = read(opsStack.get(opsStack.size() - 1), opsKey);
                    if (text != null) {
                        try { cat = OpCatalog.parse(text); } catch (RuntimeException e) { probs.add("ops.json: " + e.getMessage()); }
                    }
                }
                if (cat == null) probs.add("ops.json is missing or unusable: no mechanic can be validated");
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (ResourceLocation key : keys) {
                    if (key.equals(opsKey)) continue;
                    List<Resource> stack = files.get(key);
                    if (stack == null || stack.isEmpty()) continue;
                    String text = read(stack.get(stack.size() - 1), key);
                    if (text == null) continue;
                    MechanicParser.Parsed p = MechanicParser.parseFile(key.getPath().substring(DIR.length()), text, cat);
                    probs.addAll(p.problems());
                    for (MechanicDef d : p.defs()) {
                        if (seen.add(d.id())) defs.add(d);
                        else probs.add(d.id() + ": defined twice (the later file's copy is ignored)");
                    }
                }
            } catch (RuntimeException e) {
                probs.add("the mechanics could not be listed: " + e);
            }
            install(cat, defs, probs);
            for (String s : probs) LOG.warn("[nusmp] mechanics: {}", s);
            LOG.info("[nusmp] Dungeon mechanics: {} loaded{}", defs.size(), cat != null && cat.total() > 0 && cat.total() != defs.size() ? " (the generator wrote " + cat.total() + ")" : "");
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
