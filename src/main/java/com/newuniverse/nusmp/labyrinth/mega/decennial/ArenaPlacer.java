package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.arena.ArcArenaBuilder;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a hall is a PLAN (pure, tested) and the game must be made to agree with it. This is the only class that writes a plan into the world, and it does so in the four safe ways the Dungeon needs:
 * every write is inside {@code DungeonGuard.internal} (the guard would refuse a block going to air otherwise), the chunks of the footprint are loaded first, the work is split into steps with a
 * budget so a floor that finishes generating never stalls a tick with ninety thousand block writes, and a last pass over the fences, walls, panes, bars and stairs lets them connect (the plain
 * writes do not update neighbours, which keeps the build fast and the hall free of drops and half-broken blocks). The cursor of a build is a plain number the caller saves, so a server stop in the
 * middle of a hall resumes where it was (writing the same block twice is harmless). A plan is drawn facing north; {@code ArenaFrame} turns it to the way the floor's Gatekeeper faces, and every block
 * state is turned the same way ({@code BlockState.rotate}) so stairs, doors and banners face the right way.
 */
final class ArenaPlacer {
    private ArenaPlacer() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, BlockState> STATES = new HashMap<>();

    /** The game block state a plan text means, turned by {@code rot} quarter turns. A block the game does not know becomes stone (a typo is visible, never a hole). */
    static BlockState state(String spec, int rot) {
        String key = spec + "#" + rot;
        BlockState s = STATES.get(key);
        if (s == null) {
            BlockState base = ArcArenaBuilder.stateOf(spec);
            s = rot == 0 ? base : base.rotate(rotation(rot));
            STATES.put(key, s);
        }
        return s;
    }

    private static Rotation rotation(int rot) {
        return switch (((rot % 4) + 4) % 4) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Loads (and generates) every chunk of the footprint. */
    static void ensureChunks(ServerLevel sl, ArenaFrame f) {
        int h = f.size() / 2 + 1;
        for (int x = (f.cx() - h) >> 4; x <= (f.cx() + h) >> 4; x++) for (int z = (f.cz() - h) >> 4; z <= (f.cz() + h) >> 4; z++) sl.getChunk(x, z);
    }

    /** Does the volume of a hall of this plan fit between the bottom and the top of the level? */
    static boolean fits(ServerLevel sl, BossArenaStructure plan, int y0) {
        return y0 - BossArenaStructure.FOUNDATION >= sl.getMinBuildHeight() && y0 + plan.height <= sl.getMaxBuildHeight();
    }

    /** How many steps a full build takes: one pass writing the cells (liquids excepted) and one pass that joins the connecting blocks and then sets the liquids. */
    static int total(BossArenaStructure plan) { return plan.raw().length * 2; }

    /**
     * Advances a build from {@code cursor} by about {@code budget} block writes and returns the new cursor ({@link #total} when the build is complete). Cursor values below the plan's cell count write
     * cells; values above it run the joining pass.
     */
    static int step(ServerLevel sl, BossArenaStructure plan, ArenaFrame f, int cursor, int budget) {
        short[] raw = plan.raw();
        List<String> palette = plan.palette();
        ensureChunks(sl, f);
        int size = plan.size, layer = size * size, n = 0, i = cursor, total = raw.length;
        int[] w = new int[3];
        BlockState[] states = new BlockState[palette.size()];
        boolean[] late = new boolean[states.length];                                  // liquids go in last: a liquid placed before its basin is closed would run into the old hall
        for (int k = 1; k < states.length; k++) {
            states[k] = state(palette.get(k), f.rot());
            late[k] = k > 1 && states[k].getBlock() instanceof LiquidBlock;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        DungeonGuard.begin();
        try {
            while (i < total && n < budget) {
                short p = raw[i];
                if (p != 0 && !late[p]) {
                    f.world(i % size, i / layer - BossArenaStructure.FOUNDATION, (i / size) % size, w);
                    pos.set(w[0], w[1], w[2]);
                    sl.setBlock(pos, states[p], Block.UPDATE_CLIENTS);
                    n++;
                }
                i++;
            }
            if (i >= total) {
                int j = i - total;
                while (j < total && n < budget) {
                    short p = raw[j];
                    if (p > 1 && late[p]) {
                        f.world(j % size, j / layer - BossArenaStructure.FOUNDATION, (j / size) % size, w);
                        pos.set(w[0], w[1], w[2]);
                        sl.setBlock(pos, states[p], Block.UPDATE_CLIENTS);
                        n++;
                    } else if (p > 1 && connects(states[p])) {
                        f.world(j % size, j / layer - BossArenaStructure.FOUNDATION, (j / size) % size, w);
                        pos.set(w[0], w[1], w[2]);
                        BlockState now = sl.getBlockState(pos);
                        BlockState joined = Block.updateFromNeighbourShapes(now, sl, pos);
                        if (joined != now) sl.setBlock(pos, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        n++;
                    }
                    j++;
                }
                i = total + j;
            }
        } finally {
            DungeonGuard.end();
        }
        return i;
    }

    private static boolean connects(BlockState state) {
        Block b = state.getBlock();
        return b instanceof FenceBlock || b instanceof WallBlock || b instanceof IronBarsBlock || b instanceof StairBlock;
    }

    /** Builds the whole plan now (an admin's hall, a self-test): the same steps with no budget. */
    static void buildNow(ServerLevel sl, BossArenaStructure plan, ArenaFrame f, boolean standalone) {
        clearDecor(sl, f, plan.height);
        int cursor = 0, total = total(plan);
        while (cursor < total) cursor = step(sl, plan, f, cursor, Integer.MAX_VALUE / 2);
        if (standalone) placeExtras(sl, plan, f);
    }

    /** What only a hall that stands alone has: the Gatekeeper slab, the open entry, the altar on its pedestal. */
    static void placeExtras(ServerLevel sl, BossArenaStructure plan, ArenaFrame f) {
        ensureChunks(sl, f);
        int[] w = new int[3];
        DungeonGuard.begin();
        try {
            for (BossArenaStructure.Placed e : plan.extras()) {
                f.world(e.x(), e.y(), e.z(), w);
                sl.setBlock(new BlockPos(w[0], w[1], w[2]), state(e.spec(), f.rot()), Block.UPDATE_CLIENTS);
            }
        } finally {
            DungeonGuard.end();
        }
    }

    /** Takes away what the old hall hung on its walls: armour stands, paintings and item frames inside the footprint. */
    static int clearDecor(ServerLevel sl, ArenaFrame f, int height) {
        int h = f.size() / 2 + 1;
        AABB box = new AABB(f.cx() - h, f.y0() - BossArenaStructure.FOUNDATION, f.cz() - h, f.cx() + h + 1, f.y0() + height, f.cz() + h + 1);
        List<Entity> decor = sl.getEntitiesOfClass(Entity.class, box, e -> e instanceof ArmorStand || e instanceof HangingEntity);
        for (Entity e : decor) e.discard();
        return decor.size();
    }

    // ================================================================ single cells: elements, the seal, the gate
    /** Writes one plan cell (spec text) into the world. */
    static void setCell(ServerLevel sl, ArenaFrame f, int x, int y, int z, String spec) {
        int[] w = new int[3];
        f.world(x, y, z, w);
        BlockPos pos = new BlockPos(w[0], w[1], w[2]);
        if (!sl.isLoaded(pos)) sl.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        BlockState want = state(spec, f.rot());
        DungeonGuard.begin();
        try {
            sl.setBlock(pos, want, Block.UPDATE_CLIENTS);
        } finally {
            DungeonGuard.end();
        }
    }

    /** The world position of a plan cell. */
    static BlockPos worldPos(ArenaFrame f, int x, int y, int z) {
        int[] w = new int[3];
        f.world(x, y, z, w);
        return new BlockPos(w[0], w[1], w[2]);
    }

    /** Takes an element's blocks away (its cells become air). */
    static void removeElement(ServerLevel sl, ArenaFrame f, BossArenaStructure.Element e) {
        for (BossArenaStructure.Placed c : e.cells) setCell(sl, f, c.x(), c.y(), c.z(), BossArenaStructure.AIR);
    }

    /** Puts an element back, and takes its rubble away again. */
    static void restoreElement(ServerLevel sl, ArenaFrame f, BossArenaStructure.Element e) {
        for (BossArenaStructure.Placed r : e.rubble) setCell(sl, f, r.x(), r.y(), r.z(), BossArenaStructure.AIR);
        for (BossArenaStructure.Placed c : e.cells) setCell(sl, f, c.x(), c.y(), c.z(), c.spec());
    }

    /** Closes (iron bars) or opens (air) the seal cells of the entry. */
    static void setSeal(ServerLevel sl, BossArenaStructure plan, ArenaFrame f, boolean closed) {
        for (BossArenaStructure.Placed c : plan.sealCells()) setCell(sl, f, c.x(), c.y(), c.z(), closed ? c.spec() : BossArenaStructure.AIR);
    }

    static void log(String what) { LOG.info("[nusmp] decennial: {}", what); }
}
