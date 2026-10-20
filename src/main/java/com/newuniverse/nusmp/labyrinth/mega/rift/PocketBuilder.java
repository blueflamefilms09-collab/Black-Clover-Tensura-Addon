package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: builds the realm of a pocket in its cell: sealed rooms of dark brick with lanterns (nothing spawns in the dark, nothing gets in), laid out west to east: the Antechamber where the
 * player arrives (with the exit rift in its far niche), a short corridor, then the Hall, and behind the Hall a barred door to the Vault. In a BOSS pocket the Hall is an arena with the
 * secret boss ({@code LabyrinthGuardianEntity.configureForFloor}, scaled and D&D-profiled by the floor the rift opened on); the bars go when it dies. In a PUZZLE pocket the Hall has four coloured
 * plates on pedestals and the right order shown on the east wall as four coloured blocks; pressing them in that order lifts the bars, a wrong press resets the order and wakes two vexes. The
 * Vault holds the rewards: three chests with the dungeon's own loot tables (the relic vault's and the trial's, so the loot the owner already balanced), which is the "rare loot".
 * 1.101.0 (docs/labyrinth/pocket_fix.md): the geometry now comes from {@link RealmPlan} (the same blocks as before plus a fifth room, the LEVER CHAMBER north of the Hall, with the release lever, its
 * signs and lanterns, and a second exit rift in the Vault), and EVERYTHING here runs inside the Dungeon Guard's internal span: the guard refuses to anybody else every change that takes a block away,
 * and before this the doorways (air over the walls just built), the lifting of the bars and the clearing of the cell were all silently refused in the Mega-Dungeon, which left the Antechamber a closed box.
 * {@link #verify} reads the finished realm back (doorways open, bars, lever, chests, exits) and {@link #repair} puts back what is missing. Plain {@code setBlock} with flag 2 (sent to players, no
 * neighbour updates, so nothing has to settle); the whole footprint is known ({@link #bounds}), so clearing it is one sweep. Layout (x east, z south, y up; origin = the cell's centre at {@code POCKET_Y}):
 * <pre>
 *   Antechamber x -23..-16   corridor x -14..-13   Hall x -11..11 (z -11..11)   bars x 12   Vault x 13..19 (z -3..3)   Lever chamber x -3..3, z -19..-14 (north of the Hall)
 * </pre>
 */
final class PocketBuilder {
    private PocketBuilder() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final ResourceLocation RELIC_VAULT = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/dungeon_relic_vault");
    static final ResourceLocation TRIAL = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/labyrinth_trial");
    static final String BOSS_TAG = "nusmp_pocket_boss", GUARD_TAG = "nusmp_pocket_guard";

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Block[] PLATE_BLOCKS = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.LIME_CONCRETE, Blocks.YELLOW_CONCRETE};

    /** The block of a role. */
    static BlockState state(RealmPlan.Mat m) {
        return switch (m) {
            case AIR -> AIR;
            case WALL -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case FLOOR -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case CEIL -> Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            case LIGHT -> Blocks.SEA_LANTERN.defaultBlockState();
            case PILLAR -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
            case BARS -> Blocks.IRON_BARS.defaultBlockState();
        };
    }

    static AABB bounds(int slot) {
        double cx = RiftRules.pocketX(slot), cz = RiftRules.POCKET_Z;
        return new AABB(cx - 26, RiftRules.POCKET_Y - 3, cz - 24, cx + 22, RiftRules.POCKET_Y + 16, cz + 14);
    }

    /** The world position of a plan point of the realm in a slot. */
    static BlockPos world(int slot, int lx, int ly, int lz) { return new BlockPos(RiftRules.pocketX(slot) + lx, RiftRules.POCKET_Y + ly, RiftRules.POCKET_Z + lz); }

    /** The plan of a pocket. */
    static RealmPlan planOf(PocketRealm.Pocket p) { return RealmPlan.of(p.variantKind() == RiftRules.Variant.BOSS, p.seed); }

    /** Where the release lever of the realm in a slot is (whether or not it is built). */
    static BlockPos leverPos(int slot) { return world(slot, 0, 1, -19); }

    /** The cell in front of the lever (where a delver stands to pull it). */
    static BlockPos leverStand(int slot) { return world(slot, 0, 0, -18); }

    static void clear(ServerLevel level, int slot) {
        AABB b = bounds(slot);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        DungeonGuard.begin();
        try {
            for (int x = (int) b.minX; x < (int) b.maxX; x++)
                for (int z = (int) b.minZ; z < (int) b.maxZ; z++)
                    for (int y = (int) b.minY; y < (int) b.maxY; y++) {
                        m.set(x, y, z);
                        if (!level.getBlockState(m).isAir()) level.setBlock(m, AIR, 2 | 16 | 32);
                    }
        } finally {
            DungeonGuard.end();
        }
    }

    /** Builds the realm of {@code p} and fills in its positions (entry, exit, bars, plates, boss spawn). */
    static void build(ServerLevel level, PocketRealm.Pocket p, RegionTheme theme) {
        DungeonGuard.begin();
        try {
            clear(level, p.slot);                                                         // 1.94.0: whatever a crash or an older build left in this cell goes first
            for (Entity stray : level.getEntitiesOfClass(Entity.class, bounds(p.slot), e -> !(e instanceof ServerPlayer))) stray.discard();
            AABB b = bounds(p.slot);
            for (int x = (int) b.minX; x <= (int) b.maxX + 15; x += 16) for (int z = (int) b.minZ; z <= (int) b.maxZ + 15; z += 16) level.getChunk(x >> 4, z >> 4);       // load (and so generate) every chunk of the cell first
            RealmPlan plan = planOf(p);
            for (RealmPlan.Fill f : plan.ops()) fill(level, p.slot, f, state(f.mat()));
            p.bars.clear();
            for (int[] bar : plan.bars()) p.bars.add(world(p.slot, bar[0], bar[1], bar[2]));
            features(level, p, plan, theme, false);
        } finally {
            DungeonGuard.end();
        }
    }

    private static void fill(ServerLevel level, int slot, RealmPlan.Fill f, BlockState st) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = f.y0(); y <= f.y1(); y++)
            for (int z = f.z0(); z <= f.z1(); z++)
                for (int x = f.x0(); x <= f.x1(); x++) {
                    m.set(RiftRules.pocketX(slot) + x, RiftRules.POCKET_Y + y, RiftRules.POCKET_Z + z);
                    level.setBlock(m, st, 2);
                }
    }

    /** Places the point features of the plan (the lever, the signs, the lanterns, the exits, the chests, the plates, the boss). With {@code repair} only what is missing is put back and nothing is spawned. */
    private static void features(ServerLevel level, PocketRealm.Pocket p, RealmPlan plan, RegionTheme theme, boolean repair) {
        ResourceLocation[] tables = {RELIC_VAULT, TRIAL, TRIAL};
        List<BlockPos> plates = new ArrayList<>();
        for (RealmPlan.Feature f : plan.features()) {
            BlockPos at = world(p.slot, f.x(), f.y(), f.z());
            try {
                switch (f.type()) {
                    case ARRIVAL -> p.entry = at;
                    case EXIT -> exit(level, p, at, 0x5DEECE66DL, repair, true);
                    case EXIT_VAULT -> exit(level, p, at, 0x77AA11L, repair, false);
                    case VAULT_LAMP -> set(level, at, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                    case LEVER -> {
                        if (!level.getBlockState(at).is(Blocks.LEVER))
                            set(level, at, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, PocketSigns.dir(f.tag().charAt(0))));
                    }
                    case SIGN -> {
                        if (!(level.getBlockState(at).getBlock() instanceof WallSignBlock)) PocketSigns.place(level, at, PocketSigns.dir(f.tag().charAt(0)), f.tag().substring(2));
                    }
                    case LANTERN -> set(level, at, Blocks.LANTERN.defaultBlockState());
                    case PLATE -> {
                        int i = Integer.parseInt(f.tag());
                        set(level, at, PLATE_BLOCKS[i].defaultBlockState());
                        set(level, at.above(), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
                        plates.add(at.above());
                    }
                    case ORDER -> set(level, at, PLATE_BLOCKS[Integer.parseInt(f.tag())].defaultBlockState());
                    case CHEST -> {
                        if (!level.getBlockState(at).is(Blocks.CHEST)) {
                            int i = (f.z() + 2) / 2;
                            level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 2);
                            BlockEntity be = level.getBlockEntity(at);
                            if (be instanceof ChestBlockEntity chest) chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, tables[i]), p.seed + i);
                        }
                    }
                    case BOSS_SPAWN -> {
                        p.bossSpawn = at;
                        if (!repair) arena(level, p, theme);
                    }
                }
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] The {} of the realm in slot {} could not be placed at {}: {}", f.type(), p.slot, at.toShortString(), ex.toString());
            }
        }
        if (!plates.isEmpty()) p.plates = plates.toArray(new BlockPos[0]);
    }

    private static void exit(ServerLevel level, PocketRealm.Pocket p, BlockPos at, long salt, boolean repair, boolean primary) {
        if (primary) p.exit = at;
        if (repair && level.getBlockState(at).is(RiftRegistry.RIFT.get())) return;
        DimensionalRiftBlockEntity er = RiftService.open(level, at, RiftType.EXIT, p.seed ^ salt);
        if (er != null) er.aimPocket(p.slot);
    }

    private static void arena(ServerLevel level, PocketRealm.Pocket p, RegionTheme theme) {
        LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, p.bossSpawn, null, null);
        if (g == null) {                                                           // 1.94.0: no boss could be made: the vault must not stay barred for ever behind a guardian that does not exist
            p.bossDead = true;
            p.clearedAt = level.getGameTime();
            openVault(level, p);
            return;
        }
        g.configureForFloor(p.floor, theme);
        g.addTag(BOSS_TAG);
        level.addFreshEntity(g);
    }

    private static void set(ServerLevel level, BlockPos at, BlockState s) { level.setBlock(at, s, 2); }

    // ---------------------------------------------------------------- reading it back, and putting back what is missing
    /**
     * What is wrong with the realm as it stands (empty = whole): a doorway that is not open, a missing floor, bars that should stand (or stand in an open door), no lever, no chest, no exit. It probes a
     * few dozen cells; cheap enough to run every few seconds, and to run before anybody is taken in.
     */
    static List<String> verify(ServerLevel level, PocketRealm.Pocket p) {
        List<String> out = new ArrayList<>();
        RealmPlan plan = planOf(p);
        boolean open = p.bossDead || p.solved;
        for (RealmPlan.Door d : plan.doors()) {
            BlockPos a = world(p.slot, d.x0(), d.y0() + 1, d.z0()), b = world(p.slot, d.x1(), d.y0() + 1, d.z1());
            if (!level.getBlockState(a).getCollisionShape(level, a).isEmpty() || !level.getBlockState(b).getCollisionShape(level, b).isEmpty())
                out.add("the doorway " + d.a() + " -> " + d.b() + " is closed at " + a.toShortString());
        }
        for (RealmPlan.Room r : plan.rooms()) {
            BlockPos floor = world(p.slot, r.cx(), -1, r.cz());
            if (!level.getBlockState(floor).blocksMotion()) out.add("no floor in the " + r.id() + " at " + floor.toShortString());
        }
        for (int[] bar : plan.bars()) {
            BlockPos at = world(p.slot, bar[0], bar[1], bar[2]);
            boolean bars = level.getBlockState(at).is(Blocks.IRON_BARS);
            if (!open && !bars) out.add("a bar is missing at " + at.toShortString());
            if (open && bars) out.add("a bar stands in the open vault door at " + at.toShortString());
        }
        for (RealmPlan.Feature f : plan.features()) {
            BlockPos at = world(p.slot, f.x(), f.y(), f.z());
            switch (f.type()) {
                case LEVER -> {
                    if (!level.getBlockState(at).is(Blocks.LEVER)) out.add("the release lever is missing at " + at.toShortString());
                }
                case CHEST -> {
                    if (!level.getBlockState(at).is(Blocks.CHEST)) out.add("a chest is missing at " + at.toShortString());
                }
                case EXIT, EXIT_VAULT -> {
                    if (!level.getBlockState(at).is(RiftRegistry.RIFT.get())) out.add("an exit rift is missing at " + at.toShortString());
                }
                default -> { }
            }
        }
        return out;
    }

    /**
     * Puts back what {@link #verify} found missing: every wall, floor, ceiling, light, pillar and bar of the plan that differs from what stands, the doorways, and the features that are gone (the lever and
     * its sign, the lanterns, the chests, the exits). The air of a room is never rewritten (what stands in it stays). Returns the number of blocks it set.
     */
    static int repair(ServerLevel level, PocketRealm.Pocket p, RegionTheme theme) {
        RealmPlan plan = planOf(p);
        boolean open = p.bossDead || p.solved;
        int fixed = 0;
        DungeonGuard.begin();
        try {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            Set<Long> doorCells = new HashSet<>();
            for (RealmPlan.Door d : plan.doors())
                for (int y = d.y0(); y <= d.y1(); y++) for (int z = d.z0(); z <= d.z1(); z++) for (int x = d.x0(); x <= d.x1(); x++) doorCells.add(RealmPlan.key(x, y, z));
            for (Map.Entry<Long, RealmPlan.Mat> e : plan.voxels().entrySet()) {
                RealmPlan.Mat mat = e.getValue();
                if (mat == RealmPlan.Mat.BARS && open) mat = RealmPlan.Mat.AIR;
                boolean keep = mat != RealmPlan.Mat.AIR || doorCells.contains(e.getKey()) || e.getValue() == RealmPlan.Mat.BARS;
                if (!keep) continue;                                                       // the air of a room is never rewritten
                int[] c = RealmPlan.unkey(e.getKey());
                m.set(RiftRules.pocketX(p.slot) + c[0], RiftRules.POCKET_Y + c[1], RiftRules.POCKET_Z + c[2]);
                BlockState want = state(mat);
                if (level.getBlockState(m) == want) continue;
                level.setBlock(m, want, 2);
                fixed++;
            }
            features(level, p, plan, theme, true);
        } finally {
            DungeonGuard.end();
        }
        return fixed;
    }

    /** Opens the bars of the Vault (the boss died, the puzzle was solved, or the lever was pulled). */
    static void openVault(ServerLevel level, PocketRealm.Pocket p) {
        DungeonGuard.begin();
        try {
            for (BlockPos b : p.bars) level.setBlock(b, AIR, 3);
        } finally {
            DungeonGuard.end();
        }
        level.playSound(null, p.bars.isEmpty() ? p.entry : p.bars.get(0), net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1.5f, 0.5f);
    }

    /** The punishment of a wrong press: two vexes at the plate. */
    static void wake(ServerLevel level, BlockPos near) {
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.entity.monster.Vex v = net.minecraft.world.entity.EntityType.VEX.create(level);
            if (v == null) continue;
            v.moveTo(near.getX() + 0.5 + i, near.getY() + 1.5, near.getZ() + 0.5, 0f, 0f);
            v.addTag(GUARD_TAG);
            level.addFreshEntity(v);
        }
    }

    /** True for a stack-less helper: the loot table key of a chest. */
    static ResourceKey<LootTable> table(ResourceLocation id) { return ResourceKey.create(Registries.LOOT_TABLE, id); }
}
