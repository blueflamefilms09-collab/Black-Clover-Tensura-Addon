package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3.12.0: the server half of "head and feet relics are worn in the Curios slots" (the armour slot never drew them on the body). The right-click is {@link RelicCurioEquip}; this class moves what
 * is already in the HEAD / FEET armour slot and works out the armour points a Curios-worn head or feet relic keeps. Rules (pure, tested): {@link HeadFeetMoveRules}. Server setting
 * {@code relics.headFeetInCurios} in {@code nusmp-relicpower-server.toml}; off means none of this runs and the armour slots are as before.
 * <p>
 * Cost: once a second per player (inside the relic pass that already runs) two armour-slot reads and an instance check; only when one of them holds a head or feet relic is the Curios inventory read
 * (one reflective map), and after a try that found no place the same player is not looked at again for {@link HeadFeetMoveRules#RETRY_TICKS} ticks. No scan of entities, blocks or chunks, no packet of
 * its own (the changed slots sync as they always do), one table entry per player with a pending back-off, dropped on logout.
 */
public final class HeadFeetCurios {
    private HeadFeetCurios() {}

    /** Per player: the game time before which the armour slots are not looked at again (only set after a try that found no place). */
    private static final Map<UUID, Long> NOT_BEFORE = new ConcurrentHashMap<>();

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.FEET};

    /** Is the move on? (the server setting) */
    public static boolean enabled() { return RelicPowerConfig.headFeetInCurios(); }

    /** The Curios slot id this relic goes to under the setting ({@code relic_head}, {@code relic_feet}), or null: another slot, or the setting is off. */
    public static String redirectSlot(RelicDef d) { return d == null || d.isAdopted() || !enabled() ? null : HeadFeetMoveRules.curiosSlot(d.slot().name()); }      // an adopted item is an older item of the mod, not in the relic_head / relic_feet tags

    /** Once a second, from the relic pass: moves a head or feet relic out of the armour slot into its Curios slot. Returns how many were moved. */
    static int tick(ServerPlayer p) {
        long now = p.level().getGameTime();
        Long notBefore = NOT_BEFORE.get(p.getUUID());
        if (!HeadFeetMoveRules.due(enabled(), p.isSpectator(), now, notBefore == null ? 0L : notBefore)) return 0;
        boolean any = false;
        for (EquipmentSlot s : SLOTS) if (wornRelic(p, s) != null) any = true;
        if (!any) {
            if (notBefore != null) NOT_BEFORE.remove(p.getUUID());
            return 0;
        }
        int moved = moveWorn(p);
        boolean left = false;
        for (EquipmentSlot s : SLOTS) if (wornRelic(p, s) != null) left = true;
        if (left) NOT_BEFORE.put(p.getUUID(), HeadFeetMoveRules.retryAt(now));           // no Curios, or the place is taken: try again in ten seconds, not every second
        else NOT_BEFORE.remove(p.getUUID());
        return moved;
    }

    private static RelicDef wornRelic(ServerPlayer p, EquipmentSlot slot) {
        ItemStack worn = p.getItemBySlot(slot);
        if (worn.isEmpty()) return null;
        RelicDef d = Relics.defOf(worn);
        return d != null && !d.isAdopted() && HeadFeetMoveRules.belongsTo(d.slot().name(), slot.name()) ? d : null;
    }

    /**
     * Moves the head relic in the HEAD armour slot and the feet relic in the FEET armour slot to the first free place of their Curios slot (relic_head, relic_feet). Nothing is lost: the stack is
     * copied into the Curios place first and the armour slot is emptied only when that place really holds it. Without Curios, without the slot type or with the place taken, the relic stays where it
     * is. Returns how many were moved. Does not look at the setting (the caller does); the self-test calls it directly.
     */
    public static int moveWorn(ServerPlayer p) {
        if (!CuriosLink.available()) return 0;
        Map<String, IItemHandler> handlers = null;
        int moved = 0;
        for (EquipmentSlot s : SLOTS) {
            RelicDef d = wornRelic(p, s);
            if (d == null) continue;
            if (handlers == null) {
                handlers = CuriosLink.slotHandlers(p);
                if (handlers.isEmpty()) return moved;
            }
            String want = HeadFeetMoveRules.curiosSlot(d.slot().name());
            if (!(handlers.get(want) instanceof IItemHandlerModifiable m)) {
                if (!handlers.containsKey(want)) WornOnce.log(WornOnce.Cause.SLOT_MISSING, d.id() + " (" + want + ")");
                continue;
            }
            CurioEquipRules.Pick pick = CurioEquipRules.pick(want, RelicCurioEquip.slotsOf(handlers));
            if (pick == null) continue;                                                   // the place is taken: the relic keeps working from the armour slot
            ItemStack worn = p.getItemBySlot(s);
            m.setStackInSlot(pick.index(), worn.copy());
            if (m.getStackInSlot(pick.index()).isEmpty()) continue;                       // the slot did not take it: nothing is emptied
            p.setItemSlot(s, ItemStack.EMPTY);
            moved++;
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.relic_wear.moved", "%s moved to its Curios slot (%s)", d.cardName(), want), true);
        }
        if (moved > 0) {
            Relics.invalidate(p);
            CuriosLink.forget(p.getUUID());
        }
        return moved;
    }

    /** The armour points and toughness the head and feet relics worn in this player's Curios slots keep (their armour form is not worn); empty when none is. */
    static List<HeadFeetMoveRules.Share> shares(List<Relics.Located> located) {
        List<String> ids = new ArrayList<>(2), slots = new ArrayList<>(2);
        List<Integer> defence = new ArrayList<>(2);
        List<Double> toughness = new ArrayList<>(2);
        for (Relics.Located l : located) {
            if (l.where() != Relics.Where.CURIOS || HeadFeetMoveRules.curiosSlot(l.def().slot().name()) == null) continue;
            ids.add(l.def().id());
            slots.add(l.def().slot().name());
            defence.add(l.def().armorDefence());
            toughness.add(l.def().armorToughness());
        }
        return ids.isEmpty() ? List.of() : HeadFeetMoveRules.shares(ids, slots, defence, toughness);
    }

    /** Forgets the back-off of this player (logout). */
    public static void forget(UUID id) { if (id != null) NOT_BEFORE.remove(id); }

    public static void clear() { NOT_BEFORE.clear(); }
}
