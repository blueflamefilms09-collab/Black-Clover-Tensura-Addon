package com.newuniverse.nusmp.avatar.ritual;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: the ritual "locks the player in place" while the circle draws and the screen is open, and that lock must never be able to trap anyone. It is two
 * TRANSIENT attribute modifiers (movement speed and jump strength both multiplied by 0): transient modifiers are not written to the player's save file, so a
 * crash, a kill -9 or a power cut in the middle of a ritual leaves nothing behind, and a normal logout or death clears them as well because the ritual
 * ends and calls {@link #release}. {@link #clean} removes a stray one at login for the one case left (a modified save). No effect and no potion is used,
 * so milk, totems and other mods' cures cannot lift it early and nothing shows in the effect bar. The lock holds the feet still; the ritual itself ends it
 * when the player strays, takes damage, dies, leaves or runs out of time.
 */
public final class RitualLock {
    private RitualLock() {}

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("nusmp", "soul_forge_lock");

    /** Freezes walking and jumping. Safe to call twice. */
    public static void apply(ServerPlayer p) {
        add(p, Attributes.MOVEMENT_SPEED);
        add(p, Attributes.JUMP_STRENGTH);
    }

    /** Gives walking and jumping back. Safe to call when there is no lock. */
    public static void release(ServerPlayer p) {
        remove(p, Attributes.MOVEMENT_SPEED);
        remove(p, Attributes.JUMP_STRENGTH);
    }

    /** At login: takes off a lock that should not exist (the ritual map is empty after a restart). */
    public static void clean(ServerPlayer p) { release(p); }

    public static boolean locked(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return a != null && a.hasModifier(ID);
    }

    private static void add(ServerPlayer p, Holder<Attribute> attribute) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst != null && !inst.hasModifier(ID)) inst.addTransientModifier(new AttributeModifier(ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void remove(ServerPlayer p, Holder<Attribute> attribute) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst != null) inst.removeModifier(ID);
    }
}
