package com.newuniverse.nusmp.dungeon.nexus.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What {@link MonarchDriver} keeps about one awake or sleeping Blood-Red Commander while its chunk is loaded: its boss bar, its scheduler, the
 * phase it is in, the attacks in the air ({@link Hazard}s), the soldiers that serve it and how many times they were called, who was struck
 * when. Nothing here is saved: a Commander that is loaded again starts a fresh fight from its health, and its marks in the entity's own data.
 */
final class MonarchFight {
    /** A thing the Monarch has set going: a telegraphed ring, circles, a line, a field, a summoning. Ticked every game tick until it says it is over. */
    interface Hazard {
        /** True when it is finished. */
        boolean tick(ServerLevel level, Mob boss, MonarchFight fight, long now);
    }

    final UUID boss;
    final BlockPos centre;
    /** A red bar with twenty notches, so the two thresholds of the fight (half the bar and a quarter of it) both stand on a notch. */
    final ServerBossEvent bar = new ServerBossEvent(Component.literal(MonarchText.NAME), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
    final MonarchScheduler scheduler = new MonarchScheduler();
    final List<Hazard> hazards = new ArrayList<>();
    final List<UUID> soldiers = new ArrayList<>();
    final Map<UUID, Long> lastHit = new HashMap<>();
    /** Players the field of Authority has slowed, so the slowness can be taken off them when the fight ends. */
    final Set<UUID> slowed = new HashSet<>();
    int phase = 1;
    /** 1.35.0: the second health bar. The refill state machine, and whether the last phase has been entered (then the phase is held at 3 whatever the health, and the moves are enraged). */
    final com.newuniverse.nusmp.boss.refill.PhaseRefill refill = new com.newuniverse.nusmp.boss.refill.PhaseRefill();
    boolean finalBar;
    /** Awake and fighting; false while it sleeps. */
    boolean awake;
    /** Stands still (brain off) until this tick; and invulnerable until this other one (the roar between phases). */
    long frozenUntil, transitionUntil;
    long lastFoe, nextMelee, nextThink;
    /** The last player who hurt it, for the credit if it falls to something else. */
    UUID lastAttacker;
    /** A melee strike that threw once is not tried again. */
    boolean cannotStrike;
    /** How many times the soldiers have been called in this fight, the tick of the last time, and whether a threshold was crossed that the roar has not yet been followed by the summoning. */
    int summons;
    long lastSummonAt;
    boolean summonPending;

    MonarchFight(UUID boss, BlockPos centre) {
        this.boss = boss;
        this.centre = centre;
        bar.setVisible(true);
    }

    Vec3 centreVec() { return new Vec3(centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5); }

    boolean frozen(long now) { return now < frozenUntil; }

    boolean transitioning(long now) { return now < transitionUntil; }
}
