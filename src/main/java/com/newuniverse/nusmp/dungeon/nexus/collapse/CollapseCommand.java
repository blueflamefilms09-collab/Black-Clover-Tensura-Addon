package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * /multiverse dungeon collapse start | stop [all] | status (permission level 2): bring down the Nexus the executor stands in (or, anywhere
 * else in the Dungeon, a pocket round them), end the nearest collapse early (nothing is restored), or look at the collapses under way.
 * Attached under the dungeon node by {@link #addTo}.
 */
public final class CollapseCommand {
    private CollapseCommand() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    /** Adds {@code collapse start | stop | stop all | status} (permission level 2) under the given dungeon node. */
    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> dungeonNode) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("collapse").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("start").executes(ctx -> start(ctx.getSource())));
        root.then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource(), false)).then(Commands.literal("all").executes(ctx -> stop(ctx.getSource(), true))));
        root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
        dungeonNode.then(root);
    }

    private static int start(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!LivingDungeon.in(p)) return fail(src, "Only inside the Dungeon.");
        CollapseManager.Result r = CollapseManager.start(p.serverLevel(), p.blockPosition(), NexusZone.at(p.getBlockX(), p.getBlockZ()), p, false);
        return r == CollapseManager.Result.STARTED ? ok(src, "A collapse begins here (it ends with the wipe in three minutes; /multiverse dungeon collapse stop cancels it).") : fail(src, r.message);
    }

    private static int stop(CommandSourceStack src, boolean all) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!LivingDungeon.in(p)) return fail(src, "Only inside the Dungeon.");
        int n = CollapseManager.stop(p.serverLevel(), p.blockPosition(), all);
        return n == 0 ? fail(src, "No collapse is under way.") : ok(src, n + " collapse(s) ended; nothing was restored.");
    }

    private static int status(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!LivingDungeon.in(p)) return fail(src, "Only inside the Dungeon.");
        ServerLevel level = p.serverLevel();
        List<String> lines = CollapseManager.status(level, p.blockPosition());
        if (lines.isEmpty()) return ok(src, "No collapse is under way.");
        for (String line : lines) ok(src, line);
        return lines.size();
    }
}
