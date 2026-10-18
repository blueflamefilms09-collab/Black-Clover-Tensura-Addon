package com.newuniverse.nusmp.heroweapon.skill;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the shared machinery has to find the logic of a weapon and the skill behind an id in one place. The registry is filled once at start by
 * {@code HeroBootstrap} (one line per weapon) and is read-only afterwards. A weapon without a registered logic gets an inert one, so a missing builder
 * never crashes the game (a data test reports it instead). Skill ids are global; a repeated id keeps the first and logs the second.
 */
public final class HeroLogics {
    private HeroLogics() {}

    private static final Map<HeroKind, HeroLogic> LOGICS = new EnumMap<>(HeroKind.class);
    private static final Map<String, HeroSkill> SKILLS = new LinkedHashMap<>();

    /** Registers a weapon's logic and its skills (once per weapon; the second registration of a weapon is ignored). */
    public static synchronized void register(HeroLogic logic) {
        if (logic == null || logic.kind() == null) return;
        if (LOGICS.putIfAbsent(logic.kind(), logic) != null) return;
        for (HeroSkill s : logic.skills()) {
            if (SKILLS.putIfAbsent(s.id(), s) != null) LogUtils.getLogger().warn("[nusmp] hero skill id {} registered twice; the first stays", s.id());
        }
    }

    /** The logic of a weapon; an inert one when none was registered. Never null. */
    public static synchronized HeroLogic of(HeroKind kind) {
        HeroLogic l = LOGICS.get(kind);
        return l != null ? l : new Inert(kind);
    }

    /** True when a real logic is registered for the weapon. */
    public static synchronized boolean has(HeroKind kind) { return LOGICS.containsKey(kind); }

    /** The skill with this global id, or null. */
    public static synchronized HeroSkill skill(String id) { return id == null ? null : SKILLS.get(id); }

    /** All registered skills, in registration order. */
    public static synchronized List<HeroSkill> allSkills() { return Collections.unmodifiableList(new ArrayList<>(SKILLS.values())); }

    /** The skills of one weapon, in the order its logic lists them. */
    public static synchronized List<HeroSkill> skillsOf(HeroKind kind) {
        HeroLogic l = LOGICS.get(kind);
        return l == null ? List.of() : List.copyOf(l.skills());
    }

    /** Test seam: forget everything. */
    public static synchronized void clear() {
        LOGICS.clear();
        SKILLS.clear();
    }

    private record Inert(HeroKind kind) implements HeroLogic {
        @Override
        public List<HeroSkill> skills() { return List.of(); }
    }
}
