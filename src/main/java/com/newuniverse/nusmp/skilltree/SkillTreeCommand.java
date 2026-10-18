package com.newuniverse.nusmp.skilltree;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteLoader;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.net.TreeNet;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Why: {@code /multiverse tree ...} is how the owner and the testers see and change a player's skill tree without playing for hours: show it, give or set Skill Points, unlock or lock a route,
 * set a node, set the class, reset, re-read the route files and read the validation report of every route file. It is a Brigadier node built here and attached to the root by
 * {@code MultiverseCommands} with one line, so the command surface of the engine lives in one class. Every sub-command except {@code show} (own tree) and {@code respec} (own tree, only when the
 * config allows it) needs permission level 2, and every one of them goes through {@link SkillTreeApi}, never around it, so a command cannot leave a player in a state the rules would
 * refuse (no sync, a skill without its node). The {@code gold} sub-command belongs to the shop and is attached by the lead from the shop's own node. Route ids are read with the game's own
 * resource-location argument (a plain word cannot contain the colon); node and class ids are plain words and are suggested from the registry.
 * <ul>
 *   <li>{@code show [player]}: Skill Points, class, routes and bought nodes;</li>
 *   <li>{@code sp <player> add|set <n>}: Skill Points (add is clamped by the config cap and can be negative);</li>
 *   <li>{@code route <player> unlock|lock <route>}: unlock (with the effects) or take a route away (its points come back);</li>
 *   <li>{@code node <player> <route> <node> <level>}: set a node level for free (the parents are not checked);</li>
 *   <li>{@code class <player> <class>}: set the active class, skipping the cooldown;</li>
 *   <li>{@code reset <player> [route]}: reset the nodes (all routes, or one) and give the points back;</li>
 *   <li>{@code respec [route]}: the same for yourself, only when the config {@code skilltree.respec} is true;</li>
 *   <li>{@code reload}: re-read the route files now (lighter than {@code /reload}) and send them to everybody;</li>
 *   <li>{@code routes}: every route file, whether it loaded, its problems and warnings.</li>
 * </ul>
 */
public final class SkillTreeCommand {
    private SkillTreeCommand() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> tree = Commands.literal("tree")
                .then(Commands.literal("show")
                        .executes(ctx -> show(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(SkillTreeCommand::admin)
                                .executes(ctx -> show(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("sp").requires(SkillTreeCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(-1_000_000, 1_000_000))
                                        .executes(ctx -> spAdd(ctx, EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0, 1_000_000))
                                        .executes(ctx -> spSet(ctx, EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("route").requires(SkillTreeCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("unlock").then(Commands.argument("route", ResourceLocationArgument.id()).suggests(SkillTreeCommand::suggestRoutes)
                                        .executes(ctx -> routeUnlock(ctx, EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "route").toString()))))
                                .then(Commands.literal("lock").then(Commands.argument("route", ResourceLocationArgument.id()).suggests(SkillTreeCommand::suggestRoutes)
                                        .executes(ctx -> routeLock(ctx, EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "route").toString()))))))
                .then(Commands.literal("node").requires(SkillTreeCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("route", ResourceLocationArgument.id()).suggests(SkillTreeCommand::suggestRoutes)
                                        .then(Commands.argument("node", StringArgumentType.word()).suggests(SkillTreeCommand::suggestNodes)
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 10))
                                                        .executes(ctx -> nodeSet(ctx, EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "route").toString(),
                                                                StringArgumentType.getString(ctx, "node"), IntegerArgumentType.getInteger(ctx, "level"))))))))
                .then(Commands.literal("class").requires(SkillTreeCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("class", StringArgumentType.word()).suggests(SkillTreeCommand::suggestClasses)
                                        .executes(ctx -> classSet(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "class"))))))
                .then(Commands.literal("reset").requires(SkillTreeCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx, EntityArgument.getPlayer(ctx, "player"), null))
                                .then(Commands.argument("route", ResourceLocationArgument.id()).suggests(SkillTreeCommand::suggestRoutes)
                                        .executes(ctx -> reset(ctx, EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "route").toString())))))
                .then(Commands.literal("respec")
                        .executes(ctx -> respecSelf(ctx, null))
                        .then(Commands.argument("route", ResourceLocationArgument.id()).suggests(SkillTreeCommand::suggestRoutes)
                                .executes(ctx -> respecSelf(ctx, ResourceLocationArgument.getId(ctx, "route").toString()))))
                .then(Commands.literal("reload").requires(SkillTreeCommand::admin).executes(SkillTreeCommand::reload))
                .then(Commands.literal("routes").requires(SkillTreeCommand::admin).executes(SkillTreeCommand::routes));
        // the shop's "gold <player> add|set <n>" node goes here once the shop builder's ShopCommands exists: tree.then(com.newuniverse.nusmp.skilltree.shop.ShopCommands.node());
        return tree;
    }

    // ================================================================ helpers
    private static int ok(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), true);
        return 1;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, Component text) {
        ctx.getSource().sendSuccess(() -> text, false);
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static String name(ServerPlayer p) { return p.getGameProfile().getName(); }

    private static CompletableFuture<Suggestions> suggestRoutes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (SkillRoute r : RouteRegistry.all()) {
            ResourceLocation id = ResourceLocation.tryParse(r.id());
            if (id != null) ids.add(id);
        }
        return SharedSuggestionProvider.suggestResource(ids, b);
    }

    private static CompletableFuture<Suggestions> suggestNodes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        List<String> ids = new ArrayList<>();
        try {
            SkillRoute r = RouteRegistry.byId(ctx.getArgument("route", ResourceLocation.class).toString());
            if (r != null) r.nodes().forEach(n -> ids.add(n.id()));
        } catch (IllegalArgumentException routeNotTypedYet) {
            // the route argument is not parsed yet: nothing to suggest
        }
        return SharedSuggestionProvider.suggest(ids, b);
    }

    private static CompletableFuture<Suggestions> suggestClasses(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        List<String> ids = new ArrayList<>();
        for (SkillRoute r : RouteRegistry.all()) ids.add(r.classId());
        return SharedSuggestionProvider.suggest(ids, b);
    }

    // ================================================================ show
    private static int show(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        IPlayerSkillTree t = SkillTreeApi.of(p);
        long cd = SkillTreeApi.classCooldownTicks(p);
        say(ctx, Component.literal("Skill Tree of " + name(p)).withStyle(ChatFormatting.GOLD));
        say(ctx, Component.literal(" " + t.sp() + " SP to spend, " + t.spSpent() + " spent; class " + (t.activeClass().isEmpty() ? "(none)" : t.activeClass())
                + (cd > 0 ? " (may change in " + (cd + 19) / 20 + " s)" : "") + "; " + t.unlockedRoutes().size() + " route(s)" + (SkillTreeApi.isEnabled() ? "" : "; the engine is switched OFF in the config")));
        for (String routeId : t.unlockedRoutes()) {
            SkillRoute r = RouteRegistry.byId(routeId);
            StringBuilder sb = new StringBuilder(" ").append(routeId);
            if (r != null) sb.append(" (").append(r.name()).append(")");
            else sb.append(" (not in the registry)");
            if (r != null && r.classId().equals(t.activeClass())) sb.append(" [active]");
            sb.append(": ");
            boolean any = false;
            for (Map.Entry<String, Integer> e : t.nodeLevels().entrySet()) {
                if (!e.getKey().startsWith(routeId + "#")) continue;
                String node = e.getKey().substring(routeId.length() + 1);
                int max = r != null && r.node(node) != null ? r.node(node).maxLevel() : 0;
                sb.append(any ? ", " : "").append(node).append(' ').append(e.getValue()).append(max > 0 ? "/" + max : "");
                any = true;
            }
            if (!any) sb.append("no node bought");
            say(ctx, Component.literal(sb.toString()));
        }
        return 1;
    }

    // ================================================================ sp / route / node / class
    private static int spAdd(CommandContext<CommandSourceStack> ctx, ServerPlayer p, int amount) {
        if (!SkillTreeApi.isEnabled()) return fail(ctx, "The Skill Tree is switched off in the config (skilltree.enabled), so no points can be added.");
        int before = SkillTreeApi.sp(p);
        SkillTreeApi.addSp(p, amount, "command");
        return ok(ctx, name(p) + " now has " + SkillTreeApi.sp(p) + " SP (" + before + " before; a gain is held at the cap of " + SkillTreeApi.maxSp() + ").");
    }

    private static int spSet(CommandContext<CommandSourceStack> ctx, ServerPlayer p, int amount) {
        SkillTreeApi.of(p).setSp(amount);
        SkillTreeApi.resync(p);
        return ok(ctx, name(p) + " now has " + amount + " SP.");
    }

    private static int routeUnlock(CommandContext<CommandSourceStack> ctx, ServerPlayer p, String routeId) {
        return switch (SkillTreeApi.unlockRoute(p, routeId, true)) {
            case OK -> ok(ctx, "Unlocked " + routeId + " for " + name(p) + ".");
            case ALREADY -> fail(ctx, name(p) + " already has " + routeId + ".");
            case UNKNOWN_ROUTE -> fail(ctx, "No such route '" + routeId + "'. Known: " + routeIds());
            case DISABLED -> fail(ctx, "The Skill Tree is switched off in the config (skilltree.enabled).");
        };
    }

    private static int routeLock(CommandContext<CommandSourceStack> ctx, ServerPlayer p, String routeId) {
        int spBefore = SkillTreeApi.sp(p);
        if (!SkillTreeApi.lockRoute(p, routeId)) return fail(ctx, name(p) + " does not have " + routeId + ".");
        return ok(ctx, "Took " + routeId + " away from " + name(p) + "; its nodes were reset and " + (SkillTreeApi.sp(p) - spBefore) + " SP came back.");
    }

    private static int nodeSet(CommandContext<CommandSourceStack> ctx, ServerPlayer p, String routeId, String nodeId, int level) {
        SkillRoute r = RouteRegistry.byId(routeId);
        if (r == null) return fail(ctx, "No such route '" + routeId + "'. Known: " + routeIds());
        if (r.node(nodeId) == null) return fail(ctx, "Route " + routeId + " has no node '" + nodeId + "'.");
        if (!SkillTreeApi.hasRoute(p, routeId)) return fail(ctx, name(p) + " has not unlocked " + routeId + " (unlock it first).");
        if (!SkillTreeApi.setNodeLevel(p, routeId, nodeId, level)) return fail(ctx, "The node could not be set.");
        return ok(ctx, name(p) + ": " + routeId + " / " + nodeId + " is now level " + SkillTreeApi.nodeLevel(p, routeId, nodeId) + " of " + r.node(nodeId).maxLevel() + " (no points were spent).");
    }

    private static int classSet(CommandContext<CommandSourceStack> ctx, ServerPlayer p, String classId) {
        return switch (SkillTreeApi.switchClass(p, classId, true)) {
            case OK -> ok(ctx, name(p) + " is now " + classId + ".");
            case SAME -> ok(ctx, name(p) + " already is " + classId + ".");
            case NOT_UNLOCKED -> fail(ctx, name(p) + " has no unlocked route with the class '" + classId + "'.");
            case COOLDOWN -> fail(ctx, "The class change is on cooldown.");
        };
    }

    // ================================================================ reset / respec
    private static int reset(CommandContext<CommandSourceStack> ctx, ServerPlayer p, String routeIdOrNull) {
        if (routeIdOrNull != null && !SkillTreeApi.hasRoute(p, routeIdOrNull)) return fail(ctx, name(p) + " does not have " + routeIdOrNull + ".");
        SkillTreeApi.RespecResult r = SkillTreeApi.respec(p, routeIdOrNull);
        return ok(ctx, "Reset " + (routeIdOrNull == null ? "every route" : routeIdOrNull) + " for " + name(p) + ": " + r.cleared() + " node(s) cleared, " + r.sp() + " SP returned.");
    }

    private static int respecSelf(CommandContext<CommandSourceStack> ctx, String routeIdOrNull) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        if (!TreeConfig.respec() && !admin(ctx.getSource())) return fail(ctx, "Resetting your own nodes is switched off on this server (config skilltree.respec).");
        if (!SkillTreeApi.isEnabled()) return fail(ctx, "The Skill Tree is switched off in the config (skilltree.enabled).");
        if (routeIdOrNull != null && !SkillTreeApi.hasRoute(p, routeIdOrNull)) return fail(ctx, "You have not unlocked " + routeIdOrNull + ".");
        SkillTreeApi.RespecResult r = SkillTreeApi.respec(p, routeIdOrNull);
        p.displayClientMessage(Component.translatable("tree.msg.respec", r.cleared(), r.sp()).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    // ================================================================ reload / routes
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        RouteIndex index = RouteLoader.reloadNow(server.getResourceManager());
        TreeNet.sendRoutes(server.getPlayerList().getPlayers().stream());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) SkillTreeApi.reconcileSkills(p);
        return ok(ctx, "Skill trees re-read: " + index.all().size() + " route(s) loaded, " + index.problemFiles() + " file(s) with problems"
                + (index.problemFiles() > 0 ? " (see /multiverse tree routes)" : "") + ", sent to " + server.getPlayerList().getPlayerCount() + " player(s).");
    }

    private static int routes(CommandContext<CommandSourceStack> ctx) {
        RouteIndex index = RouteRegistry.snapshot();
        say(ctx, Component.literal("Skill tree files: " + index.report().size() + " found, " + index.all().size() + " route(s) loaded, " + index.problemFiles() + " with problems, "
                + index.bytes() + " bytes to sync").withStyle(ChatFormatting.GOLD));
        if (index.report().isEmpty()) say(ctx, Component.literal(" No file under data/<namespace>/skill_trees/ was found (no route is installed yet)."));
        for (RouteIndex.Entry e : index.report()) {
            SkillRoute r = RouteRegistry.byId(e.id());
            say(ctx, Component.literal(" " + (e.loaded() ? "[loaded] " : "[SKIPPED] ") + e.id()
                    + (r != null ? " - " + r.name() + ", " + e.nodes() + " node(s), book " + r.book() + ", class " + r.classId() + ", full price " + r.totalCost() + " SP" : ""))
                    .withStyle(e.loaded() ? (e.problems().isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW) : ChatFormatting.RED));
            int shown = 0;
            for (String p : e.problems()) {
                if (shown++ >= 8) {
                    say(ctx, Component.literal("    (+" + (e.problems().size() - 8) + " more problem(s), see the server log)").withStyle(ChatFormatting.RED));
                    break;
                }
                say(ctx, Component.literal("    problem: " + p).withStyle(ChatFormatting.RED));
            }
            shown = 0;
            for (String w : e.warnings()) {
                if (shown++ >= 4) {
                    say(ctx, Component.literal("    (+" + (e.warnings().size() - 4) + " more warning(s))").withStyle(ChatFormatting.GRAY));
                    break;
                }
                say(ctx, Component.literal("    warning: " + w).withStyle(ChatFormatting.GRAY));
            }
        }
        return index.all().size();
    }

    private static String routeIds() {
        List<String> ids = new ArrayList<>();
        for (SkillRoute r : RouteRegistry.all()) ids.add(r.id());
        return ids.isEmpty() ? "(no route is installed)" : String.join(", ", ids);
    }
}
