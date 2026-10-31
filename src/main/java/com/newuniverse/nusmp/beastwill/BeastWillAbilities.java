package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: what the twelve KINDS of ability do (3.45.0). A will file picks a kind and sets the numbers; the behaviour is here, and every behaviour is bounded in the same few ways: it works on at most
 * {@code maxTargets} creatures (the file's number, cut by the server's {@code maxTargetsCap} and the kind's own limit), inside a radius or range that the validator already capped, never places a block and
 * never spawns an entity (plants and shadows are particles and vanilla timed effects, so there is nothing to clean up later and nothing a protection can be bypassed with), sends a bounded number of
 * particles through the perf budget, and picks its targets through {@link SongTargets#isFoe} (allies, pets, creative players and, with the server's PvP off, players are never targets) plus the team check, the
 * {@code abilitiesHitPlayers} setting and {@code mayInteract} for players (a protected spot can not be fought in). Bosses take the file's {@code bossFactor} of every effect (0 = immune). Hard control goes
 * through {@link BalanceLaw} (a shared 12 s cooldown, caps of 3 s on players and 1 s on bosses, diminishing returns). {@link #run} returns whether the ability took effect; the caller charges the energy,
 * the cooldown and the strain only then.
 */
public final class BeastWillAbilities {
    private BeastWillAbilities() {}

    /** How long the time-exclusion telegraph lasts before it takes hold: long enough to step out of the ring. */
    public static final int TELEGRAPH_TICKS = 15;

    private record Exclusion(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 center, long at, BeastWillDef.Ability ability, double scale) {}

    private static final Map<UUID, Exclusion> PENDING = new ConcurrentHashMap<>();

    /** True when this player has an unfinished time exclusion (the manager ticks it only then). */
    public static boolean hasPending(UUID id) { return PENDING.containsKey(id); }

    /** Forgets the unfinished time exclusion (death, logout, the form's end, a dimension change). */
    public static void cancel(UUID id) { PENDING.remove(id); }

    // ------------------------------------------------------------------ the entry point
    public static boolean run(ServerPlayer p, BeastWillDef def, BeastWillDef.Ability a, double scale, double mastery) {
        return switch (a.kind()) {
            case "dragon_sight" -> sight(p, a);
            case "draconic_pulse" -> pulse(p, a, scale, mastery);
            case "time_exclusion" -> exclusion(p, a, scale);
            case "shadow_veil" -> veil(p, a);
            case "shadow_dash" -> dash(p, a);
            case "ambush" -> ambush(p, a, scale, mastery);
            case "vine_lash" -> lash(p, a, scale, mastery);
            case "verdant_mend" -> mend(p, a, scale);
            case "restraining_vines" -> restrain(p, a);
            case "bark_ward" -> ward(p, a, scale);
            case "mane_surge" -> mane(p, a, scale);
            case "world_howl" -> howl(p, a, scale, mastery);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ targets
    static int cap(BeastWillDef.Ability a) {
        BeastWillRules.KindLimit k = BeastWillRules.KINDS.get(a.kind());
        return Math.max(1, Math.min(Math.min(a.maxTargets(), BeastWillConfig.maxTargetsCap()), k == null ? 1 : k.maxTargets()));
    }

    static boolean bossy(LivingEntity t) { return BossIdentity.isBoss(t) || BalanceLaw.isBoss(t); }

    /** May this creature be a target of the caster's ability at all (friend or foe, teams, the players setting, protection)? */
    static boolean allowed(ServerPlayer p, LivingEntity t) {
        if (!SongTargets.isFoe(p, t)) return false;
        if (t instanceof Player o) {
            if (!BeastWillConfig.hitPlayers()) return false;
            if (FtbTeamHelper.sameTeam(p.getUUID(), o.getUUID())) return false;
            if (!p.serverLevel().mayInteract(p, o.blockPosition())) return false;
        }
        return true;
    }

    private static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : SongTargets.foes(p, center, Math.max(0.5, radius))) if (allowed(p, t)) out.add(t);
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(center)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    private static List<LivingEntity> cone(ServerPlayer p, double range, double minDot, int max) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : SongTargets.foes(p, p.position(), Math.max(0.5, range))) {
            if (!allowed(p, t)) continue;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 1.0e-4 || to.scale(1.0 / len).dot(look) < minDot) continue;
            out.add(t);
        }
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    /** The damage of a hit: the mod's balanced spell damage, cut by the boss factor. Returns what was dealt. */
    private static float hit(ServerPlayer p, LivingEntity t, double raw, double mastery, double bossFactor) {
        float dmg = BalanceLaw.damage(p, t, (float) Math.max(0, raw), mastery);
        if (bossy(t)) dmg *= (float) bossFactor;
        if (dmg <= 0.01f) return 0f;
        t.hurt(p.damageSources().playerAttack(p), dmg);
        return dmg;
    }

    /** Ticks of a timed effect on a target: the boss factor applied, and a player's control time cut by the shared rules. */
    private static int ticksFor(LivingEntity t, BeastWillDef.Ability a, int base, boolean control) {
        int ticks = control ? BalanceLaw.controlTicks(t, base) : base;
        if (bossy(t)) ticks = (int) Math.floor(ticks * a.bossFactor());
        return ticks < 5 ? 0 : ticks;
    }

    private static void fx(ServerLevel level, ParticleOptions type, Vec3 at, int want, double spread) {
        int n = PerfBudgets.particles(level.getGameTime(), Math.min(want, 24));
        if (n > 0) level.sendParticles(type, at.x, at.y, at.z, n, spread, spread, spread, 0.02);
    }

    private static void line(ServerLevel level, ParticleOptions type, Vec3 from, Vec3 to, int steps) {
        Vec3 d = to.subtract(from);
        for (int i = 0; i <= steps; i++) fx(level, type, from.add(d.scale(i / (double) Math.max(1, steps))), 1, 0.05);
    }

    // ------------------------------------------------------------------ dragon
    private static boolean sight(ServerPlayer p, BeastWillDef.Ability a) {
        List<LivingEntity> seen = around(p, p.position(), a.radius(), cap(a));
        if (seen.isEmpty()) {
            BeastWillFeedback.say(p, "no_target", false);
            return false;
        }
        for (LivingEntity t : seen) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.max(20, a.durationTicks()), 0, false, false));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.35f, 1.6f);
        fx(p.serverLevel(), ParticleTypes.DRAGON_BREATH, p.getEyePosition(), 12, 0.4);
        return true;
    }

    private static boolean pulse(ServerPlayer p, BeastWillDef.Ability a, double scale, double mastery) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double range = Math.max(2, a.range());
        for (LivingEntity t : cone(p, range, 0.6, cap(a))) {
            if (hit(p, t, a.power() * scale, mastery, a.bossFactor()) > 0 && !bossy(t) && a.power() >= 3) t.igniteForSeconds(2);
            t.push(look.x * 0.35, 0.08, look.z * 0.35);
            t.hurtMarked = true;
        }
        line(level, ParticleTypes.DRAGON_BREATH, eye.add(look.scale(0.8)), eye.add(look.scale(Math.min(range, 10))), 8);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.5f, 1.2f);
        return true;
    }

    /** A time exclusion: a telegraph ring at the caster's spot, and 0.75 s later the foes still inside it are held for a few ticks. Nothing is paused: the game, the server and everyone outside the ring run on. */
    private static boolean exclusion(ServerPlayer p, BeastWillDef.Ability a, double scale) {
        if (PENDING.containsKey(p.getUUID())) return false;
        if (around(p, p.position(), a.radius(), cap(a)).isEmpty()) {
            BeastWillFeedback.say(p, "no_target", false);
            return false;
        }
        if (!BalanceLaw.beginControl(p)) {
            BeastWillFeedback.say(p, "control_cooldown", false, BalanceLaw.controlCooldownLeft(p) / 20);
            return false;
        }
        PENDING.put(p.getUUID(), new Exclusion(p.level().dimension(), p.position(), p.level().getGameTime() + TELEGRAPH_TICKS, a, scale));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 0.5f);
        ring(p.serverLevel(), p.position(), a.radius());
        return true;
    }

    /** Called every tick for a player with an unfinished exclusion. */
    public static void tickPending(ServerPlayer p) {
        Exclusion e = PENDING.get(p.getUUID());
        if (e == null) return;
        long now = p.level().getGameTime();
        if (p.level().dimension() != e.dimension() || !p.isAlive()) {
            PENDING.remove(p.getUUID());
            return;
        }
        if (now < e.at()) {
            if ((e.at() - now) % 5 == 0) ring(p.serverLevel(), e.center(), e.ability().radius());
            return;
        }
        PENDING.remove(p.getUUID());
        BeastWillDef.Ability a = e.ability();
        int held = 0;
        for (LivingEntity t : around(p, e.center(), a.radius(), cap(a))) {
            if (bossy(t) && a.bossFactor() <= 0) continue;                                   // bosses are immune when the file says so
            int ticks = ticksFor(t, a, a.durationTicks(), true);
            if (ticks <= 0) continue;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 3, false, false));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false));
            if (t instanceof Mob m) m.getNavigation().stop();
            t.setDeltaMovement(Vec3.ZERO);
            t.hurtMarked = true;
            fx(p.serverLevel(), ParticleTypes.REVERSE_PORTAL, t.getBoundingBox().getCenter(), 8, 0.3);
            held++;
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Math.max(20, a.durationTicks()), 0, false, false));
        if (held == 0) BeastWillFeedback.say(p, "slipped_out", false);
    }

    private static void ring(ServerLevel level, Vec3 c, double r) {
        for (int i = 0; i < 12; i++) {
            double t = i * Math.PI * 2 / 12;
            fx(level, ParticleTypes.END_ROD, new Vec3(c.x + Math.cos(t) * r, c.y + 0.1, c.z + Math.sin(t) * r), 1, 0.0);
        }
    }

    // ------------------------------------------------------------------ shadow
    private static boolean veil(ServerPlayer p, BeastWillDef.Ability a) {
        BeastWillStealth.veil(p, Math.max(20, a.durationTicks()));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.15f, 1.8f);
        fx(p.serverLevel(), ParticleTypes.SMOKE, p.position().add(0, 0.9, 0), 14, 0.3);
        return true;
    }

    /** A dash along the ground. Every half block of the path is checked: loaded, inside the world border, nothing solid or an entity that blocks, not lava, and a spot the player may interact with. */
    private static boolean dash(ServerPlayer p, BeastWillDef.Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0e-4) return false;
        dir = dir.normalize();
        Vec3 from = p.position(), best = from;
        double range = Math.max(2, Math.min(a.range(), BeastWillRules.MAX_RANGE));
        for (double d = 0.5; d <= range; d += 0.5) {
            Vec3 next = from.add(dir.scale(d));
            BlockPos bp = BlockPos.containing(next);
            AABB box = p.getBoundingBox().move(next.subtract(from));
            if (!level.hasChunkAt(bp) || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(p, box)) break;
            if (level.getFluidState(bp).is(FluidTags.LAVA) || level.getFluidState(bp.below()).is(FluidTags.LAVA)) break;
            if (!level.mayInteract(p, bp)) break;
            best = next;
        }
        if (best.distanceToSqr(from) < 1.0) {
            BeastWillFeedback.say(p, "dash_blocked", false);
            return false;
        }
        fx(level, ParticleTypes.SMOKE, from.add(0, 0.9, 0), 10, 0.3);
        p.teleportTo(best.x, best.y, best.z);
        p.fallDistance = 0;
        fx(level, ParticleTypes.SMOKE, best.add(0, 0.9, 0), 10, 0.3);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 1.7f);
        return true;
    }

    private static boolean ambush(ServerPlayer p, BeastWillDef.Ability a, double scale, double mastery) {
        List<LivingEntity> seen = cone(p, Math.max(2, a.range()), 0.7, 1);
        if (seen.isEmpty()) {
            BeastWillFeedback.say(p, "no_target", false);
            return false;
        }
        LivingEntity t = seen.get(0);
        ServerLevel level = p.serverLevel();
        Vec3 facing = Vec3.directionFromRotation(0, t.getYRot());
        Vec3 behind = t.position().subtract(facing.scale(1.3));
        BlockPos bp = BlockPos.containing(behind);
        AABB box = p.getBoundingBox().move(behind.subtract(p.position()));
        boolean placed = level.hasChunkAt(bp) && level.getWorldBorder().isWithinBounds(box) && level.noCollision(p, box) && level.mayInteract(p, bp)
                && behind.distanceToSqr(p.position()) <= (a.range() + 2) * (a.range() + 2);
        if (!placed && p.distanceToSqr(t) > 9.0) {
            BeastWillFeedback.say(p, "dash_blocked", false);
            return false;
        }
        boolean unseen = BeastWillStealth.veiled(p);
        if (placed) {
            fx(level, ParticleTypes.SMOKE, p.position().add(0, 0.9, 0), 8, 0.3);
            p.teleportTo(behind.x, behind.y, behind.z);
            fx(level, ParticleTypes.SMOKE, behind.add(0, 0.9, 0), 8, 0.3);
        }
        hit(p, t, a.power() * scale * (unseen ? 1.5 : 1.0), mastery, a.bossFactor());
        BeastWillStealth.reveal(p);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 0.8f);
        return true;
    }

    // ------------------------------------------------------------------ elderwood
    private static boolean lash(ServerPlayer p, BeastWillDef.Ability a, double scale, double mastery) {
        List<LivingEntity> seen = cone(p, Math.max(2, a.range()), 0.5, cap(a));
        if (seen.isEmpty()) {
            BeastWillFeedback.say(p, "no_target", false);
            return false;
        }
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.4, 0);
        for (LivingEntity t : seen) {
            hit(p, t, a.power() * scale, mastery, a.bossFactor());
            int ticks = ticksFor(t, a, 20, false);
            if (ticks > 0) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, true));
            line(level, ParticleTypes.COMPOSTER, from, t.getBoundingBox().getCenter(), 6);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
        return true;
    }

    private static boolean mend(ServerPlayer p, BeastWillDef.Ability a, double scale) {
        boolean poisoned = p.hasEffect(MobEffects.POISON);
        if (p.getHealth() >= p.getMaxHealth() - 0.5f && !poisoned) {
            BeastWillFeedback.say(p, "full_health", false);
            return false;
        }
        BalanceLaw.heal(p, (float) Math.max(0.5, a.power() * 2.0 * scale));
        p.removeEffect(MobEffects.POISON);
        fx(p.serverLevel(), ParticleTypes.HAPPY_VILLAGER, p.position().add(0, 1.0, 0), 14, 0.4);
        return true;
    }

    /** Roots: Slowness VII for the capped control time. The vines are particles; there is no block to place and so nothing to clean up. Players get the shared hard-control rules, bosses the boss factor. */
    private static boolean restrain(ServerPlayer p, BeastWillDef.Ability a) {
        List<LivingEntity> seen = around(p, p.position(), a.radius(), cap(a));
        if (seen.isEmpty()) {
            BeastWillFeedback.say(p, "no_target", false);
            return false;
        }
        if (!BalanceLaw.beginControl(p)) {
            BeastWillFeedback.say(p, "control_cooldown", false, BalanceLaw.controlCooldownLeft(p) / 20);
            return false;
        }
        int rooted = 0;
        for (LivingEntity t : seen) {
            int ticks = ticksFor(t, a, a.durationTicks(), true);
            if (ticks <= 0) continue;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
            if (t instanceof Mob m) m.getNavigation().stop();
            fx(p.serverLevel(), ParticleTypes.COMPOSTER, t.position().add(0, 0.4, 0), 10, 0.35);
            rooted++;
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
        if (rooted == 0) BeastWillFeedback.say(p, "resisted", false);
        return true;
    }

    private static boolean ward(ServerPlayer p, BeastWillDef.Ability a, double scale) {
        int ticks = Math.max(20, a.durationTicks());
        int amp = (int) Math.min(2, Math.floor(a.power() * scale * 1.2));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, amp, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, false));
        fx(p.serverLevel(), ParticleTypes.COMPOSTER, p.position().add(0, 1.0, 0), 16, 0.5);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    // ------------------------------------------------------------------ lion
    private static boolean mane(ServerPlayer p, BeastWillDef.Ability a, double scale) {
        int ticks = Math.max(20, a.durationTicks());
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, a.power() * scale >= 1.5 ? 1 : 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, false));
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        fx(p.serverLevel(), ParticleTypes.FLAME, p.position().add(0, 1.0, 0), 14, 0.4);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.4f, 1.3f);
        return true;
    }

    /** The roar: foes in the radius are pushed away and slowed for a moment (a player only pushed unless the shared control rule allows more); bosses take the boss factor. */
    private static boolean howl(ServerPlayer p, BeastWillDef.Ability a, double scale, double mastery) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> seen = around(p, p.position(), a.radius(), cap(a));
        boolean control = false;
        boolean askedControl = false;
        for (LivingEntity t : seen) {
            double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
            double strength = Math.min(1.8, a.power() * 0.6 * scale) * (bossy(t) ? a.bossFactor() : 1.0);
            if (strength > 0.05) t.knockback(strength, dx, dz);
            t.hurtMarked = true;
            hit(p, t, a.power() * scale * 0.5, mastery, a.bossFactor());
            if (t instanceof Player) {
                if (!askedControl) {
                    askedControl = true;
                    control = BalanceLaw.beginControl(p);
                }
                if (!control) continue;
            }
            int ticks = ticksFor(t, a, Math.min(a.durationTicks(), 40), t instanceof Player);
            if (ticks > 0) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, true));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.4f, 0.55f);
        ring(level, p.position(), Math.min(a.radius(), 12));
        fx(level, ParticleTypes.SONIC_BOOM, p.position().add(0, 1.2, 0), 1, 0.0);
        return true;
    }
}
