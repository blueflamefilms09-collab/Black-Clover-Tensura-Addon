package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.skill.codex.AnimeSkill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One scored action list for one target: every legal codex skill, best first. The top entry is the plan; it is kept for at most replanTicks. */
public final class KillPlan {
    public record Entry(AnimeSkill skill, double score) {}

    public final List<Entry> entries = new ArrayList<>();

    public Entry best() { return entries.isEmpty() ? null : entries.get(0); }

    /**
     * Score = tier weight + 15 per matching target trait ("counters") - 45 per resisted_by match + range fit (out of range is illegal) -
     * a little for cast time against a fast mover. Lethal skills x1.5 in the emotional high. Unaffordable or cooling-down skills are left out.
     */
    public static KillPlan build(RivenBossEntity boss, ThreatScan scan, int phase, java.util.function.Predicate<AnimeSkill> legal, net.minecraft.util.RandomSource rng) {
        KillPlan plan = new KillPlan();
        for (AnimeSkill s : com.newuniverse.nusmp.skill.codex.AnimeSkillCodex.all()) {
            if (!legal.test(s)) continue;
            if (s.has("copy_codex_skill") && phase < 2) continue;
            if (scan.distance > s.range() && s.range() > 0) continue;                         // out of reach
            if (s.range() == 0 && !s.has("heal") && !s.has("song_buff") && !s.has("summon_construct")) continue;
            double score = 10 * s.tier();
            for (String c : s.counters()) if (scan.traits.contains(c)) score += 15;
            for (String r : s.resistedBy()) if (scan.resists.contains(r)) score -= 45;
            if (s.range() > 0 && scan.distance <= 4 && s.range() > 10 && !s.has("blink")) score -= 6;   // a bard doesn't want to cast point-blank
            if (s.range() > 0 && s.range() <= 6 && scan.distance > 6) score -= 20;
            if (scan.traits.contains("kiter") && s.castTicks() > 16 && !s.has("song_debuff")) score -= 8;
            if (s.has("heal") || s.has("song_buff")) score += boss.getHealth() < boss.getMaxHealth() * 0.7f ? 20 : -15;
            if (s.has("shield") || s.has("summon_construct")) score += scan.traits.contains("rusher") || scan.traits.contains("melee") ? 12 : -10;
            if (phase >= 3 && s.lethal()) score *= 1.5;
            if (boss.passives().forcedCounter != null && s.has(boss.passives().forcedCounter)) score += 40;      // Counter Author: his next plan is the counter
            if (!boss.passives().canRewrite(scan) && s.has("silence")) score = 0;                             // Unbelieved: a plan that cannot land is worthless
            score += rng.nextDouble() * 6;                                                     // a bard improvises a little
            plan.entries.add(new Entry(s, score));
        }
        plan.entries.sort(Comparator.comparingDouble((Entry e) -> e.score).reversed());
        return plan;
    }
}
