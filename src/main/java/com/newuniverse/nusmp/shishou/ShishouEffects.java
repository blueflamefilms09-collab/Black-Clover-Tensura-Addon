package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import com.newuniverse.nusmp.perf.PerfParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Why: Black Flame (Kuroen) is Shishou's own damage-over-time effect, a real {@link MobEffect} (so milk, death and other mods' cleansing know it, and the icon tells the victim what is wrong). It burns for five
 * seconds, one pulse every half second, as magic damage (which ignores armour points), the pulse capped by the server config; it blocks natural health regeneration of the victim (the handler in
 * {@link ShishouEvents} cancels heals while it lasts) and it is not fire, so water does not put it out. The victim remembers WHO lit it (a UUID in its persistent data), so a kill by the flame counts
 * for the wielder or the sword (Skill Eater). Dark purple flame particles go through the perf budget, to nearby players only.
 */
public final class ShishouEffects {
    private ShishouEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> BLACK_FLAME = EFFECTS.register("black_flame", BlackFlame::new);

    public static final String SOURCE_KEY = "nusmp_black_flame_by";

    /** Loads this class before the registry event. */
    public static void init() {}

    private static final DustParticleOptions DARK_PURPLE = new DustParticleOptions(new Vector3f(0.22f, 0.04f, 0.32f), 1.3f);

    private static final class BlackFlame extends MobEffect {
        BlackFlame() { super(MobEffectCategory.HARMFUL, 0x2A0A3A); }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % ShishouRules.BLACK_FLAME_PERIOD == 0; }

        @Override
        public boolean applyEffectTick(LivingEntity victim, int amplifier) {
            if (!(victim.level() instanceof ServerLevel sl) || !victim.isAlive()) return true;
            float dmg = (float) ShishouRules.blackFlamePulse(amplifier * 25 + 12, ShishouConfig.flameCap());
            Entity by = null;
            if (victim.getPersistentData().hasUUID(SOURCE_KEY)) by = sl.getEntity(victim.getPersistentData().getUUID(SOURCE_KEY));
            DamageSource src = by instanceof LivingEntity l ? victim.damageSources().indirectMagic(l, l) : victim.damageSources().magic();
            int keepTime = victim.invulnerableTime;
            victim.invulnerableTime = 0;                                               // the pulse is its own hit: the half second between hits is the effect's own pace
            victim.hurt(src, dmg);
            victim.invulnerableTime = Math.max(keepTime, victim.invulnerableTime);
            double cx = victim.getX(), cy = victim.getY() + victim.getBbHeight() * 0.5, cz = victim.getZ();
            PerfParticles.send(sl, DARK_PURPLE, cx, cy, cz, 6, victim.getBbWidth() * 0.35, victim.getBbHeight() * 0.35, victim.getBbWidth() * 0.35, 0.02);
            PerfParticles.send(sl, RukhParticles.dark(), cx, cy, cz, 1, victim.getBbWidth() * 0.3, victim.getBbHeight() * 0.3, victim.getBbWidth() * 0.3, 0.01);
            return true;
        }
    }

    /** Lights the flame on a victim for {@link ShishouRules#BLACK_FLAME_TICKS}, remembering who did it. The amplifier is the level band of the sword. Returns whether it was applied. */
    public static boolean ignite(LivingEntity victim, LivingEntity by, int swordLevel) {
        if (victim == null || !victim.isAlive() || victim.level().isClientSide) return false;
        if (by != null) victim.getPersistentData().putUUID(SOURCE_KEY, by.getUUID());
        return victim.addEffect(new MobEffectInstance(BLACK_FLAME, ShishouRules.BLACK_FLAME_TICKS, ShishouRules.blackFlameAmp(swordLevel), false, true, true));
    }

    /** Is the victim burning with the black flame (the regeneration block asks)? */
    public static boolean burning(LivingEntity e) { return e != null && e.hasEffect(BLACK_FLAME); }

    /** The id of the lighter, or null. */
    public static UUID lighter(LivingEntity victim) { return victim.getPersistentData().hasUUID(SOURCE_KEY) ? victim.getPersistentData().getUUID(SOURCE_KEY) : null; }
}
