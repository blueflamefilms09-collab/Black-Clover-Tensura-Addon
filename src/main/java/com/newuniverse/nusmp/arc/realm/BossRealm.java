package com.newuniverse.nusmp.arc.realm;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.arena.ArcArena;
import com.newuniverse.nusmp.arc.arena.ArcArenaDirector;
import com.newuniverse.nusmp.arc.arena.ArcArenaSave;
import com.newuniverse.nusmp.arc.arena.ArcArenaTravel;
import com.newuniverse.nusmp.arc.arena.ArenaGrid;
import com.newuniverse.nusmp.arc.arena.ArenaShape;
import com.newuniverse.nusmp.arc.arena.ArenaShapes;
import com.newuniverse.nusmp.arc.goku.GokuBosses;
import com.newuniverse.nusmp.arc.magi.SinbadBosses;
import com.newuniverse.nusmp.arc.world.ExistingBosses;
import com.newuniverse.nusmp.arena.ArenaState;
import com.newuniverse.nusmp.arena.ZogratisArena;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: Goku and Sinbad are not dungeon bosses (they stand on no floor), so they must not be fought at the spot where somebody happened to use an egg, in a forest, a cave or on a hillside the
 * dungeon halls were never built for. The mod already has the place for exactly this: the story-arena dimension ({@code anime_multiverse:arc_arena}) with its grid of sites, its builder, its
 * referee ({@link ArcArenaDirector}: countdown, fight, abandon, rest) and its way home ({@link ArcArenaTravel}, the Return Book). The boss realm is the thin layer that sends the party there from
 * the doors that used to spawn the boss in place (the spawn egg, {@code /multiverse arc summon}, {@code /multiverse goku_boss spawn}, {@code /multiverse sinbad_boss spawn}), gives these bosses
 * arenas of their own ({@code RealmShapes}), and closes the ways a party could be lost there:
 * <ul>
 *   <li><b>grace</b>: the referee gives up a fight 2 seconds after the arena is empty; for a realm site it waits {@code emptyGraceSeconds} (a dead or disconnected party may come back);</li>
 *   <li><b>timeout</b>: a fight longer than {@code fightTimeoutMinutes} ends, the boss withdraws;</li>
 *   <li><b>return</b>: {@code returnSeconds} after a site rests (won, withdrawn, timed out) the people still in it are carried home; at login inside a realm site the player is carried home at once;
 *       a player who dies there loses the saved way back (they respawn in the world);</li>
 *   <li><b>void</b>: a boss that is somehow below the floor is put back on its spot (the arena is a floor in a void);</li>
 *   <li><b>bounds</b>: at most {@code partyCap} players per door, {@code maxActiveSites} sites at once, a door cooldown per player.</li>
 * </ul>
 * All of it is on the server; per-player data is only the door cooldown (keyed by UUID, forgotten at logout); per-site data is a {@link BossRealmRules.Clock} per slot (at most 256).
 * Nothing here touches a client class. With {@code bossRealm.enabled = false} the doors answer "not mine" and the old behaviour is back.
 */
public final class BossRealm {
    private BossRealm() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The realm's own pass runs once a second. */
    public static final int STEP = 20;
    private static final int MAX_DOOR_ENTRIES = 512;

    private static final Map<Integer, BossRealmRules.Clock> CLOCKS = new HashMap<>();
    private static final Map<UUID, Long> DOORS = new HashMap<>();
    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(BossRealm::onTick);
        bus.addListener(BossRealm::onLogin);
        bus.addListener(BossRealm::onLogout);
        bus.addListener(BossRealm::onDeath);
        bus.addListener((ServerStoppedEvent e) -> {
            CLOCKS.clear();
            DOORS.clear();
        });
    }

    // ================================================================ who is a realm boss
    /** Does this character fight in the realm (a boss with no dungeon floor, not one of the seven older bosses, naming no arena of its own)? */
    public static boolean isRealmBoss(ArcCharacter c) {
        return c != null && BossRealmRules.isRealmBoss(c.isBoss(), c.floorMax(), ExistingBosses.owns(c.id()), !c.arena().isEmpty());
    }

    /** Would a door send this character's fight to the realm now (the setting is on and it is a realm boss)? */
    public static boolean redirects(ArcCharacter c) { return BossRealmConfig.enabled() && isRealmBoss(c); }

    /** The boss's own switch and cap, asked before a party is carried away (null = free). The door's own refusal then speaks, and nobody is moved for a boss that would not answer. */
    private static boolean bossAvailable(ArcCharacter c) {
        if (GokuBosses.owns(c.id())) return GokuBosses.gate() == null;
        if (SinbadBosses.owns(c.id())) return SinbadBosses.gate() == null;
        return true;
    }

    // ================================================================ the door
    /**
     * The door of a boss: carries {@code user} and the players near them into the boss's arena and returns true, or tells them why not and returns true; returns false when this is not the
     * realm's business (the setting is off, not a realm boss, no realm dimension, the user is already in an arena, the boss is switched off or at its cap) and the caller does what it always
     * did. Never throws.
     */
    public static boolean open(ServerPlayer user, ArcCharacter c) {
        try {
            return openChecked(user, c);
        } catch (RuntimeException ex) {
            LOG.error("[anime_multiverse] the boss realm door failed, the boss is made where the door is: {}", ex.toString());
            return false;
        }
    }

    private static boolean openChecked(ServerPlayer user, ArcCharacter c) {
        if (user == null || !redirects(c)) return false;
        MinecraftServer server = user.getServer();
        if (server == null || server.getLevel(ArcArena.DIMENSION) == null || ArcArena.in(user) || ZogratisArena.in(user)) return false;
        if (!bossAvailable(c)) return false;
        ServerLevel arena = server.getLevel(ArcArena.DIMENSION);
        ArcArenaSave save = ArcArenaSave.get(server);
        long now = server.overworld().getGameTime();
        int slot = save.slotOf(c.id());
        Set<Integer> active = activeSites(arena, save);
        Long last = DOORS.get(user.getUUID());
        boolean cooling = last != null && now - last < BossRealmConfig.doorCooldownTicks();
        BossRealmRules.Verdict v = BossRealmRules.verdict(true, active.size(), BossRealmConfig.maxActiveSites(), slot >= 0 && active.contains(slot), cooling);
        switch (v) {
            case SLOW_DOWN -> {
                user.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.realm.slow", "The realm is still settling. Try again in a moment.").withStyle(ChatFormatting.GRAY), true);
                return true;
            }
            case BUSY -> {
                user.sendSystemMessage(Component.translatableWithFallback("message.anime_multiverse.realm.busy", "Every arena of the realm is in use. Try again when a fight has ended.").withStyle(ChatFormatting.GRAY));
                return true;
            }
            case DISABLED -> { return false; }
            default -> { }
        }
        List<ServerPlayer> companions = companions(user);
        if (slot >= 0 && save.hasState(slot) && save.state(slot).phase() == ArenaState.Phase.COOLDOWN) {
            user.sendSystemMessage(Component.translatableWithFallback("message.anime_multiverse.realm.resting", "The arena is resting for %s more seconds; you will be carried home soon after.",
                    Long.toString(save.state(slot).cooldownSecondsLeft(now))).withStyle(ChatFormatting.GRAY));
        }
        ArcArenaTravel.enter(user, companions, c);
        remember(user.getUUID(), now);
        return true;
    }

    private static List<ServerPlayer> companions(ServerPlayer user) {
        double radius = BossRealmConfig.partyRadius();
        List<ServerPlayer> near = new ArrayList<>(user.level().getEntitiesOfClass(ServerPlayer.class, user.getBoundingBox().inflate(radius), p -> p != user));
        double[] d2 = new double[near.size()];
        boolean[] ok = new boolean[near.size()];
        for (int i = 0; i < near.size(); i++) {
            ServerPlayer p = near.get(i);
            d2[i] = p.distanceToSqr(user);
            ok[i] = p.isAlive() && !p.isSpectator() && !ArcArena.in(p) && !ZogratisArena.in(p);
        }
        List<ServerPlayer> out = new ArrayList<>();
        for (int i : BossRealmRules.companions(d2, ok, radius, BossRealmConfig.partyCap())) out.add(near.get(i));
        return out;
    }

    private static void remember(UUID id, long now) {
        if (DOORS.size() >= MAX_DOOR_ENTRIES) DOORS.values().removeIf(t -> now - t > BossRealmConfig.doorCooldownTicks());
        if (DOORS.size() < MAX_DOOR_ENTRIES) DOORS.put(id, now);
    }

    /** The realm sites that hold a player or a running fight (the sites a door can join without needing a free one). Bounded by the players online and by the sites of the grid. */
    static Set<Integer> activeSites(ServerLevel arena, ArcArenaSave save) {
        Set<Integer> out = new LinkedHashSet<>();
        for (ServerPlayer p : arena.players()) {
            int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
            if (slot >= 0 && isRealmBoss(characterAt(save, slot))) out.add(slot);
        }
        for (Map.Entry<Integer, ArenaState> e : save.allStates().entrySet()) if (e.getValue().fighting() && isRealmBoss(characterAt(save, e.getKey()))) out.add(e.getKey());
        return out;
    }

    private static ArcCharacter characterAt(ArcArenaSave save, int slot) {
        String id = save.characterAt(slot);
        return id == null ? null : ArcRoster.byId(id);
    }

    // ================================================================ the referee's question (grace)
    /**
     * Called by the arena referee for a site with a running fight: how many people it should believe are there. A story arena answers the real count; a realm site stays "occupied" for
     * {@code emptyGraceSeconds} after the last person is gone.
     */
    public static int presentWithGrace(int slot, String characterId, int count, long now) {
        if (!BossRealmConfig.enabled() || !isRealmBoss(ArcRoster.byId(characterId))) return count;
        return CLOCKS.computeIfAbsent(slot, s -> new BossRealmRules.Clock()).present(count, now, BossRealmConfig.graceTicks());
    }

    // ================================================================ the realm's own pass, once a second
    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % STEP != 0) return;
        if (!BossRealmConfig.enabled()) {
            CLOCKS.clear();
            return;
        }
        ServerLevel arena = server.getLevel(ArcArena.DIMENSION);
        if (arena == null || (arena.players().isEmpty() && CLOCKS.isEmpty())) return;
        try {
            pass(server, arena);
        } catch (RuntimeException ex) {
            LOG.error("[anime_multiverse] the boss realm pass failed and was skipped: {}", ex.toString());
        }
    }

    private static void pass(MinecraftServer server, ServerLevel arena) {
        ArcArenaSave save = ArcArenaSave.get(server);
        long now = arena.getGameTime();
        Map<Integer, List<ServerPlayer>> bySlot = new LinkedHashMap<>();
        for (ServerPlayer p : arena.players()) {
            int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
            if (slot >= 0) bySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(p);
        }
        Set<Integer> look = new LinkedHashSet<>(bySlot.keySet());
        look.addAll(CLOCKS.keySet());
        for (int slot : look) {
            ArcCharacter c = characterAt(save, slot);
            if (!isRealmBoss(c)) {
                CLOCKS.remove(slot);
                continue;
            }
            List<ServerPlayer> in = bySlot.getOrDefault(slot, List.of());
            ArenaState st = save.hasState(slot) ? save.state(slot) : null;
            BossRealmRules.Clock clock = CLOCKS.computeIfAbsent(slot, k -> new BossRealmRules.Clock());
            boolean fighting = st != null && st.fighting();
            clock.fight(fighting, now);
            if (fighting) {
                keepBossOnItsFloor(arena, save, c, slot);
                if (clock.timedOut(now, BossRealmConfig.timeoutTicks())) {
                    ArcArenaDirector.reset(server, slot);                                          // the boss goes, the site is ready again ...
                    save.state(slot).abandon(now);                                                  // ... and rests, so nobody starts the same fight again in the same breath
                    save.setDirty();
                    clock.fight(false, now);
                    for (ServerPlayer p : in) p.sendSystemMessage(Component.translatableWithFallback("message.anime_multiverse.realm.timeout",
                            "The fight has lasted too long. %s withdraws and the realm lets you go.", c.name()).withStyle(ChatFormatting.GRAY));
                    LOG.info("[anime_multiverse] the realm fight of {} at site {} timed out", c.id(), slot);
                    st = save.state(slot);
                }
            }
            boolean resting = st != null && st.phase() == ArenaState.Phase.COOLDOWN;
            if (clock.returnDue(resting, in.size(), now, BossRealmConfig.returnTicks())) {
                for (ServerPlayer p : in) ArcArenaTravel.leave(p);
            }
            if (clock.idle(in.size())) CLOCKS.remove(slot);
        }
    }

    /** The arena is a floor in a void: a boss that is under it (a knock, a blink, a bug) is put back on its spot instead of falling out of the world. One entity lookup per fighting site per second. */
    private static void keepBossOnItsFloor(ServerLevel arena, ArcArenaSave save, ArcCharacter c, int slot) {
        UUID id = save.bossId(slot);
        Entity boss = id == null ? null : arena.getEntity(id);
        if (boss == null || !boss.isAlive() || boss.getY() >= ArenaShape.FLOOR_Y - 12) return;
        ArenaShape shape = ArenaShapes.forCharacter(c);
        Vec3 at = ArcArena.bossStand(shape, slot);
        boss.teleportTo(at.x, at.y, at.z);
        boss.fallDistance = 0f;
        boss.setDeltaMovement(Vec3.ZERO);
        LOG.info("[anime_multiverse] {} fell below the floor of its realm arena (site {}) and was put back", c.id(), slot);
    }

    // ================================================================ login, logout, death
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !ArcArena.in(p) || p.getServer() == null) return;
            MinecraftServer server = p.getServer();
            int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
            if (slot < 0 || !isRealmBoss(characterAt(ArcArenaSave.get(server), slot))) return;      // the story arenas keep what they always did
            server.execute(() -> {
                if (!p.isRemoved() && ArcArena.in(p)) ArcArenaTravel.leave(p);                      // the fight was given up while they were away: nobody stays in a closed room
            });
        } catch (RuntimeException ex) {
            LOG.error("[anime_multiverse] the boss realm login check failed: {}", ex.toString());
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { DOORS.remove(e.getEntity().getUUID()); }

    /** A player who dies in a realm site respawns in the world, so the saved way back (where they stood before the door) is no longer needed. */
    private static void onDeath(LivingDeathEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !ArcArena.in(p) || p.getServer() == null) return;
            ArcArenaSave save = ArcArenaSave.get(p.getServer());
            int slot = ArenaGrid.slotAt(p.getX(), p.getZ());
            if (slot < 0 || !isRealmBoss(characterAt(save, slot))) return;
            if (save.returns().take(p.getUUID()) != null) save.setDirty();
        } catch (RuntimeException ex) {
            LOG.error("[anime_multiverse] the boss realm death check failed: {}", ex.toString());
        }
    }

    // ================================================================ for the status line and the self-test
    public static int clocks() { return CLOCKS.size(); }

    public static int doorEntries() { return DOORS.size(); }

    /** Forgets everything (a test). */
    public static void clearForTests() {
        CLOCKS.clear();
        DOORS.clear();
    }
}
