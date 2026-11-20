package com.newuniverse.nusmp.meteor;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The one door to the shared meteor (docs/meteor_fall.md): {@code MeteorFall.drop(caster, impact, spec)} makes a real falling meteor that lands on {@code impact} after
 * {@code spec.fallTicks} and runs the caller's landing. Server only, server thread only.
 *
 * <p>{@code drop} returns false, and makes nothing, when: the setting {@code meteor.realMeteors} is off, the caster is not on a server level, the caster already has
 * {@code maxPerCaster} meteors in the air or the server has {@code maxGlobal}, the point is not a finite point within 160 blocks of the caster, or its chunk is not loaded. Every caller
 * then runs the code it ran before the meteor existed, so a refused meteor is never a lost ability.
 *
 * <p>A landing: the caller's {@code onImpact} (if any), else or additionally the generic landing (foes in the radius take damage and a push, one sound, one effect packet through the effect
 * budget). It never changes a block and never lights a fire. Meteors of a player are discarded when the player logs out, dies or changes dimension; all are dropped when the server stops.
 *
 * <p>Worst case: {@code maxGlobal} (24) meteors in flight = 24 entity spawns that the vanilla tracker sends only to players within range, no movement packets; one landing = one
 * bounded entity query (radius at most 16, at most 24 targets), up to three sounds and the caller's own effect packets (all through {@code PerfBudgets.effectPacket}).
 */
public final class MeteorFall {
    private static final Logger LOG = LogUtils.getLogger();
    /** The meteors in the air, by caster. Server thread only. */
    private static final Map<UUID, List<MeteorEntity>> OWNED = new HashMap<>();

    private MeteorFall() {}

    /** Is the setting on? Callers check this (or just call {@link #drop}) and keep their old code for a false. */
    public static boolean enabled() { return MeteorConfig.realMeteors(); }

    /** How many meteors are in the air for {@code caster}, and in the whole server. */
    public static int inFlight(UUID caster) {
        prune();
        List<MeteorEntity> l = OWNED.get(caster);
        return l == null ? 0 : l.size();
    }

    public static int inFlightTotal() {
        prune();
        int n = 0;
        for (List<MeteorEntity> l : OWNED.values()) n += l.size();
        return n;
    }

    /** Forgets the meteors that left the world (landed, discarded, unloaded). At most {@code maxGlobal} entries, so this is a handful of checks. */
    private static void prune() {
        OWNED.values().removeIf(l -> {
            l.removeIf(MeteorEntity::isRemoved);
            return l.isEmpty();
        });
    }

    /**
     * Drops one meteor onto {@code impact} (the point on the ground). True when the meteor is in the air (the caller's old code must NOT also run); false when nothing was made
     * (the caller runs its old code now). Never throws.
     */
    public static boolean drop(LivingEntity caster, Vec3 impact, MeteorSpec spec) {
        try {
            return dropChecked(caster, impact, spec);
        } catch (RuntimeException e) {
            LOG.warn("meteor: could not drop a meteor, the caller keeps its old behaviour: {}", e.toString());
            return false;
        }
    }

    /**
     * {@link #drop}, and when no meteor was made (setting off, caps, bad point) runs {@code old} now: the code the ability ran before the meteor existed, usually its own delayed task.
     * Returns true when the meteor flew. This is what the wired abilities call.
     */
    public static boolean dropOr(LivingEntity caster, Vec3 impact, MeteorSpec spec, Runnable old) {
        if (drop(caster, impact, spec)) return true;
        old.run();
        return false;
    }

    /** The default landing picture of a style (one effect packet through the effect budget): for callers whose old code only drew a streak from the sky. */
    public static void burst(ServerLevel level, Vec3 at, MeteorStyle style, double radius) {
        VfxSpawn.send(level, impactShape(style), at, at.add(0, 1, 0), style.core, 24, (float) Math.max(0.5, radius / 3.0));
    }

    private static boolean dropChecked(LivingEntity caster, Vec3 impact, MeteorSpec spec) {
        if (caster == null || impact == null || spec == null || !enabled() || !(caster.level() instanceof ServerLevel level)) return false;
        if (!finite(impact) || caster.distanceToSqr(impact) > 160.0 * 160.0 || !level.hasChunkAt(BlockPos.containing(impact))) return false;
        if (!MeteorRules.canLaunch(inFlight(caster.getUUID()), inFlightTotal(), MeteorConfig.maxPerCaster(), MeteorConfig.maxGlobal())) return false;
        MeteorSpec s = spec.clamped(MeteorConfig.maxFallTicks());
        MeteorEntity m = MeteorEntities.METEOR.get().create(level);
        if (m == null) return false;
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double[] st = MeteorRules.start(new double[]{impact.x, impact.y + 0.55 * s.size(), impact.z}, s.fallTicks(), angle);
        Vec3 start = new Vec3(st[0], st[1], st[2]);
        m.setup(s, caster.getUUID(), impact, start);
        if (!level.addFreshEntity(m)) return false;
        OWNED.computeIfAbsent(caster.getUUID(), k -> new ArrayList<>(2)).add(m);
        level.playSound(null, BlockPos.containing(start), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.4f, 0.55f);
        return true;
    }

    private static boolean finite(Vec3 v) { return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z); }

    /** Called by the entity at the end of its fall: runs the landing once and removes the meteor. */
    static void land(MeteorEntity m) {
        ServerLevel level = m.serverLevel();
        MeteorSpec spec = m.spec();
        if (level == null || spec == null) {
            m.discard();
            return;
        }
        Vec3 at = m.impact();
        boolean loaded = level.hasChunkAt(BlockPos.containing(at));
        UUID id = m.casterId();
        m.discard();                                                                      // gone first: a landing that throws must not leave a ghost
        if (!loaded) return;                                                              // nobody is there to see it: the chunk went away during the fall
        Entity e = id == null ? null : level.getEntity(id);
        LivingEntity caster = e instanceof LivingEntity le && le.isAlive() ? le : null;
        if (spec.onImpact() != null) {
            try {
                spec.onImpact().land(level, at, caster);
            } catch (RuntimeException ex) {
                LOG.warn("meteor: the landing of a caller threw {}", ex.toString());
            }
        }
        if (spec.damage() > 0f && caster instanceof ServerPlayer sp) genericLanding(level, at, spec, sp, m);
        if (spec.onImpact() == null) {
            level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6f, 0.75f);
            burst(level, at, spec.style(), spec.radius());
        }
    }

    /** The landing of a spec with damage: the foes around the point, the nearest {@link MeteorRules#MAX_TARGETS}, each hurt by its share and pushed away. */
    static void genericLanding(ServerLevel level, Vec3 at, MeteorSpec spec, ServerPlayer caster, MeteorEntity m) {
        double r = spec.radius();
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                t -> t != caster && t.isAlive() && !t.isSpectator() && !t.isAlliedTo(caster) && !(t instanceof Player p && p.isCreative())
                        && !(t instanceof OwnableEntity o && caster.getUUID().equals(o.getOwnerUUID())) && t.distanceToSqr(at) <= r * r);
        near.sort(Comparator.comparingDouble(t -> t.distanceToSqr(at)));
        DamageSource src = new DamageSource(damageType(level, spec.damageType()), m, caster);
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= MeteorRules.MAX_TARGETS) break;
            double f = MeteorRules.falloff(Math.sqrt(t.distanceToSqr(at)), r);
            if (f <= 0) continue;
            t.hurt(src, (float) (spec.damage() * f));
            if (spec.knockback() > 0) t.knockback(spec.knockback() * f, at.x - t.getX(), at.z - t.getZ());
        }
    }

    private static net.minecraft.core.Holder<DamageType> damageType(ServerLevel level, String id) {
        var reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? MeteorSpec.MAGIC : id);
        if (rl != null) {
            var h = reg.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, rl));
            if (h.isPresent()) return h.get();
        }
        return reg.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC);
    }

    /** The existing landing picture of each style (shapes the mod already draws). */
    static VfxShape impactShape(MeteorStyle s) {
        return switch (s) {
            case FIRE -> VfxShape.FIRE_BURST;
            case ICE -> VfxShape.DEMON_ICE_FX3;
            case KI -> VfxShape.KI_SHOCKWAVE;
            case DARK, HOLY, STAR -> VfxShape.MAGIC_CIRCLE_EXPLOSION;
        };
    }

    // ---------------------------------------------------------------- cleanup
    /** Takes every meteor of {@code caster} out of the air (logout, death, dimension change): nothing lands. */
    static void clear(UUID caster) {
        List<MeteorEntity> l = OWNED.get(caster);
        if (l == null) return;
        for (MeteorEntity m : new ArrayList<>(l)) m.discard();
        OWNED.remove(caster);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p.getUUID()); }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p.getUUID()); }

    public static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p.getUUID()); }

    public static void onServerStopped(ServerStoppedEvent e) {
        OWNED.clear();
    }
}
