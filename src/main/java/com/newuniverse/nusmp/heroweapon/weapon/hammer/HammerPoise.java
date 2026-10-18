package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Hammer's blows build poise on a foe and the last point breaks it. The gauge arithmetic is {@link PoiseMath}; this is the part that touches the game: it keeps the
 * four numbers of a foe's gauge in the foe's own saved data (so nothing is left in a static table when the foe unloads or dies, and the gauge recovers by the clock), and it
 * carries out a break: the foe is slowed and weakened and takes more damage for a moment, and, if the wielder's shared control lock allows it, rooted for as long as the balance
 * law allows (3 seconds on a player, 5 on a mob). A boss is only staggered for a second. The armour-cracking helper used by Armor Crack and Crushing Weight lives here too.
 */
final class HammerPoise {
    private HammerPoise() {}

    private static final String VALUE = "nusmp_hv_pz", STAMP = "nusmp_hv_pt", BROKEN = "nusmp_hv_pb", LOCK = "nusmp_hv_pl";
    private static final ResourceLocation CRACK_ARMOR = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_hammer_crack");
    private static final ResourceLocation CRACK_TOUGHNESS = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_hammer_crack_t");
    /** The amber of the gem, used for every effect of the weapon. */
    static final int AMBER = 0xFF9A2E;

    static PoiseMath.State read(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        if (!d.contains(STAMP)) return PoiseMath.State.EMPTY;
        return new PoiseMath.State(d.getFloat(VALUE), d.getLong(STAMP), d.getLong(BROKEN), d.getLong(LOCK));
    }

    private static void write(LivingEntity t, PoiseMath.State s) {
        CompoundTag d = t.getPersistentData();
        d.putFloat(VALUE, s.value());
        d.putLong(STAMP, s.stamp());
        d.putLong(BROKEN, s.brokenUntil());
        d.putLong(LOCK, s.lockUntil());
    }

    /** Adds {@code amount} points of poise damage to a foe; returns true when that broke it. */
    static boolean add(HeroCtx c, LivingEntity t, float amount) {
        if (t == null || !t.isAlive() || amount <= 0f) return false;
        boolean boss = BalanceLaw.isBoss(t);
        float capacity = PoiseMath.capacity(t.getMaxHealth(), boss);
        PoiseMath.Result r = PoiseMath.add(read(t), amount, c.level.getGameTime(), capacity, boss);
        write(t, r.state());
        if (r.broke()) shatter(c, t, boss);
        return r.broke();
    }

    /** True while the foe is broken. */
    static boolean broken(LivingEntity t) { return PoiseMath.broken(read(t), t.level().getGameTime()); }

    private static void shatter(HeroCtx c, LivingEntity t, boolean boss) {
        Vec3 at = t.position();
        if (boss) {
            Strike.effect(t, "minecraft:slowness", PoiseMath.BREAK_TICKS_BOSS, 1);
        } else {
            Strike.effect(t, "minecraft:slowness", PoiseMath.BREAK_TICKS, 3);
            Strike.effect(t, "minecraft:weakness", HammerNumbers.BREAK_WEAKNESS_TICKS, 0);
            Strike.effect(t, "tensura:fragility", HammerNumbers.BREAK_FRAGILITY_TICKS, 0);
            if (c.control()) {
                int root = BalanceLaw.controlTicks(t, HammerNumbers.BREAK_ROOT_BASE);
                Strike.effect(t, "minecraft:slowness", root, 6);
                Strike.effect(t, "tensura:movement_interference", root, 0);
            }
        }
        StrikeFx.dustColumn(c.level, at, AMBER, boss ? 1.4 : 0.9);
        StrikeFx.sound(c.level, at, SoundEvents.ANVIL_LAND, 0.9f, 1.3f);
        StrikeFx.sound(c.level, at, SoundEvents.SHIELD_BREAK, 0.6f, 0.8f);
        c.say(t.getName().getString() + " is broken!");
    }

    /** Takes a share off a foe's armour and toughness for a while (the mod's own timed modifier: it removes itself, nothing is saved). */
    static void crackArmor(LivingEntity t, double armorShare, double toughnessShare, int ticks) {
        TimedModifiers.apply(t, Attributes.ARMOR, CRACK_ARMOR, armorShare, ticks, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (toughnessShare != 0.0) TimedModifiers.apply(t, Attributes.ARMOR_TOUGHNESS, CRACK_TOUGHNESS, toughnessShare, ticks, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
