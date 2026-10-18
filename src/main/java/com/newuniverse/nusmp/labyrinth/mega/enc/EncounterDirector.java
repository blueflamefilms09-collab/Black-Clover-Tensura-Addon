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
    private static final int SWEEP_EVERY = 100, HUNT_EVERY = 20, MAX_TRACKED = 400, HUNT_PER_PASS = 40, SIGHT_CHECKS = 24;
    private static final double AIM_RANGE = 22.0, HUNT_REACH = 96.0, LIVE_RADIUS = 64.0, SEPARATION = 4.0;

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
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static final ArrayDeque<PartyMath.Party> QUEUE = new ArrayDeque<>();
    /** Every live monster of the Director: its last known block. */
    private static final Map<UUID, Long> TRACKED = new LinkedHashMap<>();
    /** Monsters sent to find the party: the game time their hunt ends. */
    private static final Map<UUID, Long> HUNTS = new LinkedHashMap<>();
    private static int huntCursor;
    private static boolean warned;

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
        int tick = server.getTickCount();
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            if (!QUEUE.isEmpty()) QUEUE.clear();
            if (tick % SWEEP_EVERY == 37 && !TRACKED.isEmpty()) sweep(level, players);
            return;
        }
        if (tick % EncConfig.get(EncConfig.ROLL_EVERY) == 0 && QUEUE.isEmpty()) fill(players);
        PartyMath.Party next = QUEUE.poll();
        if (next != null) {
            List<ServerPlayer> members = new ArrayList<>(next.size());
            for (PartyMath.Member m : next.members()) if (level.getPlayerByUUID(m.id()) instanceof ServerPlayer sp) members.add(sp);
            roll(level, members, null);
        }
        if (tick % SWEEP_EVERY == 37) sweep(level, players);
        if (tick % HUNT_EVERY == 11) hunt(level, players, level.getGameTime());
    }

    private static void fill(List<ServerPlayer> players) {
        List<PartyMath.Member> members = new ArrayList<>();
        for (ServerPlayer p : players) if (p.isAlive() && !p.isCreative() && !p.isSpectator()) members.add(new PartyMath.Member(p.getUUID(), p.getX(), p.getY(), p.getZ(), PartyService.partyOf(p)));
        if (!members.isEmpty()) QUEUE.addAll(PartyMath.group(members, EncConfig.get(EncConfig.PARTY_RADIUS)));
    }

    // ---------------------------------------------------------------- the roll
    /**
     * Rolls for one party. {@code party} are the players (the tick passes one group of {@link PartyMath}; a command or a self-test passes whoever it
     * means). With {@code force == null} every rule applies and the dice decide. Never throws; a failure is an {@link Outcome} that says why.
     */
    public static Outcome roll(ServerLevel level, List<? extends ServerPlayer> party, Force force) {
        try {
            return rollInner(level, party, force);
        } catch (Throwable t) {
            warn("roll", t);
            return Outcome.of(Kind.NO_PLACE, "the roll failed: " + t);
        }
    }

    private static Outcome rollInner(ServerLevel level, List<? extends ServerPlayer> party, Force force) {
        if (!Labyrinth.inMega(level)) return Outcome.of(Kind.OUTSIDE, "this is not the Mega-Dungeon");
        boolean admin = force != null && force.ignoreRules();
        List<ServerPlayer> ps = new ArrayList<>();
        for (ServerPlayer p : party) if (p.isAlive() && (force != null || (!p.isCreative() && !p.isSpectator()))) ps.add(p);
        if (ps.isEmpty()) return Outcome.of(Kind.NO_PARTY, "there is nobody to roll for");
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
        double[] centre = centre(exposed);
        if (!admin) {
            long cool = 0L;
            for (ServerPlayer p : ps) cool = Math.max(cool, state(p.getUUID()).cooldownUntil);
            if (cool > now) return Outcome.of(Kind.COOLDOWN, "the party is left alone for " + (cool - now) / 20 + " more seconds");
            int live = liveNear(level, centre[0], centre[1], centre[2]);
            if (live >= EncConfig.get(EncConfig.MAX_LIVE)) return Outcome.of(Kind.CROWDED, live + " of his monsters live near the party already");
        }

        // the dice
        View v = view(level, sector, exposed, table, now);
        int threat = v.threat();
        Random rnd = new Random(level.getRandom().nextLong());
        EncounterRoll.Result res;
        if (force != null && force.band() != null) res = new EncounterRoll.Result(0, v.mods(), 0, v.dc(), 0, force.band(), false, false);
        else if (force != null && force.entryId() != null) res = null;
        else res = EncounterRoll.resolve(EncounterRoll.d20(rnd), v.mods(), v.dc());
        for (ServerPlayer p : exposed) {
            PlayerState st = state(p.getUUID());
            st.rolledAt = now;
            st.natural = res == null ? 0 : res.natural();
            st.total = res == null ? 0 : res.total();
            st.band = res == null ? Band.NOTHING : res.band();
            st.quiet = res != null && res.quiet();
            st.entry = "";
            st.note = res == null ? "forced entry" : describe(res, v);
        }
        if (res != null && res.band() == Band.NOTHING) return new Outcome(Kind.QUIET, "rolled " + describe(res, v) + ": quiet", List.of(), 0.0, 0.0, res, "", Band.NOTHING, List.of(), 0.0, 0.0);

        // the entry
        EncounterTable.Entry entry;
        Band band;
        if (force != null && force.entryId() != null) {
            entry = table.byId(force.entryId());
            if (entry == null) return Outcome.of(Kind.NO_ENTRY, "the table of " + sector.theme.id() + " has no entry '" + force.entryId() + "'");
            band = entry.band();
        } else {
            EncounterRoll.Choice choice = EncounterRoll.choose(table.entries(), res.band(), threat, v.size(), v.light().dark(), v.light().lit(), v.noisy(), e -> usable(e, table, threat), rnd);
            if (choice == null) return new Outcome(Kind.NO_ENTRY, "rolled " + describe(res, v) + " (" + res.band().label() + ") but the table has no usable entry", List.of(), 0.0, 0.0, res, "", res.band(), List.of(), 0.0, 0.0);
            entry = choice.entry();
            band = choice.band();
        }
        Outcome out = spawnEntry(level, sector, table, exposed, entry, band, v, rnd, now, res);
        if (out.spawned()) {
            for (ServerPlayer p : exposed) state(p.getUUID()).entry = entry.id();
            long cool = EncounterRoll.cooldown(EncConfig.get(EncConfig.BASE_COOLDOWN), EncConfig.get(EncConfig.MIN_COOLDOWN), v.noise());
            for (ServerPlayer p : ps) state(p.getUUID()).cooldownUntil = now + cool;
        }
        return out;
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
        double[] centre = centre(exposed);
        LevelSight sight = new LevelSight(level, exposed.get(0));
        List<SpawnPicker.Cell> candidates = CellSource.candidates(sector, centre[0], centre[2], max + 16.0, rnd);
        List<SpawnPicker.Cell> cells = SpawnPicker.pick(candidates, eyes, new SpawnPicker.Params(min, max, SEPARATION, SIGHT_CHECKS, Math.max(1, entry.cells())), sight, rnd);
        if (cells.isEmpty()) return new Outcome(Kind.NO_PLACE, "entry '" + entry.id() + "': no place " + (int) min + "-" + (int) max + " blocks away out of sight (" + candidates.size() + " cells looked at)", List.of(), budget, 0.0, res, entry.id(), band, List.of(), min, max);

        // which monsters: explicit ids, or members of a faction (the entry's, else the native one of the room, else the table's)
        String nativeFaction = nativeFaction(sector, cells.get(0));
        List<EncounterRoll.Pick> picks = new ArrayList<>();
        Map<String, String> factionOf = new HashMap<>();
        Map<String, Double> costOf = new HashMap<>();
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
            EncounterRoll.Pick fp = FoundryEncounters.pick(sector.polarity, sector.theme.id(), v.floor(), spec.kind() == EncounterTable.Kind.FACTION ? factionOf.getOrDefault(pick == null ? "" : pick.entity(), nativeFaction) : null, rnd);   // Mob Foundry: sometimes a foundry mob of this floor's tier and road instead
            if (fp != null) { pick = fp; factionOf.put(fp.entity(), FoundryEncounters.factionOf(fp.entity())); }
            picks.add(pick);
            if (pick != null) costOf.put(pick.entity(), pick.cost());
        }
        List<EncounterRoll.Planned> plan = EncounterRoll.plan(entry, picks, v.factor(), budget, EncConfig.get(EncConfig.MAX_PER_ENCOUNTER), rnd);
        if (plan.isEmpty()) return new Outcome(Kind.TOO_SMALL, "entry '" + entry.id() + "': nothing of it fits a budget of " + String.format(java.util.Locale.ROOT, "%.1f", budget), List.of(), budget, 0.0, res, entry.id(), band, cells, min, max);

        // the monsters
        int[] attempts = {EncConfig.get(EncConfig.SPAWN_ATTEMPTS)};
        EncounterSpawner.Rules rules = new EncounterSpawner.Rules(eyes, min, max, sight, attempts);
        List<Mob> made = new ArrayList<>();
        double spent = 0.0;
        int at = 0;
        spawning:
        for (EncounterRoll.Planned pl : plan) {
            EntityType<?> type = Bestiary.type(FoundryEncounters.baseId(pl.entity()));
            if (type == null) continue;
            for (int i = 0; i < pl.count(); i++) {
                SpawnPicker.Cell cell = cells.get(at++ % cells.size());
                FoundryEncounters.expect(pl.entity(), v.floor());
                Mob m = EncounterSpawner.spawn(level, type, new BlockPos(cell.x(), cell.y(), cell.z()), rules, threat, factionOf.getOrDefault(pl.entity(), entry.faction()), entry.id(), rnd);
                if (m != null) {
                    made.add(m);
                    spent += costOf.getOrDefault(pl.entity(), 1.0);
                }
                if (attempts[0] <= 0) break spawning;
            }
        }
        if (made.isEmpty()) return new Outcome(Kind.NO_PLACE, "entry '" + entry.id() + "': no position took a monster (" + (EncConfig.get(EncConfig.SPAWN_ATTEMPTS) - attempts[0]) + " tried)", List.of(), budget, 0.0, res, entry.id(), band, cells, min, max);

        long hunt = entry.hunt() >= 0 ? entry.hunt() : band.huntTicks();
        for (Mob m : made) {
            if (TRACKED.size() < MAX_TRACKED) TRACKED.put(m.getUUID(), m.blockPosition().asLong());
            if (hunt > 0 && HUNTS.size() < MAX_TRACKED) HUNTS.put(m.getUUID(), now + hunt);
        }
        if (EncConfig.get(EncConfig.CUES)) EncounterCue.play(level, exposed, cells.get(0), entry, band, rnd);
        if (!entry.message().isEmpty()) for (ServerPlayer p : exposed) EncounterCue.tell(p, entry.message());
        String text = "'" + entry.id() + "' (" + band.label() + "): " + made.size() + " monsters, " + String.format(java.util.Locale.ROOT, "%.1f of %.1f", spent, budget) + " budget, " + cells.size() + " places"
                + (res == null ? "" : ", rolled " + describe(res, v));
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
    record View(int size, int threat, int floor, double factor, double noise, LightRules.Light light, boolean noisy, EncounterRoll.Mods mods, int dc) {}

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
        EncounterRoll.Mods mods = EncounterRoll.mods(threat, noise, light.mod(), n, table.rollBonus(), EncounterRoll.Caps.DEFAULT);
        return new View(n, threat, floor, factor, noise, light, noise >= EncConfig.get(EncConfig.NOISY_AT), mods, EncConfig.get(EncConfig.DC));
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
        return "d20 " + r.natural() + " + " + m.total() + " (threat " + m.threat() + ", noise " + m.noise() + ", light " + m.light() + ", party " + m.party() + ", table " + m.table() + ") = " + r.total() + " vs DC " + v.dc()
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
        for (UUID id : TRACKED.keySet()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive() && e.distanceToSqr(x, y, z) <= r2) n++;
        }
        return n;
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
            if (m.getTarget() instanceof Player t && t.isAlive() && !t.isSpectator()) continue;       // it found someone by itself
            ServerPlayer prey = null;
            double best = HUNT_REACH * HUNT_REACH;
            for (ServerPlayer p : players) {
                if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
                double d = m.distanceToSqr(p);
                if (d >= best) continue;
                if (MegaDungeon.inSafeZone(level, p.blockPosition())) continue;
                best = d;
                prey = p;
            }
            if (prey == null) continue;
            if (best <= AIM_RANGE * AIM_RANGE) MobSteer.aim(m, prey);
            MobSteer.walk(m, prey.getX(), prey.getY(), prey.getZ(), 1.0);
        }
        huntCursor = (huntCursor + HUNT_PER_PASS) & 0x7fffffff;
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
            double near = Double.MAX_VALUE;
            for (ServerPlayer p : players) if (!p.isSpectator()) near = Math.min(near, e.distanceToSqr(p));
            boolean remove = near > far2;
            if (!remove && e instanceof Mob m && near > 24.0 * 24.0 && m.getTarget() == null) {
                long born = m.getPersistentData().getLong(EncounterSpawner.BORN);
                remove = born > 0L && now - born > maxAge;
            }
            if (remove) {
                e.discard();
                it.remove();
                HUNTS.remove(t.getKey());
            }
        }
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
        out.add("Modifiers: threat " + m.threat() + ", noise " + m.noise() + ", light " + m.light() + ", party " + m.party() + ", table " + m.table() + " = " + m.total() + " against DC " + v.dc() + ".");
        out.add("Cooldown: " + (cool > now ? (cool - now) / 20 + " s" : "none") + ". Table: " + table.id() + " (" + table.entries().size() + " entries). Live monsters near: " + liveNear(level, player.getX(), player.getY(), player.getZ()) + ".");
        PlayerState st = STATES.get(player.getUUID());
        if (st == null || st.rolledAt == Long.MIN_VALUE) out.add("Last roll: none yet.");
        else out.add("Last roll " + (now - st.rolledAt) / 20 + " s ago: " + st.note + " -> " + st.band.label() + (st.entry.isEmpty() ? "" : ", entry " + st.entry) + ".");
        return out;
    }

    private static void warn(String what, Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The Dungeon Master failed in {} (further failures are not logged)", what, t);
    }
}
