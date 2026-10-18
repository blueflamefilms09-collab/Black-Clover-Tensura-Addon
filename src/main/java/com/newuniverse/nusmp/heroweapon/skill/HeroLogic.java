package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: what makes the Shield a shield and the Whip a whip is a handful of behaviours the shared machinery cannot know: its skills, what it does every few
 * ticks, what it adds to a blow, what it does when its wielder is hurt or raises a guard. Each of the 21 weapons has exactly ONE logic object (class
 * {@code heroweapon.weapon.<pkg>.<Kind>Logic}, found by {@link HeroKind#logicClassName}), written by one builder and registered by {@code HeroBootstrap}. The
 * shared machinery (the bond, the loan of the item, the forms, the curse, the restrictions, the dispatcher) calls these hooks on the server thread and never
 * lets an exception out of them. Every hook has a default that does nothing, so a weapon implements only what it uses. The context's {@code form} is the
 * ACTIVE form (never null); a form's passives are looked up with {@link HeroForm#has}; {@code c.stack} is the summoned weapon (it can be empty in
 * {@link #onDamaged} and {@link #onKill}, which also run while the weapon is stowed: check {@code c.summoned()}).
 */
public interface HeroLogic {
    /** The weapon this logic belongs to. */
    HeroKind kind();

    /** Every skill of this weapon (any form may list any of them). Ids start with the weapon's short name, e.g. {@code shield.air_strike}. */
    List<HeroSkill> skills();

    /** Every 5 ticks while the weapon is summoned (server). Keep it cheap: no world scans larger than a few blocks unless a skill is running. */
    default void onTick(HeroCtx c) {}

    /** After a melee blow of the wielder with the summoned weapon in hand. {@code fullSwing}: the attack bar was at 90% or more. */
    default void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {}

    /** The wielder is about to take {@code amount} damage; return the new amount (a Shield may cut it, a Mirror may reflect). Runs whether or not the weapon is summoned. */
    default float onDamaged(HeroCtx c, DamageSource source, float amount) { return amount; }

    /** The wielder's raised Shield (or any guarding weapon) meets {@code amount}; return what gets through. Only called while the style is BLOCK and the guard is up. */
    default float onBlocked(HeroCtx c, DamageSource source, float amount) { return amount; }

    /** The wielder killed {@code victim} (any means, summoned or not). */
    default void onKill(HeroCtx c, LivingEntity victim) {}

    /** A drawn / charged use was released after {@code chargeTicks} (styles DRAW, THROW, CAST). Return true when you handled it. */
    default boolean onRelease(HeroCtx c, int chargeTicks) { return false; }

    /** The wielder changed form (also called with {@code from == to} when a form is first learned as the active one). */
    default void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {}

    /** The weapon was summoned into the hand / put away. */
    default void onSummon(HeroCtx c) {}

    default void onDismiss(HeroCtx c) {}
}
