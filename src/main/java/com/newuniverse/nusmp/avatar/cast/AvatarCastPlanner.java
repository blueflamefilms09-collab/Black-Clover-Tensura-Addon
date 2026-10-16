package com.newuniverse.nusmp.avatar.cast;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.chatter.Rng;

import java.util.List;
import java.util.Set;

/**
 * Why: which page an Avatar picks is a matter of character: the aggressive and the dramatic reach for the biggest spell, the sadistic and the lazy
 * for the cheapest, the coward for something it can throw from afar. This pure scorer picks from the castable candidates by distance and
 * personality, with a seeded random tie-breaker, so the choice is testable.
 */
public final class AvatarCastPlanner {
    private AvatarCastPlanner() {}

    /** @param costPercent the page's cost in percent of max magicule; @param melee does the id read as a close-range move */
    public record Candidate(int mode, String id, double costPercent, boolean melee, AvatarSkillList.Kind kind) {}

    private static final List<String> MELEE_WORDS = List.of("strike", "thrust", "claw", "fang", "fist", "cleave", "rush", "lash", "whip");

    public static boolean looksMelee(String id) {
        for (String w : MELEE_WORDS) if (id.contains(w)) return true;
        return false;
    }

    /** The mode of the chosen candidate, or -1 when the list is empty. */
    public static int choose(List<Candidate> candidates, double distance, Set<AvatarPersonality> personalities, Rng rng) {
        int best = -1;
        double bestScore = -1e9;
        for (Candidate c : candidates) {
            if (c.kind() != AvatarSkillList.Kind.ATTACK) continue;
            double s = rng.nextDouble();                                         // tie-breaker, also what makes it look alive
            boolean close = distance < 4.5;
            s += c.melee() == close ? 0.6 : -0.3;                                 // the right range for the move
            double big = c.costPercent() / MAX_PERCENT;
            if (has(personalities, AvatarPersonality.AGGRESSIVE) || has(personalities, AvatarPersonality.CHUNIBYO)) s += big * 0.8;
            if (has(personalities, AvatarPersonality.SADISTIC) || has(personalities, AvatarPersonality.LAZY)) s += (1 - big) * 0.8;
            if (has(personalities, AvatarPersonality.COWARDLY)) s += (c.melee() ? -0.8 : 0.5);
            if (has(personalities, AvatarPersonality.SCHOLAR)) s += rng.nextDouble() * 0.3;
            if (s > bestScore) { bestScore = s; best = c.mode(); }
        }
        return best;
    }

    private static final double MAX_PERCENT = AvatarSkillList.MAX_COST_PERCENT;

    private static boolean has(Set<AvatarPersonality> set, AvatarPersonality p) { return set.contains(p); }
}
