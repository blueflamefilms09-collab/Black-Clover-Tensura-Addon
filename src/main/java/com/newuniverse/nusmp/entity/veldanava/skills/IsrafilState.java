package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the small pieces of server state and server acts that Israfil's skill and events share: the stored impact of Trial of Endurance and the attribute modifiers that follow from it, and the three
 * ways one creature is moved by another (pull, place, swap). All state is on the player's own data (so it is per player and goes with the player), nothing is global. Server thread only.
 */
public final class IsrafilState {
    private IsrafilState() {}

    public static final String STORE = "nusmp_trial_store", BLINK_AT = "nusmp_israfil_blink";
    static final ResourceLocation SPEED_ID = id("israfil_speed"), HEALTH_ID = id("israfil_trial_health"), TOUGHNESS_ID = id("israfil_trial_toughness");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    // ---------------------------------------------------------------- Trial of Endurance: the stored impact
    public static double store(LivingEntity e) { return e.getPersistentData().getDouble(STORE); }

    public static void setStore(LivingEntity e, double v) {
        CompoundTag d = e.getPersistentData();
        if (v <= 0.0) d.remove(STORE);
        else d.putDouble(STORE, Math.min(IsrafilRules.STORE_MAX, v));
    }

    /** Puts a modifier on an attribute, replacing the old one; an amount of 0 removes it. Only touches the attribute when the value changed. */
    public static void modifier(LivingEntity e, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = e.getAttribute(attribute);
        if (a == null) return;
        AttributeModifier cur = a.getModifier(id);
        if (amount == 0.0) {
            if (cur != null) a.removeModifier(id);
            return;
        }
        if (cur != null && cur.amount() == amount) return;
        if (cur != null) a.removeModifier(id);
        a.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** The gifts the store gives, applied (or taken away with a store of 0): extra maximum health and armour toughness. */
    public static void applyStore(ServerPlayer p, double store) {
        modifier(p, Attributes.MAX_HEALTH, HEALTH_ID, IsrafilRules.storeHealth(store), AttributeModifier.Operation.ADD_VALUE);
        modifier(p, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, IsrafilRules.storeToughness(store), AttributeModifier.Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Spatial Domination's speed. */
    public static void applySpeed(ServerPlayer p, boolean on) {
        modifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, on ? IsrafilRules.SPEED_BONUS : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Takes everything this skill put on the player's attributes and the store away. */
    public static void clear(ServerPlayer p) {
        applySpeed(p, false);
        setStore(p, 0.0);
        applyStore(p, 0.0);
    }

    // ---------------------------------------------------------------- moving creatures
    /** True when this creature may be pulled, swapped or thrown by another creature's ability. */
    public static boolean movable(LivingEntity e) { return !VeldanavaGuard.displacementImmune(e) && !e.isSpectator(); }

    /** Pulls a creature toward a point (a push of the given strength along the line; a boss is pulled half as hard). Does nothing for an immune one. */
    public static void pull(LivingEntity t, Vec3 toward, double strength) {
        if (!movable(t)) return;
        Vec3 d = toward.subtract(t.position());
        double len = d.length();
        if (len < 0.3) return;
        double s = com.newuniverse.nusmp.balance.BalanceLaw.isBoss(t) ? strength * 0.5 : strength;
        Vec3 v = d.scale(s / len);
        t.setDeltaMovement(v.x, Math.max(v.y, 0.15), v.z);
        t.hurtMarked = true;
    }

    /** Puts a creature at a place in the same level (a player with the teleport the game uses for commands, anything else with a plain move). */
    public static void place(Entity e, ServerLevel level, Vec3 at) {
        if (e instanceof ServerPlayer sp) sp.teleportTo(level, at.x, at.y, at.z, sp.getYRot(), sp.getXRot());
        else {
            e.teleportTo(at.x, at.y, at.z);
            e.setDeltaMovement(Vec3.ZERO);
        }
        e.fallDistance = 0.0f;
        e.hurtMarked = true;
    }
}
