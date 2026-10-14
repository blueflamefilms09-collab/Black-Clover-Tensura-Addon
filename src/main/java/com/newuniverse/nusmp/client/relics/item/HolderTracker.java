package com.newuniverse.nusmp.client.relics.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * Who is holding the item that WeaponRenderer is drawing. A BlockEntityWithoutLevelRenderer is handed the stack and the display context but not the
 * holder, so the holder is tracked here: in first person it is the local player; in third person (and on a head) the living entity whose renderer is
 * running right now, between {@link RenderLivingEvent.Pre} and {@link RenderLivingEvent.Post}. An item on the ground, in a frame or in a menu has no
 * holder (null), and then draws at rest. Render thread only.
 */
public final class HolderTracker {
    private HolderTracker() {}

    private static LivingEntity current;

    public static LivingEntity holderFor(ItemDisplayContext ctx) {
        if (ctx.firstPerson()) return Minecraft.getInstance().player;
        return current;
    }

    public static void onPre(RenderLivingEvent.Pre<?, ?> e) { current = e.getEntity(); }

    public static void onPost(RenderLivingEvent.Post<?, ?> e) { current = null; }

    public static void reset() { current = null; }
}
