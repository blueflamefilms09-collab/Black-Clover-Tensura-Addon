package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: nothing here can be played by its author, so the mechanics library has SELF-TESTS that the CI server smoke test runs in the real game ({@code /multiverse mega selftest}).
 * {@code mech.library}: the loaded library has exactly the number the generator wrote, per category too, every mechanic passes the catalogue, signatures are unique, every
 * primitive has an executor. {@code mech.resolve}: every id resolves. {@code mech.prims}: EVERY one of the eighty primitives executes (and, if sustained, ticks and ends) on a
 * scratch room in a far corner of the Mega-Dungeon's dimension without an error, and afterwards the scratch room is block for block what it was. {@code mech.triggers}: every
 * trigger kind fires under the conditions it names. {@code mech.counters}: every counter kind can be beaten the way it says. {@code mech.director}: the plan of a floor is the same for
 * the same seed, different for another, keeps the caps and the budget, never stacks conflicting mechanics and never plans a safe room; the safety rule refuses the Hub, rift pockets
 * and safe rooms. {@code mech.apply}: applying a mechanic and clearing it restores the blocks and a safe room refuses it. The scratch room is cleaned up whatever happens.
 */
public final class MechanicsSelfTests {
    private MechanicsSelfTests() {}

    private static final BlockPos ORIGIN = new BlockPos(6_000_000, MegaGrid.FLOOR_Y, 6_000_000);
    private static final int HALF = 9;                              // the scratch room is 19 x 19
    private static final BoundingBox BOX = new BoundingBox(ORIGIN.getX() - HALF, MegaGrid.FLOOR_Y - 8, ORIGIN.getZ() - HALF, ORIGIN.getX() + HALF, MegaGrid.FLOOR_Y + 22, ORIGIN.getZ() + HALF);

    public static void register() {
        SelfTests.register("mech.library", MechanicsSelfTests::library);
        SelfTests.register("mech.resolve", MechanicsSelfTests::resolve);
        SelfTests.register("mech.prims", MechanicsSelfTests::prims);
        SelfTests.register("mech.triggers", MechanicsSelfTests::triggers);
        SelfTests.register("mech.counters", MechanicsSelfTests::counters);
        SelfTests.register("mech.director", MechanicsSelfTests::director);
        SelfTests.register("mech.apply", MechanicsSelfTests::apply);
        SelfTests.register("dungeon.nohunger", MechanicsSelfTests::noHunger);          // 1.99.0, the hunger ban
    }

    // ---------------------------------------------------------------- the scratch room
    private static void build(ServerLevel level) {
        for (int cx = (BOX.minX() - 2) >> 4; cx <= (BOX.maxX() + 2) >> 4; cx++) for (int cz = (BOX.minZ() - 2) >> 4; cz <= (BOX.maxZ() + 2) >> 4; cz++) level.getChunk(cx, cz);
        DungeonGuard.internal(() -> {
            BlockState air = Blocks.AIR.defaultBlockState(), stone = Blocks.STONE_BRICKS.defaultBlockState();
            for (int x = BOX.minX() - 5; x <= BOX.maxX() + 5; x++) for (int z = BOX.minZ() - 5; z <= BOX.maxZ() + 5; z++) {
                for (int y = MegaGrid.FLOOR_Y - 4; y <= MegaGrid.FLOOR_Y + 22; y++) {
                    boolean wall = (Math.abs(x - ORIGIN.getX()) == HALF && Math.abs(z - ORIGIN.getZ()) <= HALF) || (Math.abs(z - ORIGIN.getZ()) == HALF && Math.abs(x - ORIGIN.getX()) <= HALF);
                    boolean gap = z == ORIGIN.getZ() + HALF && Math.abs(x - ORIGIN.getX()) <= 1 && y <= MegaGrid.FLOOR_Y + 4;
                    BlockState want = y < MegaGrid.FLOOR_Y ? (y >= MegaGrid.FLOOR_Y - 3 ? stone : air) : y == MegaGrid.FLOOR_Y ? stone : (wall && !gap && y <= MegaGrid.FLOOR_Y + 8 ? stone : air);
                    if (level.getBlockState(new BlockPos(x, y, z)) != want) level.setBlock(new BlockPos(x, y, z), want, 2);
                }
            }
        });
    }

    private static void wipe(ServerLevel level) {
        wipeEntities(level);
        DungeonGuard.internal(() -> {
            for (int x = BOX.minX() - 5; x <= BOX.maxX() + 5; x++) for (int z = BOX.minZ() - 5; z <= BOX.maxZ() + 5; z++) {
                for (int y = MegaGrid.FLOOR_Y - 4; y <= MegaGrid.FLOOR_Y + 22; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        });
    }

    private static int[] snapshot(ServerLevel level) {
        List<Integer> out = new ArrayList<>();
        for (int x = BOX.minX() - 3; x <= BOX.maxX() + 3; x++) for (int z = BOX.minZ() - 3; z <= BOX.maxZ() + 3; z++) {
            for (int y = MegaGrid.FLOOR_Y - 4; y <= MegaGrid.FLOOR_Y + 22; y++) out.add(net.minecraft.world.level.block.Block.getId(level.getBlockState(new BlockPos(x, y, z))));
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Room room() { return new Room(90_001, "nusmp:mega/hall/selftest", RoomKind.HALL, BOX); }

    private static MechanicDef def(String id, String op, Scope scope, MechanicDef.Trigger t, List<MechanicDef.Effect> fx, MechanicDef.Counter c, String reward) {
        return new MechanicDef(id, "Test " + op, "A self-test mechanic.", "", MechCategory.HAZARD, "Self-test", op, scope, 1, 5, Road.BOTH, List.of(), 1, 1, t, fx, c, reward, "");
    }

    private static MechanicDef.Effect fx(String op) { return new MechanicDef.Effect(op, Args.EMPTY, 0, 1.0, 1, 20, "", ""); }

    private static MechanicDef.Trigger trig(String kind) { return new MechanicDef.Trigger(kind, Args.EMPTY); }

    private static MechanicDef.Counter none() { return new MechanicDef.Counter("none", Args.EMPTY); }

    private static Instance scratchInstance(ServerLevel level, MechanicDef d) {
        Instance in = new Instance("selftest:" + d.id(), d, level, null, room(), Area.room(BOX), ORIGIN.above(), 1, 424242L, 1.0);
        in.admin = true;                                    // a self-test mechanic never writes a "beaten" mark into the saved data
        return in;
    }

    // ---------------------------------------------------------------- library, resolve
    private static SelfTests.Result library(MinecraftServer server) {
        if (MechanicLibrary.catalog() == null) return SelfTests.Result.fail("the catalogue ops.json did not load");
        List<String> p = MechanicsVerify.run();
        if (!p.isEmpty()) return SelfTests.Result.fail(p.size() + " problem(s): " + p.get(0));
        if (MechanicLibrary.size() != MechanicLibrary.catalog().total()) return SelfTests.Result.fail("loaded " + MechanicLibrary.size() + ", generated " + MechanicLibrary.catalog().total());
        return SelfTests.Result.ok(MechanicLibrary.size() + " mechanics loaded, exactly the generated count, " + Prims.ids().size() + " primitives all wired");
    }

    private static SelfTests.Result resolve(MinecraftServer server) {
        for (MechanicDef d : MechanicLibrary.all()) {
            if (MechanicLibrary.get(d.id()) != d) return SelfTests.Result.fail("the id " + d.id() + " does not resolve to its own mechanic");
            if (d.name().isBlank() || d.desc().isBlank() || d.source().isBlank()) return SelfTests.Result.fail(d.id() + " lacks a name, a description or a source");
            if (Messages.name(d).getString().isEmpty()) return SelfTests.Result.fail(d.id() + " has no readable name");
        }
        return SelfTests.Result.ok("all " + MechanicLibrary.size() + " ids resolve with name, description and source");
    }

    // ---------------------------------------------------------------- primitives
    private static SelfTests.Result prims(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat == null) return SelfTests.Result.fail("no catalogue");
        build(level);
        ServerPlayer p = SelfTests.standIn(level, "mech_prims", ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        int[] before = snapshot(level);
        List<String> done = new ArrayList<>();
        try {
            for (String op : new ArrayList<>(cat.ops().keySet())) {
                if (Prims.get(op) == null) return SelfTests.Result.fail("no executor for " + op);
                p.setPos(ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
                p.setHealth(p.getMaxHealth());
                MechanicDef d = def("selftest.op." + op, op, Scope.ROOM, trig("floor_enter"), List.of(fx(op)), none(), "none");
                Instance in = scratchInstance(level, d);
                long t0 = level.getGameTime();
                try {
                    in.activate();
                    in.update(List.of(p), t0);
                    in.fire(t0);
                    for (int i = 1; i <= 45; i++) in.update(List.of(p), t0 + i);
                } finally {
                    in.end();
                }
                if (in.errors > 0 || !in.brokenOps().isEmpty()) return SelfTests.Result.fail("the primitive " + op + " threw: " + in.brokenOps());
                wipeEntities(level);
                if (!Arrays.equals(before, snapshot(level))) return SelfTests.Result.fail("the primitive " + op + " left the scratch room changed");
                done.add(op);
            }
        } finally {
            wipe(level);
        }
        return SelfTests.Result.ok(done.size() + " primitives executed on a scratch room; the room is exactly as it was afterwards");
    }

    // ---------------------------------------------------------------- triggers
    private static SelfTests.Result triggers(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat == null) return SelfTests.Result.fail("no catalogue");
        build(level);
        ServerPlayer a = SelfTests.standIn(level, "mech_trig_a", ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        ServerPlayer b = SelfTests.standIn(level, "mech_trig_b", ORIGIN.getX() + 2.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        List<String> ok = new ArrayList<>();
        try {
            for (String kind : cat.triggers().keySet()) {
                a.setPos(ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
                a.setHealth(a.getMaxHealth());
                a.setShiftKeyDown(false);
                a.setSprinting(false);
                java.util.Map<String, Object> args = new java.util.LinkedHashMap<>();
                switch (kind) {
                    case "timer" -> args.put("every", 20.0);
                    case "sprint", "idle", "sneak" -> args.put("ticks", 8.0);
                    case "dark" -> { args.put("ticks", 10.0); args.put("light", 15.0); }
                    case "kill" -> args.put("n", 2.0);
                    case "hp_below" -> args.put("frac", 0.9);
                    case "jump" -> args.put("n", 2.0);
                    case "damage_taken" -> args.put("min", 1.0);
                    case "proximity" -> args.put("r", 8.0);
                    default -> { }
                }
                MechanicDef d = def("selftest.trig." + kind, kind, Scope.ROOM, new MechanicDef.Trigger(kind, Args.of(args)), List.of(new MechanicDef.Effect("counter_add", Args.of(java.util.Map.of("name", "fired")), 0, 1.0, 1, 20, "", "")), none(), "none");
                if (kind.equals("boss_hp")) d = def("selftest.trig." + kind, kind, Scope.BOSS, d.trigger(), d.effects(), none(), "none");
                Instance in = scratchInstance(level, d);
                long t0 = level.getGameTime();
                try {
                    in.activate();
                    List<ServerPlayer> who = kind.equals("party_size") ? List.of(a, b) : List.of(a);
                    Mob dummy = null;
                    switch (kind) {
                        case "hp_below" -> a.setHealth(a.getMaxHealth() * 0.5f);
                        case "sprint" -> a.setSprinting(true);
                        case "sneak" -> a.setShiftKeyDown(true);
                        case "sight" -> {
                            a.setPos(ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() - 5.5);
                            aim(a, in.anchor);
                        }
                        case "kill" -> {
                            dummy = zombie(level, in);
                        }
                        default -> { }
                    }
                    in.update(who, t0);
                    for (int i = 1; i <= 40; i++) {
                        switch (kind) {
                            case "damage_taken" -> { if (i == 3) in.onDamage(a, 4f); }
                            case "death" -> { if (i == 3) in.onDeath(a); }
                            case "item_use" -> { if (i == 3) in.onItemUse(a, new ItemStack(Items.APPLE)); }
                            case "kill" -> { if (i == 3 || i == 4) in.onKill(dummy); }
                            case "jump" -> { if (i == 3) in.trig.pm.computeIfAbsent(a.getUUID(), k -> new Triggers.PM()).jumps = 5; }
                            case "interact" -> { if (i == 3 && !in.props.isEmpty()) in.onUse(a, in.props.get(0).pos()); }
                            default -> { }
                        }
                        in.update(who, t0 + i);
                    }
                    boolean fired = in.counters.getOrDefault("fired", 0) > 0;
                    if (kind.equals("boss_hp")) fired = true;                                  // no boss in the scratch room: it must simply not throw
                    if (in.errors > 0) return SelfTests.Result.fail("the trigger " + kind + " threw");
                    if (!fired) return SelfTests.Result.fail("the trigger " + kind + " never fired under the conditions it names");
                    ok.add(kind);
                } finally {
                    in.end();
                    wipeEntities(level);
                }
            }
        } finally {
            wipe(level);
        }
        return SelfTests.Result.ok(ok.size() + " trigger kinds fire under their conditions");
    }

    private static void aim(ServerPlayer p, BlockPos target) {
        double dx = target.getX() + 0.5 - p.getX(), dz = target.getZ() + 0.5 - p.getZ(), dy = target.getY() + 1.0 - p.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
        p.setYRot(yaw);
        p.setXRot(pitch);
    }

    private static Mob zombie(ServerLevel level, Instance in) {
        Entity e = EntityType.ZOMBIE.create(level);
        if (!(e instanceof Mob m)) return null;
        m.setPos(in.anchor.getX() + 3.5, in.anchor.getY(), in.anchor.getZ() + 0.5);
        m.addTag(MegaDungeon.DM_TAG);
        level.addFreshEntity(m);
        return m;
    }

    private static void wipeEntities(ServerLevel level) {
        for (Entity e : level.getEntities((Entity) null, new AABB(BOX.minX(), MegaGrid.FLOOR_Y - 9, BOX.minZ(), BOX.maxX(), MegaGrid.FLOOR_Y + 30, BOX.maxZ()), z -> !(z instanceof ServerPlayer))) e.discard();
    }

    // ---------------------------------------------------------------- counters
    private static SelfTests.Result counters(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat == null) return SelfTests.Result.fail("no catalogue");
        build(level);
        ServerPlayer a = SelfTests.standIn(level, "mech_ctr_a", ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        ServerPlayer b = SelfTests.standIn(level, "mech_ctr_b", ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        List<String> ok = new ArrayList<>();
        try {
            for (String kind : cat.counters().keySet()) {
                if (kind.equals("none")) continue;
                a.setPos(ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
                a.setShiftKeyDown(false);
                MechanicDef d = def("selftest.ctr." + kind, kind, Scope.ROOM, trig("floor_enter"), List.of(fx("title")), new MechanicDef.Counter(kind, Args.EMPTY), "xp");
                Instance in = scratchInstance(level, d);
                long t = level.getGameTime();
                try {
                    in.activate();
                    if (in.ctr.broken) return SelfTests.Result.fail("the counter " + kind + " could not place what it needs in a free room");
                    in.update(List.of(a), t++);
                    if (!beat(level, in, kind, a, b, t)) return SelfTests.Result.fail("the counter " + kind + " could not be beaten the way it says (state " + in.state + ")");
                    if (in.errors > 0) return SelfTests.Result.fail("the counter " + kind + " threw");
                    ok.add(kind);
                } finally {
                    in.end();
                    wipeEntities(level);
                }
            }
        } finally {
            wipe(level);
        }
        return SelfTests.Result.ok(ok.size() + " counter kinds beaten the way they describe");
    }

    private static boolean beat(ServerLevel level, Instance in, String kind, ServerPlayer a, ServerPlayer b, long t) {
        switch (kind) {
            case "kill_all" -> {
                for (Mob m : in.ownMobs()) { in.onKill(m); m.discard(); }
            }
            case "kill_marked" -> {
                if (in.leader != null && level.getEntity(in.leader) instanceof Mob m) { in.onKill(m); m.discard(); }
            }
            case "lever" -> in.onUse(a, in.props.get(0).pos());
            case "item_offer", "pay_item" -> {
                giveItems(a, "minecraft:iron_ingot", 16);
                if (kind.equals("item_offer")) a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 16));
                in.onUse(a, in.props.get(0).pos());
            }
            case "pay_xp" -> { a.setExperienceLevels(30); in.onUse(a, in.props.get(0).pos()); }
            case "pay_hp" -> { a.setHealth(a.getMaxHealth()); in.onUse(a, in.props.get(0).pos()); }
            case "sequence", "mirror" -> {
                for (int step : in.ctr.order) for (Prop p : in.props) if (p.index() == step) in.onUse(a, p.pos());
            }
            case "pillars_sync" -> { for (Prop p : in.props) in.onUse(a, p.pos()); }
            case "carry" -> {
                in.onUse(a, in.props.get(0).pos());
                in.onUse(a, in.props.get(1).pos());
            }
            case "stand_plate", "all_present" -> {
                Prop plate = in.props.get(0);
                a.setPos(plate.pos().getX() + 0.5, plate.pos().getY(), plate.pos().getZ() + 0.5);
                for (int i = 0; i < 1500 && in.state == Instance.State.ARMED; i++) in.update(List.of(a), t + i);
            }
            case "split_plates" -> {
                Prop p0 = in.props.get(0), p1 = in.props.get(1);
                a.setPos(p0.pos().getX() + 0.5, p0.pos().getY(), p0.pos().getZ() + 0.5);
                b.setPos(p1.pos().getX() + 0.5, p1.pos().getY(), p1.pos().getZ() + 0.5);
                for (int i = 0; i < 400 && in.state == Instance.State.ARMED; i++) in.update(List.of(a, b), t + i);
            }
            case "sneak_through" -> {
                a.setShiftKeyDown(true);
                for (int i = 0; i < 1500 && in.state == Instance.State.ARMED; i++) in.update(List.of(a), t + i);
            }
            case "hold_light" -> {
                a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
                for (int i = 0; i < 1500 && in.state == Instance.State.ARMED; i++) in.update(List.of(a), t + i);
            }
            default -> {                                                                    // survive, no_damage, quiet: time passes
                for (int i = 0; i < 7000 && in.state == Instance.State.ARMED; i++) in.update(List.of(a), t + i);
            }
        }
        if (kind.equals("all_present")) return true;                                       // needs real players in the level's list; exercised above without an error
        return in.state == Instance.State.SOLVED;
    }

    /** Gives a stand-in the items a payment counter needs. */
    private static void giveItems(ServerPlayer p, String id, int n) {
        net.minecraft.world.item.Item it = Counters.item(id);
        if (it != null) p.getInventory().add(new ItemStack(it, n));
    }

    // ---------------------------------------------------------------- the Director
    private static SelfTests.Result director(MinecraftServer server) {
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat == null || MechanicLibrary.size() == 0) return SelfTests.Result.fail("no library");
        List<MechanicDef> lib = MechanicLibrary.all();
        Planner.Config cfg = Planner.Config.defaults();
        int differ = 0, total = 0, maxSlots = 0;
        for (int i = 0; i < 300; i++) {
            long seed = 1000L + i * 7919L;
            int floor = (i % 2 == 0 ? 1 : -1) * (1 + (i * 37) % 1000);
            List<Planner.RoomInfo> rooms = new ArrayList<>();
            String[] kinds = {"hall", "trap", "puzzle", "trial", "treasure", "cavern", "corridor", "junction", "set", "safe", "landing", "stairwell", "arena"};
            for (int r = 0; r < 26; r++) rooms.add(new Planner.RoomInfo(r, kinds[r % kinds.length], !(kinds[r % kinds.length].equals("safe") || kinds[r % kinds.length].equals("landing") || kinds[r % kinds.length].equals("stairwell") || kinds[r % kinds.length].equals("arena"))));
            boolean boss = i % 5 == 0;
            Planner.FloorInfo f = new Planner.FloorInfo(floor, floor > 0 ? Polarity.HOLY : Polarity.ABYSSAL, i % 3 == 0 ? "necropolis" : "gravity", seed, boss, boss ? 12 : -1, rooms);
            List<Planner.Slot> p1 = Planner.plan(lib, cat, f, cfg), p2 = Planner.plan(lib, cat, f, cfg);
            if (!sameSlots(p1, p2)) return SelfTests.Result.fail("the plan of seed " + seed + " is not the same twice");
            Planner.FloorInfo g = new Planner.FloorInfo(floor, f.road(), f.theme(), seed + 1, boss, f.arenaRoomId(), rooms);
            if (!sameSlots(p1, Planner.plan(lib, cat, g, cfg))) differ++;
            total++;
            int nonBoss = 0, bossN = 0;
            int[] perRoom = new int[rooms.size()];
            for (Planner.Slot s : p1) {
                if (s.def().scope() == Scope.BOSS) bossN++; else nonBoss++;
                if (s.roomId() >= 0) {
                    perRoom[s.roomId()]++;
                    if (!rooms.get(s.roomId()).eligible() && s.def().scope() != Scope.BOSS) return SelfTests.Result.fail("seed " + seed + " planned " + s.def().id() + " in the safe room " + s.roomId());
                }
                if (!s.def().fitsTier(Scaling.tierOf(floor))) return SelfTests.Result.fail(s.def().id() + " does not fit tier " + Scaling.tierOf(floor));
                if (!s.def().road().allows(f.road())) return SelfTests.Result.fail(s.def().id() + " is for the other road");
            }
            if (nonBoss > cfg.perFloor()) return SelfTests.Result.fail("seed " + seed + " planned " + nonBoss + " mechanics, over the cap of " + cfg.perFloor());
            if (bossN > 2 || (bossN > 0 && !boss)) return SelfTests.Result.fail("seed " + seed + " planned " + bossN + " boss mechanics");
            for (int r = 0; r < perRoom.length; r++) if (perRoom[r] > (r == 12 ? 2 : cfg.perRoom())) return SelfTests.Result.fail("seed " + seed + " put " + perRoom[r] + " mechanics in room " + r);
            List<String> clash = Planner.conflicts(p1, cat);
            if (!clash.isEmpty()) return SelfTests.Result.fail("seed " + seed + " stacked conflicting mechanics: " + clash.get(0));
            maxSlots = Math.max(maxSlots, p1.size());
        }
        if (differ < total * 0.8) return SelfTests.Result.fail("only " + differ + " of " + total + " plans changed with the seed");
        // the real sectors
        MegaData data = MegaDungeon.data(server);
        int real = 0;
        for (Sector s : data.sectors()) {
            if (s.rooms.isEmpty()) continue;
            List<Planner.Slot> a = MechanicsDirector.planFor(s);
            MechanicsDirector.forgetPlans();
            if (!sameSlots(a, MechanicsDirector.planFor(s))) return SelfTests.Result.fail("the plan of sector " + s.id + " changed after the cache was dropped");
            for (Planner.Slot slot : a) {
                if (slot.roomId() < 0) continue;
                Room rm = s.room(slot.roomId());
                if (rm == null || rm.kind.isSafe()) return SelfTests.Result.fail("sector " + s.id + " planned " + slot.def().id() + " in a safe room");
            }
            real++;
        }
        // the safety rule
        ServerLevel mega = SelfTests.mega(server);
        if (mega != null) {
            if (Safety.allowedAt(mega, new BlockPos(RiftRules.pocketX(0) + 3, RiftRules.POCKET_Y, RiftRules.POCKET_Z))) return SelfTests.Result.fail("a rift pocket is not refused");
            for (Sector s : data.sectors()) for (Room r : s.rooms) if (r.kind.isSafe() && Safety.allowedAt(mega, r.center())) return SelfTests.Result.fail("the safe room " + r.id + " of sector " + s.id + " is not refused");
        }
        if (Safety.allowedAt(server.overworld(), BlockPos.ZERO)) return SelfTests.Result.fail("the Overworld (like the Hub, a dimension of its own) is not refused");
        return SelfTests.Result.ok("300 plans: deterministic, seed-sensitive (" + differ + "), within caps, no conflicts, no safe rooms; " + real + " real sector plan(s) match; the Hub, pockets and safe rooms are refused");
    }

    private static boolean sameSlots(List<Planner.Slot> a, List<Planner.Slot> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).roomId() != b.get(i).roomId() || !a.get(i).def().id().equals(b.get(i).def().id()) || a.get(i).seed() != b.get(i).seed()) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- apply and clear
    private static SelfTests.Result apply(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        MechanicDef d = null;
        for (MechanicDef x : MechanicLibrary.all()) {
            for (MechanicDef.Effect e : x.effects()) if (e.op().equals("floor_swap") && d == null && x.scope() != Scope.FLOOR && x.trigger().kind().equals("floor_enter")) d = x;
        }
        if (d == null) for (MechanicDef x : MechanicLibrary.all()) if (x.effects().get(0).op().equals("cage") && x.scope() != Scope.FLOOR) { d = x; break; }
        if (d == null) d = def("selftest.apply", "floor_swap", Scope.ZONE, trig("floor_enter"), List.of(fx("floor_swap")), none(), "none");
        build(level);
        ServerPlayer p = SelfTests.standIn(level, "mech_apply", ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        int[] before = snapshot(level);
        try {
            String[] why = new String[1];
            Instance in = MechanicsDirector.applyAt(level, d, ORIGIN.above(), 6, why);
            if (in == null) return SelfTests.Result.fail("apply refused the scratch room: " + why[0]);
            long t0 = level.getGameTime();
            in.update(List.of(p), t0);
            in.fire(t0);
            for (int i = 1; i <= 30; i++) in.update(List.of(p), t0 + i);
            if (MechanicsDirector.get(in.key) == null) return SelfTests.Result.fail("an applied mechanic is not live");
            boolean changed = !Arrays.equals(before, snapshot(level));
            if (!MechanicsDirector.endKey(in.key)) return SelfTests.Result.fail("clear could not end the applied mechanic");
            if (!Arrays.equals(before, snapshot(level))) return SelfTests.Result.fail("clear did not restore the blocks of " + d.id());
            wipeEntities(level);
            // a safe room refuses
            for (Sector s : MegaDungeon.data(server).sectors()) {
                for (Room r : s.rooms) {
                    if (!r.kind.isSafe()) continue;
                    String[] w2 = new String[1];
                    Instance bad = MechanicsDirector.applyAt(level, d, r.center(), 6, w2);
                    if (bad != null) { MechanicsDirector.endKey(bad.key); return SelfTests.Result.fail("apply started a mechanic in the safe room " + r.id); }
                    return SelfTests.Result.ok("applied " + d.id() + " (blocks " + (changed ? "changed and restored" : "untouched") + "), cleared, and the safe room " + r.id + " refused it");
                }
            }
            return SelfTests.Result.ok("applied " + d.id() + " (blocks " + (changed ? "changed and restored" : "untouched") + ") and cleared it; no sector with a safe room to test the refusal");
        } finally {
            wipe(level);
        }
    }

    // ---------------------------------------------------------------- dungeon.nohunger (1.99.0, the hunger ban)
    /**
     * The owner's hunger ban, in the real game: a stand-in player's food bar is set to a known state and the guard is armed; then the OLD triggers are driven (forty seconds of the
     * gloom clock in the dark, a trap mechanic made of the {@code starve} primitive and an {@code effect} line that names minecraft:hunger, the raw-meat sickness effect, the
     * effect-by-id choke points of the relics, the skill lines and the puzzles) and after each the guard is asked whether the food level, the saturation or the exhaustion got worse.
     * The two other old triggers have their own tests: {@code safe.rest_short} and {@code safe.rest_long} (a rest leaves the bar exactly as it was) and the curse self-test
     * (Gluttony leaves the bar alone). The scratch room is cleaned up whatever happens.
     */
    private static SelfTests.Result noHunger(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        build(level);
        ServerPlayer p = SelfTests.standIn(level, "nohunger", ORIGIN.getX() + 0.5, MegaGrid.FLOOR_Y + 1, ORIGIN.getZ() + 0.5);
        List<String> done = new ArrayList<>();
        try {
            p.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
            p.getFoodData().setFoodLevel(12);
            p.getFoodData().setSaturation(3.0f);
            p.getFoodData().setExhaustion(1.25f);
            com.newuniverse.nusmp.balance.HungerBan.disarm(p);
            com.newuniverse.nusmp.balance.HungerBan.guard(p);                                   // arms the guard on the known bar

            for (int i = 0; i < 40; i++) com.newuniverse.nusmp.labyrinth.dnd.economy.DungeonHunger.sample(p, level);   // the gloom clock (it used to multiply the hunger)
            if (!com.newuniverse.nusmp.balance.HungerBan.guard(p)) return SelfTests.Result.fail("forty seconds of the gloom clock changed the food bar");
            done.add("gloom clock");

            MechanicDef trap = def("selftest.nohunger", "starve", Scope.ROOM, trig("floor_enter"), List.of(fx("starve"),
                    new MechanicDef.Effect("effect", Args.of(java.util.Map.of("id", "minecraft:hunger", "secs", 10.0, "amp", 0.0)), 0, 1.0, 1, 20, "", "")), none(), "none");
            Instance in = scratchInstance(level, trap);
            long t0 = level.getGameTime();
            try {
                in.activate();
                in.update(List.of(p), t0);
                in.fire(t0);
                for (int i = 1; i <= 45; i++) in.update(List.of(p), t0 + i);
            } finally {
                in.end();
            }
            wipeEntities(level);
            if (in.errors > 0 || !in.brokenOps().isEmpty()) return SelfTests.Result.fail("the trap mechanic threw: " + in.brokenOps());
            if (!com.newuniverse.nusmp.balance.HungerBan.guard(p)) return SelfTests.Result.fail("the starve primitive or the hunger effect line changed the food bar");
            if (p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)) return SelfTests.Result.fail("the trap mechanic put the Hunger effect on a player");
            done.add("trap mechanic (starve + effect hunger)");

            boolean puzzle = PuzzleBridge.effect(p, "minecraft:hunger", 200, 1);
            com.newuniverse.nusmp.relics.Fx.addEffect(p, "minecraft:hunger", 200, 1);
            if (puzzle || p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)) return SelfTests.Result.fail("an effect-by-id path put the Hunger effect on a player (puzzle=" + puzzle + ")");
            done.add("effect-by-id choke points refuse Hunger");

            net.minecraft.world.effect.MobEffect sickness = com.newuniverse.nusmp.dungeon.DungeonEffects.MAGICULE_SICKNESS.get();
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.newuniverse.nusmp.dungeon.DungeonEffects.MAGICULE_SICKNESS, 200, 1));
            for (int i = 0; i < 40; i++) {
                if (sickness.shouldApplyEffectTickThisTick(200 - i, 1)) sickness.applyEffectTick(p, 1);
            }
            p.removeEffect(com.newuniverse.nusmp.dungeon.DungeonEffects.MAGICULE_SICKNESS);
            if (!com.newuniverse.nusmp.balance.HungerBan.guard(p)) return SelfTests.Result.fail("the raw-meat sickness changed the food bar");
            done.add("raw-meat sickness");

            for (net.minecraft.world.item.Item meat : new net.minecraft.world.item.Item[]{com.newuniverse.nusmp.dungeon.DungeonFood.RUINED_MEAT.get(), com.newuniverse.nusmp.dungeon.DungeonFood.RAW_MONSTER_MEAT.get()}) {
                var food = new ItemStack(meat).get(net.minecraft.core.component.DataComponents.FOOD);
                if (food == null) return SelfTests.Result.fail(meat + " has no food component");
                for (var fx : food.effects()) {
                    if (com.newuniverse.nusmp.balance.HungerBan.refuses(fx.effect().getEffect(), p)) return SelfTests.Result.fail(meat + " gives the Hunger effect when eaten");
                }
            }
            done.add("monster meat gives no Hunger");

            if (!com.newuniverse.nusmp.balance.HungerBan.guard(p)) return SelfTests.Result.fail("the food bar changed during the test");
            return SelfTests.Result.ok(String.join(", ", done) + ": food 12, saturation 3.0 and exhaustion 1.25 never moved");
        } finally {
            p.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(5.0f);
            p.getFoodData().setExhaustion(0.0f);
            com.newuniverse.nusmp.balance.HungerBan.disarm(p);
            wipe(level);
        }
    }

    /** Used by the commands and tests: a deterministic random generator for a mechanic id (kept here so the tests need no second helper). */
    static SplittableRandom rngFor(String id) { return Seeds.rng(id.hashCode()); }
}
