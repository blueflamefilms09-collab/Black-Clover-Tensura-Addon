package com.newuniverse.nusmp.visual.demonslayer;

import com.newuniverse.nusmp.visual.genesis.VuPose;
import com.newuniverse.nusmp.visual.genesis.VuSpec;

/**
 * The stack-driven poses of the upgraded Genesis Demon-Slayer (assets/nusmp/visual_upgrade/demon_slayer_sword.json, tools/visual_upgrade/demon_slayer): three animation kinds that
 * {@link VuPose#eval} does not know (it skips an unknown kind) and that depend on the stack, not on the clock alone. Pure maths, no Minecraft classes, tested without the game.
 * <ul>
 *   <li>{@code fracture}: the shards of the blade during Black Meteorite. The group drifts along {@code axis} by {@code amp} times the fracture (0..1), turns by {@code deg} degrees about z
 *       and {@code rate} degrees about x, and shivers sideways (the old {@code DemonSlayerMesh#shardPose}, with the same 2.3 rad/tick shiver).</li>
 *   <li>{@code fracture_glow}: the crimson light between the shards: the group's alpha follows the fracture and flickers like the old crack light (0.8 + 0.2 sin).</li>
 *   <li>{@code resonance}: the pommel's clover and its bloom. The scale is 1 + {@code amp} x resonance (0..4); the alpha runs from {@code min} (no resonance) to {@code max} (4) and pulses
 *       between crimson and black, faster with the resonance (the old {@code DemonSlayerMesh#bloom}: 0.25 + 0.12 x resonance radians a tick).</li>
 * </ul>
 * The renderer ({@code client/relics/item/vu/DemonSlayerVu}) calls {@link #begin} with the stack's values before it draws the sword and {@link #end} after; {@code VuRenderer} asks
 * {@link #apply(VuSpec.Group, VuPose, float)} for every posed group (a no-op while no Demon-Slayer is being drawn). Render thread only; nothing is allocated.
 */
public final class DsDynamics {
    private DsDynamics() {}

    public static final String FRACTURE = "fracture", FRACTURE_GLOW = "fracture_glow", RESONANCE = "resonance";
    public static final int MAX_RESONANCE = 4;

    private static boolean active;
    private static float fracture;
    private static int resonance;

    public static void begin(float fractureNow, int resonanceNow) {
        fracture = Math.max(0f, Math.min(1f, fractureNow));
        resonance = clampResonance(resonanceNow);
        active = true;
    }

    public static void end() {
        active = false;
        fracture = 0f;
        resonance = 0;
    }

    public static boolean active() { return active; }

    public static int clampResonance(int r) { return Math.max(0, Math.min(MAX_RESONANCE, r)); }

    /** The hook {@code VuRenderer#groupMatrix} calls for every group it poses: applies the stack-driven kinds when a Demon-Slayer is being drawn. */
    public static void apply(VuSpec.Group g, VuPose pose, float ticks) {
        if (active) apply(g, pose, ticks, fracture, resonance);
    }

    /** True when the animation kind is one of the three stack-driven ones. */
    public static boolean isDynamic(String kind) { return FRACTURE.equals(kind) || FRACTURE_GLOW.equals(kind) || RESONANCE.equals(kind); }

    /** Applies the stack-driven animations of a group to its pose (after {@link VuPose#eval} has applied the clock-driven ones). */
    public static void apply(VuSpec.Group g, VuPose pose, float ticks, float fractureNow, int resonanceNow) {
        for (int i = 0, n = g.anim.size(); i < n; i++) {
            VuSpec.Anim a = g.anim.get(i);
            switch (a.kind) {
                case FRACTURE -> {
                    if (fractureNow <= 0f) break;
                    float shiver = shiver(ticks, a.phase, fractureNow);
                    pose.ox += a.ax * a.amp * fractureNow + shiver;
                    pose.oy += a.ay * a.amp * fractureNow;
                    pose.oz += a.az * a.amp * fractureNow;
                    if (a.deg != 0f) pose.rotate(0f, 0f, 1f, a.deg * fractureNow);
                    if (a.rate != 0f) pose.rotate(1f, 0f, 0f, a.rate * fractureNow);
                }
                case FRACTURE_GLOW -> pose.alpha *= glowAlpha(ticks, a.phase, fractureNow);
                case RESONANCE -> {
                    int r = clampResonance(resonanceNow);
                    pose.scale *= 1f + a.amp * r;
                    pose.alpha *= bloomAlpha(ticks, r, a.min, a.max);
                }
                default -> { }
            }
        }
    }

    /** The sideways shiver of a drifting shard (units of the model): up to 0.18 at full fracture. */
    public static float shiver(float ticks, float phase, float fracture) { return (float) Math.sin(ticks * 2.3f + phase * 10f) * 0.18f * fracture; }

    /** The alpha of the light between the shards: 0 at rest, up to 1, flickering by 20 percent. */
    public static float glowAlpha(float ticks, float phase, float fracture) {
        if (fracture <= 0f) return 0f;
        return Math.min(1f, fracture * (0.8f + 0.2f * (float) Math.sin(ticks * 1.9f + phase * 10f)));
    }

    /** The alpha of the bloom behind the clover: {@code min} .. {@code max} by the resonance (0..4), pulsing between crimson and black, with a floor of a faint glow at 0. */
    public static float bloomAlpha(float ticks, int resonance, float min, float max) {
        int r = clampResonance(resonance);
        float pulse = 0.5f + 0.5f * (float) Math.sin(ticks * (0.25f + 0.12f * r));
        float level = min + (max - min) * r / MAX_RESONANCE;
        return level * (r == 0 ? 0.6f + 0.4f * pulse : 0.2f + 0.8f * pulse);
    }
}
