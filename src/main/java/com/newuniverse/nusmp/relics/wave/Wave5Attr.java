package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/** Transient attribute modifiers with a stable id: put on, kept in step, taken off. They vanish on their own after a relog, so no cleanup can be missed. */
final class Wave5Attr {
    private Wave5Attr() {}

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", "w5_" + path); }

    /** Make the modifier present (with this amount) or absent. Cheap when nothing changes. */
    static void sync(LivingEntity e, Holder<Attribute> attribute, ResourceLocation id, double amount, Operation op, boolean on) {
        if (e == null || attribute == null) return;
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (!on) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        if (cur != null && cur.amount() == amount && cur.operation() == op) return;
        if (cur != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    static void clamp(LivingEntity e) {
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }
}
