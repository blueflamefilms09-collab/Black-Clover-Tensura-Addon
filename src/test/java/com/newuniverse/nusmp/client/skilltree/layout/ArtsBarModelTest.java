package com.newuniverse.nusmp.client.skilltree.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Arts bar is the only way to use an art of a generated line, so what it lists, how the selection survives a purchase, and how the strip scrolls are the whole user experience of casting. The model
 * is pure, so each rule is pinned: the bar holds exactly the arts the server's gate would accept (bought, active, of the equipped line, nothing else), the selection follows the art and not the position, a
 * cycle wraps in both directions, an empty bar has no selection, and a strip narrower than the list keeps the chosen art on screen.
 */
class ArtsBarModelTest {
    private static SkillRoute fixture() throws Exception {
        JsonObject o = JsonParser.parseString(Files.readString(Path.of("src/test/resources/lines_sample/fixture_line_1.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        return RouteSchema.parse("nusmp:fixture_line_1", o).route();
    }

    private static SimpleTreeState owning(SkillRoute r, boolean equipped, int... boughtActive) {
        SimpleTreeState s = new SimpleTreeState().unlock(r.id());
        if (equipped) s.withClass(r.classId(), 0);
        int seen = 0;
        for (SkillNode n : r.nodes()) {
            if (n.nodeType() != SkillNode.NodeType.ACTIVE) continue;
            for (int b : boughtActive) if (b == seen) s.setLevel(r.id(), n.id(), 1);
            seen++;
        }
        return s;
    }

    @Test
    void theBarHoldsExactlyTheBoughtArtsOfTheEquippedLine() throws Exception {
        SkillRoute r = fixture();
        for (SkillNode n : r.nodes()) assertTrue(n.isLineNode());
        List<ArtsBarModel.Entry> none = ArtsBarModel.collect(r, owning(r, false, 0, 1, 2));
        assertTrue(none.isEmpty(), "a line that is not equipped has no arts in the bar");
        assertTrue(ArtsBarModel.collect(null, owning(r, true)).isEmpty());
        List<ArtsBarModel.Entry> three = ArtsBarModel.collect(r, owning(r, true, 0, 2, 5));
        assertEquals(3, three.size());
        SimpleTreeState s = owning(r, true, 0);
        for (SkillNode n : r.nodes()) if (n.nodeType() == SkillNode.NodeType.PASSIVE || n.nodeType() == SkillNode.NodeType.ATTR) s.setLevel(r.id(), n.id(), 1);
        assertEquals(1, ArtsBarModel.collect(r, s).size(), "bought passives and stats are not arts");
        assertTrue(ArtsBarModel.collect(r, s).get(0).stance(), "the first active node of a generated line is a stance");
    }

    @Test
    void theSelectionFollowsTheArtNotThePosition() throws Exception {
        SkillRoute r = fixture();
        ArtsBarModel m = new ArtsBarModel();
        assertNull(m.selected());
        assertEquals(-1, m.selectedIndex());
        m.cycle(1);
        m.set(ArtsBarModel.collect(r, owning(r, true, 2, 4, 6)));
        assertEquals(0, m.selectedIndex(), "the first art is selected by default");
        m.cycle(1);
        String chosen = m.selected().key();
        assertEquals(1, m.selectedIndex());
        m.set(ArtsBarModel.collect(r, owning(r, true, 0, 2, 4, 6)));          // an art bought before the chosen one: it moves to index 2, the choice stays
        assertEquals(chosen, m.selected().key());
        assertEquals(2, m.selectedIndex());
        m.set(ArtsBarModel.collect(r, owning(r, true, 0)));                   // the chosen art is gone: back to the first
        assertEquals(0, m.selectedIndex());
        m.set(List.of());
        assertNull(m.selected());
    }

    @Test
    void cyclingWrapsBothWaysAndSelectByKeyWorks() throws Exception {
        SkillRoute r = fixture();
        ArtsBarModel m = new ArtsBarModel();
        m.set(ArtsBarModel.collect(r, owning(r, true, 0, 1, 2, 3)));
        m.cycle(-1);
        assertEquals(3, m.selectedIndex());
        m.cycle(1);
        assertEquals(0, m.selectedIndex());
        m.cycle(9);
        assertEquals(1, m.selectedIndex());
        assertTrue(m.select(m.entries().get(3).key()));
        assertEquals(3, m.selectedIndex());
        assertFalse(m.select("nusmp:fixture_line_1#nothing"));
        assertEquals(3, m.selectedIndex());
    }

    @Test
    void theStripKeepsTheChosenArtOnScreen() throws Exception {
        SkillRoute r = fixture();
        ArtsBarModel m = new ArtsBarModel();
        m.set(ArtsBarModel.collect(r, owning(r, true, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9)));
        assertEquals(10, m.entries().size());
        assertEquals(0, m.windowStart(12), "everything fits");
        for (int i = 0; i < 10; i++) {
            int start = m.windowStart(5);
            assertTrue(start >= 0 && start + 5 <= 10, "the window stays in the list");
            assertTrue(m.selectedIndex() >= start && m.selectedIndex() < start + 5, "and shows the chosen art (index " + m.selectedIndex() + ", start " + start + ")");
            m.cycle(1);
        }
    }
}
