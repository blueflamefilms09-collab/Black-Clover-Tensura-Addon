package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.RegionContext;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: placing a whole sector (hundreds of pieces, millions of blocks, a hundred and more chunks) in one tick would freeze the server for seconds, so
 * the pieces are placed a few at a time, each inside the two scopes the rest of the mod needs: {@link DungeonGuard#internal} (the Mega-Dungeon is held by the
 * Dungeon Guard, which refuses every block write that takes something away, and this is the mod's own builder) and {@link RegionContext} (the theme and
 * mod-compat processors read which sector is being built and where its markers are from this thread-local; outside it they do nothing). A piece is placed
 * with the call vanilla's own {@code /place jigsaw} uses ({@code PoolElementStructurePiece.place} with an infinite box and the sector's entry as the
 * structure origin), so processors, block entities, entities and waterlogging behave exactly as for a vanilla structure. Before a piece is placed its chunks
 * get a short-lived chunk ticket, so a chunk the previous piece already filled is not dropped and reloaded for the next one and the light engine has time to
 * settle. A repair (the server stopped half way, or an admin regenerated the sector) first clears the decorative entities the earlier run spawned inside the
 * piece's box, because the templates carry armor stands and item frames and placing twice would double them. One piece that throws is logged and skipped:
 * a hole in a wall is better than a sector that is never finished.
 */
final class SectorPlacer {
    private static final Logger LOG = LogUtils.getLogger();
    /** The ticket that keeps a chunk loaded for ten seconds after a piece touched it. Not saved: it dies with the server. */
    private static final TicketType<ChunkPos> GEN_TICKET = TicketType.create("nusmp_mega_gen", Comparator.comparingLong(ChunkPos::toLong), 200);

    private final ServerLevel level;
    private final Sector sector;
    private final SectorGenerator.Built built;
    private final boolean repair;
    private final Set<Long> ticketed = new HashSet<>();
    /** 1.107.0: which sockets of which piece lead into another room (null until the first piece is placed): the 7 x 7 clearance cuts only those ({@link Clearance7}). */
    private boolean[][] connected;
    private int next;
    private int failed;
    /** Rooms rebuilt: the pieces whose rooms hold the one rare armour stand of the floor ({@link RareStands}); the stands of every other piece are dropped as they are made ({@code ArmourStandGate}). */
    private final java.util.Set<Integer> standRooms;
    private final int depth;

    SectorPlacer(ServerLevel level, Sector sector, SectorGenerator.Built built, boolean repair) {
        this.level = level;
        this.sector = sector;
        this.built = built;
        this.repair = repair;
        Result r = built.planned() == null ? null : built.planned().result();
        this.standRooms = r == null ? java.util.Set.of() : RareStands.plan(r, built.templates());
        this.depth = Math.abs(sector.firstFloor());
    }

    int total() { return built.pieces().size(); }

    int placed() { return next; }

    int failed() { return failed; }

    boolean done() { return next >= total(); }

    /** Places pieces until {@code maxPieces} are placed or the deadline passes (at least one piece per call), and returns how many were placed. */
    int step(int maxPieces, long deadlineNanos) {
        int n = 0;
        while (!done() && n < maxPieces) {
            placeOne(built.pieces().get(next), next);
            next++;
            n++;
            if (System.nanoTime() >= deadlineNanos) break;
        }
        return n;
    }

    private void placeOne(PoolElementStructurePiece piece, int index) {
        BoundingBox box = piece.getBoundingBox();
        try {
            hold(box);
            if (repair) clearDecor(box);
            final Map<BlockPos, Marker> markers = built.markersByCell();
            final ServerChunkCache cache = level.getChunkSource();
            final BlockPos pivot = sector.entry;
            final com.newuniverse.nusmp.labyrinth.props.ArmourStandGate.Scope stands = com.newuniverse.nusmp.labyrinth.props.ArmourStandGate.Scope.of(standRooms.contains(index), depth, Rng.mix(sector.seed, index));
            DungeonGuard.internal(() -> RegionContext.run(sector, markers, () -> com.newuniverse.nusmp.labyrinth.props.ArmourStandGate.run(stands, () ->
                    piece.place(level, level.structureManager(), cache.getGenerator(), level.getRandom(), BoundingBox.infinite(), pivot, false))));
            clearance(index);
        } catch (Throwable t) {
            failed++;
            LOG.error("[nusmp] Mega sector {}: piece {} at {} failed to place: {}", sector.id, piece.getElement(), piece.getPosition(), t.toString());
        }
    }

    /**
     * 1.107.0 (gen.clearance7): cuts the piece just placed to the 7 x 7 minimum clearance of every transition ({@link Clearance7}, {@link ClearanceCarver}). Inside the
     * same guard span the placement used. A failure is logged and the piece keeps its old size; it never fails the placement.
     */
    private void clearance(int index) {
        if (!ClearanceSettings.enabled()) return;
        try {
            Result r = built.planned().result();
            if (r == null || index >= r.pieces.size()) return;
            if (connected == null) connected = Clearance7.connected(r);
            final Placed placed = r.pieces.get(index);
            final boolean[] conn = connected[index];
            DungeonGuard.internal(() -> ClearanceCarver.carve(level, placed, conn));
        } catch (Throwable t) {
            LOG.error("[nusmp] Mega sector {}: the 7 x 7 clearance of piece {} failed: {}", sector.id, index, t.toString());
        }
    }

    /** Gives every chunk the piece covers a short-lived ticket (once per chunk per job). */
    private void hold(BoundingBox box) {
        ServerChunkCache cache = level.getChunkSource();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                if (ticketed.add(cp.toLong())) cache.addRegionTicket(GEN_TICKET, cp, 0, cp);
            }
        }
    }

    /** Removes the armor stands, item frames and paintings inside the box (what the templates spawn), after making sure its chunks are loaded. */
    private void clearDecor(BoundingBox box) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        AABB area = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1.0, box.maxY() + 1.0, box.maxZ() + 1.0);
        List<Entity> old = level.getEntitiesOfClass(Entity.class, area, e -> e instanceof ArmorStand || e instanceof HangingEntity);
        for (Entity e : old) e.discard();
    }

    /**
     * Opens the rubble seal of a finished sector: the {@code w x h x d} box at its expansion cell becomes air, so the tunnel to the next sector is walkable.
     * Returns how many blocks were removed. The size is clamped to what a seal can be, so a corrupt marker can never carve out a room.
     */
    static int clearSeal(ServerLevel level, Sector from) {
        final List<BlockPos> cells = sealCells(from);
        if (cells.isEmpty()) return 0;
        final int[] removed = {0};
        DungeonGuard.internal(() -> {
            for (BlockPos p : cells) {
                if (level.getBlockState(p).isAir()) continue;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                removed[0]++;
            }
            if (ClearanceSettings.enabled() && from.expansionPos != null && from.expansionFacing != null) {      // 1.107.0: the opened seal is as wide and high as the rest of the dungeon
                try {
                    Box wide = Clearance7.sealCut(SectorGenerator.vec(from.expansionPos), SectorGenerator.facing(from.expansionFacing), from.sealD);
                    removed[0] += ClearanceCarver.carveSeal(level, wide).cleared();
                } catch (Throwable t) {
                    LOG.error("[nusmp] Mega sector {}: the 7 x 7 opening of the Way Onward failed: {}", from.id, t.toString());
                }
            }
        });
        return removed[0];
    }

    /**
     * The positions of a seal's blocks: the {@code w x h x d} box at the sector's expansion cell, centred on it sideways, rising from it and running {@code d}
     * layers inward. The size is clamped to what a seal can be, so a corrupt value can never carve out a room. {@link #clearSeal} opens exactly these cells and
     * the self-test reads them without changing anything.
     */
    static List<BlockPos> sealCells(Sector from) {
        ArrayList<BlockPos> out = new ArrayList<>();
        if (from.expansionPos == null || from.expansionFacing == null) return out;
        Direction f = from.expansionFacing, lateral = f.getClockWise();
        int w = Math.max(1, Math.min(9, from.sealW)), h = Math.max(1, Math.min(8, from.sealH)), d = Math.max(1, Math.min(4, from.sealD));
        for (int i = -((w - 1) / 2); i <= w / 2; i++) {
            for (int y = 0; y < h; y++) {
                for (int k = 0; k < d; k++) out.add(from.expansionPos.relative(lateral, i).above(y).relative(f.getOpposite(), k));
            }
        }
        return out;
    }
}
