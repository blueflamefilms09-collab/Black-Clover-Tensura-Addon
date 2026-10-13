package com.newuniverse.nusmp.dungeon.nexus;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * /multiverse dungeon nexus ...: where the nearest Nexus is (a Variable Core cell, or since 1.22 a floor boss arena: a Goliath's Pit or a
 * Charybdis Basin), build it at once (for testing), force the zone of a cell that is not built yet, or forget that a cell was built. The
 * Nexuses are in the Dungeon; the commands work from anywhere and use the executor's x and z. {@code where}, {@code build} and {@code reset}
 * take an optional {@code core} or {@code arena} to look for that kind only ({@code core} is what they did in 1.21.0). Attached under the
 * dungeon node by {@link #addTo}.
 */
public final class NexusCommand {
    private NexusCommand() {}

    /** How many cells round the executor's own are searched (10 is 1280 blocks). */
    private static final int RINGS = 10;

    /** Which cells a command looks at. */
    private enum Kind {
        ANY("Nexus", NexusCells::isNexusCell),
        CORE("Variable Core", DungeonTier::isCityCell),
        ARENA("floor boss arena", (cx, cz) -> DungeonTier.arenaOf(cx, cz) != DungeonTier.ARENA_NONE);

        final String noun;
        final NexusCells.CellTest test;

        Kind(String noun, NexusCells.CellTest test) {
            this.noun = noun;
            this.test = test;
        }
    }

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    /** A command with an optional kind: {@code name}, {@code name core}, {@code name arena}. */
    private static LiteralArgumentBuilder<CommandSourceStack> kinded(String name, Function<Kind, com.mojang.brigadier.Command<CommandSourceStack>> run) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name).executes(run.apply(Kind.ANY));
        node.then(Commands.literal("core").executes(run.apply(Kind.CORE)));
        node.then(Commands.literal("arena").executes(run.apply(Kind.ARENA)));
        return node;
    }

    /** Adds {@code nexus where | build | reset [core|arena]} and {@code nexus zone <monarch|underworld|mirage|golden|goliath|charybdis>} (permission level 2) under the given dungeon node. */
    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> dungeonNode) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nexus").requires(s -> s.hasPermission(2));
        root.then(kinded("where", k -> ctx -> where(ctx.getSource(), k)));
        root.then(kinded("build", k -> ctx -> build(ctx.getSource(), k)));
        LiteralArgumentBuilder<CommandSourceStack> zone = Commands.literal("zone");
        for (String word : ZoneChoice.WORDS) zone.then(Commands.literal(word).executes(ctx -> force(ctx.getSource(), word)));
        root.then(zone);
        root.then(kinded("reset", k -> ctx -> reset(ctx.getSource(), k)));
        dungeonNode.then(root);
    }

    private static int[] nearest(CommandSourceStack src, NexusCells.CellTest test) {
        Vec3 at = src.getPosition();
        return NexusCells.nearest((int) Math.floor(at.x), (int) Math.floor(at.z), RINGS, test);
    }

    private static String none(Kind kind) { return "No " + kind.noun + " within " + RINGS * NexusZone.CELL + " blocks."; }

    private static int where(CommandSourceStack src, Kind kind) {
        int[] cell = nearest(src, kind.test);
        if (cell == null) return fail(src, none(kind));
        try {
            MinecraftServer server = src.getServer();
            NexusInfo info = NexusZones.at(server, cell[0], cell[1]);
            NexusSave.Entry e = NexusSave.get(server).lookup(info.key());
            Vec3 at = src.getPosition();
            double dx = info.footprint.centreX() - at.x, dz = info.footprint.centreZ() - at.z;
            String how = e == null || e.zone == null ? (e != null && e.override != null ? "forced" : "by the cell's hash") : "fixed by its build";
            String state = switch (info.state) {
                case UNBUILT -> "not built yet";
                case PLAN -> "planned, nothing placed yet";
                case BUILDING -> "building (stage " + (e == null ? 0 : e.stage) + ", op " + (e == null ? 0 : e.op) + ")";
                case DONE -> "built";
                case COLLAPSED -> "collapsed";
            };
            String seal = info.zone.isArena() ? "No seal (a floor boss arena)" : "Seal " + (info.sealBroken ? "broken" : "whole");
            return ok(src, "Nearest " + (info.zone.isArena() ? "floor boss arena" : "Variable Core") + ": cell (" + cell[0] + ", " + cell[1] + "), " + info.zone.label + " (" + how + "), " + state + "; centre ("
                    + info.footprint.centreX() + ", " + info.floorY + ", " + info.footprint.centreZ() + "), " + Math.round(Math.sqrt(dx * dx + dz * dz)) + " blocks away, "
                    + info.footprint.halfX() * 2 + " by " + info.footprint.halfZ() * 2 + " blocks. " + seal
                    + "; central relic " + (info.centreRelicTaken ? "taken" : "not taken") + ".");
        } catch (RuntimeException | LinkageError failure) {
            return fail(src, "Could not describe that " + kind.noun + ": " + failure);
        }
    }

    private static int build(CommandSourceStack src, Kind kind) {
        int[] cell = nearest(src, kind.test);
        if (cell == null) return fail(src, none(kind));
        ServerLevel dungeon = src.getServer().getLevel(LivingDungeon.DIMENSION);
        if (dungeon == null) return fail(src, "The Dungeon is not loaded.");
        try {
            int ops = NexusBuilder.buildNow(dungeon, cell[0], cell[1]);
            NexusInfo info = NexusZones.at(src.getServer(), cell[0], cell[1]);
            return ok(src, "Built " + info.zone.label + " of cell (" + cell[0] + ", " + cell[1] + ") at (" + info.footprint.centreX() + ", " + info.floorY + ", " + info.footprint.centreZ() + "): "
                    + ops + " ops (/multiverse dungeon nexus where tells more).");
        } catch (RuntimeException | LinkageError failure) {
            return fail(src, "The build failed: " + failure + " (see the log).");
        }
    }

    private static int force(CommandSourceStack src, String word) {
        NexusZone zone = ZoneChoice.parse(word);
        if (zone == null) return fail(src, "Unknown zone: " + word);
        MinecraftServer server = src.getServer();
        NexusSave save = NexusSave.get(server);
        int[] cell = nearest(src, (cx, cz) -> (zone == NexusZone.GOLIATH_PIT ? DungeonTier.arenaOf(cx, cz) == DungeonTier.ARENA_GOLIATH
                : zone == NexusZone.CHARYBDIS_BASIN ? DungeonTier.arenaOf(cx, cz) == DungeonTier.ARENA_CHARYBDIS : DungeonTier.isCityCell(cx, cz))        // a pit only in a pit's cell, a core zone only in a core cell
                && ZoneChoice.canForce(NexusZones.stateOf(server, cx, cz)));
        if (cell == null) return fail(src, "No cell of that kind that is not built yet within " + RINGS * NexusZone.CELL + " blocks. /multiverse dungeon nexus reset forgets a built one.");
        if (!save.forceZone(NexusSave.key(cell[0], cell[1]), zone)) return fail(src, "That cell was already started; reset it first.");
        return ok(src, "The Nexus of cell (" + cell[0] + ", " + cell[1] + ") will be " + zone.label + " when it is built (it comes when someone nears it, or /multiverse dungeon nexus build).");
    }

    private static int reset(CommandSourceStack src, Kind kind) {
        int[] cell = nearest(src, kind.test);
        if (cell == null) return fail(src, none(kind));
        boolean had = NexusBuilder.reset(src.getServer(), cell[0], cell[1]);
        return ok(src, had ? "The Nexus of cell (" + cell[0] + ", " + cell[1] + ") is forgotten: it will be built again when someone comes near, its seal is whole and its relics are back. Its blocks stay until then."
                : "That cell had nothing recorded.");
    }
}
