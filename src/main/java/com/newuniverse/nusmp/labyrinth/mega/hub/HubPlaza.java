package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.GateRegistry;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * Why: the way home of Floor 0 must ALWAYS exist, or a player who walks in through the Gateway is stranded in a safe, buildable dimension with no door.
 * So the plaza is built by the one class that owns that promise. It stamps the authored template ({@code nusmp:hub/plaza}, 33 x 33, fountain, stalls,
 * signposts, the gatehouse round the home gate) with {@code StructureTemplate.placeInWorld} exactly the way {@code /place template} does (flag 2, default
 * settings, so the game itself recomputes stair corners and fence, wall and pane connections once the blocks are in), with its corner at
 * {@link HubPlazaLayout#originX()} so the home gate lands on {@link HubLayout#HOME_GATE_X}. If the template is missing, unreadable or of another size,
 * a small programmatic plaza is built instead (a stone-brick disc of radius 12 with a pad to the gate, pillars and a lintel round the gate plane, four
 * lantern posts), so the Hub works even when its data is broken. It runs lazily, from the first arrival ({@link HubTravel#ensurePlaza}), never from a
 * chunk-load event: world generation must not place blocks into other chunks, and a world nobody enters never pays for it. After the first stamp it only
 * checks that the home gate is still there (an admin may have removed it, or the chunks may have been regenerated) and puts the gate plane back; it never
 * re-stamps over a square that players may have been using ({@code /multiverse mega hub plaza} is the explicit, admin-only re-stamp). Server thread only.
 */
public final class HubPlaza {
    private HubPlaza() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, HubPlazaLayout.TEMPLATE);

    /** What a stamp used. */
    public enum How { TEMPLATE, FALLBACK }

    /**
     * Makes sure the Hub has its plaza and a way home: stamps the plaza the first time, afterwards only restores a missing home gate plane. Never throws;
     * false means the home gate could not be guaranteed (the caller still teleports: a Hub without a gate is better than a player who cannot arrive).
     */
    public static boolean ensure(ServerLevel level) {
        if (level == null || !Labyrinth.inSanctuary(level)) return false;
        try {
            if (!HubData.get(level).built()) {
                stamp(level);
                return hasGate(level);
            }
            if (!hasGate(level)) {
                LOG.warn("The home gate of the Sanctuary is missing: putting the gate plane back at {} {} {}", HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z);
                loadHomeChunks(level);
                placeGatePlane(level);
            }
            return hasGate(level);
        } catch (RuntimeException e) {
            LOG.warn("The Sanctuary plaza could not be prepared: {}", e.toString());
            return false;
        }
    }

    /** Is the block at the recorded centre of the home gate the gate block? Loads that chunk. */
    public static boolean hasGate(ServerLevel level) {
        BlockPos centre = new BlockPos(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z);
        level.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        return level.getBlockState(centre).is(GateRegistry.LABYRINTH_GATE.get());
    }

    /**
     * Stamps the plaza now, whatever the record says (what {@code /multiverse mega hub plaza} calls): loads the chunks it covers, takes the strays out of
     * its box, places the template, falls back to the programmatic plaza if that fails, and records the build.
     */
    public static How stamp(ServerLevel level) {
        for (int cx = HubPlazaLayout.minChunkX(); cx <= HubPlazaLayout.maxChunkX(); cx++) {
            for (int cz = HubPlazaLayout.minChunkZ(); cz <= HubPlazaLayout.maxChunkZ(); cz++) level.getChunk(cx, cz);
        }
        removeStrayAnimals(level);
        How how = How.FALLBACK;
        StructureTemplate template = level.getStructureManager().get(TEMPLATE_ID).orElse(null);
        if (template == null) {
            LOG.warn("The plaza template {} is missing: building the fallback plaza", TEMPLATE_ID);
        } else if (!sizeMatches(template.getSize())) {
            LOG.warn("The plaza template {} is {} but the layout says {} x {} x {}: building the fallback plaza", TEMPLATE_ID, template.getSize(),
                    HubPlazaLayout.SIZE_X, HubPlazaLayout.SIZE_Y, HubPlazaLayout.SIZE_Z);
        } else {
            BlockPos origin = new BlockPos(HubPlazaLayout.originX(), HubPlazaLayout.originY(), HubPlazaLayout.originZ());
            StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
            if (template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2)) how = How.TEMPLATE;
            else LOG.warn("The plaza template {} could not be placed: building the fallback plaza", TEMPLATE_ID);
        }
        if (how == How.FALLBACK) buildFallback(level);
        else if (!hasGate(level)) placeGatePlane(level);
        HubData data = HubData.get(level);
        data.plazaVersion = HubPlazaLayout.BUILD_VERSION;
        data.fallback = how == How.FALLBACK;
        data.setDirty();
        LOG.info("The Sanctuary plaza was stamped ({}), build version {}", how, HubPlazaLayout.BUILD_VERSION);
        return how;
    }

    static boolean sizeMatches(Vec3i size) {
        return size.getX() == HubPlazaLayout.SIZE_X && size.getY() == HubPlazaLayout.SIZE_Y && size.getZ() == HubPlazaLayout.SIZE_Z;
    }

    private static void loadHomeChunks(ServerLevel level) {
        for (int x = HubLayout.HOME_GATE_X - 3; x <= HubLayout.HOME_GATE_X + 3; x += 3) level.getChunk(x >> 4, HubLayout.HOME_GATE_Z >> 4);
    }

    /** Puts every missing cell of the 3 x 4 home gate plane back (the plane only: the frame round it is the template's). Returns how many cells it placed. */
    public static int placeGatePlane(ServerLevel level) {
        BlockState gate = GateRegistry.LABYRINTH_GATE.get().defaultBlockState();
        int placed = 0;
        for (int x = HubLayout.HOME_GATE_X - HubPlazaLayout.GATE_HALF_WIDTH; x <= HubLayout.HOME_GATE_X + HubPlazaLayout.GATE_HALF_WIDTH; x++) {
            for (int y = HubPlazaLayout.gateBottomY(); y <= HubPlazaLayout.gateTopY(); y++) {
                BlockPos pos = new BlockPos(x, y, HubLayout.HOME_GATE_Z);
                if (!level.getBlockState(pos).is(gate.getBlock())) {
                    level.setBlock(pos, gate, 3);
                    placed++;
                }
            }
        }
        return placed;
    }

    /** Animals that wandered onto the footprint before the stamp would be buried in the new blocks; named, leashed and tamed ones are left (they have a keeper). */
    private static void removeStrayAnimals(ServerLevel level) {
        AABB box = new AABB(HubPlazaLayout.originX(), HubPlazaLayout.originY(), HubPlazaLayout.originZ(),
                HubPlazaLayout.maxX() + 1, HubPlazaLayout.maxY() + 1, HubPlazaLayout.maxZ() + 1);
        for (Animal a : level.getEntitiesOfClass(Animal.class, box, HubPlaza::isStray)) a.discard();
    }

    private static boolean isStray(Animal a) {
        if (a.hasCustomName() || a.isLeashed()) return false;
        if (a instanceof TamableAnimal t && t.isTame()) return false;
        return !(a instanceof OwnableEntity o && o.getOwnerUUID() != null);
    }

    // ---------------------------------------------------------------- the fallback
    /**
     * Why: a Hub whose template cannot be read must still work, so this builds the smallest plaza that does: the stone-brick disc, a paved pad that joins it
     * to the gate (the gate stands 14 blocks from the middle, two beyond the disc), two pillars and a lintel round the gate plane, the plane, and four lanterns.
     */
    static void buildFallback(ServerLevel level) {
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int floorY = HubLayout.SURFACE_Y - 1;
        for (int dz = -12; dz <= 12; dz++) {
            for (int dx = -12; dx <= 12; dx++) {
                if (dx * dx + dz * dz <= 12 * 12 + 12) pave(level, HubLayout.PLAZA_X + dx, floorY, HubLayout.PLAZA_Z + dz, brick, air);
            }
        }
        for (int z = HubLayout.HOME_GATE_Z - 1; z <= HubLayout.HOME_GATE_Z + 3; z++) {
            for (int x = HubLayout.HOME_GATE_X - 3; x <= HubLayout.HOME_GATE_X + 3; x++) pave(level, x, floorY, z, brick, air);
        }
        int bottom = HubPlazaLayout.gateBottomY(), top = HubPlazaLayout.gateTopY();
        for (int y = bottom; y <= top + 1; y++) {
            level.setBlock(new BlockPos(HubLayout.HOME_GATE_X - 2, y, HubLayout.HOME_GATE_Z), brick, 3);
            level.setBlock(new BlockPos(HubLayout.HOME_GATE_X + 2, y, HubLayout.HOME_GATE_Z), brick, 3);
        }
        for (int x = HubLayout.HOME_GATE_X - 2; x <= HubLayout.HOME_GATE_X + 2; x++) level.setBlock(new BlockPos(x, top + 1, HubLayout.HOME_GATE_Z), brick, 3);
        placeGatePlane(level);
        BlockState wall = Blocks.STONE_BRICK_WALL.defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos foot = new BlockPos(HubLayout.PLAZA_X + 6 * sx, HubLayout.SURFACE_Y, HubLayout.PLAZA_Z + 6 * sz);
                level.setBlock(foot, wall, 3);
                level.setBlock(foot.above(), wall, 3);
                level.setBlock(foot.above(2), lantern, 3);
            }
        }
    }

    /** One paved cell: the brick floor in place of the grass, and the air above it cleared so a tree or tall grass does not stand on the square. */
    private static void pave(ServerLevel level, int x, int floorY, int z, BlockState floor, BlockState air) {
        level.setBlock(new BlockPos(x, floorY, z), floor, 3);
        for (int h = 1; h <= 6; h++) level.setBlock(new BlockPos(x, floorY + h, z), air, 3);
    }
}
