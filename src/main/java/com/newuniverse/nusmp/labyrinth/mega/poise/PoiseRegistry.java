package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.NUSMP;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: the registration entry point of the poise layer, the pattern of every package of the Mega-Dungeon: one {@link #init} (mod bus: the attachment type, the in-game self-tests) and one
 * {@link #register} (game bus: the telegraph table's reload listener, the damage and knockback handlers, the bearers' tick, the passivity handlers), both called from {@code MegaSystem}
 * (an append-only pair of lines). The config is the {@code [foundry.poise]} section of {@code FoundryConfig}, so no new config file needs registering. The attachment {@code nusmp:mega_poise}
 * is written only for bearers, so a world with no elite or captain stores nothing.
 */
public final class PoiseRegistry {
    private PoiseRegistry() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PoiseData>> POISE = ATTACHMENTS.register("mega_poise",
            () -> AttachmentType.builder(() -> new PoiseData()).serialize(PoiseData.CODEC, d -> d.kind != PoiseBearers.Kind.NONE).build());

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        PoiseSelfTests.register();
        com.newuniverse.nusmp.labyrinth.mega.foundry.TelegraphSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new TelegraphData.Reload()));
        // the passive dungeon first: a blow between two monsters is cancelled before the poise layer ever sees it
        gameBus.addListener(EventPriority.HIGH, Passivity::onIncomingDamage);
        gameBus.addListener(EventPriority.HIGH, Passivity::onChangeTarget);
        gameBus.addListener(PoiseSystem::onIncomingDamage);
        gameBus.addListener(PoiseSystem::onKnockBack);
        gameBus.addListener(PoiseSystem::onJoin);
        gameBus.addListener(PoiseSystem::onServerTick);
    }
}
