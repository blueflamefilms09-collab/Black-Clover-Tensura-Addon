package com.newuniverse.nusmp.labyrinth.mega.foundry;

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
 * Why: the library (abilities, archetypes) and the roster (400 mob definitions) are data packs' business, so the game reads them again on every data reload and must never crash on
 * a bad file. This class owns the two immutable snapshots everything else reads ({@link #library()}, {@link #roster()}) and the reload listener that replaces them. The files
 * are {@code data/nusmp/mega/foundry/_abilities.json}, {@code _archetypes.json} and one {@code <id>.json} per mob; only the {@code nusmp} namespace is read, so a datapack
 * overrides a file by shipping the same path. Every problem the pure parsers report is logged with the file's name; one bad file never costs the others. Snapshots are swapped
 * whole (volatile fields), so the server thread never sees a half-read roster.
 */
public final class FoundryData {
    private FoundryData() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String FOLDER = "mega/foundry";

    private static volatile FoundryLibrary library = FoundryLibrary.EMPTY;
    private static volatile FoundryRoster roster = FoundryRoster.EMPTY;
    private static volatile int version;

    public static FoundryLibrary library() { return library; }

    public static FoundryRoster roster() { return roster; }

    /** Counts up on every reload (a cache built from the roster knows when to forget). */
    public static int version() { return version; }

    /** Replaces the snapshots (the reload listener, and the tests of the glue). */
    public static void install(FoundryLibrary lib, FoundryRoster r) {
        library = lib == null ? FoundryLibrary.EMPTY : lib;
        roster = r == null ? FoundryRoster.EMPTY : r;
        version++;
    }

    /** The archetype of a definition, or the neutral one when the data does not know it. */
    public static FoundryArchetype archetypeOf(FoundryDef d) {
        FoundryArchetype a = library.archetype(d.archetype());
        return a == null ? FoundryArchetype.FALLBACK : a;
    }

    /** The abilities of a definition, in the order of its file (unknown ids are skipped). */
    public static List<FoundryAbility> abilitiesOf(FoundryDef d) {
        List<FoundryAbility> out = new ArrayList<>();
        for (String id : d.abilities()) {
            FoundryAbility a = library.ability(id);
            if (a != null) out.add(a);
        }
        return out;
    }

    /** The listener the registry adds through {@code AddReloadListenerEvent}. */
    public static final class Reload implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            FoundryLibrary lib = FoundryLibrary.EMPTY;
            List<FoundryDef> defs = new ArrayList<>();
            try {
                Map<ResourceLocation, Resource> files = manager.listResources(FOLDER, rl -> rl.getNamespace().equals("nusmp") && rl.getPath().endsWith(".json"));
                List<ResourceLocation> keys = new ArrayList<>(files.keySet());
                keys.sort(Comparator.comparing(ResourceLocation::toString));
                String abilitiesText = null, archetypesText = null;
                for (ResourceLocation key : keys) {
                    String file = key.getPath().substring(key.getPath().lastIndexOf('/') + 1);
                    if (file.equals("_abilities.json")) abilitiesText = read(files.get(key), key);
                    else if (file.equals("_archetypes.json")) archetypesText = read(files.get(key), key);
                }
                var abilities = abilitiesText == null ? new FoundryLibrary.Parsed<FoundryAbility>(Map.of(), List.of("_abilities.json is missing")) : FoundryLibrary.parseAbilities(abilitiesText);
                var archetypes = archetypesText == null ? new FoundryLibrary.Parsed<FoundryArchetype>(Map.of(), List.of("_archetypes.json is missing")) : FoundryLibrary.parseArchetypes(archetypesText);
                for (String p : abilities.problems()) LOG.warn("[nusmp] foundry abilities: {}", p);
                for (String p : archetypes.problems()) LOG.warn("[nusmp] foundry archetypes: {}", p);
                lib = new FoundryLibrary(abilities.rows(), archetypes.rows());
                for (ResourceLocation key : keys) {
                    String file = key.getPath().substring(key.getPath().lastIndexOf('/') + 1);
                    if (file.startsWith("_")) continue;
                    String text = read(files.get(key), key);
                    if (text == null) continue;
                    FoundryDef.Parsed p = FoundryDef.parse(file, text, lib);
                    for (String problem : p.problems()) LOG.warn("[nusmp] foundry mob: {}", problem);
                    if (p.def() != null) defs.add(p.def());
                }
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] The Mob Foundry's data could not be listed: {}", e.toString());
            }
            install(lib, new FoundryRoster(defs));
            LOG.info("[nusmp] Mob Foundry: {} mobs ({} holy, {} abyssal), {} abilities, {} archetypes", roster.size(), roster.count(com.newuniverse.nusmp.labyrinth.mega.Polarity.HOLY),
                    roster.count(com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL), library.abilities().size(), library.archetypes().size());
        }

        private static String read(Resource res, ResourceLocation key) {
            if (res == null) return null;
            try (InputStream in = res.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException e) {
                LOG.warn("[nusmp] {} could not be read: {}", key, e.toString());
                return null;
            }
        }
    }
}
