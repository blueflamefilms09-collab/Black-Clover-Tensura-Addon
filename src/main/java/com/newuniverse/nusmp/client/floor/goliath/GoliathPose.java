package com.newuniverse.nusmp.client.floor.goliath;

import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathTable;

/**
 * Which animation clip the Goliath plays, and at what second of it (pure, unit tested). The common part (idle, walk paced by the ground covered, the plain
 * blow, the flinch, the Gates' transformation, the death) is {@link ZogratisPose}; this adds what is the Goliath's own: the rise (the clip's time is the rise
 * itself, 0 to 1, so the fists break the floor first and the body follows), and a clip for each of its moves, paced to the wind-up so that the blow lands at
 * 80% of the clip and the rest is the settling after it. A move without a clip in the file falls back to the generic cast.
 */
public final class GoliathPose {
    public static final String RISE = "rise", SLAM = "slam", STOMP = "stomp", ROAR = "roar", FIST = "fist", LINE = "line", RUBBLE = "rubble", QUAKE = "cast_big";
    private static final float REWIND = 2f, RELEASED_FROM = 0.8f;

    /** The clip of a move (by its id in {@link GoliathTable}), or null for a move without one of its own. */
    public static String clipOf(String moveId) {
        if (moveId == null) return null;
        return switch (moveId) {
            case GoliathTable.GROUND_SLAM -> SLAM;
            case GoliathTable.TWIN_STOMP -> STOMP;
            case GoliathTable.ROARING_SHOCKWAVE -> ROAR;
            case GoliathTable.BEDROCK_FIST -> FIST;
            case GoliathTable.SEISMIC_LINE -> LINE;
            case GoliathTable.RUBBLE_FALL -> RUBBLE;
            case GoliathTable.BEDROCK_QUAKE -> QUAKE;
            default -> null;
        };
    }

    private final ZogratisPose base = new ZogratisPose();
    public String clip = ZogratisPose.IDLE;
    public float seconds;

    private float lastAge = Float.NaN, lastProgress, releaseAge = -1e9f;
    private boolean wasOwn;
    private String lastClip, settleClip;

    /** Reads one frame. `moveClip` is the clip of the move being wound up (or null), `rise` 0 to 1 (1 when it stands). */
    public void update(ZogratisPose.Frame f, String moveClip, float rise, ZogratisPose.Clips clips) {
        if (!Float.isNaN(lastAge) && f.age < lastAge - REWIND) {
            wasOwn = false;
            lastClip = null;
            settleClip = null;
            releaseAge = -1e9f;
        }
        boolean own = f.cast > 0 && moveClip != null && clips.length(moveClip) > 0f;
        int cast = f.cast;
        if (own) f.cast = 0;                                              // the move's own clip, not the generic cast
        base.update(f, clips);
        f.cast = cast;
        clip = base.clip;
        seconds = base.seconds;
        if (own) {
            lastClip = moveClip;
            lastProgress = f.castProgress;
        } else if (wasOwn && lastProgress >= RELEASED_FROM) {
            releaseAge = f.age;
            settleClip = lastClip;
        }
        wasOwn = own;
        lastAge = f.age;
        if (f.dead) return;
        if (rise < 1f && clips.length(RISE) > 0f) {
            clip = RISE;
            seconds = Math.max(0f, rise) * clips.length(RISE);
            return;
        }
        if (f.transforming) return;
        if (own) {
            clip = moveClip;
            seconds = Math.max(0f, Math.min(1f, f.castProgress)) * ZogratisPose.CAST_BIG_RELEASE * clips.length(moveClip);
            return;
        }
        if (settleClip != null) {
            float len = clips.length(settleClip);
            float s = ZogratisPose.CAST_BIG_RELEASE * len + (f.age - releaseAge) / 20f;
            if (len > 0f && f.age >= releaseAge && s < len) {
                clip = settleClip;
                seconds = s;
            } else {
                settleClip = null;
            }
        }
    }

    /** The smoothed horizontal speed in blocks per tick. */
    public float speed() { return base.speed(); }

    public float smoothScale(float target, float age) { return base.smoothScale(target, age); }

    public float smoothYaw(float target, float age) { return base.smoothYaw(target, age); }
}
