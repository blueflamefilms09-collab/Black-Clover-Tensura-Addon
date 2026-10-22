package com.newuniverse.nusmp.relics;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.core.discovery.DiscoveryKeys;
import com.newuniverse.nusmp.core.discovery.DiscoveryRules;
import com.newuniverse.nusmp.core.discovery.DiscoveryTracker;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;

/**
 * /multiverse relic ...: {@code list [grade]}, {@code info <id>}, {@code grade} (what you wear and what it lifts) and {@code unattune} for everyone;
 * {@code give <player> <id>} and {@code attune} (binds the relic in the main hand at once, within the cap of three) for admins (permission 2).
 * The node is attached by the multiverse command: {@code RelicCommands.addTo(root)}. The Order's own commands hang below the same "relic" word.
 */
public final class RelicCommands {
    private RelicCommands() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("relic")
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource(), null))
                        .then(Commands.argument("grade", StringArgumentType.word()).suggests(RelicCommands::suggestGrades)
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "grade")))))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word()).suggests(RelicCommands::suggestIds)
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word()).suggests(RelicCommands::suggestIds)
                                        .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("attune").requires(s -> s.hasPermission(2))
                        .executes(ctx -> attune(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("unattune")
                        .executes(ctx -> unattune(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("grade")
                        .executes(ctx -> grade(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("power").requires(s -> s.hasPermission(2))                  // 1.104.0: needs attunement?, attuned to whom, the damage factor in force
                        .executes(ctx -> power(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> power(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("debug").requires(s -> s.hasPermission(2))                  // 1.99.0: why a worn relic is not drawn (server half here, the operator's client adds its half)
                        .executes(ctx -> debug(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> debug(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("show")                                                         // relics_wear: the player's own switch for all worn relics (a looks setting kept in the client config)
                        .executes(ctx -> show(ctx.getSource(), RelicShowPayload.STATUS))
                        .then(Commands.literal("on").executes(ctx -> show(ctx.getSource(), RelicShowPayload.ON)))
                        .then(Commands.literal("off").executes(ctx -> show(ctx.getSource(), RelicShowPayload.OFF)))
                        .then(Commands.literal("toggle").executes(ctx -> show(ctx.getSource(), RelicShowPayload.TOGGLE))))
                .then(com.newuniverse.nusmp.relics.socket.SocketCommands.node())                  // 1.30.0: socket list|cast|add|remove|core
                .then(com.newuniverse.nusmp.relics.socket.SocketCommands.unsocketNode()));
    }

    /** 1.27.0 (discovery): an operator or the console sees every relic; a player only the ones they have found (see core.discovery). */
    private static boolean seen(CommandSourceStack src, RelicDef d) {
        return src.hasPermission(2) || DiscoveryTracker.canSee(src, DiscoveryKeys.item(RelicCatalog.newStack(d)));
    }

    private static CompletableFuture<Suggestions> suggestIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(Relics.all().stream().filter(d -> seen(ctx.getSource(), d)).map(RelicDef::id), b);
    }

    private static CompletableFuture<Suggestions> suggestGrades(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(java.util.Arrays.stream(Grade.values()).map(g -> g.shortName().toLowerCase(java.util.Locale.ROOT)), b);
    }

    private static int list(CommandSourceStack src, String gradeName) {
        if (Relics.all().isEmpty()) return fail(src, "No relics are registered.");
        if (gradeName == null) {
            StringBuilder b = new StringBuilder("Relics of Many Worlds (" + Relics.all().size() + "):");
            for (Grade g : Grade.values()) {
                long n = Relics.all().stream().filter(d -> d.grade() == g).count();
                if (n > 0) b.append("  ").append(g.shortName()).append(' ').append(n);
            }
            return ok(src, b.append("  - /multiverse relic list <grade> names them.").toString());
        }
        Grade g = Grade.byName(gradeName);
        if (g == null) return fail(src, "Unknown grade \"" + gradeName + "\" (normal, special, rare, unique, legend, god, genesis, ex-singularity).");
        StringBuilder b = new StringBuilder(g.label() + ":");
        int n = 0;
        for (RelicDef d : Relics.all()) {
            if (d.grade() != g) continue;
            if (!seen(src, d)) b.append("\n  ").append(DiscoveryRules.QUESTION);                 // not found yet: no id, name or slot
            else b.append("\n  ").append(d.id()).append(" - ").append(d.cardName()).append(" (").append(d.slot().label()).append(')');
            n++;
        }
        return n == 0 ? fail(src, "No " + g.label() + " relics.") : ok(src, b.toString());
    }

    private static int info(CommandSourceStack src, String id) {
        RelicDef d = Relics.byId(id);
        if (d == null || !seen(src, d)) return fail(src, "Unknown relic \"" + id + "\".");       // an undiscovered relic answers exactly like one that does not exist
        StringBuilder b = new StringBuilder(d.cardName() + " (" + d.id() + ")\n  " + d.grade().label() + ", " + d.slot().label());
        if (d.needsAttunement()) b.append(", requires attunement");
        if (d.isVessel()) b.append(", can be a Djinn's Metal Vessel");
        if (d.isAdopted()) b.append(", a remake of an item the mod already had");
        if (!d.desc().isEmpty()) b.append("\n  ").append(d.desc());
        for (Act a : d.acts()) {
            b.append("\n  Act ").append(a.displayName());
            if (a.cooldownTicks() > 0) b.append(", cooldown ").append(UseRules.span(a.cooldownTicks()));
            if (a.manaPercent() > 0) b.append(", mana ").append(a.manaPercent()).append('%');
            if (a.epPercentCost() > 0) b.append(", EP ").append(a.epPercentCost()).append('%');
            if (a.usesPerDay() > 0) b.append(", ").append(a.usesPerDay()).append(" a day");
        }
        if (!d.engravings().isEmpty()) b.append("\n  Engravings: ").append(String.join(", ", d.engravings()));
        return ok(src, b.toString());
    }

    private static int give(CommandSourceStack src, ServerPlayer to, String id) {
        RelicDef d = Relics.byId(id);
        if (d == null) return fail(src, "Unknown relic \"" + id + "\".");
        if (d.isPairHalf()) {                                                               // 1.29.0: a pair is given whole (or the half it lacks)
            java.util.List<ItemStack> given = com.newuniverse.nusmp.relics.pair.PairGrant.issueMissing(to.serverLevel(), d);
            if (given.isEmpty())
                return fail(src, d.cardName() + " already exists in this world as a pair: only one pair may live. (Release it with /multiverse relic order release " + d.pairBaseId() + ".)");
            StringBuilder names = new StringBuilder();
            for (ItemStack s : given) {
                RelicDef gd = Relics.defOf(s);
                if (names.length() > 0) names.append(" and ");
                names.append(gd == null ? "a half" : gd.cardName());
                if (!to.getInventory().add(s)) to.drop(s, false);
            }
            return ok(src, "Gave " + names + " to " + to.getName().getString() + ".");
        }
        ItemStack stack = RelicCatalog.newStack(d);
        if (stack.isEmpty()) return fail(src, d.cardName() + " has no item.");
        if (d.isUnique() && !UniqueLedger.issue(to.serverLevel(), d.id(), stack))
            return fail(src, d.cardName() + " already exists in this world: only one of each relic may live. (Release it with /multiverse relic order release " + d.id() + ".)");
        if (!to.getInventory().add(stack)) to.drop(stack, false);
        return ok(src, "Gave " + d.cardName() + " to " + to.getName().getString() + ".");
    }

    private static int attune(CommandSourceStack src, ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        RelicDef d = Relics.defOf(held);
        if (d == null) return fail(src, "Hold a relic in the main hand.");
        if (Attune.isMine(held, p)) return ok(src, d.cardName() + " was already attuned to you.");
        if (!Attune.bind(p, held)) return fail(src, "You already have " + AttuneRules.CAP + " relics attuned: let one go first.");
        return ok(src, d.cardName() + " is now attuned to you (" + Attune.carried(p, null) + " of " + AttuneRules.CAP + ").");
    }

    /** relics_wear: {@code /multiverse relic show [on|off|toggle]}, for the player who runs it: the server only tells that player's client what to do with its own looks setting. */
    private static int show(CommandSourceStack src, int mode) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return fail(src, "Only a player has worn-relic visuals to switch.");
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new RelicShowPayload(mode));
        return 1;
    }

    /** 1.99.0: {@code /multiverse relic debug [player]} (operators): the server's worn-relic report, then the operator's client is asked for its half ({@link WornDebugPayload}). */
    private static int debug(CommandSourceStack src, ServerPlayer target) {
        WornDebug.Report rep = WornDebug.build(target);
        for (Component line : rep.lines()) src.sendSuccess(() -> line, false);
        if (src.getEntity() instanceof ServerPlayer op) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(op, new WornDebugPayload(target.getUUID(), rep.compact()));
        return rep.compact().size() + 1;
    }

    /** 1.104.0: {@code /multiverse relic power [player]} (operators): per carried relic, whether it needs attunement, who it is attuned to and the damage factor in force. */
    private static int power(CommandSourceStack src, ServerPlayer target) {
        java.util.List<Component> lines = RelicPowerEvents.report(target);
        for (Component line : lines) src.sendSuccess(() -> line, false);
        return lines.size();
    }

    private static int unattune(CommandSourceStack src, ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        RelicDef d = Relics.defOf(held);
        if (d == null) return fail(src, "Hold a relic in the main hand.");
        if (!Attune.isMine(held, p) || Attune.owner(held) == null) return fail(src, d.cardName() + " is not attuned to you.");
        Attune.release(p, held);
        return ok(src, "You let go of " + d.cardName() + ".");
    }

    private static int grade(CommandSourceStack src, ServerPlayer p) {
        Relics.Loadout lo = Relics.loadoutFresh(p);
        StringBuilder b = new StringBuilder("Attuned relics: " + Attune.carried(p, null) + " of " + AttuneRules.CAP + ".");
        if (Relics.wornList(p).isEmpty()) b.append("\n  You wear or hold no relic.");
        for (Relics.Worn w : Relics.wornList(p)) {
            boolean works = lo.active().stream().anyMatch(a -> a.def() == w.def());
            b.append("\n  ").append(w.def().cardName()).append(" - ").append(w.def().grade().label()).append(works ? "" : "  (not working: not attuned to you)");
        }
        if (lo.epBonus() > 0) b.append("\n  Existence points lifted by ").append(Math.round(lo.epBonus() * 1000) / 10.0).append("%.");
        return ok(src, b.toString());
    }
}
