package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.dungeon.EcosystemData;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Anti-Bird Compass looks at (docs/living_dungeon.md section 3): the biomass of the loaded chunks within {@link #RADIUS} chunks of
 * the holder, fed to the pure {@link ClusterFinder}. Chunks that are not loaded, or that never had an ecosystem written, are left out
 * (reading never creates data).
 */
final class CompassSense {
    private CompassSense() {}

    static final int RADIUS = 6;

    /**
     * The middle of the nearest cluster of life round this player, or null. The needle only cares about x and z, so the height is the floor of
     * the player's 16-block band: the stored position does not change (and the stack is not sent again) every time the player jumps.
     */
    static BlockPos cluster(ServerLevel level, ServerPlayer p) {
        int pcx = p.chunkPosition().x, pcz = p.chunkPosition().z;
        List<ClusterFinder.Sample> samples = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                if (chunk == null) continue;
                EcosystemData eco = chunk.getExistingDataOrNull(LivingDungeon.ECOSYSTEM.get());
                if (eco != null) samples.add(new ClusterFinder.Sample(pcx + dx, pcz + dz, eco.biomass));
            }
        }
        ClusterFinder.Cluster c = ClusterFinder.find(pcx, pcz, samples);
        return c == null ? null : BlockPos.containing(c.x(), (p.getBlockY() >> 4) << 4, c.z());
    }
}
