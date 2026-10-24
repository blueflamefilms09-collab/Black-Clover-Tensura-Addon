package com.newuniverse.nusmp.client.companion.gs;

/**
 * The expression of one portrait over time: a mood set by something that just happened lasts for its hold time and then the face goes back to its resting look (which depends on the rapport tier).
 * One small object per open screen; the clock is the screen's own tick counter (a float of ticks), so it never depends on the world. Pure, unit tested.
 */
public final class MoodTimer {
    private Mood mood;
    private float since = -1000f;

    /** Starts a mood now. */
    public void set(Mood m, float now) {
        if (m == null) return;
        mood = m;
        since = now;
    }

    /** The mood to draw at 'now' for a rapport tier. */
    public Mood at(float now, int tier) {
        if (mood != null && now >= since && now - since < CompanionCues.holdTicks(mood)) return mood;
        return Mood.resting(tier);
    }

    /** Ticks since the last mood was set (a big number when none was). */
    public float age(float now) { return Math.max(0f, now - since); }

    /** True while a mood set by an event is showing. */
    public boolean active(float now) { return mood != null && now >= since && now - since < CompanionCues.holdTicks(mood); }
}
