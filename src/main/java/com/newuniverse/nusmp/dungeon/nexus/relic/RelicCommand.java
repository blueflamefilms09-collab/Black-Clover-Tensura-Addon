package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;

/**
 * /multiverse dungeon relic ...: {@code give <name> [player]} and {@code attune [player]} for admins (permission 2: the first hands over an
 * unattuned relic, the second binds the relic in the main hand at once, still within the cap of three); {@code release [all]} and
 * {@code status} for everyone (let go of the relic in the main hand, or of the oldest attunement, or of all; see what is attuned, what is worn
 * in the two relic slots, and how long a Wild Surge still drains the mana pool).
 * The node is attached by the dungeon command: {@code RelicCommand.addTo(root)}.
 */
public final class RelicCommand {
    private RelicCommand() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> dungeonNode) {
        dungeonNode.then(Commands.literal("relic")
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word()).suggests(RelicCommand::suggest)
                                .executes(ctx -> give(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("attune").requires(s -> s.hasPermission(2))
                        .executes(ctx -> attune(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> attune(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("release")
                        .executes(ctx -> release(ctx.getSource(), ctx.getSource().getPlayerOrException(), false))
                        .then(Commands.literal("all").executes(ctx -> release(ctx.getSource(), ctx.getSource().getPlayerOrException(), true))))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))));
    }

    private static CompletableFuture<Suggestions> suggest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(Relic.ids(), b);
    }

    private static int give(CommandSourceStack src, ServerPlayer to, String name) {
        Relic r = Relic.byName(name);
        if (r == null) return fail(src, "Unknown relic \"" + name + "\". The relics: " + String.join(", ", Relic.ids()) + ".");
        ItemStack stack = r.stack();
        if (!to.getInventory().add(stack)) to.drop(stack, false);
        return ok(src, "Gave " + r.label + " (unattuned) to " + to.getName().getString() + ".");
    }

    private static int attune(CommandSourceStack src, ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        Relic r = Relic.of(held);
        if (r == null) return fail(src, p.getName().getString() + " is not holding a relic in the main hand.");
        return switch (Attunement.attune(p, held)) {
            case OK -> ok(src, r.label + " is now attuned to " + p.getName().getString() + " (" + Attunement.count(p) + " of " + Bonds.CAP + ").");
            case ALREADY -> ok(src, r.label + " was already attuned to " + p.getName().getString() + ".");
            case FULL -> fail(src, p.getName().getString() + " already holds " + Bonds.CAP + " attunements: release one first.");
        };
    }

    private static int release(CommandSourceStack src, ServerPlayer p, boolean all) {
        if (all) return ok(src, "Let go of " + Attunement.releaseAll(p) + " attunement(s).");
        ItemStack held = p.getMainHandItem();
        if (Relic.of(held) != null && Attunement.attunedToMe(held, p)) {
            Attunement.release(p, held);
            return ok(src, "You let go of " + Relic.of(held).label + ".");
        }
        if (!Attunement.releaseOldest(p)) return fail(src, "You have no attunements.");
        return ok(src, "Let go of your oldest attunement.");
    }

    private static int status(CommandSourceStack src, ServerPlayer p) {
        int n = Attunement.count(p);
        StringBuilder b = new StringBuilder("Attuned relics: " + n + " of " + Bonds.CAP + ".");
        int worn = Worn.mask(p, true);
        for (Relic r : Relic.values()) if ((worn & r.bit()) != 0) b.append(" Worn: ").append(r.label).append('.');
        b.append(" At most ").append(RelicLimit.CAP).append(" relics work at once.");
        long now = p.level().getGameTime(), until = SurgeHandler.stored(p);
        if (SurgePenalty.active(now, until)) b.append(" The Wild Surge drains a tenth of your mana pool for ").append(SurgePenalty.minutesLeft(now, until)).append(" more min.");
        return ok(src, b.toString());
    }
}
