package com.newuniverse.nusmp.book;

/**
 * Why: the once-a-second upkeep of a Spirit Dive decides in one place, as pure arithmetic, whether the dive goes on and, if not, WHY (the reason is logged once per dive at INFO as
 * {@code nusmp spirit dive ended: <reason>}). A creative player pays no magicule upkeep (his pool is not used in creative, so it can be empty: before this the first upkeep second ended the dive
 * every time with "The spirit slips away.", while {@code ArmourRules.drop} already spares creative players).
 */
public final class SpiritDiveUpkeep {
    private SpiritDiveUpkeep() {}

    /** Share of the maximum magicule one second of the dive costs. */
    public static final double UPKEEP_SHARE = 0.02;

    public enum Verdict {
        HOLD, GATE_CLOSED, NO_MAGICULE;

        public String reason() {
            return switch (this) {
                case HOLD -> "holding";
                case GATE_CLOSED -> "gate closed (spirit level is not Lord for the element, or another mage owns the spirit)";
                case NO_MAGICULE -> "not enough magicule for the upkeep";
            };
        }
    }

    public static double cost(double maxMagicule) { return Math.max(0.0, maxMagicule) * UPKEEP_SHARE; }

    /** Whether the dive goes on this second. {@code have}: the magicules the diver has; {@code cost}: {@link #cost}. */
    public static Verdict verdict(boolean gateOpen, boolean creative, double have, double cost) {
        if (!gateOpen) return Verdict.GATE_CLOSED;
        if (creative || cost <= 0.0) return Verdict.HOLD;
        return have >= cost ? Verdict.HOLD : Verdict.NO_MAGICULE;
    }
}
