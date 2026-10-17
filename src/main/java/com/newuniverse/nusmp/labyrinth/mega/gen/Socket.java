package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

/**
 * Why: a socket is one Jigsaw block of a template reduced to what the assembler needs to decide, in the TEMPLATE's own frame: the cell it
 * stands on (the first row above the floor, on the outer wall layer), which way it points out of the room, its name and the name it looks for in
 * a neighbour, the pool it draws from, and the two priorities. Keeping it a plain record (instead of the game's {@code StructureBlockInfo}) is what
 * lets the same assembler run over a JSON export of the real templates in a unit test. {@code facing} is horizontal only: the Mega-Dungeon has
 * no vertical joints, and the game-side reader drops (and logs) any other orientation.
 *
 * @param cell     the jigsaw cell in the template frame
 * @param facing   the direction the doorway points out of the piece
 * @param name     this socket's own name (what a neighbour's {@code target} must equal)
 * @param target   the name this socket looks for in the next piece
 * @param pool     the pool the next piece is drawn from; {@code minecraft:empty} means a sealed end (never filled, never capped)
 * @param selection  selection priority: higher sockets of a piece are tried first
 * @param placement  placement priority: higher sockets' children are queued first
 * @param finalState what the jigsaw cell becomes when nothing attaches (a block state string, kept for the plan file and the audit)
 */
public record Socket(Vec cell, Facing facing, String name, String target, String pool, int selection, int placement, String finalState) {

    /** A sealed end: the pool is {@code minecraft:empty}, so the generator neither fills it nor puts a cap on it. */
    public boolean sealed() { return MegaGrid.P_EMPTY.equals(pool); }

    /** Is this an ordinary door ({@code nusmp:mega_door}) rather than one of the boss-chain sockets? */
    public boolean isDoor() { return MegaGrid.DOOR.equals(name); }

    /** Is this one of the boss-chain sockets ({@code mega_chain}, {@code mega_vault}, {@code mega_onward}) that only the Sector Recipe fills? */
    public boolean isChain() { return MegaGrid.CHAIN.equals(name) || MegaGrid.VAULT.equals(name) || MegaGrid.ONWARD.equals(name); }

    /**
     * The game's {@code JigsawBlock.canAttach} for two sockets that are already in WORLD facings: the source's target must equal the candidate's name
     * and the two must point at each other. (Horizontal joints are all "aligned": there is nothing else to compare.)
     */
    public static boolean canAttach(String sourceTarget, Facing sourceFacing, String candidateName, Facing candidateFacing) {
        return sourceTarget.equals(candidateName) && sourceFacing == candidateFacing.opposite();
    }
}
