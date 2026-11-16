package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Vfx;
import com.newuniverse.nusmp.vfx.client.layer.SkillAnimeVfx;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * VFX wave 3: {@code skills.classicVfx} in {@code config/nusmp-skill-anime-vfx-client.toml} (a CLIENT config: it only changes how things look). Off (the default): the Unique / Ultimate skills (Gate of
 * Babylon, Firebolt, Argonaut, Wynn Fimbulvetr, Hestia, Freya, All Fiction, ...) draw their anime shapes (SkillAnimeLayer) on top of the old effects. On: the layer draws nothing, so only the
 * effects of 3.98.0 are seen. The value is mirrored into the pure {@link SkillAnimeVfx} switch when the file loads or reloads; the dedicated server never registers this config.
 */
public final class SkillAnimeVfxConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue CLASSIC;
    private static final ModConfigSpec.BooleanValue CLASSIC2;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("skills");
        CLASSIC = b.comment("true: the Unique / Ultimate skills show only their old effects (particle sprays and the older shapes). false: their anime effects are drawn too (the golden gates and raining weapons of the Gate of Babylon, Firebolt's instant bolt, Argonaut's bell of light, Wynn Fimbulvetr's frost circle, Hestia's sacred fire, Freya's silver allure, All Fiction's screws, ...). Looks only.")
                .define("classicVfx", false);
        CLASSIC2 = b.comment("VFX wave 4. true: the Beast Will moves, the hero / vessel weapon skills, the weakest grimoires (Cotton, Mirror, Plant, Sealing, Thread) and the wave 3 leftovers (Liaris Freese, Hati, Hawk Eye, Spirit Card, Lord of Summons, Shishou, the other Gate of Babylon modes, the Devil's Footprints kick, the Almighty, the Visionary) show only their old effects. false: their wave 4 effects are drawn too. Looks only.")
                .define("classicVfx2", false);
        b.pop();
        SPEC = b.build();
    }

    private SkillAnimeVfxConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-skill-anime-vfx-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    static void apply() {
        try { SkillAnimeVfx.setClassic(CLASSIC.get()); } catch (IllegalStateException notLoaded) { SkillAnimeVfx.setClassic(false); }
        try { SkillAnime2Vfx.setClassic(CLASSIC2.get()); } catch (IllegalStateException notLoaded) { SkillAnime2Vfx.setClassic(false); }
    }
}
