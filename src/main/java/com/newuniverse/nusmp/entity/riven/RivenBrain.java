package com.newuniverse.nusmp.entity.riven;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility AI. Per target: {@link ThreatScan} (every 10 ticks, so a gapple, a barrier or anti-magic is noticed fast), {@link KillPlan} (rebuilt every
 * replanTicks or the moment the scan's resist/trait set changes), then one telegraphed cast at a time. Below 30% two skills may chain.
 * The plan that picked each cast is logged at debug level ("a player can lose for a readable reason").
 */
public final class RivenBrain {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private final RivenBossEntity boss;
    private LivingEntity target;
    private ThreatScan scan;
    private KillPlan plan;
    private AnimeSkill pending;
    private long castAt, replanAt, nextScan, nextAct = 40;
    private boolean opened;
    private int combo;
    private final Map<String, Long> ready = new HashMap<>();

    RivenBrain(RivenBossEntity boss) { this.boss = boss; }

    /** Forces a replan at once (anti-magic hit, barrier, target swap). */
    void abort() { pending = null; replanAt = 0; nextScan = 0; boss.setClip("", ""); }

    void tick(ServerLevel sl, long t) {
        LivingEntity tg = boss.getTarget();
        if (tg == null || !tg.isAlive()) { target = null; scan = null; plan = null; pending = null; opened = false; return; }
        if (tg != target) { target = tg; scan = null; plan = null; pending = null; replanAt = 0; nextScan = 0; }
        if (!opened) {                                                                       // a bard opens with a line and Bardic Inspiration on himself
            opened = true;
            boss.say(sl, "Chaos builds the best stories.");
            var inspire = com.newuniverse.nusmp.skill.codex.AnimeSkillCodex.get("nusmp:bardic_inspiration");
            if (inspire != null) {
                boss.setClip(inspire.animation(), inspire.name());
                scan = ThreatScan.of(boss, tg, null);
                RivenAttacks.execute(boss, sl, tg, inspire, scan, true);
            }
            nextAct = t + 40;
            return;
        }
        if (t < boss.busyUntil) { boss.getNavigation().stop(); return; }
        if (t >= nextScan) {
            nextScan = t + 10;
            ThreatScan fresh = ThreatScan.of(boss, tg, scan);
            boolean changed = scan != null && (!fresh.resists.equals(scan.resists) || fresh.barrierUp != scan.barrierUp || fresh.absorption > scan.absorption + 3f);
            scan = fresh;
            if (changed) { replanAt = 0; if (pending != null) { pending = null; boss.setClip("", ""); } }
        }
        if (scan == null) return;
        if (pending != null) {
            boss.getNavigation().stop();
            boss.getLookControl().setLookAt(tg, 60f, 60f);
            if (t >= castAt) {
                AnimeSkill s = pending;
                pending = null;
                RivenAttacks.execute(boss, sl, tg, s, scan, false);
                nextAct = t + (boss.phase() >= 3 ? 8 : 16);
            }
            return;
        }
        move(tg, t);
        if (plan == null || t >= replanAt) {
            plan = KillPlan.build(boss, scan, boss.phase(), s -> ready.getOrDefault(s.id(), 0L) <= t && boss.canAfford(cost(s)), boss.getRandom());
            replanAt = t + RivenConfig.REPLAN_TICKS.get();
            if (LOG.isDebugEnabled() && plan.best() != null) LOG.debug("[nusmp] Riven plan vs {}: {} (traits {}, resists {})", tg.getName().getString(),
                    plan.entries.stream().limit(3).map(e -> e.skill().id() + "=" + Math.round(e.score())).toList(), scan.traits, scan.resists);
        }
        if (t < nextAct) return;
        KillPlan.Entry best = null;
        for (KillPlan.Entry e : plan.entries) if (ready.getOrDefault(e.skill().id(), 0L) <= t && boss.canAfford(cost(e.skill())) && e.skill().range() >= scan.distance) { best = e; break; }
        if (best == null) { replanAt = 0; nextAct = t + 10; return; }
        start(sl, best.skill(), t);
    }

    private void start(ServerLevel sl, AnimeSkill s, long t) {
        pending = s;
        castAt = t + s.castTicks();
        ready.put(s.id(), t + cooldown(s));
        boss.spendCharge(cost(s));
        String clip = s.has("melee_arc") ? "sword_combo_" + (1 + combo++ % 3) : s.animation();
        boss.setClip(clip, s.name());
        if (!s.line().isEmpty() && (s.tier() >= 3 || boss.getRandom().nextInt(4) == 0)) boss.say(sl, s.line());
        VfxSpawn.sendFollowing(sl, VfxShape.LIGHTNING_FIEND, boss, boss.position(), RivenAttacks.BLUE_VIOLET, s.castTicks() + 8, 1.0f);
        boss.getNavigation().stop();
    }

    static int cost(AnimeSkill s) { return s.tier() <= 1 ? 0 : 6 * s.tier(); }

    static int cooldown(AnimeSkill s) { return s.tier() <= 1 ? 30 : 50 + 30 * s.tier(); }

    /** A bard keeps his distance: closes past 14 blocks, backs off inside 4 unless the target has shown it can't punish it. */
    private void move(LivingEntity tg, long t) {
        if (t % 10 != 0) return;
        double d = Math.sqrt(boss.distanceToSqr(tg));
        if (d > 14) boss.getNavigation().moveTo(tg, 1.0);
        else if (d < 4 && !scan.traits.contains("anti_magic")) {
            Vec3 away = boss.position().subtract(tg.position()).normalize().scale(6).add(boss.position());
            boss.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
        } else if (d < 4) boss.getNavigation().moveTo(tg, 1.0);                               // vs anti-magic he fights with constructs and physical combos up close
        else boss.getNavigation().stop();
    }
}
