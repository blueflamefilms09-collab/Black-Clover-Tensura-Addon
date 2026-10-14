package com.newuniverse.nusmp.boss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Mod Boss Global Buff Engine's numbers (pure, unit tested): for each boss id the permanent Tensura effects (with a base level),
 * the thematic skills, the anti-magic aura and the attack-profile engravings. {@link #plan} turns a profile into what should be on the
 * boss for a global multiplier, and {@link #missing} compares it with what is already there, so applying a plan twice changes
 * nothing (idempotence).
 * <p>
 * Levels are 1-based ("level 1" = amplifier 0). A level is scaled by the global multiplier ({@code bosses.buffMultiplier}) and
 * truncated, so a multiplier under 1 thins the buffs out and 0 removes them. Nothing here ever reads an entity: the glue passes the
 * boss id in and applies the plan.
 */
public final class BossBuffTable {
    private BossBuffTable() {}

    public static final String MAGIC_BARRIER = "tensura:magic_barrier", PHYSICAL_BARRIER = "tensura:physical_barrier",
            HAKI_COAT = "tensura:haki_coat", INSTANT_REGENERATION = "tensura:instant_regeneration";
    public static final String STARVED = "tensura:starved", MATHEMATICIAN = "tensura:mathematician", MORTAL_FEAR = "tensura:mortal_fear";

    /** The effects the engine owns: the four permanent ones (barriers and regeneration are the ones suspended while exposed). */
    public static final List<String> EFFECT_IDS = List.of(MAGIC_BARRIER, PHYSICAL_BARRIER, HAKI_COAT, INSTANT_REGENERATION);

    /** Highest level any effect gets, whatever the multiplier. */
    public static final int MAX_LEVEL = 4;
    /** Highest multiplier the config may ask for. */
    public static final double MAX_MULTIPLIER = 3.0;

    /**
     * One boss's row.
     * @param tier        1 floor boss .. 4 endgame god
     * @param levels      effect id -> base level (1-based), only the effects the boss gets
     * @param skills      thematic Tensura skill ids lent to the boss itself
     * @param auraRadius  blocks of its anti-magic aura (0 = none)
     * @param auraJamSeconds  how long a caught Ultimate skill of a player is jammed (a cooldown, never a removal)
     * @param auraChance  chance per Ultimate skill and refresh that it is caught
     * @param pierceTicks barrier_piercing engraving: ticks a hit shaves off a player's Tensura barrier effects (0 = none)
     * @param soulPoints  soul_eater engraving: spiritual health a hit takes from a player (floor 1, 0 = none)
     */
    public record Profile(String bossId, int tier, Map<String, Integer> levels, List<String> skills, int auraRadius, int auraJamSeconds,
                          double auraChance, int pierceTicks, int soulPoints) {
        public Profile {
            levels = Collections.unmodifiableMap(new LinkedHashMap<>(levels));
            skills = List.copyOf(skills);
        }
    }

    private static Map<String, Integer> lv(Object... pairs) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], (Integer) pairs[i + 1]);
        return m;
    }

    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();
    static {
        // floor bosses: a thin shell, one theme skill, a small aura
        PROFILES.put(BossRegistry.GOLIATH, new Profile(BossRegistry.GOLIATH, 1, lv(PHYSICAL_BARRIER, 1), List.of(MORTAL_FEAR), 8, 2, 0.30, 0, 0));
        PROFILES.put(BossRegistry.CHARYBDIS_SPAWN, new Profile(BossRegistry.CHARYBDIS_SPAWN, 1, lv(MAGIC_BARRIER, 1), List.of(STARVED), 8, 2, 0.30, 0, 0));
        // the Dark Triad: both barriers and the haki coat, a theme skill each
        PROFILES.put(BossRegistry.DANTE, new Profile(BossRegistry.DANTE, 2, lv(MAGIC_BARRIER, 1, PHYSICAL_BARRIER, 1, HAKI_COAT, 1), List.of(STARVED, MORTAL_FEAR), 10, 2, 0.35, 20, 1));
        PROFILES.put(BossRegistry.VANICA, new Profile(BossRegistry.VANICA, 2, lv(MAGIC_BARRIER, 1, PHYSICAL_BARRIER, 1, HAKI_COAT, 1), List.of(STARVED, MATHEMATICIAN), 10, 2, 0.35, 20, 1));
        PROFILES.put(BossRegistry.ZENON, new Profile(BossRegistry.ZENON, 2, lv(MAGIC_BARRIER, 1, PHYSICAL_BARRIER, 1, HAKI_COAT, 1), List.of(MATHEMATICIAN, MORTAL_FEAR), 10, 2, 0.35, 20, 1));
        // the Devil Avatar: stronger barriers and a trickle of regeneration
        PROFILES.put(BossRegistry.DEVIL_AVATAR, new Profile(BossRegistry.DEVIL_AVATAR, 3, lv(MAGIC_BARRIER, 2, PHYSICAL_BARRIER, 2, HAKI_COAT, 1, INSTANT_REGENERATION, 1),
                List.of(STARVED, MORTAL_FEAR), 12, 3, 0.40, 30, 2));
        // Lucius: everything, at the highest level the engine hands out
        PROFILES.put(BossRegistry.LUCIUS, new Profile(BossRegistry.LUCIUS, 4, lv(MAGIC_BARRIER, 2, PHYSICAL_BARRIER, 2, HAKI_COAT, 2, INSTANT_REGENERATION, 1),
                List.of(STARVED, MATHEMATICIAN, MORTAL_FEAR), 14, 3, 0.45, 40, 3));
    }

    /** The profile of a boss id, or null when it is not one of the table's bosses (so the engine does nothing for it). */
    public static Profile profile(String bossId) { return bossId == null ? null : PROFILES.get(bossId); }

    public static List<Profile> all() { return List.copyOf(PROFILES.values()); }

    /** The multiplier the engine really uses: NaN or negative = 0, never above {@link #MAX_MULTIPLIER}. */
    public static double clampMultiplier(double m) {
        if (Double.isNaN(m) || m < 0) return 0;
        return Math.min(MAX_MULTIPLIER, m);
    }

    /** The scaled level of one effect: base level x multiplier, truncated, 0..{@link #MAX_LEVEL}. 0 = the effect is not applied. */
    public static int scaledLevel(int baseLevel, double multiplier) {
        double v = baseLevel * clampMultiplier(multiplier) + 1e-9;
        return Math.max(0, Math.min(MAX_LEVEL, (int) v));
    }

    /** What should be on a boss: effect id -> amplifier (level - 1), and the skills to lend. */
    public record Plan(Map<String, Integer> amplifiers, Set<String> skills) {
        public Plan {
            amplifiers = Collections.unmodifiableMap(new LinkedHashMap<>(amplifiers));
            skills = Collections.unmodifiableSet(new LinkedHashSet<>(skills));
        }
        public boolean isEmpty() { return amplifiers.isEmpty() && skills.isEmpty(); }
    }

    /**
     * The plan for a boss id at a multiplier. Unknown ids give an empty plan. While 'suspended' (the boss is exposed after an ultimate,
     * transforming, or Julius's window is up) the barrier and regeneration effects are left out so the fight stays winnable; the
     * thematic skills stay.
     */
    public static Plan plan(String bossId, double multiplier, boolean suspended) {
        Profile p = profile(bossId);
        if (p == null) return new Plan(Map.of(), Set.of());
        Map<String, Integer> amps = new LinkedHashMap<>();
        if (!suspended && clampMultiplier(multiplier) > 0)
            for (Map.Entry<String, Integer> e : p.levels().entrySet()) {
                int level = scaledLevel(e.getValue(), multiplier);
                if (level > 0) amps.put(e.getKey(), level - 1);
            }
        Set<String> skills = clampMultiplier(multiplier) > 0 ? new LinkedHashSet<>(p.skills()) : new LinkedHashSet<>();
        return new Plan(amps, skills);
    }

    /** Fraction of its health at or under which a boss's Instant Regeneration is taken off (Tensura's effect can stop a lethal blow). */
    public static final double REGEN_OFF_BELOW = 0.5;
    /** Once off, the effect only comes back above this fraction (so it does not flicker around the line). */
    public static final double REGEN_BACK_ABOVE = 0.6;

    /**
     * Like {@link #plan(String, double, boolean)}, and with 'lowHealth' set the Instant Regeneration is left out as well: Tensura's effect
     * can stop a death, so a boss in the second half of its health bar must not carry it (the fight has to stay winnable).
     */
    public static Plan plan(String bossId, double multiplier, boolean suspended, boolean lowHealth) {
        Plan p = plan(bossId, multiplier, suspended);
        if (!lowHealth || !p.amplifiers().containsKey(INSTANT_REGENERATION)) return p;
        Map<String, Integer> amps = new LinkedHashMap<>(p.amplifiers());
        amps.remove(INSTANT_REGENERATION);
        return new Plan(amps, p.skills());
    }

    /** Is the boss low enough that the regeneration must be off? 'wasLow' gives the hysteresis. */
    public static boolean lowHealth(double fraction, boolean wasLow) {
        if (Double.isNaN(fraction)) return true;
        return wasLow ? fraction < REGEN_BACK_ABOVE : fraction <= REGEN_OFF_BELOW;
    }

    /** What is on a boss right now (as far as the engine owns it). */
    public record Applied(Map<String, Integer> amplifiers, Set<String> skills) {
        public Applied {
            amplifiers = Map.copyOf(amplifiers);
            skills = Set.copyOf(skills);
        }
        public static Applied none() { return new Applied(Map.of(), Set.of()); }
    }

    /** What still has to be done to bring 'now' to 'plan': effects missing or at the wrong amplifier, skills not yet lent. Empty = nothing to do. */
    public static Plan missing(Plan plan, Applied now) {
        Map<String, Integer> amps = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : plan.amplifiers().entrySet())
            if (!e.getValue().equals(now.amplifiers().get(e.getKey()))) amps.put(e.getKey(), e.getValue());
        Set<String> skills = new LinkedHashSet<>();
        for (String s : plan.skills()) if (!now.skills().contains(s)) skills.add(s);
        return new Plan(amps, skills);
    }

    /** The engine effects that are on a boss but no longer in the plan (suspension, a lowered multiplier, the switch off). */
    public static List<String> surplus(Plan plan, Applied now) {
        List<String> out = new ArrayList<>();
        for (String id : EFFECT_IDS) if (now.amplifiers().containsKey(id) && !plan.amplifiers().containsKey(id)) out.add(id);
        return out;
    }

    /** Folds a plan into a state (what the glue does for real): the new state after applying 'plan' on 'now'. */
    public static Applied apply(Plan plan, Applied now) {
        Map<String, Integer> amps = new LinkedHashMap<>(now.amplifiers());
        amps.putAll(plan.amplifiers());
        Set<String> skills = new LinkedHashSet<>(now.skills());
        skills.addAll(plan.skills());
        return new Applied(amps, skills);
    }
}
