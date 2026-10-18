package com.newuniverse.nusmp.elite.nations.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Perk;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.core.Role;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.net.NationNet;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarBook;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Why: the {@code /nation ...} command tree. Every ACTION command is a thin wrapper that calls {@link NationActions#run} (the same entry the screen uses), so the commands and the GUI cannot disagree; the
 * READ-ONLY commands (info, list, level, ledger, contract list, bounty list, rep, war status/history, ally list, perks) print from the saved data. {@code /nation gui} opens the screen. The
 * {@code admin} subtree needs permission level 2 (disband, xp, deposit, end a war, set reputation, run the self-tests). Names are literals through {@link Msg} (English fallbacks), so the tree works before the lang
 * fragment is merged.
 */
public final class NationCommand {
    private NationCommand() {}

    private static final SuggestionProvider<CommandSourceStack> NATIONS = (ctx, b) -> SharedSuggestionProvider.suggest(
            NationService.data(ctx.getSource().getServer()).nations.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> MEMBERS = (ctx, b) -> {
        ServerPlayer p = ctx.getSource().getPlayer();
        Nation n = p == null ? null : NationService.data(ctx.getSource().getServer()).nationOf(p.getUUID());
        List<String> names = new ArrayList<>();
        if (n != null) for (UUID id : n.members.keySet()) names.add(n.nameOf(id));
        return SharedSuggestionProvider.suggest(names, b);
    };
    private static final SuggestionProvider<CommandSourceStack> ROLES = (ctx, b) -> {
        ServerPlayer p = ctx.getSource().getPlayer();
        Nation n = p == null ? null : NationService.data(ctx.getSource().getServer()).nationOf(p.getUUID());
        return SharedSuggestionProvider.suggest(n == null ? List.<String>of() : new ArrayList<>(n.roles.keySet()), b);
    };
    private static final SuggestionProvider<CommandSourceStack> PERMS = (ctx, b) -> {
        List<String> ids = new ArrayList<>();
        for (Permission p : Permission.values()) ids.add(p.id());
        return SharedSuggestionProvider.suggest(ids, b);
    };

    private static RequiredArgumentBuilder<CommandSourceStack, String> nationArg(String name) { return Commands.argument(name, StringArgumentType.word()).suggests(NATIONS); }

    // ---------------------------------------------------------------- output helpers
    private static void say(CommandContext<CommandSourceStack> c, Component text) { c.getSource().sendSuccess(() -> text, false); }

    private static void line(CommandContext<CommandSourceStack> c, String key, Object... args) { say(c, Msg.of(key, args)); }

    private static int run(CommandContext<CommandSourceStack> c, String action, String a, String b, long n) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Res r = NationActions.run(p, action, a, b, n);
        Msg.tell(p, r);
        return r.ok() ? 1 : 0;
    }

    private static String str(CommandContext<CommandSourceStack> c, String name) { return StringArgumentType.getString(c, name); }

    private static Nation mine(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Nation n = NationService.data(c.getSource().getServer()).nationOf(p.getUUID());
        if (n == null) Msg.tell(p, Res.fail("nusmp.nation.m.not_in_nation"));
        return n;
    }

    public static int parseColor(String s, String fallbackName) {
        try {
            String t = s.startsWith("#") ? s.substring(1) : s;
            if (t.length() == 6) return Integer.parseInt(t, 16);
        } catch (NumberFormatException ignored) { /* falls through to the derived colour */ }
        float hue = Math.floorMod(fallbackName.hashCode(), 360) / 360f;
        return java.awt.Color.HSBtoRGB(hue, 0.6f, 0.9f) & 0xFFFFFF;
    }

    // ---------------------------------------------------------------- tree
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nation");
        root.then(Commands.literal("gui").executes(c -> { NationNet.open(c.getSource().getPlayerOrException(), 0); return 1; })
                .then(Commands.argument("tab", IntegerArgumentType.integer(0, 4)).executes(c -> { NationNet.open(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "tab")); return 1; })));
        root.then(Commands.literal("found").then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(c -> run(c, "found", str(c, "name"), "", parseColor("", str(c, "name"))))));
        root.then(Commands.literal("disband").executes(c -> { line(c, "nusmp.nation.l.confirm_disband"); return 1; })
                .then(Commands.literal("confirm").executes(c -> run(c, "disband", "", "", 0))));
        root.then(Commands.literal("info").executes(c -> info(c, null)).then(nationArg("nation").executes(c -> info(c, str(c, "nation")))));
        root.then(Commands.literal("list").executes(NationCommand::list));
        root.then(Commands.literal("level").executes(NationCommand::level));
        root.then(Commands.literal("perks").executes(NationCommand::perks));
        root.then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(c -> run(c, "invite", EntityArgument.getPlayer(c, "player").getGameProfile().getName(), "", 0))));
        root.then(Commands.literal("accept").executes(c -> run(c, "accept", "", "", 0)).then(nationArg("nation").executes(c -> run(c, "accept", str(c, "nation"), "", 0))));
        root.then(Commands.literal("decline").executes(c -> run(c, "decline", "", "", 0)).then(nationArg("nation").executes(c -> run(c, "decline", str(c, "nation"), "", 0))));
        root.then(Commands.literal("leave").executes(c -> run(c, "leave", "", "", 0)));
        root.then(Commands.literal("kick").then(Commands.argument("member", StringArgumentType.word()).suggests(MEMBERS).executes(c -> run(c, "kick", str(c, "member"), "", 0))));
        root.then(Commands.literal("transfer").then(Commands.argument("member", StringArgumentType.word()).suggests(MEMBERS).executes(c -> run(c, "transfer", str(c, "member"), "", 0))));
        root.then(Commands.literal("rename").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> run(c, "rename", str(c, "name"), "", 0))));
        root.then(Commands.literal("color").then(Commands.argument("hex", StringArgumentType.word()).executes(c -> run(c, "color", "", "", parseColor(str(c, "hex"), "x")))));
        root.then(Commands.literal("tax").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> run(c, "tax", "", "", IntegerArgumentType.getInteger(c, "percent")))));
        root.then(Commands.literal("capital").executes(NationCommand::capital).then(Commands.literal("item").executes(c -> run(c, "banner_item", "", "", 0))));

        root.then(Commands.literal("role")
                .then(Commands.literal("list").executes(NationCommand::roles))
                .then(Commands.literal("set").then(Commands.argument("member", StringArgumentType.word()).suggests(MEMBERS)
                        .then(Commands.argument("role", StringArgumentType.word()).suggests(ROLES).executes(c -> run(c, "setrole", str(c, "member"), str(c, "role"), 0)))))
                .then(Commands.literal("create").then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> run(c, "role_create", str(c, "id"), str(c, "name"), 0)))))
                .then(Commands.literal("delete").then(Commands.argument("id", StringArgumentType.word()).suggests(ROLES).executes(c -> run(c, "role_delete", str(c, "id"), "", 0))))
                .then(Commands.literal("perm").then(Commands.argument("id", StringArgumentType.word()).suggests(ROLES)
                        .then(Commands.argument("perm", StringArgumentType.word()).suggests(PERMS)
                                .then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> run(c, "role_perm", str(c, "id"), str(c, "perm"), BoolArgumentType.getBool(c, "on") ? 1 : 0)))))));

        root.then(Commands.literal("treasury")
                .executes(NationCommand::treasury)
                .then(Commands.literal("balance").executes(NationCommand::treasury))
                .then(Commands.literal("ledger").executes(c -> ledger(c, 10)).then(Commands.argument("lines", IntegerArgumentType.integer(1, 30)).executes(c -> ledger(c, IntegerArgumentType.getInteger(c, "lines")))))
                .then(Commands.literal("deposit").then(Commands.argument("coins", LongArgumentType.longArg(1)).executes(c -> run(c, "deposit", "", "", LongArgumentType.getLong(c, "coins")))
                        .then(Commands.literal("ubs").executes(c -> run(c, "deposit", "ubs", "", LongArgumentType.getLong(c, "coins"))))))
                .then(Commands.literal("withdraw").then(Commands.argument("coins", LongArgumentType.longArg(1)).executes(c -> run(c, "withdraw", "", "", LongArgumentType.getLong(c, "coins")))
                        .then(Commands.literal("ubs").executes(c -> run(c, "withdraw", "ubs", "", LongArgumentType.getLong(c, "coins"))))))
                .then(Commands.literal("tax").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> run(c, "tax", "", "", IntegerArgumentType.getInteger(c, "percent"))))));

        root.then(Commands.literal("contract")
                .then(Commands.literal("list").executes(NationCommand::contracts))
                .then(Commands.literal("take").then(Commands.argument("id", LongArgumentType.longArg(1)).executes(c -> run(c, "take", "", "", LongArgumentType.getLong(c, "id")))))
                .then(Commands.literal("abandon").then(Commands.argument("id", LongArgumentType.longArg(1)).executes(c -> run(c, "abandon", "", "", LongArgumentType.getLong(c, "id")))))
                .then(Commands.literal("deliver").then(Commands.argument("id", LongArgumentType.longArg(1)).executes(c -> run(c, "deliver", "", "", LongArgumentType.getLong(c, "id"))))));

        root.then(Commands.literal("bounty")
                .then(Commands.literal("list").executes(NationCommand::bounties))
                .then(Commands.literal("cancel").then(Commands.argument("id", LongArgumentType.longArg(1)).executes(c -> run(c, "bounty_cancel", "", "", LongArgumentType.getLong(c, "id")))))
                .then(Commands.literal("player").then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument("coins", LongArgumentType.longArg(1))
                        .executes(c -> run(c, "bounty_player", str(c, "name"), "", LongArgumentType.getLong(c, "coins")))
                        .then(Commands.literal("nation").executes(c -> run(c, "bounty_player_nation", str(c, "name"), "", LongArgumentType.getLong(c, "coins")))))))
                .then(Commands.literal("mob").then(Commands.argument("entity", StringArgumentType.word()).then(Commands.argument("coins", LongArgumentType.longArg(1))
                        .executes(c -> run(c, "bounty_mob", str(c, "entity"), "", LongArgumentType.getLong(c, "coins")))
                        .then(Commands.literal("nation").executes(c -> run(c, "bounty_mob_nation", str(c, "entity"), "", LongArgumentType.getLong(c, "coins"))))))));

        root.then(Commands.literal("rep").executes(NationCommand::reputation));

        root.then(Commands.literal("war")
                .then(Commands.literal("declare").then(nationArg("nation").executes(c -> run(c, "war_declare", str(c, "nation"), "", 0))
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> run(c, "war_declare", str(c, "nation"), str(c, "reason"), 0)))))
                .then(Commands.literal("status").executes(NationCommand::warStatus))
                .then(Commands.literal("history").executes(NationCommand::warHistory))
                .then(Commands.literal("ceasefire").then(nationArg("nation").executes(c -> run(c, "war_ceasefire", str(c, "nation"), "", 0))))
                .then(Commands.literal("decline").then(nationArg("nation").executes(c -> run(c, "war_decline", str(c, "nation"), "", 0))))
                .then(Commands.literal("surrender").then(nationArg("nation").executes(c -> run(c, "war_surrender", str(c, "nation"), "", 0))))
                .then(Commands.literal("cancel").then(nationArg("nation").executes(c -> run(c, "war_cancel", str(c, "nation"), "", 0)))));

        root.then(Commands.literal("ally")
                .then(Commands.literal("list").executes(NationCommand::allies))
                .then(Commands.literal("propose").then(nationArg("nation").executes(c -> run(c, "ally_propose", str(c, "nation"), "", 0))))
                .then(Commands.literal("accept").then(nationArg("nation").executes(c -> run(c, "ally_accept", str(c, "nation"), "", 0))))
                .then(Commands.literal("decline").then(nationArg("nation").executes(c -> run(c, "ally_decline", str(c, "nation"), "", 0))))
                .then(Commands.literal("break").then(nationArg("nation").executes(c -> run(c, "ally_break", str(c, "nation"), "", 0)))));

        root.then(Commands.literal("admin").requires(s -> s.hasPermission(2))
                .then(Commands.literal("disband").then(nationArg("nation").executes(c -> adminRes(c, NationService.disbandNation(c.getSource().getServer(), str(c, "nation"), "admin")))))
                .then(Commands.literal("xp").then(nationArg("nation").then(Commands.argument("xp", LongArgumentType.longArg()).executes(c -> adminRes(c, NationService.adminAddXp(c.getSource().getServer(), str(c, "nation"), LongArgumentType.getLong(c, "xp")))))))
                .then(Commands.literal("deposit").then(nationArg("nation").then(Commands.argument("coins", LongArgumentType.longArg()).executes(c -> adminRes(c, NationService.adminDeposit(c.getSource().getServer(), str(c, "nation"), LongArgumentType.getLong(c, "coins")))))))
                .then(Commands.literal("endwar").then(nationArg("a").then(nationArg("b").executes(c -> adminRes(c, WarService.adminEnd(c.getSource().getServer(), str(c, "a"), str(c, "b")))))))
                .then(Commands.literal("rep").then(Commands.argument("player", EntityArgument.player()).then(nationArg("nation").then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000)).executes(c -> {
                    ReputationService.set(c.getSource().getServer(), EntityArgument.getPlayer(c, "player").getUUID(), str(c, "nation"), IntegerArgumentType.getInteger(c, "value"));
                    return 1;
                })))))
                .then(Commands.literal("selftest").executes(c -> { NationSelfTests.runAll(c.getSource().getServer()).forEach(l -> say(c, Component.literal(l))); return 1; })));
        d.register(root);
    }

    private static int adminRes(CommandContext<CommandSourceStack> c, Res r) {
        c.getSource().sendSuccess(() -> Msg.colored(r), true);
        return r.ok() ? 1 : 0;
    }

    // ---------------------------------------------------------------- read-only output
    private static NationsData data(CommandContext<CommandSourceStack> c) { return NationService.data(c.getSource().getServer()); }

    private static int info(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        NationsData d = data(c);
        Nation n = id == null ? d.nationOf(c.getSource().getPlayerOrException().getUUID()) : d.nations.get(id);
        if (n == null) { line(c, "nusmp.nation.m.no_such_nation", id == null ? "-" : id); return 0; }
        line(c, "nusmp.nation.l.info_head", n.name, n.level, n.memberCount(), n.nameOf(n.leader));
        line(c, "nusmp.nation.l.info_xp", n.xp, Math.round(NationService.score(n)), n.completedContracts, n.warWins, n.warLosses);
        if (d.nationIdOf(c.getSource().getPlayer() == null ? new UUID(0, 0) : c.getSource().getPlayer().getUUID()).equals(n.id)) {
            line(c, "nusmp.nation.l.info_treasury", n.ledger.balance(), NationPerks.treasuryCap(n), n.taxPercent);
        }
        line(c, n.hasCapital ? "nusmp.nation.l.info_capital" : "nusmp.nation.l.info_nocapital", n.capitalDim, n.cx, n.cy, n.cz);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        NationsData d = data(c);
        if (d.nations.isEmpty()) { line(c, "nusmp.nation.l.none"); return 0; }
        for (Nation n : d.nations.values()) line(c, "nusmp.nation.l.list_row", n.name, n.id, n.level, n.memberCount());
        return d.nations.size();
    }

    private static int level(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        long[] th = NationService.thresholds();
        double score = NationService.score(n);
        line(c, "nusmp.nation.l.level", n.level, th.length + 1, Math.round(score), com.newuniverse.nusmp.elite.nations.core.NationLevels.missingForNext(score, th));
        return 1;
    }

    private static int perks(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        for (Perk p : NationPerks.table().upTo(n.level)) line(c, "nusmp.nation.l.perk", p.type(), p.arg(), p.value());
        for (Perk p : NationPerks.table().at(n.level + 1)) line(c, "nusmp.nation.l.perk_next", n.level + 1, p.type(), p.arg(), p.value());
        return 1;
    }

    private static int capital(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        line(c, n.hasCapital ? "nusmp.nation.l.info_capital" : "nusmp.nation.l.info_nocapital", n.capitalDim, n.cx, n.cy, n.cz);
        return 1;
    }

    private static int roles(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        for (Role r : n.roles.values()) {
            StringBuilder sb = new StringBuilder();
            for (Permission p : r.permissions()) sb.append(sb.length() == 0 ? "" : ", ").append(p.id());
            line(c, "nusmp.nation.l.role_row", r.id(), r.name(), r.rank(), n.countRole(r.id()), sb.length() == 0 ? "-" : sb.toString());
        }
        for (UUID id : n.members.keySet()) line(c, "nusmp.nation.l.member_row", n.nameOf(id), n.members.get(id));
        return 1;
    }

    private static int treasury(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        line(c, "nusmp.nation.l.info_treasury", n.ledger.balance(), NationPerks.treasuryCap(n), n.taxPercent);
        return 1;
    }

    private static int ledger(CommandContext<CommandSourceStack> c, int lines) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        for (Ledger.Entry e : n.ledger.recent(lines)) {
            line(c, "nusmp.nation.l.ledger_row", new java.text.SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(new java.util.Date(e.time())), e.kind().name().toLowerCase(Locale.ROOT),
                    e.actor() == null ? "-" : n.nameOf(e.actor()), e.amount(), e.balanceAfter(), e.note());
        }
        return 1;
    }

    private static int contracts(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        for (Contract k : data(c).board(n.id)) {
            line(c, "nusmp.nation.l.contract_row", k.uid, k.title, k.progress, k.goal, k.tier, k.reward.coins(), k.state.name().toLowerCase(Locale.ROOT), k.taker == null ? "-" : n.nameOf(k.taker));
        }
        return 1;
    }

    private static int bounties(CommandContext<CommandSourceStack> c) {
        NationsData d = data(c);
        if (d.bounties.isEmpty()) { line(c, "nusmp.nation.l.none"); return 0; }
        for (Bounty b : d.bounties) line(c, "nusmp.nation.l.bounty_row", b.uid, b.targetName, b.kind.name().toLowerCase(Locale.ROOT), b.coins);
        return d.bounties.size();
    }

    private static int reputation(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        NationsData d = data(c);
        var rep = d.reputation.of(p.getUUID());
        if (rep.isEmpty()) { line(c, "nusmp.nation.l.none"); return 0; }
        rep.forEach((id, v) -> {
            Nation n = d.nations.get(id);
            line(c, "nusmp.nation.l.rep_row", n == null ? id : n.name, v, Msg.of("nusmp.nation.tier." + ReputationTier.of(v).id()));
        });
        return 1;
    }

    private static int warStatus(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        NationsData d = data(c);
        long now = NationService.now(c.getSource().getServer());
        List<War> wars = d.wars.warsOf(n.id);
        if (wars.isEmpty()) { line(c, "nusmp.nation.l.no_wars"); return 0; }
        for (War w : wars) {
            Nation o = d.nations.get(w.other(n.id));
            line(c, "nusmp.nation.l.war_row", o == null ? w.other(n.id) : o.name, w.phase.id(), Math.max(0, (w.phaseEnds - now) / 20), w.score(n.id), w.score(w.other(n.id)));
        }
        return 1;
    }

    private static int warHistory(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        NationsData d = data(c);
        Nation n = d.nationOf(c.getSource().getPlayerOrException().getUUID());
        List<WarBook.Record> rows = d.wars.historyOf(n == null ? null : n.id, 10);
        if (rows.isEmpty()) { line(c, "nusmp.nation.l.none"); return 0; }
        for (WarBook.Record r : rows) {
            Nation a = d.nations.get(r.attacker()), b = d.nations.get(r.defender());
            line(c, "nusmp.nation.l.history_row", a == null ? r.attacker() : a.name, b == null ? r.defender() : b.name, r.outcome().name().toLowerCase(Locale.ROOT), r.scoreAttacker(), r.scoreDefender(), r.spoils());
        }
        return 1;
    }

    private static int allies(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Nation n = mine(c);
        if (n == null) return 0;
        NationsData d = data(c);
        for (String a : d.alliances.alliesOf(n.id)) line(c, "nusmp.nation.l.ally_row", d.nations.containsKey(a) ? d.nations.get(a).name : a);
        for (String a : d.alliances.proposalsTo(n.id)) line(c, "nusmp.nation.l.ally_in", d.nations.containsKey(a) ? d.nations.get(a).name : a, a);
        for (String a : d.alliances.proposalsFrom(n.id)) line(c, "nusmp.nation.l.ally_out", d.nations.containsKey(a) ? d.nations.get(a).name : a);
        return 1;
    }
}
