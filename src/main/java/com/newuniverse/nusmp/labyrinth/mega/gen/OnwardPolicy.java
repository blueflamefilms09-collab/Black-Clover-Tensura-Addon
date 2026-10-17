package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.List;

/**
 * Why: the end of a sector (the Way Onward, with its rubble seal) decides where the NEXT sector can go, and that depends on every sector that
 * already exists, which the assembler must not know about (it plans one sector from a region, some exclusions and a seed). So the assembler asks
 * this interface two questions when it places the Way Onward: which variants (straight, left, right) to try, in what order, and for a candidate seal
 * at a given cell and facing, whether the next sector could be attached behind it and how long the tunnel to it would be. The planner implements it
 * over the saved sectors; a test implements it with a few lines. The assembler picks the candidate with the lowest cost, which is the one that "points to
 * free space", and falls back to the template's own pool when the policy lists no variants.
 */
public interface OnwardPolicy {
    /** Pool ids of the Way Onward variants, best first (for example {@code nusmp:mega/onward_straight}); empty to use the pool written in the template. */
    List<String> variants();

    /**
     * How good a rubble seal is whose jigsaw cell is {@code sealCell} and whose doorway points {@code facing}: LOWER is better (the planner returns the number
     * of 9-block tunnel segments the next sector would need, plus a penalty for each way out of the next region that is already blocked), and -1 means no
     * next sector can be attached there at all (it would run into an older sector).
     */
    int cost(Vec sealCell, Facing facing);
}
