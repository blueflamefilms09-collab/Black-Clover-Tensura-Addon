package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestMiragePlan.Fake;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestRules.Situation;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestRules.Step;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fake houses of the Tempest Mirage (docs/living_dungeon.md section 5, zone 3): three houses of the village are illusions. They stand
 * like the others and shimmer very faintly; when someone comes close one shimmers hard and chimes (the warning, and the way out: step
 * away), and if someone is still near when the moment is over it dissolves into haze, and the swarm comes out of it ({@link HiveDriver#ambush}).
 * Once nobody is about, and a while has passed, it forms again. Which fake is down is read from the world (is the block of its roof there?), so
 * nothing but the warnings and the timers is kept, and those only in memory: after a restart a fake that is down is simply rebuilt when nobody
 * is about. Every change of blocks is the mod's own building (inside {@code DungeonGuard.internal}), bounded by one fake's size, and at most
 * one dissolving or forming is done in a pass. The rules are {@link TempestRules}.
 */
final class TempestIllusions {
    private TempestIllusions() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** When each fake was armed, when it went down (game ticks), and when each village last sprang a trap. */
    private static final Map<Long, Long> ARMED = new HashMap<>(), DOWN = new HashMap<>(), SPRUNG = new HashMap<>();

    private static long fakeKey(long nexus, Fake f) { return nexus * 7919L + f.id(); }

    /** Forgets every timer (the world is closing): nothing of it may carry over into another world. */
    static void clear() {
        ARMED.clear();
        DOWN.clear();
        SPRUNG.clear();
    }

    /** The blocks of the fake's roof, its walls: the whole of its volume is in loaded chunks. */
    private static boolean loaded(ServerLevel level, Fake f) {
        for (int cx = f.minX() >> 4; cx <= f.maxX() >> 4; cx++) for (int cz = f.minZ() >> 4; cz <= f.maxZ() >> 4; cz++) if (level.getChunkSource().getChunkNow(cx, cz) == null) return false;
        return true;
    }

    /** About once a second while players are in the village: every fake is looked at and its next step taken. */
    static void tick(ServerLevel level, TempestMiragePlan.Layout layout, List<ServerPlayer> everyone) {
        List<ServerPlayer> inside = new ArrayList<>(everyone.size());
        for (ServerPlayer p : everyone) if (HiveDriver.fair(p)) inside.add(p);              // a creative player springs no trap: the swarm is not theirs to meet
        ZonePlan plan = layout.plan();
        long now = level.getGameTime(), nexus = NexusCells.key(plan.cellX, plan.cellZ);
        RandomSource r = level.getRandom();
        int down = 0;
        for (Fake f : layout.fakes()) if (loaded(level, f) && !standing(level, f)) down++;
        boolean acted = false;
        for (Fake f : layout.fakes()) {
            if (!loaded(level, f)) continue;
            long fk = fakeKey(nexus, f);
            boolean standing = standing(level, f);
            Long armedAt = ARMED.get(fk), downSince = DOWN.get(fk), sprung = SPRUNG.get(nexus);
            boolean triggered = false, near = false, away = true;
            ServerPlayer closest = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : inside) {
                if (Math.abs(p.getY() - (plan.floorY + 1)) > 5.0) continue;
                if (f.near(p.getX(), p.getZ(), TempestRules.TRIGGER)) triggered = true;
                if (f.near(p.getX(), p.getZ(), TempestRules.DISSOLVE_RANGE)) near = true;
                if (f.near(p.getX(), p.getZ(), TempestRules.REBUILD_RANGE)) away = false;
                double d = p.distanceToSqr(f.doorX() + 0.5, plan.floorY + 1.0, f.doorZ() + 0.5);
                if (d < best) {
                    best = d;
                    closest = p;
                }
            }
            boolean occupied = !standing && !level.getEntitiesOfClass(LivingEntity.class, new AABB(f.minX(), f.minY(), f.minZ(), f.maxX() + 1, f.maxY() + 1, f.maxZ() + 1), e -> e.isAlive()).isEmpty();
            Step step = TempestRules.decide(new Situation(standing, armedAt != null, armedAt == null ? 0L : armedAt, now, triggered, near, away, occupied,
                    downSince == null ? Long.MIN_VALUE : downSince, sprung == null ? Long.MIN_VALUE : sprung, down));
            try {
                switch (step) {
                    case ARM -> {
                        ARMED.put(fk, now);
                        warn(level, f, inside);
                    }
                    case DISARM -> ARMED.remove(fk);
                    case DISSOLVE -> {
                        if (acted || closest == null) break;
                        acted = true;
                        ARMED.remove(fk);
                        DOWN.put(fk, now);
                        SPRUNG.put(nexus, now);
                        down++;
                        dissolve(level, layout, f, closest, inside);
                    }
                    case REBUILD -> {
                        if (acted) break;
                        acted = true;
                        DOWN.remove(fk);
                        down = Math.max(0, down - 1);
                        form(level, f);
                    }
                    default -> shimmer(level, f, armedAt != null, r);
                }
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("[nusmp] A fake house of the Tempest Mirage of cell ({}, {}) failed", plan.cellX, plan.cellZ, ex);
            }
        }
        if (ARMED.size() > 256) ARMED.clear();                                            // forgotten villages: the maps stay small
        if (DOWN.size() > 256) DOWN.clear();
        if (SPRUNG.size() > 64) SPRUNG.clear();
    }

    /** Is the fake standing: is the block of its roof that only it has there? */
    static boolean standing(ServerLevel level, Fake f) { return !level.getBlockState(new BlockPos(f.markX(), f.markY(), f.markZ())).isAir(); }

    private static void shimmer(ServerLevel level, Fake f, boolean armed, RandomSource r) {
        if (!standing(level, f)) return;
        if (!armed && r.nextInt(6) != 0) return;
        double cx = (f.minX() + f.maxX() + 1) / 2.0, cz = (f.minZ() + f.maxZ() + 1) / 2.0, hx = (f.maxX() - f.minX() + 1) / 2.0, hz = (f.maxZ() - f.minZ() + 1) / 2.0;
        level.sendParticles(ParticleTypes.END_ROD, cx, (f.minY() + f.maxY()) / 2.0 + 0.5, cz, armed ? 14 : 2, hx, (f.maxY() - f.minY()) / 2.0, hz, armed ? 0.02 : 0.005);
    }

    private static void warn(ServerLevel level, Fake f, List<ServerPlayer> inside) {
        level.playSound(null, new BlockPos(f.doorX(), f.minY(), f.doorZ()), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 2.0f, 0.6f);
        Component line = Component.literal(HiveCues.FAKE_WARNING).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);
        for (ServerPlayer p : inside) if (f.near(p.getX(), p.getZ(), TempestRules.DISSOLVE_RANGE)) p.displayClientMessage(line, true);
        shimmer(level, f, true, level.getRandom());
    }

    /** The house dissolves: every block of it above the floor goes, in a puff of haze, and the swarm comes out. */
    private static void dissolve(ServerLevel level, TempestMiragePlan.Layout layout, Fake f, ServerPlayer cause, List<ServerPlayer> inside) {
        DungeonGuard.internal(() -> {
            NexusSink sink = new NexusSink(level);
            for (NexusOp op : f.ops()) {
                NexusOp air = new NexusOp(NexusOp.Kind.BOX, NexusOp.AIR, "", op.x1(), op.y1(), op.z1(), op.x2(), op.y2(), op.z2());
                for (int y = air.y1(); y <= air.y2(); y++) for (int z = air.z1(); z <= air.z2(); z++) for (int x = air.x1(); x <= air.x2(); x++) sink.place(air, x, y, z);
            }
        });
        double cx = (f.minX() + f.maxX() + 1) / 2.0, cz = (f.minZ() + f.maxZ() + 1) / 2.0;
        level.sendParticles(ParticleTypes.CLOUD, cx, f.minY() + 1.5, cz, 60, (f.maxX() - f.minX() + 1) / 2.0, 1.5, (f.maxZ() - f.minZ() + 1) / 2.0, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, cx, f.minY() + 2.5, cz, 40, (f.maxX() - f.minX() + 1) / 2.0, 2.0, (f.maxZ() - f.minZ() + 1) / 2.0, 0.08);
        level.playSound(null, new BlockPos(f.doorX(), f.minY(), f.doorZ()), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 3.0f, 0.7f);
        Component line = Component.literal(HiveCues.FAKE_DISSOLVES).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        for (ServerPlayer p : inside) if (f.near(p.getX(), p.getZ(), TempestRules.REBUILD_RANGE)) p.displayClientMessage(line, true);
        HiveDriver.ambush(level, layout, f, cause, inside.size());
    }

    /** The house forms again, block by block as the plan has it. */
    private static void form(ServerLevel level, Fake f) {
        DungeonGuard.internal(() -> {
            NexusSink sink = new NexusSink(level);
            for (NexusOp op : f.ops()) for (int y = op.y1(); y <= op.y2(); y++) for (int z = op.z1(); z <= op.z2(); z++) for (int x = op.x1(); x <= op.x2(); x++) sink.place(op, x, y, z);
        });
        double cx = (f.minX() + f.maxX() + 1) / 2.0, cz = (f.minZ() + f.maxZ() + 1) / 2.0;
        level.sendParticles(ParticleTypes.END_ROD, cx, f.minY() + 2.0, cz, 20, (f.maxX() - f.minX() + 1) / 2.0, 2.0, (f.maxZ() - f.minZ() + 1) / 2.0, 0.02);
    }
}
