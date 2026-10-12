package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two status effects of Lucius Zogratis's Soul Magic. Wire it in NUSMP: call {@link #init()} and
 * {@code EFFECTS.register(modEventBus)}.
 * <ul>
 *   <li><b>Soul Paladin</b> marks a mob that Lucius has turned into an immortal Paladin (armour and speed ride on the effect, health and
 *       attack are set by {@link SoulPaladins}). It is synced like any effect, so the client can tell a Paladin from an ordinary mob
 *       and draw it in white and gold. It is renewed every few seconds while Lucius lives, so milk does not free a Paladin.</li>
 *   <li><b>Purified</b> is what the Soul Purge leaves on a player: their devil power was stripped. It only informs; the purge itself
 *       ended the Devil Union and the attunement.</li>
 * </ul>
 */
public final class ZogratisEffects {
    private ZogratisEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SOUL_PALADIN = EFFECTS.register("soul_paladin", () -> new Plain(MobEffectCategory.BENEFICIAL, 0xF2E6B0)
            .addAttributeModifier(Attributes.ARMOR, id("soul_paladin_armor"), 6.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("soul_paladin_speed"), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> PURIFIED = EFFECTS.register("purified", () -> new Plain(MobEffectCategory.HARMFUL, 0xFFF0A0)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("purified_attack"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    /** An effect with nothing but attribute modifiers (MobEffect's constructor is protected). */
    private static final class Plain extends MobEffect {
        Plain(MobEffectCategory category, int color) { super(category, color); }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }
}
