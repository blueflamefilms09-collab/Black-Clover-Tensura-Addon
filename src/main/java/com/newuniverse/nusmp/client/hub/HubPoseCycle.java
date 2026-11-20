package com.newuniverse.nusmp.client.hub;

/**
 * Why: the player panel of the hub (Status page, Codex overview) shows the player's own model going through a cycle of poses: idle, on guard, a weapon swing (wind-up, strike) with whatever is in the main hand, a
 * cast, a gathering aura, a victory. The timeline of that cycle is plain arithmetic with no Minecraft types so {@code HubPoseCycleTest} can check it: the steps, how long each takes to ease in and how long it is held, and
 * which step is current at a given time. The swing steps are skipped when the hand is empty (an unarmed swing is not worth showing). {@link #at} loops over eight steps and allocates nothing, so the panel can ask
 * every frame. The pose names are those of {@code UiPoses.ALL} (the poses the Codex figure already had, and the three appended for the hub: swing_back, swing_through, aura_charge); the panel turns a step change
 * into one {@code UiPoses.set(index, blendTicks)}.
 */
public final class HubPoseCycle {
    private HubPoseCycle() {}

    /** One step: the pose to ease to, the ticks that easing takes, the ticks it is then held, and whether it only makes sense with a weapon in hand. */
    public record Step(String pose, float blend, float hold, boolean weapon) {
        public float length() { return blend + hold; }
    }

    public static final Step[] STEPS = {
            new Step("standing", 14f, 90f, false),
            new Step("ready", 12f, 30f, false),
            new Step("swing_back", 9f, 4f, true),
            new Step("swing_through", 4f, 22f, true),
            new Step("ready", 12f, 24f, true),
            new Step("casting", 14f, 70f, false),
            new Step("aura_charge", 16f, 80f, false),
            new Step("victory", 14f, 40f, false)};

    /** The ticks one round of the cycle takes (the swing steps only when {@code armed}). */
    public static float length(boolean armed) {
        float total = 0f;
        for (Step s : STEPS) if (armed || !s.weapon()) total += s.length();
        return total;
    }

    /** The index into {@link #STEPS} that is current {@code ticks} into the cycle (the cycle repeats; negative times wrap too). */
    public static int at(float ticks, boolean armed) {
        float total = length(armed);
        float t = ticks - (float) Math.floor(ticks / total) * total;
        float acc = 0f;
        int last = 0;
        for (int i = 0; i < STEPS.length; i++) {
            Step s = STEPS[i];
            if (!armed && s.weapon()) continue;
            last = i;
            acc += s.length();
            if (t < acc) return i;
        }
        return last;
    }

    /** The yaw, in degrees, the figure turns to at time {@code ticks}: a slow sway round {@code base}, 24 degrees to each side. */
    public static float sway(float ticks, float base) { return base + 24f * (float) Math.sin(ticks * 0.021f); }

    /** The extra yaw, in degrees, from the mouse at {@code mouseDx} units from the middle of a panel {@code halfW} wide: at most 28 degrees to each side, 0 when {@code halfW} is not positive. */
    public static float follow(float mouseDx, float halfW) {
        if (halfW <= 0f) return 0f;
        float k = mouseDx / halfW;
        return 28f * (k < -1f ? -1f : Math.min(k, 1f));
    }
}
