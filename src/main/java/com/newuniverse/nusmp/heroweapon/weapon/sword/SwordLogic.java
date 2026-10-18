package com.newuniverse.nusmp.heroweapon.weapon.sword;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Why: the Legendary Sword of Ren Amaki is the fast weapon of the four, and what makes it fast is a set of small rules about the wielder's blows that the shared
 * machinery cannot know: a run of blows builds the Tempo (and Tempo earns speed), a full Tempo turns the next full swing into a Flow strike, the step of an afterimage
 * leaves a ghost blow behind, and each form adds one passive (the opening strike of the Flash Sword, the stab in the back of the Assassin Sword, the star every fifth
 * blow of the Meteor Sword, the carapace of the Spirit Tortoise, the plunder of Greed and the fangs of Gluttony). This is its one logic object: it lists the techniques
 * ({@link SwordSkills}), keeps the {@link HitLedger} in step with the wielder's blows and implements the hooks the machinery calls. Every hook catches what it should
 * not throw and reports it once; none changes a block; a blow earns at most ONE bonus strike, never above {@link SwordNumbers#BONUS_MAX}, and it goes through the
 * balance law. The numbers are {@link SwordNumbers}'s, the forms are {@code data/nusmp/hero/forms/legendary_sword.json}, the whole is described in
 * {@code docs/heroweapon/weapon_sword.md}. The sins of the curse series (what Greed and Gluttony cost the wielder) belong to the curse system; the forms here only
 * carry the techniques and the passives that go with them.
 */
public final class SwordLogic implements HeroLogic {
    private final List<HeroSkill> skills = SwordSkills.all();

    /** The constructor runs once at start-up (the lead's {@code HeroBootstrap}): it hooks the events the state cache needs and registers the self-test. */
    public SwordLogic() {
        SwordRuntime.register(NeoForge.EVENT_BUS);
        SelfTests.register("hero.weapon.sword", SwordSelfTest::run);
    }

    @Override
    public HeroKind kind() { return HeroKind.SWORD; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    // ------------------------------------------------------------------ hooks
    @Override
    public void onTick(HeroCtx c) {
        try {
            ServerPlayer p = c.player;
            SwordState st = SwordRuntime.state(p);
            long now = c.level.getGameTime();
            int tempo = st.ledger.tempo(now);
            int level = HitLedger.speedLevel(tempo, c.form.has("wind_walker"));
            if (level > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SwordNumbers.SPEED_TICKS, level - 1, true, false));
            if (tempo != st.shownTempo) {
                for (int t : new int[]{SwordNumbers.SPEED_ONE_AT, SwordNumbers.SPEED_TWO_AT, SwordNumbers.TEMPO_MAX})
                    if (st.shownTempo < t && tempo >= t) c.say(t == SwordNumbers.TEMPO_MAX ? "Tempo full: the next full swing flows" : "Tempo " + t);
                st.shownTempo = tempo;
            }
            if (c.form.has("ember_edge")) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.sword.onTick", ex);
        }
    }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        try {
            ServerPlayer p = c.player;
            if (SwordOps.isStriking() || !c.summoned() || !SwordTargets.isFoe(p, target)) return;       // a strike of ours raises damage events that are not melee blows
            SwordState st = SwordRuntime.state(p);
            long now = c.level.getGameTime();
            Vec3 look = target.getViewVector(1f);
            boolean behind = SwordGeo.behind(look.x, look.z, p.getX() - target.getX(), p.getZ() - target.getZ());
            double health = target.getMaxHealth() > 0 ? target.getHealth() / target.getMaxHealth() : 1.0;
            HitLedger.Plan plan = st.ledger.hit(now, fullSwing, c.form::has, behind, health, st.marks.has(target.getUUID(), now));
            if (c.form.has("gravity_edge") && !BalanceLaw.isBoss(target))
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SwordNumbers.GRAVITY_EDGE_TICKS, 0));
            if (c.form.has("ember_edge")) target.igniteForSeconds(SwordNumbers.EMBER_FIRE_SECONDS);
            if (c.form.has("plunder") && now >= st.plunderReadyAt) {
                st.plunderReadyAt = now + SwordNumbers.PLUNDER_PAUSE;
                SwordOps.plunder(c, target, SwordNumbers.PLUNDER_TAKE, SwordNumbers.PLUNDER_KEEP);
            }
            if (plan != null) bonus(c, st, target, plan, now);
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.sword.onHit", ex);
        }
    }

    /** The one bonus strike a blow earned: where it lands and how it looks. */
    private static void bonus(HeroCtx c, SwordState st, LivingEntity target, HitLedger.Plan plan, long now) {
        var level = c.level;
        Vec3 mid = target.getBoundingBox().getCenter();
        switch (plan.source()) {
            case "flow" -> {
                SwordOps.strike(c, target, plan.raw());
                SwordFx.crescent(level, c.eye(), mid, SwordFx.STEEL, 1.4f);
                SwordFx.sound(level, mid, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
                c.say("Flow");
            }
            case "ghost" -> {
                SwordOps.strike(c, target, plan.raw());
                SwordFx.afterimage(level, c.player.position().subtract(c.look().multiply(1.5, 0, 1.5)), 1.8f);
                SwordFx.streak(level, c.eye(), mid, SwordFx.SKY, 0.8f);
            }
            case "quick_draw" -> {
                SwordOps.strike(c, target, plan.raw());
                SwordFx.flare(level, mid, 0.8f);
            }
            case "shadow_blade" -> {
                SwordOps.strike(c, target, plan.raw());
                SwordFx.crescent(level, mid.add(0, 0.8, 0), mid.subtract(0, 0.4, 0), SwordFx.SKY, 0.9f);
                SwordFx.sound(level, mid, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
            }
            case "star_shower" -> {
                SwordFx.star(level, target.position());
                for (LivingEntity e : SwordTargets.foesAround(c, target.position(), SwordNumbers.STAR_RADIUS)) SwordOps.strike(c, e, plan.raw());
            }
            case "hundred_edge" -> {
                for (LivingEntity e : SwordTargets.foesAround(c, target.position(), SwordNumbers.HUNDRED_EDGE_RADIUS)) SwordOps.strike(c, e, plan.raw());
                for (double[] p : SwordGeo.scatter(target.getX(), target.getZ(), 2.0, 3, now)) SwordFx.falling(level, new Vec3(p[0], target.getY(), p[1]), SwordFx.SKY);
            }
            case "thunder_edge" -> {
                LivingEntity other = null;
                for (LivingEntity e : SwordTargets.foesAround(c, target.position(), SwordNumbers.THUNDER_RANGE)) {
                    if (e != target) {
                        other = e;
                        break;
                    }
                }
                if (other != null && SwordOps.strike(c, other, plan.raw())) {
                    SwordFx.tellLine(level, mid, other.getBoundingBox().getCenter(), SwordFx.TELL_THUNDER);
                    SwordFx.bolt(level, other.position(), SwordFx.THUNDER);
                }
            }
            case "fang_maw" -> {
                SwordOps.strike(c, target, plan.raw());
                SwordFx.maw(level, mid, mid.add(0, 1, 0), 0.8f);
            }
            case "gilded" -> {
                st.marks.consume(target.getUUID(), now);
                SwordOps.strike(c, target, plan.raw());
                SwordFx.flare(level, mid, 0.6f);
            }
            default -> { }
        }
    }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) {
        try {
            if (!c.summoned() || !(amount > 0f) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
            ServerPlayer p = c.player;
            SwordState st = SwordRuntime.state(p);
            long now = c.level.getGameTime();
            float a = amount;
            if (c.form.has("wind_walker") && source.is(DamageTypeTags.IS_FALL)) a *= (float) SwordNumbers.WIND_WALKER_FALL;
            if (c.form.has("carapace_guard") && now >= st.carapaceReadyAt && a >= p.getMaxHealth() * SwordNumbers.CARAPACE_THRESHOLD) {
                st.carapaceReadyAt = now + SwordNumbers.CARAPACE_COOLDOWN;
                a *= (float) SwordNumbers.CARAPACE_SHARE;
                SwordFx.flare(c.level, p.getBoundingBox().getCenter(), 0.8f);
                SwordFx.sound(c.level, p.position(), SoundEvents.SHIELD_BLOCK, 1.2f, 0.8f);
                c.say("The carapace turns the blow");
            }
            return a;
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.sword.onDamaged", ex);
            return amount;
        }
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) {
        try {
            if (c.summoned() && victim != c.player) SwordRuntime.state(c.player).ledger.kill(c.level.getGameTime());
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.sword.onKill", ex);
        }
    }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        try {
            SwordState st = SwordRuntime.state(c.player);
            st.ledger.reset();
            st.castNumber++;                                                      // the floating swords of the old form end
            st.floatUntil = 0;
            st.shownTempo = 0;
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.sword.onFormChanged", ex);
        }
    }

    @Override
    public void onDismiss(HeroCtx c) {
        try {
            SwordState st = SwordRuntime.peek(c.player.getUUID());
            if (st == null) return;
            st.castNumber++;                                                      // the floating swords end with the weapon
            st.floatUntil = 0;
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("hero.sword.onDismiss", ex);
        }
    }
}
