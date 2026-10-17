package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: one piece of a sector plan: which shape, where (the origin is the world cell where the template's local cell (0,0,0) goes, the same
 * position the game's {@code PoolElementStructurePiece} is built with), how it is turned, how deep in the placement tree it sits, and which socket of
 * which parent it hangs from. It is also the working record of the search: {@code used} remembers which of its own sockets already lead somewhere
 * (a child, its parent, a loop partner), so the door-cap pass and the loop detector can tell a doorway that opens into a room from one that is
 * still waiting. Everything that never changes after placement is final; only {@code used} is mutable, and only the assembler writes it.
 */
public final class Placed {
    public final int index;
    public final Shape shape;
    /** The processor list of the pool element this piece was drawn from (opaque to the assembler; the placer needs it). */
    public final String processors;
    /** Clockwise quarter turns, 0..3 (vanilla {@code Rotation.ordinal()}). */
    public final int rot;
    public final Vec origin;
    public final Box box;
    /** Placement-tree depth: 0 for the Landing, parent + 1 below it, -1 for the tunnel pieces that lead to the Landing. */
    public final int depth;
    /** Index of the parent piece, or -1. */
    public final int parent;
    /** Index of the parent's socket this piece hangs from, or -1. */
    public final int parentSocket;
    /** Index of this piece's own socket that attaches to the parent, or -1. */
    public final int ownSocket;
    /** True for the pieces of the bridge between two sectors (outside every region). */
    public final boolean link;
    private final boolean[] used;

    public Placed(int index, Shape shape, String processors, int rot, Vec origin, int depth, int parent, int parentSocket, int ownSocket, boolean link) {
        this.index = index;
        this.shape = shape;
        this.processors = processors;
        this.rot = rot & 3;
        this.origin = origin;
        this.box = shape.box(origin, this.rot);
        this.depth = depth;
        this.parent = parent;
        this.parentSocket = parentSocket;
        this.ownSocket = ownSocket;
        this.link = link;
        this.used = new boolean[shape.sockets.size()];
        if (ownSocket >= 0 && ownSocket < used.length) used[ownSocket] = true;
    }

    /** The world cell of own socket {@code i}. */
    public Vec socketCell(int i) { return Rot.world(origin, rot, shape.sockets.get(i).cell()); }

    /** The world direction own socket {@code i} points out of the piece. */
    public Facing socketFacing(int i) { return Rot.facing(rot, shape.sockets.get(i).facing()); }

    /** The world cell in front of own socket {@code i}: where a neighbour's matching socket would stand. */
    public Vec socketFront(int i) { return socketCell(i).step(socketFacing(i)); }

    /** Does own socket {@code i} already lead somewhere? */
    public boolean isUsed(int i) { return used[i]; }

    void markUsed(int i, boolean v) { used[i] = v; }

    /**
     * A copy with the same placement and the same used flags. The tunnel pieces of a request are shared by every try of the solver, and a try
     * writes to the used flags (a door cap on a waystation's side door), so each try starts from its own copy.
     */
    Placed copy() {
        Placed c = new Placed(index, shape, processors, rot, origin, depth, parent, parentSocket, ownSocket, link);
        System.arraycopy(used, 0, c.used, 0, used.length);
        return c;
    }

    /** The world cell of template cell {@code c}. */
    public Vec world(Vec c) { return Rot.world(origin, rot, c); }

    @Override
    public String toString() { return "#" + index + " " + shape.id + " r" + rot + " @" + origin + " d" + depth; }
}
