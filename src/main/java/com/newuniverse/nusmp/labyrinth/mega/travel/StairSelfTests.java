package com.newuniverse.nusmp.labyrinth.mega.travel;

import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.gen.StairLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the stairs of the floor model (1.58.0) are the only way from one floor to the next, and nothing about them can be watched by the people who wrote them, so the CI
 * smoke test runs these two checks in the real game ({@code /multiverse mega selftest stairs.*}) after it has made floors grow. {@code stairs.layout} reads what the
 * generator saved and what the world really holds for every finished floor: two STAIR_EXIT markers that are the saved back and forward exits, an ARRIVAL marker on the
 * landing pad of the forward stairwell and another on the platform of the arrival stairwell (in the stairwell room that holds the way back), a floor under the 3 x 3 pad
 * of each exit, standing room above its middle, and an arrival that is NOT on a pad (a party that has just come in must not be sent straight back). {@code stairs.travel}
 * walks a stand-in player over it: onto the forward pad of a floor (arrives on the pad of the next floor's arrival stairwell, and the use cooldown is set), onto the back pad
 * of that floor (arrives on the forward landing of the floor before), and onto the arrival platform (nothing happens). The walk to the Hub from the first floor is not tried
 * here (it teleports out of the dimension, which a stand-in cannot do); the numbers behind it are tested in {@code StairLinksTest}. A check that needs finished floors says
 * so in its answer when there are none, instead of passing silently.
 */
public final class StairSelfTests {
    private StairSelfTests() {}

    public static void register() {
        SelfTests.register("stairs.layout", StairSelfTests::layout);
        SelfTests.register("stairs.travel", StairSelfTests::travel);
    }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    // ================================================================================================================ stairs.layout

    private static SelfTests.Result layout(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        int floors = 0, bosses = 0;
        for (Sector s : new ArrayList<>(data.sectors())) {                      // the floors of every party (1.58.0), not only the shared chain
            Polarity road = s.polarity;
            if (!s.isFloor() || !SectorGenerator.playable(s)) continue;
            String bad = checkFloor(level, s);
            if (bad != null) return fail("sector " + s.id + " (floor " + FloorMap.label(s.firstFloor()) + ", " + road.id() + " road" + (s.party == 0 ? "" : ", party " + s.party) + "): " + bad);
            floors++;
            if (s.isBossFloor()) bosses++;
        }
        if (floors == 0) return ok("no finished floor of the floor model yet (run /multiverse mega expand and wait for it)");
        return ok(floors + " finished floor(s) (" + bosses + " boss): each has its two exits, a floor under both 3 x 3 pads, standing room over their middles, a landing on the forward stairs and an arrival that is on no pad");
    }

    /** What is wrong with the stairs of one finished floor, or null. */
    private static String checkFloor(ServerLevel level, Sector s) {
        List<BlockPos> exits = new ArrayList<>();
        List<BlockPos> arrivals = new ArrayList<>();
        for (Marker m : s.markers) {
            if (m.kind() == MarkerKind.STAIR_EXIT) exits.add(m.pos());
            else if (m.kind() == MarkerKind.ARRIVAL) arrivals.add(m.pos());
        }
        if (exits.size() != 2) return "it has " + exits.size() + " stair exit markers, not 2";
        if (s.stairBack == null || s.stairForward == null) return "the saved stairs are incomplete (back " + s.stairBack + ", forward " + s.stairForward + ")";
        if (s.stairBack.equals(s.stairForward)) return "the way back and the way on are the same cell";
        if (!exits.contains(s.stairBack) || !exits.contains(s.stairForward)) return "the saved exits are not the STAIR_EXIT markers";
        if (arrivals.size() != 2) return "it has " + arrivals.size() + " arrival markers, not 2 (one on each stairwell)";
        if (s.arrival == null || !arrivals.contains(s.arrival)) return "its saved arrival cell is not an ARRIVAL marker";
        if (s.stairForwardPad == null || !arrivals.contains(s.stairForwardPad) || s.stairForwardPad.equals(s.arrival)) return "the landing of the forward stairs is not the ARRIVAL marker of the other stairwell";
        Room home = s.roomAt(s.arrival);
        if (home == null || home.kind != RoomKind.STAIRWELL || !home.contains(s.stairBack)) return "the arrival cell is not in the stairwell room that holds the way back";
        Room onward = s.roomAt(s.stairForward);
        if (onward == null || onward.kind != RoomKind.STAIRWELL || !onward.contains(s.stairForwardPad)) return "the forward exit is not in the stairwell room that holds its landing";
        for (BlockPos exit : exits) {
            String bad = checkPad(level, exit);
            if (bad != null) return "the pad at " + exit.toShortString() + " " + bad;
            if (!StairLinks.onPad(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5, exit.getX(), exit.getY(), exit.getZ())) return "a player standing on the exit " + exit.toShortString() + " would not be on its pad";
            for (BlockPos landing : new BlockPos[]{s.arrival, s.stairForwardPad}) {
                if (StairLinks.onPad(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, exit.getX(), exit.getY(), exit.getZ())) {
                    return "the landing " + landing.toShortString() + " is on the pad " + exit.toShortString() + ": a party that arrives there would be sent straight on";
                }
            }
        }
        return null;
    }

    /** The pad under an exit cell: a floor under all nine cells of its 3 x 3, and the exit cell and the one above it free to stand in. Null when it is fine. */
    private static String checkPad(ServerLevel level, BlockPos exit) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos under = exit.offset(dx, -1, dz);
                BlockState b = level.getBlockState(under);
                if (b.isAir()) return "has no floor under " + under.toShortString();
            }
        }
        BlockPos below = exit.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return "has no sturdy floor under its middle at " + below.toShortString();
        if (level.getBlockState(exit).blocksMotion()) return "is blocked at its middle " + exit.toShortString();
        if (level.getBlockState(exit.above()).blocksMotion()) return "has no head room over its middle at " + exit.above().toShortString();
        return null;
    }

    // ================================================================================================================ stairs.travel

    private static SelfTests.Result travel(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return fail("no mega dimension");
        MegaData data = MegaData.get(server);
        StringBuilder detail = new StringBuilder();
        int tested = 0;
        List<int[]> chains = new ArrayList<>();                                 // 1.58.0: every party has chains of its own; the stairs of each are walked ({party, road})
        for (int party : data.parties()) for (Polarity r : Polarity.values()) chains.add(new int[] {party, r.ordinal()});
        for (int[] pr : chains) {
            int party = pr[0];
            Polarity road = Polarity.values()[pr[1]];
            String ofParty = party == 0 ? "" : " of party " + party;
            Sector low = null, high = null;
            for (Sector s : data.chain(party, road)) {
                if (!s.isFloor() || !SectorGenerator.playable(s) || s.stairForward == null || s.stairBack == null) {
                    low = null;
                    continue;
                }
                if (low != null && s.chainIndex == low.chainIndex + 1) {
                    high = s;
                    break;
                }
                low = s;
            }
            if (low == null || high == null) {
                detail.append("the ").append(road.id()).append(" road").append(ofParty).append(" has no two finished floors in a row; ");
                continue;
            }
            FakePlayer p = SelfTests.player(level, "mega_stair_test_" + road.id(), low.stairForward.getX() + 0.5, low.stairForward.getY(), low.stairForward.getZ() + 0.5);
            CompoundTag mem = p.getPersistentData();
            reset(p);
            try {
                if (!MegaDungeon.mayEnter(p, road)) {
                    detail.append("the Directional Lock refuses the stand-in on the ").append(road.id()).append(" road").append(ofParty).append(", so its stairs were not walked; ");
                    continue;
                }
                // on: the forward pad of the lower floor leads to the arrival of the upper one
                StairTravel.look(level, data, p);
                BlockPos sent = LabyrinthTravel.arrivalPad(p);
                BlockPos want = SectorGenerator.padOf(high);
                if (want == null || !want.equals(sent)) {
                    return fail("on the forward pad of floor " + FloorMap.label(low.firstFloor()) + " (sector " + low.id + ") the stand-in was sent to " + (sent == null ? "nowhere" : sent.toShortString())
                            + " instead of the arrival of floor " + FloorMap.label(high.firstFloor()) + " (sector " + high.id + ") at " + (want == null ? "nowhere" : want.toShortString()));
                }
                if (mem.getLong(StairTravel.UNTIL) <= level.getGameTime()) return fail("using the stairs of sector " + low.id + " set no cooldown, so a player could be bounced");
                // a second look at once does nothing (the cooldown holds)
                LabyrinthTravel.forget(p);
                StairTravel.look(level, data, p);
                if (LabyrinthTravel.arrivalPad(p) != null) return fail("the stand-in was moved twice within the cooldown");

                // back: the back pad of the upper floor leads to the forward landing of the lower one
                reset(p);
                p.setPos(high.stairBack.getX() + 0.5, high.stairBack.getY(), high.stairBack.getZ() + 0.5);
                StairTravel.look(level, data, p);
                sent = LabyrinthTravel.arrivalPad(p);
                want = low.stairForwardPad;
                if (want == null || !want.equals(sent)) {
                    return fail("on the back pad of floor " + FloorMap.label(high.firstFloor()) + " (sector " + high.id + ") the stand-in was sent to " + (sent == null ? "nowhere" : sent.toShortString())
                            + " instead of the forward landing of floor " + FloorMap.label(low.firstFloor()) + " (sector " + low.id + ") at " + (want == null ? "nowhere" : want.toShortString()));
                }

                // the arrival platform is no pad
                reset(p);
                p.setPos(high.arrival.getX() + 0.5, high.arrival.getY(), high.arrival.getZ() + 0.5);
                StairTravel.look(level, data, p);
                if (LabyrinthTravel.arrivalPad(p) != null) return fail("standing on the arrival platform of sector " + high.id + " moved the stand-in: the arrival is on a pad");
                detail.append("the ").append(road.id()).append(" road").append(ofParty).append(": floor ").append(FloorMap.label(low.firstFloor())).append(" to ").append(FloorMap.label(high.firstFloor()))
                        .append(" and back by the pads, the arrival platform is no pad; ");
                tested++;
            } finally {
                reset(p);
                mem.remove(MegaTravel.LANDING_KEY);
                mem.remove(MegaTravel.LANDING_HOLY);
                mem.remove(MegaTravel.LANDING_ABYSSAL);
                MegaTravel.forget(p.getUUID());
            }
        }
        if (tested == 0) return ok("no road has two finished floors in a row yet: " + detail.toString().trim());
        return ok(detail.toString().trim());
    }

    /** Clears what a stair use leaves on the stand-in: the cooldown and the pad it was set down on. */
    private static void reset(FakePlayer p) {
        p.getPersistentData().remove(StairTravel.UNTIL);
        LabyrinthTravel.forget(p);
    }
}
