package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: {@code /multiverse mega ...} is how an admin looks at the Mega-Dungeon and pushes it, and how the headless CI smoke test drives it through the server
 * console and RCON, so every sub-command except {@code tp} (which needs a player to move) works from the console and prints plain, one-line, parseable
 * answers whose words are pinned in {@link CommandLines}. {@code status} lists the sectors, {@code sector [id]} shows one, {@code expand} makes the dungeon grow
 * (sector 0 when there is none, else the next sector off the newest free Way Onward), {@code regen <id>} places a sector again from its saved seed,
 * {@code audit <id>} checks the saved data against the promises the other systems rely on, {@code verify [id]} asks the game itself whether the plan's
 * rotation maths and placement agree with vanilla, {@code plan <id>} writes the sector's plan as a JSON file for the map preview, {@code rooms [id]} counts the
 * rooms by kind, {@code unseal <id>} is the admin's Gatekeeper key, {@code tp <id>} goes to a Landing, and {@code pools} lists the template pools the data pack
 * provides and how many of their templates cannot be read. Permission 2 on the whole tree, messages as literals (the pattern of {@code LabyrinthCommand}).
 * The lead attaches it under {@code /multiverse} with {@code root.then(MegaCommand.build())}; the other builders' sub-commands are attached under {@code mega} by
 * the lead too.
 */
public final class MegaCommand {
    private MegaCommand() {}

    private static int say(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int no(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mega").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
        root.then(Commands.literal("sector").executes(ctx -> sector(ctx.getSource(), -1))
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> sector(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("tp").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> tp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("expand").executes(ctx -> expand(ctx.getSource())));
        root.then(Commands.literal("regen").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> regen(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("audit").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> audit(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("verify").executes(ctx -> verify(ctx.getSource(), -1))
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> verify(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("plan").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> plan(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("unseal").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> unseal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("rooms").executes(ctx -> rooms(ctx.getSource(), -1))
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> rooms(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("pools").executes(ctx -> pools(ctx.getSource())));
        return root;
    }

    // ================================================================================================================ lookups

    private static Sector sectorOrNewest(CommandSourceStack src, int id) {
        MegaData data = MegaData.get(src.getServer());
        if (id >= 0) return data.sector(id);
        ServerPlayer p = src.getPlayer();
        if (p != null) {
            Sector here = MegaDungeon.sectorAt(p.level(), p.blockPosition());
            if (here != null) return here;
        }
        return data.latest();
    }

    private static String sectorLine(MinecraftServer server, Sector s) {
        GenData.Stat st = GenData.get(server).get(s.id);
        BoundingBox r = s.region;
        return CommandLines.sector(s.id, s.state.name(), s.theme.id(), s.threat, s.rooms.size(), s.edges.size(), st == null ? 0 : st.pieces, s.relicCount,
                r.minX(), r.minZ(), r.maxX(), r.maxZ());
    }

    private static String xyz(BlockPos p) { return p == null ? "none" : p.getX() + "," + p.getY() + "," + p.getZ(); }

    // ================================================================================================================ status and sector

    private static int status(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        say(src, CommandLines.status(data.expansionLevel, data.sectors().size(), ExpansionQueue.get().progress()));
        for (Sector s : data.sectors()) say(src, sectorLine(server, s));
        return 1;
    }

    private static int sector(CommandSourceStack src, int id) {
        Sector s = sectorOrNewest(src, id);
        if (s == null) return no(src, id >= 0 ? CommandLines.refused("sector", "no sector " + id) : CommandLines.refused("sector", "the Mega-Dungeon has no sectors yet"));
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        say(src, sectorLine(server, s));
        Sector prev = SectorGenerator.prevOf(data, s);
        say(src, "  entry=" + xyz(s.entry) + " forward=" + ExpansionQueue.describe(s.forward) + " arrival=" + xyz(s.arrival));
        say(src, "  guardian=" + xyz(s.guardianSpawn) + " gatekeeper=" + xyz(s.gatekeeper) + " vault=" + xyz(s.vaultCenter));
        say(src, "  wayOnward=" + xyz(s.expansionPos) + " facing=" + ExpansionQueue.describe(s.expansionFacing) + " seal=" + s.sealW + "x" + s.sealH + "x" + s.sealD
                + " next=" + (s.nextId < 0 ? "none" : Integer.toString(s.nextId)) + " prev=" + (prev == null ? "none" : Integer.toString(prev.id)));
        GenData.Stat st = GenData.get(server).get(s.id);
        if (st != null) {
            say(src, "  generated: tries=" + st.tries + " fallback=" + st.fallback + " deadEnd=" + st.deadEnd + " loops=" + st.loops + " caps=" + st.caps + " link=" + st.link
                    + " ms=" + st.millis);
        }
        return 1;
    }

    // ================================================================================================================ growing

    private static int expand(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        List<ExpandRule.Row> rows = new ArrayList<>();
        for (Sector s : data.sectors()) {
            rows.add(new ExpandRule.Row(s.id, s.expansionPos != null && s.expansionFacing != null, s.nextId, s.state == Sector.State.GENERATING, s.state == Sector.State.FAILED));
        }
        ExpandRule.Decision d = ExpandRule.decide(rows);
        if (d.refusal() != null) return no(src, CommandLines.refused("expand", d.refusal()));
        if (d.first()) {
            String why = ExpansionQueue.get().tryFirst(server);
            return why == null ? say(src, CommandLines.queued("expand", 0)) : no(src, CommandLines.refused("expand", why));
        }
        ServerLevel level = MegaDungeon.level(server);
        Sector from = data.sector(d.from());
        if (level == null) return no(src, CommandLines.refused("expand", "the Mega-Dungeon dimension is not loaded"));
        String why = ExpansionQueue.get().tryNext(level, from);
        return why == null ? say(src, CommandLines.queued("expand", from.nextId)) : no(src, CommandLines.refused("expand", why));
    }

    private static int regen(CommandSourceStack src, int id) {
        String why = ExpansionQueue.get().tryRegen(src.getServer(), id, false);
        return why == null ? say(src, CommandLines.queued("regen", id)) : no(src, CommandLines.refused("regen", why));
    }

    private static int unseal(CommandSourceStack src, int id) {
        MinecraftServer server = src.getServer();
        Sector s = MegaData.get(server).sector(id);
        ServerLevel level = MegaDungeon.level(server);
        if (s == null) return no(src, CommandLines.refused("unseal", "no sector " + id));
        if (level == null) return no(src, CommandLines.refused("unseal", "the Mega-Dungeon dimension is not loaded"));
        Sector.State before = s.state;
        if (!MegaDungeon.unseal(level, id)) return no(src, CommandLines.refused("unseal", "sector " + id + " is " + before.name() + " and cannot be unsealed"));
        return say(src, "unseal: sector " + id + " unsealed" + (MegaConfig.get(MegaConfig.EXPANSION) ? ", the next sector was requested" : ", expansion is off so nothing was requested"));
    }

    private static int tp(CommandSourceStack src, int id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        MinecraftServer server = src.getServer();
        Sector s = MegaData.get(server).sector(id);
        ServerLevel level = MegaDungeon.level(server);
        if (s == null) return no(src, CommandLines.refused("tp", "no sector " + id));
        if (level == null) return no(src, CommandLines.refused("tp", "the Mega-Dungeon dimension is not loaded"));
        if (!SectorGenerator.playable(s)) return no(src, CommandLines.refused("tp", "sector " + id + " is " + s.state.name()));
        return MegaTravel.go(p, level, s, false) ? say(src, "tp: sector " + id) : no(src, CommandLines.refused("tp", "sector " + id + " has no Landing pad"));
    }

    // ================================================================================================================ checking

    private static int audit(CommandSourceStack src, int id) {
        MegaData data = MegaData.get(src.getServer());
        Sector s = data.sector(id);
        if (s == null) return no(src, CommandLines.audit(id, List.of("no such sector")));
        Set<Integer> ids = new HashSet<>();
        for (Sector o : data.sectors()) ids.add(o.id);
        List<String> bad = SavedAudit.audit(SectorGenerator.view(s), ids, VanillaPools.get().recipe().requireMarkers);
        String line = CommandLines.audit(id, bad);
        return bad.isEmpty() ? say(src, line) : no(src, line);
    }

    private static int verify(CommandSourceStack src, int id) {
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        Sector s = sectorOrNewest(src, id);
        ServerLevel level = MegaDungeon.level(server);
        if (s == null) return no(src, CommandLines.refused("verify", id >= 0 ? "no sector " + id : "the Mega-Dungeon has no sectors yet"));
        if (level == null) return no(src, CommandLines.refused("verify", "the Mega-Dungeon dimension is not loaded"));
        int world = SectorGenerator.playable(s) ? 12 : 0;
        SectorGenerator.Planned planned = SectorGenerator.plan(level, data, s);
        if (!planned.ok()) return no(src, "verify " + s.id + " FAIL: the sector cannot be planned: " + planned.failure());
        SectorGenerator.Built built = SectorGenerator.build(level, planned);
        GeometryCheck.Report r = GeometryCheck.run(level, built, world);
        List<String> failures = new ArrayList<>(r.failures());
        int roomsPlanned = planned.result().rooms().size();
        if (world > 0 && roomsPlanned != s.rooms.size()) {
            failures.add(0, "the saved sector has " + s.rooms.size() + " rooms but its plan now has " + roomsPlanned + " (the pools or the config changed since it was generated)");
        }
        GeometryCheck.Report shown = new GeometryCheck.Report(r.pieces(), r.boxes(), r.jigsaws(), r.markers(), r.worldPieces(), r.sampled(), r.found(), r.jigsawsLeft(), failures);
        return shown.ok() ? say(src, shown.line(s.id)) : no(src, shown.line(s.id));
    }

    // ================================================================================================================ plan, rooms, pools

    private static int plan(CommandSourceStack src, int id) {
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        Sector s = data.sector(id);
        ServerLevel level = MegaDungeon.level(server);
        if (s == null) return no(src, CommandLines.refused("plan", "no sector " + id));
        if (level == null) return no(src, CommandLines.refused("plan", "the Mega-Dungeon dimension is not loaded"));
        SectorGenerator.Planned planned = SectorGenerator.plan(level, data, s);
        if (!planned.ok()) return no(src, CommandLines.refused("plan", "the sector cannot be planned: " + planned.failure()));
        String json = PlanJson.write(planned.result(), SectorGenerator.vec(s.entry), SectorGenerator.facing(s.forward), planned.solved().audit(), planned.solved().tries(),
                planned.solved().fallback(), "sector " + id);
        Path path = FMLPaths.GAMEDIR.get().resolve("mega_plan_" + id + ".json").toAbsolutePath();
        try {
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return no(src, CommandLines.refused("plan", "cannot write " + path + ": " + e.getMessage()));
        }
        return say(src, CommandLines.plan(path.toString()));
    }

    private static int rooms(CommandSourceStack src, int id) {
        Sector s = sectorOrNewest(src, id);
        if (s == null) return no(src, CommandLines.refused("rooms", id >= 0 ? "no sector " + id : "the Mega-Dungeon has no sectors yet"));
        int[] count = new int[RoomKind.values().length];
        for (Room r : s.rooms) count[r.kind.ordinal()]++;
        for (RoomKind k : RoomKind.values()) {
            if (k == RoomKind.CAP) continue;                                          // door caps are never saved as rooms
            say(src, CommandLines.roomKind(k, count[k.ordinal()]));
        }
        return 1;
    }

    private static int pools(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        ServerLevel level = MegaDungeon.level(server);
        VanillaPools pools = VanillaPools.get();
        if (level != null) pools.bind(level.getStructureManager());
        List<String> ids = pools.poolIds();
        if (ids.isEmpty()) return no(src, CommandLines.refused("pools", "the data pack has no data/nusmp/worldgen/template_pool/mega pools"));
        say(src, "pools: " + ids.size() + " pools, recipe minRooms=" + pools.recipe().minRooms + " minLoops=" + pools.recipe().minLoops);
        for (String id : ids) {
            PoolDef d = pools.pool(id);
            int unreadable = 0;
            for (PoolEntry e : d.entries()) if (pools.shape(e.shape()) == null) unreadable++;
            say(src, "pool " + id + " entries=" + d.entries().size() + " empty=" + d.emptyWeight() + " fallback=" + d.fallback() + " unreadable=" + unreadable);
        }
        return 1;
    }
}
