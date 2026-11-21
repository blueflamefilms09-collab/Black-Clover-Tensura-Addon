package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Why (4.89.4): vanilla tries the main hand first and stops at the first hand that answers, and a weapon of the mod in the main hand answers every use key (a technique, a guard, a draw),
 * so a hold-and-use item in the off hand (the Hourglass of the Final Hour) never got its turn. When {@link UtilityItems#mainYields} says so, the main-hand click is cancelled with PASS,
 * which makes the game go on to the off hand. The decision reads only the two stacks, the sneak state and the cooldown (all of which the client has too), so it is the same on both sides and
 * nothing is predicted wrongly. Not run for the off-hand event, nor while sneaking, nor when the setting {@code utilityItemsAlwaysWork} is off. Better Combat answers EMPTY for the off hand
 * behind a two-handed weapon (see BcHands), so there is nothing to hand the click to and nothing changes there. Cost: two tag reads per right-click, no allocation, no packet.
 */
public final class UtilityYieldEvents {
    private UtilityYieldEvents() {}

    /** Registers the listener on the game bus (before the weapons' own handlers, which run at HIGH and below). */
    public static void register(IEventBus bus) { bus.addListener(EventPriority.HIGHEST, UtilityYieldEvents::onRightClick); }

    private static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !HeroConfig.utilityItemsAlwaysWork()) return;
        Player p = e.getEntity();
        ItemStack main = e.getItemStack(), off = p.getOffhandItem();
        if (off.isEmpty() || main.isEmpty()) return;
        boolean offUtility = HeroRules.utilityTagged(off) || (HeroRules.utility(off) && !HeroRules.weaponLike(off));
        if (!UtilityItems.mainYields(true, p.isShiftKeyDown(), HeroRules.weaponLike(main), HeroRules.utility(main), offUtility, !p.getCooldowns().isOnCooldown(off.getItem()))) return;
        e.setCancellationResult(InteractionResult.PASS);
        e.setCanceled(true);
    }
}
