package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.mana.zone.TrueMagic;
import com.newuniverse.nusmp.mana.zone.TrueMagicRules;
import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.PerfParticles;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.quests.NusmpEvents;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import io.github.manasmods.tensura.util.EnergyHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The rune constructs of the Mana Method and the bolts they fire. A construct is NOT an entity: it is a small record in a list on the server (nothing to register, save, sync or
 * render; the clients draw the rune from one packet, {@link RuneNet}). This is the cheapest of the mod's spell-construct mechanisms ({@code SpellRuntime}) with the caps it lacks:
 * <ul>
 *   <li>at most {@link ManaMethodConfig#perCaster} casts per caster and {@link ManaMethodConfig#global} on the server (a cast that would pass either is refused before it costs
 *       anything);</li>
 *   <li>a construct lives {@code charge} ticks (18 to 30) plus at most {@link RuneRules#MAX_FLIGHT_TICKS} ticks of flight: it despawns by itself; it is dropped at once when its caster
 *       dies, logs out, changes dimension or the server stops;</li>
 *   <li>each tick, per bolt: one move, one block ray (a short segment, only inside loaded chunks), and one entity query of the last stretch, which all bolts together share a budget
 *       of {@link RuneRules#QUERIES_PER_TICK} for (a bolt denied sweeps a longer stretch next tick, never more than three ticks in a row); Homing looks the target up by id, it
 *       scans nothing after the launch; one trail packet every {@link RuneRules#TRAIL_EVERY} ticks.</li>
 * </ul>
 * Idle cost: one empty-list check per server tick.
 */
public final class RuneManager {
    private RuneManager() {}

    /** One cast: the construct while it charges, then the bolt. */
    static final class Cast {
        final UUID ownerId;
        final ServerPlayer owner;
        final GrimoireBook book;
        final ManasSkillInstance inst;
        final int mode;
        final ServerLevel level;
        final RuneSpec spec;
        final CompoundTag data;
        final long born;
        final int charge;
        final Vec3 center;
        boolean flying;
        double px, py, pz, dx, dy, dz, travelled, lx, ly, lz;
        int age, denied;
        UUID target;
        double density = 1.0;

        Cast(ServerPlayer owner, GrimoireBook book, ManasSkillInstance inst, int mode, RuneSpec spec, Vec3 center, long born) {
            this.ownerId = owner.getUUID();
            this.owner = owner;
            this.book = book;
            this.inst = inst;
            this.mode = mode;
            this.level = owner.serverLevel();
            this.spec = spec;
            this.data = RuneSpecNbt.write(spec);
            this.born = born;
            this.charge = data.getInt(RuneSpecNbt.CHARGE);
            this.center = center;
        }
    }

    private static final List<Cast> ACTIVE = new ArrayList<>();
    private static final PerfBudget QUERIES = new PerfBudget("mana-method-queries", RuneRules.QUERIES_PER_TICK);
    private static final PerfProbe.Section PROBE = PerfProbe.section("s.mana.method");
    private static final double[] DIR = new double[3], WANT = new double[3];

    public enum Begin { OK, CASTER_FULL, SERVER_FULL, OFF }

    public static int activeCount() { return ACTIVE.size(); }

    public static int activeCount(UUID owner) {
        int n = 0;
        for (Cast c : ACTIVE) if (c.ownerId.equals(owner)) n++;
        return n;
    }

    // ---------------------------------------------------------------- the cast
    /** Starts a cast: the rune forms in front of the caster. Refused (nothing spent, nothing sent) over the caps or with the setting off. */
    public static Begin begin(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, RuneSpec spec, long now) {
        if (!ManaMethodConfig.enabled()) return Begin.OFF;
        if (activeCount(p.getUUID()) >= ManaMethodConfig.perCaster()) return Begin.CASTER_FULL;
        if (ACTIVE.size() >= ManaMethodConfig.global()) return Begin.SERVER_FULL;
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle().normalize();
        Vec3 center = eye.add(look.scale(RuneRules.RUNE_DISTANCE));
        HitResult wall = level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) center = eye.add(look.scale(Math.max(0.8, wall.getLocation().distanceTo(eye) - 0.35)));      // a wall in front: the rune forms nearer
        Cast c = new Cast(p, book, inst, mode, spec, center, now);
        c.density = spec.trueMagic() ? ManaLaw.density(level, BlockPos.containing(center)) : 1.0;
        ACTIVE.add(c);
        RuneNet.send(level, center, look, spec, c.charge, p.getId());
        level.playSound(null, BlockPos.containing(center), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.1f, spec.trueMagic() ? 0.7f : 1.0f);
        NusmpEvents.fire(p, "mana_method.cast", spec.element().name().toLowerCase(java.util.Locale.ROOT));
        return Begin.OK;
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        if (!ACTIVE.isEmpty()) {
            long t0 = PerfProbe.begin();
            try {
                tickAll(server, now);
            } finally {
                PerfProbe.end(PROBE, t0);
            }
        }
        TrueBlocks.tick(server, now);
    }

    /** One pass over every cast at tick {@code now} (public for the self-test, which advances time by hand). */
    public static void tickAll(MinecraftServer server, long now) {
        for (Iterator<Cast> it = ACTIVE.iterator(); it.hasNext(); ) {
            Cast c = it.next();
            ServerPlayer p = c.owner;
            if (p.isRemoved() || !p.isAlive() || p.level() != c.level) { it.remove(); continue; }
            boolean[] done = {false};
            boolean ok = SafeCast.tick("mana-method", () -> done[0] = step(c, now));
            if (!ok || done[0]) it.remove();
        }
    }

    /** Advances one cast; true when it is over. */
    private static boolean step(Cast c, long now) {
        if (!c.flying) {
            if (now - c.born < c.charge) return false;
            launch(c);
            return false;
        }
        return fly(c, now);
    }

    private static void launch(Cast c) {
        ServerPlayer p = c.owner;
        Vec3 look = p.getLookAngle().normalize();
        c.dx = look.x;
        c.dy = look.y;
        c.dz = look.z;
        c.px = c.lx = c.center.x;
        c.py = c.ly = c.center.y;
        c.pz = c.lz = c.center.z;
        if (c.spec.homing()) c.target = acquire(c, look);
        c.flying = true;
        c.level.playSound(null, BlockPos.containing(c.center), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2f, 1.3f);
    }

    /** The ONE query of a Homing cast: the foe nearest the launch direction inside the cone. */
    private static UUID acquire(Cast c, Vec3 look) {
        AABB box = new AABB(c.center, c.center.add(look.scale(RuneRules.HOMING_RANGE))).inflate(RuneRules.HOMING_RANGE * 0.5);
        LivingEntity best = null;
        double bestCos = RuneRules.HOMING_CONE_COS;
        for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator() && Foes.isFoe(c.owner, e))) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(c.center);
            double len = d.length();
            if (len < 0.5 || len > RuneRules.HOMING_RANGE) continue;
            double cos = d.dot(look) / len;
            if (cos >= bestCos) { bestCos = cos; best = e; }
        }
        return best == null ? null : best.getUUID();
    }

    private static boolean fly(Cast c, long now) {
        c.age++;
        if (c.age > RuneRules.MAX_FLIGHT_TICKS || c.travelled > RuneRules.MAX_RANGE) { fizzle(c); return true; }
        // Homing: a bounded turn toward the target picked at the launch (looked up by id: no scan)
        if (c.target != null && RuneRules.homingActive(c.spec, c.age)) {
            Entity t = c.level.getEntity(c.target);
            if (t instanceof LivingEntity le && le.isAlive()) {
                Vec3 want = le.getBoundingBox().getCenter().subtract(c.px, c.py, c.pz);
                double len = want.length();
                if (len > 0.3) {
                    DIR[0] = c.dx; DIR[1] = c.dy; DIR[2] = c.dz;
                    WANT[0] = want.x / len; WANT[1] = want.y / len; WANT[2] = want.z / len;
                    RuneRules.steer(DIR, WANT, RuneRules.HOMING_TURN, DIR);
                    c.dx = DIR[0]; c.dy = DIR[1]; c.dz = DIR[2];
                }
            } else {
                c.target = null;                                                     // it died or left: the bolt flies straight on, nothing is searched again
            }
        }
        double speed = RuneRules.speed(c.spec);
        Vec3 from = new Vec3(c.px, c.py, c.pz), next = from.add(c.dx * speed, c.dy * speed, c.dz * speed);
        if (!c.level.isLoaded(BlockPos.containing(next)) || !c.level.getWorldBorder().isWithinBounds(BlockPos.containing(next))) { fizzle(c); return true; }
        boolean wallHit = false;
        HitResult wall = c.level.clip(new ClipContext(from, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.owner));
        if (wall.getType() != HitResult.Type.MISS) { next = wall.getLocation(); wallHit = true; }
        double step = from.distanceTo(next);
        // the entity sweep of the stretch since the last query: budgeted, but never skipped three ticks in a row, and always done when a wall ends the flight
        boolean query = wallHit || c.denied >= 2 || QUERIES.tryTake(now, 1);
        LivingEntity hit = null;
        if (query) {
            c.denied = 0;
            double radius = RuneRules.hitRadius(c.spec, c.travelled + step);
            Vec3 last = new Vec3(c.lx, c.ly, c.lz);
            double best = Double.MAX_VALUE;
            for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, new AABB(last, next).inflate(radius),
                    e -> e.isAlive() && !e.isSpectator() && Foes.isFoe(c.owner, e))) {
                double d = e.getBoundingBox().getCenter().distanceToSqr(last);
                if (d < best) { best = d; hit = e; }
            }
            c.lx = next.x; c.ly = next.y; c.lz = next.z;
        } else {
            c.denied++;
        }
        c.travelled += step;
        c.px = next.x; c.py = next.y; c.pz = next.z;
        if (hit != null) {
            impact(c, hit.getBoundingBox().getCenter(), hit, now);
            return true;
        }
        if (wallHit) {
            impact(c, next, null, now);
            return true;
        }
        if (c.age % RuneRules.TRAIL_EVERY == 0) {
            double spread = 0.08 + 0.12 * (RuneRules.growthScale(c.travelled) - 1.0) * (c.spec.growth() ? 1.0 : 0.0);
            PerfParticles.send(c.level, trailParticle(c.spec.element()), c.px, c.py, c.pz, RuneRules.TRAIL_PARTICLES, spread, spread, spread, 0.01);
        }
        return false;
    }

    private static void fizzle(Cast c) {
        PerfParticles.send(c.level, ParticleTypes.POOF, c.px, c.py, c.pz, 4, 0.15, 0.15, 0.15, 0.01);
    }

    // ---------------------------------------------------------------- the impact
    private static void impact(Cast c, Vec3 at, LivingEntity primary, long now) {
        ServerPlayer p = c.owner;
        RuneSpec spec = c.spec;
        double splash = RuneRules.splashRadius(spec, c.travelled);
        List<LivingEntity> targets = new ArrayList<>(RuneRules.MAX_TARGETS);
        if (primary != null) targets.add(primary);
        for (LivingEntity t : GrimoireBook.around(p, at, splash)) {
            if (targets.size() >= RuneRules.MAX_TARGETS) break;
            if (t != primary && Foes.isFoe(p, t)) targets.add(t);
        }
        for (LivingEntity t : targets) {
            c.book.hurt(c.inst, p, t, c.mode, (float) damageFor(c, t, t == primary));
            rider(spec.element(), t, BalanceLaw.isBoss(t));
        }
        c.book.impact(p, at, (float) (1.0 + 0.5 * RuneRules.growthScale(spec.growth() ? c.travelled : 0.0)));
        PerfParticles.send(c.level, burstParticle(spec.element()), at.x, at.y, at.z, 14, splash * 0.35, splash * 0.25, splash * 0.35, 0.08);
        c.level.playSound(null, BlockPos.containing(at), impactSound(spec.element()), SoundSource.PLAYERS, 1.1f, spec.trueMagic() ? 0.8f : 1.1f);
        if (spec.trueMagic()) TrueBlocks.place(c.level, c.ownerId, at, spec.element(), TrueBlocks.mayBuild(p), now);
    }

    /** The damage of one hit before the balance law: the growth of the distance, the splash share, and for True Magic the cancelled resistance, the devil's weakness, the dense mana. */
    static double damageFor(Cast c, LivingEntity t, boolean main) {
        RuneSpec s = c.spec;
        double raw = RuneRules.rawDamage(s) * RuneRules.damageFactor(s, c.travelled);
        if (!main) raw *= RuneRules.SPLASH_SHARE;
        if (s.trueMagic()) {
            boolean boss = BalanceLaw.isBoss(t);
            raw *= RuneRules.bypassFor(resistance(t), boss) * TrueMagicRules.powerMult(c.density) * TrueMagicRules.devilMult(TrueMagic.isDevil(t), boss);
        }
        return raw;
    }

    /** Tensura's magic resistance of a creature as 0..1 (0 when the attribute is missing). */
    public static double resistance(LivingEntity e) {
        try {
            AttributeInstance a = e.getAttribute(TensuraAttributes.MAGIC_RESISTANCE);
            return a == null ? 0.0 : RuneRules.resistanceOf(a.getValue());
        } catch (Throwable unknown) {
            return 0.0;
        }
    }

    /** The short, capped rider of the element (control goes through the balance law's caps). */
    private static void rider(RuneElement el, LivingEntity t, boolean boss) {
        switch (el) {
            case FIRE -> t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), boss ? 30 : 80));
            case WATER -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 40), 0, false, true));
            case WIND -> { t.setDeltaMovement(t.getDeltaMovement().add(0, boss ? 0.15 : 0.45, 0)); t.hurtMarked = true; }
            case EARTH -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 50), 1, false, true));
            case LIGHTNING -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.controlTicks(t, 50), 0, false, true));
            case PLANT -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 60), 1, false, true));
        }
    }

    static ParticleOptions trailParticle(RuneElement el) {
        return switch (el) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.BUBBLE;
            case WIND -> ParticleTypes.CLOUD;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case PLANT -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    static ParticleOptions burstParticle(RuneElement el) {
        return switch (el) {
            case FIRE -> ParticleTypes.LAVA;
            case WATER -> ParticleTypes.SPLASH;
            case WIND -> ParticleTypes.CLOUD;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case PLANT -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    private static SoundEvent impactSound(RuneElement el) {
        return switch (el) {
            case FIRE, EARTH -> SoundEvents.GENERIC_EXPLODE.value();
            case WATER -> SoundEvents.PLAYER_SPLASH;
            case WIND -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case PLANT -> SoundEvents.GRASS_BREAK;
        };
    }

    /** The old fuel idea of the True Magic page: the environment's dense mana pays a share of the cost back. */
    public static void refundFuel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, double density) {
        try {
            double cost = b.getMagiculeCost(p, i, mode);
            EnergyHelper.gainMagicule(p, cost * TrueMagicRules.fuelShare(density), EnergyHelper.GainType.NORMAL);
        } catch (Throwable ignored) { /* no Tensura data: nothing to refund */ }
    }

    // ---------------------------------------------------------------- clean-up
    /** Drops every cast of one player (death, logout, dimension change). */
    public static void cancelOwner(UUID owner) {
        if (ACTIVE.isEmpty()) return;
        ACTIVE.removeIf(c -> c.ownerId.equals(owner));
    }

    public static void clearAll() { ACTIVE.clear(); }

    public static void onDeath(LivingDeathEvent e) {
        if (!ACTIVE.isEmpty() && e.getEntity() instanceof ServerPlayer p) cancelOwner(p.getUUID());
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!ACTIVE.isEmpty() && e.getEntity() instanceof ServerPlayer p) cancelOwner(p.getUUID());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!ACTIVE.isEmpty() && e.getEntity() instanceof ServerPlayer p) cancelOwner(p.getUUID());
    }

    public static void onServerStopping(MinecraftServer server) {
        ACTIVE.clear();
        TrueBlocks.clearAll(server);
    }

    // ---------------------------------------------------------------- test hooks
    /** The bolt of the first cast of an owner, or null: position and travelled distance (tests only). */
    static Cast castOf(UUID owner) {
        for (Cast c : ACTIVE) if (c.ownerId.equals(owner)) return c;
        return null;
    }
}
