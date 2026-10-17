package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.system.PartyRules;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: nothing the party layer does can be watched by its author, so the CI smoke test runs these five checks in the real game ({@code /multiverse mega selftest party.*})
 * after it has built a first floor for a second party with {@code /multiverse mega expand holy party 1}. {@code party.lanes}: every floor that exists stands on the lane of
 * its own party and road, the lanes of all parties are different and apart, and the fixed number the documentation quotes for party 0 is still the old one.
 * {@code party.book}: the saved book of the parties survives being written and read back, with teams, solo parties and names. {@code party.resolve}: two stand-in
 * players of two parties are given two different lanes, two of one team the same lane, asking again changes nothing, and the System's group of a player is their party.
 * {@code party.guard}: the guard lets a player stand on their own party's floors, adopts the floor of a player with no record, and judges a player on another party's floor out.
 * {@code party.chains}: every chain is whole and never leaks into another party (a floor's successor is a floor of the same party and road), and the Hub's gate, driven by a
 * stand-in player, puts each tested party on the pad of ITS OWN first floor and on nobody else's. The checks that need a built floor say so in their answer when there is none,
 * instead of passing silently. They mutate nothing that lasts: the stand-ins are put in places no floor occupies, the book entries they make are forgotten again, the resolver
 * is put back, and the chains are only read.
 */
final class PartySelfTests {
    private PartySelfTests() {}

    static void register() {
        SelfTests.register("party.lanes", PartySelfTests::lanes);
        SelfTests.register("party.book", PartySelfTests::book);
        SelfTests.register("party.resolve", PartySelfTests::resolve);
        SelfTests.register("party.guard", PartySelfTests::guard);
        SelfTests.register("party.chains", PartySelfTests::chains);
    }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    /** Where the stand-ins stand: in the empty middle between the two lane groups, in no sector. */
    private static final double NOWHERE_Z = 150_000.5;

    private static FakePlayer standIn(ServerLevel level, String name) {
        FakePlayer p = SelfTests.player(level, "party_test_" + name, 0.5, 70.0, NOWHERE_Z);
        p.getPersistentData().remove(PartyService.KEY);
        return p;
    }

    private static void clean(FakePlayer p) {
        p.getPersistentData().remove(PartyService.KEY);
        p.getPersistentData().remove(MegaTravel.LANDING_KEY);
        p.getPersistentData().remove(MegaTravel.LANDING_HOLY);
        p.getPersistentData().remove(MegaTravel.LANDING_ABYSSAL);
        MegaTravel.forget(p.getUUID());
        LabyrinthTravel.forget(p);
    }

    // ================================================================================================================ party.lanes

    private static SelfTests.Result lanes(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        if (MegaGrid.laneZ(Polarity.HOLY, 0) != 0 || MegaGrid.laneZ(Polarity.ABYSSAL, 0) != 300_000) return fail("party 0 no longer stands on the old lanes (Holy z 0, Abyssal z 300000)");
        Set<Integer> seen = new HashSet<>();
        for (int party = 0; party < MegaGrid.MAX_PARTIES; party++) {
            for (Polarity road : Polarity.values()) {
                int z = MegaGrid.laneZ(road, party);
                if (!seen.add(z)) return fail("the lane of party " + party + " on the " + road.id() + " road (z " + z + ") is the lane of another party");
                if (PartyLanes.partyAt(z, MegaGrid.MAX_PARTIES) != party || PartyLanes.roadAt(z) != road) return fail("the lane z " + z + " does not read back as party " + party + " on the " + road.id() + " road");
            }
        }
        MegaData data = MegaData.get(server);
        int floors = 0, banded = 0;
        for (Sector s : data.sectors()) {
            if (!s.isFloor()) {
                banded++;                                                          // the ten-floor sectors of the legacy model wind through the dimension; only the floor model is held to its lane
                if (s.party != 0) return fail("sector " + s.id + " is of the legacy ten-floor model but belongs to party " + s.party + ": parties are not instanced in that model");
                if (PartyService.instanced() && !PartyLanes.insideBand(s.region.minZ(), s.region.maxZ(), 0)) {
                    return fail("sector " + s.id + " (legacy ten-floor model, shared chain) winds through z " + s.region.minZ() + ".." + s.region.maxZ() + ", farther than half a lane step from its lane: it could meet the floors of another party; set party.mode to shared");
                }
                continue;
            }
            int lane = MegaGrid.laneZ(s.polarity, s.party);
            if (s.region.minZ() > lane || s.region.maxZ() < lane) return fail("sector " + s.id + " (party " + s.party + ", " + s.polarity.id() + " road) has its region at z " + s.region.minZ() + ".." + s.region.maxZ() + ", which does not hold its lane z " + lane);
            if (Math.abs(s.region.minZ() - lane) > PartyLanes.REACH || Math.abs(s.region.maxZ() - lane) > PartyLanes.REACH) return fail("sector " + s.id + " reaches farther from its lane z " + lane + " than a floor may (" + PartyLanes.REACH + ")");
            if (s.entry.getZ() != lane) return fail("sector " + s.id + " has its door at z " + s.entry.getZ() + " but its party's lane is z " + lane);
            floors++;
        }
        int far = PartyLanes.maxZ(PartyService.maxParties()), near = PartyLanes.minZ(PartyService.maxParties());
        boolean border = level.getWorldBorder().isWithinBounds(new BlockPos(0, 64, far)) && level.getWorldBorder().isWithinBounds(new BlockPos(0, 64, near));
        return ok(MegaGrid.MAX_PARTIES + " parties x 2 roads have 512 different lanes, party 0 is where it always was, " + floors + " floor(s) stand on their own lane" + (banded > 0 ? ", " + banded + " legacy sector(s) are party 0" : "")
                + "; the lanes of the " + PartyService.maxParties() + " allowed parties run from z " + near + " to z " + far + (border ? " (inside the world border)" : " (OUTSIDE the world border of this dimension)"));
    }

    // ================================================================================================================ party.book

    private static SelfTests.Result book(MinecraftServer server) {
        MegaParties live = MegaParties.get(server);
        // a scratch book through the real file format
        MegaParties scratch = new MegaParties();
        UUID a = UUID.nameUUIDFromBytes("party-book-a".getBytes(java.nio.charset.StandardCharsets.UTF_8)), b = UUID.nameUUIDFromBytes("party-book-b".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        scratch.index().assign(PartyKey.solo(a), "Solo Sam", 3);
        scratch.index().assign(PartyKey.team(b), "The Delvers", 7);
        CompoundTag saved = scratch.save(new CompoundTag(), server.registryAccess());
        MegaParties read = MegaParties.load(saved, server.registryAccess());
        if (read.index().size() != 2) return fail("the saved book has 2 parties but " + read.index().size() + " were read back");
        PartyIndex.Entry ea = read.index().get(a), eb = read.index().get(b);
        if (ea == null || ea.index() != 3 || ea.team() || !"Solo Sam".equals(ea.label())) return fail("the solo party was read back as " + ea);
        if (eb == null || eb.index() != 7 || !eb.team() || !"The Delvers".equals(eb.label())) return fail("the team was read back as " + eb);
        // a damaged entry is dropped and the rest is kept
        CompoundTag damaged = saved.copy();
        net.minecraft.nbt.ListTag list = damaged.getList("parties", net.minecraft.nbt.Tag.TAG_COMPOUND);
        CompoundTag bad = new CompoundTag();
        bad.putString("key", "t:not-a-uuid");
        bad.putInt("index", 9);
        list.add(bad);
        MegaParties tolerant = MegaParties.load(damaged, server.registryAccess());
        if (tolerant.index().size() != 2) return fail("a damaged entry changed the size of the book to " + tolerant.index().size());
        return ok("the book of parties survives a save and a load (a team and a solo party with their lanes and names), a damaged entry is dropped; the live book has " + live.index().size() + " part" + (live.index().size() == 1 ? "y" : "ies"));
    }

    // ================================================================================================================ party.resolve

    private static SelfTests.Result resolve(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        if (!PartyService.instanced()) return ok("the dungeon is not instanced here (party.mode = " + PartyConfig.mode().id() + " or the legacy ten-floor model): everybody is party 0, nothing to separate");
        FakePlayer a = standIn(level, "a"), b = standIn(level, "b"), c = standIn(level, "c"), d = standIn(level, "d");
        UUID team = UUID.nameUUIDFromBytes("party-test-team".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<UUID, PartyKey> keys = new HashMap<>();
        keys.put(a.getUUID(), PartyKey.solo(a.getUUID()));
        keys.put(b.getUUID(), PartyKey.solo(b.getUUID()));
        keys.put(c.getUUID(), PartyKey.team(team));
        keys.put(d.getUUID(), PartyKey.team(team));
        MegaParties live = MegaParties.get(server);
        PartyResolver.setSource(p -> keys.get(p.getUUID()));
        try {
            int ia = PartyService.enter(a), ib = PartyService.enter(b), ic = PartyService.enter(c), id = PartyService.enter(d);
            if (ia < 0 || ib < 0 || ic < 0 || id < 0) return fail("a stand-in could not be given a lane (a=" + ia + " b=" + ib + " c=" + ic + " d=" + id + "): the book is full for party.maxParties=" + PartyService.maxParties());
            if (ia == ib) return fail("two players on their own were given the same lane " + ia);
            if (ic == ia || ic == ib) return fail("a team was given the lane " + ic + " of a player on their own");
            if (ic != id) return fail("two members of one team were given the lanes " + ic + " and " + id + " instead of one");
            if (PartyService.enter(a) != ia || PartyService.enter(c) != ic) return fail("asking again changed a lane");
            Integer ra = PartyService.assigned(a), rc = PartyService.assigned(d);
            if (ra == null || ra != ia || rc == null || rc != ic) return fail("the party was not written into the player's data (a: " + ra + " expected " + ia + ", d: " + rc + " expected " + ic + ")");
            if (MegaGrid.laneZ(Polarity.HOLY, ia) == MegaGrid.laneZ(Polarity.HOLY, ib)) return fail("two parties' lanes are the same z");
            List<ServerPlayer> everybody = List.of(a, b, c, d);
            List<ServerPlayer> withC = PartyRules.sameGroup(c, everybody, PartyService::groupOf);
            List<ServerPlayer> withA = PartyRules.sameGroup(a, everybody, PartyService::groupOf);
            if (withC.size() != 2 || withC.get(0) != c || !withC.contains(d)) return fail("the System's group of a team member should be the player and the other member, but is " + withC.size() + " player(s)");
            if (withA.size() != 1) return fail("the System's group of a player on their own should be just the player, but is " + withA.size());
            // the admin's hand: put the first player's party on another lane (by id, as for an offline player), and refuse a lane the config does not allow
            int target = (ia + 2) % PartyService.maxParties() == ia ? ia + 1 : (ia + 2) % PartyService.maxParties();
            PartyService.SetResult moved = PartyService.set(server, a.getUUID(), "party_test_a", target);
            if (!moved.ok() || moved.before() != ia || live.index().indexOf(a.getUUID()) != target) return fail("party set did not move the party from lane " + ia + " to lane " + target + ": " + moved);
            PartyService.SetResult refused = PartyService.set(server, a.getUUID(), "party_test_a", PartyService.maxParties());
            if (refused.ok() || live.index().indexOf(a.getUUID()) != target) return fail("party set accepted the lane " + PartyService.maxParties() + ", which party.maxParties does not allow");
            return ok("two players alone got lanes " + ia + " and " + ib + ", two members of one team got lane " + ic + " together, asking again changes nothing, the System groups the team's members and nobody else, an admin can move a party (lane " + ia + " to " + target + ") but not past party.maxParties");
        } finally {
            PartyResolver.setSource(null);
            for (PartyKey k : keys.values()) live.forget(k.id());
            clean(a);
            clean(b);
            clean(c);
            clean(d);
        }
    }

    // ================================================================================================================ party.guard

    private static SelfTests.Result guard(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        if (!PartyService.instanced()) return ok("the dungeon is not instanced here: the guard decides nothing");
        MegaData data = MegaData.get(server);
        Sector mine = null, other = null;
        for (Sector s : data.sectors()) {
            if (!s.isFloor() || !SectorGenerator.playable(s) || SectorGenerator.padOf(s) == null) continue;
            if (mine == null) mine = s;
            else if (s.party != mine.party && other == null) other = s;
        }
        if (mine == null) return ok("no finished floor yet to stand a player on (run /multiverse mega expand and wait for it)");
        FakePlayer p = standIn(level, "guard");
        BlockPos pad = SectorGenerator.padOf(mine);
        p.setPos(pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5);
        StringBuilder detail = new StringBuilder();
        try {
            if (MegaDungeon.sectorAt(level, p.blockPosition()) != mine) return fail("the stand-in does not stand in sector " + mine.id + " at " + pad.toShortString());
            if (PartyGuard.verdict(p) != PartyPolicy.Verdict.ADOPT) return fail("a player with no record on the floors of party " + mine.party + " should adopt that party but the verdict is " + PartyGuard.verdict(p));
            PartyGuard.check(p);
            Integer adopted = PartyService.assigned(p);
            if (adopted == null || adopted != mine.party) return fail("the guard should have written party " + mine.party + " as the record of a player with none, but it is " + adopted);
            if (PartyGuard.verdict(p) != PartyPolicy.Verdict.OK) return fail("a player on the floors of their own party (" + mine.party + ") should be left alone but the verdict is " + PartyGuard.verdict(p));
            detail.append("a player with no record adopts party ").append(mine.party).append(" and is then left alone; ");
            int stranger = (mine.party + 1) % PartyService.maxParties() == mine.party ? mine.party + 1 : (mine.party + 1) % PartyService.maxParties();
            PartyService.store(p, stranger);
            if (PartyGuard.verdict(p) != PartyPolicy.Verdict.EVICT) return fail("a player recorded in party " + stranger + " on the floors of party " + mine.party + " should be sent out but the verdict is " + PartyGuard.verdict(p));
            if (!PartyGuard.check(p)) return fail("the guard did not act on a player who stands on another party's floors");
            detail.append("a player of party ").append(stranger).append(" on the floors of party ").append(mine.party).append(" is sent out; ");
            if (other != null) {
                BlockPos pad2 = SectorGenerator.padOf(other);
                p.setPos(pad2.getX() + 0.5, pad2.getY(), pad2.getZ() + 0.5);
                PartyService.store(p, mine.party);
                if (PartyGuard.verdict(p) != PartyPolicy.Verdict.EVICT) return fail("a player of party " + mine.party + " who stands on the floors of party " + other.party + " should be sent out but the verdict is " + PartyGuard.verdict(p));
                PartyService.store(p, other.party);
                if (PartyGuard.verdict(p) != PartyPolicy.Verdict.OK) return fail("a player of party " + other.party + " on their own floors should be left alone");
                detail.append("on a real second party (").append(other.party).append(") the same holds; ");
            } else {
                detail.append("only one party has floors yet, so no second real party was tried; ");
            }
            return ok(detail.toString().trim());
        } finally {
            clean(p);
            p.setPos(0.5, 70.0, NOWHERE_Z);
        }
    }

    // ================================================================================================================ party.chains

    private static SelfTests.Result chains(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        // 1. every chain is whole and never leaks into another party
        int sectors = 0;
        for (Sector s : data.sectors()) {
            sectors++;
            if (s.nextId >= 0) {
                Sector next = data.sector(s.nextId);
                if (next == null) return fail("sector " + s.id + " leads to sector " + s.nextId + ", which does not exist");
                if (next.party != s.party || next.polarity != s.polarity) return fail("sector " + s.id + " (party " + s.party + ", " + s.polarity.id() + " road) leads to sector " + next.id + " of party " + next.party + " on the " + next.polarity.id() + " road: a chain must stay in its party");
                if (next.chainIndex != s.chainIndex + 1) return fail("sector " + s.id + " (place " + s.chainIndex + ") leads to sector " + next.id + " at place " + next.chainIndex);
            }
        }
        List<Integer> parties = data.parties();
        for (int party : parties) {
            for (Polarity road : Polarity.values()) {
                List<Sector> chain = data.chain(party, road);
                for (int i = 0; i < chain.size(); i++) {
                    if (chain.get(i).chainIndex != i) return fail("the " + road.id() + " chain of party " + party + " has a hole or a duplicate at place " + i + " (sector " + chain.get(i).id + " says place " + chain.get(i).chainIndex + ")");
                }
            }
        }
        // 2. the Hub's gate, driven by stand-ins whose party is a real chain: each arrives on the pad of THAT party's first floor, two of one party arrive together, two parties apart
        List<String> tried = new ArrayList<>();
        MegaParties live = MegaParties.get(server);
        Map<Integer, BlockPos> pads = new HashMap<>();
        boolean twinned = false;
        int tested = 0;
        for (int party : parties) {
            if (!PartyService.instanced() && party != 0) continue;
            if (party >= PartyService.maxParties()) continue;
            for (Polarity road : Polarity.values()) {
                Sector first = data.chainSector(party, road, 0);
                if (first == null || !SectorGenerator.playable(first)) continue;
                BlockPos want = SectorGenerator.padOf(first);
                if (want == null) continue;
                Gate g = throughGate(server, level, live, "gate_" + party + "_" + road.id(), party, road);
                if (g.failure() != null) return fail(g.failure());
                if (!want.equals(g.pad())) return fail("a stand-in of party " + party + " was sent to " + (g.pad() == null ? "nowhere" : g.pad().toShortString()) + " by the " + road.id() + " gate instead of the pad of its own first floor, sector " + first.id + " at " + want.toShortString());
                if (!instancedOrZero(g.record(), party)) return fail("the gate recorded party " + g.record() + " for a stand-in of party " + party);
                if (g.offered() == null || g.offered().id != first.id) return fail("a newcomer of party " + party + " should be offered sector " + first.id + " but got " + (g.offered() == null ? "nothing" : Integer.toString(g.offered().id)));
                for (int other : parties) {
                    if (other == party) continue;
                    Sector os = MegaTravel.choose(data, other, standIn(level, "offer"), road, true);
                    if (os != null && os.party == party) return fail("the offer for party " + other + " is a floor of party " + party);
                }
                pads.putIfAbsent(party, g.pad());
                if (!twinned) {                                                   // a second stand-in of the SAME party arrives in the same chain: on the same pad
                    twinned = true;
                    Gate twin = throughGate(server, level, live, "gate_twin", party, road);
                    if (twin.failure() != null) return fail(twin.failure());
                    if (!want.equals(twin.pad())) return fail("two stand-ins of party " + party + " were not sent to the same floor: one to " + want.toShortString() + ", the other to " + (twin.pad() == null ? "nowhere" : twin.pad().toShortString()));
                    tried.add("two stand-ins of party " + party + " arrive together on the pad of sector " + first.id);
                }
                tried.add("party " + party + " " + road.id() + " -> sector " + first.id + " (floor " + FloorMap.label(first.firstFloor()) + ")");
                tested++;
                if (tested >= 6) break;
            }
            if (tested >= 6) break;
        }
        if (pads.size() >= 2 && PartyService.instanced()) {                           // two parties land in different lanes
            List<Integer> who = new ArrayList<>(pads.keySet());
            java.util.Collections.sort(who);
            BlockPos a = pads.get(who.get(0)), b = pads.get(who.get(1));
            int gap = Math.abs(a.getZ() - b.getZ());
            if (PartyLanes.partyAt(a.getZ(), MegaGrid.MAX_PARTIES) == PartyLanes.partyAt(b.getZ(), MegaGrid.MAX_PARTIES) || gap < MegaGrid.PARTY_STEP - 2 * PartyLanes.REACH)
                return fail("party " + who.get(0) + " and party " + who.get(1) + " landed on pads only " + gap + " blocks apart in z (" + a.toShortString() + " and " + b.toShortString() + "): they share a lane");
            tried.add("party " + who.get(0) + " and party " + who.get(1) + " land " + gap + " blocks apart");
        }
        String note = parties.size() < 2 ? " (only " + (parties.isEmpty() ? "no party" : "party " + parties.get(0)) + " has floors: run /multiverse mega expand holy party 1 to see a second one)" : "";
        if (tested == 0) return ok(sectors + " sector(s): every chain is whole and stays in its party; no finished first floor to drive a gate to" + note);
        return ok(sectors + " sector(s) of " + parties.size() + " part" + (parties.size() == 1 ? "y" : "ies") + ": every chain is whole and stays in its party; the gate puts each tested party on its own first floor: " + String.join(", ", tried) + note);
    }

    /** What the Hub's gate did with one stand-in: why it failed (null when it did not), the pad it was sent to, the party it recorded and the floor a newcomer of the party is offered. */
    private record Gate(String failure, BlockPos pad, Integer record, Sector offered) {}

    /**
     * Drives one stand-in through the gate of {@code road} as a member of {@code party}: its key joins the party in the book (merged into that lane), the resolver is told so, the real
     * {@code MegaDungeon.enterChain} is called, and everything is put back before returning.
     */
    private static Gate throughGate(MinecraftServer server, ServerLevel level, MegaParties live, String name, int party, Polarity road) {
        FakePlayer p = standIn(level, name);
        PartyKey key = PartyKey.solo(p.getUUID());
        live.assign(key, "party.chains", party);                                  // the stand-in joins the party for the test (merged into its lane); forgotten again below
        PartyResolver.setSource(q -> q.getUUID().equals(p.getUUID()) ? key : null);
        try {
            if (!MegaDungeon.enterChain(p, road)) return new Gate("the gate refused a stand-in of party " + party + " on the " + road.id() + " road (see the server log)", null, null, null);
            return new Gate(null, LabyrinthTravel.arrivalPad(p), PartyService.assigned(p), MegaTravel.choose(MegaData.get(server), party, p, road, true));
        } finally {
            PartyResolver.setSource(null);
            live.forget(key.id());
            clean(p);
        }
    }

    /** The record the gate wrote: the party tested when the dungeon is instanced, 0 when it is not (everybody is then party 0). */
    private static boolean instancedOrZero(Integer record, int party) {
        if (record == null) return false;
        return PartyService.instanced() ? record == party : record == 0;
    }
}
