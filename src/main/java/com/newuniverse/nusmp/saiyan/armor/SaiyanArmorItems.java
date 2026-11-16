package com.newuniverse.nusmp.saiyan.armor;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Saiyan alloy ingot, the four armour pieces and the armour material {@code saiyan_alloy} are registered here (docs/saiyan_armor.md). The material carries the numbers of
 * {@link SaiyanArmorRules}; the layer texture is {@code anime_multiverse:saiyan_alloy} (textures/models/armor/saiyan_alloy_layer_1/2.png, painted by tools/saiyan_armor/gen_layers.py); the repair
 * ingredient is the alloy ingot (looked up when asked for).
 */
public final class SaiyanArmorItems {
    private SaiyanArmorItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ALLOY = MATERIALS.register(SaiyanArmorRules.MATERIAL, SaiyanArmorItems::material);

    public static final DeferredItem<Item> INGOT = REGISTER.register(SaiyanArmorRules.INGOT, () -> new Item(new Item.Properties()));
    public static final DeferredItem<SaiyanArmorItem> SCOUTER = piece(SaiyanArmorRules.SCOUTER, ArmorItem.Type.HELMET);
    public static final DeferredItem<SaiyanArmorItem> BATTLE_ARMOR = piece(SaiyanArmorRules.BATTLE_ARMOR, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<SaiyanArmorItem> WAIST_GUARD = piece(SaiyanArmorRules.WAIST_GUARD, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<SaiyanArmorItem> GREAVES = piece(SaiyanArmorRules.GREAVES, ArmorItem.Type.BOOTS);

    private static DeferredItem<SaiyanArmorItem> piece(String id, ArmorItem.Type type) {
        return REGISTER.register(id, () -> new SaiyanArmorItem(ALLOY, type));
    }

    private static ArmorMaterial material() {
        Map<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            m.put(ArmorItem.Type.HELMET, SaiyanArmorRules.DEFENSE[0]);
            m.put(ArmorItem.Type.CHESTPLATE, SaiyanArmorRules.DEFENSE[1]);
            m.put(ArmorItem.Type.LEGGINGS, SaiyanArmorRules.DEFENSE[2]);
            m.put(ArmorItem.Type.BOOTS, SaiyanArmorRules.DEFENSE[3]);
            m.put(ArmorItem.Type.BODY, SaiyanArmorRules.BODY_DEFENSE);
        });
        return new ArmorMaterial(defense, SaiyanArmorRules.ENCHANTABILITY, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(INGOT.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, SaiyanArmorRules.MATERIAL))), SaiyanArmorRules.TOUGHNESS, SaiyanArmorRules.KNOCKBACK);
    }

    /** The four pieces, head to feet. */
    public static List<Item> pieces() { return List.of(SCOUTER.get(), BATTLE_ARMOR.get(), WAIST_GUARD.get(), GREAVES.get()); }

    /** The creative menu: the armour into the first output (the armour tab), the ingot into the second (the miscellaneous tab). */
    public static void addTo(CreativeModeTab.Output armour, CreativeModeTab.Output misc) {
        for (Item i : pieces()) armour.accept(i);
        misc.accept(INGOT.get());
    }
}
