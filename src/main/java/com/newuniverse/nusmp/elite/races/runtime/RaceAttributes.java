package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.state.StateModifier;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Why: the race files name attributes by short key; this is the one table that turns a key into the real attribute holder, written out in full (not looked up by string) so a
 * renamed Tensura or vanilla attribute fails the compile instead of silently dropping a bonus. The holders are fetched lazily, on first use, because Tensura's are only bound after
 * its own registration. The pure list {@code data.RaceAttributeKeys.KNOWN} must name exactly these keys; the self-test {@code elite.races.templates} proves it.
 */
public final class RaceAttributes {
    private RaceAttributes() {}

    private static final Map<String, Supplier<Holder<Attribute>>> TABLE = new LinkedHashMap<>();

    private static void put(String key, Supplier<Holder<Attribute>> holder) { TABLE.put(key, holder); }

    static {
        put("max_health", () -> Attributes.MAX_HEALTH);
        put("attack_damage", () -> Attributes.ATTACK_DAMAGE);
        put("attack_speed", () -> Attributes.ATTACK_SPEED);
        put("movement_speed", () -> Attributes.MOVEMENT_SPEED);
        put("armor", () -> Attributes.ARMOR);
        put("armor_toughness", () -> Attributes.ARMOR_TOUGHNESS);
        put("knockback_resistance", () -> Attributes.KNOCKBACK_RESISTANCE);
        put("jump_strength", () -> Attributes.JUMP_STRENGTH);
        put("step_height", () -> Attributes.STEP_HEIGHT);
        put("scale", () -> Attributes.SCALE);
        put("luck", () -> Attributes.LUCK);
        put("safe_fall_distance", () -> Attributes.SAFE_FALL_DISTANCE);
        put("fall_damage_multiplier", () -> Attributes.FALL_DAMAGE_MULTIPLIER);
        put("entity_interaction_range", () -> Attributes.ENTITY_INTERACTION_RANGE);
        put("block_interaction_range", () -> Attributes.BLOCK_INTERACTION_RANGE);
        put("attack_knockback", () -> Attributes.ATTACK_KNOCKBACK);
        put("tensura:max_aura", () -> TensuraAttributes.MAX_AURA);
        put("tensura:max_magicule", () -> TensuraAttributes.MAX_MAGICULE);
        put("tensura:aura_regeneration_multiplier", () -> TensuraAttributes.AURA_REGENERATION_MULTIPLIER);
        put("tensura:magicule_regeneration_multiplier", () -> TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        put("tensura:max_spiritual_health", () -> TensuraAttributes.MAX_SPIRITUAL_HEALTH);
        put("tensura:spiritual_health_regeneration", () -> TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION);
        put("tensura:magic_resistance", () -> TensuraAttributes.MAGIC_RESISTANCE);
        put("tensura:flame_resistance", () -> TensuraAttributes.FLAME_RESISTANCE);
        put("tensura:water_resistance", () -> TensuraAttributes.WATER_RESISTANCE);
        put("tensura:wind_resistance", () -> TensuraAttributes.WIND_RESISTANCE);
        put("tensura:earth_resistance", () -> TensuraAttributes.EARTH_RESISTANCE);
        put("tensura:light_resistance", () -> TensuraAttributes.LIGHT_RESISTANCE);
        put("tensura:darkness_resistance", () -> TensuraAttributes.DARKNESS_RESISTANCE);
        put("tensura:space_resistance", () -> TensuraAttributes.SPACE_RESISTANCE);
        put("tensura:ability_mastery_gain", () -> TensuraAttributes.ABILITY_MASTERY_GAIN);
        put("tensura:dodge_strength", () -> TensuraAttributes.DODGE_STRENGTH);
        put("tensura:auto_melee_dodge_chance", () -> TensuraAttributes.AUTO_MELEE_DODGE_CHANCE);
        put("tensura:presence_sense_radius", () -> TensuraAttributes.PRESENCE_SENSE_RADIUS);
    }

    /** The attribute for a key, or null when the key is unknown or the attribute is not bound yet. */
    public static Holder<Attribute> get(String key) {
        Supplier<Holder<Attribute>> s = TABLE.get(key);
        if (s == null) return null;
        try { return s.get(); } catch (RuntimeException | LinkageError notBound) { return null; }
    }

    public static Set<String> keys() { return TABLE.keySet(); }

    public static Operation operation(StateModifier.Op op) {
        return switch (op) {
            case ADD -> Operation.ADD_VALUE;
            case MULT_BASE -> Operation.ADD_MULTIPLIED_BASE;
            case MULT_TOTAL -> Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
