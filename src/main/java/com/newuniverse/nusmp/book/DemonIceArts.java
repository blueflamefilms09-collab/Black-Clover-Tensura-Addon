package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Demon Ice Magic: the casts of the grimoire that are not a shared shape. The whole magic runs on one loop: chill a foe (Slowness plus
 * frozen ticks), and what is fully frozen can be shattered for far more. Spiritual damage rides on the heavy pages (it ignores armour).
 */
final class DemonIceArts {
    private DemonIceArts() {}

    // ---------------------------------------------------------------- shared
    /** Creative and spectator players are never touched. */
    static boolean valid(LivingEntity t) { return !(t instanceof Player pl && (pl.isCreative() || pl.isSpectator())); }

    /** Fully frozen by this magic and free to take damage (a time-stopped foe takes none until it thaws). */
    static boolean shatterable(LivingEntity t) { return t.isFullyFrozen() && !TimeStop.isFrozen(t); }

    /** Slowness plus frozen ticks (they drain 2 per tick, so 'frozenTicks' keeps a foe fully frozen for about half of that). Bosses get a third of it. */
    static void chill(LivingEntity t, int slowTicks, int slowAmp, int frozenTicks) {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? Math.min(slowTicks, 40) : slowTicks, boss ? Math.min(slowAmp, 1) : slowAmp));
        t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze()) + (boss ? frozenTicks / 3 : frozenTicks));
    }

    static ElementBook.Rider chilled(int slowTicks, int slowAmp, int frozenTicks) { return (t, p) -> { if (valid(t)) chill(t, slowTicks, slowAmp, frozenTicks); }; }

    private static void send(ServerPlayer p, GrimoireBook b, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, b.color, ticks, power);
    }

    // ---------------------------------------------------------------- starters
    /** Piercing Lance: an instant lance of black ice through everything in a line; it ignores barriers and chills each foe to the bone. */
    static boolean lance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(26 * s));
        HitResult wall = p.level().clip(new ClipContext(a, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 0.8)) {
            if (!valid(t)) continue;
            if (n++ >= 12) break;
            b.hurt(i, p, t, mode, 9f);
            chill(t, 80, 2, 80);
            EnergyBridge.effect(t, "frost", 80, 0);
            EnergyBridge.spirit(t, 2);
        }
        Nullification.shatterBarriers(p.serverLevel(), end, 2.5, p);
        b.castCircle(p, 0.7f);
        send(p, b, VfxShape.DEMON_ICE_FX1, a, end, 12, 1f);
        send(p, b, VfxShape.DEMON_ICE_FX3, end, end, 24, 1f);
        return true;
    }

    // ---------------------------------------------------------------- mid
    /** Frostbite Wave: a wave of killing cold in a cone; whatever it catches is shoved back, slowed and frozen. */
    static boolean wave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 9 * GrimoireBook.size(i, p);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (!valid(t) || t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.35) continue;
            if (n++ >= 16) break;
            b.hurt(i, p, t, mode, 8f);
            chill(t, 100, 2, 100);
            Vec3 d = t.position().subtract(p.position()).normalize();
            t.knockback(0.9, -d.x, -d.z);
        }
        b.castCircle(p, 0.7f);
        send(p, b, VfxShape.DEMON_ICE_FX1, eye, eye.add(look.scale(r)), 14, 1.2f);
        send(p, b, VfxShape.DEMON_ICE_FX3, eye.add(look.scale(r * 0.5)), eye.add(look.scale(r)), 26, (float) (r / 6));
        return true;
    }

    /** Frozen Soul: the cold reaches the spirit; one foe is stopped in a block of ice and hurt in the soul, then it cracks. */
    static boolean frozenSoul(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "No one to freeze."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 70);
        b.hurt(i, p, t, mode, 6f);
        EnergyBridge.spirit(t, 12);
        EnergyBridge.effect(t, "frost", ticks + 60, 0);
        chill(t, ticks + 40, 3, 200);
        TimeStop.freeze(t, ticks);
        Vec3 c = t.getBoundingBox().getCenter();
        b.castCircle(p, 0.9f);
        send(p, b, VfxShape.DEMON_ICE_FX1, p.getEyePosition(), c, 12, 1f);
        send(p, b, VfxShape.DEMON_ICE_FX2, t.position(), t.position(), ticks, Math.max(1.2f, t.getBbWidth() * 1.2f));
        SpellRuntime.later(p.serverLevel(), ticks + 2, () -> {
            if (!t.isAlive()) return;
            b.hurt(i, p, t, mode, 8f);
            t.setTicksFrozen(0);
            send(p, b, VfxShape.DEMON_ICE_FX3, t.getBoundingBox().getCenter(), t.position(), 24, 1f);
        });
        return true;
    }

    /** Shatter: every fully frozen foe around you bursts for heavy damage and is left fragile. Needs frozen foes. */
    static boolean shatter(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 12 * GrimoireBook.size(i, p))) {
            if (hit.size() >= 10) break;
            if (valid(t) && shatterable(t)) hit.add(t);
        }
        if (hit.isEmpty()) { GrimoireBook.fail(p, "No frozen foes in reach."); return false; }
        b.castCircle(p, 1f);
        int fx = 0;
        for (LivingEntity t : hit) {
            b.hurt(i, p, t, mode, 16f);
            t.setTicksFrozen(0);
            EnergyBridge.effect(t, "fragility", 80, 0);
            if (fx++ < 6) send(p, b, VfxShape.DEMON_ICE_FX3, t.getBoundingBox().getCenter(), p.position(), 24, 1f);
        }
        return true;
    }

    /** Glacier Wall: a six-wide wall of blue ice rises ahead; the cold that bursts out of it slows and shoves everyone near its foot. */
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(3));
        int placed = 0;
        for (int w = -3; w < 3; w++) for (int h = 0; h < 4; h++) {
            BlockPos pos = BlockPos.containing(base.add(side.scale(w + 0.5)).add(0, h, 0));
            if (sl.isLoaded(pos) && SpellRuntime.tempBlock(sl, pos, Blocks.BLUE_ICE.defaultBlockState(), 200)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for a wall."); return false; }
        for (LivingEntity t : GrimoireBook.around(p, base, 3.5)) {
            if (!valid(t)) continue;
            chill(t, 80, 2, 60);
            Vec3 d = t.position().subtract(base).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.01) { d = d.normalize(); t.knockback(0.6, -d.x, -d.z); }
        }
        b.castCircle(p, 0.8f);
        send(p, b, VfxShape.DEMON_ICE_FX2, base, base, 60, 3.5f);
        send(p, b, VfxShape.DEMON_ICE_FX3, base.add(0, 1, 0), base.add(dir), 26, 1f);
        return true;
    }

    /** Glacier Slide: you skate forward on a sheet of ice, cutting through foes; the frost left behind keeps chilling anyone who follows. */
    static boolean slide(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        if (!ElementBook.dash(6f, 10, false, VfxShape.DEMON_ICE_FX1, chilled(60, 1, 40)).cast(b, i, p, mode)) return false;
        Vec3 to = p.position();
        send(p, b, VfxShape.DEMON_ICE_FX2, from, from, 40, 1.8f);
        send(p, b, VfxShape.DEMON_ICE_FX3, to, to.add(0, 1, 0), 24, 1f);
        SpellRuntime.zone(p.serverLevel(), 40, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, from, 2.5)) if (valid(t)) chill(t, 30, 1, 10);
        });
        return true;
    }

    /** Frost Body: for 20 s your body is ice (resistance and an absorbing shell); your blows chill and anyone who hits you is chilled. */
    static boolean body(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 400;
        var tag = i.getOrCreateTag();
        tag.putLong("IceBodyUntil", p.level().getGameTime() + ticks);
        tag.putFloat("IceBodyBonus", 4f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEMON_ICE_FX2, p, p.position().add(0, 1, 0), b.color, ticks, 1.4f);
        send(p, b, VfxShape.DEMON_ICE_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 26, 1f);
        return true;
    }

    // ---------------------------------------------------------------- zone
    /** Blizzard: a storm of snow and ice shards at the aim point for 7 s; everything inside is cut, slowed and kept frozen. */
    static boolean blizzard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float r = 7f * GrimoireBook.size(i, p);
        int ticks = 140;
        b.castCircle(p, 1f);
        send(p, b, VfxShape.DEMON_ICE_FX2, c, c, ticks, r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t)) continue;
                if (n++ >= 24) break;
                b.hurt(i, p, t, mode, 2.5f);
                chill(t, 50, 3, 30);
                t.clearFire();
            }
            if (age % 40 == 0) send(p, b, VfxShape.DEMON_ICE_FX3, c, c.add(0, 1, 0), 20, r / 6f);
        });
        return true;
    }

    // ---------------------------------------------------------------- signature
    /** Absolute Zero: the air around you stops. Everything in 9 blocks is hurt, frozen solid and hurt in the soul, barriers break, and a moment later the ice bursts. */
    static boolean absoluteZero(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 9 * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.5f);
        send(p, b, VfxShape.DEMON_ICE_FX2, c, c, 70, (float) r);
        send(p, b, VfxShape.DEMON_ICE_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 32, 1.5f);
        Nullification.shatterBarriers(p.serverLevel(), c, r, p);
        boolean hold = BalanceLaw.beginControl(p);
        List<LivingEntity> caught = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (!valid(t)) continue;
            if (caught.size() >= 16) break;
            b.hurt(i, p, t, mode, 14f);
            chill(t, 160, 4, 200);
            EnergyBridge.effect(t, "frost", 120, 0);
            EnergyBridge.spirit(t, 6);
            if (hold) TimeStop.freeze(t, BalanceLaw.controlTicks(t, 50));
            caught.add(t);
        }
        SpellRuntime.later(p.serverLevel(), 58, () -> {
            int fx = 0;
            for (LivingEntity t : caught) {
                if (!t.isAlive() || !shatterable(t)) continue;
                b.hurt(i, p, t, mode, 10f);
                t.setTicksFrozen(0);
                if (fx++ < 6) send(p, b, VfxShape.DEMON_ICE_FX3, t.getBoundingBox().getCenter(), t.position(), 24, 1f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- daily
    /** Eternal Winter: a winter that does not end settles over 14 blocks for 15 s. Each second it cuts, freezes, puts out fire, bleeds aura and breaks hostile magic. */
    static boolean winter(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        float r = 14f * GrimoireBook.size(i, p);
        int ticks = 300;
        b.castCircle(p, 1.8f);
        send(p, b, VfxShape.DEMON_ICE_FX2, c, c, ticks, r);
        send(p, b, VfxShape.DEMON_ICE_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 34, 2f);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t)) continue;
                if (n++ >= 32) break;
                b.hurt(i, p, t, mode, 4f);
                chill(t, 60, 3, 80);
                EnergyBridge.effect(t, "frost", 60, 0);
                EnergyBridge.burnAura(t, 0.02);
                t.clearFire();
            }
            Nullification.shatterBarriers(p.serverLevel(), c, r, p);
            if (age % 60 == 0) send(p, b, VfxShape.DEMON_ICE_FX3, c, c.add(0, 1, 0), 22, 2f);
        });
        return true;
    }
}
