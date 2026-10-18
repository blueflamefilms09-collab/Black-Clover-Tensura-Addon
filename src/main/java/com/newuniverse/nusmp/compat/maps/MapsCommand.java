package com.newuniverse.nusmp.compat.maps;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Why: the two commands of the map-mod compatibility. {@code /multiverse waypoint share [on|off|here|gate|return]} (everybody): the switch and the explicit lines;
 * {@code /multiverse compat maps} (operators): a one-page status of which map mods were seen and what the mod did for each, the dimension profiles, the sharing switches and the
 * FTB Chunks claim check. Attach {@link #waypointNode()} to the {@code multiverse} root and {@link #compatMapsNode()} under {@code compat}.
 * Note for the status page: a CLIENT-only map mod (Xaero's, JourneyMap, VoxelMap) is not in a dedicated server's mod list, so on a dedicated server it is reported as not seen;
 * on a single-player world (integrated server) the client's mod list is the server's.
 */
public final class MapsCommand {
    private MapsCommand() {}

    static MutableComponent t(String key, String english, Object... args) { return Component.translatableWithFallback("message.nusmp.maps." + key, english, args); }

    public static LiteralArgumentBuilder<CommandSourceStack> waypointNode() {
        return Commands.literal("waypoint")
                .then(Commands.literal("share")
                        .executes(MapsCommand::shareState)
                        .then(Commands.literal("on").executes(ctx -> setShare(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setShare(ctx, false)))
                        .then(Commands.literal("here").executes(MapsCommand::shareHere))
                        .then(Commands.literal("gate").executes(MapsCommand::shareGate))
                        .then(Commands.literal("return").executes(MapsCommand::shareReturn)));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> compatMapsNode() {
        return Commands.literal("maps").requires(s -> s.hasPermission(2)).executes(MapsCommand::status);
    }

    // ---------------------------------------------------------------- waypoint share
    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException notAPlayer) {
            ctx.getSource().sendFailure(t("player_only", "Only a player has a position to share."));
            return null;
        }
    }

    private static int shareState(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        if (p == null) return 0;
        boolean server = MapsConfig.get(MapsConfig.SHARE_WAYPOINTS), mine = MapShare.prefOn(p);
        ctx.getSource().sendSuccess(() -> t("share.state", "Waypoint lines: %1$s for you (server: %2$s). /multiverse waypoint share on|off|here|gate|return",
                mine ? "ON" : "OFF", server ? "on" : "off").withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int setShare(CommandContext<CommandSourceStack> ctx, boolean on) {
        ServerPlayer p = player(ctx);
        if (p == null) return 0;
        MapShare.setPref(p, on);
        ctx.getSource().sendSuccess(() -> t(on ? "share.on" : "share.off", on ? "You will get waypoint lines for places that matter (gates, rifts, safe rooms)." : "You will not get automatic waypoint lines any more.")
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int shareHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        if (p == null) return 0;
        MapShare.send(p, MapShare.here(p));
        return 1;
    }

    private static int shareGate(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        if (p == null) return 0;
        List<MapShare.Place> gates = MapShare.knownGates(p);
        if (gates.isEmpty()) {
            ctx.getSource().sendSuccess(() -> t("share.no_gate", "You do not know a Gate yet: walk through the Labyrinth Gateway first.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        for (MapShare.Place g : gates) MapShare.send(p, g);
        return gates.size();
    }

    private static int shareReturn(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        if (p == null) return 0;
        MapShare.Place r = MapShare.returnPlace(p);
        if (r == null) {
            ctx.getSource().sendSuccess(() -> t("share.no_return", "You have no Return Waypoint yet: stand somewhere in the dungeon first.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        MapShare.send(p, r);
        return 1;
    }

    // ---------------------------------------------------------------- /multiverse compat maps
    private static void line(CommandContext<CommandSourceStack> ctx, Component c) { ctx.getSource().sendSuccess(() -> c, false); }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ModList list = ModList.get();
        List<MapMods.Found> found = MapMods.detect(list == null ? null : list::isLoaded);
        line(ctx, t("status.header", "Map mods (compat.maps)").withStyle(ChatFormatting.GOLD));
        if (found.isEmpty()) line(ctx, t("status.none", " No map mod seen by this side. (A client-only map mod such as Xaero's is invisible to a dedicated server; the data below still works for it.)").withStyle(ChatFormatting.GRAY));
        for (MapMods.Found f : found)
            line(ctx, t("status.mod", " %1$s (%2$s): %3$s", f.mod().name(), f.id(), f.mod().does()).withStyle(ChatFormatting.GREEN));
        MapsSelfTests.Colours colours = MapsSelfTests.blockColours();
        if (colours.missing().isEmpty())
            line(ctx, t("status.colours", " Block map colours: all %1$s custom blocks have a mapColor.", colours.total()).withStyle(ChatFormatting.GRAY));
        else
            line(ctx, t("status.colours_bad", " Block map colours: %1$s of %2$s custom blocks have NO mapColor (a vanilla map looks through them): %3$s. Re-run MapBlockColourGuardTest and fix the registration.",
                    colours.missing().size(), colours.total(), String.join(", ", colours.missing())).withStyle(ChatFormatting.RED));
        for (DimensionAudit.Report r : DimensionAudit.auditAll(MapsCommand.class.getClassLoader())) {
            line(ctx, t("status.dim", " %1$s: %2$s, y %3$s..%4$s%5$s", r.id(), r.profile().text, r.minY(), r.minY() + r.height(), r.ok() ? "" : "  PROBLEMS: " + r.problems())
                    .withStyle(r.ok() ? ChatFormatting.DARK_GRAY : ChatFormatting.RED));
        }
        boolean cfg = MapsConfig.get(MapsConfig.SHARE_WAYPOINTS);
        line(ctx, t("status.share", " Waypoint lines: %1$s (raw Xaero's string %2$s, JourneyMap form %3$s, at most %4$s per player per minute). Players: /multiverse waypoint share on|off|here|gate|return.",
                cfg ? "on" : "off", MapsConfig.get(MapsConfig.RAW_XAERO_STRING) ? "on" : "off", MapsConfig.get(MapsConfig.SHARE_JOURNEYMAP_FORM) ? "on" : "off", MapsConfig.get(MapsConfig.MAX_LINES_PER_MINUTE)).withStyle(ChatFormatting.GRAY));
        if (FtbChunksGuard.ftbChunksLoaded()) {
            line(ctx, t("status.ftb_block", " FTB Chunks claim block: %1$s (config blockFtbClaims = %2$s, refused in: %3$s).", FtbClaimBlock.active() ? "ACTIVE" : "not active",
                    MapsConfig.get(MapsConfig.BLOCK_FTB_CLAIMS) ? "on" : "off", MapsConfig.get(MapsConfig.FTB_BLOCKED_DIMENSIONS))
                    .withStyle(FtbClaimBlock.active() && MapsConfig.get(MapsConfig.BLOCK_FTB_CLAIMS) ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            FtbChunksCheck.Result r = FtbChunksGuard.evaluate(src.getServer());
            switch (r.status()) {
                case COVERED -> line(ctx, t("status.ftb_ok", " FTB Chunks: claims are blocked in this mod's dimensions (%1$s).", r.source()).withStyle(ChatFormatting.GREEN));
                case OPEN -> {
                    line(ctx, t("status.ftb_open", " FTB Chunks WARNING: players can claim chunks in %1$s.", String.join(", ", r.uncovered())).withStyle(ChatFormatting.GOLD));
                    line(ctx, t("status.ftb_fix", " Add to FTB Chunks' world config (%1$s): %2$s", r.source(), FtbChunksCheck.suggestion(r.uncovered())).withStyle(ChatFormatting.GOLD));
                }
                default -> line(ctx, t("status.ftb_unknown", " FTB Chunks: its world config (%1$s) was not found, so claims in this mod's dimensions cannot be checked. Make sure it has: %2$s",
                        FtbChunksCheck.FILE, FtbChunksCheck.suggestion(FtbChunksGuard.ourDimensions())).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            line(ctx, t("status.ftb_absent", " FTB Chunks: not loaded on this side.").withStyle(ChatFormatting.GRAY));
        }
        line(ctx, t("status.test", " Self-test: /multiverse mega selftest compat.maps. Details: docs/compat/maps.md.").withStyle(ChatFormatting.DARK_GRAY));
        return found.size();
    }
}
