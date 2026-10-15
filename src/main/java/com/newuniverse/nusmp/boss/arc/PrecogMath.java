package com.newuniverse.nusmp.boss.arc;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Why: the old precognition was a few charges that ran out; the owner wants a passive that phases Lucius out of <i>every</i> ordinary
 * blow and can only be beaten by something the players have to arrange: striking together, striking very fast, an Anti-Magic weapon
 * (anti-magic is outside the time he reads), or a synchronized set of high-tier relics. That is a rule about a short history of
 * hits, so it is a pure tracker (no Minecraft types, unit tested per rule); the entity glue turns a damage source into the three facts
 * the tracker needs (who hit, is it anti-magic, is the attacker's relic set synchronized).
 * <ul>
 *   <li><b>Multi-attacker</b>: two different attackers within {@value #MULTI_WINDOW} ticks of each other. The second blow was not in
 *       the future he read, and neither is the first one's next.</li>
 *   <li><b>Rapid</b>: one attacker lands {@value #RAPID_HITS} blows within {@value #RAPID_WINDOW} ticks; from the fourth on, every blow
 *       lands while the pace holds. Every blow counts, phased or not.</li>
 *   <li><b>Anti-magic</b> and <b>synchronized relics</b> always land.</li>
 *   <li>A blow with <b>no attacker</b> (poison, fire, a field the party left) has no future to read: it lands, as before.</li>
 *   <li>While precognition is <b>off</b> (he channels Time Reversal, is staggered, the gem is out, Julius holds the body) everything lands.</li>
 * </ul>
 */
public final class PrecogMath {
    private PrecogMath() {}

    /** Ticks inside which a second attacker's blow is out of sequence. */
    public static final int MULTI_WINDOW = 6;
    /** Blows by one attacker within the window that count as very fast. */
    public static final int RAPID_HITS = 4, RAPID_WINDOW = 20;
    /** Hits older than this are forgotten; the history never holds more than {@link #KEEP}. */
    public static final int FORGET_AFTER = 40, KEEP = 64;
    /** Worn relics of at least UNIQUE grade that make a wielder "synchronized". */
    public static final int SYNC_RELICS = 2;

    public enum Verdict {
        /** Precognition is off: the blow lands. */
        OFF(true),
        /** The blow was read: it is phased out (no damage). */
        PHASED(false),
        NO_ATTACKER(true),
        ANTI_MAGIC(true),
        SYNCED_RELICS(true),
        MULTI_ATTACKER(true),
        RAPID(true);

        private final boolean lands;

        Verdict(boolean lands) { this.lands = lands; }

        /** Does the blow hurt him? */
        public boolean lands() { return lands; }
    }

    /** At least {@link #SYNC_RELICS} worn relics of grade UNIQUE or better ('gradeOrdinal' of UNIQUE is 3). */
    public static boolean synchronizedRelics(int wornAtLeastUnique) { return wornAtLeastUnique >= SYNC_RELICS; }

    private record Hit(long tick, int attacker) {}

    /** The short history of blows on one Lucius. */
    public static final class Tracker {
        private final ArrayDeque<Hit> hits = new ArrayDeque<>();
        private int phased;

        public void reset() {
            hits.clear();
            phased = 0;
        }

        /** How many blows have been phased out so far in this fight. */
        public int phasedCount() { return phased; }

        /**
         * A blow arrives.
         * @param attacker   the attacker's entity id, or a negative number if there is none
         * @param active     is precognition on (not channelling, not staggered, the gem not out, Julius asleep)
         */
        public Verdict onHit(long now, int attacker, boolean antiMagic, boolean syncedRelics, boolean active) {
            if (!active) return Verdict.OFF;
            if (attacker < 0) return Verdict.NO_ATTACKER;
            prune(now);
            hits.addLast(new Hit(now, attacker));
            while (hits.size() > KEEP) hits.pollFirst();
            if (antiMagic) return Verdict.ANTI_MAGIC;
            if (syncedRelics) return Verdict.SYNCED_RELICS;
            Set<Integer> recent = new HashSet<>();
            int mine = 0;
            for (Hit h : hits) {
                if (now - h.tick <= MULTI_WINDOW) recent.add(h.attacker);
                if (h.attacker == attacker && now - h.tick <= RAPID_WINDOW) mine++;
            }
            if (recent.size() >= 2) return Verdict.MULTI_ATTACKER;
            if (mine >= RAPID_HITS) return Verdict.RAPID;
            phased++;
            return Verdict.PHASED;
        }

        private void prune(long now) {
            Iterator<Hit> it = hits.iterator();
            while (it.hasNext()) if (now - it.next().tick > FORGET_AFTER) it.remove();
        }
    }
}
