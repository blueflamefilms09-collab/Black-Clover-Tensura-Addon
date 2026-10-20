package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.BindGesture.Catalog;
import com.newuniverse.nusmp.relics.djinn.BindGesture.Entry;
import com.newuniverse.nusmp.relics.djinn.BindGesture.Gesture;
import com.newuniverse.nusmp.relics.djinn.BindGesture.Look;
import com.newuniverse.nusmp.relics.djinn.BindGesture.Stack;
import com.newuniverse.nusmp.relics.djinn.BindGesture.Why;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.101.0: binding a Djinn Seal with Better Combat installed. Pure: the failing path (Better Combat hides the off hand behind a two-handed weapon,
 * so the Seal's "other hand" read came back EMPTY), the fix (the raw inventory), every gesture, the refusal reasons, and a vessel that carries
 * extra components (Better Combat's preset id, attribute modifiers, other mods' data).
 */
class BindGestureTest {
    private static final String ME = "00000000-0000-0000-0000-00000000000a";
    private static final String OTHER = "00000000-0000-0000-0000-00000000000b";

    /** A tiny relic catalog: a Legend weapon vessel, a God weapon vessel, a Legend relic that is not a vessel, a Rare relic, a plain item. */
    private static final Catalog CATALOG = id -> switch (id) {
        case "nusmp:vorpal_sword", "nusmp:noon_cleaver" -> new Entry(true, true, true);
        case "nusmp:staff_of_the_magi" -> new Entry(true, true, true);
        case "nusmp:ring_free" -> new Entry(true, true, false);                 // a vessel that needs no attunement
        case "nusmp:legend_toy" -> new Entry(false, true, true);                  // Legend, not flagged as a vessel
        case "nusmp:rare_blade" -> new Entry(true, false, true);                  // flagged, below Legend
        default -> null;
    };

    private static Map<String, String> comps(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static Look look(String item, Map<String, String> c) { return BindGesture.look(Stack.of(item, c), CATALOG, ME); }

    // ---------------------------------------------------------------- the failing path, modelled
    @Test
    void betterCombatHidesTheOffHandBehindATwoHandedWeapon() {
        // the Seal in the main hand, a two-handed vessel in the off hand: the old read saw nothing
        assertEquals("EMPTY", BcHands.legacyOther(true, "seal", "noon_cleaver", "EMPTY", false, true), "off-hand weapon two-handed: hidden");
        assertEquals("EMPTY", BcHands.legacyOther(true, "seal", "ring", "EMPTY", true, false), "a two-handed weapon in the main hand hides the off hand too");
        assertEquals("noon_cleaver", BcHands.rawOther(true, "seal", "noon_cleaver"), "the raw inventory is not touched by Better Combat");
        // without a two-handed weapon the two reads agree (this is why it only broke with the mod and only for some weapons)
        assertEquals("vorpal_sword", BcHands.legacyOther(true, "seal", "vorpal_sword", "EMPTY", false, false));
        assertEquals(BcHands.rawOther(true, "seal", "vorpal_sword"), BcHands.legacyOther(true, "seal", "vorpal_sword", "EMPTY", false, false));
        // the Seal in the off hand, the vessel in the main hand: the main hand is never hidden, but the Seal itself cannot be used
        assertEquals("noon_cleaver", BcHands.legacyOther(false, "noon_cleaver", "seal", "EMPTY", true, false));
        assertFalse(BcHands.offHandUsable(true, false), "a two-handed weapon in the main hand: a use of the off hand never reaches the server");
        assertFalse(BcHands.offHandUsable(false, true));
        assertTrue(BcHands.offHandUsable(false, false));
    }

    @Test
    void theLegacyReadRefusedEveryTwoHandedVesselAndTheRawReadAcceptsIt() {
        for (String vessel : new String[]{"nusmp:noon_cleaver", "nusmp:staff_of_the_magi"}) {
            Stack real = Stack.of(vessel, comps(BindGesture.COMPONENT_ATTUNED, ME));
            Stack seen = BcHands.legacyOther(true, Stack.of("nusmp:djinn_seal_baal", null), real, Stack.of("minecraft:air", null), false, true);
            assertEquals(Why.EMPTY, BindGesture.judge(BindGesture.look(seen, CATALOG, ME)), "the old ritual: " + vessel + " not found -> a vague refusal");
            Stack raw = BcHands.rawOther(true, Stack.of("nusmp:djinn_seal_baal", null), real);
            assertEquals(Why.OK, BindGesture.judge(BindGesture.look(raw, CATALOG, ME)), "the new ritual finds " + vessel);
        }
    }

    // ---------------------------------------------------------------- extra components change nothing
    @Test
    void extraComponentsOnTheVesselAreIgnored() {
        Map<String, String> plain = comps(BindGesture.COMPONENT_ATTUNED, ME);
        Look base = look("nusmp:vorpal_sword", plain);
        assertEquals(Why.OK, BindGesture.judge(base));
        Map<String, String> noisy = new HashMap<>(plain);
        noisy.put("bettercombat:preset_id", "bettercombat:claymore");
        noisy.put("minecraft:attribute_modifiers", "{modifiers:[{type:'minecraft:generic.attack_damage',amount:9.0}]}");
        noisy.put("minecraft:enchantments", "{'tensura:severance':2}");
        noisy.put("minecraft:custom_name", "Warden's Edge");
        noisy.put("othermod:tier", "17");
        noisy.put("nusmp:relic_tier", "3");
        noisy.put("minecraft:damage", "37");
        assertEquals(base, look("nusmp:vorpal_sword", noisy), "the judgement reads the item and two components only");
        for (int i = 0; i < 200; i++) noisy.put("junk:c" + i, "v" + i);
        assertEquals(Why.OK, BindGesture.judge(look("nusmp:vorpal_sword", noisy)));
    }

    @Test
    void theRitualsOwnComponentsStillDecide() {
        assertEquals(Why.NOT_ATTUNED, BindGesture.judge(look("nusmp:vorpal_sword", comps())));
        assertEquals(Why.NOT_ATTUNED, BindGesture.judge(look("nusmp:vorpal_sword", comps(BindGesture.COMPONENT_ATTUNED, OTHER))), "attuned to someone else");
        assertEquals(Why.OK, BindGesture.judge(look("nusmp:vorpal_sword", comps(BindGesture.COMPONENT_ATTUNED, ME, "bettercombat:preset_id", "x"))));
        assertEquals(Why.ALREADY_BOUND, BindGesture.judge(look("nusmp:vorpal_sword", comps(BindGesture.COMPONENT_ATTUNED, ME, BindGesture.COMPONENT_BOUND, "baal"))));
        assertEquals(Why.OK, BindGesture.judge(look("nusmp:ring_free", comps())), "a vessel that needs no attunement");
    }

    // ---------------------------------------------------------------- the reasons
    @Test
    void everyRefusalHasItsOwnReason() {
        assertEquals(Why.EMPTY, BindGesture.judge(Look.NONE));
        assertEquals(Why.EMPTY, BindGesture.judge(null));
        assertEquals(Why.EMPTY, BindGesture.judge(look("minecraft:air", comps())));
        assertEquals(Why.NOT_A_RELIC, BindGesture.judge(look("minecraft:iron_sword", comps())));
        assertEquals(Why.NOT_A_VESSEL, BindGesture.judge(look("nusmp:legend_toy", comps(BindGesture.COMPONENT_ATTUNED, ME))));
        assertEquals(Why.NOT_LEGEND, BindGesture.judge(look("nusmp:rare_blade", comps(BindGesture.COMPONENT_ATTUNED, ME))));
        assertEquals(Why.NOT_ATTUNED, BindGesture.judge(look("nusmp:noon_cleaver", comps())));
        assertEquals(Why.ALREADY_BOUND, BindGesture.judge(look("nusmp:noon_cleaver", comps(BindGesture.COMPONENT_BOUND, "amon", BindGesture.COMPONENT_ATTUNED, ME))));
    }

    @Test
    void everyReasonHasAKeyAndATextAndTheTextsAreDistinct() {
        java.util.Set<String> texts = new java.util.HashSet<>();
        for (Why w : Why.values()) {
            assertEquals("message.nusmp.djinn_bind." + w.name().toLowerCase(java.util.Locale.ROOT), w.key());
            assertFalse(w.fallback().isBlank(), w.name());
            assertTrue(texts.add(w.fallback()), "two reasons share a text: " + w);
            if (w != Why.OK && w != Why.GESTURE_DISABLED && w != Why.NO_SEAL && w != Why.EMPTY)
                assertTrue(w.fallback().contains("%s"), w + " names the item");
        }
        assertTrue(Why.EMPTY.fallback().contains("/multiverse djinn bind"), "the empty-hand text points to the command");
        assertEquals(Why.OK, Why.values()[0], "OK stays first and the enum is only ever appended");
    }

    // ---------------------------------------------------------------- the gestures
    @Test
    void everyGestureAcceptsALegalVesselWithExtraComponents() {
        Look ok = look("nusmp:noon_cleaver", comps(BindGesture.COMPONENT_ATTUNED, ME, "bettercombat:preset_id", "bettercombat:double_axe"));
        for (Gesture g : Gesture.values()) assertEquals(Why.OK, BindGesture.decide(g, true, ok, true, true), g.name());
    }

    @Test
    void noSealNoRitualWhateverTheGesture() {
        Look ok = look("nusmp:vorpal_sword", comps(BindGesture.COMPONENT_ATTUNED, ME));
        for (Gesture g : Gesture.values()) assertEquals(Why.NO_SEAL, BindGesture.decide(g, false, ok, true, true), g.name());
    }

    @Test
    void theSwitchesTurnOffOnlyTheFallbackGestures() {
        Look ok = look("nusmp:vorpal_sword", comps(BindGesture.COMPONENT_ATTUNED, ME));
        assertEquals(Why.OK, BindGesture.decide(Gesture.USE_SEAL_MAIN, true, ok, false, false), "right-clicking the Seal never depends on a switch");
        assertEquals(Why.OK, BindGesture.decide(Gesture.USE_SEAL_OFF, true, ok, false, false));
        assertEquals(Why.GESTURE_DISABLED, BindGesture.decide(Gesture.COMMAND, true, ok, false, true));
        assertEquals(Why.GESTURE_DISABLED, BindGesture.decide(Gesture.KEY, true, ok, false, true));
        assertEquals(Why.OK, BindGesture.decide(Gesture.GUI_STACK, true, ok, false, true));
        assertEquals(Why.GESTURE_DISABLED, BindGesture.decide(Gesture.GUI_STACK, true, ok, true, false));
        assertEquals(Why.OK, BindGesture.decide(Gesture.COMMAND, true, ok, true, false));
        // a disabled gesture is told first, before anything about the vessel
        assertEquals(Why.GESTURE_DISABLED, BindGesture.decide(Gesture.COMMAND, true, Look.NONE, false, true));
    }

    @Test
    void aRefusedGestureNamesTheProblemNotAGenericNoVessel() {
        assertEquals(Why.NOT_LEGEND, BindGesture.decide(Gesture.GUI_STACK, true, look("nusmp:rare_blade", comps()), true, true));
        assertEquals(Why.NOT_A_RELIC, BindGesture.decide(Gesture.COMMAND, true, look("minecraft:diamond_sword", comps()), true, true));
        assertEquals(Why.EMPTY, BindGesture.decide(Gesture.USE_SEAL_MAIN, true, Look.NONE, true, true));
    }

    @Test
    void theGestureEnumIsOnlyAppended() {
        Gesture[] g = Gesture.values();
        assertEquals(Gesture.USE_SEAL_MAIN, g[0]);
        assertEquals(Gesture.USE_SEAL_OFF, g[1]);
        assertEquals(5, g.length);
    }
}
