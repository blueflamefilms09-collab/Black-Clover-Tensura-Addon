package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.List;

/**
 * The Goliath's Pit (the Upper Catacombs' floor boss arena, a Nexus cell of the Variable Core's framework): a ruined colosseum of pale stone brick with a
 * seal of bedrock in the middle and, on one side, a Treasury behind a mana-locked gate. The plan is {@link GoliathPitPlan}; the three sealed gates of
 * its wall are opened to the caves by {@link GoliathPitGates}; the Goliath is made by {@link GoliathBoss}. Nothing lives in the pit until a player comes
 * near the seal: then the Goliath rises out of it (three seconds of cracking, then the climb), once. Taking the central relic (the pedestal posts the
 * event itself) or killing the Goliath collapses the pit; a Goliath still standing when it collapses crumbles ({@link #onCollapse}). Nothing breeds here.
 */
public final class GoliathPitSite implements ZoneSite {
    public static final GoliathPitSite INSTANCE = new GoliathPitSite();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private GoliathPitSite() {}

    @Override
    public NexusZone zone() { return NexusZone.GOLIATH_PIT; }

    @Override
    public ZonePlan plan(int cellX, int cellZ) { return GoliathPitPlan.of(cellX, cellZ); }

    /** No ecosystem: nothing breeds in the pit. */
    @Override
    public boolean lifeAllowed() { return false; }

    private static BlockPos pos(ZonePlan.Spot s) { return new BlockPos(s.x(), s.y(), s.z()); }

    /** The air of the pit, and a little round it: where a Goliath of this pit can be. */
    private static AABB volume(ZonePlan plan) {
        return new AABB(plan.footprint.minX(), plan.floorY - 2, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
    }

    // ---------------------------------------------------------------- populate
    @Override
    public void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre) {
        ZonePlan plan = NexusZones.plan(NexusZone.GOLIATH_PIT, cellX, cellZ);
        try {
            placeRelics(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The relics of the Goliath's Pit of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeGate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mana gate of the Goliath's Pit of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            clearGoliaths(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The old Goliath of the pit of cell ({}, {}) could not be cleared", cellX, cellZ, failure);
        }
        try {
            GoliathPitGates.openAll(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The gates of the Goliath's Pit of cell ({}, {}) could not be opened", cellX, cellZ, failure);
        }
    }

    /** The central pedestal holds one of the relics (a different one in each pit), the others hold the rest. */
    private static void placeRelics(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        Relic[] all = Relic.values();
        if (all.length == 0) return;
        int central = GoliathPitPlan.centralRelicIndex(cellX, cellZ, all.length);
        if (plan.centralRelic != null) Relics.placePedestal(level, pos(plan.centralRelic), all[central], true);
        int i = 0;
        for (ZonePlan.Spot s : plan.minorRelics) Relics.placePedestal(level, pos(s), all[(central + 1 + i++) % all.length], false);
    }

    /** The doorway is two high; if the gate the relics agent places fills one cell, the cell above it is filled too. */
    private static void placeGate(ServerLevel level, ZonePlan plan) {
        for (ZonePlan.Spot g : plan.gates) {
            BlockPos lower = pos(g), upper = lower.above();
            Relics.placeManaGate(level, lower);
            if (level.getBlockState(upper).isAir()) Relics.placeManaGate(level, upper);
        }
    }

    /** A pit that is made again (an admin rebuilt it) starts without a Goliath: one left from its earlier life goes. */
    private static void clearGoliaths(ServerLevel level, ZonePlan plan) {
        for (GoliathBoss old : level.getEntitiesOfClass(GoliathBoss.class, volume(plan))) old.discard();
    }

    // ---------------------------------------------------------------- while players are about
    /**
     * About once a second while players are inside: a player (not in creative mode) within the trigger range of the seal wakes the Goliath, if the pit
     * stands (the framework only calls this for a pit that is built and not collapsed), its central relic is still there and no Goliath of the pit lives.
     * While it sleeps, a slow heartbeat sounds under the floor.
     */
    @Override
    public void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {
        ZonePlan plan = NexusZones.plan(NexusZone.GOLIATH_PIT, cellX, cellZ);
        if (!level.getEntitiesOfClass(GoliathBoss.class, volume(plan), g -> !g.isRemoved()).isEmpty()) return;
        if (NexusZones.centreRelicTaken(level.getServer(), cellX, cellZ)) return;            // robbed: the pit is coming down, nothing rises
        int[] seal = GoliathPitPlan.sealCentre(cellX, cellZ);
        for (ServerPlayer p : inside) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (GoliathRise.triggers(p.getX() - (seal[0] + 0.5), p.getY() - (seal[1] + 1.0), p.getZ() - (seal[2] + 0.5))) {
                wake(level, plan, seal, p);
                return;
            }
        }
        if (level.getGameTime() % 100 < 20) level.playSound(null, centre.above(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 2.0f, 0.5f);
    }

    /** The Goliath is made in the seal, facing the way players arrive, and the server is told who woke it. */
    private static void wake(ServerLevel level, ZonePlan plan, int[] seal, ServerPlayer cause) {
        List<ZonePlan.Spot> arrival = plan.spawns(ZonePlan.ROLE_ARRIVAL);
        double fx = arrival.isEmpty() ? cause.getX() - (seal[0] + 0.5) : arrival.get(0).x() - (seal[0] + 0.5);
        double fz = arrival.isEmpty() ? cause.getZ() - (seal[2] + 0.5) : arrival.get(0).z() - (seal[2] + 0.5);
        float yaw = (float) Math.toDegrees(Math.atan2(-fx, fz));
        GoliathBoss g = GoliathBoss.spawnRising(level, new BlockPos(seal[0], seal[1], seal[2]), yaw, true);
        if (g == null) {
            LOG.warn("[nusmp] The Goliath of the pit of cell ({}, {}) could not be made", plan.cellX, plan.cellZ);
            return;
        }
        DungeonMaw.broadcast(level.getServer(), Component.literal(GoliathText.woken(cause.getName().getString())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** About every five seconds while players are within 96 blocks: a gate still shut is tried again (the caves round it may only now be loaded). */
    @Override
    public void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {
        GoliathPitGates.retry(level, NexusZones.plan(NexusZone.GOLIATH_PIT, cellX, cellZ));
    }

    // ---------------------------------------------------------------- the end
    /**
     * The Nexus is collapsing (the central relic was taken, or the Goliath fell): a Goliath still standing in the pit crumbles into rubble. The pit is
     * found by the position of the event, which says where the trouble is (a pedestal, a player) and carries the cell's hashed zone, not necessarily the
     * zone the cell was built as.
     */
    public static void onCollapse(DungeonCollapseEvent event) {
        ServerLevel level = event.level();
        if (!LivingDungeon.in(level)) return;
        try {
            NexusInfo info = NexusZones.containing(level, event.centre());
            if (info == null || info.zone != NexusZone.GOLIATH_PIT) return;
            ZonePlan plan = NexusZones.plan(NexusZone.GOLIATH_PIT, info.cellX, info.cellZ);
            for (GoliathBoss g : level.getEntitiesOfClass(GoliathBoss.class, volume(plan), b -> b.isAlive())) g.crumble();
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Goliath's Pit could not clean up after its collapse", failure);
        }
    }

    /** The world is closing: nothing is remembered of when a gate was last tried. */
    public static void onServerStopping(ServerStoppingEvent event) { GoliathPitGates.forget(); }
}
