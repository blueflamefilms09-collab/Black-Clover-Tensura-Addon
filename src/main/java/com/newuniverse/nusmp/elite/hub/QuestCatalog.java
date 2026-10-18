package com.newuniverse.nusmp.elite.hub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the loaded quests as one immutable value, so the reload listener swaps the whole catalog in a single assignment (a reload in progress never exposes half of it) and every reader asks the
 * same small API: by id, the dailies, the progression chain in file order. A later quest with the same id replaces an earlier one (a datapack overrides a shipped row), keeping the earlier
 * position. Pure.
 */
public final class QuestCatalog {
    public static final QuestCatalog EMPTY = new QuestCatalog(List.of());

    private final Map<String, QuestDef> byId = new LinkedHashMap<>();

    public QuestCatalog(List<QuestDef> quests) {
        for (QuestDef q : quests) byId.put(q.id(), q);
    }

    public QuestDef get(String id) { return id == null ? null : byId.get(id); }

    public int size() { return byId.size(); }

    public boolean isEmpty() { return byId.isEmpty(); }

    /** Every quest, in load order. */
    public List<QuestDef> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    public List<QuestDef> dailies() { return ofType(QuestType.DAILY); }

    public List<QuestDef> progression() { return ofType(QuestType.PROGRESSION); }

    private List<QuestDef> ofType(QuestType t) {
        List<QuestDef> out = new ArrayList<>();
        for (QuestDef q : byId.values()) if (q.type() == t) out.add(q);
        return out;
    }
}
