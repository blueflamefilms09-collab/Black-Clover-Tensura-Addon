package com.newuniverse.nusmp.dragonballs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import com.newuniverse.nusmp.dragonballs.voice.DbVoiceConfig;
import com.newuniverse.nusmp.dragonballs.voice.SpeechBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * The commands of the Dragon Balls. {@code /multiverse dragonballs ...} (operators, permission 2): {@code status}, {@code scatter <earth|super>}, {@code lock <set> <minutes>}, {@code unlock <set>},
 * {@code end <set>} (send the dragon away). {@code /dragonwish <id> [words]} is the clickable wish menu's command: it only works for the summoner of a running ritual, so every other sender gets a refusal.
 */
public final class DbCommands {
    private DbCommands() {}

    private static LiteralArgumentBuilder<CommandSourceStack> setArg(String name, java.util.function.BiFunction<CommandSourceStack, BallSet, Integer> run) {
        return Commands.literal(name).then(Commands.argument("set", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(BallSet.values()).map(BallSet::id), b))
                .executes(c -> {
                    BallSet s = BallSet.byId(StringArgumentType.getString(c, "set"));
                    if (s == null) { c.getSource().sendFailure(Component.literal("Unknown set. Use earth or super.")); return 0; }
                    return run.apply(c.getSource(), s);
                }));
    }

    /** The {@code dragonballs} node, merged under {@code /multiverse}. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("dragonballs").requires(c -> c.hasPermission(2))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(setArg("scatter", (src, s) -> {
                    if (DbSession.of(s) != null) { src.sendFailure(Component.literal("A ritual of that set is running: end it first.")); return 0; }
                    int n = DbManager.scatter(src.getServer(), s, false);
                    src.sendSuccess(() -> Component.literal("Scattered " + n + " " + s.ballName() + "s (they appear when a player loads the place; see status)."), true);
                    return n;
                }))
                .then(setArg("unlock", (src, s) -> {
                    DbManager.setLock(src.getServer(), s, 0);
                    src.sendSuccess(() -> Component.literal("The " + s.id() + " balls are no longer stone."), true);
                    return 1;
                }))
                .then(setArg("end", (src, s) -> {
                    DbSession x = DbSession.of(s);
                    if (x == null) { src.sendFailure(Component.literal("No ritual of that set is running.")); return 0; }
                    x.end(src.getServer(), "ended by an operator", true);
                    return 1;
                }))
                .then(Commands.literal("lock").then(Commands.argument("set", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(BallSet.values()).map(BallSet::id), b))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(0, (int) DbRules.MAX_LOCK_MINUTES)).executes(c -> {
                            BallSet s = BallSet.byId(StringArgumentType.getString(c, "set"));
                            if (s == null) { c.getSource().sendFailure(Component.literal("Unknown set. Use earth or super.")); return 0; }
                            int m = IntegerArgumentType.getInteger(c, "minutes");
                            DbManager.setLock(c.getSource().getServer(), s, m);
                            c.getSource().sendSuccess(() -> Component.literal("The " + s.id() + " balls are stone for " + m + " more minute(s)."), true);
                            return 1;
                        }))))
                // the relay for any voice-to-text mod, plugin or command block: the words count as if the player had said them (docs/dragon_balls_voice.md)
                .then(Commands.literal("say").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("words", StringArgumentType.greedyString()).executes(c -> {
                            ServerPlayer target = EntityArgument.getPlayer(c, "player");
                            if (!DbVoiceConfig.enabled()) { c.getSource().sendFailure(Component.translatable("anime_multiverse.dbvoice.cmd.off")); return 0; }
                            String words = StringArgumentType.getString(c, "words");
                            if (!SpeechBridge.heard(target.getUUID(), words)) { c.getSource().sendFailure(Component.translatable("anime_multiverse.dbvoice.cmd.fail", target.getDisplayName())); return 0; }
                            c.getSource().sendSuccess(() -> Component.translatable("anime_multiverse.dbvoice.cmd.ok", target.getDisplayName(), words), true);
                            return 1;
                        }))));
    }

    private static int status(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        DbData d = DbManager.data(server);
        for (BallSet s : BallSet.values()) {
            int o = s.ordinal();
            long rem = DbManager.lockRemainingMs(server, s);
            StringBuilder b = new StringBuilder(s.id()).append(": generation ").append(d.gen[o]).append(", ").append(rem > 0 ? "stone for " + DbRules.format(rem) : "open")
                    .append(DbSession.of(s) != null ? ", a ritual is running" : "").append(" | ");
            for (int star = 1; star <= DbConfig.count(); star++) {
                DbData.Ball ball = d.balls[o][star - 1];
                b.append(star).append("*=").append(ball == null ? "none" : (ball.pending ? "waiting for chunk " : "") + (int) ball.x + "," + (int) ball.z).append(star < DbConfig.count() ? "; " : "");
            }
            String line = b.toString();
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /** {@code /dragonwish}: the clickable menu of the summoner. */
    public static void registerWish(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("dragonwish")
                .then(Commands.argument("wish", StringArgumentType.word()).suggests(DbWishSuggest::wishes).executes(c -> wish(c.getSource(), StringArgumentType.getString(c, "wish")))
                        .then(Commands.argument("words", StringArgumentType.greedyString()).suggests(DbWishSuggest::words).executes(c -> wish(c.getSource(), StringArgumentType.getString(c, "wish") + " " + StringArgumentType.getString(c, "words"))))));
    }

    private static int wish(CommandSourceStack src, String text) {
        ServerPlayer p = src.getPlayer();
        DbSession s = p == null ? null : DbSession.byPlayer(p.getUUID());
        if (s == null) { src.sendFailure(DbMsg.bad("no_ritual")); return 0; }
        s.wish(p, text);
        return 1;
    }
}
