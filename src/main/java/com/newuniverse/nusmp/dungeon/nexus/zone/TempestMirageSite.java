package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.ZoneSite;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * The Tempest Mirage (docs/living_dungeon.md section 5, zone 3): a twisted, overgrown illusion of a peaceful monster village, with an
 * incredibly high biomass and an overwhelming swarm of mutated Tensura mobs under a hive-mind tactical AI. The plan is {@link TempestMiragePlan};
 * the swarm is made by {@link HiveMutant}, thought by {@link HiveTactics} and run by {@link HiveDriver}; a few houses are fakes
 * ({@link TempestIllusions}); the four gates in the shell are opened to the caves by {@link TempestPassages}; the mood is {@link TempestAmbience}.
 * Behind a mana-locked gate in the town hall lies the Treasury with the five relics; taking the central relic (the pedestal posts the event
 * itself) collapses the Nexus, and the swarm dissolves with the mirage.
 */
public final class TempestMirageSite implements ZoneSite {
    public static final TempestMirageSite INSTANCE = new TempestMirageSite();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The ground's richness is pushed up this often (calls of tickPlayers, about a second each). */
    private static final int PUSH_EVERY = 4;
    private static int calls;

    private TempestMirageSite() {}

    @Override
    public NexusZone zone() { return NexusZone.TEMPEST_MIRAGE; }

    @Override
    public ZonePlan plan(int cellX, int cellZ) { return TempestMiragePlan.of(cellX, cellZ); }

    /**
     * No: the swarm comes instead of the normal food web (docs/living_dungeon.md section 5, zone 3). The village's chunks are kept rich on
     * purpose (biomass and magicule, see {@link TempestAmbience#pushLife}), which is exactly what would make the Dungeon's own breeding
     * crowd every chunk of it with ordinary slimes, armoured things and apex predators, hundreds of them, outside the swarm's cap and its
     * hive mind. Only {@link HiveDriver} makes creatures here, and it is bounded ({@link HiveWaves#CAP}). The same switch also keeps the
     * Dungeon from overwriting the biomass the village is kept at (the Anti-Bird Compass reads it).
     */
    @Override
    public boolean lifeAllowed() { return false; }

    private static BlockPos pos(ZonePlan.Spot s) { return new BlockPos(s.x(), s.y(), s.z()); }

    // ---------------------------------------------------------------- populate
    @Override
    public void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre) {
        TempestMiragePlan.Layout layout = TempestMiragePlan.layout(cellX, cellZ);
        ZonePlan plan = layout.plan();
        try {
            placeRelics(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The relics of the Tempest Mirage of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeGate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mana gate of the Tempest Mirage of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            TempestAmbience.pushLife(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The ground of the Tempest Mirage of cell ({}, {}) could not be enriched", cellX, cellZ, failure);
        }
        try {
            HiveDriver.populate(level, layout);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The swarm of the Tempest Mirage of cell ({}, {}) could not be made", cellX, cellZ, failure);
        }
        TempestPassages.openAll(level, layout);
    }

    /** The central pedestal holds one of the five relics (a different one in each village), the four others hold the rest. */
    private static void placeRelics(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        Relic[] all = Relic.pool(com.newuniverse.nusmp.dungeon.sentient.SentientMode.active());
        if (all.length == 0) return;
        int central = TempestMiragePlan.centralRelicIndex(cellX, cellZ, all.length);
        if (plan.centralRelic != null) Relics.placePedestal(level, pos(plan.centralRelic), all[central], true);
        int i = 0;
        for (ZonePlan.Spot s : plan.minorRelics) {
            int off = 1 + i++;
            if (off % all.length == 0) continue;     // 1.22.0: never a twin of the central relic
            Relics.placePedestal(level, pos(s), all[(central + off) % all.length], false);
        }
    }

    /** The doorway is two high; if the gate the Treasury's relics agent places fills one cell, the cell above it is filled too. */
    private static void placeGate(ServerLevel level, ZonePlan plan) {
        for (ZonePlan.Spot g : plan.gates) {
            BlockPos lower = pos(g), upper = lower.above();
            Relics.placeManaGate(level, lower);
            if (level.getBlockState(upper).isAir()) Relics.placeManaGate(level, upper);
        }
    }

    // ---------------------------------------------------------------- while players are about
    /** About once a second while players are inside: the mood, the fakes, the swarm's waves, and every few seconds the ground's richness. */
    @Override
    public void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {
        TempestMiragePlan.Layout layout = TempestMiragePlan.layout(cellX, cellZ);
        long now = level.getGameTime();
        try {
            TempestAmbience.tick(level, inside);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mood of the Tempest Mirage failed", failure);
        }
        try {
            TempestIllusions.tick(level, layout, inside);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The fake houses of the Tempest Mirage failed", failure);
        }
        try {
            HiveDriver.waves(level, layout, inside, now);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The waves of the Tempest Mirage failed", failure);
        }
        if (++calls % PUSH_EVERY == 0) {
            try {
                TempestAmbience.pushLife(level, layout.plan());
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] The ground of the Tempest Mirage could not be enriched", failure);
            }
        }
    }

    /** About every five seconds while players are within 96 blocks: a gate still shut is tried again (the caves round it may only now be loaded). */
    @Override
    public void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {
        TempestPassages.retry(level, TempestMiragePlan.layout(cellX, cellZ));
    }
}
