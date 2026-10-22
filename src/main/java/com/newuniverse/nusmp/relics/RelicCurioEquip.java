package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Right-click puts a Curios relic on. A relic of the neck, back, belt, hands or charm slot used to reach the body only through the Curios screen; right-clicking it fired its first ability
 * (or showed the attunement hint). Curios' own right-click equip belongs to its item interface, which the mod cannot compile against (its maven is out of reach, the mod reads Curios by
 * reflection, see {@link CuriosLink}), so the mod does it itself through the same public inventory: the first free place of the relic's slot type gets one copy, the stack in the hand
 * shrinks, the wearer is told. Rules ({@link CurioEquipRules}): not while sneaking (sneak-use stays the attunement ritual and the second ability); only a relic whose slot is a Curios slot;
 * when the slot has no free place, or Curios is absent, nothing happens and the old right-click behaviour runs. The listener has a LOW priority and ignores an empty stack, so if Curios
 * (or any other mod) already took the item on that click, nothing is done twice. Server only; one inventory read per right-click of a Curios relic, nothing per tick.
 */
public final class RelicCurioEquip {
    private RelicCurioEquip() {}

    /** Game bus. */
    public static void register(IEventBus gameBus) { gameBus.addListener(EventPriority.LOW, RelicCurioEquip::onRightClick); }

    static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack inHand = p.getItemInHand(e.getHand());
        RelicDef def = Relics.defOf(inHand);
        boolean curio = def != null && def.slot().isCurio();
        if (!CurioEquipRules.tryOnUse(curio, p.isShiftKeyDown(), inHand.isEmpty())) return;
        if (tryEquip(p, e.getHand()) != CurioEquipRules.Result.EQUIPPED) return;      // nothing fits: the relic's own right-click runs as it always did
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    /** The places of every slot type the player has, from the Curios inventory (a free place is an empty stack). */
    static List<CurioEquipRules.Slot> slotsOf(Map<String, IItemHandler> handlers) {
        List<CurioEquipRules.Slot> out = new ArrayList<>(handlers.size());
        for (Map.Entry<String, IItemHandler> en : handlers.entrySet()) {
            IItemHandler h = en.getValue();
            List<Boolean> free = new ArrayList<>(h.getSlots());
            for (int i = 0; i < h.getSlots(); i++) free.add(h.getStackInSlot(i).isEmpty());
            out.add(new CurioEquipRules.Slot(en.getKey(), free));
        }
        return out;
    }

    /**
     * Puts the relic in {@code hand} on, if a free place of its slot type exists. Returns what happened. On success one copy sits in the Curios slot, the stack in the hand has shrunk by one,
     * the cached looks at the player are dropped (so the worn layer and the relic effects see it at once) and the player is told in the action bar.
     */
    public static CurioEquipRules.Result tryEquip(ServerPlayer p, InteractionHand hand) {
        ItemStack inHand = p.getItemInHand(hand);
        RelicDef def = Relics.defOf(inHand);
        if (def == null || !def.slot().isCurio()) return CurioEquipRules.Result.NOT_A_CURIO_RELIC;
        if (!CuriosLink.available()) return CurioEquipRules.Result.NO_CURIOS;
        Map<String, IItemHandler> handlers = CuriosLink.slotHandlers(p);
        if (handlers.isEmpty()) return CurioEquipRules.Result.NO_CURIOS;
        String want = def.slot().curiosSlot();
        if (!handlers.containsKey(want)) {
            WornOnce.log(WornOnce.Cause.SLOT_MISSING, def.id() + " (" + want + ")");
            return CurioEquipRules.Result.NO_SLOT_TYPE;
        }
        CurioEquipRules.Pick pick = CurioEquipRules.pick(want, slotsOf(handlers));
        if (pick == null) return CurioEquipRules.Result.SLOT_FULL;
        IItemHandler h = handlers.get(want);
        if (!(h instanceof IItemHandlerModifiable m)) {
            WornOnce.log(WornOnce.Cause.EQUIP_FAILED, def.id() + " (the " + want + " handler is not modifiable)");
            return CurioEquipRules.Result.NO_CURIOS;
        }
        m.setStackInSlot(pick.index(), inHand.copyWithCount(1));
        inHand.shrink(1);
        Relics.invalidate(p);
        CuriosLink.forget(p.getUUID());
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.relic_wear.equipped", "%s put on (%s slot)", def.cardName(), want), true);
        return CurioEquipRules.Result.EQUIPPED;
    }
}
