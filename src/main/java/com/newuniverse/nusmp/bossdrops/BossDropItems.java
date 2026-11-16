package com.newuniverse.nusmp.bossdrops;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Why: every item the boss drop framework adds (docs/bosses/boss_drops.md), in one place with its own registers (attached to the mod bus by {@link BossDropBoot}):
 * <ul>
 *   <li>23 trophies {@code trophy_<boss>}: one relic shape (made in Blender, tools/blender/boss_trophy.py) recoloured per boss, drawn as a small solid object in hand by the 3D weapon renderer
 *       ({@link #renderables()}); they are crafting material and sit in the item tag {@code anime_multiverse:boss_trophies};</li>
 *   <li>the Trophy Plate armour (4 pieces, crafted from any trophies, between iron and diamond);</li>
 *   <li>Lucifero's armour (4 pieces, drop only, between netherite and the Boss Core armour).</li>
 * </ul>
 * The armour pieces are plain {@code ArmorItem}s with a vanilla layer texture, exactly like the Boss Core armour (no custom worn 3D model).
 */
public final class BossDropItems {
    private BossDropItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);
    public static final TagKey<Item> TROPHIES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "boss_trophies"));

    private static final Map<String, DeferredItem<Item>> TROPHY = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<ArmorItem>> GEAR = new LinkedHashMap<>();

    private static final ArmorItem.Type[] TYPES = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

    /** Lucifero: heavier than netherite (3/8/6/3, toughness 3, knockback 0.1), lighter than the Aegis (4/9/7/4, toughness 4). */
    private static final int[] LUCIFERO_POINTS = {4, 8, 6, 3};
    /** The Trophy Plate: a step over iron (2/6/5/2), under diamond (3/8/6/3). */
    private static final int[] PLATE_POINTS = {3, 6, 5, 2};

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LUCIFERO_MATERIAL = MATERIALS.register("lucifero",
            () -> material(LUCIFERO_POINTS, 30, 3.5f, 0.05f, "lucifero", () -> Ingredient.of(trophyOrDefault("lucifero"))));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PLATE_MATERIAL = MATERIALS.register("trophy_plate",
            () -> material(PLATE_POINTS, 18, 1.0f, 0.0f, "trophy_plate", () -> Ingredient.of(TROPHIES)));

    static {
        for (String boss : BossDropCatalog.BOSSES) {
            TROPHY.put(boss, REGISTER.register(BossDropCatalog.trophy(boss), () -> new TrophyItem(boss, new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant())));
        }
        for (int i = 0; i < 4; i++) {
            ArmorItem.Type type = TYPES[i];
            GEAR.put(BossDropCatalog.LUCIFERO_PIECES.get(i), REGISTER.register(BossDropCatalog.LUCIFERO_PIECES.get(i),
                    () -> new GearItem(LUCIFERO_MATERIAL, type, "lucifero", new Item.Properties().stacksTo(1).durability(type.getDurability(40)).rarity(Rarity.EPIC).fireResistant())));
            GEAR.put(BossDropCatalog.PLATE_PIECES.get(i), REGISTER.register(BossDropCatalog.PLATE_PIECES.get(i),
                    () -> new GearItem(PLATE_MATERIAL, type, "trophy_plate", new Item.Properties().stacksTo(1).durability(type.getDurability(33)).rarity(Rarity.UNCOMMON))));
        }
    }

    private static Item trophyOrDefault(String boss) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, BossDropCatalog.trophy(boss))).orElse(Items.NETHERITE_INGOT);
    }

    private static ArmorMaterial material(int[] points, int enchant, float toughness, float knockback, String layer, Supplier<Ingredient> repair) {
        Map<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            for (int i = 0; i < 4; i++) m.put(TYPES[i], points[i]);
        });
        return new ArmorMaterial(defense, enchant, SoundEvents.ARMOR_EQUIP_NETHERITE, repair,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, layer))), toughness, knockback);
    }

    /** The armour points of a piece of Lucifero's set (0 helmet .. 3 boots), for the tests and the docs. */
    public static int luciferoPoints(int piece) { return LUCIFERO_POINTS[piece]; }

    public static int platePoints(int piece) { return PLATE_POINTS[piece]; }

    public static Item trophy(String boss) { return TROPHY.get(boss).get(); }

    public static Item gear(String id) { return GEAR.get(id).get(); }

    /** The list the client's 3D hand renderer reads ({@code client.WeaponRenderer}): the 23 trophies, drawn as small solid relics. */
    public static List<Supplier<? extends Item>> renderables() { return new ArrayList<>(TROPHY.values()); }

    /** The creative menu: trophies into the first output (the relic tab), every armour piece into the second (the armour tab). */
    public static void addTo(CreativeModeTab.Output trophies, CreativeModeTab.Output armour) {
        for (DeferredItem<Item> d : TROPHY.values()) trophies.accept(d.get());
        for (DeferredItem<ArmorItem> d : GEAR.values()) armour.accept(d.get());
    }

    /** A trophy: crafting material that remembers whose it is (the tooltip names the boss). */
    public static final class TrophyItem extends Item {
        private final String boss;

        public TrophyItem(String boss, Properties p) {
            super(p);
            this.boss = boss;
        }

        public String boss() { return boss; }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.trophy", "A trophy of a boss kill: material for the Trophy Plate armour.").withStyle(ChatFormatting.GRAY));
        }
    }

    /** One armour piece of the two families here: an ordinary {@code ArmorItem} with a tooltip line. */
    public static final class GearItem extends ArmorItem {
        private final String family;

        public GearItem(Holder<ArmorMaterial> material, Type type, String family, Properties p) {
            super(material, type, p);
            this.family = family;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.boss_gear." + family, family.equals("lucifero")
                    ? "Armour of the Spade devil Lucifero: it drops from him." : "Plate beaten from boss trophies.").withStyle(ChatFormatting.GRAY));
        }
    }
}
