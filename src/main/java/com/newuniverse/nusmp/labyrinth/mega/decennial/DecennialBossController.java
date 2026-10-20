package com.newuniverse.nusmp.labyrinth.mega.decennial;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the phase machine of a Decennial boss, with no Minecraft type in it. A boss fight is a story told in time: the boss starts in its first form, at a share of its health it shifts into the
 * next (and for two seconds it cannot be hurt while the hall answers: that is the cinematic), the hall acts on its own clock (the lair actions), reinforcements arrive in waves and from broken
 * tombstones, and all of it must obey budgets (how many adds may be alive, how many a whole fight may spawn) or a boss becomes a lag machine. Putting that story in a pure object driven by
 * {@link #tick} (the game time, the boss's health share, whether a party is in the hall, how many adds are alive) gives three things at once: a JUnit test can run a whole fight in a loop and
 * assert the order of its events; the entity side ({@code DecennialLayer}) shrinks to "do what the events say" so a mistake in the schedule can never be a mistake in the world code; and
 * the schedule is deterministic, so two servers (or a reload) tell the same fight. The controller does not hurt anybody: it emits {@link Event}s and answers {@link #invulnerable} and the
 * speed and damage factors of the phase; every harmful number is applied by the layer through the damage budget and the player's hit cap.
 */
public final class DecennialBossController {
    public enum Kind {
        /** A shift begins: the boss is invulnerable for {@link #CINEMATIC_TICKS}; {@code phase} is the phase it is shifting INTO. */
        PHASE_START,
        /** The shift ends: {@code phase} is now the current phase. */
        PHASE_END,
        /** A lair action fires: {@code id} is the lair id, {@code power} its damage factor. */
        LAIR,
        /** The layer should spawn {@code count} adds ({@code id} says why: "phase" or "wave"); already trimmed to the budget. */
        ADDS
    }

    public record Event(Kind kind, int phase, String id, int count, double power) {}

    /** The invulnerable cinematic of a phase shift: two seconds. */
    public static final int CINEMATIC_TICKS = 40;
    /** The first lair action comes this long after the fight starts (6 s), a second lair action of the same boss this much later still, and no two lair actions come closer than the gap (3 s). */
    public static final int FIRST_LAIR_DELAY = 120, LAIR_STAGGER = 80, LAIR_GAP = 60;
    /** Each phase makes the lair actions come this much faster (8 %), never faster than {@link DecennialRules#MIN_LAIR_SECONDS}. */
    public static final double LAIR_SPEEDUP = 0.08;

    private final DecennialProfile profile;
    private int phase = 1, target, spent;
    private long cinematicEnd = -1, fightStart = -1, nextAnyLair, nextWave;
    private boolean fighting;
    private final long[] nextLair;

    public DecennialBossController(DecennialProfile profile) {
        this.profile = profile;
        this.nextLair = new long[profile.lairs().size()];
    }

    // ================================================================ queries
    public DecennialProfile profile() { return profile; }

    /** The current phase, 1-based. */
    public int phase() { return phase; }

    public DecennialProfile.Phase current() { return profile.phase(phase); }

    public boolean fighting() { return fighting; }

    public boolean inCinematic(long now) { return cinematicEnd >= 0 && now < cinematicEnd; }

    /** True while the boss cannot be hurt (the cinematic of a shift). */
    public boolean invulnerable(long now) { return inCinematic(now); }

    /** The phase it is shifting into, or 0 when it is not shifting. */
    public int shiftingTo() { return cinematicEnd >= 0 ? target : 0; }

    /** How far the cinematic has got, 0 to 1 (0 when none runs). */
    public double shiftProgress(long now) {
        if (cinematicEnd < 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (cinematicEnd - now) / (double) CINEMATIC_TICKS));
    }

    public double speedMult() { return current().speed(); }

    public double damageMult() { return current().damage(); }

    public int addsSpent() { return spent; }

    public long fightStart() { return fightStart; }

    public boolean isFinalPhase() { return phase >= profile.phaseCount(); }

    // ================================================================ the add budget
    /**
     * How many of {@code wanted} adds the budget lets in now (a tombstone broken, a phase, a wave): at most the room left under the alive cap and under the cap of the whole fight, and at least
     * zero. What it grants is counted as spent.
     */
    public int grantAdds(int wanted, int aliveNow) {
        DecennialProfile.AddsSpec a = profile.adds();
        int room = Math.min(a.maxAlive() - Math.max(0, aliveNow), a.fightCap() - spent);
        int granted = Math.max(0, Math.min(Math.min(wanted, DecennialRules.MAX_ADDS_PER_WAVE), room));
        spent += granted;
        return granted;
    }

    // ================================================================ the tick
    /**
     * Advances the fight to game time {@code now}. {@code hp} is the boss's health as a share of its maximum (0 to 1); {@code fighting} says that a party is in the hall; {@code addsAlive} is how many of
     * the boss's adds live. Returns the events due, in the order the layer must carry them out (never more than one shift per call).
     */
    public List<Event> tick(long now, double hp, boolean fighting, int addsAlive) {
        List<Event> out = new ArrayList<>(3);
        if (!fighting) {
            this.fighting = false;
            return out;
        }
        if (!this.fighting) begin(now);
        // a shift in progress: nothing else happens until it ends
        if (cinematicEnd >= 0) {
            if (now >= cinematicEnd) {
                cinematicEnd = -1;
                phase = target;
                out.add(new Event(Kind.PHASE_END, phase, "", 0, current().damage()));
                DecennialProfile.Phase ph = current();
                if (ph.addCount() > 0) {
                    int granted = grantAdds(ph.addCount(), addsAlive);
                    if (granted > 0) out.add(new Event(Kind.ADDS, phase, "phase", granted, 1.0));
                }
                for (int i = 0; i < nextLair.length; i++) nextLair[i] = Math.max(nextLair[i], now + FIRST_LAIR_DELAY / 2 + i * LAIR_STAGGER);   // the hall draws breath after a shift
                nextAnyLair = Math.max(nextAnyLair, now + LAIR_GAP);
            }
            return out;
        }
        // the next threshold
        if (phase < profile.phaseCount() && hp <= profile.phase(phase + 1).at()) {
            target = phase + 1;
            cinematicEnd = now + CINEMATIC_TICKS;
            out.add(new Event(Kind.PHASE_START, target, "", 0, 1.0));
            return out;
        }
        // lair actions
        if (now >= nextAnyLair) {
            for (int i = 0; i < nextLair.length; i++) {
                DecennialProfile.LairRef l = profile.lairs().get(i);
                if (l.fromPhase() > phase || now < nextLair[i]) continue;
                out.add(new Event(Kind.LAIR, phase, l.id(), 0, l.power() * Math.min(DecennialRules.MAX_DAMAGE_MULT, current().damage())));
                nextLair[i] = now + intervalTicks(l.every(), phase);
                nextAnyLair = now + LAIR_GAP;
                break;                                                  // one lair action per call: the gap rule
            }
        }
        // reinforcements
        DecennialProfile.AddsSpec a = profile.adds();
        if (a.wave() > 0 && a.waveEvery() > 0 && now >= nextWave) {
            nextWave = now + a.waveEvery() * 20L;
            if (addsAlive * 2 <= a.maxAlive()) {
                int granted = grantAdds(a.wave(), addsAlive);
                if (granted > 0) out.add(new Event(Kind.ADDS, phase, "wave", granted, 1.0));
            }
        }
        return out;
    }

    private void begin(long now) {
        fighting = true;
        if (fightStart < 0) fightStart = now;
        for (int i = 0; i < nextLair.length; i++) nextLair[i] = now + FIRST_LAIR_DELAY + i * LAIR_STAGGER;
        nextAnyLair = now + FIRST_LAIR_DELAY / 2;
        nextWave = now + profile.adds().waveEvery() * 20L;
    }

    /** The ticks between two firings of a lair action that comes every {@code seconds} while the boss is in phase {@code phase}. */
    public static int intervalTicks(int seconds, int phase) {
        double s = seconds * (1.0 - LAIR_SPEEDUP * Math.max(0, phase - 1));
        return (int) Math.round(Math.max(DecennialRules.MIN_LAIR_SECONDS, s) * 20.0);
    }

    // ================================================================ reset and save
    /** The party wiped, or the fight was abandoned: back to the first phase with a clean budget and schedule. */
    public void reset() {
        phase = 1;
        target = 0;
        spent = 0;
        cinematicEnd = -1;
        fightStart = -1;
        fighting = false;
        nextAnyLair = 0;
        nextWave = 0;
        java.util.Arrays.fill(nextLair, 0L);
    }

    /** The three numbers a reload of the world must not forget: {@code {phase, shifting-into, adds spent}}. */
    public int[] save() { return new int[] {phase, cinematicEnd >= 0 ? target : 0, spent}; }

    /** Restores {@link #save}. A shift that was running starts again (a full cinematic) on the first tick after the load. */
    public void restore(int[] saved) {
        if (saved == null || saved.length < 3) return;
        phase = Math.max(1, Math.min(profile.phaseCount(), saved[0]));
        spent = Math.max(0, saved[2]);
        target = saved[1] > phase && saved[1] <= profile.phaseCount() ? saved[1] : 0;
        cinematicEnd = -1;
        fighting = false;
    }
}
