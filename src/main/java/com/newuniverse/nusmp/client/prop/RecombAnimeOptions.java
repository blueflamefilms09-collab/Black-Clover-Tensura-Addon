package com.newuniverse.nusmp.client.prop;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client option of the Recombination Magic anime look: config/nusmp-recomb-anime-client.toml, key {@code visual.recombAnime} (default true). On = the Raging Black Bull is drawn as the house-golem of the
 * anime (a humanoid of stacked houses with tower horns and brick fists) and the other constructs (rooms, the ring of boards, the drill) as houses and bricks, with dust and impact particles; off = every
 * construct is drawn exactly as before (the four-legged bull, the oak rooms, the boards, the mana drill: those models and painters stay in the mod). It only changes how things look on THIS machine; the server
 * decides everything else (hit box, ram, cannons). Registered once from the client section of the mod constructor:
 * {@code modContainer.registerConfig(ModConfig.Type.CLIENT, RecombAnimeOptions.SPEC, "nusmp-recomb-anime-client.toml")}.
 */
public final class RecombAnimeOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ANIME, PARTICLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        ANIME = b.comment("Draw Recombination Magic like the anime: the Raging Black Bull as a giant built of houses (stacked house torso, two tower horns, glowing eyes, brick fists), rooms as small houses, "
                + "the ring of boards as flying building pieces, the Mana Corkscrew as a brick drill. Off = the older four-legged bull, oak rooms, boards and drill. A resource reload (F3+T) is not needed.")
                .define("recombAnime", true);
        PARTICLES = b.comment("Dust bursts under the golem's steps, the orange impact flash with spark lines when a fist lands, chimney smoke (custom particles; needs visual.recombAnime and particles.custom).")
                .define("recombAnimeParticles", true);
        b.pop();
        SPEC = b.build();
    }

    private RecombAnimeOptions() {}

    /** True when the anime look is used ({@code visual.recombAnime}); true while the config is not loaded (the safe default is the new look). */
    public static boolean on() {
        try {
            return ANIME.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the anime particles are on (only meaningful with {@link #on()}). */
    public static boolean particles() {
        try {
            return ANIME.get() && PARTICLES.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
