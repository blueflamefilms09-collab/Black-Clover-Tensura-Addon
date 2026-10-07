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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ice Wedge Magic: the casts of the grimoire that are not a shared shape. Wedges are driven in, they slow and pin what they hold, and a
 * pinned foe that is split open takes the wedge's full weight. World End Martyr and World End Church are the wiki's two great spells.
 */
final class IceWedgeArts {
    private IceWedgeArts() {}

    // ---------------------------------------------------------------- shared
    /** Creative and spectator players are never touched. */
    static boolean valid(LivingEntity t) { return !(t instanceof Player pl && (pl.isCreative() || pl.isSpectator())); }

    /** Slowness plus frozen ticks; bosses get a third of the frost and a milder slow. */
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
    /** Wedge Driver: a wedge is hammered into one foe within reach; it bleeds magicules and is pinned, and a foe already frozen takes half again. */
    static boolean driver(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 7);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "No one in reach."); return false; }
        float dmg = t.isFullyFrozen() ? 13.5f : 9f;
        b.hurt(i, p, t, mode, dmg);
        chill(t, 80, 3, 60);
        Nullification.bleed(t, 0.04);
        EnergyBridge.effect(t, "frost", 60, 0);
        b.castCircle(p, 0.6f);
        Vec3 c = t.getBoundingBox().getCenter();
        send(p, b, VfxShape.ICE_WEDGE_FX1, p.getEyePosition(), c, 8, 1f);
        send(p, b, VfxShape.ICE_WEDGE_FX3, c, c.subtract(p.position()), 22, 1f);
        return true;
    }

    // ---------------------------------------------------------------- mid
    /** Splitting Wedge: a wedge pins one foe where it stands (it cannot move), then the wedge splits and bursts for damage that chills everyone beside it. */
    static boolean split(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "No one to pin."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 70);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        t.setTicksFrozen(t.getTicksRequiredToFreeze() + ticks * 2);
        b.hurt(i, p, t, mode, 5f);
        b.castCircle(p, 0.8f);
        send(p, b, VfxShape.ICE_WEDGE_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1f);
        send(p, b, VfxShape.ICE_WEDGE_FX2, t.position(), t.position(), ticks, Math.max(1.2f, t.getBbWidth() * 1.2f));
        SpellRuntime.later(p.serverLevel(), ticks, () -> {
            if (!t.isAlive()) return;
            Vec3 c = t.position();
            b.hurt(i, p, t, mode, 11f);
            int n = 0;
            for (LivingEntity o : GrimoireBook.around(p, c, 3.5)) {
                if (!valid(o) || o == t) continue;
                if (n++ >= 8) break;
                b.hurt(i, p, o, mode, 6f);
                chill(o, 80, 2, 60);
            }
            send(p, b, VfxShape.ICE_WEDGE_FX3, t.getBoundingBox().getCenter(), c, 26, 1.2f);
        });
        return true;
    }

    /** Frost Brand: for 20 s your blows carry a wedge of ice (extra damage, chill, a little bleed); see the book's damage hooks. */
    static boolean brand(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 400;
        var tag = i.getOrCreateTag();
        tag.putLong("WedgeBrandUntil", p.level().getGameTime() + ticks);
        tag.putFloat("WedgeBrandBonus", 4.5f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ICE_WEDGE_FX2, p, p.position().add(0, 1, 0), b.color, ticks, 1.3f);
        send(p, b, VfxShape.ICE_WEDGE_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 24, 0.9f);
        return true;
    }

    /** Wedge Aegis: wedges ring you for 15 s. They break the shots that come at you, thicken your skin and chill whoever strikes you. */
    static boolean aegis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 300;
        i.getOrCreateTag().putLong("WedgeAegisUntil", p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ICE_WEDGE_FX2, p, p.position().add(0, 1, 0), b.color, ticks, 2.2f);
        send(p, b, VfxShape.ICE_WEDGE_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 24, 1f);
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            if (!p.isAlive()) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(4)))
                if (pr.getOwner() != p) {
                    send(p, b, VfxShape.ICE_WEDGE_FX3, pr.position(), pr.position(), 12, 0.5f);
                    pr.discard();
                }
        });
        return true;
    }

    /** Wedge Wall: two angled rows of packed ice are driven up in a V that points at the foe; those at its foot are slowed. */
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(2.5));
        int placed = 0;
        for (int w = -3; w <= 3; w++) for (int h = 0; h < 3; h++) {
            BlockPos pos = BlockPos.containing(base.add(side.scale(w)).add(dir.scale(Math.abs(w) * 0.7)).add(0, h, 0));
            if (sl.isLoaded(pos) && SpellRuntime.tempBlock(sl, pos, Blocks.PACKED_ICE.defaultBlockState(), 200)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for a wall."); return false; }
        Vec3 tip = base.add(dir.scale(2.1));
        for (LivingEntity t : GrimoireBook.around(p, tip, 4.5)) if (valid(t)) chill(t, 80, 2, 60);
        b.castCircle(p, 0.8f);
        send(p, b, VfxShape.ICE_WEDGE_FX2, tip, tip, 60, 3.5f);
        send(p, b, VfxShape.ICE_WEDGE_FX3, base.add(0, 1, 0), base.add(dir), 24, 1f);
        return true;
    }

    // ---------------------------------------------------------------- zone
    /** Wedge Field: for 8 s wedges thrust out of the ground at the aim point; each wave cuts everyone inside, slows them hard and keeps them frozen. */
    static boolean field(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float r = 6.5f * GrimoireBook.size(i, p);
        int ticks = 160;
        b.castCircle(p, 1f);
        send(p, b, VfxShape.ICE_WEDGE_FX2, c, c, ticks, r);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t)) continue;
                if (n++ >= 24) break;
                b.hurt(i, p, t, mode, 3f);
                chill(t, 50, 3, 40);
            }
            for (int k = 0; k < 3; k++) {
                double a = (age * 1.7 + k * 2.1), d = r * (0.3 + 0.25 * k);
                Vec3 at = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                send(p, b, VfxShape.ICE_WEDGE_FX3, at, at.add(0, 1, 0), 16, 0.6f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- signature
    /**
     * World End Martyr: a giant of sharp ice (angel wings, talons, two halos, a cross for a face) rises at the aim point for 6 s. Its wings
     * break hostile barriers on rising; every half second it looses freezing beams at the three nearest foes within 18 blocks and its
     * talons stamp everything at its feet. The mechanical body is the beams and the footfall: no real mob is spawned.
     */
    static boolean martyr(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        float s = GrimoireBook.size(i, p);
        int ticks = 120;
        Vec3 head = c.add(0, 7, 0);
        b.castCircle(p, 1.6f);
        send(p, b, VfxShape.ICE_WEDGE_FX2, c, c, ticks, 9f * s);
        send(p, b, VfxShape.ICE_WEDGE_FX3, c.add(0, 3, 0), c.add(0, 8, 0), 40, 2f);
        Nullification.shatterBarriers(p.serverLevel(), c, 12 * s, p);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            List<LivingEntity> near = new ArrayList<>(GrimoireBook.around(p, c, 18 * s));
            near.removeIf(t -> !valid(t));
            near.sort(Comparator.comparingDouble(t -> t.distanceToSqr(c)));
            for (int k = 0; k < Math.min(3, near.size()); k++) {
                LivingEntity t = near.get(k);
                b.hurt(i, p, t, mode, 6f);
                chill(t, 80, 3, 100);
                EnergyBridge.effect(t, "frost", 60, 0);
                send(p, b, VfxShape.ICE_WEDGE_FX1, head, t.getBoundingBox().getCenter(), 10, 1.4f);
            }
            if (age % 30 == 0) {
                for (LivingEntity t : GrimoireBook.around(p, c, 5 * s)) {
                    if (!valid(t)) continue;
                    b.hurt(i, p, t, mode, 8f);
                    Vec3 d = t.position().subtract(c).multiply(1, 0, 1);
                    if (d.lengthSqr() > 0.01) { d = d.normalize(); t.knockback(0.8, -d.x, -d.z); }
                }
                send(p, b, VfxShape.ICE_WEDGE_FX3, c, c.add(0, 1, 0), 24, 1.6f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- mobility
    /** Wedge Step: you ride a wedge of ice behind the foe in front of you and drive it in as you land. */
    static boolean step(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return ElementBook.dash(8f, 0, true, VfxShape.ICE_WEDGE_FX1, chilled(80, 2, 60)).cast(b, i, p, mode);
    }

    // ---------------------------------------------------------------- daily
    /**
     * World End Church: a church of ice rises around you and the cold falls to absolute zero for 15 s inside 15 blocks: every second it cuts,
     * freezes, puts out fire, bleeds magicules and breaks hostile magic; eight pillars of blue ice stand at its walls. (Lava is not turned to
     * stone: blocks are only ever placed temporarily.)
     */
    static boolean church(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        float r = 15f * GrimoireBook.size(i, p);
        int ticks = 300;
        b.castCircle(p, 1.8f);
        send(p, b, VfxShape.ICE_WEDGE_FX2, c, c, ticks, r);
        send(p, b, VfxShape.ICE_WEDGE_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 36, 2f);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            for (int h = 0; h < 5; h++) {
                BlockPos pos = BlockPos.containing(c.add(Math.cos(a) * 7, h, Math.sin(a) * 7));
                if (sl.isLoaded(pos)) SpellRuntime.tempBlock(sl, pos, Blocks.BLUE_ICE.defaultBlockState(), ticks);
            }
        }
        SpellRuntime.zone(sl, ticks, 20, age -> {
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
            Nullification.shatterBarriers(sl, c, r, p);
            if (age % 60 == 0) send(p, b, VfxShape.ICE_WEDGE_FX3, c, c.add(0, 1, 0), 22, 2f);
        });
        return true;
    }
}
