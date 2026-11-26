package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Why: {@code /multiverse mega party ...} is how an admin looks at the parties of the dungeon and puts them right, and how the headless CI smoke test checks them through the
 * server console. {@code list} prints the mode and one line per party (its lane index, whether it is a team or a player alone, its name, the floors it has built on each road
 * and how many of it are inside now), {@code of <player>} says which party a player belongs to (an offline player too: FTB Teams is asked by id), {@code tp <index> [holy|abyssal]}
 * takes the admin to a party's first floor to look at it, and {@code set <player> <index>} puts a player's party (their team, if they have one) on a lane, which is how two groups
 * that must share floors are merged, a party is moved, or a party is given the floors of a world saved before parties (index 0). Permission 2 like the rest of the tree; every
 * line is built by {@link PartyLines} so the smoke test can read it; the words are literals, the pattern of the generator's command. The lead attaches it under {@code mega}.
 */
public final class PartyCommand {
    private PartyCommand() {}

    private static int say(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int no(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("party").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("of").then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> of(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player")))))
                .then(Commands.literal("lanes").executes(ctx -> lanes(ctx.getSource()))
                        .then(Commands.literal("free").then(Commands.argument("index", IntegerArgumentType.integer(1, 255)).executes(ctx -> free(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"))))))
                .then(Commands.literal("tp").then(Commands.argument("index", IntegerArgumentType.integer(0, 255)).executes(ctx -> tp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), null))
                        .then(Commands.literal("holy").executes(ctx -> tp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), Polarity.HOLY)))
                        .then(Commands.literal("abyssal").executes(ctx -> tp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), Polarity.ABYSSAL)))))
                .then(Commands.literal("set").then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, 255)).executes(ctx -> set(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"), IntegerArgumentType.getInteger(ctx, "index"))))));
    }

    // ================================================================================================================ list

    private static int list(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        List<PartyService.Row> rows = PartyService.rows(server);
        say(src, PartyLines.header(PartyConfig.mode().id(), PartyService.instanced(), PartyService.maxParties(), MegaParties.get(server).index().size(), MegaData.get(server).sectors().size()));
        for (PartyService.Row r : rows) say(src, PartyLines.row(r.index(), r.kind(), r.key(), r.name(), r.holy(), r.abyssal(), r.online()));
        return 1;
    }

    private static String when(long ticks, long now) {
        long hours = Math.max(0L, now - ticks) / 72_000L;
        return hours < 1 ? "<1 h ago" : hours + " h ago";
    }

    private static String size(long bytes) {
        if (bytes < 1024L * 1024L) return (bytes / 1024L) + " KB";
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1048576.0);
    }

    private static int lanes(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        MegaParties book = MegaParties.get(server);
        long now = server.overworld().getGameTime();
        java.util.Map<Integer, Long> bytes = LanePurge.sizes(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
        say(src, "Lanes: cleanup " + PartyConfig.laneCleanup() + ", idle after " + PartyConfig.get(PartyConfig.LANE_IDLE_HOURS) + " h of running time; freed and waiting for the next start: " + book.retired());
        long total = 0L;
        for (PartyService.Row r : PartyService.rows(server)) {
            Long at = book.used().get(r.index());
            long b = bytes.getOrDefault(r.index(), 0L);
            total += b;
            say(src, "lane " + r.index() + " " + r.kind() + " " + r.name() + ": last used " + (at == null ? "never recorded" : when(at, now)) + ", " + size(b) + ", " + r.online() + " inside");
        }
        for (int index : book.retired()) {
            long b = bytes.getOrDefault(index, 0L);
            total += b;
            say(src, "lane " + index + " freed: " + size(b) + " still on disk");
        }
        return say(src, "Dungeon lanes on disk: " + size(total));
    }

    private static int free(CommandSourceStack src, int index) {
        PartyService.Freed f = PartyService.retire(src.getServer(), index);
        if (!f.ok()) return no(src, "lane " + index + " not freed: " + f.why());
        return say(src, "lane " + index + " freed (" + f.keys() + " parties, " + f.sectors() + " sectors forgotten); its files are " + PartyConfig.laneCleanup() + " at the next server start");
    }

    // ================================================================================================================ of

    private static int of(CommandSourceStack src, Collection<GameProfile> profiles) {
        MinecraftServer server = src.getServer();
        int n = 0;
        for (GameProfile gp : profiles) {
            PartyKey key = PartyResolver.keyOf(server, gp.getId());
            PartyIndex.Entry e = MegaParties.get(server).index().get(key.id());
            ServerPlayer online = server.getPlayerList().getPlayer(gp.getId());
            String where = "outside";
            if (online != null && MegaDungeon.in(online.level())) {
                Sector s = MegaDungeon.sectorAt(online.level(), online.blockPosition());
                where = s == null ? "party " + PartyService.partyOf(online) : "party " + s.party + " floor " + FloorMap.label(MegaDungeon.floorAt(online.level(), online.blockPosition()));
            }
            String name = e != null ? e.label() : key.team() ? "-" : gp.getName();
            n += say(src, PartyLines.of(gp.getName(), e == null ? -1 : e.index(), key.team() ? "team" : "solo", key.id().toString(), name, where));
        }
        return n;
    }

    // ================================================================================================================ set

    private static int set(CommandSourceStack src, Collection<GameProfile> profiles, int index) {
        MinecraftServer server = src.getServer();
        int n = 0;
        for (GameProfile gp : profiles) {
            PartyService.SetResult r = PartyService.set(server, gp.getId(), gp.getName(), index);
            if (!r.ok()) {
                no(src, PartyLines.refused("set", r.why()));
                continue;
            }
            say(src, PartyLines.set(gp.getName(), r.index(), r.before(), r.key().id().toString(), r.key().team() ? "team" : "solo"));
            n++;
        }
        return n;
    }

    // ================================================================================================================ tp

    /** Takes the admin to the first floor of a party's road (the Holy road first when none is named). */
    private static int tp(CommandSourceStack src, int index, Polarity road) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        MinecraftServer server = src.getServer();
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return no(src, PartyLines.refused("tp", "the Mega-Dungeon dimension is not loaded"));
        MegaData data = MegaData.get(server);
        Polarity[] order = road != null ? new Polarity[] {road} : new Polarity[] {Polarity.HOLY, Polarity.ABYSSAL};
        for (Polarity r : order) {
            for (Sector s : data.chain(index, r)) {
                if (!SectorGenerator.playable(s)) continue;
                if (!MegaTravel.go(p, level, s, false)) return no(src, PartyLines.refused("tp", "sector " + s.id + " has no pad to stand on"));
                return say(src, PartyLines.tp(index, r, s.firstFloor(), s.id));
            }
        }
        return no(src, PartyLines.refused("tp", "party " + index + " has no finished floor" + (road == null ? "" : " on the " + road.id() + " road")));
    }
}
