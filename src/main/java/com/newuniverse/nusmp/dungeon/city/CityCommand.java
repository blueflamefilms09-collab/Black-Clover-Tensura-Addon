package com.newuniverse.nusmp.dungeon.city;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * /multiverse dungeon city ...: where the nearest Golden City is, build it at once (for testing), or forget that it was built. The cities
 * are in the Dungeon; the commands work from anywhere and use the executor's x and z.
 */
public final class CityCommand {
    private CityCommand() {}

    /** How many cells round the executor's own are searched (10 is 1280 blocks). */
    private static final int RINGS = 10;

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("city").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("where").executes(ctx -> where(ctx.getSource())));
        root.then(Commands.literal("build").executes(ctx -> buildNearest(ctx.getSource())));
        root.then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())));
        return root;
    }

    private static int[] nearest(CommandSourceStack src) {
        Vec3 at = src.getPosition();
        return CityPlan.nearest((int) Math.floor(at.x), (int) Math.floor(at.z), RINGS, DungeonTier::isCityCell);
    }

    private static int where(CommandSourceStack src) {
        int[] cell = nearest(src);
        if (cell == null) return fail(src, "No Golden City within " + RINGS * CityPlan.CELL + " blocks.");
        CityPlan plan = CityBuilder.plan(cell[0], cell[1]);
        Vec3 at = src.getPosition();
        double dx = plan.centreX - at.x, dz = plan.centreZ - at.z;
        long key = CitySave.key(cell[0], cell[1]);
        CitySave.Entry e = CitySave.get(src.getServer()).lookup(key);
        String state = e == null ? "not built yet" : e.state == CitySave.DONE ? "built" : "building (" + (100L * e.op / Math.max(1, plan.ops.size())) + "%)";
        int[][] gates = plan.gateFaces();
        return ok(src, "Nearest Golden City: centre (" + plan.centreX + ", " + plan.floorY + ", " + plan.centreZ + ") in the Dungeon, " + Math.round(Math.sqrt(dx * dx + dz * dz))
                + " blocks away, " + plan.semiX * 2 + " by " + plan.semiZ * 2 + " blocks, cell (" + cell[0] + ", " + cell[1] + "): " + state
                + (e == null || e.claims.isEmpty() ? "" : "; " + e.claims.size() + " player(s) have taken the altar's gift")
                + ". Sealed gates (dig in here): (" + gates[0][0] + ", " + (plan.floorY + 3) + ", " + gates[0][1] + ") and (" + gates[1][0] + ", " + (plan.floorY + 3) + ", " + gates[1][1] + ").");
    }

    private static int buildNearest(CommandSourceStack src) {
        int[] cell = nearest(src);
        if (cell == null) return fail(src, "No Golden City within " + RINGS * CityPlan.CELL + " blocks.");
        ServerLevel dungeon = src.getServer().getLevel(LivingDungeon.DIMENSION);
        if (dungeon == null) return fail(src, "The Dungeon is not loaded.");
        int ops = CityBuilder.buildNow(dungeon, cell[0], cell[1]);
        CityPlan plan = CityBuilder.plan(cell[0], cell[1]);
        return ok(src, "Built the Golden City of cell (" + cell[0] + ", " + cell[1] + ") at (" + plan.centreX + ", " + plan.floorY + ", " + plan.centreZ + "): " + ops + " ops.");
    }

    private static int reset(CommandSourceStack src) {
        int[] cell = nearest(src);
        if (cell == null) return fail(src, "No Golden City within " + RINGS * CityPlan.CELL + " blocks.");
        long key = CitySave.key(cell[0], cell[1]);
        CityBuilder.forget(key);
        boolean had = CitySave.get(src.getServer()).forget(key);
        return ok(src, had ? "The Golden City of cell (" + cell[0] + ", " + cell[1] + ") is forgotten: it will be built again when someone comes near, and its altar gives again. Its blocks stay until then."
                : "That city had nothing recorded.");
    }
}
