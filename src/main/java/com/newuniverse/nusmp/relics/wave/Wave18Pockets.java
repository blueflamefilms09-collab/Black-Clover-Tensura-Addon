package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.UseRules;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Wave 18: the inside of the Bountiful Bags. A bag keeps its contents as a list on its own stack (the key {@code w18_items}), so it moves with the
 * stack wherever it goes; opening it shows a chest menu over a container that writes itself back to whichever copy of the bag the player still
 * carries (found by a bag id stamped on the stack), and the menu closes by itself when the bag leaves the player. Each bag decides what may go in
 * ({@link #accepts}), and what a day inside does ({@link #dailyPass}): gilding, composting, the Devouring bag's spitting. No bag may go inside a bag.
 */
final class Wave18Pockets {
    private Wave18Pockets() {}

    static final String KEY_ID = "w18_bag", KEY_ITEMS = "w18_items", KEY_DAY = "w18_day", KEY_FED = "w18_fed", KEY_FED_DAY = "w18_fed_day";
    private static final String ITEM = "Item", SLOT = "Slot";

    // ---------------------------------------------------------------- what may go inside
    /** Is this stack something the bag takes? (Never another bag: an extradimensional space inside another tears both.) */
    static boolean accepts(Wave18Pure.Bag bag, ItemStack s) {
        if (s.isEmpty()) return true;
        RelicDef d = Relics.defOf(s);
        if (d != null && Wave18Pure.Bag.isBag(d.id())) return false;
        if (Wave17Vault.storage(s) || Wave17Vault.uniqueRelic(s)) return false;                                         // no pouch of wave 17 inside a bag, nor a box carrying one
        return switch (bag) {
            case COLDING -> s.has(DataComponents.FOOD);
            case MOLDING -> compostLevel(s) > 0.0;
            case GOLDING -> s.is(Tags.Items.INGOTS) || s.is(Tags.Items.NUGGETS) || Wave18Pure.coinLike(path(s.getItem()));
            default -> true;
        };
    }

    static String path(Item item) {
        ResourceLocation k = BuiltInRegistries.ITEM.getKey(item);
        return k == null ? "" : k.getPath();
    }

    static String id(Item item) {
        ResourceLocation k = BuiltInRegistries.ITEM.getKey(item);
        return k == null ? "" : k.toString();
    }

    /** The composter chance of one item of this stack (0 when it cannot be composted). */
    static double compostLevel(ItemStack s) {
        try {
            return Math.max(0f, ComposterBlock.COMPOSTABLES.getFloat(s.getItem()));
        } catch (RuntimeException none) {
            return 0.0;
        }
    }

    /** Animal or vegetable matter: food, or anything a composter takes. The Devouring bag eats it. */
    static boolean organic(ItemStack s) { return !s.isEmpty() && (s.has(DataComponents.FOOD) || compostLevel(s) > 0.0); }

    // ---------------------------------------------------------------- finding the bag again
    static String idOf(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null ? "" : cd.copyTag().getString(KEY_ID);
    }

    /** Stamps a bag id on the stack the first time it is opened, and returns it. */
    static String ensureId(ItemStack bag) {
        String have = idOf(bag);
        if (!have.isEmpty()) return have;
        String fresh = UUID.randomUUID().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, bag, t -> t.putString(KEY_ID, fresh));
        return fresh;
    }

    /** The live stack of the bag with this id that the player carries (inventory, cursor, Curios), or EMPTY. */
    static ItemStack locate(ServerPlayer p, String bagId) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (is(inv.getItem(i), bagId)) return inv.getItem(i);
        ItemStack carried = p.containerMenu == null ? ItemStack.EMPTY : p.containerMenu.getCarried();
        if (is(carried, bagId)) return carried;
        for (ItemStack s : CuriosLink.equipped(p)) if (is(s, bagId)) return s;
        return ItemStack.EMPTY;
    }

    private static boolean is(ItemStack s, String bagId) {
        if (s.isEmpty()) return false;
        RelicDef d = Relics.defOf(s);
        return d != null && Wave18Pure.Bag.isBag(d.id()) && bagId.equals(idOf(s));
    }

    // ---------------------------------------------------------------- the container
    private static final class Pocket extends SimpleContainer {
        private final ServerPlayer owner;
        private final Wave18Pure.Bag bag;
        private final String bagId;
        private boolean loading = true, busy;

        Pocket(ServerPlayer owner, Wave18Pure.Bag bag, String bagId) {
            super(bag.slots());
            this.owner = owner;
            this.bag = bag;
            this.bagId = bagId;
        }

        void load(ItemStack bagStack) {
            CompoundTag t = tag(bagStack);
            ListTag list = t.getList(KEY_ITEMS, 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                int slot = e.getInt(SLOT);
                if (slot < 0 || slot >= getContainerSize() || !e.contains(ITEM)) continue;
                ItemStack s = ItemStack.parse(owner.registryAccess(), e.get(ITEM)).orElse(ItemStack.EMPTY);
                if (!s.isEmpty()) setItem(slot, s);
            }
            long today = UseRules.day(owner.level().getGameTime());
            long last = t.contains(KEY_DAY) ? t.getLong(KEY_DAY) : today;
            dailyPass(last, today);
            loading = false;
            save();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack s) { return accepts(bag, s); }

        @Override
        public boolean stillValid(Player p) { return p == owner && owner.isAlive() && !locate(owner, bagId).isEmpty(); }

        @Override
        public void setChanged() {
            super.setChanged();
            if (loading || busy) return;
            busy = true;
            try {
                if (bag == Wave18Pure.Bag.DEVOURING) devour();
                save();
            } finally {
                busy = false;
            }
        }

        private void save() {
            ItemStack stack = locate(owner, bagId);
            if (stack.isEmpty()) return;
            ListTag list = new ListTag();
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (s.isEmpty()) continue;
                CompoundTag e = new CompoundTag();
                e.putInt(SLOT, i);
                e.put(ITEM, s.save(owner.registryAccess()));
                list.add(e);
            }
            long today = UseRules.day(owner.level().getGameTime());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                t.put(KEY_ITEMS, list);
                t.putLong(KEY_DAY, today);
            });
        }

        // ------------------------------------------------------------ what a day inside does
        private void dailyPass(long last, long today) {
            if (bag == Wave18Pure.Bag.SCOLDING) sort();
            if (bag == Wave18Pure.Bag.DEVOURING) {
                devour();
                if (Wave18Pure.dayPassed(last, today)) spit();
            }
            if (!Wave18Pure.dayPassed(last, today)) return;
            if (bag == Wave18Pure.Bag.GOLDING) gild();
            if (bag == Wave18Pure.Bag.MOLDING) compost();
        }

        /** Worked metals left a day in the Bag of Golding come out gilded (gold nuggets), at most 64 items a day. */
        private void gild() {
            int budget = Wave18Tune.DAILY_ITEM_CAP, made = 0;
            for (int i = 0; i < getContainerSize() && budget > 0; i++) {
                ItemStack s = getItem(i);
                String to = Wave18Pure.gildedOf(id(s.getItem()));
                if (s.isEmpty() || to == null) continue;
                ResourceLocation rl = ResourceLocation.tryParse(to);
                Item target = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (target == null) continue;
                int n = Math.min(s.getCount(), budget);
                int each = Wave18Pure.gildedCount(id(s.getItem()));
                s.shrink(n);
                if (s.isEmpty()) setItem(i, ItemStack.EMPTY);
                budget -= n;
                made += n * each;
                give(new ItemStack(target, Math.min(64, n * each)));
                if (n * each > 64) give(new ItemStack(target, n * each - 64));
            }
            if (made > 0) owner.displayClientMessage(Component.literal("The bag gilded " + made + " nuggets.").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }

        /** Plant matter left a day in the Bag of Molding rots to bone meal: one per five composter levels. */
        private void compost() {
            double levels = 0;
            int counted = 0;
            for (int i = 0; i < getContainerSize() && counted < Wave18Tune.DAILY_ITEM_CAP; i++) {
                ItemStack s = getItem(i);
                double c = compostLevel(s);
                if (s.isEmpty() || c <= 0) continue;
                int n = Math.min(s.getCount(), Wave18Tune.DAILY_ITEM_CAP - counted);
                levels += n * c;
                counted += n;
            }
            int meals = Wave18Pure.boneMealFor(levels);
            if (meals <= 0) return;
            double need = meals * Wave18Tune.COMPOST_LEVELS_PER_MEAL, used = 0;
            for (int i = 0; i < getContainerSize() && used < need; i++) {
                ItemStack s = getItem(i);
                double c = compostLevel(s);
                if (s.isEmpty() || c <= 0) continue;
                while (!s.isEmpty() && used < need) {
                    s.shrink(1);
                    used += c;
                }
                if (s.isEmpty()) setItem(i, ItemStack.EMPTY);
            }
            give(new ItemStack(Items.BONE_MEAL, Math.min(64, meals)));
            if (meals > 64) give(new ItemStack(Items.BONE_MEAL, meals - 64));
            owner.displayClientMessage(Component.literal("The bag rotted its plants down to " + meals + " bone meal.").withStyle(net.minecraft.ChatFormatting.GREEN), true);
        }

        /** The Devouring bag eats animal and vegetable matter put in it, and what it eats feeds the one who carries it with magicules. */
        private void devour() {
            int eaten = 0;
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (!organic(s)) continue;
                eaten += s.getCount();
                setItem(i, ItemStack.EMPTY);
            }
            if (eaten > 0) feed(eaten);
        }

        private void feed(int eaten) {
            ItemStack stack = locate(owner, bagId);
            if (stack.isEmpty()) return;
            CompoundTag t = tag(stack);
            long today = UseRules.day(owner.level().getGameTime());
            int fed = t.getLong(KEY_FED_DAY) == today ? t.getInt(KEY_FED) : 0;
            double percent = Wave18Pure.feedPercent(eaten, fed);
            int counted = Wave18Pure.feedCounted(eaten, fed);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tt -> {
                tt.putLong(KEY_FED_DAY, today);
                tt.putInt(KEY_FED, fed + counted);
            });
            if (percent <= 0) return;
            try {
                var ex = TensuraStorages.getExistenceFrom(owner);
                if (ex == null) return;
                double max = EnergyHelper.getMaxMagicule(owner);
                ex.setMagicule(Math.min(max, ex.getMagicule() + max * percent / 100.0));
                ex.markDirty();
            } catch (RuntimeException | LinkageError none) {
                // no Tensura data: the bag simply eats
            }
        }

        /** Once a day the Devouring bag swallows what is in it and spits it out on another plane: here, onto the ground at the bearer's feet. */
        private void spit() {
            int n = 0;
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (s.isEmpty()) continue;
                owner.spawnAtLocation(s.copy());
                setItem(i, ItemStack.EMPTY);
                n++;
            }
            if (n > 0) owner.displayClientMessage(Component.literal("The bag swallowed its contents and spat them out at your feet.").withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
        }

        /** The Bag of Scolding cannot stand a mess: it sorts its contents by name and merges what stacks every time it is opened. */
        private void sort() {
            List<ItemStack> all = new ArrayList<>();
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (s.isEmpty()) continue;
                boolean merged = false;
                for (ItemStack o : all) {
                    if (ItemStack.isSameItemSameComponents(o, s) && o.getCount() < o.getMaxStackSize()) {
                        int move = Math.min(s.getCount(), o.getMaxStackSize() - o.getCount());
                        o.grow(move);
                        s.shrink(move);
                        if (s.isEmpty()) { merged = true; break; }
                    }
                }
                if (!merged) all.add(s.copy());
                setItem(i, ItemStack.EMPTY);
            }
            all.sort(Comparator.comparing((ItemStack s) -> id(s.getItem())).thenComparing(ItemStack::getCount, Comparator.reverseOrder()));
            for (int i = 0; i < all.size() && i < getContainerSize(); i++) setItem(i, all.get(i));
        }

        /** Puts a stack in the bag, or at the bearer's feet when it will not fit. */
        private void give(ItemStack s) {
            ItemStack left = addItem(s);
            if (!left.isEmpty()) owner.spawnAtLocation(left);
        }
    }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    // ---------------------------------------------------------------- the menu
    private static MenuType<?> type(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    /** A chest menu whose slots honour what the bag accepts (the plain one lets anything in). */
    private static final class BagMenu extends AbstractContainerMenu {
        private final SimpleContainer pocket;
        private final int rows;

        BagMenu(int id, Inventory inv, SimpleContainer pocket, int rows) {
            super(type(rows), id);
            this.pocket = pocket;
            this.rows = rows;
            for (int j = 0; j < rows; j++) {
                for (int k = 0; k < 9; k++) {
                    this.addSlot(new Slot(pocket, k + j * 9, 8 + k * 18, 18 + j * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) { return pocket.canPlaceItem(getContainerSlot(), stack); }
                    });
                }
            }
            int shift = (rows - 4) * 18;
            for (int l = 0; l < 3; l++) for (int m = 0; m < 9; m++) this.addSlot(new Slot(inv, m + l * 9 + 9, 8 + m * 18, 103 + l * 18 + shift));
            for (int n = 0; n < 9; n++) this.addSlot(new Slot(inv, n, 8 + n * 18, 161 + shift));
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            ItemStack moved = ItemStack.EMPTY;
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack inSlot = slot.getItem();
                moved = inSlot.copy();
                if (index < rows * 9) {
                    if (!this.moveItemStackTo(inSlot, rows * 9, this.slots.size(), true)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(inSlot, 0, rows * 9, false)) {
                    return ItemStack.EMPTY;
                }
                if (inSlot.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
                else slot.setChanged();
            }
            return moved;
        }

        @Override
        public boolean stillValid(Player player) { return pocket.stillValid(player); }
    }

    // ---------------------------------------------------------------- opening
    /** Opens the bag {@code stack} (a live stack the player carries) for {@code p}. */
    static void open(ServerPlayer p, ItemStack stack, Wave18Pure.Bag bag, String title) {
        if (stack == null || stack.isEmpty()) return;
        if (p.containerMenu != p.inventoryMenu) p.closeContainer();
        String id = ensureId(stack);
        Pocket pocket = new Pocket(p, bag, id);
        pocket.load(stack);
        p.openMenu(new SimpleMenuProvider((cid, inv, pl) -> new BagMenu(cid, inv, pocket, bag.rows()), Component.literal(title)));
    }

    /** The stacks inside a bag, read from its data (for the snack of the Bag of Colding and for tests of the glue). */
    static List<ItemStack> contents(ServerPlayer p, ItemStack bagStack) {
        List<ItemStack> out = new ArrayList<>();
        ListTag list = tag(bagStack).getList(KEY_ITEMS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.contains(ITEM)) ItemStack.parse(p.registryAccess(), e.get(ITEM)).ifPresent(out::add);
        }
        return out;
    }

    /** Takes one item the predicate likes out of the bag's stored contents (the bag need not be open); EMPTY if there is none. */
    static ItemStack takeOne(ServerPlayer p, ItemStack bagStack, java.util.function.Predicate<ItemStack> want) {
        if (p.containerMenu != p.inventoryMenu) p.closeContainer();                          // an open pocket would overwrite the change
        CompoundTag t = tag(bagStack);
        ListTag list = t.getList(KEY_ITEMS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (!e.contains(ITEM)) continue;
            ItemStack s = ItemStack.parse(p.registryAccess(), e.get(ITEM)).orElse(ItemStack.EMPTY);
            if (s.isEmpty() || !want.test(s)) continue;
            ItemStack one = s.copyWithCount(1);
            s.shrink(1);
            ListTag fresh = new ListTag();
            for (int j = 0; j < list.size(); j++) {
                if (j != i) fresh.add(list.getCompound(j));
                else if (!s.isEmpty()) {
                    CompoundTag keep = new CompoundTag();
                    keep.putInt(SLOT, e.getInt(SLOT));
                    keep.put(ITEM, s.save(p.registryAccess()));
                    fresh.add(keep);
                }
            }
            CustomData.update(DataComponents.CUSTOM_DATA, bagStack, tt -> tt.put(KEY_ITEMS, fresh));
            return one;
        }
        return ItemStack.EMPTY;
    }
}
