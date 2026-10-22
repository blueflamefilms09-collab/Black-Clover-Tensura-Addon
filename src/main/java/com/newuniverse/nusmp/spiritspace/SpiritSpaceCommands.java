package com.newuniverse.nusmp.spiritspace;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a waypoint has a NAME, and a name is typed. {@code /spiritspace list | set <slot> [name] | name <slot> <name> | clear <slot> | go <slot>} (no permission needed, they only touch the caller's own
 * book) are the typed twins of the three keys of the spirit; every one goes through {@link SpiritSpace}, which applies the same checks (the spirit must be linked, the cooldowns, the names are cleaned by
 * {@link Waypoints#cleanName}). Slots are 1-based here (1..the configured number) and 0-based inside.
 */
public final class SpiritSpaceCommands {
    private SpiritSpaceCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("spiritspace").requires(s -> s.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("list").executes(SpiritSpaceCommands::list))
                .then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(1, SpiritSpaceRules.WAYPOINTS_HARD))
                        .executes(c -> set(c, null))
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> set(c, StringArgumentType.getString(c, "name"))))))
                .then(Commands.literal("name").then(Commands.argument("slot", IntegerArgumentType.integer(1, SpiritSpaceRules.WAYPOINTS_HARD))
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(SpiritSpaceCommands::rename))))
                .then(Commands.literal("clear").then(Commands.argument("slot", IntegerArgumentType.integer(1, SpiritSpaceRules.WAYPOINTS_HARD)).executes(SpiritSpaceCommands::clear)))
                .then(Commands.literal("go").then(Commands.argument("slot", IntegerArgumentType.integer(1, SpiritSpaceRules.WAYPOINTS_HARD)).executes(SpiritSpaceCommands::go))));
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> c) { return c.getSource().getEntity() instanceof ServerPlayer p ? p : null; }

    private static int slot(CommandContext<CommandSourceStack> c) { return IntegerArgumentType.getInteger(c, "slot") - 1; }

    private static boolean ready(ServerPlayer p, boolean needLink) {
        if (!SpiritSpace.allowed(p)) { p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_space.refused", "The Spirit of Space does not answer you."), true); return false; }
        if (needLink && !SpiritSpace.stateOf(p).linked) { p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_space.notlinked", "Link the spirit first."), true); return false; }
        return true;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = player(c);
        if (p == null || !ready(p, false)) return 0;
        for (String line : SpiritSpace.describe(p, SpiritSpace.stateOf(p))) c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, String name) {
        ServerPlayer p = player(c);
        return p != null && ready(p, true) && SpiritSpace.setWaypoint(p, SpiritSpace.stateOf(p), slot(c), name) ? 1 : 0;
    }

    private static int rename(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = player(c);
        return p != null && ready(p, false) && SpiritSpace.renameWaypoint(p, SpiritSpace.stateOf(p), slot(c), StringArgumentType.getString(c, "name")) ? 1 : 0;
    }

    private static int clear(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = player(c);
        return p != null && ready(p, false) && SpiritSpace.clearWaypoint(p, SpiritSpace.stateOf(p), slot(c)) ? 1 : 0;
    }

    private static int go(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = player(c);
        return p != null && ready(p, true) && SpiritSpace.beginPortal(p, SpiritSpace.stateOf(p), slot(c)) ? 1 : 0;
    }
}
