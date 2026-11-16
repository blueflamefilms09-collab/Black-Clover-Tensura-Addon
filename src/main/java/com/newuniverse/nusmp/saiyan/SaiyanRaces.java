package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Saiyan as a REAL Tensura race (id {@code anime_multiverse:saiyan}), registered in ManasCore's race registry the same way this mod registers its skills ({@code NUSkills}: a NeoForge DeferredRegister on the
 * registry key of the ManasCore API). Kept apart from {@link SaiyanTensuraRace} on purpose: this holder names no Tensura class, so the mod constructor can register it without loading Tensura's race classes early;
 * the race object is only made when the registry fills. The race is ALWAYS registered (a registry can not depend on a server setting, and a saved race id must always be readable); whether a player is put into
 * it is the server setting {@code phase.race.tensuraRace} ({@link SaiyanRaceLink}). Register {@link #RACES} on the mod bus (done in {@link SaiyanBoot}).
 */
public final class SaiyanRaces {
    private SaiyanRaces() {}

    public static final DeferredRegister<ManasRace> RACES = DeferredRegister.create(RaceAPI.getRaceRegistryKey(), NUSMP.MODID);

    public static final DeferredHolder<ManasRace, ManasRace> SAIYAN = RACES.register("saiyan", () -> new SaiyanTensuraRace());

    /** The id of the Saiyan race. */
    public static ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "saiyan"); }
}
