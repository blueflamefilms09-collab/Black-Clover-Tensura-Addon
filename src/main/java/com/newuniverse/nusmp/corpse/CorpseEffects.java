package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Frankenstein look of a Soul Corpse has to appear on ANY creature (a zombie, a wolf, a player) and strengthen as it rots,
 * and the client must learn that without an extra packet. A mob effect is the one piece of state vanilla already syncs to every client
 * that tracks the entity, so the marker is {@code nusmp:soul_corpse}: infinite duration, and its AMPLIFIER is the decay stage 0..3
 * ({@link CorpseDecay#amplifier}), refreshed by the server every five seconds. The effect itself does nothing (no attribute, no tick);
 * the rendering lives in client.corpse.CorpseDecayLayer and the rules in {@link Corpses}. It is neutral and shows no icon or particles.
 * <p>
 * Wire it in NUSMP: {@code CorpseEffects.init()} and {@code CorpseEffects.EFFECTS.register(modEventBus)}.
 */
public final class CorpseEffects {
    private CorpseEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SOUL_CORPSE = EFFECTS.register("soul_corpse", MarkEffect::new);

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    public static Holder<MobEffect> holder() { return SOUL_CORPSE; }

    /** The marker: a colour (pale grey-green) and nothing else. */
    static final class MarkEffect extends MobEffect {
        MarkEffect() { super(MobEffectCategory.NEUTRAL, 0x9FB0A4); }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return false; }
    }
}
