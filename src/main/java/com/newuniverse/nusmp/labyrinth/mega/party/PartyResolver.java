package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.quests.QuestTeams;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: "which party is this player in" is a question for the TEAM layer, and the team layer here is FTB Teams, which this mod must never depend on (it builds, loads and
 * runs without it). So the answer comes from the soft bridge the mod already has ({@code QuestTeams}, FTB Teams' public API reached by reflection and tolerant of the API
 * being absent, of its manager not being loaded yet, and of any call failing: every failure is "no team", never an exception), and this class only turns a team into a
 * {@link PartyKey} ({@link PartyKey#of}: the team's id, or the player's own for a player without a team or with only FTB's personal team). The answer is cached for the
 * server tick, because the guard, the System's kill credit and the gates ask about the same players many times a tick and the answer cannot change inside one. The
 * {@link Source} seam is where a different team layer, or a self-test that needs two players in one party, plugs in; the default asks FTB Teams unless
 * {@code party.useFtbTeams} is off. The label and the candidates for a new team's inherited lane are asked of the same bridge, only when a team is NEW. Server thread only.
 */
public final class PartyResolver {
    private static final Logger LOG = LogUtils.getLogger();

    private PartyResolver() {}

    /** Where the party of a player comes from. */
    @FunctionalInterface
    public interface Source {
        /** The key of the player's party, or null for "no opinion" (then the player is a party of one). Must not throw. */
        PartyKey keyOf(ServerPlayer player);
    }

    /** Everything the book needs to know about a party the first time it is entered: the key, the name to show, and the players whose own lane a NEW team may take over, most deserving first. */
    public record Resolved(PartyKey key, String label, List<UUID> candidates) {}

    private static final Source FTB = PartyResolver::fromFtb;
    private static volatile Source source = FTB;
    private static long cacheTick = Long.MIN_VALUE;
    private static final Map<UUID, PartyKey> CACHE = new HashMap<>();
    private static boolean warned;

    /** Replaces where parties come from (a self-test, or another team layer); null restores FTB Teams. */
    public static void setSource(Source s) {
        source = s == null ? FTB : s;
        clear();
    }

    /** Forgets the cached answers (a different source, a stopped server). */
    public static void clear() {
        CACHE.clear();
        cacheTick = Long.MIN_VALUE;
    }

    /** The key of a player's party now: their team's id, else their own id. Cached for the tick. Never throws. */
    public static PartyKey keyOf(ServerPlayer p) {
        long tick = p.getServer() == null ? Long.MIN_VALUE : p.getServer().getTickCount();
        if (tick != cacheTick) {
            CACHE.clear();
            cacheTick = tick;
        }
        PartyKey cached = tick == Long.MIN_VALUE ? null : CACHE.get(p.getUUID());
        if (cached != null) return cached;
        PartyKey k = ask(p);
        if (tick != Long.MIN_VALUE) CACHE.put(p.getUUID(), k);
        return k;
    }

    private static PartyKey ask(ServerPlayer p) {
        try {
            PartyKey k = source.keyOf(p);
            return k == null ? PartyKey.solo(p.getUUID()) : k;
        } catch (RuntimeException | LinkageError e) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The party of {} could not be read (further failures are not logged); the player is a party of one: {}", p.getGameProfile().getName(), e.toString());
            }
            return PartyKey.solo(p.getUUID());
        }
    }

    /** The default source: the FTB team of the player, or nothing when the switch is off, FTB Teams is absent or does not answer. */
    private static PartyKey fromFtb(ServerPlayer p) {
        if (!PartyConfig.get(PartyConfig.USE_FTB_TEAMS)) return null;
        Optional<Object> team = QuestTeams.teamOf(p);
        if (team.isEmpty()) return null;
        return PartyKey.of(QuestTeams.idOf(team.get()), p.getUUID());
    }

    /**
     * What the book needs for a party's first entry: the key, a name, and (for a team only) the players whose own lane a new team may take over: the owner first, then the
     * other members in id order, so the choice never depends on who happened to be online or on the order a set iterates in.
     */
    public static Resolved resolve(ServerPlayer p) {
        PartyKey key = keyOf(p);
        if (!key.team()) return new Resolved(key, p.getGameProfile().getName(), List.of());
        Optional<Object> team = PartyConfig.get(PartyConfig.USE_FTB_TEAMS) ? QuestTeams.teamOf(p) : Optional.empty();
        if (team.isEmpty() || !key.id().equals(QuestTeams.idOf(team.get()))) return new Resolved(key, "team " + key.id().toString().substring(0, 8), List.of());
        String label = PartyPolicy.displayName(QuestTeams.nameOf(team.get()));
        List<UUID> candidates = new ArrayList<>();
        UUID owner = QuestTeams.ownerOf(team.get());
        if (owner != null && (owner.getMostSignificantBits() != 0L || owner.getLeastSignificantBits() != 0L)) candidates.add(owner);      // FTB Teams answers the nil id for a team nobody owns (the server console's)
        else owner = null;
        List<UUID> others = new ArrayList<>(QuestTeams.memberIds(team.get()));
        others.sort(UUID::compareTo);
        for (UUID m : others) if (!m.equals(owner)) candidates.add(m);
        return new Resolved(key, label, candidates);
    }

    /**
     * The key of a player who may be OFFLINE (the admin's commands ask about names): an online player is asked as usual; otherwise FTB Teams is asked by id, and a player it
     * does not know is a party of one.
     */
    public static PartyKey keyOf(MinecraftServer server, UUID playerId) {
        ServerPlayer on = server == null ? null : server.getPlayerList().getPlayer(playerId);
        if (on != null) return keyOf(on);
        if (!PartyConfig.get(PartyConfig.USE_FTB_TEAMS)) return PartyKey.solo(playerId);
        Optional<Object> team = QuestTeams.teamOfId(playerId);
        return team.isEmpty() ? PartyKey.solo(playerId) : PartyKey.of(QuestTeams.idOf(team.get()), playerId);
    }
}
