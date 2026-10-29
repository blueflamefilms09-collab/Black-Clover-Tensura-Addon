package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.rift.Waypoint;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so the CI server smoke test runs {@code dungeon.admin} in the real game and it checks the things a unit test cannot, on the
 * server's own command dispatcher after every mod has registered: the player's {@code /dungeon} subcommands are all still there and still open to a level-0 source; every admin
 * literal exists and is hidden from a level-1 source and usable by a level-2 source; the usage strings Brigadier prints for each level are EXACTLY {@link AdminSpec}'s (so a node
 * added without its spec line, a spec line without its node, or a node that lost its gate fails the run); the integer ranges hold at their edges (1000 parses, 1001 does not;
 * level 100 and 101); every command run from a source with no player (the console) returns 0 and throws nothing; every command that needs a player returns a clean
 * refusal for a stand-in standing in the Overworld; the teleport planner says ARRIVE for a floor that stands and does not for one far beyond (without growing anything); and
 * the particle view toggles on a built floor and off again. A stand-in
 * has no connection, so the teleport itself is covered by the planner and the pure rules, as in the other self-tests.
 */
public final class AdminSelfTests {
    private AdminSelfTests() {}

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    static SelfTests.Result run(MinecraftServer server) {
        CommandDispatcher<CommandSourceStack> d = server.getCommands().getDispatcher();
        CommandNode<CommandSourceStack> root = d.getRoot().getChild(AdminSpec.ROOT);
        if (root == null) return fail("no /" + AdminSpec.ROOT + " command is registered");
        CommandSourceStack console = server.createCommandSourceStack();
        CommandSourceStack op = console.withPermission(AdminSpec.PERMISSION);
        CommandSourceStack low = console.withPermission(AdminSpec.PERMISSION - 1);
        StringBuilder done = new StringBuilder();

        // ---- the player's subcommands are all still there, for everyone
        for (String lit : AdminSpec.PLAYER_LITERALS) {
            CommandNode<CommandSourceStack> n = root.getChild(lit);
            if (n == null) return fail("the player subcommand /" + AdminSpec.ROOT + " " + lit + " is gone (the merge dropped it)");
            if (!n.canUse(low)) return fail("the player subcommand /" + AdminSpec.ROOT + " " + lit + " was gated by the admin tree");
        }
        if (!root.canUse(low)) return fail("the /" + AdminSpec.ROOT + " root itself is gated; the player's subcommands would be lost");
        done.append(AdminSpec.PLAYER_LITERALS.size()).append(" player subcommands kept; ");

        // ---- the admin literals: present, hidden below level 2, usable at level 2
        for (String lit : AdminSpec.ADMIN_LITERALS) {
            CommandNode<CommandSourceStack> n = root.getChild(lit);
            if (n == null) return fail("the admin literal '" + lit + "' is missing");
            if (n.canUse(low)) return fail("the admin literal '" + lit + "' can be used below permission level " + AdminSpec.PERMISSION);
            if (!n.canUse(op)) return fail("the admin literal '" + lit + "' cannot be used at permission level " + AdminSpec.PERMISSION);
        }
        for (CommandNode<CommandSourceStack> n : root.getChildren()) {
            String name = n.getName();
            if (!AdminSpec.PLAYER_LITERALS.contains(name) && !AdminSpec.ADMIN_LITERALS.contains(name)) return fail("an unexpected child of /" + AdminSpec.ROOT + ": " + name);
        }
        done.append(AdminSpec.ADMIN_LITERALS.size()).append(" admin literals gated; ");

        // ---- the usage the dispatcher prints is the spec, for both levels
        Set<String> atLow = new HashSet<>(List.of(d.getAllUsage(root, low, true)));
        Set<String> atOp = new HashSet<>(List.of(d.getAllUsage(root, op, true)));
        for (String usage : AdminSpec.usages()) {
            if (!atOp.contains(usage)) return fail("the usage '" + usage + "' is in the spec but not in the tree at level " + AdminSpec.PERMISSION);
            if (atLow.contains(usage)) return fail("the usage '" + usage + "' is visible below level " + AdminSpec.PERMISSION);
        }
        Set<String> adminOnly = new HashSet<>(atOp);
        adminOnly.removeAll(atLow);
        adminOnly.removeAll(AdminSpec.usages());
        if (!adminOnly.isEmpty()) return fail("the tree has admin usages the spec does not list: " + adminOnly);
        for (String lit : AdminSpec.PLAYER_LITERALS) {
            boolean seen = false;
            for (String u : atLow) if (u.equals(lit) || u.startsWith(lit + " ")) seen = true;
            if (!seen) return fail("the player subcommand '" + lit + "' has no usage at level " + (AdminSpec.PERMISSION - 1));
        }
        done.append(AdminSpec.ENTRIES.size()).append(" usages match the spec; ");

        // ---- the ranges hold at their edges
        String[][] edges = {{"tp 1000", "y"}, {"tp -1000", "y"}, {"tp 1001", "n"}, {"tp -1001", "n"}, {"tp 0", "y"}, {"skip 1000", "y"}, {"skip 1001", "n"},
                {"boss spawn 100", "y"}, {"boss spawn -100", "y"}, {"boss spawn 101", "n"}, {"boss spawn -101", "n"},
                {"rift spawn pocket", "y"}, {"rift spawn shortcut", "y"}, {"rift spawn banana", "n"}, {"reset everything", "n"}};
        for (String[] e : edges) {
            boolean full = parsesFully(d, AdminSpec.ROOT + " " + e[0], op);
            if (full != e[1].equals("y")) return fail("'/" + AdminSpec.ROOT + " " + e[0] + "' " + (full ? "was accepted" : "was refused") + " at permission level " + AdminSpec.PERMISSION);
            if (parsesFully(d, AdminSpec.ROOT + " " + e[0], low)) return fail("'/" + AdminSpec.ROOT + " " + e[0] + "' parses at permission level " + (AdminSpec.PERMISSION - 1));
        }
        done.append(edges.length).append(" argument edges; ");

        // ---- no player: every command refuses cleanly
        int refused = 0;
        for (AdminSpec.Entry e : AdminSpec.ENTRIES) {
            String cmd = AdminSpec.ROOT + " " + concrete(e.usage());
            int r;
            try {
                r = d.execute(cmd, op);
            } catch (Throwable t) {
                return fail("'/" + cmd + "' from a source with no player threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            boolean help = e.usage().equals("admin help");
            if (help ? r <= 0 : r != 0) return fail("'/" + cmd + "' from a source with no player returned " + r + (help ? ", expected the help lines" : ", expected a clean refusal (0)"));
            if (!help) refused++;
        }
        done.append(refused).append(" refuse a console; ");

        // ---- a stand-in in the Overworld: the commands that need a dungeon refuse cleanly, the others do their work
        ServerLevel over = server.overworld();
        FakePlayer fp = SelfTests.standIn(over, "dungeon_admin", 0.5, 100, 0.5);
        CommandSourceStack asFp = fp.createCommandSourceStack().withPermission(AdminSpec.PERMISSION);
        String[] refusals = {"debug wfc", "debug info", "boss kill", "boss spawn 0", "skip 0", "skip -3"};
        for (String c : refusals) {
            int r;
            try {
                r = d.execute(AdminSpec.ROOT + " " + c, asFp);
            } catch (Throwable t) {
                return fail("'/" + AdminSpec.ROOT + " " + c + "' threw " + t.getClass().getSimpleName() + " for a stand-in in the Overworld: " + t.getMessage());
            }
            if (r != 0) return fail("'/" + AdminSpec.ROOT + " " + c + "' returned " + r + " for a stand-in in the Overworld (expected 0)");
        }
        if (WfcDebugService.on(fp.getUUID())) return fail("the particle view switched on outside the dungeon");
        done.append(refusals.length).append(" clean refusals outside the dungeon; ");
        try {
            if (d.execute(AdminSpec.ROOT + " reset alignment", asFp) != 1) return fail("reset alignment did not report success");
            if (d.execute(AdminSpec.ROOT + " reset waypoint", asFp) != 1) return fail("reset waypoint did not report success");
        } catch (Throwable t) {
            return fail("a reset threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        if (!fp.getPersistentData().getCompound(Waypoint.KEY).isEmpty()) return fail("reset waypoint left a waypoint behind");
        done.append("resets; ");

        // ---- the teleport planner, the debug model and the particle view on a built floor
        ServerLevel mega = SelfTests.mega(server);
        MegaData data = MegaData.get(server);
        Sector built = null;
        for (Sector s : data.sectors()) if (SectorGenerator.playable(s) && !s.rooms.isEmpty() && s.party == 0) { built = s; break; }
        BlockPos pad = built == null ? null : SectorGenerator.padOf(built);
        if (mega == null || built == null || pad == null) {
            done.append("no built floor to plan on");
            return ok(done.toString());
        }
        int floor = built.firstFloor();
        TpRules.Step here = FloorTeleport.stepFor(server, 0, floor);
        if (here != TpRules.Step.ARRIVE) return fail("the planner says " + here + " for floor " + floor + ", which stands (sector " + built.id + ")");
        int far = built.polarity.sign() * DungeonAdminMath.MAX_FLOOR;
        TpRules.Step beyond = FloorTeleport.stepFor(server, 0, far);
        if (beyond == TpRules.Step.ARRIVE || beyond == TpRules.Step.FAIL_STUCK) return fail("the planner says " + beyond + " for floor " + far + ", which cannot stand yet");
        done.append("planner: floor ").append(floor).append(" ").append(here).append(", floor ").append(far).append(" ").append(beyond).append("; ");

        WfcDebugModel.Model model = WfcDebugService.modelOf(built);
        if (model.summary.rooms() != built.rooms.size()) return fail("the debug model has " + model.summary.rooms() + " rooms for a sector of " + built.rooms.size());
        if (model.points() <= 0) return fail("the debug model of sector " + built.id + " has no points to draw");
        if (model.summary.skippedEdges() > 0) return fail("sector " + built.id + " has " + model.summary.skippedEdges() + " doorway(s) that name a room it does not have");
        WfcDebugModel.Burst burst = WfcDebugModel.next(model, new WfcDebugModel.Cursor(), pad.getX(), pad.getY(), pad.getZ(), WfcDebugModel.RANGE, WfcDebugModel.MAX_PER_BURST);
        if (burst.count() <= 0 || burst.count() > WfcDebugModel.MAX_PER_BURST) return fail("the first burst has " + burst.count() + " points near the landing (expected 1 to " + WfcDebugModel.MAX_PER_BURST + ")");
        done.append("debug model: ").append(model.summary.rooms()).append(" rooms, ").append(model.summary.edges()).append(" doorways, ").append(model.summary.deadEnds()).append(" dead ends, ")
                .append(model.points()).append(" points, first burst ").append(burst.count()).append("; ");

        FakePlayer inside = SelfTests.standIn(mega, "dungeon_admin_floor", pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5);
        CommandSourceStack asInside = inside.createCommandSourceStack().withPermission(AdminSpec.PERMISSION);
        try {
            if (d.execute(AdminSpec.ROOT + " debug info", asInside) != 1) return fail("debug info returned 0 for a stand-in on the landing of sector " + built.id);
            if (d.execute(AdminSpec.ROOT + " debug wfc", asInside) != 1 || !WfcDebugService.on(inside.getUUID())) return fail("debug wfc did not switch the particle view on for a stand-in on a built floor");
            if (d.execute(AdminSpec.ROOT + " debug wfc", asInside) != 1 || WfcDebugService.on(inside.getUUID())) return fail("a second debug wfc did not switch the particle view off");
        } catch (Throwable t) {
            return fail("the debug commands threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            WfcDebugService.forget(inside.getUUID());
        }
        done.append("debug wfc on/off; ");
        return ok(done.toString());
    }

    /** Does the command parse to the end and to a runnable node? */
    private static boolean parsesFully(CommandDispatcher<CommandSourceStack> d, String command, CommandSourceStack source) {
        ParseResults<CommandSourceStack> r = d.parse(command, source);
        return !r.getReader().canRead() && r.getContext().getCommand() != null && r.getExceptions().isEmpty();
    }

    /** A usage with every argument filled in with a valid value. */
    private static String concrete(String usage) {
        return usage.replace("<floor>", "5").replace("<floors>", "2").replace("<level>", "3").replace("<skill>", "all").replace("<amount>", "5");
    }
}
