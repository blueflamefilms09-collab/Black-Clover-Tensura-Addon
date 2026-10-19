package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.unique.MasteryLevels;

/**
 * Why: the owner's rule for a passive is "it works only while its skill is owned AND switched on", and a rule that is written in every one of 51 executors is a rule that is eventually forgotten in one of them
 * (and a forgotten gate here means a passive of a skill that is switched off quietly keeps working). So the rule is written ONCE, in this pure class, and every path into the engine goes through it: the
 * passive dispatcher asks {@link #passiveLevel} before it runs any passive or attribute executor, the stance reads {@link #activeLevel}. An executor therefore cannot be reached for a node that is not allowed to
 * act, and a test of this class is a test of the whole rule. Since 1.96.0 there are no bought node levels and no "equipped line": a node counts at the level its skill's mastery gives ({@link MasteryLevels#level}),
 * and the gate's two questions are only "is this node of the kind that acts in this way" and "is the skill switched on". Pure (no Minecraft type), so it is tested without a game.
 */
public final class LineGate {
    private LineGate() {}

    /**
     * The level at which a passive or attribute node counts RIGHT NOW, or 0: the node must be a line node with a switch (a passive or a stat bonus) and the skill must be switched on.
     *
     * @param skillOn         is the unique skill of the node's line switched on (Tensura's toggle)
     * @param masteryFraction the skill's mastery, 0..1
     */
    public static int passiveLevel(boolean skillOn, SkillNode node, double masteryFraction) {
        if (node == null || !node.hasToggle() || !skillOn) return 0;
        return MasteryLevels.level(masteryFraction, node.maxLevel());
    }

    /** The level at which an art or a stance counts RIGHT NOW, or 0 when the node is not an active line node. The caller has already checked that the art has awakened and that the skill is owned. */
    public static int activeLevel(SkillNode node, double masteryFraction) {
        if (node == null || !node.isLineNode() || node.nodeType() != SkillNode.NodeType.ACTIVE) return 0;
        return MasteryLevels.level(masteryFraction, node.maxLevel());
    }
}
