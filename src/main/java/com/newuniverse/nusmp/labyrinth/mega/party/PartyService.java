package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig;
import com.newuniverse.nusmp.labyrinth.mega.system.PartyRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is the one place the rest of the mod asks "which party" (the Hub's gates and the stairs through {@code MegaTravel}, the guard, the admin commands, the System's
 * kill credit), so the answer is the same everywhere. A player's party is decided when they ENTER the dungeon from the Hub ({@link #enter}: their team's key is looked up in
 * the saved book, a lane is handed out the first time, and the index is written into the player's persistent data), so teammates who go in together share floors and a
 * teammate who comes later joins the chain that exists; it is kept while the player is in the dungeon whatever happens to their team meanwhile, and decided afresh at the next
 * entry. Inside the dungeon the truth is the floor under the player's feet ({@link #partyOf}): the party of a sector is stored in the sector itself, so a login in the middle of
 * a floor, a restart or a lost record can never put somebody in the wrong party. Parties are instanced only in {@code party} mode and in the floor model
 * ({@link #instanced}); otherwise every answer is 0, the shared chain, which is exactly how the dungeon worked before 1.58.0. Server thread only; nothing here throws out of
 * an event handler.
 */
public final class PartyService {
    private static final Logger LOG = LogUtils.getLogger();

    /** The persistent key (an int) in a player's data: the party they went into the dungeon under. */
    public static final String KEY = "nusmp_mega_party";

    private static boolean warnedFull;

    private PartyService() {}

    // ================================================================================================================ the switch

    /** Are the parties separate chains right now? {@code party.mode = party} and the floor model ({@code floors.perSector = 1}). */
    public static boolean instanced() {
        boolean floorModel = FloorMap.isFloorModel(FloorMap.floorsPerSector(FloorsConfig.get(FloorsConfig.PER_SECTOR)));
        return PartyPolicy.instanced(PartyConfig.mode(), floorModel);
    }

    /** The most parties the lanes are handed out to. */
    public static int maxParties() { return PartyConfig.maxParties(); }

    // ================================================================================================================ the party of a player

    /** The party a player went into the dungeon under, or null when they have no record (never entered, or entered before parties existed). */
    public static Integer assigned(ServerPlayer p) {
        CompoundTag t = p.getPersistentData();
        return t.contains(KEY, Tag.TAG_INT) ? Integer.valueOf(t.getInt(KEY)) : null;
    }

    /** Writes the player's record. */
    static void store(ServerPlayer p, int party) { p.getPersistentData().putInt(KEY, party); }

    /** The index the book already has for the player's party, or -1 when it has none. Hands nothing out and saves nothing. */
    public static int peek(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return -1;
        return MegaParties.get(server).index().indexOf(PartyResolver.keyOf(p).id());
    }

    /**
     * The party a player is in now, for everything that is not an entry: 0 when parties are not instanced; inside the dungeon the party of the floor under their feet (so it
     * is true for a player nobody ever recorded), else their record; outside it the book's index for their party, or 0 for a party that has never been in (use
     * {@link #knownParty} to tell that apart from party 0).
     */
    public static int partyOf(ServerPlayer p) { return Math.max(0, knownParty(p)); }

    /**
     * Like {@link #partyOf}, but -1 when the player's party has no lane yet (it has never entered the dungeon): the answer for code that must not guess, such as an Avatar that
     * delves for its creator and must never be sent among the floors of a party that is not theirs. Always 0 when parties are not instanced. Hands nothing out.
     */
    public static int knownParty(ServerPlayer p) {
        if (!instanced()) return 0;
        if (MegaDungeon.in(p.level())) {
            Sector s = MegaDungeon.sectorAt(p.level(), p.blockPosition());
            if (s != null) return s.party;
            Integer a = assigned(p);
            if (a != null) return a;
        }
        return peek(p);
    }

    /**
     * Takes the player into the dungeon under their party: looks their party up in the book, hands it a lane the first time (a new team may take over the lane of the member who
     * played alone, see {@link PartyIndex}), and writes the index into the player's data. Returns the party index, 0 when parties are not instanced, and -1 when the player
     * cannot be placed (the book is full for {@code party.maxParties}, or something failed): the caller must not let them in, because the only other place to put them is
     * the lane of strangers.
     */
    public static int enter(ServerPlayer p) {
        try {
            if (!instanced()) {
                store(p, 0);
                return 0;
            }
            MinecraftServer server = p.getServer();
            if (server == null) return -1;
            PartyResolver.Resolved r = PartyResolver.resolve(p);
            MegaParties book = MegaParties.get(server);
            List<UUID> inherit = PartyConfig.get(PartyConfig.TEAM_INHERITS_LANE) ? r.candidates() : List.of();
            PartyIndex.Allocation a = book.allocate(r.key(), r.label(), maxParties(), reservedLanes(server, book), inherit);
            if (!a.ok()) {
                if (!warnedFull) {
                    warnedFull = true;
                    LOG.warn("[nusmp] The dungeon has no free lane for the party of {} ({} parties are allowed: party.maxParties); further refusals are not logged.", p.getGameProfile().getName(), maxParties());
                }
                return -1;
            }
            if (a.created()) {
                LOG.info("[nusmp] Party {} ({} {}) was given lane {}{}.", a.index(), r.key().team() ? "team" : "solo", r.label(), a.index(),
                        a.inherited() ? ", the floors that " + a.inheritedFrom() + " explored alone" : "");
            } else {
                book.relabel(r.key().id(), r.label());
            }
            store(p, a.index());
            return a.index();
        } catch (RuntimeException e) {
            LOG.error("[nusmp] The party of {} could not be decided: {}", p.getGameProfile().getName(), e.toString(), e);
            return -1;
        }
    }

    /**
     * The indices that floors already use although the book does not know them (a lost book, or a party an admin moved away): never handed out to a stranger. The shared lane
     * (0) is left out on purpose: it is the lane every world saved before parties has, and the first party to enter adopts it.
     */
    static Set<Integer> reservedLanes(MinecraftServer server, MegaParties book) {
        Set<Integer> reserved = new HashSet<>();
        for (int party : MegaData.get(server).parties()) if (party != 0 && !book.index().holds(party)) reserved.add(party);
        return reserved;
    }

    // ================================================================================================================ who plays together

    /**
     * The players who play together with {@code p}, for the System's shared kill credit ({@code SystemParty.setProvider}): {@code p} first, then every other player in the same
     * level of the same group. In {@code party} mode inside the dungeon the group is the party of the floors (two players whose parties an admin merged share credit, and
     * strangers can never be on the same floors); otherwise it is the team (FTB Teams), so a shared dungeon credits teammates and not whoever happens to be standing nearby.
     * The System itself still asks that a member is alive, in the same level and near the fallen creature.
     */
    public static List<ServerPlayer> questMembers(ServerPlayer p) {
        return PartyRules.sameGroup(p, p.serverLevel().players(), PartyService::groupOf);
    }

    static Object groupOf(ServerPlayer q) {
        if (instanced() && MegaDungeon.in(q.level())) return Integer.valueOf(partyOf(q));
        return PartyResolver.keyOf(q);
    }

    /** The players inside the floors of a party now. */
    public static List<ServerPlayer> onlineIn(MinecraftServer server, int party) {
        List<ServerPlayer> out = new ArrayList<>();
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) return out;
        for (ServerPlayer q : mega.players()) if (partyOf(q) == party) out.add(q);
        return out;
    }

    // ================================================================================================================ words for the player

    /**
     * The line a player reads on the way in, or null when the dungeon is shared: which party they walk with and how many of it are in the dungeon now (the player included: the
     * line follows the arrival). This is the prompt the Hub's gate gives (the gate teleports at once, so there is no moment to ask before).
     */
    public static Component entryNote(ServerPlayer p, int party) {
        if (!instanced()) return null;
        MinecraftServer server = p.getServer();
        PartyKey key = PartyResolver.keyOf(p);
        if (!key.team()) return PartyText.text("entry.solo", "You walk the floors of your own: a party of one. Join an FTB team to share them with friends.");
        PartyIndex.Entry e = server == null ? null : MegaParties.get(server).index().get(key.id());
        String name = e == null ? "?" : PartyLines.clean(e.label());
        int inside = server == null ? 0 : onlineIn(server, party).size();
        return PartyText.text("entry.team", "Your party %s has floors of its own. %s of you are in the dungeon now.", name, inside);
    }

    // ================================================================================================================ the admin's hand

    /** The outcome of {@link #set}. */
    public record SetResult(boolean ok, String why, PartyKey key, int before, int index, int moved) {
        static SetResult refused(String why) { return new SetResult(false, why, null, -1, -1, 0); }
    }

    /**
     * Puts the party of a player (their team, if they are in one) on a lane by hand: for merging two groups, for moving a party, for tests. The player's team is asked by id,
     * so an offline player works. Every online member then gets the new record, and a member who stands on the floors of another party is sent out (the guard would do it
     * within a second anyway; doing it here lets the admin hear about it). Refuses an index the config does not allow.
     */
    public static SetResult set(MinecraftServer server, UUID playerId, String playerName, int index) {
        if (server == null) return SetResult.refused("no server");
        if (index < 0 || index >= maxParties()) return SetResult.refused("party " + index + " is outside 0.." + (maxParties() - 1) + " (party.maxParties)");
        PartyKey key = PartyResolver.keyOf(server, playerId);
        MegaParties book = MegaParties.get(server);
        int before = book.assign(key, key.team() ? "" : playerName, index);
        int moved = 0;
        for (ServerPlayer q : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (!PartyResolver.keyOf(q).equals(key)) continue;
            store(q, index);
            if (PartyGuard.check(q)) moved++;
        }
        LOG.info("[nusmp] An admin put party {} on lane {} (it was {}).", key, index, before < 0 ? "new" : Integer.toString(before));
        return new SetResult(true, null, key, before, index, moved);
    }

    /**
     * For self-tests of the travel code (a stand-in player is a party of its own, and a fresh party has no floors): puts the player's party on lane {@code party} until
     * {@link #unpin} is called, so the stand-in walks into the chain the test is about. Returns the key to hand back to {@link #unpin}. Does nothing that lasts.
     */
    public static PartyKey pin(MinecraftServer server, ServerPlayer p, int party) {
        PartyKey key = PartyResolver.keyOf(p);
        MegaParties.get(server).assign(key, "self-test", Math.max(0, Math.min(MegaGrid.MAX_PARTIES - 1, party)));
        return key;
    }

    /** Takes a party that {@link #pin} put in the book out again. */
    public static void unpin(MinecraftServer server, PartyKey key) {
        if (key != null) MegaParties.get(server).forget(key.id());
    }

    /** One line of the admin's list. */
    public record Row(int index, String kind, String key, String name, int holy, int abyssal, int online) {}

    /** Every party of the book, then every lane that has floors but no party, lowest index first. */
    public static List<Row> rows(MinecraftServer server) {
        MegaParties book = MegaParties.get(server);
        MegaData data = MegaData.get(server);
        List<Row> out = new ArrayList<>();
        Set<Integer> shown = new HashSet<>();
        for (PartyIndex.Entry e : book.index().entries()) {
            out.add(new Row(e.index(), e.team() ? "team" : "solo", e.key().toString(), e.label(), data.chain(e.index(), Polarity.HOLY).size(), data.chain(e.index(), Polarity.ABYSSAL).size(), onlineIn(server, e.index()).size()));
            shown.add(e.index());
        }
        for (int party : data.parties()) {
            if (shown.contains(party)) continue;
            out.add(new Row(party, "unowned", "none", "-", data.chain(party, Polarity.HOLY).size(), data.chain(party, Polarity.ABYSSAL).size(), onlineIn(server, party).size()));
        }
        out.sort((a, b) -> Integer.compare(a.index(), b.index()));
        return out;
    }

    /** Forgets what was said about a full book (a stopped server). */
    public static void onServerStopped() {
        warnedFull = false;
        PartyResolver.clear();
    }
}
