package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.boss.arc.SoulCorruptionMath;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Soul Touch must leave a mark that outlives the blow, and the owner asked for a severe one: Soul Corruption, which cuts
 * maximum health by 10, 20 or 30% for a minute. It is a mob effect of its own (own {@link DeferredRegister}, so the lead wires one
 * line and no other registry is touched) because an effect is the one thing in Minecraft that every cleansing path already knows about:
 * milk and the totem remove it, death clears it, the effect icon shows it, other mods list it. The maximum-health change is not the
 * stock attribute-template modifier, because that cannot honour the floor: the effect writes one transient modifier whose size is
 * {@link SoulCorruptionMath#reduction}, so the player's maximum health never goes under four hearts and the debuff can never kill by
 * itself. The modifier is recomputed every two seconds against the current base maximum (gear or buffs may change it) and is removed
 * with the effect, whichever way it ends.
 * <p>
 * Wire it in NUSMP like {@link ZogratisEffects}: {@code LuciusEffects.init()} and {@code LuciusEffects.EFFECTS.register(modEventBus)}.
 */
public final class LuciusEffects {
    private LuciusEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SOUL_CORRUPTION = EFFECTS.register("soul_corruption", SoulCorruptionEffect::new);

    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "soul_corruption_max_health");

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    /**
     * Puts Soul Corruption on a victim: level 1 the first time, one level more each time it is refreshed, three at the most, a fresh
     * minute each time. Creative and spectator players and dead things are left alone.
     */
    public static void corrupt(LivingEntity victim) {
        if (victim == null || !victim.isAlive() || victim.level().isClientSide || victim.isSpectator()) return;
        if (victim instanceof Player p && p.isCreative()) return;
        MobEffectInstance cur = victim.getEffect(SOUL_CORRUPTION);
        int next = SoulCorruptionMath.nextAmplifier(cur == null ? -1 : cur.getAmplifier());
        victim.addEffect(new MobEffectInstance(SOUL_CORRUPTION, SoulCorruptionMath.DURATION_TICKS, next, false, true, true));
    }

    /** The effect: a harmful, long-lived cut of maximum health with a floor. */
    static final class SoulCorruptionEffect extends MobEffect {
        SoulCorruptionEffect() { super(MobEffectCategory.HARMFUL, 0x6B3FA0); }

        @Override
        public void addAttributeModifiers(AttributeMap map, int amplifier) {
            AttributeInstance max = map.getInstance(Attributes.MAX_HEALTH);
            if (max == null) return;
            max.removeModifier(MODIFIER);
            double cut = SoulCorruptionMath.reduction(max.getBaseValue(), amplifier);
            if (cut > 0.0) max.addTransientModifier(new AttributeModifier(MODIFIER, -cut, AttributeModifier.Operation.ADD_VALUE));
        }

        @Override
        public void removeAttributeModifiers(AttributeMap map) {
            AttributeInstance max = map.getInstance(Attributes.MAX_HEALTH);
            if (max != null) max.removeModifier(MODIFIER);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 40 == 0; }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            AttributeInstance max = entity.getAttribute(Attributes.MAX_HEALTH);
            if (max == null) return true;
            double want = SoulCorruptionMath.reduction(max.getBaseValue(), amplifier);
            AttributeModifier cur = max.getModifier(MODIFIER);
            if (cur == null || Math.abs(cur.amount() + want) > 1e-6) {
                max.removeModifier(MODIFIER);
                if (want > 0.0) max.addTransientModifier(new AttributeModifier(MODIFIER, -want, AttributeModifier.Operation.ADD_VALUE));
            }
            return true;
        }
    }
}
