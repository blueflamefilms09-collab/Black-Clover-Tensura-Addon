package com.newuniverse.nusmp.elite.records;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Category;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Why: {@code /records} is how a player reads their progress without opening the screen (a summary, the records of a category, the titles held, choosing the active title) and how the owner and the
 * testers change it without playing for hours (grant or revoke a record or a title, erase a player's data, read the validation report of the data files). The player commands need no permission; every
 * grant, revoke and reset needs permission level 2. Ids are suggested from the loaded catalog. Every change goes through {@link RecordsService}, never around it, so a command leaves the player in a
 * state the rules would also reach (the toast, the synchronised screen and the title bonuses all follow).
 */
public final class RecordsCommand {
    private RecordsCommand() {}

    static void register() { NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> register(e.getDispatcher())); }

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("records")
                .executes(c -> summary(c, c.getSource().getPlayerOrException()))
                .then(Commands.literal("list")
                        .then(Commands.argument("category", StringArgumentType.word()).suggests(RecordsCommand::suggestCategories)
                                .executes(c -> list(c, StringArgumentType.getString(c, "category")))))
                .then(Commands.literal("titles").executes(RecordsCommand::titles))
                .then(Commands.literal("title")
                        .then(Commands.literal("clear").executes(c -> setActive(c, "")))
                        .then(Commands.argument("title", StringArgumentType.word()).suggests(RecordsCommand::suggestTitles)
                                .executes(c -> setActive(c, StringArgumentType.getString(c, "title")))))
                .then(Commands.literal("problems").requires(RecordsCommand::admin).executes(RecordsCommand::problems))
                .then(Commands.literal("check").requires(RecordsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> check(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("grant").requires(RecordsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("record", StringArgumentType.word()).suggests(RecordsCommand::suggestRecords)
                                        .executes(c -> grant(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "record"))))))
                .then(Commands.literal("revoke").requires(RecordsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("record", StringArgumentType.word()).suggests(RecordsCommand::suggestRecords)
                                        .executes(c -> revoke(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "record"))))))
                .then(Commands.literal("grant_title").requires(RecordsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("title", StringArgumentType.word()).suggests(RecordsCommand::suggestTitles)
                                        .executes(c -> grantTitle(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "title"))))))
                .then(Commands.literal("revoke_title").requires(RecordsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("title", StringArgumentType.word()).suggests(RecordsCommand::suggestTitles)
                                        .executes(c -> revokeTitle(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "title"))))))
                .then(Commands.literal("reset").requires(RecordsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> reset(c, EntityArgument.getPlayer(c, "player"))))));
    }

    private static CompletableFuture<Suggestions> suggestRecords(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        List<String> ids = new ArrayList<>();
        for (RecordDef r : RecordsCatalog.get().records()) ids.add(r.id());
        return SharedSuggestionProvider.suggest(ids, b);
    }

    private static CompletableFuture<Suggestions> suggestTitles(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        List<String> ids = new ArrayList<>();
        for (TitleDef t : RecordsCatalog.get().titles()) ids.add(t.id());
        return SharedSuggestionProvider.suggest(ids, b);
    }

    private static CompletableFuture<Suggestions> suggestCategories(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        List<String> ids = new ArrayList<>();
        for (Category k : Category.values()) ids.add(k.id());
        return SharedSuggestionProvider.suggest(ids, b);
    }

    private static boolean offline(CommandContext<CommandSourceStack> c) {
        if (RecordsConfig.enabled()) return false;
        c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_OFF));
        return true;
    }

    private static MutableComponent colored(String name, int color) { return Component.literal(name).withStyle(Style.EMPTY.withColor(color)); }

    private static int summary(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (offline(c)) return 0;
        Catalog cat = RecordsCatalog.get();
        RecordData d = RecordsAttachments.of(p);
        int sum = 0;
        for (String id : d.unlockedIds()) sum += cat.pointsOf(id);
        int points = sum;
        TitleDef active = cat.title(d.active());
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_SUMMARY, d.unlockedCount(), cat.records().size(), points, d.heldCount(),
                active == null ? RecordsText.tr(RecordsText.NO_ACTIVE_TITLE) : colored(active.name(), active.rarity().color)), false);
        return d.unlockedCount();
    }

    private static int list(CommandContext<CommandSourceStack> c, String category) throws CommandSyntaxException {
        if (offline(c)) return 0;
        Category cat = Category.parse(category, null);
        if (cat == null) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_UNKNOWN, category));
            return 0;
        }
        RecordData d = RecordsAttachments.of(c.getSource().getPlayerOrException());
        int shown = 0, done = 0;
        for (RecordDef r : RecordsCatalog.get().records()) {
            if (r.category() != cat) continue;
            boolean has = d.isUnlocked(r.id());
            if (has) done++;
            if (r.hidden() && !has) continue;
            shown++;
            MutableComponent line = Component.literal((has ? "[x] " : "[ ] ")).withStyle(has ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY).append(colored(r.name(), r.rarity().color));
            c.getSource().sendSuccess(() -> line, false);
        }
        return done;
    }

    private static int titles(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        if (offline(c)) return 0;
        RecordData d = RecordsAttachments.of(c.getSource().getPlayerOrException());
        Catalog cat = RecordsCatalog.get();
        for (String id : d.heldTitles()) {
            TitleDef t = cat.title(id);
            if (t == null) continue;
            MutableComponent line = Component.literal(id.equals(d.active()) ? "> " : "  ").append(colored(t.name(), t.rarity().color)).append(Component.literal("  (" + id + ")").withStyle(ChatFormatting.DARK_GRAY));
            c.getSource().sendSuccess(() -> line, false);
        }
        return d.heldCount();
    }

    private static int setActive(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        if (offline(c)) return 0;
        ServerPlayer p = c.getSource().getPlayerOrException();
        RecordsService.onSetActive(p, id);
        return 1;
    }

    private static int problems(CommandContext<CommandSourceStack> c) {
        List<String> ps = RecordsCatalog.problems();
        Catalog cat = RecordsCatalog.get();
        if (ps.isEmpty()) {
            c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_PROBLEMS_NONE, cat.records().size(), cat.titles().size(), cat.synergies().size()), false);
            return 0;
        }
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_PROBLEMS, ps.size()), false);
        for (int i = 0; i < Math.min(15, ps.size()); i++) {
            String line = ps.get(i);
            c.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.RED), false);
        }
        return ps.size();
    }

    private static int check(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (offline(c)) return 0;
        var r = RecordsService.beat(p, true);
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": " + r.newRecords().size() + " new record(s), " + r.newTitles().size() + " new title(s)"), false);
        return r.newRecords().size() + r.newTitles().size();
    }

    private static int grant(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        if (offline(c)) return 0;
        RecordDef def = RecordsCatalog.get().record(id);
        if (def == null) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_UNKNOWN, id));
            return 0;
        }
        if (!RecordsService.grantRecord(p, id)) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_NOTHING));
            return 0;
        }
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_GRANTED, p.getDisplayName(), colored(def.name(), def.rarity().color)), true);
        return 1;
    }

    private static int revoke(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        if (offline(c)) return 0;
        RecordDef def = RecordsCatalog.get().record(id);
        if (def == null) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_UNKNOWN, id));
            return 0;
        }
        if (!RecordsService.revokeRecord(p, id)) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_NOTHING));
            return 0;
        }
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_REVOKED, p.getDisplayName(), colored(def.name(), def.rarity().color)), true);
        return 1;
    }

    private static int grantTitle(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        if (offline(c)) return 0;
        TitleDef def = RecordsCatalog.get().title(id);
        if (def == null) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_UNKNOWN, id));
            return 0;
        }
        if (!RecordsService.grantTitle(p, id)) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_NOTHING));
            return 0;
        }
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_TITLE_GRANTED, p.getDisplayName(), colored(def.name(), def.rarity().color)), true);
        return 1;
    }

    private static int revokeTitle(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        if (offline(c)) return 0;
        TitleDef def = RecordsCatalog.get().title(id);
        if (def == null) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_UNKNOWN, id));
            return 0;
        }
        if (!RecordsService.revokeTitle(p, id)) {
            c.getSource().sendFailure(RecordsText.tr(RecordsText.CMD_NOTHING));
            return 0;
        }
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_TITLE_REVOKED, p.getDisplayName(), colored(def.name(), def.rarity().color)), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        RecordsService.reset(p);
        c.getSource().sendSuccess(() -> RecordsText.tr(RecordsText.CMD_RESET, p.getDisplayName()), true);
        return 1;
    }
}
