package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.BurstGauge;
import com.newuniverse.nusmp.arc.sds.kit.DamageKind;

/**
 * Why: Heavy Metal turns Diane's body into metal: heavier, stronger, harder to hurt, and "melted by high heat" (the Nanatsu no Taizai wiki, see docs/arc/diane.md). A
 * flat percentage cut would just be a stat check; a FLAT reduction per blow is a rule with a counter: each blow loses a fixed amount (so a swarm of small hits, which is
 * how a damage-over-time or a fast weapon works, is nearly walled) but never more than 80% of itself (so nothing is ever harmless), and the big blows of a charged attack
 * or a heavy spell pass almost whole. Fire is not cut at all and, if enough of it lands in a few seconds, melts the metal for good. This pure object keeps the skin's
 * clock, its heat gauge and the verdict on a blow.
 */
public final class MetalSkin {
    /** What happened to one blow. */
    public enum Result { CLOSED, REDUCED, HEATED, MELTED }

    public record Verdict(Result result, float passed) {}

    /** The least share of a blow that always gets through. */
    public static final float MIN_PASS = 0.2f;
    /** The window the heat is counted over, in ticks. */
    public static final int HEAT_SPAN = 60;

    private long openUntil = Long.MIN_VALUE;
    private final BurstGauge heat = new BurstGauge(HEAT_SPAN, 0);

    public void open(long now, int ticks) {
        openUntil = now + Math.max(1, ticks);
        heat.reset();
    }

    public boolean isOpen(long now) { return now < openUntil; }

    public void close() {
        openUntil = Long.MIN_VALUE;
        heat.reset();
    }

    public int ticksLeft(long now) { return isOpen(now) ? (int) (openUntil - now) : 0; }

    /**
     * Judges a blow. {@code flat} is what each blow loses (a share of her health set by the glue), {@code meltAt} the fire that melts the metal inside {@link #HEAT_SPAN}
     * ticks. A melted skin is closed here.
     */
    public Verdict judge(long now, DamageKind kind, float amount, float flat, float meltAt) {
        if (!isOpen(now) || !(amount > 0f)) return new Verdict(Result.CLOSED, amount);
        if (kind == DamageKind.FIRE) {
            if (heat.hit(now, amount, meltAt, false)) {
                close();
                return new Verdict(Result.MELTED, amount);
            }
            return new Verdict(Result.HEATED, amount);
        }
        return new Verdict(Result.REDUCED, Math.max(amount - Math.max(0f, flat), amount * MIN_PASS));
    }
}
