package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.line.LineGate;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: arts of a generated line are not Tensura skills, so they have no skill key and no place in Tensura's skill menu; the player uses them from an ARTS BAR that lists the unlocked arts of the line they have
 * equipped and lets them cycle with one key and use the chosen art with another. The logic of that bar is small but easy to get subtly wrong (what is in the bar, in which order, what stays selected when a
 * purchase adds an art or a class change empties the bar, how the strip scrolls so the chosen art is always visible), so it is a pure model with no Minecraft type and a unit test. The list is exactly what the
 * server's cast gate will accept (the same {@link LineGate#activeLevel}), so the bar never offers an art the server would refuse; the server still decides every cast. The selection is remembered by the
 * art's key ({@code route#node}), not by position, so buying a new art or an update that reorders the bar does not move the player's choice.
 */
public final class ArtsBarModel {
    /** One art in the bar. */
    public record Entry(String routeId, String nodeId, String name, String icon, boolean stance) {
        public String key() { return SkillRoute.key(routeId, nodeId); }
    }

    private List<Entry> entries = List.of();
    private String selectedKey = "";

    /** The arts the player may use now: the bought active nodes of the equipped line, in the line's own order. Empty when no line is equipped. */
    public static List<Entry> collect(SkillRoute equipped, TreeState player) {
        List<Entry> out = new ArrayList<>();
        if (equipped == null) return out;
        for (SkillNode n : equipped.nodes()) {
            if (LineGate.activeLevel(player, equipped, n) < 1) continue;
            out.add(new Entry(equipped.id(), n.id(), n.name(), n.icon().isEmpty() ? equipped.icon() : n.icon(), "stance".equals(n.kind())));
        }
        return out;
    }

    /** Replaces the list; the selection stays on the same art when it is still there, otherwise the first. */
    public void set(List<Entry> next) {
        entries = next;
        boolean found = false;
        for (Entry e : next) if (e.key().equals(selectedKey)) found = true;
        if (!found) selectedKey = next.isEmpty() ? "" : next.get(0).key();
    }

    public List<Entry> entries() { return entries; }

    public boolean isEmpty() { return entries.isEmpty(); }

    public int selectedIndex() {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).key().equals(selectedKey)) return i;
        return -1;
    }

    public Entry selected() {
        int i = selectedIndex();
        return i < 0 ? null : entries.get(i);
    }

    /** Moves the selection by {@code delta} places, wrapping around. */
    public void cycle(int delta) {
        if (entries.isEmpty()) return;
        int i = Math.max(0, selectedIndex());
        int n = entries.size();
        selectedKey = entries.get((((i + delta) % n) + n) % n).key();
    }

    /** Selects the art with this key, if it is in the bar. */
    public boolean select(String key) {
        for (Entry e : entries) {
            if (e.key().equals(key)) {
                selectedKey = key;
                return true;
            }
        }
        return false;
    }

    /**
     * The index of the first cell to draw in a strip of {@code visible} cells so that the selected art is inside it and, when there is room, in the middle. 0 when everything fits.
     */
    public int windowStart(int visible) {
        int n = entries.size();
        if (n <= visible || visible <= 0) return 0;
        int start = Math.max(0, selectedIndex()) - visible / 2;
        return Math.max(0, Math.min(n - visible, start));
    }
}
