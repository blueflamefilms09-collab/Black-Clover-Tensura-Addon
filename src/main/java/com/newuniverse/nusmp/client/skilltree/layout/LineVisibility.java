package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Why: the owner's visibility rule is "the library shows ONLY the lines the player owns; an operator or a creative player may turn on an inspect mode that lists every line". The server already keeps the
 * trees of lines a player does not own off that player's client, but a single-player world runs the server in the same game, where the client can see the server's whole registry, so the SCREEN has to
 * enforce the rule too, and the rule has to be exactly the same code in both cases. This is that rule as a pure function over the route list and the player's owned set (no Minecraft type, so it is unit
 * tested): a line is visible when the player owns it, or when inspect mode is on. It also answers whether a route record is a real tree or only the node-less stub that stands for a line the player has
 * not got (the stubs let a book be named and tinted; they are never shown as a tree). The functions allocate only the result list, and the screen calls them through a cache that notices when the inputs
 * changed ({@link #ownedSignature}), so a library of 283 lines is not re-filtered and re-grouped every frame.
 */
public final class LineVisibility {
    private LineVisibility() {}

    /** The lines to list: the ones the player owns, plus every other line when {@code inspect} is on. Order is kept. */
    public static List<SkillRoute> visible(List<SkillRoute> all, TreeState player, boolean inspect) {
        if (inspect) return all;
        List<SkillRoute> out = new ArrayList<>();
        for (SkillRoute r : all) if (player.hasRoute(r.id())) out.add(r);
        return out;
    }

    /** True when the record carries the tree (it has nodes); false for the stub of a line the client only knows by its card. */
    public static boolean isFullTree(SkillRoute r) { return r != null && !r.nodes().isEmpty(); }

    /** Whether the player may SEE this line as a tree: owned, or inspect mode (then it is read-only unless owned). */
    public static boolean canOpen(SkillRoute r, TreeState player, boolean inspect) { return r != null && (inspect || player.hasRoute(r.id())); }

    /** An order-independent signature of the owned set, so a cache can tell that the owned lines changed without comparing sets. */
    public static int ownedSignature(Collection<String> owned) {
        int sum = 0, xor = 0;
        for (String id : owned) {
            int h = id.hashCode() * 0x9E3779B1;
            sum += h;
            xor ^= Integer.rotateLeft(h, 13);
        }
        return sum * 31 + xor + owned.size() * 0x85EBCA6B;
    }
}
