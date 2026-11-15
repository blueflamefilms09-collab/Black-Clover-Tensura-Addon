package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.perf.PerfParticles;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: what each of the eight forms DOES, on the server only: the plain right-click of the forms that have one (Black Flames, Spatial Threads, the piercing thrust, the blink strike, the lock-on arrow), the eight Secret
 * Techniques ({@link GreedRules.Form#techniqueEnglish}) and the Soul Sever of the scythe / Reflection Fortress pool of the shield. Every effect is bounded by construction: a box query along the look line, at most
 * {@link GreedRules#MAX_TARGETS} targets, a fixed number of particle bursts through the perf budget ({@link PerfParticles}), a cooldown at the caller. Nothing is scheduled, nothing is kept per tick: the only state is two
 * small maps keyed by player UUID (the shield's pool and the last reflection), cleared at logout, death and server stop ({@link #forget}, {@link #clearAll}).
 */
public final class GreedForms {
    private GreedForms() {}

    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.85f, 0.05f, 0.12f), 1.6f);
    private static final DustParticleOptions ZERO = new DustParticleOptions(new Vector3f(0.95f, 0.05f, 0.20f), 1.4f);
    static final String SEVER_KEY = "anime_multiverse_greed_sever";
    public static final float POOL_CAP = 400.0f;

    private static final Map<UUID, Float> POOL = new HashMap<>();
    private static final Map<UUID, Long> REFLECTED = new HashMap<>();

    public static void forget(UUID id) {
        POOL.remove(id);
        REFLECTED.remove(id);
    }

    public static void clearAll() {
        POOL.clear();
        REFLECTED.clear();
    }

    // ---------------------------------------------------------------- targeting
    /** A creature the wielder may harm: alive, not the wielder, not an ally or a tamed pet of the wielder, not a creative or spectator player. */
    public static boolean canHit(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (e instanceof Player pl && (pl.isCreative() || !p.canHarmPlayer(pl))) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return false;
        return !p.isAlliedTo(e);
    }

    /** The creatures within {@code radius} of the look line, {@code range} blocks long, nearest first, at most {@code cap}. One box query. */
    static List<LivingEntity> alongRay(ServerPlayer p, double range, double radius, int cap) {
        Vec3 from = p.getEyePosition(), look = p.getLookAngle(), to = from.add(look.scale(range));
        AABB box = new AABB(from, to).inflate(radius + 0.5);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, x -> canHit(p, x))) {
            Vec3 c = e.getBoundingBox().getCenter().subtract(from);
            double along = c.dot(look);
            if (along < 0.0 || along > range + 0.5) continue;
            double off = c.subtract(look.scale(along)).length();
            if (off <= radius + e.getBbWidth() * 0.5) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
    }

    /** The creatures within {@code radius} of the wielder, nearest first, at most {@code cap}. */
    static List<LivingEntity> around(ServerPlayer p, double radius, int cap) {
        List<LivingEntity> out = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), x -> canHit(p, x) && x.distanceToSqr(p) <= radius * radius));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
    }

    private static void hurt(ServerPlayer p, LivingEntity t, float amount, boolean magic) {
        t.invulnerableTime = 0;
        t.hurt(magic ? p.serverLevel().damageSources().magic() : p.serverLevel().damageSources().playerAttack(p), (float) (amount * GreedConfig.damage()));
    }

    private static void trail(ServerLevel level, ParticleOptions o, Vec3 from, Vec3 dir, double range, int every) {
        for (int i = 1; i <= range; i += every) {
            Vec3 c = from.add(dir.scale(i));
            PerfParticles.send(level, o, c.x, c.y, c.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void sound(ServerPlayer p, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        p.serverLevel().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, vol, pitch);
    }

    // ---------------------------------------------------------------- Soul Sever (scythe)
    public static void soulSever(LivingEntity t, long gameTime, int ticks) { t.getPersistentData().putLong(SEVER_KEY, gameTime + ticks); }

    public static boolean severed(LivingEntity t) { return t.getPersistentData().getLong(SEVER_KEY) > t.level().getGameTime(); }

    // ---------------------------------------------------------------- the plain right click
    /** The plain right click of a form (not the bow or the shield: those are drawn and held). Returns the cooldown in ticks, 0 when nothing happened. */
    public static int use(ServerPlayer p, GreedRules.Form form, GreedState st) {
        double atk = st.bonusAtk;
        switch (form) {
            case STAFF -> { flames(p, atk); return 25; }
            case GAUNTLETS -> { threads(p, p.isShiftKeyDown(), atk); return 20; }
            case SPEAR -> { return thrust(p, atk) ? 18 : 6; }
            case DUAL_BLADES -> { return blink(p, atk) ? 12 : 6; }
            default -> { return 0; }
        }
    }

    /** Black Flames: a 14 block cone that ignites, hurts and siphons magicule (a little of the caster's maximum per creature, at most five percent). */
    static void flames(ServerPlayer p, double atk) {
        List<LivingEntity> hit = alongRay(p, 14.0, 3.0, 12);
        for (LivingEntity t : hit) {
            t.setRemainingFireTicks(80);
            hurt(p, t, (float) (4.0 + atk * 0.3), true);
        }
        restoreMagicule(p, Math.min(5.0, hit.size()));
        Vec3 eye = p.getEyePosition();
        trail(p.serverLevel(), ParticleTypes.SOUL_FIRE_FLAME, eye, p.getLookAngle(), 14, 2);
        sound(p, SoundEvents.BLAZE_SHOOT, 0.8f, 0.6f);
    }

    /** Spatial Threads: up to ten creatures within 12 blocks are cut; sneaking yanks them in. */
    static void threads(ServerPlayer p, boolean yank, double atk) {
        List<LivingEntity> hit = alongRay(p, 12.0, 3.0, 10);
        Vec3 pos = p.position();
        for (LivingEntity t : hit) {
            hurt(p, t, (float) (3.0 + atk * 0.4), false);
            if (yank) {
                Vec3 d = pos.subtract(t.position()).normalize().scale(1.2);
                t.setDeltaMovement(d.x, 0.25, d.z);
                t.hurtMarked = true;
            }
            PerfParticles.send(p.serverLevel(), CRIMSON, t.getX(), t.getY() + 1.0, t.getZ(), 4, 0.2, 0.4, 0.2, 0.0);
        }
        sound(p, SoundEvents.CHAIN_PLACE, 1.0f, 1.4f);
    }

    /** The 8.5 block piercing thrust: it breaks the guard of a player who blocks (the shield is put on cooldown). */
    static boolean thrust(ServerPlayer p, double atk) {
        List<LivingEntity> hit = alongRay(p, 8.5, 1.0, 8);
        for (LivingEntity t : hit) {
            if (t instanceof Player pl && pl.isBlocking()) {
                pl.stopUsingItem();
                pl.getCooldowns().addCooldown(Items.SHIELD, 100);
            }
            hurt(p, t, (float) (10.0 + atk * 0.8), false);
        }
        trail(p.serverLevel(), CRIMSON, p.getEyePosition(), p.getLookAngle(), 8, 1);
        sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
        return !hit.isEmpty();
    }

    /** Level Zero's blink strike: behind the nearest creature on the look line within 14 blocks, two cuts. */
    static boolean blink(ServerPlayer p, double atk) {
        List<LivingEntity> hit = alongRay(p, 14.0, 2.5, 1);
        if (hit.isEmpty()) return false;
        LivingEntity t = hit.get(0);
        Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
        p.teleportTo(behind.x, t.getY(), behind.z);
        for (int i = 0; i < 2; i++) hurt(p, t, (float) (12.0 + atk * 1.0), false);
        PerfParticles.send(p.serverLevel(), ZERO, t.getX(), t.getY() + 1.0, t.getZ(), 16, 0.5, 0.7, 0.5, 0.08);
        sound(p, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.4f);
        return true;
    }

    /** The drawn bow: a lock-on arrow of magicule (the nearest creature near the look line, 48 blocks, else the look line). */
    public static void bolt(ServerPlayer p, int chargeTicks, double atk) {
        if (chargeTicks < 5) return;
        List<LivingEntity> hit = alongRay(p, 48.0, 3.0, 1);
        if (!hit.isEmpty()) {
            LivingEntity t = hit.get(0);
            hurt(p, t, (float) (6.0 + Math.min(chargeTicks, 20) * 0.4 + atk * 0.6), true);
        }
        trail(p.serverLevel(), ParticleTypes.SOUL_FIRE_FLAME, p.getEyePosition(), p.getLookAngle(), 48, 4);
        sound(p, SoundEvents.ARROW_SHOOT, 1.0f, 0.7f);
    }

    // ---------------------------------------------------------------- the eight Secret Techniques
    /** Casts the Secret Technique of a form with the power of the sacrificed stats. */
    public static void secret(ServerPlayer p, GreedRules.Form form, double power) {
        switch (form) {
            case SWORD -> ptarmiganCross(p, power);
            case BOW -> piercingMeteor(p, power);
            case SCYTHE -> soulReap(p, power);
            case SHIELD -> reflectionFortress(p, power);
            case STAFF -> twilightHealing(p, power);
            case GAUNTLETS -> dimensionDestruction(p, power);
            case SPEAR -> revoltBrionac(p, power);
            case DUAL_BLADES -> infinityDivide(p, power);
        }
    }

    private static void ptarmiganCross(ServerPlayer p, double power) {
        ServerLevel level = p.serverLevel();
        Vec3 o = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity t : alongRay(p, 14.0, 2.0, GreedRules.MAX_TARGETS)) hurt(p, t, (float) (15.0 + power * 1.8), false);
        for (int step = 1; step <= 14; step += 2) {
            Vec3 c = o.add(look.scale(step));
            double s = 0.2 + step * 0.12;
            PerfParticles.send(level, CRIMSON, c.x + s, c.y + s, c.z, 1, 0, 0, 0, 0);
            PerfParticles.send(level, CRIMSON, c.x - s, c.y + s, c.z, 1, 0, 0, 0, 0);
            PerfParticles.send(level, CRIMSON, c.x + s, c.y - s, c.z, 1, 0, 0, 0, 0);
            PerfParticles.send(level, CRIMSON, c.x - s, c.y - s, c.z, 1, 0, 0, 0, 0);
        }
        sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
    }

    private static void piercingMeteor(ServerPlayer p, double power) {
        for (LivingEntity t : alongRay(p, 32.0, 1.4, GreedRules.MAX_TARGETS)) hurt(p, t, (float) (22.0 + power * 2.2), true);
        trail(p.serverLevel(), ParticleTypes.SOUL_FIRE_FLAME, p.getEyePosition(), p.getLookAngle(), 32, 2);
        sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.4f);
    }

    private static void soulReap(ServerPlayer p, double power) {
        long now = p.serverLevel().getGameTime();
        float heal = 0.0f;
        for (LivingEntity t : around(p, 4.5, GreedRules.MAX_TARGETS)) {
            soulSever(t, now, 300);
            hurt(p, t, (float) (25.0 + power * 2.5), true);
            heal += 3.0f;
        }
        p.heal(Math.min(30.0f, heal));
        PerfParticles.send(p.serverLevel(), ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1.0, p.getZ(), 6, 2.0, 0.5, 2.0, 0.0);
        sound(p, SoundEvents.WITHER_SHOOT, 0.9f, 0.8f);
    }

    /** Reflection Fortress: the damage the shield has absorbed (and the power paid) is released in an 8 block burst. */
    private static void reflectionFortress(ServerPlayer p, double power) {
        float pool = Math.min(POOL_CAP, POOL.getOrDefault(p.getUUID(), 0.0f) + (float) power);
        POOL.remove(p.getUUID());
        for (LivingEntity t : around(p, 8.0, GreedRules.MAX_TARGETS)) hurt(p, t, Math.max(6.0f, pool), true);
        PerfParticles.send(p.serverLevel(), CRIMSON, p.getX(), p.getY() + 1.0, p.getZ(), 40, 4.0, 0.8, 4.0, 0.1);
        sound(p, SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.2f);
    }

    private static void twilightHealing(ServerPlayer p, double power) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8.0), e -> e == p || e.isAlive() && p.isAlliedTo(e)));
        int n = 0;
        for (LivingEntity e : near) {
            if (n++ >= GreedRules.MAX_TARGETS) break;
            e.heal((float) (20.0 + power * 2.0));
            for (MobEffectInstance m : new ArrayList<>(e.getActiveEffects())) if (!m.getEffect().value().isBeneficial()) e.removeEffect(m.getEffect());
            PerfParticles.send(level, ParticleTypes.END_ROD, e.getX(), e.getY() + 1.0, e.getZ(), 10, 0.4, 0.8, 0.4, 0.04);
        }
        sound(p, SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.2f);
    }

    private static void dimensionDestruction(ServerPlayer p, double power) {
        for (LivingEntity t : alongRay(p, 10.0, 3.5, GreedRules.MAX_TARGETS)) {
            hurt(p, t, (float) (20.0 + power * 2.0), true);
            PerfParticles.send(p.serverLevel(), ParticleTypes.PORTAL, t.getX(), t.getY() + 1.0, t.getZ(), 12, 0.3, 0.6, 0.3, 0.3);
        }
        sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 1.6f);
    }

    /** Revolt Brionac: a 30 block lance that executes what it leaves under a quarter of its health. */
    private static void revoltBrionac(ServerPlayer p, double power) {
        for (LivingEntity t : alongRay(p, 30.0, 1.5, GreedRules.MAX_TARGETS)) {
            if (t instanceof Player pl && pl.isBlocking()) {
                pl.stopUsingItem();
                pl.getCooldowns().addCooldown(Items.SHIELD, 200);
            }
            hurt(p, t, (float) (28.0 + power * 2.8), true);
            if (t.isAlive() && t.getHealth() <= t.getMaxHealth() * 0.25f) hurt(p, t, t.getMaxHealth(), true);
        }
        trail(p.serverLevel(), CRIMSON, p.getEyePosition(), p.getLookAngle(), 30, 2);
        sound(p, SoundEvents.TRIDENT_THUNDER.value(), 1.5f, 1.0f);
    }

    private static void infinityDivide(ServerPlayer p, double power) {
        for (LivingEntity t : around(p, 10.0, GreedRules.MAX_TARGETS)) {
            for (int i = 0; i < 6; i++) hurt(p, t, (float) (18.0 + power * 1.1), true);
            PerfParticles.send(p.serverLevel(), ZERO, t.getX(), t.getY() + 1.0, t.getZ(), 20, 0.6, 0.8, 0.6, 0.12);
        }
        sound(p, SoundEvents.TRIDENT_THUNDER.value(), 1.8f, 1.3f);
    }

    // ---------------------------------------------------------------- the shield
    /** The shield took a hit: the damage is stored for Reflection Fortress and reflected once every half second. Called by the damage event only for a wielder who is blocking with the shield form. */
    public static void shieldTook(ServerPlayer p, float amount, LivingEntity attacker, long now, int tier) {
        POOL.merge(p.getUUID(), Math.min(amount, 100.0f), (a, b) -> Math.min(POOL_CAP, a + b));
        if (attacker == null || !canHit(p, attacker)) return;
        Long last = REFLECTED.get(p.getUUID());
        if (last != null && now - last < 10L) return;
        REFLECTED.put(p.getUUID(), now);
        float back = Math.min(60.0f, amount * 1.5f + tier * 1.0f);
        attacker.invulnerableTime = 0;
        attacker.hurt(p.serverLevel().damageSources().indirectMagic(p, p), (float) (back * GreedConfig.damage()));
    }

    public static float pool(UUID id) { return POOL.getOrDefault(id, 0.0f); }

    // ---------------------------------------------------------------- magicule
    /** Gives back {@code percent} of the caster's maximum magicule, never past it. */
    static void restoreMagicule(ServerPlayer p, double percent) {
        if (percent <= 0.0) return;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double max = Math.max(0.0, EnergyHelper.getMaxMagicule(p));
            if (max <= 0.0 || ex.getMagicule() >= max) return;
            ex.setMagicule(Math.min(max, ex.getMagicule() + max * percent / 100.0));
            ex.markDirty();
        } catch (RuntimeException | LinkageError ignored) {
            // no Tensura data on this player: nothing to restore
        }
    }
}
