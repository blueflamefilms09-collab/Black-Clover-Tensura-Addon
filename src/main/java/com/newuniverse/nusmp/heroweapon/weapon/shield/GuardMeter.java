package com.newuniverse.nusmp.heroweapon.weapon.shield;

/**
 * Why: "absolute defence" cannot mean that a raised shield soaks any blow for ever, or the Shield is a free win against every boss; it has to be a defence a
 * player can read and manage. This is the small machine that makes it so, with no Minecraft types so it is tested alone: the guard has a RESOLVE (the damage it
 * can still soak, a multiple of the wielder's maximum health) that falls with every blow it stops and comes back after a quiet spell; a PARRY (the block in the
 * first ticks after the guard became active) costs none of it; when the Resolve is gone the guard BREAKS (the rest of the blow goes through and the shield cannot
 * be raised for a moment); and every point of damage it soaked also charges the COUNTER (0 to 100), the stored answer that the Shield's counter techniques spend.
 * All times are game ticks, passed in by the caller, so a world clock that jumps back only makes the meter forgiving, never stuck.
 */
public final class GuardMeter {
    private double resolve = -1;                  // -1: not started yet, it begins full
    private double counter;
    private long lastBlock = Long.MIN_VALUE / 4, lastRegen = Long.MIN_VALUE / 4, lastCounterGain = Long.MIN_VALUE / 4, lastParry = Long.MIN_VALUE / 4, brokenUntil;
    private boolean wasBroken;

    /** What a blow came to: the part that got through, the part the guard soaked, whether it broke the guard, whether it was a parry, the Counter charge it gave. */
    public record Result(double through, double absorbed, boolean broken, boolean parried, double counterGain) {}

    /** The Resolve a guard can hold: the wielder's maximum health times the form's multiple. */
    public static double capacity(double maxHealth, double multiple) { return Math.max(1.0, maxHealth * Math.max(0.0, multiple)); }

    /**
     * True when a block {@code ticksUsing} ticks into the use of the shield is a parry: the guard is active from {@link ShieldNumbers#BLOCK_DELAY} ticks, and the
     * {@code window} ticks after that count.
     */
    public static boolean isParry(int ticksUsing, int window) { return ticksUsing >= 0 && ticksUsing <= ShieldNumbers.BLOCK_DELAY + Math.max(0, window); }

    /**
     * A blow of {@code amount} meets the raised guard at game time {@code now}. {@code maxHealth} and {@code multiple} give the capacity, {@code timed} says whether
     * the block fell in the parry window (it is a parry only if the last parry was at least {@link ShieldNumbers#PARRY_PAUSE} ticks ago), {@code rate} scales the
     * Counter charge (1.0 normally).
     */
    public Result block(long now, double amount, double maxHealth, double multiple, boolean timed, double rate) {
        if (!(amount > 0)) return new Result(0, 0, false, false, 0);
        double cap = capacity(maxHealth, multiple);
        start(cap);
        sanitise(now);
        lastBlock = now;
        boolean parry = timed && now >= brokenUntil && now - lastParry >= ShieldNumbers.PARRY_PAUSE;
        if (parry) lastParry = now;
        if (now < brokenUntil) return new Result(amount, 0, false, false, 0);        // a broken guard soaks nothing
        double absorbed, through = 0;
        boolean broken = false;
        if (parry) {
            absorbed = amount;
        } else if (amount <= resolve) {
            absorbed = amount;
            resolve -= amount;
        } else {
            absorbed = resolve;
            through = amount - resolve;
            resolve = 0;
            broken = true;
            wasBroken = true;
            brokenUntil = now + ShieldNumbers.BREAK_TICKS;
        }
        double gain = CounterMath.gain(absorbed, maxHealth, rate, parry);
        addCounter(now, gain);
        return new Result(through, absorbed, broken, parry, gain);
    }

    /** Called every few ticks while the weapon is summoned: lets a broken guard heal, gives Resolve back after a quiet spell, lets an unused Counter fade. */
    public void tick(long now, double maxHealth, double multiple) {
        double cap = capacity(maxHealth, multiple);
        start(cap);
        sanitise(now);
        long dt = Math.max(0, Math.min(40, now - lastRegen));
        lastRegen = now;
        if (wasBroken && now >= brokenUntil) {
            wasBroken = false;
            resolve = Math.max(resolve, cap * ShieldNumbers.AFTER_BREAK);
        }
        if (now >= brokenUntil && now - lastBlock >= ShieldNumbers.REGEN_DELAY) resolve = Math.min(cap, resolve + cap * ShieldNumbers.REGEN_PER_TICK * dt);
        if (resolve > cap) resolve = cap;                                             // the capacity can fall (a form change, less health)
        if (counter > 0 && now - lastCounterGain >= ShieldNumbers.COUNTER_HOLD) counter = Math.max(0.0, counter - ShieldNumbers.COUNTER_DECAY_PER_TICK * dt);
    }

    /** A world clock that went back must not leave the meter waiting for a time that will not come for hours: anything stamped in the future is forgotten. */
    private void sanitise(long now) {
        if (lastBlock > now) lastBlock = Long.MIN_VALUE / 4;
        if (lastCounterGain > now) lastCounterGain = Long.MIN_VALUE / 4;
        if (lastRegen > now) lastRegen = now;
        if (lastParry > now) lastParry = Long.MIN_VALUE / 4;
        if (brokenUntil - now > ShieldNumbers.BREAK_TICKS) brokenUntil = now;
    }

    private void start(double cap) {
        if (resolve < 0) resolve = cap;
        else if (resolve > cap) resolve = cap;
    }

    /** The Resolve now, for a capacity (starts full). */
    public double resolve(double maxHealth, double multiple) {
        start(capacity(maxHealth, multiple));
        return resolve;
    }

    /** The Resolve as a share of the capacity, 0 to 1 (starts at 1). */
    public double share(double maxHealth, double multiple) {
        double cap = capacity(maxHealth, multiple);
        return Math.max(0.0, Math.min(1.0, resolve(maxHealth, multiple) / cap));
    }

    /** True while the guard is broken at {@code now}. */
    public boolean broken(long now) { return now < brokenUntil; }

    public double counter() { return counter; }

    /** Adds charge (0 to {@link ShieldNumbers#COUNTER_MAX}); returns the charge now. */
    public double addCounter(long now, double gain) {
        if (gain > 0) {
            counter = Math.min(ShieldNumbers.COUNTER_MAX, counter + gain);
            lastCounterGain = now;
        }
        return counter;
    }

    /** Spends up to {@code amount} of the charge and returns what was really spent. */
    public double spendCounter(double amount) {
        double spent = Math.max(0.0, Math.min(counter, amount));
        counter -= spent;
        return spent;
    }

    /** Back to the start: full Resolve, no Counter, not broken (a new form, a new body). */
    public void reset() {
        resolve = -1;
        counter = 0;
        brokenUntil = 0;
        wasBroken = false;
        lastBlock = Long.MIN_VALUE / 4;
        lastRegen = Long.MIN_VALUE / 4;
        lastCounterGain = Long.MIN_VALUE / 4;
        lastParry = Long.MIN_VALUE / 4;
    }
}
