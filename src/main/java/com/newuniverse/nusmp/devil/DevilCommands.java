package com.newuniverse.nusmp.devil;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Locale;

/**
 * Why: the commands of the Devil Contract system (3.48.0), a root command of their own so no shared command class changes. For everybody: {@code /devil status} (your own contract, in words). For operators
 * (permission 2): {@code status <player>}, {@code grant <player> <tier> [attribute]} (a contract with no price), {@code level <player> <0-100>}, {@code sacrifice <player> add|remove <name>},
 * {@code cooldown <player> reset} and {@code clear <player>} (forget the contract). Sacrifices can only be removed here, never by milk, a totem, a heal or a Tensura reset. Nothing here touches anything outside the system.
 */
public final class DevilCommands {
    private DevilCommands() {}

    public static void register(IEventBus bus) { bus.addListener(DevilCommands::onRegister); }

    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> TIERS = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DevilTier.values()).filter(t -> t != DevilTier.NONE).map(DevilTier::key), b);
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> ATTRS = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DevilAttribute.values()).filter(a -> a != DevilAttribute.NONE).map(DevilAttribute::key), b);
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> SACS = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Sacrifice.values()).map(s -> s.name().toLowerCase(Locale.ROOT)), b);

    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> WORLDS = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DevilWorld.values()).map(DevilWorld::key), b);
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> DEVILS = (c, b) -> {
        for (int i = 0; i < WorldDevil.count(); i++) b.suggest(WorldDevil.at(i).id());
        return b.buildFuture();
    };

    private static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("devil")
                .then(Commands.literal("status").executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("worlds").executes(c -> worlds(c)))
                .then(Commands.literal("world").then(Commands.argument("world", StringArgumentType.word()).suggests(WORLDS).executes(c -> world(c, StringArgumentType.getString(c, "world")))))
                .then(Commands.literal("call").then(Commands.argument("devil", StringArgumentType.word()).suggests(DEVILS).executes(c -> call(c, StringArgumentType.getString(c, "devil")))))
                .then(Commands.literal("sign").executes(c -> DevilRituals.sign(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("refuse").executes(c -> DevilRituals.refuse(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("grant").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("tier", StringArgumentType.word()).suggests(TIERS)
                                .executes(c -> grant(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "tier"), null))
                                .then(Commands.argument("attribute", StringArgumentType.word()).suggests(ATTRS)
                                        .executes(c -> grant(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "tier"), StringArgumentType.getString(c, "attribute")))))))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 100))
                                .executes(c -> level(c, EntityArgument.getPlayer(c, "player"), (float) DoubleArgumentType.getDouble(c, "percent"))))))
                .then(Commands.literal("sacrifice").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word()).suggests(SACS)
                                        .executes(c -> sacrifice(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "name"), true))))
                                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).suggests(SACS)
                                        .executes(c -> sacrifice(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "name"), false))))))
                .then(Commands.literal("cooldown").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.literal("reset").executes(c -> cooldownReset(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> clear(c, EntityArgument.getPlayer(c, "player"))))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        DevilContract d = DevilStore.peek(p);
        if (d == null || !d.hasContract()) return say(c, p.getName().getString() + " has no devil contract.");
        long now = DevilContracts.time(p);
        StringBuilder sb = new StringBuilder(p.getName().getString()).append(": ").append(d.devil.isEmpty() || WorldDevil.byId(d.devil) == null ? "" : WorldDevil.byId(d.devil).name() + " (" + WorldDevil.byId(d.devil).world().label + "), ").append(d.tier.key()).append(" devil, ").append(d.attribute.key())
                .append(", assimilation ").append(Math.round(d.assimilation)).append("%").append(d.assimilating ? " (rising)" : "");
        if (d.inUnion()) sb.append(", Devil Union ").append(DevilRules.secondsLeft(d.unionUntil, now)).append(" s left");
        else if (DevilRules.cooling(d.unionReadyAt, now)) sb.append(", union locked for ").append(DevilRules.secondsLeft(d.unionReadyAt, now)).append(" s");
        else sb.append(", union ready");
        if (!d.sacrifices.isEmpty()) sb.append(", sacrifices ").append(d.sacrifices.toString().toLowerCase(Locale.ROOT));
        if (!DevilConfig.enabled()) sb.append(" [system is off on this server]");
        return say(c, sb.toString());
    }

    private static int grant(CommandContext<CommandSourceStack> c, ServerPlayer p, String tier, String attribute) {
        DevilTier t = DevilTier.byName(tier.toUpperCase(Locale.ROOT));
        if (t == DevilTier.NONE) return fail(c, "Unknown tier '" + tier + "' (low, mid, high, supreme).");
        DevilAttribute a = attribute == null ? DevilAttribute.WORD : DevilAttribute.byName(attribute.toUpperCase(Locale.ROOT));
        if (attribute != null && a == DevilAttribute.NONE) return fail(c, "Unknown attribute '" + attribute + "'.");
        DevilContract d = DevilStore.of(p);
        d.tier = t;
        d.attribute = a;
        d.resetLive();
        DevilContracts.applyStats(p, d);
        DevilContracts.applySacrifices(p, d);
        DevilContracts.syncNow(p, d);
        return say(c, p.getName().getString() + " now holds a " + t.key() + " contract (" + a.key() + ").");
    }

    private static int level(CommandContext<CommandSourceStack> c, ServerPlayer p, float percent) {
        DevilContract d = DevilStore.peek(p);
        if (d == null || !d.hasContract()) return fail(c, p.getName().getString() + " has no devil contract.");
        d.assimilation = DevilRules.clampF(percent, 0f, 100f);
        d.assimilating = false;
        if (d.assimilation < 100f && d.inUnion()) d.unionUntil = 0;
        DevilContracts.applyStats(p, d);
        DevilContracts.syncNow(p, d);
        return say(c, p.getName().getString() + " is at " + Math.round(d.assimilation) + "%.");
    }

    private static int sacrifice(CommandContext<CommandSourceStack> c, ServerPlayer p, String name, boolean add) {
        Sacrifice s = Sacrifice.byName(name.toUpperCase(Locale.ROOT));
        if (s == null) return fail(c, "Unknown sacrifice '" + name + "'.");
        DevilContract d = DevilStore.of(p);
        if (add) d.sacrifices.add(s); else d.sacrifices.remove(s);
        DevilContracts.applySacrifices(p, d);
        DevilContracts.syncNow(p, d);
        return say(c, p.getName().getString() + (add ? " now carries " : " no longer carries ") + s.name().toLowerCase(Locale.ROOT) + ".");
    }

    // ================================================================ 3.76.0: the devils of other worlds
    private static int worlds(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (!DevilRituals.on()) return fail(c, DevilRitualRules.Denial.DISABLED.text);
        p.sendSystemMessage(Component.literal("Devils answer from eight worlds. Each is called in its own way:").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        for (DevilWorld w : DevilWorld.values()) {
            p.sendSystemMessage(Component.literal(" [" + w.label + "] ").withStyle(s -> s.withColor(w.color | 0x404040)
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/devil world " + w.key()))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(w.need + "\nPrice: " + w.price))))
                    .append(Component.literal(w.ritual + ", " + WorldDevil.countOf(w) + (WorldDevil.countOf(w) == 1 ? " devil" : " devils")).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int world(CommandContext<CommandSourceStack> c, String name) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        DevilWorld w = DevilWorld.byName(name);
        if (w == null) return fail(c, "No such world. Try /devil worlds.");
        p.sendSystemMessage(Component.literal(w.label + ": " + w.ritual).withColor(w.color | 0x404040));
        p.sendSystemMessage(Component.literal("  It asks: " + w.need).withStyle(net.minecraft.ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal("  Price: " + w.price).withStyle(net.minecraft.ChatFormatting.GRAY));
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            if (d.world() != w) continue;
            DevilRules.Effect fx = DevilRules.effect(d.attribute());
            p.sendSystemMessage(Component.literal("  [Call " + d.name() + "] ").withStyle(s -> s.withColor(d.color() | 0x404040)
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/devil call " + d.id()))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(fx.blurb()))))
                    .append(Component.literal(d.title() + ": " + d.tier().key() + ", " + d.attribute().key().replace('_', ' ')).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int call(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        WorldDevil d = WorldDevil.byId(id);
        if (d == null) return fail(c, "No devil answers to that name. Try /devil worlds.");
        return DevilRituals.begin(p, d) == DevilRitualRules.Denial.NONE ? 1 : 0;
    }

    private static int cooldownReset(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        DevilContract d = DevilStore.peek(p);
        if (d == null || !d.hasContract()) return fail(c, p.getName().getString() + " has no devil contract.");
        d.unionReadyAt = 0;
        DevilContracts.syncNow(p, d);
        return say(c, "The union of " + p.getName().getString() + " is ready again.");
    }

    private static int clear(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        DevilContract d = DevilStore.peek(p);
        if (d == null) return say(c, p.getName().getString() + " had no devil contract.");
        d.tier = DevilTier.NONE;
        d.attribute = DevilAttribute.NONE;
        d.devil = "";
        d.offer = "";
        d.ritual = null;
        d.sacrifices.clear();
        d.resetLive();
        d.unionReadyAt = 0;
        DevilContracts.applyStats(p, d);
        DevilContracts.applySacrifices(p, d);
        DevilContracts.syncNow(p, d);
        return say(c, "The devil contract of " + p.getName().getString() + " was cleared.");
    }
}
