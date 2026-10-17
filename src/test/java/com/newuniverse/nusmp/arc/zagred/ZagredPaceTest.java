package com.newuniverse.nusmp.arc.zagred;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner asked for a fight where the party can READ what he says, and that is a claim about RHYTHM (one title at a time, a quiet between Words, a closed heart
 * that opens in step with his speech) which no single-function test can prove. This plays the real pieces together (the framework's scheduler, the real table, the real AI
 * weights, the pace of speech, the heart's windows) through a few minutes of fight and measures the pace, so the numbers in the docs are measured, not hoped for.
 */
class ZagredPaceTest {
    static final List<ZagredTable.Spec> SPECS = ZagredTable.specs();

    /** What a played fight did. */
    static final class Fight {
        final Map<String, Integer> used = new HashMap<>();
        final List<Long> wordReleases = new ArrayList<>();
        long busy, heartOpen, ticks;
        int words;

        double wordsPerMinute() { return words * 1200.0 / ticks; }

        double busyShare() { return busy / (double) ticks; }

        double heartShare() { return heartOpen / (double) ticks; }

        long closestWords() {
            long best = Long.MAX_VALUE;
            for (int i = 1; i < wordReleases.size(); i++) best = Math.min(best, wordReleases.get(i) - wordReleases.get(i - 1));
            return best;
        }
    }

    /**
     * One fight of {@code ticks} ticks with a target who stays {@code distance} blocks away, {@code foes} foes of which {@code close} stand near him, at {@code power}.
     * Mana is unbound (as it is from 60%); the AI sees no volleys, no casters, no burst and no fliers, so this is the fight's baseline rhythm, not its answers.
     */
    static Fight play(long seed, int power, boolean anchored, double distance, int foes, int close, int ticks) {
        Random rng = new Random(seed);
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(power);
        int stage = PowerCurve.stage(power);
        ZagredHeart heart = new ZagredHeart();
        Fight f = new Fight();
        f.ticks = ticks;
        long busyUntil = 0, nextThink = 20, nextWordAt = 60, heartOpensAt = -1;
        int think = Math.max(2, (int) Math.round(5 * tu.cooldownMultiplier()));
        String last = "";
        for (long t = 0; t < ticks; t++) {
            if (t == heartOpensAt) heart.open(t, ZagredHeart.windowAfterWord(anchored));      // the heart opens when the Word is FINISHED, not when it begins
            if (heart.isOpen(t)) f.heartOpen++;
            if (t < busyUntil) { f.busy++; continue; }
            if (t < nextThink) continue;
            nextThink = t + think;
            MoveScheduler.Situation s = new MoveScheduler.Situation(distance, 0.6f, power, foes, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            ZagredAi.Read read = new ZagredAi.Read(distance, foes, close, 0, 0, 0, 0.0, true, stage, power >= 100, last, 0, ZagredMath.lesserCap(stage, 3), t < nextWordAt, false, false);
            double[] w = new double[SPECS.size()];
            for (int i = 0; i < w.length; i++) w[i] = sch.eligible(SPECS.get(i), s) ? ZagredAi.weight(SPECS.get(i), read) : 0;
            int idx = ZagredAi.choose(w, rng.nextDouble());
            if (idx < 0) continue;
            ZagredTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, (int) Math.max(10, Math.round(m.cooldownTicks() * tu.cooldownMultiplier())));
            int wind = m.ultimate() ? Math.max(30, Math.round(m.windUpTicks() * (float) tu.windUpMultiplier())) : Math.max(8, Math.round(m.windUpTicks() * (float) tu.windUpMultiplier()));
            long release = t + wind;
            busyUntil = release + m.recovery();
            last = m.id();
            f.used.merge(m.id(), 1, Integer::sum);
            if (m.isWord()) {
                f.words++;
                f.wordReleases.add(release);
                nextWordAt = release + ZagredMath.wordGap(stage);
                if (power >= 100) heartOpensAt = release;
            }
        }
        return f;
    }

    @Test
    void aWordNeverComesCloserToTheLastThanTheQuietBetweenWords() {
        for (int power : new int[]{20, 40, 60, 80, 100}) {
            for (long seed = 1; seed <= 4; seed++) {
                Fight f = play(seed, power, true, 5, 3, 2, 36000);
                assertTrue(f.words >= 8, "he speaks at power " + power + ": " + f.words);
                assertTrue(f.closestWords() >= ZagredMath.wordGap(PowerCurve.stage(power)), "power " + power + " seed " + seed + ": two Words only " + f.closestWords() + " ticks apart");
            }
        }
    }

    @Test
    void thePaceIsReadableInEveryAct() {
        for (int power : new int[]{20, 40, 60, 80, 100}) {
            double sum = 0;
            int n = 0;
            for (long seed = 1; seed <= 6; seed++) {
                for (double distance : new double[]{4, 12}) {
                    Fight f = play(seed, power, true, distance, 3, 2, 36000);
                    sum += f.wordsPerMinute();
                    n++;
                    assertTrue(f.wordsPerMinute() >= 4.0, "a Word at least every fifteen seconds at power " + power + ", distance " + distance + ": " + f.wordsPerMinute());
                    assertTrue(f.wordsPerMinute() <= 13.0, "never more than one Word every 4.6 seconds at power " + power + ", distance " + distance + ": " + f.wordsPerMinute());
                }
            }
            System.out.printf("pace: power %3d  %.1f Words a minute on average%n", power, sum / n);
        }
    }

    @Test
    void theHeartIsClosedMoreThanOpenWhileHisAnchorsStandAndOpensLongerOnceTheyFall() {
        double anchored = 0, free = 0;
        int n = 0;
        for (long seed = 1; seed <= 8; seed++) {
            anchored += play(seed, 100, true, 6, 3, 2, 36000).heartShare();
            free += play(seed, 100, false, 6, 3, 2, 36000).heartShare();
            n++;
        }
        anchored /= n;
        free /= n;
        System.out.printf("heart: open %.0f%% of the time with anchors, %.0f%% without%n", anchored * 100, free * 100);
        assertTrue(anchored >= 0.25 && anchored <= 0.50, "with anchors the heart is closed more than it is open: " + anchored);
        assertTrue(free > anchored, "killing the anchors lengthens the windows: " + free + " vs " + anchored);
        assertTrue(free <= 0.85, "even then it closes between Words: " + free);
    }

    @Test
    void theManAtRangeStillSpeaksAndTheManUpCloseStillFights() {
        Fight far = play(5, 20, true, 12, 2, 0, 36000);
        Fight near = play(5, 20, true, 3, 2, 1, 36000);
        assertNull(far.used.get(ZagredTable.CLAW), "he does not swing at the air twelve blocks away");
        assertTrue(far.words >= 6, "but he speaks: " + far.words);
        assertTrue(near.used.getOrDefault(ZagredTable.CLAW, 0) >= 10, "up close the claw is his answer between Words: " + near.used);
        assertTrue(near.busyShare() > far.busyShare(), "he is busier up close: " + near.busyShare() + " vs " + far.busyShare());
    }

    @Test
    void theUltimateIsRareAndAllTheOtherMovesStillGetUsedWhenTheirMomentComes() {
        Fight f = play(3, 100, false, 6, 5, 5, 72000);
        int ults = f.used.getOrDefault(ZagredTable.OVERWRITE, 0);
        assertTrue(ults >= 2 && ults <= 1 + 72000 / PowerCurve.tuning(100).ultimateGapTicks(), "Overwrite a few times in an hour of play, never faster than its gap: " + ults);
        for (String id : new String[]{ZagredTable.CLAW, ZagredTable.HALT, ZagredTable.FALL, ZagredTable.TRIDENT, ZagredTable.REDACT, ZagredTable.SWORDS, ZagredTable.BANISH, ZagredTable.VERDICT})
            assertTrue(f.used.getOrDefault(id, 0) > 0, id + " is used against a crowd at the Gates: " + f.used);
    }
}
