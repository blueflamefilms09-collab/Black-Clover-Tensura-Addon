package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.avatar.ai.AvatarAi;
import com.newuniverse.nusmp.avatar.betrayal.BetrayalDimensionHandler;
import com.newuniverse.nusmp.avatar.goal.AvatarTargeting;
import com.newuniverse.nusmp.avatar.item.AvatarItems;
import com.newuniverse.nusmp.avatar.item.GenesisCoreDrops;
import com.newuniverse.nusmp.avatar.menu.AvatarMenus;
import com.newuniverse.nusmp.avatar.naming.NamingSleepEffect;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingEngine;
import com.newuniverse.nusmp.avatar.net.AvatarNet;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: one entry point for the whole Avatar / NPC creation system, so the lead wires it with a single line in the mod constructor
 * ({@code AvatarSystem.init(modEventBus, modContainer)}, at the same spot as {@code CompatItems.init()}, before {@code NUItems.ITEMS.register}). It registers
 * everything the system owns (the entity type, the Genesis Core item, the player attachments, the Naming Sleep effect, the race reload listener, the
 * command, the config), calls the hook points of the other two parts (menus and network of the GUI, the AI listeners), and holds the event handlers that are
 * rules rather than behaviour: the damage guard (an Avatar can never hurt its creator unless it betrayed them, never the creator's friends, and a betrayed
 * one hits for at most a quarter of the victim's health), the boss drop of the Genesis Core, the creator's tax refresh, and the cross-dimension wake pass.
 */
public final class AvatarSystem {
    private AvatarSystem() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    /** The creator's lasting facts (their Avatars, the pity record), saved and copied on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AvatarPlayerData>> PLAYER_DATA = ATTACHMENTS.register("avatar_player_data",
            () -> AttachmentType.builder(() -> AvatarPlayerData.EMPTY).serialize(AvatarPlayerData.CODEC).copyOnDeath().build());
    /** The pending creation roll: never saved, never copied (a roll dies with the session). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AvatarSession>> SESSION = ATTACHMENTS.register("avatar_session",
            () -> AttachmentType.builder(AvatarSession::new).build());

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);
    /** {@code nusmp:naming_sleep}: the creator's exhaustion after naming an Avatar. */
    public static final DeferredHolder<MobEffect, NamingSleepEffect> NAMING_SLEEP = EFFECTS.register("naming_sleep", NamingSleepEffect::new);

    private static boolean started;

    /** Registers everything. Call once from the mod constructor, before {@code NUItems.ITEMS.register(modEventBus)}. */
    public static void init(IEventBus modEventBus, ModContainer container) {
        if (started) return;
        started = true;
        AvatarItems.init();
        AvatarEntities.ENTITIES.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        EFFECTS.register(modEventBus);
        modEventBus.addListener(AvatarEntities::attributes);
        modEventBus.addListener(AvatarNet::register);
        modEventBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(AvatarNamingHook::register));
        AvatarMenus.init(modEventBus);
        com.newuniverse.nusmp.avatar.ritual.SoulForgeSystem.init(modEventBus);                      // 1.58.0: the Soul Forge altar, its ritual and the creator screen (before NUItems.ITEMS.register)
        if (container != null) container.registerConfig(ModConfig.Type.SERVER, AvatarConfig.SPEC, "nusmp-avatar-server.toml");

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((AddReloadListenerEvent e) -> e.addListener(new AvatarRaces()));
        game.addListener((RegisterCommandsEvent e) -> AvatarCommand.register(e.getDispatcher()));
        game.addListener(EventPriority.LOW, AvatarSystem::onIncomingDamage);
        game.addListener(EventPriority.LOWEST, GenesisCoreDrops::onDeath);
        game.addListener(AvatarSystem::onLogin);
        game.addListener(AvatarSystem::onServerTick);
        AvatarAi.registerEvents(game);
    }

    /** Convenience for callers that only have the mod bus. */
    public static void init(IEventBus modEventBus) { init(modEventBus, null); }

    // ================================================================ helpers
    /** Drops an Avatar id from its creator's list (called when the Avatar dies or is removed). */
    public static void removeFromPlayer(ServerPlayer p, java.util.UUID avatarId) {
        AvatarPlayerData d = p.getData(PLAYER_DATA);
        if (d.avatars().contains(avatarId)) p.setData(PLAYER_DATA, d.withoutAvatar(avatarId));
    }

    // ================================================================ rules
    /**
     * The damage guard. Whatever hurts through an Avatar (its fists, its signature moves, a spell the AI casts for it) passes here:
     * the creator is never hurt by a loyal or independent Avatar, nor are the creator's squadmates, pets or other Avatars; a betrayed Avatar hits for at most
     * {@code betrayalHitCap} of the victim's maximum health.
     */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof AvatarEntity a)) return;
        LivingEntity victim = e.getEntity();
        if (!AvatarTargeting.canHarm(a, victim)) { e.setCanceled(true); return; }
        if (a.isBetrayed() && victim instanceof net.minecraft.world.entity.player.Player) {
            float cap = (float) (victim.getMaxHealth() * AvatarConfig.betrayalHitCap());
            if (e.getAmount() > cap) e.setAmount(cap);
        }
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) TensuraNamingEngine.refreshTax(p);
    }

    static void onServerTick(ServerTickEvent.Post e) {
        if (!AvatarConfig.enabled()) return;
        if (e.getServer().getTickCount() % BetrayalDimensionHandler.WAKE_INTERVAL == 0) BetrayalDimensionHandler.wake(e.getServer());
    }
}
