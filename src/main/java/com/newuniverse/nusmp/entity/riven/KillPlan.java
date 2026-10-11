package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.skill.codex.AnimeSkill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A scored list of the skills Riven could use on one target right now. Higher is better. A skill scores up from its tier and from
 * every {@code counters} tag the target carries, and down for every {@code resisted_by} tag, for a gap it can't cover, for a cast
 * slower than a speedster allows, and for damage kinds that have already bounced off this target in this fight.
 */
public final class KillPlan {
    public record Option(AnimeSkill skill, float score, String why) {}

    public final List<Option> options = new ArrayList<>();

    public Option best() { return options.isEmpty() ? null : options.get(0); }

    /** Best option that is not {@code not}, for the second skill of a phase 3 chain. */
    public Option next(AnimeSkill not) {
        for (Option o : options) if (o.skill() != not) return o;
        return null;
    }

    public static KillPlan build(RivenBossEntity boss, ThreatScan scan, Iterable<AnimeSkill> pool, long now, Map<String, Long> ready,
                                 Map<String, Long> resistedUntil) {
        KillPlan plan = new KillPlan();
        int phase = boss.phase();
        for (AnimeSkill s : pool) {
            if (s.tier() > phase + 1) continue;                                   // the story unlocks as it goes
            if (RivenCombat.firstPhase(s) > phase) continue;
            if (ready.getOrDefault(s.id(), 0L) > now) continue;                   // cooldown
            if (s.tier() >= 3 && boss.story() < RivenBossEntity.STORY_COST) continue;   // can't afford it
            if (s.has("summon_construct") && !boss.canManifest()) continue;
            if (s.nativeId().equals("audience_collapse") && !boss.canStartAudienceCollapse()) continue;
            if (s.nativeId().equals("rewrite_round") && !boss.canRewriteRound()) continue;
            if (s.nativeId().equals("final_page") && !boss.finalPageReady()) continue;
            if (s.has("heal") && boss.getHealth() > boss.getMaxHealth() * 0.9f && s.primitives().size() == 1) continue;
            StringBuilder why = new StringBuilder();
            float score = 1f + s.tier() * 0.6f;
            for (String c : s.counters()) if (scan(scan, c)) { score += 2.2f; why.append("+").append(c).append(' '); }
            for (String r : s.resistedBy()) if (scan(scan, r)) { score -= 3.2f; why.append("-").append(r).append(' '); }
            if (s.has("magic_damage") && resisted(resistedUntil, "magic", now)) { score -= 2.5f; why.append("-magic_bounced "); }
            if (s.has("physical_damage") && (scan.has("physical_immune") || resisted(resistedUntil, "physical", now))) { score -= 3f; why.append("-physical_bounced "); }
            if (s.has("magic_damage") && (scan.has("magic_null") || scan.has("anti_magic"))) { score -= 2f; why.append("-anti_magic "); }
            float gap = scan.distance - s.range();
            boolean gapCloser = s.has("blink") || s.has("pull");
            if (gap > 0 && !gapCloser) { score -= Math.min(4f, gap * 0.4f); why.append("-out_of_range "); }
            else if (s.range() >= 8 && scan.distance <= 4f && !gapCloser) { score += scan.has("melee") ? 0.4f : 0f; }
            if (s.has("melee_arc") && scan.distance > 5f && !s.has("blink")) { score -= 3f; why.append("-too_far "); }
            if (s.has("melee_arc") && scan.distance <= 4f) { score += 1.2f; why.append("+close "); }
            if (scan.has("speedster") && s.castTicks() > 20 && !s.has("summon_construct")) { score -= 1.5f; why.append("-slow_cast "); }
            if (s.has("blink") && scan.distance < 5f && scan.has("kiter")) { score += 0.8f; }
            if (!scan.lineOfSight && (s.has("projectile") || s.has("pull"))) { score -= 2f; why.append("-no_los "); }
            if (s.has("heal") || s.has("shield")) score += (1f - boss.getHealth() / boss.getMaxHealth()) * 3f;
            if (s.has("song_buff") && boss.hasSongBuff()) score -= 3f;
            if (boss.emotionalHigh() && s.lethal()) { score *= 1.5f; why.append("x1.5(emotional) "); }
            score += boss.getRandom().nextFloat() * 0.6f;                         // never the same script twice
            plan.options.add(new Option(s, score, why.toString().trim()));
        }
        plan.options.sort(Comparator.comparingDouble((Option o) -> o.score()).reversed());
        return plan;
    }

    private static boolean scan(ThreatScan scan, String tag) { return scan.has(tag.toLowerCase(Locale.ROOT)); }

    private static boolean resisted(Map<String, Long> map, String kind, long now) { return map.getOrDefault(kind, 0L) > now; }
}
