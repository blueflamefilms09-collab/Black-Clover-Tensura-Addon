package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/**
 * Why: the forms system's part of {@code /multiverse hero ...}, attached by the core's {@code HeroCommand} so that no two builders edit one command class. Anyone may
 * read: {@code form list [weapon]} prints the forms of a weapon (with the caller's own progress when it is their weapon) and {@code form book} opens the Weapon Book.
 * Operators (permission 2) may change a player's forms: {@code learn} (one form, or {@code all} ordinary forms, ignoring parents and requirements), {@code set}
 * (change form by the normal rules), {@code absorb} (the main-hand or off-hand stack into the gem), {@code reset} (forget forms, mastery and meals; keep the weapon
 * and its level), {@code mastery} (set a form's mastery, finishing it when full) and {@code report} (what the loader found in the data files). Nothing here bypasses
 * a rule the game enforces except {@code learn}, which exists to skip the grind when testing.
 */
public final class FormsCommands {
    private FormsCommands() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("form")
                .then(Commands.literal("list").executes(ctx -> list(ctx, null))
                        .then(Commands.argument("weapon", StringArgumentType.word()).suggests(FormsCommands::weapons)
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "weapon")))))
                .then(Commands.literal("book").executes(FormsCommands::book))
                .then(Commands.literal("report").requires(FormsCommands::admin).executes(FormsCommands::report))
                .then(Commands.literal("learn").requires(FormsCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("form", StringArgumentType.word()).suggests((c, b) -> formIds(c, b, true)).executes(FormsCommands::learn))))
                .then(Commands.literal("set").requires(FormsCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("form", StringArgumentType.word()).suggests((c, b) -> formIds(c, b, false)).executes(FormsCommands::set))))
                .then(Commands.literal("absorb").requires(FormsCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(FormsCommands::absorb)))
                .then(Commands.literal("reset").requires(FormsCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(FormsCommands::reset)))
                .then(Commands.literal("mastery").requires(FormsCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("form", StringArgumentType.word()).suggests((c, b) -> formIds(c, b, false))
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 100000)).executes(FormsCommands::mastery)))));
    }

    // ------------------------------------------------------------------------------------------------ suggestions
    private static CompletableFuture<Suggestions> weapons(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        List<String> ids = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) ids.add(k.itemId());
        return SharedSuggestionProvider.suggest(ids, b);
    }

    private static CompletableFuture<Suggestions> formIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b, boolean withAll) {
        List<String> ids = new ArrayList<>();
        try {
            HeroKind kind = HeroBonds.kindOf(EntityArgument.getPlayer(ctx, "player"));
            if (kind != null) for (HeroForm f : HeroForms.of(kind)) ids.add(f.id());
        } catch (CommandSyntaxException | RuntimeException unresolved) {
            // the player argument is not complete yet: fall back to every weapon's forms
        }
        if (ids.isEmpty()) for (HeroKind k : HeroKind.values()) for (HeroForm f : HeroForms.of(k)) if (!ids.contains(f.id())) ids.add(f.id());
        if (withAll) ids.add("all");
        return SharedSuggestionProvider.suggest(ids, b);
    }

    // ------------------------------------------------------------------------------------------------ read-only commands
    private static int list(CommandContext<CommandSourceStack> ctx, String weapon) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer self = src.getPlayer();
        HeroKind kind;
        if (weapon == null) {
            kind = self == null ? null : HeroBonds.kindOf(self);
            if (kind == null) {
                src.sendFailure(Component.literal("You have no hero weapon: name one, /multiverse hero form list <weapon>."));
                return 0;
            }
        } else {
            kind = HeroKind.parse(weapon);
            if (kind == null) {
                src.sendFailure(Component.literal("There is no weapon '" + weapon + "'."));
                return 0;
            }
        }
        HeroBond own = self == null ? null : HeroBonds.of(self);
        HeroBond bond = own != null && own.isBound() && own.kind() == kind ? own : null;
        List<HeroForm> forms = HeroForms.of(kind);
        FormsSchema.Parsed report = FormsStore.reports().get(kind);
        String source = report == null ? (FormsStore.hasSynced(kind) ? "sent by the server" : "no data file: the base form only") : report.fileRejected() ? "FILE REJECTED: the base form only" : "data file";
        src.sendSuccess(() -> Component.literal(kind.display() + ": " + forms.size() + " forms (" + source + ")").withStyle(ChatFormatting.GOLD), false);
        for (FormTree.Row row : FormTree.rows(forms)) {
            HeroForm f = row.form();
            if (row.curseHeading()) src.sendSuccess(() -> Component.literal("  Curse Series").withStyle(ChatFormatting.DARK_RED), false);
            StringBuilder line = new StringBuilder("  ").append("  ".repeat(row.indent())).append(f.id()).append(" (").append(f.name()).append(") tier ").append(f.tier());
            if (bond != null)
                line.append(" [").append(FormTree.status(f, bond.form(), bond.unlocked(), bond.absorbedMap(), FormTags.MATCHER, CurseLink.series(bond)).name().toLowerCase()).append("]");
            if (f.mastery() > 0) line.append(", mastery ").append(bond == null ? "" : bond.mastery(f.id()) + "/").append(f.mastery());
            if (f.curse() && f.sin() != null) line.append(", ").append(f.sin().label());
            if (!f.requires().isEmpty()) line.append("; needs ").append(describe(f, bond));
            String text = line.toString();
            src.sendSuccess(() -> Component.literal(text), false);
        }
        return forms.size();
    }

    /** {@code 32x #minecraft:leaves, 8x minecraft:glow_berries}, with the progress ({@code 5/32}) when the caller owns the weapon. */
    private static String describe(HeroForm f, HeroBond bond) {
        StringJoiner j = new StringJoiner(", ");
        for (HeroForm.Requirement r : f.requires()) {
            String name = (r.isTag() ? "#" + r.tag() : r.item());
            j.add(bond == null ? r.count() + "x " + name : FormTree.progress(r, bond.absorbedMap(), FormTags.MATCHER) + "/" + r.count() + " " + name);
        }
        return j.toString();
    }

    private static int book(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!HeroBonds.has(p)) {
                ctx.getSource().sendFailure(Component.literal("You have no hero weapon."));
                return 0;
            }
            FormsSync.open(p, "", 0);
            return 1;
        } catch (CommandSyntaxException notAPlayer) {
            ctx.getSource().sendFailure(Component.literal("Only a player can open the Weapon Book."));
            return 0;
        }
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Map<HeroKind, FormsSchema.Parsed> reports = FormsStore.reportsCopy();
        src.sendSuccess(() -> Component.literal("Hero forms files: " + reports.size() + " of " + HeroKind.values().length + " weapons have one.").withStyle(ChatFormatting.GOLD), false);
        for (Map.Entry<HeroKind, FormsSchema.Parsed> e : reports.entrySet()) {
            FormsSchema.Parsed p = e.getValue();
            String line = "  " + e.getKey().itemId() + ": " + p.forms().size() + " forms, " + p.errors().size() + " error(s), " + p.warnings().size() + " warning(s)"
                    + (p.fileRejected() ? ", REJECTED" : "");
            src.sendSuccess(() -> Component.literal(line).withStyle(p.clean() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
            int shown = 0;
            for (String m : p.errors()) if (shown++ < 3) src.sendSuccess(() -> Component.literal("    error: " + m).withStyle(ChatFormatting.RED), false);
            for (String m : p.warnings()) if (shown++ < 3) src.sendSuccess(() -> Component.literal("    warning: " + m).withStyle(ChatFormatting.YELLOW), false);
        }
        for (String s : FormsStore.stray()) src.sendSuccess(() -> Component.literal("  stray file (no such weapon): " + s).withStyle(ChatFormatting.RED), false);
        return reports.size();
    }

    // ------------------------------------------------------------------------------------------------ operator commands
    private static ServerPlayer target(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        if (!HeroBonds.has(p)) ctx.getSource().sendFailure(Component.literal(p.getGameProfile().getName() + " has no hero weapon."));
        return HeroBonds.has(p) ? p : null;
    }

    private static HeroForm form(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        String id = StringArgumentType.getString(ctx, "form");
        HeroForm f = HeroForms.get(HeroBonds.kindOf(p), id);
        if (f == null) ctx.getSource().sendFailure(Component.literal("The " + HeroBonds.kindOf(p).display() + " has no form '" + id + "'."));
        return f;
    }

    private static int learn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = target(ctx);
        if (p == null) return 0;
        HeroKind kind = HeroBonds.kindOf(p);
        String which = StringArgumentType.getString(ctx, "form");
        if (which.equals("all")) {
            int n = 0;
            for (HeroForm f : HeroForms.of(kind)) if (!f.curse() && FormLearning.force(p, f)) n++;
            int learned = n;
            ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " learned " + learned + " ordinary form(s) of the " + kind.display() + "."), true);
            return learned;
        }
        HeroForm f = form(ctx, p);
        if (f == null) return 0;
        boolean fresh = FormLearning.force(p, f);
        ctx.getSource().sendSuccess(() -> Component.literal(fresh ? p.getGameProfile().getName() + " learned " + f.name() + "." : f.name() + " was already learned."), true);
        return fresh ? 1 : 0;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = target(ctx);
        if (p == null) return 0;
        HeroForm f = form(ctx, p);
        if (f == null) return 0;
        Component why = HeroForms.trySetActive(p, f.id());
        if (why != null) {
            ctx.getSource().sendFailure(why);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " is now in the form " + f.name() + "."), true);
        return 1;
    }

    private static int absorb(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = target(ctx);
        if (p == null) return 0;
        int taken = HeroAbsorb.fromHands(p);
        if (taken <= 0) {
            ctx.getSource().sendFailure(Component.literal("The gem took nothing: hold something one of the forms asks for (main hand, or the off hand if the main hand holds the weapon)."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("The gem of " + p.getGameProfile().getName() + " absorbed " + taken + " item(s)."), true);
        return taken;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = target(ctx);
        if (p == null) return 0;
        FormLearning.resetForms(p);
        ctx.getSource().sendSuccess(() -> Component.literal("The forms, mastery and meals of " + p.getGameProfile().getName() + " are reset; the weapon and its level are kept."), true);
        return 1;
    }

    private static int mastery(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = target(ctx);
        if (p == null) return 0;
        HeroForm f = form(ctx, p);
        if (f == null) return 0;
        if (f.mastery() <= 0) {
            ctx.getSource().sendFailure(Component.literal(f.name() + " has no mastery to earn."));
            return 0;
        }
        int n = IntegerArgumentType.getInteger(ctx, "n");
        FormMastery.set(p, f, n);
        ctx.getSource().sendSuccess(() -> Component.literal("Mastery of " + f.name() + " is now " + HeroBonds.of(p).mastery(f.id()) + "/" + f.mastery() + "."), true);
        return 1;
    }
}
