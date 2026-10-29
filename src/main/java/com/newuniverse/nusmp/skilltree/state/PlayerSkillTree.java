package com.newuniverse.nusmp.skilltree.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the implementation of {@link IPlayerSkillTree}: plain collections plus an NBT form, so it is saved with the player, copied to the new body after death, and sent to the client
 * by the same code in the same shape. Every key of the NBT is optional (an old save, a save written by hand, a save from before a field existed must still load) and every read
 * clamps what it finds (a negative SP or level from a corrupted file becomes 0). Server thread for writes; the client holds a copy that the sync packet overwrites.
 */
public final class PlayerSkillTree implements IPlayerSkillTree {
    private final Set<String> routes = new LinkedHashSet<>();
    private final Map<String, Integer> nodes = new LinkedHashMap<>();
    private int sp, spSpent;
    private String activeClass = "";
    private long classChangedAt;
    private CompoundTag extra = new CompoundTag();
    /** 1.82.0: the on/off switches of the line passives (see {@link PassiveToggles}). */
    private final PassiveToggles toggles = new PassiveToggles();

    @Override public Set<String> unlockedRoutes() { return Collections.unmodifiableSet(routes); }

    @Override public boolean hasRoute(String routeId) { return routes.contains(routeId); }

    @Override public boolean addRoute(String routeId) { return routeId != null && !routeId.isEmpty() && routes.add(routeId); }

    @Override public boolean removeRoute(String routeId) { return routes.remove(routeId); }

    @Override public int sp() { return sp; }

    @Override public void setSp(int v) { sp = Math.max(0, v); }

    @Override public void addSp(int amount) { sp = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, (long) sp + amount)); }

    @Override public int spSpent() { return spSpent; }

    @Override public void setSpSpent(int v) { spSpent = Math.max(0, v); }

    @Override public int nodeLevel(String routeId, String nodeId) { return nodes.getOrDefault(routeId + "#" + nodeId, 0); }

    @Override
    public void setNodeLevel(String routeId, String nodeId, int level) {
        String key = routeId + "#" + nodeId;
        if (level <= 0) nodes.remove(key);
        else nodes.put(key, level);
    }

    @Override public Map<String, Integer> nodeLevels() { return Collections.unmodifiableMap(nodes); }

    @Override
    public int clearRoute(String routeId) {
        String prefix = routeId + "#";
        int before = nodes.size();
        nodes.keySet().removeIf(k -> k.startsWith(prefix));
        toggles.clearRoute(routeId);                                         // a respec starts the line's switches from their default again
        return before - nodes.size();
    }

    @Override public Map<String, Boolean> passiveToggles() { return toggles.asMap(); }

    @Override public boolean isPassiveEnabled(String routeId, String nodeId) { return toggles.isEnabled(routeId, nodeId); }

    @Override public void setPassiveEnabled(String routeId, String nodeId, boolean enabled) { toggles.set(routeId, nodeId, enabled); }

    @Override public String activeClass() { return activeClass; }

    @Override public void setActiveClass(String classId) { activeClass = classId == null ? "" : classId; }

    @Override public long classChangedAt() { return classChangedAt; }

    @Override public void setClassChangedAt(long gameTime) { classChangedAt = gameTime; }

    @Override
    public CompoundTag extra(String system) {
        if (!extra.contains(system, Tag.TAG_COMPOUND)) extra.put(system, new CompoundTag());
        return extra.getCompound(system);
    }

    @Override public boolean hasExtra(String system) { return extra.contains(system, Tag.TAG_COMPOUND); }

    @Override
    public boolean isEmpty() { return routes.isEmpty() && nodes.isEmpty() && sp == 0 && spSpent == 0 && activeClass.isEmpty() && extra.isEmpty() && toggles.isEmpty(); }

    @Override
    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        ListTag r = new ListTag();
        for (String id : routes) r.add(StringTag.valueOf(id));
        t.put("routes", r);
        t.putInt("sp", sp);
        t.putInt("spent", spSpent);
        CompoundTag n = new CompoundTag();
        for (Map.Entry<String, Integer> e : nodes.entrySet()) n.putInt(e.getKey(), e.getValue());
        t.put("nodes", n);
        t.putString("class", activeClass);
        t.putLong("classAt", classChangedAt);
        t.put("extra", extra.copy());
        if (!toggles.isEmpty()) {                                            // 1.82.0: written only when there is something, so an old save and a new save without lines look alike
            CompoundTag tg = new CompoundTag();
            for (Map.Entry<String, Boolean> e : toggles.asMap().entrySet()) tg.putBoolean(e.getKey(), e.getValue());
            t.put("toggles", tg);
        }
        return t;
    }

    public static PlayerSkillTree fromTag(CompoundTag t) {
        PlayerSkillTree p = new PlayerSkillTree();
        if (t == null) return p;
        if (t.contains("routes", Tag.TAG_LIST)) {
            ListTag r = t.getList("routes", Tag.TAG_STRING);
            for (int i = 0; i < r.size(); i++) if (!r.getString(i).isEmpty()) p.routes.add(r.getString(i));
        }
        p.sp = Math.max(0, t.getInt("sp"));
        p.spSpent = Math.max(0, t.getInt("spent"));
        if (t.contains("nodes", Tag.TAG_COMPOUND)) {
            CompoundTag n = t.getCompound("nodes");
            for (String k : n.getAllKeys()) if (n.getInt(k) > 0) p.nodes.put(k, n.getInt(k));
        }
        p.activeClass = t.getString("class");
        p.classChangedAt = t.getLong("classAt");
        if (t.contains("extra", Tag.TAG_COMPOUND)) p.extra = t.getCompound("extra").copy();
        if (t.contains("toggles", Tag.TAG_COMPOUND)) {                       // tolerant: a missing compound, a key that is not route#node and a value that is not a number are skipped
            CompoundTag tg = t.getCompound("toggles");
            Map<String, Object> raw = new LinkedHashMap<>();
            for (String k : tg.getAllKeys()) if (tg.contains(k, Tag.TAG_ANY_NUMERIC)) raw.put(k, tg.getByte(k));
            p.toggles.copyFrom(PassiveToggles.fromMap(raw));
        }
        return p;
    }

    /** Copies everything from another tree (the new body after death, a sync from the server). */
    public void copyFrom(IPlayerSkillTree o) {
        PlayerSkillTree c = fromTag(o.toTag());
        routes.clear();
        routes.addAll(c.routes);
        nodes.clear();
        nodes.putAll(c.nodes);
        sp = c.sp;
        spSpent = c.spSpent;
        activeClass = c.activeClass;
        classChangedAt = c.classChangedAt;
        extra = c.extra;
        toggles.copyFrom(c.toggles);
    }
}
