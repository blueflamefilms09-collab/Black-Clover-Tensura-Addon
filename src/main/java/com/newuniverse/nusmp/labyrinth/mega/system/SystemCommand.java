package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the owner's hands on the System, attached by the lead under {@code /multiverse mega} (permission 2): {@code status [player]} says everything about a
 * player's System in a few lines (the first thing to ask when a quest "did not count"), {@code quest roll|complete|fail|clear [player]} drives the lifecycle by
 * hand (the only way to test a reward without clearing five rooms; {@code quest start <id>} starts one named template), {@code stat give <player> <stat> <n>}
 * adds or takes points (and re-applies the attribute modifiers at once), {@code core give [player]} hands over a Shadow Core, and {@code minions clear
 * [player]} releases every shadow. Every reply is a translatable component with an English fallback from {@link SystemText}, so it reads fine before the lang
 * fragment is merged. {@code status} also says whether the stat bonus is active where the player stands (1.58.0: only in the dungeon dimensions) and how many
 * players share their quest credit. No command changes anything outside the System's own data and the target player's inventory.
 */
public final class SystemCommand {
    private SystemCommand() {}

    @FunctionalInterface
    private interface PlayerAction {
        int run(CommandSourceStack src, ServerPlayer target) throws CommandSyntaxException;
    }

    /** The {@code system} node: {@code status | quest | stat | core | minions}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("system").requires(s -> s.hasPermission(2))
                .then(withPlayer("status", SystemCommand::status))
                .then(Commands.literal("quest")
                        .then(withPlayer("roll", (src, p) -> roll(src, p, null)))
                        .then(withPlayer("complete", SystemCommand::complete))
                        .then(withPlayer("fail", SystemCommand::failQuest))
                        .then(withPlayer("clear", SystemCommand::clear))
                        .then(Commands.literal("start").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(templateIds(), builder))
                                .executes(ctx -> start(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> start(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "id")))))))
                .then(Commands.literal("stat").then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(statIds(), builder))
                                .then(Commands.argument("n", IntegerArgumentType.integer(-1000, 1000))
                                        .executes(ctx -> giveStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "stat"),
                                                IntegerArgumentType.getInteger(ctx, "n"))))))))
                .then(Commands.literal("core").then(withPlayer("give", SystemCommand::giveCore)))
                .then(Commands.literal("minions").then(withPlayer("clear", SystemCommand::clearMinions)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withPlayer(String name, PlayerAction action) {
        return Commands.literal(name)
                .executes(ctx -> action.run(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> action.run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));
    }

    private static List<String> templateIds() {
        List<String> ids = new ArrayList<>();
        for (QuestTemplate t : SystemData.quests().all()) ids.add(t.id());
        return ids;
    }

    private static List<String> statIds() {
        List<String> ids = new ArrayList<>();
        for (Stat s : Stat.values()) ids.add(s.id());
        return ids;
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int error(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static Component who(ServerPlayer p) { return p.getDisplayName(); }

    // ================================================================ status
    private static int status(CommandSourceStack src, ServerPlayer p) {
        SystemState st = SystemQuests.state(p);
        ServerLevel level = p.serverLevel();
        int floor = MegaDungeon.in(level) ? MegaDungeon.floorAt(level, p.blockPosition()) : 0;
        Polarity pol = Polarity.ofFloor(floor);
        ok(src, SystemMsg.text(SystemText.CMD_STATUS_HEAD, who(p), SystemMsg.text(SystemConfig.enabled() ? SystemText.CMD_ON : SystemText.CMD_OFF)));
        ok(src, SystemMsg.text(SystemText.CMD_STATUS_FLOOR, floor == 0 ? "0" : FloorMap.label(floor), pol == null ? "-" : pol.id(), floor == 0 ? 0 : FloorMap.threat(floor), st.maxFloor()));
        QuestProgress q = st.quest();
        if (q == null) ok(src, SystemMsg.text(SystemText.CMD_STATUS_NOQUEST));
        else ok(src, SystemMsg.text(SystemText.CMD_STATUS_QUEST, q.id(), q.kind().id(), q.state().name().toLowerCase(Locale.ROOT), q.progress(), q.target(), FloorMap.label(q.floor())));
        ok(src, SystemMsg.text(SystemText.CMD_STATUS_STATS, st.points(Stat.STR), st.points(Stat.AGI), st.points(Stat.VIT), st.points(Stat.INT), st.points(Stat.SENSE),
                SystemConfig.get(SystemConfig.MAX_STAT_POINTS)));
        ok(src, SystemMsg.text(SystemText.CMD_STATUS_BUFF, SystemMsg.text(SystemStats.wanted(p) ? SystemText.CMD_ON : SystemText.CMD_DORMANT), SystemStats.dimensionId(p),
                SystemMsg.text(SystemConfig.buffOnlyInDungeon() ? SystemText.CMD_ON : SystemText.CMD_OFF)));
        ok(src, SystemMsg.text(SystemText.CMD_STATUS_PARTY, SystemParty.members(p).size(), SystemMsg.text(SystemConfig.get(SystemConfig.SHARE_PROGRESS) ? SystemText.CMD_ON : SystemText.CMD_OFF),
                SystemConfig.get(SystemConfig.SHARE_RADIUS)));
        ok(src, SystemMsg.text(SystemText.CMD_STATUS_COUNTERS, st.done(), st.failed(), st.cores(), ShadowMinions.countOf(p.getUUID()), SystemConfig.get(SystemConfig.MAX_MINIONS),
                ShadowExtraction.pending(p) == null ? 0 : 1));
        return 1;
    }

    // ================================================================ quests
    private static int roll(CommandSourceStack src, ServerPlayer p, QuestTemplate template) {
        QuestProgress q = SystemQuests.roll(p, template);
        if (q == null) return error(src, SystemMsg.text(SystemText.CMD_QUEST_NONE));
        return ok(src, SystemMsg.text(SystemText.CMD_QUEST_ROLLED, who(p), q.id(), q.kind().id(), FloorMap.label(q.floor())));
    }

    private static int start(CommandSourceStack src, ServerPlayer p, String id) {
        QuestTemplate t = SystemData.quests().get(id);
        if (t == null) return error(src, SystemMsg.text(SystemText.CMD_QUEST_UNKNOWN, id, String.join(", ", templateIds())));
        return roll(src, p, t);
    }

    private static int complete(CommandSourceStack src, ServerPlayer p) {
        return SystemQuests.complete(p) ? ok(src, SystemMsg.text(SystemText.CMD_QUEST_COMPLETED, who(p))) : error(src, SystemMsg.text(SystemText.CMD_QUEST_NOACTIVE, who(p)));
    }

    private static int failQuest(CommandSourceStack src, ServerPlayer p) {
        return SystemQuests.fail(p) ? ok(src, SystemMsg.text(SystemText.CMD_QUEST_FAILED, who(p))) : error(src, SystemMsg.text(SystemText.CMD_QUEST_NOACTIVE, who(p)));
    }

    private static int clear(CommandSourceStack src, ServerPlayer p) {
        SystemQuests.clear(p);
        return ok(src, SystemMsg.text(SystemText.CMD_QUEST_CLEARED, who(p)));
    }

    // ================================================================ stats, cores, shadows
    private static int giveStat(CommandSourceStack src, ServerPlayer p, String statWord, int n) {
        Stat stat = Stat.fromId(statWord);
        if (stat == null) return error(src, SystemMsg.text(SystemText.CMD_STAT_UNKNOWN, statWord));
        SystemState st = SystemQuests.state(p);
        int changed = st.addPoints(stat, n, SystemConfig.get(SystemConfig.MAX_STAT_POINTS));
        SystemStats.apply(p, st);
        SystemSync.request(p);
        return ok(src, SystemMsg.text(SystemText.CMD_STAT_GIVEN, who(p), st.points(stat), stat.name(), changed));
    }

    private static int giveCore(CommandSourceStack src, ServerPlayer p) {
        ItemStack core = ShadowCoreItem.create();
        if (!p.getInventory().add(core)) p.drop(core, false);
        return ok(src, SystemMsg.text(SystemText.CMD_CORE_GIVEN, who(p)));
    }

    private static int clearMinions(CommandSourceStack src, ServerPlayer p) {
        int n = ShadowMinions.dismissAll(src.getServer(), p.getUUID());
        return ok(src, SystemMsg.text(SystemText.CMD_MINIONS_CLEARED, n, who(p)));
    }
}
