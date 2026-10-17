package com.newuniverse.nusmp.arc.zagred;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Why: Zagred is supposed to ANSWER what the party does (Reverse against a burst of damage, Seal against a caster, a turn on whoever hurts him most), so
 * he has to keep a short memory of what was done to him: how much damage was thrown at him in the last five seconds, who struck him with magic lately,
 * and who has hurt him most. That memory is plain arithmetic over ticks and player ids, so it lives here without any game type, where its edge cases (a
 * long quiet spell, a hit at the very start, two players with the same threat) are unit tested, and the boss class only feeds it and asks.
 */
public final class ZagredAttention {
    /** Ticks in one bucket of the damage window, and how many buckets there are (five seconds in all). */
    public static final int BUCKET_TICKS = 20, BUCKETS = 5;
    /** A magic hit counts for this many ticks when he looks for casters; the list is forgotten after {@link #FORGET_TICKS}. */
    public static final int CASTER_TICKS = 160, FORGET_TICKS = 400;

    private final float[] loss = new float[BUCKETS];
    private long bucketStart = -1;
    private final Map<UUID, Long> magic = new HashMap<>();
    private final Map<UUID, Float> threat = new HashMap<>();

    /** Moves the damage window forward to {@code now}: whole buckets that have passed are shifted out, a gap longer than the window empties it. */
    private void roll(long now) {
        if (bucketStart < 0 || now < bucketStart || now - bucketStart >= (long) BUCKET_TICKS * BUCKETS) {
            Arrays.fill(loss, 0f);
            bucketStart = now;
            return;
        }
        while (now - bucketStart >= BUCKET_TICKS) {
            for (int i = loss.length - 1; i > 0; i--) loss[i] = loss[i - 1];
            loss[0] = 0f;
            bucketStart += BUCKET_TICKS;
        }
    }

    /** A blow of {@code amount} landed at {@code now}. */
    public void hit(long now, float amount) {
        if (amount <= 0f) return;
        roll(now);
        loss[0] += amount;
    }

    /** The share of {@code maxHealth} thrown at him in the last five seconds (can pass 1 for a huge burst; 0 when nothing was). */
    public double burst(long now, float maxHealth) {
        roll(now);
        double sum = 0;
        for (float f : loss) sum += f;
        return sum / Math.max(1f, maxHealth);
    }

    /** {@code who} struck him with magic (or anti-magic) at {@code now}. */
    public void magicHit(UUID who, long now) { if (who != null) magic.put(who, now); }

    /** Players who struck him with magic within the last {@link #CASTER_TICKS} ticks. */
    public int casters(long now) {
        int n = 0;
        for (long when : magic.values()) if (now - when < CASTER_TICKS) n++;
        return n;
    }

    /** {@code who} did {@code amount} of damage to him. */
    public void threatAdd(UUID who, float amount) { if (who != null && amount > 0f) threat.merge(who, amount, Float::sum); }

    /** The eligible player who has hurt him most (the first of equals), or null when nobody eligible has hurt him. */
    public UUID topThreat(Predicate<UUID> eligible) {
        UUID top = null;
        float best = 0f;
        for (Map.Entry<UUID, Float> e : threat.entrySet()) {
            if (e.getValue() > best && eligible.test(e.getKey())) {
                best = e.getValue();
                top = e.getKey();
            }
        }
        return top;
    }

    /** Old grudges fade: every threat is halved and the small ones are dropped. */
    public void decayThreat() {
        threat.replaceAll((k, v) -> v * 0.5f);
        threat.values().removeIf(v -> v < 1f);
    }

    /** Forgets magic hits that are too old to matter. */
    public void prune(long now) { magic.values().removeIf(w -> now - w > FORGET_TICKS); }

    /** Forgets everything (a new fight). */
    public void clear() {
        Arrays.fill(loss, 0f);
        bucketStart = -1;
        magic.clear();
        threat.clear();
    }
}
