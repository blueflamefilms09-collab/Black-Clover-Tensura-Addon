package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsConfig;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Planner;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Scope;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the floor report is the owner's "the System should tell you what the theme is for the dungeon's floor and also what the mechanic", and nobody can play it, so the CI
 * server smoke test runs it for real. {@code system.floor_info} stands a player in a room of a generated sector, fires the real floor event through the facade
 * ({@code MegaDungeon.fireFloorEntered}, the call the floor tracker makes on a staircase, an arrival, a respawn and a login), and checks what the System built: the floor and
 * its direction, the theme of THAT sector with a name and a one-line description and its road, the floor-wide mechanics exactly as the Director planned them (each with its
 * counter line), the depth numbers, that the words compose with every name in them, that a second arrival at the same floor is not repeated (no spam) but a different floor and a
 * request are, that the card survives the real network codec, that a theme or a mechanic with no text still reads (the fallbacks), and that a mechanic waking in a room is the
 * one kind of change the mid-floor watcher announces. A fake player has no connection, so the card is read back from {@link FloorInfo#last} instead of the wire.
 */
public final class FloorInfoSelfTests {
    private FloorInfoSelfTests() {}

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    public static void register() {
        SelfTests.register("system.floor_info", FloorInfoSelfTests::floorInfo);
    }

    private static SelfTests.Result floorInfo(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        if (!FloorInfoConfig.enabled() || !SystemConfig.enabled()) return ok("skipped: the floor report or the System is switched off in the config");
        Sector s = null;
        Room room = null;
        for (Sector x : MegaData.get(server).sectors()) {
            if (x.rooms.isEmpty() || x.state == Sector.State.GENERATING || x.state == Sector.State.FAILED) continue;
            for (Room r : x.rooms) if (!r.kind.isSafe() && Math.min(r.box.getXSpan(), r.box.getZSpan()) >= 9) {
                s = x;
                room = r;
                break;
            }
            if (s != null) break;
        }
        if (s == null) return ok("no generated sector with an ordinary room to stand in; the pure parts are covered by FloorInfoRulesTest and FloorCardLinesTest");
        BlockPos at = room.center();
        ServerPlayer p = SelfTests.standIn(level, "floor_info_tester", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        FloorInfo.forget(p.getUUID());
        try {
            int floor = FloorInfo.floorOf(p);
            if (floor == 0) floor = room.floor != 0 ? room.floor : s.firstFloor();
            if (floor == 0) return fail("sector " + s.id + " has no floor number");

            // ---- an arrival through the real facade
            MegaDungeon.fireFloorEntered(p, floor, 0);
            FloorCard c = FloorInfo.last(p.getUUID());
            if (c == null) return fail("the floor event built no report");
            if (c.floor() != floor || c.reasonKind() != FloorInfoRules.Reason.ARRIVE) return fail("the report is for floor " + c.floor() + " (" + c.reasonKind() + "), expected an arrival on " + floor);
            if (c.up() != (floor > 0)) return fail("the direction is " + (c.up() ? "UP" : "DOWN") + " for floor " + floor);
            if (!c.themeId().equals(s.theme.id())) return fail("the report names the theme '" + c.themeId() + "' but the sector wears '" + s.theme.id() + "'");
            if (c.themeName().isEmpty() || c.themeDesc().isEmpty()) return fail("the theme has no name or no description");
            if (!c.alignRoad().equals("holy") && !c.alignRoad().equals("abyssal") && !c.alignRoad().equals("any")) return fail("the alignment code is '" + c.align() + "'");
            if (c.depth() != Math.abs(floor)) return fail("the depth is " + c.depth() + " for floor " + floor);

            // ---- the mechanics, exactly as planned
            if (c.mechanicsOn() != MechanicsConfig.enabled()) return fail("the report says mechanics are " + (c.mechanicsOn() ? "on" : "off") + " but the config says the opposite");
            List<String> planned = new ArrayList<>();
            if (MechanicsConfig.enabled()) for (Planner.Slot slot : MechanicsDirector.planFor(s)) if (slot.def().scope() == Scope.FLOOR) planned.add(slot.def().id());
            List<String> shown = new ArrayList<>();
            for (FloorCard.Mech m : c.floorMechs()) shown.add(m.id());
            List<String> expected = planned.size() <= FloorCard.MAX_MECHS + 1 ? planned : planned.subList(0, FloorCard.MAX_MECHS + 1);
            if (!expected.equals(shown)) return fail("the report lists the floor mechanics " + shown + " but the Director planned " + planned);
            for (FloorCard.Mech m : c.floorMechs()) {
                if (m.name().isEmpty() || m.desc().isEmpty()) return fail("the mechanic " + m.id() + " has no name or no description in the report");
                if (m.counterKind().isEmpty()) return fail("the mechanic " + m.id() + " has no counter line");
            }

            // ---- the words
            List<FloorCardLines.Line> lines = FloorCardLines.compose(c, FloorCardLines.english(), "", true);
            StringBuilder all = new StringBuilder();
            for (FloorCardLines.Line l : lines) all.append(l.text()).append('\n');
            String text = all.toString();
            if (!text.contains("FLOOR " + (floor > 0 ? "+" + floor : Integer.toString(floor)))) return fail("the words do not name the floor: " + text);
            if (!text.contains(c.themeName()) || !text.contains(c.themeDesc())) return fail("the words do not carry the theme's name and description");
            for (FloorCard.Mech m : c.floorMechs()) if (!text.contains(m.name()) || !text.contains("Counter:")) return fail("the words do not carry the mechanic " + m.id() + " and its counter");
            if (!text.contains("DEPTH")) return fail("the words have no depth line");

            // ---- no spam
            FloorCard before = FloorInfo.last(p.getUUID());
            if (FloorInfo.show(p, floor, 0, FloorInfoRules.Reason.ARRIVE)) return fail("a second arrival on the same floor within the cooldown was shown again");
            MegaDungeon.fireFloorEntered(p, floor, 0);
            if (FloorInfo.last(p.getUUID()) != before) return fail("the floor event repeated the report for the same floor");
            if (!FloorInfo.show(p, floor + (floor > 0 ? 1 : -1), floor, FloorInfoRules.Reason.FORWARD)) return fail("a different floor did not get its report");
            FloorCard next = FloorInfo.last(p.getUUID());
            if (next.reasonKind() != FloorInfoRules.Reason.FORWARD || next.depth() != Math.abs(floor) + 1) return fail("the next floor's report is wrong: " + next.reasonKind() + ", depth " + next.depth());
            if (!FloorInfo.show(p, floor, 0, FloorInfoRules.Reason.REQUEST)) return fail("a request (the key, /dungeon floor) was refused");
            if (FloorInfo.show(p, floor, 0, FloorInfoRules.Reason.REQUEST)) return fail("two requests in the same tick both got a report");

            // ---- the wire
            FloorInfoNet.Card sent = new FloorInfoNet.Card(c);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            FloorInfoNet.Card.CODEC.encode(buf, sent);
            FloorInfoNet.Card back = FloorInfoNet.Card.CODEC.decode(buf);
            if (!sent.equals(back)) return fail("the card changed in a round trip through the real codec");
            FloorCard wordy = new FloorCard(0, 7, 0, true, 7, false, "x".repeat(300), "n".repeat(300), "d".repeat(900), "holy_only", 100, 5, 3, 1, 105, true, 2,
                    List.of(new FloorCard.Mech("m".repeat(300), "n".repeat(300), "d".repeat(900), "kill_all", "h".repeat(600), "floor", "armed")), List.of());
            FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
            FloorInfoNet.Card.CODEC.encode(buf2, new FloorInfoNet.Card(wordy));
            FloorCard cut = FloorInfoNet.Card.CODEC.decode(buf2).card();
            if (cut.themeDesc().length() > FloorInfoNet.DESC || cut.floorMechs().get(0).desc().length() > FloorInfoNet.DESC) return fail("an over-long text was not cut to the wire limit (it would kick the player)");

            // ---- the fallbacks: a theme and a mechanic with no text still read
            FloorCard bare = new FloorCard(3, -4, 0, false, 4, false, "brand_new_theme", "", "", "any", 100, 0, 0, 0, 100, true, 0,
                    List.of(new FloorCard.Mech("hazard.brand_new", "", "", "kill_all", "", "floor", "waiting")), List.of());
            StringBuilder b = new StringBuilder();
            for (FloorCardLines.Line l : FloorCardLines.compose(bare, FloorCardLines.english(), "", false)) b.append(l.text()).append('\n');
            if (!b.toString().contains("Hazard Brand New")) return fail("a mechanic with no name does not read as its readable id: " + b);
            if (FloorInfoRules.genericThemeDesc("flood").isEmpty()) return fail("a theme with no description has no generic line");

            // ---- the mid-floor watcher announces a room mechanic waking and any mechanic overcome, and nothing else
            Set<String> a = Set.of("floor.x|floor|waiting", "hazard.y|room|armed");
            Set<String> b2 = Set.of("floor.x|floor|armed", "hazard.y|room|armed", "puzzle.z|room|armed", "floor.w|floor|solved");
            List<FloorInfo.Event> ev = FloorInfo.events(a, b2);
            boolean newRoom = false, done = false, noise = false;
            for (FloorInfo.Event e : ev) {
                if (e.id().equals("puzzle.z") && !e.overcome()) newRoom = true;
                else if (e.id().equals("floor.w") && e.overcome()) done = true;
                else noise = true;
            }
            if (!newRoom || !done || noise) return fail("the watcher's events are wrong: " + ev);

            return ok("floor " + floor + " of sector " + s.id + ": theme " + c.themeName() + " (" + c.align() + "), " + c.floorMechs().size() + " floor mechanic(s) as planned, " + lines.size()
                    + " lines, the next floor and a request shown, repeats suppressed, the card survives the codec and its limits");
        } finally {
            FloorInfo.forget(p.getUUID());
        }
    }
}
