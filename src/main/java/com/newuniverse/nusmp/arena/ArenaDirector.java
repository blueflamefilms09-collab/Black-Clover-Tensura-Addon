package com.newuniverse.nusmp.arena;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisCloneEntity;
import com.newuniverse.nusmp.entity.zogratis.ZogratisEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Lucius must not be a thing that is simply standing in a room. The arena director is the server-side referee of the Final Hour: it
 * watches the arena dimension ten ticks at a time, starts a ten-second countdown ("The Final Hour begins") when the first player arrives (or five seconds
 * after one stands on the dais ring), spawns Lucius once at the dais with the {@link SpawnGate#TAG} on him, and then follows the fight with the pure
 * {@link ArenaState}: if every player leaves or dies the boss is removed and the arena rests for 60 seconds (nobody can leave, heal him and come back to
 * a half-health boss), when he is defeated for good ({@code LivingDeathEvent}; his stage changes never fire it) the arena rests for the configured
 * minutes, the players get a clickable prompt to go home, and the Return Sigils wake. Nothing about the boss himself is changed here: his drops, his stats
 * and his voice are his own (his lines already reach everyone in the arena). A player who dies in the arena gets a five-minute marker (the time vanilla
 * keeps the dropped items) and a message that says where the belongings lie; no game rule is touched. A player who falls below the courtyard (over the
 * wall) is set back on the arrival ring, because the void below is not part of the fight. Everything runs on the server thread and does nothing at
 * all while the arena has no players and no running state, so a server without the fight pays one level lookup every ten ticks.
 */
public final class ArenaDirector {
    private ArenaDirector() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String SOUL = "nusmp_arena_soul", SOUL_AT = "nusmp_arena_soul_at";
    public static final int SOUL_TICKS = 6000, STEP = 10;

    public static void register(IEventBus bus) {
        bus.addListener(ArenaDirector::onServerTick);
        bus.addListener(ArenaDirector::onDeath);
        bus.addListener(ArenaDirector::onRespawn);
    }

    // ---------------------------------------------------------------- questions
    /** Is the boss fighting in the arena right now? (The hourglass and the sigil ask.) */
    public static boolean fightRunning(MinecraftServer server) {
        return server != null && ArenaSave.get(server).state().fighting();
    }

    private static List<ServerPlayer> inside(ServerLevel sl) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) if (p.isAlive() && !p.isSpectator()) out.add(p);
        return out;
    }

    // ---------------------------------------------------------------- the tick
    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % STEP != 0) return;
        ServerLevel sl = server.getLevel(ZogratisArena.DIMENSION);
        if (sl == null) return;
        ArenaSave save = ArenaSave.get(server);
        ArenaState st = save.state();
        List<ServerPlayer> players = inside(sl);
        if (players.isEmpty() && st.phase() == ArenaState.Phase.READY) {
            st.tickReady(STEP, 0, 0);                                                        // keeps the countdown from surviving an empty room
            return;
        }
        long now = sl.getGameTime();
        for (ServerPlayer p : players) rescue(p);
        try {
            switch (st.phase()) {
                case READY -> tickReady(server, sl, save, players);
                case FIGHTING -> tickFighting(sl, save, players, now);
                case COOLDOWN -> {
                    if (st.tickCooldown(now)) {
                        save.setDirty();
                        for (ServerPlayer p : players) p.sendSystemMessage(Component.literal("The arena is quiet again. The Final Hour can begin.").withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] The arena director failed; the arena is reset so it cannot stay stuck", ex);
            st.reset();
            save.setDirty();
        }
    }

    private static void tickReady(MinecraftServer server, ServerLevel sl, ArenaSave save, List<ServerPlayer> players) {
        ArenaState st = save.state();
        int onDais = 0;
        Vec3 centre = ZogratisArena.daisStand();
        for (ServerPlayer p : players) {
            double dx = p.getX() - centre.x, dz = p.getZ() - centre.z;
            if (dx * dx + dz * dz <= ZogratisArena.DAIS_RING * ZogratisArena.DAIS_RING && p.getY() < ArenaLayout.FLOOR_Y + 6) onDais++;
        }
        ArenaState.Start s = st.tickReady(STEP, players.size(), onDais);
        if (s == ArenaState.Start.COUNTDOWN_BEGAN) {
            title(players, "The Final Hour approaches", "Stand on the dais, or wait ten seconds", ChatFormatting.GOLD);
            save.setDirty();
        } else if (s == ArenaState.Start.NOTHING) {
            int left = st.countdownLeft();
            if (left > 0 && left <= 100 && left % 20 == 0) for (ServerPlayer p : players) p.displayClientMessage(Component.literal("Lucius answers in " + left / 20 + "...").withStyle(ChatFormatting.GOLD), true);
        } else if (s == ArenaState.Start.SPAWN) {
            title(players, "The Final Hour begins", "Lucius Zogratis", ChatFormatting.DARK_RED);
            spawnBoss(sl, save);
            save.setDirty();
        }
    }

    private static void tickFighting(ServerLevel sl, ArenaSave save, List<ServerPlayer> players, long now) {
        ArenaState st = save.state();
        LuciusBoss boss = save.bossId() != null && sl.getEntity(save.bossId()) instanceof LuciusBoss b && b.isAlive() ? b : null;
        ArenaState.Watch w = st.tickFighting(STEP, players.size(), boss != null);
        if (w == ArenaState.Watch.ABANDONED) {
            discardBoss(sl, save);
            st.abandon(now);
            save.setDirty();
            for (ServerPlayer p : players) p.sendSystemMessage(Component.literal("Lucius withdraws into the Final Hour. The arena is quiet for 60 seconds.").withStyle(ChatFormatting.GRAY));
            LOG.info("[nusmp] The Final Hour was abandoned; the arena rests for 60 seconds");
        }
    }

    // ---------------------------------------------------------------- the boss
    private static void spawnBoss(ServerLevel sl, ArenaSave save) {
        clearStrays(sl);
        LuciusBoss boss = ZogratisBoss.spawnAt(sl, ZogratisEntities.LUCIUS.get(), ZogratisArena.daisStand(), 0f);
        if (boss == null) {
            LOG.warn("[nusmp] The arena could not create Lucius; the arena rests for 60 seconds");
            save.state().abandon(sl.getGameTime());
            return;
        }
        boss.getPersistentData().putBoolean(SpawnGate.TAG, true);
        boss.setArena(ZogratisArena.bossAnchor());
        save.bossId(boss.getUUID());
    }

    /** Removes the boss of the fight and his clones (an abandoned fight, an operator's reset). */
    static void discardBoss(ServerLevel sl, ArenaSave save) {
        clearStrays(sl);
        save.bossId(null);
    }

    /** Every Lucius and clone in the arena level goes (loaded ones; an unloaded one is removed by the same call when it loads and the next fight starts). */
    private static void clearStrays(ServerLevel sl) {
        List<Entity> gone = new ArrayList<>();
        for (Entity e : sl.getAllEntities()) if (e instanceof LuciusBoss || e instanceof ZogratisCloneEntity) gone.add(e);
        for (Entity e : gone) e.discard();
    }

    /** An operator's reset: nothing running, the sigils open. */
    public static void reset(MinecraftServer server) {
        ArenaSave save = ArenaSave.get(server);
        ServerLevel sl = server.getLevel(ZogratisArena.DIMENSION);
        if (sl != null) discardBoss(sl, save);
        save.state().reset();
        save.setDirty();
    }

    // ---------------------------------------------------------------- defeat, death, respawn
    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide || !(e.getEntity().level() instanceof ServerLevel sl) || !ZogratisArena.in(sl)) return;
        if (e.getEntity() instanceof LuciusBoss boss) {
            MinecraftServer server = sl.getServer();
            ArenaSave save = ArenaSave.get(server);
            if (!save.state().fighting() || (save.bossId() != null && !save.bossId().equals(boss.getUUID()))) return;       // a boss the director did not start (an operator's summon) changes nothing
            save.state().defeat(sl.getGameTime(), ArenaConfig.cooldownTicks());
            save.bossId(null);
            for (ServerPlayer p : inside(sl)) {
                p.sendSystemMessage(Component.literal("Lucius Zogratis has fallen. The Final Hour is over, and the Return Sigils have woken.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                com.newuniverse.nusmp.quests.NusmpEvents.fireTeam(p, "arena.victory", "lucius");
                com.newuniverse.nusmp.quests.QuestHooks.arenaVictory(p);
                p.sendSystemMessage(Component.literal("[ Return home ]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/zogratis_arena leave"))));
            }
            LOG.info("[nusmp] Lucius was defeated in the arena; it rests for {} minutes", ArenaConfig.cooldownMinutes());
        } else if (e.getEntity() instanceof ServerPlayer p) {
            long now = sl.getGameTime();
            p.getPersistentData().putLong(SOUL, now + SOUL_TICKS);
            p.getPersistentData().putString(SOUL_AT, (int) p.getX() + ", " + (int) p.getY() + ", " + (int) p.getZ());
            p.sendSystemMessage(Component.literal("You fell in the Final Hour. Your belongings lie where you fell (" + (int) p.getX() + ", " + (int) p.getY() + ", " + (int) p.getZ()
                    + ") for five minutes. Take an Hourglass of the Final Hour and go back for them, or let them go.").withStyle(ChatFormatting.RED));
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        long until = p.getPersistentData().getLong(SOUL);
        if (until <= 0) return;
        long left = until - p.level().getGameTime();
        if (left > 0) p.sendSystemMessage(Component.literal("Your belongings still lie in the arena at " + p.getPersistentData().getString(SOUL_AT) + " for another " + (left / 1200 + 1)
                + " minutes. The Hourglass of the Final Hour takes you there (it cannot be entered while Lucius fights).").withStyle(ChatFormatting.GOLD));
        p.getPersistentData().remove(SOUL);
        p.getPersistentData().remove(SOUL_AT);
    }

    // ---------------------------------------------------------------- small things
    /** A traveller just arrived: tell them the state of the arena. */
    static void welcome(MinecraftServer server, ServerLevel arena, ServerPlayer p) {
        ArenaState st = ArenaSave.get(server).state();
        switch (st.phase()) {
            case READY -> p.sendSystemMessage(Component.literal("Lucius will answer ten seconds after the first of you arrives, or five seconds after one stands on the dais.").withStyle(ChatFormatting.GRAY));
            case FIGHTING -> p.sendSystemMessage(Component.literal("The Final Hour is underway. The Return Sigils are dormant; the hourglass takes ten seconds to leave.").withStyle(ChatFormatting.GRAY));
            case COOLDOWN -> p.sendSystemMessage(Component.literal(st.defeated()
                    ? "Lucius has fallen and does not answer for " + (st.cooldownSecondsLeft(arena.getGameTime()) / 60 + 1) + " more minutes."
                    : "The arena is resting for " + st.cooldownSecondsLeft(arena.getGameTime()) + " more seconds.").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A player below the courtyard (over the wall, off the edge) is set back on the arrival ring. */
    private static void rescue(ServerPlayer p) {
        if (p.getY() >= ArenaLayout.FLOOR_Y - 12) return;
        int[] s = ArenaLayout.arrivalSpots(1)[0];
        p.teleportTo(s[0] + 0.5, ArenaLayout.FLOOR_Y + ArenaLayout.ARRIVAL_DY, s[1] + 0.5);
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        p.displayClientMessage(Component.literal("The void gives you back.").withStyle(ChatFormatting.GRAY), true);
    }

    private static void title(List<ServerPlayer> players, String title, String subtitle, ChatFormatting color) {
        for (ServerPlayer p : players) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.GRAY)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
        }
    }
}
