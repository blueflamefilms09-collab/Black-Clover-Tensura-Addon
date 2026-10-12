package com.newuniverse.nusmp.dungeon.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired on the NeoForge event bus when a Nexus is robbed: the central Relic is taken from its pedestal, or the zone's boss dies
 * (docs/living_dungeon.md section 8). The collapse manager answers it: a broadcast, the Dungeon's life drained, the walls coming down and a
 * three minute countdown to get out.
 */
public class DungeonCollapseEvent extends Event {
    private final ServerLevel level;
    private final BlockPos centre;
    private final NexusZone zone;
    private final ServerPlayer cause;
    private final boolean bossDefeated;

    public DungeonCollapseEvent(ServerLevel level, BlockPos centre, NexusZone zone, ServerPlayer cause, boolean bossDefeated) {
        this.level = level;
        this.centre = centre;
        this.zone = zone;
        this.cause = cause;
        this.bossDefeated = bossDefeated;
    }

    public ServerLevel level() { return level; }

    /** The middle of the Nexus that is collapsing. */
    public BlockPos centre() { return centre; }

    public NexusZone zone() { return zone; }

    /** Whoever took the relic or dealt the killing blow; null when the cause is not a player. */
    public ServerPlayer cause() { return cause; }

    public boolean bossDefeated() { return bossDefeated; }
}
