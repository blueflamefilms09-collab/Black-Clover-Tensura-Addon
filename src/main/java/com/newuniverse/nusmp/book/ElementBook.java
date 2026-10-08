package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

/**
 * A grimoire family built from shared spell shapes (bolts, cones, novas, zones, dashes, constructs).
 * Every shape is a real hitbox through createSource + the balance law; potion effects only ride on hits.
 */
public class ElementBook extends GrimoireBook {
    private final ResourceKey<DamageType> damage;
    private final List<BookPage> pages;

    public ElementBook(MagicType magic, int color, ResourceKey<DamageType> damage, List<BookPage> pages) {
        super(magic, color);
        this.damage = damage;
        this.pages = pages;
    }

    /** 0.53: this book with extra pages appended (an attribute upgrade, see book.ext). */
    public ElementBook plus(List<BookPage> extra) {
        return extra.isEmpty() ? this : new ElementBook(magic, color, damage, com.newuniverse.nusmp.book.ext.Ext.join(pages, extra));
    }

    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return damage; }

    @Override
    public void vfx(ServerPlayer player, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape identity = switch (magic) {
            case STAR -> VfxShape.STAR_MAGIC;
            case SAND -> VfxShape.SAND_MAGIC;
            case MIST -> VfxShape.MIST_MAGIC;
            case BONE -> switch (shape) {
                case WATER_RING, WIND_RING, EARTH_SPIKES -> VfxShape.BONE_FX2;
                case WATER_SPLASH, MAGIC_CIRCLE_EXPLOSION, FLAME_EXPLOSION -> VfxShape.BONE_FX3;
                default -> VfxShape.BONE_FX1;
            };
            case BLOOD -> switch (shape) {
                case WATER_RING, WIND_RING, EARTH_SPIKES -> VfxShape.BLOOD_FX2;
                case WATER_SPLASH, MAGIC_CIRCLE_EXPLOSION, FLAME_EXPLOSION -> VfxShape.BLOOD_FX3;
                default -> VfxShape.BLOOD_FX1;
            };
            default -> shape;
        };
        super.vfx(player, identity, from, to, ticks, power);
        float scale = Math.max(0.35f, Math.min(3.5f, Math.abs(power)));
        float yaw = (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
        if (magic == MagicType.BONE) {
            PropKind prop = identity == VfxShape.BONE_FX2 ? PropKind.BONE_2 : PropKind.BONE_1;
            MagicProps.spawn(player.serverLevel(), prop, from, yaw, scale, Math.max(8, Math.min(ticks, 80)), 0, player);
        } else if (magic == MagicType.SAND) {
            PropKind prop = ticks >= 30 ? PropKind.SAND_2 : PropKind.SAND_1;
            MagicProps.spawn(player.serverLevel(), prop, from, yaw, scale, Math.max(8, Math.min(ticks, 80)), 0, player);
        } else if (magic == MagicType.RECOMBINATION) {
            PropKind prop = identity == VfxShape.RECOMBINE_CONSTRUCT ? PropKind.RECOMBINATION_1 : PropKind.RECOMBINATION_2;
            MagicProps.spawn(player.serverLevel(), prop, from, yaw, Math.max(0.8f, Math.min(scale, 2.5f)),
                    Math.max(12, Math.min(ticks, 100)), 0, player);
        }
    }

    /** What a hit does on top of damage. */
    @FunctionalInterface public interface Rider { void apply(LivingEntity target, ServerPlayer caster); }
    public static final Rider NONE = (t, p) -> {};

    public static Rider effect(Supplier<MobEffectInstance> e) { return (t, p) -> t.addEffect(e.get()); }
    public static Rider ignite(int s) { return (t, p) -> t.igniteForSeconds(s); }
    public static Rider knock(double k) { return (t, p) -> { Vec3 d = t.position().subtract(p.position()).normalize(); t.knockback(k, -d.x, -d.z); }; }
    public static Rider lift(double y) { return (t, p) -> { t.setDeltaMovement(t.getDeltaMovement().add(0, y, 0)); t.hurtMarked = true; }; }
    public static Rider leech(float hp) { return (t, p) -> BalanceLaw.heal(p, hp); }
    public static Rider strip() { return (t, p) -> {
        for (MobEffectInstance e : new java.util.ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); return; } }; }
    public static Rider all(Rider... rs) { return (t, p) -> { for (Rider r : rs) r.apply(t, p); }; }

    // ------------------------------------------------------------------ spell shapes
    /** A flying hitbox. aoe > 0 makes it burst on impact. */
    public static BookPage.Cast bolt(float dmg, double speed, double radius, int life, boolean pierce, double aoe,
                                     VfxShape trail, VfxShape burst, Rider rider) {
        return (b, i, p, mode) -> {
            float s = size(i, p);
            Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
            b.castCircle(p, 0.7f);
            if (trail != null) b.vfx(p, trail, start, start.add(dir.scale(speed * life)), life, 0.9f * s);
            SpellRuntime.bolt(p, start, dir.scale(speed), radius * s, life, pierce, null, (bolt, t) -> {
                if (aoe <= 0) { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); }
            }, (bolt, at) -> {
                if (aoe > 0) for (LivingEntity t : around(p, at, aoe * s)) { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); }
                if (burst != null) b.vfx(p, burst, at, at, 20, (float) Math.max(0.6, aoe / 3) * s);
                else b.impact(p, at, 0.5f);
            });
            return true;
        };
    }

    /** n bolts in a fan, optionally homing on the nearest enemies. */
    public static BookPage.Cast volley(int n, float dmg, double speed, boolean homing, VfxShape trail, Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
            List<LivingEntity> targets = around(p, p.position(), 16);
            b.castCircle(p, 0.9f);
            for (int k = 0; k < n; k++) {
                double off = (k - (n - 1) / 2.0) * 0.12;
                Vec3 d = dir.add(side.scale(off)).add(0, Math.abs(off) * 0.5, 0).normalize();
                LivingEntity home = homing && !targets.isEmpty() ? targets.get(k % targets.size()) : null;
                b.vfx(p, trail, start, home != null ? home.getBoundingBox().getCenter() : start.add(d.scale(20)), 14, 0.6f);
                SpellRuntime.bolt(p, start, d.scale(speed), 0.5, 30, false, home, (bolt, t) -> { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); },
                        (bolt, at) -> b.impact(p, at, 0.35f));
            }
            return true;
        };
    }

    /** A cone in front of you (slashes, roars, punches). */
    public static BookPage.Cast cone(float dmg, double range, double width, VfxShape shape, Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
            double r = range * size(i, p);
            for (LivingEntity t : around(p, p.position(), r)) {
                if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < width) continue;
                b.hurt(i, p, t, mode, dmg);
                rider.apply(t, p);
            }
            b.castCircle(p, 0.6f);
            b.vfx(p, shape, eye, eye.add(look.scale(r)), 14, (float) r / 4f);
            return true;
        };
    }

    /** A burst around you (or at the aim point when atAim). */
    public static BookPage.Cast nova(float dmg, double radius, boolean atAim, VfxShape shape, Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 c = atAim ? aim(p, 24) : p.position();
            double r = radius * size(i, p);
            for (LivingEntity t : around(p, c, r)) { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); }
            b.castCircle(p, 1f);
            b.vfx(p, shape, c, c.add(0, 1, 0), 24, (float) (r / 3));
            return true;
        };
    }

    /** A lingering field: every 'interval' ticks hits everything inside. */
    public static BookPage.Cast field(float dmg, double radius, int ticks, int interval, boolean atAim, VfxShape shape, Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 c = atAim ? aim(p, 24) : p.position();
            double r = radius * size(i, p);
            b.castCircle(p, 1f);
            b.vfx(p, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), ticks, (float) r / 1.6f);
            SpellRuntime.zone(p.serverLevel(), ticks, interval, age -> {
                Vec3 at = atAim ? c : p.position();
                for (LivingEntity t : around(p, at, r)) { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); }
                b.vfx(p, shape, at, at.add(0, 1, 0), Math.max(10, interval + 6), (float) r / 3f);
            });
            return true;
        };
    }

    /** An instant line (beams, spike rows). */
    public static BookPage.Cast line(float dmg, double length, double width, VfxShape shape, Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 a = shape == VfxShape.EARTH_SPIKES ? p.position() : p.getEyePosition();
            Vec3 dir = shape == VfxShape.EARTH_SPIKES ? p.getViewVector(1f).multiply(1, 0, 1).normalize() : p.getViewVector(1f);
            Vec3 end = a.add(dir.scale(length * size(i, p)));
            for (LivingEntity t : along(p, a, end, width)) { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); }
            b.castCircle(p, 0.8f);
            b.vfx(p, shape, a, end, 18, 1.2f);
            return true;
        };
    }

    /** Dash forward, hitting everything you pass. behind = blink behind the target instead. */
    public static BookPage.Cast dash(float dmg, double dist, boolean behind, VfxShape shape, Rider rider) {
        return (b, i, p, mode) -> {
            Vec3 from = p.position(), dest = null;
            if (behind) {
                LivingEntity t = target(p, 16);
                if (t == null) { fail(p, "No one to step behind."); return false; }
                Vec3 back = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
                if (p.level().noCollision(p, p.getBoundingBox().move(back.subtract(from)))) dest = back;
            } else {
                Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
                for (double d = dist; d >= 1; d -= 0.5) {
                    Vec3 c = from.add(dir.scale(d));
                    if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
                }
            }
            if (dest == null) { fail(p, "No room to move."); return false; }
            if (dmg > 0) for (LivingEntity t : along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.3)) { b.hurt(i, p, t, mode, dmg); rider.apply(t, p); }
            b.castCircle(p, 0.6f);
            p.teleportTo(dest.x, dest.y, dest.z);
            p.fallDistance = 0;
            b.vfx(p, shape, from.add(0, 1, 0), dest.add(0, 1, 0), 12, 1f);
            return true;
        };
    }

    /** Hard control on one target (shares the 12 s lock). */
    public static BookPage.Cast bind(float dmg, double range, boolean freeze, VfxShape shape, Rider rider) {
        return (b, i, p, mode) -> {
            LivingEntity t = target(p, range);
            if (t == null) { fail(p, "No target."); return false; }
            if (!control(p)) return false;
            int ticks = BalanceLaw.controlTicks(t, 60);
            if (freeze) TimeStop.freeze(t, ticks);
            else t.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
            b.hurt(i, p, t, mode, dmg);
            rider.apply(t, p);
            b.castCircle(p, 0.7f);
            b.vfx(p, shape, p.getEyePosition(), t.getBoundingBox().getCenter(), ticks, 1f);
            return true;
        };
    }

    /** Self power-up: your melee hits deal +bonus for the duration (Reinforcement, Beast Form, Bone Armor). */
    public static BookPage.Cast empower(int ticks, float bonus, boolean thorns, VfxShape shape, Supplier<MobEffectInstance>... effects) {
        return (b, i, p, mode) -> {
            var tag = i.getOrCreateTag();
            tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
            tag.putFloat("EmpowerBonus", bonus);
            if (thorns) tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
            for (Supplier<MobEffectInstance> e : effects) p.addEffect(e.get());
            b.castCircle(p, 1f);
            VfxSpawn.sendFollowing(p.serverLevel(), shape, p, p.position().add(0, 1, 0), b.color, ticks, 1f);
            return true;
        };
    }

    /** Magic constructs (swords, axes, shields) orbit you and cut anything that comes close. */
    public static BookPage.Cast constructs(float dmg, double radius, int ticks) {
        return (b, i, p, mode) -> {
            b.castCircle(p, 1.2f);
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WEAPON_CONSTRUCTS, p, p.position().add(0, 1, 0), b.color, ticks, (float) radius / 3f);
            SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
                for (LivingEntity t : around(p, p.position(), radius)) b.hurt(i, p, t, mode, dmg);
                for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(radius))) if (pr.getOwner() != p) pr.discard();
            });
            return true;
        };
    }

    /** A temporary wall in front of you. */
    public static BookPage.Cast wall(net.minecraft.world.level.block.state.BlockState state) {
        return (b, i, p, mode) -> {
            Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
            Vec3 base = p.position().add(dir.scale(2.5));
            int placed = 0;
            for (int w = -2; w < 2; w++) for (int h = 0; h < 3; h++)
                if (SpellRuntime.tempBlock(p.serverLevel(), BlockPos.containing(base.add(side.scale(w + 0.5)).add(0, h, 0)), state, 160)) placed++;
            if (placed == 0) { fail(p, "No room."); return false; }
            b.castCircle(p, 0.8f);
            b.vfx(p, VfxShape.EARTH_SPIKES, base.add(side.scale(-2)), base.add(side.scale(2)), 16, 0.8f);
            return true;
        };
    }

    /** Mobs hunting you lose your trail; you vanish briefly (decoy). */
    public static BookPage.Cast decoy() {
        return (b, i, p, mode) -> {
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) if (m.getTarget() == p) m.setTarget(null);
            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 60, 0));
            b.castCircle(p, 0.8f);
            b.vfx(p, VfxShape.MIRROR_PANE, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)).add(0, 1, 0), 30, 1f);
            return true;
        };
    }

    /** Heal nearby allies (healing decay applies). */
    public static BookPage.Cast healAllies(double radius, float fraction) {
        return (b, i, p, mode) -> {
            for (ServerPlayer ally : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(radius))) {
                if (ally == p || ally.isAlliedTo(p)) BalanceLaw.heal(ally, ally.getMaxHealth() * fraction);
            }
            b.castCircle(p, 1f);
            b.vfx(p, VfxShape.ELF_CIRCLE, p.position(), p.position().add(0, 1, 0), 30, (float) radius / 3f);
            return true;
        };
    }

    /** Copy Magic: cast the last (non-forbidden) page that hit you in the last 20 s. */
    public static BookPage.Cast copy() {
        return (b, i, p, mode) -> {
            var d = p.getPersistentData();
            if (p.level().getGameTime() - d.getLong("nusmp_copy_at") > 400 || !d.contains("nusmp_copy_book")) { fail(p, "No spell to copy (get hit by one first)."); return false; }
            var rl = net.minecraft.resources.ResourceLocation.tryParse(d.getString("nusmp_copy_book"));
            var skill = rl == null ? null : io.github.manasmods.manascore.skill.api.SkillAPI.getSkillRegistry().get(rl);
            if (!(skill instanceof GrimoireBook other) || other instanceof ForbiddenBook) { fail(p, "That magic cannot be copied."); return false; }
            BookPage page = other.page(d.getInt("nusmp_copy_mode"));
            if (page == null) return false;
            b.vfx(p, VfxShape.MIRROR_PANE, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f)), 20, 1f);
            return page.cast().cast(other, i, p, d.getInt("nusmp_copy_mode"));
        };
    }

    // ------------------------------------------------------------------ Anti-Magic awakening
    // 0.48: mastering the Anti-Magic grimoire awakens the Anti-Magic Lord inside the grimoire itself (book.AntiMagicBook), no longer a
    // separate skill.

    // ------------------------------------------------------------------ passives
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("EmpowerUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("EmpowerBonus"), masteryFrac(i)));
        }
        return true;
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("ThornsUntil") && source.getEntity() instanceof LivingEntity att
                && att != owner && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
            att.hurt(owner.damageSources().thorns(owner), 2f);
        }
        return true;
    }

    public static net.minecraft.world.level.block.state.BlockState block(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }
    public static final net.minecraft.world.level.block.state.BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
}
