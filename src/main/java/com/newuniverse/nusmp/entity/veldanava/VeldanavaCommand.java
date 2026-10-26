package com.newuniverse.nusmp.entity.veldanava;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /multiverse boss veldanava} (permission 2) spawns the Creator God at the executor with the arena there; {@code /multiverse veldanava boon <0..3>} and {@code /multiverse veldanava skill <id>} (any player) are
 * what the clickable lines of the Will of the Creator run. The second one is validated on the server: the sender must be a player, the orb must be waiting for them, the
 * choice must be 0 (loot only, for a player who already holds a boon) or 1..3, and the boon is given once in the player's life ({@link VeldanavaBoons#grant}).
 */
public final class VeldanavaCommand {
    private VeldanavaCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> bossNode) {
        bossNode.then(Commands.literal("veldanava").requires(s -> s.hasPermission(2)).executes(VeldanavaCommand::summon));
    }

    /** The player-level node, added under {@code /multiverse}. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("veldanava")
                .then(Commands.literal("boon").then(Commands.argument("choice", IntegerArgumentType.integer(0, VeldanavaBoons.COUNT)).executes(VeldanavaCommand::boon)))
                .then(Commands.literal("skill").then(Commands.argument("id", StringArgumentType.word()).executes(VeldanavaCommand::skill)));      // 3.13.0: the card the player clicked in chat
    }

    private static int summon(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        if (!VeldanavaConfig.enabled()) return fail(ctx, "Veldanava is turned off on this server (nusmp-veldanava-server.toml, enabled).");
        if (VeldanavaBoss.exists(level)) return fail(ctx, "Veldanava is already here: only one Creator God per world.");
        VeldanavaBoss boss = VeldanavaBoss.spawnAt(level, VeldanavaBoss.ground(level, ctx.getSource().getPosition()), 0f);
        if (boss == null) return fail(ctx, "Veldanava could not be summoned here.");
        ctx.getSource().sendSuccess(() -> Component.literal("Veldanava, the Creator God, descends in the shape of the Star King Dragon. The arena is here."), true);
        return 1;
    }

    private static int boon(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return fail(ctx, "Only a player can take a boon.");
        int choice = IntegerArgumentType.getInteger(ctx, "choice");
        WillOfTheCreator orb = WillOfTheCreator.waitingFor(p);
        if (orb == null) return fail(ctx, "The Will of the Creator is not waiting for you.");
        String answer = orb.claim(p, choice);
        boolean ok = orb.hasClaimed(p.getUUID());
        if (ok) ctx.getSource().sendSuccess(() -> Component.literal(answer), false);
        else ctx.getSource().sendFailure(Component.literal(answer));
        return ok ? 1 : 0;
    }

    private static int skill(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return fail(ctx, "Only a player can take a skill.");
        String id = StringArgumentType.getString(ctx, "id");
        return com.newuniverse.nusmp.entity.veldanava.skills.VeldanavaSkillNet.onClaim(p, id.length() > com.newuniverse.nusmp.entity.veldanava.skills.VeldanavaSkillNet.ID_MAX ? "" : id) ? 1 : 0;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
