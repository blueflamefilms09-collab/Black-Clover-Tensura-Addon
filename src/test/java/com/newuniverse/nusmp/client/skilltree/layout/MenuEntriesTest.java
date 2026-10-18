package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.menu.LineIndex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: this is the fog of war and the ADMIN INSPECT bypass as the rule the screen uses: a player sees exactly the lines they own; an operator or a creative player sees all 283 while the switch is on and only
 * then; nobody else can see more by forcing the switch. The diff must change the screen's list only when something changed, never add an entry twice and never remove a skill that is not a line's stand-in
 * (it is given ids only, so the rest of Tensura's list is out of its reach by construction).
 */
class MenuEntriesTest {
    private static List<String> all() {
        List<String> ids = new ArrayList<>();
        for (LineIndex.Line l : LineIndex.load()) ids.add(l.routeId());
        return ids;
    }

    @Test
    void aPlayerSeesExactlyTheLinesTheyOwn() {
        List<String> all = all();
        assertEquals(283, all.size());
        assertTrue(MenuEntries.wanted(all, Set.of(), false).isEmpty(), "a new player sees no line at all");
        List<String> own = List.of(all.get(5), all.get(100), "nusmp:not_a_line");
        List<String> shown = MenuEntries.wanted(all, own, false);
        assertEquals(List.of(all.get(5), all.get(100)), shown, "owned lines only, in the list's order, and an id without a stand-in is not listed");
    }

    @Test
    void theInspectBypassListsAllButOnlyForThoseWhoMayUseIt() {
        assertTrue(MenuEntries.mayInspect(true, false), "an operator");
        assertTrue(MenuEntries.mayInspect(false, true), "a creative player");
        assertFalse(MenuEntries.mayInspect(false, false), "everybody else");
        assertTrue(MenuEntries.inspecting(true, true));
        assertFalse(MenuEntries.inspecting(true, false), "may, but has not turned it on");
        assertFalse(MenuEntries.inspecting(false, true), "forcing the switch on without the permission lists nothing extra");
        List<String> all = all();
        assertEquals(all, MenuEntries.wanted(all, Set.of(), true), "all 283 while inspecting, owned or not");
        assertEquals(283, MenuEntries.wanted(all, List.of(all.get(0)), true).size());
    }

    @Test
    void theDiffAddsWhatIsMissingAndRemovesWhatMustNotBeSeen() {
        MenuEntries.Diff d = MenuEntries.diff(List.of("a", "x"), List.of("a", "b", "c"));
        assertEquals(List.of("b", "c"), d.add());
        assertEquals(List.of("x"), d.remove());
        assertTrue(MenuEntries.diff(List.of("a", "b"), List.of("a", "b")).isEmpty(), "nothing changed, the screen's list is not touched");
        assertTrue(MenuEntries.diff(List.of(), List.of()).isEmpty());
    }

    @Test
    void turningInspectOnAndOffMovesTheListThere283AndBack() {
        List<String> all = all();
        List<String> own = List.of(all.get(3));
        List<String> before = MenuEntries.wanted(all, own, false);
        MenuEntries.Diff on = MenuEntries.diff(before, MenuEntries.wanted(all, own, true));
        assertEquals(282, on.add().size());
        assertTrue(on.remove().isEmpty());
        MenuEntries.Diff off = MenuEntries.diff(MenuEntries.wanted(all, own, true), before);
        assertEquals(282, off.remove().size());
        assertFalse(off.remove().contains(all.get(3)), "the line the player owns stays when inspect goes off");
        assertTrue(off.add().isEmpty());
    }

    @Test
    void aDuplicateInThePresentListIsRemovedOnlyIfUnwantedAndNeverAddedTwice() {
        MenuEntries.Diff d = MenuEntries.diff(List.of("x", "x", "a"), List.of("a"));
        assertEquals(List.of("x"), d.remove());
        assertTrue(d.add().isEmpty());
    }
}
