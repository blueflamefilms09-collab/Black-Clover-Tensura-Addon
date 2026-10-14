package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Zenon's Bone Magic and Beelzebub's Spatial Magic as plain static functions on any {@link ZogratisBoss}: Zenon uses them, and Lucius
 * (who took the Triad's gifts into himself) calls the same four public ones. Everything is bounded: a spear is a point that moves along
 * a clipped ray, a volley shares one hit budget per victim, a combo is a short chain of delayed slashes, a garden queues at most a few
 * hundred blocks through the boss's ledger, and the cube is a {@link SpellSeal}.
 * <ul>
 *   <li>{@link #boneSpears}: dense bone spears grown out of the body, launched one after another in a narrow fan, each a small hit.</li>
 *   <li>{@link #boneBlades}: bone blades from the forearms in a close combo, with a blink in if the target is out of reach.</li>
 *   <li>{@link #spatialBlink}: a visible entry portal, a short blink time, a visible exit portal.</li>
 *   <li>{@link #spatialDomain}: Spatial Mana Domination, a cubic domain that severs the mana of everyone inside ({@link SpellSeal}).</li>
 *   <li>Zenon's own: {@link #boneGarden}, {@link #boneLance}, {@link #riftBackstab}, {@link #portalLink}, {@link #warpBarrage},
 *       {@link #shardBurst}, {@link #slip}.</li>
 * </ul>
 */
public final class ZenonMoves {
    private ZenonMoves() {}

    private static final int RGB = DevilStyle.color(Devil.BEELZEBUB), ACCENT = DevilStyle.accent(Devil.BEELZEBUB);
    /** ARGB for the VFX payloads. */
    static final int INDIGO = 0xFF000000 | ACCENT, BONE = 0xFFF4F1E6;
    private static final int BONE_RGB = 0xE9E4D4;
    private static final double PORTAL_RADIUS = 0.9, LANCE_SPEED = 2.2, LANCE_REACH = 0.8, LINK_SPEED = 1.5, WARP_SPEED = 1.3;
    private static final int LANCE_LIFE = 30, HOP_TICKS = 8, MAX_BLADE_CHASES = 2, MAX_GARDEN_VFX = 4;
    /** How far the Dense Bone Lance flies if nothing stops it (the wind-up draws this far). */
    static final double LANCE_RANGE = LANCE_SPEED * LANCE_LIFE;

    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of Zenon's goes through here: a step
     * that throws is logged once and that zone or task stops, it never takes the tick down.
     */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] Zenon's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ the four moves other bosses reuse
    /**
     * 'count' dense bone spears grown out of the boss's shoulders (an arch of bone, shown for 5 ticks as it grows) and launched at
     * 'target' two ticks apart in a narrow fan, each re-aimed as it leaves. A spear is a point that flies 1.6 blocks a tick for 34
     * ticks, stops at the first wall and hurts the first foe it touches for 'damage' (a plain blow: a raised shield stops it). One volley
     * hits any one victim at most three times, whatever the count; the rest fly on past.
     */
    public static void boneSpears(ZogratisBoss boss, LivingEntity target, int count, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null) return;
        final int n = Math.max(1, Math.min(BoneMath.MAX_SPEARS, count));
        final VolleyBudget budget = new VolleyBudget(BoneMath.MAX_HITS_PER_VICTIM);
        final Vec3[] lastAim = {null};
        final int vfxStride = n > 5 ? 2 : 1;
        for (int i = 0; i < n; i++) {
            Vec3 g = growthWorld(boss, i, n);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), g.x, g.y, g.z, 5, 0.12, 0.12, 0.12, 0.02);
            sl.sendParticles(ParticleTypes.CRIT, g.x, g.y, g.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
        for (int i = 0; i < n; i++) {
            final int k = i;
            boss.later(BoneMath.GROW_TICKS + i * BoneMath.SPEAR_STAGGER, safe("bone spear", () -> {
                Vec3 from = growthWorld(boss, k, n);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(from) : lastAim[0];
                if (aim == null || aim.lengthSqr() < 1e-4) aim = boss.getViewVector(1f);
                lastAim[0] = aim;
                Vec3 dir = rotateY(aim.normalize(), BoneMath.spread(k, n));
                launch(sl, boss, new BoneSpear(from, dir.scale(BoneMath.SPEAR_SPEED), BoneMath.SPEAR_LIFE, damage, budget, (float) BoneMath.SPEAR_REACH), k % vfxStride == 0, 0.7f);
            }));
        }
    }

    /**
     * A combo of 'count' bone blade slashes at 'target', six ticks apart: bone blades grow from both forearms and cut a cone of 4.2 blocks
     * in front of the boss (120 degrees), each slash hurting everyone in the cone for 'damage' and shoving them back half a block.
     * If the target is out of reach the boss blinks in first (a visible portal), and again if the target backs away, at most twice;
     * stepping out of the cone or back out of reach is the answer.
     */
    public static void boneBlades(ZogratisBoss boss, LivingEntity target, int count, float damage) {
        if (!(boss.level() instanceof ServerLevel) || target == null) return;
        sendBlades(boss, target, Math.max(1, Math.min(6, count)), damage, 0);
    }

    private static void sendBlades(ZogratisBoss boss, LivingEntity target, int left, float damage, int chases) {
        if (left <= 0 || !boss.isAlive() || boss.isRemoved() || !target.isAlive()) return;
        if (boss.distanceTo(target) > BoneMath.BLADE_REACH * 0.85 && chases < MAX_BLADE_CHASES) {
            Vec3 spot = approachSpot(boss, target);
            if (spot != null) {
                spatialBlink(boss, spot);
                boss.later(PortalMath.BLINK_TICKS + 3, safe("bone blades", () -> sendBlades(boss, target, left, damage, chases + 1)));
                return;
            }
        }
        slash(boss, target, damage, left == 1);
        boss.later(BoneMath.BLADE_GAP, safe("bone blades", () -> sendBlades(boss, target, left - 1, damage, chases)));
    }

    /** One slash: faces the target, draws the arc, and hurts every foe in the cone. */
    private static void slash(ZogratisBoss boss, LivingEntity target, float damage, boolean finisher) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 o = boss.position();
        Vec3 flat = target.position().subtract(o).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 1e-4 ? boss.getViewVector(1f).multiply(1, 0, 1) : flat;
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        boss.lookAt(target, 360f, 360f);
        boss.swing(InteractionHand.MAIN_HAND);
        Vec3 hand = o.add(0, 1.3, 0).add(dir.scale(0.5));
        VfxSpawn.send(sl, VfxShape.WIND_SLASH, hand, hand.add(dir.scale(BoneMath.BLADE_REACH)), BONE, 7, 1.1f);
        Tells.line(sl, hand.add(rotateY(dir, 1.0).scale(BoneMath.BLADE_REACH)), hand.add(rotateY(dir, -1.0).scale(BoneMath.BLADE_REACH)), BONE_RGB);
        sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4f, 0.8f);
        for (LivingEntity e : boss.foesAround(o, BoneMath.BLADE_REACH + 1.5)) {
            if (Math.abs(e.getY() - o.y) > 3.0) continue;
            if (!BoneMath.inCone(o.x, o.z, dir.x, dir.z, e.getX(), e.getZ(), BoneMath.BLADE_REACH + e.getBbWidth() * 0.5, BoneMath.BLADE_HALF_ARC)) continue;
            if (boss.strike(e, damage, false)) e.knockback(0.45, o.x - e.getX(), o.z - e.getZ());
        }
        if (finisher) {
            Vec3 tip = hand.add(dir.scale(BoneMath.BLADE_REACH * 0.7));
            VfxSpawn.send(sl, VfxShape.BONE_FX3, tip, tip, BONE, 12, 0.5f);
        }
    }

    /** A spot 2.4 blocks from the target on the line to the boss, with room to stand. Null if there is none. */
    private static Vec3 approachSpot(ZogratisBoss boss, LivingEntity target) {
        Vec3 away = boss.position().subtract(target.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
        return freeSpot(boss, target.position().add(away.normalize().scale(BoneMath.DASH_STOP)));
    }

    /**
     * Teleports the boss to 'to' through a pair of portals: an entry portal opens where it stands and an exit portal opens at the
     * destination, both visible, and {@link PortalMath#BLINK_TICKS} ticks later it steps through. If there is no room at the
     * destination (or beside it) it stays where it is. A boss that dies in the meantime does not go.
     */
    public static void spatialBlink(ZogratisBoss boss, Vec3 to) {
        if (!(boss.level() instanceof ServerLevel sl) || to == null) return;
        Vec3 dest = freeSpot(boss, to);
        if (dest == null) return;
        Vec3 from = boss.position();
        portal(sl, from, dest, PortalMath.BLINK_TICKS + 10);
        portal(sl, dest, from, PortalMath.BLINK_TICKS + 10);
        boss.later(PortalMath.BLINK_TICKS, safe("spatial blink", () -> boss.blinkTo(dest)));
    }

    /**
     * Spatial Mana Domination (Absolute Space): a cubic domain of warped space centred on 'centre' (a point on the ground), 'half'
     * blocks from the middle to each wall (3..24), standing 'ticks' ticks. For the first 16 ticks it only closes; after that everyone
     * inside who is not of the Zogratis family is cut off from mana ({@link SpellSeal}): no grimoire spells, no Tensura skills, spell
     * damage cut to a fifth. The cube has visible walls, can be walked out of, and breaks if the caster is hurt enough.
     */
    public static void spatialDomain(ZogratisBoss boss, Vec3 centre, double half, int ticks) {
        if (!(boss.level() instanceof ServerLevel sl) || centre == null) return;
        SpellSeal.seal(sl, centre, half, ticks, boss.getUUID());
        ZogratisVfx.crack(sl, centre.add(0, 0.1, 0), centre.add(0, 1.1, 0), half, 60, 0xB8E8FF, true);
    }

    // ================================================================ Zenon's own moves
    /**
     * Bone Garden: bone erupts at every spot of 'spots' (a ring of {@value BoneMath#GARDEN_RADIUS} blocks each, shown in the wind-up):
     * everyone inside takes 'damage' and is thrown up. With 'again' (the Gates) a second burst follows six ticks later. If the
     * griefBlocks rule allows it, bone columns grow round each spot, just outside the ring; the cells are returned for the boss to
     * plant (no more than {@link BoneMath#GROWTH_CELL_CAP}) and to retract later.
     */
    public static List<BlockPos> boneGarden(ZogratisBoss boss, List<Vec3> spots, float damage, boolean again) {
        List<BlockPos> cells = new ArrayList<>();
        if (!(boss.level() instanceof ServerLevel sl) || spots == null || spots.isEmpty()) return cells;
        double phase = boss.getRandom().nextDouble() * Math.PI * 2;
        int drawn = 0;
        for (Vec3 s : spots) {
            erupt(boss, sl, s, damage);
            if (again) boss.later(6, safe("bone garden", () -> erupt(boss, sl, s, damage * 0.6f)));
            if (drawn++ < MAX_GARDEN_VFX) {
                VfxSpawn.send(sl, VfxShape.BONE_FX2, s, s.add(0, 1, 0), BONE, 40, (float) BoneMath.GARDEN_RADIUS);
                ZogratisVfx.spaceShatter(sl, s, BoneMath.GARDEN_RADIUS, 0xB8E8FF);
            }
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2f, 1.3f);
        if (!boss.blocksMayChange()) return cells;
        int idx = 0;
        for (Vec3 s : spots) {
            List<double[]> bases = BoneMath.columnBases(s.x, s.z, phase + idx++);
            for (int k = 0; k < bases.size(); k++) {
                if (cells.size() >= BoneMath.GROWTH_CELL_CAP) return cells;
                Vec3 g = ZogratisBoss.ground(sl, new Vec3(bases.get(k)[0], s.y, bases.get(k)[1]));
                BlockPos base = BlockPos.containing(g);
                for (int h = 0; h < BoneMath.columnHeight(k); h++) {
                    BlockPos cell = base.above(h);
                    BlockState st = sl.getBlockState(cell);
                    if (!(st.isAir() || st.canBeReplaced()) || !st.getFluidState().isEmpty() || !sl.getBlockState(base.below()).blocksMotion()) break;
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    private static void erupt(ZogratisBoss boss, ServerLevel sl, Vec3 s, float damage) {
        Tells.burst(sl, s.add(0, 0.8, 0), BONE_RGB, 26);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), s.x, s.y + 0.4, s.z, 24, BoneMath.GARDEN_RADIUS * 0.5, 0.3, BoneMath.GARDEN_RADIUS * 0.5, 0.15);
        for (LivingEntity e : boss.foesAround(s, BoneMath.GARDEN_RADIUS + 2)) {
            double dx = e.getX() - s.x, dz = e.getZ() - s.z;
            if (dx * dx + dz * dz > BoneMath.GARDEN_RADIUS * BoneMath.GARDEN_RADIUS || Math.abs(e.getY() - s.y) > 3.5) continue;
            if (boss.strike(e, damage, false)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.55, 0));
                e.hurtMarked = true;
            }
        }
    }

    /**
     * Dense Bone Lance: one huge near-indestructible lance of bone launched from 'from' along 'dir' (unit vector, the line the wind-up
     * drew). It flies 2.2 blocks a tick for 30 ticks, stops at the first wall and pierces everything in its path once for 'damage',
     * shoving it along the line. The line is locked when the wind-up starts, so stepping off it is the answer.
     */
    public static void boneLance(ZogratisBoss boss, Vec3 from, Vec3 dir, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || from == null || dir == null || dir.lengthSqr() < 1e-6) return;
        Vec3 d = dir.normalize();
        Vec3 start = from.add(d.scale(0.8));
        VfxSpawn.send(sl, VfxShape.BONE_FX1, start, rayEnd(sl, boss, start, d, LANCE_SPEED * LANCE_LIFE), BONE, BoneMath.flightTicks(LANCE_SPEED * LANCE_LIFE, LANCE_SPEED), 1.6f);
        ZogratisVfx.tear(sl, start, rayEnd(sl, boss, start, d, LANCE_SPEED * LANCE_LIFE), 1.6, 14, 0xB8E8FF);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.6f, 1.6f);
        BoneSpear lance = new BoneSpear(start, d.scale(LANCE_SPEED), LANCE_LIFE, damage, null, (float) LANCE_REACH);
        lance.pierce = true;
        SpellRuntime.zone(sl, LANCE_LIFE + 2, 1, safe("bone lance", age -> lance.step(sl, boss)));
    }

    /**
     * Rift Backstab: the boss steps through a portal to 'behind' (the spot shown in the wind-up, normally behind the target), cuts a
     * short wide arc at the target three ticks after arriving, and slips away through a second portal eight ticks later. The cut hurts
     * anything within 3.4 blocks in front of him for 'damage': turning and stepping aside after the portal opens is the answer.
     */
    public static void riftBackstab(ZogratisBoss boss, LivingEntity target, Vec3 behind, float damage) {
        if (!(boss.level() instanceof ServerLevel) || target == null) return;
        Vec3 spot = behind != null ? behind : target.position();
        spatialBlink(boss, spot);
        int arrive = PortalMath.BLINK_TICKS + 3;
        boss.later(arrive, safe("rift backstab", () -> {
            if (!(boss.level() instanceof ServerLevel s2)) return;
            Vec3 o = boss.position();
            Vec3 to = (target.isAlive() ? target.position() : o.add(boss.getViewVector(1f))).subtract(o).multiply(1, 0, 1);
            Vec3 dir = to.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : to.normalize();
            boss.lookAt(target, 360f, 360f);
            boss.swing(InteractionHand.MAIN_HAND);
            Vec3 hand = o.add(0, 1.3, 0).add(dir.scale(0.5));
            VfxSpawn.send(s2, VfxShape.WIND_SLASH, hand, hand.add(dir.scale(3.4)), BONE, 7, 1.3f);
            VfxSpawn.send(s2, VfxShape.BONE_FX3, hand.add(dir.scale(1.6)), hand.add(dir.scale(1.6)), BONE, 12, 0.5f);
            s2.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5f, 0.7f);
            for (LivingEntity e : boss.foesAround(o, 4.6)) {
                if (Math.abs(e.getY() - o.y) > 3.0) continue;
                if (BoneMath.inCone(o.x, o.z, dir.x, dir.z, e.getX(), e.getZ(), 3.4 + e.getBbWidth() * 0.5, Math.toRadians(70)) && boss.strike(e, damage, false))
                    e.knockback(0.6, o.x - e.getX(), o.z - e.getZ());
            }
        }));
        boss.later(arrive + 8, safe("rift backstab", () -> {
            if (!target.isAlive()) return;
            double[] r = PortalMath.retreat(boss.getX(), boss.getZ(), target.getX(), target.getZ(), 8.5 + boss.getRandom().nextDouble() * 2);
            spatialBlink(boss, ZogratisBoss.ground(boss.level(), new Vec3(r[0], boss.getY(), r[1])));
        }));
    }

    /**
     * Portal Link: 'pairs' entry portals open in an arc in front of the boss and as many exit portals round the target (6 to 9
     * blocks away), all visible, with a line from each exit to where the target stands. Eighteen ticks later a bone spear enters each
     * entry portal and comes out of its exit, three ticks apart, aimed at wherever the target is by then. Each spear flies 1.5 blocks
     * a tick, hurts the first foe it touches for 'damage' and hits any one victim at most twice. With 'waves' above one, the portals
     * chain: the next wave opens as the last spears leave.
     */
    public static void portalLink(ZogratisBoss boss, LivingEntity target, int pairs, float damage, int waves) {
        if (!(boss.level() instanceof ServerLevel) || target == null) return;
        linkWave(boss, target, Math.max(1, Math.min(6, pairs)), damage, Math.max(1, Math.min(3, waves)), boss.getRandom().nextDouble() * Math.PI * 2);
    }

    private static void linkWave(ZogratisBoss boss, LivingEntity target, int pairs, float damage, int waves, double phase) {
        if (!(boss.level() instanceof ServerLevel sl) || !boss.isAlive() || !target.isAlive()) return;
        Vec3 t = target.position();
        List<double[]> entries = PortalMath.entryArc(boss.getX(), boss.getZ(), boss.getYRot(), pairs, 1.7, 2.2);
        List<double[]> exits = PortalMath.exitPoints(pairs, t.x, t.z, 6, 9, phase);
        final VolleyBudget budget = new VolleyBudget(2);
        final Vec3[] in = new Vec3[pairs], out = new Vec3[pairs];
        for (int i = 0; i < pairs; i++) {
            in[i] = ZogratisBoss.ground(sl, new Vec3(entries.get(i)[0], boss.getY(), entries.get(i)[1]));
            out[i] = ZogratisBoss.ground(sl, new Vec3(exits.get(i)[0], t.y, exits.get(i)[1]));
            portal(sl, in[i], out[i], PortalMath.EMERGE_TICKS + 10);
            portal(sl, out[i], t, PortalMath.EMERGE_TICKS + 10);
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.6f, 0.5f);
        SpellRuntime.zone(sl, PortalMath.EMERGE_TICKS - 2, 3, safe("portal link tell", age -> {
            if (!boss.isAlive() || !target.isAlive()) return;
            Vec3 c = target.getBoundingBox().getCenter();
            for (Vec3 o : out) Tells.line(sl, o.add(0, 1.4, 0), c, RGB);
        }));
        for (int i = 0; i < pairs; i++) {
            final int k = i;
            boss.later(PortalMath.EMERGE_TICKS + i * 3, safe("portal link", () -> {
                if (!target.isAlive()) return;
                Vec3 gate = in[k].add(0, 1.3, 0);
                VfxSpawn.send(sl, VfxShape.BONE_FX1, boss.position().add(0, boss.getBbHeight() * 0.7, 0), gate, BONE, 4, 0.6f);
                Tells.burst(sl, gate, ACCENT, 8);
                Vec3 from = out[k].add(0, 1.4, 0);
                Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
                if (aim.lengthSqr() < 1e-4) return;
                launch(sl, boss, new BoneSpear(from, aim.normalize().scale(LINK_SPEED), BoneMath.SPEAR_LIFE, damage, budget, (float) BoneMath.SPEAR_REACH), true, 0.7f);
            }));
        }
        if (waves > 1) boss.later(PortalMath.EMERGE_TICKS + pairs * 3 + 10, safe("portal chain", () -> linkWave(boss, target, pairs, damage, waves - 1, phase + 1.1)));
    }

    /**
     * Warp Barrage: 'hops' times the boss blinks to a new spot round the target (8 ticks apart, each through a visible portal, each
     * hop a long way from the last) and throws one bone spear from there, two ticks after arriving. The aim line of each hop is drawn
     * as its portal opens. A spear flies 1.3 blocks a tick and hurts the first foe it touches for 'damage'.
     */
    public static void warpBarrage(ZogratisBoss boss, LivingEntity target, int hops, float damage) {
        if (!(boss.level() instanceof ServerLevel) || target == null) return;
        warpHop(boss, target, 0, Math.max(1, Math.min(6, hops)), damage, boss.getRandom().nextDouble() * Math.PI * 2, new VolleyBudget(2));
    }

    private static void warpHop(ZogratisBoss boss, LivingEntity target, int k, int hops, float damage, double start, VolleyBudget budget) {
        if (k >= hops || !(boss.level() instanceof ServerLevel sl) || !boss.isAlive() || !target.isAlive()) return;
        double[] p = PortalMath.onRing(target.getX(), target.getZ(), 7.5 + (k % 2) * 1.5, PortalMath.hopAngle(k, start));
        Vec3 spot = ZogratisBoss.ground(sl, new Vec3(p[0], target.getY(), p[1]));
        Tells.line(sl, spot.add(0, 1.4, 0), target.getBoundingBox().getCenter(), RGB);
        spatialBlink(boss, spot);
        boss.later(PortalMath.BLINK_TICKS + 2, safe("warp throw", () -> {
            if (!target.isAlive()) return;
            Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.75, 0);
            Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
            if (aim.lengthSqr() < 1e-4) return;
            launch(sl, boss, new BoneSpear(from, aim.normalize().scale(WARP_SPEED), BoneMath.SPEAR_LIFE, damage, budget, (float) BoneMath.SPEAR_REACH), true, 0.7f);
        }));
        boss.later(HOP_TICKS, safe("warp hop", () -> warpHop(boss, target, k + 1, hops, damage, start, budget)));
    }

    /**
     * The bone armour shatters: a burst of shards round the boss. Everyone within {@value BoneMath#SHARD_RADIUS} blocks takes 'damage'
     * and is thrown back. Hitting the armour from close range is the one way to be hurt by it.
     */
    public static void shardBurst(ZogratisBoss boss, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 at = boss.position().add(0, 1, 0);
        VfxSpawn.send(sl, VfxShape.BONE_FX3, at, at, BONE, 18, 0.9f);
        ZogratisVfx.spaceShatter(sl, boss.position(), BoneMath.SHARD_RADIUS, 0xB8E8FF);
        sl.playSound(null, boss.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6f, 1.4f);
        for (LivingEntity e : boss.foesAround(boss.position(), BoneMath.SHARD_RADIUS + 1)) {
            if (e.distanceToSqr(boss) > BoneMath.SHARD_RADIUS * BoneMath.SHARD_RADIUS) continue;
            if (boss.strike(e, damage, false)) e.knockback(0.8, boss.getX() - e.getX(), boss.getZ() - e.getZ());
        }
    }

    /**
     * Spatial Evasion: the boss slips through a rift to a spot 5 to 8 blocks away, preferring the side away from 'threat'. Returns
     * whether it found room and went.
     */
    public static boolean slip(ZogratisBoss boss, Vec3 threat) {
        if (!(boss.level() instanceof ServerLevel sl)) return false;
        Vec3 away = threat == null ? boss.getViewVector(1f).scale(-1) : boss.position().subtract(threat).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) away = new Vec3(0, 0, 1);
        away = away.normalize();
        double dist = 5 + boss.getRandom().nextDouble() * 3;
        for (double turn : new double[]{0, 0.9, -0.9, 1.8, -1.8, 2.7}) {
            Vec3 d = rotateY(away, turn).scale(dist);
            Vec3 want = landing(sl, boss, boss.position().add(d.x, 0, d.z));
            if (want != null && boss.blinkTo(want)) return true;
        }
        return false;
    }

    // ================================================================ 1.35.0: Rift Collapse
    /** The half-width of a rift cut, in blocks (the tear drawn in the wind-up is the same width). */
    public static final double CUT_HALF_WIDTH = 1.6, CUT_REACH = 18;

    /** The lines of a Rift Collapse through 'centre': 'count' of them, evenly turned, the first facing along 'baseAngle' (radians); each is [from, to]. */
    public static List<Vec3[]> cutLines(Vec3 centre, double baseAngle, int count) {
        List<Vec3[]> out = new ArrayList<>();
        int n = Math.max(2, Math.min(5, count));
        for (int i = 0; i < n; i++) {
            double a = baseAngle + i * Math.PI / n;
            Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(CUT_REACH);
            out.add(new Vec3[]{centre.subtract(d), centre.add(d)});
        }
        return out;
    }

    /** Space is cut along each line: whoever stands within the half-width takes 'damage' once in all and is thrown up a little. */
    public static void riftCuts(ZogratisBoss boss, List<Vec3[]> lines, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || lines == null) return;
        final java.util.Set<Integer> struck = new java.util.HashSet<>();
        for (Vec3[] ln : lines) {
            ZogratisVfx.tear(sl, ln[0].add(0, 0.8, 0), ln[1].add(0, 0.8, 0), 1.6, 16, 0xB8E8FF);
            for (LivingEntity e : boss.foesAround(ln[0].lerp(ln[1], 0.5), CUT_REACH + 3)) {
                if (struck.contains(e.getId()) || Math.abs(e.getY() - ln[0].y) > 3.5) continue;
                if (distToLine(e.getX(), e.getZ(), ln[0], ln[1]) > CUT_HALF_WIDTH + e.getBbWidth() / 2) continue;
                struck.add(e.getId());
                if (boss.strike(e, damage, true)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
                    e.hurtMarked = true;
                }
            }
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
    }

    /** Distance of the point (x, z) from the segment a-b on the ground plane. */
    static double distToLine(double x, double z, Vec3 a, Vec3 b) {
        double dx = b.x - a.x, dz = b.z - a.z, len2 = dx * dx + dz * dz;
        double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * dx + (z - a.z) * dz) / len2));
        double px = a.x + dx * t - x, pz = a.z + dz * t - z;
        return Math.sqrt(px * px + pz * pz);
    }

    // ================================================================ portals and places
    /** A dimensional gate standing at 'base' for 'ticks' ticks, facing 'facing', with a ring of dust at its foot. */
    static void portal(ServerLevel sl, Vec3 base, Vec3 facing, int ticks) {
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, base, facing, INDIGO, ticks, (float) PORTAL_RADIUS);
        ZogratisVfx.gate(sl, base.add(0, 1.2, 0), facing.add(0, 1.2, 0), PORTAL_RADIUS * 0.9, ticks, 0xB8E8FF);     // 1.35.0: Zenon's own gate ring
        Tells.ring(sl, base, 0.9, RGB, 1.2f, 16);
    }

    /**
     * A place near 'want' where the boss's body fits and there is floor under it: the ground under it, then a ring of eight spots
     * round it at 1.4 blocks, then a block higher. Null if there is none (never a spot over a drop or the void).
     */
    static Vec3 freeSpot(ZogratisBoss boss, Vec3 want) {
        if (!(boss.level() instanceof ServerLevel sl)) return null;
        Vec3 g = landing(sl, boss, want);
        if (g == null) return null;
        if (fits(sl, boss, g)) return g;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            Vec3 c = landing(sl, boss, g.add(Math.cos(a) * 1.4, 0, Math.sin(a) * 1.4));
            if (c != null && fits(sl, boss, c)) return c;
        }
        Vec3 up = g.add(0, 1, 0);
        return fits(sl, boss, up) ? up : null;
    }

    /**
     * The floor near 'p' to land on: {@link ZogratisBoss#groundOrNull}, and failing that the first spot from 3 above to 6 below where
     * the boss's body fits (so thin snow, grass and the like count as air: a snowy field is ground) over a block that is neither air
     * nor a fluid. Null over a drop deeper than that, over a fluid, or over the void.
     */
    static Vec3 landing(ServerLevel level, ZogratisBoss boss, Vec3 p) {
        Vec3 g = ZogratisBoss.groundOrNull(level, p);
        if (g != null) return g;
        BlockPos base = BlockPos.containing(p);
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos b = base.offset(0, dy, 0);
            BlockState floor = level.getBlockState(b.below());
            if (floor.isAir() || !floor.getFluidState().isEmpty() || !level.getFluidState(b).isEmpty()) continue;
            Vec3 at = new Vec3(p.x, b.getY(), p.z);
            if (fits(level, boss, at)) return at;
        }
        return null;
    }

    private static boolean fits(ServerLevel sl, ZogratisBoss boss, Vec3 at) {
        return sl.noCollision(boss, boss.getBoundingBox().move(at.subtract(boss.position())));
    }

    /** Where spear 'i' of 'n' grows, in the world. */
    private static Vec3 growthWorld(ZogratisBoss boss, int i, int n) {
        double[] g = BoneMath.growthPoint(i, n, boss.getYRot());
        return boss.position().add(g[0], g[1], g[2]);
    }

    /** The end of a ray from 'from' along the unit vector 'dir', stopped by the first wall within 'length'. */
    static Vec3 rayEnd(ServerLevel sl, Entity by, Vec3 from, Vec3 dir, double length) {
        Vec3 to = from.add(dir.scale(length));
        BlockHitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    // ================================================================ a flying bone spear
    /** The launch of a spear: its bone-spear effect (if 'drawn'), a sound, and a zone that steps it every tick. */
    private static void launch(ServerLevel sl, ZogratisBoss boss, BoneSpear spear, boolean drawn, float size) {
        Vec3 dir = spear.vel.normalize();
        double reach = spear.vel.length() * spear.life;
        if (drawn) VfxSpawn.send(sl, VfxShape.BONE_FX1, spear.pos, rayEnd(sl, boss, spear.pos, dir, reach), BONE, BoneMath.flightTicks(reach, spear.vel.length()), size);
        sl.playSound(null, BlockPos.containing(spear.pos), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.9f, 0.7f);
        SpellRuntime.zone(sl, spear.life + 2, 1, safe("bone spear", age -> spear.step(sl, boss)));
    }

    /** One dense bone spear in flight: a point on a clipped ray, drawn as chips of bone and pale dust. */
    private static final class BoneSpear {
        Vec3 pos;
        final Vec3 vel;
        final float damage, reach;
        final VolleyBudget budget;
        final Set<Integer> touched = new HashSet<>();
        int life;
        boolean done, pierce;

        BoneSpear(Vec3 pos, Vec3 vel, int life, float damage, VolleyBudget budget, float reach) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
            this.damage = damage;
            this.budget = budget;
            this.reach = reach;
        }

        void step(ServerLevel sl, ZogratisBoss boss) {
            if (done) return;
            if (!boss.isAlive() || boss.isRemoved()) { done = true; return; }
            Vec3 next = pos.add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            AABB sweep = new AABB(pos, next).inflate(reach);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweep, boss::isFoe)) {
                if (!touched.add(e.getId())) continue;
                if (budget != null && !budget.tryHit(e.getId())) continue;
                if (boss.strike(e, damage, false)) {
                    Vec3 d = vel.normalize();
                    e.knockback(pierce ? 1.0 : 0.4, -d.x, -d.z);
                    e.hurtMarked = true;
                }
                if (!pierce) {
                    impact(sl, next);
                    done = true;
                    return;
                }
            }
            pos = next;
            draw(sl);
            if (blocked || --life <= 0) {
                impact(sl, pos);
                done = true;
            }
        }

        private void draw(ServerLevel sl) {
            Vec3 d = vel.normalize();
            Vec3 tail = pos.subtract(d.scale(0.7));
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.0);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), tail.x, tail.y, tail.z, 1, 0.06, 0.06, 0.06, 0.0);
            sl.sendParticles(new DustParticleOptions(new Vector3f(DevilStyle.red(ACCENT), DevilStyle.green(ACCENT), DevilStyle.blue(ACCENT)), 0.8f),
                    pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
        }

        private static void impact(ServerLevel sl, Vec3 at) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.08);
            sl.playSound(null, BlockPos.containing(at), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 0.8f, 1.4f);
        }
    }
}
