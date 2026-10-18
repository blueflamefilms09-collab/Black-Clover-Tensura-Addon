package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.charter.CharterTable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: a Charter Grimoire is the reward of a boss and the Charter system has laws of its own (at most {@value CharterTable#MAX_SKILLS} skills, never a
 * skill that takes things from others, a grade of LEGEND or above), but the arc bosses declare their grimoire in the character record, in a builder's
 * own file. This is the one place that turns that declaration into a {@link CharterTable.Boss}, applying the laws, so a builder who lists thirteen
 * skills or a thief's skill gets a lawful grimoire instead of a broken table, and the result is testable without a game. It cannot know whether a skill id
 * really exists in Tensura (that is a registry question): the in-game self-test {@code arc.charter} checks every id against the skill registry.
 */
public final class ArcCharterRules {
    private ArcCharterRules() {}

    /** The grade of a grimoire: the god tier gives a GOD book, everything else LEGEND (the lowest grade the Charter allows). */
    public static String gradeFor(int tier) { return tier >= ArcBossProfile.MAX_TIER ? "GOD" : "LEGEND"; }

    /** Skill ids as the Charter wants them: {@code namespace:path}, lower case, no duplicates, none of {@link CharterTable#NEVER_LENT}, at most the cap. */
    public static List<String> sanitize(List<String> ids) {
        Set<String> out = new LinkedHashSet<>();
        if (ids != null) {
            for (String raw : ids) {
                if (raw == null) continue;
                String id = raw.trim().toLowerCase(Locale.ROOT);
                if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || CharterTable.NEVER_LENT.contains(id)) continue;
                out.add(id);
                if (out.size() >= CharterTable.MAX_SKILLS) break;
            }
        }
        return new ArrayList<>(out);
    }

    /** The ids a builder listed that {@link #sanitize} would drop (for the self-test and the command to report). */
    public static List<String> dropped(List<String> ids) {
        List<String> kept = sanitize(ids), out = new ArrayList<>();
        if (ids != null) for (String raw : ids) if (raw != null && !kept.contains(raw.trim().toLowerCase(Locale.ROOT))) out.add(raw);
        return out;
    }

    /** The lore line of the grimoire's tooltip. */
    public static String loreFor(ArcCharacter c) {
        return "The Canon of " + c.fullName() + (c.source().isEmpty() ? "" : ", from " + c.source()) + ": their power on loan for as long as you hold it.";
    }

    /** The Charter row of a boss, or null when the character has no grimoire (not a boss, no skills, or none left after the laws). */
    public static CharterTable.Boss toBoss(ArcCharacter c) {
        if (c == null || !c.hasCharter()) return null;
        List<String> skills = sanitize(c.charterSkills());
        if (skills.isEmpty()) return null;
        return new CharterTable.Boss(c.charterId(), c.id(), c.name(), gradeFor(c.tier()), c.charterChance(), c.color() & 0xFFFFFF, c.accent() & 0xFFFFFF, loreFor(c), skills);
    }
}
