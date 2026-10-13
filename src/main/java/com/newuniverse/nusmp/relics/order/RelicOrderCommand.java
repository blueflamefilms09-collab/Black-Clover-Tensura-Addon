package com.newuniverse.nusmp.relics.order;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /multiverse relic order ...} (permission 2): the Order's ledger for operators. Attached to the multiverse root like {@code RelicCommands}:
 * {@code RelicOrderCommand.addTo(root)}; Brigadier merges its {@code relic} word with the one of the relic command.
 * <ul>
 *   <li>{@code census [hold]}: starts a ten minute census now (the Order only counts); with {@code hold} it stays a census until {@code enforce}</li>
 *   <li>{@code enforce}: ends any census now; duplicates are taken from here on</li>
 *   <li>{@code status}: the mode, the waiting list, the running visit and every relic's last known place</li>
 *   <li>{@code release <id>}: the valid copy of a relic is declared lost (it can be issued or found again; the old copy becomes a duplicate if it shows up)</li>
 *   <li>{@code summon <player> <id>}: the God of Order is sent to a player for a relic, at once, whether or not a duplicate exists</li>
 * </ul>
 */
public final class RelicOrderCommand {
    private RelicOrderCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("relic").then(Commands.literal("order").requires(s -> s.hasPermission(2))
                .then(Commands.literal("census").executes(ctx -> census(ctx, false))
                        .then(Commands.literal("hold").executes(ctx -> census(ctx, true))))
                .then(Commands.literal("enforce").executes(RelicOrderCommand::enforce))
                .then(Commands.literal("status").executes(RelicOrderCommand::status))
                .then(Commands.literal("release")
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                                .executes(RelicOrderCommand::release)))
                .then(Commands.literal("summon")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                                        .executes(RelicOrderCommand::summon))))));
    }

    private static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (RelicDef d : Relics.all()) out.add(d.id());
        return out;
    }

    private static int census(CommandContext<CommandSourceStack> ctx, boolean hold) {
        MinecraftServer server = ctx.getSource().getServer();
        UniqueLedger l = UniqueLedger.get(server);
        long now = server.overworld().getGameTime();
        l.beginCensus(now);
        RelicScanner.resetCensus();
        if (hold) l.holdCensus();
        UniqueLedger.scan(server);
        ctx.getSource().sendSuccess(() -> Component.literal(hold
                ? "The Order counts and keeps counting; duplicates are only listed. Use 'enforce' to end it."
                : "The Order counts for " + LedgerRules.CENSUS_TICKS / 1200 + " minutes; duplicates are only listed."), true);
        return 1;
    }

    private static int enforce(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        UniqueLedger l = UniqueLedger.get(server);
        l.enforceNow(server.overworld().getGameTime());
        UniqueLedger.scan(server);
        ctx.getSource().sendSuccess(() -> Component.literal("The Order keeps the ledger from now on: duplicates will be taken."), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        UniqueLedger l = UniqueLedger.get(server);
        long now = server.overworld().getGameTime();
        CommandSourceStack src = ctx.getSource();
        String mode = l.mode(now) == LedgerRules.Mode.CENSUS
                ? "CENSUS" + (l.censusHeld() ? " (held)" : l.censusPending() ? " (starts with the first player)" : " (" + LedgerRules.censusMinutesLeft(l.censusEnd(), now) + " min left)")
                : "ENFORCE";
        src.sendSuccess(() -> Component.literal("Order: " + mode + ", " + l.liveCount() + " relics alive, " + l.lostCount() + " lost, " + OrderDirector.waiting() + " waiting"
                + (OrderDirector.busy() ? ", visit: " + OrderDirector.describeActive() : ", no visit")), false);
        int shown = 0;
        for (Map.Entry<String, UniqueLedger.Entry> me : l.entries().entrySet()) {
            if (shown++ >= 24) {
                int more = l.entries().size() - 24;
                src.sendSuccess(() -> Component.literal("  ... and " + more + " more"), false);
                break;
            }
            UniqueLedger.Entry e = me.getValue();
            String line = "  " + OrderText.titleCase(e.relicId()) + ": " + (e.live() ? "alive " + e.spot().describe() : "lost");
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        UniqueLedger l = UniqueLedger.get(server);
        if (!l.lose(id)) {
            ctx.getSource().sendFailure(Component.literal("No live copy of '" + id + "' is written in the ledger."));
            return 0;
        }
        OrderDirector.forget(id);
        ctx.getSource().sendSuccess(() -> Component.literal(OrderText.titleCase(id) + " is released: the ledger no longer knows a valid copy."), true);
        return 1;
    }

    private static int summon(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String id = StringArgumentType.getString(ctx, "id");
        RelicDef def = Relics.byId(id);
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("There is no relic '" + id + "'."));
            return 0;
        }
        if (!OrderDirector.summon(ctx.getSource().getServer(), target, id)) {
            ctx.getSource().sendFailure(Component.literal("The Order is busy with another visit, or there is no ground to stand on."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("The Order is on its way to " + target.getGameProfile().getName() + " for " + OrderText.titleCase(id) + "."), true);
        return 1;
    }
}
