package com.newuniverse.nusmp.avatar.cast;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.chatter.Rng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Why: the owner wants the Avatar's Soul Forge choices to be what it fights with: the Ultimate and the Unique skill it was given, the Magic Aptitudes it was
 * born with, the move of its awakened perk, and then the creator's own grimoire and plain Tensura magic. That is one ordered list ("the virtual skill
 * loadout") and one decision: which entry to press now. This pure class holds the shape of an entry, the fixed ranking of the kinds (the creator's picks before
 * the fallbacks), the order, and the choice, which looks at the fight (distance, how big the enemy is, the Avatar's health, whether the fight has just
 * begun) and at the personality (the aggressive and the dramatic reach for the big skill, the sadistic and the lazy for the cheap one, a coward for something
 * it can throw from afar). Readiness (cooldown, cost, learned) is the glue's job: only ready entries are handed in.
 */
public final class LoadoutPlanner {
    private LoadoutPlanner() {}

    /** Where an entry comes from, best first. */
    public enum Kind { ULTIMATE, UNIQUE, PERK, APTITUDE_SKILL, APTITUDE_PAGE, CREATOR_PAGE, TENSURA_MAGIC }

    /**
     * One thing the Avatar can press.
     *
     * @param id           stable key (a Tensura skill id, a page id, a perk id): cooldowns are kept by it
     * @param label        what the Avatar shouts
     * @param source       which grimoire copy a PAGE belongs to (0 = the creator's), else 0
     * @param mode         the page's mode, or the skill's first mode to try
     * @param costPercent  cost in percent of the Avatar's magicule pool (the planner only compares)
     * @param cooldownTicks the wait before this entry may be pressed again (before the INT / WIS / personality / frenzy scaling)
     * @param melee        does it read as a close-range move
     * @param rank         the position in the loadout, 0 first ({@link #rankOf})
     */
    public record Entry(Kind kind, String id, String label, int source, int mode, SkillRoles.Role role, double costPercent, int cooldownTicks, boolean melee, int rank) {}

    /** The fight at this moment. */
    public record Ctx(double distance, double targetMaxHealth, double hpFraction, boolean fightJustStarted) {}

    /** The fixed position of a kind in the loadout; {@code index} orders entries of the same kind (aptitudes in the order they were chosen). */
    public static int rankOf(Kind kind, int index) {
        int i = Math.max(0, Math.min(index, 9));
        return switch (kind) {
            case ULTIMATE -> 0;
            case UNIQUE -> 1;
            case PERK -> 2;
            case APTITUDE_SKILL -> 3 + i;
            case APTITUDE_PAGE -> 13 + i;
            case CREATOR_PAGE -> 25;
            case TENSURA_MAGIC -> 35;
        };
    }

    /** The loadout in order: by rank, then by id so the order is stable. */
    public static List<Entry> order(List<Entry> entries) {
        List<Entry> out = new ArrayList<>(entries);
        out.sort(Comparator.comparingInt(Entry::rank).thenComparing(Entry::id));
        return out;
    }

    /** An Ultimate or a big skill is spent on an enemy that matters: a big one, or when the Avatar itself is in trouble. */
    public static boolean worthTheBigOne(Ctx c) { return c.targetMaxHealth() >= 60.0 || c.hpFraction() < 0.5; }

    /**
     * The index (into {@code ready}) of the entry to press, or -1. Order of care: a heal when below half health, then a buff when the fight has just begun,
     * then the best attack by rank, range, size and personality. Nothing is chosen from an empty list.
     */
    public static int pick(List<Entry> ready, Ctx c, Set<AvatarPersonality> personalities, Rng rng) {
        if (ready == null || ready.isEmpty()) return -1;
        Set<AvatarPersonality> ps = personalities == null ? Set.of() : personalities;
        if (c.hpFraction() < 0.5) {
            int heal = firstOf(ready, SkillRoles.Role.HEAL);
            if (heal >= 0) return heal;
        }
        if (c.fightJustStarted()) {
            int buff = firstOf(ready, SkillRoles.Role.BUFF);
            if (buff >= 0) return buff;
        }
        int best = -1;
        double bestScore = -1e9;
        boolean close = c.distance() < 4.5;
        for (int i = 0; i < ready.size(); i++) {
            Entry e = ready.get(i);
            if (e.role() != SkillRoles.Role.ATTACK) continue;
            double s = rng.nextDouble() * 0.4 + 1.0 / (1.0 + e.rank() * 0.25);
            s += e.melee() == close ? 0.5 : -0.25;
            double big = Math.min(1.0, e.costPercent() / AvatarSkillList.MAX_COST_PERCENT);
            if (ps.contains(AvatarPersonality.AGGRESSIVE) || ps.contains(AvatarPersonality.CHUNIBYO)) s += big * 0.6;
            if (ps.contains(AvatarPersonality.SADISTIC) || ps.contains(AvatarPersonality.LAZY)) s += (1.0 - big) * 0.6;
            if (ps.contains(AvatarPersonality.COWARDLY)) s += e.melee() ? -0.6 : 0.4;
            if (ps.contains(AvatarPersonality.SCHOLAR)) s += rng.nextDouble() * 0.2;
            if ((e.kind() == Kind.ULTIMATE || e.kind() == Kind.UNIQUE) && !worthTheBigOne(c)) s -= e.kind() == Kind.ULTIMATE ? 2.0 : 0.4;
            if (s > bestScore) { bestScore = s; best = i; }
        }
        return best;
    }

    private static int firstOf(List<Entry> ready, SkillRoles.Role role) {
        int best = -1;
        for (int i = 0; i < ready.size(); i++) {
            Entry e = ready.get(i);
            if (e.role() == role && (best < 0 || e.rank() < ready.get(best).rank())) best = i;
        }
        return best;
    }
}
