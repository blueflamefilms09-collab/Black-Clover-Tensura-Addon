package com.newuniverse.nusmp.arc.zagred;

/**
 * Why: Zagred's first four acts are a fight against a SENTENCE he has written round himself: a few layers of the ward, each holding a share of his
 * health, each of one grain (kinetic, which swords and arrows wear down, or arcane, which spells wear down). A layer takes full damage from its own grain
 * and 40% from the other, so a party of one build has to work harder than a mixed party; anti-magic reaches through both at 150%. When the last layer
 * falls he is UNWRITTEN for three seconds (he takes 150% and can do nothing), then writes the ward again one layer short for twenty seconds. That is the
 * raid shape the owner asked for ("break the shield, burst the window") and it replaces the old physical-attack immunity, which made melee builds useless.
 * The old class this comes from ({@code entity/ZagredDefense.Barrier}) keeps its place in the tree for the legacy boss; this one is pure (no game types,
 * no NBT) so every rule is unit tested, and it counts time in plain ticks passed in by the caller.
 *
 * <p>The ward is only the first four acts' shell. At the Gates it is switched off ({@code set(0, ...)}) and the heart takes its place
 * ({@link ZagredHeart}).
 */
public final class ZagredWard {
    /** What wears a layer down at full strength. */
    public enum Grain { KINETIC, ARCANE }

    /** Most layers a ward can have. */
    public static final int MAX = 5;
    /** Ticks he is unwritten after the last layer breaks (he takes 150% meanwhile). */
    public static final int REFORM_TICKS = 60;
    /** Ticks the rewritten ward is one layer short. */
    public static final int PENALTY_TICKS = 400;
    /** Ticks between two refreshes of the weakest layer, and the same with an Arch daemon anchoring him. */
    public static final int REFRESH_TICKS = 220, REFRESH_ANCHORED_TICKS = 120;
    /** The share of a layer a refresh gives back. */
    public static final float REFRESH_SHARE = 0.35f;
    /** What the other grain does to a layer, what anti-magic does, and what an unwritten body takes. */
    public static final float CROSS = 0.4f, PIERCE = 1.5f, UNWRITTEN = 1.5f;

    private final float[] hp = new float[MAX];
    private int count;
    private float cap = 1f;
    private long nextRefresh, reformUntil, penaltyUntil;
    private int broken;
    private boolean reformStarted;

    /** Writes a fresh ward: {@code layers} layers (0 = no ward) of {@code maxHealth x layerShare} each. */
    public void set(int layers, float maxHealth, float layerShare, long now) {
        count = Math.max(0, Math.min(MAX, layers));
        cap = Math.max(1f, maxHealth * layerShare);
        reformUntil = 0;
        penaltyUntil = 0;
        for (int i = 0; i < MAX; i++) hp[i] = i < count ? cap : 0f;
        nextRefresh = now + REFRESH_TICKS;
        broken = 0;
        reformStarted = false;
    }

    /** Does he have a ward at all (false at the Gates)? */
    public boolean present() { return count > 0; }

    /** Layers he is meant to have right now (one fewer for a while after a rewrite). */
    public int effective(long now) {
        if (count == 0) return 0;
        return now < penaltyUntil ? Math.max(1, count - 1) : count;
    }

    /** True while he is unwritten. */
    public boolean reforming(long now) { return now < reformUntil; }

    /** Layers still standing. */
    public int alive(long now) {
        int n = 0;
        for (int i = 0, e = effective(now); i < e; i++) if (hp[i] > 0.01f) n++;
        return n;
    }

    /** Layers to draw (none while he is unwritten). */
    public int layers(long now) { return reforming(now) ? 0 : effective(now); }

    /** The grain of layer {@code i} (0 is the innermost): they alternate, the innermost is arcane. */
    public static Grain grainOfLayer(int i) { return i % 2 == 0 ? Grain.ARCANE : Grain.KINETIC; }

    /** Is layer {@code i} standing? */
    public boolean standing(int i) { return i >= 0 && i < MAX && hp[i] > 0.01f; }

    /** The share (0..1) of layer {@code i} that is left. */
    public float fraction(int i) { return i < 0 || i >= MAX ? 0f : Math.max(0f, Math.min(1f, hp[i] / cap)); }

    /** What one layer holds. */
    public float cap() { return cap; }

    /**
     * A blow of {@code amount} reaches the ward. Returns what is left for his body: nothing when the layers took it all, the surplus at 150% when the
     * hit breaks the last layer or lands while he is unwritten, and the whole blow when there is no ward. Afterwards {@link #lastBroken} says how many
     * layers this blow broke and {@link #lastReformStarted} whether it opened the unwritten window.
     */
    public float absorb(float amount, Grain grain, boolean piercing, long now) {
        broken = 0;
        reformStarted = false;
        if (count == 0 || amount <= 0f) return Math.max(0f, amount);
        if (now < reformUntil) return amount * UNWRITTEN;
        int n = effective(now);
        float remaining = amount;
        for (int i = n - 1; i >= 0 && remaining > 0f; i--) {
            if (hp[i] <= 0.01f) continue;
            float k = piercing ? PIERCE : (grainOfLayer(i) == grain ? 1f : CROSS);
            float need = hp[i] / k;                                       // the damage this layer can still take
            if (remaining >= need) {
                remaining -= need;
                hp[i] = 0f;
                broken++;
            } else {
                hp[i] -= remaining * k;
                remaining = 0f;
            }
        }
        if (alive(now) == 0) {
            startReform(now);
            return remaining * UNWRITTEN;
        }
        return 0f;
    }

    /** Layers the last {@link #absorb} broke. */
    public int lastBroken() { return broken; }

    /** Did the last {@link #absorb} (or {@link #breakOne}) open the unwritten window? */
    public boolean lastReformStarted() { return reformStarted; }

    private void startReform(long now) {
        reformUntil = now + REFORM_TICKS;
        reformStarted = true;
        for (int i = 0; i < MAX; i++) hp[i] = 0f;
    }

    /** An Arch daemon fell or the Last Word struck: the outermost standing layer breaks at once. Returns false when none was standing. */
    public boolean breakOne(long now) {
        broken = 0;
        reformStarted = false;
        if (count == 0 || now < reformUntil) return false;
        for (int i = effective(now) - 1; i >= 0; i--) {
            if (hp[i] > 0.01f) {
                hp[i] = 0f;
                broken = 1;
                if (alive(now) == 0) startReform(now);
                return true;
            }
        }
        return false;
    }

    /**
     * One tick of upkeep. Returns true on the tick the ward is written again after an unwritten window (one layer short for {@link #PENALTY_TICKS}).
     * While he channels a long word nothing refreshes; the weakest layer gets a third back every eleven seconds (six with an anchor).
     */
    public boolean tick(long now, boolean channeling, boolean anchored) {
        if (count == 0) return false;
        if (reformUntil > 0 && now >= reformUntil) {
            reformUntil = 0;
            penaltyUntil = now + PENALTY_TICKS;
            int n = effective(now);
            for (int i = 0; i < MAX; i++) hp[i] = i < n ? cap : 0f;
            nextRefresh = now + REFRESH_TICKS;
            return true;
        }
        if (now < reformUntil || channeling) return false;
        if (now >= nextRefresh) {
            int n = effective(now), weakest = -1;
            for (int i = 0; i < n; i++) if (hp[i] < cap - 0.01f && (weakest < 0 || hp[i] < hp[weakest])) weakest = i;
            if (weakest >= 0) hp[weakest] = Math.min(cap, hp[weakest] + cap * REFRESH_SHARE);
            nextRefresh = now + (anchored ? REFRESH_ANCHORED_TICKS : REFRESH_TICKS);
        }
        return false;
    }

    // ---------------------------------------------------------------- save and load (the caller writes the numbers to NBT)
    /** The layers' hit points, a copy. */
    public float[] hitPoints() { return hp.clone(); }

    /** The number of layers he was given at the last {@link #set}. */
    public int count() { return count; }

    /** Restores a saved ward (a world that reloaded in the middle of a fight). Timers start fresh. */
    public void load(int layers, float layerCap, float[] saved, long now) {
        count = Math.max(0, Math.min(MAX, layers));
        cap = Math.max(1f, layerCap);
        reformUntil = 0;
        penaltyUntil = 0;
        for (int i = 0; i < MAX; i++) {
            float v = saved != null && i < saved.length ? saved[i] : 0f;
            hp[i] = i < count ? Math.max(0f, Math.min(cap, v)) : 0f;
        }
        nextRefresh = now + REFRESH_TICKS;
        broken = 0;
        reformStarted = false;
    }
}
