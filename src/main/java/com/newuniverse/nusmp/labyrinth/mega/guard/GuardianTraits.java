package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling.Trait;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling.TraitSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: what the nine theme traits DO in the world, kept out of the entity class so {@link LabyrinthGuardianEntity} stays a readable boss
 * and every trait is one short method with its own cooldown rule. The numbers all come from {@link GuardianScaling#spec} (so the caps are
 * tested there) and the shapes from {@link TraitMath} (tested there); this class only asks "who is near", hurts, applies an effect and
 * sends particles. Three rules hold for every trait. (1) Players only: a trait never targets creative or spectating players, nor the
 * dungeon's wildlife, so it cannot start a fight with the ecology. (2) No block is ever changed: a gravity well, a fire trail and a ring
 * of stars are damage zones and particles, because the Mega-Dungeon is held by the Dungeon Guard and a real fire would spread to the
 * banners. (3) Every hard control goes through {@link BalanceLaw#beginControl} and its caps, and the effects the brief limits (blindness
 * two seconds, a root one second) are clamped again here, so a mistake in a table cannot turn into a stun-lock. State that must not
 * outlive the fight (cooldowns, the trail, the stars in the air) is in {@link State} and is not saved: a Guardian that is reloaded simply
 * restarts its cooldowns.
 */
final class GuardianTraits {
    private GuardianTraits() {}

    /** The runtime state of one Guardian's traits. */
    static final class State {
        /** Game time the main periodic ability is next ready. */
        long readyAt;
        final TraitMath.FireTrail trail = new TraitMath.FireTrail();
        long nextTrailAt, nextBurnAt, nextEmberAt;
        double lastX = Double.NaN, lastZ = Double.NaN;
        /** Falling stars: {x, floorY, z, tickItLands}. */
        final List<double[]> stars = new ArrayList<>();

        /** A fresh start (a new phase, a spawn): nothing in the air, abilities ready in a moment. */
        void restart(long now, int delay) {
            readyAt = now + delay;
            trail.clear();
            stars.clear();
            lastX = lastZ = Double.NaN;
        }
    }

    /**
     * Test seam, set only by {@link GuardSelfTests}: while it is not null these players are the whole population a trait can see. A
     * {@code FakePlayer} (the stand-in for a person in the in-game self-tests) is in no level's entity list, so the world query below can never
     * find it, and it reports its position as the origin, which is why every distance in this class is measured from {@code getX/Y/Z}.
     */
    static volatile List<Player> testPlayers;

    /** Survival players near the Guardian (the only thing a trait ever targets). */
    static List<Player> players(ServerLevel sl, LabyrinthGuardianEntity g, double radius) {
        List<Player> injected = testPlayers;
        if (injected != null) {
            List<Player> out = new ArrayList<>();
            net.minecraft.world.phys.AABB box = g.getBoundingBox().inflate(radius);
            for (Player p : injected) if (p.isAlive() && !p.isSpectator() && !p.isCreative() && box.intersects(p.getBoundingBox())) out.add(p);
            return out;
        }
        return sl.getEntitiesOfClass(Player.class, g.getBoundingBox().inflate(radius), p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    /** Squared distance from the Guardian to a player, from the coordinate fields (see {@link #testPlayers}). */
    private static double d2(LabyrinthGuardianEntity g, Player p) { return g.distanceToSqr(p.getX(), p.getY(), p.getZ()); }

    /** The per-tick entry: runs the periodic part of the Guardian's trait. BLOOD and STORM have none (they act in {@link #onHit} and {@link #onSlam}). */
    static void tick(LabyrinthGuardianEntity g, ServerLevel sl, State st, Trait trait, boolean phaseTwo) {
        long now = sl.getGameTime();
        TraitSpec spec = GuardianScaling.spec(trait, phaseTwo);
        switch (trait) {
            case GRAVITY -> gravityWell(g, sl, st, spec, now);
            case SPATIAL -> blink(g, sl, st, spec, now);
            case FLAME -> flame(g, sl, st, spec, now);
            case FROST -> frost(g, sl, st, spec, now);
            case VERDANT -> roots(g, sl, st, spec, now);
            case SHADOW -> shadow(g, sl, st, spec, now);
            case ASTRAL -> astral(g, sl, st, spec, now);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- GRAVITY: pull everyone in, then the (wider) slam takes them
    private static void gravityWell(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now < st.readyAt) return;
        List<Player> pulled = new ArrayList<>();
        for (Player p : players(sl, g, spec.range())) if (d2(g, p) > 9.0 && d2(g, p) <= spec.range() * spec.range()) pulled.add(p);
        if (pulled.isEmpty()) { st.readyAt = now + 20; return; }                 // everyone is already at its feet, or nobody is near: look again in a second
        st.readyAt = now + spec.cooldown();
        float damage = (float) (spec.power() * g.attackDamage());
        for (Player p : pulled) {
            p.hurt(g.damageSources().mobAttack(g), damage);                      // first: the blow's own push is overwritten by the pull below
            double tx = g.getX() - p.getX(), tz = g.getZ() - p.getZ();
            double d = Math.max(0.5, Math.hypot(tx, tz));
            double pull = Math.min(1.5, 0.35 + d * 0.11);
            p.setDeltaMovement(tx / d * pull, 0.28, tz / d * pull);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, spec.duration(), 0));
        }
        sl.playSound(null, g.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.4f, 0.5f);
        ring(sl, Fx.options(FxThemes.DUNGEON_GUARDIAN, FxOld.PORTAL), g.getX(), g.getY() + 0.3, g.getZ(), spec.range(), 40);
        ring(sl, Fx.options(FxThemes.DUNGEON_GUARDIAN, FxOld.PORTAL), g.getX(), g.getY() + 0.3, g.getZ(), spec.range() * 0.5, 20);
    }

    // ---------------------------------------------------------------- SPATIAL: a blink behind the target and a strike
    private static void blink(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now < st.readyAt) return;
        if (!(g.getTarget() instanceof Player p) || !p.isAlive() || p.isCreative() || p.isSpectator()) return;
        double dist2 = d2(g, p);
        if (dist2 < 9.0 || dist2 > spec.range() * spec.range()) return;
        Vec3 look = p.getLookAngle();
        double[][] spots = TraitMath.blinkSpots(p.getX(), p.getZ(), look.x, look.z, 1.2 + g.getBbWidth());
        Vec3 from = g.position();
        for (double[] s : spots) {
            Vec3 c = new Vec3(s[0], p.getY(), s[1]);
            BlockPos feet = BlockPos.containing(c);
            if (sl.getBlockState(feet.below()).getCollisionShape(sl, feet.below()).isEmpty()) continue;       // no floor there
            if (!sl.noCollision(g, g.getBoundingBox().move(c.subtract(from)))) continue;                       // would be inside a wall
            if (!g.insideLeash(c.x, c.z)) continue;                                                            // never out of its hall
            Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.REVERSE_PORTAL, from.x, from.y + 1.4, from.z, 30, 0.5, 1.2, 0.5, 0.1);
            sl.playSound(null, g.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.4f);
            g.teleportTo(c.x, c.y, c.z);
            g.getNavigation().stop();
            g.lookAt(p, 180f, 180f);
            Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.REVERSE_PORTAL, c.x, c.y + 1.4, c.z, 30, 0.5, 1.2, 0.5, 0.1);
            sl.playSound(null, feet, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.4f);
            g.swing(InteractionHand.MAIN_HAND);
            p.hurt(g.damageSources().mobAttack(g), (float) (spec.power() * g.attackDamage()));
            st.readyAt = now + spec.cooldown();
            return;
        }
        st.readyAt = now + 20;                                                                                 // nowhere to land: try again in a second
    }

    // ---------------------------------------------------------------- FLAME: cells of fire where it has walked
    private static void flame(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now >= st.nextTrailAt && g.onGround()) {
            st.nextTrailAt = now + 4;
            double dx = g.getX() - st.lastX, dz = g.getZ() - st.lastZ;
            if (Double.isNaN(st.lastX) || dx * dx + dz * dz >= 0.36) {
                st.trail.add(g.getBlockX(), g.blockPosition().getY(), g.getBlockZ(), now + spec.duration(), spec.count());
                st.lastX = g.getX();
                st.lastZ = g.getZ();
            }
        }
        st.trail.prune(now);
        if (st.trail.size() == 0) return;
        if (now >= st.nextEmberAt) {
            st.nextEmberAt = now + 6;
            for (int i = 0; i < st.trail.size(); i++) {
                int[] c = st.trail.cell(i);
                Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.FLAME, c[0] + 0.5, c[1] + 0.1, c[2] + 0.5, 2, 0.25, 0.05, 0.25, 0.01);
                if ((i + (int) now) % 5 == 0) Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.LAVA, c[0] + 0.5, c[1] + 0.3, c[2] + 0.5, 1, 0.2, 0.05, 0.2, 0.0);
            }
        }
        if (now >= st.nextBurnAt) {
            st.nextBurnAt = now + 10;
            float damage = (float) (spec.power() * g.attackDamage() * 0.5);          // per half second: `power` of its attack damage every second
            for (Player p : players(sl, g, 40.0)) {
                if (!st.trail.burnsAt(p.getX(), p.getY(), p.getZ(), spec.range(), now)) continue;
                p.igniteForSeconds(3.0f);
                p.hurt(g.damageSources().onFire(), damage);
            }
        }
    }

    // ---------------------------------------------------------------- FROST: a slowing pulse
    private static void frost(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now < st.readyAt) return;
        List<Player> in = new ArrayList<>();
        for (Player p : players(sl, g, spec.range())) if (d2(g, p) <= spec.range() * spec.range()) in.add(p);
        if (in.isEmpty()) { st.readyAt = now + 20; return; }
        st.readyAt = now + spec.cooldown();
        for (Player p : in) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, spec.duration(), spec.count()));
            p.setTicksFrozen(Math.min(100, p.getTicksFrozen() + 30));
        }
        sl.playSound(null, g.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 0.8f, 0.6f);
        ring(sl, Fx.options(FxThemes.DUNGEON_GUARDIAN, FxOld.SNOWFLAKE), g.getX(), g.getY() + 0.2, g.getZ(), spec.range(), 36);
        Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.SNOWFLAKE, g.getX(), g.getY() + 1.5, g.getZ(), 30, 1.2, 1.4, 1.2, 0.02);
    }

    // ---------------------------------------------------------------- VERDANT: one root, one second, under the shared control lock
    private static void roots(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now < st.readyAt) return;
        Player victim = null;
        double best = spec.range() * spec.range();
        for (Player p : players(sl, g, spec.range())) {
            double d = d2(g, p);
            if (d <= best && g.hasLineOfSight(p)) { best = d; victim = p; }
        }
        if (victim == null) { st.readyAt = now + 20; return; }
        if (!BalanceLaw.beginControl(g)) { st.readyAt = now + 40; return; }       // the shared 12 s lock: another hard control ran lately
        st.readyAt = now + spec.cooldown();
        int ticks = Math.min(GuardianScaling.ROOT_MAX_TICKS, Math.min(spec.duration(), BalanceLaw.controlTicks(victim, spec.duration())));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        victim.setDeltaMovement(0.0, Math.min(0.0, victim.getDeltaMovement().y), 0.0);
        victim.hurtMarked = true;
        sl.playSound(null, victim.blockPosition(), SoundEvents.VINE_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), victim.getX(), victim.getY() + 0.4, victim.getZ(), 30, 0.4, 0.5, 0.4, 0.05);
        Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.COMPOSTER, victim.getX(), victim.getY() + 0.2, victim.getZ(), 16, 0.4, 0.3, 0.4, 0.02);
    }

    // ---------------------------------------------------------------- SHADOW: a short pulse of darkness, close in only
    private static void shadow(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now < st.readyAt) return;
        double range = Math.min(GuardianScaling.SHADOW_MAX_RANGE, spec.range());
        List<Player> in = new ArrayList<>();
        for (Player p : players(sl, g, range)) if (d2(g, p) <= range * range) in.add(p);
        if (in.isEmpty()) { st.readyAt = now + 20; return; }
        st.readyAt = now + spec.cooldown();
        int ticks = Math.min(GuardianScaling.SHADOW_MAX_TICKS, spec.duration());
        for (Player p : in) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
        sl.playSound(null, g.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.2f, 0.7f);
        for (double r = 1.5; r <= range; r += 1.5) ring(sl, Fx.options(FxThemes.DUNGEON_GUARDIAN, FxOld.SQUID_INK), g.getX(), g.getY() + 0.5, g.getZ(), r, (int) (r * 6));
    }

    // ---------------------------------------------------------------- ASTRAL: stars fall on a ring round it, damage only
    private static void astral(LabyrinthGuardianEntity g, ServerLevel sl, State st, TraitSpec spec, long now) {
        if (now >= st.readyAt && g.getTarget() instanceof Player t && t.isAlive() && d2(g, t) <= 24.0 * 24.0) {
            st.readyAt = now + spec.cooldown();
            double phase = g.getRandom().nextDouble() * Math.PI * 2.0;
            for (double[] p : TraitMath.starRing(g.getX(), g.getZ(), spec.range(), spec.count(), phase)) {
                double floor = floorY(sl, p[0], g.getY() + 2.0, p[1]);
                st.stars.add(new double[] {p[0], floor, p[1], now + spec.duration()});
                Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.END_ROD, p[0], floor + 0.1, p[1], 4, 0.2, 0.05, 0.2, 0.01);
            }
            sl.playSound(null, g.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 1.4f);
        }
        if (st.stars.isEmpty()) return;
        float damage = (float) (spec.power() * g.attackDamage());
        Iterator<double[]> it = st.stars.iterator();
        while (it.hasNext()) {
            double[] s = it.next();
            long left = (long) s[3] - now;
            if (left > 0) {
                if (left % 2 == 0) Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.END_ROD, s[0], s[1] + left * 0.45, s[2], 1, 0.0, 0.0, 0.0, 0.0);
                continue;
            }
            it.remove();
            for (Player p : players(sl, g, 40.0)) {
                double dx = p.getX() - s[0], dz = p.getZ() - s[2];
                if (dx * dx + dz * dz <= 4.0 && Math.abs(p.getY() - s[1]) <= 3.0) p.hurt(g.damageSources().magic(), damage);      // no attacker: no push, damage only
            }
            Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.FIREWORK, s[0], s[1] + 0.3, s[2], 14, 0.4, 0.3, 0.4, 0.12);
            Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.FLASH, s[0], s[1] + 0.5, s[2], 1, 0.0, 0.0, 0.0, 0.0);
            sl.playSound(null, BlockPos.containing(s[0], s[1], s[2]), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.9f, 1.3f);
        }
    }

    /** The top of the first solid block at or below {@code startY} (up to 8 blocks down), or {@code startY - 8} when there is none. */
    static double floorY(ServerLevel sl, double x, double startY, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        for (int y = (int) Math.floor(startY); y > (int) Math.floor(startY) - 8; y--) {
            BlockPos p = new BlockPos(bx, y, bz);
            if (!sl.getBlockState(p).getCollisionShape(sl, p).isEmpty()) return y + 1.0;
        }
        return startY - 8.0;
    }

    // ---------------------------------------------------------------- STORM: chain lightning after a Ground Slam
    static void onSlam(LabyrinthGuardianEntity g, ServerLevel sl, boolean phaseTwo) {
        TraitSpec spec = GuardianScaling.spec(Trait.STORM, phaseTwo);
        List<Player> ps = players(sl, g, spec.range() + 2.0);
        if (ps.isEmpty()) return;
        double[][] pts = new double[ps.size()][];
        for (int i = 0; i < pts.length; i++) pts[i] = new double[] {ps.get(i).getX(), ps.get(i).getY() + 1.0, ps.get(i).getZ()};
        int[] order = TraitMath.chainOrder(g.getX(), g.getY() + 1.5, g.getZ(), pts, spec.range(), TraitMath.CHAIN_HOP, 1 + spec.count());
        double px = g.getX(), py = g.getY() + 2.0, pz = g.getZ();
        double base = spec.power() * g.attackDamage();
        for (int hop = 0; hop < order.length; hop++) {
            Player p = ps.get(order[hop]);
            arc(sl, px, py, pz, pts[order[hop]][0], pts[order[hop]][1], pts[order[hop]][2]);
            p.hurt(g.damageSources().lightningBolt(), (float) TraitMath.chainDamage(base, hop));
            sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.6f, 1.6f);
            px = pts[order[hop]][0];
            py = pts[order[hop]][1];
            pz = pts[order[hop]][2];
        }
    }

    // ---------------------------------------------------------------- BLOOD: a shield fed by its own hits (not a heal: boss rules forbid healing)
    static void onHit(LabyrinthGuardianEntity g, ServerLevel sl, Entity victim, boolean phaseTwo) {
        TraitSpec spec = GuardianScaling.spec(Trait.BLOOD, phaseTwo);
        double cap = g.getMaxHealth() * spec.count() / 100.0;
        double gain = spec.power() * g.attackDamage();
        g.setAbsorptionAmount((float) Math.min(cap, g.getAbsorptionAmount() + gain));
        Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.DAMAGE_INDICATOR, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 4, 0.25, 0.3, 0.25, 0.1);
        Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.CRIMSON_SPORE, g.getX(), g.getY() + 1.6, g.getZ(), 6, 0.5, 0.8, 0.5, 0.02);
    }

    // ---------------------------------------------------------------- particles
    /** A flat ring of {@code points} particles of one kind. */
    private static void ring(ServerLevel sl, net.minecraft.core.particles.SimpleParticleType type, double cx, double cy, double cz, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2.0 / points;
            sl.sendParticles(type, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, 1, 0.0, 0.05, 0.0, 0.0);
        }
    }

    /** A line of sparks from one point to another. */
    private static void arc(ServerLevel sl, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        int n = Math.max(2, (int) (Math.sqrt(dx * dx + dy * dy + dz * dz) / 0.6));
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            Fx.spray(sl, FxThemes.DUNGEON_GUARDIAN, FxOld.ELECTRIC_SPARK, x1 + dx * t, y1 + dy * t, z1 + dz * t, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

}
