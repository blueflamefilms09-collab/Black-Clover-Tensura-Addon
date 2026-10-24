package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.companion.CompanionRules;

/**
 * The heal-ready ring of the Pouch tab. A companion that walks with you uses a healing potion or the most nourishing food from her pouch when your health falls under two fifths (at most every 30
 * seconds, and only while the switch is on). The client cannot see the 30 second timer, so the ring tells what it CAN know: READY (the switch is on and the pouch holds something she would use: the
 * ring breathes green and the slot she would pick is circled), EMPTY (the switch is on but nothing in the pouch helps), OFF (the switch is off) and LOCKED (the pouch is not open to you, so the server
 * offers no switch). Pure, unit tested; the item kinds come from the same {@link CompanionRules#bestHeal} the server uses.
 */
public final class PouchRing {
    private PouchRing() {}

    public enum State { LOCKED, OFF, EMPTY, READY }

    /**
     * The state for the view's switch ({@code -1} not offered, 0 off, 1 on) and the kinds of the nine slots ({@link CompanionRules#NOT_USEFUL} / FOOD_KIND / POTION_KIND with a value each).
     */
    public static State of(int useIt, int[] kinds, int[] values) {
        if (useIt < 0) return State.LOCKED;
        if (useIt == 0) return State.OFF;
        return bestSlot(kinds, values) >= 0 ? State.READY : State.EMPTY;
    }

    /** The slot she would use (the server's own rule), or -1. */
    public static int bestSlot(int[] kinds, int[] values) {
        if (kinds == null || values == null) return -1;
        return CompanionRules.bestHeal(kinds, values);
    }

    /** How bright the ring is at 'time' ticks: READY breathes, the others sit still and dim. */
    public static float brightness(State s, float time) {
        return switch (s) {
            case READY -> 0.65f + 0.35f * (float) Math.sin(time * 0.12f);
            case EMPTY -> 0.45f;
            case OFF -> 0.25f;
            case LOCKED -> 0.12f;
        };
    }

    /** The progress 0..1 of the sweep that runs round a READY ring (one lap every 80 ticks). */
    public static float sweep(float time) {
        float t = (time % 80f) / 80f;
        return t < 0f ? t + 1f : t;
    }
}
