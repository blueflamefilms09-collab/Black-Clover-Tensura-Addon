package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.ExclusivePockets;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketBuild;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketCell;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDef;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDefs;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketLayout;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketPlan;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketRuntime;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketWatchdog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

/**
 * Why: {@code rift.pocket_fix} (1.101.0, docs/labyrinth/pocket_fix.md) is the in-game proof that the hidden dungeons are what the owner asked for after he found "only one room, or no switch": it builds each
 * of the old realms (the boss and the puzzle kind) and each of the thirteen exclusive tombs in a scratch cell of the pocket field, under the REAL Dungeon Guard (the test says whether the guard is on and that it
 * would have refused the doorways), and asserts on the REAL blocks: the realm has at least four rooms and a tomb at least five; a flood fill over air from the arrival reaches every room (a tomb's treasury only after
 * the seal opens, a realm's vault only after the bars lift); the release lever stands on a wall in a room that the arrival reaches WITHOUT crossing the door it opens, with its signs and lanterns; pulling it early
 * is refused with a clock, pulling it when it is ready really opens the door (the bars are air, the seal blocks are air, the treasury is reachable); the door opens by itself after the configured time; a tomb is
 * not entered while it is still forming (the delver is held at the tear and taken in the tick it stands); the watchdog finds a doorway that was walled up and the repair pass opens it again; and the cell is cleared
 * at the end. Everything it builds is cleaned up whatever happens.
 */
public final class PocketFixSelfTests {
    private PocketFixSelfTests() {}

    private static final int SLOT = RiftRules.MAX_SLOTS - 3;

    public static void register() { SelfTests.register("rift.pocket_fix", PocketFixSelfTests::run); }

    private static SelfTests.Result fail(String s) { return SelfTests.Result.fail(s); }

    private static void air(ServerLevel level, BlockPos pos) { DungeonGuard.internal(() -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2)); }

    private static void put(ServerLevel level, BlockPos pos, BlockState s) { DungeonGuard.internal(() -> level.setBlock(pos, s, 2)); }

    private static SelfTests.Result run(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("the Mega-Dungeon dimension is not loaded");
        PocketRealm realm = PocketRealm.get(server);
        if (realm.at(SLOT) != null) realm.collapseSlot(mega, SLOT);
        // under the REAL guard: it holds the dimension, and without the builder's internal span it would refuse a doorway (a wall block turned to air)
        BlockPos probe = PocketBuilder.world(SLOT, -15, 1, 0);
        mega.getChunk(probe.getX() >> 4, probe.getZ() >> 4);
        put(mega, probe, Blocks.STONE.defaultBlockState());
        boolean refuses = DungeonGuard.refusesSet(mega, probe, Blocks.AIR.defaultBlockState());
        air(mega, probe);
        List<String> notes = new ArrayList<>();
        notes.add("Dungeon Guard " + (DungeonGuard.on(mega) ? "ON" : "off in this world") + ", and outside the builder's internal span it " + (refuses ? "REFUSES a wall block turned to air (the old defect)" : "does not refuse that"));
        if (DungeonGuard.on(mega) && !refuses) return fail("the Dungeon Guard is on but does not refuse a wall block turned to air: the premise of this test (and of the fix) does not hold in this build");
        for (RiftRules.Variant v : RiftRules.Variant.values()) {
            SelfTests.Result r = realmCase(server, mega, realm, v, notes);
            if (!r.ok()) return r;
        }
        List<PocketDef> defs = PocketDefs.all();
        if (defs.size() < 13) return fail("only " + defs.size() + " exclusive tombs are loaded, 13 are expected");
        for (int i = 0; i < defs.size(); i++) {
            SelfTests.Result r = tombCase(server, mega, realm, defs.get(i), 5150L + i * 17L, i == 0, notes);
            if (!r.ok()) return r;
        }
        return SelfTests.Result.ok(String.join("; ", notes));
    }

    // ================================================================ a flood fill over the real blocks
    /** The cells a walking player can stand in and reach from {@code from} (no collision, no fluid) within a box. */
    private static Set<Long> flood(ServerLevel level, BlockPos from, int x0, int y0, int z0, int x1, int y1, int z1) {
        Set<Long> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(from);
        seen.add(from.asLong());
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            BlockPos c = q.poll();
            for (int[] d : dirs) {
                int nx = c.getX() + d[0], ny = c.getY() + d[1], nz = c.getZ() + d[2];
                if (nx < x0 || nx > x1 || ny < y0 || ny > y1 || nz < z0 || nz > z1) continue;
                m.set(nx, ny, nz);
                long key = m.asLong();
                if (seen.contains(key)) continue;
                BlockState st = level.getBlockState(m);
                if (!st.getCollisionShape(level, m).isEmpty() || !st.getFluidState().isEmpty()) continue;
                seen.add(key);
                q.add(new BlockPos(nx, ny, nz));
            }
        }
        return seen;
    }

    // ================================================================ the old realms
    private static SelfTests.Result realmCase(MinecraftServer server, ServerLevel mega, PocketRealm realm, RiftRules.Variant want, List<String> notes) {
        String k = "[realm " + want.id() + "] ";
        PocketRealm.Pocket p = new PocketRealm.Pocket();
        p.slot = SLOT;
        p.floor = 12;
        p.variant = want.id();
        p.seed = 616161L;
        p.createdAt = mega.getGameTime();
        p.expiresAt = p.createdAt + 20L * 720;
        FakePlayer fake = SelfTests.standIn(mega, "pocket_fix_realm", RiftRules.pocketX(SLOT) - 19, RiftRules.POCKET_Y + 1, RiftRules.POCKET_Z);
        try {
            PocketBuilder.build(mega, p, RegionTheme.GRAVITY);
            realm.pockets().put(p.slot, p);
            RealmPlan plan = PocketBuilder.planOf(p);
            if (plan.rooms().size() < 4) return fail(k + "only " + plan.rooms().size() + " rooms");
            List<String> gaps = PocketBuilder.verify(mega, p);
            if (!gaps.isEmpty()) return fail(k + "the finished realm is not whole: " + gaps);
            // every doorway really is open under the real guard, and the rooms are connected on foot
            int x0 = RiftRules.pocketX(SLOT) - 30, x1 = RiftRules.pocketX(SLOT) + 26, z0 = RiftRules.POCKET_Z - 26, z1 = RiftRules.POCKET_Z + 16, y0 = RiftRules.POCKET_Y - 2, y1 = RiftRules.POCKET_Y + 14;
            Set<Long> seen = flood(mega, p.entry, x0, y0, z0, x1, y1, z1);
            Set<String> reached = new TreeSet<>();
            for (RealmPlan.Room r : plan.rooms()) {
                for (int dx = r.x0(); dx <= r.x1() && !reached.contains(r.id()); dx++)
                    for (int dz = r.z0(); dz <= r.z1(); dz++)
                        if (seen.contains(PocketBuilder.world(SLOT, dx, 0, dz).asLong())) {
                            reached.add(r.id());
                            break;
                        }
            }
            if (!reached.containsAll(List.of(RealmPlan.ANTECHAMBER, RealmPlan.CORRIDOR, RealmPlan.HALL, RealmPlan.LEVER_ROOM))) return fail(k + "the arrival walks only to " + reached + " (the doorways are shut)");
            if (reached.contains(RealmPlan.VAULT)) return fail(k + "the vault can be walked into with the bars up");
            // the lever: a real lever block on a wall, in a room that was reached, with its signs and lanterns
            BlockPos lever = PocketBuilder.leverPos(SLOT);
            BlockState ls = mega.getBlockState(lever);
            if (!ls.is(Blocks.LEVER)) return fail(k + "no release lever at " + lever.toShortString() + " (a " + ls.getBlock() + " is there)");
            if (!seen.contains(PocketBuilder.leverStand(SLOT).asLong())) return fail(k + "the cell in front of the lever cannot be reached from the arrival");
            int signs = 0, lanterns = 0;
            for (RealmPlan.Feature f : plan.features()) {
                BlockPos at = PocketBuilder.world(SLOT, f.x(), f.y(), f.z());
                if (f.type() == RealmPlan.Type.SIGN) {
                    if (!(mega.getBlockState(at).getBlock() instanceof WallSignBlock)) return fail(k + "no sign at " + at.toShortString());
                    signs++;
                }
                if (f.type() == RealmPlan.Type.LANTERN) {
                    if (!mega.getBlockState(at).is(Blocks.LANTERN)) return fail(k + "no lantern at " + at.toShortString());
                    lanterns++;
                }
            }
            if (signs < 3 || lanterns < 2) return fail(k + "the lever has " + signs + " signs and " + lanterns + " lanterns, not 3 and 2");
            for (BlockPos b : p.bars) if (!mega.getBlockState(b).is(Blocks.IRON_BARS)) return fail(k + "the vault is not barred at " + b.toShortString());
            // pulling it too early is refused with a clock (when the config has a delay)
            long unlock = 20L * RiftConfig.get(RiftConfig.SWITCH_UNLOCK_SECONDS);
            if (unlock > 0) {
                String early = PocketSwitch.pull(mega, realm, p, fake);
                if (!early.equals("locked")) return fail(k + "pulling the lever at once answered '" + early + "', not 'locked'");
                for (BlockPos b : p.bars) if (!mega.getBlockState(b).is(Blocks.IRON_BARS)) return fail(k + "the stiff lever opened the bars");
            }
            // the time passes: the lever works and the bars really go
            p.createdAt -= unlock + 20;
            String pulled = PocketSwitch.pull(mega, realm, p, fake);
            if (!pulled.equals("opened")) return fail(k + "pulling the ready lever answered '" + pulled + "'");
            for (BlockPos b : p.bars) if (!mega.getBlockState(b).isAir()) return fail(k + "a bar stayed after the lever was pulled at " + b.toShortString());
            if (!mega.getBlockState(lever).getValue(LeverBlock.POWERED)) return fail(k + "the lever did not click down");
            Set<Long> open = flood(mega, p.entry, x0, y0, z0, x1, y1, z1);
            if (!open.contains(PocketBuilder.world(SLOT, 16, 0, 0).asLong())) return fail(k + "the vault cannot be walked into after the bars lifted");
            if (!PocketSwitch.pull(mega, realm, p, fake).equals("spent")) return fail(k + "a second pull was not 'spent'");
            if (realm.clear(mega, p, "again")) return fail(k + "the vault was opened twice");
            // the watchdog: a doorway walled up is found and mended
            BlockPos door = PocketBuilder.world(SLOT, -15, 1, 0);
            put(mega, door, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            if (PocketBuilder.verify(mega, p).isEmpty()) return fail(k + "verify did not see the doorway walled up");
            List<String> found = realm.keepWhole(mega, p, RegionTheme.GRAVITY);
            if (found.isEmpty() || !PocketBuilder.verify(mega, p).isEmpty() || !mega.getBlockState(door).isAir()) return fail(k + "the repair did not open the doorway again");
            // the door opens by itself after the configured time (a second realm state: bars shut again by a rebuild)
            notes.add(k + plan.rooms().size() + " rooms walked, lever pulled (locked first, then opened), watchdog mended a doorway");
        } finally {
            fake.discard();
            realm.pockets().remove(SLOT);
            PocketBuilder.clear(mega, SLOT);
            for (var e : mega.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, PocketBuilder.bounds(SLOT))) e.discard();
            realm.setDirty();
        }
        BlockPos bar = PocketBuilder.world(SLOT, 12, 0, 0);
        if (!mega.getBlockState(bar).isAir() || mega.getBlockState(PocketBuilder.world(SLOT, 0, 1, -19)).is(Blocks.LEVER)) return fail(k + "the cell was not cleared after the realm collapsed");
        return autoOpen(mega, realm, want, notes);
    }

    /** The door that opens by itself: a realm older than the configured time, with someone inside, opens its vault on its own tick. */
    private static SelfTests.Result autoOpen(ServerLevel mega, PocketRealm realm, RiftRules.Variant want, List<String> notes) {
        String k = "[realm " + want.id() + " auto] ";
        PocketRealm.Pocket p = new PocketRealm.Pocket();
        p.slot = SLOT;
        p.floor = 12;
        p.variant = want.id();
        p.seed = 717171L;
        p.createdAt = mega.getGameTime() - 20L * (RiftConfig.get(RiftConfig.SWITCH_AUTO_SECONDS) + 60);
        p.expiresAt = mega.getGameTime() + 20L * 600;
        FakePlayer fake = SelfTests.standIn(mega, "pocket_fix_auto", RiftRules.pocketX(SLOT) - 19, RiftRules.POCKET_Y + 1, RiftRules.POCKET_Z);
        try {
            PocketBuilder.build(mega, p, RegionTheme.GRAVITY);
            realm.pockets().put(p.slot, p);
            if (PocketSwitch.tick(mega, realm, p, List.of(), mega.getGameTime())) return fail(k + "an empty realm opened its vault by itself");
            boolean did = PocketSwitch.tick(mega, realm, p, List.of(fake), mega.getGameTime());
            if (!did) return fail(k + "a realm past its auto-open time with someone inside did not open its vault");
            for (BlockPos b : p.bars) if (!mega.getBlockState(b).isAir()) return fail(k + "a bar stayed after the automatic opening at " + b.toShortString());
            notes.add(k + "opened by itself");
        } finally {
            fake.discard();
            realm.pockets().remove(SLOT);
            PocketBuilder.clear(mega, SLOT);
            for (var e : mega.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, PocketBuilder.bounds(SLOT))) e.discard();
            realm.setDirty();
        }
        return SelfTests.Result.ok("");
    }

    // ================================================================ the thirteen tombs
    private static SelfTests.Result tombCase(MinecraftServer server, ServerLevel mega, PocketRealm realm, PocketDef def, long seed, boolean gating, List<String> notes) {
        String k = "[tomb " + def.id() + "] ";
        BlockPos site = new BlockPos(RiftRules.POCKET_X0 + 20, RiftRules.POCKET_Y + 20, RiftRules.POCKET_Z + 400);
        mega.getChunk(site.getX() >> 4, site.getZ() >> 4);
        FakePlayer p = SelfTests.standIn(mega, "pocket_fix_tomb", site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
        PocketRealm.Pocket pk = null;
        try {
            pk = ExclusivePockets.create(mega, realm, def, seed, 12, SLOT);
            if (pk == null) return fail(k + "the tomb could not be raised in the test cell");
            PocketRuntime rt = PocketRuntime.of(pk);
            if (rt == null) return fail(k + "no runtime");
            if (gating) {                                                                  // nobody arrives in a tomb that is still being built: the delver is held at the tear until it stands
                ExclusivePockets.join(mega, realm, pk, def, List.of(p), null);
                if (RiftRules.slotAt(p.getX(), p.getZ()) == SLOT) return fail(k + "the delver was taken into a tomb that was still forming (phase " + pk.phase + ")");
                if (!rt.waiting.containsKey(p.getUUID())) return fail(k + "the delver was not held at the tear");
                int n = 0;
                while (pk.phase == 0 && n++ < 8000) ExclusivePockets.fast(server);
                if (pk.phase != 1) return fail(k + "the tomb did not finish forming through the real jobs (phase " + pk.phase + " after " + n + " ticks, " + rt.build.percent() + "%)");
                if (RiftRules.slotAt(p.getX(), p.getZ()) != SLOT) return fail(k + "the delver was not taken in when the tomb stood: " + p.position());
                notes.add(k + "held at the tear for " + n + " ticks, then taken in");
            } else {
                int n = 0;
                while (!rt.build.done() && n++ < 4000) rt.build.step(mega, 200_000);
                if (!rt.build.done()) return fail(k + "the build did not finish (" + rt.build.percent() + "%)");
                pk.phase = 1;
            }
            PocketLayout l = rt.layout;
            if (l.rooms().size() < 5) return fail(k + "only " + l.rooms().size() + " rooms");
            // 1. the tomb is whole
            List<String> gaps = PocketWatchdog.missing(mega, rt, pk);
            if (!gaps.isEmpty()) return fail(k + "the finished tomb is not whole: " + gaps);
            // 2. walk it on the real blocks with the seal shut
            int cx = PocketCell.centreX(SLOT), cz = PocketCell.centreZ();
            Set<Long> seen = flood(mega, rt.arrival, cx + rt.plan.minX() - 2, PocketPlan.Y0 - 3, cz + rt.plan.minZ() - 2, cx + rt.plan.maxX() + 2, PocketPlan.Y0 + 26, cz + rt.plan.maxZ() + 2);
            for (PocketLayout.Room r : l.rooms()) {
                if (r.id() == l.treasuryId()) continue;
                if (!roomReached(rt, r, seen)) return fail(k + "room " + r.id() + " (" + r.kind() + ") cannot be walked to from the arrival: a doorway is shut");
            }
            if (roomReached(rt, l.treasury(), seen)) return fail(k + "the treasury can be walked into with the seal shut");
            // 3. the lever: a real lever on a wall of the key room, reachable with the seal shut, with its sign
            if (rt.switchPos == null || !mega.getBlockState(rt.switchPos).is(Blocks.LEVER)) return fail(k + "no seal lever at " + rt.switchPos);
            if (l.roomAt(rt.lx(rt.switchPos.getX() + 0.5), rt.lz(rt.switchPos.getZ() + 0.5)) != l.keyId()) return fail(k + "the seal lever is not in the key room");
            net.minecraft.core.Direction face = mega.getBlockState(rt.switchPos).getValue(LeverBlock.FACING);
            if (!seen.contains(rt.switchPos.relative(face).below().asLong())) return fail(k + "the cell in front of the seal lever is not reachable with the seal shut: the lever would be behind its own door");
            if (rt.sigilChest == null || !mega.getBlockState(rt.sigilChest).is(Blocks.CHEST)) return fail(k + "no chest for the Sigil at " + rt.sigilChest);
            if (rt.signs.isEmpty()) return fail(k + "no signs");
            for (BlockPos sign : rt.signs) if (!(mega.getBlockState(sign).getBlock() instanceof WallSignBlock)) return fail(k + "no sign at " + sign.toShortString());
            if (rt.shortcutExit == null || !mega.getBlockState(rt.shortcutExit).is(RiftRegistry.RIFT.get())) return fail(k + "no exit rift in the treasury at " + rt.shortcutExit);
            // 4. too early: stiff
            long unlock = ExclusivePockets.unlockTicks();
            if (unlock > 0) {
                if (!ExclusivePockets.pullLever(mega, realm, pk, rt, p).equals("locked")) return fail(k + "the lever opened at once");
                if (pk.sealOpen) return fail(k + "a stiff lever opened the seal");
            }
            // 5. time passes: the lever really opens the seal, the treasury becomes reachable, and the watchdog finds a doorway walled up
            pk.createdAt -= unlock + 20;
            if (!ExclusivePockets.pullLever(mega, realm, pk, rt, p).equals("opened")) return fail(k + "the ready lever did not open the seal");
            if (!pk.sealOpen) return fail(k + "the seal is not open after the lever");
            for (long sb : rt.sealBlocks) if (!mega.getBlockState(BlockPos.of(sb)).isAir()) return fail(k + "a seal block stayed after the lever at " + BlockPos.of(sb).toShortString());
            Set<Long> open = flood(mega, rt.arrival, cx + rt.plan.minX() - 2, PocketPlan.Y0 - 3, cz + rt.plan.minZ() - 2, cx + rt.plan.maxX() + 2, PocketPlan.Y0 + 26, cz + rt.plan.maxZ() + 2);
            if (!roomReached(rt, l.treasury(), open)) return fail(k + "the treasury cannot be reached after the lever");
            if (!open.contains(rt.shortcutExit.asLong())) return fail(k + "the treasury's exit rift cannot be reached after the lever");
            if (!ExclusivePockets.pullLever(mega, realm, pk, rt, p).equals("spent")) return fail(k + "a second pull was not 'spent'");
            PocketLayout.Door first = null;
            for (PocketLayout.Door d : l.doors()) if (!d.sealed()) { first = d; break; }
            int[] w = rt.plan.doorway(first, first.a());
            BlockPos mid = PocketCell.world(SLOT, (w[0] + w[3]) / 2, w[1] + 1, (w[2] + w[5]) / 2);
            put(mega, mid, Blocks.STONE.defaultBlockState());
            if (PocketWatchdog.missing(mega, rt, pk).isEmpty()) return fail(k + "the watchdog did not see a doorway walled up");
            rt.repairJob = new PocketBuild(rt, true, pk.sealOpen);
            int n = 0;
            while (!rt.repairJob.step(mega, 200_000) && n++ < 4000) { /* the repair pass runs to its end */ }
            rt.repairJob = null;
            if (!mega.getBlockState(mid).isAir()) return fail(k + "the repair pass did not open the doorway again");
            for (long sb : rt.sealBlocks) if (!mega.getBlockState(BlockPos.of(sb)).isAir()) return fail(k + "the repair pass put the opened seal back");
            if (!PocketWatchdog.missing(mega, rt, pk).isEmpty()) return fail(k + "the tomb is not whole after the repair: " + PocketWatchdog.missing(mega, rt, pk));
            // 6. the sweep gives the cell back (under the real guard)
            ExclusivePockets.collapse(mega, realm, pk);
            int steps = 0;
            while (realm.at(SLOT) != null && steps++ < 4000) {
                if (rt.clear != null) rt.clear.step(mega, 200_000);
                ExclusivePockets.tick(mega, realm, pk, List.of(), mega.getGameTime(), 0);
            }
            if (realm.at(SLOT) != null) return fail(k + "the cell was not freed after " + steps + " sweeps");
            BlockPos probe = PocketCell.world(SLOT, rt.arrival.getX() - cx, PocketPlan.Y0 - 1, rt.arrival.getZ() - cz);
            if (!mega.getBlockState(probe).isAir()) return fail(k + "the entry floor is still standing after the sweep: the Dungeon Guard refused the clearing");
            pk = null;
            notes.add(k + l.rooms().size() + " rooms (" + kinds(l) + "), lever, Sigil chest, " + rt.signs.size() + " signs, treasury exit, watchdog, sweep");
            return SelfTests.Result.ok("");
        } finally {
            if (pk != null && realm.at(SLOT) != null) {
                ExclusivePockets.collapse(mega, realm, pk);
                PocketRuntime rt = PocketRuntime.peek(SLOT);
                for (int i = 0; rt != null && rt.clear != null && !rt.clear.done() && i < 4000; i++) rt.clear.step(mega, 200_000);
                realm.pockets().remove(SLOT);
                PocketRuntime.forget(SLOT);
                realm.setDirty();
            }
            p.getPersistentData().remove(PocketRealm.RETURN);
            p.discard();
        }
    }

    private static String kinds(PocketLayout l) {
        Set<PocketLayout.Kind> k = new TreeSet<>();
        for (PocketLayout.Room r : l.rooms()) k.add(r.kind());
        return k.size() + " kinds";
    }

    /** Is any floor cell of the room's interior reached? */
    private static boolean roomReached(PocketRuntime rt, PocketLayout.Room r, Set<Long> seen) {
        PocketPlan.RoomBox b = rt.plan.box(r.id());
        for (int x = b.x0(); x <= b.x1(); x++)
            for (int z = b.z0(); z <= b.z1(); z++)
                if (seen.contains(PocketCell.world(rt.slot, x, PocketPlan.Y0, z).asLong()) || seen.contains(PocketCell.world(rt.slot, x, PocketPlan.Y0 + 1, z).asLong())) return true;
        return false;
    }
}
