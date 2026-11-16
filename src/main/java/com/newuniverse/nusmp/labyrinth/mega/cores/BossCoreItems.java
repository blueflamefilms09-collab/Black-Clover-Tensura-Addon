package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Kit;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Piece;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the seven cores and the twenty-eight armour pieces (seven sets of four) are registered in a LOOP over {@link Kit} and {@link Piece}, so a set added to the enum is a core, four pieces and a material at once.
 * One material per set carries its armour points, toughness and knockback resistance (the set's layer texture is {@code anime_multiverse:bosscore_<set>}, painted by
 * {@code tools/gen_boss_core_assets.py}); the health and attack damage come from {@link BossCoreEvents}. The Wellspring is one set per resource (four sets) because a piece is one item with one effect: no
 * stack component has to travel through a recipe.
 */
public final class BossCoreItems {
    private BossCoreItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);

    private static final Map<Kit, DeferredItem<BossCoreItem>> CORES = new EnumMap<>(Kit.class);
    private static final Map<Kit, DeferredHolder<ArmorMaterial, ArmorMaterial>> BY_KIT = new EnumMap<>(Kit.class);
    private static final Map<String, DeferredItem<BossCoreArmorItem>> PIECES = new LinkedHashMap<>();

    static {
        for (Kit kit : Kit.values()) {
            DeferredItem<BossCoreItem> core = REGISTER.register(kit.coreId(), () -> new BossCoreItem(kit));
            CORES.put(kit, core);
            String layer = "bosscore_" + kit.id();
            DeferredHolder<ArmorMaterial, ArmorMaterial> mat = MATERIALS.register(layer, () -> material(kit, layer));
            BY_KIT.put(kit, mat);
            for (Piece p : Piece.values()) {
                ArmorItem.Type type = typeOf(p);
                ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "boss_core_" + kit.pieceId(p));
                DeferredItem<BossCoreArmorItem> piece = REGISTER.register(kit.pieceId(p), () -> new BossCoreArmorItem(mat, type, kit, p, modId));
                PIECES.put(kit.pieceId(p), piece);
            }
        }
    }

    public static ArmorItem.Type typeOf(Piece p) {
        switch (p) {
            case HELMET: return ArmorItem.Type.HELMET;
            case CHESTPLATE: return ArmorItem.Type.CHESTPLATE;
            case LEGGINGS: return ArmorItem.Type.LEGGINGS;
            default: return ArmorItem.Type.BOOTS;
        }
    }

    /** The armour points of a piece of a set: the Aegis is the heaviest, the Striker next, the Wellsprings and the Ascendant a light netherite. Toughness and knockback follow the set. */
    static int points(Kit kit, Piece p) {
        int[] v;
        switch (kit.family()) {
            case AEGIS: v = new int[] {4, 9, 7, 4}; break;
            case STRIKER: v = new int[] {3, 8, 6, 3}; break;
            default: v = new int[] {3, 7, 5, 3}; break;
        }
        return v[p.ordinal()];
    }

    private static ArmorMaterial material(Kit kit, String layer) {
        Map<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            for (Piece p : Piece.values()) m.put(typeOf(p), points(kit, p));
        });
        float toughness = kit.family() == BossCoreRules.Family.AEGIS ? 4.0f : kit.family() == BossCoreRules.Family.STRIKER ? 3.0f : 2.0f;
        float knockback = kit.family() == BossCoreRules.Family.AEGIS ? 0.1f : 0.0f;
        return new ArmorMaterial(defense, 25, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, kit.coreId())).orElse(Items.NETHERITE_INGOT)),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, layer))), toughness, knockback);
    }

    public static Item core(Kit kit) { return CORES.get(kit).get(); }

    public static List<Item> allCores() {
        List<Item> out = new ArrayList<>();
        for (Kit k : Kit.values()) out.add(CORES.get(k).get());
        return out;
    }

    public static Item piece(Kit kit, Piece p) { return PIECES.get(kit.pieceId(p)).get(); }

    public static List<Item> allPieces() {
        List<Item> out = new ArrayList<>();
        for (DeferredItem<BossCoreArmorItem> d : PIECES.values()) out.add(d.get());
        return out;
    }

    /** The creative menu: the cores into the first output (the dungeon tab), the armour into the second (the armour tab). */
    public static void addTo(CreativeModeTab.Output cores, CreativeModeTab.Output armour) {
        for (Item i : allCores()) cores.accept(i);
        for (Item i : allPieces()) armour.accept(i);
    }

    public static Holder<ArmorMaterial> material(Kit kit) { return BY_KIT.get(kit); }
}
