package com.newuniverse.nusmp.dungeon.placed;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the server-side events of the player-placed exemption. A player's placement (a single block, or the several blocks of a bed, a door or a tall
 * plant, which arrive as one multi-place event) is remembered at the LOWEST priority and only if nothing cancelled it, so a refused placement leaves no
 * mark. A chunk sent to a player carries that chunk's marks (only if it has any), a chunk dropped from a player's view makes the client forget them, and a
 * stopped server forgets its cached handles. Nothing here ever throws into the game loop.
 */
public final class PlacedEvents {
    private PlacedEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOWEST, PlacedEvents::onPlace);
        gameBus.addListener(PlacedEvents::onChunkSent);
        gameBus.addListener(PlacedEvents::onChunkUnwatched);
        gameBus.addListener((ServerStoppedEvent e) -> PlayerPlaced.clearCache());
    }

    /** The positions a placement event covers: the placed block and, for a multi-place event, every block it replaced. */
    static List<BlockPos> positions(BlockEvent.EntityPlaceEvent e) {
        List<BlockPos> out = new ArrayList<>();
        out.add(e.getPos().immutable());
        if (e instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot s : multi.getReplacedBlockSnapshots()) {
                BlockPos p = s.getPos().immutable();
                if (!out.contains(p)) out.add(p);
            }
        }
        return out;
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        try {
            LevelAccessor acc = e.getLevel();
            if (!(acc instanceof ServerLevel level) || !PlayerPlaced.active(level)) return;
            markPlacement(level, e.getEntity(), positions(e));
        } catch (RuntimeException ex) {
            failed(ex);
        }
    }

    /** The decision behind the event, public so the self-test drives it without constructing an event: a player's placement is remembered, anything else's is not. */
    public static int markPlacement(ServerLevel level, Entity who, List<BlockPos> positions) {
        if (!PlacedRules.remembers(PlayerPlaced.active(level), who instanceof ServerPlayer)) return 0;
        int n = 0;
        for (BlockPos p : positions) if (PlayerPlaced.mark(level, p, (ServerPlayer) who)) n++;
        return n;
    }

    private static void onChunkSent(ChunkWatchEvent.Sent e) {
        try {
            ServerLevel level = e.getLevel();
            if (!PlayerPlaced.active(level) || !PlacedConfig.get(PlacedConfig.SYNC)) return;
            PlacedBlocksData d = PlacedBlocksData.peek(level);
            if (d == null || d.set().isEmpty()) return;
            int cx = e.getPos().x, cz = e.getPos().z;
            if (d.set().chunkSize(cx, cz) == 0) return;
            PlacedNet.sendChunk(e.getPlayer(), level, cx, cz, d.set().chunkKeys(cx, cz));
        } catch (RuntimeException ex) {
            failed(ex);
        }
    }

    private static void onChunkUnwatched(ChunkWatchEvent.UnWatch e) {
        try {
            ServerLevel level = e.getLevel();
            if (!PlayerPlaced.active(level) || !PlacedConfig.get(PlacedConfig.SYNC)) return;
            PlacedBlocksData d = PlacedBlocksData.peek(level);
            if (d == null || d.set().isEmpty()) return;
            int cx = e.getPos().x, cz = e.getPos().z;
            if (d.set().chunkSize(cx, cz) == 0) return;
            PlacedNet.sendChunk(e.getPlayer(), level, cx, cz, new long[0]);
        } catch (RuntimeException ex) {
            failed(ex);
        }
    }

    private static void failed(RuntimeException ex) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] the player-placed block exemption hit a fault and carries on (logged once): {}", ex.toString());
    }
}
