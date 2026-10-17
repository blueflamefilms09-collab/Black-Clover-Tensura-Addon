package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: {@link ThreatInput} is the pure rule, and something has to ask the world the one question it needs: which floor are these players on? The answer comes
 * from the facade ({@link MegaDungeon#floorAt}: the signed floor of the room the player stands in, the first floor of the sector when the rooms were not banded,
 * 0 in the gaps between sectors), the deepest member decides, and the config switch {@code floorThreat} chooses between the floor and the sector's own threat.
 * Both the Dungeon Master's roll and the wandering bosses read their threat here, so the two can never disagree about how deep a party is. Server thread only.
 */
final class FloorThreat {
    private FloorThreat() {}

    /** The signed floor of the deepest of these players (largest distance from the Hub), 0 when none of them stands in a sector. */
    static int floorOf(ServerLevel level, Iterable<? extends ServerPlayer> players) {
        int best = 0;
        for (ServerPlayer p : players) best = ThreatInput.deepest(best, MegaDungeon.floorAt(level, p.blockPosition()));
        return best;
    }

    /** The threat the dice read for these players in this sector: their floor's when the rule is on and the floor is known, else the sector's. */
    static int of(ServerLevel level, Sector sector, Iterable<? extends ServerPlayer> players) {
        boolean useFloor = EncConfig.get(EncConfig.FLOOR_THREAT);
        return ThreatInput.of(sector.threat, useFloor ? floorOf(level, players) : 0, useFloor);
    }
}
