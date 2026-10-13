package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.ZoneSite;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Charybdis Basin (section 2, Type B of the design): the Deep Waterways' floor boss arena, a drowned circular basin 64 blocks across, flooded to
 * its roof, with ruins, sea lanterns and a sunken dais, three airlocks to the caves and a dry Treasury behind a mana gate. The plan is
 * {@link CharybdisBasinPlan}; the airlocks are opened to the caves by {@link CharybdisGates}; the leviathan that guards the relics is a
 * {@link CharybdisSpawnBoss}, woken by {@link CharybdisSurface} when someone comes near the dais. Taking the central relic (the pedestal posts the
 * event itself) or killing the Charybdis Spawn collapses the basin.
 *
 * <p>No ecosystem breeds here ({@link #lifeAllowed}), and {@link #populate} does the building only: the relics, the gate and the airlocks.
 */
public final class CharybdisBasinSite implements ZoneSite {
    public static final CharybdisBasinSite INSTANCE = new CharybdisBasinSite();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The basins that are coming down (cell keys), so the leviathan's fall does not post a second event. Cleared when the world closes. */
    private static final Set<Long> COLLAPSING = new HashSet<>();

    private CharybdisBasinSite() {}

    @Override
    public NexusZone zone() { return NexusZone.CHARYBDIS_BASIN; }

    @Override
    public ZonePlan plan(int cellX, int cellZ) { return CharybdisBasinPlan.of(cellX, cellZ); }

    /** The leviathan and its megalodons are the whole population: nothing breeds here. */
    @Override
    public boolean lifeAllowed() { return false; }

    private static BlockPos pos(ZonePlan.Spot s) { return new BlockPos(s.x(), s.y(), s.z()); }

    // ---------------------------------------------------------------- populate
    @Override
    public void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre) {
        ZonePlan plan = NexusZones.plan(NexusZone.CHARYBDIS_BASIN, cellX, cellZ);
        COLLAPSING.remove(NexusCells.key(cellX, cellZ));                                  // whatever was said of an earlier collapse of it no longer holds
        try {
            placeRelics(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The relics of the Charybdis Basin of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeGate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mana gate of the Charybdis Basin of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            CharybdisMinions.removeAll(level, CharybdisSurface.volume(plan));                 // a leviathan left from an earlier basin in this place goes
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not clear the old leviathan of the Charybdis Basin of cell ({}, {})", cellX, cellZ, failure);
        }
        CharybdisGates.openAll(level, plan);
    }

    /** The central pedestal holds one of the five relics (a different one in each basin), the four others hold the rest. */
    private static void placeRelics(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        Relic[] all = Relic.pool(com.newuniverse.nusmp.dungeon.sentient.SentientMode.active());
        if (all.length == 0) return;
        int central = CharybdisBasinPlan.centralRelicIndex(cellX, cellZ, all.length);
        if (plan.centralRelic != null) Relics.placePedestal(level, pos(plan.centralRelic), all[central], true);
        int i = 0;
        for (ZonePlan.Spot s : plan.minorRelics) {
            int off = 1 + i++;
            if (off % all.length == 0) continue;     // never a twin of the central relic
            Relics.placePedestal(level, pos(s), all[(central + off) % all.length], false);
        }
    }

    /** The doorway is two high and solid until now: the mana gate is set into both of its cells. */
    private static void placeGate(ServerLevel level, ZonePlan plan) {
        for (ZonePlan.Spot g : plan.gates) {
            BlockPos lower = pos(g);
            Relics.placeManaGate(level, lower);
            Relics.placeManaGate(level, lower.above());
        }
    }

    // ---------------------------------------------------------------- while players are about
    /** About once a second while players are inside: the leviathan surfaces when someone comes near the dais; megalodons whose boss is gone leave. */
    @Override
    public void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {
        ZonePlan plan = NexusZones.plan(NexusZone.CHARYBDIS_BASIN, cellX, cellZ);
        CharybdisSurface.tick(level, plan, centre, inside);
        if (level.getGameTime() % 100 < 20 && level.isLoaded(centre)) CharybdisMinions.sweepOrphans(level, CharybdisSurface.volume(plan));
    }

    /** About every five seconds while players are within 96 blocks: an airlock still shut is tried again (the caves round it may only now be loaded). */
    @Override
    public void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {
        CharybdisGates.retry(level, NexusZones.plan(NexusZone.CHARYBDIS_BASIN, cellX, cellZ));
    }

    // ---------------------------------------------------------------- the end
    /**
     * The basin collapses. If the leviathan fell, its megalodons have gone with it already (any left are swept); if the central relic was taken
     * first, the leviathan and its megalodons go now. The basin is found by the position of the event (a pedestal, a player).
     */
    public static void onCollapse(DungeonCollapseEvent event) {
        ServerLevel level = event.level();
        if (!LivingDungeon.in(level)) return;
        try {
            NexusInfo info = NexusZones.containing(level, event.centre());
            if (info == null || info.zone != NexusZone.CHARYBDIS_BASIN) return;
            ZonePlan plan = NexusZones.plan(NexusZone.CHARYBDIS_BASIN, info.cellX, info.cellZ);
            COLLAPSING.add(info.key());
            AABB volume = CharybdisSurface.volume(plan);
            if (event.bossDefeated()) CharybdisMinions.sweepOrphans(level, volume);
            else CharybdisMinions.removeAll(level, volume);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Charybdis Basin could not clean up after its collapse", failure);
        }
    }

    /**
     * The leviathan fell at 'arena': the basin it guarded comes down (the same event the central relic posts), exactly once, and the server is
     * told. A leviathan made by hand outside any basin has nothing to bring down.
     */
    static void fell(ServerLevel level, BlockPos arena, ServerPlayer killer) {
        try {
            NexusInfo info = NexusZones.containing(level, arena);
            if (info == null || info.zone != NexusZone.CHARYBDIS_BASIN) return;
            long key = info.key();
            boolean already = COLLAPSING.contains(key) || info.state == NexusState.COLLAPSED || info.centreRelicTaken;
            String who = killer == null ? null : killer.getName().getString();
            DungeonMaw.broadcast(level.getServer(), Component.literal(already ? CharybdisText.fallenDuringCollapse(who) : CharybdisText.fallen(who))
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
            if (already) return;
            COLLAPSING.add(key);
            NeoForge.EVENT_BUS.post(new DungeonCollapseEvent(level, info.centre(), NexusZone.CHARYBDIS_BASIN, killer, true));
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not announce the fall of the Charybdis Spawn", failure);
        }
    }

    /** The world is closing: nothing of it (the levels, the entities) may stay behind in the statics. */
    public static void onServerStopping(ServerStoppingEvent event) {
        COLLAPSING.clear();
        CharybdisSurface.clear();
        CharybdisGates.clear();
        CharybdisMinions.clear();
    }
}
