package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the 72 Pillars as a field of its own kind ({@link JudgmentFields.Behavior}): the field gives the list of the cast's work to the one manager that already has the caps, the replace-oldest rule and the
 * cleanup on death, logout and server stop. Each pulse ({@link SolomonRules#PILLAR_PULSE_TICKS} ticks) lets the pillars whose landing tick has come land: at most 72 in the whole cast, about three a pulse.
 * The landing places and tick come from {@link SolomonRules} (pure functions of the seed), the same ones the client draws from, so no packet carries a pillar.
 *
 * <p>An impact: one box query of {@link SolomonRules#PILLAR_IMPACT_RADIUS} blocks, at most {@link SolomonRules#PILLAR_IMPACT_MAX_CREATURES} creatures that are foes of the caster take a true hit (it bypasses
 * Michael's Castle Guard, see {@link JudgmentDamage#raw}) of the pillar's damage through the Balance Law and the config's cap, no creature more than {@link SolomonRules#PILLAR_MAX_HITS} times in one cast; then
 * {@link SolomonTerrain} may dig a small crater (at most {@link SolomonRules#TERRAIN_PER_PILLAR} blocks a pillar and {@link SolomonRules#TERRAIN_MAX_BLOCKS} a cast).
 */
final class SolomonPillars implements JudgmentFields.Behavior {
    static final String KIND = "pillars";

    /** One marked target: the ground point under it when it was marked, how many pillars fall there, the seed of the packet. */
    record Target(Vec3 ground, int count, long seed) {}

    private final List<Target> targets;
    private final long start;
    private int lastElapsed = -1, terrain, impacts;
    private final Map<UUID, Integer> hits = new HashMap<>();

    SolomonPillars(List<Target> targets, long start) {
        this.targets = new ArrayList<>(targets);
        this.start = start;
    }

    int terrainRemoved() { return terrain; }

    int impacts() { return impacts; }

    @Override public int period() { return SolomonRules.PILLAR_PULSE_TICKS; }

    @Override
    public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
        int elapsed = (int) (level.getGameTime() - start);
        for (Target t : targets) {
            for (int k = 0; k < t.count(); k++) {
                int land = SolomonRules.pillarSpawnTick(k, t.count()) + SolomonRules.PILLAR_DELAY_TICKS;
                if (land > lastElapsed && land <= elapsed) impact(owner, level, t, k);
            }
        }
        lastElapsed = elapsed;
    }

    private void impact(ServerPlayer owner, ServerLevel level, Target t, int k) {
        double[] off = SolomonRules.pillarOffset(t.seed(), k, SolomonRules.PILLAR_SPREAD);
        Vec3 at = new Vec3(t.ground().x + off[0], t.ground().y, t.ground().z + off[1]);
        impacts++;
        double r = SolomonRules.PILLAR_IMPACT_RADIUS, r2 = r * r;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r, 3.0, r),
                x -> x.isAlive() && !x.isSpectator() && Foes.isFoe(owner, x) && (x.getX() - at.x) * (x.getX() - at.x) + (x.getZ() - at.z) * (x.getZ() - at.z) <= r2);
        int n = 0;
        for (LivingEntity x : near) {
            if (n++ >= SolomonRules.PILLAR_IMPACT_MAX_CREATURES) break;
            int so = hits.getOrDefault(x.getUUID(), 0);
            double d = SolomonRules.pillarDamage(so);
            if (d <= 0.0) continue;
            hits.put(x.getUUID(), so + 1);
            float scaled = BalanceLaw.damage(owner, x, (float) d, 1.0);
            JudgmentDamage.raw(owner, x, BalanceLaw.cap(x, (float) Math.min(JudgmentConfig.activeCap(), scaled)));
        }
        if (terrain < SolomonRules.TERRAIN_MAX_BLOCKS) {
            BlockPos s = SolomonTerrain.surface(level, at.x, at.z);
            if (s != null && Math.abs(s.getY() - t.ground().y) <= 8.0) terrain += SolomonTerrain.crater(level, s, Math.min(SolomonRules.TERRAIN_PER_PILLAR, SolomonRules.TERRAIN_MAX_BLOCKS - terrain));
        }
        if (impacts % 6 == 1) SolomonFx.impact(level, at);
    }
}
