package com.newuniverse.nusmp.saiyan;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.config.ReincarnationConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: keeps the Saiyan flag ({@code SaiyanState.saiyan}, the source of truth for every Saiyan rule) and the real Tensura race {@code anime_multiverse:saiyan} ({@link SaiyanRaces}) in step, in both directions, behind the
 * server setting {@code phase.race.tensuraRace} (off = none of this runs: the old flag-only behaviour). The decisions are pure ({@link SaiyanRaceRules#afterFlag}, {@link SaiyanRaceRules#atLogin},
 * {@link SaiyanRaceRules#onSetRace}); this class only reads and sets the race through ManasCore ({@code RaceAPI.getRaceFrom(p)}) and listens to ManasCore's {@code RaceEvents.SET_RACE}.
 * <ul>
 * <li>flag on ({@code /saiyan race on}, {@link SaiyanManager#setSaiyan}): the race the player had is remembered (player data, key {@link #PREV}) and the Saiyan race is set. Flag off: the remembered race (else Human) is set back.</li>
 * <li>Tensura sets the Saiyan race (race menu, command, scroll): the flag goes on. Tensura takes the player out of the Saiyan race: the flag goes off.</li>
 * <li>login: a flagged Saiyan without the race gets it; a holder of the race without the flag gets the flag (a missed event, or a world from before).</li>
 * <li>server start: the Saiyan is added once to Tensura's starting-race list, so it shows in the race menu ({@link #listInMenu}).</li>
 * </ul>
 * Every Tensura call is wrapped: a failure is logged and the flag path goes on. Server thread only; the one static is a re-entrancy guard (our own race change fires {@code SET_RACE} again), not player state.
 * Cost: one race lookup per flag change, login or race change; one list check per server start. No tick work.
 */
final class SaiyanRaceLink {
    private SaiyanRaceLink() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Player data: the race the player had before the Saiyan race (a race id, may be empty). */
    static final String PREV = "nusmp_saiyan_prev_race";
    private static final ResourceLocation HUMAN = ResourceLocation.fromNamespaceAndPath("tensura", "human");

    /** True while this class itself changes the flag or the race, so the {@code SET_RACE} event it causes is not answered again. */
    private static boolean linking;

    static boolean enabled() { return SaiyanPhaseConfig.tensuraRace(); }

    /** The id of the player's Tensura race, or null (none, or Tensura / ManasCore could not say). */
    static ResourceLocation current(Player p) {
        try {
            return RaceAPI.getRaceFrom(p).getRace().map(ManasRaceInstance::getRaceId).orElse(null);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** True if the player's Tensura race is the Saiyan race. */
    static boolean holdsRace(Player p) { return SaiyanRaces.id().equals(current(p)); }

    /** {@link SaiyanManager#setSaiyan} changed the flag: the race follows. */
    static void afterFlag(ServerPlayer p, boolean on) {
        if (linking || !enabled()) return;
        apply(p, SaiyanRaceRules.afterFlag(true, on, holdsRace(p)));
    }

    /** Login: bring flag and race in step. */
    static void onLogin(ServerPlayer p) {
        if (!enabled()) return;
        apply(p, SaiyanRaceRules.atLogin(true, SaiyanStore.isSaiyan(p), holdsRace(p)));
    }

    /** ManasCore's {@code RaceEvents.SET_RACE}: fired BEFORE the race changes ({@code old} may be null). Never cancels. */
    static EventResult onSetRace(ManasRaceInstance old, LivingEntity entity, ManasRaceInstance now, boolean evolution, Changeable<Boolean> teleport, Changeable<MutableComponent> message) {
        if (linking || !(entity instanceof ServerPlayer p) || !enabled()) return EventResult.pass();
        try {
            ResourceLocation id = SaiyanRaces.id();
            apply(p, SaiyanRaceRules.onSetRace(true, SaiyanStore.isSaiyan(p), old != null && id.equals(old.getRaceId()), now != null && id.equals(now.getRaceId())));
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Saiyan race link: the race change of {} was not mirrored to the flag ({})", p.getGameProfile().getName(), e.toString());
        }
        return EventResult.pass();
    }

    private static void apply(ServerPlayer p, SaiyanRaceRules.Link what) {
        switch (what) {
            case GIVE_RACE -> giveRace(p);
            case TAKE_RACE -> takeRace(p);
            case SET_FLAG -> setFlag(p, true);
            case CLEAR_FLAG -> setFlag(p, false);
            default -> { }
        }
    }

    private static void setFlag(ServerPlayer p, boolean on) {
        boolean was = linking;
        linking = true;
        try {
            SaiyanManager.setSaiyan(p, on);
        } finally {
            linking = was;
        }
    }

    private static void giveRace(ServerPlayer p) {
        boolean was = linking;
        linking = true;
        try {
            ResourceLocation had = current(p);
            CompoundTag data = p.getPersistentData();
            data.putString(PREV, had == null ? "" : had.toString());
            RaceAPI.getRaceFrom(p).setRace(SaiyanRaces.SAIYAN.get(), false);         // false = do not teleport to a new spawn
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Saiyan race link: could not set the Saiyan race on {} ({})", p.getGameProfile().getName(), e.toString());
        } finally {
            linking = was;
        }
    }

    private static void takeRace(ServerPlayer p) {
        boolean was = linking;
        linking = true;
        try {
            CompoundTag data = p.getPersistentData();
            String prev = data.getString(PREV);
            data.remove(PREV);
            ResourceLocation back = prev.isEmpty() ? null : ResourceLocation.tryParse(prev);
            if (back == null || SaiyanRaces.id().equals(back)) back = HUMAN;
            if (!RaceAPI.getRaceFrom(p).setRace(back, false)) RaceAPI.getRaceFrom(p).setRace(HUMAN, false);     // the old race is gone (a removed add-on): Human
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Saiyan race link: could not take the Saiyan race off {} ({})", p.getGameProfile().getName(), e.toString());
        } finally {
            linking = was;
        }
    }

    /**
     * Server start: puts the Saiyan into Tensura's STARTING race list (the menu a new player picks from). Tensura reads that list from its reincarnation config FILE (it is re-read from the file whenever the config is
     * synced to a client, so a change in memory alone is lost), so the list is changed and the file saved, once; nothing happens if the id is already there. Removing it by hand later is respected with
     * {@code phase.race.listInRaceMenu = false}. A failure is only logged.
     */
    static void listInMenu() {
        if (!enabled() || !SaiyanPhaseConfig.listInRaceMenu()) return;
        try {
            ReincarnationConfig cfg = ConfigRegistry.getConfig(ReincarnationConfig.class);
            if (cfg == null || cfg.Races == null) return;
            List<String> now = SaiyanRaceRules.withMenuEntry(cfg.Races.startingRaces, SaiyanRaceRules.RACE_ID);
            if (now == cfg.Races.startingRaces) return;
            cfg.Races.startingRaces = now;
            cfg.save();
            LOG.info("Saiyan race link: {} added to Tensura's starting-race list.", SaiyanRaceRules.RACE_ID);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Saiyan race link: Tensura's race menu list was not changed ({})", e.toString());
        }
    }
}
