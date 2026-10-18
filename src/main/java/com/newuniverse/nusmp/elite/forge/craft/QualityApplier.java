package com.newuniverse.nusmp.elite.forge.craft;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.forge.ForgeComponents;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: writes a quality onto a freshly forged stack, in the three ways the player can feel it and the tooltip can show it: the data component
 * {@code nusmp:forge_quality} (and the smith's name), a bigger maximum durability (the {@code max_damage} component, scaled by the quality's durability bonus), and
 * an extra attribute modifier on the item's own attack damage and armour entries ({@code ADD_MULTIPLIED_TOTAL}, so +30% means +30% of the whole value, in the same slot
 * as the entry it grows). The item's default modifiers are copied first, then ours is added, so nothing the item had is lost; an item with neither attack damage nor
 * armour (a bow, a hoe) gets the durability and the label only. Applying twice is a no-op (the component marks it). The strengths are the {@link Quality} table times the
 * config's {@code qualityStatScale}.
 */
public final class QualityApplier {
    private QualityApplier() {}

    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "forge_quality");

    /** Applies the quality; returns false when the stack was already forged or is empty. */
    public static boolean apply(ItemStack stack, Quality q, String smith, double scale) {
        if (stack.isEmpty() || stack.has(ForgeComponents.QUALITY.get())) return false;
        stack.set(ForgeComponents.QUALITY.get(), q.level());
        if (smith != null && !smith.isEmpty()) stack.set(ForgeComponents.SMITH.get(), smith.length() > 40 ? smith.substring(0, 40) : smith);
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int bonus = (int) Math.round(max * q.durabilityBonus() * scale);
            if (bonus > 0) stack.set(DataComponents.MAX_DAMAGE, max + bonus);
        }
        double stat = q.statBonus() * scale;
        if (stat > 0.0) {
            ItemAttributeModifiers current = stack.getAttributeModifiers();
            List<ItemAttributeModifiers.Entry> entries = new ArrayList<>(current.modifiers());
            boolean changed = false;
            for (ItemAttributeModifiers.Entry e : current.modifiers()) {
                if (e.modifier().id().equals(MODIFIER_ID)) continue;
                if (e.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().get()) || e.attribute().is(Attributes.ARMOR.unwrapKey().get())) {
                    entries.add(new ItemAttributeModifiers.Entry(e.attribute(), new AttributeModifier(MODIFIER_ID, stat, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), e.slot()));
                    changed = true;
                }
            }
            if (changed) stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(entries, current.showInTooltip()));
        }
        return true;
    }

    /** True for an attribute holder that gets a quality bonus (used by the self-test). */
    public static boolean grows(net.minecraft.core.Holder<Attribute> attribute) {
        return attribute.is(Attributes.ATTACK_DAMAGE.unwrapKey().get()) || attribute.is(Attributes.ARMOR.unwrapKey().get());
    }
}
