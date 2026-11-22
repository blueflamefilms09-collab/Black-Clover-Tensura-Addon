package com.newuniverse.nusmp.arc.magi;

import java.util.List;

/**
 * Why: the one rule that makes Sinbad Sinbad (only ONE djinn at a time, switched fast), as pure functions so it is tested without a game: how long an equip lasts, which moves may be
 * chosen while a djinn is on, and which djinn he reaches for in a situation. {@link SinbadBoss} only asks these.
 */
final class SinbadEquip {
    private SinbadEquip() {}

    /** Ticks one equip lasts: 20 s, 12 s once the Gates are open. */
    static int equipTicks(boolean gatesOpen) { return gatesOpen ? SinbadTable.EQUIP_TICKS_GATES : SinbadTable.EQUIP_TICKS; }

    /** 4.105.0: ticks one equip lasts in a phase (0 King of Sindria, 1 Djinn Master, 2 the Gates): 20 s, 16 s, 12 s. {@code phase} 0 gives the old numbers. */
    static int equipTicks(boolean gatesOpen, int phase) {
        if (gatesOpen) return SinbadTable.EQUIP_TICKS_GATES;
        return phase >= 1 ? SinbadTable.EQUIP_TICKS_PHASE2 : SinbadTable.EQUIP_TICKS;
    }

    /** 4.105.0: the phase of a power: 2 once the Gates are open, 1 from {@link SinbadTable#PHASE2_POWER}, else 0. */
    static int phaseOf(int power, boolean gatesOpen) {
        return gatesOpen ? 2 : power >= SinbadTable.PHASE2_POWER ? 1 : 0;
    }

    /** 4.105.0: the last ticks of an equip in which a direct switch may be chosen, by phase. */
    static int switchWindow(int phase) { return phase >= 1 ? SinbadTable.SWITCH_WINDOW_PHASE2 : SinbadTable.SWITCH_WINDOW; }

    /** 4.105.0: {@link #allowed(SinbadTable.Spec, SinbadDjinn, boolean, long)} with the phase's own switch window. */
    static boolean allowed(SinbadTable.Spec spec, SinbadDjinn equip, boolean gatesOpen, long ticksLeft, int phase) {
        if (spec != null && SinbadTable.DJINN_EQUIP.equals(spec.id())) return equip == SinbadDjinn.NONE || ticksLeft < switchWindow(phase);
        return allowed(spec, equip, gatesOpen, ticksLeft);
    }

    /**
     * May this move be chosen now? The plain sword moves always; the equip move when nothing is on or the equip is about to run out; the finale only once the Gates are open; any
     * other move only while its own djinn is the equipped one.
     */
    static boolean allowed(SinbadTable.Spec spec, SinbadDjinn equip, boolean gatesOpen, long ticksLeft) {
        if (spec == null) return true;     // a move of the framework's rework rows (not in the table) is a plain move, never held back
        if (SinbadTable.DJINN_EQUIP.equals(spec.id())) return equip == SinbadDjinn.NONE || ticksLeft < SinbadTable.SWITCH_WINDOW;
        if (SinbadTable.SEVEN_SEAS.equals(spec.id())) return gatesOpen;
        return spec.djinn() == SinbadDjinn.NONE || spec.djinn() == equip;
    }

    /**
     * The djinn he reaches for: the crowd's djinn when three or more foes stand near, a ranged djinn when the target is far, a close-range one otherwise, then Furfur and Baal; the first of
     * them that is unlocked at this power and is not the one already on. Never the current one, never {@link SinbadDjinn#NONE}; when nothing else is unlocked yet, Baal.
     */
    static SinbadDjinn pick(double distance, int foes, int power, SinbadDjinn current) {
        List<SinbadDjinn> want = new java.util.ArrayList<>(8);
        if (foes >= 3) { want.add(SinbadDjinn.VEPAR); want.add(SinbadDjinn.ZEPAR); }
        if (distance > 10.0) { want.add(SinbadDjinn.BAAL); want.add(SinbadDjinn.FOCALOR); }
        else { want.add(SinbadDjinn.CROCELL); want.add(SinbadDjinn.VALEFOR); }
        want.add(SinbadDjinn.FURFUR);
        want.add(SinbadDjinn.BAAL);
        for (SinbadDjinn d : want) if (d != current && power >= SinbadTable.unlockPower(d)) return d;
        for (SinbadDjinn d : SinbadDjinn.values()) if (d != SinbadDjinn.NONE && d != current && power >= SinbadTable.unlockPower(d)) return d;
        return SinbadDjinn.BAAL;
    }
}
