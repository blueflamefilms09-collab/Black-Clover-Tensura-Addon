package com.newuniverse.nusmp.soul;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the client has to know which creatures to dress in a halo and wings, and a mob effect is the one piece of state vanilla
 * already syncs to everyone tracking an entity, shows in no list a player can break, and saves with the creature. So a Paladin
 * carries {@code nusmp:paladin}: an infinite, particle-less, icon-less beneficial effect that does nothing by itself (the stats are
 * attribute modifiers, the behaviour is in {@link Paladins}); it is only the marker. It is its own {@link DeferredRegister}, so the
 * lead wires two lines and no other registry is touched, the same way as {@code LuciusEffects}.
 * <p>
 * Wire it in NUSMP: {@code PaladinEffects.init()} and {@code PaladinEffects.EFFECTS.register(modEventBus)}.
 */
public final class PaladinEffects {
    private PaladinEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> PALADIN = EFFECTS.register("paladin", PaladinEffect::new);

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    /** The marker as it is applied: infinite, ambient (no particles), invisible in the HUD list. */
    public static MobEffectInstance marker() {
        return new MobEffectInstance(PALADIN, MobEffectInstance.INFINITE_DURATION, 0, true, false, false);
    }

    /** True if this creature carries the marker. Works on the client too (the effect is synced to trackers). */
    public static boolean marked(LivingEntity e) { return e != null && e.hasEffect(PALADIN); }

    /** The effect: beneficial, white-gold, and inert (it never ticks anything). */
    static final class PaladinEffect extends MobEffect {
        PaladinEffect() { super(MobEffectCategory.BENEFICIAL, 0xFFE9A8); }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return false; }
    }
}
