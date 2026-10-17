package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Why: resting is a capability the PLAYER carries: how many short rests they have spent since their last long rest, when that was (the
 * cool-down is measured in game time, so it survives logouts and restarts), and the rest they are in the middle of right now. It is a NeoForge
 * data attachment on the player ({@link SafeRegistry#REST_STATE}) rather than persistent NBT keys because an attachment is typed, has a codec,
 * costs nothing for players who never rest (nothing is created or saved until the first rest, and a default state is not saved at all) and is
 * dropped on death on purpose ({@code copyOnDeath} is not set: a new body starts with all its short rests and no cool-down, the dungeon
 * punishes dying with the walk back, not with a rest timer). The CHANNEL (the rest in progress: kind, start, anchor, ambush time) is
 * deliberately NOT saved: a rest cannot survive a logout or a restart, it simply ends, so a crash can never leave someone "resting" for ever and
 * the saved state stays three numbers. The spent count is stored (not "left") so changing {@code shortRestUses} in the config never leaves a
 * player with a stale full or negative count.
 */
public final class RestState {
    /** Short rests used since the last long rest. */
    public int shortRestsUsed;
    /** Game tick of the last completed long rest, or -1 for never. */
    public long lastLongRest = -1L;
    /** Game tick of the last completed rest of either kind, or -1 (shown by the admin command). */
    public long lastRest = -1L;
    /** The rest in progress; null when idle. Never saved. */
    public Channel channel;

    public static final Codec<RestState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("used", 0).forGetter(s -> s.shortRestsUsed),
            Codec.LONG.optionalFieldOf("long", -1L).forGetter(s -> s.lastLongRest),
            Codec.LONG.optionalFieldOf("last", -1L).forGetter(s -> s.lastRest)
    ).apply(i, RestState::new));

    public RestState() {}

    public RestState(int used, long lastLongRest, long lastRest) {
        this.shortRestsUsed = Math.max(0, used);
        this.lastLongRest = lastLongRest;
        this.lastRest = lastRest;
    }

    /** Is there nothing worth saving (no short rest spent, never rested)? Such a state is not written. */
    public boolean isDefault() { return shortRestsUsed == 0 && lastLongRest < 0 && lastRest < 0; }

    /** A fresh state for the admin refill: every short rest back, the long-rest cool-down cleared, any rest in progress ended. */
    public void refill() {
        shortRestsUsed = 0;
        lastLongRest = -1L;
        channel = null;
    }

    /**
     * The rest in progress. {@code origin} is where the player sat down (the "moved too far" test measures from here: a click may come from up to
     * four blocks away, so the seat itself cannot be the reference), {@code anchor} is the clicked block, {@code ambushAt} is the tick offset from
     * {@code start} at which an ambush arrives (-1 for none) and {@code ambushDone} stops it firing twice.
     */
    public static final class Channel {
        public final RestKind kind;
        public final long start;
        public final int total;
        public final Vec3 origin;
        public final BlockPos anchor;
        public final int sectorId;
        public final long ambushAt;
        public boolean ambushDone;

        public Channel(RestKind kind, long start, int total, Vec3 origin, BlockPos anchor, int sectorId, long ambushAt) {
            this.kind = kind;
            this.start = start;
            this.total = total;
            this.origin = origin;
            this.anchor = anchor;
            this.sectorId = sectorId;
            this.ambushAt = ambushAt;
        }
    }
}
