package com.newuniverse.nusmp.skilltree.unique;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a unique skill shows its name, its description and the names of its modes in Tensura's own Ability Selection screen, and every one of those texts must be a translatable key whose English fallback is
 * the text itself, so a missing lang entry shows the real words and not a raw key. The keys and the exact words are built here, in one pure class with no Minecraft type, from the shipped table of books
 * ({@link LineIndex.Line}); the skill class asks this class at run time and the lang fragment generator ({@code tools/skillbooks/gen_unique_books.py} writes the same words, and a unit test compares
 * them), so the three can never drift. The description is the line's own sentence, then one line listing the arts (the skill's modes, the first one is the signature art) and one listing what works while
 * the skill is on.
 */
public final class SkillTexts {
    private SkillTexts() {}

    /** The lang key of a skill's name: {@code nusmp.skill.line_<bare>}. */
    public static String nameKey(String bare) { return "nusmp.skill." + LineIndex.skillPath(bare); }

    /** The lang key of a skill's description. */
    public static String descKey(String bare) { return nameKey(bare) + ".description"; }

    /** The description of a skill: the line's sentence, its arts and its passives. */
    public static String description(LineIndex.Line line) {
        StringBuilder sb = new StringBuilder(line.desc());
        if (!line.arts().isEmpty()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("Arts (press the skill key, scroll to change): ").append(String.join(", ", line.arts())).append('.');
        }
        if (!line.passives().isEmpty()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("While switched on: ").append(String.join(", ", line.passives())).append('.');
        }
        return sb.toString();
    }

    /** Every lang entry the skill books need that is specific to a line, in a stable order (what the generator writes). */
    public static Map<String, String> langFor(List<LineIndex.Line> lines) {
        Map<String, String> out = new LinkedHashMap<>();
        for (LineIndex.Line l : lines) {
            out.put(nameKey(l.bare()), l.name());
            out.put(descKey(l.bare()), description(l));
        }
        return out;
    }

    /** The mode names of a line: the arts, or one generic mode when the line has no art. */
    public static List<String> modeNames(LineIndex.Line line) {
        if (!line.arts().isEmpty()) return line.arts();
        List<String> one = new ArrayList<>();
        one.add(line.name());
        return one;
    }
}
