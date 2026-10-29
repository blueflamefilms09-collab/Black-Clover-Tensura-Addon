package com.newuniverse.nusmp.skilltree.state;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the owner wants Tensura's way of handling passives: a skill you own can be switched OFF (and back ON) without losing it, from a list. With 275 lines of 6 to 12 passives each a player
 * may own hundreds, so the on/off state is plain data in the player's skill-tree record, a map {@code "<route>#<node>" -> boolean} with the SAME key the node levels use ({@code SkillRoute.key}), so a
 * toggle can never outlive or drift from its node. This class is that map and its rules, kept free of Minecraft types so they are unit-tested without a game (the NBT form is the tree's job and
 * uses {@link #asMap} / {@link #fromMap}):
 * <ul>
 * <li>a key that is not in the map means ON. The owner's rule is "default ON when bought", and treating "no entry" as ON is what makes a node bought before this feature existed, bought by an
 * administrator's command, or bought on another server version behave the same as one bought now, with no migration;</li>
 * <li>loading is tolerant: a save written by another version may hold keys that are not {@code route#node}, or values that are not booleans; they are skipped, never an error and never
 * a crash of the login;</li>
 * <li>a respec or taking a line away clears every toggle of that route, so a line bought again later starts from its default and the map cannot grow without bound.</li>
 * </ul>
 */
public final class PassiveToggles {
    private final Map<String, Boolean> map = new LinkedHashMap<>();

    /** The map key of a node: {@code <route>#<node>}, the same as the node-level key. */
    public static String key(String routeId, String nodeId) { return routeId + "#" + nodeId; }

    /** True when the key has the shape {@code <route>#<node>} with both parts non-empty (what a load keeps). */
    public static boolean validKey(String key) {
        if (key == null) return false;
        int i = key.indexOf('#');
        return i > 0 && i < key.length() - 1 && key.indexOf('#', i + 1) < 0;
    }

    /** Whether the node's switch is on: ON unless the player turned it off. */
    public boolean isEnabled(String routeId, String nodeId) { return map.getOrDefault(key(routeId, nodeId), Boolean.TRUE); }

    /** Sets the switch. Returns true when the stored state changed (an absent key counts as ON). */
    public boolean set(String routeId, String nodeId, boolean enabled) {
        String k = key(routeId, nodeId);
        boolean before = map.getOrDefault(k, Boolean.TRUE);
        map.put(k, enabled);
        return before != enabled;
    }

    /** Forgets the switch of one node (it is ON again). */
    public void remove(String routeId, String nodeId) { map.remove(key(routeId, nodeId)); }

    /** Forgets every switch of a route; returns how many were removed. */
    public int clearRoute(String routeId) {
        String prefix = routeId + "#";
        int before = map.size();
        map.keySet().removeIf(k -> k.startsWith(prefix));
        return before - map.size();
    }

    public boolean isEmpty() { return map.isEmpty(); }

    public int size() { return map.size(); }

    /** An unmodifiable view of the stored entries (key to state). */
    public Map<String, Boolean> asMap() { return Collections.unmodifiableMap(map); }

    /** Replaces the content with a copy of another set of toggles. */
    public void copyFrom(PassiveToggles other) {
        map.clear();
        map.putAll(other.map);
    }

    /**
     * Builds the toggles from loosely typed entries (what NBT gave): an entry whose key is not {@code route#node}, or whose value is not a {@link Boolean} or a {@link Number} (0 is off, anything
     * else is on), is skipped.
     */
    public static PassiveToggles fromMap(Map<String, ?> raw) {
        PassiveToggles t = new PassiveToggles();
        if (raw == null) return t;
        for (Map.Entry<String, ?> e : raw.entrySet()) {
            if (!validKey(e.getKey())) continue;
            Object v = e.getValue();
            if (v instanceof Boolean b) t.map.put(e.getKey(), b);
            else if (v instanceof Number n) t.map.put(e.getKey(), n.intValue() != 0);
        }
        return t;
    }
}
