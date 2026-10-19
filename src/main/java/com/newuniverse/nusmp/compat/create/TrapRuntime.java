package com.newuniverse.nusmp.compat.create;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the part of a Create trap that hurts and moves things, run by the mod itself so a trap works whatever Create does with its own blocks. Create's saws, drills, pistons,
 * belts and spouts are REAL blocks in the walls and the ceiling, powered by a creative motor through a clutch that this class cuts and opens with a redstone block (so the
 * machinery visibly winds up when the trap fires, and stops when it ends or is disarmed), but the hurt itself is a "band": a slab of the lane that warns, strikes and rests on
 * the schedule of {@link TrapClock}. Everything that hurts is bounded by {@link TrapRules} (one hit, one run, mercy for the nearly dead, never lethal), a victim is hit at most
 * once per band per cycle, and only survival and adventure players are hurt or thrown. The ram of a press is a block this class puts into the two edge columns of the lane for
 * the length of a strike (never the middle column, never onto an entity) and takes away again, and on the first look at a trap after a restart every ram and clutch is put back
 * to its idle state, so a server that stopped mid-strike leaves nothing in the lane.
 */
public final class TrapRuntime {
    private TrapRuntime() {}

    /** The live state of one built trap; not saved (a restart rebuilds it from the site and normalises the blocks). */
    public static final class Live {
        public final TrapSite site;
        public final Blueprint bp;
        public final Frame frame;
        public final TrapClock.Machine machine = new TrapClock.Machine();
        boolean normalized;
        long lastAssert, lastNear;
        final TrapClock.Phase[] last;
        final Set<BlockPos> rams = new HashSet<>();
        final Map<UUID, Float> taken = new HashMap<>();
        final Map<String, Long> hit = new HashMap<>();
        final Map<UUID, Long> hinted = new HashMap<>();

        public Live(TrapSite site) {
            this.site = site;
            this.bp = site.blueprint();
            this.frame = site.frame();
            this.last = new TrapClock.Phase[bp.bands().size()];
            java.util.Arrays.fill(last, TrapClock.Phase.QUIET);
        }

        public int ramsOut() { return rams.size(); }
    }

    /** What one look at a trap did, for the self-test and the status command. */
    public record Report(TrapClock.State state, int hits, float asked, int ramsOut, boolean disarmed, boolean triggered) {}

    // ---------------------------------------------------------------- one look
    /**
     * Looks at a built trap at game tick {@code now}. {@code force} fires the trap as if its trigger had been pressed (the admin command and the self-test).
     * {@code players} is the list of players to consider; null = the players standing in the level (a self-test passes its stand-ins, which are not in the level's lists).
     */
    public static Report step(ServerLevel level, Live lv, long now, boolean force, List<ServerPlayer> players) {
        Blueprint bp = lv.bp;
        Frame f = lv.frame;
        if (!TrapBuilder.loaded(level, f, bp)) return new Report(lv.machine.state(), 0, 0f, lv.rams.size(), false, false);
        if (!lv.normalized) {
            idle(level, lv);
            lv.normalized = true;
        }
        boolean disarmed = isDisarmed(level, lv) || !CreateConfig.get(CreateConfig.ENABLED);
        boolean fired = lv.machine.state() == TrapClock.State.IDLE && (force || triggered(level, lv, players));
        TrapClock.State before = lv.machine.state();
        TrapClock.State after = lv.machine.look(now, fired, disarmed, bp.runTicks(), bp.cooldownTicks());
        if (before == TrapClock.State.RUNNING && after != TrapClock.State.RUNNING) endRun(level, lv);
        if (before != TrapClock.State.RUNNING && after == TrapClock.State.RUNNING) startRun(level, lv);
        int hits = 0;
        float asked = 0f;
        if (after == TrapClock.State.RUNNING) {
            long since = lv.machine.runAge(now);
            float[] tally = run(level, lv, now, since, players);
            hits = (int) tally[0];
            asked = tally[1];
        } else if (now - lv.lastAssert > 100) {
            idle(level, lv);
            lv.lastAssert = now;
        }
        if (after != TrapClock.State.DISARMED) hint(level, lv, now, players);
        return new Report(after, hits, asked, lv.rams.size(), after == TrapClock.State.DISARMED, fired);
    }

    // ---------------------------------------------------------------- the two ends of a run
    private static void startRun(ServerLevel level, Live lv) {
        lv.taken.clear();
        lv.hit.clear();
        java.util.Arrays.fill(lv.last, TrapClock.Phase.QUIET);
        cutClutches(level, lv, false);
        Vec3 mid = lv.frame.point(0, (lv.bp.length() - 1) / 2.0);
        level.playSound(null, mid.x, lv.frame.origin().getY() + 2, mid.z, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.2f, 0.6f);
        level.playSound(null, mid.x, lv.frame.origin().getY() + 2, mid.z, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0f, 0.5f);
    }

    private static void endRun(ServerLevel level, Live lv) {
        idle(level, lv);
        Vec3 mid = lv.frame.point(0, (lv.bp.length() - 1) / 2.0);
        level.playSound(null, mid.x, lv.frame.origin().getY() + 2, mid.z, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    /** Puts every movable part back to idle: clutches cut, nixie tubes dark, every ram taken away. */
    public static void idle(ServerLevel level, Live lv) {
        DungeonGuard.internal(() -> {
            cutClutches(level, lv, true);
            lights(level, lv, -1, false);
            retract(level, lv, null);
            // after a restart the set of rams is empty but a ram block may still stand: look for them too
            for (int[] r : lv.bp.rams()) {
                BlockPos p = lv.frame.pos(r[0], r[1], r[2]);
                if (level.getBlockState(p).getBlock() == PartResolver.ram().getBlock()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    // ---------------------------------------------------------------- triggers and the lever
    private static boolean powered(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.hasProperty(BlockStateProperties.POWERED) && s.getValue(BlockStateProperties.POWERED);
    }

    /** Is any lever of the trap thrown? */
    public static boolean isDisarmed(ServerLevel level, Live lv) {
        for (Blueprint.Placed l : lv.bp.levers()) if (powered(level, lv.frame.pos(l.dx(), l.dy(), l.dz()))) return true;
        return false;
    }

    private static boolean triggered(ServerLevel level, Live lv, List<ServerPlayer> players) {
        switch (lv.bp.trigger()) {
            case PLATE, TRIPWIRE -> {
                for (Blueprint.Placed c : lv.bp.triggerCells()) if (powered(level, lv.frame.pos(c.dx(), c.dy(), c.dz()))) return true;
                return false;
            }
            default -> {
                AABB inner = lv.frame.box(-1, 1, 1, 3, 1, lv.bp.length() - 2);
                return !inBox(level, inner, players).isEmpty();
            }
        }
    }

    private static boolean hurtable(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && !p.isCreative(); }

    private static List<ServerPlayer> inBox(ServerLevel level, AABB box, List<ServerPlayer> players) {
        List<ServerPlayer> out = new ArrayList<>();
        if (players == null) {
            out.addAll(level.getEntitiesOfClass(ServerPlayer.class, box, TrapRuntime::hurtable));
        } else {
            for (ServerPlayer p : players) if (hurtable(p) && box.contains(p.getX(), p.getY(), p.getZ())) out.add(p);
        }
        return out;
    }

    // ---------------------------------------------------------------- the run
    /** Runs the bands and the belt; returns {hits, damage asked}. */
    private static float[] run(ServerLevel level, Live lv, long now, long since, List<ServerPlayer> players) {
        Blueprint bp = lv.bp;
        int hits = 0;
        float asked = 0f;
        for (int i = 0; i < bp.bands().size(); i++) {
            Blueprint.Band band = bp.bands().get(i);
            TrapClock.Phase ph = TrapClock.phase(since, band);
            TrapClock.Phase old = lv.last[i];
            if (ph != old) {
                lv.last[i] = ph;
                changed(level, lv, i, band, old, ph);
            }
            if (ph == TrapClock.Phase.WARN && now % 4 == 0) show(level, lv, band, false);
            if (ph == TrapClock.Phase.STRIKE) {
                if (now % 2 == 0) show(level, lv, band, true);
                float[] r = strike(level, lv, i, band, since, players);
                hits += (int) r[0];
                asked += r[1];
            }
        }
        if (bp.conveyor() != null) belt(level, lv, players);
        return new float[] {hits, asked};
    }

    /** A band changed phase: light the nixie tubes, move the rams, make the noise of the change. */
    private static void changed(ServerLevel level, Live lv, int i, Blueprint.Band band, TrapClock.Phase from, TrapClock.Phase to) {
        Vec3 c = lv.frame.point((band.xMin() + band.xMax()) / 2.0, (band.zMin() + band.zMax()) / 2.0);
        double y = lv.frame.origin().getY() + 1.5;
        DungeonGuard.internal(() -> {
            if (band.lightIndex() >= 0) lights(level, lv, band.lightIndex(), to != TrapClock.Phase.QUIET);
            if (band.kind() == HazardKind.CRUSH && !lv.bp.rams().isEmpty()) {
                if (to == TrapClock.Phase.STRIKE) extend(level, lv, band);
                else retract(level, lv, band);
            }
        });
        if (to == TrapClock.Phase.WARN) {
            level.playSound(null, c.x, y, c.z, warnSound(band.kind()), SoundSource.BLOCKS, 1.0f, 0.8f);
        } else if (to == TrapClock.Phase.STRIKE) {
            level.playSound(null, c.x, y, c.z, strikeSound(band.kind()), SoundSource.BLOCKS, 1.4f, band.kind() == HazardKind.CRUSH ? 0.6f : 1.0f);
        } else if (from == TrapClock.Phase.STRIKE && band.kind() == HazardKind.CRUSH) {
            level.playSound(null, c.x, y, c.z, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.9f, 0.9f);
        }
    }

    private static SoundEvent warnSound(HazardKind k) {
        return switch (k) {
            case SLASH -> SoundEvents.GRINDSTONE_USE;
            case CRUSH -> SoundEvents.CHAIN_HIT;
            case BORE -> SoundEvents.IRON_GOLEM_ATTACK;
            case SCALD -> SoundEvents.LAVA_POP;
            case SHOCK -> SoundEvents.BEACON_ACTIVATE;
        };
    }

    private static SoundEvent strikeSound(HazardKind k) {
        return switch (k) {
            case SLASH -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case CRUSH -> SoundEvents.PISTON_EXTEND;
            case BORE -> SoundEvents.IRON_GOLEM_DAMAGE;
            case SCALD -> SoundEvents.BUCKET_EMPTY_LAVA;
            case SHOCK -> SoundEvents.LIGHTNING_BOLT_IMPACT;
        };
    }

    /** Particles of a warning or a strike along the band. */
    private static void show(ServerLevel level, Live lv, Blueprint.Band band, boolean strike) {
        ParticleOptions p = switch (band.kind()) {
            case SLASH -> strike ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.CRIT;
            case CRUSH -> strike ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
            case BORE -> strike ? ParticleTypes.CRIT : ParticleTypes.SMOKE;
            case SCALD -> strike ? ParticleTypes.FALLING_LAVA : ParticleTypes.DRIPPING_LAVA;
            case SHOCK -> ParticleTypes.ELECTRIC_SPARK;
        };
        double zMid = (band.zMin() + band.zMax()) / 2.0, xMid = (band.xMin() + band.xMax()) / 2.0;
        Vec3 c = lv.frame.point(xMid, zMid);
        double width = Math.max(0.4, (band.xMax() - band.xMin() + 1) / 2.0);
        int n = strike ? 6 : 3;
        double top = band.kind() == HazardKind.SCALD ? band.yMax() + 0.9 : 1.4;
        level.sendParticles(p, c.x, lv.frame.origin().getY() + top, c.z, n, width, 0.5, width, strike ? 0.05 : 0.01);
    }

    /** Hits everyone in the band who was not already hit in this cycle. Returns {hits, damage asked}. */
    private static float[] strike(ServerLevel level, Live lv, int index, Blueprint.Band band, long since, List<ServerPlayer> players) {
        AABB box = lv.frame.box(band.xMin(), band.xMax(), 1, band.yMax(), band.zMin(), band.zMax());
        long cycle = TrapClock.cycle(since, band);
        int hits = 0;
        float asked = 0f;
        for (ServerPlayer p : inBox(level, box, players)) {
            String key = p.getUUID() + "|" + index;
            Long prev = lv.hit.get(key);
            if (prev != null && prev == cycle) continue;
            lv.hit.put(key, cycle);
            float takenSoFar = lv.taken.getOrDefault(p.getUUID(), 0f);
            double wanted = TrapRules.damage(band.kind(), lv.site.depth, lv.site.threat, CreateConfig.get(CreateConfig.DAMAGE)) * band.damageFactor();
            float dmg = TrapRules.safeDamage(wanted, p.getHealth(), p.getMaxHealth(), takenSoFar);
            if (dmg > 0f) {
                float before = p.getHealth() + p.getAbsorptionAmount();
                p.hurt(source(level, band.kind()), dmg);
                float lost = Math.max(0f, before - (p.getHealth() + p.getAbsorptionAmount()));
                lv.taken.merge(p.getUUID(), lost, Float::sum);
                if (band.kind().fire() && p.getHealth() > p.getMaxHealth() * 0.5f) p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(), 40));
                hits++;
                asked += dmg;
            }
            throwOut(lv, band, p);
        }
        return new float[] {hits, asked};
    }

    /** Throws a victim out of the band, along the lane, towards the nearer end. */
    private static void throwOut(Live lv, Blueprint.Band band, ServerPlayer p) {
        double[] l = lv.frame.local(p.position());
        double mid = (band.zMin() + band.zMax()) / 2.0;
        double sign = l[2] > mid ? 1.0 : -1.0;
        Vec3 dir = lv.frame.forwardVec().scale(sign * band.kind().throwStrength());
        p.setDeltaMovement(dir.x, 0.25, dir.z);
        p.hurtMarked = true;
    }

    /** The damage source of a hit: Create's own damage type when the mod has one, a vanilla source otherwise. */
    public static DamageSource source(ServerLevel level, HazardKind kind) {
        try {
            if (PartResolver.createLoaded()) {
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(CreateIds.MOD_ID, CreateIds.damageTypePath(kind));
                Registry<DamageType> reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                Optional<Holder.Reference<DamageType>> h = reg.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, rl));
                if (h.isPresent()) return new DamageSource(h.get());
                PartResolver.logOnce("damage:" + kind, "the damage type create:" + CreateIds.damageTypePath(kind) + " does not exist; a vanilla source is used for " + kind.id());
            }
        } catch (RuntimeException e) {
            PartResolver.logOnce("damage-err:" + kind, "looking up the damage type of " + kind.id() + " failed: " + e);
        }
        return kind == HazardKind.SCALD ? level.damageSources().lava() : level.damageSources().generic();
    }

    // ---------------------------------------------------------------- the belt
    private static void belt(ServerLevel level, Live lv, List<ServerPlayer> players) {
        Blueprint.Conveyor c = lv.bp.conveyor();
        AABB box = lv.frame.box(0, 0, 1, 2, c.zMin(), c.zMax());
        for (ServerPlayer p : inBox(level, box, players)) {
            if (!p.onGround()) continue;
            Vec3 add = lv.frame.forwardVec().scale(c.strength());
            Vec3 m = p.getDeltaMovement();
            p.setDeltaMovement(m.x + add.x, m.y, m.z + add.z);
            p.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- the parts the runtime moves
    /** Cuts (redstone block in place) or opens (inert block) every clutch of the trap. */
    private static void cutClutches(ServerLevel level, Live lv, boolean cut) {
        BlockState want = PartResolver.power(cut);
        for (Blueprint.PowerCell pc : lv.bp.power()) {
            if (pc.role() != Blueprint.Role.CLUTCH) continue;
            BlockPos p = lv.frame.pos(pc.dx(), pc.dy(), pc.dz());
            if (level.getBlockState(p).getBlock() != want.getBlock()) level.setBlock(p, want, 3);
        }
    }

    /** Lights (or darkens) the nixie group {@code index}; {@code -1} = every group. */
    private static void lights(ServerLevel level, Live lv, int index, boolean on) {
        BlockState want = PartResolver.power(on);
        for (Blueprint.PowerCell pc : lv.bp.power()) {
            if (pc.role() != Blueprint.Role.LIGHT || (index >= 0 && pc.index() != index)) continue;
            BlockPos p = lv.frame.pos(pc.dx(), pc.dy(), pc.dz());
            if (level.getBlockState(p).getBlock() != want.getBlock()) level.setBlock(p, want, 3);
        }
    }

    /** Puts a ram into each free cell of a band's edge columns. A cell with anything standing in it is skipped. */
    private static void extend(ServerLevel level, Live lv, Blueprint.Band band) {
        BlockState ram = PartResolver.ram();
        for (int[] r : lv.bp.rams()) {
            if (r[2] < band.zMin() || r[2] > band.zMax()) continue;
            BlockPos p = lv.frame.pos(r[0], r[1], r[2]);
            if (!level.getBlockState(p).isAir()) continue;
            if (!level.getEntities((Entity) null, new AABB(p), e -> !(e instanceof net.minecraft.world.entity.item.ItemEntity)).isEmpty()) continue;
            if (level.setBlock(p, ram, 3)) lv.rams.add(p);
        }
    }

    /** Takes the rams of one band (or of all, with null) away again. Only blocks the runtime put there are touched, plus any ram block found at a ram cell. */
    private static void retract(ServerLevel level, Live lv, Blueprint.Band band) {
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        Block ramBlock = PartResolver.ram().getBlock();
        List<BlockPos> gone = new ArrayList<>();
        for (BlockPos p : lv.rams) {
            if (band != null) {
                int dz = (int) Math.round(lv.frame.local(Vec3.atCenterOf(p))[2]);
                if (dz < band.zMin() || dz > band.zMax()) continue;
            }
            if (level.getBlockState(p).getBlock() == ramBlock) level.setBlock(p, air, 3);
            gone.add(p);
        }
        lv.rams.removeAll(gone);
    }

    // ---------------------------------------------------------------- the hint
    private static void hint(ServerLevel level, Live lv, long now, List<ServerPlayer> players) {
        if (now % 20 != 0) return;
        Vec3 a = lv.frame.point(0, 0), b = lv.frame.point(0, lv.bp.length() - 1);
        double r2 = 8.0 * 8.0;
        List<ServerPlayer> list = players != null ? players : level.players();
        for (ServerPlayer p : list) {
            if (!hurtable(p)) continue;
            double da = (p.getX() - a.x) * (p.getX() - a.x) + (p.getZ() - a.z) * (p.getZ() - a.z), db = (p.getX() - b.x) * (p.getX() - b.x) + (p.getZ() - b.z) * (p.getZ() - b.z);
            if (Math.abs(p.getY() - lv.frame.origin().getY()) > 4 || (da > r2 && db > r2)) continue;
            Long prev = lv.hinted.get(p.getUUID());
            if (prev != null && now - prev < 600) continue;
            lv.hinted.put(p.getUUID(), now);
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.create.hint", "Gears grind behind the walls. A lever on the wall would stop them."), true);
        }
    }
}
