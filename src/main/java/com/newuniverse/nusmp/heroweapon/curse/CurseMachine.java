package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Why: the Curse Series is the one place a hero weapon asks the wielder for something, so its rules are a state machine that can be proved without a game: how
 * despair grows (each cause counted once per pause) and fades (only when the wielder is calm), when the weapon whispers an offer and when the offer lapses, what
 * accepting costs (a permanent tax of hearts, 4 for the first series and 2 for each further one, with a floor of maximum health that is never crossed), how
 * corruption rises while a curse form is worn and falls when it is not, when the weapon takes over (the Overrun at 1000) and what it leaves behind (corruption 400
 * and a minute of weakness), and what cleanses (the Purification Charm, a rest at a Safe Zone, a purge by an admin). Nothing here knows Minecraft: it works on a
 * {@link CurseState}, a clock in game ticks and a {@link CurseTuning}, and answers with small results the game glue turns into messages, sounds and effects.
 * Randomness is passed in (a roll in [0, 1)) so a test decides it. Nothing ever unlocks by itself: the only way into the series list is {@link #embrace} on an
 * open offer or the admin's {@link #grant}.
 */
public final class CurseMachine {
    private CurseMachine() {}

    /** What an Overrun did on this look: nothing, it just began, or it just ended. */
    public enum Step { NONE, OVERRUN_STARTED, OVERRUN_ENDED }

    /** Why an Embrace was refused. */
    public enum Refusal { NONE, NO_OFFER, LOCKED, ALREADY }

    /** The result of {@link #embrace}: refused with a reason, or the sin that was embraced and the hearts it cost. */
    public record Embraced(Refusal refusal, Sin sin, int hearts) {
        public boolean ok() { return refusal == Refusal.NONE; }

        static Embraced refused(Refusal why) { return new Embraced(why, null, 0); }
    }

    /** What a Purification Charm did: the corruption it took away and the hearts of tax it gave back. */
    public record Cleansed(int corruption, int hearts) {
        public boolean any() { return corruption > 0 || hearts > 0; }
    }

    // ------------------------------------------------------------------ despair
    /**
     * Adds despair for a cause. Returns the amount that was added: 0 when the amount is not positive, when the same cause was counted less than its pause ago, or
     * when the despair was already at the top (the report still counts for the pause). The wielder is then "not calm" for the calm timer.
     */
    public static int addDespair(CurseState s, Cause cause, int amount, long now, CurseTuning t) {
        if (amount <= 0) return 0;
        int pause = t.pause(cause);
        Long last = s.causeAt.get(cause.key());
        if (pause > 0 && last != null && now >= last && now - last < pause) return 0;
        s.causeAt.put(cause.key(), now);
        int added = Math.min(amount, CurseTuning.MAX - s.despair);
        s.despair += added;
        s.lastGain = now;
        return added;
    }

    /**
     * Once a second: despair falls by the configured amount while the wielder is calm (the caller says so: nothing has hurt them lately), nothing was added for
     * the calm time, and no offer is open (an open offer holds the despair until it is answered or runs out).
     */
    public static void decayDespair(CurseState s, long now, boolean calm, CurseTuning t) {
        if (s.despair <= 0 || !calm || !s.offered.isEmpty()) return;
        if (now >= s.lastGain && now - s.lastGain < t.calmTicks()) return;
        s.despair = Math.max(0, s.despair - Math.max(0, t.despairDecayPerSecond));
    }

    /** Forgets the pauses of causes that cannot matter any more (older than the longest pause), so the saved record stays small. */
    public static void pruneCauses(CurseState s, long now, CurseTuning t) {
        long keep = Math.max(1L, Math.max(t.pauseAlly, Math.max(t.pauseBetrayal, t.pauseNearDeath)));
        Iterator<Map.Entry<String, Long>> it = s.causeAt.entrySet().iterator();
        while (it.hasNext()) {
            long at = it.next().getValue();
            if (now >= at && now - at > keep) it.remove();
        }
    }

    // ------------------------------------------------------------------ the offer
    /** True while an offer is open (made and not yet run out). */
    public static boolean offerOpen(CurseState s, long now, CurseTuning t) {
        return !s.offered.isEmpty() && (now < s.offeredAt || now - s.offeredAt < t.offerTicks());
    }

    /** Ticks the open offer has left, 0 when none is open. */
    public static int offerTicksLeft(CurseState s, long now, CurseTuning t) {
        if (!offerOpen(s, now, t)) return 0;
        long left = s.offeredAt + t.offerTicks() - now;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, left));
    }

    /** If an offer was made and ran out unanswered, lets it lapse (see {@link #decline}) and says so. */
    public static boolean lapseIfExpired(CurseState s, long now, CurseTuning t) {
        if (s.offered.isEmpty() || offerOpen(s, now, t)) return false;
        lapse(s, now, t);
        return true;
    }

    /**
     * Opens an offer when the despair has reached the threshold, no offer is open, the pause since the last one is over and the wielder is not in an Overrun: the
     * first candidate sin that is not yet embraced. Returns that sin, or null when no offer opened (candidates are in the order the weapon prefers).
     */
    public static Sin tryOffer(CurseState s, List<Sin> candidates, long now, CurseTuning t) {
        if (!s.offered.isEmpty()) return null;
        if (s.despair < t.offerThreshold) return null;
        if (now < s.nextOfferAt) return null;
        if (inOverrun(s, now)) return null;
        for (Sin sin : candidates) {
            if (sin == null || s.series.contains(sin.id())) continue;
            s.offered = sin.id();
            s.offeredAt = now;
            return sin;
        }
        return null;
    }

    /** The wielder turns the whisper away: the open offer lapses (despair drops, the weapon waits before it speaks again). False when none was open. */
    public static boolean decline(CurseState s, long now, CurseTuning t) {
        if (!offerOpen(s, now, t)) return false;
        lapse(s, now, t);
        return true;
    }

    private static void lapse(CurseState s, long now, CurseTuning t) {
        s.offered = "";
        s.offeredAt = 0;
        s.despair = Math.min(s.despair, Math.max(0, t.declineDespair));
        s.nextOfferAt = now + t.reofferTicks();
    }

    // ------------------------------------------------------------------ the Embrace and the tax
    /**
     * The wielder accepts the open offer: the series is added for good, its tax is added to the hearts taken, the offer closes, the despair falls to the
     * after-Embrace level and the weapon waits before it offers again. Refused (nothing changes) during an Overrun, with no open offer, or for a series that is
     * already embraced.
     */
    public static Embraced embrace(CurseState s, long now, CurseTuning t) {
        if (inOverrun(s, now)) return Embraced.refused(Refusal.LOCKED);
        if (!offerOpen(s, now, t)) return Embraced.refused(Refusal.NO_OFFER);
        Sin sin = Sin.byId(s.offered);
        if (sin == null) {
            s.offered = "";
            s.offeredAt = 0;
            return Embraced.refused(Refusal.NO_OFFER);
        }
        if (s.series.contains(sin.id())) {
            s.offered = "";
            s.offeredAt = 0;
            return Embraced.refused(Refusal.ALREADY);
        }
        int hearts = taxFor(s.series.size(), t);
        s.series.add(sin.id());
        s.tax += hearts;
        s.offered = "";
        s.offeredAt = 0;
        s.despair = Math.min(s.despair, Math.max(0, t.embraceDespair));
        s.nextOfferAt = now + t.reofferTicks();
        return new Embraced(Refusal.NONE, sin, hearts);
    }

    /**
     * An administrator unlocks a series without an offer. {@code withTax} adds the tax of that stage like a real Embrace. Returns the hearts taken (0 or more), or
     * -1 when the series was already embraced. An open offer for the same sin is closed.
     */
    public static int grant(CurseState s, Sin sin, boolean withTax, CurseTuning t) {
        if (sin == null || s.series.contains(sin.id())) return -1;
        int hearts = withTax ? taxFor(s.series.size(), t) : 0;
        s.series.add(sin.id());
        s.tax += hearts;
        if (sin.id().equals(s.offered)) {
            s.offered = "";
            s.offeredAt = 0;
        }
        return hearts;
    }

    /** Opens an offer of {@code sin} at once (an administrator's tool, and the self-test's), whatever the despair. False when it is embraced already or an offer is open. */
    public static boolean forceOffer(CurseState s, Sin sin, long now) {
        if (sin == null || s.series.contains(sin.id()) || !s.offered.isEmpty()) return false;
        s.offered = sin.id();
        s.offeredAt = now;
        return true;
    }

    /** The hearts the next series costs when {@code seriesBefore} are embraced already: the first costs {@code taxFirstHearts}, every further one {@code taxNextHearts}. */
    public static int taxFor(int seriesBefore, CurseTuning t) { return Math.max(0, seriesBefore <= 0 ? t.taxFirstHearts : t.taxNextHearts); }

    /**
     * The health points the tax really takes off the maximum: all of it ({@code 2 * hearts}) unless that would leave less than {@code minHearts} hearts, in which
     * case only what leaves exactly that many (0 when the wielder is at or below the floor already). {@code maxHealthWithoutTax} is the maximum before the tax.
     */
    public static double appliedTaxHp(int taxHearts, double maxHealthWithoutTax, int minHearts) {
        double want = 2.0 * Math.max(0, taxHearts);
        double room = Math.max(0.0, maxHealthWithoutTax - 2.0 * Math.max(0, minHearts));
        return Math.min(want, room);
    }

    // ------------------------------------------------------------------ corruption and the Overrun
    /** True while the weapon holds its wielder (the Overrun has begun and its time has not come). */
    public static boolean inOverrun(CurseState s, long now) { return s.overrunUntil > 0 && now < s.overrunUntil; }

    /** The corruption one technique adds: the configured amount, times the ultimate factor for an ultimate, after the global scale. */
    public static int techniqueCorruption(boolean ultimate, CurseTuning t) { return t.scaled(t.corruptionPerTechnique * (ultimate ? Math.max(1, t.ultimateFactor) : 1)); }

    /**
     * Once a second. While a curse form is worn corruption rises by the form's rate (never less than the configured minimum, after the global scale); otherwise it
     * falls by the decay. During an Overrun nothing moves. Reaching 1000 starts an Overrun ({@code roll} picks its length).
     */
    public static Step tickCorruption(CurseState s, boolean curseActive, int formPerSecond, long now, CurseTuning t, double roll) {
        if (inOverrun(s, now)) return Step.NONE;
        if (curseActive) return raise(s, t.scaled(Math.max(formPerSecond, t.minCorruptionPerSecond)), now, t, roll);
        s.corruption = Math.max(0, s.corruption - Math.max(0, t.corruptionDecayPerSecond));
        return Step.NONE;
    }

    /** Corruption from outside the once-a-second rule (a technique, a skill's price, an admin); a negative amount lowers it. No change during an Overrun. */
    public static Step addCorruption(CurseState s, int amount, long now, CurseTuning t, double roll) {
        if (inOverrun(s, now) || amount == 0) return Step.NONE;
        if (amount < 0) {
            s.corruption = Math.max(0, s.corruption + amount);
            return Step.NONE;
        }
        return raise(s, amount, now, t, roll);
    }

    private static Step raise(CurseState s, int amount, long now, CurseTuning t, double roll) {
        if (amount <= 0) return Step.NONE;
        s.corruption = Math.min(CurseTuning.MAX, s.corruption + amount);
        if (s.corruption < CurseTuning.MAX) return Step.NONE;
        startOverrun(s, now, t, roll);
        return Step.OVERRUN_STARTED;
    }

    /** Starts an Overrun at once (corruption at the top, an open offer withdrawn). Used by the rise above and by the admin's command. */
    public static void startOverrun(CurseState s, long now, CurseTuning t, double roll) {
        s.corruption = CurseTuning.MAX;
        s.overrunUntil = now + OverrunMath.ticks(t.overrunMinSeconds, t.overrunMaxSeconds, roll);
        s.offered = "";
        s.offeredAt = 0;
        s.nextOfferAt = Math.max(s.nextOfferAt, s.overrunUntil + t.reofferTicks());
    }

    /** True when an Overrun has run its time but has not been ended yet (the caller then runs {@link #endOverrun}). */
    public static boolean overrunDue(CurseState s, long now) { return s.overrunUntil > 0 && now >= s.overrunUntil; }

    /** Ends an Overrun that is due: corruption falls to the configured level (never raised) and the weakness starts. */
    public static Step endOverrunIfDue(CurseState s, long now, CurseTuning t) {
        if (!overrunDue(s, now)) return Step.NONE;
        s.overrunUntil = 0;
        s.corruption = Math.min(s.corruption, Math.max(0, t.overrunEndCorruption));
        s.weakUntil = now + t.weakTicks();
        return Step.OVERRUN_ENDED;
    }

    /** The wielder died: an Overrun ends without the weakness (there is nobody left to weaken) and the corruption falls to the configured level. */
    public static void abortOverrun(CurseState s, CurseTuning t) {
        if (s.overrunUntil <= 0) return;
        s.overrunUntil = 0;
        s.corruption = Math.min(s.corruption, Math.max(0, t.overrunEndCorruption));
    }

    /** A death ends the Overrun (see {@link #abortOverrun}) and the weakness too: the new body starts rested, with whatever corruption and despair were left. */
    public static void onWielderDeath(CurseState s, CurseTuning t) {
        abortOverrun(s, t);
        s.weakUntil = 0;
    }

    /** True while the weakness after an Overrun lasts. */
    public static boolean weak(CurseState s, long now) { return s.weakUntil > 0 && now < s.weakUntil; }

    // ------------------------------------------------------------------ cleansing
    /** The charm and the rest cannot reach a wielder the weapon holds. */
    public static boolean canCleanse(CurseState s, long now) { return !inOverrun(s, now); }

    /**
     * A Purification Charm: all corruption is cleared, and if the wielder still owes tax and the charm has not given hearts back more than {@code charmRefundUses}
     * times yet, {@code charmRefundHearts} (at most the tax owed) are given back and the use is counted. Returns what changed; nothing happens during an Overrun.
     */
    public static Cleansed charm(CurseState s, long now, CurseTuning t) {
        if (!canCleanse(s, now)) return new Cleansed(0, 0);
        int cleared = s.corruption;
        s.corruption = 0;
        int hearts = 0;
        if (s.tax > 0 && s.refunds < t.charmRefundUses && t.charmRefundHearts > 0) {
            hearts = Math.min(s.tax, t.charmRefundHearts);
            s.tax -= hearts;
            s.refunds++;
        }
        return new Cleansed(cleared, hearts);
    }

    /**
     * A rest finished at game time {@code restTime} (the Safe Zone's own record): the first look at a newer rest clears the configured share of the corruption
     * if the rest ended within the last five seconds (an old rest seen after a login is remembered but not honoured). Returns the corruption cleared.
     */
    public static int restClear(CurseState s, long restTime, long now, CurseTuning t) {
        if (restTime <= s.lastRest) return 0;
        s.lastRest = restTime;
        if (now >= restTime && now - restTime > 100) return 0;
        if (s.corruption <= 0 || !canCleanse(s, now)) return 0;
        int cleared = (int) Math.round(s.corruption * Math.max(0.0, Math.min(1.0, t.restClearShare)));
        s.corruption -= cleared;
        return cleared;
    }

    /** An administrator wipes the whole record (despair, corruption, series, offer, Overrun, weakness, tax, the charm's count and the pauses). */
    public static void clearAll(CurseState s) {
        s.despair = 0;
        s.corruption = 0;
        s.series.clear();
        s.offered = "";
        s.offeredAt = 0;
        s.nextOfferAt = 0;
        s.overrunUntil = 0;
        s.weakUntil = 0;
        s.tax = 0;
        s.refunds = 0;
        s.lastGain = 0;
        s.causeAt.clear();
    }
}
