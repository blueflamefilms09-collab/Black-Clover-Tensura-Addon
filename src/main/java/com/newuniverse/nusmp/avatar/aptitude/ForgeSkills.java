package com.newuniverse.nusmp.avatar.aptitude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: one list of the skills the Soul Forge offers, filled from {@code data/<namespace>/avatar_skills/*.json} by {@link ForgeSkillLoader}, the sibling of
 * {@link AptitudeCatalog}: replaced whole on every reload (a reload never leaves a half-filled list), read lock-free on an immutable snapshot, first id wins.
 * This is the DATA list; which entries actually exist in the running game (and in which slot) is decided on the server by
 * {@code ritual.ForgeSkillResolver}, because only the game can look into the skill registry.
 */
public final class ForgeSkills {
    private ForgeSkills() {}

    private static volatile Map<String, ForgeSkill> byId = Map.of();

    public static void replaceAll(List<ForgeSkill> list) {
        Map<String, ForgeSkill> m = new LinkedHashMap<>();
        if (list != null) for (ForgeSkill s : list) if (s != null) m.putIfAbsent(s.id(), s);
        byId = Collections.unmodifiableMap(m);
    }

    public static List<ForgeSkill> all() { return new ArrayList<>(byId.values()); }

    public static ForgeSkill byId(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }

    public static List<ForgeSkill> ofKind(ForgeSkill.Kind kind) {
        List<ForgeSkill> out = new ArrayList<>();
        for (ForgeSkill s : byId.values()) if (s.kind() == kind) out.add(s);
        return out;
    }

    public static int size() { return byId.size(); }
}
