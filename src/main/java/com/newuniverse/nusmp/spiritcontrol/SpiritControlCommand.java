package com.newuniverse.nusmp.spiritcontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Why: the COMMAND route of the spirit control plane: {@code /multiverse spirit summon|release|status|combat on|off|ability <name> [slot]} (and the short {@code /spirit ...}). No permission is needed: the
 * commands only ever touch the caller's own spirit (the source must be a player, and the spirit must answer that player), they go through the same {@link SpiritControl} methods as the grimoire's pages, so they
 * have the same checks and the same per-player rate limit ({@code control.askGapTicks}). The names are validated (a spirit id of the registry, an ability of that spirit's list), the waypoint slot is 1-based
 * for the player and checked against the spirit's own limit. Nothing here is op-only, and nothing reaches another player.
 */
public final class SpiritControlCommand {
    private SpiritControlCommand() {}

    /** Adds the verbs to the existing {@code /multiverse spirit} node (one append-only line in MultiverseCommands). */
    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> spirit) { verbs(spirit); }

    /** Registers the short {@code /spirit} root with the same verbs. */
    public static void registerAlias(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spirit").requires(s -> s.getEntity() instanceof ServerPlayer);
        verbs(root);
        d.register(root);
    }

    private static void verbs(LiteralArgumentBuilder<CommandSourceStack> n) {
        n.then(Commands.literal("summon").executes(c -> summon(c, null))
                .then(Commands.argument("spirit", StringArgumentType.word()).suggests(SpiritControlCommand::spirits).executes(c -> summon(c, StringArgumentType.getString(c, "spirit")))));
        n.then(Commands.literal("release").executes(c -> release(c, null))
                .then(Commands.argument("spirit", StringArgumentType.word()).suggests(SpiritControlCommand::spirits).executes(c -> release(c, StringArgumentType.getString(c, "spirit")))));
        n.then(Commands.literal("status").executes(SpiritControlCommand::status));
        n.then(Commands.literal("combat")
                .then(Commands.literal("on").executes(c -> combat(c, true)))
                .then(Commands.literal("off").executes(c -> combat(c, false))));
        n.then(Commands.literal("ability")
                .then(Commands.argument("name", StringArgumentType.word()).suggests(SpiritControlCommand::abilities).executes(c -> ability(c, 1))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, SpiritControl.MAX_SLOT)).executes(c -> ability(c, IntegerArgumentType.getInteger(c, "slot"))))));
    }

    // ================================================================================================ suggestions
    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> spirits(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        if (c.getSource().getEntity() instanceof ServerPlayer p) for (SpiritHandle h : SpiritControl.eligible(p)) b.suggest(h.id());
        return b.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> abilities(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        if (c.getSource().getEntity() instanceof ServerPlayer p) {
            Set<String> names = new LinkedHashSet<>();
            for (SpiritHandle h : SpiritControl.eligible(p)) for (SpiritAbilityDef d : h.abilities()) names.add(d.id());
            for (String s : names) b.suggest(s);
        }
        return b.buildFuture();
    }

    // ================================================================================================ the verbs
    private static int report(CommandContext<CommandSourceStack> c, SpiritControl.Verdict v, String done) {
        switch (v) {
            case DONE -> { c.getSource().sendSuccess(() -> Component.literal(done), false); return 1; }
            case OFF -> c.getSource().sendFailure(Component.literal("The spirit control plane is switched off on this server."));
            case RATE -> c.getSource().sendFailure(Component.literal("Slow down: a spirit answers once every half second."));
            case NONE -> c.getSource().sendFailure(Component.literal("Nothing to do (no such spirit, it is not out, or it has no such ability)."));
            default -> c.getSource().sendFailure(Component.literal("The spirit did not answer."));
        }
        return 0;
    }

    private static int summon(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (id != null && !valid(id)) return unknown(c);
        SpiritHandle h = SpiritControl.find(p, id, false);
        return report(c, SpiritControl.summon(p, h), h == null ? "" : h.display() + " answers.");
    }

    private static int release(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (id != null && !valid(id)) return unknown(c);
        SpiritHandle h = SpiritControl.find(p, id, true);
        return report(c, SpiritControl.release(p, h), h == null ? "" : h.display() + " returns.");
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (String line : SpiritControl.status(p)) c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int combat(CommandContext<CommandSourceStack> c, boolean on) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        return report(c, SpiritControl.combat(p, on), "Spirit combat mode " + (on ? "on." : "off."));
    }

    private static int ability(CommandContext<CommandSourceStack> c, int slot1) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        SpiritHandle pick = null;
        for (SpiritHandle h : SpiritControl.eligible(p)) {
            if (SpiritControl.abilityOf(h, name) == null) continue;
            if (pick == null || h.isOut(p)) pick = h;
            if (h.isOut(p)) break;
        }
        if (pick == null) return unknown(c);
        SpiritAbilityDef d = SpiritControl.abilityOf(pick, name);
        int slot = Math.max(0, Math.min(pick.slots() - 1, slot1 - 1));
        if (d.takesSlot()) SpiritControlPages.rememberSlot(p, slot);
        return report(c, SpiritControl.ability(p, pick, d.id(), d.takesSlot() ? slot : 0), pick.display() + ": " + d.label() + ".");
    }

    private static boolean valid(String id) { return SpiritControl.REG.get(id) != null; }

    private static int unknown(CommandContext<CommandSourceStack> c) {
        c.getSource().sendFailure(Component.literal("No such spirit or ability for you. Try /multiverse spirit status."));
        return 0;
    }
}
