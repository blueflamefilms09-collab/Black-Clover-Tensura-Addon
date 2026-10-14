package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

/**
 * Wave 16: the Satchel of Unmet Needs. It cannot be stocked; it only gives. One object of the list at a time can be out, and it vanishes after a game
 * day outside the bag (or when put back, it is simply home again); an object that vanished is missing from the bag for three dawns. The state is on the
 * satchel's own data (the chosen object, which are out until when, which are missing until which day); an object it produced carries a small mark so
 * it can be found, taken back and made to vanish. The card's list is cut off at the bottom, so the last two entries (torches, rations) are an addition.
 */
final class Wave16Satchel {
    private Wave16Satchel() {}

    /** One object of the bag: the name on the card, the item it becomes, and how many. */
    record Entry(String name, String item, int count) {}

    static final List<Entry> ITEMS = List.of(
            new Entry("Pen and Ink", "minecraft:writable_book", 1),
            new Entry("Bedroll and Blanket", "minecraft:white_bed", 1),
            new Entry("Crowbar", "minecraft:iron_pickaxe", 1),
            new Entry("Bell", "minecraft:bell", 1),
            new Entry("Empty Bottles", "minecraft:glass_bottle", 3),
            new Entry("Hempen Rope", "minecraft:lead", 2),
            new Entry("Chain", "minecraft:chain", 15),
            new Entry("Lock and Key", "minecraft:tripwire_hook", 2),
            new Entry("Lantern", "minecraft:lantern", 1),
            new Entry("Wooden Bucket", "minecraft:bucket", 1),
            new Entry("Iron Pot", "minecraft:cauldron", 1),
            new Entry("Tinderbox", "minecraft:flint_and_steel", 1),
            new Entry("Magnifying Glass", "minecraft:spyglass", 1),
            new Entry("Steel Mirror", "minecraft:glass_pane", 1),
            new Entry("Ten-foot Pole", "minecraft:stick", 10),
            new Entry("Folded Tent", "minecraft:white_wool", 6),
            new Entry("Torches", "minecraft:torch", 16),
            new Entry("Rations", "minecraft:bread", 6));

    private static ItemStack live(ActCtx c) {
        ItemStack s = Wave6Kit.find(c.player(), Wave16Tune.ID_SATCHEL);
        return s.isEmpty() ? c.stack() : s;
    }

    private static int selected(ItemStack s) { return Math.floorMod(Wave6Kit.tag(s).getInt(Wave16Tune.S_SAT_SEL), ITEMS.size()); }

    /** Choose Object: the next entry of the list. */
    static void next(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = live(c);
        int sel = (selected(s) + 1) % ITEMS.size();
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt(Wave16Tune.S_SAT_SEL, sel));
        Entry e = ITEMS.get(sel);
        Fx.say(p, "You reach for: " + e.name() + (e.count() > 1 ? " x" + e.count() : "") + ".");
    }

    /** Reach In: the chosen object is in your hand, unless it is out already or missing. */
    static void reach(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = live(c);
        int sel = selected(s);
        Entry e = ITEMS.get(sel);
        long now = p.level().getGameTime(), today = Wave6Kit.today(p.serverLevel());
        CompoundTag t = Wave6Kit.tag(s);
        String k = Integer.toString(sel);
        Wave16Rules.Slot slot = Wave16Rules.satchelSlot(t.getCompound(Wave16Tune.S_SAT_OUT).getLong(k), t.getCompound(Wave16Tune.S_SAT_GONE).getLong(k), now, today);
        if (slot == Wave16Rules.Slot.MISSING) {
            Fx.say(p, e.name() + " is missing from the bag until dawn of day " + t.getCompound(Wave16Tune.S_SAT_GONE).getLong(k) + ".");
            return;
        }
        if (slot == Wave16Rules.Slot.OUT) {
            Fx.say(p, "A second " + e.name() + " cannot be produced while the first exists. Put it back, or wait for it to vanish.");
            return;
        }
        Optional<Item> item = Wave16Kit.item(e.item());
        if (item.isEmpty()) {
            Fx.say(p, "Your hand finds nothing that was ever " + e.name() + ".");
            return;
        }
        ItemStack made = new ItemStack(item.get(), e.count());
        long expires = now + Wave16Tune.SAT_OUT_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, made, tag -> {
            tag.putInt(Wave16Tune.T_SAT_IDX, sel);
            tag.putLong(Wave16Tune.T_SAT_EXP, expires);
        });
        if (!p.getInventory().add(made)) p.drop(made, false);
        CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> {
            CompoundTag out = tag.getCompound(Wave16Tune.S_SAT_OUT).copy();
            out.putLong(k, expires);
            tag.put(Wave16Tune.S_SAT_OUT, out);
        });
        Fx.say(p, "You pull out " + e.name() + ". It will not last a day outside the bag.");
    }

    /** Put Back: the chosen object, wherever it is in your pack, goes home and can be called again. */
    static void putBack(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = live(c);
        int sel = selected(s);
        Inventory inv = p.getInventory();
        boolean found = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {                           // every part of it: a stack that was split in two must go home whole, or the object could be called again with half still in the pack
            ItemStack it = inv.getItem(i);
            if (marked(it, sel)) { inv.setItem(i, ItemStack.EMPTY); found = true; }
        }
        if (!found) { Fx.say(p, "Nothing of that kind is in your pack to put back."); return; }
        String k = Integer.toString(sel);
        CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> {
            CompoundTag out = tag.getCompound(Wave16Tune.S_SAT_OUT).copy();
            out.remove(k);
            tag.put(Wave16Tune.S_SAT_OUT, out);
        });
        Fx.say(p, ITEMS.get(sel).name() + " vanishes into the bag.");
    }

    private static boolean marked(ItemStack it, int idx) {
        if (it.isEmpty()) return false;
        CustomData cd = it.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        CompoundTag t = cd.copyTag();
        return t.contains(Wave16Tune.T_SAT_IDX) && t.getInt(Wave16Tune.T_SAT_IDX) == idx;
    }

    /**
     * Every few seconds while the satchel is worn: an object whose day outside the bag is up vanishes from the pack, and the bag marks it missing for
     * three dawns.
     */
    static void sweep(ServerPlayer p) {
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_SATCHEL);
        if (s.isEmpty()) return;
        CompoundTag out = Wave6Kit.tag(s).getCompound(Wave16Tune.S_SAT_OUT);
        if (out.isEmpty()) return;
        long now = p.level().getGameTime(), today = Wave6Kit.today(p.serverLevel());
        Inventory inv = p.getInventory();
        for (String k : out.getAllKeys()) {
            if (out.getLong(k) > now) continue;
            int idx;
            try { idx = Integer.parseInt(k); } catch (NumberFormatException bad) { idx = -1; }
            for (int i = 0; i < inv.getContainerSize(); i++) if (marked(inv.getItem(i), idx)) inv.setItem(i, ItemStack.EMPTY);
            String key = k;
            CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> {
                CompoundTag o = tag.getCompound(Wave16Tune.S_SAT_OUT).copy();
                o.remove(key);
                tag.put(Wave16Tune.S_SAT_OUT, o);
                CompoundTag g = tag.getCompound(Wave16Tune.S_SAT_GONE).copy();
                g.putLong(key, today + Wave16Tune.SAT_MISSING_DAYS);
                tag.put(Wave16Tune.S_SAT_GONE, g);
            });
            if (idx >= 0 && idx < ITEMS.size()) Fx.say(p, ITEMS.get(idx).name() + " has been outside the bag too long and vanishes into thin air. It is missing for three dawns.");
        }
    }
}
