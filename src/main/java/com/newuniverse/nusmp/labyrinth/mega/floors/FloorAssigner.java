package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: a floor only exists once somebody writes it down, and the one moment the whole room graph of a sector is known is the moment the generator has
 * finished it. So this is a {@link SectorListener}: when a sector has been generated (or generated again by {@code /multiverse mega regen}) it bands the
 * rooms with {@link FloorBands} and stores the signed floor in {@link Room#floor}, which is saved with the sector, so nothing is recomputed per player or per
 * tick and {@code MegaDungeon.floorAt} just reads it. It is idempotent: a second run over the same sector writes the same numbers. The number of floors per
 * sector ({@link Sector#perRegion}) is decided once, the first time the sector is numbered, and stored in the sector, so changing the config later never
 * renumbers a sector that exists; and a sector of a road that already has numbered sectors takes THEIR length, because floors are counted as
 * {@code chainIndex * perRegion + band} and two lengths in one chain would overlap (a 5-floor sector after a 10-floor one would reuse floors +6..+10); a
 * regenerated sector has new, unnumbered rooms but keeps the length it was given (see {@link FloorBands#lengthFor}).
 * {@link #ensureBanded} is the safety net used when a player is found in a sector whose rooms were never numbered: a world saved by 1.54.0 has none, and a
 * generator that forgot to announce a sector would leave none; the sector is numbered on first contact instead of being read as one endless first floor.
 * How the graph is cut into floors is {@link FloorBands.Strategy} (config {@code floors.banding}). Nothing here may break generation, so every failure is
 * logged and swallowed.
 */
public final class FloorAssigner implements SectorListener {
    private static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onSectorGenerated(ServerLevel level, Sector sector) {
        try {
            MegaData data = MegaData.get(level.getServer());
            int changed = assign(sector, data);
            data.setDirty();
            LOG.info("[nusmp] Sector {} ({} chain {}): {} of {} rooms numbered, floors {} to {}", sector.id, sector.polarity.id(), sector.chainIndex, changed, sector.rooms.size(),
                    FloorMap.label(sector.firstFloor()), FloorMap.label(sector.lastFloor()));
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] Floors of sector {} could not be numbered", sector.id, ex);
        }
    }

    /** Numbers the rooms of one sector without a view of the other sectors (the config alone decides the length of a sector never numbered before). Returns how many rooms changed floor. */
    public static int assign(Sector s) { return assign(s, null); }

    /**
     * Numbers the rooms of one sector: {@code Room.floor = FloorMap.floorOf(polarity, chainIndex, band, perRegion)} for every room, the band coming from
     * {@link FloorBands}. {@code data} (nullable) is only used to find the length of the road's other sectors. The caller saves ({@code MegaData.setDirty}).
     * Returns how many rooms changed floor (0 on a second run).
     */
    public static int assign(Sector s, MegaData data) {
        if (s == null || s.rooms.isEmpty()) return 0;
        if (!isBanded(s)) s.perRegion = firstPerRegion(s, data);
        int per = Math.max(1, s.perRegion);
        Room landing = landingOf(s);
        Room arena = arenaOf(s);
        List<FloorBands.Node> nodes = new ArrayList<>(s.rooms.size());
        for (Room r : s.rooms) nodes.add(new FloorBands.Node(r.id, r.kind));
        // a sector without a Landing is walked from its lowest room id (the pure rule decides), one without an arena has the deepest room play it
        Map<Integer, Integer> floors = FloorBands.floors(nodes, landing == null ? FloorBands.NONE : landing.id, arena == null ? FloorBands.NONE : arena.id, s.edges, s.polarity, s.chainIndex, per, strategy());
        int changed = 0;
        for (Room r : s.rooms) {
            int floor = floors.getOrDefault(r.id, FloorMap.floorOf(s.polarity, s.chainIndex, per, per));
            if (r.floor != floor) {
                r.floor = floor;
                changed++;
            }
        }
        return changed;
    }

    /** The banding strategy the config asks for (the balanced one when the value is missing or unknown). */
    public static FloorBands.Strategy strategy() {
        FloorBands.Strategy s = FloorBands.Strategy.fromId(FloorsConfig.get(FloorsConfig.BANDING));
        return s == null ? FloorBands.Strategy.EVEN : s;
    }

    /**
     * Numbers the sector if it never was, so a player found in it reads the right floor. False when nothing was done: it already has numbers, it has no
     * rooms, or it is still being generated (its rooms are not final) or failed. Saves.
     */
    public static boolean ensureBanded(MinecraftServer server, Sector s) {
        if (server == null || s == null || s.rooms.isEmpty() || isBanded(s)) return false;
        if (s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return false;
        try {
            MegaData data = MegaData.get(server);
            assign(s, data);
            data.setDirty();
            return true;
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] Sector {} could not be numbered on first contact", s.id, ex);
            return false;
        }
    }

    /**
     * Numbers every finished sector whose rooms carry no floors yet: the sectors of a world saved by 1.54.0, or of a generator that never announced one.
     * Called once when the server has started, so {@code MegaDungeon.floorAt} is right for old worlds from the first tick instead of from the first player
     * who happens to stand in the sector. Returns how many sectors were numbered.
     */
    public static int numberOldSectors(MinecraftServer server) {
        if (server == null) return 0;
        int n = 0;
        for (Sector s : new ArrayList<>(MegaData.get(server).sectors())) if (ensureBanded(server, s)) n++;
        return n;
    }

    /** Has any room of the sector been given a floor? (A numbered room is never floor 0: floors are signed and never 0.) */
    public static boolean isBanded(Sector s) {
        for (Room r : s.rooms) if (r.floor != 0) return true;
        return false;
    }

    /**
     * The length to give a sector whose rooms carry no numbers: the length of any other numbered sector of the same road (so the chain's floors never overlap
     * or leave a gap), else the length the sector already carries when a numbering decided it before (a regenerated sector has new, unnumbered rooms but keeps
     * its length), else the config's {@code perRegion}. The rule itself is the pure {@link FloorBands#lengthFor}.
     */
    static int firstPerRegion(Sector s, MegaData data) {
        int other = 0;
        if (data != null) {
            for (Sector o : data.sectors()) {
                if (o != s && o.polarity == s.polarity && isBanded(o)) {
                    other = Math.max(1, o.perRegion);
                    break;
                }
            }
        }
        return FloorBands.lengthFor(s.perRegion, other, FloorsConfig.get(FloorsConfig.PER_REGION));
    }

    /** The Landing: the room that holds the arrival marker, else the first room of kind LANDING, else null. */
    public static Room landingOf(Sector s) {
        Room r = s.arrival == null ? null : s.roomAt(s.arrival);
        if (r != null) return r;
        for (Room x : s.rooms) if (x.kind == RoomKind.LANDING) return x;
        return null;
    }

    /** The Guardian's arena: the first room of kind ARENA (a sector that passed the audit has exactly one), or null. */
    public static Room arenaOf(Sector s) {
        for (Room x : s.rooms) if (x.kind == RoomKind.ARENA) return x;
        return null;
    }
}
