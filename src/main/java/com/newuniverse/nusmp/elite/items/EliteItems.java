package com.newuniverse.nusmp.elite.items;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.items.armor.NightingaleArmorItem;
import com.newuniverse.nusmp.elite.items.armor.NightingaleBonus;
import com.newuniverse.nusmp.elite.items.consumable.ChronicleBookItem;
import com.newuniverse.nusmp.elite.items.consumable.HealthPotionItem;
import com.newuniverse.nusmp.elite.items.consumable.HealthPotionTier;
import com.newuniverse.nusmp.elite.items.consumable.PotentialCatalystItem;
import com.newuniverse.nusmp.elite.items.consumable.RefItem;
import com.newuniverse.nusmp.elite.items.crate.CrateBlock;
import com.newuniverse.nusmp.elite.items.crate.CrateKeyItem;
import com.newuniverse.nusmp.elite.items.crate.CrateKeyLootModifier;
import com.newuniverse.nusmp.elite.items.crate.CrateTables;
import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.plush.PlushBlock;
import com.newuniverse.nusmp.elite.items.plush.PlushCollection;
import com.newuniverse.nusmp.elite.items.plush.PlushKinds;
import com.newuniverse.nusmp.elite.items.weapon.AbilityTables;
import com.newuniverse.nusmp.elite.items.weapon.EliteWeaponItem;
import com.newuniverse.nusmp.elite.items.weapon.RuneTable;
import com.newuniverse.nusmp.elite.items.weapon.RunicSwordItem;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
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
 * Why: the ONE registry class of the elite item subsystem ({@code elite/items}), started by one line in {@code EliteSystem}. It registers, in registers of its own (so the mod's shared
 * {@code NUItems} register is not touched): the six weapons, the four Nightingale pieces and their armour material, the Potential Catalyst, the three Health Potions, the Chronicle Book, the
 * Walpurgis Orb and Seal, the three crate blocks with their items and keys, and one plushie block and item per mob of {@link PlushKinds}; plus the data components, the sound events, the player
 * attachment, the key loot modifier type and the subsystem's own creative tab. Then it attaches the game-bus handlers (the swing charge, the Nightingale bonus, the plushie scan, the catalyst's
 * health on login and respawn) and the three datapack loaders (crate tables, technique files, rune table), registers the config file {@code nusmp-elite-items-server.toml}, the self-tests, and on the
 * client the 3D weapon renderer hook. Safe to call twice. Item ids come from {@link EliteIds} (the parity tests use the same list).
 */
public final class EliteItems {
    private EliteItems() {}

    private static boolean done;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, NUSMP.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NUSMP.MODID);

    // ------------------------------------------------------------------ weapons
    private static final Map<EliteWeaponItem.Def, DeferredItem<? extends EliteWeaponItem>> WEAPONS = new EnumMap<>(EliteWeaponItem.Def.class);

    static {
        for (EliteWeaponItem.Def d : EliteWeaponItem.Def.values())
            WEAPONS.put(d, d.runic() ? ITEMS.register(d.id(), () -> new RunicSwordItem(d)) : ITEMS.register(d.id(), () -> new EliteWeaponItem(d)));
    }

    public static DeferredItem<? extends EliteWeaponItem> weapon(EliteWeaponItem.Def d) { return WEAPONS.get(d); }

    // ------------------------------------------------------------------ Nightingale armour
    public static final Holder<ArmorMaterial> NIGHTINGALE = MATERIALS.register("nightingale", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 3);
                m.put(ArmorItem.Type.LEGGINGS, 6);
                m.put(ArmorItem.Type.CHESTPLATE, 8);
                m.put(ArmorItem.Type.HELMET, 3);
                m.put(ArmorItem.Type.BODY, 11);
            }),
            24, SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(Items.PHANTOM_MEMBRANE),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "nightingale"))), 2.0f, 0f));

    public static final DeferredItem<NightingaleArmorItem> NIGHTINGALE_HOOD = ITEMS.register("nightingale_hood", () -> new NightingaleArmorItem(NIGHTINGALE, ArmorItem.Type.HELMET));
    public static final DeferredItem<NightingaleArmorItem> NIGHTINGALE_CLOAK = ITEMS.register("nightingale_cloak", () -> new NightingaleArmorItem(NIGHTINGALE, ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<NightingaleArmorItem> NIGHTINGALE_LEGGINGS = ITEMS.register("nightingale_leggings", () -> new NightingaleArmorItem(NIGHTINGALE, ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<NightingaleArmorItem> NIGHTINGALE_BOOTS = ITEMS.register("nightingale_boots", () -> new NightingaleArmorItem(NIGHTINGALE, ArmorItem.Type.BOOTS));

    // ------------------------------------------------------------------ consumables and plain items
    public static final DeferredItem<PotentialCatalystItem> POTENTIAL_CATALYST = ITEMS.register("potential_catalyst", PotentialCatalystItem::new);
    private static final Map<HealthPotionTier, DeferredItem<HealthPotionItem>> POTIONS = new EnumMap<>(HealthPotionTier.class);

    static {
        for (HealthPotionTier t : HealthPotionTier.values()) POTIONS.put(t, ITEMS.register(t.itemId(), () -> new HealthPotionItem(t)));
    }

    public static DeferredItem<HealthPotionItem> healthPotion(HealthPotionTier t) { return POTIONS.get(t); }

    public static final DeferredItem<ChronicleBookItem> CHRONICLE_BOOK = ITEMS.register("chronicle_book", ChronicleBookItem::new);
    public static final DeferredItem<RefItem> WALPURGIS_ORB = ITEMS.register("walpurgis_orb", () -> new RefItem("walpurgis_orb", Rarity.EPIC));
    public static final DeferredItem<RefItem> WALPURGIS_SEAL = ITEMS.register("walpurgis_seal", () -> new RefItem("walpurgis_seal", Rarity.EPIC));

    // ------------------------------------------------------------------ crates and keys
    private static final Map<CrateTier, DeferredBlock<CrateBlock>> CRATES = new EnumMap<>(CrateTier.class);
    private static final Map<CrateTier, DeferredItem<BlockItem>> CRATE_ITEMS = new EnumMap<>(CrateTier.class);
    private static final Map<CrateTier, DeferredItem<CrateKeyItem>> KEYS = new EnumMap<>(CrateTier.class);

    static {
        for (CrateTier t : CrateTier.values()) {
            DeferredBlock<CrateBlock> b = BLOCKS.register(t.crateId(), () -> new CrateBlock(t));
            CRATES.put(t, b);
            CRATE_ITEMS.put(t, ITEMS.register(t.crateId(), () -> new BlockItem(b.get(), new Item.Properties().rarity(t == CrateTier.ELITE ? Rarity.EPIC : t == CrateTier.RARE ? Rarity.RARE : Rarity.UNCOMMON))));
            KEYS.put(t, ITEMS.register(t.keyId(), () -> new CrateKeyItem(t)));
        }
    }

    public static DeferredBlock<CrateBlock> crate(CrateTier t) { return CRATES.get(t); }

    public static DeferredItem<BlockItem> crateItem(CrateTier t) { return CRATE_ITEMS.get(t); }

    public static DeferredItem<CrateKeyItem> key(CrateTier t) { return KEYS.get(t); }

    // ------------------------------------------------------------------ plushies
    private static final Map<String, DeferredBlock<PlushBlock>> PLUSH_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> PLUSH_ITEMS = new LinkedHashMap<>();

    static {
        for (PlushKinds.Kind k : PlushKinds.ALL) {
            DeferredBlock<PlushBlock> b = BLOCKS.register(k.blockId(), () -> new PlushBlock(k));
            PLUSH_BLOCKS.put(k.id(), b);
            PLUSH_ITEMS.put(k.id(), ITEMS.register(k.blockId(), () -> new BlockItem(b.get(), new Item.Properties().stacksTo(16))));
        }
    }

    public static DeferredBlock<PlushBlock> plushBlock(String kindId) { return PLUSH_BLOCKS.get(kindId); }

    public static DeferredItem<BlockItem> plushItem(String kindId) { return PLUSH_ITEMS.get(kindId); }

    // ------------------------------------------------------------------ the creative tab
    /** Every item of the subsystem in the tab's order: weapons, armour, consumables, crates and keys, plushies. */
    public static List<Item> allItems() {
        List<Item> out = new ArrayList<>();
        for (EliteWeaponItem.Def d : EliteWeaponItem.Def.values()) out.add(WEAPONS.get(d).get());
        out.add(NIGHTINGALE_HOOD.get());
        out.add(NIGHTINGALE_CLOAK.get());
        out.add(NIGHTINGALE_LEGGINGS.get());
        out.add(NIGHTINGALE_BOOTS.get());
        out.add(POTENTIAL_CATALYST.get());
        for (HealthPotionTier t : HealthPotionTier.values()) out.add(POTIONS.get(t).get());
        out.add(CHRONICLE_BOOK.get());
        out.add(WALPURGIS_ORB.get());
        out.add(WALPURGIS_SEAL.get());
        for (CrateTier t : CrateTier.values()) {
            out.add(CRATE_ITEMS.get(t).get());
            out.add(KEYS.get(t).get());
        }
        for (PlushKinds.Kind k : PlushKinds.ALL) out.add(PLUSH_ITEMS.get(k.id()).get());
        return out;
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("elite_items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nusmp.elite_items"))
            .icon(() -> new ItemStack(WEAPONS.get(EliteWeaponItem.Def.ASTRAL_EDGE).get()))
            .displayItems((params, output) -> {
                for (Item i : allItems()) output.accept(i);
            }).build());

    // ------------------------------------------------------------------ start
    /** Starts the subsystem: registers, handlers, loaders, config, self-tests and (on the client) the 3D weapon hook. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        MATERIALS.register(modBus);
        TABS.register(modBus);
        EliteComponents.init(modBus);
        EliteSounds.init(modBus);
        EliteItemsData.init(modBus);
        CrateKeyLootModifier.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, EliteItemsConfig.SPEC, "nusmp-elite-items-server.toml");

        NeoForge.EVENT_BUS.addListener(CrateTables::onReload);
        NeoForge.EVENT_BUS.addListener(AbilityTables::onReload);
        NeoForge.EVENT_BUS.addListener(RuneTable::onReload);
        NeoForge.EVENT_BUS.addListener(EliteWeaponItem::onAttack);
        NeoForge.EVENT_BUS.addListener(NightingaleBonus::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlushCollection::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) PotentialCatalystItem.applyHealth(p);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) PotentialCatalystItem.applyHealth(p);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> EliteWeaponItem.forget(e.getEntity().getUUID()));

        EliteItemsSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.elite.items.EliteItemsClient.init(modBus);
    }
}
