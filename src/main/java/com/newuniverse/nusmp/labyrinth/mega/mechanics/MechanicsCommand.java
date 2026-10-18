package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's hands on the mechanics library, attached by the lead under {@code /multiverse mega} (permission 2), in the style of the Hub and Rift commands:
 * {@code mechanic list [category] [page]} counts the library or lists a category's ids, {@code info <id>} prints everything about one mechanic (text, source, scope, tiers, road,
 * trigger, counter, effects), {@code apply <id> here} starts it where the sender stands (refused in a safe room), {@code clear} ends every live mechanic and keeps the sector quiet,
 * {@code reset} lets it start again and forgets which were beaten, {@code count} says how many there are and how many are live, {@code verify} runs the same check as the self-test.
 * All text goes through lang keys {@code message.nusmp.mega.mech.cmd.*} with an English fallback, so it reads fine before the lang fragment is merged.
 */
public final class MechanicsCommand {
    private MechanicsCommand() {}

    private static final int PAGE = 40;

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("mechanic").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> counts(ctx.getSource()))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(categoryIds(), b))
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "category"), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "category"), IntegerArgumentType.getInteger(ctx, "page"))))))
                .then(Commands.literal("info").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(MechanicLibrary.all().stream().map(MechanicDef::id), b))
                        .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("apply").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(MechanicLibrary.all().stream().map(MechanicDef::id), b))
                        .then(Commands.literal("here").executes(ctx -> apply(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("count").executes(ctx -> counts(ctx.getSource())))
                .then(Commands.literal("verify").executes(ctx -> verify(ctx.getSource())));
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.mech.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static List<String> categoryIds() {
        List<String> out = new ArrayList<>();
        for (MechCategory c : MechCategory.values()) out.add(c.id());
        return out;
    }

    private static int counts(CommandSourceStack src) {
        ok(src, text("count.total", "Dungeon mechanics: %s loaded, %s live now (enabled: %s).", MechanicLibrary.size(), MechanicsDirector.ACTIVE.size(), MechanicsConfig.enabled()));
        for (MechCategory c : MechCategory.values()) ok(src, text("count.category", "  %s: %s", c.id(), MechanicLibrary.of(c).size()));
        return MechanicLibrary.size();
    }

    private static int list(CommandSourceStack src, String category, int page) {
        MechCategory c = MechCategory.fromId(category);
        if (c == null) return fail(src, text("badcategory", "No such category '%s'. Try: %s", category, String.join(", ", categoryIds())));
        List<MechanicDef> all = MechanicLibrary.of(c);
        int from = (page - 1) * PAGE;
        if (from >= all.size()) return fail(src, text("nopage", "%s has only %s mechanics.", c.id(), all.size()));
        ok(src, text("list.head", "%s mechanics %s-%s of %s:", c.id(), from + 1, Math.min(all.size(), from + PAGE), all.size()));
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < Math.min(all.size(), from + PAGE); i++) sb.append(all.get(i).id()).append("  ");
        ok(src, Component.literal(sb.toString().trim()));
        return all.size();
    }

    private static int info(CommandSourceStack src, String id) {
        MechanicDef d = MechanicLibrary.get(id);
        if (d == null) return fail(src, text("unknown", "No mechanic '%s'.", id));
        ok(src, Messages.name(d).append(Component.literal(" (" + d.id() + ")")));
        ok(src, Component.translatableWithFallback(d.descKey(), d.desc()));
        ok(src, text("info.line1", "category %s, source %s, scope %s, tiers %s-%s, road %s, cost %s, weight %s", d.category().id(), d.source(), d.scope().id(), d.tierMin(), d.tierMax(), d.road().id(), d.cost(), d.weight()));
        ok(src, text("info.line2", "themes: %s", d.themes().isEmpty() ? "any" : String.join(", ", d.themes())));
        ok(src, text("info.line3", "trigger %s %s, counter %s %s, reward %s", d.trigger().kind(), d.trigger().args(), d.counter().kind(), d.counter().args(), d.reward()));
        for (MechanicDef.Effect e : d.effects()) ok(src, text("info.effect", "  effect %s %s%s", e.op(), e.args(), e.cond().isEmpty() ? "" : " if " + e.cond()));
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat != null) ok(src, text("info.sig", "channels %s, signature %s", Channels.of(d, cat), Signature.hash(Signature.of(d, cat))));
        return 1;
    }

    private static int apply(CommandSourceStack src, String id) {
        MechanicDef d = MechanicLibrary.get(id);
        if (d == null) return fail(src, text("unknown", "No mechanic '%s'.", id));
        ServerPlayer p;
        try { p = src.getPlayerOrException(); } catch (Exception e) { return fail(src, text("noplayer", "Run this as a player.")); }
        if (!(p.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return fail(src, text("notmega", "Stand in the Mega-Dungeon."));
        String[] why = new String[1];
        Instance in = MechanicsDirector.applyAt(level, d, p.blockPosition(), 6, why);
        if (in == null) return fail(src, text("refused", "Not here: %s.", why[0] == null ? "refused" : why[0]));
        return ok(src, text("applied", "Started %s here (clear it with /multiverse mega mechanic clear).", Messages.name(d)));
    }

    private static int clear(CommandSourceStack src) {
        int n;
        ServerPlayer p = src.getPlayer();
        Sector s = p == null ? null : MegaDungeon.sectorAt(p.level(), p.blockPosition());
        n = s == null ? MechanicsDirector.clearAll() : MechanicsDirector.clearSector(s.id) + clearAdmin();
        return ok(src, text("cleared", "Ended %s live mechanic(s); their blocks are back. This sector stays quiet until /multiverse mega mechanic reset.", n));
    }

    private static int clearAdmin() {
        int n = 0;
        for (Instance in : MechanicsDirector.active()) if (in.admin) { MechanicsDirector.endKey(in.key); n++; }
        return n;
    }

    private static int reset(CommandSourceStack src) {
        MechanicsDirector.resetSuppression();
        MechanicsDirector.forgetPlans();
        MechanicsData.get(src.getServer()).clearSolved();
        return ok(src, text("reset", "Mechanics may start again everywhere, and none counts as beaten."));
    }

    private static int verify(CommandSourceStack src) {
        List<String> problems = MechanicsVerify.run();
        if (problems.isEmpty()) return ok(src, text("verify.ok", "Library verified: %s mechanics, %s primitives, nothing wrong.", MechanicLibrary.size(), Prims.ids().size()));
        fail(src, text("verify.bad", "%s problem(s):", problems.size()));
        for (int i = 0; i < Math.min(12, problems.size()); i++) src.sendFailure(Component.literal("  " + problems.get(i)));
        return 0;
    }
}
