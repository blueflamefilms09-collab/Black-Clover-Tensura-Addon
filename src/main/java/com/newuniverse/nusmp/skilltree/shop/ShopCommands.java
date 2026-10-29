package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: gold has no other source in this version besides the witch buying goods, and the owner and the testers need to hand it out, take it back, see the shelf and force a new
 * restock without playing for hours; a server owner needs to see why the shop is empty. Two nodes hang under {@code /multiverse skillbook} (the lead adds one line for each):
 * {@link #node()} is {@code gold <player> [add|set <amount>]} and {@link #shopNode()} is {@code shop status|stock|restock}. Both require permission level 2, like every
 * administrative command of the skill tree, because they create value out of nothing. Every change goes through {@link MarketGold} (so {@link GoldLedger}'s rules apply: never
 * negative, never above the cap) and the player's client is re-synced, so the number on an open screen is not a lie.
 */
public final class ShopCommands {
    private ShopCommands() {}

    /** {@code gold <player>} shows, {@code gold <player> add <n>} adds (negative removes), {@code gold <player> set <n>} sets. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("gold").requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ShopCommands::show)
                        .then(Commands.literal("add").then(Commands.argument("amount", LongArgumentType.longArg(-GoldLedger.CAP, GoldLedger.CAP)).executes(ShopCommands::add)))
                        .then(Commands.literal("set").then(Commands.argument("amount", LongArgumentType.longArg(0, GoldLedger.CAP)).executes(ShopCommands::set))));
    }

    /** {@code shop status} (data problems and the restock clock), {@code shop stock} (the shelf now), {@code shop restock} (a new shelf at once). */
    public static LiteralArgumentBuilder<CommandSourceStack> shopNode() {
        return Commands.literal("shop").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(ShopCommands::status))
                .then(Commands.literal("stock").executes(ShopCommands::stock))
                .then(Commands.literal("restock").executes(ShopCommands::restock));
    }

    // ================================================================ gold
    private static int show(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        say(ctx, p.getGameProfile().getName() + " has " + GoldFormat.full(MarketGold.get(p)) + " gold.");
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        long amount = LongArgumentType.getLong(ctx, "amount");
        long now = MarketGold.add(p, amount);
        ShopService.syncTree(p);
        ShopService.reply(p, ShopService.Result.of(ShopRules.Reply.NONE));
        say(ctx, (amount >= 0 ? "Gave " : "Took ") + GoldFormat.full(Math.abs(amount == Long.MIN_VALUE ? Long.MAX_VALUE : amount)) + " gold " + (amount >= 0 ? "to " : "from ")
                + p.getGameProfile().getName() + ", who now has " + GoldFormat.full(now) + ".");
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        long now = MarketGold.set(p, LongArgumentType.getLong(ctx, "amount"));
        ShopService.syncTree(p);
        ShopService.reply(p, ShopService.Result.of(ShopRules.Reply.NONE));
        say(ctx, p.getGameProfile().getName() + " now has " + GoldFormat.full(now) + " gold.");
        return 1;
    }

    // ================================================================ the shop
    private static int status(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        MarketState state = MarketState.get(server);
        long now = server.overworld().getGameTime();
        say(ctx, "Black Market: " + (ShopConfig.enabled() ? "open" : "closed (config)") + ", restock every " + ShopConfig.restockDays() + " day(s), next in "
                + GoldFormat.clock(StockBoard.ticksUntilRestock(now, ShopConfig.restockDays())) + ", period " + StockBoard.period(now, ShopConfig.restockDays()) + ", restocks forced " + state.epoch() + ".");
        say(ctx, "Data: " + MarketData.stock().size() + " shelf line(s), the witch buys " + MarketData.prices().size() + " item(s) in " + MarketData.prices().categories().size()
                + " categories (bulk: " + String.join(", ", MarketData.prices().bulkCategories()) + ").");
        List<String> problems = MarketData.problems();
        if (problems.isEmpty()) say(ctx, "No problems in stock.json or prices.json.");
        else for (String p : problems) say(ctx, "  problem: " + p);
        if (!MarketData.unknownItems().isEmpty()) say(ctx, "Priced items this game does not have (dropped): " + String.join(", ", MarketData.unknownItems()));
        return 1;
    }

    private static int stock(CommandContext<CommandSourceStack> ctx) {
        List<StockBoard.Offer> shelf = ShopService.shelf(ctx.getSource().getServer());
        if (shelf.isEmpty()) {
            say(ctx, "The shelf is empty (the Skill Books were removed, so the witch has nothing to sell; she still buys goods).");
            return 0;
        }
        double scale = ShopConfig.priceScale();
        for (StockBoard.Offer o : shelf) {
            say(ctx, o.id() + "  " + o.kind().id() + " " + o.ref() + "  " + GoldFormat.full(ShopRules.buyPrice(o.price(), scale)) + " gold  " + o.left() + "/" + o.total() + " left"
                    + (o.flagship() ? "  (flagship)" : ""));
        }
        return shelf.size();
    }

    private static int restock(CommandContext<CommandSourceStack> ctx) {
        ShopService.restock(ctx.getSource().getServer());
        say(ctx, "The witch has turned her shelves over: new prices, new stock, nothing bought.");
        return 1;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
