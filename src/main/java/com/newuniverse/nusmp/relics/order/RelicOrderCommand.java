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
 *   <li>1.68.0 {@code unique ...}: the list of what is and is not one world only (see {@link UniqueRules}): {@code list}, {@code show <id>}, {@code set <id> one_world|many}, {@code reset <id>},
 *       {@code reset_all}, {@code grade <grade> one_world|many}, {@code all one_world|many}. The gamerule {@code nusmpGodOfOrder} (default true) keeps the God away altogether.</li>
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
                .then(uniqueTree())
                .then(Commands.literal("summon")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                                        .executes(RelicOrderCommand::summon))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> uniqueTree() {
        var mode = Commands.argument("mode", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("one_world", "many"), b));
        return Commands.literal("unique")
                .then(Commands.literal("list").executes(RelicOrderCommand::uniqueList))
                .then(Commands.literal("show").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b)).executes(RelicOrderCommand::uniqueShow)))
                .then(Commands.literal("set").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                        .then(mode.executes(RelicOrderCommand::uniqueSet))))
                .then(Commands.literal("reset").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b)).executes(RelicOrderCommand::uniqueReset)))
                .then(Commands.literal("reset_all").executes(RelicOrderCommand::uniqueResetAll))
                .then(Commands.literal("grade").then(Commands.argument("grade", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(com.newuniverse.nusmp.relics.Grade.values()).map(g -> g.name().toLowerCase(java.util.Locale.ROOT)).toList(), b))
                        .then(Commands.argument("mode", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("one_world", "many"), b)).executes(RelicOrderCommand::uniqueGrade))))
                .then(Commands.literal("all").then(Commands.argument("mode", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("one_world", "many"), b)).executes(RelicOrderCommand::uniqueAll)));
    }

    /** "one_world" -> true, "many" -> false, anything else null. */
    static Boolean parseMode(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(java.util.Locale.ROOT)) {
            case "one_world", "one", "unique" -> Boolean.TRUE;
            case "many", "any", "free" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static int uniqueList(CommandContext<CommandSourceStack> ctx) {
        UniqueRules r = UniqueRules.get(ctx.getSource().getServer());
        int[] c = UniqueRules.counts();
        ctx.getSource().sendSuccess(() -> Component.literal("One world only: " + c[0] + " relics. May exist many times: " + c[1] + ". Changed by an operator: " + r.all().size() + "."), false);
        int shown = 0;
        for (Map.Entry<String, Boolean> e : r.all().entrySet()) {
            if (shown++ >= 40) {
                int more = r.all().size() - 40;
                ctx.getSource().sendSuccess(() -> Component.literal("  ... and " + more + " more"), false);
                break;
            }
            String line = "  " + e.getKey() + ": " + (e.getValue() ? "one world only" : "many");
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int uniqueShow(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        RelicDef d = Relics.byId(id);
        if (d == null) { ctx.getSource().sendFailure(Component.literal("There is no relic '" + id + "'.")); return 0; }
        Boolean o = UniqueRules.get(ctx.getSource().getServer()).override(id);
        ctx.getSource().sendSuccess(() -> Component.literal(OrderText.titleCase(id) + ": " + (d.isUnique() ? "one world only" : "may exist many times")
                + (o == null ? " (the default)" : " (set by an operator; the default is " + (d.uniqueByDefault() ? "one world only" : "many") + ")")), false);
        return 1;
    }

    private static int uniqueSet(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        Boolean mode = parseMode(StringArgumentType.getString(ctx, "mode"));
        RelicDef d = Relics.byId(id);
        if (d == null) { ctx.getSource().sendFailure(Component.literal("There is no relic '" + id + "'.")); return 0; }
        if (mode == null) { ctx.getSource().sendFailure(Component.literal("Say one_world or many.")); return 0; }
        UniqueRules.get(ctx.getSource().getServer()).set(d.isPairLeft() ? d.pairBaseId() : id, mode);
        ctx.getSource().sendSuccess(() -> Component.literal(OrderText.titleCase(id) + " is now " + (mode ? "one world only (a copy that already exists becomes the valid one; extra copies are taken once the Order enforces)" : "allowed to exist many times") + "."), true);
        UniqueLedger.scan(ctx.getSource().getServer());
        return 1;
    }

    private static int uniqueReset(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        boolean had = UniqueRules.get(ctx.getSource().getServer()).reset(id);
        ctx.getSource().sendSuccess(() -> Component.literal(had ? OrderText.titleCase(id) + " is back to its default." : OrderText.titleCase(id) + " had no setting."), true);
        return had ? 1 : 0;
    }

    private static int uniqueResetAll(CommandContext<CommandSourceStack> ctx) {
        int n = UniqueRules.get(ctx.getSource().getServer()).resetAll();
        ctx.getSource().sendSuccess(() -> Component.literal(n + " settings cleared: every relic has its default again."), true);
        return n;
    }

    private static int uniqueGrade(CommandContext<CommandSourceStack> ctx) {
        String g = StringArgumentType.getString(ctx, "grade");
        Boolean mode = parseMode(StringArgumentType.getString(ctx, "mode"));
        com.newuniverse.nusmp.relics.Grade grade = null;
        for (com.newuniverse.nusmp.relics.Grade x : com.newuniverse.nusmp.relics.Grade.values()) if (x.name().equalsIgnoreCase(g)) grade = x;
        if (grade == null) { ctx.getSource().sendFailure(Component.literal("There is no grade '" + g + "'.")); return 0; }
        if (mode == null) { ctx.getSource().sendFailure(Component.literal("Say one_world or many.")); return 0; }
        UniqueRules r = UniqueRules.get(ctx.getSource().getServer());
        int n = 0;
        for (RelicDef d : Relics.all()) {
            if (d.grade() != grade || d.isPairLeft() || d.slot() == com.newuniverse.nusmp.relics.RelicSlot.CONSUMABLE) continue;
            r.set(d.id(), mode);
            n++;
        }
        int count = n;
        String gradeLabel = grade.label();
        ctx.getSource().sendSuccess(() -> Component.literal(count + " " + gradeLabel + " relics are now " + (mode ? "one world only" : "allowed to exist many times") + "."), true);
        UniqueLedger.scan(ctx.getSource().getServer());
        return count;
    }

    private static int uniqueAll(CommandContext<CommandSourceStack> ctx) {
        Boolean mode = parseMode(StringArgumentType.getString(ctx, "mode"));
        if (mode == null) { ctx.getSource().sendFailure(Component.literal("Say one_world or many.")); return 0; }
        UniqueRules r = UniqueRules.get(ctx.getSource().getServer());
        int n = 0;
        for (RelicDef d : Relics.all()) {
            if (d.isPairLeft() || d.slot() == com.newuniverse.nusmp.relics.RelicSlot.CONSUMABLE) continue;
            r.set(d.id(), mode);
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.literal(count + " relics are now " + (mode ? "one world only" : "allowed to exist many times") + "."), true);
        UniqueLedger.scan(ctx.getSource().getServer());
        return count;
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
        if (!com.newuniverse.nusmp.NUGameRules.godOfOrder(ctx.getSource().getServer().overworld())) {
            ctx.getSource().sendFailure(Component.literal("The God of Order is switched off by the gamerule nusmpGodOfOrder."));
            return 0;
        }
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
