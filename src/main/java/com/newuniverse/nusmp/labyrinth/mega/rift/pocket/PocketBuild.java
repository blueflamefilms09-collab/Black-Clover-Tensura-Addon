package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftService;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftType;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import com.newuniverse.nusmp.labyrinth.vault.VaultRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: a tomb of 170 blocks across is some 30,000 blocks, and placing them in one tick would stall the server for a second, so the builder is a JOB that places a budget of blocks per tick
 * ({@code [mega] buildPerTick}, 2,500 by default: about fifteen ticks for a whole tomb). The order is the plan's: the entry room first and whole (shell, lights, pillars, the exit rift), placed at
 * once so the delver has a floor under their feet the moment they arrive ({@link #entry}); then every other room's shell, every corridor, and only then the doorways, so nothing is open before
 * everything stands; then the decoration; then the biome of each chunk of the cell; then the features (trap runes, the pedestals with their loot tables, the chests). Water is always placed after the walls that hold it
 * (the plan orders it so), blocks are set with flags 2 and 16 (sent to the players, no shape updates: the tomb is a ruin, nothing in it needs to settle), chunk tickets keep the cell loaded while
 * the job runs and expire by themselves (a crash cannot leave a forced chunk behind), and every step is guarded by the caller: a failed build clears the cell instead of leaving half a tomb.
 */
public final class PocketBuild {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int FLAGS = 2 | 16, FLAGS_AIR = 2 | 16 | 32;

    private final PocketRuntime rt;
    private final List<PocketPlan.Fill> ops;
    private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
    private final Set<Long> biomed = new HashSet<>();
    private final List<int[]> allChunks;
    private final long total;
    private int stage, opIdx, biomeIdx;
    private long cursor, placed, lastTicket = -1000;

    public PocketBuild(PocketRuntime rt) {
        this.rt = rt;
        this.ops = rt.plan.ops();
        long v = 0;
        for (PocketPlan.Fill f : ops) v += f.volume();
        this.total = Math.max(1, v);
        this.allChunks = PocketBiomes.chunksOf(rt.slot);
    }

    public boolean done() { return stage >= 3; }

    /** Percent of the tomb's blocks placed (0 to 100). */
    public int percent() { return stage >= 1 ? 100 : (int) Math.min(99, placed * 100 / total); }

    public long placed() { return placed; }

    public long total() { return total; }

    // ---------------------------------------------------------------- stage A: the entry room, at once
    /** Places the entry room whole, lights its exit rift and sets the biome of the chunks it stands in. */
    public void entry(ServerLevel level) {
        keepLoaded(level, 0);
        int cx0 = PocketCell.centreX(rt.slot), cz0 = PocketCell.centreZ();
        int[] box = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int i = 0; i < rt.plan.stageA(); i++) {
            PocketPlan.Fill f = ops.get(i);
            fillAll(level, f, cx0, cz0);
            box[0] = Math.min(box[0], f.x0());
            box[1] = Math.min(box[1], f.z0());
            box[2] = Math.max(box[2], f.x1());
            box[3] = Math.max(box[3], f.z1());
        }
        opIdx = rt.plan.stageA();
        cursor = 0;
        for (int cz = (cz0 + box[1]) >> 4; cz <= (cz0 + box[3]) >> 4; cz++)
            for (int cx = (cx0 + box[0]) >> 4; cx <= (cx0 + box[2]) >> 4; cx++) {
                PocketBiomes.applyChunk(level, cx, cz, rt.def.biomeId());
                biomed.add(ChunkPos.asLong(cx, cz));
            }
        PocketPlan.Feature exit = rt.plan.first(PocketPlan.Type.EXIT_RIFT);
        if (exit != null) {
            BlockPos at = PocketCell.world(rt.slot, exit.x(), exit.y(), exit.z());
            DimensionalRiftBlockEntity er = RiftService.open(level, at, RiftType.EXIT, rt.seed ^ 0x5DEECE66DL);
            if (er != null) {
                er.aimPocket(rt.slot);
                er.extendLife(20 * 60 * (com.newuniverse.nusmp.labyrinth.mega.rift.RiftConfig.get(com.newuniverse.nusmp.labyrinth.mega.rift.RiftConfig.EXCLUSIVE_MINUTES) + 2));
            }
        }
    }

    private void fillAll(ServerLevel level, PocketPlan.Fill f, int cx0, int cz0) {
        BlockState st = rt.palette.state(f.mat());
        int flags = f.mat() == PocketPlan.Mat.AIR ? FLAGS_AIR : FLAGS;
        for (int y = f.y0(); y <= f.y1(); y++)
            for (int z = f.z0(); z <= f.z1(); z++)
                for (int x = f.x0(); x <= f.x1(); x++) {
                    m.set(cx0 + x, y, cz0 + z);
                    level.setBlock(m, st, flags);
                    placed++;
                }
    }

    // ---------------------------------------------------------------- the rest, a budget a tick
    /** One slice of the job. Returns true when the whole tomb stands. */
    public boolean step(ServerLevel level, int budget) {
        if (stage >= 3) return true;
        long now = level.getGameTime();
        if (now - lastTicket >= 100) keepLoaded(level, now);
        int left = Math.max(1, budget);
        if (stage == 0) {
            left -= placeOps(level, left);
            if (opIdx >= ops.size()) stage = 1;
        }
        if (stage == 1 && left > 0) {
            int n = 0;
            while (biomeIdx < allChunks.size() && n < 8) {
                int[] c = allChunks.get(biomeIdx++);
                if (biomed.add(ChunkPos.asLong(c[0], c[1]))) {
                    PocketBiomes.applyChunk(level, c[0], c[1], rt.def.biomeId());
                    n++;
                }
            }
            if (biomeIdx >= allChunks.size()) stage = 2;
        }
        if (stage == 2 && left > 0) {
            features(level);
            stage = 3;
        }
        return stage >= 3;
    }

    private int placeOps(ServerLevel level, int budget) {
        int cx0 = PocketCell.centreX(rt.slot), cz0 = PocketCell.centreZ(), did = 0;
        while (did < budget && opIdx < ops.size()) {
            PocketPlan.Fill f = ops.get(opIdx);
            long vol = f.volume();
            BlockState st = rt.palette.state(f.mat());
            int flags = f.mat() == PocketPlan.Mat.AIR ? FLAGS_AIR : FLAGS;
            int w = f.x1() - f.x0() + 1, d = f.z1() - f.z0() + 1;
            while (cursor < vol && did < budget) {
                int dx = (int) (cursor % w), dz = (int) ((cursor / w) % d), dy = (int) (cursor / ((long) w * d));
                m.set(cx0 + f.x0() + dx, f.y0() + dy, cz0 + f.z0() + dz);
                level.setBlock(m, st, flags);
                cursor++;
                did++;
                placed++;
            }
            if (cursor >= vol) {
                opIdx++;
                cursor = 0;
            }
        }
        return did;
    }

    /** Chunk tickets for the whole footprint: they run out by themselves (300 ticks), the job renews them every 100. */
    private void keepLoaded(ServerLevel level, long now) {
        lastTicket = now;
        for (long key : rt.chunkKeys()) {
            ChunkPos cp = new ChunkPos(key);
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, cp, 1, cp.getWorldPosition());
        }
    }

    // ---------------------------------------------------------------- the features
    private void features(ServerLevel level) {
        int slot = rt.slot;
        String id = rt.pocketId;
        int chestN = 0;
        for (PocketPlan.Feature f : rt.plan.features()) {
            BlockPos at = PocketCell.world(slot, f.x(), f.y(), f.z());
            try {
                switch (f.type()) {
                    case TRAP -> {
                        PocketElement e = PocketElement.byId(f.tag());
                        if (e != null) level.setBlock(at.below(), PocketPalette.rune(e), FLAGS);
                    }
                    case PEDESTAL_PRIMARY -> pedestal(level, at, "treasury", rt.seed ^ 0x51L);
                    case PEDESTAL_SECONDARY -> pedestal(level, at, "secondary", rt.seed ^ 0x52L);
                    case CHEST_TREASURE -> chest(level, at, "chest", rt.seed + 0x100L + chestN++);
                    case CHEST_MINOR -> chest(level, at, "minor", rt.seed + 0x200L + chestN++);
                    default -> { }
                }
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] The {} of the tomb {} could not be placed at {}: {}", f.type(), id, at.toShortString(), ex.toString());
            }
        }
    }

    private void pedestal(ServerLevel level, BlockPos at, String table, long seed) {
        level.setBlock(at, VaultRegistry.PEDESTAL.get().defaultBlockState(), FLAGS);
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof RelicPedestalBlockEntity ped) {
            CompoundTag t = new CompoundTag();
            t.putString("LootTable", "nusmp:pocket/" + rt.pocketId + "/" + table);
            t.putLong("LootTableSeed", seed);
            ped.loadCustomOnly(t, level.registryAccess());
            ped.setChanged();
        }
    }

    private void chest(ServerLevel level, BlockPos at, String table, long seed) {
        Direction face = Direction.NORTH;
        int room = rt.layout.roomAt(rt.lx(at.getX() + 0.5), rt.lz(at.getZ() + 0.5));
        if (room >= 0) {
            PocketLayout.Room r = rt.layout.room(room);
            double dx = r.cx(rt.layout.grid()) - rt.lx(at.getX() + 0.5), dz = r.cz(rt.layout.grid()) - rt.lz(at.getZ() + 0.5);
            face = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        }
        level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face), FLAGS);
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof ChestBlockEntity chest) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("nusmp", "pocket/" + rt.pocketId + "/" + table));
            chest.setLootTable(key, seed);
        }
    }
}
