package com.newuniverse.nusmp.heroweapon.curse;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * Why: the curse system's part of {@code /multiverse hero ...}, attached by the core's {@code HeroCommand}. A player may read their own curse ({@code show}) and
 * answer the weapon's offer ({@code embrace}, {@code decline}) from the keyboard; an operator (permission 2) may read anyone's curse and set every part of it
 * ({@code despair}, {@code corruption}, {@code unlock}, {@code offer}, {@code clear}, {@code purify}) so the whole system can be tried without waiting for a tragedy.
 * Each command is a few lines that call {@link CurseAdmin} (which does exactly what the game's own events do) and say what happened; a player without a hero weapon
 * is refused with that reason.
 *
 * <pre>
 * /multiverse hero curse show [player]
 * /multiverse hero curse embrace | decline
 * /multiverse hero curse despair|corruption &lt;player&gt; &lt;0..1000&gt;
 * /multiverse hero curse unlock &lt;player&gt; &lt;sin&gt; [tax]
 * /multiverse hero curse offer &lt;player&gt; &lt;sin&gt;
 * /multiverse hero curse clear|purify &lt;player&gt;
 * </pre>
 */
public final class CurseCommands {
    private CurseCommands() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("curse")
                .then(Commands.literal("show").executes(ctx -> show(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(CurseCommands::admin).executes(ctx -> show(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("embrace").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    return CurseSystem.embrace(p) ? 1 : 0;
                }))
                .then(Commands.literal("decline").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    return CurseSystem.decline(p) ? 1 : 0;
                }))
                .then(Commands.literal("despair").requires(CurseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, CurseTuning.MAX)).executes(ctx -> {
                            ServerPlayer p = bonded(ctx);
                            if (p == null) return 0;
                            CurseAdmin.setDespair(p, IntegerArgumentType.getInteger(ctx, "amount"), CurseService.now(p), CurseEvents.tuning(p.getServer()));
                            return ok(ctx, p.getGameProfile().getName() + " despair set to " + IntegerArgumentType.getInteger(ctx, "amount") + ".");
                        }))))
                .then(Commands.literal("corruption").requires(CurseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, CurseTuning.MAX)).executes(ctx -> {
                            ServerPlayer p = bonded(ctx);
                            if (p == null) return 0;
                            CurseAdmin.setCorruption(p, IntegerArgumentType.getInteger(ctx, "amount"), CurseService.now(p), CurseEvents.tuning(p.getServer()));
                            return ok(ctx, p.getGameProfile().getName() + " corruption set to " + IntegerArgumentType.getInteger(ctx, "amount") + ".");
                        }))))
                .then(Commands.literal("unlock").requires(CurseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("sin", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(sinIds(), b))
                                .executes(ctx -> unlock(ctx, false))
                                .then(Commands.literal("tax").executes(ctx -> unlock(ctx, true))))))
                .then(Commands.literal("offer").requires(CurseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("sin", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(sinIds(), b)).executes(ctx -> {
                            ServerPlayer p = bonded(ctx);
                            Sin sin = sinArg(ctx);
                            if (p == null || sin == null) return 0;
                            String why = CurseAdmin.offer(p, sin, CurseService.now(p), CurseEvents.tuning(p.getServer()));
                            return why == null ? ok(ctx, "The weapon of " + p.getGameProfile().getName() + " offers " + sin.label() + ".") : fail(ctx, why);
                        }))))
                .then(Commands.literal("clear").requires(CurseCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = bonded(ctx);
                    if (p == null) return 0;
                    CurseAdmin.clear(p, CurseService.now(p), CurseEvents.tuning(p.getServer()));
                    return ok(ctx, "The curse of " + p.getGameProfile().getName() + " is wiped: series, tax, despair, corruption, offer.");
                })))
                .then(Commands.literal("purify").requires(CurseCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = bonded(ctx);
                    if (p == null) return 0;
                    boolean done = CurseAdmin.purify(p, CurseService.now(p), CurseEvents.tuning(p.getServer()));
                    return done ? ok(ctx, p.getGameProfile().getName() + " is purified.") : fail(ctx, "Nothing to purify for " + p.getGameProfile().getName() + ".");
                })));
    }

    private static int show(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        for (String line : CurseAdmin.show(p, CurseService.now(p), CurseEvents.tuning(p.getServer()))) ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int unlock(CommandContext<CommandSourceStack> ctx, boolean tax) throws CommandSyntaxException {
        ServerPlayer p = bonded(ctx);
        Sin sin = sinArg(ctx);
        if (p == null || sin == null) return 0;
        String why = CurseAdmin.unlock(p, sin, tax, CurseService.now(p), CurseEvents.tuning(p.getServer()));
        return why == null ? ok(ctx, p.getGameProfile().getName() + " has embraced " + sin.label() + (tax ? " (with the tax)." : " (as a gift, no tax).")) : fail(ctx, why);
    }

    /** The player argument, or null (with the reason shown) when they have no hero weapon. */
    private static ServerPlayer bonded(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
            if (HeroBonds.has(p)) return p;
            fail(ctx, p.getGameProfile().getName() + " has no hero weapon.");
        } catch (CommandSyntaxException e) {
            fail(ctx, "No such player.");
        }
        return null;
    }

    private static Sin sinArg(CommandContext<CommandSourceStack> ctx) {
        Sin sin = CurseAdmin.sinOf(StringArgumentType.getString(ctx, "sin"));
        if (sin == null) fail(ctx, "Unknown sin. Use one of: " + String.join(", ", sinIds()) + ".");
        return sin;
    }

    private static Iterable<String> sinIds() { return Arrays.stream(Sin.values()).map(Sin::id).toList(); }

    private static int ok(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
