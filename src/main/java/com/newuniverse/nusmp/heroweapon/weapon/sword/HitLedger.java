package com.newuniverse.nusmp.heroweapon.weapon.sword;

import java.util.function.Predicate;

/**
 * Why: the Legendary Sword is the fast weapon, and its speed is made of small rules about the blows of the wielder: a run of blows keeps the TEMPO up, a full
 * Tempo turns into a Flow strike, the step of an afterimage leaves a ghost blow behind, and the form passives (the opening strike of the Flash Sword, the stab in the
 * back of the Assassin Sword, the star every fifth blow of the Meteor Sword ...) each add a small bonus when their condition holds. If those rules lived in the
 * hook that handles a blow they could not be tested, and the one rule that must never break (at most ONE bonus strike per blow, never more than the bonus cap, a
 * passive only spends what it chose) would depend on a careful reading. This ledger holds all of it with no Minecraft types: it is told the blow and what the
 * game knows about it, and answers with the single bonus strike to deal, if any. Times are game ticks passed in by the caller; a clock that went back only resets
 * the counters.
 */
public final class HitLedger {
    private static final long NEVER = Long.MIN_VALUE / 4;

    /** The bonus strike chosen for a blow: where it came from, how much raw damage, and whether the Flow was spent. */
    public record Plan(String source, double raw, boolean flow) {}

    private long lastHit = NEVER, lastStar = NEVER, lastThunder = NEVER, ghostUntil;
    private int tempo, sinceStar, sinceHundred;

    /** The Tempo at {@code now}: 0 when the run of blows has lapsed. */
    public int tempo(long now) {
        if (tempo <= 0 || now < lastHit || now - lastHit > SwordNumbers.TEMPO_WINDOW) return 0;
        return tempo;
    }

    /** The level of Speed the Tempo earns (0 none, 1 Speed I, 2 Speed II); {@code early} lowers both thresholds by one blow (the Wind Walker). */
    public static int speedLevel(int tempo, boolean early) {
        int shift = early ? 1 : 0;
        if (tempo >= SwordNumbers.SPEED_TWO_AT - shift) return 2;
        if (tempo >= SwordNumbers.SPEED_ONE_AT - shift) return 1;
        return 0;
    }

    /** The step of an afterimage: the next blow within {@code ticks} leaves a ghost strike. */
    public void armGhost(long now, int ticks) { ghostUntil = now + Math.max(0, ticks); }

    public boolean ghostArmed(long now) { return now < ghostUntil && ghostUntil - now <= SwordNumbers.GHOST_TICKS * 2L; }

    /** A kill adds to the Tempo (the wielder is in the flow of the fight). */
    public void kill(long now) {
        int t = tempo(now);
        tempo = Math.min(SwordNumbers.TEMPO_MAX - 1, t + 2);                          // a kill alone never completes the Flow
        lastHit = now;
    }

    /**
     * A melee blow of the wielder landed at {@code now}. {@code has} tells which passives the active form has; {@code fullSwing} is whether the attack bar was full;
     * {@code behind} whether the wielder struck from the back of the foe; {@code targetHealth} the foe's health as a share of its maximum; {@code gilded} whether the
     * foe carries the mark of Gold Rebellion. Returns the one bonus strike to deal (null for none) and updates every counter. Only the strike that is chosen spends
     * what it needs (the ghost, the Flow, the star's count, the thunder's pause); a candidate that lost keeps its charge for the next blow.
     */
    public Plan hit(long now, boolean fullSwing, Predicate<String> has, boolean behind, double targetHealth, boolean gilded) {
        if (now < lastHit) {                                                          // the clock went back: start over
            lastHit = NEVER;
            lastStar = NEVER;
            lastThunder = NEVER;
            tempo = 0;
            sinceStar = 0;
            sinceHundred = 0;
            ghostUntil = 0;
        }
        long idle = now - lastHit;
        tempo = idle <= SwordNumbers.TEMPO_WINDOW && tempo > 0 ? Math.min(SwordNumbers.TEMPO_MAX, tempo + 1) : 1;
        lastHit = now;
        if (has.test("star_shower")) sinceStar++;
        if (has.test("hundred_edge")) sinceHundred++;

        String best = null;
        double raw = 0;
        boolean flow = false;
        // each candidate: (source, raw); the largest wins, the first listed wins a tie
        if (tempo >= SwordNumbers.TEMPO_MAX && fullSwing) {
            best = "flow";
            raw = SwordNumbers.FLOW_RAW;
            flow = true;
        }
        if (ghostArmed(now) && SwordNumbers.GHOST_RAW > raw) {
            best = "ghost";
            raw = SwordNumbers.GHOST_RAW;
            flow = false;
        }
        if (has.test("star_shower") && sinceStar >= SwordNumbers.STAR_EVERY && now - lastStar >= SwordNumbers.STAR_PAUSE && SwordNumbers.STAR_RAW > raw) {
            best = "star_shower";
            raw = SwordNumbers.STAR_RAW;
            flow = false;
        }
        if (has.test("quick_draw") && idle >= SwordNumbers.QUICK_DRAW_IDLE && SwordNumbers.QUICK_DRAW_RAW > raw) {
            best = "quick_draw";
            raw = SwordNumbers.QUICK_DRAW_RAW;
            flow = false;
        }
        if (has.test("shadow_blade") && behind && SwordNumbers.SHADOW_RAW > raw) {
            best = "shadow_blade";
            raw = SwordNumbers.SHADOW_RAW;
            flow = false;
        }
        if (has.test("hundred_edge") && sinceHundred >= SwordNumbers.HUNDRED_EDGE_EVERY && SwordNumbers.HUNDRED_EDGE_RAW > raw) {
            best = "hundred_edge";
            raw = SwordNumbers.HUNDRED_EDGE_RAW;
            flow = false;
        }
        if (has.test("fang_maw") && targetHealth > 0 && targetHealth < SwordNumbers.FANG_BELOW && SwordNumbers.FANG_RAW > raw) {
            best = "fang_maw";
            raw = SwordNumbers.FANG_RAW;
            flow = false;
        }
        if (has.test("thunder_edge") && now - lastThunder >= SwordNumbers.THUNDER_PAUSE && SwordNumbers.THUNDER_RAW > raw) {
            best = "thunder_edge";
            raw = SwordNumbers.THUNDER_RAW;
            flow = false;
        }
        if (gilded && SwordNumbers.GILDED_RAW > raw) {
            best = "gilded";
            raw = SwordNumbers.GILDED_RAW;
            flow = false;
        }
        if (best == null) return null;
        switch (best) {
            case "flow" -> tempo = 0;
            case "ghost" -> ghostUntil = 0;
            case "star_shower" -> {
                sinceStar = 0;
                lastStar = now;
            }
            case "hundred_edge" -> sinceHundred = 0;
            case "thunder_edge" -> lastThunder = now;
            default -> { }
        }
        return new Plan(best, Math.min(raw, SwordNumbers.BONUS_MAX), flow);
    }

    public void reset() {
        lastHit = NEVER;
        lastStar = NEVER;
        lastThunder = NEVER;
        ghostUntil = 0;
        tempo = 0;
        sinceStar = 0;
        sinceHundred = 0;
    }
}
