package com.newuniverse.nusmp.heroweapon.weapon.axe;

import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: Brawn ({@link BrawnMath}) is the Axe's muscle enhancement, and it has to live somewhere the game can read it every tick without a table: two numbers in the wielder's own
 * saved data (how many stacks, and the tick of the last gain), read lazily so the fading needs no timer. This class reads and writes them, grants stacks (only to a form with the
 * Muscle Enhancement passive, which every form has), spends them for Shine Axe Burst, and every few ticks puts what the stacks give on the wielder's body: more melee damage, more
 * knockback resistance and a little speed, through the mod's timed modifiers, which take themselves off two seconds after the last refresh, so putting the axe away, dying or
 * leaving leaves nothing behind.
 */
final class AxeBrawn {
    private AxeBrawn() {}

    private static final String STACKS = "nusmp_ax_bn", STAMP = "nusmp_ax_bt";
    private static final ResourceLocation MELEE = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_axe_brawn_melee");
    private static final ResourceLocation KNOCK = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_axe_brawn_knock");
    private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_axe_brawn_speed");
    /** The green of the gem, used for every effect of the weapon. */
    static final int GREEN = 0x35E07C;

    private static boolean slowFade(HeroCtx c) { return c.form.has(AxeNumbers.RADIANCE); }

    /** The stacks the wielder has right now. */
    static int stacks(HeroCtx c) {
        CompoundTag d = c.player.getPersistentData();
        return BrawnMath.current(d.getInt(STACKS), d.getLong(STAMP), c.level.getGameTime(), slowFade(c));
    }

    /** Grants stacks; says so when the wielder first reaches the five that Shine Axe Burst needs and when they reach the top. */
    static void gain(HeroCtx c, int add) {
        if (add <= 0 || !c.form.has(AxeNumbers.MUSCLE)) return;
        CompoundTag d = c.player.getPersistentData();
        long now = c.level.getGameTime();
        int before = stacks(c);
        int after = BrawnMath.gain(d.getInt(STACKS), d.getLong(STAMP), now, add, slowFade(c));
        d.putInt(STACKS, after);
        d.putLong(STAMP, now);
        if (after >= BrawnMath.SHINE_MIN && before < BrawnMath.SHINE_MIN) c.say("Brawn " + after + ": Shine Axe Burst is ready.");
        else if (after >= BrawnMath.MAX && before < BrawnMath.MAX) c.say("Brawn at its peak.");
    }

    /** Spends every stack. */
    static void clear(HeroCtx c) {
        CompoundTag d = c.player.getPersistentData();
        d.putInt(STACKS, 0);
        d.putLong(STAMP, c.level.getGameTime());
    }

    /** Puts what the stacks give on the wielder's body for the next two seconds (called every few ticks while the axe is out). */
    static void apply(HeroCtx c) {
        int s = stacks(c);
        if (s <= 0) return;
        TimedModifiers.apply(c.player, Attributes.ATTACK_DAMAGE, MELEE, BrawnMath.meleeBonus(s), 40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        TimedModifiers.apply(c.player, Attributes.KNOCKBACK_RESISTANCE, KNOCK, BrawnMath.knockbackResistance(s), 40, AttributeModifier.Operation.ADD_VALUE);
        TimedModifiers.apply(c.player, Attributes.MOVEMENT_SPEED, SPEED, BrawnMath.speedBonus(s), 40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
