package com.newuniverse.nusmp.skilltree.line;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the legacy switches ({@link PassiveToggles}, still read from old saves) must default ON, load tolerantly and be cleared with their route; the gate ({@link LineGate}) is the ONE place that decides
 * whether a passive, an attribute bonus or an art may act, so every combination of (skill on/off, mastery, node type) is tried here and only the allowed one answers a level.
 */
class ToggleAndGateTest {
    private static SkillRoute fixture() throws Exception {
        JsonObject o = JsonParser.parseString(Files.readString(Path.of("src/test/resources/lines_sample/fixture_line_0.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        SkillRoute r = RouteSchema.parse("nusmp:fixture_line_0", o).route();
        assertNotNull(r);
        return r;
    }

    private static SkillNode first(SkillRoute r, SkillNode.NodeType t) {
        for (SkillNode n : r.nodes()) if (n.nodeType() == t) return n;
        throw new AssertionError("no node of type " + t);
    }

    @Test
    void aSwitchIsOnUntilItIsTurnedOffAndBackOnAgain() {
        PassiveToggles t = new PassiveToggles();
        assertTrue(t.isEnabled("nusmp:a", "x"), "no entry means ON: passives are on by default");
        assertTrue(t.set("nusmp:a", "x", false), "turning it off changes it");
        assertFalse(t.isEnabled("nusmp:a", "x"));
        assertFalse(t.set("nusmp:a", "x", false), "turning it off twice changes nothing the second time");
        assertTrue(t.set("nusmp:a", "x", true));
        assertTrue(t.isEnabled("nusmp:a", "x"));
        assertFalse(t.set("nusmp:a", "y", true), "setting a never-stored switch to ON is not a change");
    }

    @Test
    void loadingToleratesMissingAndUnknownThings() {
        assertTrue(PassiveToggles.fromMap(null).isEmpty(), "a missing map is an empty set");
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("nusmp:a#x", (byte) 0);
        raw.put("nusmp:a#y", (byte) 1);
        raw.put("nusmp:a#z", true);
        raw.put("no_hash", true);
        raw.put("#nonode", true);
        raw.put("noroute#", true);
        raw.put("two#hash#es", true);
        raw.put("nusmp:a#text", "yes");
        raw.put(null, true);
        PassiveToggles t = PassiveToggles.fromMap(raw);
        assertEquals(3, t.size(), "only the three well-formed numeric or boolean entries survive");
        assertFalse(t.isEnabled("nusmp:a", "x"));
        assertTrue(t.isEnabled("nusmp:a", "y") && t.isEnabled("nusmp:a", "z"));
        assertTrue(t.isEnabled("nusmp:a", "never-stored"));
        Map<String, Boolean> view = t.asMap();
        boolean threw = false;
        try {
            view.put("a#b", false);
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue(threw, "the view cannot be edited from outside");
    }

    @Test
    void aRespecClearsTheSwitchesOfThatRouteOnly() {
        PassiveToggles t = new PassiveToggles();
        t.set("nusmp:a", "x", false);
        t.set("nusmp:a", "y", false);
        t.set("nusmp:ab", "x", false);
        assertEquals(2, t.clearRoute("nusmp:a"));
        assertTrue(t.isEnabled("nusmp:a", "x"));
        assertFalse(t.isEnabled("nusmp:ab", "x"), "a route whose id merely starts with the same text is not touched");
    }

    @Test
    void copyingReplacesTheContent() {
        PassiveToggles a = new PassiveToggles(), b = new PassiveToggles();
        a.set("r", "n", false);
        b.set("q", "m", false);
        a.copyFrom(b);
        assertTrue(a.isEnabled("r", "n"));
        assertFalse(a.isEnabled("q", "m"));
    }

    @Test
    void aPassiveActsOnlyWhenItsSkillIsSwitchedOn() throws Exception {
        SkillRoute r = fixture();
        SkillNode passive = first(r, SkillNode.NodeType.PASSIVE);
        assertEquals(0, LineGate.passiveLevel(false, passive, 0.0), "the skill is off");
        assertEquals(1, LineGate.passiveLevel(true, passive, 0.0), "on, no mastery: level 1");
        assertEquals(passive.maxLevel(), LineGate.passiveLevel(true, passive, 1.0), "on, fully mastered: the node's top level");
        assertEquals(0, LineGate.passiveLevel(true, null, 0.5), "no node, no level");
    }

    @Test
    void anAttributeBonusFollowsTheSameGateAndAnArtIsNeverAPassive() throws Exception {
        SkillRoute r = fixture();
        SkillNode attr = first(r, SkillNode.NodeType.ATTR), art = first(r, SkillNode.NodeType.ACTIVE);
        assertEquals(1, LineGate.passiveLevel(true, attr, 0.0));
        assertEquals(0, LineGate.passiveLevel(true, art, 0.5), "an art has no switch, so the passive gate never lets it through");
        assertEquals(1, LineGate.activeLevel(art, 0.0));
        assertEquals(0, LineGate.activeLevel(attr, 0.5), "an attribute bonus is not an art");
        assertEquals(0, LineGate.passiveLevel(false, attr, 1.0), "a switched-off skill gives no stat bonus");
    }

    @Test
    void everyNodeOfTheFixtureLineIsSortedIntoExactlyOneGate() throws Exception {
        SkillRoute r = fixture();
        int passive = 0, active = 0;
        for (SkillNode n : r.nodes()) {
            boolean p = LineGate.passiveLevel(true, n, 0.0) > 0, a = LineGate.activeLevel(n, 0.0) > 0;
            assertTrue(p ^ a, n.id() + " (" + n.kind() + ") must pass exactly one gate");
            if (p) passive++;
            else active++;
        }
        assertEquals(r.nodes().size(), passive + active);
        assertTrue(passive >= 8 && active >= 10);
    }
}
