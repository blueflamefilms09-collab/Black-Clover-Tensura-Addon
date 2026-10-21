package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: eighty new items need a place to be found, and the creative tab is the owner's way to check them: one tab, "Dungeon Loot", with the foods and elixirs first, then
 * the monster parts, then the gear. Every piece of gear appears twice, as a common piece from the first floor (what a recipe would give) and as an epic piece rolled for floor
 * 100 (what a deep drop looks like), so the range of the rolls can be seen without a farm. Both are pre-rolled stacks with a fixed seed, so the tab is the same on every launch.
 */
public final class LootCreative {
    private LootCreative() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NUSMP.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("dungeon_loot", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nusmp.dungeon_loot"))
            .icon(() -> {
                Item i = LootItems.get("mana_elixir");
                return new ItemStack(i == null ? Items.EXPERIENCE_BOTTLE : i);
            })
            .displayItems((params, out) -> {
                for (String id : LootItems.foodIds()) accept(out, id);
                for (String id : LootItems.materialIds()) accept(out, id);
                for (String id : LootItems.gearIds()) {
                    Item item = LootItems.get(id);
                    if (item == null) continue;
                    out.accept(item.getDefaultInstance());
                    if (item instanceof LootGear) out.accept(GearStacks.create(item, LootRarity.EPIC, id.hashCode() * 31L + 7L, 100));
                }
                for (com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackTier t : com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackTier.values())
                    out.accept(com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackItem.of(t));                       // 1.107.0: a backpack of every tier, then the upgrades
                for (var up : com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackRegistry.upgrades()) out.accept(up.get());
            })
            .build());

    private static void accept(CreativeModeTab.Output out, String id) {
        Item item = LootItems.get(id);
        if (item != null) out.accept(item);
    }
}
