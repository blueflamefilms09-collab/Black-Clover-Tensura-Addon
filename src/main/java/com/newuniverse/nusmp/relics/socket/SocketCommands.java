package com.newuniverse.nusmp.relics.socket;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;

/**
 * The command nodes of the sockets, for the lead to attach under {@code /multiverse relic}:
 * <pre>
 *   SocketCommands.node()          /multiverse relic socket  list | cast [n] | add &lt;key&gt; | remove [n] | core &lt;player&gt; &lt;key&gt;
 *   SocketCommands.unsocketNode()  /multiverse relic unsocket [n]
 * </pre>
 * {@code list} and {@code cast} are for everyone; {@code add}, {@code remove}, {@code core} and {@code unsocket} are for operators (permission 2). An
 * operator's {@code add} puts the Core in without using one up and without the bond check; {@code remove} and {@code unsocket} return the Core as an item.
 * Players socket with the sneak-use flow instead (see {@link SocketEngine}).
 */
public final class SocketCommands {
    private SocketCommands() {}

    private static int ok(CommandSourceStack s, Component msg) { s.sendSuccess(() -> msg, false); return 1; }

    private static int fail(CommandSourceStack s, String key, Object... args) { s.sendFailure(Component.translatable(key, args)); return 0; }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("socket")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("cast")
                        .executes(ctx -> cast(ctx.getSource(), ctx.getSource().getPlayerOrException(), 1))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> cast(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "n")))))
                .then(Commands.literal("add").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("key", StringArgumentType.word()).suggests(SocketCommands::suggestKeys)
                                .executes(ctx -> add(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "key")))))
                .then(Commands.literal("remove").requires(s -> s.hasPermission(2))
                        .executes(ctx -> remove(ctx.getSource(), ctx.getSource().getPlayerOrException(), 0))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, SocketData.MAX))
                                .executes(ctx -> remove(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "n")))))
                .then(Commands.literal("core").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("key", StringArgumentType.word()).suggests(SocketCommands::suggestKeys)
                                        .executes(ctx -> core(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "key"))))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> unsocketNode() {
        return Commands.literal("unsocket").requires(s -> s.hasPermission(2))
                .executes(ctx -> remove(ctx.getSource(), ctx.getSource().getPlayerOrException(), 0))
                .then(Commands.argument("n", IntegerArgumentType.integer(1, SocketData.MAX))
                        .executes(ctx -> remove(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "n"))));
    }

    private static CompletableFuture<Suggestions> suggestKeys(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(SocketCatalog.all().stream().map(SocketDef::id), b);
    }

    private static int list(CommandSourceStack src, ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        RelicDef d = Relics.defOf(held);
        if (d == null) return fail(src, "socket.nusmp.cmd.hold_relic");
        SocketData cur = SocketComponents.get(held);
        int slots = SocketService.slotsOf(held);
        StringBuilder b = new StringBuilder();
        for (SocketData.Entry e : cur.entries()) {
            SocketDef sd = SocketCatalog.byId(e.id());
            b.append("  ").append(sd == null ? e.id() : sd.name()).append(sd == null ? "" : " (" + sd.act().name() + ")").append('\n');
        }
        return ok(src, Component.translatable("socket.nusmp.cmd.list", d.cardName(), cur.size(), slots).append(Component.literal("\n" + b.toString().stripTrailing())));
    }

    private static int cast(CommandSourceStack src, ServerPlayer p, int n) {
        return SocketEngine.castWorn(p, n) ? 1 : 0;
    }

    private static int add(CommandSourceStack src, ServerPlayer p, String key) {
        ItemStack held = p.getMainHandItem();
        if (Relics.defOf(held) == null) return fail(src, "socket.nusmp.cmd.hold_relic");
        SocketDef d = SocketCatalog.byKey(key);
        if (d == null) return fail(src, "socket.nusmp.cmd.unknown", key);
        return SocketService.socket(p, held, d, true) == SocketRules.Outcome.OK ? 1 : 0;      // the player was told why if not
    }

    private static int remove(CommandSourceStack src, ServerPlayer p, int n) {
        ItemStack held = p.getMainHandItem();
        if (Relics.defOf(held) == null) return fail(src, "socket.nusmp.cmd.hold_relic");
        SocketData cur = SocketComponents.get(held);
        if (cur.isEmpty()) return fail(src, "socket.nusmp.msg.empty_socket");
        ItemStack back = SocketService.unsocket(p, held, n <= 0 ? cur.size() - 1 : n - 1);
        SocketService.give(p, back);
        return back.isEmpty() ? 0 : 1;
    }

    private static int core(CommandSourceStack src, ServerPlayer to, String key) {
        SocketDef d = SocketCatalog.byKey(key);
        if (d == null) return fail(src, "socket.nusmp.cmd.unknown", key);
        SocketService.give(to, SocketItems.stack(d.id()));
        return ok(src, Component.translatable("socket.nusmp.cmd.gave", Component.translatable(d.nameKey()), to.getName()));
    }
}
