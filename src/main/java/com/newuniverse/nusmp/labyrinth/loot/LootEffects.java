package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: "mana regeneration" as a food buff has no vanilla effect to borrow, and the mod's own way of giving a mana buff is the way it already uses for the naming sleep: a
 * status effect that carries an attribute modifier on Tensura's magicule regeneration. {@code nusmp:mana_surge} adds 75 percent to the regeneration multiplier for as long as
 * it lasts, nothing else: no tick work, no drain, and it is a beneficial effect (so milk and the like treat it like any other buff). Spirit Tea, the Mana Elixirs and the
 * Sage's Elixir give it. It is registered on the mod bus by {@link LootRegistry#init}.
 */
public final class LootEffects {
    private LootEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    /** Magicules come back faster. */
    public static final DeferredHolder<MobEffect, MobEffect> MANA_SURGE = EFFECTS.register("mana_surge", ManaSurge::new);

    static final class ManaSurge extends MobEffect {
        ManaSurge() {
            super(MobEffectCategory.BENEFICIAL, 0x5AA8FF);
            addAttributeModifier(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, ResourceLocation.fromNamespaceAndPath("nusmp", "mana_surge"), 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }
}
