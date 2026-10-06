package com.newuniverse.nusmp.blackclover;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Temporary attribute changes (e.g. armor shred) that remove themselves. */
public final class TimedModifiers {
    private record Timed(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, long until) {}
    private static final List<Timed> ACTIVE = new ArrayList<>();

    private TimedModifiers() {}

    public static void apply(LivingEntity e, Holder<Attribute> attr, ResourceLocation id, double amount, int ticks) {
        var inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        ACTIVE.add(new Timed(e, attr, id, e.level().getGameTime() + ticks));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Timed> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            if (t.entity().isRemoved() || t.entity().level().getGameTime() >= t.until()) {
                var inst = t.entity().getAttribute(t.attribute());
                if (inst != null) inst.removeModifier(t.id());
                it.remove();
            }
        }
    }
}
