package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.skill.codex.AnimeSkill;

import java.util.ArrayList;
import java.util.List;

/** Damage and phase rules for Riven's raised-ceiling encounter. */
final class RivenCombat {
    private RivenCombat() {}

    static int phase(float healthFraction, long fightTicks, int currentPhase) {
        if (currentPhase >= 4 || healthFraction <= 0.25f) return 4;
        if (currentPhase >= 3 || fightTicks >= 1200) return 3;
        if (currentPhase >= 2 || fightTicks >= 400 || healthFraction <= 0.75f) return 2;
        return 1;
    }

    static int nextPhase(int currentPhase, int desiredPhase) {
        return Math.min(4, Math.max(currentPhase, Math.min(desiredPhase, currentPhase + 1)));
    }

    static int rollInterval(int phase) {
        return phase >= 3 ? 160 : phase >= 2 ? 240 : 240;
    }

    static String combatType(int roll) {
        return switch (Math.floorMod(roll, 6)) {
            case 0 -> "Caster";
            case 1 -> "Hexblade";
            case 2 -> "Legion";
            case 3 -> "Rift";
            case 4 -> "Song";
            default -> "Hack";
        };
    }

    static String avatarFor(int phase, boolean tank, boolean healer, boolean flier, boolean magicNull, boolean caster) {
        if (phase >= 4) return "The Creator";
        if (phase == 3) {
            if (flier) return "Anti-Spiral";
            if (magicNull) return "Arceus";
            if (healer || caster) return "Lord of Nightmares";
            return "Grand Zeno";
        }
        if (phase == 2) {
            if (tank) return "Beerus";
            if (healer) return "Ultimate Madoka";
            return "Zeus";
        }
        return "";
    }

    static List<AnimeSkill> grimoirePages(Iterable<AnimeSkill> skills, int phase) {
        List<AnimeSkill> pages = new ArrayList<>();
        for (AnimeSkill skill : skills) {
            if ("black_clover".equals(skill.anime()) && firstPhase(skill) <= phase
                    && !skill.id().contains("anti_magic") && !skill.nativeId().contains("anti_magic")) pages.add(skill);
        }
        return List.copyOf(pages);
    }

    static boolean signature(AnimeSkill skill) {
        if (skill == null || skill.nativeId().equals("crown_break")) return false;
        return switch (skill.nativeId()) {
            case "page_tear", "hexblade_waltz", "severance_aria", "island_fall", "maw_of_the_rift", "final_page", "unwritten_ending",
                    "gold_ring", "two_moons", "doom_gate", "discord" -> true;
            default -> skill.tier() >= 4;
        };
    }

    static int firstPhase(AnimeSkill skill) {
        return switch (skill.nativeId()) {
            case "page_tear", "bull_ward", "soul_note", "eldritch_verse" -> 1;
            case "discord" -> 2;
            case "severance_aria", "legion_knight", "hexblade_waltz" -> 2;
            case "island_fall", "maw_of_the_rift", "gold_ring", "two_moons", "doom_gate" -> 3;
            case "final_page", "unwritten_ending", "audience_collapse", "crown_break", "rewrite_round" -> 4;
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

    static float audienceDamage(int livingPlayers) {
        return 48f + Math.max(0, livingPlayers - 1) * 8f;
    }

    static long nextSignatureAt(long tick) { return tick + 80; }
    static boolean signatureReady(long tick, long readyAt) { return tick >= readyAt; }
    static int eldritchBoltDelay(int boltIndex) { return Math.max(0, boltIndex) * 4; }
    static int hexbladeSwingDelay(int swingIndex) { return Math.max(0, swingIndex) * 4; }
}
