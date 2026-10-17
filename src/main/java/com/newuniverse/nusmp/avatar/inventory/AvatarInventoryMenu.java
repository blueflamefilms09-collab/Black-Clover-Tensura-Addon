package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.Stance;
import com.newuniverse.nusmp.avatar.party.AvatarPartyManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

/**
 * Why: "give the Avatar a fully functional equipment inventory" needs a window where the creator can see and change what the Avatar holds and wears: this is its
 * menu. Thirty-two Avatar slots (hands, four armour pieces, eight trinket slots, eighteen pockets, laid out by {@link InventoryLayout}) over the creator's own
 * inventory, opened by a sneak-right-click with an empty hand. The Avatar slots are the live container of {@link AvatarInventory} (on the server) so a change is the
 * Avatar's change at once; the client has a plain container that the usual slot packets fill. Armour slots take only armour of their kind, trinket slots only relics
 * (see {@link AvatarRelicBridge}), the rest anything. The four buttons give the orders of {@link AvatarOrders} through the vanilla menu-button packet, so this adds no
 * network message of its own; three numbers (stance, valuable stacks, party size) are synced the vanilla way for the screen to show.
 */
public class AvatarInventoryMenu extends AbstractContainerMenu {
    /** Menu button ids, in the order of the buttons on screen. */
    public static final int BTN_FOLLOW = 0, BTN_STAY = 1, BTN_DELVE = 2, BTN_SHARE = 3;

    private final Container container;
    private final AvatarEntity avatar;                 // server side only
    private final int avatarEntityId;
    private final int[] shown = new int[3];            // stance ordinal, valuable stacks, party size
    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return i >= 0 && i < shown.length ? shown[i] : 0; }
        @Override public void set(int i, int v) { if (i >= 0 && i < shown.length) shown[i] = v; }
        @Override public int getCount() { return shown.length; }
    };

    /** Server side: the real Avatar. */
    public AvatarInventoryMenu(int id, Inventory playerInv, AvatarEntity avatar) {
        this(id, playerInv, avatar.inventory(), avatar, avatar.getId());
    }

    /** Client side (the menu type's factory): a plain container, and the id of the Avatar entity for the portrait. */
    public AvatarInventoryMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, new SimpleContainer(InventoryLayout.SIZE), null, buf.readVarInt());
    }

    private AvatarInventoryMenu(int id, Inventory playerInv, Container container, AvatarEntity avatar, int entityId) {
        super(AvatarInventoryMenus.INVENTORY.get(), id);
        this.container = container;
        this.avatar = avatar;
        this.avatarEntityId = entityId;
        for (int i = 0; i < InventoryLayout.SIZE; i++) {
            int[] p = InventoryLayout.position(i);
            addSlot(new AvatarSlot(container, i, p[0] + 1, p[1] + 1));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int[] p = InventoryLayout.playerCell(row, col);
                addSlot(new Slot(playerInv, 9 + row * 9 + col, p[0] + 1, p[1] + 1));
            }
        }
        for (int col = 0; col < 9; col++) {
            int[] p = InventoryLayout.playerCell(3, col);
            addSlot(new Slot(playerInv, col, p[0] + 1, p[1] + 1));
        }
        addDataSlots(data);
        if (avatar != null) refreshShown();
    }

    // ---------------------------------------------------------------- opening
    /** Opens the window for the creator. Server side. */
    public static void open(ServerPlayer player, AvatarEntity avatar) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AvatarInventoryMenu(id, inv, avatar), Component.literal(avatar.avatarName())),
                buf -> buf.writeVarInt(avatar.getId()));
    }

    // ---------------------------------------------------------------- what the screen shows
    /** The id of the Avatar entity, for the portrait. */
    public int avatarEntityId() { return avatarEntityId; }

    public Stance stance() { return Stance.byOrdinal(shown[0]); }

    /** How many pockets hold something worth handing to the creator. */
    public int valuables() { return shown[1]; }

    public int partySize() { return shown[2]; }

    private void refreshShown() {
        shown[0] = avatar.stance().ordinal();
        shown[1] = AvatarGear.worthGiving(avatar).size();
        shown[2] = avatar.level() instanceof net.minecraft.server.level.ServerLevel sl ? AvatarPartyManager.get(sl.getServer()).count(avatar.getUUID()) : 0;
    }

    @Override
    public void broadcastChanges() {
        if (avatar != null) {
            avatar.inventory().refresh();               // somebody else may have changed the relic pack (a take-back, a give)
            refreshShown();
        }
        super.broadcastChanges();
    }

    // ---------------------------------------------------------------- the buttons
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (avatar == null || !(player instanceof ServerPlayer sp) || !avatar.isCreator(sp)) return false;
        AvatarOrders.Order order = switch (id) {
            case BTN_FOLLOW -> AvatarOrders.Order.FOLLOW;
            case BTN_STAY -> AvatarOrders.Order.STAY;
            case BTN_DELVE -> AvatarOrders.Order.DELVE;
            case BTN_SHARE -> AvatarOrders.Order.SHARE;
            default -> null;
        };
        return order != null && AvatarOrders.tell(sp, avatar, order);
    }

    @Override
    public boolean stillValid(Player player) {
        return avatar == null || (avatar.isAlive() && avatar.isCreator(player) && player.distanceToSqr(avatar) <= 64.0);
    }

    // ---------------------------------------------------------------- slots
    /** The armour slot a stack belongs in (as the game sees it: armour, an elytra, a head item), or null when it is no armour. */
    static EquipmentSlot armorSlotOf(ItemStack s) {
        Equipable e = Equipable.get(s);
        if (e == null) return null;
        EquipmentSlot slot = e.getEquipmentSlot();
        return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? slot : null;
    }

    /** May this stack go into Avatar slot {@code index}? */
    static boolean accepts(int index, ItemStack s) {
        if (s.isEmpty()) return true;
        if (InventoryLayout.isArmor(index)) return armorSlotOf(s) == AvatarInventory.slotOf(index);
        if (InventoryLayout.isTrinket(index)) return AvatarRelicBridge.isRelic(s);
        return true;
    }

    /** The index of the empty armour slot this stack would be worn in, or -1. */
    private int armorIndexFor(ItemStack s) {
        EquipmentSlot slot = armorSlotOf(s);
        return slot == null ? -1 : AvatarInventory.indexOf(slot);
    }

    private static final class AvatarSlot extends Slot {
        private final int avatarIndex;

        AvatarSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
            this.avatarIndex = index;
        }

        @Override public boolean mayPlace(ItemStack stack) { return accepts(avatarIndex, stack); }

        /** Armour and trinkets hold one item; hands and pockets hold a stack (the off hand may carry the arrows). */
        @Override public int getMaxStackSize() { return InventoryLayout.isArmor(avatarIndex) || InventoryLayout.isTrinket(avatarIndex) ? 1 : 64; }

        @Override public int getMaxStackSize(ItemStack stack) { return Math.min(getMaxStackSize(), stack.getMaxStackSize()); }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int avatarEnd = InventoryLayout.SIZE, playerEnd = slots.size();
        if (index < avatarEnd) {
            // Avatar -> creator: what the creator receives is no longer bonded to the Avatar (a relic is free to attune to a player again)
            ItemStack probe = stack.copy();
            if (avatar != null) AvatarRelicBridge.onLeave(avatar, probe);
            if (!moveItemStackTo(probe, avatarEnd, playerEnd, true)) return ItemStack.EMPTY;
            if (probe.getCount() == before.getCount()) return ItemStack.EMPTY;
            if (probe.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else {
                stack.setCount(probe.getCount());
                slot.setChanged();
            }
        } else {
            // creator -> Avatar: the matching armour slot if it is free, a trinket slot for a relic, else the pockets
            int armor = armorIndexFor(stack);
            boolean moved = armor >= 0 && !slots.get(armor).hasItem() && moveItemStackTo(stack, armor, armor + 1, false);
            if (!moved && AvatarRelicBridge.isRelic(stack)) moved = moveItemStackTo(stack, InventoryLayout.TRINKET_START, InventoryLayout.TRINKET_START + InventoryLayout.TRINKETS, false);
            if (!moved && !moveItemStackTo(stack, InventoryLayout.POCKET_START, InventoryLayout.POCKET_START + InventoryLayout.POCKETS, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }
}
