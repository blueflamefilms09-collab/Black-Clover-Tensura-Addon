package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * One armour material per HEAD or CHEST relic, in a register of the relics' own (register {@link #MATERIALS} on the mod bus). The layer is named
 * {@code nusmp:relic_<id>}: the client paints its texture at run time ({@code client.relics.RelicTextures}) unless a resource pack ships a PNG for it.
 */
public final class RelicArmorMaterials {
    private RelicArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);

    /** The material name of a relic id (its layer texture is textures/models/armor/&lt;name&gt;_layer_1.png). */
    public static String name(String relicId) { return "relic_" + relicId; }

    /** Registers the material of this armour def; call it once per def while registering items. */
    static Holder<ArmorMaterial> of(RelicDef def) {
        int d = def.armorDefence();
        String name = name(def.id());
        return MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.BOOTS, 0);
                    m.put(ArmorItem.Type.LEGGINGS, 0);
                    m.put(ArmorItem.Type.CHESTPLATE, d);
                    m.put(ArmorItem.Type.HELMET, d);
                    m.put(ArmorItem.Type.BODY, d);
                }),
                20, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(Items.NETHERITE_INGOT),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, name))),
                (float) def.armorToughness(), 0f));
    }
}
