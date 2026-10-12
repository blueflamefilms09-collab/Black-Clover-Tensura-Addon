package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Underworld Gate (docs/living_dungeon.md section 5, zone 2): a dark, gravity-warped ruin of obsidian and blackstone in the Spade
 * Kingdom's manner, with a vast arch at its heart and the Treasury behind a mana-locked gate in the arch's portal wall. Players have to
 * get through gravity zones that press, loosen and flip on a visible slow cycle ({@link GravityWarp}), past high Devils (a Gatekeeper, lesser
 * daemons) and Dark Disciples channelling Lucifero, Megicula and Beelzebub ({@link UnderworldDirector}). Taking the central relic from its
 * pedestal (the pedestal posts the event itself) collapses the Nexus. The plan is {@link UnderworldGatePlan}; the gates of the shell are
 * opened to the caves by {@link UnderworldGates}. Nothing breeds here.
 */
public final class UnderworldGateSite implements ZoneSite {
    public static final UnderworldGateSite INSTANCE = new UnderworldGateSite();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Who has been told about the gravity of the hall in this session. */
    private static final Set<UUID> WELCOMED = new HashSet<>();
    /** The core cells (NexusCells keys) whose hall was told to collapse in this session: their gravity stays off and their Devils and Disciples stay gone until the hall is made again. */
    private static final Set<Long> COLLAPSING = new HashSet<>();

    private UnderworldGateSite() {}

    /** The world is closing: nobody is welcomed twice by what the next world remembers. */
    static void forget() {
        WELCOMED.clear();
        COLLAPSING.clear();
    }

    @Override
    public NexusZone zone() { return NexusZone.UNDERWORLD_GATE; }

    @Override
    public ZonePlan plan(int cellX, int cellZ) { return UnderworldGatePlan.of(cellX, cellZ); }

    /** No ecosystem: nothing breeds in the ruin; its creatures are the ones put there. */
    @Override
    public boolean lifeAllowed() { return false; }

    private static BlockPos pos(ZonePlan.Spot s) { return new BlockPos(s.x(), s.y(), s.z()); }

    // ---------------------------------------------------------------- populate
    @Override
    public void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre) {
        ZonePlan plan = NexusZones.plan(NexusZone.UNDERWORLD_GATE, cellX, cellZ);
        COLLAPSING.remove(NexusCells.key(cellX, cellZ));                      // a hall made again has not collapsed
        try {
            placeRelics(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The relics of the Underworld Gate of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeGate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mana gate of the Underworld Gate of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            UnderworldDirector.populate(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The creatures of the Underworld Gate of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            UnderworldGates.openAll(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The gates of the Underworld Gate of cell ({}, {}) could not be opened", cellX, cellZ, failure);
        }
    }

    /** The central pedestal holds one of the five relics (a different one in each ruin), the four others hold the rest. */
    private static void placeRelics(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        Relic[] all = Relic.values();
        if (all.length == 0) return;
        int central = UnderworldGatePlan.centralRelicIndex(cellX, cellZ, all.length);
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

    // ---------------------------------------------------------------- while players are about
    /** About once a second while players are inside: the gravity, the Gatekeeper, the Disciples. */
    @Override
    public void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {
        if (COLLAPSING.contains(NexusCells.key(cellX, cellZ))) return;       // coming down: the gravity has let go and the Devils are gone, and neither comes back
        ZonePlan plan = NexusZones.plan(NexusZone.UNDERWORLD_GATE, cellX, cellZ);
        for (ServerPlayer p : inside) {
            if (WELCOMED.add(p.getUUID())) p.displayClientMessage(Component.literal(UnderworldText.ENTER).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        GravityWarp.watch(level, cellX, cellZ, inside);
        UnderworldDirector.tick(level, plan, cellX, cellZ, inside);
    }

    /** About every five seconds while players are within 96 blocks: a gate still shut is tried again (the caves round it may only now be loaded). */
    @Override
    public void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {
        UnderworldGates.retry(level, NexusZones.plan(NexusZone.UNDERWORLD_GATE, cellX, cellZ), cellX, cellZ);
    }

    // ---------------------------------------------------------------- the end
    /**
     * The Nexus collapses: the gravity lets everyone go, and the Devils and Disciples of the ruin go with it, for good (the hall's once-a-second
     * look stops feeding the gravity and bringing Disciples back). The hall is found by the position of the event, which says where the trouble
     * is (a pedestal, a player) and carries the cell's hashed zone, not necessarily the zone the cell was built as.
     */
    public static void onCollapse(DungeonCollapseEvent event) {
        ServerLevel level = event.level();
        if (!LivingDungeon.in(level)) return;
        try {
            NexusInfo info = NexusZones.containing(level, event.centre());
            if (info == null || info.zone != NexusZone.UNDERWORLD_GATE) return;
            ZonePlan plan = NexusZones.plan(NexusZone.UNDERWORLD_GATE, info.cellX, info.cellZ);
            COLLAPSING.add(info.key());
            GravityWarp.release(level.getServer(), plan.cellX, plan.cellZ);
            UnderworldDirector.clear(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Underworld Gate could not clean up after its collapse", failure);
        }
    }
}
