package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * 0.49: smarter summons (painted constructs, mirror doubles, spirit lords). Two goals:
 * <ul>
 *   <li>{@link Guard} (target): every second it re-reads the fight round the owner and picks the most dangerous foe - whoever
 *       is attacking the owner, then whoever is attacking an ally summon, then the weakest hostile within reach.</li>
 *   <li>{@link Caster}: fights like a mage. It keeps a casting distance, casts a Tensura spell from its kit
 *       ({@link TensuraCaster}), or else this mod's mana bolt in its owner's colour, and mends its owner when they fall below 40%
 *       health.</li>
 * </ul>
 */
public final class SummonBrain {
    private SummonBrain() {}

    /** Target choice by threat. */
    public static final class Guard extends Goal {
        private final TamableAnimal mob;
        private final double reach;
        private int next;

        public Guard(TamableAnimal mob, double reach) { this.mob = mob; this.reach = reach; setFlags(EnumSet.of(Flag.TARGET)); }

        @Override public boolean canUse() { return mob.getOwner() != null && --next <= 0; }
        @Override public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            next = 20;
            LivingEntity owner = mob.getOwner();
            if (owner == null) return;
            List<LivingEntity> near = mob.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(reach),
                    e -> e.isAlive() && e != owner && e != mob && !e.isAlliedTo(owner) && !(e instanceof TamableAnimal ta && ta.getOwner() == owner));
            LivingEntity best = null;
            double top = 0;
            for (LivingEntity e : near) {
                double s = 0;
                if (e instanceof Mob m && m.getTarget() == owner) s += 100;
                if (owner.getLastHurtByMob() == e) s += 80;
                if (e instanceof Mob m && m.getTarget() instanceof TamableAnimal ta && ta.getOwner() == owner) s += 40;
                if (e instanceof Enemy) s += 20;
                if (s <= 0) continue;
                s += 10 * (1 - e.getHealth() / Math.max(1, e.getMaxHealth()));   // finish the weak
                s -= Math.sqrt(e.distanceToSqr(mob)) * 0.5;
                if (best == null || s > top) { best = e; top = s; }
            }
            if (best != null && best != mob.getTarget()) mob.setTarget(best);
        }
    }

    /** Casting, distance and support. */
    public static final class Caster extends Goal {
        private final TamableAnimal mob;
        private final int color;
        private final float bolt;
        private final boolean keepRange;
        private final String[] keywords;
        private List<ResourceLocation> kit;
        private int cooldown = 40, healCooldown;

        /**
         * 'keywords' choose the Tensura spells it learns; 'bolt' is the mana bolt's base damage; 'keepRange' makes it a backline
         * caster that holds its distance (otherwise it keeps brawling and casts between blows).
         */
        public Caster(TamableAnimal mob, int color, float bolt, boolean keepRange, String... keywords) {
            this.mob = mob; this.color = color; this.bolt = bolt; this.keepRange = keepRange; this.keywords = keywords;
            setFlags(keepRange ? EnumSet.of(Flag.MOVE) : EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget(), owner = mob.getOwner();
            return owner != null && (t != null && t.isAlive() || owner.getHealth() < owner.getMaxHealth() * 0.4f && healCooldown <= 0);
        }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel sl)) return;
            cooldown--; healCooldown--;
            LivingEntity owner = mob.getOwner(), t = mob.getTarget();
            if (owner != null && healCooldown <= 0 && owner.getHealth() < owner.getMaxHealth() * 0.4f && owner.distanceToSqr(mob) < 12 * 12) {
                healCooldown = 300;
                BalanceLaw.heal(owner, owner.getMaxHealth() * 0.15f);
                VfxSpawn.send(sl, VfxShape.MIRROR_RAY, mob.getEyePosition(), owner.getBoundingBox().getCenter(), 0xFF8CFFB0, 14, 0.6f);
                sl.playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
            if (t == null || !t.isAlive()) return;
            double d = mob.distanceToSqr(t);
            if (!keepRange) { /* brawler: the melee goal moves it */ }
            else if (d < 4 * 4) {                                                   // too close for spells: step back
                Vec3 away = mob.position().subtract(t.position()).normalize().scale(5).add(mob.position());
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            } else if (d > 12 * 12) mob.getNavigation().moveTo(t, 1.1);
            else mob.getNavigation().stop();
            if (keepRange) mob.getLookControl().setLookAt(t, 30, 30);
            if (cooldown > 0 || d > 16 * 16 || !mob.hasLineOfSight(t)) return;
            cooldown = 70 + mob.getRandom().nextInt(30);
            if (kit == null) { kit = TensuraCaster.learn(mob, 2, keywords); TensuraCaster.ensureMana(mob, 50_000); }
            TensuraCaster.ensureMana(mob, 50_000);
            if (!kit.isEmpty() && mob.getRandom().nextBoolean() && TensuraCaster.cast(mob, t, kit, 6) != null) return;
            manaBolt(sl, t);
        }

        /** This mod's spell: a homing mana bolt in the summon's colour, its owner credited (balance-law damage). */
        void manaBolt(ServerLevel sl, LivingEntity t) {
            if (!(mob.getOwner() instanceof ServerPlayer owner)) return;
            Vec3 from = mob.getEyePosition(), dir = t.getBoundingBox().getCenter().subtract(from).normalize();
            SpellRuntime.bolt(owner, from, dir.scale(1.2), 0.6, 30, false, t, (b, hit) -> {
                if (hit == mob || hit == owner || hit.isAlliedTo(owner)) return;
                hit.hurt(mob.damageSources().indirectMagic(mob, owner), BalanceLaw.damage(owner, hit, bolt, 0.4));
            }, null);
            VfxSpawn.send(sl, VfxShape.MIRROR_RAY, from, from.add(dir.scale(2)), color, 8, 0.5f);
            sl.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }
}
