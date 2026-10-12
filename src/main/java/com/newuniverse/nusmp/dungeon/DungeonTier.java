package com.newuniverse.nusmp.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The atmospheric tiers of the Living Dungeon (docs/living_dungeon.md section 3): a function of depth and place. */
public enum DungeonTier {
    UPPER_CATACOMBS("Upper Catacombs", 100f), DEEP_WATERWAYS("Deep Waterways", 100f), VOLCANIC_VENTS("Volcanic Vents", 60f), GOLDEN_CITY("Golden City", 100f);

    public final String label;
    /** The most oxygen the air of this tier can hold. */
    public final float oxygenCeiling;

    DungeonTier(String label, float oxygenCeiling) { this.label = label; this.oxygenCeiling = oxygenCeiling; }

    public static final int UPPER_Y = 24, CITY_Y = -40, VENT_CELL = 112, CITY_CELL = 128;

    /** A stable hash of a cell of the map, 0..99. */
    private static int cell(int cx, int cz, int salt) {
        long h = cx * 0x9E3779B97F4A7C15L ^ cz * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) Math.floorMod(h, 100L);
    }

    public static DungeonTier at(int x, int y, int z) {
        if (y < CITY_Y && cell(Math.floorDiv(x, CITY_CELL), Math.floorDiv(z, CITY_CELL), 7) < 18) return GOLDEN_CITY;
        if (y >= CITY_Y && cell(Math.floorDiv(x, VENT_CELL), Math.floorDiv(z, VENT_CELL), 3) < 28) return VOLCANIC_VENTS;
        return y >= UPPER_Y ? UPPER_CATACOMBS : DEEP_WATERWAYS;
    }

    public static DungeonTier at(Level level, BlockPos p) { return at(p.getX(), p.getY(), p.getZ()); }
}
