package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.ZoneSite;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import com.newuniverse.nusmp.dungeon.sentient.SentientMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The Monarch's Vault (docs/living_dungeon.md section 5, zone 1): a colossal hollow chamber of obsidian piled with gold, artifacts and bones,
 * with no ecosystem in it, one towering Boss asleep on the dais in the middle, and behind a mana-locked gate the Treasury with the five
 * relics. The plan is {@link MonarchVaultPlan}; the Monarch is made by {@link MonarchBoss} and run by {@link MonarchDriver}; the gates in the
 * shell are opened to the caves by {@link MonarchGates}. Taking the central relic (the pedestal posts the event itself) or killing the Monarch
 * collapses the Nexus.
 */
public final class MonarchVaultSite implements ZoneSite {
    public static final MonarchVaultSite INSTANCE = new MonarchVaultSite();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private MonarchVaultSite() {}

    @Override
    public NexusZone zone() { return NexusZone.MONARCH_VAULT; }

    @Override
    public ZonePlan plan(int cellX, int cellZ) { return MonarchVaultPlan.of(cellX, cellZ); }

    /** No ecosystem: nothing breeds here. */
    @Override
    public boolean lifeAllowed() { return false; }

    private static BlockPos pos(ZonePlan.Spot s) { return new BlockPos(s.x(), s.y(), s.z()); }

    // ---------------------------------------------------------------- populate
    @Override
    public void populate(ServerLevel level, int cellX, int cellZ, BlockPos centre) {
        ZonePlan plan = NexusZones.plan(NexusZone.MONARCH_VAULT, cellX, cellZ);
        try {
            placeRelics(level, plan, cellX, cellZ);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The relics of the Monarch's Vault of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeGate(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The mana gate of the Monarch's Vault of cell ({}, {}) could not be placed", cellX, cellZ, failure);
        }
        try {
            placeMonarch(level, plan, centre);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Monarch of the Vault of cell ({}, {}) could not be made", cellX, cellZ, failure);
        }
        MonarchGates.openAll(level, plan);
    }

    /**
     * The central pedestal holds one of the Treasury's relics (a different one in each hall), the others hold the rest. The pool is the four relics
     * of the Sentient Dungeon, or the five of 1.21.0 (in their old order, so a hall keeps its central relic) when it is switched off.
     */
    private static void placeRelics(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        Relic[] all = Relic.pool(SentientMode.active());
        if (all.length == 0) return;
        int central = MonarchVaultPlan.centralRelicIndex(cellX, cellZ, all.length);
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

    /** Exactly one Monarch: any left from an earlier hall in this place goes. */
    private static void placeMonarch(ServerLevel level, ZonePlan plan, BlockPos centre) {
        AABB box = new AABB(plan.footprint.minX(), plan.floorY - 2, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
        for (Mob old : level.getEntitiesOfClass(Mob.class, box, e -> MonarchBoss.is(e) || MonarchBoss.isSoldier(e))) old.discard();
        MonarchDriver.forgetCollapse(centre);
        MonarchBoss.spawn(level, centre);
    }

    // ---------------------------------------------------------------- while players are about
    /** About once a second while players are inside: entering the Treasury wakes the Monarch (the relics are about to be taken); a slow heartbeat under the floor while it sleeps. */
    @Override
    public void tickPlayers(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> inside) {
        int[] t = MonarchVaultPlan.treasuryBounds(cellX, cellZ);
        for (ServerPlayer p : inside) {
            if (p.isCreative()) continue;
            if (p.getBlockX() >= t[0] && p.getBlockX() <= t[3] && p.getBlockY() >= t[1] - 1 && p.getBlockY() <= t[4] && p.getBlockZ() >= t[2] && p.getBlockZ() <= t[5]) {
                MonarchDriver.rouseAt(level, centre, p);
                break;
            }
        }
        if (level.getGameTime() % 100 < 20 && !MonarchDriver.fighting(centre)) {
            level.playSound(null, centre.above(MonarchVaultPlan.DAIS_TOP), SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.AMBIENT, 2.0f, 0.6f);
        }
    }

    /** About every five seconds while players are within 96 blocks: a gate still shut is tried again (the caves round it may only now be loaded). */
    @Override
    public void tickNear(ServerLevel level, int cellX, int cellZ, BlockPos centre, List<ServerPlayer> near) {
        MonarchGates.retry(level, NexusZones.plan(NexusZone.MONARCH_VAULT, cellX, cellZ));
    }
}
