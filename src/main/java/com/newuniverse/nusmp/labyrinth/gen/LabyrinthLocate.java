package com.newuniverse.nusmp.labyrinth.gen;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.function.Predicate;

/**
 * Why: the one place that turns "where is the nearest Gate" into a block position, for {@code /multiverse labyrinth locate} and the Vizier of the
 * relic deck. {@code ServerLevel.findNearestMapStructure} (the engine behind {@code /locate}) only answers with the structure's LOCATE position, a
 * corner of its start chunk at y 0, which is useless to stand on; so the structure START is resolved through
 * {@code StructureManager.startsForStructure(chunk, predicate)} (the starts referenced by that chunk, which always include its own) and the spot is
 * computed from the START PIECE's bounding box. Nothing is cached: the structure manager already memoises starts per chunk, and a cache here would
 * outlive {@code /reload}. The radius is 64 chunks (1024 blocks). Until 1.58.0 it also found the Spire's arrival pad; the Spire was removed.
 */
public final class LabyrinthLocate {
    private LabyrinthLocate() {}

    /** Search radius of findNearestMapStructure, in chunks. */
    public static final int LOCATE_RADIUS_CHUNKS = 64;

    /**
     * The nearest Overworld Gate (the Mega-Dungeon's Gateway): the centre of its start piece, one above the piece's bottom (the gate is projected onto the surface, so
     * that is ground level); if the start cannot be resolved, the locate position lifted onto the heightmap. Null if none within the radius.
     */
    public static BlockPos nearestGate(ServerLevel overworld, BlockPos near) {
        BlockPos found = overworld.findNearestMapStructure(Labyrinth.GATE_TAG, near, LOCATE_RADIUS_CHUNKS, false);
        if (found == null) return null;
        StructureStart start = startAt(overworld, Labyrinth.GATE_TAG, found);
        if (start != null) {
            BoundingBox box = start.getPieces().get(0).getBoundingBox();
            return new BlockPos(box.minX() + box.getXSpan() / 2, box.minY() + 1, box.minZ() + box.getZSpan() / 2);
        }
        return new BlockPos(found.getX(), overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, found.getX(), found.getZ()), found.getZ());
    }

    /**
     * The start of a tagged structure referenced by the chunk of a locate position: the one that starts in that chunk, else the nearest to
     * it. Loads that chunk to STRUCTURE_REFERENCES (a few milliseconds on the server thread if it was never generated), nothing more.
     */
    public static StructureStart startAt(ServerLevel level, TagKey<Structure> tag, BlockPos locatePos) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Predicate<Structure> tagged = s -> structures.wrapAsHolder(s).is(tag);
        ChunkPos chunk = new ChunkPos(locatePos);
        List<StructureStart> starts = level.structureManager().startsForStructure(chunk, tagged);
        StructureStart best = null;
        double bestDistance = Double.MAX_VALUE;
        for (StructureStart start : starts) {
            if (start == null || !start.isValid() || start.getPieces().isEmpty()) continue;
            if (chunk.equals(start.getChunkPos())) return start;
            double d = start.getBoundingBox().getCenter().distSqr(locatePos);
            if (d < bestDistance) { bestDistance = d; best = start; }
        }
        return best;
    }
}
