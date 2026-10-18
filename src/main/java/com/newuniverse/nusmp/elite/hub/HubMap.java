package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Dungeon Map reveals "the layout of the CURRENT floor (rooms and their connections as the generator knows them)", and the generator's answer is the {@link Sector} the player stands in:
 * its placed rooms (a template, a {@code RoomKind}, a bounding box) and the {@code edges} between room ids. This class turns that into the compact {@link MapView} (only this floor's rooms, the
 * doors between them, a visited flag from the player's {@link MapMemory}), applies the configured cost ({@link MapMode}) and sends it. Everything that decides what is on the map is in the pure
 * {@link MapView#build}; this is only the read of the live sector and the economy of the item. A player outside the Mega-Dungeon, or in the gap between two sectors, is told the map shows nothing.
 * The map does not give away what is IN a room (monsters, chests): the brief asks for the layout, and a player who wants to know what waits in a room should have to go and see.
 */
public final class HubMap {
    private HubMap() {}

    public static boolean on() { return HubConfig.enabled() && HubConfig.get(HubConfig.MAP_ENABLED); }

    public static MapMode mode() { return MapMode.parse(HubConfig.get(HubConfig.MAP_MODE)); }

    /** The use of a map by a player: builds the picture of the floor they stand on, charges the use, sends it. */
    static void use(ServerPlayer sp, ItemStack stack) {
        if (!on()) {
            QuestService.tell(sp, "message.nusmp.hub.map.off", "The map is blank: the Dungeon Map is switched off on this server.");
            return;
        }
        ServerLevel level = sp.serverLevel();
        Sector sector = MegaDungeon.in(level) ? MegaDungeon.sectorAt(level, sp.blockPosition()) : null;
        if (sector == null) {
            QuestService.tell(sp, "message.nusmp.hub.map.nothing", "The map shows nothing here. Use it on a floor of the Mega-Dungeon.");
            return;
        }
        int floor = MegaDungeon.floorAt(level, sp.blockPosition());
        MapMemory memory = sp.getData(HubAttachments.MAP);
        boolean mapped = memory.isMapped(sector.id);
        int cost = mode().cost(mapped);
        if (cost > 0 && !sp.getAbilities().instabuild) stack.shrink(cost);
        memory.markMapped(sector.id);
        Room here = sector.roomAt(sp.blockPosition());
        if (here != null) memory.visit(sector.id, here.id);
        String note = cost > 0 ? "This map is used up" : mapped && mode() == MapMode.PER_FLOOR ? "Already mapped: free" : "";
        MapView view = view(sector, floor, memory, HubConfig.get(HubConfig.MAP_MAX_ROOMS));
        if (HubSync.hasConnection(sp)) PacketDistributor.sendToPlayer(sp, new HubNet.MapSync(view, note));
    }

    /** The picture of one floor of a sector for a player's memory. */
    public static MapView view(Sector sector, int floor, MapMemory memory, int maxRooms) {
        List<MapView.Source> sources = new ArrayList<>();
        for (Room r : sector.rooms) sources.add(new MapView.Source(r.id, r.kind.ordinal(), r.floor, r.box.minX(), r.box.minZ(), r.box.maxX(), r.box.maxZ()));
        BlockPos arrival = sector.arrival != null ? sector.arrival : sector.entry;
        BlockPos exit = sector.stairForward;
        int first = sector.firstFloor();
        String road = sector.polarity.id();
        return MapView.build(sources, sector.edges, floor, first, road, maxRooms, id -> memory.visited(sector.id, id),
                arrival.getX(), arrival.getZ(), exit != null, exit == null ? 0 : exit.getX(), exit == null ? 0 : exit.getZ());
    }
}
