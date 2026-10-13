package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.city.CityNexusBridge;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.ZoneSite;
import com.newuniverse.nusmp.dungeon.nexus.ZoneStage;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Golden City (docs/living_dungeon.md section 5, zone 4): the ultimate peak of the Dungeon's ecosystem. A beautiful, pristine biosphere,
 * one vast cavern garden under a glowing ceiling, with the old Golden City of 1.19.0 standing at its heart: golden trees and giant mushrooms,
 * moss meadows and flower beds, glow berries, amethyst, pools under waterfalls. Apex predators roam it freely over herds of grazers
 * ({@link GoldenWild}), and the garden shifts and regrows around whoever walks it ({@link Biosphere}). Four gates in its shell are opened to the
 * caves ({@link GoldenGates}); the city's own ways are opened as before ({@link CityNexusBridge}). Under the plaza, behind a mana-locked gate
 * ({@link GoldenTreasury}), lies the Treasury with the five relics; the central pedestal collapses the Nexus when it is emptied.
 *
 * <p>The plan is {@link GoldenCityPlan}; the city and the Treasury are built as stages ({@link GoldenStages}) after the garden's own ops. A
 * Golden City of the older builder that is already standing is adopted by the framework as this zone with its own, smaller footprint: it
 * keeps its ways and its guards but has no garden, so none of the garden's upkeep runs there.
 */
public final class GoldenCitySite implements ZoneSite {
    public static final GoldenCitySite INSTANCE = new GoldenCitySite();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** A city with no way in is looked at again, near a player, this often (ticks): the caves round it may only now be loaded. */
    private static final long WAYS_EVERY = 1200L;
    /** How long the mild air of the garden lasts (ticks), renewed every second while someone stands in it. */
    private static final int MILD_TICKS = 100;
    private static final Map<Long, Long> WAYS_TRIED = new HashMap<>();

    private GoldenCitySite() {}

    @Override
    public NexusZone zone() { return NexusZone.GOLDEN_CITY; }

    @Override
    public ZonePlan plan(int cellX, int cellZ) { return GoldenCityPlan.of(cellX, cellZ); }

    /**
     * No: the garden keeps its own creatures, apex predators and the herds they feed on, within the caps of {@link GoldenEcology} ({@link GoldenWild}
     * makes them, slowly, and takes the surplus away). The Dungeon's own breeding is not wanted on top of that: an open garden of fifty chunks
     * with room to stand everywhere would fill with hundreds of creatures of every tier, far past any cap, and the two would only undo each
     * other (births here, a creature taken away there). The garden's chunk biomass, which the breeding used to count, is kept by
     * {@code GoldenWild.weigh}.
     */
    @Override
    public boolean lifeAllowed() { return false; }

    /** After the garden's own ops: the old city (the centrepiece), then the Treasury cut under its plaza. */
    @Override
    public List<ZoneStage> stages(ZonePlan plan) { return List.of(GoldenStages.city(plan.cellX, plan.cellZ), GoldenStages.treasury(plan.cellX, plan.cellZ)); }

    private static BlockPos pos(ZonePlan.Spot s) { return new BlockPos(s.x(), s.y(), s.z()); }

    /** Is what stands in this cell the new garden? An older city adopted by the framework has its own, smaller footprint and no garden. */
    private static boolean isGarden(ServerLevel level, ZonePlan plan) {
        NexusInfo info = NexusZones.at(level.getServer(), plan.cellX, plan.cellZ);
        return info.footprint.equals(plan.footprint) && info.floorY == plan.floorY;
    }

    // ---------------------------------------------------------------- populate
    @Override
    public void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre) {
        ZonePlan plan = NexusZones.plan(NexusZone.GOLDEN_CITY, cellX, cellZ);
        GoldenGrid g = GoldenGrid.of(cellX, cellZ);
        try {
            placeRelics(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The relics of the Golden City of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeGate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mana gate of the Golden City of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            CityNexusBridge.openWays(level, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The ways into the old city of the Golden City of cell ({}, {}) could not be opened", cellX, cellZ, failure);
        }
        try {
            GoldenGates.openAll(level, g);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The gates of the Golden City of cell ({}, {}) could not be opened", cellX, cellZ, failure);
        }
        try {
            placeGuards(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The guards of the Golden City of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            GoldenWild.populate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The wildlife of the Golden City of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
    }

    /** The central pedestal holds one of the five relics (a different one in each city), the four others hold the rest. */
    private static void placeRelics(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        Relic[] all = Relic.pool(com.newuniverse.nusmp.dungeon.sentient.SentientMode.active());
        if (all.length == 0) return;
        int central = GoldenCityPlan.centralRelicIndex(cellX, cellZ, all.length);
        if (plan.centralRelic != null) Relics.placePedestal(level, pos(plan.centralRelic), all[central], true);
        int i = 0;
        for (ZonePlan.Spot s : plan.minorRelics) {
            int off = 1 + i++;
            if (off % all.length == 0) continue;     // 1.22.0: never a twin of the central relic
            Relics.placePedestal(level, pos(s), all[(central + off) % all.length], false);
        }
    }

    /** The doorway is two high; if the gate the relic code places fills one cell, the cell above it is filled too. */
    private static void placeGate(ServerLevel level, ZonePlan plan) {
        for (ZonePlan.Spot g : plan.gates) {
            BlockPos lower = pos(g), upper = lower.above();
            Relics.placeManaGate(level, lower);
            if (level.getBlockState(upper).isAir()) Relics.placeManaGate(level, upper);
        }
    }

    /** Two to four mid-tier creatures of the base Tensura mod stand guard on the plaza, as the old builder put them. */
    private static void placeGuards(ServerLevel level, ZonePlan plan) {
        int spawned = 0;
        for (ZonePlan.Spot s : plan.spawns(ZonePlan.ROLE_GUARD)) {
            try {
                if (Wildlife.spawnGuard(level, pos(s))) spawned++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A guard of the Golden City could not be spawned", failure);
            }
        }
        if (spawned == 0) LOG.warn("[nusmp] The Golden City of cell ({}, {}) has no guards", plan.cellX, plan.cellZ);
    }

    // ---------------------------------------------------------------- while players are about
    /**
     * About once a second while players are inside: the garden shifts and regrows round them, and its air stays mild. The roof garden and the
     * crowns of the tall trees lie above y = -40, where the Dungeon's tiers are the ones of the caves and a Volcanic Vents patch would scorch
     * whoever stands there with no warning: a short fire resistance, renewed while they are in the garden, keeps the biosphere pristine.
     */
    @Override
    public void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {
        ZonePlan plan = NexusZones.plan(NexusZone.GOLDEN_CITY, cellX, cellZ);
        if (!isGarden(level, plan)) return;
        for (ServerPlayer p : inside) {
            if (DungeonTier.at(level, p.blockPosition()) != DungeonTier.VOLCANIC_VENTS) continue;
            MobEffectInstance cool = p.getEffect(MobEffects.FIRE_RESISTANCE);
            if (cool == null || cool.getDuration() < MILD_TICKS / 2) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MILD_TICKS, 0, true, false));       // renewed before it runs out: no gap for the vent's heat
        }
        Biosphere.tick(level, GoldenGrid.of(cellX, cellZ), inside);
    }

    /** About every five seconds while players are within 96 blocks: a way or a gate still shut is tried again, and the wildlife is kept in balance. */
    @Override
    public void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {
        retryCityWays(level, cellX, cellZ);
        ZonePlan plan = NexusZones.plan(NexusZone.GOLDEN_CITY, cellX, cellZ);
        if (!isGarden(level, plan)) return;
        GoldenGates.retry(level, GoldenGrid.of(cellX, cellZ));
        GoldenWild.tick(level, plan);
    }

    /** The old city's ways: a city with none is looked at again now and then, as the old builder did. */
    private static void retryCityWays(ServerLevel level, int cellX, int cellZ) {
        if (CityNexusBridge.ways(level.getServer(), cellX, cellZ) > 0) return;
        long key = (((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL);
        long now = level.getGameTime();
        Long last = WAYS_TRIED.get(key);
        if (last != null && now >= last && now - last < WAYS_EVERY) return;
        WAYS_TRIED.put(key, now);
        CityNexusBridge.openWays(level, cellX, cellZ);
    }
}
