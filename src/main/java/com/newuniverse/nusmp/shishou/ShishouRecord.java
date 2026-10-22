package com.newuniverse.nusmp.shishou;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: Shishou's own state (level, experience, mana, the skills he has eaten, his stat steps) is ONE record per world, because there is exactly one Shishou. The record lives in the world's saved data
 * ({@link ShishouData}), not on the item, so it survives the owner's death, a lost item and a recovery, and a copy of the item cannot carry a second copy of the sword's strength. The item only shows a mirror.
 * This is the pure core: experience and levels, mana that refills LAZILY from a time stamp (nothing ticks), the absorbed-skill bits, the capped steps, and a flat map form for saving. Server thread only.
 */
public final class ShishouRecord {
    private long totalExp = ShishouLevels.totalFor(ShishouRules.START_LEVEL);
    private double mana = ShishouRules.maxMana(ShishouRules.START_LEVEL, 0);
    private long manaStamp;
    private long learned;
    private int manaSteps, attackSteps;
    private long kills;
    private int selected = -1;
    private boolean broadcast, autoStomach;
    /** Wild phase counters: kills since the sword woke, the game time it woke, and its wild mana (which only the strikes of the wild phase spend). */
    private int wildKills;
    private long wildSince;
    private double wildMana = ShishouRules.WILD_MANA;

    // ---------------------------------------------------------------- level and experience
    public long totalExp() { return totalExp; }
    public int level() { return ShishouLevels.levelFor(totalExp); }
    public long kills() { return kills; }
    public double progress() { return ShishouLevels.progress(totalExp); }

    /** The result of adding experience: the levels before and after. */
    public record Gain(int from, int to, long added) {
        public boolean leveled() { return to > from; }
    }

    /** Adds experience (never negative, never above the cap total). */
    public Gain addExp(long amount) {
        int from = level();
        long add = Math.max(0L, amount);
        long before = totalExp;
        totalExp = Math.min(ShishouLevels.capTotal(), totalExp + add);
        int to = level();
        if (to > from) {
            double max = maxMana();
            mana = Math.min(max, mana + (max - mana) * 0.25);                          // a level up gives a quarter of the missing mana back
        }
        return new Gain(from, to, totalExp - before);
    }

    /**
     * Raises the sword to at least {@code minLevel} (never lowers anything): the fresh record starts at {@link ShishouRules#START_LEVEL}, and a config that asks for more raises it here. When the level rises the
     * pool is raised to the new maximum share it had (a full pool stays full). Returns true when the level rose.
     */
    public boolean raiseTo(int minLevel, long now) {
        int want = Math.max(1, Math.min(ShishouLevels.MAX_LEVEL, minLevel));
        if (level() >= want) return false;
        double share = maxMana() <= 0 ? 1.0 : Math.min(1.0, mana(now) / maxMana());
        totalExp = ShishouLevels.totalFor(want);
        mana = maxMana() * share;
        manaStamp = Math.max(manaStamp, now);
        return true;
    }

    public void setLevel(int level) {
        totalExp = ShishouLevels.totalFor(Math.max(1, Math.min(ShishouLevels.MAX_LEVEL, level)));
        mana = Math.min(mana, maxMana());
    }

    public void countKill() { kills++; }

    /** A kill made by the wild sword (the wild phase's own counter, which settles him into his pedestal). */
    public void countWildKill() { kills++; wildKills++; }

    // ---------------------------------------------------------------- mana (lazy)
    public double maxMana() { return ShishouRules.maxMana(level(), manaSteps); }

    /** The mana at game time {@code now}: the stored value plus the regeneration since the stamp (nothing runs in between). */
    public double mana(long now) {
        if (now <= manaStamp) return mana;
        double m = Math.min(maxMana(), mana + (now - manaStamp) * ShishouRules.regenPerTick(level()));
        return m;
    }

    private void settle(long now) {
        mana = mana(now);
        manaStamp = Math.max(manaStamp, now);
    }

    /** Pays {@code cost} from the pool at {@code now}; false (and nothing paid) when it does not cover it. */
    public boolean spend(long now, double cost) {
        if (cost <= 0.0) return true;
        settle(now);
        if (mana + 1e-9 < cost) return false;
        mana -= cost;
        return true;
    }

    public void refill(long now) {
        mana = maxMana();
        manaStamp = Math.max(manaStamp, now);
    }

    public void gainMana(long now, double amount) {
        settle(now);
        mana = Math.min(maxMana(), mana + Math.max(0.0, amount));
    }

    public boolean isLow(long now) { return mana(now) < maxMana() * ShishouRules.LOW_MANA_SHARE; }

    public boolean isEmpty(long now) { return mana(now) < 1.0; }

    // ---------------------------------------------------------------- skills
    public long learned() { return learned; }
    public int manaSteps() { return manaSteps; }
    public int attackSteps() { return attackSteps; }

    public boolean has(ShishouSkill s) { return s.available(level(), learned); }

    /** Records an absorbed skill; true when it was new. */
    public boolean learn(ShishouSkill s) {
        if (!s.absorbed()) return false;
        long bit = 1L << s.bit();
        if ((learned & bit) != 0L) return false;
        learned |= bit;
        return true;
    }

    public void forget(ShishouSkill s) { if (s.absorbed()) learned &= ~(1L << s.bit()); }

    public int absorbedCount() { return Long.bitCount(learned); }

    /** One capped step of base mana and/or attack (Skill Eater's growth); returns true when something rose. */
    public boolean step(boolean mana, boolean attack) {
        boolean r = false;
        if (mana && manaSteps < ShishouRules.MAX_STEPS) { manaSteps++; r = true; }
        if (attack && attackSteps < ShishouRules.MAX_STEPS) { attackSteps++; r = true; }
        return r;
    }

    public ShishouSkill selected() { return selected < 0 ? null : ShishouSkill.byBit(selected); }

    public void select(ShishouSkill s) { selected = s == null ? -1 : s.bit(); }

    /** The selected skill if it is still castable, else the first castable one, else null. */
    public ShishouSkill selectedOrFirst() {
        ShishouSkill s = selected();
        if (s != null && s.castable() && s.available(level(), learned)) return s;
        return ShishouSkill.nextCastable(null, level(), learned);
    }

    public boolean broadcast() { return broadcast; }
    public void setBroadcast(boolean b) { broadcast = b; }
    public boolean autoStomach() { return autoStomach; }
    public void setAutoStomach(boolean b) { autoStomach = b; }

    // ---------------------------------------------------------------- the wild phase
    public int wildKills() { return wildKills; }
    public long wildSince() { return wildSince; }
    public double wildMana() { return wildMana; }

    public void startWild(long now) {
        wildKills = 0;
        wildSince = now;
        wildMana = ShishouRules.WILD_MANA;
    }

    /** The wild sword pays for a strike out of its wild mana (not the pool of a wielder); false when it cannot. */
    public boolean spendWild(double cost) {
        if (wildMana + 1e-9 < cost) return false;
        wildMana -= cost;
        return true;
    }

    // ---------------------------------------------------------------- saving
    public Map<String, Long> toMap() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("exp", totalExp);
        m.put("mana", Double.doubleToLongBits(mana));
        m.put("stamp", manaStamp);
        m.put("learned", learned);
        m.put("msteps", (long) manaSteps);
        m.put("asteps", (long) attackSteps);
        m.put("kills", kills);
        m.put("sel", (long) selected);
        m.put("flags", (broadcast ? 1L : 0L) | (autoStomach ? 2L : 0L));
        m.put("wkills", (long) wildKills);
        m.put("wsince", wildSince);
        m.put("wmana", Double.doubleToLongBits(wildMana));
        m.put("ver", (long) ShishouLevels.CURVE_VERSION);
        return m;
    }

    private static long get(Map<String, Long> m, String k, long d) {
        Long v = m == null ? null : m.get(k);
        return v == null ? d : v;
    }

    public static ShishouRecord fromMap(Map<String, Long> m) {
        ShishouRecord r = new ShishouRecord();
        long exp = Math.max(0L, get(m, "exp", 0L));
        boolean legacy = m != null && get(m, "ver", 1L) < ShishouLevels.CURVE_VERSION;
        if (legacy) {                                                                         // 2.0.0 save: the old curve's level is kept (the new curve is gentler, so it never drops) and the new floor applies
            int oldLevel = ShishouLevels.legacyLevelFor(exp);
            exp = Math.max(ShishouLevels.totalFor(oldLevel), exp);
        }
        if (m == null || m.isEmpty()) exp = ShishouLevels.totalFor(ShishouRules.START_LEVEL);
        r.totalExp = Math.min(ShishouLevels.capTotal(), Math.max(exp, ShishouLevels.totalFor(ShishouRules.START_LEVEL)));
        double mm = Double.longBitsToDouble(get(m, "mana", Double.doubleToLongBits(ShishouRules.maxMana(ShishouRules.START_LEVEL, 0))));
        r.mana = Double.isNaN(mm) || mm < 0 ? 0.0 : mm;
        r.manaStamp = get(m, "stamp", 0L);
        r.learned = get(m, "learned", 0L) & validMask();
        r.manaSteps = (int) Math.max(0, Math.min(ShishouRules.MAX_STEPS, get(m, "msteps", 0L)));
        r.attackSteps = (int) Math.max(0, Math.min(ShishouRules.MAX_STEPS, get(m, "asteps", 0L)));
        r.kills = Math.max(0L, get(m, "kills", 0L));
        r.selected = (int) get(m, "sel", -1L);
        if (r.selected >= 0 && ShishouSkill.byBit(r.selected) == null) r.selected = -1;
        long f = get(m, "flags", 0L);
        r.broadcast = (f & 1L) != 0L;
        r.autoStomach = (f & 2L) != 0L;
        r.wildKills = (int) Math.max(0L, get(m, "wkills", 0L));
        r.wildSince = get(m, "wsince", 0L);
        double wm = Double.longBitsToDouble(get(m, "wmana", Double.doubleToLongBits(ShishouRules.WILD_MANA)));
        if (legacy) wm *= ShishouRules.WILD_MANA / ShishouRules.LEGACY_WILD_MANA;               // the wild budget of 2.0.0 (100) is the same share of the new one (400)
        r.wildMana = Double.isNaN(wm) ? ShishouRules.WILD_MANA : Math.max(0.0, Math.min(ShishouRules.WILD_MANA, wm));
        if (legacy) r.mana = Math.max(r.mana, r.maxMana() * Math.min(1.0, r.mana / Math.max(1.0, ShishouRules.LEGACY_MANA_BASE + ShishouRules.LEGACY_MANA_PER_LEVEL * ShishouLevels.legacyLevelFor(exp) + ShishouRules.LEGACY_MANA_PER_STEP * r.manaSteps)));   // the same share of the (larger) pool
        r.mana = Math.min(r.mana, r.maxMana());
        return r;
    }

    /** The bits that name an absorbed skill (a saved bit of a skill a later version removed is dropped). */
    public static long validMask() {
        long m = 0L;
        for (ShishouSkill s : ShishouSkill.values()) if (s.absorbed()) m |= 1L << s.bit();
        return m;
    }
}
