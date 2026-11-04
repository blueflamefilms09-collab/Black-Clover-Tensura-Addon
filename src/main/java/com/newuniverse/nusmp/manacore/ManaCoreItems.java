package com.newuniverse.nusmp.manacore;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the 24 rank cores as items (3.51.0): ids {@code nusmp:core_<rank>_<variant>} (core_e_standard ... core_ss_ancient). One register of its own, attached to the mod bus by {@link ManaCoreBoot}. They are not the
 * sector-key {@code nusmp:mana_core}. No recipe: they come from drops, dungeon rewards and {@code /manacore give}.
 */
public final class ManaCoreItems {
    private ManaCoreItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);
    private static final Map<String, DeferredItem<RankCoreItem>> BY_ID = new LinkedHashMap<>();
    private static final Rarity[] RARITY = {Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.EPIC};

    static {
        for (ManaCoreRules.Core c : ManaCoreRules.all()) {
            final ManaCoreRules.Core core = c;
            BY_ID.put(c.id(), REGISTER.register(c.id(), () -> new RankCoreItem(core, new Item.Properties().stacksTo(1).rarity(RARITY[core.rank()]).fireResistant())));
        }
    }

    /** The 24 cores as suppliers, for the 3D hand renderer ({@link CoreItems#renderables()}). */
    static List<java.util.function.Supplier<? extends Item>> suppliers() { return new ArrayList<>(BY_ID.values()); }

    public static RankCoreItem item(String id) {
        DeferredItem<RankCoreItem> d = BY_ID.get(id);
        return d == null || !d.isBound() ? null : d.get();
    }

    /** A stack of this core, or empty when the id is unknown. */
    public static ItemStack stack(ManaCoreRules.Core c) {
        RankCoreItem i = c == null ? null : item(c.id());
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    /** The core a stack is, or null for any other item. */
    public static ManaCoreRules.Core coreOf(ItemStack s) { return s != null && s.getItem() instanceof RankCoreItem r ? r.core() : null; }

    /** All 24 for the creative tab. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (ManaCoreRules.Core c : ManaCoreRules.all()) {
            ItemStack s = stack(c);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
