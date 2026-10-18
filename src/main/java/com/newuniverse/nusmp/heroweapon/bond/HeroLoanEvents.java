package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Why: the guards that keep a hero weapon "permanently bound to the user's body" (the owner's text): it cannot be tossed, it never becomes an item in the
 * world, it never drops on death, it cannot be hung in an item frame, put on an armour stand or given to an allay. The loan makes the weapon whole again every
 * four ticks; these listeners stop the things that would put it somewhere the loan cannot reach, and tell the loan to repair at once instead of waiting for the
 * beat. A toss that is cancelled has already taken the stack out of the slot, so the weapon is simply gone for a moment and the next tick makes it again. A
 * dying wielder's loan ends BEFORE the drops (so the displaced item is among them and the weapon is not); the bond stays. Creative players may toss nothing
 * either: no hero weapon ever exists as an item in the world, by anyone's hand.
 */
public final class HeroLoanEvents {
    private HeroLoanEvents() {}

    /** Registers the listeners on the game bus. */
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, HeroLoanEvents::onToss);
        bus.addListener(HeroLoanEvents::onJoin);
        bus.addListener(EventPriority.HIGHEST, HeroLoanEvents::onDrops);
        bus.addListener(EventPriority.HIGHEST, HeroLoanEvents::onDeath);
        bus.addListener(EventPriority.HIGH, HeroLoanEvents::onEntityInteract);
        bus.addListener(EventPriority.HIGH, HeroLoanEvents::onEntityInteractSpecific);
    }

    /** A toss of a hero weapon is cancelled; the loan makes the weapon again at the next tick. */
    private static void onToss(ItemTossEvent e) {
        if (HeroItems.kindOf(e.getEntity().getItem()) == null) return;
        e.setCanceled(true);
        HeroLoan.markDirty(e.getPlayer().getUUID());
    }

    /** No hero weapon ever becomes an item entity in the world (a dispenser, a hopper, a container spill, a death drop that got past the guard). */
    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof ItemEntity ie && HeroItems.kindOf(ie.getItem()) != null) e.setCanceled(true);
    }

    /** Nothing a dying creature drops is a hero weapon. */
    private static void onDrops(LivingDropsEvent e) {
        e.getDrops().removeIf(ie -> HeroItems.kindOf(ie.getItem()) != null);
    }

    /** A dying wielder: the loan ends before the drops are made. */
    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SafeCast.tick("hero.death", () -> HeroLoan.onDeath(p));
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (refuses(e.getItemStack(), e.getTarget(), e.getEntity())) e.setCanceled(true);
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific e) {
        if (refuses(e.getItemStack(), e.getTarget(), e.getEntity())) e.setCanceled(true);
    }

    /** Would this use of a hero weapon on that entity hang it up, dress it or hand it over? */
    private static boolean refuses(ItemStack held, Entity target, Player who) {
        if (HeroItems.kindOf(held) == null || who.isCreative()) return false;
        return target instanceof ItemFrame || target instanceof ArmorStand || target instanceof Allay;
    }
}
