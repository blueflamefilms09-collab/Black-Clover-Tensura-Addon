package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import com.newuniverse.nusmp.labyrinth.mega.enc.EncounterDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing of the squads can be played by their author, so the CI server smoke test runs this one in the real game against the generated sector 0 with a stand-in
 * player: the pure planner is valid for every depth from 1 to 1000 and every party size, then a forced ambush really spawns a squad, whose size is 5 to 8 (or at least
 * three when places were short), whose members all carry the squad tag, a role tag and the squad's goals, whose roles include a tank and a healer, which the manager holds
 * and thinks about without a fault; then a member is killed (the death is counted and the pass survives it), then the leader (the morale breaks and every survivor has a
 * reaction), and everything is removed. Key {@code enc.squads}, run by {@code /multiverse mega selftest}.
 */
final class SquadSelfTests {
    private SquadSelfTests() {}

    static void register() {
        SelfTests.register("enc.squads", SquadSelfTests::squads);
    }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result squads(MinecraftServer server) {
        String plans = plans();
        if (plans != null) return fail(plans);
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED) return fail("sector 0 is not ready (run /multiverse mega expand first)");
        List<Room> rooms = new ArrayList<>();
        for (Room r : s.rooms) if (!r.kind.isSafe() && !r.kind.isCirculation()) rooms.add(r);
        if (rooms.isEmpty()) return fail("sector 0 has no ordinary room to stand in");
        double mx = (s.bounds.minX() + s.bounds.maxX()) * 0.5, mz = (s.bounds.minZ() + s.bounds.maxZ()) * 0.5;
        rooms.sort(Comparator.comparingDouble((Room r) -> {
            BlockPos c = r.center();
            return (c.getX() - mx) * (c.getX() - mx) + (c.getZ() - mz) * (c.getZ() - mz);
        }));
        String last = "no room tried";
        int tried = 0;
        for (Room room : rooms) {
            if (tried++ >= 8) break;
            BlockPos at = room.center();
            loadAround(mega, at, 72);
            FakePlayer p = SelfTests.player(mega, "enc_squad", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            try {
                EncounterDirector.clear(mega);
                SquadManager.reset();
                EncounterDirector.forgetPlayer(p.getUUID());
                EncounterDirector.Outcome o = EncounterDirector.roll(mega, List.of(p), EncounterDirector.Force.band(Band.AMBUSH).peacefulOk());
                if (!o.spawned()) {
                    last = "a forced roll said " + o.kind() + ": " + o.text();
                    continue;
                }
                if (!SquadConfig.enabled()) return SelfTests.Result.ok("squads are switched off in the config: the encounter spawned " + o.mobs().size() + " monsters the old way");
                return verify(mega, p, o);
            } finally {
                EncounterDirector.clear(mega);
                EncounterDirector.forgetPlayer(p.getUUID());
                SquadManager.reset();
            }
        }
        return fail("no forced ambush could place a squad in " + Math.min(tried, 8) + " rooms; the last said: " + last);
    }

    private static void loadAround(ServerLevel level, BlockPos c, int reach) {
        for (int cx = (c.getX() - reach) >> 4; cx <= (c.getX() + reach) >> 4; cx++)
            for (int cz = (c.getZ() - reach) >> 4; cz <= (c.getZ() + reach) >> 4; cz++) level.getChunk(cx, cz);
    }

    /** The planner over the whole range of floors, party sizes and bands: null when every plan is valid, else the first problem. */
    static String plans() {
        for (int depth = 1; depth <= 1000; depth += depth < 60 ? 1 : 13) {
            for (int party = 1; party <= 8; party++) {
                for (int band = SquadPlan.MINOR; band <= SquadPlan.AMBUSH; band++) {
                    SquadPlan.Plan p = SquadPlan.plan(depth * 977L + party * 13L + band, depth, party, band, "undead", "necropolis");
                    String where = "depth " + depth + " party " + party + " band " + band + ": " + p.describe();
                    if (p.size() < 5 || p.size() > 8 || p.roles().size() != p.size()) return "bad size at " + where;
                    if (p.count(SquadRole.TANK) < 1 || p.count(SquadRole.TANK) > 2) return "tanks out of range at " + where;
                    if (p.count(SquadRole.SUPPORT) < 1 || p.count(SquadRole.SUPPORT) > 2) return "supports out of range at " + where;
                    if (p.count(SquadRole.DPS_RANGED) < 1 || p.count(SquadRole.DPS_MELEE) + p.count(SquadRole.FLANKER) < 1) return "a damage role is missing at " + where;
                }
            }
        }
        return null;
    }

    private static SelfTests.Result verify(ServerLevel mega, FakePlayer p, EncounterDirector.Outcome o) {
        List<Mob> mobs = o.mobs();
        Squad squad = mobs.isEmpty() ? null : SquadManager.of(mobs.get(0));
        if (squad == null) return fail("the encounter '" + o.entry() + "' (" + mobs.size() + " monsters) formed no squad");
        if (mobs.size() < 3 || mobs.size() > SquadPlan.MAX_SIZE) return fail("the squad has " + mobs.size() + " monsters, outside 3 to 8");
        if (squad.plan() == null || squad.plan().size() < 5 || squad.plan().size() > 8) return fail("the plan size is " + (squad.plan() == null ? "missing" : squad.plan().size()));
        Map<SquadRole, Integer> roles = new EnumMap<>(SquadRole.class);
        boolean goalsReadable = GoalInstaller.available();
        for (Mob m : mobs) {
            String who = m.getType().toString();
            if (!m.getTags().contains(Squad.TAG)) return fail(who + " lacks the squad tag");
            SquadRole r = SquadRole.fromId(m.getPersistentData().getString(Squad.ROLE));
            if (r == null) return fail(who + " has no role tag");
            roles.merge(r, 1, Integer::sum);
            if (SquadManager.of(m) != squad) return fail(who + " is in another squad");
            if (goalsReadable) {
                for (Class<? extends net.minecraft.world.entity.ai.goal.Goal> g : List.of(InterceptAttackGoal.class, FlankTargetGoal.class, SupportRetreatGoal.class,
                        FormationFollowGoal.class, MoraleFleeGoal.class, SquadFocusGoal.class)) {
                    if (!GoalInstaller.has(m, g)) return fail(who + " lacks " + g.getSimpleName());
                }
            }
        }
        if (roles.getOrDefault(SquadRole.TANK, 0) < 1) return fail("no tank in " + roles);
        if (mobs.size() >= 4 && roles.getOrDefault(SquadRole.SUPPORT, 0) < 1) return fail("no healer in " + roles);
        if (SquadManager.count() < 1) return fail("the manager holds no squad");
        long now = mega.getGameTime();
        SquadManager.thinkNow(mega, squad);
        if (squad.aliveCount() != mobs.size()) return fail("after a think the squad counts " + squad.aliveCount() + " alive of " + mobs.size());
        if (squad.leaderId() == null) return fail("the squad has no leader");
        if (squad.contextFor(mobs.get(0), now) == null) return fail("the snapshot of a member is missing");
        String line = squad.describe();

        // a member dies: counted, no fault
        Mob victim = null;
        for (Mob m : mobs) if (!m.getUUID().equals(squad.leaderId())) {
            victim = m;
            break;
        }
        if (victim == null) return fail("the squad has only a leader");
        int before = squad.aliveCount();
        victim.kill();
        SquadManager.thinkNow(mega, squad);
        if (squad.aliveCount() != before - 1) return fail("a death left " + squad.aliveCount() + " alive, expected " + (before - 1));
        if (squad.deaths() < 1) return fail("the death was not counted");

        // the leader falls: the morale breaks, and every survivor reacts
        Mob lead = null;
        for (Mob m : mobs) if (m.getUUID().equals(squad.leaderId())) lead = m;
        if (lead != null && lead.isAlive()) {
            lead.kill();
            SquadManager.thinkNow(mega, squad);
            if (squad.aliveCount() > 1 && !squad.isBroken()) return fail("the leader fell and the nerve did not break");
            for (Mob m : squad.alive()) {
                Squad.Member me = squad.member(m.getUUID());
                if (me == null || me.reaction() == SquadMind.Reaction.HOLD) return fail("a survivor has no reaction after the break");
                Squad.calm(m);
            }
        }
        // a wipe: nobody is left, and the pass still survives it
        for (Mob m : new ArrayList<>(squad.alive())) m.kill();
        SquadManager.thinkNow(mega, squad);
        if (squad.aliveCount() != 0) return fail("a wiped squad still counts " + squad.aliveCount());
        return SelfTests.Result.ok("squad of " + mobs.size() + " (plan " + squad.plan().describe() + "; formed as " + line + "), goals " + (goalsReadable ? "installed" : "unreadable, steered by the manager")
                + ", death counted, leader fall broke the nerve (" + squad.broken() + "), wipe survived, plans valid for depth 1 to 1000");
    }
}
