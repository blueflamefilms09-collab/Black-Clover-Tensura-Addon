package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Why: the Legendary Shield of Naofumi Iwatani is the defender of the four Holy Weapons, and what makes it one is a handful of behaviours the shared machinery
 * cannot know: a raised guard that soaks blows until it breaks, a parry for the player who times it, the stored Counter that blocked damage becomes, the cover and
 * wards that carry its protection to other people, and the passives of its forms (the Leaf Shield's herbs, the Needle Shield's needles, the Soul Eater Shield's
 * souls, the fury of the Wrath series). This is its one logic object: it lists the techniques ({@link ShieldSkills}), keeps the guard ({@link GuardMeter}) in step
 * with the weapon, and implements the hooks the machinery calls. Every hook catches what it should not throw and reports it once; none changes a block; the damage a
 * hook deals goes through the balance law and never exceeds the blow it answers. The numbers are {@link ShieldNumbers}'s, the forms are
 * {@code data/nusmp/hero/forms/legendary_shield.json}, the whole is described in {@code docs/heroweapon/weapon_shield.md}.
 */
public final class ShieldLogic implements HeroLogic {
    private final List<HeroSkill> skills = ShieldSkills.all();

    /** The constructor runs once at start-up (the lead's {@code HeroBootstrap}): it hooks the events the cover needs and registers the self-test. */
    public ShieldLogic() {
        ShieldRuntime.register(NeoForge.EVENT_BUS);
        SelfTests.register("hero.weapon.shield", ShieldSelfTest::run);
    }

    @Override
    public HeroKind kind() { return HeroKind.SHIELD; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    // ------------------------------------------------------------------ the form passives, as numbers
    /** The Resolve multiple of the active form: the Light Metal form's Bulwark raises it. */
    static double capacityMultiple(HeroForm f) { return f.has("bulwark") ? ShieldNumbers.BULWARK_CAPACITY : ShieldNumbers.GUARD_CAPACITY; }

    static int parryWindow(HeroForm f) { return f.has("bulwark") ? ShieldNumbers.PARRY_WINDOW_BULWARK : ShieldNumbers.PARRY_WINDOW; }

    // ------------------------------------------------------------------ hooks
    @Override
    public void onTick(HeroCtx c) {
        try {
            ServerPlayer p = c.player;
            ShieldState st = ShieldRuntime.state(p);
            long now = c.level.getGameTime();
            st.guard.tick(now, p.getMaxHealth(), capacityMultiple(c.form));
            int step = (int) (st.guard.counter() / 25.0);
            if (step > st.announcedStep) c.say("Counter " + (step * 25) + " percent");
            st.announcedStep = step;
            double share = st.guard.share(p.getMaxHealth(), capacityMultiple(c.form));
            if (share < 0.25 && !st.warnedLow && !st.guard.broken(now)) {
                st.warnedLow = true;
                c.say("The guard is failing");
            } else if (share > 0.5) {
                st.warnedLow = false;
            }
            if (c.form.has("herb_regen") && now - st.lastHurt > 60 && p.getHealth() < p.getMaxHealth())
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
            if (c.form.has("drain_null")) {
                p.removeEffect(MobEffects.WITHER);
                p.removeEffect(MobEffects.HUNGER);
            }
            if (c.form.has("hatchling_guard")) {
                for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ShieldNumbers.HATCHLING_RADIUS),
                        e -> e.isAlive() && e instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())))
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, true, false));
            }
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.shield.onTick", ex);
        }
    }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) {
        try {
            ShieldState st = ShieldRuntime.state(c.player);
            long now = c.level.getGameTime();
            if (amount > 0f) st.lastHurt = now;
            if (!c.summoned() || !(amount > 0f) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
            ServerPlayer p = c.player;
            float a = amount;
            if (c.form.has("balloon_bounce") && source.is(DamageTypeTags.IS_FALL)) return 0f;
            if (c.form.has("gel_absorb")) a -= (float) Math.min(a * ShieldNumbers.GEL_SHARE, p.getMaxHealth() * ShieldNumbers.GEL_CAP);
            if (c.form.has("rage_fury")) {
                boolean running = st.furyUntil > now;
                st.furyLevel = running ? Math.min(ShieldNumbers.FURY_MAX_LEVEL, st.furyLevel + 1) : 0;
                st.furyUntil = now + ShieldNumbers.FURY_TICKS;
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ShieldNumbers.FURY_TICKS, st.furyLevel, true, true));
            }
            if (c.form.has("rotting_endurance") && now >= st.enduranceReadyAt && p.getHealth() - a < p.getMaxHealth() * ShieldNumbers.ENDURANCE_BELOW) {
                st.enduranceReadyAt = now + ShieldNumbers.ENDURANCE_COOLDOWN;
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ShieldNumbers.ENDURANCE_REGEN_TICKS, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
                c.say("The flesh holds");
            }
            return Math.max(0f, a);
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.shield.onDamaged", ex);
            return amount;
        }
    }

    @Override
    public float onBlocked(HeroCtx c, DamageSource source, float amount) {
        try {
            if (!(amount > 0f)) return amount;
            ServerPlayer p = c.player;
            ShieldState st = ShieldRuntime.state(p);
            long now = c.level.getGameTime();
            st.lastHurt = now;
            boolean parry = GuardMeter.isParry(p.getTicksUsingItem(), parryWindow(c.form));
            double rate = c.form.has("rage_fury") ? 1.5 : ShieldNumbers.COUNTER_RATE;                    // a Rage form turns more of what it soaks into Counter
            GuardMeter.Result r = st.guard.block(now, amount, p.getMaxHealth(), capacityMultiple(c.form), parry, rate);
            Entity attacker = source.getEntity();
            boolean melee = attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
            Vec3 at = p.getBoundingBox().getCenter();
            if (r.parried()) {
                ShieldFx.shatter(c.level, at.add(c.look().scale(0.8)), at.add(c.look()), 0.6f, ShieldFx.SILVER);
                ShieldFx.sound(c.level, p.position(), SoundEvents.SHIELD_BLOCK, 1.2f, 1.7f);
                if (melee) c.shove((LivingEntity) attacker, p.position(), 0.6);
                c.say("Parry");
            } else if (r.broken()) {
                ShieldFx.shatter(c.level, at, at.add(0, 1, 0), 1.0f, ShieldFx.STEEL);
                ShieldFx.sound(c.level, p.position(), SoundEvents.SHIELD_BREAK, 1.2f, 0.9f);
                if (!c.stack.isEmpty()) p.getCooldowns().addCooldown(c.stack.getItem(), ShieldNumbers.BREAK_TICKS);
                p.stopUsingItem();
                c.say("The guard breaks");
            }
            if (melee && r.absorbed() > 0) answer(c, st, (LivingEntity) attacker, r.absorbed(), now);
            return (float) Math.max(0.0, r.through());
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.shield.onBlocked", ex);
            return amount;
        }
    }

    /** What the forms add to a blocked melee blow: the needles that answer it, and the viper's fang. */
    private static void answer(HeroCtx c, ShieldState st, LivingEntity attacker, double soaked, long now) {
        if (c.form.has("needle_counter") && ShieldTargets.isFoe(c.player, attacker) && st.mayAnswer(attacker.getUUID(), now)) {
            double limit = CounterMath.limit(soaked, ShieldNumbers.NEEDLE_SHARE);
            double raw = Math.min(ShieldNumbers.NEEDLE_RAW_CAP, Math.max(1.0, limit));
            float probe = BalanceLaw.damage(c.player, attacker, (float) (raw * c.power), c.masteryShare());
            double fitted = CounterMath.fit(raw, probe, limit);
            if (fitted > 0) {
                ShieldOps.strike(c, attacker, fitted);
                ShieldFx.tellLine(c.level, c.player.getBoundingBox().getCenter(), attacker.getBoundingBox().getCenter(), ShieldFx.TELL_STEEL);
            }
        }
        if (c.form.has("venom_fang") && !(attacker instanceof Player) && !BalanceLaw.isBoss(attacker) && ShieldTargets.isFoe(c.player, attacker))
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, ShieldNumbers.VENOM_TICKS, 0));
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) {
        try {
            if (!c.summoned() || !c.form.has("soul_eater") || victim == c.player || victim.distanceToSqr(c.player) > ShieldNumbers.SOUL_KILL_RADIUS * ShieldNumbers.SOUL_KILL_RADIUS) return;
            int n = ShieldRuntime.state(c.player).souls.add(c.level.getGameTime());
            c.say(n + (n == 1 ? " soul" : " souls") + " eaten");
            ShieldFx.souls(c.level, victim.position(), 1.5);
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.shield.onKill", ex);
        }
    }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        try {
            ShieldState st = ShieldRuntime.state(c.player);
            st.furyLevel = 0;
            st.furyUntil = 0;
            st.announcedStep = (int) (st.guard.counter() / 25.0);
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.shield.onFormChanged", ex);
        }
    }

    @Override
    public void onDismiss(HeroCtx c) {
        try {
            ShieldState st = ShieldRuntime.peek(c.player.getUUID());
            if (st != null) st.endFloating();
            ShieldRuntime.book().forget(c.player.getUUID());                 // the promises are the weapon's: they end when it is put away
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.shield.onDismiss", ex);
        }
    }
}
