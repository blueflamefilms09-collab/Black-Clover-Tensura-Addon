package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LegacySeal;
import com.newuniverse.nusmp.dungeon.city.CityBuilder;
import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.city.CitySave;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenCitySite;
import com.newuniverse.nusmp.dungeon.nexus.zone.MonarchVaultSite;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestMirageSite;
import com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldGateSite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The front door of the Variable Core for the rest of the mod: which site answers for which zone, the plan of any cell, what stands at
 * a position, whether the Dungeon's ecosystem may breed there, and the few facts other systems write back (the central relic was
 * taken, the Nexus collapsed). All of it is server side and needs the Dungeon's saved data; nothing here builds anything.
 */
public final class NexusZones {
    private NexusZones() {}

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    /** The zones whose failing {@code lifeAllowed} was logged already. */
    private static final java.util.Set<NexusZone> LIFE_WARNED = java.util.EnumSet.noneOf(NexusZone.class);

    /** The site that builds and runs a zone. */
    public static ZoneSite site(NexusZone zone) {
        return switch (zone) {
            case MONARCH_VAULT -> MonarchVaultSite.INSTANCE;
            case UNDERWORLD_GATE -> UnderworldGateSite.INSTANCE;
            case TEMPEST_MIRAGE -> TempestMirageSite.INSTANCE;
            case GOLDEN_CITY -> GoldenCitySite.INSTANCE;
        };
    }

    // ---------------------------------------------------------------- plans
    private record PlanKey(int cellX, int cellZ, NexusZone zone) {}

    private static final Map<PlanKey, ZonePlan> PLANS = new LinkedHashMap<PlanKey, ZonePlan>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PlanKey, ZonePlan> eldest) { return size() > 8; }
    };

    /** The plan of a cell as a zone would build it (kept for a few: planning is cheap but not free). */
    public static ZonePlan plan(NexusZone zone, int cellX, int cellZ) {
        PlanKey key = new PlanKey(cellX, cellZ, zone);
        ZonePlan p = PLANS.get(key);
        if (p == null) {
            p = site(zone).plan(cellX, cellZ);
            if (p == null || p.zone != zone) throw new IllegalStateException("the site of " + zone + " returned a plan for " + (p == null ? "nothing" : p.zone.toString()));
            if (!p.insideCell()) LOG.warn("[nusmp] The plan of the {} in cell ({}, {}) reaches outside its 128-block cell; lookups by position will miss the part that does", zone.label, cellX, cellZ);
            if (p.opsStrayFromFootprint()) LOG.warn("[nusmp] The plan of the {} in cell ({}, {}) places blocks in chunks its footprint does not cover; it only builds once those are loaded too", zone.label, cellX, cellZ);
            PLANS.put(key, p);
        }
        return p;
    }

    /** The plan of the Nexus of a cell, in the zone the cell has (built, forced or hashed). */
    public static ZonePlan planOf(MinecraftServer server, int cellX, int cellZ) { return plan(zoneOf(server, cellX, cellZ), cellX, cellZ); }

    // ---------------------------------------------------------------- zones and records
    /**
     * The record of a core cell, with a Golden City of an older version adopted on the way: the old builder's saved city (done, or part
     * built) becomes a Golden City record here, whatever zone the hash would give the cell, so it is never built over. Null if nothing is
     * recorded. An unstarted record (a forced zone, a broken seal) is returned as it is.
     */
    public static NexusSave.Entry resolve(MinecraftServer server, NexusSave save, int cellX, int cellZ) {
        long key = NexusSave.key(cellX, cellZ);
        NexusSave.Entry e = save.lookup(key);
        if (e != null && e.state.started()) return e;
        if (!DungeonTier.isCityCell(cellX, cellZ)) return e;
        CitySave.Entry legacy = CitySave.get(server).lookup(key);
        if (legacy == null) return e;
        CityPlan city = CityBuilder.plan(cellX, cellZ);
        NexusSave.Entry a = save.entry(key);
        a.zone = NexusZone.GOLDEN_CITY;
        a.fp = Footprint.oval(city.centreX, city.centreZ, city.semiX, city.semiZ);
        a.floorY = city.floorY;
        a.topY = CityPlan.CAP_Y;
        if (legacy.state == CitySave.DONE) {
            a.state = NexusState.DONE;
            a.sealBroken = a.sealBroken || LegacySeal.broken(server);
        } else {
            a.state = NexusState.PLAN;                                        // a half-built old city is built again from its start: the ops are idempotent
        }
        save.setDirty();
        return a;
    }

    /** The zone of a core cell: the one its build fixed, else a forced one, else the stable hash. */
    public static NexusZone zoneOf(MinecraftServer server, int cellX, int cellZ) {
        NexusSave save = NexusSave.get(server);
        resolve(server, save, cellX, cellZ);
        return save.zoneOf(cellX, cellZ);
    }

    /** The zone of the core cell that holds this column. */
    public static NexusZone zoneAt(ServerLevel level, int x, int z) { return zoneOf(level.getServer(), Math.floorDiv(x, NexusZone.CELL), Math.floorDiv(z, NexusZone.CELL)); }

    /** How far a core cell is along (no plan is made: cheap enough to ask about many cells). */
    public static NexusState stateOf(MinecraftServer server, int cellX, int cellZ) {
        NexusSave.Entry e = resolve(server, NexusSave.get(server), cellX, cellZ);
        return e == null ? NexusState.UNBUILT : e.state;
    }

    /** A look at a core cell; never null. A cell nothing was built in is described by the plan it would get. */
    public static NexusInfo at(MinecraftServer server, int cellX, int cellZ) {
        NexusSave save = NexusSave.get(server);
        NexusSave.Entry e = resolve(server, save, cellX, cellZ);
        NexusZone zone = save.zoneOf(cellX, cellZ);
        if (e != null && e.state.started() && e.fp != null) return new NexusInfo(cellX, cellZ, zone, e.state, e.fp, e.floorY, e.topY, e.relicTaken, e.sealBroken);
        ZonePlan plan = plan(zone, cellX, cellZ);
        return new NexusInfo(cellX, cellZ, zone, e == null ? NexusState.UNBUILT : e.state, plan.footprint, plan.floorY, plan.topY, e != null && e.relicTaken, e != null && e.sealBroken);
    }

    /** The Nexus whose volume holds this block (planned, being built, standing or collapsed); null if the block is in no Nexus. */
    public static NexusInfo containing(ServerLevel level, BlockPos pos) {
        int cx = Math.floorDiv(pos.getX(), NexusZone.CELL), cz = Math.floorDiv(pos.getZ(), NexusZone.CELL);
        if (!DungeonTier.isCityCell(cx, cz)) return null;
        MinecraftServer server = level.getServer();
        NexusSave save = NexusSave.get(server);
        NexusSave.Entry e = resolve(server, save, cx, cz);
        if (e == null || !e.state.started() || e.fp == null) return null;
        NexusInfo info = new NexusInfo(cx, cz, save.zoneOf(cx, cz), e.state, e.fp, e.floorY, e.topY, e.relicTaken, e.sealBroken);
        return info.contains(pos, 0) ? info : null;
    }

    /** The plan of the Nexus whose volume holds this block, or null. */
    public static ZonePlan planAt(ServerLevel level, BlockPos pos) {
        NexusInfo info = containing(level, pos);
        return info == null ? null : plan(info.zone, info.cellX, info.cellZ);
    }

    // ---------------------------------------------------------------- life
    /**
     * May the Dungeon's ecosystem breed in this chunk? True outside every Nexus. Inside one: not while it is planned, being built or
     * collapsed; once it stands, whatever its zone says ({@link ZoneSite#lifeAllowed}). A cheap test for the common case, a chunk that is
     * in no core cell.
     */
    public static boolean lifeAllowed(ServerLevel level, ChunkPos chunk) {
        int cx = Math.floorDiv(chunk.getMinBlockX(), NexusZone.CELL), cz = Math.floorDiv(chunk.getMinBlockZ(), NexusZone.CELL);
        if (!DungeonTier.isCityCell(cx, cz)) return true;
        NexusSave.Entry e = NexusSave.get(level.getServer()).lookup(NexusSave.key(cx, cz));
        if (e == null || !e.state.started() || e.fp == null || !e.fp.touchesChunk(chunk.x, chunk.z)) return true;
        if (e.state != NexusState.DONE) return false;
        NexusZone zone = e.zone == null ? NexusZone.of(cx, cz) : e.zone;
        try {
            return site(zone).lifeAllowed();
        } catch (RuntimeException | LinkageError failure) {                 // the ecosystem tick must never be brought down by a zone
            if (LIFE_WARNED.add(zone)) LOG.warn("[nusmp] The {} could not say whether life is allowed in it; life is allowed", zone.label, failure);
            return true;
        }
    }

    // ---------------------------------------------------------------- facts written back
    /** Has the central relic of the Nexus in this core cell been taken? */
    public static boolean centreRelicTaken(MinecraftServer server, int cellX, int cellZ) { return NexusSave.get(server).centreRelicTaken(NexusSave.key(cellX, cellZ)); }

    /** Has the central relic of the Nexus that holds this block been taken? False if the block is in no Nexus. */
    public static boolean centreRelicTaken(ServerLevel level, BlockPos pos) {
        NexusInfo info = containing(level, pos);
        return info != null && info.centreRelicTaken;
    }

    /** Records that the central relic of the Nexus that holds this block was taken (kept with the world). False if the block is in no Nexus. */
    public static boolean markCentreRelicTaken(ServerLevel level, BlockPos pos) {
        NexusInfo info = containing(level, pos);
        if (info == null) return false;
        NexusSave.get(level.getServer()).markCentreRelicTaken(info.key());
        return true;
    }

    /** Records that the Nexus that holds this block has collapsed: it is not rebuilt by itself, and it breeds nothing. False if the block is in no Nexus. */
    public static boolean markCollapsed(ServerLevel level, BlockPos pos) {
        NexusInfo info = containing(level, pos);
        if (info == null) return false;
        NexusSave.get(level.getServer()).markCollapsed(info.key());
        return true;
    }
}
