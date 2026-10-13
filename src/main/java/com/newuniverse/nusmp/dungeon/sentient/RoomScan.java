package com.newuniverse.nusmp.dungeon.sentient;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * A look over one room of the Dungeon, made once when a player walks into it: how many standing places it has (every other block of its
 * chunk column, every height of its band; on the Deep Waterways swimming places count too), and a sample of up to 64 of those that have a
 * wall beside them, the places a wall can give birth into. Reads only the chunk the room is in, and only if it is loaded.
 */
final class RoomScan {
    private RoomScan() {}

    /** The most places kept for the births. */
    static final int MAX_WALLED = 64;

    /** How many standing places the room has, and a fair sample of those beside a wall. */
    record Result(int standable, List<SpotPick.Spot> walled) {}

    /** Null if the room's chunk is not loaded (look again later). */
    static Result scan(ServerLevel level, long room, FloorKind kind, Dice dice) {
        if (!Spots.loaded(level, RoomKey.minX(room), RoomKey.minZ(room))) return null;
        int minY = Math.max(level.getMinBuildHeight() + 1, RoomKey.minY(room));
        int maxY = Math.min(level.getMaxBuildHeight() - 3, RoomKey.maxY(room));
        boolean water = kind == FloorKind.WATERWAYS;
        int standable = 0, seen = 0;
        List<SpotPick.Spot> walled = new ArrayList<>();
        for (int x = RoomKey.minX(room); x <= RoomKey.maxX(room); x += 2) {
            for (int z = RoomKey.minZ(room); z <= RoomKey.maxZ(room); z += 2) {
                for (int y = minY; y <= maxY; y++) {
                    boolean fits = Spots.standable(level, x, y, z) || (water && Spots.swimmable(level, x, y, z));
                    if (!fits) continue;
                    standable++;
                    if (Spots.wallBeside(level, x, y, z) == null) continue;
                    seen++;
                    SpotPick.Spot spot = new SpotPick.Spot(x, y, z);
                    if (walled.size() < MAX_WALLED) {
                        walled.add(spot);
                    } else {                                                          // a fair sample of all that were seen (reservoir sampling)
                        int j = dice.nextInt(seen);
                        if (j < MAX_WALLED) walled.set(j, spot);
                    }
                }
            }
        }
        return new Result(standable, walled);
    }
}
