package com.newuniverse.nusmp.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The few things of the old Golden City builder that the Variable Core's Golden City zone (dungeon/nexus/zone/GoldenCitySite) needs and that
 * are package-private here: a sink that writes the city's ops into the world, and the opening of the ways in through the city's wall. The
 * city itself, its plan, its altar and its saved ways stay exactly as they were; this only hands them out.
 */
public final class CityNexusBridge {
    private CityNexusBridge() {}

    /** Send the change to clients, but do not update shapes or neighbours (as the city builder does). */
    private static final int FLAGS = 2 | 16;

    /** The plan of the city of a cell (the same one the city builder keeps). */
    public static CityPlan plan(int cellX, int cellZ) { return CityBuilder.plan(cellX, cellZ); }

    /** A sink that writes city ops into a level, quietly. Run it inside {@code DungeonGuard.internal}. */
    public static CityCursor.Sink sink(ServerLevel level) { return new Sink(level); }

    /**
     * Opens the ways into the city of a cell (see {@link CityWays}): a passage through its wall and shell, and through the rock to a cave
     * when the cave is not at the wall, ending in an open iron door. Returns how many ways were opened; 0 if no cave was close enough or
     * the chunks round the city are not loaded. The doors are saved with the city's record.
     */
    public static int openWays(ServerLevel level, int cellX, int cellZ) {
        CityPlan plan = CityBuilder.plan(cellX, cellZ);
        return CityBuilder.openWays(level, plan, CitySave.key(cellX, cellZ), CitySave.get(level.getServer()));
    }

    /** How many ways in the city of a cell has open (saved). */
    public static int ways(MinecraftServer server, int cellX, int cellZ) { return CitySave.get(server).ways(CitySave.key(cellX, cellZ)).length; }

    /** Writes the cells of a city plan into a level. */
    private static final class Sink implements CityCursor.Sink {
        private final ServerLevel level;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private CityPlan.Op last;
        private BlockState lastState;

        Sink(ServerLevel level) { this.level = level; }

        @Override
        public boolean place(CityPlan.Op op, int x, int y, int z) {
            pos.set(x, y, z);
            BlockState cur = level.getBlockState(pos);
            if (op.kind() == CityPlan.Kind.CARVE) {
                if (cur.isAir() || cur.is(Blocks.BEDROCK)) return false;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                return true;
            }
            if (op != last) {
                last = op;
                lastState = CityBuilder.state(op.pal(), op.variant());
            }
            if (cur == lastState || (lastState.isAir() && cur.isAir())) return false;
            level.setBlock(pos, lastState, FLAGS);
            return true;
        }
    }
}
