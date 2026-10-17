package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the roller, the commands and the self-tests all ask the same questions of the loaded quest data (all of it, one by id, the ones of a kind), and a data
 * reload must swap the whole set in one assignment so a reload in progress never shows a half-filled catalog. This is that immutable set: built once by the
 * parser from the shipped file and any datapack file, later entries of the same id replacing earlier ones, and read through {@link SystemData}. Pure.
 */
public final class QuestCatalog {
    public static final QuestCatalog EMPTY = new QuestCatalog(List.of());

    private final List<QuestTemplate> templates;
    private final Map<String, QuestTemplate> byId = new LinkedHashMap<>();

    public QuestCatalog(List<QuestTemplate> list) {
        for (QuestTemplate t : list) byId.put(t.id(), t);        // a later template of the same id replaces the earlier one (a datapack override)
        this.templates = List.copyOf(byId.values());
    }

    /** Every template, in load order, a replaced one in the place of the first. */
    public List<QuestTemplate> all() { return templates; }

    public int size() { return templates.size(); }

    public boolean isEmpty() { return templates.isEmpty(); }

    /** The template with this id (the namespace, if any, is ignored), or null. */
    public QuestTemplate get(String id) {
        if (id == null) return null;
        int c = id.indexOf(':');
        return byId.get(c >= 0 ? id.substring(c + 1) : id);
    }

    public List<QuestTemplate> ofKind(QuestKind kind) {
        List<QuestTemplate> out = new ArrayList<>();
        for (QuestTemplate t : templates) if (t.kind() == kind) out.add(t);
        return out;
    }

    /** True when at least one template of this kind exists. */
    public boolean hasKind(QuestKind kind) {
        for (QuestTemplate t : templates) if (t.kind() == kind) return true;
        return false;
    }
}
