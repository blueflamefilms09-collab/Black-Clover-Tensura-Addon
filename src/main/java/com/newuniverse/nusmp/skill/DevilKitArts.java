package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.dual.DevilMagic;
import com.newuniverse.nusmp.devil.dual.DualConfig;
import com.newuniverse.nusmp.devil.dual.DualRules;
import com.newuniverse.nusmp.devil.mastery.DevilKit;
import com.newuniverse.nusmp.devil.mastery.MasteryRules;
import com.newuniverse.nusmp.devil.phase2.Phase2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the work of one art of a devil's kit ({@link DevilKit.Art}, docs/devil_mastery.md), kept apart from the price, cooldown and mastery of {@link DevilAwakeningSkill} so a self-test can fire it. The four shapes
 * come from the data file and nothing else: a strike (a cone), a burst (a radius around the caster), a rush (a short dash that hits what it passes) and a ward (only effects on the caster). The damage goes through the
 * balance law as devil magic ({@link DevilMagic#source}), the foe effects and the specials reuse {@link DevilArts#apply}.
 * Worst case of one cast: one entity query, at most {@link #MAX_STRIKE} / {@link #MAX_BURST} / {@link #MAX_RUSH} hits, three self effects, one velocity write, one small particle burst per hit. Nothing is spawned.
 */
public final class DevilKitArts {
    private DevilKitArts() {}

    public static final int MAX_STRIKE = 6, MAX_BURST = 8, MAX_RUSH = 4;
    private static final double CONE = 0.35, RUSH_WIDTH = 1.6, RUSH_SPEED_CAP = 2.0;

    /** The raw hit of the art before the balance law: the tier's base hit times the art's multiple, the mastery's scale, the server's scale and the kind's share; never above {@link DualRules#MAX_RAW}. */
    public static double raw(ServerPlayer p, DevilContract c, DevilKit.Art art, float mastery) {
        double v = DualRules.baseDamage(c.tier) * art.dmg() * MasteryRules.artScale(mastery) * Math.max(0.0, Math.min(10.0, DualConfig.damageScale())) * Phase2.power(p, c);
        return Math.max(0.0, Math.min(DualRules.MAX_RAW, v));
    }

    /** Fires the art; returns how many foes were hit (0 for a ward or an empty cone). */
    public static int cast(ServerPlayer p, DevilContract c, DevilKit.Art art, float mastery) {
        if (p == null || !p.isAlive() || art == null) return 0;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        for (DevilKit.Fx f : art.self()) com.newuniverse.nusmp.relics.Fx.addEffect(p, f.id(), f.ticks(), f.amp());
        List<LivingEntity> foes;
        int max;
        switch (art.kind()) {
            case "strike" -> { foes = SkillTargets.foesInCone(p, art.range(), CONE); max = MAX_STRIKE; }
            case "burst" -> { foes = SkillTargets.foes(p, p.position(), art.range()); max = MAX_BURST; }
            case "rush" -> {
                Vec3 look = p.getLookAngle();
                Vec3 dir = new Vec3(look.x, 0, look.z);
                dir = dir.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : dir.normalize();
                Vec3 from = p.position(), to = from.add(dir.scale(art.range()));
                p.setDeltaMovement(dir.x * Math.min(RUSH_SPEED_CAP, 0.6 + art.range() * 0.12), 0.15, dir.z * Math.min(RUSH_SPEED_CAP, 0.6 + art.range() * 0.12));
                p.hurtMarked = true;
                p.fallDistance = 0f;
                foes = SkillTargets.foesAlong(p, from, to, RUSH_WIDTH);
                max = MAX_RUSH;
            }
            default -> { foes = List.of(); max = 0; }                                                // a ward: only the self effects
        }
        double raw = raw(p, c, art, mastery);
        int hits = 0;
        for (LivingEntity t : foes) {
            if (hits >= max) break;
            float dmg = BalanceLaw.damage(p, t, (float) raw, mastery / 100.0, null);
            if (dmg <= 0f) continue;
            float before = t.getHealth();
            t.hurt(DevilMagic.source(p), dmg);
            float dealt = Math.max(0f, before - t.getHealth());
            hits++;
            for (DevilKit.Fx f : art.fx()) DevilArts.apply(p, t, f.id(), f.ticks(), f.amp(), "", false, now);
            special(p, t, art.special(), dealt, now);
            SkillFx.burst(level, t.getBoundingBox().getCenter(), false, 6, 0.3);
        }
        SkillUtil.castVfx(p, 0xFF000000 | DualRules.color(c.attribute));
        return hits;
    }

    private static void special(ServerPlayer p, LivingEntity t, String special, float dealt, long now) {
        switch (special) {
            case "drain" -> com.newuniverse.nusmp.relics.Fx.heal(p, (float) Math.min(6.0, dealt * 0.15));
            case "launch" -> { t.setDeltaMovement(t.getDeltaMovement().add(0, 0.9, 0)); t.hurtMarked = true; }
            case "" -> { }
            default -> DevilArts.apply(p, t, "", 0, 0, special, "strip".equals(special), now);           // burn, pull, push, strip
        }
    }
}
