package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Black Oil Magic: the spells of BlackOilBook that need their own logic (oiled targets, tar traps, the ignition, the curse). */
public final class BlackOilArts {
    private BlackOilArts() {}

    private static final int COLOR = BlackOilBook.COLOR;
    /** End game-time of the oil on an entity; the coat of the caster; the nails' curse. */
    private static final String OILED = "nusmp_boil_oiled", COAT = "nusmp_boil_coat";
    private static boolean hooked;

    /** Registers the damage hooks (oil feeds fire, the viscous coat). Called once from BlackOilProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            var src = e.getSource();
            long now = victim.level().getGameTime();
            var data = victim.getPersistentData();
            if (data.getLong(OILED) > now && (src.is(DamageTypeTags.IS_FIRE) || src.is(TensuraDamageTypes.FIRE_ELEMENTAL))) {
                e.setAmount(e.getAmount() * 1.5f);
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 100));
            }
            if (data.getLong(COAT) <= now || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            e.setAmount(e.getAmount() * (src.is(DamageTypeTags.IS_PROJECTILE) ? 0.5f : 0.6f));
            if (src.getDirectEntity() instanceof LivingEntity att && att != victim) { gum(att, 60, 2); oil(att, 120); }
        });
    }

    // ------------------------------------------------------------------ small helpers
    /** Soaks the target in oil: fire hurts it half again as much while it lasts. */
    static void oil(LivingEntity t, int ticks) {
        t.getPersistentData().putLong(OILED, t.level().getGameTime() + ticks);
    }

    static boolean oiled(LivingEntity t) { return t.getPersistentData().getLong(OILED) > t.level().getGameTime(); }

    static void wash(LivingEntity t) { t.getPersistentData().putLong(OILED, 0L); }

    /** Sticky oil on a target: Slowness for 'ticks', its sprint stopped and its momentum dragged down. */
    static void gum(LivingEntity t, int ticks, int amp) {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? Math.min(ticks, 40) : ticks, boss ? Math.min(amp, 1) : amp));
        t.setSprinting(false);
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.35, v.y, v.z * 0.35);
        t.hurtMarked = true;
    }

    /** Rider for a bolt: the target is gummed up and soaked. */
    static ElementBook.Rider slick(int ticks, int amp) { return (t, p) -> { gum(t, ticks, amp); oil(t, ticks * 4); }; }

    private static float k(ManasSkillInstance i, ServerPlayer p) { return GrimoireBook.size(i, p) * EnergyBridge.scale(p); }

    private static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    private static void flames(ServerPlayer p, Vec3 c, int n, double spread) {
        p.serverLevel().sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, n, spread, 0.4, spread, 0.06);
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, n / 2, spread, 0.5, spread, 0.04);
    }

    // ------------------------------------------------------------------ Sticky Tar: a glob that leaves a trapping pool
    static boolean stickyTar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.BLACK_OIL_FX1, start, start.add(dir.scale(20)), 14, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.3), 0.5, 16, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 5f);
            gum(t, 100, 2);
            oil(t, 200);
        }, (bolt, at) -> {
            b.vfx(p, VfxShape.BLACK_OIL_FX3, at, at, 20, 0.9f);
            tarPool(b, p, at, 2.2 * s, 120, 2);
        });
        return true;
    }

    /** A pool of tar: everything inside is gummed, soaked and pressed to the ground. */
    static void tarPool(GrimoireBook b, ServerPlayer p, Vec3 c, double r, int ticks, int amp) {
        b.vfx(p, VfxShape.BLACK_OIL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                gum(t, 30, amp);
                oil(t, 100);
                Vec3 v = t.getDeltaMovement();
                if (v.y > 0) { t.setDeltaMovement(v.x, v.y * 0.3, v.z); t.hurtMarked = true; }
            }
        });
    }

    // ------------------------------------------------------------------ Curse-Filled Nails (wiki)
    static boolean cursedNails(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 5);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "The nails need someone within reach."); return false; }
        boolean boss = BalanceLaw.isBoss(t);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.BLACK_OIL_FX1, p.getEyePosition().add(0, -0.4, 0), t.getBoundingBox().getCenter(), 8, 0.8f);
        b.hurtAs(i, p, t, mode, 7f, TensuraDamageTypes.CURSE);
        gum(t, 80, 1);
        oil(t, 160);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        // the nails read the body they touch: health, magicule, warmth
        String heat = t.isOnFire() ? "scorching" : t.getTicksFrozen() > 0 ? "chilled" : "warm";
        String mana = "none";
        var ex = TensuraStorages.getExistenceFrom(t);
        double max = EnergyHelper.getMaxMagicule(t);
        if (ex != null && max > 0) mana = Math.round(100.0 * ex.getMagicule() / max) + "%";
        p.displayClientMessage(Component.literal("The nails read " + t.getDisplayName().getString() + ": health " + Math.round(t.getHealth()) + "/"
                + Math.round(t.getMaxHealth()) + ", magicule " + mana + ", body " + heat + ".").withStyle(ChatFormatting.DARK_GREEN), true);
        b.vfx(p, VfxShape.BLACK_OIL_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 18, 0.7f);
        // the curse left in the wound
        SpellRuntime.zone(p.serverLevel(), 100, 20, age -> {
            if (!t.isAlive() || age == 0) return;
            b.hurtAs(i, p, t, mode, boss ? 1f : 2f, TensuraDamageTypes.CURSE);
            Nullification.bleed(t, 0.01);
        });
        return true;
    }

    // ------------------------------------------------------------------ Oil Slick: a spray that soaks everything in front
    static boolean oilSlick(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 8 * s;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.65) continue;
            b.hurt(i, p, t, mode, 4f);
            gum(t, 120, 1);
            oil(t, 240);
        }
        Vec3 mid = p.position().add(look.multiply(1, 0, 1).scale(r * 0.5));
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.BLACK_OIL_FX1, eye, eye.add(look.scale(r)), 16, 1.3f);
        b.vfx(p, VfxShape.BLACK_OIL_FX2, mid, mid.add(0, 1, 0), 60, (float) (r * 0.45));
        return true;
    }

    // ------------------------------------------------------------------ Ignite Oil: every soaked enemy goes up
    static boolean igniteOil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 16 * k(i, p);
        List<LivingEntity> soaked = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) if (oiled(t) && soaked.size() < 12) soaked.add(t);
        if (soaked.isEmpty()) { GrimoireBook.fail(p, "Nothing is soaked in oil."); return false; }
        b.castCircle(p, 0.9f);
        Set<LivingEntity> blasted = new HashSet<>();
        for (LivingEntity t : soaked) {
            Vec3 c = t.getBoundingBox().getCenter();
            b.vfx(p, VfxShape.BLACK_OIL_FX1, p.getEyePosition(), c, 6, 0.6f);
            b.vfx(p, VfxShape.BLACK_OIL_FX3, c, c.add(0, 1, 0), 24, 1.0f);
            flames(p, c, 24, 0.7);
            for (LivingEntity n : GrimoireBook.around(p, c, 3)) {
                if (!blasted.add(n)) continue;
                b.hurtAs(i, p, n, mode, n == t ? 12f : 6f, TensuraDamageTypes.FIRE_ELEMENTAL);
                n.igniteForSeconds(n == t ? 6 : 3);
                Vec3 out = n.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : out.normalize();
                double kb = BalanceLaw.isBoss(n) ? 0.1 : 0.6;
                n.setDeltaMovement(n.getDeltaMovement().add(out.scale(kb)).add(0, kb * 0.6, 0));
                n.hurtMarked = true;
            }
        }
        for (LivingEntity t : soaked) wash(t);
        return true;
    }

    // ------------------------------------------------------------------ Oil Slide: skate forward on oil, leaving a slick behind
    static boolean oilSlide(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        p.setDeltaMovement(look.x * 1.9, Math.max(p.getDeltaMovement().y, 0.12), look.z * 1.9);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        Vec3 from = p.position();
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.BLACK_OIL_FX1, from.add(0, 0.3, 0), from.add(look.scale(10)).add(0, 0.3, 0), 14, 1.0f);
        List<Vec3> trail = new ArrayList<>();
        SpellRuntime.zone(p.serverLevel(), 100, 2, age -> {
            if (p.isRemoved()) return;
            if (age <= 12 && trail.size() < 7) {
                trail.add(p.position());
                if (age % 4 == 0) b.vfx(p, VfxShape.BLACK_OIL_FX2, p.position(), p.position().add(0, 1, 0), 90 - age, 1.6f);
            }
            if (age % 4 == 0) for (Vec3 pt : trail) for (LivingEntity t : GrimoireBook.around(p, pt, 1.8)) { gum(t, 40, 2); oil(t, 120); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Oil-Soaked Arms: melee hits that gum and soak
    static boolean soakedArms(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        i.getOrCreateTag().putLong("OilArmsUntil", p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BLACK_OIL_FX3, p, p.position().add(0, 1, 0), COLOR, 30, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Viscous Coat: a thick skin that stops blows and catches projectiles
    static boolean viscousCoat(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 200;
        p.getPersistentData().putLong(COAT, p.level().getGameTime() + ticks);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BLACK_OIL_FX2, p, p.position(), COLOR, ticks, 2.2f);
        b.vfx(p, VfxShape.BLACK_OIL_FX3, p.position().add(0, 1, 0), p.position().add(0, 1.5, 0), 24, 1.0f);
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> {
            if (!p.isAlive() || p.level().getGameTime() >= p.getPersistentData().getLong(COAT)) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(0.35));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Tar Pit: a zone that swallows what walks in
    static boolean tarPit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 5 * s;
        int ticks = 160;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.BLACK_OIL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (age % 10 == 0) b.hurt(i, p, t, mode, 2f);
                gum(t, 30, 3);
                oil(t, 100);
                Vec3 v = t.getDeltaMovement();
                Vec3 pull = c.subtract(t.position()).multiply(1, 0, 1);
                double f = BalanceLaw.isBoss(t) ? 0.02 : 0.05;
                Vec3 in = pull.lengthSqr() > 0.5 ? pull.normalize().scale(f) : Vec3.ZERO;
                t.setDeltaMovement(v.x * 0.3 + in.x, Math.min(v.y, 0) - 0.04, v.z * 0.3 + in.z);
                t.hurtMarked = true;
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Curse Candle Ritual Disk (wiki)
    static boolean candleDisk(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = p.position();
        double r = 6 * s;
        int ticks = 240;
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), ticks, (float) r / 1.6f);
        b.vfx(p, VfxShape.BLACK_OIL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        // six black candles light around the disk
        var lit = Blocks.BLACK_CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true);
        for (int n = 0; n < 6; n++) {
            double a = n * Math.PI / 3;
            BlockPos at = BlockPos.containing(c.x + Math.cos(a) * r * 0.7, c.y, c.z + Math.sin(a) * r * 0.7);
            if (p.level().isLoaded(at) && p.level().getBlockState(at.below()).isFaceSturdy(p.level(), at.below(), Direction.UP))
                SpellRuntime.tempBlock(p.serverLevel(), at, lit, ticks);
        }
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (p.isRemoved()) return;
            int cursed = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurtAs(i, p, t, mode, 2.5f, TensuraDamageTypes.CURSE);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                gum(t, 40, 0);
                EnergyBridge.effect(t, "fragility", 40, 0);
                Nullification.bleed(t, 0.01);
                if (cursed < 3) cursed++;
            }
            // the ritual feeds the one who drew it
            if (cursed > 0 && p.distanceToSqr(c) <= r * r) BalanceLaw.heal(p, cursed * 1.0f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Black Oil Eruption (signature): the oil geyser, then the fire
    static boolean eruption(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 6 * s;
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.BLACK_OIL_FX2, c, c.add(0, 1, 0), 70, (float) r);
        b.vfx(p, VfxShape.BLACK_OIL_FX3, c, c.add(0, 3, 0), 32, 1.5f);
        List<LivingEntity> hit = GrimoireBook.around(p, c, r);
        for (LivingEntity t : hit) {
            b.hurt(i, p, t, mode, 12f);
            gum(t, 100, 3);
            oil(t, 300);
            double up = BalanceLaw.isBoss(t) ? 0.25 : 1.0;
            t.setDeltaMovement(t.getDeltaMovement().add(0, up, 0));
            t.hurtMarked = true;
        }
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1, c.z, 40, r * 0.2, 1.5, r * 0.2, 0.1);
        // two seconds later the geyser catches
        SpellRuntime.later(p.serverLevel(), 40, () -> {
            if (p.isRemoved()) return;
            b.vfx(p, VfxShape.BLACK_OIL_FX3, c, c.add(0, 3, 0), 36, 1.8f);
            b.vfx(p, VfxShape.BLACK_OIL_FX2, c, c.add(0, 1, 0), 60, (float) (r + 1));
            flames(p, c, 70, r * 0.3);
            for (LivingEntity t : GrimoireBook.around(p, c, r + 1)) {
                b.hurtAs(i, p, t, mode, 18f, TensuraDamageTypes.FIRE_ELEMENTAL);
                t.igniteForSeconds(8);
                wash(t);
                Vec3 out = t.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : out.normalize();
                double kb = BalanceLaw.isBoss(t) ? 0.1 : 0.8;
                t.setDeltaMovement(t.getDeltaMovement().add(out.scale(kb)).add(0, kb * 0.5, 0));
                t.hurtMarked = true;
            }
            SpellRuntime.zone(p.serverLevel(), 60, 10, age -> {
                for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                    b.hurtAs(i, p, t, mode, 3f, TensuraDamageTypes.FIRE_ELEMENTAL);
                    t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
                }
                flames(p, c, 10, r * 0.4);
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Black Oil Sea (daily): a lake of oil around you
    static boolean oilSea(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 c = p.position();
        double r = 11 * s;
        int ticks = 300;
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.BLACK_OIL_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        b.vfx(p, VfxShape.BLACK_OIL_FX3, c.add(0, 0.5, 0), c.add(0, 2, 0), 30, 1.5f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
                gum(t, 30, 2);
                oil(t, 160);
            }
            // the caster and friends skate over it
            for (LivingEntity f : friends(p, c, r)) {
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1));
                f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0));
            }
        });
        return true;
    }
}
