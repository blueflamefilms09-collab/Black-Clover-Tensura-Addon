package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * /multiverse dungeon vault ... and /multiverse dungeon gate ...: where the nearest hidden room is, build the one near you now (for
 * testing), and where the nearest Descent Gate is. The rooms are in the Dungeon; the commands work from anywhere and use the executor's x
 * and z. Attached under the dungeon node by {@link #addTo}.
 */
public final class VaultCommand {
    private VaultCommand() {}

    /** How far the "nearest gate" search looks, in blocks. */
    private static final int GATE_RADIUS = 4000;

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    /** Adds {@code vault where | build [puzzle|nutrient|miniboss|gate]} and {@code gate where} (permission level 2) under the given dungeon node. */
    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> dungeonNode) {
        LiteralArgumentBuilder<CommandSourceStack> vault = Commands.literal("vault").requires(s -> s.hasPermission(2));
        vault.then(Commands.literal("where").executes(ctx -> where(ctx.getSource())));
        LiteralArgumentBuilder<CommandSourceStack> build = Commands.literal("build").executes(ctx -> build(ctx.getSource(), null));
        for (VaultKind k : VaultKind.values()) build.then(Commands.literal(k.name().toLowerCase(Locale.ROOT)).executes(ctx -> build(ctx.getSource(), k)));
        vault.then(build);
        dungeonNode.then(vault);
        LiteralArgumentBuilder<CommandSourceStack> gate = Commands.literal("gate").requires(s -> s.hasPermission(2));
        gate.then(Commands.literal("where").executes(ctx -> gateWhere(ctx.getSource())));
        dungeonNode.then(gate);
    }

    private static ServerLevel dungeon(CommandSourceStack src) { return src.getServer().getLevel(LivingDungeon.DIMENSION); }

    private static int where(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        Vec3 at = src.getPosition();
        VaultSave.Entry best = null;
        double bestD = Double.MAX_VALUE;
        int known = 0, searching = 0;
        for (VaultSave.Entry e : VaultSave.get(server).all()) {
            if (e.state == VaultSave.SCANNING) searching++;
            if (e.site == null || e.state < VaultSave.PLANNED) continue;
            known++;
            double dx = e.site.centreX() - at.x, dz = e.site.centreZ() - at.z, d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best == null) {
            return fail(src, "No hidden room is known yet (" + searching + " cell(s) still being searched). They are found as players come within 80 blocks of them; "
                    + "/multiverse dungeon vault build makes the one near you.");
        }
        String[] state = {"searching", "none", "planned, not built yet", "being built", "built", "ready"};
        return ok(src, "Nearest hidden room: " + best.kind.label + ", " + Math.round(Math.sqrt(bestD)) + " blocks away, " + VaultDirector.describe(best) + "; " + state[Math.min(best.state, state.length - 1)]
                + ". " + known + " known in all.");
    }

    private static int build(CommandSourceStack src, VaultKind kind) {
        ServerLevel level = dungeon(src);
        if (level == null) return fail(src, "The Dungeon is not loaded.");
        Vec3 at = src.getPosition();
        try {
            VaultDirector.Built b = VaultDirector.buildNow(level, (int) Math.floor(at.x), (int) Math.floor(at.z), kind);
            return b.ok() ? ok(src, b.message()) : fail(src, b.message());
        } catch (RuntimeException | LinkageError failure) {
            return fail(src, "The build failed: " + failure + " (see the log).");
        }
    }

    private static int gateWhere(CommandSourceStack src) {
        ServerLevel level = dungeon(src);
        if (level == null) return fail(src, "The Dungeon is not loaded.");
        GateIndex idx = GateIndex.get(src.getServer());
        Vec3 at = src.getPosition();
        BlockPos from = new BlockPos((int) Math.floor(at.x), (int) Math.floor(at.y), (int) Math.floor(at.z));
        BlockPos g = GateIndex.nearest(level, from, GATE_RADIUS);
        if (g == null) {
            return fail(src, "No Descent Gate is known within " + GATE_RADIUS + " blocks (" + idx.downCount() + " known in all). They appear as players come near the shrines that hold them.");
        }
        double dx = g.getX() + 0.5 - at.x, dy = g.getY() - at.y, dz = g.getZ() + 0.5 - at.z;
        String floor = GateTravel.floorOf(g.getY()) == 0 ? "the Upper Catacombs" : "the Deep Waterways";
        return ok(src, "Nearest Descent Gate: (" + g.getX() + ", " + g.getY() + ", " + g.getZ() + ") in " + floor + ", " + Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz))
                + " blocks away. " + idx.downCount() + " gate(s) lead down, " + idx.upCount() + " lead up.");
    }
}
