package com.newuniverse.nusmp.dungeon.sentient;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Looks a place of the Dungeon up in the Nexus and the tiers and says which floor kind it is ({@link FloorKind#at}). */
final class FloorLookup {
    private FloorLookup() {}

    /** The Nexus whose volume holds the position, or null if it is in none (or the lookup failed: the Dungeon must never be brought down by a zone). */
    static NexusInfo nexusAt(ServerLevel level, BlockPos pos) {
        try {
            return NexusZones.containing(level, pos);
        } catch (RuntimeException | LinkageError failure) {
            return null;
        }
    }

    static FloorKind kindAt(ServerLevel level, BlockPos pos) {
        NexusInfo info = nexusAt(level, pos);
        if (info != null) return FloorKind.ofZone(info.zone.name());
        return FloorKind.ofTier(DungeonTier.at(level, pos).name());
    }

    /** The floor kind rooms are born by: like {@link #kindAt}, but a Nexus that is not finished yet (planned, being built, collapsed) births nothing. */
    static FloorKind birthKindAt(ServerLevel level, BlockPos pos) {
        NexusInfo info = nexusAt(level, pos);
        if (info != null) return info.state == NexusState.DONE ? FloorKind.ofZone(info.zone.name()) : FloorKind.LEGACY;
        return FloorKind.ofTier(DungeonTier.at(level, pos).name());
    }
}
