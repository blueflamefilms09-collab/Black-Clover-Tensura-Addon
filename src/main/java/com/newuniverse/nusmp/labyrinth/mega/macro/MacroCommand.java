package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Why: the Hub's terrain cannot be watched by the author, so the admin gets two read-only tools to check it on a real server: {@code /multiverse mega macro info}
 * shows the seed the generator derived for this world, whether the landmarks are on, and what the Wave Function Collapse did (restarts, fallback, collapses,
 * backtracks, time), and {@code sample <x> <y> <z>} says what the pure model expects at a position next to what the world really has there (it never loads or
 * generates a chunk), which is the fastest way to find a block that is not where the plan says. The tree is attached by the lead under {@code /multiverse mega};
 * the permission is 2. Messages are literals, like the other admin commands.
 */
public final class MacroCommand {
    private MacroCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("macro").requires(s -> s.hasPermission(2))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("sample").then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer(HubModel.MIN_Y, HubModel.MAX_Y))
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(ctx -> sample(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z")))))));
    }

    private static int say(CommandSourceStack s, String msg) {
        s.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int fail(CommandSourceStack s, String msg) {
        s.sendFailure(Component.literal(msg));
        return 0;
    }

    private static HubModel model(ServerLevel level) {
        return HubModel.of(SanctuaryGenerator.hubSeed(level.getChunkSource().randomState()), MacroConfig.params());
    }

    private static int info(CommandSourceStack src) {
        ServerLevel level = src.getServer().getLevel(Labyrinth.SANCTUARY_DIMENSION);
        if (level == null) return fail(src, "This build has no nusmp:sanctuary dimension.");
        MacroParams p = MacroConfig.params();
        HubModel m = model(level);
        say(src, "Sanctuary generator: hub seed " + m.seed() + ", landmarks " + (p.landmarks() ? "on" : "OFF") + ", cell " + p.cell() + ", spire noise " + p.spireNoise()
                + ", chasm noise " + p.chasmNoise() + ", hills " + p.hillAmplitude() + ", wfc tries " + p.wfcTries());
        if (m.spire() == null) return say(src, "Landmarks are off: plain plains, the two gates stand on small platforms at their coordinates.");
        long[] s = m.wfcStats();
        say(src, "Model built in " + m.buildNanos() / 1_000_000 + " ms. Wave Function Collapse: " + s[0] + " restarts, " + s[1] + " fallbacks, " + s[2] + " collapses, " + s[3] + " backtracks, solver time " + s[4] / 1_000_000 + " ms.");
        long[] sp = m.spire().wfcStats(), ch = m.chasm().wfcStats();
        say(src, "Spire: " + m.spire().galleryCount() + " galleries of trial chambers (" + sp[2] + " collapses, " + sp[1] + " fallbacks). Chasm: " + m.chasm().sectors() + " x " + m.chasm().levels() + " x " + ChasmModel.RINGS
                + " cells of cavern (" + ch[2] + " collapses, " + ch[1] + " fallbacks).");
        return 1;
    }

    private static int sample(CommandSourceStack src, int x, int y, int z) {
        ServerLevel level = src.getServer().getLevel(Labyrinth.SANCTUARY_DIMENSION);
        if (level == null) return fail(src, "This build has no nusmp:sanctuary dimension.");
        HubModel m = model(level);
        BlockKind k = m.kindAt(x, y, z);
        BlockPos pos = new BlockPos(x, y, z);
        String world = level.hasChunkAt(pos) ? level.getBlockState(pos).getBlock().getDescriptionId() : "(chunk not loaded)";
        return say(src, "At " + x + " " + y + " " + z + ": the model says " + k + " (" + KindBlocks.of(k).block() + "), the world has " + world + "; plains height here " + m.surfaceHeight(x, z) + ", top of the column " + m.topY(x, z));
    }
}
