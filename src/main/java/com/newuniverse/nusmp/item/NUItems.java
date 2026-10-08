package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** Magic Knight robes, relics and magic tools. */
public final class NUItems {
    private NUItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);

    // ---------------------------------------------------------------- robes (one chest piece = the whole set)
    private static Holder<ArmorMaterial> material(String id, int chestDefense, float toughness) {
        return MATERIALS.register(id, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.BOOTS, 1); m.put(ArmorItem.Type.LEGGINGS, 2);
                    m.put(ArmorItem.Type.CHESTPLATE, chestDefense); m.put(ArmorItem.Type.HELMET, 1); m.put(ArmorItem.Type.BODY, chestDefense);
                }),
                15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.WHITE_WOOL),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id))), toughness, 0f));
    }

    /** One robe set = hood, robe (chest, carries the bonus), leggings, boots. */
    public record RobeSet(DeferredItem<RobeItem> hood, DeferredItem<RobeItem> chest, DeferredItem<RobeItem> legs, DeferredItem<RobeItem> boots) {}
    public static final java.util.List<RobeSet> SETS = new java.util.ArrayList<>();

    private static DeferredItem<RobeItem> robe(String id, RobeItem.Kind kind, int defense, float toughness) {
        Holder<ArmorMaterial> mat = material(id, defense, toughness);
        RobeSet set = new RobeSet(
                ITEMS.register(id + "_hood", () -> new RobeItem(mat, kind, ArmorItem.Type.HELMET, id)),
                ITEMS.register(id, () -> new RobeItem(mat, kind, ArmorItem.Type.CHESTPLATE, id)),
                ITEMS.register(id + "_leggings", () -> new RobeItem(mat, kind, ArmorItem.Type.LEGGINGS, id)),
                ITEMS.register(id + "_boots", () -> new RobeItem(mat, kind, ArmorItem.Type.BOOTS, id)));
        SETS.add(set);
        return set.chest();
    }

    public static final DeferredItem<RobeItem> ROBE_JUNIOR = robe("magic_knight_robe_junior", RobeItem.Kind.JUNIOR, 3, 0f);
    public static final DeferredItem<RobeItem> ROBE_SENIOR = robe("magic_knight_robe_senior", RobeItem.Kind.SENIOR, 4, 0f);
    public static final DeferredItem<RobeItem> GOLDEN_DAWN = robe("golden_dawn_mantle", RobeItem.Kind.GOLDEN_DAWN, 5, 1f);
    public static final DeferredItem<RobeItem> BLACK_BULL = robe("black_bull_robe", RobeItem.Kind.BLACK_BULL, 5, 1f);
    public static final DeferredItem<RobeItem> SILVER_EAGLE = robe("silver_eagle_cloak", RobeItem.Kind.SILVER_EAGLE, 6, 1f);
    public static final DeferredItem<RobeItem> CRIMSON_LION = robe("crimson_lion_mantle", RobeItem.Kind.CRIMSON_LION, 5, 1f);
    public static final DeferredItem<RobeItem> CORAL_PEACOCK = robe("coral_peacock_robe", RobeItem.Kind.CORAL_PEACOCK, 5, 1f);
    public static final DeferredItem<RobeItem> HEART_UNIFORM = robe("heart_kingdom_uniform", RobeItem.Kind.HEART, 5, 1f);
    public static final DeferredItem<RobeItem> DIAMOND_COAT = robe("diamond_mage_coat", RobeItem.Kind.DIAMOND, 5, 1f);
    public static final DeferredItem<RobeItem> SPADE_COAT = robe("spade_war_coat", RobeItem.Kind.SPADE, 6, 2f);
    public static final DeferredItem<RobeItem> DEVIL_COAT = robe("devil_bound_coat", RobeItem.Kind.DEVIL, 7, 2f);

    // ---------------------------------------------------------------- relics
    private static DeferredItem<RelicItem> relic(String id, RelicItem.Kind kind, Rarity r) {
        return ITEMS.register(id, () -> new RelicItem(kind, new Item.Properties().stacksTo(kind.stack).rarity(r)));
    }

    public static final DeferredItem<RelicItem> COMMUNICATION_DEVICE = relic("communication_magic_device", RelicItem.Kind.COMMUNICATION, Rarity.UNCOMMON);
    public static final DeferredItem<RelicItem> RUNE_STONE = relic("mana_method_rune_stone", RelicItem.Kind.RUNE_STONE, Rarity.UNCOMMON);
    public static final DeferredItem<RelicItem> SPIRIT_CHARM = relic("spirit_charm", RelicItem.Kind.SPIRIT_CHARM, Rarity.RARE);
    public static final DeferredItem<RelicItem> BOND_THREAD = relic("bond_thread", RelicItem.Kind.BOND_THREAD, Rarity.RARE);
    public static final DeferredItem<RelicItem> FORTUNE_DIE = relic("fortune_die", RelicItem.Kind.FORTUNE_DIE, Rarity.EPIC);
    public static final DeferredItem<RelicItem> GRIMOIRE_CHAIN = relic("grimoire_chain", RelicItem.Kind.GRIMOIRE_CHAIN, Rarity.UNCOMMON);
    public static final DeferredItem<RelicItem> ANTI_BIRD_CHARM = relic("anti_bird_charm", RelicItem.Kind.ANTI_BIRD, Rarity.COMMON);
    public static final DeferredItem<RelicItem> RECOVERY_SALVE = relic("recovery_salve", RelicItem.Kind.RECOVERY_SALVE, Rarity.UNCOMMON);
    public static final DeferredItem<RelicItem> WRITTEN_CONSENT = relic("written_consent", RelicItem.Kind.CONSENT, Rarity.COMMON);
    public static final DeferredItem<RelicItem> DEVIL_CONTRACT = relic("devil_contract", RelicItem.Kind.DEVIL_CONTRACT, Rarity.EPIC);
    public static final DeferredItem<RelicItem> GAUCHE_MIRROR = relic("gauches_hand_mirror", RelicItem.Kind.GAUCHE_MIRROR, Rarity.RARE);   // 0.34
    // 0.52: Zagred's drops (see BossRelics)
    public static final DeferredItem<RelicItem> SHROUD_OF_MARGINS = relic("shroud_of_margins", RelicItem.Kind.MARGINS, Rarity.EPIC);
    public static final DeferredItem<RelicItem> CIRCLET_OF_THOUGHT = relic("circlet_of_quickened_thought", RelicItem.Kind.QUICKENED, Rarity.EPIC);
    public static final DeferredItem<Item> HEART_OF_WORDS = ITEMS.register("heart_of_words", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    // ---------------------------------------------------------------- magic tools (attribute conduction)
    public static final DeferredItem<SwordItem> MAGIC_TOOL_SWORD = ITEMS.register("magic_tool_sword",
            () -> new MagicToolSword(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))));
    public static final DeferredItem<SwordItem> MAGIC_TOOL_SPEAR = ITEMS.register("magic_tool_spear",
            () -> new MagicToolSword(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.9f))));
    public static final DeferredItem<MagicToolBow> MAGIC_TOOL_BOW = ITEMS.register("magic_tool_bow",
            () -> new MagicToolBow(new Item.Properties().durability(500)));

    // ---------------------------------------------------------------- swords
    private static DeferredItem<MagicWeaponItem> weapon(String id, MagicWeaponItem.Kind kind) {
        return ITEMS.register(id, () -> new MagicWeaponItem(kind));
    }
    public static final DeferredItem<MagicWeaponItem> DEMON_SLASHER_KATANA = weapon("demon_slasher_katana", MagicWeaponItem.Kind.DEMON_SLASHER_KATANA);
    public static final DeferredItem<MagicWeaponItem> MIASMA_KATANA = weapon("miasma_infused_katana", MagicWeaponItem.Kind.MIASMA_KATANA);
    public static final DeferredItem<MagicWeaponItem> SPELL_FORGED_RAPIER = weapon("spell_forged_rapier", MagicWeaponItem.Kind.SPELL_FORGED_RAPIER);
    public static final DeferredItem<MagicWeaponItem> SEVERING_GREATSWORD = weapon("severing_greatsword", MagicWeaponItem.Kind.SEVERING_GREATSWORD);
    // 0.48: the Genesis Demon-Slayer replaces the 0.28 sword at the same id (Black Divider kept; see DemonSlayerSwordItem)
    public static final DeferredItem<MagicWeaponItem> DEMON_SLAYER = ITEMS.register("demon_slayer_sword", () -> new DemonSlayerSwordItem());
    public static final DeferredItem<MagicWeaponItem> DEMON_DWELLER = weapon("demon_dweller_sword", MagicWeaponItem.Kind.DEMON_DWELLER);
    public static final DeferredItem<MagicWeaponItem> DEMON_DESTROYER = weapon("demon_destroyer_sword", MagicWeaponItem.Kind.DEMON_DESTROYER);
    // 0.28: Licht's white swords (Sword Magic), drawn from the grimoire
    public static final DeferredItem<MagicWeaponItem> LICHT_DWELLER = weapon("licht_dweller_sword", MagicWeaponItem.Kind.LICHT_DWELLER);
    public static final DeferredItem<MagicWeaponItem> LICHT_DESTROYER = weapon("licht_destroyer_sword", MagicWeaponItem.Kind.LICHT_DESTROYER);
    // 0.32: the Rimeheart Runeblade
    public static final DeferredItem<MagicWeaponItem> RIMEHEART_RUNEBLADE = weapon("rimeheart_runeblade", MagicWeaponItem.Kind.RIMEHEART);
    // 0.52: Zagred's quill-blade
    public static final DeferredItem<MagicWeaponItem> LAST_WORD = weapon("last_word", MagicWeaponItem.Kind.LAST_WORD);

    // 0.44: Painting Magic's palette & brush (manifested by a Painting grimoire; see book.PaintStudio)
    public static final DeferredItem<PaintToolItem> PAINT_BRUSH = ITEMS.register("paint_brush", () -> new PaintToolItem(PaintToolItem.Kind.BRUSH));
    public static final DeferredItem<PaintToolItem> PAINT_PALETTE = ITEMS.register("paint_palette", () -> new PaintToolItem(PaintToolItem.Kind.PALETTE));

    // 0.47: Kotodama's otherworldly trident (spoken into being, bound; see book.KotodamaWords). Not in the creative tab.
    public static final DeferredItem<OtherworldTridentItem> OTHERWORLD_TRIDENT = ITEMS.register("otherworld_trident", OtherworldTridentItem::new);

    /** Spirit Lord Skill: grants the non-grimoire Spirit Lord path. */
    public static final DeferredItem<SpiritLordSkillItem> SPIRIT_LORD_SKILL = ITEMS.register("spirit_lord_skill", SpiritLordSkillItem::new);
    /** Physical key charge collected through Key Magic and consumed by Janus Abigail. */
    public static final DeferredItem<Item> MAGIC_KEY = ITEMS.register("magic_key",
            () -> new MagicKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static List<DeferredItem<? extends Item>> all() {
        List<DeferredItem<? extends Item>> l = new java.util.ArrayList<>();
        for (RobeSet s : SETS) { l.add(s.hood()); l.add(s.chest()); l.add(s.legs()); l.add(s.boots()); }
        l.addAll(List.of(DEMON_SLASHER_KATANA, MIASMA_KATANA, SPELL_FORGED_RAPIER, SEVERING_GREATSWORD, DEMON_SLAYER, DEMON_DWELLER, DEMON_DESTROYER, LICHT_DWELLER, LICHT_DESTROYER, RIMEHEART_RUNEBLADE));
        l.addAll(List.of(COMMUNICATION_DEVICE, RUNE_STONE, SPIRIT_CHARM, BOND_THREAD, FORTUNE_DIE,
                GRIMOIRE_CHAIN, ANTI_BIRD_CHARM, RECOVERY_SALVE, WRITTEN_CONSENT, DEVIL_CONTRACT, GAUCHE_MIRROR, MAGIC_TOOL_SWORD, MAGIC_TOOL_SPEAR, MAGIC_TOOL_BOW, SPIRIT_LORD_SKILL));
        l.addAll(List.of(PAINT_BRUSH, PAINT_PALETTE));                                          // 0.44
        l.addAll(List.of(LAST_WORD, SHROUD_OF_MARGINS, CIRCLET_OF_THOUGHT, HEART_OF_WORDS));    // 0.52
        l.add(MAGIC_KEY);
        return l;
    }
}
