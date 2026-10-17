package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
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
 * provides and how many of their templates cannot be read. Since 1.55.0 the dungeon is two roads: {@code expand [holy|abyssal]} makes the named road grow (the Holy
 * road when none is named), {@code status} lists both roads (their sector ids, the floors they span and the state of their newest sector) and every sector line says
 * which road and floors it belongs to. Permission 2 on the whole tree, messages as literals (the pattern of {@code LabyrinthCommand}).
 * The lead attaches it under {@code /multiverse} with {@code root.then(MegaCommand.build())}; the other builders' sub-commands are attached under {@code mega} by
 * the lead too.
 *
 * <p>Parties (1.58.0): the dungeon is instanced, so {@code status} and {@code expand} work on ONE party's chains. They take an optional trailing {@code party <index>}; without
 * it the console means party 0 (the shared chain, so the smoke test and every older habit read exactly what they always read), and a player means their own party (the floors
 * they stand on, else the party their gate would take them to). Every command that names a sector by its id ({@code sector}, {@code regen}, {@code audit}, {@code tp} ...)
 * needs no party: ids are global. A sector of any party but 0 says {@code party=<n>} at the end of its line; party 0 lines are unchanged.
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
        root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource(), -1))
                .then(Commands.literal("party").then(Commands.argument("index", IntegerArgumentType.integer(0, MegaGrid.MAX_PARTIES - 1)).executes(ctx -> status(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"))))));
        root.then(Commands.literal("sector").executes(ctx -> sector(ctx.getSource(), -1))
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> sector(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("tp").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> tp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        root.then(Commands.literal("expand").executes(ctx -> expand(ctx.getSource(), Polarity.HOLY, -1))
                .then(partyNode(Polarity.HOLY))
                .then(Commands.literal("holy").executes(ctx -> expand(ctx.getSource(), Polarity.HOLY, -1)).then(partyNode(Polarity.HOLY)))
                .then(Commands.literal("abyssal").executes(ctx -> expand(ctx.getSource(), Polarity.ABYSSAL, -1)).then(partyNode(Polarity.ABYSSAL))));
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

    /** {@code party <index>}: the optional tail of {@code expand}, naming the party whose road grows. */
    private static LiteralArgumentBuilder<CommandSourceStack> partyNode(Polarity road) {
        return Commands.literal("party").then(Commands.argument("index", IntegerArgumentType.integer(0, MegaGrid.MAX_PARTIES - 1))
                .executes(ctx -> expand(ctx.getSource(), road, IntegerArgumentType.getInteger(ctx, "index"))));
    }

    // ================================================================================================================ lookups

    /**
     * The party a read-only command shows when none is named: 0 for the console, the party of the floors a player stands on or of their record for a player, 0 for a
     * player whose party has no floors yet (the shared chain, which is what every command showed before parties).
     */
    private static int viewParty(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        return p == null ? 0 : Math.max(0, PartyService.knownParty(p));
    }

    /**
     * The party a command that BUILDS grows when none is named: 0 for the console; for a player the floors they stand on, else the party their gate would take them to (handed
     * out now if they have none: they are growing their own chain). -1 when a player's party cannot be placed.
     */
    private static int growParty(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        int known = PartyService.knownParty(p);
        return known >= 0 ? known : PartyService.enter(p);
    }

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
        return CommandLines.withParty(CommandLines.sectorOnRoad(CommandLines.sector(s.id, s.state.name(), s.theme.id(), s.threat, s.rooms.size(), s.edges.size(), st == null ? 0 : st.pieces, s.relicCount,
                r.minX(), r.minZ(), r.maxX(), r.maxZ()), s.polarity, s.chainIndex, s.firstFloor(), s.lastFloor()), s.party);
    }

    private static String xyz(BlockPos p) { return p == null ? "none" : p.getX() + "," + p.getY() + "," + p.getZ(); }

    // ================================================================================================================ status and sector

    private static int status(CommandSourceStack src, int explicitParty) {
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        int party = explicitParty >= 0 ? explicitParty : viewParty(src);
        say(src, CommandLines.status(data.expansionLevel, data.sectors().size(), ExpansionQueue.get().progress()));
        List<Integer> known = data.parties();
        if (party != 0 || known.size() > 1) say(src, CommandLines.partyView(party, known));
        for (Polarity road : Polarity.values()) {
            List<Sector> chain = data.chain(party, road);
            List<Integer> ids = new ArrayList<>();
            for (Sector s : chain) ids.add(s.id);
            Sector newest = chain.isEmpty() ? null : chain.get(chain.size() - 1);
            say(src, CommandLines.road(road, ids, chain.isEmpty() ? 0 : chain.get(0).firstFloor(), newest == null ? 0 : newest.lastFloor(), newest == null ? "" : newest.state.name()));
            for (Sector s : chain) say(src, sectorLine(server, s));
        }
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
        if (s.isFloor()) say(src, CommandLines.stairs(s.stairBack == null ? null : xyz(s.stairBack), s.stairForward == null ? null : xyz(s.stairForward),
                s.stairForwardPad == null ? null : xyz(s.stairForwardPad), StairLinks.back(s.chainIndex), StairLinks.forward(s.chainIndex), s.polarity, s.isBossFloor(),
                data.chainSector(s.party, s.polarity, s.chainIndex + 1) == null ? -1 : data.chainSector(s.party, s.polarity, s.chainIndex + 1).id,
                s.chainIndex == 0 ? -1 : data.chainSector(s.party, s.polarity, s.chainIndex - 1) == null ? -1 : data.chainSector(s.party, s.polarity, s.chainIndex - 1).id));
        GenData.Stat st = GenData.get(server).get(s.id);
        if (st != null) {
            say(src, "  generated: tries=" + st.tries + " fallback=" + st.fallback + " deadEnd=" + st.deadEnd + " loops=" + st.loops + " caps=" + st.caps + " link=" + st.link
                    + " ms=" + st.millis);
        }
        return 1;
    }

    // ================================================================================================================ growing

    /**
     * Makes one road grow: its first sector when the road has none, else the next sector off the road's newest sector that has a free Way Onward. The refusal says which
     * road and why. Works at any time after the road's sector is built, whatever state its Guardian, Gatekeeper and Crucible are in (an admin's push).
     */
    private static int expand(CommandSourceStack src, Polarity road, int explicitParty) {
        MinecraftServer server = src.getServer();
        MegaData data = MegaData.get(server);
        int party = explicitParty >= 0 ? explicitParty : growParty(src);
        if (party < 0) return no(src, CommandLines.refused("expand", "your party has no free lane (party.maxParties)"));
        if (party > 0 && !PartyService.instanced()) return no(src, CommandLines.refused("expand", "the dungeon is not instanced (party.mode is shared, or floors.perSector is above 1): only party 0 can grow"));
        List<ExpandRule.Row> rows = new ArrayList<>();
        for (Sector s : data.chain(party, road)) rows.add(ExpansionQueue.rowOf(s));
        ExpandRule.Decision d = ExpandRule.decide(rows);
        if (d.refusal() != null) return no(src, CommandLines.refused("expand", "the " + road.id() + " road" + (party == 0 ? "" : " of party " + party) + ": " + d.refusal()));
        if (d.first()) {
            String why = ExpansionQueue.get().tryFirst(server, road, party);
            if (why != null) return no(src, CommandLines.refused("expand", why));
            Sector made = data.chainTail(party, road);
            return say(src, CommandLines.withParty(CommandLines.queuedOn("expand", made.id, road, made.firstFloor(), made.lastFloor()), party));
        }
        ServerLevel level = MegaDungeon.level(server);
        Sector from = data.sector(d.from());
        if (level == null) return no(src, CommandLines.refused("expand", "the Mega-Dungeon dimension is not loaded"));
        String why = ExpansionQueue.get().tryNext(level, from);
        if (why != null) return no(src, CommandLines.refused("expand", why));
        Sector made = data.sector(from.nextId);
        return say(src, CommandLines.withParty(CommandLines.queuedOn("expand", made.id, road, made.firstFloor(), made.lastFloor()), party));
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
        if (!MegaConfig.get(MegaConfig.EXPANSION)) return say(src, "unseal: sector " + id + " unsealed, expansion is off so nothing was requested");
        // with mega.expandAfterVault the facade held the next sector back until the Crucible is resolved; the admin's key does not wait for the vault
        boolean skippedVault = s.expansionPending && MegaDungeon.requestPending(level, s);
        if (s.nextId < 0) return say(src, "unseal: sector " + id + " unsealed, but no next sector was queued (see the server log" + (s.expansionPending ? "; it still waits for the vault" : "") + ")");
        return say(src, "unseal: sector " + id + " unsealed, the next sector was requested (sector " + s.nextId + (skippedVault ? ", the vault was skipped" : "") + ")");
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
        Recipe fr = pools.floorRecipe();
        say(src, "floors: " + MegaConfig.get(MegaConfig.FLOOR_ROOMS) + " rooms per floor (config), floor recipe minRooms=" + fr.minRooms + " minLoops=" + fr.minLoops + " stairMinWalk=" + fr.stairMinWalk
                + ", " + pools.themes().size() + " template theme rule(s)");
        for (String id : ids) {
            PoolDef d = pools.pool(id);
            int unreadable = 0;
            for (PoolEntry e : d.entries()) if (pools.shape(e.shape()) == null) unreadable++;
            say(src, "pool " + id + " entries=" + d.entries().size() + " empty=" + d.emptyWeight() + " fallback=" + d.fallback() + " unreadable=" + unreadable);
        }
        return 1;
    }
}
