package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.cast.VirtualSkillLoadout;
import com.newuniverse.nusmp.avatar.inventory.AvatarGear;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the weapon goals (melee, bow and crossbow, mage, shield) all need the same few answers from the world: what is the Avatar holding, which way does that
 * make it fight, how far does its personality want it to stand, may it chase this target from where it was told to stay, and how is an arrow actually loosed.
 * This is the one place that reads the entity and calls the pure {@link CombatPlanner}; the goals only do the walking, the drawing and the timing. Server side.
 */
public final class AvatarCombat {
    private AvatarCombat() {}

    /** How far from its post a staying Avatar will still go to fight (blocks). */
    public static final double STAY_LEASH = 8.0;

    // ---------------------------------------------------------------- the style
    /** Reads both hands and sets the Avatar's combat style; call when a hand changes and now and then. Returns the style. */
    public static CombatStyle refresh(AvatarEntity a) {
        CombatStyle style = decide(a);
        a.setCombatStyle(style);
        return style;
    }

    private static CombatStyle decide(AvatarEntity a) {
        if (a.level().isClientSide || !AvatarAiConfig.weaponAi()) return CombatStyle.MELEE;
        WeaponKind main = AvatarGear.kindOf(a.getMainHandItem());
        WeaponKind off = AvatarGear.kindOf(a.getOffhandItem());
        boolean shooter = main == WeaponKind.BOW || main == WeaponKind.CROSSBOW;
        boolean focus = main == WeaponKind.FOCUS || off == WeaponKind.FOCUS;
        return CombatPlanner.choose(new CombatPlanner.Facts(main, off, shooter && AvatarGear.hasAmmo(a), focus && VirtualSkillLoadout.canCastAnything(a)));
    }

    // ---------------------------------------------------------------- personality and distance
    public static boolean cowardly(AvatarEntity a) { return a.has(AvatarPersonality.COWARDLY); }

    public static boolean aggressive(AvatarEntity a) { return a.has(AvatarPersonality.AGGRESSIVE); }

    /** The distance band for a style, with the personality and the configured bow band applied. */
    public static CombatPlanner.Range range(AvatarEntity a, CombatStyle style) {
        return CombatPlanner.range(style, cowardly(a), aggressive(a), AvatarAiConfig.rangedMin(), AvatarAiConfig.rangedMax());
    }

    /** May the Avatar fight this target from where it is? Always, unless it was told to stay: then only a target within its leash of its post. */
    public static boolean mayEngage(AvatarEntity a, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (!a.staying()) return true;
        Vec3 post = a.stayPost();
        return post == null || target.position().distanceToSqr(post) <= STAY_LEASH * STAY_LEASH;
    }

    // ---------------------------------------------------------------- hands
    /** The hand holding something of this kind (the main hand first), or null. */
    public static InteractionHand handHolding(AvatarEntity a, WeaponKind kind) {
        if (AvatarGear.kindOf(a.getMainHandItem()) == kind) return InteractionHand.MAIN_HAND;
        if (AvatarGear.kindOf(a.getOffhandItem()) == kind) return InteractionHand.OFF_HAND;
        return null;
    }

    // ---------------------------------------------------------------- loosing arrows
    /**
     * Looses one arrow at the target, the way a skeleton does (a mob's arrow, aimed a little high for the drop), with the spread of its DEX score and the damage
     * of its body ({@link CombatPlanner#arrowDamageScale}). The arrow comes from {@link AvatarEntity#getProjectile}: free, or one from its pockets. False when it
     * holds no bow or has nothing to shoot.
     */
    public static boolean shootBow(AvatarEntity a, LivingEntity target, float power) {
        InteractionHand hand = handHolding(a, WeaponKind.BOW);
        if (hand == null || target == null) return false;
        ItemStack bow = a.getItemInHand(hand);
        ItemStack ammo = a.getProjectile(bow);
        if (ammo.isEmpty()) return false;
        AbstractArrow arrow = ProjectileUtil.getMobArrow(a, ammo, power, bow);
        arrow.setBaseDamage(arrow.getBaseDamage() * CombatPlanner.arrowDamageScale(a.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        double dx = target.getX() - a.getX();
        double dy = target.getY(1.0 / 3.0) - arrow.getY();
        double dz = target.getZ() - a.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.2, dz, 1.6f, CombatPlanner.inaccuracy(a.dndStat("DEX")));
        a.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (a.getRandom().nextFloat() * 0.4f + 0.8f));
        a.level().addFreshEntity(arrow);
        return true;
    }

    /** Fires a loaded crossbow at the target (the loading is the goal's job: it uses the item for the charge time and releases it). False when it is not loaded. */
    public static boolean shootCrossbow(AvatarEntity a, LivingEntity target) {
        InteractionHand hand = handHolding(a, WeaponKind.CROSSBOW);
        if (hand == null || target == null) return false;
        ItemStack crossbow = a.getItemInHand(hand);
        if (!CrossbowItem.isCharged(crossbow) || !(crossbow.getItem() instanceof CrossbowItem item)) return false;
        item.performShooting(a.level(), a, hand, crossbow, 1.6f, CombatPlanner.inaccuracy(a.dndStat("DEX")), target);
        return true;
    }
}
