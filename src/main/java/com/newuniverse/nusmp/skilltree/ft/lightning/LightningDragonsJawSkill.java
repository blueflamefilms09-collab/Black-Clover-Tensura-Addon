package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Lightning Dragon's Jaw is the close-quarters area technique of the Lightning route (canon: both hands clasped and brought down on the target, a shockwave around the
 * point of impact), the answer to being surrounded, and it sits on the fist branch next to the Breakdown Fist: one spot a short way in front of the caster, everyone around that
 * spot hit once, numbed, and thrown outward. The slam is an instant server-side effect (a slam needs no flight), centred on a point fixed relative to the caster so it is easy to
 * learn, announced by a ring of warning dust for the levels above the owner's eight-raw-damage line, and drawn by the mod's own lightning pillar. A shove is knock-back, not hard
 * control, so no control gate is needed.
 */
public final class LightningDragonsJawSkill extends LightningDragonSkill {
    public LightningDragonsJawSkill() { super(LightningDragonNumbers.JAW, "ft_lightning_jaw"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        float raw = (float) num("damage", lv);
        double radius = num("radius", lv), numb = num("numb_seconds", lv), knock = num("knockback", lv);
        Vec3 dir = DragonKit.flatLook(p);
        Vec3 center = p.position().add(dir.scale(num("reach", lv)));

        if (DragonBudget.needsTell(raw)) DragonKit.tellRing(level, center, radius, TELL_RGB);
        List<LivingEntity> foes = DragonKit.sight(p, AbilityKit.around(p, center, radius));
        List<LivingEntity> hit = DragonKit.strike(p, center, foes, inum("targets", lv), raw, DragonKit.mastery(inst), t -> {
            DragonKit.numb(t, numb, 1);
            DragonKit.knock(t, DragonKit.awayFrom(center, t, dir), knock);
        });

        AbilityKit.fx(level, VfxShape.LIGHTNING_GOD, center, center.add(0.0, 1.0, 0.0), 0, 24, (float) (radius * 0.9));
        AbilityKit.fx(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, center, center.add(0.0, 1.0, 0.0), BOLT_ARGB, 20, (float) (radius * 0.4));
        DragonKit.arcsTo(level, center.add(0.0, 0.2, 0.0), hit, 10, 0.8f);
        AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.7f);
        DragonKit.soundAt(level, center, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.1f, 0.9f);
        return true;
    }
}
