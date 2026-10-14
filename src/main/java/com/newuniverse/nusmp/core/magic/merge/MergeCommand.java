package com.newuniverse.nusmp.core.magic.merge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.profile.MagicAudit;
import com.newuniverse.nusmp.core.magic.profile.MagicAuditRuntime;
import com.newuniverse.nusmp.core.magic.profile.MagicKeys;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code /multiverse magic ...}: the commands of the magic framework and the merge generator. One node; MultiverseCommands attaches it
 * with {@code root.then(MergeCommand.node())}.
 * <pre>
 *   /multiverse magic merge &lt;a&gt; &lt;b&gt; [&lt;c&gt; [&lt;d&gt;]] [name]   op: creates the merged magic of 2..4 magics (the same set always gives the same one)
 *   /multiverse magic list                                  every merged magic of the world
 *   /multiverse magic info &lt;merged&gt;                          its parents, palette, hit effects and spells
 *   /multiverse magic remove &lt;merged&gt;                        op: deletes the definition (only on request; the parents are never touched)
 *   /multiverse magic grant &lt;player&gt; &lt;merged&gt;                op: gives the player the merged magic
 *   /multiverse magic revoke &lt;player&gt; &lt;merged&gt;               op: takes it back
 *   /multiverse magic audit                                 op: the magic framework's audit (missing lang / icons / books)
 * </pre>
 * &lt;merged&gt; is its name, its id ("merged:flame+water") or "#n" (the number /multiverse magic list shows).
 */
public final class MergeCommand {
    private MergeCommand() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    private static final SuggestionProvider<CommandSourceStack> MAGICS = (ctx, b) -> {
        List<String> names = new ArrayList<>();
        for (MagicType t : MagicType.values()) if (!t.restricted()) names.add(t.name().toLowerCase(Locale.ROOT));
        return SharedSuggestionProvider.suggest(names, b);
    };

    private static final SuggestionProvider<CommandSourceStack> MERGED = (ctx, b) -> {
        List<String> names = new ArrayList<>();
        for (MergedDef d : MergedRegistry.all()) names.add(d.id());
        return SharedSuggestionProvider.suggest(names, b);
    };

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> magic = Commands.literal("magic");
        magic.then(Commands.literal("merge").requires(MergeCommand::admin)
                .then(Commands.argument("a", StringArgumentType.word()).suggests(MAGICS)
                        .then(Commands.argument("b", StringArgumentType.word()).suggests(MAGICS)
                                .executes(ctx -> merge(ctx, ""))
                                .then(Commands.argument("rest", StringArgumentType.greedyString()).suggests(MAGICS)
                                        .executes(ctx -> merge(ctx, StringArgumentType.getString(ctx, "rest")))))));
        magic.then(Commands.literal("list").executes(MergeCommand::list));
        magic.then(Commands.literal("info").then(Commands.argument("merged", StringArgumentType.greedyString()).suggests(MERGED).executes(MergeCommand::info)));
        magic.then(Commands.literal("remove").requires(MergeCommand::admin)
                .then(Commands.argument("merged", StringArgumentType.greedyString()).suggests(MERGED).executes(MergeCommand::remove)));
        magic.then(Commands.literal("grant").requires(MergeCommand::admin).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("merged", StringArgumentType.greedyString()).suggests(MERGED).executes(ctx -> give(ctx, true)))));
        magic.then(Commands.literal("revoke").requires(MergeCommand::admin).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("merged", StringArgumentType.greedyString()).suggests(MERGED).executes(ctx -> give(ctx, false)))));
        magic.then(Commands.literal("audit").requires(MergeCommand::admin).executes(MergeCommand::audit));
        return magic;
    }

    // ---------------------------------------------------------------- helpers
    private static int ok(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendSuccess(() -> msg, true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendFailure(msg);
        return 0;
    }

    /** A merged magic by name (any case), id or "#slot". */
    static MergedDef find(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return null;
        if (q.startsWith("#")) {
            try { return MergedRegistry.bySlot(Integer.parseInt(q.substring(1).trim()) - 1); } catch (NumberFormatException e) { return null; }      // "#1" = the first slot, as /magic list numbers them
        }
        MergedDef byId = MergedRegistry.byId(q);
        if (byId != null) return byId;
        for (MergedDef d : MergedRegistry.all()) if (d.name().equalsIgnoreCase(q)) return d;
        return null;
    }

    private static Component parents(MergedDef d) {
        StringBuilder sb = new StringBuilder();
        for (String p : d.parents()) sb.append(sb.length() == 0 ? "" : " + ").append(MagicProfiles.of(p).displayName());
        return Component.literal(sb.toString());
    }

    private static Set<String> reservedNames() {
        Set<String> s = new HashSet<>();
        for (MagicType t : MagicType.values()) { s.add(t.displayName.toLowerCase(Locale.ROOT)); s.add(t.word.toLowerCase(Locale.ROOT)); }
        return s;
    }

    // ---------------------------------------------------------------- merge
    private static int merge(CommandContext<CommandSourceStack> ctx, String rest) {
        String a = StringArgumentType.getString(ctx, "a"), b = StringArgumentType.getString(ctx, "b");
        MergeArgs.Parsed parsed = MergeArgs.parse(a, b, rest, w -> MagicType.lookup(w) != null);
        var server = ctx.getSource().getServer();
        MergedMagicData data = MergedMagicData.get(server);
        MergeStore.Outcome o = data.create(parsed.parents(), parsed.name(), n -> MagicType.lookup(n) != null, reservedNames(),
                part -> MagicProfiles.of(part).displayName());
        switch (o.status()) {
            case REFUSED -> {
                MergeCheck.Result r = o.check();
                String detail = r.refusal() == MergeCheck.Refusal.UNKNOWN_PARENT || r.refusal() == MergeCheck.Refusal.RESTRICTED_PARENT
                        ? MagicKeys.prettify(r.detail()) : r.detail();
                return fail(ctx, Component.translatable(r.refusal().langKey, detail));
            }
            case EXISTS -> {
                return ok(ctx, Component.translatable("nusmp.merge.exists", o.def().name(), o.def().id()));
            }
            default -> {
                MergedSync.sendToAll(server, data);
                for (ServerPlayer p : server.getPlayerList().getPlayers()) MergeAccess.tick(p);          // players who already hold every parent get it now
                return ok(ctx, Component.translatable("nusmp.merge.created", o.def().name(), o.def().id(), o.def().slot() + 1,
                        MergeLimits.MAX_MERGED - data.store().size()));
            }
        }
    }

    // ---------------------------------------------------------------- list / info
    private static int list(CommandContext<CommandSourceStack> ctx) {
        List<MergedDef> all = MergedRegistry.all();
        if (all.isEmpty()) return ok(ctx, Component.translatable("nusmp.merge.list_empty"));
        ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.merge.list_header", all.size(), MergeLimits.MAX_MERGED).withStyle(ChatFormatting.GOLD), false);
        for (MergedDef d : all)
            ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.merge.list_entry", d.slot() + 1, d.name(), parents(d)), false);
        return all.size();
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        MergedDef d = find(StringArgumentType.getString(ctx, "merged"));
        if (d == null) return fail(ctx, Component.translatable("nusmp.merge.not_found", StringArgumentType.getString(ctx, "merged")));
        MagicProfile prof = MagicProfiles.of(d.id());
        MergedBlend b = MergedRegistry.blend(d);
        var src = ctx.getSource();
        src.sendSuccess(() -> Component.literal(d.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(Component.literal("  " + d.id()).withStyle(ChatFormatting.DARK_GRAY)), false);
        src.sendSuccess(() -> Component.translatable("nusmp.merge.info_parents", parents(d), d.dominant().isEmpty() ? "" : MagicProfiles.of(d.dominant()).displayName()), false);
        src.sendSuccess(() -> Component.translatable("nusmp.merge.info_soul", prof.soulType(), b.hitPrimary, b.hitSecondary == null ? "-" : b.hitSecondary), false);
        StringBuilder hex = new StringBuilder();
        for (int c : b.colors) hex.append(String.format("#%06X ", c & 0xFFFFFF));
        src.sendSuccess(() -> Component.translatable("nusmp.merge.info_palette", hex.toString().trim(), String.format("#%06X", b.primary & 0xFFFFFF)), false);
        var pages = MergedPages.pages(d.slot());
        src.sendSuccess(() -> Component.translatable("nusmp.merge.info_spells", pages.size()), false);
        for (var page : pages)
            src.sendSuccess(() -> Component.literal("  - " + page.name() + String.format(Locale.ROOT, " (%.0f%%, %ds)", page.costPercent(), page.cooldown() / 20)).withStyle(ChatFormatting.GRAY), false);
        if (src.getEntity() instanceof ServerPlayer p)
            src.sendSuccess(() -> Component.translatable(MergeAccess.held(p, d).isPresent() ? "nusmp.merge.info_holds" : "nusmp.merge.info_not_holds"), false);
        return 1;
    }

    // ---------------------------------------------------------------- remove / grant / revoke
    private static int remove(CommandContext<CommandSourceStack> ctx) {
        MergedDef d = find(StringArgumentType.getString(ctx, "merged"));
        if (d == null) return fail(ctx, Component.translatable("nusmp.merge.not_found", StringArgumentType.getString(ctx, "merged")));
        var server = ctx.getSource().getServer();
        MergedMagicData data = MergedMagicData.get(server);
        if (!data.remove(d.id())) return fail(ctx, Component.translatable("nusmp.merge.not_found", d.id()));
        MergeAccess.purge(server, d);
        MergedSync.sendToAll(server, data);
        return ok(ctx, Component.translatable("nusmp.merge.removed", d.name()));
    }

    private static int give(CommandContext<CommandSourceStack> ctx, boolean grant) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        MergedDef d = find(StringArgumentType.getString(ctx, "merged"));
        if (d == null) return fail(ctx, Component.translatable("nusmp.merge.not_found", StringArgumentType.getString(ctx, "merged")));
        boolean changed = grant ? MergeAccess.grant(p, d) : MergeAccess.revoke(p, d);
        if (!changed) return fail(ctx, Component.translatable(grant ? "nusmp.merge.already_holds" : "nusmp.merge.does_not_hold", p.getName(), d.name()));
        return ok(ctx, Component.translatable(grant ? "nusmp.merge.granted" : "nusmp.merge.revoked", d.name(), p.getName()));
    }

    // ---------------------------------------------------------------- audit
    private static int audit(CommandContext<CommandSourceStack> ctx) {
        MagicAudit.Report r = MagicAuditRuntime.run();
        List<String> lines = r.lines();
        ctx.getSource().sendSuccess(() -> Component.literal(lines.get(0)).withStyle(r.errors() > 0 ? ChatFormatting.RED : ChatFormatting.GREEN), false);
        ctx.getSource().sendSuccess(() -> Component.literal(r.summary()).withStyle(ChatFormatting.GRAY), false);
        int shown = 0;
        for (MagicAudit.Finding f : r.findings()) {
            if (f.severity() == MagicAudit.Severity.INFO || ++shown > 30) continue;
            ctx.getSource().sendSuccess(() -> Component.literal(f.toString()).withStyle(f.severity() == MagicAudit.Severity.ERROR ? ChatFormatting.RED : ChatFormatting.YELLOW), false);
        }
        if (shown > 30) ctx.getSource().sendSuccess(() -> Component.literal("... and more (see the log)").withStyle(ChatFormatting.GRAY), false);
        return r.errors() == 0 ? 1 : 0;
    }
}
