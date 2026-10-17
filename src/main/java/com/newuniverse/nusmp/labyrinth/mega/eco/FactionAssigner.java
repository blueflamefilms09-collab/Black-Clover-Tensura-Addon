package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SectorListener;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: a faction is only at home somewhere if the dungeon says where, and the dungeon says it when a sector has just been placed: every room that
 * is not a safe room or a corridor is given a native faction, written to {@link Room#faction} (the one field the encounter director reads to prefer
 * "the room's own kind" when it spawns, and this package reads to give a faction its defending reach). The faction comes from the room's template
 * ({@code nusmp:faction;faction=<id>} markers, which the room builders place once per hall) and otherwise from a seeded draw weighted by the sector's
 * theme ({@link NativeFactions}), so a crypt is undead because its template says so and an unmarked arena is whatever the region favours. It
 * runs once per generation and is safe to run again: a room that already has a known faction keeps it (a re-run of the same plan gives the same
 * answers anyway, and an admin's {@code /faction set} must not be overwritten), and the sector is saved dirty only when something changed. Nothing
 * here may break generation, so any failure is logged and swallowed.
 */
public final class FactionAssigner implements SectorListener {
    private static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onSectorGenerated(ServerLevel level, Sector sector) {
        try {
            FactionGraph graph = FactionRegistry.graph();
            if (graph.size() == 0) {
                LOG.warn("[nusmp] Sector {} has no native factions: no faction file is loaded", sector.id);
                return;
            }
            List<Marker> marks = sector.markers(MarkerKind.FACTION);
            String theme = sector.theme.id();
            int changed = 0;
            for (Room room : sector.rooms) {
                if (!NativeFactions.eligible(room.kind) || (room.faction != null && graph.knows(room.faction))) continue;
                String hint = null;
                for (Marker m : marks) {
                    if (room.contains(m.pos())) {
                        hint = m.arg("faction", null);
                        break;
                    }
                }
                String chosen = NativeFactions.choose(graph, sector.seed, room.id, theme, hint);
                if (chosen != null && !chosen.equals(room.faction)) {
                    room.faction = chosen;
                    changed++;
                }
            }
            if (changed > 0) MegaData.get(level.getServer()).setDirty();
            LOG.info("[nusmp] Sector {} ({}): {} rooms got a native faction", sector.id, theme, changed);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] Native factions of sector {} could not be assigned", sector.id, ex);
        }
    }
}
