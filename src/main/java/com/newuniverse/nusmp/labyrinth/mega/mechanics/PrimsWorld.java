package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the primitives that change blocks: place a shape, swap the floor layer, open a shallow pit, raise water, cage the party, bar the doorways, snuff or add light, make walls
 * blink, slide a platform or a ceiling, lay a fading bridge, place props, set out a chest that may be a mimic. EVERY change goes through the run's journal (or the instance's named
 * "doors" / "cage" journals when the effect lasts until the mechanic is beaten), so ending the mechanic puts the dungeon back; nothing is placed except into free air or onto the
 * floor layer, never where a creature stands, never on a block entity, never in a safe room. Fifteen of the library's eighty primitives.
 */
final class PrimsWorld {
    private PrimsWorld() {}

    static final int FLOOR = MegaGrid.FLOOR_Y;

    /** The journal an effect writes into: its own (restored when the run ends) when it is timed, else the instance's named one (restored when the mechanic is beaten or ends). */
    static Journal journal(Run r, String name, boolean timed) {
        return timed ? r.journal() : r.inst.named.computeIfAbsent(name, k -> new Journal(r.level()));
    }

    /** May a mechanic place something here? Free of creatures and not in a safe room (cached per 4x4 column group within one call). */
    private static boolean allowed(Run r, BlockPos p, Map<Long, Boolean> cache) {
        long k = ((long) (p.getX() >> 2) << 32) ^ (p.getZ() >> 2);
        Boolean ok = cache.get(k);
        if (ok == null) {
            ok = Safety.allowedAt(r.level(), p);
            cache.put(k, ok);
        }
        return ok && !PrimUtil.occupied(r.level(), p);
    }

    static void register(Map<String, Prim> m) {
        m.put("place", Prims.timed(r -> r.ticks("secs"), r -> {
            BlockState st = PrimUtil.block(r.str("block"));
            if (st.isAir()) return;
            int h = (int) r.num("height"), rad = (int) r.num("radius"), cnt = (int) r.num("count");
            String shape = r.str("shape");
            Map<Long, Boolean> cache = new HashMap<>();
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int[] o : Shapes.offsets(shape, rad, cnt, r.inst.rng)) {
                    for (int dy = 0; dy < h; dy++) {
                        BlockPos p = new BlockPos(c.getX() + o[0], FLOOR + 1 + dy, c.getZ() + o[1]);
                        if (!allowed(r, p, cache) || !r.journal().placeInAir(p, st)) break;
                    }
                }
            }
        }, r -> { }, r -> { }));

        m.put("floor_swap", Prims.timed(r -> r.ticks("secs"), r -> {
            BlockState st = PrimUtil.block(r.str("block"));
            if (st.isAir()) return;
            Map<Long, Boolean> cache = new HashMap<>();
            String shape = r.str("shape");
            if (shape.equals("room") && r.inst.room != null) {
                Room rm = r.inst.room;
                for (int x = rm.box.minX() + 3; x <= rm.box.maxX() - 3; x++) for (int z = rm.box.minZ() + 3; z <= rm.box.maxZ() - 3; z++) swapFloor(r, new BlockPos(x, FLOOR, z), st, cache);
                return;
            }
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int[] o : Shapes.offsets(shape.equals("room") ? "disc" : shape, (int) r.num("radius"), 14, r.inst.rng)) swapFloor(r, new BlockPos(c.getX() + o[0], FLOOR, c.getZ() + o[1]), st, cache);
            }
        }, r -> { }, r -> { }));

        m.put("pit", Prims.timed(r -> r.ticks("secs"), r -> {
            int depth = (int) r.num("depth");
            Map<Long, Boolean> cache = new HashMap<>();
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int[] o : Shapes.offsets("disc", (int) r.num("radius"), 0, r.inst.rng)) {
                    int x = c.getX() + o[0], z = c.getZ() + o[1];
                    BlockPos bottom = new BlockPos(x, FLOOR - depth, z);
                    BlockState under = r.level().getBlockState(bottom);
                    if (under.isAir() || !under.getFluidState().isEmpty()) continue;          // never open a hole into the void or a pit that already has no floor
                    for (int d = 0; d < depth; d++) {
                        BlockPos p = new BlockPos(x, FLOOR - d, z);
                        if (!r.level().getBlockState(p.above()).isAir() && d == 0) break;
                        if (!allowed(r, p.above(), cache)) break;
                        if (!r.journal().set(p, Blocks.AIR.defaultBlockState())) break;
                    }
                }
            }
        }, r -> { }, r -> { }));

        m.put("flood", Prims.timed(r -> r.ticks("secs"), r -> flood(r, 0), r -> {
            if (r.age() % 20 == 0) flood(r, (int) Math.min(r.num("height") - 1, r.age() / 20));
        }, r -> { }));

        m.put("cage", Prims.timed(r -> r.ticks("secs"), r -> {
            BlockState st = PrimUtil.block(r.str("block"));
            if (st.isAir()) return;
            int rad = (int) r.num("radius"), h = (int) r.num("height");
            Journal j = journal(r, "cage", r.ticks("secs") > 0);
            Map<Long, Boolean> cache = new HashMap<>();
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != rad) continue;
                    for (int dy = 0; dy < h; dy++) {
                        BlockPos p = new BlockPos(c.getX() + x, FLOOR + 1 + dy, c.getZ() + z);
                        if (!allowed(r, p, cache) || !j.placeInAir(p, st)) break;
                    }
                }
            }
        }, r -> { }, r -> { }));

        m.put("seal_doors", Prims.timed(r -> r.ticks("secs"), r -> {
            Room rm = r.inst.room;
            BlockState st = PrimUtil.block(r.str("block"));
            if (rm == null || st.isAir()) return;
            Journal j = journal(r, "doors", r.ticks("secs") > 0);
            for (BlockPos col : doorColumns(r.level(), rm)) {
                for (int dy = 0; dy < MegaGrid.DOOR_H; dy++) {
                    BlockPos p = col.above(dy);
                    if (PrimUtil.occupied(r.level(), p)) continue;
                    j.placeInAir(p, st);
                }
            }
        }, r -> { }, r -> { }));

        m.put("unseal", Prims.once(r -> {
            for (String n : new String[] {"doors", "cage"}) {
                Journal j = r.inst.named.remove(n);
                if (j != null) j.restore();
            }
        }));

        m.put("lights_out", Prims.timed(r -> r.ticks("secs"), r -> {
            int rad = (int) r.num("radius");
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
                    if (x * x + z * z > rad * rad) continue;
                    for (int y = FLOOR; y <= FLOOR + 14; y++) {
                        BlockPos p = new BlockPos(c.getX() + x, y, c.getZ() + z);
                        BlockState s = r.level().getBlockState(p);
                        if (s.isAir() || s.getLightEmission() <= 0 || !s.getFluidState().isEmpty()) continue;
                        if (!r.inst.area.contains(p)) continue;
                        r.journal().set(p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }, r -> { }, r -> { }));

        m.put("lights_on", Prims.timed(r -> r.ticks("secs"), r -> {
            BlockState st = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, (int) r.num("level"));
            int n = (int) r.num("count");
            Map<Long, Boolean> cache = new HashMap<>();
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int[] o : Shapes.offsets("scatter", 8, n, r.inst.rng)) {
                    BlockPos p = new BlockPos(c.getX() + o[0], FLOOR + 3 + r.inst.rng.nextInt(3), c.getZ() + o[1]);
                    if (allowed(r, p, cache)) r.journal().placeInAir(p, st);
                }
            }
        }, r -> { }, r -> { }));

        m.put("wall_blink", Prims.timed(r -> r.ticks("secs"), r -> {
            r.mem.put("on", Boolean.FALSE);
            r.mem.put("c", r.centres(r.str("at")).get(0));
            blink(r);
        }, r -> {
            if (r.age() > 0 && r.age() % Math.max(10, (int) r.num("period")) == 0) blink(r);
        }, r -> { }));

        m.put("slider", Prims.timed(r -> r.ticks("secs"), r -> {
            r.mem.put("c", r.centres(r.str("at")).get(0));
            r.mem.put("s", 0);
            r.mem.put("d", 1);
            slide(r, 0, 0);
        }, r -> {
            if (r.age() % Math.max(2, (int) r.num("period")) != 0) return;
            int s = (Integer) r.mem.get("s"), d = (Integer) r.mem.get("d"), range = (int) r.num("range");
            int next = s + d;
            if (next < 0 || next > range) { d = -d; next = s + d; }
            r.mem.put("d", d);
            r.mem.put("s", next);
            slide(r, s, next);
        }, r -> { }));

        m.put("ceiling", Prims.timed(r -> r.ticks("secs"), r -> {
            r.mem.put("c", r.centres(r.str("at")).get(0));
            r.mem.put("n", 0);
        }, r -> {
            int period = Math.max(2, (int) r.num("period"));
            if (r.age() % period != 0) return;
            int steps = 8;
            int n = (Integer) r.mem.get("n");
            int phase = n % (steps * 2);
            int drop = phase < steps ? phase : steps * 2 - 1 - phase;                       // 0..7..0
            r.mem.put("n", n + 1);
            int y = FLOOR + 10 - drop;
            r.restoreBlocks();
            BlockState st = PrimUtil.block(r.str("block"));
            BlockPos c = (BlockPos) r.mem.get("c");
            int rad = (int) r.num("radius");
            Map<Long, Boolean> cache = new HashMap<>();
            for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
                BlockPos p = new BlockPos(c.getX() + x, y, c.getZ() + z);
                if (Safety.allowedAt(r.level(), p)) r.journal().placeInAir(p, st);
            }
            if (drop >= steps - 1) {
                for (ServerPlayer pl : r.players()) {
                    if (Math.abs(pl.getX() - (c.getX() + 0.5)) <= rad + 0.5 && Math.abs(pl.getZ() - (c.getZ() + 0.5)) <= rad + 0.5 && pl.getY() < y) PrimUtil.hurt(r.level(), pl, r.hard("dmg"), "generic");
                }
            }
        }, r -> { }));

        m.put("trail", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            BlockState st = PrimUtil.block(r.str("block"));
            @SuppressWarnings("unchecked")
            List<long[]> faded = (List<long[]>) r.mem.computeIfAbsent("l", k -> new ArrayList<long[]>());
            long now = r.inst.clock();
            for (ServerPlayer p : r.players()) {
                BlockPos under = p.blockPosition().below();
                if (r.level().getBlockState(under).isAir() && r.journal().placeInAir(under, st)) faded.add(new long[] {under.asLong(), now});
            }
            long life = (long) r.num("life");
            faded.removeIf(e -> {
                if (now - e[1] < life) return false;
                r.journal().restoreOne(BlockPos.of(e[0]));
                return true;
            });
        }, r -> { }));

        m.put("prop", Prims.once(r -> Props.place(r.inst, r.str("kind"), (int) r.num("count"))));

        m.put("spawn_chest", Prims.once(r -> {
            List<BlockPos> cells = Props.cells(r.inst, 1);
            if (cells.isEmpty()) return;
            BlockPos p = cells.get(0);
            Direction face = Direction.from2DDataValue(r.inst.rng.nextInt(4));
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face);
            Journal j = r.journal();
            if (!j.placeInAir(p, chest)) return;
            ResourceLocation tbl = ResourceLocation.tryParse(r.str("loot"));
            BlockEntity be = r.level().getBlockEntity(p);
            if (tbl != null && be instanceof RandomizableContainerBlockEntity c) c.setLootTable(ResourceKey.<LootTable>create(Registries.LOOT_TABLE, tbl), r.inst.rng.nextLong());
            boolean mimic = r.inst.rng.nextDouble() < r.num("mimic");
            String kind = mimic ? "mimic" : r.bool("cursed") ? "cursed_chest" : "chest";
            r.inst.props.add(new Prop(p, kind, r.inst.props.size()));
            Rukh.at(r.inst, p, mimic ? "dark" : "auto", "burst", 6);
        }));
    }

    // ---------------------------------------------------------------- helpers
    private static void swapFloor(Run r, BlockPos p, BlockState st, Map<Long, Boolean> cache) {
        BlockState cur = r.level().getBlockState(p);
        if (cur.isAir() || !cur.isCollisionShapeFullBlock(r.level(), p) || !r.level().getBlockState(p.above()).isAir()) return;
        if (!allowed(r, p.above(), cache)) return;
        r.journal().set(p, st);
    }

    private static void flood(Run r, int layer) {
        BlockState st = PrimUtil.block(r.str("block"));
        if (st.isAir()) return;
        Room rm = r.inst.room;
        int y = FLOOR + 1 + layer;
        Map<Long, Boolean> cache = new HashMap<>();
        if (r.str("at").equals("room") && rm != null) {
            for (int x = rm.box.minX() + 2; x <= rm.box.maxX() - 2; x++) for (int z = rm.box.minZ() + 2; z <= rm.box.maxZ() - 2; z++) {
                BlockPos p = new BlockPos(x, y, z);
                if (r.level().getBlockState(p).isAir() && allowed(r, p, cache)) r.journal().placeInAir(p, st);
            }
        } else {
            for (BlockPos c : r.centres(r.str("at"))) {
                for (int[] o : Shapes.offsets("disc", 5, 0, r.inst.rng)) {
                    BlockPos p = new BlockPos(c.getX() + o[0], y, c.getZ() + o[1]);
                    if (r.level().getBlockState(p).isAir() && allowed(r, p, cache)) r.journal().placeInAir(p, st);
                }
            }
        }
    }

    /** The bottom cell of every doorway column of a room: an air cell on the outermost wall layer that is open on the second layer too. */
    static List<BlockPos> doorColumns(ServerLevel level, Room rm) {
        List<BlockPos> out = new ArrayList<>();
        int y = FLOOR + 1;
        for (int side = 0; side < 4; side++) {
            List<BlockPos> found = new ArrayList<>();
            boolean xSide = side < 2;
            int fixed = side == 0 ? rm.box.minZ() : side == 1 ? rm.box.maxZ() : side == 2 ? rm.box.minX() : rm.box.maxX();
            int step = (side == 0 || side == 2) ? 1 : -1;
            int from = xSide ? rm.box.minX() + 1 : rm.box.minZ() + 1, to = xSide ? rm.box.maxX() - 1 : rm.box.maxZ() - 1;
            for (int i = from; i <= to; i++) {
                BlockPos outer = xSide ? new BlockPos(i, y, fixed) : new BlockPos(fixed, y, i);
                BlockPos inner = xSide ? outer.offset(0, 0, step) : outer.offset(step, 0, 0);
                if (!level.hasChunkAt(outer)) continue;
                if (level.getBlockState(outer).isAir() && level.getBlockState(inner).isAir()) found.add(outer);
            }
            if (found.size() <= 30) out.addAll(found);                    // a whole side that is open is a cavern, not a doorway
        }
        return out;
    }

    private static void blink(Run r) {
        boolean on = Boolean.TRUE.equals(r.mem.get("on"));
        r.restoreBlocks();
        if (on) { r.mem.put("on", Boolean.FALSE); return; }
        r.mem.put("on", Boolean.TRUE);
        BlockState st = PrimUtil.block(r.str("block"));
        BlockPos c = (BlockPos) r.mem.get("c");
        Map<Long, Boolean> cache = new HashMap<>();
        for (int[] o : Shapes.offsets("walls", (int) r.num("radius"), 0, r.inst.rng)) {
            for (int dy = 0; dy < 3; dy++) {
                BlockPos p = new BlockPos(c.getX() + o[0], FLOOR + 1 + dy, c.getZ() + o[1]);
                if (!allowed(r, p, cache) || !r.journal().placeInAir(p, st)) break;
            }
        }
    }

    /** Moves the platform from step {@code from} to step {@code to}, carrying whoever stands on it. */
    private static void slide(Run r, int from, int to) {
        BlockPos c = (BlockPos) r.mem.get("c");
        String axis = r.str("axis");
        int range = (int) r.num("range"), rad = (int) r.num("radius");
        BlockState st = PrimUtil.block(r.str("block"));
        boolean slab = st.getBlock() instanceof net.minecraft.world.level.block.SlabBlock;
        double top = slab ? 0.5 : 1.0;
        int[] a = offsetOf(axis, from, range), b = offsetOf(axis, to, range);
        int ay = FLOOR + 1 + a[1], by = FLOOR + 1 + b[1];
        if (from != to) {
            AABB feet = new AABB(c.getX() + a[0] - rad, ay + top - 0.4, c.getZ() + a[2] - rad, c.getX() + a[0] + rad + 1, ay + top + 0.9, c.getZ() + a[2] + rad + 1);
            for (LivingEntity e : r.level().getEntitiesOfClass(LivingEntity.class, feet, LivingEntity::isAlive)) {
                PrimUtil.teleport(e, e.getX() + (b[0] - a[0]), e.getY() + (b[1] - a[1]), e.getZ() + (b[2] - a[2]));
                e.setDeltaMovement(Vec3.ZERO);
                e.hurtMarked = true;
            }
        }
        r.restoreBlocks();
        Map<Long, Boolean> cache = new HashMap<>();
        for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
            BlockPos p = new BlockPos(c.getX() + b[0] + x, by, c.getZ() + b[2] + z);
            if (Safety.allowedAt(r.level(), p)) r.journal().placeInAir(p, st);
        }
    }

    private static int[] offsetOf(String axis, int step, int range) {
        int off = step - range / 2;
        return switch (axis) {
            case "z" -> new int[] {0, 0, off};
            case "y" -> new int[] {0, step, 0};
            default -> new int[] {off, 0, 0};
        };
    }
}
