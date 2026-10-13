package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Wave 6: the pockets of Neket's Practical Mage Robe (two side pockets, "each a Bag of Holding") and Skitch's Smuggling Coat (deep pockets and seam
 * pockets). Each pocket is a plain vanilla chest menu over a container whose contents are written back to the worn relic's data on every change, so
 * nothing needs a registered menu type or a data component of its own. The menu closes by itself when the garment is taken off.
 */
public final class Wave6Pockets {
    private Wave6Pockets() {}

    private static final String ITEM = "Item", SLOT = "Slot";

    /** A pocket: {@code rows} rows of nine, filled from and saved to the data key {@code key} of the worn relic {@code relicId}. */
    private static final class Pocket extends SimpleContainer {
        private final ServerPlayer owner;
        private final String relicId, key;
        private boolean loading = true;

        Pocket(ServerPlayer owner, String relicId, String key, int rows) {
            super(rows * 9);
            this.owner = owner;
            this.relicId = relicId;
            this.key = key;
        }

        void load(ItemStack relic) {
            ListTag list = Wave6Kit.tag(relic).getList(key, 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                int slot = e.getInt(SLOT);
                if (slot < 0 || slot >= getContainerSize() || !e.contains(ITEM)) continue;
                ItemStack s = ItemStack.parse(owner.registryAccess(), e.get(ITEM)).orElse(ItemStack.EMPTY);
                if (!s.isEmpty()) setItem(slot, s);
            }
            loading = false;
        }

        @Override
        public boolean stillValid(Player p) {
            return p == owner && owner.isAlive() && !Wave6Kit.find(owner, relicId).isEmpty();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) save();
        }

        private void save() {
            ItemStack relic = Wave6Kit.find(owner, relicId);
            if (relic.isEmpty()) return;
            ListTag list = new ListTag();
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (s.isEmpty()) continue;
                CompoundTag e = new CompoundTag();
                e.putInt(SLOT, i);
                e.put(ITEM, s.save(owner.registryAccess()));
                list.add(e);
            }
            CustomData.update(DataComponents.CUSTOM_DATA, relic, t -> t.put(key, list));
        }
    }

    /** Opens pocket {@code key} of the worn relic {@code relicId} for {@code p}. */
    public static void open(ServerPlayer p, String relicId, String key, int rows, String title) {
        ItemStack relic = Wave6Kit.find(p, relicId);
        if (relic.isEmpty()) return;
        if (p.containerMenu != p.inventoryMenu) p.closeContainer();
        Pocket pocket = new Pocket(p, relicId, key, rows);
        pocket.load(relic);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> rows >= 6 ? ChestMenu.sixRows(id, inv, pocket) : ChestMenu.threeRows(id, inv, pocket), Component.literal(title)));
    }
}
