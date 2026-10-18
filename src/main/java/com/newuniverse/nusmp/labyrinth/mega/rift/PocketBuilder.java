package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * Why: builds the realm of a pocket in its cell: sealed rooms of dark brick with lanterns (nothing spawns in the dark, nothing gets in), laid out west to east: the Antechamber where the
 * player arrives (with the exit rift in its far niche), a short corridor, then the Hall, and behind the Hall a barred door to the Vault. In a BOSS pocket the Hall is an arena with the
 * secret boss ({@code LabyrinthGuardianEntity.configureForFloor}, scaled and D&D-profiled by the floor the rift opened on); the bars go when it dies. In a PUZZLE pocket the Hall has four coloured
 * plates on pedestals and the right order shown on the east wall as four coloured blocks; pressing them in that order lifts the bars, a wrong press resets the order and wakes two vexes. The
 * Vault holds the rewards: three chests with the dungeon's own loot tables (the relic vault's and the trial's, so the loot the owner already balanced), which is the "rare loot".
 * Everything is placed with plain {@code setBlock} (flag 2: sent to players, no neighbour updates, so nothing has to settle) and the whole footprint is known ({@link #bounds}), so clearing it is
 * one fill. Layout (x east, z south, y up; origin = the cell's centre at {@code POCKET_Y}):
 * <pre>
 *   Antechamber x -23..-16   corridor x -14..-13   Hall x -11..11 (z -11..11)   bars x 12   Vault x 13..19 (z -3..3)
 * </pre>
 */
final class PocketBuilder {
    private PocketBuilder() {}

    static final ResourceLocation RELIC_VAULT = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/dungeon_relic_vault");
    static final ResourceLocation TRIAL = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/labyrinth_trial");
    static final String BOSS_TAG = "nusmp_pocket_boss", GUARD_TAG = "nusmp_pocket_guard";

    private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), FLOOR = Blocks.DEEPSLATE_TILES.defaultBlockState(),
            CEIL = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), LIGHT = Blocks.SEA_LANTERN.defaultBlockState(), AIR = Blocks.AIR.defaultBlockState(),
            PILLAR = Blocks.GILDED_BLACKSTONE.defaultBlockState(), BARS = Blocks.IRON_BARS.defaultBlockState();
    private static final Block[] PLATE_BLOCKS = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.LIME_CONCRETE, Blocks.YELLOW_CONCRETE};

    static AABB bounds(int slot) {
        double cx = RiftRules.pocketX(slot), cz = RiftRules.POCKET_Z;
        return new AABB(cx - 26, RiftRules.POCKET_Y - 3, cz - 14, cx + 22, RiftRules.POCKET_Y + 16, cz + 14);
    }

    static void clear(ServerLevel level, int slot) {
        AABB b = bounds(slot);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = (int) b.minX; x < (int) b.maxX; x++)
            for (int z = (int) b.minZ; z < (int) b.maxZ; z++)
                for (int y = (int) b.minY; y < (int) b.maxY; y++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) level.setBlock(m, AIR, 2 | 16 | 32);
                }
    }

    /** Builds the realm of {@code p} and fills in its positions (entry, exit, bars, plates, boss spawn). */
    static void build(ServerLevel level, PocketRealm.Pocket p, RegionTheme theme) {
        int cx = RiftRules.pocketX(p.slot), cz = RiftRules.POCKET_Z, y = RiftRules.POCKET_Y;
        for (int x = cx - 26; x <= cx + 22; x += 16) for (int z = cz - 14; z <= cz + 14; z += 16) level.getChunk(x >> 4, z >> 4);       // load (and so generate) every chunk of the cell first
        level.getChunk((cx + 22) >> 4, (cz + 14) >> 4);
        room(level, cx - 23, cz - 4, cx - 16, cz + 4, y, 6);                          // the Antechamber
        room(level, cx - 14, cz - 1, cx - 13, cz + 1, y, 4);                          // the corridor
        room(level, cx - 11, cz - 11, cx + 11, cz + 11, y, 11);                       // the Hall
        room(level, cx + 13, cz - 3, cx + 19, cz + 3, y, 6);                          // the Vault
        for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy < 4; dy++) {            // the doorways: antechamber -> corridor -> hall
            for (int dx = -15; dx <= -12; dx++) set(level, cx + dx, y + dy, cz + dz, AIR);
        }
        for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy < 3; dy++) {             // the barred door between the Hall and the Vault (x = +12)
            BlockPos bar = new BlockPos(cx + 12, y + dy, cz + dz);
            set(level, bar.getX(), bar.getY(), bar.getZ(), BARS);
            p.bars.add(bar);
        }
        for (int[] c : new int[][]{{-10, -10}, {10, -10}, {-10, 10}, {10, 10}}) for (int dy = 0; dy < 10; dy++) set(level, cx + c[0], y + dy, cz + c[1], PILLAR);
        for (int i = -9; i <= 9; i += 6) for (int j = -9; j <= 9; j += 6) set(level, cx + i, y + 10, cz + j, LIGHT);
        set(level, cx - 19, y + 5, cz, LIGHT);
        set(level, cx + 16, y + 5, cz, LIGHT);
        p.entry = new BlockPos(cx - 19, y, cz);
        BlockPos exit = new BlockPos(cx - 22, y, cz);                                  // the exit rift: in the far niche of the Antechamber, facing the way in
        p.exit = exit;
        DimensionalRiftBlockEntity er = RiftService.open(level, exit, RiftType.EXIT, p.seed ^ 0x5DEECE66DL);
        if (er != null) er.aimPocket(p.slot);
        vault(level, cx, cz, y, p);
        if (p.variantKind() == RiftRules.Variant.BOSS) arena(level, cx, cz, y, p, theme);
        else puzzle(level, cx, cz, y, p);
    }

    private static void arena(ServerLevel level, int cx, int cz, int y, PocketRealm.Pocket p, RegionTheme theme) {
        p.bossSpawn = new BlockPos(cx + 4, y, cz);
        LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, p.bossSpawn, null, null);
        if (g == null) return;
        g.configureForFloor(p.floor, theme);
        g.addTag(BOSS_TAG);
        level.addFreshEntity(g);
    }

    private static void puzzle(ServerLevel level, int cx, int cz, int y, PocketRealm.Pocket p) {
        int[][] at = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
        p.plates = new BlockPos[4];
        for (int i = 0; i < 4; i++) {
            BlockPos base = new BlockPos(cx + at[i][0], y, cz + at[i][1]);
            set(level, base.getX(), base.getY(), base.getZ(), PLATE_BLOCKS[i].defaultBlockState());
            set(level, base.getX(), base.getY() + 1, base.getZ(), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
            p.plates[i] = base.above();
        }
        int[] order = RiftRules.puzzleOrder(p.seed);                                   // the wall shows the order: four coloured blocks over the barred door, left to right
        for (int i = 0; i < 4; i++) set(level, cx + 11, y + 4, cz - 3 + i * 2, PLATE_BLOCKS[order[i]].defaultBlockState());
    }

    private static void vault(ServerLevel level, int cx, int cz, int y, PocketRealm.Pocket p) {
        long seed = p.seed;
        ResourceLocation[] tables = {RELIC_VAULT, TRIAL, TRIAL};
        for (int i = 0; i < 3; i++) {
            BlockPos at = new BlockPos(cx + 18, y, cz - 2 + i * 2);
            level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 2);
            BlockEntity be = level.getBlockEntity(at);
            if (be instanceof ChestBlockEntity chest) chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, tables[i]), seed + i);
        }
        set(level, cx + 16, y + 5, cz - 3, Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    /** A sealed rectangular room of interior x0..x1, z0..z1 and {@code h} cells high above {@code y}: floor, walls, ceiling, all of dark brick, the interior air. */
    private static void room(ServerLevel level, int x0, int z0, int x1, int z1, int y, int h) {
        for (int x = x0 - 1; x <= x1 + 1; x++)
            for (int z = z0 - 1; z <= z1 + 1; z++)
                for (int dy = -1; dy <= h; dy++) {
                    boolean wall = x < x0 || x > x1 || z < z0 || z > z1;
                    BlockState s = dy == -1 ? FLOOR : dy == h ? CEIL : wall ? WALL : AIR;
                    set(level, x, y + dy, z, s);
                }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, 2); }

    /** Opens the bars of the Vault (the boss died, or the puzzle was solved). */
    static void openVault(ServerLevel level, PocketRealm.Pocket p) {
        for (BlockPos b : p.bars) level.setBlock(b, AIR, 3);
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
