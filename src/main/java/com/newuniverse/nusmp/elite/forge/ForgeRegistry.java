package com.newuniverse.nusmp.elite.forge;

import com.newuniverse.nusmp.NUCreativeTab;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlock;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlockEntity;
import com.newuniverse.nusmp.elite.forge.data.ForgeRecipes;
import com.newuniverse.nusmp.elite.forge.item.UpgradeKitItem;
import com.newuniverse.nusmp.elite.forge.net.ForgeNet;
import com.newuniverse.nusmp.elite.forge.net.ForgeServer;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.session.ForgeSessions;
import com.newuniverse.nusmp.elite.forge.store.ForgeStore;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Forge & Custom Crafting System (the "forge" section of the EliteTensuraAddon inventory, docs/elite/forge.md) is one subsystem with ONE entry point,
 * {@link #init}, wired by a single line in {@code EliteSystem}. It owns its registers (four station blocks and their items, the three upgrade kits, the block entity
 * type, the data components, the progress attachment), its server config, its payloads, the data reload listener for the recipes, its commands, its self-tests, and the
 * two {@code EliteHooks} it provides. Nothing is added to a shared registry class: the items go to the creative tab by the tab-contents event. Everything but the
 * registries is inert while the master switch {@code enabled} of {@code nusmp-elite-forge-server.toml} is off.
 */
public final class ForgeRegistry {
    private ForgeRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);

    /** The four station blocks, in tier order. */
    public static final List<DeferredBlock<ForgeStationBlock>> STATIONS = new ArrayList<>();
    private static final Map<StationTier, DeferredBlock<ForgeStationBlock>> STATION_BY_TIER = new EnumMap<>(StationTier.class);
    private static final Map<StationTier, DeferredItem<BlockItem>> STATION_ITEMS = new EnumMap<>(StationTier.class);
    private static final Map<StationTier, DeferredItem<UpgradeKitItem>> KITS = new EnumMap<>(StationTier.class);

    static {
        for (StationTier t : StationTier.values()) {
            DeferredBlock<ForgeStationBlock> b = BLOCKS.register("forge_station_" + t.id(), () -> new ForgeStationBlock(t, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(4.0f, 30.0f).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion()
                    .lightLevel(s -> s.getValue(ForgeStationBlock.LIT) ? 13 : 4 + t.index())));
            STATIONS.add(b);
            STATION_BY_TIER.put(t, b);
            STATION_ITEMS.put(t, ITEMS.register("forge_station_" + t.id(), () -> new BlockItem(b.get(), new Item.Properties().rarity(rarity(t)))));
            if (t.prev() != null) {
                KITS.put(t, ITEMS.register("forge_upgrade_kit_" + t.id(), () -> new UpgradeKitItem(t, new Item.Properties().stacksTo(16).rarity(rarity(t)))));
            }
        }
    }

    private static Rarity rarity(StationTier t) {
        return switch (t) {
            case BASIC -> Rarity.COMMON;
            case MAGISTEEL -> Rarity.UNCOMMON;
            case MYTHRIL -> Rarity.RARE;
            case DRAGONFORGE -> Rarity.EPIC;
        };
    }

    public static DeferredBlock<ForgeStationBlock> station(StationTier t) { return STATION_BY_TIER.get(t); }

    public static DeferredItem<BlockItem> stationItem(StationTier t) { return STATION_ITEMS.get(t); }

    /** The upgrade kit that makes a station of tier {@code t} (null for Basic, which is crafted directly). */
    public static DeferredItem<UpgradeKitItem> kit(StationTier t) { return KITS.get(t); }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeStationBlockEntity>> STATION_BE = BLOCK_ENTITIES.register("forge_station",
            () -> BlockEntityType.Builder.of(ForgeStationBlockEntity::new, STATIONS.stream().map(DeferredBlock::get).toArray(ForgeStationBlock[]::new)).build(null));

    /** Everything the creative tab should offer: the four stations, then the three kits. */
    public static List<Item> creativeItems() {
        List<Item> out = new ArrayList<>();
        for (StationTier t : StationTier.values()) out.add(stationItem(t).get());
        for (StationTier t : StationTier.values()) if (kit(t) != null) out.add(kit(t).get());
        return out;
    }

    /** The one entry point, called by {@code EliteSystem} from its forge section. */
    public static void init(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ForgeComponents.COMPONENTS.register(modBus);
        ForgeStore.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ForgeConfig.SPEC, "nusmp-elite-forge-server.toml");
        modBus.addListener(ForgeNet::register);
        modBus.addListener(ForgeRegistry::onTabs);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new ForgeRecipes()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ForgeCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ForgeStore.serverStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            ForgeStore.serverStopped();
            ForgeSessions.clearAll();
        });
        NeoForge.EVENT_BUS.addListener(ForgeRegistry::onLogin);
        NeoForge.EVENT_BUS.addListener(ForgeRegistry::onLogout);
        NeoForge.EVENT_BUS.addListener(ForgeRegistry::onServerTick);
        ForgeStore.install();
        ForgeSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.elite.forge.ForgeClient.init(modBus);
    }

    private static void onTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(NUCreativeTab.MAIN.getKey()) || e.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            for (Item it : creativeItems()) e.accept(it);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && ForgeConfig.enabled()) {
            ForgeStore.mirror(sp);
            ForgeServer.sendSync(sp);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            ForgeSessions.Session s = ForgeSessions.drop(sp.getUUID());
            if (s != null && sp.getServer() != null) {
                var level = sp.getServer().getLevel(s.dimension());
                if (level != null && !ForgeSessions.activeAt(level, s.pos())) ForgeStationBlock.setLit(level, s.pos(), false);
            }
        }
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 200 != 0 || ForgeSessions.openCount() == 0) return;
        for (ForgeSessions.Session s : ForgeSessions.expire()) {
            var level = e.getServer().getLevel(s.dimension());
            if (level != null && !ForgeSessions.activeAt(level, s.pos())) ForgeStationBlock.setLit(level, s.pos(), false);
        }
    }
}
