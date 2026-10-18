package com.newuniverse.nusmp.elite.world;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.elite.world.calamity.CalamityManager;
import com.newuniverse.nusmp.elite.world.item.CalamityCoreItem;
import com.newuniverse.nusmp.elite.world.item.MagiculeGaugeItem;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/**
 * Why: the operator's tools of the world engine under {@code /multiverse elite}: {@code magicule get} (everyone: the reading where you stand), {@code magicule set <0..1> [radius]} and
 * {@code magicule bump <delta> [radius]} (operators: force a density, or add an event bump that fades), and {@code calamity status|list|summon <id> [seconds]|core <id> [player]|clear}.
 * The literal {@code multiverse} and {@code elite} are shared with the other builders of the elite systems: Brigadier merges nodes of the same name, so each registers its own branch.
 */
public final class WorldCommands {
    private WorldCommands() {}

    private static boolean op(CommandSourceStack s) { return s.hasPermission(2); }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> magicule = Commands.literal("magicule")
                .then(Commands.literal("get").executes(WorldCommands::get))
                .then(Commands.literal("set").requires(WorldCommands::op)
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0)).executes(c -> set(c, 0))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 8)).executes(c -> set(c, IntegerArgumentType.getInteger(c, "radius"))))))
                .then(Commands.literal("bump").requires(WorldCommands::op)
                        .then(Commands.argument("delta", DoubleArgumentType.doubleArg(-0.5, 0.8)).executes(c -> bump(c, 0))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 8)).executes(c -> bump(c, IntegerArgumentType.getInteger(c, "radius"))))));

        LiteralArgumentBuilder<CommandSourceStack> calamity = Commands.literal("calamity")
                .then(Commands.literal("status").executes(WorldCommands::status))
                .then(Commands.literal("list").executes(WorldCommands::list))
                .then(Commands.literal("summon").requires(WorldCommands::op)
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> ids(b)).executes(c -> summon(c, -1))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600)).executes(c -> summon(c, IntegerArgumentType.getInteger(c, "seconds"))))))
                .then(Commands.literal("core").requires(WorldCommands::op)
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> ids(b)).executes(WorldCommands::core)))
                .then(Commands.literal("clear").requires(WorldCommands::op).executes(WorldCommands::clear));

        event.getDispatcher().register(Commands.literal("multiverse").then(Commands.literal("elite").then(magicule).then(calamity)));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> ids(com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(WorldData.calamities().stream().map(CalamityDef::id), b);
    }

    // ---------------------------------------------------------------- magicule

    private static int get(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        LevelChunk ch = MagiculeApi.loaded(level, p.blockPosition());
        ChunkMagicule d = ch == null ? null : MagiculeApi.existing(ch);
        double eff = MagiculeApi.effectiveAt(p);
        Component head = MagiculeGaugeItem.reading(level, p);
        c.getSource().sendSuccess(() -> head, false);
        if (d != null && d.initialised()) {
            Component more = Component.translatable("nusmp.elite.magicule.detail", String.format(Locale.ROOT, "%.3f", d.density), String.format(Locale.ROOT, "%+.3f", d.event), d.crystalScore, d.mothers.size(), d.ore.total,
                    MagiculeModel.Band.of(eff).id);
            c.getSource().sendSuccess(() -> more, false);
        }
        return (int) Math.round(eff * 100.0);
    }

    private static int set(CommandContext<CommandSourceStack> c, int radius) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        if (!WorldGuard.allowed(level)) {
            c.getSource().sendFailure(Component.translatable("nusmp.elite.magicule.off"));
            return 0;
        }
        double v = DoubleArgumentType.getDouble(c, "value");
        int n = 0;
        ChunkPos cp = p.chunkPosition();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk ch = MagiculeApi.loaded(level, new ChunkPos(cp.x + dx, cp.z + dz));
                if (ch == null) continue;
                MagiculeApi.force(level, ch, v);
                n++;
            }
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.translatable("nusmp.elite.magicule.set", String.format(Locale.ROOT, "%.2f", v), count), true);
        return n;
    }

    private static int bump(CommandContext<CommandSourceStack> c, int radius) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        if (!WorldGuard.allowed(level)) {
            c.getSource().sendFailure(Component.translatable("nusmp.elite.magicule.off"));
            return 0;
        }
        double delta = DoubleArgumentType.getDouble(c, "delta");
        MagiculeApi.bump(level, p.blockPosition(), delta, radius);
        c.getSource().sendSuccess(() -> Component.translatable("nusmp.elite.magicule.bumped", String.format(Locale.ROOT, "%+.2f", delta), radius), true);
        return 1;
    }

    // ---------------------------------------------------------------- calamity

    private static int status(CommandContext<CommandSourceStack> c) {
        var lines = CalamityManager.status(c.getSource().getServer());
        for (Component l : lines) c.getSource().sendSuccess(() -> l, false);
        return lines.size();
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (CalamityDef d : WorldData.calamities()) {
            String line = d.id() + " (" + d.entity() + ", " + (d.enabled() ? "on" : "off") + ", " + d.triggers().size() + " triggers)";
            c.getSource().sendSuccess(() -> Component.literal(line), false);
            n++;
        }
        return n;
    }

    private static int summon(CommandContext<CommandSourceStack> c, int seconds) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "id");
        Component err = CalamityManager.start(p.serverLevel(), p, id, "command", seconds);
        if (err != null) {
            c.getSource().sendFailure(err);
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("nusmp.elite.calamity.summoned", id), true);
        return 1;
    }

    private static int core(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "id");
        if (WorldData.calamity(id) == null) {
            c.getSource().sendFailure(Component.translatable("nusmp.elite.calamity.unknown", id));
            return 0;
        }
        p.getInventory().placeItemBackInInventory(CalamityCoreItem.stackFor(id));
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c) {
        int n = CalamityManager.clearAll(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.translatable("nusmp.elite.calamity.cleared", n), true);
        return n;
    }
}
