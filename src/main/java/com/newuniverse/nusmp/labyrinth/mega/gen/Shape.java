package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a Shape is a template as the assembler sees it: its size, its kind (taken from the folder it lives in), its sockets and its data markers,
 * all in the template's own frame and all immutable. The search never touches a block: it only turns shapes by quarter turns and asks "does the
 * box fit and does a socket line up". Two facts that the plan depends on are worked out once here instead of in every search step: which socket is
 * the END of a Way Onward corridor (the one under an EXPANSION_SOCKET marker, so the generator knows where the rubble seal is and which way the next
 * sector attaches) and how many sockets could still need a door cap (the budget the assembler keeps for caps so a sector never exceeds its piece limit).
 * {@code max} limits how many times one template may appear in one sector (0 = no limit): unique set pieces use it.
 */
public final class Shape {
    public final String id;
    public final int sx, sy, sz;
    public final RoomKind kind;
    public final List<Socket> sockets;
    public final List<ShapeMarker> markers;
    public final int max;
    /** Index of the socket under the EXPANSION_SOCKET marker (the end of a Way Onward), or -1. */
    public final int expansionSocket;
    /** Did the template carry an EXPANSION_SOCKET marker at all? (An Onward without one has no readable seal.) */
    public final boolean expansionMarked;
    /** The size of the rubble seal (width, height, depth) from the marker's {@code w}, {@code h}, {@code d} arguments (defaults 3, 4, 2). */
    public final int sealW, sealH, sealD;
    private final int open;

    public Shape(String id, int sx, int sy, int sz, RoomKind kind, List<Socket> sockets, List<ShapeMarker> markers, int max) {
        this.id = id;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.kind = kind;
        this.sockets = List.copyOf(sockets);
        this.markers = List.copyOf(markers);
        this.max = Math.max(0, max);
        int o = 0;
        for (Socket s : this.sockets) if (!s.sealed()) o++;
        this.open = o;
        int exp = -1;
        boolean marked = false;
        int w = 3, h = 4, d = 2;
        for (ShapeMarker m : this.markers) {
            if (m.spec().kind() != MarkerKind.EXPANSION_SOCKET) continue;
            marked = true;
            w = m.spec().intArg("w", 3);
            h = m.spec().intArg("h", 4);
            d = m.spec().intArg("d", 2);
            Vec below = m.cell().add(0, -1, 0);
            for (int i = 0; i < this.sockets.size(); i++) {
                if (this.sockets.get(i).cell().equals(below)) { exp = i; break; }
            }
            if (exp < 0) {
                int sealedCount = 0, last = -1;
                for (int i = 0; i < this.sockets.size(); i++) if (this.sockets.get(i).sealed()) { sealedCount++; last = i; }
                if (sealedCount == 1) exp = last;
            }
            break;
        }
        this.expansionSocket = exp;
        this.expansionMarked = marked;
        this.sealW = w;
        this.sealH = h;
        this.sealD = d;
    }

    /** Is this a door cap (3 wide, at most 8 deep, one socket)? */
    public boolean isCap() { return kind == RoomKind.CAP; }

    /** How many sockets are not sealed ends: the most door caps this shape could still ask for. */
    public int openSockets() { return open; }

    /** The bounding box of this shape placed at {@code origin} turned {@code r} quarter turns. */
    public Box box(Vec origin, int r) { return Rot.box(origin, r, sx, sy, sz); }

    /** The indices of the sockets with this name, in file order. */
    public List<Integer> socketsNamed(String name) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < sockets.size(); i++) if (sockets.get(i).name().equals(name)) out.add(i);
        return out;
    }

    /** How many markers of this kind the template has. */
    public int countMarkers(MarkerKind k) {
        int n = 0;
        for (ShapeMarker m : markers) if (m.spec().kind() == k) n++;
        return n;
    }

    @Override
    public String toString() { return id + " " + sx + "x" + sy + "x" + sz + " " + kind + " sockets=" + sockets.size(); }
}
