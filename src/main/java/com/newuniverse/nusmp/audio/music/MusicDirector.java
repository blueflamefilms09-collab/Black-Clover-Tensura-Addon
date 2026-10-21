package com.newuniverse.nusmp.audio.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why: the cross-fade between tracks is the part of a music engine that is easy to get wrong (a fight that flaps the music every second, two loops fading forever, a track that
 * cuts off), and it needs no Minecraft class, so it is a pure state machine that the client engine drives once per tick and the tests drive without a game.
 * <p>
 * Each tick it is told the track the player SHOULD hear ({@code wanted}, null for silence) and that track's priority. It keeps a list of voices: the one that is current (fading
 * in or full) and any that are fading out. Rules, in the order they matter:
 * <ul>
 *   <li>Nothing playing: the wanted track starts at once. Nothing wanted: the current voice fades out (leaving the dungeon, the switch turned off).</li>
 *   <li>The wanted track changes to a HIGHER priority (a fight, a boss): it must have been asked for for {@code escalate} ticks (half a second, so one stray target does not
 *       trigger it), ignores the cooldown, and fades in at the faster speed.</li>
 *   <li>The wanted track changes to the same or a LOWER priority (the fight is over, another family of theme): it must have been asked for, without a break, for {@code settle}
 *       ticks AND the last switch must be {@code cooldown} ticks old. So a fight keeps playing a few seconds after the last blow, and a floor border does not restart the music.</li>
 *   <li>A switch fades the old voice out and the new one in over the same time; the gain curve is equal power (sine), so the sum does not dip in the middle.</li>
 *   <li>At most three voices exist; a fourth request drops the quietest one that is fading out.</li>
 * </ul>
 * Everything is in ticks and in numbers; {@code now} is any steadily increasing tick counter.
 */
public final class MusicDirector {
    /** Timing in ticks. */
    public record Params(int fadeIn, int fadeOut, int fastFadeIn, int cooldown, int settle, int escalate) {
        public Params {
            fadeIn = Math.max(1, fadeIn);
            fadeOut = Math.max(1, fadeOut);
            fastFadeIn = Math.max(1, fastFadeIn);
            cooldown = Math.max(0, cooldown);
            settle = Math.max(0, settle);
            escalate = Math.max(0, escalate);
        }

        /** From the two numbers the player sets: how long a fade lasts and how long a switch is blocked afterwards, both in seconds. */
        public static Params ofSeconds(double fadeSeconds, double cooldownSeconds) {
            int in = (int) Math.round(Math.max(0.5, Math.min(20.0, fadeSeconds)) * 20.0);
            int out = Math.max(1, (int) Math.round(in * 0.75));
            int fast = Math.max(10, in / 2);
            int cool = (int) Math.round(Math.max(0.0, Math.min(120.0, cooldownSeconds)) * 20.0);
            return new Params(in, out, fast, cool, 100, 10);
        }

        public static Params defaults() { return ofSeconds(4.0, 12.0); }
    }

    /** One playing track. Only the director changes it; readers use {@link #level()}. */
    public static final class Voice {
        private final long id;
        private final String track;
        private final float stepIn;
        private float progress;
        private boolean fadingOut;

        Voice(long id, String track, float stepIn) {
            this.id = id;
            this.track = track;
            this.stepIn = stepIn;
        }

        public long id() { return id; }

        public String track() { return track; }

        public boolean fadingOut() { return fadingOut; }

        /** 0 to 1 along the fade; equal-power: the loudness of the voice is {@code sin(progress * pi / 2)}. */
        public float progress() { return progress; }

        /** The gain to play the voice at, 0 to 1. */
        public float level() { return (float) Math.sin(Math.max(0f, Math.min(1f, progress)) * Math.PI / 2.0); }
    }

    private static final int MAX_VOICES = 3;

    private Params params;
    private final List<Voice> voices = new ArrayList<>(4);
    private final List<Voice> view = Collections.unmodifiableList(voices);
    private long nextId = 1;
    private Voice current;
    private int currentPriority;
    private long lastSwitch = Long.MIN_VALUE / 2;
    private String pending;
    private long pendingSince;

    public MusicDirector(Params params) { this.params = params == null ? Params.defaults() : params; }

    public Params params() { return params; }

    /** Changes the timing; voices already fading keep the speed they started with. */
    public void setParams(Params p) { if (p != null) params = p; }

    /** The voices, the current one and the ones fading out. Do not keep the list between ticks. */
    public List<Voice> voices() { return view; }

    public Voice voice(long id) {
        for (int i = 0; i < voices.size(); i++) if (voices.get(i).id == id) return voices.get(i);
        return null;
    }

    /** The track that is current (fading in or full), or null. */
    public String currentTrack() { return current == null ? null : current.track; }

    /** Is anything playing or fading? */
    public boolean isActive() { return !voices.isEmpty(); }

    /** The tick of the last switch (a huge negative number before the first). */
    public long lastSwitch() { return lastSwitch; }

    /** Drops every voice at once (logout, dimension change with the engine stopped). */
    public void reset() {
        voices.clear();
        current = null;
        pending = null;
        currentPriority = 0;
        lastSwitch = Long.MIN_VALUE / 2;
    }

    /**
     * One tick.
     *
     * @param now      a steadily increasing tick counter
     * @param wanted   the track that should play, null for none
     * @param priority the priority of the wanted track ({@link MusicLayer#priority()})
     * @return true when a new voice was started this tick (the engine can announce the title)
     */
    public boolean tick(long now, String wanted, int priority) {
        boolean started = false;
        if (wanted == null) {
            pending = null;
            if (current != null) fadeOutCurrent();
        } else if (wanted.equals(currentTrack())) {
            pending = null;
            currentPriority = priority;
        } else if (current == null) {
            start(now, wanted, priority, false);
            started = true;
        } else {
            if (!wanted.equals(pending)) {
                pending = wanted;
                pendingSince = now;
            }
            boolean up = priority > currentPriority;
            long stable = now - pendingSince;
            boolean ok = up ? stable >= params.escalate() : (stable >= params.settle() && now - lastSwitch >= params.cooldown());
            if (ok) {
                start(now, wanted, priority, up);
                started = true;
            }
        }
        advance();
        return started;
    }

    private void start(long now, String track, int priority, boolean fast) {
        if (current != null) fadeOutCurrent();
        while (voices.size() >= MAX_VOICES) dropQuietest();
        Voice v = new Voice(nextId++, track, 1.0f / (fast ? params.fastFadeIn() : params.fadeIn()));
        voices.add(v);
        current = v;
        currentPriority = priority;
        lastSwitch = now;
        pending = null;
    }

    private void fadeOutCurrent() {
        current.fadingOut = true;
        current = null;
        currentPriority = 0;
    }

    private void dropQuietest() {
        int worst = -1;
        for (int i = 0; i < voices.size(); i++) {
            Voice v = voices.get(i);
            if (v == current) continue;
            if (worst < 0 || v.progress < voices.get(worst).progress) worst = i;
        }
        voices.remove(worst < 0 ? 0 : worst);
    }

    private void advance() {
        float out = 1.0f / params.fadeOut();
        for (int i = voices.size() - 1; i >= 0; i--) {
            Voice v = voices.get(i);
            if (v.fadingOut) {
                v.progress -= out;
                if (v.progress <= 0f) voices.remove(i);
            } else if (v.progress < 1f) {
                v.progress = Math.min(1f, v.progress + v.stepIn);
            }
        }
    }
}
