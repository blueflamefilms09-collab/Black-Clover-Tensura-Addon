package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.skilltree.item.LegacyLook;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-skillbook-look-client.toml} of the 3.26.0 Skill Book look (it only changes what THIS client draws; nothing is sent, the server never registers it):
 * <ul>
 *   <li>{@code skillbookLook.legacyLook} (default false): true brings back the 1.82.0 grey covers, the old sprite and the always-glint. Mirrored into {@link LegacyLook#legacy}.</li>
 *   <li>{@code skillbookLook.dropShimmer} (default true): rare and better dropped books give off a slow, rarity-coloured particle ({@link BookDropFx}).</li>
 * </ul>
 * The values are mirrored into plain fields when the file loads or reloads, so the per-frame item property and the per-tick shimmer read a field, never the config.
 */
public final class BookLookConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue LEGACY_LOOK, DROP_SHIMMER;
    public static final boolean DEFAULT_LEGACY = false, DEFAULT_SHIMMER = true;

    /** Read each tick by {@link BookDropFx}. */
    public static volatile boolean dropShimmer = DEFAULT_SHIMMER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("skillbookLook");
        LEGACY_LOOK = b.comment("true: Skill Books keep the first (1.82.0) look: grey covers tinted by the line colour, the old flat sprite and the always-glint. false (default, 3.26.0): the painted 3D grimoires, one per franchise and rarity. Only changes what this client draws.")
                .define("legacyLook", DEFAULT_LEGACY);
        DROP_SHIMMER = b.comment("true (default): a rare, epic or mythic Skill Book lying on the ground gives off a slow particle in its rarity colour (at most a few particles a second per book, only within 24 blocks, at most 24 books). false: no particles. Only changes what this client draws.")
                .define("dropShimmer", DEFAULT_SHIMMER);
        b.pop();
        SPEC = b.build();
    }

    private BookLookConfig() {}

    private static boolean done;

    /** One call from {@link BookClient#init}. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-skillbook-look-client.toml");      // a literal, so ConfigFileNamesTest sees the name
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            LegacyLook.legacy = LEGACY_LOOK.get();
            dropShimmer = DROP_SHIMMER.get();
        } catch (IllegalStateException notLoaded) {
            LegacyLook.legacy = DEFAULT_LEGACY;
            dropShimmer = DEFAULT_SHIMMER;
        }
    }
}
