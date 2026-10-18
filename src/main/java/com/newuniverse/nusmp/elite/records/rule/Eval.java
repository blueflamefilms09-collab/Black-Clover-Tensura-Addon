package com.newuniverse.nusmp.elite.records.rule;

/**
 * Why: an evaluation answers three different things the screen and the unlock loop treat differently: MET (unlock it), NOT_MET with a progress {@code current / target} (draw the bar), and
 * UNAVAILABLE (the source does not exist here: never unlock, show "unavailable" instead of a misleading 0). Pure, immutable.
 */
public record Eval(State state, double current, double target) {
    public enum State { MET, NOT_MET, UNAVAILABLE }

    public static final Eval UNAVAILABLE = new Eval(State.UNAVAILABLE, 0, 1);

    public static Eval atLeast(double current, double target) {
        double t = target <= 0 ? 1 : target;
        return new Eval(current >= t ? State.MET : State.NOT_MET, Math.max(0, current), t);
    }

    public boolean met() { return state == State.MET; }

    public boolean unavailable() { return state == State.UNAVAILABLE; }

    /** 0..1 for the progress bar (1 when met, 0 when unavailable). */
    public double fraction() {
        if (state == State.MET) return 1.0;
        if (state == State.UNAVAILABLE || target <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, current / target));
    }
}
