package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the collapse asks of the Nexus framework ({@link NexusZones}, the plans, the saved cells), behind one door. Each call is
 * guarded: if the framework throws (a zone whose plan fails, data that cannot be read), the failure is logged once and the answer is the
 * safe one, "there is no Nexus here" or "nothing to write back", so a broken zone can turn a collapse into a harmless pocket but never into
 * a crash of the server tick.
 */
final class CollapseHooks {
    private CollapseHooks() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();

    private static void warn(String what, Throwable failure) {
        if (WARNED.add(what + failure.getClass().getName())) LOG.warn("[nusmp] The Treasury Collapse could not {}; going on without it", what, failure);
    }

    /** The Nexus whose volume holds this block (planned, being built, standing or collapsed), or null if there is none or it cannot be read. */
    static NexusInfo containing(ServerLevel level, BlockPos pos) {
        try {
            return NexusZones.containing(level, pos);
        } catch (RuntimeException | LinkageError failure) {
            warn("look up the Nexus at " + pos.toShortString(), failure);
            return null;
        }
    }

    /** How far along the core cell is; COLLAPSED if it cannot be read (so a failing lookup never makes the collapse think it was reset). */
    static NexusState stateOf(MinecraftServer server, int cellX, int cellZ) {
        try {
            return NexusZones.stateOf(server, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            warn("read the state of cell (" + cellX + ", " + cellZ + ")", failure);
            return NexusState.COLLAPSED;
        }
    }

    /** The ways out of a standing Nexus, from its plan; with an unreadable plan, the middle of its floor. Never empty. */
    static List<int[]> exitsOf(NexusInfo info) {
        try {
            ZonePlan plan = NexusZones.plan(info.zone, info.cellX, info.cellZ);
            ZonePlan.Spot r = plan.rescuePoint();
            return CollapseExits.fromPlan(spots(plan.entrances), spots(plan.spawns(ZonePlan.ROLE_ARRIVAL)), new int[]{r.x(), r.y(), r.z()}, info.footprint, info.floorY);
        } catch (RuntimeException | LinkageError failure) {
            warn("read the plan of the " + info.zone.label, failure);
            return CollapseExits.fromPlan(null, null, null, info.footprint, info.floorY);
        }
    }

    /** Is this block where the plan of the Nexus puts its central relic? (The pedestal posts the collapse with its own position.) */
    static boolean isCentralRelic(NexusInfo info, BlockPos pos) {
        try {
            ZonePlan.Spot c = NexusZones.plan(info.zone, info.cellX, info.cellZ).centralRelic;
            return c != null && c.x() == pos.getX() && c.y() == pos.getY() && c.z() == pos.getZ();
        } catch (RuntimeException | LinkageError failure) {
            warn("read the central relic of the " + info.zone.label, failure);
            return false;
        }
    }

    /** Writes into the Nexus's saved cell that it has collapsed (it is never rebuilt by itself, it breeds nothing). */
    static void markCollapsed(ServerLevel level, BlockPos inside) {
        try {
            NexusZones.markCollapsed(level, inside);
        } catch (RuntimeException | LinkageError failure) {
            warn("mark the Nexus as collapsed", failure);
        }
    }

    static void markCentreRelicTaken(ServerLevel level, BlockPos inside) {
        try {
            NexusZones.markCentreRelicTaken(level, inside);
        } catch (RuntimeException | LinkageError failure) {
            warn("note that the central relic was taken", failure);
        }
    }

    private static List<int[]> spots(List<ZonePlan.Spot> in) {
        List<int[]> out = new ArrayList<>();
        for (ZonePlan.Spot s : in) out.add(new int[]{s.x(), s.y(), s.z()});
        return out;
    }
}
