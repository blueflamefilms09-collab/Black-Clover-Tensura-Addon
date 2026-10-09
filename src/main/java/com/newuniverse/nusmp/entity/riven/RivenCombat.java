package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.skill.codex.AnimeSkill;

/** Damage and phase rules for Riven's raised-ceiling encounter. */
final class RivenCombat {
    private RivenCombat() {}

    static int phase(float healthFraction) {
        return healthFraction > 0.75f ? 1 : healthFraction > 0.50f ? 2 : healthFraction > 0.25f ? 3 : 4;
    }

    static boolean signature(AnimeSkill skill) {
        return switch (skill.nativeId()) {
            case "page_tear", "severance_aria", "island_fall", "maw_of_the_rift", "final_page", "unwritten_ending" -> true;
            default -> skill.tier() >= 4;
        };
    }

    static int firstPhase(AnimeSkill skill) {
        return switch (skill.nativeId()) {
            case "page_tear" -> 1;
            case "severance_aria" -> 2;
            case "island_fall", "maw_of_the_rift" -> 3;
            case "final_page", "unwritten_ending" -> 4;
            default -> 1;
        };
    }

    static float damage(int phase, boolean signature, float roll) {
        float r = Math.max(0f, Math.min(1f, roll));
        if (signature) return switch (phase) {
            case 1 -> 22f;
            case 2 -> 42f;
            case 3 -> 62f;
            default -> 85f;
        };
        float min = switch (phase) { case 1 -> 12f; case 2 -> 22f; case 3 -> 34f; default -> 48f; };
        float max = switch (phase) { case 1 -> 18f; case 2 -> 34f; case 3 -> 48f; default -> 64f; };
        return min + r * (max - min);
    }

    static int castTicks(int baseTicks, int phase) {
        float speed = 1f + 0.08f * (phase - 1);
        return Math.max(14, Math.min(28, Math.round(baseTicks / speed)));
    }
}
