package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.boss.BossBuffTable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the global buff engine ({@link BossBuffTable}) has a hand-written row for each of the seven old bosses, but the arcs add thirty more and no one
 * should write a row per character. A character already carries a threat tier (1 minor ... 5 god tier, see {@link ArcCharacter}); this class turns the
 * tier into the engine's row, so a new boss gets sensible permanent barriers, a theme skill and an anti-magic aura just by existing in the roster, and
 * every tier is one line to tune here. The rows only use what the boss rules allow (no regeneration effect, no Unique or Ultimate skill: the engine
 * would strip them anyway, see {@code BossSkillRules}), so the table says what really reaches the boss.
 *
 * <p>The ladder follows the old rows: tier 1 is the Goliath's thin shell, tier 2 adds the second barrier (the Dark Triad's), tier 3 the haki coat, tier 4
 * a stronger magic barrier, tier 5 every barrier at level 2 (Lucius's, minus the regeneration he is not allowed any more).
 */
public final class ArcBossProfile {
    private ArcBossProfile() {}

    /** One rung of the ladder: the numbers of a {@link BossBuffTable.Profile} without the boss id. */
    public record Template(int tier, Map<String, Integer> levels, List<String> skills, int auraRadius, int auraJamSeconds, double auraChance, int pierceTicks, int soulPoints) {
        public Template {
            levels = Map.copyOf(levels);
            skills = List.copyOf(skills);
        }
    }

    public static final int MIN_TIER = 1, MAX_TIER = 5;

    private static Map<String, Integer> lv(Object... pairs) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], (Integer) pairs[i + 1]);
        return m;
    }

    private static final Template[] LADDER = {
            new Template(1, lv(BossBuffTable.PHYSICAL_BARRIER, 1), List.of(BossBuffTable.MORTAL_FEAR), 8, 2, 0.30, 0, 0),
            new Template(2, lv(BossBuffTable.MAGIC_BARRIER, 1, BossBuffTable.PHYSICAL_BARRIER, 1), List.of(BossBuffTable.MORTAL_FEAR), 10, 2, 0.30, 10, 0),
            new Template(3, lv(BossBuffTable.MAGIC_BARRIER, 1, BossBuffTable.PHYSICAL_BARRIER, 1, BossBuffTable.HAKI_COAT, 1), List.of(BossBuffTable.MORTAL_FEAR), 10, 2, 0.35, 20, 1),
            new Template(4, lv(BossBuffTable.MAGIC_BARRIER, 2, BossBuffTable.PHYSICAL_BARRIER, 1, BossBuffTable.HAKI_COAT, 1), List.of(BossBuffTable.MORTAL_FEAR), 12, 3, 0.40, 30, 2),
            new Template(5, lv(BossBuffTable.MAGIC_BARRIER, 2, BossBuffTable.PHYSICAL_BARRIER, 2, BossBuffTable.HAKI_COAT, 2), List.of(BossBuffTable.MORTAL_FEAR), 14, 3, 0.45, 40, 3),
    };

    /** The rung for a tier (clamped to 1..5). */
    public static Template forTier(int tier) { return LADDER[Math.max(MIN_TIER, Math.min(MAX_TIER, tier)) - 1]; }

    /** The engine row of the boss {@code bossId} at {@code tier}. */
    public static BossBuffTable.Profile profileFor(String bossId, int tier) {
        Template t = forTier(tier);
        return new BossBuffTable.Profile(bossId, t.tier(), t.levels(), t.skills(), t.auraRadius(), t.auraJamSeconds(), t.auraChance(), t.pierceTicks(), t.soulPoints());
    }

    public static BossBuffTable.Profile profileFor(ArcCharacter c) { return profileFor(c.id(), c.tier()); }
}
