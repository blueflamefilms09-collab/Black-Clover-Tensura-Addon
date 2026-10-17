package com.newuniverse.nusmp.avatar.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the goals of one Avatar share a few facts that no single goal owns (what it is afraid of, how long its creator has stood still, whether the
 * low-health line already fired, when it last pressed a skill, the drop it is walking to). One small mutable holder per Avatar, created by
 * {@code AvatarAi.install}, keeps them without touching the entity class. Server side only.
 */
public final class AvatarMind {
    /** The boss a Cowardly Avatar is currently afraid of (within 24 blocks), or null. */
    public LivingEntity fear;
    /** Where the creator was last seen standing, and for how many ticks they have not moved (Lazy idles when this is long). */
    public Vec3 creatorSpot = Vec3.ZERO;
    public int creatorStillTicks;
    /** The low-health bark fires once per drop, until the creator has recovered. */
    public boolean lowHpLatched;
    /** The game time of the last skill press attempt: a long silence before it means a new fight has begun (a buff goes first). */
    public long lastEngagedAt = -10_000;
}
