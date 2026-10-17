package com.newuniverse.nusmp.arc.story;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

/**
 * Why: what one player has done in the story of the arcs, as plain data with no game types: the characters they have defeated, the NPCs they have met,
 * how far each NPC's quest has gone, and the saga they are in. It is the value stored in the player attachment ({@link StoryAttachments}), read by the
 * unlock rules ({@link StoryRules}) and by the dialogue selector, and being pure it can be tested and round-tripped through its serialized form without a
 * running game. Ids are lower-cased so a character is the same character however a data file or a command spells it. Insertion order is kept (the order
 * the player met and beat things in is shown by the story command).
 */
public final class StoryData {
    /** The quest stages (see {@code TalkPlan}); stored as numbers so a quest system can add its own stages above them. */
    public static final int QUEST_MAX = 9;

    private final Set<String> defeated = new LinkedHashSet<>();
    private final Set<String> met = new LinkedHashSet<>();
    private final Map<String, Integer> quest = new LinkedHashMap<>();
    private String saga = "";

    public StoryData() {}

    /** Rebuilds the data from its stored parts (nulls are empty, ids are normalised, a quest stage is kept in range). */
    public static StoryData of(Collection<String> defeated, Collection<String> met, Map<String, Integer> quest, String saga) {
        StoryData d = new StoryData();
        if (defeated != null) for (String s : defeated) d.defeat(s);
        if (met != null) for (String s : met) d.meet(s);
        if (quest != null) for (Map.Entry<String, Integer> e : quest.entrySet()) if (e.getValue() != null) d.setQuestStage(e.getKey(), e.getValue());
        d.saga = saga == null ? "" : saga.toLowerCase(Locale.ROOT);
        return d;
    }

    private static String norm(String id) { return id == null ? "" : id.trim().toLowerCase(Locale.ROOT); }

    /** Marks a character defeated. True only the first time. */
    public boolean defeat(String id) {
        String n = norm(id);
        return !n.isEmpty() && defeated.add(n);
    }

    public boolean hasDefeated(String id) { return defeated.contains(norm(id)); }

    /** The defeated ids in the order they fell (a copy). */
    public List<String> defeatedList() { return new ArrayList<>(defeated); }

    public Set<String> defeatedSet() { return Collections.unmodifiableSet(defeated); }

    /** Marks an NPC as met. True only the first time. */
    public boolean meet(String id) {
        String n = norm(id);
        return !n.isEmpty() && met.add(n);
    }

    public boolean hasMet(String id) { return met.contains(norm(id)); }

    public List<String> metList() { return new ArrayList<>(met); }

    /** The quest stage with this NPC (0 when none). */
    public int questStage(String npc) { return quest.getOrDefault(norm(npc), 0); }

    /** Sets a quest stage (clamped to 0..{@value #QUEST_MAX}; 0 forgets the entry). True when the stored value changed. */
    public boolean setQuestStage(String npc, int stage) {
        String n = norm(npc);
        if (n.isEmpty()) return false;
        int s = Math.max(0, Math.min(QUEST_MAX, stage));
        int old = quest.getOrDefault(n, 0);
        if (old == s) return false;
        if (s == 0) quest.remove(n);
        else quest.put(n, s);
        return true;
    }

    public Map<String, Integer> questMap() { return Collections.unmodifiableMap(quest); }

    /** The id of the saga the player is in, or empty when it was never worked out. */
    public String saga() { return saga; }

    public void setSaga(String id) { saga = id == null ? "" : id.toLowerCase(Locale.ROOT); }

    public boolean isEmpty() { return defeated.isEmpty() && met.isEmpty() && quest.isEmpty() && saga.isEmpty(); }

    /** Forgets everything. */
    public void clear() {
        defeated.clear();
        met.clear();
        quest.clear();
        saga = "";
    }

    public StoryData copy() { return of(defeated, met, quest, saga); }

    @Override public boolean equals(Object o) {
        return o instanceof StoryData d && defeated.equals(d.defeated) && met.equals(d.met) && quest.equals(d.quest) && saga.equals(d.saga);
    }

    @Override public int hashCode() { return Objects.hash(defeated, met, quest, saga); }

    @Override public String toString() { return "StoryData{defeated=" + defeated + ", met=" + met + ", quest=" + quest + ", saga=" + saga + "}"; }
}
