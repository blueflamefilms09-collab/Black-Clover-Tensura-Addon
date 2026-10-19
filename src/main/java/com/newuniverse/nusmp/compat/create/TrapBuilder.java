package com.newuniverse.nusmp.compat.create;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: puts a {@link Blueprint} into the world at a {@link Frame}, after proving the place is right, and never half way. {@link #check} reads the world and changes nothing:
 * the chunks are loaded, every lane cell is air, the floor under the lane is solid and is not one of the dungeon's own floor traps, every cell a part will take is air or a
 * plain block (no block entity, nothing unbreakable by nature), and every lever and tripwire hook has a wall to hang on. {@link #build} runs the check and then places
 * everything inside {@link DungeonGuard#internal} (the machinery replaces wall stone, which the guard would refuse from anyone else): vanilla structure first, then the working
 * parts, then the power cells in their idle state, then the motors, so the power train assembles once. A belt is asked to join itself up afterwards, by reflection, and
 * the answer is ignored if Create does not have that method.
 */
public final class TrapBuilder {
    private TrapBuilder() {}

    /** The outcome: whether it worked, a short reason when not, and how many blocks were placed. */
    public record Result(boolean ok, String reason, int placed) {
        static Result no(String why) { return new Result(false, why, 0); }
    }

    private static boolean ownFloorTrap(BlockState s) {
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return path.contains("pitfall") || path.contains("glyph") || path.contains("sigil") || path.contains("riddle") || path.contains("altar") || path.contains("gatekeeper");
    }

    /** May a part take this cell? Air, or a plain block: no block entity, not unbreakable by nature, not one of the dungeon's own traps or altars. */
    public static boolean replaceable(ServerLevel level, BlockPos pos, BlockState s) {
        if (s.isAir()) return true;
        if (s.hasBlockEntity() || ownFloorTrap(s)) return false;
        return s.getDestroySpeed(level, pos) >= 0f;
    }

    /** The world cells a drawing covers, for the chunk check. */
    private static BlockPos[] corners(Frame f, Blueprint b) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int[][] extremes = {{-4, -1, -1}, {4, b.headroom() + 2, b.length()}};
        for (int[] e : extremes) {
            for (int sx : new int[] {-4, 4}) {
                BlockPos p = f.pos(sx, e[1], e[2]);
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY());
                maxY = Math.max(maxY, p.getY());
                minZ = Math.min(minZ, p.getZ());
                maxZ = Math.max(maxZ, p.getZ());
            }
        }
        return new BlockPos[] {new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)};
    }

    /** Are all the chunks a drawing at this frame touches loaded? (A trap that is only partly loaded is left alone, never forced to load.) */
    public static boolean loaded(ServerLevel level, Frame f, Blueprint b) {
        BlockPos[] c = corners(f, b);
        return level.hasChunksAt(c[0], c[1]);
    }

    /** Reads the world and says whether the drawing fits at the frame. Changes nothing. */
    public static Result check(ServerLevel level, Frame f, Blueprint b) {
        if (!loaded(level, f, b)) return Result.no("the chunks are not loaded");
        int n = b.length();
        for (int z = 0; z < n; z++) {
            for (int x = -1; x <= 1; x++) {
                BlockPos floor = f.pos(x, 0, z);
                BlockState fs = level.getBlockState(floor);
                if (!fs.isFaceSturdy(level, floor, Direction.UP) || fs.hasBlockEntity() || ownFloorTrap(fs)) return Result.no("the floor at " + floor.toShortString() + " is not plain solid ground");
                for (int y = 1; y <= b.headroom(); y++) {
                    BlockPos p = f.pos(x, y, z);
                    if (!level.getBlockState(p).isAir()) return Result.no("the lane is not clear at " + p.toShortString());
                }
            }
        }
        for (Blueprint.Placed p : b.parts()) {
            BlockPos at = f.pos(p.dx(), p.dy(), p.dz());
            BlockState now = level.getBlockState(at);
            if (!replaceable(level, at, now)) return Result.no("a " + p.part() + " would replace " + BuiltInRegistries.BLOCK.getKey(now.getBlock()) + " at " + at.toShortString());
            if ((p.part() == Part.LEVER || p.part() == Part.HOOK) && p.facing() != null) {
                Direction face = f.dir(p.facing());
                BlockPos support = at.relative(face.getOpposite());
                if (!level.getBlockState(support).isFaceSturdy(level, support, face)) return Result.no("nothing to hang the " + p.part() + " on at " + at.toShortString());
            }
        }
        for (Blueprint.PowerCell pc : b.power()) {
            BlockPos at = f.pos(pc.dx(), pc.dy(), pc.dz());
            if (!replaceable(level, at, level.getBlockState(at))) return Result.no("the power cell at " + at.toShortString() + " would replace a block that cannot be replaced");
        }
        return new Result(true, "", 0);
    }

    /** Checks, then places. On a refusal nothing has changed. */
    public static Result build(ServerLevel level, Frame f, Blueprint b) {
        Result c = check(level, f, b);
        if (!c.ok()) return c;
        final int[] placed = {0};
        final List<BlockPos> belts = new ArrayList<>();
        DungeonGuard.internal(() -> {
            // 1. vanilla parts and the passive pieces
            for (Blueprint.Placed p : b.parts()) {
                if (p.part().kinetic() || p.part() == Part.MOTOR || p.part() == Part.STRING) continue;
                if (put(level, f, p)) placed[0]++;
            }
            // the wire goes in after both hooks, so placing it joins the hooks to it (vanilla's own update on placing a tripwire)
            for (Blueprint.Placed p : b.parts()) {
                if (p.part() != Part.STRING) continue;
                if (put(level, f, p)) placed[0]++;
            }
            // 2. the working and passing parts of the power train, motors last
            for (Blueprint.Placed p : b.parts()) {
                if (!p.part().kinetic() || p.part() == Part.MOTOR) continue;
                if (put(level, f, p)) placed[0]++;
                if (p.part() == Part.BELT) belts.add(f.pos(p.dx(), p.dy(), p.dz()));
            }
            // 3. the power cells, idle: the clutches are cut, the nixie tubes dark
            for (Blueprint.PowerCell pc : b.power()) {
                BlockPos at = f.pos(pc.dx(), pc.dy(), pc.dz());
                BlockState want = PartResolver.power(pc.role() == Blueprint.Role.CLUTCH);
                if (level.setBlock(at, want, 3)) placed[0]++;
            }
            // 4. the motors
            for (Blueprint.Placed p : b.parts()) {
                if (p.part() != Part.MOTOR) continue;
                if (put(level, f, p)) placed[0]++;
            }
        });
        if (!belts.isEmpty()) joinBelt(level, belts.get(0));
        return new Result(true, "", placed[0]);
    }

    private static boolean put(ServerLevel level, Frame f, Blueprint.Placed p) {
        BlockPos at = f.pos(p.dx(), p.dy(), p.dz());
        return level.setBlock(at, PartResolver.state(p, f), 3);
    }

    /**
     * Asks Create's belt block to join a freshly placed line of belts into one belt (its own static helper, which I believe is {@code BeltBlock.initBelt(Level, BlockPos)}).
     * Done by reflection and ignored on any failure: the belt's push on players is done by the mod, not by the belt.
     */
    private static void joinBelt(ServerLevel level, BlockPos start) {
        if (!PartResolver.resolve(Part.BELT).create()) return;
        try {
            Class<?> belt = PartResolver.resolve(Part.BELT).block().getClass();
            for (Class<?> c = belt; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals("initBelt") && m.getParameterCount() == 2 && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        m.setAccessible(true);
                        m.invoke(null, level, start);
                        return;
                    }
                }
            }
            PartResolver.logOnce("belt-init", "BeltBlock.initBelt was not found; the belt may need a block update to join up");
        } catch (Throwable t) {
            PartResolver.logOnce("belt-init-fail", "joining the belt failed: " + t);
        }
    }
}
