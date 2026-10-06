package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Time Magic (Julius). Gold circles + stasis rings. */
public class TimeBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.zone("chrono_stasis", "Chrono Stasis", TimeBook::stasis).withCooldown(200),
            BookPage.signature("grigora", "Chrono Stasis Grigora", TimeBook::grigora).withCooldown(600),
            BookPage.mid("acceleration", "Time Acceleration", TimeBook::accel).withCooldown(240),
            BookPage.zone("reversal", "Time Reversal", TimeBook::reversal).withCooldown(400),
            BookPage.starter("stolen_time", "Stolen Time", TimeBook::stolen).withCooldown(160));

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
        b.vfx(p, VfxShape.WATER_RING, t.position().add(0, t.getBbHeight() / 2, 0), t.position().add(0, 2, 0), ticks, 0.6f);
        return true;
    }

    /** 6-block zone, 3 s, caster rooted. Sneak at cast to spare players. Shares the control lock. */
    static boolean grigora(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!control(p)) return false;
        boolean spare = p.isShiftKeyDown();
        Vec3 c = p.position();
        for (Entity e : p.serverLevel().getEntities(p, new AABB(c, c).inflate(6))) {
            if (e.distanceToSqr(c) > 36 || (spare && e instanceof Player)) continue;
            if (e instanceof LivingEntity le) TimeStop.freeze(le, BalanceLaw.controlTicks(le, 60));
            else if (e instanceof Projectile) TimeStop.freeze(e, 60);
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6, false, false));
        b.vfx(p, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), 60, 6 / 1.6f);
        b.vfx(p, VfxShape.WATER_RING, c.add(0, 0.1, 0), c.add(0, 1, 0), 40, 2.0f);   // signature: shake
        return true;
    }

    static boolean accel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 240, 0));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.WIND_RING, p.position(), p.position().add(0, 1, 0), 20, 0.7f);
        return true;
    }

    static boolean reversal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = aim(p, 12);
        int n = com.newuniverse.nusmp.blackclover.BlockHistory.rewindCube(p.serverLevel(), BlockPos.containing(at), 1, 8, 1200);
        if (n == 0) { fail(p, "Nothing here remembers being whole."); return false; }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.MAGIC_CIRCLE, at.add(0, 0.1, 0), at.add(0, 1, 0), 30, 1f);
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
}
