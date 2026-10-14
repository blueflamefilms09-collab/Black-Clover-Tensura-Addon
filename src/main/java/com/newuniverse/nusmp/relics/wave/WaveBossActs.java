package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The abilities of the three boss relics (server side). Dante's Gravity Crown: Weight of the Crown (a gravity well) and Lift. Zenon's Spatial Ring:
 * Rift Step and Twin Gates. The Artifact Mimic: Bite and Hoard. Every number lives in {@link WaveBossRules}; the clients are told what to animate
 * through {@link WaveBossNet}. Nothing here kills outright: damage goes through {@link Fx#damage}, which caps it for players.
 */
final class WaveBossActs {
    private WaveBossActs() {}

    private static final int VIOLET = 0x7B4BCF, INDIGO = 0x5B7BFF;
    private static final String HOARD_KEY = "wb_hoard";

    // ================================================================ Dante's Gravity Crown
    /** Weight of the Crown: a well of gravity that drags foes in, slows and sinks them for four seconds and crushes them as it closes. */
    static void weight(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        final Vec3 c = p.position();
        Fx.lendSkills(p, WaveBossRules.WELL_TICKS + 20, WaveBossSkills.GRAVITY_FIELD);          // the crown merges Tensura's own field into the well
        WaveBossNet.cue(p, WaveBossNet.WHICH_CROWN, WaveBossNet.KIND_WELL);
        l.playSound(null, BlockPos.containing(c), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6f, 0.45f);
        VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), 0xFF000000 | VIOLET, WaveBossRules.WELL_TICKS, (float) (WaveBossRules.WELL_RADIUS / 1.6));
        Fx.ring(l, c, WaveBossRules.WELL_RADIUS, VIOLET);
        SpellRuntime.zone(l, WaveBossRules.WELL_TICKS, WaveBossRules.WELL_STEP, safe("gravity well", age -> wellStep(p, l, c, age)));
    }

    private static void wellStep(ServerPlayer p, ServerLevel l, Vec3 c, int age) {
        if (!p.isAlive() || p.isRemoved()) return;
        double r = WaveBossRules.WELL_RADIUS;
        if (age > 0 && age % 20 == 0) l.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1.0, c.z, 24, r * 0.4, 0.6, r * 0.4, 0.05);
        double reach = r + Math.sqrt(p.distanceToSqr(c.x, c.y, c.z));                        // the well stays where it was cast; the caster may walk away
        boolean closing = WaveBossRules.closing(age);
        for (LivingEntity t : Fx.foesAround(p, Math.min(32.0, reach))) {
            double dx = c.x - t.getX(), dz = c.z - t.getZ(), dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > r || Math.abs(t.getY() - c.y) > 6.0) continue;
            int amp = WaveBossRules.slowAmp(dist, r);
            if (amp >= 0) Fx.addEffect(t, "minecraft:slowness", WaveBossRules.WELL_STEP * 2 + 2, amp);
            double pull = WaveBossRules.pull(dist, r);
            Vec3 v = t.getDeltaMovement();
            double nx = v.x, nz = v.z, ny = v.y;
            if (pull > 0.0 && dist > 1.0e-3) {
                nx += dx / dist * pull * WaveBossRules.WELL_STEP * 0.5;
                nz += dz / dist * pull * WaveBossRules.WELL_STEP * 0.5;
            }
            double h = Math.sqrt(nx * nx + nz * nz);
            if (h > WaveBossRules.SPEED_CAP) {
                nx *= WaveBossRules.SPEED_CAP / h;
                nz *= WaveBossRules.SPEED_CAP / h;
            }
            if (!t.onGround()) ny = Math.min(ny, WaveBossRules.sink(dist, r));
            t.setDeltaMovement(nx, ny, nz);
            t.hurtMarked = true;
            t.fallDistance = 0;
            if (closing && dist <= r * WaveBossRules.CRUSH_FRACTION) Fx.damage(p, t, WaveBossRules.CRUSH, WaveBossSkills.GRAVITY_PRESS);
        }
        if (closing) Fx.burst(l, c, VIOLET);
    }

    /** Lift: Tensura's gravity flight lent for seven seconds, with a slow fall and a high jump to go with it. */
    static void lift(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        int ticks = 140;
        boolean flight = Fx.lendSkills(p, ticks, WaveBossSkills.GRAVITY_FLIGHT) > 0;
        Fx.addEffect(p, "minecraft:slow_falling", ticks, 0);
        Fx.addEffect(p, "minecraft:jump_boost", ticks, 1);
        WaveBossNet.cue(p, WaveBossNet.WHICH_CROWN, WaveBossNet.KIND_WELL);
        Fx.burst(l, p.position(), VIOLET);
        Fx.say(p, flight ? "The crown lifts the weight off you for 7 seconds." : "The crown makes you feather-light for 7 seconds.");
    }

    // ================================================================ Zenon's Spatial Ring
    /** Rift Step: a short blink where you look (10 blocks, less if the room is short), through a flash of rift on both ends. */
    static void riftStep(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Fx.lendSkills(p, 40, WaveBossSkills.SPATIAL_MOTION);
        Vec3 from = p.position();
        boolean ok = false;
        for (double range : WaveBossRules.STEP_TRIES) {
            if (Fx.teleportLook(p, range)) {
                ok = true;
                break;
            }
        }
        WaveBossNet.cue(p, WaveBossNet.WHICH_RING, WaveBossNet.KIND_STEP);
        if (!ok) {
            Fx.say(p, "The rift finds no room to step into.");
            return;
        }
        Vec3 to = p.position();
        VfxSpawn.send(l, VfxShape.SPACE_PORTAL, from, to, 0xFF000000 | INDIGO, 14, 1.0f);                // 'to' is a point: the gate faces it
        VfxSpawn.send(l, VfxShape.SPACE_PORTAL, to, from, 0xFF000000 | INDIGO, 14, 1.0f);
        Fx.burst(l, from.add(0, 0.2, 0), INDIGO);
        Fx.burst(l, to.add(0, 0.2, 0), INDIGO);
        l.playSound(null, BlockPos.containing(to), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    /** The twin gates of one player: where they stand, how long, and the level they are in. */
    private record Pair(ResourceKey<Level> dim, double[] a, double[] b, long until) {}

    private static final Map<UUID, Pair> PAIRS = new HashMap<>();
    private static final String K_GATE = "wb_gate_use";

    /** Twin Gates: two linked gates for fifteen seconds, one just ahead and one up to 12 blocks away; you and your pets step through either. */
    static void gates(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        double[] nearXZ = WaveBossRules.ahead(p.getX(), p.getZ(), look.x, look.z, WaveBossRules.GATE_NEAR);
        Vec3 first = landing(l, p, nearXZ[0], p.getY(), nearXZ[1]);
        Vec3 second = null;
        for (double d : WaveBossRules.farTries()) {
            double[] xz = WaveBossRules.ahead(p.getX(), p.getZ(), look.x, look.z, d);
            Vec3 spot = landing(l, p, xz[0], p.getY(), xz[1]);
            if (spot != null && wallFree(l, p, xz[0], spot.y + 1.0, xz[1])) {
                second = spot;
                break;
            }
        }
        if (first == null || second == null) {
            Fx.say(p, "There is no ground to open the gates on.");
            return;
        }
        WaveBossNet.cue(p, WaveBossNet.WHICH_RING, WaveBossNet.KIND_GATES);
        Fx.lendSkills(p, 100, WaveBossSkills.WARP_PORTAL);                                    // the ring merges Tensura's warp gate into the pair
        Pair pair = new Pair(l.dimension(), new double[]{first.x, first.y, first.z}, new double[]{second.x, second.y, second.z}, l.getGameTime() + WaveBossRules.GATES_TICKS);
        PAIRS.put(p.getUUID(), pair);
        VfxSpawn.send(l, VfxShape.SPACE_PORTAL, first, second, 0xFF000000 | INDIGO, WaveBossRules.GATES_TICKS, 1.0f);
        VfxSpawn.send(l, VfxShape.SPACE_PORTAL, second, first, 0xFF000000 | INDIGO, WaveBossRules.GATES_TICKS, 1.0f);
        Fx.burst(l, first.add(0, 0.2, 0), INDIGO);
        Fx.burst(l, second.add(0, 0.2, 0), INDIGO);
        l.playSound(null, BlockPos.containing(first), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5f, 1.6f);
        Fx.say(p, "Two gates open for 15 seconds: step into either to come out of the other.");
        SpellRuntime.zone(l, WaveBossRules.GATES_TICKS, 2, safe("twin gates", age -> gateStep(p, l, pair, age)));
    }

    private static void gateStep(ServerPlayer p, ServerLevel l, Pair pair, int age) {
        if (PAIRS.get(p.getUUID()) != pair) return;                                            // replaced by a newer pair, or cleared
        if (!p.isAlive() || p.isRemoved() || p.level().dimension() != pair.dim()) {
            PAIRS.remove(p.getUUID(), pair);
            return;
        }
        long now = l.getGameTime();
        if (age % 10 == 0) {
            for (double[] g : new double[][]{pair.a(), pair.b()}) l.sendParticles(ParticleTypes.REVERSE_PORTAL, g[0], g[1] + 1.0, g[2], 3, 0.3, 0.6, 0.3, 0.02);
        }
        for (int side = 0; side < 2; side++) {
            double[] in = side == 0 ? pair.a() : pair.b(), out = side == 0 ? pair.b() : pair.a();
            AABB box = new AABB(in[0] - 1.0, in[1] - 0.8, in[2] - 1.0, in[0] + 1.0, in[1] + 2.2, in[2] + 1.0);
            List<LivingEntity> near = l.getEntitiesOfClass(LivingEntity.class, box, e -> e == p || (e instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())));
            int moved = 0;
            for (LivingEntity e : near) {
                if (moved >= 4) break;
                if (WaveBossRules.which(e.getX(), e.getY(), e.getZ(), pair.a(), pair.b()) != side + 1) continue;
                if (!WaveBossRules.reentryOk(now, e.getPersistentData().getLong(K_GATE))) continue;
                e.getPersistentData().putLong(K_GATE, now);
                e.teleportTo(out[0], out[1], out[2]);
                e.setDeltaMovement(Vec3.ZERO);
                e.fallDistance = 0;
                e.hurtMarked = true;
                moved++;
                Fx.burst(l, new Vec3(out[0], out[1] + 0.2, out[2]), INDIGO);
                l.playSound(null, BlockPos.containing(out[0], out[1], out[2]), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.5f);
            }
        }
    }

    /** Forgets the gates of this player (death, logout, a stopping server). */
    static void clearGates(UUID id) { PAIRS.remove(id); }

    static void clearAllGates() { PAIRS.clear(); }

    static boolean hasGates(UUID id) { return PAIRS.containsKey(id); }

    // ================================================================ the Artifact Mimic
    /** Bite: the belt opens its jaws (the telegraph), then snaps and lunges at the nearest foe in front, up to four blocks away. */
    static void bite(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        WaveBossNet.cue(p, WaveBossNet.WHICH_MIMIC, WaveBossNet.KIND_BITE);
        l.playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1.0f, 0.6f);
        l.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.5, 0.3, 0.5, 0.05);
        SpellRuntime.later(l, WaveBossRules.BITE_DELAY, safe("mimic bite", () -> snap(p, l)));
    }

    private static void snap(ServerPlayer p, ServerLevel l) {
        if (!p.isAlive() || p.isRemoved() || !Relics.worn(p, WaveBoss.ID_MIMIC)) return;
        l.playSound(null, p.blockPosition(), SoundEvents.CHEST_CLOSE, SoundSource.PLAYERS, 1.2f, 0.5f);
        List<LivingEntity> foes = Fx.foesAround(p, WaveBossRules.BITE_REACH + 0.5);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double[][] rel = new double[foes.size()][];
        for (int i = 0; i < rel.length; i++) {
            LivingEntity t = foes.get(i);
            Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            rel[i] = new double[]{at.x, at.y, at.z};
        }
        int pick = WaveBossRules.pickTarget(rel, look.x, look.y, look.z);
        if (pick < 0) return;                                                                  // it snapped at air
        LivingEntity target = foes.get(pick);
        double[] r = rel[pick];
        double dist = Math.sqrt(r[0] * r[0] + r[2] * r[2]);
        if (dist > 1.0e-3) {
            double v = WaveBossRules.lungeSpeed(dist);
            p.setDeltaMovement(r[0] / dist * v, Math.max(p.getDeltaMovement().y, 0.14), r[2] / dist * v);
            p.hurtMarked = true;
        }
        Fx.damage(p, target, WaveBossRules.BITE_DAMAGE, "minecraft:player_attack");
        Fx.addEffect(target, "minecraft:slowness", 40, 1);
        Fx.heal(p, 1.0f);                                                                      // it keeps a bite's worth of what it chews
        l.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.3, 0.3, 0.3, 0.2);
    }

    /** Hoard: opens the belt's twenty-seven slot pocket (the Mimic yawns while you rummage). */
    static void hoard(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        WaveBossNet.cue(p, WaveBossNet.WHICH_MIMIC, WaveBossNet.KIND_YAWN);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.8f, 0.8f);
        Wave6Pockets.open(p, WaveBoss.ID_MIMIC, HOARD_KEY, 3, "The Mimic's Hoard");
    }

    // ================================================================ ground and walls
    /** The floor near (x, y, z): from 3 blocks above to 6 below, the first spot where the player's body fits over something solid; null over a drop or in a fluid. */
    static Vec3 landing(ServerLevel l, Entity e, double x, double y, double z) {
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos b = BlockPos.containing(x, y + dy, z);
            if (!l.hasChunkAt(b)) return null;
            BlockState floor = l.getBlockState(b.below());
            if (floor.isAir() || !floor.getFluidState().isEmpty() || floor.getCollisionShape(l, b.below()).isEmpty()) continue;
            if (!l.getFluidState(b).isEmpty()) continue;
            AABB box = e.getBoundingBox().move(x - e.getX(), b.getY() - e.getY(), z - e.getZ());
            if (l.noCollision(e, box)) return new Vec3(x, b.getY(), z);
        }
        return null;
    }

    /** Is the straight line from the caster's chest to this point free of walls (so the far gate is not behind rock)? */
    private static boolean wallFree(ServerLevel l, ServerPlayer p, double x, double y, double z) {
        Vec3 from = p.getEyePosition(), to = new Vec3(x, y, z);
        BlockHitResult hit = l.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS;
    }

    // ================================================================ guards
    private static java.util.function.IntConsumer safe(String what, java.util.function.IntConsumer step) {
        return age -> {
            try {
                step.accept(age);
            } catch (RuntimeException failed) {
                org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] boss relics: {} failed", what, failed);
            }
        };
    }

    private static Runnable safe(String what, Runnable run) {
        return () -> {
            try {
                run.run();
            } catch (RuntimeException failed) {
                org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] boss relics: {} failed", what, failed);
            }
        };
    }
}
