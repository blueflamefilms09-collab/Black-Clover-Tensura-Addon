package com.newuniverse.nusmp.elite.records.rule;

import java.util.Map;
import java.util.Set;

/**
 * Why: turns the two inputs of a beat, the saved {@link RecordData} and the {@link Live} readings, into the {@link Facts} the evaluators read, and folds the live readings back into the saved data
 * first ({@link #absorb}). The folding is the rule that makes records permanent in practice: EP, claims, statuses, distances and deaths are kept as the highest value ever seen, skills as the set
 * ever learned, so losing EP, releasing a claim or forgetting a skill cannot make a condition false again. Sources that never answered are marked unavailable. The per-subsystem counters
 * (kills, war wins ...) are the mod's own and are read as they are. Pure; the keys are listed in {@link ConditionEvaluator}.
 */
public final class FactsBuilder {
    private FactsBuilder() {}

    /** Folds the live readings into the saved data (bests only rise; the found-set of skills only grows). Returns true when anything changed. */
    public static boolean absorb(RecordData d, Live live) {
        boolean changed = false;
        if (live.tensuraOk) {
            changed |= d.bestMax("src_tensura", 1);
            changed |= d.bestMax("ep", live.ep);
            changed |= d.bestMax("demon_status", live.demon);
            changed |= d.bestMax("hero_status", live.hero);
            changed |= d.bestMax("higher_being", live.higher);
        }
        if (live.ftbChunks != null) {
            changed |= d.bestMax("src_ftbchunks", 1);
            changed |= d.bestMax("ftb_chunks", live.ftbChunks);
        }
        changed |= d.bestMax("deaths_vanilla", live.deaths);
        for (Map.Entry<String, Double> e : live.distance.entrySet()) changed |= d.bestMax("distance_" + e.getKey(), e.getValue());
        changed |= d.bestMax("hook_forge_count", live.hookForgeCount);
        changed |= d.bestMax("hook_forge_best", live.hookForgeBest);
        if (live.skills != null) {
            changed |= d.bestMax("src_skills", 1);
            for (String s : live.skills) changed |= d.setAdd("skills", s);
            for (Map.Entry<String, Set<String>> e : live.skillsByType.entrySet()) {
                for (String s : e.getValue()) changed |= d.setAdd("skills:" + e.getKey(), s);
            }
        }
        return changed;
    }

    /** The facts of the saved data (call {@link #absorb} first on a beat). */
    public static Facts build(RecordData d) {
        Facts f = new Facts();
        f.num("deaths", Math.max(d.best("deaths_vanilla"), d.counter("deaths")));
        for (String g : ConditionEvaluator.KILL_GROUPS) f.num("kills_" + g, d.counter("kills_" + g));
        for (String m : ConditionEvaluator.DISTANCE_MODES) f.num("distance_" + m, d.best("distance_" + m));
        f.num("ep", d.best("ep"));
        f.num("single_hit", d.best("single_hit"));
        f.num("forge_count", Math.max(d.counter("forge_count"), d.best("hook_forge_count")));
        f.num("forge_best_quality", Math.max(d.best("forge_quality"), d.best("hook_forge_best")));
        f.num("ftb_chunks", d.best("ftb_chunks"));
        f.num("calamity_damage", d.counter("calamity_damage"));
        f.num("calamity_kills", d.counter("calamity_kills"));
        f.num("walpurgis", d.counter("walpurgis"));
        f.num("war_kills", d.counter("war_kills"));
        f.num("war_wins", d.counter("war_wins"));
        f.num("demon_status", d.best("demon_status"));
        f.num("hero_status", d.best("hero_status"));
        f.num("higher_being", d.best("higher_being"));
        for (String m : new String[]{"kills", "calamity_kills", "forge_quality"}) {
            for (Map.Entry<String, Integer> e : d.map(m).entrySet()) f.count(m, e.getKey(), e.getValue());
        }
        for (String s : new String[]{"biomes", "structures", "skills"}) f.set(s, d.set(s));
        for (String t : ConditionEvaluator.SKILL_TYPES) f.set("skills:" + t, d.set("skills:" + t));
        if (d.best("src_tensura") <= 0) f.markUnavailable("tensura");
        if (d.best("src_ftbchunks") <= 0) f.markUnavailable("ftbchunks");
        if (d.best("src_skills") <= 0) f.markUnavailable("skills");
        return f;
    }

    /** Absorb then build. */
    public static Facts build(RecordData d, Live live) {
        absorb(d, live);
        return build(d);
    }
}
