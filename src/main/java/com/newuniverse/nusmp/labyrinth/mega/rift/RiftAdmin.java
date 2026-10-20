package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.ExclusivePockets;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDef;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDefs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the one door the admin tree (and any other system) uses to work the rifts and the exclusive pocket dungeons, so nothing outside this package needs to know how they are built. The four calls
 * never throw, never depend on the level being the dungeon, and say with their return value what happened: {@link #spawnRift} opens a tear (a MEGA-RARE tear to a random exclusive tomb when
 * {@code pocket} is true, an ordinary one otherwise); {@link #pocketIds} lists the tombs that are loaded; {@link #enter} puts a player into a fresh tomb of that id; {@link #collapseAll} brings
 * everything down. They are the same code paths the game's own rift spawner and stepping-in use, not copies of them.
 */
public final class RiftAdmin {
    private RiftAdmin() {}

    private static final Logger LOG = LogUtils.getLogger();

    /**
     * Opens a rift at {@code at} (or in the nearest free cell just above it). {@code pocket}: a mega-rare tear that leads to a random exclusive pocket dungeon, drawn in that dungeon's colours; else an
     * ordinary tear: in the Mega-Dungeon, on a floor with a sector, a shortcut aimed from that sector's chain; anywhere else a return tear (it leads to the player's waypoint). Safe on any level:
     * false (and nothing placed) when the level is null, the place is not loaded, no free cell is near, or no tomb is loaded for a pocket tear.
     */
    public static boolean spawnRift(ServerLevel level, BlockPos at, boolean pocket) {
        try {
            if (level == null || at == null || !level.isLoaded(at)) return false;
            BlockPos feet = freeCell(level, at);
            if (feet == null) return false;
            var rnd = level.getRandom();
            if (pocket) {
                PocketDef def = MegaRifts.pickForced("", rnd);
                if (def == null) return false;
                DimensionalRiftBlockEntity be = RiftService.open(level, feet, RiftType.POCKET, rnd.nextLong());
                if (be == null) return false;
                MegaRifts.aim(be, def);
                MegaRifts.announce(level, feet, def, null);
                return true;
            }
            Sector s = com.newuniverse.nusmp.labyrinth.Labyrinth.inMega(level) ? MegaData.get(level.getServer()).sectorAt(feet) : null;
            if (s != null && s.isFloor() && !s.isBossFloor()) {
                DimensionalRiftBlockEntity be = RiftService.open(level, feet, RiftType.SHORTCUT, rnd.nextLong());
                if (be == null) return false;
                RiftService.aimShortcut(MegaData.get(level.getServer()), s, be);
                return true;
            }
            return RiftService.open(level, feet, RiftType.RETURN, rnd.nextLong()) != null;
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] RiftAdmin.spawnRift failed at {}: {}", at, ex.toString());
            return false;
        }
    }

    /** A cell for a rift's feet at or just above {@code at}: air or replaceable with air above it; null when six cells up find none. */
    private static BlockPos freeCell(ServerLevel level, BlockPos at) {
        for (int dy = 0; dy <= 6; dy++) {
            BlockPos c = at.above(dy);
            if (!level.isLoaded(c) || !level.isInWorldBounds(c)) return null;
            var here = level.getBlockState(c);
            if ((here.isAir() || here.canBeReplaced()) && level.getBlockState(c.above()).isAir()) return c;
        }
        return null;
    }

    /** The ids of the exclusive pocket dungeons that are loaded, in id order. */
    public static List<String> pocketIds() { return PocketDefs.ids(); }

    /**
     * Puts {@code p} into a fresh tomb of the pocket {@code pocketId} (the cap on concurrent tombs applies). False when the player is null, already inside a pocket, the id is unknown, the dungeon's
     * dimension is not loaded, no cell is free, or the tomb could not be raised. The way home is the place the player stands now.
     */
    public static boolean enter(ServerPlayer p, String pocketId) {
        try {
            if (p == null || p.getServer() == null) return false;
            MinecraftServer server = p.getServer();
            ServerLevel mega = MegaDungeon.level(server);
            PocketDef def = PocketDefs.get(pocketId);
            if (mega == null || def == null || RiftRules.slotAt(p.getX(), p.getZ()) >= 0 && p.level() == mega) return false;
            PocketRealm realm = PocketRealm.get(server);
            if (realm.exclusiveCount() >= RiftConfig.get(RiftConfig.EXCLUSIVE_MAX)) return false;
            int floor = 1;
            if (com.newuniverse.nusmp.labyrinth.Labyrinth.inMega(p.level())) {
                Sector s = MegaData.get(server).sectorAt(p.blockPosition());
                if (s != null) floor = Math.abs(s.firstFloor());
            }
            PocketRealm.Pocket pocket = ExclusivePockets.create(mega, realm, def, mega.getRandom().nextLong(), floor);
            if (pocket == null) return false;
            ExclusivePockets.join(mega, realm, pocket, def, List.of(p), null);
            return true;
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] RiftAdmin.enter failed for {}: {}", pocketId, ex.toString());
            return false;
        }
    }

    /**
     * Brings everything down: every pocket realm of the dungeon's dimension collapses (the players inside are sent home first; the exclusive tombs are swept clear over the next ticks) and every open
     * rift of {@code level} closes. Returns the number of pockets collapsed plus the number of tears closed; 0 when there was nothing, and 0 on a level of a server without the dungeon's dimension
     * (the tears of {@code level} are still closed).
     */
    public static int collapseAll(ServerLevel level) {
        int n = 0;
        try {
            if (level == null) return 0;
            MinecraftServer server = level.getServer();
            ServerLevel mega = server == null ? null : MegaDungeon.level(server);
            if (mega != null) {
                PocketRealm realm = PocketRealm.get(server);
                for (PocketRealm.Pocket p : new ArrayList<>(realm.pockets().values())) {
                    if (p.exclusive() && p.phase >= 3) continue;                                  // already being swept
                    if (realm.collapseSlot(mega, p.slot)) n++;
                }
            }
            for (BlockPos r : RiftService.openRifts(level)) {
                if (level.isLoaded(r) && level.getBlockEntity(r) instanceof DimensionalRiftBlockEntity be) {
                    be.close();
                    n++;
                }
            }
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] RiftAdmin.collapseAll failed: {}", ex.toString());
        }
        return n;
    }
}
