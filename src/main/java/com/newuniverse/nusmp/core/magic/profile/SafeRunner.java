package com.newuniverse.nusmp.core.magic.profile;

import java.util.function.BooleanSupplier;

/**
 * The pure core of {@link SafeCast}: runs one magic's code, and if it throws records the fault in a {@link FaultLedger}, runs the
 * fallback (itself guarded) and reports "handled" instead of letting the exception reach the game's tick. Out-of-memory style VM errors
 * are not swallowed; everything else (exceptions, linkage errors from a missing class, assertion errors, stack overflows) is.
 */
public final class SafeRunner {
    private final FaultLedger ledger;

    public SafeRunner(FaultLedger ledger) { this.ledger = ledger; }

    public FaultLedger ledger() { return ledger; }

    static boolean fatal(Throwable t) { return t instanceof VirtualMachineError && !(t instanceof StackOverflowError); }

    /**
     * Runs a cast. Returns what the cast returned; if it threw, the fault is recorded, {@code fallback} runs and the result is true
     * (the cast is spent, so the player is not left with a half-paid failure).
     */
    public boolean run(String id, long now, String what, BooleanSupplier cast, Runnable fallback) {
        try {
            return cast.getAsBoolean();
        } catch (Throwable t) {
            if (fatal(t)) throw (VirtualMachineError) t;
            ledger.record(id, now, what, t);
            if (fallback != null) {
                try { fallback.run(); }
                catch (Throwable t2) {
                    if (fatal(t2)) throw (VirtualMachineError) t2;
                    ledger.record(id, now, what + " (fallback)", t2);
                }
            }
            return true;
        }
    }

    /** Runs code that has no result (a per-tick callback, a render pass). Returns false if it threw (the fault is recorded, once logged). */
    public boolean guard(String id, long now, String what, Runnable code) {
        try {
            code.run();
            return true;
        } catch (Throwable t) {
            if (fatal(t)) throw (VirtualMachineError) t;
            ledger.record(id, now, what, t);
            return false;
        }
    }
}
