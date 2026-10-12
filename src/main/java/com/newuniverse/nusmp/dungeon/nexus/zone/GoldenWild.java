package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.EcosystemData;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.wild.Roster;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The wildlife of the Golden City's garden (docs/living_dungeon.md, "The Golden City"): apex predators, legendary ones when the roster has any,
 * and the herds of tier 1 and 2 grazers they feed on, brought in and kept within the caps of {@link GoldenEcology}. The creatures are the
 * Dungeon's own: the same roster ({@link Wildlife}), the same way of making one ({@link WildSpawn}), and from then on the same hunger and the
 * same hunting ({@code WildDriver}) as everywhere else. This only keeps their number: one birth or one removal about every five seconds at
 * the most, never within 24 blocks of a player or 14 of a gate, and a removal only of a creature 40 blocks from everybody.
 */
final class GoldenWild {
    private GoldenWild() {}

    /** The game ticks of the last birth of each kind (prey, apex, legend) in each garden. Not saved: a restart starts the clocks again, and the caps still hold. */
    private static final Map<Long, long[]> CLOCKS = new HashMap<>();
    private static final int TRIES = 3;

    private static long[] clock(ZonePlan plan) {
        return CLOCKS.computeIfAbsent(((long) plan.cellX << 32) ^ (plan.cellZ & 0xFFFFFFFFL), k -> new long[]{Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE});
    }

    // ---------------------------------------------------------------- the first inhabitants
    /** Fills a new garden: a herd of grazers and a few apex predators, wherever the plan allows, whoever is about. */
    static void populate(ServerLevel level, ZonePlan plan) {
        long now = level.getGameTime();
        long[] clock = clock(plan);
        int prey = 0, apex = 0;
        for (int i = 0; i < 14; i++) if (spawn(level, plan, GoldenEcology.Act.PREY, 0.0)) prey++;
        for (int i = 0; i < 3; i++) if (spawn(level, plan, GoldenEcology.Act.APEX, 0.0)) apex++;
        if (prey > 0) clock[0] = now;
        if (apex > 0) clock[1] = now;
    }

    // ---------------------------------------------------------------- keeping the balance
    /** About every five seconds while players are near: count, decide, and bring one in or take one away. */
    static void tick(ServerLevel level, ZonePlan plan) {
        AABB box = new AABB(plan.footprint.minX(), plan.floorY - 2, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
        List<Mob> life = level.getEntitiesOfClass(Mob.class, box, m -> Wildlife.tierOf(m) > 0 && !kept(m));
        List<Mob> prey = new ArrayList<>(), apex = new ArrayList<>(), legend = new ArrayList<>();
        for (Mob m : life) {
            if (Wildlife.isLegendary(m)) legend.add(m);
            else if (Wildlife.tierOf(m) >= 3) apex.add(m);
            else prey.add(m);
        }
        weigh(level, plan, life);
        long now = level.getGameTime();
        long[] clock = clock(plan);
        boolean births = DungeonConfig.get(DungeonConfig.LIFE);                         // the Dungeon's switch for births: off, and nothing is brought in (what stands is still kept within the caps)
        GoldenEcology.Act act = GoldenEcology.decide(new GoldenEcology.Census(prey.size(), apex.size(), legend.size()),
                GoldenEcology.since(now, clock[0]), GoldenEcology.since(now, clock[1]), GoldenEcology.since(now, clock[2]),
                Wildlife.roster().legendCount() > 0, level.getRandom().nextDouble());
        switch (act) {
            case PREY -> {
                if (births && spawn(level, plan, act, GoldenEcology.SPAWN_CLEAR)) clock[0] = now;
            }
            case APEX -> {
                if (births && spawn(level, plan, act, GoldenEcology.SPAWN_CLEAR)) clock[1] = now;
            }
            case LEGEND -> {
                if (births && spawn(level, plan, act, GoldenEcology.SPAWN_CLEAR)) clock[2] = now;
            }
            case CULL_APEX -> cull(level, apex);
            case CULL_PREY -> cull(level, prey);
            default -> { }
        }
    }

    /** Tamed, ridden, carrying a rider, on a leash or given a name: somebody's own, so neither counted nor ever taken away. */
    private static boolean kept(Mob m) {
        return (m instanceof TamableAnimal t && t.isTame()) || m.isVehicle() || m.isPassenger() || m.isLeashed() || m.hasCustomName();
    }

    /**
     * The count the Dungeon's breeding no longer makes here (it does not breed in the garden): the biomass of each chunk of the garden is the
     * weight of the creatures that stand in it, as {@code DungeonEcology} sets it elsewhere. The Anti-Bird Compass reads it to find the food web.
     */
    private static void weigh(ServerLevel level, ZonePlan plan, List<Mob> life) {
        Map<Long, Float> mass = new HashMap<>();
        for (Mob m : life) mass.merge(ChunkPos.asLong(m.blockPosition()), Wildlife.massOf(m), Float::sum);
        for (int[] ch : plan.footprint.chunks()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ch[0], ch[1]);
            if (chunk == null) continue;
            EcosystemData e = chunk.getData(LivingDungeon.ECOSYSTEM);
            float want = Math.min(EcosystemData.BIOMASS_MAX, mass.getOrDefault(ChunkPos.asLong(ch[0], ch[1]), 0f));
            if (Math.abs(e.biomass - want) < 0.5f) continue;
            e.biomass = want;
            e.clamp();
            chunk.setUnsaved(true);
        }
    }

    /** Takes away the creature that is farthest from every player (and at least 40 blocks from all of them), if there is one. */
    private static void cull(ServerLevel level, List<Mob> from) {
        double[] far = new double[from.size()];
        for (int i = 0; i < far.length; i++) {
            Mob m = from.get(i);
            double best = Double.MAX_VALUE;
            for (Player p : level.players()) {
                if (!p.isSpectator()) best = Math.min(best, p.distanceToSqr(m));
            }
            far[i] = best;
        }
        int i = GoldenEcology.cull(far);
        if (i >= 0) from.get(i).discard();
    }

    // ---------------------------------------------------------------- bringing one in
    /** Brings one creature of the kind into the garden, at one of the plan's places that is clear of the players and the gates; false if it could not. */
    private static boolean spawn(ServerLevel level, ZonePlan plan, GoldenEcology.Act act, double playerClear) {
        List<ZonePlan.Spot> spots = plan.spawns(act == GoldenEcology.Act.PREY ? "prey" : "apex");
        if (spots.isEmpty()) return false;
        int[][] places = new int[spots.size()][];
        for (int i = 0; i < places.length; i++) places[i] = new int[]{spots.get(i).x(), spots.get(i).y(), spots.get(i).z()};
        List<double[]> people = new ArrayList<>();
        if (playerClear > 0) for (Player p : level.players()) if (!p.isSpectator()) people.add(new double[]{p.getX(), p.getZ()});
        double[][] gates = new double[plan.entrances.size()][];
        for (int i = 0; i < gates.length; i++) gates[i] = new double[]{plan.entrances.get(i).x() + 0.5, plan.entrances.get(i).z() + 0.5};
        RandomSource r = level.getRandom();
        for (int t = 0; t < TRIES; t++) {
            int i = GoldenEcology.pick(places, people.toArray(new double[0][]), gates, playerClear, GoldenEcology.GATE_CLEAR, r.nextDouble());
            if (i < 0) return false;
            String id = choose(act, r);
            if (id == null) return false;
            if (WildSpawn.spawnId(level, id, new BlockPos(places[i][0], places[i][1], places[i][2]), r) != null) return true;
        }
        return false;
    }

    /** The kind of creature to bring: a legendary one, an apex predator of tier 3, or a tier 1 or 2 grazer if one turns up in a few draws. */
    private static String choose(GoldenEcology.Act act, RandomSource r) {
        Roster roster = Wildlife.roster();
        int place = DungeonTier.GOLDEN_CITY.ordinal();
        if (act == GoldenEcology.Act.LEGEND) {
            Roster.Entry e = roster.pickLegend(r.nextDouble());
            return e == null ? null : e.id();
        }
        if (act == GoldenEcology.Act.APEX) {
            Roster.Entry e = roster.pick(3, place, false, r.nextDouble());
            return e == null ? null : e.id();
        }
        Roster.Entry best = null;
        for (int t = 0; t < 4; t++) {
            Roster.Entry e = roster.pick(r.nextInt(5) < 3 ? 1 : 2, place, false, r.nextDouble());
            if (e == null) continue;
            if (best == null || e.grazer()) best = e;
            if (e.grazer()) break;
        }
        return best == null ? null : best.id();
    }
}
