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
 * Why: the one place that turns "where is the nearest Spire / Gate" into a block position, for the travel code (C's LabyrinthTravel puts
 * the arriving player on the pad this returns) and for {@code /multiverse labyrinth locate}. {@code ServerLevel.findNearestMapStructure}
 * (the engine behind {@code /locate}) only answers with the structure's LOCATE position, a corner of its start chunk at y 0, which is
 * useless to stand on; so the structure START is resolved through {@code StructureManager.startsForStructure(chunk, predicate)} (the
 * starts referenced by that chunk, which always include its own) and the pad is computed from the START PIECE's bounding box: the
 * base-piece contract says the arrival floor is at local y 4 under air up to y 12, centred horizontally, so the pad is
 * {@code (minX + xSpan/2, minY + 5, minZ + zSpan/2)} of {@code getPieces().get(0)}, which Jigsaw always makes the start pool's piece.
 * Spans rather than a hard-coded 41 so a datapack that replaces the base template still lands players on its centre. Nothing is cached:
 * the structure manager already memoises starts per chunk, and a cache here would outlive {@code /reload}. The radius is 64 chunks
 * (1024 blocks): the spire set spawns every 24 chunks, so the nearest one is always inside it, and a wider search would make the
 * gate's first use stall on the structure check.
 */
public final class LabyrinthLocate {
    private LabyrinthLocate() {}

    /** Search radius of findNearestMapStructure, in chunks. */
    public static final int LOCATE_RADIUS_CHUNKS = 64;
    /** The arrival pad floor is at local y 4 of the base piece; the player stands on y 5 (base-piece contract). */
    public static final int PAD_STAND_Y = 5;

    /** The arrival pad of the Spire nearest to {@code near} in the Labyrinth: centre of the start piece, one above its pad floor. Null if none within the radius. */
    public static BlockPos arrivalPad(ServerLevel labyrinth, BlockPos near) {
        return arrivalPadOf(nearestStart(labyrinth, Labyrinth.SPIRE_TAG, near));
    }

    /** The arrival pad of a given Spire start (see the class doc for the geometry); null for an invalid or empty start. */
    public static BlockPos arrivalPadOf(StructureStart start) {
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) return null;
        BoundingBox box = start.getPieces().get(0).getBoundingBox();
        return new BlockPos(box.minX() + box.getXSpan() / 2, box.minY() + PAD_STAND_Y, box.minZ() + box.getZSpan() / 2);
    }

    /**
     * The nearest Overworld Gate: the centre of its start piece, one above the piece's bottom (the gate is projected onto the surface, so
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

    /** The nearest start of a structure in the tag (already generated or not: {@code skipKnownStructures} is false), or null. */
    public static StructureStart nearestStart(ServerLevel level, TagKey<Structure> tag, BlockPos near) {
        BlockPos found = level.findNearestMapStructure(tag, near, LOCATE_RADIUS_CHUNKS, false);
        return found == null ? null : startAt(level, tag, found);
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
