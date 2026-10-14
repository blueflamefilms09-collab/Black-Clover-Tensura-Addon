package com.newuniverse.nusmp.client.relics.item;

import java.util.ArrayList;
import java.util.List;

/**
 * Which 3D mesh a bow or crossbow shows: a pure function of the draw, with vanilla's own timing and thresholds (no Minecraft types, so it is tested).
 * <ul>
 *   <li>bow: {@code pull = ticksUsed / 20} (vanilla's {@code minecraft:pull}); the models switch at 0.65 and 0.9;</li>
 *   <li>crossbow: {@code pull = ticksUsed / chargeDuration} ({@code CrossbowItem.getChargeDuration}, so Quick Charge counts); the models switch at
 *       0.58 and 1.0, and a loaded crossbow ({@code CrossbowItem.isCharged}) shows the loaded mesh whoever holds it;</li>
 *   <li>stages: REST (&lt;id&gt;), PULL1 (drawing, any pull), PULL2, PULL3, CHARGED (&lt;id&gt;_charged), and SPENT (&lt;id&gt;_spent) for the mod's two
 *       self-loading crossbows while they recharge.</li>
 * </ul>
 * A mesh a weapon does not ship is skipped: the chain for PULL3 is _pull3, _pull2, _pull1, then the rest mesh.
 */
public final class RangedStage {
    private RangedStage() {}

    public enum Kind { BOW, CROSSBOW }

    public static final int REST = 0, PULL1 = 1, PULL2 = 2, PULL3 = 3, CHARGED = 4, SPENT = 5;

    /** Vanilla bow model thresholds on the {@code pull} property, and the bow's full draw in ticks (BowItem.MAX_DRAW_DURATION). */
    public static final float BOW_PULL2 = 0.65f, BOW_PULL3 = 0.9f;
    public static final int BOW_DRAW_TICKS = 20;
    /** Vanilla crossbow model thresholds. */
    public static final float CROSSBOW_PULL2 = 0.58f, CROSSBOW_PULL3 = 1.0f;

    /** {@code minecraft:pull} of a bow: ticks used over the full draw, 0 to 1. */
    public static float bowPull(int useDuration, int remaining) {
        return clamp01((useDuration - remaining) / (float) BOW_DRAW_TICKS);
    }

    /** {@code minecraft:pull} of a crossbow that is not loaded: ticks used over its charge duration, 0 to 1 (0 when loaded or the duration is zero). */
    public static float crossbowPull(boolean charged, int useDuration, int remaining, int chargeDuration) {
        if (charged || chargeDuration <= 0) return 0f;
        return clamp01((useDuration - remaining) / (float) chargeDuration);
    }

    /**
     * The stage. {@code using}: the holder is drawing THIS stack. {@code charged}: a crossbow is loaded (ignored for a bow). A loaded crossbow is
     * CHARGED even when nobody holds it; otherwise a weapon that is not being drawn is at REST.
     */
    public static int stage(Kind kind, boolean using, boolean charged, float pull) {
        if (kind == Kind.CROSSBOW && charged) return CHARGED;
        if (!using) return REST;
        float two = kind == Kind.BOW ? BOW_PULL2 : CROSSBOW_PULL2;
        float three = kind == Kind.BOW ? BOW_PULL3 : CROSSBOW_PULL3;
        if (pull >= three) return PULL3;
        if (pull >= two) return PULL2;
        return PULL1;
    }

    public static String suffix(int stage) {
        return switch (stage) {
            case PULL1 -> "_pull1";
            case PULL2 -> "_pull2";
            case PULL3 -> "_pull3";
            case CHARGED -> "_charged";
            case SPENT -> "_spent";
            default -> "";
        };
    }

    /** The mesh ids to try for a stage, best first: the stage's own mesh, the lower draw stages, then the rest mesh {@code id} itself. */
    public static List<String> chain(String id, int stage) {
        List<String> out = new ArrayList<>(4);
        if (stage >= PULL1 && stage <= PULL3) {
            for (int s = stage; s >= PULL1; s--) out.add(id + suffix(s));
        } else if (stage == CHARGED || stage == SPENT) {
            out.add(id + suffix(stage));
        }
        out.add(id);
        return out;
    }

    private static float clamp01(float f) { return f < 0f ? 0f : Math.min(f, 1f); }
}
