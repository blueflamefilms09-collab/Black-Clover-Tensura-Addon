package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.sinweapons.SinVfx;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: {@code sinweapons.classicVfx} in {@code config/nusmp-sinweapons-vfx-client.toml} (a CLIENT config: it only changes how things look). Off (the default): the five Berserk weapons (Black Axe of Sloth, Gunblade of Envy,
 * Spear of Vanity, Chakrams, Seifort Holy Sword) draw their own themed effect for every technique and hit ({@code layer.SinWeaponLayer}). On: the layer draws nothing, so only the small particle puff the server also sends
 * is seen. The value is mirrored into the pure {@link SinVfx} switch when the file loads or reloads; the dedicated server never registers this config.
 */
public final class SinWeaponVfxConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sinweapons");
        CLASSIC = b.comment("true: the Berserk of Gluttony weapons show only the small old particle puff. false: each weapon and technique draws its own effect (gravity rings, green eyes, mirror shards, spinning rings, crosses of light). Looks only.")
                .define("classicVfx", false);
        b.pop();
        SPEC = b.build();
    }

    private SinWeaponVfxConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-sinweapons-vfx-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    static void apply() {
        try { SinVfx.setClassic(CLASSIC.get()); } catch (IllegalStateException notLoaded) { SinVfx.setClassic(false); }
    }
}
