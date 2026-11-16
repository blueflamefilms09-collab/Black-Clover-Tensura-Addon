package com.newuniverse.nusmp.saiyan;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: the few racial bonuses that are real attribute modifiers (3.63.0), applied and removed in ONE place so cleanup can never miss one. Strength and durability are NOT attributes (they are the damage
 * calculations in {@link SaiyanManager}); speed, attack speed, jump and knockback resistance are small modifiers cut by the hard caps of the config, and the Ki capacity is a modifier on Tensura's maximum
 * magicule, because Ki is the magicule pool. All are TRANSIENT (never saved: a crash can not leave a permanent one), each has a fixed id {@code anime_multiverse:saiyan/<name>} so applying again replaces instead of
 * stacking, and {@link #clear} removes every one (death, logout, race removal).
 */
public final class SaiyanAttributes {
    private SaiyanAttributes() {}

    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", "saiyan/" + name); }

    private static final ResourceLocation SPEED = id("speed"), ATTACK_SPEED = id("attack_speed"), JUMP = id("jump"), KNOCKBACK = id("knockback"), KI_CAPACITY = id("ki_capacity"), KI_BASE = id("ki_base"),
            FORM_SPEED = id("form_speed"), FORM_ATTACK_SPEED = id("form_attack_speed"),
            TECH_SPEED = id("tech_speed"), TECH_ATTACK_SPEED = id("tech_attack_speed"), TECH_JUMP = id("tech_jump"),
            RACE_ATTACK = id("race_attack"), RACE_SPEED = id("race_speed"), RACE_HEALTH = id("race_health"), RACE_ARMOR = id("race_armor"), RACE_KNOCKBACK = id("race_knockback");

    /** Dragon Ball rework: the timed technique modifiers (Combat Instinct speed and attack speed, the gravity slowdown as negative speed and jump). Replaces the last one; transient, removed by {@link #clear}. */
    public static void applyTech(ServerPlayer p, double speed, double attackSpeed, double jump) {
        set(p, Attributes.MOVEMENT_SPEED, TECH_SPEED, Math.max(-0.9, Math.min(1.0, speed)), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.ATTACK_SPEED, TECH_ATTACK_SPEED, Math.max(-0.9, Math.min(1.0, attackSpeed)), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.JUMP_STRENGTH, TECH_JUMP, Math.max(-0.9, Math.min(1.0, jump)), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** Dragon Ball rework (phase 2): the small buff of the race stage ({@link SaiyanRaceRules#bonus}); all zero takes it off. */
    public static void applyRace(ServerPlayer p, SaiyanRaceRules.Bonus b) {
        set(p, Attributes.ATTACK_DAMAGE, RACE_ATTACK, b.attack(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.MOVEMENT_SPEED, RACE_SPEED, b.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.MAX_HEALTH, RACE_HEALTH, b.health(), AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.ARMOR, RACE_ARMOR, b.armor(), AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, RACE_KNOCKBACK, b.knockback(), AttributeModifier.Operation.ADD_VALUE);
    }

    /** A form's movement and attack-speed change (3.65.0; negative = slower, as Super Saiyan 3). Replaces the last one; both are transient and removed by {@link #clear}. */
    public static void applyForm(ServerPlayer p, double speed, double attackSpeed) {
        set(p, Attributes.MOVEMENT_SPEED, FORM_SPEED, Math.max(-0.9, Math.min(1.0, speed)), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.ATTACK_SPEED, FORM_ATTACK_SPEED, Math.max(-0.9, Math.min(1.0, attackSpeed)), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** Takes a form's modifiers off. */
    public static void clearForm(ServerPlayer p) { applyForm(p, 0.0, 0.0); }

    /** Puts the modifiers for these bonuses on the player, replacing the old ones only where the value changed. {@code kiBase} is the flat Ki every Saiyan has (the spec's "Ki capacity 100"). */
    public static void apply(ServerPlayer p, SaiyanRules.Table t, double kiBase) {
        set(p, Attributes.MOVEMENT_SPEED, SPEED, t.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.ATTACK_SPEED, ATTACK_SPEED, t.attackSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.JUMP_STRENGTH, JUMP, t.jump(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, t.knockback(), AttributeModifier.Operation.ADD_VALUE);
        set(p, TensuraAttributes.MAX_MAGICULE, KI_BASE, kiBase, AttributeModifier.Operation.ADD_VALUE);
        set(p, TensuraAttributes.MAX_MAGICULE, KI_CAPACITY, t.kiCapacity(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Removes every modifier of the system. */
    public static void clear(ServerPlayer p) {
        set(p, Attributes.MOVEMENT_SPEED, SPEED, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.ATTACK_SPEED, ATTACK_SPEED, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.JUMP_STRENGTH, JUMP, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, 0.0, AttributeModifier.Operation.ADD_VALUE);
        set(p, TensuraAttributes.MAX_MAGICULE, KI_BASE, 0.0, AttributeModifier.Operation.ADD_VALUE);
        set(p, TensuraAttributes.MAX_MAGICULE, KI_CAPACITY, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        clearForm(p);
        applyTech(p, 0.0, 0.0, 0.0);
        applyRace(p, SaiyanRaceRules.bonus(SaiyanRaceRules.NONE, 0.0));
    }

    /** True while any modifier of the system is on the player (the self-test reads it). */
    public static boolean any(ServerPlayer p) {
        return has(p, Attributes.MOVEMENT_SPEED, FORM_SPEED) || has(p, Attributes.ATTACK_SPEED, FORM_ATTACK_SPEED)
                || has(p, Attributes.MOVEMENT_SPEED, SPEED) || has(p, Attributes.ATTACK_SPEED, ATTACK_SPEED) || has(p, Attributes.JUMP_STRENGTH, JUMP)
                || has(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK) || has(p, TensuraAttributes.MAX_MAGICULE, KI_CAPACITY) || has(p, TensuraAttributes.MAX_MAGICULE, KI_BASE)
                || has(p, Attributes.MOVEMENT_SPEED, TECH_SPEED) || has(p, Attributes.ATTACK_SPEED, TECH_ATTACK_SPEED) || has(p, Attributes.JUMP_STRENGTH, TECH_JUMP)
                || has(p, Attributes.ATTACK_DAMAGE, RACE_ATTACK) || has(p, Attributes.MOVEMENT_SPEED, RACE_SPEED) || has(p, Attributes.MAX_HEALTH, RACE_HEALTH)
                || has(p, Attributes.ARMOR, RACE_ARMOR) || has(p, Attributes.KNOCKBACK_RESISTANCE, RACE_KNOCKBACK);
    }

    private static boolean has(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance inst = p.getAttribute(attribute);
        return inst != null && inst.getModifier(id) != null;
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier now = inst.getModifier(id);
        if (!(Math.abs(amount) > 1.0e-6)) {
            if (now != null) inst.removeModifier(id);
            return;
        }
        if (now != null && now.amount() == amount && now.operation() == op) return;
        if (now != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
