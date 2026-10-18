package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobCategory;

import java.util.Locale;
import java.util.Map;

/**
 * Why: the catalogue speaks in short words ({@code strength}, {@code low_health}, {@code undead}, {@code lightning}, {@code max_health}, {@code WIND_SLASH}) and the game speaks in registry holders, tags
 * and damage types. Turning one into the other is the same small job for 51 executors, and a word that maps to the wrong thing (or to nothing, silently) would make a whole family of nodes do the wrong
 * thing, so the six tables live here once, each is checked against the catalogue's word list by a test, and an unknown word falls back to a harmless default instead of throwing in the middle of a
 * combat event. Everything is a static lookup over the game's own constants; there is no state.
 */
public final class LineLookups {
    private LineLookups() {}

    // ------------------------------------------------------------------ effects
    private static final Map<String, Holder<MobEffect>> EFFECTS = Map.ofEntries(
            Map.entry("speed", MobEffects.MOVEMENT_SPEED), Map.entry("slowness", MobEffects.MOVEMENT_SLOWDOWN), Map.entry("haste", MobEffects.DIG_SPEED),
            Map.entry("mining_fatigue", MobEffects.DIG_SLOWDOWN), Map.entry("strength", MobEffects.DAMAGE_BOOST), Map.entry("weakness", MobEffects.WEAKNESS),
            Map.entry("resistance", MobEffects.DAMAGE_RESISTANCE), Map.entry("regeneration", MobEffects.REGENERATION), Map.entry("fire_resistance", MobEffects.FIRE_RESISTANCE),
            Map.entry("water_breathing", MobEffects.WATER_BREATHING), Map.entry("invisibility", MobEffects.INVISIBILITY), Map.entry("night_vision", MobEffects.NIGHT_VISION),
            Map.entry("jump_boost", MobEffects.JUMP), Map.entry("absorption", MobEffects.ABSORPTION), Map.entry("luck", MobEffects.LUCK), Map.entry("poison", MobEffects.POISON),
            Map.entry("wither", MobEffects.WITHER), Map.entry("blindness", MobEffects.BLINDNESS), Map.entry("hunger", MobEffects.HUNGER), Map.entry("glowing", MobEffects.GLOWING),
            Map.entry("slow_falling", MobEffects.SLOW_FALLING), Map.entry("nausea", MobEffects.CONFUSION), Map.entry("levitation", MobEffects.LEVITATION),
            Map.entry("darkness", MobEffects.DARKNESS));

    /** The effect a catalogue word names, or null (also for {@code none} and anything unknown). */
    public static Holder<MobEffect> effect(String word) { return word == null ? null : EFFECTS.get(word); }

    /** Every effect word this table knows (a test compares it with the catalogue). */
    public static java.util.Set<String> effectWords() { return EFFECTS.keySet(); }

    /**
     * Gives a creature the effect a catalogue word names for {@code ticks} ticks at amplifier {@code amp}; false (nothing given) for {@code none}, an unknown word or a non-positive duration.
     * {@code particles} false keeps the swirl off (the passives refresh their effects every half second and must not flicker).
     */
    public static boolean give(LivingEntity target, String word, int ticks, int amp, boolean particles) {
        Holder<MobEffect> e = effect(word);
        if (e == null || target == null || ticks <= 0) return false;
        return target.addEffect(new net.minecraft.world.effect.MobEffectInstance(e, ticks, Math.max(0, Math.min(amp, 9)), !particles, particles, particles));
    }

    // ------------------------------------------------------------------ attributes
    private static final Map<String, Holder<Attribute>> ATTRIBUTES = Map.ofEntries(
            Map.entry("max_health", Attributes.MAX_HEALTH), Map.entry("armor", Attributes.ARMOR), Map.entry("armor_toughness", Attributes.ARMOR_TOUGHNESS),
            Map.entry("attack_damage", Attributes.ATTACK_DAMAGE), Map.entry("attack_speed", Attributes.ATTACK_SPEED), Map.entry("movement_speed", Attributes.MOVEMENT_SPEED),
            Map.entry("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE), Map.entry("luck", Attributes.LUCK), Map.entry("step_height", Attributes.STEP_HEIGHT),
            Map.entry("jump_strength", Attributes.JUMP_STRENGTH), Map.entry("safe_fall_distance", Attributes.SAFE_FALL_DISTANCE),
            Map.entry("block_interaction_range", Attributes.BLOCK_INTERACTION_RANGE), Map.entry("entity_interaction_range", Attributes.ENTITY_INTERACTION_RANGE));

    /** The attribute a catalogue word names, or null. */
    public static Holder<Attribute> attribute(String word) { return word == null ? null : ATTRIBUTES.get(word); }

    public static java.util.Set<String> attributeWords() { return ATTRIBUTES.keySet(); }

    // ------------------------------------------------------------------ effects of conditions
    /**
     * Does the condition of the catalogue hold for this player right now. An unknown word is false (a passive that cannot read its condition does nothing, which is the safe direction).
     * {@code in_combat} reads the stamp the damage events leave in the player's own data ({@link LineState#markCombat}).
     */
    public static boolean condition(String word, ServerPlayer p) {
        if (word == null) return false;
        ServerLevel level = p.serverLevel();
        return switch (word) {
            case "low_health" -> p.getHealth() <= p.getMaxHealth() * 0.35f;
            case "full_health" -> p.getHealth() >= p.getMaxHealth() - 0.01f;
            case "sprinting" -> p.isSprinting();
            case "sneaking" -> p.isShiftKeyDown();
            case "night" -> isNight(level);
            case "day" -> !isNight(level);
            case "in_water" -> p.isInWaterOrBubble();
            case "on_fire" -> p.isOnFire();
            case "airborne" -> !p.onGround() && !p.isInWaterOrBubble() && !p.onClimbable();
            case "in_combat" -> LineState.inCombat(p);
            case "holding_sword" -> p.getMainHandItem().is(ItemTags.SWORDS);
            case "unarmored" -> p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    && p.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && p.getItemBySlot(EquipmentSlot.FEET).isEmpty();
            case "underground" -> !level.canSeeSky(p.blockPosition()) && p.getY() < level.getSeaLevel();
            case "raining" -> level.isRainingAt(p.blockPosition());
            default -> false;
        };
    }

    private static boolean isNight(ServerLevel level) {
        long t = Math.floorMod(level.getDayTime(), 24000L);
        return t >= 13000L && t < 23000L;
    }

    // ------------------------------------------------------------------ what a creature is
    /** Is {@code target} of the catalogue's category. */
    public static boolean category(String word, LivingEntity target) {
        if (word == null || target == null) return false;
        return switch (word) {
            case "undead" -> target.getType().is(EntityTypeTags.UNDEAD);
            case "arthropod" -> target.getType().is(EntityTypeTags.ARTHROPOD);
            case "illager" -> target.getType().is(EntityTypeTags.ILLAGER);
            case "burning" -> target.isOnFire();
            case "boss" -> com.newuniverse.nusmp.balance.BalanceLaw.isBoss(target);
            case "monster" -> target.getType().getCategory() == MobCategory.MONSTER;
            case "animal" -> target instanceof Animal;
            case "player" -> target instanceof Player;
            default -> false;
        };
    }

    /** Is {@code src} damage of the catalogue's kind. */
    public static boolean damageKind(String word, DamageSource src) {
        if (word == null || src == null) return false;
        return switch (word) {
            case "fire" -> src.is(DamageTypeTags.IS_FIRE);
            case "magic" -> src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.SONIC_BOOM);
            case "explosion" -> src.is(DamageTypeTags.IS_EXPLOSION);
            case "projectile" -> src.is(DamageTypeTags.IS_PROJECTILE);
            case "fall" -> src.is(DamageTypeTags.IS_FALL);
            case "freeze" -> src.is(DamageTypeTags.IS_FREEZING);
            case "lightning" -> src.is(DamageTypeTags.IS_LIGHTNING);
            case "wither" -> src.is(DamageTypes.WITHER);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ effects of the vfx word
    /** The mod's effect shape a catalogue {@code vfx} word names, or {@code fallback}. */
    public static VfxShape shape(String word, VfxShape fallback) {
        if (word == null || word.isEmpty()) return fallback;
        try {
            return VfxShape.valueOf(word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
