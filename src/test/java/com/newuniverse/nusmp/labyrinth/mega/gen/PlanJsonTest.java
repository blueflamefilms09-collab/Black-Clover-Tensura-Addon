package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the plan file is the one way the owner (and the lead's compositor) can LOOK at a sector without the game, so it has to be exact. The compositor
 * rebuilds a picture of a whole sector from the {@code pieces} list alone: for every piece it takes the template, turns it by {@code rotation} quarter turns
 * with the vanilla transform, and moves it to ({@code x}, {@code y}, {@code z}). This test does exactly that, independently (a second transcription of the
 * vanilla rule), for every piece and every socket of a real plan and for a plan with a tunnel, and demands the same boxes and the same socket cells the
 * assembler computed. It also checks the other keys the Python map preview reads, that door caps are listed, and that strings are escaped.
 */
class PlanJsonTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.standard();

    /** The vanilla rule with the pivot at the origin, written out again here on purpose. */
    private static int[] vanilla(int rotation, int i, int j, int k) {
        switch (rotation) {
            case 3: return new int[] {k, j, -i};
            case 1: return new int[] {-k, j, i};
            case 2: return new int[] {-i, j, -k};
            default: return new int[] {i, j, k};
        }
    }

    private static Map<String, Object> obj(Object o) { return MiniJson.object(o); }

    private static List<Object> arr(Object o) { return MiniJson.array(o); }

    private static int num(Object o) { return ((Number) o).intValue(); }

    @Test
    void thePiecesListRebuildsEveryBoxAndEverySocketCell() {
        SimWorld w = new SimWorld(CAT, 31, new Recipe(), 450, 40, 24);
        assertTrue(w.first());
        assertTrue(w.next(), w.stopped);
        for (SimWorld.Entry e : w.sectors) {
            Result r = e.result();
            String json = PlanJson.write(r, e.plan().entryFloor(), e.plan().forward(), e.solved().audit(), e.solved().tries(), e.solved().fallback(), "sector " + e.id());
            Map<String, Object> root = obj(MiniJson.parse(json));
            List<Object> pieces = arr(root.get("pieces"));
            assertEquals(r.pieces.size(), pieces.size(), "every piece is listed, caps and the tunnel included");
            for (int i = 0; i < pieces.size(); i++) {
                Map<String, Object> p = obj(pieces.get(i));
                Placed placed = r.pieces.get(i);
                assertEquals(placed.shape.id, p.get("template"));
                int rot = num(p.get("rotation"));
                assertEquals(placed.rot, rot);
                assertTrue(rot >= 0 && rot <= 3);
                assertEquals(placed.origin, new Vec(num(p.get("x")), num(p.get("y")), num(p.get("z"))));
                assertEquals(placed.depth, num(p.get("depth")));
                assertEquals(placed.parent, num(p.get("parent")));
                assertEquals(placed.shape.isCap(), p.get("cap"));
                assertEquals(placed.link, p.get("link"));
                // the compositor's view: origin + vanilla rotation of the template cells
                Shape sh = CAT.shape((String) p.get("template"));
                Vec o = new Vec(num(p.get("x")), num(p.get("y")), num(p.get("z")));
                int[] a = vanilla(rot, 0, 0, 0), b = vanilla(rot, sh.sx - 1, sh.sy - 1, sh.sz - 1);
                List<Object> box = arr(p.get("box"));
                assertEquals(o.x() + Math.min(a[0], b[0]), num(box.get(0)));
                assertEquals(o.y(), num(box.get(1)));
                assertEquals(o.z() + Math.min(a[2], b[2]), num(box.get(2)));
                assertEquals(o.x() + Math.max(a[0], b[0]), num(box.get(3)));
                assertEquals(o.y() + sh.sy - 1, num(box.get(4)));
                assertEquals(o.z() + Math.max(a[2], b[2]), num(box.get(5)));
                for (int s = 0; s < sh.sockets.size(); s++) {
                    Vec c = sh.sockets.get(s).cell();
                    int[] v = vanilla(rot, c.x(), c.y(), c.z());
                    assertEquals(new Vec(o.x() + v[0], o.y() + v[1], o.z() + v[2]), placed.socketCell(s), e.id() + " piece " + i + " socket " + s);
                }
            }
        }
    }

    @Test
    void theOtherKeysTheMapPreviewReadsAreThere() {
        SimWorld w = new SimWorld(CAT, 5, new Recipe(), 450, 40, 24);
        assertTrue(w.first());
        SimWorld.Entry e = w.sectors.get(0);
        Result r = e.result();
        Map<String, Object> root = obj(MiniJson.parse(PlanJson.write(r, e.plan().entryFloor(), e.plan().forward(), List.of(), 2, false, "test \"quoted\"")));
        assertEquals("test \"quoted\"", root.get("label"));
        assertEquals(2, num(root.get("tries")));
        assertEquals(false, root.get("fallback"));
        assertEquals(r.seed, ((Number) root.get("seed")).longValue());
        List<Object> region = arr(root.get("region"));
        assertEquals(List.of((long) r.region.minX(), (long) r.region.minZ(), (long) r.region.maxX(), (long) r.region.maxZ()), region);
        List<Object> entry = arr(root.get("entry"));
        assertEquals(0L, entry.get(0));
        assertEquals(64L, entry.get(1));
        assertEquals(0L, entry.get(2));
        assertEquals("east", entry.get(3));
        Map<String, Object> audit = obj(root.get("audit"));
        assertEquals(true, audit.get("ok"));
        assertEquals(0, arr(audit.get("failures")).size());
        List<Object> rooms = arr(root.get("rooms"));
        assertEquals(r.rooms().size(), rooms.size(), "rooms exclude door caps");
        for (Object ro : rooms) {
            Map<String, Object> room = obj(ro);
            assertEquals(4, arr(room.get("box")).size());
            assertNotNull(room.get("kind"));
            assertTrue(Set.of("LANDING", "HALL", "CORRIDOR", "JUNCTION", "SAFE", "TRIAL", "TREASURE", "SET_PIECE", "CAVERN", "TRAP", "PUZZLE", "ARENA", "ANTECHAMBER", "VAULT", "ONWARD", "LINK").contains(room.get("kind")));
        }
        List<Object> doors = arr(root.get("doors"));
        assertEquals(r.edges().size(), doors.size(), "one door mark per join between rooms and one per loop");
        for (Object d : doors) {
            List<Object> door = arr(d);
            assertEquals(3, door.size());
            assertTrue(Set.of("north", "east", "south", "west").contains(door.get(2)));
        }
        assertEquals(r.loops.size(), arr(root.get("loops")).size());
        Map<String, Object> exp = obj(root.get("expansion"));
        assertEquals(r.expansion.piece(), num(exp.get("piece")));
        assertEquals(r.expansion.facing().lower(), exp.get("facing"));
        assertEquals(List.of(3L, 4L, 2L), arr(exp.get("size")));
        assertEquals(false, exp.get("deadEnd"));
        assertFalse(obj(root.get("stats")).isEmpty());
        assertNotNull(root.get("warnings"));
    }

    @Test
    void aFailedAuditIsWrittenWithItsSentences() {
        SimWorld w = new SimWorld(CAT, 5, new Recipe(), 450, 40, 24);
        assertTrue(w.first());
        SimWorld.Entry e = w.sectors.get(0);
        Map<String, Object> root = obj(MiniJson.parse(PlanJson.write(e.result(), e.plan().entryFloor(), e.plan().forward(), List.of("only 3 rooms", "a \"bad\" thing"), 24, true, "x")));
        Map<String, Object> audit = obj(root.get("audit"));
        assertEquals(false, audit.get("ok"));
        assertEquals(List.of("only 3 rooms", "a \"bad\" thing"), arr(audit.get("failures")));
        assertEquals(true, root.get("fallback"));
    }

    @Test
    void stringsAreEscaped() {
        assertEquals("\"a\\\"b\\\\c\\nd\\te\"", PlanJson.str("a\"b\\c\nd\te"));
        assertEquals("null", PlanJson.str(null));
        assertEquals("\"\\u0001\"", PlanJson.str("\u0001"));
        assertEquals("\"plain text\"", PlanJson.str("plain text"));
    }

    private static final class Set {
        private Set() {}

        static java.util.Set<String> of(String... s) { return java.util.Set.of(s); }
    }
}
