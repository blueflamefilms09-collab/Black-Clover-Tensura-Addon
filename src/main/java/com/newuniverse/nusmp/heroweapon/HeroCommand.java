package com.newuniverse.nusmp.heroweapon;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoan;
import com.newuniverse.nusmp.heroweapon.bond.HeroRegistry;
import com.newuniverse.nusmp.heroweapon.bond.HeroSync;
import com.newuniverse.nusmp.heroweapon.curse.CurseCommands;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.forms.FormsCommands;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.gem.GemCommands;
import com.newuniverse.nusmp.heroweapon.rules.HeroInterference;
import com.newuniverse.nusmp.heroweapon.skill.HeroUse;
import net.minecraft.ChatFormatting;
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
import java.util.UUID;

/**
 * Why: {@code /multiverse hero ...} is one command tree built from four parts so that four builders never edit one class: the core's own sub-commands and the
 * nodes of the forms, curse and gem systems. The core's are: {@code status [player]} (your own weapon, anyone; another player's, permission 2), {@code list}
 * (the 21 weapons and who holds each), {@code summon} and {@code dismiss} (yourself), and for operators (permission 2) {@code bind <player> <weapon>},
 * {@code release <player>}, {@code level <player> set <n>} and {@code registry [list | clear <weapon>]} (the register of who holds what; {@code clear} is the
 * way out when a holder's data is gone for good and a unique Holy Weapon is stuck on them). The weapon argument suggests the 21 item ids and accepts a short name
 * too ({@link HeroKind#parse}). Attached to the root by {@code MultiverseCommands} with one line.
 */
public final class HeroCommand {
    private HeroCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("hero")
                .then(Commands.literal("status").executes(ctx -> status(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(ctx -> status(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("list").executes(HeroCommand::list))
                .then(Commands.literal("summon").executes(ctx -> summon(ctx, true)))
                .then(Commands.literal("dismiss").executes(ctx -> summon(ctx, false)))
                .then(Commands.literal("bind").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("weapon", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(weaponIds(), b))
                                        .executes(HeroCommand::bind))))
                .then(Commands.literal("release").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(HeroCommand::release)))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("n", IntegerArgumentType.integer(0, HeroLevels.MAX)).executes(HeroCommand::setLevel)))))
                .then(Commands.literal("registry").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("list").executes(HeroCommand::list))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("weapon", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(weaponIds(), b))
                                        .executes(HeroCommand::registryClear))))
                .then(FormsCommands.node())
                .then(CurseCommands.node())
                .then(GemCommands.node());
    }

    // ================================================================ helpers
    private static List<String> weaponIds() {
        List<String> out = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) out.add(k.itemId());
        return out;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String text) { ctx.getSource().sendSuccess(() -> Component.literal(text), false); }

    private static void say(CommandContext<CommandSourceStack> ctx, Component text) { ctx.getSource().sendSuccess(() -> text, false); }

    private static int fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static HeroKind weapon(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "weapon");
        HeroKind k = HeroKind.parse(text);
        if (k == null) fail(ctx, "No such weapon '" + text + "'. Known: " + String.join(", ", weaponIds()));
        return k;
    }

    private static String nameOf(ServerPlayer p) { return p.getGameProfile().getName(); }

    // ================================================================ status and list
    private static int status(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            say(ctx, nameOf(p) + " has no hero weapon.");
            return 1;
        }
        HeroKind k = b.kind();
        int level = b.level();
        say(ctx, Component.literal(k.display() + " (" + k.heroClass().label() + ")  held by " + nameOf(p)).withStyle(ChatFormatting.GOLD));
        String next = level >= HeroLevels.MAX ? "the highest level" : (HeroLevels.expFor(level + 1) - b.exp()) + " to level " + (level + 1);
        say(ctx, " level " + level + ", experience " + b.exp() + " (" + next + ")");
        String formName = b.form();
        try {
            HeroForm f = HeroForms.active(b);
            if (f != null) formName = f.name() + " [" + f.id() + "]";
        } catch (RuntimeException unreadable) {
            // the forms system could not answer; the id will do
        }
        say(ctx, " form " + formName + ", " + b.unlocked().size() + " learned, " + b.absorbedMap().size() + " kinds absorbed");
        say(ctx, " the weapon is " + (b.summoned() ? "out" : "put away") + (HeroInterference.blocked(p) ? ", repelled by another Holy wielder (no experience)" : ""));
        try {
            say(ctx, " despair " + HeroCurse.despair(p) + ", corruption " + HeroCurse.corruption(p) + (HeroCurse.active(p) ? ", a curse form is active" : "")
                    + (HeroCurse.locked(p) ? ", the weapon holds its wielder" : ""));
        } catch (RuntimeException unreadable) {
            // the curse system could not answer
        }
        Map<String, Integer> running = HeroUse.running(p);
        if (!running.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> e : running.entrySet()) parts.add(e.getKey() + " " + String.format(java.util.Locale.ROOT, "%.1f", e.getValue() / 20.0) + " s");
            say(ctx, " cooling down: " + String.join(", ", parts));
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        int held = 0;
        for (HeroKind k : HeroKind.values()) {
            List<String> names = new ArrayList<>();
            for (UUID id : HeroRegistry.holdersOf(server, k)) names.add(HeroRegistry.nameOf(server, id));
            if (!names.isEmpty()) held++;
            say(ctx, " " + k.itemId() + "  " + k.display() + "  [" + k.heroClass().label() + "]  " + (names.isEmpty() ? "free" : "held by " + String.join(", ", names)));
        }
        say(ctx, held + " of " + HeroKind.values().length + " weapons are held.");
        return held;
    }

    // ================================================================ summon and dismiss
    private static int summon(CommandContext<CommandSourceStack> ctx, boolean out) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HeroLoan.Outcome o = out ? HeroLoan.summon(p, false) : HeroLoan.dismiss(p, false);
        if (o == HeroLoan.Outcome.OK) return 1;
        return fail(ctx, switch (o) {
            case NO_BOND -> "You have no hero weapon.";
            case ALREADY -> "The weapon is already out.";
            case NOT_OUT -> "The weapon is not out.";
            case EXEMPT -> "In creative or spectator mode a hero weapon is a plain item; there is nothing to summon.";
            case LOCKED -> "The curse holds you: the weapon cannot be put away now.";
            case DISABLED -> "The hero weapons are switched off on this server.";
            default -> "That cannot be done now.";
        });
    }

    // ================================================================ operators
    private static int bind(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        HeroKind kind = weapon(ctx);
        if (kind == null) return 0;
        String why = HeroBonds.bind(target, kind);
        if (why != null) return fail(ctx, "Cannot bind " + nameOf(target) + " to the " + kind.display() + ": " + why + ".");
        say(ctx, "Bound " + nameOf(target) + " to the " + kind.display() + ". They summon it with the summon key, or /multiverse hero summon.");
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        HeroKind was = HeroBonds.kindOf(target);
        if (!HeroBonds.release(target)) return fail(ctx, nameOf(target) + " has no hero weapon.");
        say(ctx, "Released " + nameOf(target) + " from the " + (was == null ? "weapon" : was.display()) + ". Its level, forms and mastery are gone.");
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int n = IntegerArgumentType.getInteger(ctx, "n");
        HeroBond b = HeroBonds.of(target);
        if (!b.isBound()) return fail(ctx, nameOf(target) + " has no hero weapon.");
        b.setExp(HeroLevels.expFor(n));
        HeroSync.sendNow(target);
        say(ctx, nameOf(target) + "'s " + b.kind().display() + " is now level " + n + " (" + b.exp() + " experience).");
        return 1;
    }

    private static int registryClear(CommandContext<CommandSourceStack> ctx) {
        HeroKind kind = weapon(ctx);
        if (kind == null) return 0;
        int n = HeroRegistry.clear(ctx.getSource().getServer(), kind);
        if (n == 0) return fail(ctx, "Nobody was recorded as holding the " + kind.display() + ".");
        say(ctx, "Cleared " + n + " holder(s) of the " + kind.display() + " from the register. A holder who is still bound re-registers at their next login.");
        return n;
    }
}
