package com.newuniverse.nusmp.relics.tier;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiplies the attribute modifiers of a raised relic. {@link ItemAttributeModifierEvent} fires every time the game asks a stack for its modifiers
 * (the tooltip, the equipment-change check, applying them to the wearer), so the first lines are a single component lookup that leaves an unraised
 * stack alone. The new values keep the modifier's id and slot (the event replaces by attribute and id), and they come from {@link TierRules}, whose
 * caps decide how far a stat may go.
 */
public final class TierEvents {
    private TierEvents() {}

    public static void register(IEventBus gameBus) { gameBus.addListener(TierEvents::onAttributes); }

    static void onAttributes(ItemAttributeModifierEvent e) {
        ItemStack stack = e.getItemStack();
        Integer raw = stack.get(TierComponents.TIER.get());
        if (raw == null || raw <= 0) return;                                       // never raised: untouched, no lookups
        RelicDef def = Relics.defOf(stack);
        if (def == null) return;
        int steps = Tiers.steps(stack, def);
        if (steps <= 0) return;
        List<ItemAttributeModifiers.Entry> entries = new ArrayList<>(e.getModifiers());
        for (ItemAttributeModifiers.Entry en : entries) {
            AttributeModifier m = en.modifier();
            if (m.operation() != AttributeModifier.Operation.ADD_VALUE) continue;
            Holder<Attribute> a = en.attribute();
            double v = m.amount(), nv = v;
            if (a.value() == Attributes.ATTACK_DAMAGE.value()) nv = TierRules.scaledDamageModifier(v, steps);
            else if (a.value() == Attributes.ATTACK_SPEED.value()) nv = TierRules.scaledSpeedModifier(v, steps);
            else if (a.value() == Attributes.ARMOR.value()) nv = TierRules.scaledArmor(v, steps);
            else if (a.value() == Attributes.ARMOR_TOUGHNESS.value()) nv = TierRules.scaledToughness(v, steps);
            if (nv == v) continue;
            EquipmentSlotGroup slot = en.slot();
            e.replaceModifier(a, new AttributeModifier(m.id(), nv, m.operation()), slot);
        }
    }
}
