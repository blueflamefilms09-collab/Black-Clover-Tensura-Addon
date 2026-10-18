package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Why: the owner's two restrictions that act on what a player DOES (a Holy wielder fighting with another class of weapon; two Holy wielders gaining experience
 * near each other) are enforced by listening to the game, not by changing the other items. These are those listeners, thin on purpose: each reads the event,
 * asks {@link HeroRules} or {@link HeroInterference} the question, and cancels or zeroes. Rejection: a blow ({@code AttackEntityEvent}), a physical hit that
 * reached the damage step by another route (a mod item's right-click technique), the start of a draw or throw, the loosing of an arrow, and the right-click
 * that would fire a loaded crossbow or throw a trident. Experience: an orb picked up, and the orbs a kill would drop. The shock's own damage gets its cap here
 * too, from the last damage listener to run, so no other mod's multiplier can turn it into a death. Every handler runs inside {@link SafeCast#tick} so a fault
 * is logged once and never reaches the game, and every one ignores the client's copy of the player.
 */
public final class HeroRuleEvents {
    private HeroRuleEvents() {}

    /** Registers the listeners on the game bus. */
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onAttack);
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onPhysicalDamage);
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onUseStart);
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onArrow);
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onRightClick);
        bus.addListener(EventPriority.LOWEST, HeroRuleEvents::onShockDamage);
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onPickupXp);
        bus.addListener(EventPriority.HIGH, HeroRuleEvents::onKillXp);
    }

    // ================================================================ restriction one: rejection
    /** A blow with an item of another class. */
    private static void onAttack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SafeCast.tick("hero.rule.attack", () -> {
            if (HeroRules.rejectedClass(p, p.getMainHandItem()) == WeaponClass.NONE) return;
            e.setCanceled(true);
            HeroRules.shock(p);
        });
    }

    /** A physical blow that reached the damage step without an attack event (a mod item's technique): the same rule, on the item in the main hand. */
    private static void onPhysicalDamage(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK)) return;
        SafeCast.tick("hero.rule.damage", () -> {
            if (HeroRules.rejectedClass(p, p.getMainHandItem()) == WeaponClass.NONE) return;
            e.setCanceled(true);
            HeroRules.shock(p);
        });
    }

    /** Starting to draw a bow or a crossbow, or to wind up a trident, with a class that is not the wielder's. */
    private static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SafeCast.tick("hero.rule.use", () -> {
            if (!fired(HeroRules.rejectedClass(p, e.getItem()))) return;
            e.setCanceled(true);
            HeroRules.shock(p);
        });
    }

    /** Letting go of a string drawn before the bond was made. */
    private static void onArrow(ArrowLooseEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SafeCast.tick("hero.rule.arrow", () -> {
            if (!fired(HeroRules.rejectedClass(p, e.getBow()))) return;
            e.setCanceled(true);
            HeroRules.shock(p);
        });
    }

    /** A right-click with a fired class of weapon (a loaded crossbow shoots at once, without a draw). */
    private static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SafeCast.tick("hero.rule.click", () -> {
            ItemStack held = e.getItemStack();
            if (!fired(HeroRules.rejectedClass(p, held))) return;
            e.setCanceled(true);
            HeroRules.shock(p);
        });
    }

    private static boolean fired(WeaponClass c) { return c != WeaponClass.NONE && WeaponClassifier.firesWhenUsed(c); }

    /**
     * The last word on the shock's damage (this listener runs after every other mod's): whatever was done to the amount on its way, the shock leaves the wielder
     * at half a heart at least. It reads the health, not the absorption, so it is the cautious side.
     */
    private static void onShockDamage(LivingDamageEvent.Pre e) {
        if (!HeroRules.isShocking(e.getEntity())) return;
        SafeCast.tick("hero.rule.shock", () -> e.setNewDamage(ShockRules.damage(e.getNewDamage(), e.getEntity().getHealth())));
    }

    // ================================================================ restriction three: experience
    /** An experience orb reaches a blocked wielder: it stays where it is. */
    private static void onPickupXp(PlayerXpEvent.PickupXp e) {
        Player who = e.getEntity();
        if (!(who instanceof ServerPlayer p) || !HeroInterference.blocked(p)) return;
        e.setCanceled(true);
        HeroInterference.noteRepelled(p);
    }

    /** A kill by a blocked wielder drops no experience. */
    private static void onKillXp(LivingExperienceDropEvent e) {
        Player killer = e.getAttackingPlayer();
        if (!(killer instanceof ServerPlayer p) || e.getDroppedExperience() <= 0 || !HeroInterference.blocked(p)) return;
        e.setDroppedExperience(0);
        HeroInterference.noteRepelled(p);
    }
}
