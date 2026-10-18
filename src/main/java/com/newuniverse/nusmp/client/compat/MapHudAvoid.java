package com.newuniverse.nusmp.client.compat;

import com.newuniverse.nusmp.compat.maps.MapMods;
import com.newuniverse.nusmp.compat.maps.MapsClientConfig;
import com.newuniverse.nusmp.compat.maps.MinimapRect;
import net.neoforged.fml.ModList;

/**
 * Why: Xaero's Minimap, FTB Chunks' minimap, JourneyMap and VoxelMap all sit in the TOP-RIGHT corner by default, so a hud element of this mod that defaults to that corner
 * would be drawn under or over the map. This tiny shared helper answers "where is the minimap, if one is installed" and "where must my box go to stay clear of it". It never
 * moves anything by itself, and a layer must call it ONLY for a position it chose as its own default: a position the player picked (the System window's saved anchor and
 * offsets, a layer's own x/y option) always wins and must not go through here. As of this commit NO layer of the mod defaults to the top-right corner (see
 * docs/compat/maps.md section 5), so nothing calls it yet. Detection is by mod id through the mod list (nothing of the map mods is loaded); the size is the client config
 * {@code compat.maps.minimapSize}. Safe on both sides (no client class is touched). The geometry is {@link MinimapRect}, which is unit-tested.
 */
public final class MapHudAvoid {
    private MapHudAvoid() {}

    private static volatile Boolean present;

    /** Is a minimap-drawing map mod installed? Cached once the mod list exists. */
    public static boolean minimapPresent() {
        Boolean p = present;
        if (p != null) return p;
        ModList list = ModList.get();
        if (list == null) return false;                                    // too early: ask again later, cache nothing
        boolean on = MapMods.anyMinimap(MapMods.detect(list::isLoaded));
        present = on;
        return on;
    }

    /** The rectangle the minimap is assumed to occupy on a screen of this GUI size, or null when none is installed or the player switched the avoidance off. */
    public static MinimapRect.Rect rect(int guiWidth, int guiHeight) {
        if (!MapsClientConfig.avoidMinimap() || !minimapPresent()) return null;
        return MinimapRect.topRight(guiWidth, guiHeight, MapsClientConfig.minimapSize());
    }

    /** The x for a box (top-left x, y, width, height) that defaults to the top-right corner: moved left of the minimap if they would overlap, else unchanged. */
    public static int avoidX(int x, int y, int w, int h, int guiWidth, int guiHeight) {
        return MinimapRect.avoidX(x, y, w, h, rect(guiWidth, guiHeight));
    }

    /** The y for a box that cannot move sideways: moved below the minimap if they would overlap, else unchanged. */
    public static int avoidY(int x, int y, int w, int h, int guiWidth, int guiHeight) {
        return MinimapRect.avoidY(x, y, w, h, rect(guiWidth, guiHeight));
    }
}
