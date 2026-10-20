package com.newuniverse.nusmp.mana.zone;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /multiverse zone list | info [id] | tp <id> [player]} (operators): the Grand Magic Zones, the places of dense, unstable natural mana. {@code tp} puts you on the
 * surface above the zone's heart (or at its {@code tp} point), {@code info} shows its theme, effects, density and the EP it asks, and how it treats you.
 */
public final class GrandZoneCommands {
    private GrandZoneCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("zone").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(GrandZoneCommands::list))
                .then(Commands.literal("info").executes(ctx -> info(ctx, null))
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> { for (var r : GrandZones.all()) b.suggest(r.id()); return b.buildFuture(); })
                                .executes(ctx -> info(ctx, StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("bind").then(Commands.argument("key", StringArgumentType.word()).then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> { for (var r : GrandZones.all()) b.suggest(r.id()); return b.buildFuture(); })
                        .then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(GrandZoneCommands::bind))))))
                .then(Commands.literal("unbind").then(Commands.argument("key", StringArgumentType.word()).executes(ctx -> {
                    boolean had = GrandZones.unbind(StringArgumentType.getString(ctx, "key"));
                    send(ctx, had ? "Released." : "No such binding.", ChatFormatting.GRAY);
                    return had ? 1 : 0;
                })))
                .then(Commands.literal("tp").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> { for (var r : GrandZones.all()) b.suggest(r.id()); return b.buildFuture(); })
                        .executes(ctx -> tp(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> tp(ctx, EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var all = GrandZones.all();
        if (all.isEmpty()) { send(ctx, "No Grand Magic Zones are loaded (data/<namespace>/grand_magic_zones/*.json).", ChatFormatting.GRAY); return 0; }
        for (GrandZoneRegion r : all)
            send(ctx, r.id() + " | " + r.name() + " | " + r.dimension() + " " + at(r) + " r" + (int) r.effectiveRadius() + " | EP " + String.format(Locale.ROOT, "%,d", (long) r.recommendedEp())
                    + " | density " + r.density() + " | " + r.theme().name().toLowerCase(Locale.ROOT), ChatFormatting.LIGHT_PURPLE);
        return all.size();
    }

    private static int info(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        GrandZoneRegion r;
        ServerPlayer p = ctx.getSource().getPlayer();
        if (id == null) {
            r = p == null ? null : GrandZones.regionAt(p.level(), p.blockPosition());
            if (r == null) { send(ctx, "You are not inside a Grand Magic Zone. Use /multiverse zone info <id>.", ChatFormatting.GRAY); return 0; }
        } else {
            r = GrandZones.byId(id);
            if (r == null) { ctx.getSource().sendFailure(Component.literal("No such zone: " + id)); return 0; }
        }
        send(ctx, r.name() + " (" + r.id() + "): " + r.theme().name().toLowerCase(Locale.ROOT) + " in " + r.dimension() + " at " + at(r) + ", radius " + (int) r.effectiveRadius(), ChatFormatting.LIGHT_PURPLE);
        send(ctx, "Density " + r.density() + ", recommended EP " + String.format(Locale.ROOT, "%,d", (long) r.recommendedEp()) + ", effects: "
                + r.effects().stream().map(e -> e.name().toLowerCase(Locale.ROOT)).sorted().collect(Collectors.joining(", ")), ChatFormatting.GRAY);
        if (p != null) {
            var ex = GrandZones.exposure(p, r);
            send(ctx, "For you: harm " + String.format(Locale.ROOT, "%.2f", ex.harm()) + ", boon " + String.format(Locale.ROOT, "%.2f", ex.boon())
                    + (ManaZoneManager.active(p) ? " (your Mana Zone halves the harm)" : ""), ChatFormatting.AQUA);
        }
        return 1;
    }

    /** {@code /multiverse zone bind <key> <id> <from> <to>}: gives the box between two corners, in this dimension, the environment of zone {@code id} (the dungeon's hook by hand; not saved). */
    private static int bind(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var a = BlockPosArgument.getBlockPos(ctx, "from");
        var b = BlockPosArgument.getBlockPos(ctx, "to");
        String key = StringArgumentType.getString(ctx, "key"), id = StringArgumentType.getString(ctx, "id");
        String dim = ctx.getSource().getLevel().dimension().location().toString();
        boolean ok = GrandZones.bind(key, id, dim, a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
        if (!ok) { ctx.getSource().sendFailure(Component.literal("No such zone to copy: " + id)); return 0; }
        send(ctx, "Bound " + key + " to the box in " + dim + " with the environment of " + id + " (not saved: a floor binds itself when it loads).", ChatFormatting.LIGHT_PURPLE);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx, ServerPlayer who) {
        String id = StringArgumentType.getString(ctx, "id");
        GrandZoneRegion r = GrandZones.byId(id);
        if (r == null) { ctx.getSource().sendFailure(Component.literal("No such zone: " + id)); return 0; }
        ResourceLocation dim = ResourceLocation.tryParse(r.dimension());
        ServerLevel level = dim == null ? null : ctx.getSource().getServer().getLevel(ResourceKey.<Level>create(Registries.DIMENSION, dim));
        if (level == null) { ctx.getSource().sendFailure(Component.literal("The dimension " + r.dimension() + " is not loaded.")); return 0; }
        double x = r.tp() != null ? r.tp()[0] : r.cx(), z = r.tp() != null ? r.tp()[2] : r.cz();
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);                 // load the spot before asking its height
        double y = r.tp() != null ? r.tp()[1] : Math.max(level.getMinBuildHeight() + 2, level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z)) + 1);
        who.teleportTo(level, x, y, z, who.getYRot(), who.getXRot());
        send(ctx, who.getName().getString() + " is at " + r.name() + " (" + (int) x + ", " + (int) y + ", " + (int) z + ").", ChatFormatting.LIGHT_PURPLE);
        return 1;
    }

    private static String at(GrandZoneRegion r) { return "(" + (int) r.cx() + ", " + (int) r.cy() + ", " + (int) r.cz() + ")"; }

    private static void send(CommandContext<CommandSourceStack> ctx, String msg, ChatFormatting f) { ctx.getSource().sendSuccess(() -> Component.literal(msg).withStyle(f), false); }
}
