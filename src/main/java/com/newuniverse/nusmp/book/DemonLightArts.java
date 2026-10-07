package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Demon Light Magic: black light with a magenta bloom. It blinds, burns what comes from the underworld (undead and demons
 * take more and catch fire), strikes the spirit, and cuts through barriers. Spells that need more than a shared shape live here.
 */
final class DemonLightArts {
    private DemonLightArts() {}

    private static final VfxShape FX1 = VfxShape.DEMON_LIGHT_FX1, FX2 = VfxShape.DEMON_LIGHT_FX2, FX3 = VfxShape.DEMON_LIGHT_FX3;

    /** Undead and demons (and devil-tainted mages) feel demon light as holy fire. */
    static boolean holy(LivingEntity t) {
        if (t.getType().is(EntityTypeTags.UNDEAD) || GrimoireBook.inDevilState(t)) return true;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(t.getType());
        return id != null && id.getPath().contains("demon");
    }

    /** A hit of demon light: x1.6 on undead and demons, which also burn for a few seconds. */
    static void strike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw) {
        boolean holy = holy(t);
        b.hurt(i, p, t, mode, holy ? raw * 1.6f : raw);
        if (holy) t.igniteForSeconds(4);
    }

    /** Spiritual damage on top of a hit (bypasses armour; bosses are spared). */
    static void spirit(LivingEntity t, double amount) {
        if (!BalanceLaw.isBoss(t)) EnergyBridge.spirit(t, amount);
    }

    /** Blindness; mobs forget who they were hunting (not bosses). */
    static void blind(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
        if (t instanceof Mob m && !BalanceLaw.isBoss(t)) m.setTarget(null);
    }

    /** A rider for the shared shapes: blindness plus glowing, so the blinded can be seen in the dark. */
    static ElementBook.Rider blinding(int ticks) {
        return (t, p) -> { blind(t, ticks); t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0)); };
    }

    /** A rider: a little spiritual damage. */
    static ElementBook.Rider spiritual(double amount) {
        return (t, p) -> spirit(t, amount);
    }

    private static void pull(LivingEntity t, Vec3 to, double force) {
        if (BalanceLaw.isBoss(t)) return;
        Vec3 d = to.subtract(t.position());
        if (d.lengthSqr() < 0.01) return;
        t.setDeltaMovement(d.normalize().scale(force).add(0, 0.2, 0));
        t.hurtMarked = true;
    }

    // ------------------------------------------------------------------ starters
    /** Light Sword of Judgment: shards of black light, four lines through each, thrown at a target. */
    static boolean swordOfJudgment(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sz = GrimoireBook.size(i, p);
        int shards = Math.min(8, 5 + EnergyBridge.modifier(p) / 2);
        float dmg = 3f * (float) EnergyBridge.scale(p);
        LivingEntity home = GrimoireBook.target(p, 24);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        b.castCircle(p, 0.9f);
        for (int k = 0; k < shards; k++) {
            int n = k;
            SpellRuntime.later(p.serverLevel(), n * 2, () -> {
                if (!p.isAlive()) return;
                double off = (n - (shards - 1) / 2.0) * 0.5;
                Vec3 from = p.getEyePosition().add(side.scale(off)).add(0, 0.5 + 0.15 * (n % 3), 0).add(dir.scale(0.6));
                Vec3 aim = home != null && home.isAlive() ? home.getBoundingBox().getCenter() : start.add(dir.scale(24));
                Vec3 d = aim.subtract(from).normalize();
                b.vfx(p, FX1, from, from.add(d.scale(26)), 16, 0.8f * sz);
                SpellRuntime.bolt(p, from, d.scale(1.7), 0.6 * sz, 16, false, home, (bolt, t) -> {
                    strike(b, i, p, t, mode, dmg);
                    spirit(t, 1.5);
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                }, (bolt, at) -> b.vfx(p, FX3, at, at, 14, 0.6f * sz));
            });
        }
        return true;
    }

    /** Bright Judgment Whip: a lash of light across 12 blocks that bites and drags the target in. */
    static boolean judgmentWhip(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sc = (float) EnergyBridge.scale(p);
        Vec3 a = p.getEyePosition().add(0, -0.3, 0), dir = p.getViewVector(1f), end = a.add(dir.scale(12 * GrimoireBook.size(i, p)));
        List<LivingEntity> hit = GrimoireBook.along(p, a, end, 0.9);
        if (hit.isEmpty()) { GrimoireBook.fail(p, "Nothing in reach of the whip."); return false; }
        b.castCircle(p, 0.7f);
        b.vfx(p, FX1, a, end, 18, 1.1f);
        for (int k = 0; k < 2; k++) {
            SpellRuntime.later(p.serverLevel(), k * 7, () -> {
                if (!p.isAlive()) return;
                for (LivingEntity t : hit) {
                    if (!t.isAlive()) continue;
                    strike(b, i, p, t, mode, 4f * sc);
                    spirit(t, 2.0);
                    pull(t, p.position(), 0.9);
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    b.vfx(p, FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 12, 0.5f);
                }
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ mid
    /** Prism Breaker: a beam that goes through barriers; magic is jammed, auras burn, the barrier skills switch off. */
    static boolean prismBreaker(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sc = (float) EnergyBridge.scale(p);
        Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(22 * GrimoireBook.size(i, p)));
        b.castCircle(p, 1f);
        b.vfx(p, FX1, a, end, 20, 1.3f);
        int broken = 0;
        for (int k = 1; k <= 5; k++) broken += Nullification.shatterBarriers(p.serverLevel(), a.add(end.subtract(a).scale(k / 5.0)), 3.5, p);
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.1)) {
            strike(b, i, p, t, mode, 6f * sc);
            spirit(t, 3.0);
            EnergyBridge.burnAura(t, 0.1);
            EnergyBridge.effect(t, "silence", BalanceLaw.isBoss(t) ? 20 : 80, 0);
            b.vfx(p, FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 14, 0.7f);
        }
        if (broken > 0) b.vfx(p, FX3, end, end, 18, 1.1f);
        return true;
    }

    /** Purging Light: holy fire around you. The underworld burns and loses its blessings; your allies are cleansed. */
    static boolean purgingLight(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sc = (float) EnergyBridge.scale(p);
        double r = 7 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.1f);
        b.vfx(p, FX2, p.position(), p.position(), 30, (float) r);
        b.vfx(p, FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 24, 1.2f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (holy(t)) {
                for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects()))
                    if (e.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) t.removeEffect(e.getEffect());
                strike(b, i, p, t, mode, 6f * sc);
                spirit(t, 3.0);
            } else {
                b.hurt(i, p, t, mode, 2f * sc);
                blind(t, 40);
            }
        }
        for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r), x -> x == p || (x.isAlive() && x.isAlliedTo(p)))) {
            a.removeEffect(MobEffects.POISON);
            a.removeEffect(MobEffects.WITHER);
            a.removeEffect(MobEffects.WEAKNESS);
            a.removeEffect(MobEffects.BLINDNESS);
        }
        return true;
    }

    /** Light Prison: bars of black light close round one target; it cannot walk, jump or see. Hard control. */
    static boolean lightPrison(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No one to imprison."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        t.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128));
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks + 20, 0));
        if (t instanceof Mob m && !BalanceLaw.isBoss(t)) m.setTarget(null);
        b.castCircle(p, 0.8f);
        b.vfx(p, FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1f);
        b.vfx(p, FX2, t.position(), t.position(), ticks, 1.8f);
        strike(b, i, p, t, mode, 3f * (float) EnergyBridge.scale(p));
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (t.isAlive()) spirit(t, 1.0);
        });
        return true;
    }

    /** Light Step: you become a line of black light, hitting and blinding everything on the path, then run fast for a while. */
    static BookPage.Cast lightStep() {
        BookPage.Cast dash = ElementBook.dash(5, 14, false, FX1, blinding(60));
        return (b, i, p, mode) -> {
            if (!dash.cast(b, i, p, mode)) return false;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
            b.vfx(p, FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 20, 1f);
            return true;
        };
    }

    /** Veil of Black Light: a mirror-bright film of dark light. Shots that enter it are thrown back at the shooter. */
    static boolean veil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 160;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), FX3, p, p.position().add(0, 1, 0), b.color, ticks, 1.2f);
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> {
            if (!p.isAlive()) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.5), x -> x.getOwner() != p)) {
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.2));
                pr.setOwner(p);
                pr.hurtMarked = true;
                b.vfx(p, FX3, pr.position(), pr.position(), 10, 0.4f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ zone
    /** Eclipse Domain: a dark sun hangs over the point you look at. Foes inside are blinded, slowed and burned; allies are mended. */
    static boolean eclipseDomain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 6 * GrimoireBook.size(i, p);
        int ticks = 120;
        float sc = (float) EnergyBridge.scale(p);
        b.castCircle(p, 1f);
        b.vfx(p, FX2, c, c, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                strike(b, i, p, t, mode, 2.5f * sc);
                spirit(t, 1.0);
            }
            for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                    x -> x.isAlive() && (x == p || x.isAlliedTo(p)) && x.distanceToSqr(c) <= r * r)) {
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ signature
    /** Light Shaft of Divine Punishment: the light gathers between your hands, then falls as a column too wide and fast to avoid. */
    static boolean divineShaft(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 32);
        double r = 4.5 * GrimoireBook.size(i, p);
        float sc = (float) EnergyBridge.scale(p);
        int charge = 24;
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MANA_CHARGE, p, p.position().add(0, 1, 0), b.color, charge, 1.2f);
        b.vfx(p, FX2, c, c, charge + 10, (float) r);
        SpellRuntime.later(p.serverLevel(), charge, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, FX1, c.add(0, 30, 0), c, 14, 2f);
            b.vfx(p, FX3, c, c, 30, 1.6f);
            Nullification.shatterBarriers(p.serverLevel(), c, r + 2, p);
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                strike(b, i, p, t, mode, 14f * sc);
                spirit(t, 6.0);
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.isBoss(t) ? 40 : 120, 0));
                if (!BalanceLaw.isBoss(t)) t.igniteForSeconds(5);
            }
            SpellRuntime.later(p.serverLevel(), 12, () -> {
                if (!p.isAlive()) return;
                b.vfx(p, FX3, c, c, 24, 1.1f);
                for (LivingEntity t : GrimoireBook.around(p, c, r * 1.4)) strike(b, i, p, t, mode, 5f * sc);
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ daily
    /** Dawn of the Demon Light: everything hostile is blinded and weakened; allies are healed and cleansed. */
    static boolean demonDawn(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sc = (float) EnergyBridge.scale(p);
        double r = 12 * GrimoireBook.size(i, p);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            blind(t, BalanceLaw.isBoss(t) ? 60 : 200);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            if (holy(t)) { strike(b, i, p, t, mode, 8f * sc); spirit(t, 4.0); }
        }
        List<ServerPlayer> who = new ArrayList<>(p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r),
                a -> a != p && a.isAlive() && a.isAlliedTo(p)));
        who.add(p);
        for (ServerPlayer a : who) {
            for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects()))
                if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) a.removeEffect(e.getEffect());
            BalanceLaw.heal(a, a.getMaxHealth() * (a == p ? 0.3f : 0.2f) * Math.min(1.2f, sc));
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        }
        b.castCircle(p, 1.5f);
        b.vfx(p, FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 40, 1.8f);
        b.vfx(p, FX2, p.position(), p.position(), 60, (float) r);
        return true;
    }
}
