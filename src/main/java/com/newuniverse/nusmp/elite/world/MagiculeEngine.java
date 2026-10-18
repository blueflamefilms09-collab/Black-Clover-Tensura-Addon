package com.newuniverse.nusmp.elite.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: the heartbeat of the chunk magicule. Every {@code updateTicks} it updates the density of the chunks around each player ({@link MagiculeApi#update}) and runs the slow ore conversion
 * on the dense ones; every {@code syncTicks} it tells the clients ({@link MagiculeSync}). Only chunks that are loaded AND near a player are touched, so a world with nobody in it costs
 * nothing, a chunk nobody visits keeps its last value (and catches up in one step when somebody comes back, see {@link MagiculeModel#step}), and the work per pass is bounded.
 */
public final class MagiculeEngine {
    private MagiculeEngine() {}

    /** Chunks updated per level per pass, whatever the number of players. */
    public static final int MAX_CHUNKS_PER_LEVEL = 256;

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MagiculeEngine::onServerTick);
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> MagiculeSync.forget(e.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent e) -> MagiculeSync.clear());
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        if (!WorldConfig.enabled()) return;
        MinecraftServer s = e.getServer();
        int t = s.getTickCount();
        if (t % WorldConfig.syncTicks() == 3) MagiculeSync.tick(s);
        if (t % WorldConfig.updateTicks() == 5) updatePass(s);
    }

    /** One update pass over every simulated level. Returns the number of chunks updated (for the self-test). */
    public static int updatePass(MinecraftServer s) {
        int total = 0;
        for (ServerLevel level : s.getAllLevels()) {
            if (!WorldGuard.allowed(level) || level.players().isEmpty()) continue;
            Set<Long> done = new HashSet<>();
            int r = WorldConfig.radiusChunks();
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator()) continue;
                ChunkPos c = p.chunkPosition();
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (done.size() >= MAX_CHUNKS_PER_LEVEL) break;
                        ChunkPos cp = new ChunkPos(c.x + dx, c.z + dz);
                        if (!done.add(cp.toLong())) continue;
                        LevelChunk chunk = MagiculeApi.loaded(level, cp);
                        if (chunk == null) continue;
                        updateChunk(level, chunk);
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /** The update of one chunk plus, one time in three, a pass of the ore conversion. Public for the self-test. */
    public static void updateChunk(ServerLevel level, LevelChunk chunk) {
        MagiculeApi.update(level, chunk);
        if (level.getRandom().nextInt(3) == 0) OreConverter.pass(level, chunk, MagiculeApi.data(chunk), level.getRandom());
    }
}
