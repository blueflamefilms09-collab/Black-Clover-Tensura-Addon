package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.economy.LightItems;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryEncounters;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.squad.Squad;
import com.newuniverse.nusmp.labyrinth.squad.SquadHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Why: this is the Dungeon Master himself, the tick handler that answers "Vanilla spawning is too predictable". Every {@code rollEveryTicks} he
 * gathers the players of the Mega-Dungeon into parties, and then, one party per tick (so a crowded server never pays for all the rolls in one
 * tick), he does what a game master does behind the screen: checks that the party is exposed (not resting in a safe room, not in the middle of
 * the Guardian's fight), that it has been left alone long enough and is not already facing a crowd of his monsters, reads the sector's threat,
 * the party's size, the noise it has made and the light it carries, rolls a d20 against the difficulty class, and, if the dice say trouble,
 * picks an entry of the sector's theme table for the size of that trouble and puts the monsters where nobody can see them. The monsters then
 * get an order to go and find the party for a while (a hunt), because a monster that merely appears 40 blocks away and wanders off is not an
 * encounter. Everything is bounded: one roll per tick, a ray-cast cap in the picker, an attempt cap in the spawner, and a sweep that removes
 * his monsters when nobody is near. His state is a cooldown per PLAYER (a party's cooldown is the longest of its members', so regrouping does
 * not reset it) plus the last roll for the admin command; it is all in memory and is dropped at logout, at dimension change and at server
 * stop. The same {@link #roll} method serves the tick, the admin commands and the in-game self-tests, which pass the party explicitly (a
 * stand-in player is not in the level's player list).
 */
public final class EncounterDirector {
    private EncounterDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** {@code SIGHT_CHECKS_PER_EYE} (1.94.0): ray casts per member of the party per rung of the search (the single cap of 24 left a party of four only six cells to look at). */
    private static final int SWEEP_EVERY = 100, HUNT_EVERY = 20, MAX_TRACKED = 400, HUNT_PER_PASS = 40, SIGHT_CHECKS = 24, SIGHT_CHECKS_PER_EYE = 48;
    /** 1.94.0: places tried per monster = {@code spawnAttempts} times this (one cell and its neighbours is eleven places); the whole encounter never tries more than {@code MAX_PLACES}; three monsters in a row that find no place end it. */
    private static final int ATTEMPT_FACTOR = 4, MAX_PLACES = 400, MAX_FAILURES = 3;
    /** Persistent data on a monster whose entry has {@code hunt: 0} (it stays where it appeared): the stuck sweep leaves it alone. */
    static final String HOLD = "nusmp_dm_hold";
    /** {@code ROOM_HUNT_FROM} (1.94.0): a hunter farther than this from its prey follows the rooms; nearer it walks straight (the last stretch of a hunt, and the whole of a hunt in one big hall). */
    private static final double AIM_RANGE = 22.0, HUNT_REACH = 96.0, LIVE_RADIUS = 64.0, SEPARATION = 4.0, ROOM_HUNT_FROM = 12.0;

    // ---------------------------------------------------------------- results
    /** What a roll came to. */
    public enum Kind { SPAWNED, QUIET, NO_PARTY, OUTSIDE, SAFE_ZONE, NOT_READY, COOLDOWN, PEACEFUL, CROWDED, NO_ENTRY, NO_PLACE, TOO_SMALL }

    /**
     * Admin overrides. {@code ignoreRules}: skip the cooldown, the crowd cap and the safe-zone filter. {@code allowPeaceful}: go ahead even on
     * Peaceful (the monsters would be removed at once; the self-tests do not tick them). {@code band}: skip the dice and use this band.
     * {@code entryId}: skip the dice and the choice and use this entry of the sector's table.
     */
    public record Force(boolean ignoreRules, boolean allowPeaceful, Band band, String entryId) {
        public static Force admin() { return new Force(true, false, null, null); }

        public static Force band(Band b) { return new Force(true, false, b, null); }

        public static Force entry(String id) { return new Force(true, false, null, id); }

        public Force peacefulOk() { return new Force(ignoreRules, true, band, entryId); }
    }

    /** The result of one roll: what happened, a sentence for the admin, and (for a spawn) the monsters, the budget and what was spent of it. */
    public record Outcome(Kind kind, String text, List<Mob> mobs, double budget, double spent, EncounterRoll.Result roll, String entry, Band band,
                          List<SpawnPicker.Cell> cells, double min, double max) {
        public boolean spawned() { return kind == Kind.SPAWNED; }

        static Outcome of(Kind k, String text) { return new Outcome(k, text, List.of(), 0.0, 0.0, null, "", Band.NOTHING, List.of(), 0.0, 0.0); }
    }

    /** What the Director remembers of a player. */
    private static final class PlayerState {
        long cooldownUntil;
        long rolledAt = Long.MIN_VALUE;
        int natural, total;
        Band band = Band.NOTHING;
        boolean quiet;
        String entry = "";
        String note = "";
        /** 1.94.0: quiet passes since the last encounter (the pity timer, {@link Tension}). */
        int quietPasses;
        /** 1.94.0: a band the dice rolled but no place could be found for; the next passes try it again without rolling ({@code retryPasses}). Null when nothing is pending. */
        Band pendingBand;
        int retries;
        long pendingAt;
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static final ArrayDeque<PartyMath.Party> QUEUE = new ArrayDeque<>();
    /** Every live monster of the Director: its last known block. */
    private static final Map<UUID, Long> TRACKED = new LinkedHashMap<>();
    /** Monsters sent to find the party: the game time their hunt ends. */
    private static final Map<UUID, Long> HUNTS = new LinkedHashMap<>();
    private static int huntCursor;
    /** 1.94.0: how long each of his monsters has stayed where it is (the crowd cap must not count monsters that are stuck behind a wall). */
    private static final StuckTracker STUCK = new StuckTracker();
    /** 1.94.0: the outcomes since the last idle line. */
    private static final IdleReport IDLE = new IdleReport();
    private static long idleWindowStart = Long.MIN_VALUE;
    /** 1.94.0: proof for {@code why} and the self-test that the handler really runs: how many times, and the game time of the last time. */
    private static long ticksSeen, lastTickAt = Long.MIN_VALUE, lastPassAt = Long.MIN_VALUE;
    /** 1.94.0: every distinct failure is logged once with its stack; the count per kind and the latest are kept for {@code why}. */
    private static final Map<String, Integer> FAILURES = new LinkedHashMap<>();
    private static String lastFailure = "";

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        try {
            tick(e.getServer());
        } catch (Throwable t) {
            warn("tick", t);
        }
    }

    private static void tick(MinecraftServer server) {
        if (!EncConfig.get(EncConfig.ENABLED)) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        int tick = server.getTickCount() & Integer.MAX_VALUE;                        // never negative: the modulo tests below stop matching after the counter wraps (3.4 years of uptime)
        long now = level.getGameTime();
        ticksSeen++;
        lastTickAt = now;
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            if (!QUEUE.isEmpty()) QUEUE.clear();
            if (tick % SWEEP_EVERY == 37 && !TRACKED.isEmpty()) sweep(level, players);
            return;
        }
        IDLE.playersPresent();
        if (tick % EncConfig.get(EncConfig.ROLL_EVERY) == 0 && QUEUE.isEmpty()) fill(players);
        PartyMath.Party next = QUEUE.poll();
        if (next != null) {
            List<ServerPlayer> members = new ArrayList<>(next.size());
            for (PartyMath.Member m : next.members()) if (level.getPlayerByUUID(m.id()) instanceof ServerPlayer sp) members.add(sp);
            Outcome o = roll(level, members, null);
            lastPassAt = now;
            IDLE.pass(o.kind().name(), o.text(), o.spawned());
        }
        if (tick % SWEEP_EVERY == 37) sweep(level, players);
        if (tick % HUNT_EVERY == 11) hunt(level, players, now);
        idleLog(now);
    }

    /** The parties to roll for: every player the rules allow (Creative and Spectator players only when {@code rollForCreative} says so; Spectators never). */
    private static void fill(List<ServerPlayer> players) {
        List<PartyMath.Member> members = new ArrayList<>();
        boolean creativeToo = EncConfig.get(EncConfig.ROLL_FOR_CREATIVE);
        int skipped = 0;
        for (ServerPlayer p : players) {
            if (p.isAlive() && !p.isSpectator() && (creativeToo || !p.isCreative())) members.add(new PartyMath.Member(p.getUUID(), p.getX(), p.getY(), p.getZ(), PartyService.partyOf(p)));
            else skipped++;
        }
        if (!members.isEmpty()) QUEUE.addAll(PartyMath.group(members, EncConfig.get(EncConfig.PARTY_RADIUS)));
        else if (skipped > 0) IDLE.pass("NO_PARTY", skipped + " player(s) in the dungeon are in Creative or Spectator mode, or dead (enc.rollForCreative = false)", false);
    }

    /** Once a minute (config), when players were in the dungeon and nothing spawned, one line of the log says why. */
    private static void idleLog(long now) {
        int seconds = EncConfig.get(EncConfig.IDLE_LOG_SECONDS);
        if (seconds <= 0) return;
        if (idleWindowStart == Long.MIN_VALUE || now < idleWindowStart) {
            idleWindowStart = now;
            return;
        }
        if (now - idleWindowStart < seconds * 20L) return;
        String line = IDLE.line(seconds);
        if (line != null) LOG.info("[nusmp] Dungeon Master {}", line);
        IDLE.reset();
        idleWindowStart = now;
    }

    // ---------------------------------------------------------------- the roll
    /**
     * Rolls for one party. {@code party} are the players (the tick passes one group of {@link PartyMath}; a command or a self-test passes whoever it
     * means). With {@code force == null} every rule applies and the dice decide. Never throws; a failure is an {@link Outcome} that says why.
     */
    public static Outcome roll(ServerLevel level, List<? extends ServerPlayer> party, Force force) { return roll(level, party, force, null); }

    /** 1.94.0: like {@link #roll(ServerLevel, List, Force)} with the source of chance given (a self-test passes a seeded one so the same roll comes out every time); null = the level's. */
    public static Outcome roll(ServerLevel level, List<? extends ServerPlayer> party, Force force, Random random) {
        try {
            return rollInner(level, party, force, random);
        } catch (Throwable t) {
            warn("roll", t);
            return Outcome.of(Kind.NO_PLACE, "the roll failed: " + t);
        }
    }

    private static Outcome rollInner(ServerLevel level, List<? extends ServerPlayer> party, Force force, Random random) {
        if (!Labyrinth.inMega(level)) return Outcome.of(Kind.OUTSIDE, "this is not the Mega-Dungeon");
        boolean admin = force != null && force.ignoreRules();
        boolean creativeToo = EncConfig.get(EncConfig.ROLL_FOR_CREATIVE);
        List<ServerPlayer> ps = new ArrayList<>();
        for (ServerPlayer p : party) if (p.isAlive() && (force != null || (!p.isSpectator() && (creativeToo || !p.isCreative())))) ps.add(p);
        if (ps.isEmpty()) return Outcome.of(Kind.NO_PARTY, "there is nobody to roll for (Creative and Spectator players are skipped unless enc.rollForCreative is true)");
        long now = level.getGameTime();

        // the sector, and who is exposed (not resting, not in the boss chain)
        Sector sector = null;
        List<ServerPlayer> exposed = new ArrayList<>();
        for (ServerPlayer p : ps) {
            BlockPos at = p.blockPosition();
            Sector s = MegaDungeon.sectorAt(level, at);
            if (s == null) continue;
            if (sector == null) sector = s;
            if (s != sector) continue;
            Room r = s.roomAt(at);
            if (!admin && r != null && Sanctuary.is(r.kind)) continue;
            exposed.add(p);
        }
        if (sector == null) return Outcome.of(Kind.OUTSIDE, "the party stands between two sectors");
        if (exposed.isEmpty()) return Outcome.of(Kind.SAFE_ZONE, "the party is in a safe zone or a boss room: the Dungeon Master does not roll");
        if (sector.state == Sector.State.GENERATING || sector.state == Sector.State.FAILED || sector.rooms.isEmpty()) return Outcome.of(Kind.NOT_READY, "sector " + sector.id + " is not ready");
        EncounterTable table = EncData.table(sector.theme, sector.polarity);
        if (table.entries().isEmpty()) return Outcome.of(Kind.NO_ENTRY, "the encounter table of " + sector.theme.id() + " is empty");
        if (level.getDifficulty() == Difficulty.PEACEFUL && !(force != null && force.allowPeaceful())) return Outcome.of(Kind.PEACEFUL, "the world is Peaceful: the monsters would vanish at once");
        if (!admin) {
            long cool = 0L;
            for (ServerPlayer p : ps) cool = Math.max(cool, state(p.getUUID()).cooldownUntil);
            if (cool > now) return Outcome.of(Kind.COOLDOWN, "the party is left alone for " + (cool - now) / 20 + " more seconds");
            int live = liveNear(level, exposed);
            if (live >= EncConfig.get(EncConfig.MAX_LIVE)) return Outcome.of(Kind.CROWDED, live + " of his monsters live near the party already (maxLiveMobsPerParty " + EncConfig.get(EncConfig.MAX_LIVE) + ")");
        }

        // the dice (or, when a rolled encounter found no place a moment ago, the same band again: the roll is not lost)
        View v = view(level, sector, exposed, table, now);
        int threat = v.threat();
        Random rnd = random != null ? random : new Random(level.getRandom().nextLong());
        EncounterRoll.Result res;
        Band retry = admin ? null : pendingBand(exposed, now);
        if (force != null && force.band() != null) res = new EncounterRoll.Result(0, v.mods(), 0, v.dc(), 0, force.band(), false, false);
        else if (force != null && force.entryId() != null) res = null;
        else if (retry != null) res = new EncounterRoll.Result(0, v.mods(), 0, v.dc(), 0, retry, false, false);
        else res = EncounterRoll.resolve(EncounterRoll.d20(rnd), v.mods(), v.dc());
        for (ServerPlayer p : exposed) {
            PlayerState st = state(p.getUUID());
            st.rolledAt = now;
            st.natural = res == null ? 0 : res.natural();
            st.total = res == null ? 0 : res.total();
            st.band = res == null ? Band.NOTHING : res.band();
            st.quiet = res != null && res.quiet();
            st.entry = "";
            st.note = res == null ? "forced entry" : retry != null ? "retry of a rolled " + retry.label() + " encounter" : describe(res, v);
        }
        if (res != null && res.band() == Band.NOTHING) {
            if (force == null) for (ServerPlayer p : exposed) {                                                           // a quiet pass raises the party's tension (the pity timer)
                PlayerState st = state(p.getUUID());
                st.quietPasses = Tension.quiet(st.quietPasses);
            }
            return new Outcome(Kind.QUIET, "rolled " + describe(res, v) + ": quiet", List.of(), 0.0, 0.0, res, "", Band.NOTHING, List.of(), 0.0, 0.0);
        }

        // the entry
        EncounterTable.Entry entry;
        Band band;
        if (force != null && force.entryId() != null) {
            entry = table.byId(force.entryId());
            if (entry == null) return Outcome.of(Kind.NO_ENTRY, "the table of " + sector.theme.id() + " has no entry '" + force.entryId() + "'");
            band = entry.band();
        } else {
            EncounterRoll.Choice choice = EncounterRoll.choose(table.entries(), res.band(), threat, v.size(), v.light().dark(), v.light().lit(), v.noisy(), e -> usable(e, table, threat), rnd);
            if (choice == null && res.band() != Band.AMBUSH) {
                // 1.94.0: no usable entry at the rolled band or below (a thin table, tags with no member in the toughness window): the nearest bigger fight, paid for with the ROLLED band's
                // budget, so a roll that the dice won is not thrown away. (If even the first monster of that entry costs more than the budget the plan comes out empty and the roll is retried.)
                EncounterRoll.Choice up = EncounterRoll.choose(table.entries(), res.band().up(), threat, v.size(), v.light().dark(), v.light().lit(), v.noisy(), e -> usable(e, table, threat), rnd);
                if (up != null) choice = new EncounterRoll.Choice(up.entry(), res.band());
            }
            if (choice == null) return new Outcome(Kind.NO_ENTRY, "rolled " + describe(res, v) + " (" + res.band().label() + ") but the table has no usable entry", List.of(), 0.0, 0.0, res, "", res.band(), List.of(), 0.0, 0.0);
            entry = choice.entry();
            band = choice.band();
        }
        Outcome out = spawnEntry(level, sector, table, exposed, entry, band, v, rnd, now, res);
        if (out.spawned()) {
            for (ServerPlayer p : exposed) {
                PlayerState st = state(p.getUUID());
                st.entry = entry.id();
                st.quietPasses = 0;                                                       // the tension is spent
                st.pendingBand = null;
                st.retries = 0;
            }
            long cool = EncounterRoll.cooldown(EncConfig.get(EncConfig.BASE_COOLDOWN), EncConfig.get(EncConfig.MIN_COOLDOWN), v.noise());
            for (ServerPlayer p : ps) state(p.getUUID()).cooldownUntil = now + cool;
        } else if (force == null && (out.kind() == Kind.NO_PLACE || out.kind() == Kind.TOO_SMALL)) {
            keepForRetry(exposed, out.band(), now);                                      // the dice said trouble: do not lose it because the world had no room
        }
        return out;
    }

    /** The band a party's earlier roll could not place and is still owed, or null. A band that has waited more than two minutes is dropped. */
    private static Band pendingBand(List<ServerPlayer> exposed, long now) {
        Band best = null;
        for (ServerPlayer p : exposed) {
            PlayerState st = STATES.get(p.getUUID());
            if (st == null || st.pendingBand == null) continue;
            if (now - st.pendingAt > 2400L) {
                st.pendingBand = null;
                st.retries = 0;
                continue;
            }
            if (best == null || st.pendingBand.ordinal() > best.ordinal()) best = st.pendingBand;
        }
        return best;
    }

    private static void keepForRetry(List<ServerPlayer> exposed, Band band, long now) {
        int max = EncConfig.get(EncConfig.RETRY_PASSES);
        for (ServerPlayer p : exposed) {
            PlayerState st = state(p.getUUID());
            if (max <= 0 || band == null || band == Band.NOTHING || st.retries >= max) {
                st.pendingBand = null;                                                    // out of retries: the next pass rolls the dice again (and the tension is still there)
                st.retries = 0;
            } else {
                if (st.pendingBand == null) st.pendingAt = now;
                st.pendingBand = band;
                st.retries++;
            }
        }
    }

    private static Outcome spawnEntry(ServerLevel level, Sector sector, EncounterTable table, List<ServerPlayer> exposed, EncounterTable.Entry entry, Band band,
                                      View v, Random rnd, long now, EncounterRoll.Result res) {
        int threat = v.threat();
        double budget = EncounterRoll.budget(EncConfig.get(EncConfig.BASE_BUDGET), band, threat, v.factor(), table.budgetScale());
        double min = entry.minRange() > 0.0 ? entry.minRange() : EncConfig.get(EncConfig.SPAWN_MIN);
        double max = entry.maxRange() > 0.0 ? entry.maxRange() : Math.max(min + 8.0, EncConfig.get(EncConfig.SPAWN_MAX));
        List<SpawnPicker.Eye> eyes = new ArrayList<>(exposed.size());
        for (ServerPlayer p : exposed) {
            Vec3 eye = p.getEyePosition();
            eyes.add(new SpawnPicker.Eye(eye.x, eye.y, eye.z));
        }
        LevelSight sight = new LevelSight(level, exposed.get(0));
        // 1.94.0: the ladder of SpawnSearch (the ring, a wider ring, every room, a far place in view) from every member's eyes, instead of one ring round the party's centre
        SpawnSearch.Found found = SpawnSearch.find(CellSource.layoutOf(sector), eyes, min, max, Math.max(1, entry.cells()), sight, SIGHT_CHECKS_PER_EYE,
                EncConfig.get(EncConfig.SPAWN_WIDEN), EncConfig.get(EncConfig.RELAX_SIGHT_TIER) - 1, rnd);
        if (!found.ok()) {
            return new Outcome(Kind.NO_PLACE, "entry '" + entry.id() + "': no place found out of sight (" + String.join("; ", found.trace()) + ")", List.of(), budget, 0.0, res, entry.id(), band, List.of(), min, max);
        }
        List<SpawnPicker.Cell> cells = found.cells();
        SpawnSearch.Tier tier = found.tier();
        min = tier.min();                                                                  // the rung that worked sets the distances the placement keeps to
        max = tier.max();

        // which monsters: explicit ids, or members of a faction (the entry's, else the native one of the room, else the table's)
        String nativeFaction = nativeFaction(sector, cells.get(0));
        List<EncounterRoll.Pick> picks = new ArrayList<>();
        Map<String, String> factionOf = new HashMap<>();
        Map<String, Double> costOf = new HashMap<>();
        int foundryFloor = FoundryEncounters.floorOf(sector, v.floor());          // Mob Foundry: the floor the foundry tier is read from
        for (EncounterTable.MobSpec spec : entry.mobs()) {
            EncounterRoll.Pick pick = null;
            if (spec.kind() == EncounterTable.Kind.ENTITY) {
                EntityType<?> t = Bestiary.type(spec.entity());
                if (Bestiary.usable(t)) pick = new EncounterRoll.Pick(spec.entity(), EncounterRoll.cost(Bestiary.toughness(t)));
            } else {
                String fixed = !spec.faction().isEmpty() ? spec.faction() : entry.faction();
                MobPicker.Candidate c = null;
                String used = "";
                if (!fixed.isEmpty()) {
                    c = MobPicker.pick(Bestiary.members(fixed), threat, rnd);
                    used = fixed;
                } else {
                    if (nativeFaction != null) {
                        c = MobPicker.pick(Bestiary.members(nativeFaction), threat, rnd);
                        used = nativeFaction;
                    }
                    if (c == null && table.defaultFaction() != null) {
                        c = MobPicker.pick(Bestiary.members(table.defaultFaction()), threat, rnd);
                        used = table.defaultFaction();
                    }
                }
                if (c != null) {
                    pick = new EncounterRoll.Pick(c.id(), EncounterRoll.cost(c.toughness()));
                    factionOf.put(c.id(), used);
                }
            }
            EncounterRoll.Pick fp = FoundryEncounters.pick(sector.polarity, sector.theme.id(), foundryFloor, spec.kind() == EncounterTable.Kind.FACTION ? factionOf.getOrDefault(pick == null ? "" : pick.entity(), nativeFaction) : null, rnd);   // Mob Foundry: sometimes a foundry mob of this floor's tier and road instead
            if (fp != null) { pick = fp; factionOf.put(fp.entity(), FoundryEncounters.factionOf(fp.entity())); }
            picks.add(pick);
            if (pick != null) costOf.put(pick.entity(), pick.cost());
        }
        List<EncounterRoll.Planned> plan = EncounterRoll.plan(entry, picks, v.factor(), budget, EncConfig.get(EncConfig.MAX_PER_ENCOUNTER), rnd);
        if (plan.isEmpty()) return new Outcome(Kind.TOO_SMALL, "entry '" + entry.id() + "': nothing of it fits a budget of " + String.format(java.util.Locale.ROOT, "%.1f", budget), List.of(), budget, 0.0, res, entry.id(), band, cells, min, max);
        plan = SquadBridge.shape(level, plan, entry, band, v, sector.theme.id(), nativeFaction, table, factionOf, costOf, rnd);          // squads: 5 to 8 monsters with roles (squads.enabled; false = the plan as it is)

        // the monsters
        // 1.94.0: each monster has a budget of its own (the old single budget of twelve places for the whole encounter was spent by one bad cell), and when its cell and the
        // places beside it fail it falls back on the other cells of the group, then on the picker's reserve, then on the mob's own pathfinder
        int perMob = EncConfig.get(EncConfig.SPAWN_ATTEMPTS) * ATTEMPT_FACTOR;
        int[] rejects = new int[EncounterSpawner.Reject.values().length];
        int totalCount = 0;
        for (EncounterRoll.Planned pl : plan) totalCount += pl.count();
        int budgetLeft = Math.min(MAX_PLACES, perMob * Math.max(1, totalCount)), triedPlaces = 0, failures = 0;
        List<BlockPos> group = new ArrayList<>(), reserve = new ArrayList<>();
        for (SpawnPicker.Cell c : cells) group.add(new BlockPos(c.x(), c.y(), c.z()));
        for (SpawnPicker.Cell c : found.reserve()) reserve.add(new BlockPos(c.x(), c.y(), c.z()));
        List<Mob> made = new ArrayList<>();
        double spent = 0.0;
        int at = 0;
        spawning:
        for (EncounterRoll.Planned pl : plan) {
            EntityType<?> type = Bestiary.type(FoundryEncounters.baseId(pl.entity()));
            if (type == null) continue;
            for (int i = 0; i < pl.count(); i++) {
                if (budgetLeft <= 0 || failures >= MAX_FAILURES) break spawning;
                int index = at++ % cells.size();
                SpawnPicker.Cell cell = cells.get(index);
                List<BlockPos> alternatives = new ArrayList<>(group.size() + reserve.size());
                for (int k = 1; k < group.size(); k++) alternatives.add(group.get((index + k) % group.size()));
                alternatives.addAll(reserve);
                int[] attempts = {Math.min(perMob, budgetLeft)};
                EncounterSpawner.Rules rules = new EncounterSpawner.Rules(eyes, min, max, sight, attempts, rejects, tier.relaxSightFrom(), true);
                FoundryEncounters.expect(pl.entity(), foundryFloor);
                Mob m = EncounterSpawner.spawn(level, type, new BlockPos(cell.x(), cell.y(), cell.z()), alternatives, rules, threat, factionOf.getOrDefault(pl.entity(), entry.faction()), entry.id(), rnd);
                int used = Math.min(perMob, budgetLeft) - attempts[0];
                budgetLeft -= used;
                triedPlaces += used;
                if (m != null) {
                    made.add(m);
                    spent += costOf.getOrDefault(pl.entity(), 1.0);
                    failures = 0;
                } else {
                    failures++;
                }
            }
        }
        if (made.isEmpty()) {
            SquadHook.abort();
            return new Outcome(Kind.NO_PLACE, "entry '" + entry.id() + "': no position took a monster (" + triedPlaces + " places tried on rung '" + tier.name() + "' " + tier.range() + "; refused: " + EncounterSpawner.describe(rejects) + ")",
                    List.of(), budget, 0.0, res, entry.id(), band, cells, min, max);
        }

        Squad squad = SquadHook.end(level, made);                                      // squads: the roles, the formation and the System line
        long hunt = entry.hunt() >= 0 ? entry.hunt() : band.huntTicks();
        for (Mob m : made) {
            if (TRACKED.size() < MAX_TRACKED) TRACKED.put(m.getUUID(), m.blockPosition().asLong());
            if (hunt > 0 && HUNTS.size() < MAX_TRACKED) HUNTS.put(m.getUUID(), now + hunt);
            STUCK.update(m.getUUID(), m.getX(), m.getY(), m.getZ(), now);
            if (hunt == 0) m.getPersistentData().putBoolean(HOLD, true);                    // a turret that was told to stay put is not "stuck"
        }
        if (EncConfig.get(EncConfig.CUES)) EncounterCue.play(level, exposed, cells.get(0), entry, band, rnd);
        if (!entry.message().isEmpty()) for (ServerPlayer p : exposed) EncounterCue.tell(p, entry.message());
        String text = "'" + entry.id() + "' (" + band.label() + "): " + made.size() + " of " + totalCount + " monsters, " + String.format(java.util.Locale.ROOT, "%.1f of %.1f", spent, budget) + " budget, " + cells.size() + " places"
                + (tier.index() > 0 ? ", rung '" + tier.name() + "' " + tier.range() : "") + (made.size() < totalCount ? ", refused: " + EncounterSpawner.describe(rejects) : "")
                + (res == null ? "" : ", rolled " + describe(res, v)) + (squad == null ? "" : ", squad " + squad.describe());
        return new Outcome(Kind.SPAWNED, text, made, budget, spent, res, entry.id(), band, cells, min, max);
    }

    /** Can the game spawn this entry for a sector of this threat (is the first monster there to be had)? */
    private static boolean usable(EncounterTable.Entry e, EncounterTable table, int threat) {
        if (e.mobs().isEmpty()) return false;
        EncounterTable.MobSpec first = e.mobs().get(0);
        if (first.kind() == EncounterTable.Kind.ENTITY) return Bestiary.usable(Bestiary.type(first.entity()));
        String faction = !first.faction().isEmpty() ? first.faction() : !e.faction().isEmpty() ? e.faction() : table.defaultFaction();
        return faction != null && !MobPicker.window(Bestiary.members(faction), MobPicker.cap(threat)).isEmpty();
    }

    private static String nativeFaction(Sector sector, SpawnPicker.Cell cell) {
        Room r = cell.roomId() < 0 ? null : sector.room(cell.roomId());
        return r == null || r.faction == null || r.faction.isEmpty() || !EncounterTable.FACTIONS.contains(r.faction) ? null : r.faction;
    }

    // ---------------------------------------------------------------- the reading of a party
    /** Everything the dice are made of, for the roll and for the status command. */
    record View(int size, int threat, int floor, double factor, double noise, LightRules.Light light, boolean noisy, EncounterRoll.Mods mods, int dc, int quietPasses) {}

    static View view(ServerLevel level, Sector sector, List<ServerPlayer> exposed, EncounterTable table, long now) {
        int n = exposed.size();
        int[] block = new int[n];
        boolean[] held = new boolean[n];
        double noise = 0.0;
        for (int i = 0; i < n; i++) {
            ServerPlayer p = exposed.get(i);
            block[i] = level.getBrightness(LightLayer.BLOCK, BlockPos.containing(p.getEyePosition()));
            held[i] = heldLight(p);
            noise = Math.max(noise, NoiseEvents.reading(p.getUUID(), now));
        }
        LightRules.Light light = LightRules.evaluate(block, held);
        double[] factors = PartyMath.parseFactors(EncConfig.get(EncConfig.PARTY_FACTORS));
        double factor = PartyMath.factor(n, factors, EncConfig.get(EncConfig.PARTY_EXTRA));
        boolean byFloor = EncConfig.get(EncConfig.FLOOR_THREAT);
        int floor = byFloor ? FloorThreat.floorOf(level, exposed) : 0;
        int threat = ThreatInput.of(sector.threat, floor, byFloor);
        int quiet = 0;
        for (ServerPlayer p : exposed) {
            PlayerState st = STATES.get(p.getUUID());
            if (st != null) quiet = Math.max(quiet, st.quietPasses);                      // the party's tension is its most restless member's
        }
        EncounterRoll.Mods mods = EncounterRoll.mods(threat, noise, light.mod(), n, table.rollBonus(), EncounterRoll.Caps.DEFAULT)
                .withTension(Tension.bonus(quiet, EncConfig.get(EncConfig.TENSION_EVERY), EncConfig.get(EncConfig.TENSION_CAP)));
        return new View(n, threat, floor, factor, noise, light, noise >= EncConfig.get(EncConfig.NOISY_AT), mods, EncConfig.get(EncConfig.DC), quiet);
    }

    /** A light in either hand: a block item whose block emits light, or an item the id test knows (the idea of the gloom clock). */
    static boolean heldLight(ServerPlayer p) {
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getLightEmission() > 0) return true;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (LightItems.isLight(id.getNamespace(), id.getPath())) return true;
        }
        return false;
    }

    private static String describe(EncounterRoll.Result r, View v) {
        if (r.natural() == 0) return "forced " + r.band().label();
        EncounterRoll.Mods m = r.mods();
        return "d20 " + r.natural() + " + " + m.total() + " (threat " + m.threat() + ", noise " + m.noise() + ", light " + m.light() + ", party " + m.party() + ", table " + m.table() + ", tension " + m.tension() + ") = " + r.total() + " vs DC " + v.dc()
                + (r.escalated() ? ", natural 20" : "") + (r.quiet() ? ", natural 1" : "");
    }

    private static double[] centre(List<ServerPlayer> ps) {
        double x = 0, y = 0, z = 0;
        for (ServerPlayer p : ps) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int n = Math.max(1, ps.size());
        return new double[] {x / n, y / n, z / n};
    }

    private static PlayerState state(UUID id) { return STATES.computeIfAbsent(id, k -> new PlayerState()); }

    // ---------------------------------------------------------------- his monsters: counting, hunting, sweeping
    /** How many of his monsters live within 64 blocks of a point. */
    static int liveNear(ServerLevel level, double x, double y, double z) {
        int n = 0;
        double r2 = LIVE_RADIUS * LIVE_RADIUS;
        long now = level.getGameTime();
        for (UUID id : TRACKED.keySet()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive() && e.distanceToSqr(x, y, z) <= r2 && !stuck(e, now)) n++;
        }
        return n;
    }

    /** 1.94.0: how many of his monsters live within 64 blocks of ANY member of the party (the centre of a spread party can be far from all of them). */
    static int liveNear(ServerLevel level, List<? extends ServerPlayer> party) {
        int n = 0;
        double r2 = LIVE_RADIUS * LIVE_RADIUS;
        long now = level.getGameTime();
        for (UUID id : TRACKED.keySet()) {
            Entity e = level.getEntity(id);
            if (e == null || !e.isAlive() || stuck(e, now)) continue;
            for (ServerPlayer p : party) {
                if (e.distanceToSqr(p) <= r2) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    /** Has this monster stood in one place for {@code stuckTicks} (it cannot reach anyone, and it must not hold back the next encounter)? */
    private static boolean stuck(Entity e, long now) {
        return STUCK.idle(e.getUUID(), e.getX(), e.getY(), e.getZ(), now) >= EncConfig.get(EncConfig.STUCK_TICKS);
    }

    /** One look a second at the monsters sent to find the party: walk to the nearest exposed player, and aim at them when close. */
    static void hunt(ServerLevel level, List<? extends ServerPlayer> players, long now) {
        if (HUNTS.isEmpty()) return;
        List<UUID> ids = new ArrayList<>(HUNTS.keySet());
        int n = ids.size();
        int start = Math.floorMod(huntCursor, n);
        for (int k = 0; k < Math.min(n, HUNT_PER_PASS); k++) {
            UUID id = ids.get((start + k) % n);
            Long until = HUNTS.get(id);
            if (until == null) continue;
            Entity e = level.getEntity(id);
            if (e == null) continue;                                             // unloaded or gone: the sweep decides
            if (!(e instanceof Mob m) || !m.isAlive() || now >= until) {
                HUNTS.remove(id);
                continue;
            }
            if (SquadHook.followsLeader(m)) continue;                          // squads: only the leader is sent to find the party, the others keep formation behind it
            if (m.getTarget() instanceof Player t && t.isAlive() && !t.isSpectator()) {
                if (m.hasLineOfSight(t) || !EncConfig.get(EncConfig.HUNT_BY_ROOMS)) continue;          // it found someone by itself, and sees them: its own goals take over
                MobSteer.release(m);                                                                    // 1.94.0: a target behind a wall is a walk its own goals cannot make; the rooms can
            }
            ServerPlayer prey = null;
            double best = HUNT_REACH * HUNT_REACH;
            boolean creativeToo = EncConfig.get(EncConfig.ROLL_FOR_CREATIVE);
            for (ServerPlayer p : players) {
                if (!p.isAlive() || p.isSpectator() || (p.isCreative() && !creativeToo)) continue;
                double d = m.distanceToSqr(p);
                if (d >= best) continue;
                if (MegaDungeon.inSafeZone(level, p.blockPosition())) continue;
                best = d;
                prey = p;
            }
            if (prey == null) continue;
            if (best <= AIM_RANGE * AIM_RANGE && (m.hasLineOfSight(prey) || !EncConfig.get(EncConfig.HUNT_BY_ROOMS))) MobSteer.aim(m, prey);
            double[] goal = {prey.getX(), prey.getY(), prey.getZ()};
            if (best > ROOM_HUNT_FROM * ROOM_HUNT_FROM && EncConfig.get(EncConfig.HUNT_BY_ROOMS)) roomGoal(level, m, prey, goal);
            MobSteer.walk(m, goal[0], goal[1], goal[2], 1.0);
        }
        huntCursor = (huntCursor + HUNT_PER_PASS) & 0x7fffffff;
    }

    /**
     * 1.94.0: when the prey is in another room, the goal becomes the waypoint of the next room on the way along the doorways ({@link HuntRoute}); {@code goal} is left
     * alone (the straight walk at the player) when the mob or the prey is outside the floor plan, in the same room, or no way is known. Never throws.
     */
    private static void roomGoal(ServerLevel level, Mob m, ServerPlayer prey, double[] goal) {
        try {
            Sector sector = MegaDungeon.sectorAt(level, m.blockPosition());
            if (sector == null || sector.rooms.isEmpty() || MegaDungeon.sectorAt(level, prey.blockPosition()) != sector) return;
            Room from = sector.roomAt(m.blockPosition()), to = sector.roomAt(prey.blockPosition());
            if (from == null || to == null) return;
            PatrolGraph.Node next = HuntRoute.next(WandererDirector.graphOf(sector), from.id, to.id, m.getX(), m.getZ());
            if (next == null) return;
            goal[0] = next.x();
            goal[1] = next.y();
            goal[2] = next.z();
        } catch (RuntimeException ignored) {
            // the straight walk stays
        }
    }

    /** Every {@code SWEEP_EVERY} ticks: forget the dead, remove his monsters that no player is near, and the old idle ones. */
    static void sweep(ServerLevel level, List<? extends ServerPlayer> players) {
        double far = EncConfig.get(EncConfig.DESPAWN_DISTANCE), far2 = far * far;
        long maxAge = EncConfig.get(EncConfig.MAX_AGE), now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> t = it.next();
            Entity e = level.getEntity(t.getKey());
            if (e == null) {
                if (level.hasChunkAt(BlockPos.of(t.getValue()))) {                  // its chunk is loaded and it is not there: it is gone
                    it.remove();
                    HUNTS.remove(t.getKey());
                }
                continue;
            }
            if (!e.isAlive() || e.isRemoved()) {
                it.remove();
                HUNTS.remove(t.getKey());
                continue;
            }
            t.setValue(e.blockPosition().asLong());
            long idle = STUCK.update(t.getKey(), e.getX(), e.getY(), e.getZ(), now);
            double near = Double.MAX_VALUE;
            for (ServerPlayer p : players) if (!p.isSpectator()) near = Math.min(near, e.distanceToSqr(p));
            boolean remove = near > far2;
            if (!remove && e instanceof Mob m && near > 24.0 * 24.0 && m.getTarget() == null) {
                long born = m.getPersistentData().getLong(EncounterSpawner.BORN);
                remove = born > 0L && now - born > maxAge;
                // 1.94.0: one that has not moved for stuckTicks, that nobody can see, is behind a wall it cannot pass: it goes, so it stops holding the next encounter back
                if (!remove && idle >= EncConfig.get(EncConfig.STUCK_TICKS) && !m.getPersistentData().getBoolean(HOLD) && !seenByAny(players, e)) remove = true;
            }
            if (remove) {
                e.discard();
                it.remove();
                HUNTS.remove(t.getKey());
                STUCK.forget(t.getKey());
            }
        }
    }

    /** Can any player within 64 blocks see the entity (a ray cast from their eyes)? */
    private static boolean seenByAny(List<? extends ServerPlayer> players, Entity e) {
        for (ServerPlayer p : players) {
            if (p.isSpectator() || p.distanceToSqr(e) > LIVE_RADIUS * LIVE_RADIUS) continue;
            if (p.hasLineOfSight(e)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- events and housekeeping
    /** A monster of his that comes back from a saved chunk is looked after again. */
    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            Entity en = e.getEntity();
            if (e.getLevel().isClientSide() || !(en instanceof Mob) || !Labyrinth.inMega(e.getLevel())) return;
            if (TRACKED.size() < MAX_TRACKED && EncounterSpawner.isMine(en)) TRACKED.putIfAbsent(en.getUUID(), en.blockPosition().asLong());
        } catch (Throwable t) {
            warn("join", t);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forgetPlayer(e.getEntity().getUUID()); }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { forgetPlayer(e.getEntity().getUUID()); }

    public static void onServerStopped(ServerStoppedEvent e) { reset(); }

    /** Drops what is remembered of a player (their cooldown and last roll, their noise). */
    public static void forgetPlayer(UUID id) {
        STATES.remove(id);
        NoiseEvents.forget(id);
    }

    /** Forgets everything (server stop). */
    public static void reset() {
        STATES.clear();
        QUEUE.clear();
        TRACKED.clear();
        HUNTS.clear();
        STUCK.clear();
        IDLE.reset();
        idleWindowStart = Long.MIN_VALUE;
        ticksSeen = 0L;
        lastTickAt = Long.MIN_VALUE;
        lastPassAt = Long.MIN_VALUE;
        huntCursor = 0;
        NoiseEvents.clear();
        EncounterSpawner.forgetWarnings();
        Bestiary.clear();
    }

    /** Removes every monster of the Director in the level (the admin command and the self-tests). Returns how many were removed. */
    public static int clear(ServerLevel level) {
        int n = 0;
        List<Entity> found = new ArrayList<>();
        for (Entity e : level.getAllEntities()) if (EncounterSpawner.isMine(e)) found.add(e);
        for (Entity e : found) {
            e.discard();
            n++;
        }
        TRACKED.clear();
        HUNTS.clear();
        STUCK.clear();
        return n;
    }

    /** How many monsters the Director is looking after. */
    public static int tracked() { return TRACKED.size(); }

    // ---------------------------------------------------------------- the admin view
    /** Lines for {@code /multiverse mega dm status}: the party, the dice inputs, the cooldown and the last roll. */
    public static List<String> status(ServerLevel level, ServerPlayer player, List<? extends ServerPlayer> party) {
        List<String> out = new ArrayList<>();
        long now = level.getGameTime();
        Sector sector = MegaDungeon.sectorAt(level, player.blockPosition());
        if (sector == null) {
            out.add("You are not inside a sector.");
            return out;
        }
        Room room = sector.roomAt(player.blockPosition());
        EncounterTable table = EncData.table(sector.theme, sector.polarity);
        List<ServerPlayer> ps = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        for (ServerPlayer p : party) {
            ps.add(p);
            if (names.length() > 0) names.append(", ");
            names.append(p.getGameProfile().getName());
        }
        View v = view(level, sector, ps, table, now);
        long cool = 0L;
        for (ServerPlayer p : ps) cool = Math.max(cool, state(p.getUUID()).cooldownUntil);
        out.add("Party of " + ps.size() + ": " + names + ". Sector " + sector.id + " (" + sector.theme.id() + ", " + sector.polarity.id() + " road, sector threat " + sector.threat + "), room "
                + (room == null ? "-" : room.kind.name().toLowerCase(java.util.Locale.ROOT)) + ".");
        out.add("Floor " + (v.floor() == 0 ? "unknown" : FloorMap.label(v.floor())) + ", the dice read threat " + v.threat() + (v.floor() != 0 ? " (from the floor)" : " (from the sector)") + ".");
        out.add("Noise " + String.format(java.util.Locale.ROOT, "%.2f", v.noise()) + (v.noisy() ? " (noisy)" : "") + ", light " + v.light().brightness() + (v.light().dark() ? " (dark)" : v.light().lit() ? " (lit)" : "")
                + ", party factor " + String.format(java.util.Locale.ROOT, "%.2f", v.factor()) + ".");
        EncounterRoll.Mods m = v.mods();
        out.add("Modifiers: threat " + m.threat() + ", noise " + m.noise() + ", light " + m.light() + ", party " + m.party() + ", table " + m.table() + ", tension " + m.tension() + " = " + m.total() + " against DC " + v.dc()
                + " (" + v.quietPasses() + " quiet passes so far).");
        out.add("Cooldown: " + (cool > now ? (cool - now) / 20 + " s" : "none") + ". Table: " + table.id() + " (" + table.entries().size() + " entries). Live monsters near: " + liveNear(level, ps) + ".");
        PlayerState st = STATES.get(player.getUUID());
        if (st == null || st.rolledAt == Long.MIN_VALUE) out.add("Last roll: none yet.");
        else out.add("Last roll " + (now - st.rolledAt) / 20 + " s ago: " + st.note + " -> " + st.band.label() + (st.entry.isEmpty() ? "" : ", entry " + st.entry) + ".");
        return out;
    }

    /**
     * 1.94.0: a failure is no longer swallowed after the first one of any kind. Each DISTINCT failure (where it happened, its type and the line it was thrown from) is
     * logged once with its stack; after that only its count grows. The latest is kept for {@code why}. (A handler that threw on every tick used to leave one line
     * in the log and a dead Dungeon Master.)
     */
    private static void warn(String what, Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        String where = st.length > 0 ? st[0].getClassName().substring(st[0].getClassName().lastIndexOf('.') + 1) + "." + st[0].getMethodName() + ":" + st[0].getLineNumber() : "?";
        String key = what + "/" + t.getClass().getSimpleName() + "@" + where;
        Integer count = FAILURES.get(key);
        lastFailure = "in " + what + ": " + t + " at " + where + " (" + ((count == null ? 0 : count) + 1) + " time(s))";
        if (count == null) {
            if (FAILURES.size() < 32) FAILURES.put(key, 1);
            LOG.warn("[nusmp] The Dungeon Master failed in {} (this failure is logged once; /multiverse mega dm why shows the latest)", what, t);
        } else {
            FAILURES.put(key, count + 1);
        }
    }

    /** The latest failure of the Director, or an empty string. */
    static String lastFailure() { return lastFailure; }

    // ---------------------------------------------------------------- 1.94.0: why nothing happened
    /** The ticks the handler has seen with the Mega-Dungeon loaded, and the game time of the latest (Long.MIN_VALUE when never). */
    static long ticksSeen() { return ticksSeen; }

    static long lastTickAt() { return lastTickAt; }

    /**
     * One pass of the Director for a party, exactly as the tick makes it (no dice forced, no rule ignored), for a self-test or a command. {@code random} may be null.
     * The cooldown and the crowd cap are honoured, so a caller that wants a second encounter clears them with {@link #forgetPlayer}.
     */
    public static Outcome pass(ServerLevel level, List<? extends ServerPlayer> party, Random random) { return roll(level, party, null, random); }

    /**
     * The gates between a player and an encounter, in the order the Director checks them, each with the value it reads. Never throws. The first line is the verdict.
     * A {@code force} command or a self-test can read {@link EncGates#firstBlock} of the same list.
     */
    public static List<EncGates.Gate> gates(ServerLevel level, ServerPlayer player, List<? extends ServerPlayer> party) {
        List<EncGates.Gate> g = new ArrayList<>();
        try {
            gatesInner(level, player, party, g);
        } catch (Throwable t) {
            g.add(EncGates.block("diagnosis", "failed: " + t));
        }
        return g;
    }

    private static void gatesInner(ServerLevel level, ServerPlayer player, List<? extends ServerPlayer> party, List<EncGates.Gate> g) {
        long now = level.getGameTime();
        int every = EncConfig.get(EncConfig.ROLL_EVERY);
        g.add(EncConfig.get(EncConfig.ENABLED) ? EncGates.pass("switch enc.enabled", "true") : EncGates.block("switch enc.enabled", "false: the Dungeon Master is switched off in nusmp-labyrinth-mega-enc-server.toml"));
        if (ticksSeen == 0L) g.add(EncGates.block("tick handler", "it has never run in this session (is the Mega-Dungeon dimension loaded, EncRegistry.register called?)"));
        else if (now - lastTickAt > 40L) g.add(EncGates.block("tick handler", "last ran " + EncGates.duration(now - lastTickAt) + " ago (" + ticksSeen + " ticks seen)"));
        else g.add(EncGates.pass("tick handler", ticksSeen + " ticks seen, a pass for a party every " + every + " ticks (" + (every - (int) (level.getServer().getTickCount() % every)) + " ticks to the next), last pass "
                + (lastPassAt == Long.MIN_VALUE ? "never" : EncGates.duration(now - lastPassAt) + " ago")));
        g.add(Labyrinth.inMega(level) ? EncGates.pass("dimension", "the Mega-Dungeon") : EncGates.block("dimension", level.dimension().location() + " is not the Mega-Dungeon"));
        boolean creativeToo = EncConfig.get(EncConfig.ROLL_FOR_CREATIVE);
        String mode = player.isSpectator() ? "Spectator" : player.isCreative() ? "Creative" : "Survival/Adventure";
        if (!player.isAlive() || player.isSpectator()) g.add(EncGates.block("player", mode + (player.isAlive() ? "" : ", dead") + ": never rolled for"));
        else if (player.isCreative() && !creativeToo) g.add(EncGates.block("player", "Creative: the Dungeon Master does not roll for Creative players (set enc.rollForCreative = true to test as one)"));
        else g.add(EncGates.pass("player", mode + (player.isCreative() ? " (counted: enc.rollForCreative = true)" : "")));
        Sector sector = MegaDungeon.sectorAt(level, player.blockPosition());
        if (sector == null) {
            g.add(EncGates.block("sector", "you stand in no sector (the gap between two, or the hub): nothing is rolled here"));
            return;
        }
        g.add(sector.state == Sector.State.GENERATING || sector.state == Sector.State.FAILED || sector.rooms.isEmpty()
                ? EncGates.block("sector", "sector " + sector.id + " is " + sector.state + " with " + sector.rooms.size() + " rooms: not ready")
                : EncGates.pass("sector", "sector " + sector.id + " (" + sector.theme.id() + ", " + sector.polarity.id() + " road" + (sector.cyclic ? ", cyclic" : "") + ", " + sector.state + ", " + sector.rooms.size() + " rooms, "
                + sector.markers(com.newuniverse.nusmp.labyrinth.mega.MarkerKind.SPAWN).size() + " spawn markers)"));
        Room room = sector.roomAt(player.blockPosition());
        if (room == null) g.add(EncGates.warn("room", "you stand in no room box (a corridor gap?): treated as an ordinary place"));
        else g.add(Sanctuary.is(room.kind) ? EncGates.block("room", room.kind.name().toLowerCase(java.util.Locale.ROOT) + " " + room.id + " is a safe or boss room: the Dungeon Master leaves it alone; walk out of it")
                : EncGates.pass("room", room.kind.name().toLowerCase(java.util.Locale.ROOT) + " " + room.id + " (template " + room.template + ")"));
        g.add(level.getDifficulty() == Difficulty.PEACEFUL ? EncGates.block("difficulty", "Peaceful: monsters would vanish at once") : EncGates.pass("difficulty", level.getDifficulty().getKey()));
        EncounterTable table = EncData.table(sector.theme, sector.polarity);
        if (table.entries().isEmpty()) g.add(EncGates.block("table", "'" + table.id() + "' has no entries (loaded tables: " + EncData.tableNames() + "; a table that failed to parse is named in the log as [nusmp] <file>: <problem>)"));
        else if (!table.id().equals(sector.theme.id())) g.add(EncGates.warn("table", "no table for the theme '" + sector.theme.id() + "': this sector borrows '" + table.id() + "' (" + table.entries().size() + " entries; " + EncData.tableNames().size() + " tables loaded)"));
        else g.add(EncGates.pass("table", table.id() + ", " + table.entries().size() + " entries (" + EncData.tableNames().size() + " tables loaded)"));
        List<ServerPlayer> ps = new ArrayList<>(party.size());
        for (ServerPlayer p : party) ps.add(p);
        if (ps.isEmpty()) ps.add(player);
        PlayerState me = STATES.get(player.getUUID());
        long cool = 0L;
        for (ServerPlayer p : ps) {
            PlayerState s = STATES.get(p.getUUID());
            if (s != null) cool = Math.max(cool, s.cooldownUntil);
        }
        g.add(cool > now ? EncGates.block("cooldown", "the party is left alone for " + EncGates.duration(cool - now) + " more") : EncGates.pass("cooldown", "none"));
        int live = liveNear(level, ps);
        int max = EncConfig.get(EncConfig.MAX_LIVE);
        g.add(live >= max ? EncGates.block("crowd", live + " of his monsters live within 64 blocks (maxLiveMobsPerParty " + max + "); " + TRACKED.size() + " tracked in all; kill them or wait for the stuck sweep")
                : EncGates.pass("crowd", live + " live near, cap " + max));
        View v = view(level, sector, ps, table, now);
        g.add(EncGates.info("party", ps.size() + " player(s), factor " + String.format(java.util.Locale.ROOT, "%.2f", v.factor()) + ", floor " + (v.floor() == 0 ? "unknown" : FloorMap.label(v.floor())) + ", the dice read threat " + v.threat()));
        g.add(EncGates.info("noise and light", "noise " + String.format(java.util.Locale.ROOT, "%.2f", v.noise()) + (v.noisy() ? " (noisy)" : "") + ", light " + v.light().brightness() + (v.light().dark() ? " (dark)" : v.light().lit() ? " (lit)" : "")
                + "; noise events seen " + NoiseEvents.eventsSeen() + ", heard " + NoiseEvents.eventsHeard() + (NoiseEvents.eventsSeen() == 0L ? " (none yet: walk, open a door, break a block)" : "")));
        EncounterRoll.Mods m = v.mods();
        g.add(EncGates.info("modifiers", "threat " + m.threat() + ", noise " + m.noise() + ", light " + m.light() + ", party " + m.party() + ", table " + m.table() + ", tension " + m.tension() + " (" + v.quietPasses() + " quiet passes)"));
        g.add(EncGates.dice(m.withTension(0), v.dc(), every, EncConfig.get(EncConfig.TENSION_EVERY), EncConfig.get(EncConfig.TENSION_CAP)));
        // what the dice could pick from at this moment
        int[] perBand = new int[Band.values().length];
        for (EncounterTable.Entry e : table.entries()) {
            if (e.weight() > 0 && e.fitsThreat(v.threat()) && v.size() >= e.minParty() && e.needs().met(v.light().dark(), v.light().lit(), v.noisy()) && usable(e, table, v.threat())) perBand[e.band().ordinal()]++;
        }
        String usableText = "minor " + perBand[Band.MINOR.ordinal()] + ", standard " + perBand[Band.STANDARD.ordinal()] + ", hard " + perBand[Band.HARD.ordinal()] + ", ambush " + perBand[Band.AMBUSH.ordinal()];
        g.add(perBand[Band.MINOR.ordinal()] + perBand[Band.STANDARD.ordinal()] + perBand[Band.HARD.ordinal()] + perBand[Band.AMBUSH.ordinal()] == 0
                ? EncGates.block("entries", "no entry of '" + table.id() + "' fits this party now (" + usableText + "): every faction tag empty, or the needs do not match")
                : EncGates.pass("entries", "usable now: " + usableText));
        // would a place be found? the same search the spawn makes, with the config's ring and no entry override
        List<SpawnPicker.Eye> eyes = new ArrayList<>(ps.size());
        for (ServerPlayer p : ps) {
            Vec3 eye = p.getEyePosition();
            eyes.add(new SpawnPicker.Eye(eye.x, eye.y, eye.z));
        }
        double min = EncConfig.get(EncConfig.SPAWN_MIN), maxD = Math.max(min + 8.0, EncConfig.get(EncConfig.SPAWN_MAX));
        SpawnSearch.Found found = SpawnSearch.find(CellSource.layoutOf(sector), eyes, min, maxD, 2, new LevelSight(level, ps.get(0)), SIGHT_CHECKS_PER_EYE, EncConfig.get(EncConfig.SPAWN_WIDEN),
                EncConfig.get(EncConfig.RELAX_SIGHT_TIER) - 1, new Random(1234L));
        g.add(found.ok() ? (found.tier().index() == 0 ? EncGates.pass("place", String.join("; ", found.trace())) : EncGates.warn("place", "only found on rung '" + found.tier().name() + "': " + String.join("; ", found.trace())))
                : EncGates.block("place", "no cell on any rung: " + String.join("; ", found.trace())));
        if (me == null || me.rolledAt == Long.MIN_VALUE) g.add(EncGates.info("last roll", "none yet for you"));
        else g.add(EncGates.info("last roll", EncGates.duration(now - me.rolledAt) + " ago: " + me.note + " -> " + me.band.label() + (me.entry.isEmpty() ? "" : ", entry " + me.entry)
                + (me.pendingBand != null ? "; a rolled " + me.pendingBand.label() + " encounter is waiting for a place (" + me.retries + " retries)" : "")));
        if (!lastFailure.isEmpty()) g.add(EncGates.warn("last failure", lastFailure));
    }

    /** The report of {@link #gates} as lines of text, the verdict last. */
    public static List<String> why(ServerLevel level, ServerPlayer player, List<? extends ServerPlayer> party) {
        List<EncGates.Gate> gates = gates(level, player, party);
        List<String> out = new ArrayList<>(gates.size() + 1);
        for (EncGates.Gate gate : gates) out.add(gate.line());
        out.add(EncGates.verdict(gates));
        return out;
    }
}
