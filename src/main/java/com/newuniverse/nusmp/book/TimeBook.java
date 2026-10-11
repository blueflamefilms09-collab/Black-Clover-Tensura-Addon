package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Time Magic (Julius). Gold casting circles; the spells themselves are drawn after the anime by TimeMagicLayer: Chrono Stasis is a
 * glass sphere with a ribbon of Roman numerals orbiting it, Chrono Anastasis a huge clock face raining gold light.
 *
 * <p>Passive, Time Sense (always on while you hold this book):
 * <ul>
 *   <li>Deceleration: enemy projectiles that come within 4 blocks (6 mastered) are caught in a small stasis bubble and lose 60% of
 *       their speed, once each. At most 3 per moment.</li>
 *   <li>Reversal: 3 s after you are hurt, 20% (35% mastered) of the damage taken in that window is rewound, capped at 4 hearts.
 *       Then it rests for 10 s.</li>
 * </ul>
 */
public class TimeBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.zone("chrono_stasis", "Chrono Stasis", TimeBook::stasis).withCooldown(200),
            BookPage.signature("grigora", "Chrono Stasis Grigora", TimeBook::grigora).withCooldown(600),
            BookPage.mid("acceleration", "Time Acceleration", TimeBook::accel).withCooldown(240),
            BookPage.zone("reversal", "Time Reversal", TimeBook::reversal).withCooldown(400),
            BookPage.starter("stolen_time", "Stolen Time", TimeBook::stolen).withCooldown(160),
            // appended last so the existing pages keep their mode numbers and unlock bits
            BookPage.signature("chrono_anastasis", "Chrono Anastasis", TimeBook::anastasis).withCooldown(1200),
            // 0.61: new Julius-inspired time techniques are appended to preserve every existing mode index.
            BookPage.mid("hourglass_step", "Hourglass Step", TimeBook::hourglassStep).withCooldown(180),
            BookPage.mid("future_sight", "Borrowed Future", TimeBook::futureSight).withCooldown(320),
            BookPage.zone("pendulum_ward", "Pendulum Ward", TimeBook::pendulumWard).withCooldown(500),
            BookPage.zone("age_breaker", "Age Breaker", TimeBook::ageBreaker).withCooldown(280),
            BookPage.signature("moment_reprise", "Moment Reprise", TimeBook::momentReprise).withCooldown(700),
            BookPage.signature("hourglass_storm", "Hourglass Storm", TimeBook::hourglassStorm).withCooldown(900));

    public TimeBook() { super(MagicType.TIME, 0xFFF5D76E); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** One target stops 3 s (1 s bosses). Not invulnerable: 4 hearts break it. */
    static boolean stasis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 12);
        if (t == null) { fail(p, "There is nothing there to stop."); return false; }
        if (!control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 60);
        TimeStop.freeze(t, ticks);
        b.castCircle(p, 0.8f);
        stasisFx(p.serverLevel(), t, ticks);
        p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    /** 6-block zone, 3 s, caster rooted. Sneak at cast to spare players. Shares the control lock. */
    static boolean grigora(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!control(p)) return false;
        boolean spare = p.isShiftKeyDown();
        Vec3 c = p.position();
        int bubbles = 0;
        for (Entity e : p.serverLevel().getEntities(p, new AABB(c, c).inflate(6))) {
            if (e.distanceToSqr(c) > 36 || (spare && e instanceof Player)) continue;
            int ticks;
            if (e instanceof LivingEntity le) TimeStop.freeze(le, ticks = BalanceLaw.controlTicks(le, 60));
            else if (e instanceof Projectile) TimeStop.freeze(e, ticks = 60);
            else continue;
            if (bubbles++ < GRIGORA_BUBBLES) stasisFx(p.serverLevel(), e, ticks);   // one Chrono Stasis sphere per frozen target
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6, false, false));
        b.vfx(p, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), 60, 6 / 1.6f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2f, 0.5f);
        return true;
    }

    static boolean accel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 240, 0));
        b.castCircle(p, 0.8f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.TIME_ACCEL, p, p.position().add(0, 1, 0), 0, 40, 1.0f);
        return true;
    }

    static boolean reversal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = aim(p, 12);
        int n = com.newuniverse.nusmp.blackclover.BlockHistory.rewindCube(p.serverLevel(), BlockPos.containing(at), 1, 8, 1200);
        if (n == 0) { fail(p, "Nothing here remembers being whole."); return false; }
        b.castCircle(p, 0.8f);
        VfxSpawn.send(p.serverLevel(), VfxShape.TIME_REWIND, at.add(0, 0.05, 0), at.add(0, 1, 0), 0, 40, 1.6f);
        return true;
    }

    /** The slash is the spell; the brief slowness on both sides is the rider. */
    static boolean stolen(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        for (LivingEntity t : around(p, p.position(), 4)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.normalize().dot(look) < 0.55) continue;
            b.hurt(i, p, t, mode, 8f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.WIND_SLASH, eye, eye.add(look.scale(4)), 12, 0.8f);
        return true;
    }
    // ---------------------------------------------------------------- Chrono Anastasis
    /**
     * A clock face opens over a wide area and turns time back under it: broken blocks of the last minute climb back, the caster and
     * their allies (every player when nobody is on a team) get a quarter of their health back, and enemy projectiles in the air are
     * uncast. 12 blocks (16 mastered).
     */
    static boolean anastasis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 at = aim(p, 24);
        double r = i.isMastered(p) ? 16 : 12;
        AABB box = new AABB(at, at).inflate(r);
        int blocks = com.newuniverse.nusmp.blackclover.BlockHistory.rewind(level, BlockPos.containing(at), r, 1200);
        int healed = 0, uncast = 0;
        for (Player pl : level.getEntitiesOfClass(Player.class, box, x -> x.isAlive() && !x.isSpectator() && x.distanceToSqr(at) <= r * r)) {
            boolean ally = pl == p || p.isAlliedTo(pl) || (p.getTeam() == null && pl.getTeam() == null);
            if (!ally || pl.getHealth() >= pl.getMaxHealth()) continue;
            pl.heal(pl.getMaxHealth() * 0.25f);
            pl.clearFire();
            healed++;
        }
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box, x -> x.distanceToSqr(at) <= r * r)) {
            Entity owner = pr.getOwner();
            if (owner == p || (owner != null && p.isAlliedTo(owner))) continue;
            pr.discard();
            uncast++;
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, new VfxPayload(VfxShape.TIME_CLOCK.ordinal(), at, at.add(0, r * 0.5 + 6, 0), 0, 80, (float) r, -1, level.random.nextLong()));
        level.playSound(null, BlockPos.containing(at), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0f, 0.5f);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Time turns back: " + blocks + " blocks, " + healed + " wounds, "
                + uncast + " spells undone.").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        return true;
    }

    /** Short, collision-safe rewind along the caster's horizontal line of sight. */
    static boolean hourglassStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        Vec3 destination = null;
        for (double distance = 7; distance >= 1; distance -= 0.5) {
            Vec3 candidate = from.add(dir.scale(distance));
            if (p.serverLevel().noCollision(p, p.getBoundingBox().move(candidate.subtract(from)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null) {
            fail(p, "There is no room to step through time.");
            return false;
        }
        p.teleportTo(destination.x, destination.y, destination.z);
        p.fallDistance = 0;
        b.castCircle(p, 0.7f);
        VfxSpawn.send(p.serverLevel(), VfxShape.TIME_REWIND, from.add(0, 1, 0), destination.add(0, 1, 0), 0, 18, 0.8f);
        return true;
    }

    /** Briefly accelerates the caster's reactions without granting damage immunity. */
    static boolean futureSight(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int duration = i.isMastered(p) ? 220 : 160;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
        b.castCircle(p, 0.9f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.TIME_ACCEL, p, p.position().add(0, 1, 0), 0, duration, 1.15f);
        return true;
    }

    /** A short-lived field that catches hostile projectiles and repeatedly slows nearby foes. */
    static boolean pendulumWard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 center = p.position();
        int duration = i.isMastered(p) ? 100 : 80;
        b.castCircle(p, 1.0f);
        VfxSpawn.sendFollowing(level, VfxShape.TIME_CLOCK, p, center.add(0, 1, 0), 0, duration, 3.5f);
        SpellRuntime.zone(level, duration, 5, age -> {
            if (!p.isAlive() || p.serverLevel() != level) return;
            AABB area = new AABB(p.position(), p.position()).inflate(5);
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
                Entity owner = projectile.getOwner();
                if (owner == p || (owner != null && p.isAlliedTo(owner))) continue;
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.55));
                projectile.hurtMarked = true;
            }
            if (age % 20 == 0) {
                for (LivingEntity target : around(p, p.position(), 4.5)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                }
            }
        });
        return true;
    }

    /** A time-shearing line that damages and briefly slows enemies, scaled by the normal grimoire damage law. */
    static boolean ageBreaker(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(14));
        int hits = 0;
        for (LivingEntity target : along(p, eye, end, 1.25)) {
            b.hurt(i, p, target, mode, 12f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            hits++;
        }
        if (hits == 0) {
            fail(p, "The time-shear found no target.");
            return false;
        }
        b.castCircle(p, 0.9f);
        VfxSpawn.send(p.serverLevel(), VfxShape.TIME_REWIND, eye, end, 0, 24, 1.1f);
        return true;
    }

    /** Restores the caster and nearby allies, clearing fire and harmful status effects. */
    static boolean momentReprise(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int restored = 0;
        double radius = i.isMastered(p) ? 8 : 6;
        for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new AABB(p.position(), p.position()).inflate(radius),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && (candidate == p || candidate.isAlliedTo(p) || (p.getTeam() == null && candidate.getTeam() == null)))) {
            float healing = Math.max(4f, ally.getMaxHealth() * 0.18f);
            BalanceLaw.heal(ally, healing);
            ally.clearFire();
            for (MobEffectInstance effect : new java.util.ArrayList<>(ally.getActiveEffects())) {
                if (!effect.getEffect().value().isBeneficial()) ally.removeEffect(effect.getEffect());
            }
            restored++;
        }
        if (restored == 0) {
            fail(p, "No allied moment could be restored.");
            return false;
        }
        b.castCircle(p, 1.15f);
        VfxSpawn.send(p.serverLevel(), VfxShape.TIME_REWIND, p.position().add(0, 0.1, 0), p.position().add(0, 2, 0), 0, 50, 2.2f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 0.7f);
        return true;
    }

    /** Delayed golden strikes converge on the aimed point; each enemy can be hit only once per pulse. */
    static boolean hourglassStorm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 center = aim(p, 20);
        double radius = i.isMastered(p) ? 5 : 4;
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.TIME_CLOCK, center.add(0, 7, 0), center, 0, 70, (float) radius);
        for (int strike = 0; strike < 5; strike++) {
            int index = strike;
            SpellRuntime.later(level, 8 + strike * 8, () -> {
                if (!p.isAlive() || p.serverLevel() != level) return;
                double angle = index * Math.PI * 2 / 5;
                Vec3 impact = center.add(Math.cos(angle) * radius * 0.55, 0, Math.sin(angle) * radius * 0.55);
                VfxSpawn.send(level, VfxShape.LIGHTNING_SPEAR, impact.add(0, 8, 0), impact, 0xFFFFD66E, 10, 1.0f);
                for (LivingEntity target : around(p, impact, 2.0)) {
                    b.hurt(i, p, target, mode, 8f);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
            });
        }
        return true;
    }

    // ---------------------------------------------------------------- VFX helpers
    private static final int GRIGORA_BUBBLES = 16;

    /** Chrono Stasis sphere around an entity for {@code ticks}: sized to enclose it, centred on its body, following it. */
    static void stasisFx(ServerLevel level, Entity e, int ticks) {
        float radius = Math.max(e.getBbHeight(), e.getBbWidth()) * 0.6f + 0.25f;
        Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
        VfxSpawn.send(level, new VfxPayload(VfxShape.TIME_STASIS.ordinal(), c, c, 0, ticks, radius / 0.6f, e.getId(), level.random.nextLong()));
    }

    // ---------------------------------------------------------------- passive: Time Sense
    private static final int REWIND_DELAY = 60, REWIND_REST = 200, DECEL_PER_MOMENT = 3;
    private static final float REWIND_CAP = 8f;
    private static final String DECEL_TAG = "nusmp_decelerated";

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        super.onTick(i, e);
        if (!(e instanceof ServerPlayer p) || !p.isAlive()) return;
        long now = p.level().getGameTime();
        CompoundTag tag = i.getOrCreateTag();
        if (tag.getLong("PassiveAt") == now) return;                          // 0.49: once a tick, whoever drives it (see passiveTick)
        tag.putLong("PassiveAt", now);
        // Reversal: the wound from 3 s ago is partly undone
        long at = tag.getLong("RewindAt");
        if (at > 0 && now >= at) {
            float heal = Math.min(tag.getFloat("RewindHeal"), REWIND_CAP);
            tag.putLong("RewindAt", 0);
            tag.putFloat("RewindHeal", 0);
            tag.putLong("RewindReady", now + REWIND_REST);
            if (heal > 0.01f && p.getHealth() < p.getMaxHealth()) {
                p.heal(heal);
                VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.TIME_REWIND, p, p.position().add(0, 1, 0), 0, 30, 0.9f);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.6f);
            }
        }
        // Deceleration: enemy projectiles closing in are caught in a small stasis bubble and slowed
        if (p.tickCount % 2 != 0) return;
        double r = i.isMastered(p) ? 6 : 4;
        int caught = 0;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
            if (caught >= DECEL_PER_MOMENT) break;
            Entity owner = pr.getOwner();
            if (owner == p || (owner != null && p.isAlliedTo(owner)) || pr.getPersistentData().getBoolean(DECEL_TAG)) continue;
            Vec3 v = pr.getDeltaMovement();
            if (v.lengthSqr() < 0.04 || v.dot(p.getBoundingBox().getCenter().subtract(pr.position())) <= 0) continue;   // resting or leaving
            pr.setDeltaMovement(v.scale(0.4));
            pr.hurtMarked = true;
            pr.getPersistentData().putBoolean(DECEL_TAG, true);
            stasisFx(p.serverLevel(), pr, 12);
            caught++;
        }
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        boolean r = super.onTakenDamage(i, owner, source, amount);
        float dmg = amount.get();
        if (dmg <= 0 || !(owner instanceof ServerPlayer p)) return r;
        long now = p.level().getGameTime();
        CompoundTag tag = i.getOrCreateTag();
        float share = dmg * (i.isMastered(p) ? 0.35f : 0.20f);
        if (tag.getLong("RewindAt") > now) {
            tag.putFloat("RewindHeal", tag.getFloat("RewindHeal") + share);      // same window: add to the pending rewind
        } else if (now >= tag.getLong("RewindReady")) {
            tag.putLong("RewindAt", now + REWIND_DELAY);
            tag.putFloat("RewindHeal", share);
        }
        return r;
    }

    /**
     * 0.49 fix: ManasCore does not tick passive skills, so Time Magic's passive (Reversal, Deceleration) never ran. The player
     * tick drives it here; onTick's once-a-tick guard keeps it single if ManasCore does tick it too.
     */
    public static void passiveTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(p).getSkill(com.newuniverse.nusmp.skill.NUSkills.BOOK_TIME.getId())
                .ifPresent(i -> { if (i.getSkill() instanceof TimeBook b) b.onTick(i, p); });
    }
}
