package com.newuniverse.nusmp.skilltree.rules;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: a plain in-memory {@link TreeState}, so the rules can be run on a player-shaped object by the unit tests (no NBT, no game) and by the in-game self-test as a fast pre-check before the
 * real attachment is used. It stores the node levels under the same {@code <route>#<node>} key as the real tree ({@link SkillRoute#key}), so a test that fills it and a player's saved tree
 * look alike. Not thread-safe: it is only ever used by one test at a time.
 */
public final class SimpleTreeState implements TreeState {
    private final Set<String> routes = new LinkedHashSet<>();
    private final Map<String, Integer> levels = new LinkedHashMap<>();
    private int sp, spSpent;
    private String activeClass = "";
    private long classChangedAt;

    // ---- builder-style helpers for tests
    public SimpleTreeState unlock(String routeId) {
        routes.add(routeId);
        return this;
    }

    public SimpleTreeState withSp(int points) {
        sp = Math.max(0, points);
        return this;
    }

    public SimpleTreeState withClass(String classId, long changedAt) {
        activeClass = classId == null ? "" : classId;
        classChangedAt = changedAt;
        return this;
    }

    /** Every bought node as {@code <route>#<node>} to level (a copy). */
    public Map<String, Integer> levels() { return Collections.unmodifiableMap(new LinkedHashMap<>(levels)); }

    // ---- TreeState
    @Override public Collection<String> routes() { return Collections.unmodifiableSet(routes); }

    @Override public boolean hasRoute(String routeId) { return routes.contains(routeId); }

    @Override public int level(String routeId, String nodeId) { return levels.getOrDefault(SkillRoute.key(routeId, nodeId), 0); }

    @Override
    public void setLevel(String routeId, String nodeId, int level) {
        String key = SkillRoute.key(routeId, nodeId);
        if (level <= 0) levels.remove(key);
        else levels.put(key, level);
    }

    @Override
    public int clearRoute(String routeId) {
        String prefix = routeId + "#";
        int before = levels.size();
        levels.keySet().removeIf(k -> k.startsWith(prefix));
        return before - levels.size();
    }

    @Override public int sp() { return sp; }

    @Override public void setSp(int points) { sp = Math.max(0, points); }

    @Override public int spSpent() { return spSpent; }

    @Override public void setSpSpent(int spent) { spSpent = Math.max(0, spent); }

    @Override public String activeClass() { return activeClass; }

    @Override public long classChangedAt() { return classChangedAt; }
}
