package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.requip.BarrageMath;
import com.newuniverse.nusmp.requip.RequipMagicFx;
import com.newuniverse.nusmp.requip.RequipMagicRules;
import com.newuniverse.nusmp.requip.RequipWeapons;
import com.newuniverse.nusmp.skill.RequipScripts.Script;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the two weapon attacks of the Equip Magic that are built from the weapon itself. <b>Requip: Barrage</b> is Fairy Tail's sword rain: a ring of 8 to 24 spectral copies of the weapon in the hand (or
 * the armoury weapon that is selected) appears round the caster, hangs for a beat and then the swords leave one after another for the foes under the crosshair. Each sword hits for a share of the weapon's
 * own damage read through the grade table ({@link BarrageMath#perSwordRaw}), scaled by the balance law, may pierce one to three creatures, and the whole volley may take at most a tenth of a boss's health.
 * <b>Requip: Strike</b> throws the weapon in the hand along the crosshair and calls it back: it hits on the way out and on the way home. The swords are drawn with the mod's own blade shape (the client
 * flies the blade from where it hangs to where it lands); the damage is resolved by the server when each sword lands.
 */
final class RequipBarrage {
    private RequipBarrage() {}

    private static final double RING_RADIUS = 3.2;
    static final int GOLD = 0xFFFFE08A;

    /** Plans the volley of the weapon. The script is not started: the caller starts it. Returns the script, with {@code launched} filled in as the swords leave. */
    static Script barrage(ServerPlayer p, ItemStack weapon, double mastery, int count, int argb) {
        Vec3 aim = DMUtil.lookPoint(p, 40);
        List<LivingEntity> foes = new ArrayList<>();
        LivingEntity look = SkillTargets.lookFoe(p, 40);
        if (look != null) foes.add(look);
        for (LivingEntity t : SkillTargets.foes(p, aim, 8.0)) if (!foes.contains(t)) foes.add(t);
        return barrage(p, weapon, mastery, count, argb, aim, foes);
    }

    /** The same with the aim point and the foes given (the in-game self-test uses it to put the target close by). */
    static Script barrage(ServerPlayer p, ItemStack weapon, double mastery, int count, int argb, Vec3 aim, List<LivingEntity> foes) {
        double wdmg = RequipWeapons.damageOf(weapon);
        double raw = BarrageMath.perSwordRaw(wdmg, count, mastery);
        int pierce = BarrageMath.pierce(mastery);
        Script s = new Script(p, "barrage", mastery);
        int rgb = argb & 0xFFFFFF;

        s.at(0, pl -> {
            ServerLevel level = pl.serverLevel();
            SkillFx.shape(level, VfxShape.MAGIC_CIRCLE, pl.position().add(0, 0.1, 0), pl.position().add(0, 1.1, 0), argb, BarrageMath.HOVER_TICKS + 10, 1.8f);
            SkillFx.ring(level, pl.position(), RING_RADIUS, rgb);
            SkillFx.sound(level, pl.position(), SoundEvents.BEACON_ACTIVATE, 0.8F, 1.7F);
        });
        for (int t = 0; t < BarrageMath.HOVER_TICKS; t += 2) {
            final int tick = t;
            s.at(t, pl -> {
                double[][] ring = BarrageMath.ring(count, RING_RADIUS, tick * 0.12);
                int shown = Math.min(count, (tick / 2 + 1) * Math.max(1, (count + 6) / 7));       // the swords appear a few at a time
                for (int i = 0; i < shown; i++) RequipMagicFx.hover(pl.serverLevel(), pl.position().add(ring[i][0], ring[i][1], ring[i][2]), rgb);
            });
        }
        for (int i = 0; i < count; i++) {
            final int idx = i;
            final int at = BarrageMath.launchTick(i, count);
            s.at(at, pl -> launch(s, pl, idx, count, at, raw, pierce, aim, foes, argb));
        }
        return s;
    }

    private static void launch(Script s, ServerPlayer pl, int i, int count, int at, double raw, int pierce, Vec3 aim, List<LivingEntity> foes, int argb) {
        ServerLevel level = pl.serverLevel();
        double[] off = BarrageMath.ring(count, RING_RADIUS, at * 0.12)[i];
        Vec3 from = pl.position().add(off[0], off[1], off[2]);
        LivingEntity target = foes.isEmpty() ? null : foes.get(i % foes.size());
        Vec3 to;
        if (target != null && target.isAlive()) to = target.getBoundingBox().getCenter();
        else {
            double[] a = BarrageMath.aimOffset(i, count, 2.2);
            to = new Vec3(aim.x + a[0], aim.y + 0.4, aim.z + a[1]);
        }
        int flight = BarrageMath.flightTicks(from.distanceTo(to));
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, from, to, argb, flight, 0.9f);
        if (i % 4 == 0) SkillFx.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.5F + (i % 8) * 0.03F);
        s.launched++;
        final Vec3 end = to;
        s.at(at + flight, p2 -> land(s, p2, from, end, target, raw, pierce, argb));
    }

    /** The sword lands: the creatures along its path, nearest first, up to its pierce, each hit once. */
    private static void land(Script s, ServerPlayer pl, Vec3 from, Vec3 to, LivingEntity chosen, double raw, int pierce, int argb) {
        Vec3 dir = to.subtract(from);
        Vec3 beyond = dir.lengthSqr() < 1.0e-6 ? to : to.add(dir.normalize().scale(1.2));
        List<LivingEntity> path = SkillTargets.foesAlong(pl, from, beyond, 1.1);
        if (chosen != null && chosen.isAlive() && !path.contains(chosen) && chosen.getBoundingBox().getCenter().distanceTo(to) < 2.0 && SkillTargets.isFoe(pl, chosen)) path.add(0, chosen);
        int n = 0;
        for (LivingEntity t : path) {
            if (n >= pierce) break;
            s.hit(t, raw, 0.0);
            n++;
        }
        RequipMagicFx.land(pl.serverLevel(), to, argb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ Requip: Strike
    /**
     * Plans the throw of the weapon that {@link RequipWeapons#throwOut} has already taken out of the hand: out along the crosshair, a hit on every foe on the line, and home again with a second hit.
     * The weapon is put back by the last step; {@link ErzaRequipMagic#safetyNet} puts it back if the caster is gone before that.
     */
    static Script strike(ServerPlayer p, ItemStack thrown, double mastery, int capacity, int argb) {
        double raw = BarrageMath.strikeRaw(RequipWeapons.damageOf(thrown), mastery);
        double range = RequipMagicRules.strikeRange(mastery);
        Vec3 eye = p.getEyePosition();
        Vec3 end = DMUtil.lookPoint(p, range);
        int leg = BarrageMath.flightTicks(eye.distanceTo(end));
        Script s = new Script(p, "strike", mastery);
        s.at(0, pl -> {
            ServerLevel level = pl.serverLevel();
            SkillFx.shape(level, VfxShape.WIND_SLASH, pl.getEyePosition().add(0, -0.3, 0), end, argb, leg, 1.4f);
            SkillFx.sound(level, pl.position(), SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.2F);
        });
        s.at(leg, pl -> {
            ServerLevel level = pl.serverLevel();
            for (LivingEntity t : SkillTargets.foesAlong(pl, eye.add(0, -0.3, 0), end, 1.4)) s.hit(t, raw, 0.0);
            RequipMagicFx.land(level, end, argb & 0xFFFFFF);
            SkillFx.shape(level, VfxShape.WIND_SLASH, end, pl.getEyePosition().add(0, -0.3, 0), argb, leg, 1.4f);
            SkillFx.sound(level, end, SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F);
        });
        s.at(leg * 2, pl -> {
            for (LivingEntity t : SkillTargets.foesAlong(pl, end, pl.getEyePosition(), 1.4)) s.hit(t, raw * 0.8, 0.0);
            if (!RequipWeapons.recall(pl, ErzaRequipMagic.capacity(pl))) SkillUtil.fail(pl, SkillMsg.t("erza.strike_wait", "The weapon waits in the pocket for room."));
        });
        s.at(RequipMagicRules.STRIKE_MAX_TICKS, pl -> RequipWeapons.recall(pl, ErzaRequipMagic.capacity(pl)));          // never out longer than this
        return s;
    }

    /** How many swords a cast will have (for the messages and the test). */
    static int swordsFor(ServerPlayer p, double mastery) { return BarrageMath.swordCount(mastery, Nullification.maxEP(p)); }
}
