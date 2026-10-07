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
        apply(e, attr, id, amount, ticks, AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * 0.48: the same with any operation (e.g. ADD_MULTIPLIED_TOTAL -0.1 = 10% less). Transient: never saved, so it is also gone
     * after a relog or respawn. Re-applying the same id restarts its timer instead of stacking.
     */
    public static void apply(LivingEntity e, Holder<Attribute> attr, ResourceLocation id, double amount, int ticks, AttributeModifier.Operation op) {
        var inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
        ACTIVE.removeIf(t -> t.entity() == e && t.id().equals(id) && t.attribute().equals(attr));
        ACTIVE.add(new Timed(e, attr, id, e.level().getGameTime() + ticks));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Timed> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            if (t.entity().isRemoved() || t.entity().level().getGameTime() >= t.until()) {
                var inst = t.entity().getAttribute(t.attribute());
                if (inst != null) inst.removeModifier(t.id());
                // 0.48: a player who came back from the End is a new entity that may carry the modifier over: clear it there too
                if (t.entity() instanceof net.minecraft.server.level.ServerPlayer sp && sp.getServer() != null) {
                    var now = sp.getServer().getPlayerList().getPlayer(sp.getUUID());
                    if (now != null && now != sp && now.getAttribute(t.attribute()) != null) now.getAttribute(t.attribute()).removeModifier(t.id());
                }
                it.remove();
            }
        }
    }
}
