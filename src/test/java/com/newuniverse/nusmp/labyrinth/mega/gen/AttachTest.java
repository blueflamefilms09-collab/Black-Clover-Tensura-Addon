package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: whether two pieces can be glued is two small rules (the source's target equals the candidate's name; the doorways point at each other), and where
 * a socket of a placed piece ends up in the world is the rotation maths applied to a template cell. Both are tested with numbers worked out by hand,
 * because a wrong sign here would not crash anything: it would shift every doorway of every sector by a few blocks and the rooms would simply not line up.
 */
class AttachTest {

    @Test
    void canAttachNeedsTheNameAndOppositeFacings() {
        assertTrue(Socket.canAttach("nusmp:mega_door", Facing.NORTH, "nusmp:mega_door", Facing.SOUTH));
        assertFalse(Socket.canAttach("nusmp:mega_door", Facing.NORTH, "nusmp:mega_door", Facing.NORTH), "same direction");
        assertFalse(Socket.canAttach("nusmp:mega_door", Facing.NORTH, "nusmp:mega_door", Facing.EAST), "at right angles");
        assertFalse(Socket.canAttach("nusmp:mega_door", Facing.EAST, "nusmp:mega_chain", Facing.WEST), "a door does not look for a chain socket");
        assertFalse(Socket.canAttach("nusmp:mega_chain", Facing.EAST, "nusmp:mega_door", Facing.WEST), "the rule is one-way: target against name");
        assertTrue(Socket.canAttach("nusmp:mega_chain", Facing.WEST, "nusmp:mega_chain", Facing.EAST));
    }

    @Test
    void socketKindsAreRecognisedByNameAndPool() {
        Socket door = new Socket(new Vec(4, 9, 0), Facing.NORTH, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_SIDE, 0, 0, "minecraft:air");
        Socket seal = new Socket(new Vec(4, 9, 0), Facing.NORTH, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_EMPTY, 0, 0, "minecraft:cobbled_deepslate");
        Socket chain = new Socket(new Vec(4, 9, 0), Facing.NORTH, MegaGrid.CHAIN, MegaGrid.CHAIN, MegaGrid.P_ANTECHAMBER, 0, 0, "minecraft:air");
        assertTrue(door.isDoor());
        assertFalse(door.sealed());
        assertFalse(door.isChain());
        assertTrue(seal.sealed(), "an empty pool is a sealed end");
        assertTrue(chain.isChain());
        assertFalse(chain.isDoor());
    }

    @Test
    void aPlacedPiecesSocketLandsWhereTheHandCalculationSays() {
        // a 27 x 48 x 18 room with a door at template cell (13, 9, 0) facing north, placed at (100, 56, 200)
        Shape room = new Shape("t/room", 27, 48, 18, RoomKind.HALL,
                List.of(new Socket(new Vec(13, 9, 0), Facing.NORTH, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_SIDE, 0, 0, "minecraft:air")), List.of(), 0);
        Vec o = new Vec(100, 56, 200);
        Placed r0 = new Placed(0, room, "p", 0, o, 0, -1, -1, -1, false);
        assertEquals(new Vec(113, 65, 200), r0.socketCell(0));
        assertEquals(Facing.NORTH, r0.socketFacing(0));
        assertEquals(new Vec(113, 65, 199), r0.socketFront(0));
        Placed r1 = new Placed(0, room, "p", 1, o, 0, -1, -1, -1, false);
        assertEquals(new Vec(100, 65, 213), r1.socketCell(0), "CLOCKWISE_90 sends (13, 0) to (0, 13)");
        assertEquals(Facing.EAST, r1.socketFacing(0));
        assertEquals(new Vec(101, 65, 213), r1.socketFront(0));
        Placed r2 = new Placed(0, room, "p", 2, o, 0, -1, -1, -1, false);
        assertEquals(new Vec(87, 65, 200), r2.socketCell(0));
        assertEquals(Facing.SOUTH, r2.socketFacing(0));
        Placed r3 = new Placed(0, room, "p", 3, o, 0, -1, -1, -1, false);
        assertEquals(new Vec(100, 65, 187), r3.socketCell(0), "COUNTERCLOCKWISE_90 sends (13, 0) to (0, -13)");
        assertEquals(Facing.WEST, r3.socketFacing(0));
        assertTrue(r1.box.contains(r1.socketCell(0)), "a socket cell lies in its own piece's box");
        assertFalse(r1.box.contains(r1.socketFront(0)), "and the cell in front of it does not");
    }

    @Test
    void theAttachedSocketIsMarkedUsedAndCopiesKeepTheirOwnFlags() {
        Shape room = new Shape("t/room", 9, 48, 9, RoomKind.HALL, List.of(
                new Socket(new Vec(4, 9, 0), Facing.NORTH, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_SIDE, 0, 0, "minecraft:air"),
                new Socket(new Vec(4, 9, 8), Facing.SOUTH, MegaGrid.DOOR, MegaGrid.DOOR, MegaGrid.P_SIDE, 0, 0, "minecraft:air")), List.of(), 0);
        Placed p = new Placed(3, room, "p", 0, Vec.ZERO, 2, 1, 0, 1, false);
        assertTrue(p.isUsed(1), "the socket that attaches to the parent is used");
        assertFalse(p.isUsed(0));
        Placed copy = p.copy();
        copy.markUsed(0, true);
        assertTrue(copy.isUsed(0));
        assertFalse(p.isUsed(0), "a copy has its own flags");
        assertTrue(copy.isUsed(1));
        assertEquals(p.box, copy.box);
    }

    @Test
    void shapesFindTheirExpansionSocketUnderTheMarker() {
        SyntheticCatalog c = SyntheticCatalog.standard();
        Shape straight = c.shape("nusmp:mega/onward/straight_a");
        assertNotNull(straight);
        assertTrue(straight.expansionMarked);
        assertEquals(1, straight.expansionSocket, "the north door under the marker at (4,10,0)");
        assertEquals(new Vec(4, 9, 0), straight.sockets.get(straight.expansionSocket).cell());
        assertEquals(3, straight.sealW);
        assertEquals(4, straight.sealH);
        assertEquals(2, straight.sealD);
        Shape hall = c.shape("nusmp:mega/hall/cell_1x1");
        assertEquals(-1, hall.expansionSocket);
        assertFalse(hall.expansionMarked);
        assertEquals(4, hall.openSockets());
        Shape cap = c.shape("nusmp:mega/cap/alcove_3");
        assertTrue(cap.isCap());
        assertEquals(0, cap.openSockets(), "a cap's only socket is a sealed end");
    }
}
