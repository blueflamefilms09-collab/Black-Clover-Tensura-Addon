package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxTone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Why: a desperate foundry mob unlocks its archetype's ultimate, but a captain that is NOT a foundry mob (a zombie, a Tensura monster, anything the Director put in a squad) has no ability
 * engine to unlock an ultimate in. It still has to become the most dangerous thing in the room, so it gets this one goal: when it is desperate (the poise layer's flag), has a target within
 * reach and its cooldown is over, it plants its feet for 26 ticks (with super armor, so the party must break its bar to stop it) while the ground in a 5 block circle around it is outlined with
 * particles, then the ground bursts: every player in the circle takes 1.5 times the mob's attack damage (never a share of anyone's health) and is thrown outwards. It only ever hits
 * players; it is added once, by {@link PoiseSystem}, when the mob turns desperate, and only to mobs that are not {@link PoiseAware}. A fault is swallowed: the goal answers "no".
 */
public final class DesperationShockwaveGoal extends Goal {
    static final int WIND = 26, COOLDOWN = 260;
    static final double RADIUS = 5.0, TRIGGER = 6.5;

    private final Mob m;
    private int wind, nextAt;

    public DesperationShockwaveGoal(Mob m) {
        this.m = m;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        try {
            if (m.level().isClientSide || !m.isAlive() || m.tickCount < nextAt || !PoiseSystem.desperate(m) || PoiseSystem.staggered(m)) return false;
            LivingEntity t = m.getTarget();
            return t != null && t.isAlive() && m.distanceToSqr(t) <= TRIGGER * TRIGGER;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() { return wind > 0 && m.isAlive() && !PoiseSystem.staggered(m); }

    @Override
    public void start() {
        wind = Math.max(Desperation.MIN_WIND, Desperation.wind(WIND, true, PoiseSystem.tuning()));
        PoiseSystem.raiseArmor(m, wind + PoiseSystem.tuning().armorTail());
        m.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity t = m.getTarget();
        if (t != null) m.getLookControl().setLookAt(t, 30.0f, 30.0f);
        wind--;
        if (!(m.level() instanceof ServerLevel level)) return;
        if (wind % 4 == 0 && wind > 0) {
            double progress = 1.0 - wind / (double) WIND;
            PoiseCue.shape(level, TelegraphShape.CIRCLE, RADIUS, 360.0, RADIUS, 1.0, true, m.getX(), m.getY(), m.getZ(), 0.0, progress);
        }
        if (wind <= 0) blast(level);
    }

    private void blast(ServerLevel level) {
        double attack = m.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? m.getAttributeValue(Attributes.ATTACK_DAMAGE) : 4.0;
        float dmg = (float) Math.max(1.0, attack * 1.5);
        Vec3 c = m.position();
        for (Player p : level.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive() || p.distanceToSqr(c) > RADIUS * RADIUS) continue;
            p.hurt(level.damageSources().mobAttack(m), dmg);
            double dx = p.getX() - c.x, dz = p.getZ() - c.z, len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1.0E-3) p.knockback(0.9, -dx / len, -dz / len);
        }
        Fx.burst(level, PoiseCue.THEME, m, FxTone.DARK, 12);
        Fx.ring(level, PoiseCue.THEME, FxTone.DARK, c.x, c.y + 0.1, c.z, RADIUS * 0.8, 24);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.2f);
        nextAt = m.tickCount + COOLDOWN;
        wind = 0;
    }

    @Override
    public void stop() {
        if (wind > 0) {
            nextAt = m.tickCount + COOLDOWN / 2;                 // interrupted (the bar broke, it died): a shorter wait, the attack was lost
            PoiseSystem.dropArmor(m);
        }
        wind = 0;
    }
}
