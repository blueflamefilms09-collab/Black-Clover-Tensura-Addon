package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.labyrinth.mega.FloorListener;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionRegistry;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.skilltree.loot.LootRules;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the quests are fed by the game, and every feed is a listener on the game bus that only DELEGATES to {@link QuestService} (the pattern of the System's {@code SystemEvents}): a death in the
 * Mega-Dungeon (the kill hook), the two floor events of the dungeon's own {@link FloorListener} (a floor entered, a floor cleared), the player's comings and goings (login and respawn send the
 * overlay at once; logout forgets the throttle) and two light ticks (the server tick flushes the overlay syncs; the player tick, a few times a second, rolls the player's day over and
 * remembers which room they stand in for the Dungeon Map's fog of war). The kill hook reads FIVE facts off the dead creature ({@link KillFact}): for a Mob Foundry mob its own tier, road and faction (the
 * definition knows them), for anything else the tier of the floor it died on, the road of the sector and the faction of its entity type; a decennial Guardian is a boss. Only a PLAYER's kill
 * counts, only for an ordinary hostile monster or a Guardian ({@link LootVictims#kindOf}: never a pet, a shadow or a summon) and never for a summoner's minion, so there is nothing to farm.
 * Every handler is wrapped: an exception here must never break the game's own event.
 */
public final class QuestEvents {
    private QuestEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(QuestEvents::onDeath);
        bus.addListener(QuestEvents::onPlayerTick);
        bus.addListener(QuestEvents::onServerTick);
        bus.addListener(QuestEvents::onLogin);
        bus.addListener(QuestEvents::onRespawn);
        bus.addListener(QuestEvents::onChangedDimension);
        bus.addListener(QuestEvents::onLogout);
        bus.addListener(QuestEvents::onStopped);
    }

    /** Registers the dungeon's floor callbacks (mod init). */
    public static void registerFloorListener() {
        MegaDungeon.addFloorListener(new FloorListener() {
            @Override public void onFloorEntered(ServerPlayer player, int floor, int previous) { QuestService.onFloorEntered(player, floor); }

            @Override public void onFloorCleared(ServerPlayer player, int floor) { QuestService.onFloorCleared(player, floor); }
        });
    }

    // ================================================================ kills
    static void onDeath(LivingDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (dead.level().isClientSide || !HubConfig.enabled() || !(dead.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
            ServerPlayer killer = LootVictims.killer(e.getSource(), dead);
            if (killer == null) return;
            KillFact fact = factOf(level, dead);
            if (fact != null) QuestService.onKill(killer, fact);
        } catch (RuntimeException ex) {
            // a quest must never break a death
        }
    }

    /** The facts of a death, or null when the creature does not count (a pet, a summon, a servant, a non-monster). */
    static KillFact factOf(ServerLevel level, LivingEntity dead) {
        if (dead instanceof FoundryMobEntity f) {
            if (f.isMinion()) return null;
            int floor = f.floor() != 0 ? f.floor() : MegaDungeon.floorAt(level, dead.blockPosition());
            return new KillFact(f.tier(), f.road().id(), f.faction(), true, false, floor);
        }
        LootRules.Kind kind = LootVictims.kindOf(dead);
        if (kind == LootRules.Kind.OTHER) return null;
        boolean boss = kind == LootRules.Kind.GUARDIAN;
        int floor;
        if (boss) {
            LootVictims.Site site = LootVictims.guardianSite(level, dead);
            floor = site == null ? MegaDungeon.floorAt(level, dead.blockPosition()) : site.floor();
        } else {
            floor = MegaDungeon.floorAt(level, dead.blockPosition());
        }
        if (floor == 0) return null;
        return KillFact.onFloor(floor, FactionRegistry.factionOf(dead), false, boss || dead.getTags().contains(LabyrinthGuardianEntity.SECTOR_TAG));
    }

    // ================================================================ ticks
    static void onServerTick(ServerTickEvent.Post e) {
        try {
            HubSync.tick(e.getServer());
        } catch (RuntimeException ex) {
            // the Hub must never break the server tick
        }
    }

    static void onPlayerTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !HubConfig.enabled()) return;
            int t = p.tickCount;
            if (t % 200 == 17 && QuestService.on()) QuestService.fresh(p);                 // a day boundary is noticed within ten seconds
            if (t % 10 == 3 && p.level() instanceof ServerLevel level && MegaDungeon.in(level)) remember(p, level);
        } catch (RuntimeException ex) {
            // never break a player tick
        }
    }

    /** Fog of war: the room the player stands in is now known. */
    private static void remember(ServerPlayer p, ServerLevel level) {
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        if (s == null) return;
        Room r = s.roomAt(p.blockPosition());
        if (r != null) p.getData(HubAttachments.MAP).visit(s.id, r.id);
    }

    // ================================================================ players
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && QuestService.on()) {
                QuestService.fresh(p);
                HubSync.request(p);
            }
        } catch (RuntimeException ex) {
            // never break a login
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && QuestService.on()) HubSync.request(p);
        } catch (RuntimeException ex) {
            // never break a respawn
        }
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && QuestService.on()) HubSync.request(p);
        } catch (RuntimeException ex) {
            // never break a dimension change
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        ActionGate.forget(e.getEntity().getUUID());
        HubSync.forget(e.getEntity().getUUID());
    }

    static void onStopped(ServerStoppedEvent e) {
        ActionGate.clear();
        HubSync.clear();
        HubCatalogs.clear();
    }
}
