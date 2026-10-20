package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a vanilla armour piece needs an {@code ArmorMaterial}: the armour points of each piece, the enchantability, the equip sound, what mends it, the layer texture and the
 * toughness and knockback resistance. The two sets (Drake-Scale, Shadowsteel) get one each, built from the roster so the armour points of a piece are the number written in
 * {@code tools/loot/roster.py} (the material is a map of piece to points, not a copy of anyone else's). The layer is {@code nusmp:loot_<set>}, whose textures
 * ({@code loot_<set>_layer_1/2.png}) {@code tools/gen_loot_icons.py} paints. The repair ingredient is looked up when it is asked for (the item may not be registered yet when the
 * material is), and falls back to leather if the item is missing.
 */
public final class LootArmorMaterials {
    private LootArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);
    private static final Map<String, DeferredHolder<ArmorMaterial, ArmorMaterial>> BY_SET = new HashMap<>();

    static {
        LootRoster r = LootRoster.get();
        for (LootRoster.ArmourSetDef set : r.armourSets().values()) {
            String layer = "loot_" + set.id();
            BY_SET.put(set.id(), MATERIALS.register(layer, () -> create(r, set, layer)));
        }
    }

    private static ArmorMaterial create(LootRoster r, LootRoster.ArmourSetDef set, String layer) {
        Map<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            for (ArmorItem.Type t : ArmorItem.Type.values()) m.put(t, 0);
            for (LootRoster.ArmourDef a : r.armour()) if (a.set().equals(set.id())) m.put(LootArmorItem.typeOf(a.piece()), a.defense());
        });
        return new ArmorMaterial(defense, set.enchant(), set.id().equals("drakescale") ? SoundEvents.ARMOR_EQUIP_LEATHER : SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, set.repair())).orElse(Items.LEATHER)),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, layer))), (float) set.toughness(), (float) set.knockback());
    }

    /** The material of a set id ({@code drakescale}, {@code shadowsteel}). */
    public static Holder<ArmorMaterial> of(String setId) { return BY_SET.get(setId); }

    /** The durability factor of the set (the multiplier of the piece's base durability): scales are leather-light, shadowsteel is netherite-hard. */
    public static int durabilityFactor(String setId) { return setId.equals("shadowsteel") ? 30 : 22; }
}
