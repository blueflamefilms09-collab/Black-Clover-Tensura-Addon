package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Why: eighty items, registered in a LOOP over the roster, so that adding an item to {@code tools/loot/roster.py} adds it here, to the drop tables, the sell prices, the
 * creative tab, the weapon renderer's list and the tests, all of which iterate the same roster. The register is the loot's own ({@code DeferredRegister.Items}, attached to the
 * mod bus by {@link LootRegistry#init}); the class of each item follows its kind: a food or elixir is a {@link LootFoodItem}, a monster part or crafted ingot a
 * {@link LootMaterialItem}, the arrow a {@link BoneArrowItem}, and the gear its own class. The 3D pieces (the eight weapons and the eight trinkets) are handed to the weapon
 * renderer through {@link #renderables()}.
 */
public final class LootItems {
    private LootItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    private static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();
    private static final List<Supplier<? extends Item>> RENDERABLE = new ArrayList<>();
    private static final List<String> FOODS = new ArrayList<>(), MATERIALS = new ArrayList<>(), GEAR = new ArrayList<>();

    static {
        LootRoster r = LootRoster.get();
        for (LootRoster.FoodDef f : r.foods()) {
            reg(f.id(), () -> new LootFoodItem(LootFoodItem.props(f), f));
            FOODS.add(f.id());
        }
        for (LootRoster.MaterialDef m : r.materials()) {
            boolean code = r.codeUses().containsKey(m.id());
            reg(m.id(), () -> new LootMaterialItem(LootMaterialItem.props(m.tier()), m.id(), m.lore(), m.value(), code));
            MATERIALS.add(m.id());
        }
        for (LootRoster.CraftedDef c : r.crafted()) {
            reg(c.id(), () -> new LootMaterialItem(LootMaterialItem.props(3), c.id(), c.lore(), c.value(), false));
            MATERIALS.add(c.id());
        }
        for (LootRoster.CraftedDef a : r.ammo()) {
            reg(a.id(), () -> new BoneArrowItem(new Item.Properties(), a.lore()));
            MATERIALS.add(a.id());
        }
        for (LootRoster.WeaponDef w : r.weapons()) {
            DeferredItem<LootWeaponItem> d = REGISTER.register(w.id(), () -> new LootWeaponItem(LootWeaponItem.props(w), w));
            ITEMS.put(w.id(), d);
            RENDERABLE.add(d);
            GEAR.add(w.id());
        }
        for (LootRoster.ArmourDef a : r.armour()) {
            ArmorItem.Type type = LootArmorItem.typeOf(a.piece());
            reg(a.id(), () -> new LootArmorItem(LootArmorMaterials.of(a.set()), type, LootArmorItem.props(type, LootArmorMaterials.durabilityFactor(a.set())), a));
            GEAR.add(a.id());
        }
        for (LootRoster.TrinketDef t : r.trinkets()) {
            DeferredItem<LootTrinketItem> d = REGISTER.register(t.id(), () -> new LootTrinketItem(LootTrinketItem.props(), t));
            ITEMS.put(t.id(), d);
            RENDERABLE.add(d);
            GEAR.add(t.id());
        }
    }

    private static void reg(String id, Supplier<? extends Item> sup) { ITEMS.put(id, REGISTER.register(id, sup)); }

    /** The item with this roster id, or null when it is not one of the loot items (or is not registered yet). */
    public static Item get(String id) {
        Item i = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id)).orElse(null);
        return i == null || i == Items.AIR ? null : i;
    }

    public static boolean isLoot(String id) { return ITEMS.containsKey(id); }

    /** Every loot item, in registration order. */
    public static List<DeferredItem<? extends Item>> all() { return new ArrayList<>(ITEMS.values()); }

    /** The weapons and trinkets: drawn in 3D by the weapon renderer (each has a mesh, a skin and an {@code _icon} model). */
    public static List<Supplier<? extends Item>> renderables() { return RENDERABLE; }

    public static List<String> foodIds() { return FOODS; }

    public static List<String> materialIds() { return MATERIALS; }

    public static List<String> gearIds() { return GEAR; }
}
