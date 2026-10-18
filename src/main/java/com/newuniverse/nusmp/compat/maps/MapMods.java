package com.newuniverse.nusmp.compat.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: one table of the map mods this mod knows about, how to detect each by mod id (soft: the id is only asked of the mod list, nothing of those mods is loaded),
 * and what this mod does for each. Used by {@code /multiverse compat maps}, the minimap helper and the self-test. PURE: detection takes a {@code Predicate<String>}
 * (in the game {@code ModList.get()::isLoaded}). The ids are the owner's list; whether the Fair Play builds really use {@code ...fair} ids is not verified here.
 */
public final class MapMods {
    private MapMods() {}

    /** A map mod family: its display name, the mod ids that mean "it is installed", whether it draws a minimap that sits top-right by default, and what we did for it. */
    public record Mod(String key, String name, List<String> ids, boolean minimap, String does) {}

    public static final List<Mod> ALL = List.of(
            new Mod("xaero_minimap", "Xaero's Minimap", List.of("xaerominimap", "xaerominimapfair"), true,
                    "waypoint chat lines in its share format; block map colours; biome and dimension names; top-right HUD helper"),
            new Mod("xaero_worldmap", "Xaero's World Map", List.of("xaeroworldmap", "xaeroworldmapfair"), false,
                    "same waypoint chat lines (it shares Xaero's waypoint format); dimension profiles audited (roofed Mega-Dungeon, surface Hub)"),
            new Mod("ftb_chunks", "FTB Chunks", List.of("ftbchunks"), true,
                    "block map colours; claim check (claim_dimension_blacklist) with a start-up log line and an op warning; top-right HUD helper"),
            new Mod("journeymap", "JourneyMap", List.of("journeymap"), true,
                    "block map colours; optional [name:\"..\", x:.., y:.., z:.., dim:..] waypoint line (off by default); top-right HUD helper"),
            new Mod("voxelmap", "VoxelMap", List.of("voxelmap"), true,
                    "block map colours only (no waypoint format is written for it); top-right HUD helper"));

    /** A detected map mod and the id that matched. */
    public record Found(Mod mod, String id) {}

    public static List<Found> detect(Predicate<String> loaded) {
        List<Found> out = new ArrayList<>();
        if (loaded == null) return out;
        for (Mod m : ALL) {
            for (String id : m.ids()) {
                boolean on;
                try { on = loaded.test(id); } catch (RuntimeException ex) { on = false; }
                if (on) { out.add(new Found(m, id)); break; }
            }
        }
        return out;
    }

    /** Is any detected mod one that draws a minimap? */
    public static boolean anyMinimap(List<Found> found) {
        for (Found f : found) if (f.mod().minimap()) return true;
        return false;
    }

    public static boolean isLoaded(Predicate<String> loaded, String modKey) {
        for (Found f : detect(loaded)) if (f.mod().key().equals(modKey)) return true;
        return false;
    }
}
