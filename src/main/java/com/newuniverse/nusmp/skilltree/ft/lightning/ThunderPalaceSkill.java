package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTimers;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Thunder Palace is the area-denial technique of the Lightning route (canon: lacrima orbs filled with lightning hang in a circle over a place and, once released, strike the
 * area with bolts), and it is the one that WARNS: a ring of dust on the ground where the palace stands, an orb circle in the air, and a column of dust a few ticks before every
 * single bolt, so the foes (and the allies) can leave the spots that are about to be struck. A palace is a schedule, not an entity: the strike points are drawn once at the cast,
 * each warning and each bolt is a server event joined by {@link DragonTimers}, the bolts are spaced by at least the vanilla hurt-frame margin so a foe standing in the area is hit
 * by each bolt that reaches it and the balance law's per-hit cap is the only limit, and the whole thing stops if the caster dies. No entity is spawned and no block is touched.
 */
public final class ThunderPalaceSkill extends LightningDragonSkill {
    /** Ticks before a bolt that its warning column appears. */
    private static final int WARNING_TICKS = 6;

    public ThunderPalaceSkill() { super(LightningDragonNumbers.THUNDER_PALACE, "ft_lightning_thunder_palace"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 center = DragonKit.groundBelow(p, DragonKit.aimPoint(p, num("range", lv)));
        double radius = num("radius", lv);
        int strikes = Math.max(1, Math.min(DragonBudget.MAX_FOLLOW_UPS, inum("strikes", lv)));
        int delay = Math.max(1, inum("delay", lv));
        int gap = Math.max(DragonBudget.MIN_PULSE_GAP_TICKS, inum("gap", lv));
        double mastery = DragonKit.mastery(inst);
        RandomSource rnd = p.getRandom();

        DragonKit.tellRing(level, center, radius, TELL_RGB);
        Vec3 sky = center.add(0.0, 6.0, 0.0);
        AbilityKit.fx(level, VfxShape.MAGIC_CIRCLE, sky, center, BOLT_ARGB, delay + strikes * gap, (float) (radius * 0.45));
        AbilityKit.sound(p, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.7f);

        for (int i = 0; i < strikes; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0, r = Math.sqrt(rnd.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
            int due = delay + i * gap;
            DragonTimers.later(level, Math.max(0, due - WARNING_TICKS), () -> {
                if (DragonKit.ready(p, level)) DragonKit.tellColumn(level, at, 3.0, TELL_RGB);
            });
            DragonTimers.later(level, due, () -> bolt(p, level, lv, at, mastery));
        }
        return true;
    }

    /** One bolt of the palace falls on {@code at}. */
    private void bolt(ServerPlayer p, ServerLevel level, int lv, Vec3 at, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        double numb = num("numb_seconds", lv), radius = num("strike_radius", lv);
        List<LivingEntity> foes = AbilityKit.around(p, at, radius);
        List<LivingEntity> hit = DragonKit.strike(p, at, foes, inum("targets", lv), (float) num("damage", lv), mastery, t -> DragonKit.numb(t, numb, 1));
        AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, at.add(0.0, 14.0, 0.0), at, 0, 8, 1.2f);
        AbilityKit.fx(level, VfxShape.LIGHTNING_GOD, at, at.add(0.0, 1.0, 0.0), 0, 20, (float) (radius * 0.8));
        DragonKit.arcsTo(level, at, hit, 8, 0.7f);
        DragonKit.soundAt(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.3f);
    }
}
