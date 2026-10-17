package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the owner's hands on the floors and the lock, attached by the lead under {@code /multiverse mega} (permission 2): {@code floors here} says which
 * floor, road and band the operator stands in (the quickest way to see that the numbering matches the map), {@code player <target>} shows a player's
 * alignment and what the lock would let them do, {@code set / lock / unlock / reset <target>} are the repair tools (a player who locked themselves out
 * by mistake, a test of the second road, a wiped world), and {@code bands <sector>} / {@code assign <sector>} show and redo the numbering of a sector
 * (the only way to look at the banding without a game's worth of walking). Every change also resyncs the player's client. All text goes through lang keys
 * {@code message.nusmp.mega.mfl.cmd.*} with an English fallback, so the command reads fine before the lang fragment is merged. A translatable argument is
 * always a string, a number or a component (never an enum or another object): the game cannot send any other kind to a client, and the sender of a
 * command is usually a client.
 */
public final class FloorsCommand {
    private FloorsCommand() {}

    /** Longest list of room ids printed on one line of {@code bands}. */
    private static final int MAX_IDS = 40;

    /** The {@code floors} node: {@code here | player <t> | set <t> <road> | lock <t> | unlock <t> | reset <t> | bands <sector> | assign <sector>}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("floors").requires(s -> s.hasPermission(2))
                .then(Commands.literal("here").executes(ctx -> here(ctx.getSource())))
                .then(Commands.literal("player").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("set").then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("road", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[] {"holy", "abyssal"}, builder))
                                .executes(ctx -> choose(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "road"))))))
                .then(Commands.literal("lock").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> lock(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("unlock").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> unlock(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("reset").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("bands").then(Commands.argument("sector", IntegerArgumentType.integer(0))
                        .executes(ctx -> bands(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "sector")))))
                .then(Commands.literal("assign").then(Commands.argument("sector", IntegerArgumentType.integer(0))
                        .executes(ctx -> assign(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "sector")))));
    }

    private static MutableComponent text(String key, String english, Object... args) { return FloorsText.text("cmd." + key, english, args); }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    // ---------------------------------------------------------------- here
    private static int here(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        if (Labyrinth.inSanctuary(level)) return ok(src, text("here.hub", "You are on Floor 0, the Sanctuary Hub: no road, no lock."));
        if (!MegaDungeon.in(level)) return fail(src, text("here.outside", "You are not in the Mega-Dungeon (this is %s).", level.dimension().location().toString()));
        BlockPos pos = p.blockPosition();
        Sector s = MegaDungeon.sectorAt(level, pos);
        if (s == null) return ok(src, text("here.gap", "You are in the Mega-Dungeon but in no sector (a gap between two sectors). The tracker keeps floor %s for you.", FloorsText.floor(FloorTracker.currentFloor(p))));
        Room r = s.roomAt(pos);
        if (r == null) return ok(src, text("here.noroom", "Sector %s (%s road) but in no room (a door cap, or the air above the rooms). The tracker keeps floor %s for you.", s.id, s.polarity.id(), FloorsText.floor(FloorTracker.currentFloor(p))));
        int floor = r.floor != 0 ? r.floor : s.firstFloor();
        ok(src, text("here.line", "Floor %s: band %s of %s, %s, sector %s (chain index %s), room #%s (%s), threat %s.",
                FloorsText.floor(floor), FloorMap.band(floor, s.perRegion), s.perRegion, FloorsText.road(s.polarity), s.id, s.chainIndex, r.id, r.kind.name().toLowerCase(Locale.ROOT), FloorMap.threat(floor)));
        if (r.floor == 0) ok(src, text("here.unnumbered", "This room has no floor number yet (the sector was never numbered): run floors assign %s.", s.id));
        if (FloorMap.isDecennial(floor, s.perRegion)) ok(src, text("here.boss", "This is a boss floor (the last of its sector)."));
        return ok(src, text("here.tracker", "The tracker has you on floor %s.", FloorsText.floor(FloorTracker.currentFloor(p))));
    }

    // ---------------------------------------------------------------- a player's state
    private static int show(CommandSourceStack src, ServerPlayer target) {
        PlayerDungeonAlignment a = AlignmentAttachments.get(target);
        String name = target.getGameProfile().getName();
        ok(src, text("player.line", "%s: %s, %s; deepest reached: Holy %s, Abyssal %s; the tracker has them on floor %s.",
                name, FloorsText.road(a.chosen()), a.locked() ? text("player.locked", "LOCKED (since game tick %s)", a.lockedAt()) : text("player.open", "not locked"),
                a.deepestHoly(), a.deepestAbyssal(), FloorsText.floor(FloorTracker.currentFloor(target))));
        ok(src, text("player.may", "The lock is %s. The Holy road is %s to them, the Abyssal road is %s to them.",
                FloorsConfig.get(FloorsConfig.LOCK_ENABLED) ? text("state.on", "on") : text("state.off", "OFF in the config"),
                LockPolicy.allows(target, Polarity.HOLY) ? text("state.open", "open") : text("state.shut", "shut"),
                LockPolicy.allows(target, Polarity.ABYSSAL) ? text("state.open", "open") : text("state.shut", "shut")));
        if (LockPolicy.bypasses(target)) ok(src, text("player.op", "They are an operator and pass the lock (floors.opsBypass)."));
        return 1;
    }

    private static int choose(CommandSourceStack src, ServerPlayer target, String word) {
        Polarity road = Polarity.fromId(word.trim().toLowerCase(Locale.ROOT));
        if (road == null) return fail(src, text("set.unknown", "'%s' is not a road. Use holy or abyssal.", word));
        PlayerDungeonAlignment a = AlignmentAttachments.get(target);
        boolean changed = AlignmentRules.choose(a, road);
        AlignmentNet.send(target);
        String name = target.getGameProfile().getName();
        ok(src, changed ? text("set.ok", "%s is now on the %s.", name, FloorsText.road(road)) : text("set.same", "%s was on the %s already.", name, FloorsText.road(road)));
        if (a.locked()) ok(src, text("set.locked", "They are locked, so the lock now closes the other road."));
        return 1;
    }

    private static int lock(CommandSourceStack src, ServerPlayer target) {
        PlayerDungeonAlignment a = AlignmentAttachments.get(target);
        if (a.chosen() == null) return fail(src, text("lock.noroad", "%s has no road yet: use floors set first.", target.getGameProfile().getName()));
        if (!AlignmentRules.lock(a, target.level().getGameTime())) return fail(src, text("lock.already", "%s is locked already.", target.getGameProfile().getName()));
        AlignmentNet.send(target);
        return ok(src, text("lock.ok", "%s is locked to the %s.", target.getGameProfile().getName(), FloorsText.road(a.chosen())));
    }

    private static int unlock(CommandSourceStack src, ServerPlayer target) {
        PlayerDungeonAlignment a = AlignmentAttachments.get(target);
        if (!AlignmentRules.unlock(a)) return fail(src, text("unlock.not", "%s is not locked.", target.getGameProfile().getName()));
        AlignmentNet.send(target);
        return ok(src, text("unlock.ok", "%s is free to take either road again (their road, the %s, is kept).", target.getGameProfile().getName(), FloorsText.road(a.chosen())));
    }

    private static int reset(CommandSourceStack src, ServerPlayer target) {
        AlignmentRules.reset(AlignmentAttachments.get(target));
        AlignmentNet.send(target);
        return ok(src, text("reset.ok", "%s has no road, no lock and no record of the floors any more.", target.getGameProfile().getName()));
    }

    // ---------------------------------------------------------------- a sector's numbering
    private static Sector sector(CommandSourceStack src, int id) {
        return MegaData.get(src.getServer()).sector(id);
    }

    private static int bands(CommandSourceStack src, int id) {
        Sector s = sector(src, id);
        if (s == null) return fail(src, text("sector.none", "There is no sector %s.", id));
        if (s.rooms.isEmpty()) return fail(src, text("bands.empty", "Sector %s has no rooms.", id));
        Room landing = FloorAssigner.landingOf(s), arena = FloorAssigner.arenaOf(s);
        List<FloorBands.Node> nodes = new ArrayList<>();
        int lowest = Integer.MAX_VALUE;
        for (Room r : s.rooms) {
            nodes.add(new FloorBands.Node(r.id, r.kind));
            lowest = Math.min(lowest, r.id);
        }
        int root = landing != null ? landing.id : lowest;                              // the same root the numbering uses
        Map<Integer, Integer> depth = FloorBands.depths(nodes, root, s.edges);
        ok(src, text("bands.head", "Sector %s (%s, chain index %s, state %s): %s rooms, %s floors per sector, floors %s to %s. Landing #%s, arena %s.",
                s.id, s.polarity.id(), s.chainIndex, s.state.name(), s.rooms.size(), s.perRegion, FloorsText.floor(s.firstFloor()), FloorsText.floor(s.lastFloor()),
                landing == null ? "?" : landing.id, arena == null ? "none" : "#" + arena.id + " (depth " + depth.get(arena.id) + ")"));
        TreeMap<Integer, List<Room>> byFloor = new TreeMap<>((x, y) -> Integer.compare(order(x), order(y)));
        for (Room r : s.rooms) byFloor.computeIfAbsent(r.floor, k -> new ArrayList<>()).add(r);
        for (Map.Entry<Integer, List<Room>> e : byFloor.entrySet()) {
            StringBuilder ids = new StringBuilder();
            int shown = 0;
            for (Room r : e.getValue()) {
                if (shown++ >= MAX_IDS) { ids.append(" ..."); break; }
                if (ids.length() > 0) ids.append(' ');
                ids.append('#').append(r.id).append(':').append(r.kind.name().toLowerCase(Locale.ROOT)).append('@').append(depth.get(r.id));
            }
            ok(src, text("bands.line", "%s: %s rooms: %s", e.getKey() == 0 ? "unnumbered" : FloorsText.floor(e.getKey()), e.getValue().size(), ids.toString()));
        }
        return s.rooms.size();
    }

    /** Sorts the floors of one sector from the nearest to Floor 0 outwards, the rooms that were never numbered (floor 0) last. */
    private static int order(int floor) { return floor == 0 ? Integer.MAX_VALUE : Math.abs(floor); }

    private static int assign(CommandSourceStack src, int id) {
        MegaData data = MegaData.get(src.getServer());
        Sector s = data.sector(id);
        if (s == null) return fail(src, text("sector.none", "There is no sector %s.", id));
        if (s.rooms.isEmpty()) return fail(src, text("bands.empty", "Sector %s has no rooms.", id));
        int changed = FloorAssigner.assign(s, data);
        data.setDirty();
        return ok(src, text("assign.ok", "Sector %s numbered again: %s of %s rooms changed floor, floors %s to %s (%s per sector).",
                s.id, changed, s.rooms.size(), FloorsText.floor(s.firstFloor()), FloorsText.floor(s.lastFloor()), s.perRegion));
    }
}
