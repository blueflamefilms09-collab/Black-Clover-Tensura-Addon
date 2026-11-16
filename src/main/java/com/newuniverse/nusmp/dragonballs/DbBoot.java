package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Dragon Balls are one self-contained feature, so they have one entry point, the single line the mod constructor calls. It registers the 16 ball items and the Dragon Radar, the Shenron entity, the
 * server config {@code nusmp-dragonballs-server.toml} ({@code dragonBalls.*}), the sky packet, the server events (tick sweep, chat, ball item handling, deaths, login / respawn / clone for the permanent power,
 * start / stop), the commands ({@code /multiverse dragonballs ...}, {@code /dragonwish}) and the CI self-tests ({@code dragonballs.*}). Safe to call twice. The client half is
 * {@code client.dragonballs.DbClient}, called from the client block of the constructor; the creative tab lists {@link DbItems#creativeItems()}. See docs/dragon_balls.md.
 */
public final class DbBoot {
    private DbBoot() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    /** Shenron / Super Shenron in the sky during a ritual (never saved, never spawned by a player). */
    public static final DeferredHolder<EntityType<?>, EntityType<ShenronEntity>> SHENRON = ENTITIES.register("shenron",
            () -> EntityType.Builder.<ShenronEntity>of(ShenronEntity::new, MobCategory.MISC).sized(4.0f, 14.0f).fireImmune().noSave().noSummon().clientTrackingRange(16).updateInterval(20).build("shenron"));

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        DbItems.ITEMS.register(modBus);
        ENTITIES.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, DbConfig.SPEC, "nusmp-dragonballs-server.toml");
        modBus.addListener(DbNet::registerPayloads);
        DbSaiyanBridge.install();
        NeoForge.EVENT_BUS.addListener(DbManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(DbManager::onServerStarted);
        NeoForge.EVENT_BUS.addListener(DbManager::onServerStopping);
        NeoForge.EVENT_BUS.addListener(DbManager::onItemJoin);
        NeoForge.EVENT_BUS.addListener(DbManager::onItemLeave);
        NeoForge.EVENT_BUS.addListener(DbManager::onDeath);
        NeoForge.EVENT_BUS.addListener(DbChat::onChat);
        NeoForge.EVENT_BUS.addListener(DbChat::onLogout);
        NeoForge.EVENT_BUS.addListener(DbWishes::onLogin);
        NeoForge.EVENT_BUS.addListener(DbWishes::onRespawn);
        NeoForge.EVENT_BUS.addListener(DbWishes::onClone);
        NeoForge.EVENT_BUS.addListener(DbCommands::registerWish);
        DbSelfTests.register();
    }
}
