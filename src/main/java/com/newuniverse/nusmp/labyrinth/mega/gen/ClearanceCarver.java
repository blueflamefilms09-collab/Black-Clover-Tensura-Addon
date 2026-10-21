package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Why: this is the game half of {@link Clearance7}. The pure class says which boxes of a placed piece must be 7 x 7 and which blocks may go; this applies it to the world
 * right after the piece was placed (inside the same Dungeon Guard span the placer uses, because the mod's own builder is allowed to take blocks away and nothing else is):
 * it removes every block of a cut box that {@link Clearance7#carvable} allows (never a chest, a spawner, an altar or a gate: those stay and the rule reports them), puts a
 * roof block where a low ceiling was cut through ({@link Clearance7#needsCap}; the block is copied from the wall beside the cut so the sector's theme carries over),
 * replaces the hanging lanterns of a cut corridor with invisible light blocks (switch {@code gen.clearance7Light}), and discards the armour stands and item frames the
 * template spawned inside the box (a hanging frame would lose its wall). Blocks are removed without neighbour updates (flag 16) so nothing pops off as an item and no
 * fluid flows in. A cut that fails is logged and skipped: a doorway that stayed 3 wide is better than a sector that is never finished.
 */
final class ClearanceCarver {
    private static final Logger LOG = LogUtils.getLogger();
    /** Totals since the server started, for the status line and the self-test. */
    static final AtomicLong PIECES = new AtomicLong(), CLEARED = new AtomicLong(), KEPT = new AtomicLong(), CAPPED = new AtomicLong(), LIT = new AtomicLong(), SEALS = new AtomicLong();

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ClearanceCarver() {}

    /** What one call did. */
    record Stats(int cleared, int kept, int capped, int lit) {
        Stats plus(Stats o) { return new Stats(cleared + o.cleared, kept + o.kept, capped + o.capped, lit + o.lit); }
    }

    /** The world as the rule sees it: a cell is free when nothing in it stops a player. */
    static Clearance7.Space space(ServerLevel level) {
        return (x, y, z) -> {
            BlockPos pos = new BlockPos(x, y, z);
            return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        };
    }

    /** Was sector {@code id} finished with the 7 x 7 clearance on (its saved statistics say so)? An older sector, or one made with the switch off, answers false. */
    static boolean wasCut(net.minecraft.server.MinecraftServer server, int id) {
        GenData.Stat st = GenData.get(server).get(id);
        return st != null && st.clear7;
    }

    /** Cuts one placed piece. {@code connected} says which of its sockets lead into another room ({@link Clearance7#connected}). */
    static Stats carve(ServerLevel level, Placed p, boolean[] connected) {
        List<Clearance7.Cut> cuts = new ArrayList<>(Clearance7.cuts(p, connected));
        List<Vec> stands = List.of();
        if (p.shape.kind == RoomKind.STAIRWELL) {
            stands = standsOf(level, p.box);
            cuts.addAll(Clearance7.stairCuts(p.index, stands, p.shape.id));
        }
        Stats total = new Stats(0, 0, 0, 0);
        java.util.Set<Vec> standSet = new java.util.HashSet<>(stands);
        for (Clearance7.Cut c : cuts) {
            try {
                total = total.plus(cut(level, c, standSet));
            } catch (Throwable t) {
                LOG.error("[nusmp] Mega clearance: the cut {} of {} failed: {}", c.box(), p.shape.id, t.toString());
            }
        }
        PIECES.incrementAndGet();
        CLEARED.addAndGet(total.cleared());
        KEPT.addAndGet(total.kept());
        CAPPED.addAndGet(total.capped());
        LIT.addAndGet(total.lit());
        return total;
    }

    /** Opens the 7 x 7 box round a rubble seal that was just cleared. */
    static Stats carveSeal(ServerLevel level, Box box) {
        Clearance7.Cut c = new Clearance7.Cut(Clearance7.Kind.DOORWAY, -1, box, Facing.NORTH, "the Way Onward seal");
        Stats s = cut(level, c, java.util.Set.of());
        SEALS.incrementAndGet();
        CLEARED.addAndGet(s.cleared());
        KEPT.addAndGet(s.kept());
        CAPPED.addAndGet(s.capped());
        return s;
    }

    /** The cells a player stands on a stair inside the box: every bottom-half stair block. */
    static List<Vec> standsOf(ServerLevel level, Box box) {
        List<Vec> out = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockState bs = level.getBlockState(m.set(x, y, z));
                    if (bs.getBlock() instanceof StairBlock && bs.getValue(StairBlock.HALF) == Half.BOTTOM) out.add(new Vec(x, y, z));
                }
            }
        }
        return out;
    }

    private static Stats cut(ServerLevel level, Clearance7.Cut c, java.util.Set<Vec> stands) {
        Box b = c.box();
        boolean stair = c.kind() == Clearance7.Kind.STAIR;
        int cleared = 0, kept = 0, capped = 0, lit = 0;
        if (!stair) discardDecor(level, b);
        BlockState cap = null;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int rows = b.sizeY();
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                boolean[] was = new boolean[rows];
                for (int y = b.minY(); y <= b.maxY(); y++) {
                    m.set(x, y, z);
                    BlockState bs = level.getBlockState(m);
                    if (bs.isAir()) continue;
                    if (stair && stands.contains(new Vec(x, y, z))) continue;
                    String id = BuiltInRegistries.BLOCK.getKey(bs.getBlock()).toString();
                    if (!Clearance7.carvable(id, bs.hasBlockEntity())) {
                        if (!bs.getCollisionShape(level, m).isEmpty()) kept++;
                        continue;
                    }
                    was[y - b.minY()] = !bs.getCollisionShape(level, m).isEmpty();
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), FLAGS);
                    cleared++;
                }
                if (stair) continue;
                m.set(x, b.maxY() + 1, z);
                if (Clearance7.needsCap(was, level.getBlockState(m).isAir())) {
                    if (cap == null) cap = capState(level, b);
                    level.setBlock(m, cap, FLAGS);
                    capped++;
                }
            }
        }
        if (ClearanceSettings.lights()) {
            BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12);
            for (Vec v : Clearance7.lightCells(c)) {
                m.set(v.x(), v.y(), v.z());
                if (level.getBlockState(m).isAir()) {
                    level.setBlock(m, light, FLAGS);
                    lit++;
                }
            }
        }
        return new Stats(cleared, kept, capped, lit);
    }

    /** The roof block of a cut: the first full solid block found in the row above the box, just outside it (so the sector's theme is kept), else deepslate bricks. */
    private static BlockState capState(ServerLevel level, Box b) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int y = b.maxY() + 1;
        for (int d = 1; d <= 2; d++) {
            for (int x = b.minX() - d; x <= b.maxX() + d; x++) {
                for (int z = b.minZ() - d; z <= b.maxZ() + d; z++) {
                    if (x > b.minX() - d && x < b.maxX() + d && z > b.minZ() - d && z < b.maxZ() + d) continue;
                    BlockState bs = level.getBlockState(m.set(x, y, z));
                    if (!bs.isAir() && bs.isSolidRender(level, m) && !bs.hasBlockEntity() && bs.getFluidState().isEmpty()) return bs;
                }
            }
        }
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    private static void discardDecor(ServerLevel level, Box b) {
        AABB area = new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1.0, b.maxY() + 1.0, b.maxZ() + 1.0);
        for (Entity e : level.getEntitiesOfClass(Entity.class, area, x -> x instanceof ArmorStand || x instanceof HangingEntity)) e.discard();
    }
}
