package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.EarthConstruct;
import com.newuniverse.nusmp.prop.EarthConstructProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 The spells of the earth family that are more than a parametrised shape:
 * <ul>
 *   <li>{@link #prison}: Sandstone Magic's Variable Hardness Prison (the one canon spell of the family): a large block of sandstone leaves the grimoire, dissolves into
 *       sand and reforms round the target, then hardens and the target is left to suffocate. A cage prop (EARTH_CONSTRUCT / CAGE) closes round it, holds it inside and
 *       presses the breath out of it for the control time (damage ticks, rooted); hard control, so it spends the shared control lock and bosses are held for less.</li>
 *   <li>{@link #titan}: Rock Magic's Boulder Titan, a crag giant that stomps and roars (hard control on the roar) with a shock where it rises.</li>
 *   <li>{@link #garden}: Mineral Magic's crystal garden: three spires grown round the aim point, each pulsing shards.</li>
 *   <li>{@link #warPaint}: Red Ochre's War Paint: the caster's body is painted (the clay armour forges), melee hits deal more, and every foe near is marked.</li>
 *   <li>{@link #sinkhole}: Earth Magic's Swallowing Ground: the earth opens under a target (rooted, crushed) and closes again with a second blow.</li>
 * </ul>
 */
final class EarthFamilyArts {
    private EarthFamilyArts() {}

    static boolean prison(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 22);
        if (t == null || t == p || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to imprison."); return false; }
        if (!GrimoireBook.control(p)) return false;
        ServerLevel level = p.serverLevel();
        int hold = BalanceLaw.controlTicks(t, 100);
        float s = Math.max(0.9f, Math.min(2.4f, Math.max(t.getBbWidth() / 1.1f, t.getBbHeight() / 1.9f)));
        MagicPropEntity cage = EarthConstructProps.spawn(level, EarthConstruct.CAGE, t.position(), 0f, s, 14 + hold + 18, p);
        if (cage == null) { GrimoireBook.fail(p, "The sandstone would not take form."); return false; }
        cage.setTarget(t);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14 + hold, 9));
        b.castCircle(p, 1.2f);
        EarthKit.fx(b, level, VfxShape.EARTHFAM_CAST, p.getEyePosition(), t.getBoundingBox().getCenter(), 14, 1.3f);       // the block leaves the grimoire and dissolves into sand
        EarthKit.flash(b, p);
        return true;
    }

    static boolean titan(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!EarthKit.guardian(EarthConstruct.TITAN, 600, 1).cast(b, i, p, mode)) return false;
        Vec3 c = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).scale(2));
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, 4.0)) {
            if (n++ >= 8) break;
            b.hurt(i, p, t, mode, 9f);
            EarthKit.knock(0.7).apply(t, p);
        }
        EarthKit.fx(b, p.serverLevel(), VfxShape.EARTHFAM_BURST, c, c.add(0, 1, 0), 28, 1.6f);
        return true;
    }

    static boolean garden(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 20);
        EarthKit.dismiss(p, EarthConstruct.SPIRE);
        float s = Math.min(1.5f, EarthConstructProps.sizeFor(p) * GrimoireBook.size(i, p));
        int life = EarthConstructProps.lifeFor(p, 200), made = 0;
        int count = EarthKit.scaled(p, 3, 2);
        for (int k = 0; k < count; k++) {
            double a = k * Math.PI * 2 / count + p.getYRot() * 0.0174;
            if (EarthConstructProps.spawn(level, EarthConstruct.SPIRE, c.add(Math.cos(a) * 2.2, 0, Math.sin(a) * 2.2), (float) Math.toDegrees(a), s, life, p) != null) made++;
        }
        if (made == 0) { GrimoireBook.fail(p, "The crystals would not grow."); return false; }
        b.castCircle(p, 1f);
        EarthKit.fx(b, level, VfxShape.EARTHFAM_ZONE, c.add(0, 0.05, 0), c.add(0, 1, 0), 60, 3.2f);
        return true;
    }

    static boolean warPaint(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!EnergyBridge.aura(p, 0.05, 40)) { GrimoireBook.fail(p, "Not enough aura to paint your body."); return false; }
        int ticks = (int) (240 * Math.min(1.5, 1 + 0.04 * (BalanceLaw.epScale(p) - 1)));
        EarthKit.armour(b, i, p, ticks, -1, 5f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
            if (n++ >= 10) break;
            EarthKit.mark(120).apply(t, p);
        }
        b.castCircle(p, 1.3f);
        EarthKit.fx(b, p.serverLevel(), VfxShape.EARTHFAM_ZONE, p.position(), p.position().add(0, 1, 0), 30, 3.5f);
        return true;
    }

    /** Earth Magic's Swallowing Ground (called from EarthBook). */
    static boolean sinkhole(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity target = GrimoireBook.target(p, 20);
        Vec3 c = target != null ? target.position() : GrimoireBook.aim(p, 20);
        ServerLevel level = p.serverLevel();
        double r = 2.6 * GrimoireBook.size(i, p);
        boolean root = BalanceLaw.beginControl(p);
        b.castCircle(p, 1f);
        EarthKit.fx(b, level, VfxShape.EARTHFAM_ZONE, c.add(0, 0.05, 0), c.add(0, 1, 0), 44, (float) r);
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, 6f);
            if (root) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 50), 8));
        }
        SpellRuntime.later(level, 24, () -> {                                                                // the ground closes again
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, 9f);
                t.setDeltaMovement(t.getDeltaMovement().add(0, BalanceLaw.isBoss(t) ? 0.15 : 0.5, 0));
                t.hurtMarked = true;
            }
            EarthKit.fx(b, level, VfxShape.EARTHFAM_BURST, c, c.add(0, 1, 0), 24, (float) r * 0.5f);
        });
        return true;
    }
}
