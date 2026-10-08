package com.newuniverse.nusmp.entity.riven;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.skill.codex.AnimeSkillCodex;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Riven's utility AI. Every {@code replanTicks} (or at once when the target changes, drinks, raises a barrier or pops anti-magic)
 * it rescans the target ({@link ThreatScan}) and scores every legal skill ({@link KillPlan}); it then commits to the top plan for at
 * most four seconds. The plan that is chosen is logged at debug, so a player can lose for a readable reason.
 */
public final class RivenBrain {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int COMMIT_TICKS = 80;

    private final RivenBossEntity boss;
    private UUID lastTarget;
    private long planAt = Long.MIN_VALUE, committedUntil;
    private int lastTagHash;
    private ThreatScan scan;
    private KillPlan plan;
    private AnimeSkill committed;
    private final Map<String, Long> ready = new HashMap<>();
    private final Map<String, Long> resistedUntil = new HashMap<>();
    private final Deque<String> recentTaken = new ArrayDeque<>();

    public RivenBrain(RivenBossEntity boss) { this.boss = boss; }

    public ThreatScan scan() { return scan; }
    public KillPlan plan() { return plan; }
    public void invalidate() { planAt = Long.MIN_VALUE; committed = null; }

    public void cooldown(AnimeSkill s, long now) { ready.put(s.id(), now + s.cooldownTicks()); }

    /** The last 8 kinds of damage that hurt him in this fight (memory only; shown in the debug plan). */
    public void noteTaken(String kind) {
        recentTaken.addFirst(kind);
        while (recentTaken.size() > 8) recentTaken.removeLast();
    }

    /** A damage skill that did nothing: its kind is out of favour with this target for ten seconds. */
    public void bounced(AnimeSkill s, long now) {
        if (s.has("magic_damage")) resistedUntil.put("magic", now + 200);
        if (s.has("physical_damage")) resistedUntil.put("physical", now + 200);
    }

    /** Replans when due and returns the skill to use now, or null. */
    public AnimeSkill choose(LivingEntity target, long now) {
        boolean newTarget = lastTarget == null || !lastTarget.equals(target.getUUID());
        ThreatScan fresh = null;
        if (newTarget || now - planAt >= com.newuniverse.nusmp.entity.riven.RivenConfig.REPLAN_TICKS.get() || scan == null) {
            fresh = ThreatScan.of(boss, target);
            int hash = fresh.tags.hashCode();
            boolean changed = !newTarget && hash != lastTagHash;                    // new gear, race state, barrier or anti-magic: replan now
            if (changed) committed = null;
            lastTagHash = hash;
            scan = fresh;
            lastTarget = target.getUUID();
            planAt = now;
            plan = KillPlan.build(boss, scan, AnimeSkillCodex.all(), now, ready, resistedUntil);
            if (committed == null || now >= committedUntil || !plan.options.isEmpty() && newTarget) {
                KillPlan.Option best = plan.best();
                committed = best == null ? null : best.skill();
                committedUntil = now + COMMIT_TICKS;
                if (best != null && LOG.isDebugEnabled())
                    LOG.debug("[riven] plan vs {}: {} (score {} {}) | scan {} | recent taken {}", target.getName().getString(), best.skill().id(),
                            String.format("%.1f", best.score()), best.why(), scan, recentTaken);
            }
        }
        if (committed != null && ready.getOrDefault(committed.id(), 0L) > now) {   // used it: take the next best
            plan = KillPlan.build(boss, scan, AnimeSkillCodex.all(), now, ready, resistedUntil);
            KillPlan.Option best = plan.best();
            committed = best == null ? null : best.skill();
        }
        return committed;
    }

    /** Preferred distance for the movement goal, from the committed skill. */
    public double preferredDistance() {
        if (committed == null) return 8;
        if (committed.has("melee_arc") && !committed.has("projectile")) return 2.8;
        return Math.min(10, Math.max(6, committed.range() * 0.6));
    }

    public List<String> recent() { return List.copyOf(recentTaken); }
}
