package com.newuniverse.nusmp.lords;

import net.minecraft.nbt.CompoundTag;

/**
 * Why: what each player has built with the Mortal Sin weapons, saved with the player ({@link LordStore}): one burden (0..100, the weapon's named drawback) and one mastery experience total per sin (the four weapons and
 * Greed's Half-Starved Mastery). One float and one int per sin, clamped when read, so a damaged save cannot give a number outside {@link LordRules}. The mastery belongs to the player, not to the weapon: a new wielder
 * starts from their own. The overload clock is not saved (an overload never survives a logout). Server thread only.
 */
public final class LordState {
    private final float[] burden = new float[LordRules.Sin.values().length];
    private final int[] xp = new int[LordRules.Sin.values().length];
    /** Game time at which an overload ends (not saved). */
    private final long[] overloadUntil = new long[LordRules.Sin.values().length];

    public boolean isEmpty() {
        for (int i = 0; i < xp.length; i++) if (xp[i] > 0 || burden[i] > 0.0f) return false;
        return true;
    }

    public float burden(LordRules.Sin s) { return burden[s.ordinal()]; }

    public void setBurden(LordRules.Sin s, float b) { burden[s.ordinal()] = LordRules.clampBurden(b); }

    public int xp(LordRules.Sin s) { return xp[s.ordinal()]; }

    public int level(LordRules.Sin s) { return LordRules.level(xp[s.ordinal()]); }

    /** Adds experience (never below 0, never above the top). Returns true when a level was gained. */
    public boolean addXp(LordRules.Sin s, int amount) {
        int before = level(s);
        xp[s.ordinal()] = LordRules.clampXp((long) xp[s.ordinal()] + Math.max(0, amount));
        return level(s) > before;
    }

    public long overloadUntil(LordRules.Sin s) { return overloadUntil[s.ordinal()]; }

    public void setOverloadUntil(LordRules.Sin s, long t) { overloadUntil[s.ordinal()] = t; }

    public boolean overloaded(LordRules.Sin s, long now) { return overloadUntil[s.ordinal()] > now; }

    /** Clears everything (the self-tests and {@code /multiverse lords} use it; never called by play). */
    public void reset() {
        java.util.Arrays.fill(burden, 0.0f);
        java.util.Arrays.fill(xp, 0);
        java.util.Arrays.fill(overloadUntil, 0L);
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        for (LordRules.Sin s : LordRules.Sin.values()) {
            t.putInt("xp_" + s.id, xp[s.ordinal()]);
            t.putFloat("burden_" + s.id, burden[s.ordinal()]);
        }
        return t;
    }

    public static LordState fromTag(CompoundTag t) {
        LordState st = new LordState();
        for (LordRules.Sin s : LordRules.Sin.values()) {
            st.xp[s.ordinal()] = LordRules.clampXp(t.getInt("xp_" + s.id));
            st.burden[s.ordinal()] = LordRules.clampBurden(t.getFloat("burden_" + s.id));
        }
        return st;
    }
}
