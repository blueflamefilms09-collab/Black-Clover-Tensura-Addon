package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaThemes;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Why: this is the floor-generation hook of the brief. The Mega-Dungeon builds a boss floor from its templates (the cyclic WFC engine and the jigsaw pools stay exactly as they are), the generator
 * announces {@code onSectorGenerated}, and this listener, registered like every other system's, lays the boss's THEMATIC HALL over the Guardian's hall of that floor: the same footprint (45 blocks),
 * turned to the way that floor's own Gatekeeper faces, with the cells the template owns (the two doorways, the altar column) reserved so the Guardian Altar, the Gatekeeper and the jigsaw doors
 * of the floor keep working untouched. The hall is built in steps over the next ticks ({@link DecennialArenas#stepBuilds}), long before a party reaches the floor, and its progress is saved, so a
 * stop in the middle resumes. Nothing is built when there is no boss for the level (the old hall stays, which is also what {@code arenas = false} and {@code enabled = false} give), and nothing here
 * touches the engine or the templates: the only coupling to the generator is the listener interface it already has.
 */
public final class DecennialArenaHook implements SectorListener {
    private static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onSectorGenerated(ServerLevel level, Sector sector) {
        try {
            hook(level, sector);
        } catch (RuntimeException e) {
            LOG.error("[nusmp] The Decennial hall of sector {} could not be planned (the old hall stays): {}", sector == null ? -1 : sector.id, e.toString());
        }
    }

    /** The frame a hall takes in a floor, or null when the floor's own landmarks do not say where its hall is. Public so the self-test can check the arithmetic against a real sector. */
    public static ArenaFrame frameOf(ServerLevel level, Sector sector) {
        BlockPos g = sector.guardianSpawn;
        if (g == null) return null;
        int cx = g.getX(), cz = g.getZ();
        Room arena = sector.roomAt(g);
        if (arena != null && arena.kind == RoomKind.ARENA && arena.box.getXSpan() == BossArenaStructure.SECTOR_SIZE && arena.box.getZSpan() == BossArenaStructure.SECTOR_SIZE) {
            int bx = (arena.box.minX() + arena.box.maxX()) / 2, bz = (arena.box.minZ() + arena.box.maxZ()) / 2;
            if (Math.abs(bx - cx) <= 1 && Math.abs(bz - cz) <= 1) {
                cx = bx;
                cz = bz;
            }
        }
        int y0 = MegaGrid.FLOOR_Y;
        if (level.getBlockState(g.below()).is(GuardRegistry.GUARDIAN_ALTAR.get())) y0 = g.getY() - 4;                  // the altar stands three above the floor, the marker one above the altar
        int rot = sector.gatekeeper == null ? 0 : ArenaFrame.rotationFacing(sector.gatekeeper.getX() - cx, sector.gatekeeper.getZ() - cz);
        return new ArenaFrame(cx, y0, cz, BossArenaStructure.SECTOR_SIZE, rot);
    }

    private static void hook(ServerLevel level, Sector sector) {
        if (sector == null || !DecennialConfig.get(DecennialConfig.ENABLED) || !DecennialConfig.get(DecennialConfig.ARENAS)) return;
        if (!sector.isBossFloor()) return;
        int level0 = DecennialRules.levelOfFloor(sector.lastFloor());
        DecennialProfile p = level0 == 0 ? null : DecennialRoster.forLevel(level0);
        if (p == null) return;                                                  // no boss for this level: the Guardian's old hall stays
        BossArenaStructure plan = ArenaThemes.buildForFloor(p.theme());
        if (plan == null) {
            LOG.warn("[nusmp] The boss of level {} names the unknown hall '{}': the old hall stays.", level0, p.theme());
            return;
        }
        ArenaFrame frame = frameOf(level, sector);
        if (frame == null) {
            LOG.warn("[nusmp] Sector {} has no Guardian spawn marker: its hall is left as the template made it.", sector.id);
            return;
        }
        DecennialArenas.registerSector(level, sector.id, p, plan, frame);
        LOG.info("[nusmp] Sector {} (floor {}) gets the hall '{}' for {} ({}), turned {}.", sector.id, sector.lastFloor(), p.theme(), p.name(), p.id(), frame.rot());
    }
}
