package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the ONE registry class of the records subsystem ({@code elite/records}), called by the single line in {@code EliteSystem}. It registers the server config
 * ({@code nusmp-elite-records-server.toml}), the player attachment, the network payloads, the data reload listener for {@code data/<ns>/elite/records|titles}, the game-bus handlers
 * ({@link RecordsService}), the {@code /records} command, the in-game self-tests ({@code elite.records.*}) and the four {@link EliteHooks} readers this subsystem owns
 * ({@code recordUnlocked}, {@code hasTitle}, {@code activeTitle}, {@code heldTitleCount}); on a client it also starts the screen, the key, the toast and the menu button. Nothing else of the mod
 * knows this system exists, and the other elite subsystems reach it only through {@link EliteHooks} (to read) and {@link RecordEvents} (to report). Safe to call twice.
 */
public final class RecordsSystem {
    private RecordsSystem() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, RecordsConfig.SPEC, "nusmp-elite-records-server.toml");
        RecordsAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(RecordsNet::registerPayloads);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new RecordsCatalog.Reload()));
        RecordsService.register();
        RecordsCommand.register();
        RecordsSelfTests.register();
        installHooks();
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.elite.records.RecordsClient.init(modBus);
    }

    /** The hooks this subsystem owns (see {@code EliteHooks}); they answer for online players and, since the server started, for players who logged out. */
    private static void installHooks() {
        EliteHooks.recordUnlocked = RecordsService::recordUnlocked;
        EliteHooks.hasTitle = RecordsService::hasTitle;
        EliteHooks.activeTitle = RecordsService::activeTitle;
        EliteHooks.heldTitleCount = RecordsService::heldTitleCount;
    }
}
