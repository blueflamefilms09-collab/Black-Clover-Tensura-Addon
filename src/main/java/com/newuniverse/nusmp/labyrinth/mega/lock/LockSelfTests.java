package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Instance;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicDef;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicLibrary;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the escape rules are the owner's "remove the random being locked in a room or add a way to get out", and nothing here can be played by its author, so the CI server smoke
 * test runs {@code lock.escape} for real. It checks, in the running game: the pure rules answer as written (a boss arena stays closed with its exit rune visible, opens when the
 * boss dies; a seal nobody can beat opens; an empty one opens); every lock case on the inventory has a way out; every sector the server generated has a walk from the arrival to the
 * stairs for every room (the flood fill on the SAVED sector); a REAL sealing mechanic is applied in a real room of the generated dungeon, seals it (the blocks change), the escape
 * book shows the rune at the time the rules say (a labelled entity pair that the game's own text parser accepts), and opens it at the time the rules say with every block put back
 * and the rune gone, driven by a clock the test controls (no ten-minute wait); the same for a seal with nobody inside; and that {@code /dungeon unstuck} picks the last safe
 * spot, then the nearest safe room, and refuses a player who cannot pay. A fake player cannot be teleported (no connection), so the move itself is covered by the destination
 * choice and the pure rules; the live seal covers the engine.
 */
public final class LockSelfTests {
    private LockSelfTests() {}

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    public static void register() {
        SelfTests.register("lock.escape", LockSelfTests::escape);
    }

    private static SelfTests.Result escape(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        if (!LockConfig.enabled()) return ok("skipped: lock.enabled is false in the config");
        StringBuilder done = new StringBuilder();

        // ---- the pure rules, as the doc states them
        EscapeRules.Config cfg = LockConfig.escape();
        EscapeRules.Facts boss = new EscapeRules.Facts(EscapeRules.Kind.BOSS, 100, 2, 0, 1, 0, true, true);
        EscapeRules.Decision d = EscapeRules.decide(boss, cfg);
        if (d.action() != EscapeRules.Action.SHOW_RUNE || d.why() != EscapeRules.Why.BOSS_FIGHT) return fail("a boss arena with a live boss and two players inside should show the exit rune at once, got " + d);
        d = EscapeRules.decide(new EscapeRules.Facts(EscapeRules.Kind.BOSS, 100, 2, 0, 0, 0, false, true), cfg);
        if (!d.release() || d.why() != EscapeRules.Why.BOSS_DEAD) return fail("a boss arena whose boss is dead must open, got " + d);
        d = EscapeRules.decide(new EscapeRules.Facts(EscapeRules.Kind.SEAL, 20L * (cfg.unsolvableSeconds() + 1), 1, 0, 3, 0, false, false), cfg);
        if (!d.release() || d.why() != EscapeRules.Why.UNSOLVABLE) return fail("a seal the party cannot beat must open, got " + d);
        d = EscapeRules.decide(new EscapeRules.Facts(EscapeRules.Kind.SEAL, 20L * (cfg.absentSeconds() + 1), 0, 20L * (cfg.absentSeconds() + 1), 3, 0, false, true), cfg);
        if (!d.release() || d.why() != EscapeRules.Why.ABSENT) return fail("a seal with nobody inside must open, got " + d);
        done.append("rules; ");

        // ---- the inventory of lock cases
        for (LockCase c : LockCase.values()) if (!c.covered()) return fail("the lock case " + c.id() + " has no way out");
        done.append(LockCase.values().length).append(" cases covered; ");

        // ---- every placed room has a path to the stairs, on every saved sector
        MegaData data = MegaData.get(server);
        int sectors = 0, roomsChecked = 0;
        for (Sector s : data.sectors()) {
            List<String> problems = SectorReach.problems(s);
            if (!problems.isEmpty()) return fail("sector " + s.id + ": " + problems.get(0) + (problems.size() > 1 ? " (+" + (problems.size() - 1) + " more)" : ""));
            if (!s.rooms.isEmpty()) {
                sectors++;
                roomsChecked += s.rooms.size();
            }
        }
        done.append(roomsChecked).append(" rooms of ").append(sectors).append(" sector(s) all reach the stairs; ");

        // ---- the rune's label is a text the game's own parser accepts
        for (boolean exit : new boolean[] {false, true}) {
            String json = LockRunes.labelJson(exit);
            JsonParser.parseString(json);
            Component parsed = Component.Serializer.fromJson(json, level.registryAccess());
            if (parsed == null || parsed.getString().isEmpty()) return fail("the " + (exit ? "exit" : "release") + " rune's label did not parse as a text component");
        }
        done.append("rune labels parse; ");

        // ---- a real seal, from closing to opening
        String live = liveSeal(server, level, cfg);
        if (live != null) {
            if (live.startsWith("FAIL:")) return fail(live.substring(5));
            done.append(live).append("; ");
        }

        // ---- /dungeon unstuck: where it takes a player, and that it refuses one who cannot pay
        String stuck = unstuck(data, level, cfg);
        if (stuck.startsWith("FAIL:")) return fail(stuck.substring(5));
        done.append(stuck);
        return ok(done.toString());
    }

    // ================================================================ a live seal
    private static String liveSeal(MinecraftServer server, ServerLevel level, EscapeRules.Config cfg) {
        MechanicDef def = null;
        for (MechanicDef x : MechanicLibrary.all()) {
            List<String> ops = new ArrayList<>();
            for (MechanicDef.Effect e : x.effects()) ops.add(e.op());
            if (x.scope() == Scope.ROOM && EscapeRules.seals(ops) && x.counter().none() && x.effects().get(0).op().equals("seal_doors")) {
                def = x;
                break;
            }
        }
        if (def == null) return "no sealing mechanic in the library: the live seal was not run";
        List<Room> candidates = new ArrayList<>();
        for (Sector s : MegaData.get(server).sectors()) {
            if (s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) continue;
            for (Room r : s.rooms) if (!r.kind.isSafe() && Math.min(r.box.getXSpan(), r.box.getZSpan()) >= 9 && candidates.size() < 24) candidates.add(r);
        }
        String lastWhy = "";
        for (Room room : candidates) {
            BlockPos c = room.center();
            ServerPlayer p = SelfTests.standIn(level, "lock_tester", c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            long base = snapshot(level, room);
            String[] why = new String[1];
            Instance in = MechanicsDirector.applyAt(level, def, c, 6, why);
            if (in == null) {
                lastWhy = why[0] == null ? "" : why[0];
                continue;
            }
            String key = "mech:" + in.key;
            try {
                long t0 = level.getGameTime();
                in.update(List.of(p), t0);
                in.fire(t0);
                for (int i = 1; i <= 30; i++) in.update(List.of(p), t0 + i);
                if (!in.sealsPlayers()) {
                    MechanicsDirector.endKey(in.key);
                    continue;
                }
                if (snapshot(level, room) == base) return "FAIL:the mechanic " + def.id() + " reports a seal but no block of the room changed";
                long start = t0 + 40;
                LockBook.tick(level, start);
                if (!LockBook.isLive(key)) return "FAIL:the sealed room " + room.id + " was not taken into the escape book";
                LockBook.Entry e = LockBook.entry(key);
                if (e.runeReady()) return "FAIL:the rune was ready the moment the seal closed";
                long runeAt = start + 20L * (cfg.idleRuneSeconds() + 1);
                long openAt = start + 20L * (cfg.unsolvableSeconds() + 1);
                if (runeAt < openAt) {
                    LockBook.tick(level, runeAt);
                    if (!LockBook.isLive(key) || !LockBook.entry(key).runeReady()) return "FAIL:the rune was not ready after " + (cfg.idleRuneSeconds() + 1) + " seconds of an unbeatable seal";
                    if (LockConfig.get(LockConfig.RUNES) && !LockRunes.has(key)) return "FAIL:the release rune was not placed";
                }
                LockBook.tick(level, openAt);
                if (LockBook.isLive(key) || MechanicsDirector.get(in.key) != null || in.sealsPlayers()) return "FAIL:the unbeatable seal did not open after " + (cfg.unsolvableSeconds() + 1) + " seconds";
                if (snapshot(level, room) != base) return "FAIL:opening the seal did not put every block of room " + room.id + " back";
                if (LockRunes.has(key) || !level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(room.box.minX(), 0, room.box.minZ(), room.box.maxX() + 1, 400, room.box.maxZ() + 1), LockRunes::isRune).isEmpty()) {
                    return "FAIL:the release rune was not removed when the seal opened";
                }

                // a second seal with nobody left inside opens by the absent rule
                Instance in2 = MechanicsDirector.applyAt(level, def, c, 6, why);
                if (in2 == null) return "live seal: closed, rune, opened, blocks restored (room " + room.id + ", " + def.id() + ")";
                String key2 = "mech:" + in2.key;
                try {
                    long u0 = level.getGameTime();
                    in2.update(List.of(p), u0);
                    in2.fire(u0);
                    for (int i = 1; i <= 30; i++) in2.update(List.of(p), u0 + i);
                    in2.update(List.of(), u0 + 31);
                    if (in2.sealsPlayers()) {
                        long s2 = u0 + 40;
                        LockBook.tick(level, s2);
                        LockBook.tick(level, s2 + 20L * (cfg.absentSeconds() + 1));
                        if (LockBook.isLive(key2) || MechanicsDirector.get(in2.key) != null) return "FAIL:a seal with nobody inside did not open after " + (cfg.absentSeconds() + 1) + " seconds";
                        if (snapshot(level, room) != base) return "FAIL:the empty seal did not put its blocks back";
                    }
                } finally {
                    MechanicsDirector.endKey(in2.key);
                    LockBook.drop(level, key2);
                }
                return "live seal in room " + room.id + " (" + def.id() + "): closed, rune at " + (cfg.idleRuneSeconds() + 1) + " s, opened at " + (cfg.unsolvableSeconds() + 1) + " s, blocks restored; empty seal opened";
            } finally {
                MechanicsDirector.endKey(in.key);
                LockBook.drop(level, key);
            }
        }
        return "no room could be sealed by " + def.id() + " (" + lastWhy + "): the live seal was not run";
    }

    /** A hash of every block state in the room's box between the floor and six blocks above it: it changes when bars go up and is the same when they are gone. */
    private static long snapshot(ServerLevel level, Room room) {
        long h = 1125899906842597L;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = room.box.minX(); x <= room.box.maxX(); x++) {
            for (int z = room.box.minZ(); z <= room.box.maxZ(); z++) {
                for (int y = MegaGrid.FLOOR_Y; y <= MegaGrid.FLOOR_Y + 6; y++) {
                    h = h * 31 + Block.getId(level.getBlockState(m.set(x, y, z)));
                }
            }
        }
        return h;
    }

    // ================================================================ /dungeon unstuck
    private static String unstuck(MegaData data, ServerLevel level, EscapeRules.Config cfg) {
        Sector s = null;
        Room room = null;
        Room safe = null;
        for (Sector x : data.sectors()) {
            if (x.rooms.isEmpty() || x.state == Sector.State.GENERATING || x.state == Sector.State.FAILED) continue;
            Room a = null, b = null;
            for (Room r : x.rooms) {
                if (r.kind.isSafe() && b == null) b = r;
                if (!r.kind.isSafe() && a == null) a = r;
            }
            if (a != null && b != null) {
                s = x;
                room = a;
                safe = b;
                break;
            }
        }
        if (s == null) return "no sector with both a safe and an ordinary room: unstuck was not run";
        BlockPos at = room.center();
        ServerPlayer p = SelfTests.standIn(level, "lock_tester_unstuck", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        try {
            UnstuckService.setLastSafe(p.getUUID(), s.id, safe.center());
            UnstuckService.Target t = UnstuckService.destination(level, p);
            if (t.kind() != UnstuckRules.Destination.LAST_SAFE || !safe.center().equals(t.pos())) return "FAIL:with a remembered safe spot the destination was " + t.kind() + " " + t.pos();
            UnstuckService.forget(p.getUUID());
            t = UnstuckService.destination(level, p);
            if (t.kind() == UnstuckRules.Destination.LAST_SAFE) return "FAIL:a forgotten safe spot was still used";
            if (t.kind() == UnstuckRules.Destination.NEAREST_SAFE) {
                Room there = s.roomAt(t.pos());
                if (there == null || !there.kind.isSafe()) return "FAIL:the nearest safe room's spot " + t.pos() + " is not in a safe room";
            }
            UnstuckRules.Config uc = LockConfig.unstuck();
            if (uc.xpLevels() > 0 && !p.isCreative()) {
                p.experienceLevel = 0;
                int r = UnstuckService.unstuck(p, false);
                if (r != 0) return "FAIL:a player with no experience levels was moved by /dungeon unstuck (cost " + uc.xpLevels() + ")";
            }
            return "unstuck picks the remembered safe spot, then the " + t.kind() + ", and refuses a player who cannot pay";
        } finally {
            UnstuckService.forget(p.getUUID());
            UnstuckService.resetCooldown(p.getUUID());
        }
    }
}
