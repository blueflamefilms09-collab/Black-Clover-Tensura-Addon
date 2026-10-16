package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the god-tier Lucius sabotages three things on a player for a limited time (their relics, their Djinn and spirits, their passive
 * healing), and an effect is the one thing in Minecraft that every cleansing path already knows about: milk removes it, death clears it,
 * the icon tells the player what is wrong and how long it lasts, other mods list it. Three plain effects with no behaviour of their own
 * (the rules that read them are {@code RelicNullification}, {@code Severance} and the Time Decay aura of {@link LuciusBossEventHandler}),
 * in their own {@link DeferredRegister} so the lead wires one line and no other registry is touched.
 * <p>
 * Wire it in NUSMP like {@link LuciusEffects}: {@code LuciusGodEffects.init()} and {@code LuciusGodEffects.EFFECTS.register(modEventBus)}.
 */
public final class LuciusGodEffects {
    private LuciusGodEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    /** Every relic of the wearer is de-attuned. */
    public static final DeferredHolder<MobEffect, MobEffect> RELIC_NULLIFIED = EFFECTS.register("relic_nullified", () -> new Marker(0x7FD7FF));
    /** Djinn and spirits do not answer. */
    public static final DeferredHolder<MobEffect, MobEffect> SEVERED = EFFECTS.register("severed", () -> new Marker(0xB07CFF));
    /** No passive healing: shown on whoever stands in Lucius's Time Decay aura. */
    public static final DeferredHolder<MobEffect, MobEffect> TIME_DECAY = EFFECTS.register("time_decay", () -> new Marker(0xE0B84A));

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    /** An effect that only marks: harmful, no attribute, no tick. */
    private static final class Marker extends MobEffect {
        Marker(int rgb) { super(MobEffectCategory.HARMFUL, rgb); }
    }

    /**
     * Puts an effect on a player or mob for 'ticks' (a fresh duration, never a longer stack), with the icon visible and no particles.
     * Creative and spectator players are left alone. Returns whether it was applied.
     */
    public static boolean mark(LivingEntity who, DeferredHolder<MobEffect, MobEffect> effect, int ticks) {
        if (who == null || !who.isAlive() || who.level().isClientSide || who.isSpectator()) return false;
        if (who instanceof Player p && p.isCreative()) return false;
        return who.addEffect(new MobEffectInstance(effect, Math.max(1, ticks), 0, false, false, true));
    }
}
