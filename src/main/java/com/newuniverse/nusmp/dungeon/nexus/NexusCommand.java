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

/**
 * /multiverse dungeon nexus ...: where the nearest Variable Core is, build it at once (for testing), force the zone of a cell that is
 * not built yet, or forget that a cell was built. The cores are in the Dungeon; the commands work from anywhere and use the executor's
 * x and z. Attached under the dungeon node by {@link #addTo}.
 */
public final class NexusCommand {
    private NexusCommand() {}

    /** How many cells round the executor's own are searched (10 is 1280 blocks). */
    private static final int RINGS = 10;

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    /** Adds {@code nexus where | build | zone <monarch|underworld|mirage|golden> | reset} (permission level 2) under the given dungeon node. */
    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> dungeonNode) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nexus").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("where").executes(ctx -> where(ctx.getSource())));
        root.then(Commands.literal("build").executes(ctx -> build(ctx.getSource())));
        LiteralArgumentBuilder<CommandSourceStack> zone = Commands.literal("zone");
        for (String word : ZoneChoice.WORDS) zone.then(Commands.literal(word).executes(ctx -> force(ctx.getSource(), word)));
        root.then(zone);
        root.then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())));
        dungeonNode.then(root);
    }

    private static int[] nearest(CommandSourceStack src, NexusCells.CellTest test) {
        Vec3 at = src.getPosition();
        return NexusCells.nearest((int) Math.floor(at.x), (int) Math.floor(at.z), RINGS, test);
    }

    private static String none() { return "No Variable Core within " + RINGS * NexusZone.CELL + " blocks."; }

    private static int where(CommandSourceStack src) {
        int[] cell = nearest(src, DungeonTier::isCityCell);
        if (cell == null) return fail(src, none());
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
            return ok(src, "Nearest Variable Core: cell (" + cell[0] + ", " + cell[1] + "), " + info.zone.label + " (" + how + "), " + state + "; centre ("
                    + info.footprint.centreX() + ", " + info.floorY + ", " + info.footprint.centreZ() + "), " + Math.round(Math.sqrt(dx * dx + dz * dz)) + " blocks away, "
                    + info.footprint.halfX() * 2 + " by " + info.footprint.halfZ() * 2 + " blocks. Seal " + (info.sealBroken ? "broken" : "whole")
                    + "; central relic " + (info.centreRelicTaken ? "taken" : "not taken") + ".");
        } catch (RuntimeException | LinkageError failure) {
            return fail(src, "Could not describe that Variable Core: " + failure);
        }
    }

    private static int build(CommandSourceStack src) {
        int[] cell = nearest(src, DungeonTier::isCityCell);
        if (cell == null) return fail(src, none());
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
        int[] cell = nearest(src, (cx, cz) -> DungeonTier.isCityCell(cx, cz) && ZoneChoice.canForce(NexusZones.stateOf(server, cx, cz)));
        if (cell == null) return fail(src, "No Variable Core that is not built yet within " + RINGS * NexusZone.CELL + " blocks. /multiverse dungeon nexus reset forgets a built one.");
        if (!save.forceZone(NexusSave.key(cell[0], cell[1]), zone)) return fail(src, "That cell was already started; reset it first.");
        return ok(src, "The Variable Core of cell (" + cell[0] + ", " + cell[1] + ") will be " + zone.label + " when it is built (it comes when someone nears it, or /multiverse dungeon nexus build).");
    }

    private static int reset(CommandSourceStack src) {
        int[] cell = nearest(src, DungeonTier::isCityCell);
        if (cell == null) return fail(src, none());
        boolean had = NexusBuilder.reset(src.getServer(), cell[0], cell[1]);
        return ok(src, had ? "The Variable Core of cell (" + cell[0] + ", " + cell[1] + ") is forgotten: it will be built again when someone comes near, its seal is whole and its relics are back. Its blocks stay until then."
                : "That cell had nothing recorded.");
    }
}
