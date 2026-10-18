package com.newuniverse.nusmp.heroweapon.weapon.fan;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Forgetful;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Why: the behaviour of the Vassal Fan (Glass' war fan): graceful wind and spirit play, wide sweeps, gusts that push and pull, a gliding dash and a veil of
 * mist, built around the magic residue the fan drinks from its foes (the series' Ambient Mana Absorption). The nine techniques are in {@link FanSkills}, their
 * numbers in {@link FanNumbers}, their words in {@link FanText}; this class is the part the shared machinery calls: it lists the skills, softens a fall for the
 * Featherfall passive, dulls arrows while the Keystone Fan's veil holds, ends the veil when the wielder strikes, and gathers residue from full swings and kills.
 * It also registers the weapon's self-test {@code hero.weapon.fan} and clears its small per-wielder state when a wielder leaves or the server stops.
 */
public final class FanLogic implements HeroLogic, Forgetful {
    private final List<HeroSkill> skills = FanSkills.all();

    public FanLogic() {
        SelfTests.register("hero.weapon.fan", server -> GlassSelfTest.run(server, HeroKind.FAN, FanNumbers.table(), null));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> FanState.clearAll());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> FanState.forget(e.getEntity().getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.FAN; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) {
        long now = c.level.getGameTime();
        FanState.VEIL.prune(now);
        FanState.GLIDE.prune(now);
        // the glide of Featherfall: sneak while falling and the wind holds you up (slow falling, renewed every five ticks for as long as you hold)
        Player p = c.player;
        if (!c.summoned() || !c.form.has(FanNumbers.P_FEATHERFALL)) return;                 // the wind is the fan's: it must be in the hand
        boolean carried = p.getAbilities().flying || p.isFallFlying() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger();
        boolean was = FanState.GLIDE.active(p.getUUID(), now);
        if (FanNumbers.glides(p.onGround(), p.isShiftKeyDown(), carried, -p.getDeltaMovement().y, p.fallDistance, was)) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FanNumbers.GLIDE_TICKS, 0, false, false, false));
            FanState.GLIDE.set(p.getUUID(), now + FanNumbers.GLIDE_TICKS);
        }
    }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (GlassKit.striking()) return;                              // the fan's own techniques are not melee swings
        FanSkills.unveil(c);
        if (fullSwing) FanSkills.swung(c);
    }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) {
        if (!c.summoned()) return amount;
        if (c.form.has(FanNumbers.P_FEATHERFALL) && source.is(DamageTypeTags.IS_FALL)) return amount * FanNumbers.FALL_FACTOR;
        if (c.form.has(FanNumbers.P_KEYSTONE_WARD) && source.is(DamageTypeTags.IS_PROJECTILE) && FanState.VEIL.active(c.player.getUUID(), c.level.getGameTime())) {
            return amount * (float) FanNumbers.VEIL_PROJECTILE_FACTOR;
        }
        return amount;
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) {
        if (c.summoned()) FanSkills.gain(c, 1);
    }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        // the residue belongs to the fan, not to a form: it stays; only a veil does not outlive a change of form
        FanState.VEIL.clear(c.player.getUUID());
    }

    @Override
    public void onDismiss(HeroCtx c) {
        forget(c.player.getUUID());
    }

    @Override
    public void forget(UUID wielder) {
        FanState.forget(wielder);
    }
}
