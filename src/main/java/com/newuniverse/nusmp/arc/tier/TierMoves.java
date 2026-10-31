package com.newuniverse.nusmp.arc.tier;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.early.EarlyGeometry;
import com.newuniverse.nusmp.arc.early.EarlyMoves;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: turns a data row ({@link TierMove}) into the framework's {@code Move} (numbers for the scheduler, a telegraph, the code that runs when the wind-up ends)
 * with exactly one implementation per shape, built on the early bosses' acts ({@link EarlyMoves}: cone, circle, ring wave, bolt, charge, marked circles, wisps). All
 * of them look at at most {@value EarlyMoves#MAX_FOES} foes, none changes a block, spawns an entity or heals, a bolt lives at most 28 ticks, a volley is at most 5 bolts
 * and a swarm at most 4 wisps, so the worst case of any move is a handful of short zones. A move that throws is logged and skipped, never allowed to take the tick down.
 */
public final class TierMoves {
    private TierMoves() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** The moves of a row, in its order. */
    public static List<Move> build(TierRow row) {
        List<Move> out = new ArrayList<>();
        for (TierMove m : row.moves()) out.add(build(m));
        return out;
    }

    public static Move build(TierMove m) {
        Move.Builder b = Move.builder(m.id(), m.label()).minPower(m.minPower()).windUp(m.windUp()).cooldown(m.cooldown()).range(m.minRange(), m.maxRange()).weight(m.weight());
        if (m.ultimate()) b.ultimate();
        if (m.crowd()) b.crowd();
        b.tell((boss, target, left, total) -> {
            tellSafe(m, boss, target, left);
            if (boss instanceof TierBoss tb) {
                try {
                    TierTraits.onTell(tb, m);                       // 3.39.0: the Balance's window opens with the wind-up of its move
                } catch (RuntimeException e) {
                    LOG.debug("[nusmp] tier trait telegraph hook {} failed: {}", m.id(), e.toString());
                }
            }
        });
        return b.run((boss, target) -> {
            runSafe(m, boss, target);
            if (boss instanceof TierBoss tb) {
                try {
                    TierTraits.afterMove(tb, m, target);            // 3.39.0: the traits that hang on this move
                } catch (RuntimeException e) {
                    LOG.warn("[nusmp] tier trait hook of move {} of {} failed: {}", m.id(), boss.bossName(), e.toString());
                }
            }
        });
    }

    /** A move's body, with a thrown error logged and swallowed (also the body the Echo trait repeats). */
    static void runSafe(TierMove m, ZogratisBoss boss, LivingEntity target) {
        try {
            run(m, boss, target);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] tier move {} of {} failed: {}", m.id(), boss.bossName(), e.toString());
        }
    }

    private static void tellSafe(TierMove m, ZogratisBoss boss, LivingEntity target, int left) {
        try {
            tell(m, boss, target, left);
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] tier move telegraph {} failed: {}", m.id(), e.toString());
        }
    }

    /** The telegraph of a move shown once more (the Echo's second use). */
    static void tellSafe(TierMove m, ZogratisBoss boss, LivingEntity target) { tellSafe(m, boss, target, 0); }

    // ================================================================ the damage that lands
    private static float damage(TierMove m, ZogratisBoss boss) { return (float) (boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * m.damage()); }

    private static void hit(TierMove m, ZogratisBoss boss, LivingEntity victim, float dmg) {
        if (!boss.strike(victim, dmg, m.magic())) return;
        status(m, victim);
        if (m.knock() > 0) EarlyMoves.shove(victim, boss.position(), m.knock());
    }

    /** The status a move leaves; lengths are capped by {@link TierRules}, so this only maps. */
    public static void status(LivingEntity e, TierMove.Effect fx, int ticks) { status(e, fx, ticks, false); }

    /** {@code all}: a crowd or ultimate move strips every beneficial effect instead of one. The 3.39.0 statuses do nothing while the setting {@code tierPowers} is off. */
    public static void status(LivingEntity e, TierMove.Effect fx, int ticks, boolean all) {
        if (ticks <= 0) return;
        switch (fx) {
            case SLOW -> EarlyMoves.slow(e, ticks, 1);
            case WEAKEN -> EarlyMoves.weaken(e, ticks, 0);
            case BLIND -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
            case FROST -> EarlyMoves.chill(e, ticks);
            case BURN -> e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), ticks));
            case POISON -> { if (ArcConfig.tierPowers()) e.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0)); }
            case SHIELD_BREAK -> TierTraits.breakShield(e, ticks);
            case STRIP -> TierTraits.strip(e, all);
            case NONE -> { }
        }
    }

    private static void status(TierMove m, LivingEntity e) { status(e, m.effect(), m.effectTicks(), m.ultimate() || m.crowd()); }

    private static Vec3 facing(ZogratisBoss boss, LivingEntity target) {
        return EarlyMoves.flat(target.position().subtract(boss.position()), boss.getLookAngle());
    }

    // ================================================================ the code of each shape
    private static void run(TierMove m, ZogratisBoss boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        float dmg = damage(m, boss);
        int rgb = boss.styleColor(), accent = boss.styleAccent();
        Vec3 pos = boss.position();
        switch (m.kind()) {
            case CLEAVE -> {
                Vec3 face = facing(boss, target);
                for (LivingEntity e : EarlyMoves.inCone(boss, pos, face, m.size(), Math.toRadians(65))) hit(m, boss, e, dmg);
                Tells.burst(sl, pos.add(face.scale(m.size() * 0.55)).add(0, 1.1, 0), rgb, 8);
            }
            case SLAM -> {
                for (LivingEntity e : EarlyMoves.inCircle(boss, pos, m.size())) hit(m, boss, e, dmg);
                Tells.ring(sl, pos.add(0, 0.1, 0), m.size(), rgb, 1.5f, (int) Math.min(48, 12 + m.size() * 5));
                Tells.burst(sl, pos.add(0, 0.6, 0), accent, 10);
            }
            case WAVE -> {
                EarlyMoves.ringWave(boss, pos, m.size(), dmg, m.knock(), m.magic(), rgb);
                if (m.effect() != TierMove.Effect.NONE)
                    for (LivingEntity e : EarlyMoves.inCircle(boss, pos, m.size())) if (EarlyMoves.feetAboveFloor(sl, e) < EarlyGeometry.JUMP_HEIGHT) status(m, e);
            }
            case BOLT -> shoot(m, boss, target, dmg, 0.0, rgb);
            case VOLLEY -> {
                double[] angles = EarlyGeometry.fan(m.count(), Math.toRadians(m.size()));
                for (int i = 0; i < angles.length; i++) {
                    final double a = angles[i];
                    boss.later(i * 3, () -> shoot(m, boss, target, dmg, a, rgb));
                }
            }
            case MARKED -> {
                List<Vec3> centres = new ArrayList<>();
                Vec3 base = ZogratisBoss.ground(sl, target.position());
                centres.add(base);
                for (int i = 1; i < m.count(); i++) {
                    double ang = boss.getRandom().nextDouble() * Math.PI * 2, r = 2.0 + boss.getRandom().nextDouble() * 3.0;
                    centres.add(ZogratisBoss.ground(sl, base.add(Math.cos(ang) * r, 0, Math.sin(ang) * r)));
                }
                EarlyMoves.batchStrike(boss, centres, m.size(), 18, dmg, m.magic(), rgb, accent, (lsl, victim, c) -> {
                    status(m, victim);
                    if (m.knock() > 0) EarlyMoves.shove(victim, c, m.knock());
                }, (bsl, c) -> Tells.burst(bsl, c.add(0, 0.5, 0), accent, 8));
            }
            case RUSH -> EarlyMoves.charge(boss, facing(boss, target), 1.1, (int) m.size(), 2.4, dmg, m.knock());
            case BEAM -> {
                Vec3 from = boss.getEyePosition(), face = facing(boss, target);
                Vec3 to = EarlyMoves.stopAtWall(sl, from, from.add(face.scale(m.size())), boss);
                for (LivingEntity e : EarlyMoves.inCorridor(boss, pos, new Vec3(to.x, pos.y, to.z), 1.2)) hit(m, boss, e, dmg);
                Tells.line(sl, from, to, accent);
            }
            case SWARM -> {
                List<Vec3> starts = new ArrayList<>();
                for (int i = 0; i < m.count(); i++) {
                    double ang = i * Math.PI * 2 / m.count();
                    starts.add(pos.add(Math.cos(ang) * 2.0, 1.2, Math.sin(ang) * 2.0));
                }
                EarlyMoves.wisps(boss, starts, 0.35, 70, m.size(), dmg, m.magic(), rgb, (lsl, victim, c) -> status(m, victim),
                        (bsl, c) -> Tells.burst(bsl, c, accent, 8));
            }
        }
    }

    /** One bolt at the target, turned {@code angle} radians sideways (0 for a single bolt). */
    private static void shoot(TierMove m, ZogratisBoss boss, LivingEntity target, float dmg, double angle, int rgb) {
        if (!boss.isAlive() || !target.isAlive()) return;
        Vec3 from = boss.getEyePosition(), to = target.getBoundingBox().getCenter(), d = to.subtract(from);
        double[] r = EarlyGeometry.rotate(d.x, d.z, angle);
        Vec3 dir = new Vec3(r[0], d.y, r[1]);
        EarlyMoves.bolt(boss, from, dir, 1.4, 28, 0.9, dmg, m.magic(), (sl, where, victim) -> {
            Tells.burst(sl, where, rgb, 6);
            if (victim != null) status(m, victim);
        });
    }

    // ================================================================ the telegraph (every third tick of the wind-up)
    private static void tell(TierMove m, ZogratisBoss boss, LivingEntity target, int left) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        int rgb = boss.styleColor(), accent = boss.styleAccent();
        Vec3 pos = boss.position();
        switch (m.kind()) {
            case SLAM, WAVE -> {
                Tells.ring(sl, pos.add(0, 0.1, 0), m.size(), rgb, 1.2f, (int) Math.min(48, 12 + m.size() * 4));
                Tells.gather(sl, pos.add(0, boss.getBbHeight() * 0.6, 0), 1.0, accent, 4);
            }
            case CLEAVE -> Tells.line(sl, pos.add(0, 1.0, 0), pos.add(facing(boss, target).scale(m.size())).add(0, 1.0, 0), rgb);
            case BOLT, VOLLEY, RUSH, BEAM -> {
                Vec3 from = boss.getEyePosition(), to = target.getBoundingBox().getCenter(), d = to.subtract(from);
                double len = Math.min(16.0, d.length());
                if (len > 0.1) Tells.line(sl, from, from.add(d.normalize().scale(len)), rgb);
                Tells.gather(sl, boss.getEyePosition(), 0.7, accent, 3);
            }
            case MARKED -> {
                Vec3 at = ZogratisBoss.ground(sl, target.position());
                Tells.ring(sl, at, m.size(), rgb, 1.2f, 20);
                Tells.gather(sl, at.add(0, 0.2, 0), m.size() * 0.5, accent, 3);
            }
            case SWARM -> Tells.gather(sl, pos.add(0, 1.2, 0), 2.0, rgb, 6);
        }
    }
}
