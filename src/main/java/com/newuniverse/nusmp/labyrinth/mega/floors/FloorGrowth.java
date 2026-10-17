package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorListener;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.slf4j.Logger;

/**
 * Why: in the floor model (1.58.0) a floor is "reached" the moment a party's first player stands in its sector, by the arrival stairs, a Hub gate or an admin's
 * teleport, and that is exactly when the floor AFTER it should start to be built, so that the stairs are normally ready by the time the party has found them (the
 * brief: the next floor is generated when a party ENTERS a floor). The floor tracker already announces every change of floor to the {@link FloorListener}s, so this is
 * one more listener: it asks the expansion queue to queue the next floor of the sector the player stands in. The queue decides everything else (a boss floor waits for its
 * Crucible, a floor that already has a successor, the limit on the size of the dungeon, a sector of the old model: it answers "no" without a sound). A fake player (a
 * machine, or the stand-in of a self-test) is no party and never starts a floor, so a test that walks one through the floors cannot grow the dungeon. It never throws.
 */
public final class FloorGrowth implements FloorListener {
    private static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onFloorEntered(ServerPlayer player, int floor, int previous) {
        try {
            if (player instanceof FakePlayer) return;
            ServerLevel level = player.serverLevel();
            Sector s = MegaDungeon.sectorAt(level, player.blockPosition());
            if (s != null) ExpansionQueue.get().onFloorReached(level, s);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The next floor could not be queued when a player entered floor {}: {}", floor, e.toString());
        }
    }
}
