package com.newuniverse.nusmp.celestial;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: a Star Dress is the player taking on the power of a spirit instead of calling it: for a minute the spirit's gift is worn as attribute modifiers (speed, attack,
 * armour, health, from the {@link CelestialKey} table) and one signature effect. The modifiers are transient (they never reach a save) and are taken off when the minute
 * ends, when the player logs out and when another dress is put on. The numbers come from the table; the rule for when a dress opens is
 * {@link CelestialRules#starDressUnlocked}.
 */
public final class CelestialDress {
    private CelestialDress() {}

    static final ResourceLocation SPEED = id("star_dress_speed"), ATTACK = id("star_dress_attack"), ARMOR = id("star_dress_armor"), HEALTH = id("star_dress_health");

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    /** Puts the dress of {@code key} on the player until {@code until} (game time). Any dress already worn is replaced. */
    public static void apply(ServerPlayer p, CelestialKey key, long until) {
        clear(p);
        if (key.dressSpeed != 0) add(p, Attributes.MOVEMENT_SPEED, SPEED, key.dressSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (key.dressAttack != 0) add(p, Attributes.ATTACK_DAMAGE, ATTACK, key.dressAttack, AttributeModifier.Operation.ADD_VALUE);
        if (key.dressArmor != 0) add(p, Attributes.ARMOR, ARMOR, key.dressArmor, AttributeModifier.Operation.ADD_VALUE);
        if (key.dressHealth != 0) add(p, Attributes.MAX_HEALTH, HEALTH, key.dressHealth, AttributeModifier.Operation.ADD_VALUE);
        int ticks = (int) Math.max(20, until - p.level().getGameTime());
        for (MobEffectInstance e : effects(key, ticks)) p.addEffect(e);
        CelestialAttachments.of(p).setDress(key.ordinal(), until);
    }

    /** Takes the dress off (no-op when none is worn). */
    public static void clear(ServerPlayer p) {
        remove(p, Attributes.MOVEMENT_SPEED, SPEED);
        remove(p, Attributes.ATTACK_DAMAGE, ATTACK);
        remove(p, Attributes.ARMOR, ARMOR);
        remove(p, Attributes.MAX_HEALTH, HEALTH);
        CelestialAttachments.of(p).setDress(-1, 0);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** True while a dress is worn. */
    public static boolean worn(ServerPlayer p) { return CelestialAttachments.of(p).dressActive(p.level().getGameTime()); }

    /** Once a second: takes the dress off when its minute is over. */
    public static void tick(ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        if (d.dressKey() >= 0 && p.level().getGameTime() >= d.dressUntil()) clear(p);
        else if (d.dressKey() < 0 && p.getAttribute(Attributes.MOVEMENT_SPEED) != null && p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(SPEED)) clear(p);   // a stray modifier
    }

    /** The one signature effect of each dress (the gifts of the numbers are in the table). */
    static MobEffectInstance[] effects(CelestialKey key, int ticks) {
        return switch (key) {
            case AQUARIUS -> new MobEffectInstance[]{e(MobEffects.DOLPHINS_GRACE, ticks, 0), e(MobEffects.WATER_BREATHING, ticks, 0)};
            case TAURUS -> new MobEffectInstance[]{e(MobEffects.DAMAGE_BOOST, ticks, 1)};
            case LEO -> new MobEffectInstance[]{e(MobEffects.NIGHT_VISION, ticks, 0), e(MobEffects.DAMAGE_BOOST, ticks, 0)};
            case VIRGO -> new MobEffectInstance[]{e(MobEffects.DIG_SPEED, ticks, 1)};
            case SAGITTARIUS -> new MobEffectInstance[]{e(MobEffects.JUMP, ticks, 1), e(MobEffects.MOVEMENT_SPEED, ticks, 0)};
            case GEMINI -> new MobEffectInstance[]{e(MobEffects.INVISIBILITY, Math.min(ticks, 200), 0), e(MobEffects.MOVEMENT_SPEED, ticks, 0)};
            case CANCER -> new MobEffectInstance[]{e(MobEffects.DIG_SPEED, ticks, 0), e(MobEffects.DAMAGE_BOOST, ticks, 0)};
            case SCORPIO -> new MobEffectInstance[]{e(MobEffects.FIRE_RESISTANCE, ticks, 0), e(MobEffects.DAMAGE_BOOST, ticks, 0)};
            case CAPRICORN -> new MobEffectInstance[]{e(MobEffects.DAMAGE_RESISTANCE, ticks, 0)};
            case PISCES -> new MobEffectInstance[]{e(MobEffects.REGENERATION, ticks, 0), e(MobEffects.WATER_BREATHING, ticks, 0)};
            case ARIES -> new MobEffectInstance[]{e(MobEffects.ABSORPTION, ticks, 2), e(MobEffects.SLOW_FALLING, ticks, 0)};
            case LIBRA -> new MobEffectInstance[]{e(MobEffects.SLOW_FALLING, ticks, 0), e(MobEffects.JUMP, ticks, 2)};
            default -> new MobEffectInstance[0];
        };
    }

    private static MobEffectInstance e(Holder<MobEffect> eff, int ticks, int amp) { return new MobEffectInstance(eff, ticks, amp, false, true, true); }

    private static void add(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void remove(ServerPlayer p, Holder<Attribute> a, ResourceLocation id) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst != null) inst.removeModifier(id);
    }
}
