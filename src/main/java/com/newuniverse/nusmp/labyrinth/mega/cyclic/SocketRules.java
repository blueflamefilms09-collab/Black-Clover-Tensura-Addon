package com.newuniverse.nusmp.labyrinth.mega.cyclic;

/**
 * Why: this is the socket-matching algorithm in one place, as plain functions, so that the placement step of the generator, the loop pass and the tests all
 * apply the SAME rules. A doorway ("socket") of a piece is a jigsaw block at the centre of a module on a wall; two doorways form a passage when
 * <ol>
 * <li><b>names agree</b>: the target name of each is the name of the other (the vanilla {@code JigsawBlock.canAttach} rule; {@code nusmp:mega_door} meets
 *     {@code nusmp:mega_door}, and the boss-chain names meet only themselves, which is how a loop can never open into the vault);</li>
 * <li><b>they face each other</b>: A points east and B points west;</li>
 * <li><b>they line up</b>: the module in front of A is the module B stands in, and the module in front of B is the one A stands in. Because every piece is
 *     on the 9-block grid and every doorway is at a module centre, the second condition is a comparison of two integer cells, not of blocks.</li>
 * </ol>
 * The second half is the PLACEMENT score of the WFC-style step: when a socket has several candidates that fit, the one whose own doorways line up with open
 * doorways already in the plan (each such pair is a free loop) wins, one that would wall its own doorways into a neighbour's blank wall loses, and so does a
 * room with no doorway but its parent's (a dead end by construction). Pure.
 */
public final class SocketRules {
    private SocketRules() {}

    /** Do the two doorways line up: face each other and each stand in the module the other points at? */
    public static boolean aligned(LoopPlanner.Door a, LoopPlanner.Door b) {
        return a.dir() == b.dir().opposite() && a.front().equals(b.cell()) && b.front().equals(a.cell());
    }

    /** The name rule: each doorway's target is the other's name. */
    public static boolean namesMatch(String targetA, String nameA, String targetB, String nameB) {
        return targetA != null && targetB != null && targetA.equals(nameB) && targetB.equals(nameA);
    }

    /**
     * What the placement step knows about one fitting candidate.
     *
     * @param alignedPairs  how many of the candidate's other doorways line up with a doorway that is still open in the plan (each is a free loop)
     * @param blockedOwn    how many of its other doorways face a module that is already built but has no matching doorway (the doorway would be a niche in a blank wall)
     * @param killedOthers  how many open doorways of existing rooms the candidate's body covers without matching them (it walls them up)
     * @param freeFronts    how many of its other doorways face free space (they can still grow into more rooms or a hallway)
     * @param otherDoors    how many doorways it has besides the one that attaches it; 0 means a leaf
     * @param category      the candidate's functional category
     * @param leafAllowed   may the candidate be a leaf without penalty (a required room, a terminal, a piece the recipe forces)
     */
    public record PlacementFacts(int alignedPairs, int blockedOwn, int killedOthers, int freeFronts, int otherDoors, RoomCategory category, boolean leafAllowed) {}

    /**
     * The score of a candidate; higher is better. The first free loop is worth four points (the second 2.5, each further 1.5), a doorway walled into a blank wall costs one, walling up somebody else's
     * costs 0.6, a doorway that can still grow earns 0.15, a leaf costs 2.5, and the category nudge is {@code balanceWeight * 3 * want} (about -1 .. +1) plus a steep penalty once the category is more than
     * {@link CategoryBalance#SLACK} over its target share. The weights are chosen so one aligned pair outweighs any single blocked doorway, a category that is on
     * target never decides against a loop, and one that has run away (the Transition share of a corridor-heavy pool) does.
     */
    public static double placementScore(PlacementFacts f, LoopConfig cfg, int[] categoryCounts) {
        int n = f.alignedPairs();
        double loops = n == 0 ? 0.0 : 4.0 + 2.5 * Math.min(1, n - 1) + 1.5 * Math.max(0, n - 2);          // the first free loop is worth most: a room with five aligned doors is a grid, not a dungeon
        double s = loops - 1.0 * f.blockedOwn() - 0.6 * f.killedOthers() + 0.15 * f.freeFronts();
        if (f.otherDoors() == 0 && !f.leafAllowed()) s -= 2.5;
        if (cfg.balanceWeight() > 0 && categoryCounts != null) {
            double want = CategoryBalance.want(f.category(), categoryCounts);
            double nudge = 3.0 * want;
            if (want < -CategoryBalance.SLACK) nudge -= 40.0 * (-want - CategoryBalance.SLACK);              // far over its share: this alone outweighs two free loops
            s += cfg.balanceWeight() * nudge;
        }
        return s;
    }
}
