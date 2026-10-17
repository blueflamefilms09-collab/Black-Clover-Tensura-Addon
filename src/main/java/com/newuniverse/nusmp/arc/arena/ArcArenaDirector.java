package com.newuniverse.nusmp.arc.arena;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.world.ArcWorldConfig;
import com.newuniverse.nusmp.arc.world.ExistingBossSpawner;
import com.newuniverse.nusmp.arc.world.ExistingBosses;
import com.newuniverse.nusmp.arena.ArenaSave;
import com.newuniverse.nusmp.arena.ArenaState;
import com.newuniverse.nusmp.arena.SpawnGate;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: a story boss must not simply be standing in a room. The director is the server-side referee of every story arena, the Final Hour's referee ({@code
 * ArenaDirector}) multiplied by the grid of sites: ten ticks at a time it looks at each site that has a player or a fight to keep, feeds the pure
 * {@link ArenaState} (the same state machine, reused: a ten-second countdown from the first arrival, or five seconds after one stands near the boss's place;
 * abandoned when everyone is gone or the boss vanished; resting after a defeat for {@code arc.arenaCooldownMinutes}) and carries out its answers: makes the
 * boss (through the door that fits it, with the spawn gate's tag and the site's number on it), removes it when the fight is abandoned, and when it falls closes
 * the fight, tells the people there and offers the way home. The STORY credit, the Charter Grimoire and the saga reward are not given here: they are given
 * by the places that hear every death of these bosses wherever it happens ({@code ArcStory}, {@code CharterEvents}, {@code ArcWorldCredit}), so a boss beaten
 * here, in the dungeon or by a wanderer pays the same. A player who falls below the floor (over a wall, off an edge) is set back on the arrival ring, because the
 * void below is not part of the fight. Everything runs on the server thread and does nothing at all while no site has a player or a running state.
 */
public final class ArcArenaDirector {
    private ArcArenaDirector() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int STEP = 10;
    /** Blocks (horizontal) from the boss's place within which a player counts as "standing at the boss" for the shortened countdown. */
    public static final double NEAR_BOSS = 12.0;

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(ArcArenaDirector::onServerTick);
        bus.addListener(EventPriority.LOWEST, ArcArenaDirector::onDeath);
        bus.addListener((ServerStoppedEvent e) -> ArcArena.setEditing(false));
    }

    // ---------------------------------------------------------------- questions
    /** What the Chronicle needs to know about the arena of a character: a fight is running, or the arena is resting (and for how many seconds). */
    public record Status(boolean fighting, boolean resting, long restSeconds) {
        public static final Status IDLE = new Status(false, false, 0);
    }

    public static Status status(MinecraftServer server, ArcCharacter c) {
        if (server == null || c == null) return Status.IDLE;
        if (ArenaShapes.usesFinalHour(c)) {
            ArenaState st = ArenaSave.get(server).state();
            return new Status(st.fighting(), st.phase() == ArenaState.Phase.COOLDOWN, st.cooldownSecondsLeft(server.overworld().getGameTime()));
        }
        ArcArenaSave save = ArcArenaSave.get(server);
        int slot = save.slotOf(c.id());
        if (slot < 0 || !save.hasState(slot)) return Status.IDLE;
        ArenaState st = save.state(slot);
        return new Status(st.fighting(), st.phase() == ArenaState.Phase.COOLDOWN, st.cooldownSecondsLeft(server.overworld().getGameTime()));
    }

    /** Is a fight running in the arena the player stands in? (The sigil asks, to word its refusal and its channel.) */
    public static boolean fightRunningAt(MinecraftServer server, ServerPlayer p) {
        if (server == null) return false;
        if (com.newuniverse.nusmp.arena.ZogratisArena.in(p)) return com.newuniverse.nusmp.arena.ArenaDirector.fightRunning(server);
        int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
        return slot >= 0 && ArcArenaSave.get(server).hasState(slot) && ArcArenaSave.get(server).state(slot).fighting();
    }

    // ---------------------------------------------------------------- the tick
    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % STEP != 0) return;
        try {
            step(server);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] The arena director's pass failed and was skipped: {}", ex.toString());          // never out of an event: the next pass tries again
        }
    }

    private static void step(MinecraftServer server) {
        ServerLevel sl = server.getLevel(ArcArena.DIMENSION);
        if (sl == null) return;
        ArcArenaSave save = ArcArenaSave.get(server);
        Map<Integer, List<ServerPlayer>> bySlot = new LinkedHashMap<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
            if (slot < 0) continue;
            bySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(p);
        }
        Set<Integer> look = new LinkedHashSet<>(bySlot.keySet());
        for (Map.Entry<Integer, ArenaState> st : save.allStates().entrySet()) if (st.getValue().phase() != ArenaState.Phase.READY) look.add(st.getKey());
        if (look.isEmpty()) return;
        for (int slot : new ArrayList<>(look)) {
            List<ServerPlayer> players = bySlot.getOrDefault(slot, List.of());
            try {
                tickSlot(sl, save, slot, players);
            } catch (RuntimeException ex) {
                LOG.error("[nusmp] The arena director failed at site {}; the site is reset so it cannot stay stuck", slot, ex);
                save.state(slot).reset();
                save.setDirty();
            }
        }
    }

    private static void tickSlot(ServerLevel sl, ArcArenaSave save, int slot, List<ServerPlayer> players) {
        String id = save.characterAt(slot);
        ArcCharacter c = id == null ? null : ArcRoster.byId(id);
        ArenaState st = save.state(slot);
        if (c == null) {                                                                   // the character left the roster: nothing can run here
            if (st.phase() != ArenaState.Phase.READY) {
                st.reset();
                save.setDirty();
            }
            return;
        }
        ArenaShape shape = ArenaShapes.forCharacter(c);
        long now = sl.getGameTime();
        for (ServerPlayer p : players) rescue(p, shape, slot);
        switch (st.phase()) {
            case READY -> tickReady(sl, save, slot, c, shape, players);
            case FIGHTING -> tickFighting(sl, save, slot, players, now);
            case COOLDOWN -> {
                if (st.tickCooldown(now)) {
                    save.setDirty();
                    for (ServerPlayer p : players) p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.quiet", "The arena is quiet again.").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    private static void tickReady(ServerLevel sl, ArcArenaSave save, int slot, ArcCharacter c, ArenaShape shape, List<ServerPlayer> players) {
        ArenaState st = save.state(slot);
        int near = 0;
        Vec3 spot = ArcArena.bossStand(shape, slot);
        for (ServerPlayer p : players) {
            double dx = p.getX() - spot.x, dz = p.getZ() - spot.z;
            if (dx * dx + dz * dz <= NEAR_BOSS * NEAR_BOSS) near++;
        }
        ArenaState.Start s = st.tickReady(STEP, players.size(), near);
        if (s == ArenaState.Start.COUNTDOWN_BEGAN) {
            title(players, shape.title(), "Ten seconds, or five once someone stands near the boss's place", ChatFormatting.GOLD);
            save.setDirty();
        } else if (s == ArenaState.Start.NOTHING) {
            int left = st.countdownLeft();
            if (left > 0 && left <= 100 && left % 20 == 0) {
                for (ServerPlayer p : players) p.displayClientMessage(Component.literal(c.name() + " answers in " + left / 20 + "...").withStyle(ChatFormatting.GOLD), true);
            }
        } else if (s == ArenaState.Start.SPAWN) {
            title(players, c.name(), c.fullName(), ChatFormatting.DARK_RED);
            spawnBoss(sl, save, slot, c, shape, players);
            save.setDirty();
        }
    }

    private static void tickFighting(ServerLevel sl, ArcArenaSave save, int slot, List<ServerPlayer> players, long now) {
        ArenaState st = save.state(slot);
        Entity e = save.bossId(slot) == null ? null : sl.getEntity(save.bossId(slot));
        boolean alive = e instanceof LivingEntity le && le.isAlive();
        ArenaState.Watch w = st.tickFighting(STEP, players.size(), alive);
        if (w == ArenaState.Watch.ABANDONED) {
            clearStrays(sl, slot);
            save.bossId(slot, null);
            st.abandon(now);
            save.setDirty();
            for (ServerPlayer p : players) p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.withdraws", "The boss withdraws. The arena is quiet for 60 seconds.").withStyle(ChatFormatting.GRAY));
            LOG.info("[nusmp] The fight at site {} was abandoned; the arena rests for 60 seconds", slot);
        }
    }

    // ---------------------------------------------------------------- the boss
    private static void spawnBoss(ServerLevel sl, ArcArenaSave save, int slot, ArcCharacter c, ArenaShape shape, List<ServerPlayer> players) {
        clearStrays(sl, slot);
        Vec3 at = ArcArena.bossStand(shape, slot);
        ZogratisBoss boss;
        if (ExistingBosses.owns(c.id())) {
            boss = ExistingBossSpawner.make(sl, c, at, 0f, true).boss();
        } else {
            ArcSpawner.Result r = ArcSpawner.summon(sl, c, at, 0f, List.of(), true);       // forced: the Chronicle checked the gate when it chose the boss
            boss = r.outcome() == ArcSpawner.Outcome.SPAWNED ? r.boss() : null;
        }
        if (boss == null) {
            LOG.warn("[nusmp] The arena could not create {}; the arena rests for 60 seconds", c.id());
            save.state(slot).abandon(sl.getGameTime());
            for (ServerPlayer p : players) p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.no_boss", "%s does not answer. Try again in a minute.", c.name()).withStyle(ChatFormatting.GRAY));
            return;
        }
        boss.getPersistentData().putBoolean(SpawnGate.TAG, true);
        boss.getPersistentData().putInt(ArcArena.SLOT_KEY, slot);
        save.bossId(slot, boss.getUUID());
    }

    /** Every boss of the framework standing in a site's box goes (an abandoned fight, a new one starting, an operator's reset); loaded ones only, an unloaded one is removed by the same call when the next fight starts. */
    private static void clearStrays(ServerLevel sl, int slot) {
        double cx = ArenaGrid.centreX(slot) + 0.5, cz = ArenaGrid.centreZ(slot) + 0.5, half = ArenaGrid.SPACING / 2.0;
        AABB box = new AABB(cx - half, sl.getMinBuildHeight(), cz - half, cx + half, sl.getMaxBuildHeight(), cz + half);
        for (ZogratisBoss b : sl.getEntitiesOfClass(ZogratisBoss.class, box)) b.discard();
    }

    /** An operator's reset of one site: nothing running, the arena ready. */
    public static void reset(MinecraftServer server, int slot) {
        ArcArenaSave save = ArcArenaSave.get(server);
        ServerLevel sl = server.getLevel(ArcArena.DIMENSION);
        if (sl != null) clearStrays(sl, slot);
        save.bossId(slot, null);
        save.state(slot).reset();
        save.setDirty();
    }

    // ---------------------------------------------------------------- defeat, death
    private static void onDeath(LivingDeathEvent e) {
        try {
            Entity dead = e.getEntity();
            if (dead.level().isClientSide || !(dead.level() instanceof ServerLevel sl) || !ArcArena.in(sl)) return;
            MinecraftServer server = sl.getServer();
            if (server == null) return;
            ArcArenaSave save = ArcArenaSave.get(server);
            if (dead instanceof ZogratisBoss boss && boss.getPersistentData().contains(ArcArena.SLOT_KEY)) {
                int slot = boss.getPersistentData().getInt(ArcArena.SLOT_KEY);
                ArenaState st = save.state(slot);
                if (!st.fighting() || (save.bossId(slot) != null && !save.bossId(slot).equals(boss.getUUID()))) return;       // a boss the director did not start changes nothing
                st.defeat(sl.getGameTime(), ArcWorldConfig.arenaCooldownTicks());
                save.bossId(slot, null);
                save.setDirty();
                String id = save.characterAt(slot);
                ArcCharacter c = id == null ? null : ArcRoster.byId(id);
                for (ServerPlayer p : sl.players()) {
                    if (ArenaGrid.slotAt(p.getX(), p.getZ()) != slot || !p.isAlive() || p.isSpectator()) continue;
                    p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.won", "%s has fallen. The arena is yours.", c == null ? "The boss" : c.fullName())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.go_home", "[ Return home ]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse arc arena leave"))));
                }
                LOG.info("[nusmp] {} was defeated at site {}; the arena rests for {} minutes", id, slot, ArcWorldConfig.arenaCooldownMinutes());
            } else if (dead instanceof ServerPlayer p) {
                int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
                if (slot < 0) return;
                p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.fell",
                        "You fell in the arena. Your belongings lie where you fell (%s) for five minutes; the Chronicle Sigil takes you back once the fight is over.",
                        (int) p.getX() + ", " + (int) p.getY() + ", " + (int) p.getZ()).withStyle(ChatFormatting.RED));
            }
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the arena director could not close a fight: {}", ex.toString());
        }
    }

    // ---------------------------------------------------------------- small things
    /** A traveller just arrived: tell them the state of the arena. */
    static void welcome(MinecraftServer server, ServerLevel arena, ServerPlayer p, int slot, ArcCharacter c) {
        ArenaState st = ArcArenaSave.get(server).state(slot);
        switch (st.phase()) {
            case READY -> p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.ready",
                    "%s will answer ten seconds after the first of you arrives, or five seconds after one of you stands near their place.", c.name()).withStyle(ChatFormatting.GRAY));
            case FIGHTING -> p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.fighting",
                    "The fight is underway. Hold the Chronicle Sigil for ten seconds, or use a way home, to leave.").withStyle(ChatFormatting.GRAY));
            case COOLDOWN -> p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.arena.resting",
                    "The arena is resting for %s more seconds.", Long.toString(st.cooldownSecondsLeft(arena.getGameTime()))).withStyle(ChatFormatting.GRAY));
        }
    }

    /** A player below the floor (over the wall, off the edge) is set back on the first arrival column. */
    private static void rescue(ServerPlayer p, ArenaShape shape, int slot) {
        if (p.getY() >= ArenaShape.FLOOR_Y - 12) return;
        int[] s = shape.arrivalSpots(1)[0];
        Vec3 at = ArcArena.arrivalStand(shape, slot, s);
        p.teleportTo(at.x, at.y, at.z);
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.arena.void", "The void gives you back.").withStyle(ChatFormatting.GRAY), true);
    }

    private static void title(List<ServerPlayer> players, String title, String subtitle, ChatFormatting color) {
        for (ServerPlayer p : players) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.GRAY)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
        }
    }
}
