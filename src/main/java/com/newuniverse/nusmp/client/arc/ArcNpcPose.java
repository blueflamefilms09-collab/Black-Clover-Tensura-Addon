package com.newuniverse.nusmp.client.arc;

import com.newuniverse.nusmp.client.zogratis.ZogratisPose;

/**
 * Why: a friendly character of the arcs plays three clips in everyday life (idle, walk, talk) and the usual ones when it is hurt or dies, and it must pick them by the same
 * rules as every other body of the mod, so the choice is made by {@link ZogratisPose} (the tested clock that paces a walk by the ground covered) and this class only decides
 * what an NPC tells it: it is "talking" when someone stands close and it is not walking, and a talking NPC plays the {@code cast} clip (the art kit makes that clip a calm
 * conversational gesture for an NPC). Pure: no game classes.
 */
public final class ArcNpcPose {
    private ArcNpcPose() {}

    /** A player this many blocks away or closer makes a standing NPC talk. */
    public static final double TALK_RANGE = 3.2;

    /** True when an NPC should gesture: a player within TALK_RANGE (squared distance given), the NPC standing still, and nothing else going on. */
    public static boolean talking(double nearestPlayerDistanceSqr, boolean standing, boolean dead) {
        return !dead && standing && nearestPlayerDistanceSqr >= 0.0 && nearestPlayerDistanceSqr <= TALK_RANGE * TALK_RANGE;
    }

    /** Fills the parts of a pose frame that depend on the NPC: talking is a cast, and the strides of its model. */
    public static void fill(ZogratisPose.Frame f, boolean talking, float walkCycle, float runCycle) {
        f.cast = talking ? 1 : 0;
        f.castProgress = talking ? 1f : 0f;
        f.walkCycle = walkCycle;
        f.runCycle = runCycle;
    }

    /** The clip an NPC is playing, as the clock decided it. A convenience for tests and logs. */
    public static String clipOf(ZogratisPose clock) {
        return clock.clip;
    }
}
