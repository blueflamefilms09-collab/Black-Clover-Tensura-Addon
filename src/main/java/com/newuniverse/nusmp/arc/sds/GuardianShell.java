package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.DamageKind;

/**
 * Why: Chastiefol's Guardian is a bear of moss, and the canon of its body is a rule a player can use: it turns blows aside (it is "heavily resistant to blunt force"),
 * its water keeps heat and fire off it, and the same water makes it "vulnerable to attacks that can freeze it solid" (the Nanatsu no Taizai wiki, see
 * docs/arc/king.md). For a fight that must "transcend standard stat checks" that is a gift: a party that only swings blades and shoots arrows is walled, a party with
 * spells is not, and a party with ice (or anti-magic, which unmakes a magic creature) ends the shell at once. This pure object keeps the shell's clock and judges a
 * blow by its {@link DamageKind}: physical blows pass at 35%, fire at 50%, ice and anti-magic break it (the blow itself lands in full), everything else is unchanged.
 */
public final class GuardianShell {
    /** What happened to one blow. */
    public enum Result { CLOSED, PARRIED, DAMPED, PASSED, THAWED, BROKEN }

    public record Verdict(Result result, float passed) {}

    public static final float PHYSICAL_PASS = 0.35f, FIRE_PASS = 0.5f;

    private long openUntil = Long.MIN_VALUE;

    /** The shell stands for {@code ticks} from {@code now}. */
    public void open(long now, int ticks) { openUntil = now + Math.max(1, ticks); }

    public boolean isOpen(long now) { return now < openUntil; }

    public void close() { openUntil = Long.MIN_VALUE; }

    public int ticksLeft(long now) { return isOpen(now) ? (int) (openUntil - now) : 0; }

    /** Judges a blow of {@code kind} and {@code amount}; {@code anti} says it was anti-magic. A broken shell is closed here. */
    public Verdict judge(long now, DamageKind kind, boolean anti, float amount) {
        if (!isOpen(now) || !(amount > 0f)) return new Verdict(Result.CLOSED, amount);
        if (anti) {
            close();
            return new Verdict(Result.BROKEN, amount);
        }
        if (kind == DamageKind.ICE) {
            close();
            return new Verdict(Result.THAWED, amount);
        }
        if (kind != null && kind.physical()) return new Verdict(Result.PARRIED, amount * PHYSICAL_PASS);
        if (kind == DamageKind.FIRE) return new Verdict(Result.DAMPED, amount * FIRE_PASS);
        return new Verdict(Result.PASSED, amount);
    }
}
