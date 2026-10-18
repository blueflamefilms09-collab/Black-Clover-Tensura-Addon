package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTimers;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: Lightning Dragon's Heavenward Halberd is the long-range answer of the Lightning route (canon: lightning forged between the palms into a great spear and thrown), the
 * technique for the foes the fist and the roar cannot reach, and it is deliberately the opposite of the Fire route's fireball: no flight time and no area, just a long, thin,
 * piercing lane that hits everything in it once, nearest first, capped by a table. Because it reaches eighteen to twenty-six blocks it has a real wind-up, which is its tell (the
 * owner's rule that a technique above eight raw damage shows what it is about to do, which for a far hit must come BEFORE the hit): the lightning gathers between the palms and the
 * lane is drawn at once, the spear is thrown a few ticks later along that same lane, and whoever is in it then is struck, so a foe can step out of a lane that is under a block
 * wide. A thrown spear that pierces is, on the server, a segment test done at once, which is exact and costs nothing per tick, instead of a projectile entity to spawn, track and
 * clean up (the design allows very few spawned entities). The impact pillar at the end is the mod's own lightning effect; the client does no work.
 */
public final class LightningDragonsHalberdSkill extends LightningDragonSkill {
    public LightningDragonsHalberdSkill() { super(LightningDragonNumbers.HALBERD, "ft_lightning_halberd"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (!pay(p, lv)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 from = DragonKit.mouth(p);
        Vec3 end = DragonKit.wallEnd(p, from, from.add(p.getViewVector(1f).scale(num("range", lv))));

        DragonKit.tellLine(level, from, end, TELL_RGB);
        DragonKit.tellGather(level, from, 0.6, TELL_RGB, 14);
        AbilityKit.sound(p, SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 2.0f);

        double mastery = DragonKit.mastery(inst);
        if (!DragonTimers.later(level, Math.max(1, inum("windup", lv)), () -> hurl(p, level, lv, from, end, mastery))) hurl(p, level, lv, from, end, mastery);
        return true;
    }

    /** The spear leaves the palms, a few ticks after the lightning gathered, along the lane that was shown. */
    private void hurl(ServerPlayer p, ServerLevel level, int lv, Vec3 from, Vec3 end, double mastery) {
        if (!DragonKit.ready(p, level)) return;
        double numb = num("numb_seconds", lv);
        List<LivingEntity> foes = AbilityKit.along(p, from, end, num("width", lv));
        List<LivingEntity> hit = DragonKit.strike(p, from, foes, inum("targets", lv), (float) num("damage", lv), mastery, t -> DragonKit.numb(t, numb, 1));

        AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, from, end, 0, 14, 1.8f + 0.2f * lv);
        AbilityKit.fx(level, VfxShape.LIGHTNING_GOD, end, end.add(0.0, 1.0, 0.0), 0, 24, 1.0f + 0.2f * lv);
        DragonKit.arcsTo(level, end, hit, 10, 0.8f);
        AbilityKit.sound(p, SoundEvents.TRIDENT_RETURN, 1.0f, 1.6f);
        DragonKit.soundAt(level, end, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
    }
}
