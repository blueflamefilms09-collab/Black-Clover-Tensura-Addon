package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.eco.FactionDef;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionRegistry;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseSystem;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Why: a foundry mob's behaviour is its ARCHETYPE's movement style plus its abilities, so the AI is a handful of goals, each for one style ({@link FoundryArchetype.Style}), that
 * ALL exist on every mob and each switches itself on only when the mob's style is its own. That is what lets one entity class (whose goals are registered in the constructor,
 * before the identity is known) behave as ten archetypes: melee (close in, hit), hit-and-run (strike, step away), ranged (keep a band of distance, shoot), support (stay by the
 * allies, keep out of reach), charge (telegraph, then ram), air (hover over the target, swoop). On top of them sits the {@link AbilityGoal}, which asks the pure {@link FoundryBrain}
 * for an ability, stands through its wind-up (the cast clip plays on the client) and fires it, and the {@link RivalGoal}, the foundry's own turf-war look: with no player near, a
 * mob goes for the nearest mob of a rival faction ({@code FactionRegistry.rivals}). Every goal is cheap per tick and swallows nothing it should not: the world work is in
 * {@link FoundryAbilityRunner}.
 */
public final class FoundryGoals {
    private FoundryGoals() {}

    /** The archetype styles with a goal of their own: the self-test checks that every style of every shipped archetype is here. */
    public static final List<FoundryArchetype.Style> HANDLED = List.of(FoundryArchetype.Style.values());

    /** Adds the six style goals (priority 3) to a goal selector. */
    public static void addStyleGoals(FoundryMobEntity m, GoalSelector selector) {
        selector.addGoal(3, new Melee(m));
        selector.addGoal(3, new HitRun(m));
        selector.addGoal(3, new Ranged(m));
        selector.addGoal(3, new Support(m));
        selector.addGoal(3, new Charge(m));
        selector.addGoal(3, new Air(m));
    }

    /** The style a goal serves, or null when it is not one of the six style goals (the self-test counts them). */
    static FoundryArchetype.Style styleOf(Goal g) { return g instanceof StyleGoal s ? s.style : null; }

    // ================================================================ the ability goal
    /**
     * Asks the brain, stands through the wind-up, fires. Holds MOVE and LOOK only while casting. 1.107.0: no longer final, so {@link TelegraphedAttackGoal} (the version that draws the warning, grants
     * super armor and freezes its aim) can extend it; with {@code foundry.poise.telegraphedAttacks} off this class is used as it always was.
     */
    static class AbilityGoal extends Goal {
        final FoundryMobEntity m;
        final Random rnd = new Random();
        int pending = -1, active = -1;
        LivingEntity locked;

        AbilityGoal(FoundryMobEntity m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (m.level().isClientSide || !m.isAlive() || m.casting() || PoiseSystem.staggered(m) || !FoundryConfig.get(FoundryConfig.ABILITIES)) return false;
            long now = m.tickCount;
            if (!m.brain().due(now)) return false;
            pending = m.brain().choose(now, false, FoundryAbilityRunner.situation(m), rnd);
            return pending >= 0;
        }

        @Override
        public boolean canContinueToUse() { return m.isAlive() && m.casting() && active >= 0; }

        @Override
        public void start() {
            active = pending;
            FoundryAbility a = m.brain().abilities().get(active);
            locked = m.foe();
            int wind = Math.max(1, a.wind());
            m.setCast(wind, wind);
            m.getNavigation().stop();
        }

        @Override
        public void tick() {
            FoundryAbility a = m.brain().abilities().get(active);
            LivingEntity t = locked != null && locked.isAlive() ? locked : m.foe();
            if (t != null) m.getLookControl().setLookAt(t, 40.0f, 40.0f);
            int left = m.castLeft() - 1;
            if (m.level() instanceof ServerLevel level && left % 4 == 0) {
                RukhFx.gather(level, m, RukhFx.Alignment.of(a.dark(m.road() == com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL)), 3);
            }
            if (left <= 0) {
                m.setCast(0, 0);
                m.brain().used(active, m.tickCount);
                FoundryAbilityRunner.fire(m, a, t);
                active = -1;
            } else {
                m.setCast(left, m.castMax());
            }
        }

        @Override
        public void stop() {
            if (m.castLeft() > 0) m.setCast(0, 0);          // interrupted (it died, or another goal took over): the cast is lost, the cooldown is not started
            active = -1;
            locked = null;
        }
    }

    // ================================================================ the styles
    private abstract static class StyleGoal extends Goal {
        final FoundryMobEntity m;
        final FoundryArchetype.Style style;
        int cooldown, repath;

        StyleGoal(FoundryMobEntity m, FoundryArchetype.Style style) {
            this.m = m;
            this.style = style;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        LivingEntity target() {
            LivingEntity t = m.getTarget();
            return t != null && t.isAlive() ? t : null;
        }

        @Override
        public boolean canUse() { return !m.level().isClientSide && m.isAlive() && !m.casting() && !PoiseSystem.staggered(m) && m.style() == style && target() != null; }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void stop() {
            m.getNavigation().stop();
            cooldown = 0;
        }

        double reachSqr(LivingEntity t) { return m.getBbWidth() * 2.0 * m.getBbWidth() * 2.0 + t.getBbWidth(); }

        /** One swing: the arm animation on the client, the damage on the server. */
        boolean strike(LivingEntity t, int every) {
            if (cooldown > 0 || m.distanceToSqr(t) > reachSqr(t) || !m.hasLineOfSight(t)) return false;
            cooldown = PoiseSystem.scale(m, every);                        // 1.107.0: a desperate elite or captain swings faster
            m.swing(InteractionHand.MAIN_HAND);
            m.doHurtTarget(t);
            return true;
        }

        void chase(LivingEntity t, double speed) {
            if (--repath <= 0) {
                repath = 4 + m.getRandom().nextInt(7);
                m.getNavigation().moveTo(t, speed);
            }
        }

        void away(LivingEntity t, double speed) {
            Vec3 p = DefaultRandomPos.getPosAway(m, 9, 4, t.position());
            if (p != null) m.getNavigation().moveTo(p.x, p.y, p.z, speed);
        }
    }

    /** Close in and hit (brute, tank). */
    static final class Melee extends StyleGoal {
        Melee(FoundryMobEntity m) { super(m, FoundryArchetype.Style.MELEE); }

        @Override
        public void tick() {
            LivingEntity t = target();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 30.0f, 30.0f);
            if (cooldown > 0) cooldown--;
            chase(t, 1.15);
            strike(t, 20);
        }
    }

    /** Strike, then step away for a moment, then again (skirmisher, assassin). */
    static final class HitRun extends StyleGoal {
        private int retreat;

        HitRun(FoundryMobEntity m) { super(m, FoundryArchetype.Style.HITRUN); }

        @Override
        public void tick() {
            LivingEntity t = target();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 30.0f, 30.0f);
            if (cooldown > 0) cooldown--;
            if (retreat > 0) {
                retreat--;
                if (retreat % 6 == 0) away(t, 1.3);
                return;
            }
            chase(t, 1.3);
            if (strike(t, 16)) retreat = 12 + m.getRandom().nextInt(8);
        }
    }

    /** Keep a band of distance and shoot (archer, caster, summoner). */
    static final class Ranged extends StyleGoal {
        private int strafe, side = 1;

        Ranged(FoundryMobEntity m) { super(m, FoundryArchetype.Style.RANGED); }

        @Override
        public void tick() {
            LivingEntity t = target();
            if (t == null) return;
            FoundryArchetype a = m.archetype();
            m.getLookControl().setLookAt(t, 30.0f, 30.0f);
            if (cooldown > 0) cooldown--;
            double d = Math.sqrt(m.distanceToSqr(t));
            boolean sees = m.hasLineOfSight(t);
            if (d < Math.max(2.0, a.keep())) {
                if (--repath <= 0) {
                    repath = 10;
                    away(t, 1.25);
                }
            } else if (d > a.far() || !sees) {
                chase(t, 1.0);
            } else {
                m.getNavigation().stop();
                if (--strafe <= 0) {
                    strafe = 20 + m.getRandom().nextInt(30);
                    side = m.getRandom().nextBoolean() ? 1 : -1;
                }
                m.getMoveControl().strafe(0.0f, 0.5f * side);
            }
            if (sees && d <= a.far() + 2.0 && cooldown <= 0) {
                cooldown = PoiseSystem.scale(m, 36);
                if (a.id().equals("archer")) FoundryAbilityRunner.shootArrow(m, t);
                else FoundryAbilityRunner.shootBolt(m, t);
            }
        }
    }

    /** Stay by the allies and out of reach; jab only when cornered (support). */
    static final class Support extends StyleGoal {
        private FoundryMobEntity anchor;
        private int look;

        Support(FoundryMobEntity m) { super(m, FoundryArchetype.Style.SUPPORT); }

        @Override
        public boolean canUse() { return !m.level().isClientSide && m.isAlive() && !m.casting() && !PoiseSystem.staggered(m) && m.style() == style && (target() != null || anchor() != null); }

        private FoundryMobEntity anchor() {
            if (--look <= 0 || (anchor != null && !anchor.isAlive())) {
                look = 20;
                anchor = null;
                double best = Double.MAX_VALUE;
                for (FoundryMobEntity f : m.level().getEntitiesOfClass(FoundryMobEntity.class, m.getBoundingBox().inflate(16.0), e -> e != m && e.isAlive() && m.friend(e))) {
                    double d = m.distanceToSqr(f);
                    if (d < best) {
                        best = d;
                        anchor = f;
                    }
                }
            }
            return anchor;
        }

        @Override
        public void tick() {
            LivingEntity t = target();
            FoundryMobEntity a = anchor();
            if (cooldown > 0) cooldown--;
            if (t != null) {
                m.getLookControl().setLookAt(t, 30.0f, 30.0f);
                double d = Math.sqrt(m.distanceToSqr(t));
                if (d < Math.max(2.0, m.archetype().keep())) {
                    if (--repath <= 0) {
                        repath = 10;
                        away(t, 1.2);
                    }
                    if (d < 2.5) strike(t, 24);
                    return;
                }
            }
            if (a != null && m.distanceToSqr(a) > 36.0) {
                if (--repath <= 0) {
                    repath = 10;
                    m.getNavigation().moveTo(a, 1.0);
                }
            } else {
                m.getNavigation().stop();
            }
        }
    }

    /** Walk up, plant its feet (the swing clip is the telegraph), then ram in a straight line (charger). */
    static final class Charge extends StyleGoal {
        private int phase, timer, rest;
        private Vec3 dir = Vec3.ZERO;
        private boolean hit;

        Charge(FoundryMobEntity m) { super(m, FoundryArchetype.Style.CHARGE); }

        @Override
        public void stop() {
            super.stop();
            phase = 0;
            timer = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = target();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 40.0f, 40.0f);
            if (cooldown > 0) cooldown--;
            if (rest > 0) rest--;
            double d = Math.sqrt(m.distanceToSqr(t));
            if (phase == 0) {
                chase(t, 1.0);
                strike(t, 24);
                if (rest <= 0 && d >= 5.0 && d <= 16.0 && m.hasLineOfSight(t)) {
                    phase = 1;
                    timer = 16;
                    m.getNavigation().stop();
                    m.swing(InteractionHand.MAIN_HAND);
                }
            } else if (phase == 1) {
                if (--timer <= 0) {
                    Vec3 v = t.position().subtract(m.position());
                    double len = Math.sqrt(v.x * v.x + v.z * v.z);
                    dir = len < 1.0E-3 ? Vec3.ZERO : new Vec3(v.x / len, 0.0, v.z / len);
                    phase = 2;
                    timer = 16;
                    hit = false;
                }
            } else {
                m.setDeltaMovement(dir.x * 0.85, m.getDeltaMovement().y, dir.z * 0.85);
                m.hurtMarked = true;
                if (!hit && m.distanceToSqr(t) < 3.0) {
                    hit = true;
                    m.doHurtTarget(t);
                    t.knockback(1.1, -dir.x, -dir.z);
                    timer = Math.min(timer, 3);
                }
                if (--timer <= 0 || m.horizontalCollision) {
                    phase = 0;
                    rest = 80;
                }
            }
        }
    }

    /** Hover over the target and swoop (flier). */
    static final class Air extends StyleGoal {
        Air(FoundryMobEntity m) { super(m, FoundryArchetype.Style.AIR); }

        @Override
        public void start() { m.setNoGravity(true); }

        @Override
        public void stop() {
            super.stop();
            m.setNoGravity(false);
        }

        @Override
        public void tick() {
            LivingEntity t = target();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 40.0f, 40.0f);
            if (cooldown > 0) cooldown--;
            double a = m.tickCount * 0.09;
            Vec3 goal = t.position().add(Math.sin(a) * 3.0, 2.4 + Math.sin(a * 1.7) * 0.5, Math.cos(a) * 3.0);
            Vec3 d = goal.subtract(m.position());
            double len = d.length();
            Vec3 push = len < 0.3 ? Vec3.ZERO : d.scale(1.0 / len).scale(0.075);
            m.setDeltaMovement(m.getDeltaMovement().scale(0.86).add(push));
            m.fallDistance = 0f;
            strike(t, 24);
        }
    }

    // ================================================================ turf wars
    /** With no player near, go for the nearest mob of a rival faction (the eco package's turf wars, for the foundry's one entity type). */
    static final class RivalGoal extends TargetGoal {
        private final FoundryMobEntity m;
        private LivingEntity victim;
        private int next;

        RivalGoal(FoundryMobEntity m) {
            super(m, false);
            this.m = m;
            setFlags(EnumSet.of(Flag.TARGET));
            this.next = m.getRandom().nextInt(40);
        }

        @Override
        public boolean canUse() {
            if (m.level().isClientSide || !FoundryConfig.get(FoundryConfig.FACTION_WARS) || m.faction() == null || m.isMinion()) return false;
            if (m.tickCount < next) return false;
            next = m.tickCount + FoundryConfig.get(FoundryConfig.WAR_SCAN_TICKS) + m.getRandom().nextInt(8);
            LivingEntity cur = m.getTarget();
            if (cur != null && cur.isAlive()) return false;
            victim = look();
            return victim != null;
        }

        @Override
        public boolean canContinueToUse() { return victim != null && victim.isAlive() && m.distanceToSqr(victim) < 40.0 * 40.0 && !playerNear(); }

        @Override
        public void start() { m.setTarget(victim); }

        @Override
        public void stop() {
            if (m.getTarget() == victim) m.setTarget(null);
            victim = null;
        }

        private boolean playerNear() {
            for (Player p : m.level().players()) if (!p.isCreative() && !p.isSpectator() && p.distanceToSqr(m) < 12.0 * 12.0) return true;
            return false;
        }

        private LivingEntity look() {
            if (playerNear()) return null;
            String mine = m.faction();
            FactionDef def = FactionRegistry.graph().def(mine);
            double range = def == null || def.range() <= 0 ? 14.0 : def.range();
            LivingEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (Mob o : m.level().getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(range), e -> e != m && e.isAlive())) {
                String theirs = o instanceof FoundryMobEntity f ? f.faction() : FactionRegistry.factionOf(o);
                if (theirs == null || !FactionRegistry.rivals(mine, theirs) || (o instanceof FoundryMobEntity f2 && f2.isMinion() && m.friend(f2))) continue;
                if (!m.hasLineOfSight(o)) continue;
                double d = m.distanceToSqr(o);
                if (d < bestD) {
                    bestD = d;
                    best = o;
                }
            }
            return best;
        }
    }
}
