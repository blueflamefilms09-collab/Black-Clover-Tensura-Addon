package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: speed is half of what a lightning dragon slayer is (canon: the body turns to lightning, moves where it likes at the speed of a bolt and arrives almost like a teleport),
 * so the route needs a technique that MOVES the caster: Lightning Flash streaks forward along the ground and shocks every foe on the way. It is a server-side teleport to a point
 * that is computed first, stopping at the first wall (so it can never carry the caster through a block or into one), refused cleanly when there is no room or the caster rides
 * a mount, and only then paid for. A teleport is exact where a velocity depends on lag; the picture is the mod's lightning bolt along the path and a crackle that follows the
 * caster. The "Flash" name is this builder's design (canon gives the speed but no spell name), and docs/skilltree/ft_lightning.md says so.
 */
public final class LightningDragonsFlashSkill extends LightningDragonSkill {
    public LightningDragonsFlashSkill() { super(LightningDragonNumbers.LIGHTNING_FLASH, "ft_lightning_flash"); }

    @Override
    protected boolean perform(ServerPlayer p, ManasSkillInstance inst, int lv) {
        if (p.isPassenger()) { AbilityKit.say(p, "You cannot flash from a mount."); return false; }
        Vec3 dir = DragonKit.flatLook(p);
        Vec3 start = p.position();
        Vec3 dest = DragonKit.clearDash(p, dir, num("dash", lv));
        if (dest.distanceToSqr(start) < 1.0) { AbilityKit.say(p, "No room to flash."); return false; }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        double numb = num("numb_seconds", lv);
        Vec3 from = start.add(0.0, 1.0, 0.0), to = dest.add(0.0, 1.0, 0.0);

        List<LivingEntity> foes = AbilityKit.along(p, from, to, num("width", lv));
        List<LivingEntity> hit = DragonKit.strike(p, start, foes, inum("targets", lv), (float) num("damage", lv), DragonKit.mastery(inst), t -> DragonKit.numb(t, numb, 1));

        p.teleportTo(dest.x, dest.y, dest.z);
        AbilityKit.fx(level, VfxShape.LIGHTNING_SPEAR, from, to, 0, 10, 1.2f + 0.1f * lv);
        VfxSpawn.sendFollowing(level, VfxShape.LIGHTNING_FIEND, p, p.position(), 0, 14, 1.0f);
        DragonKit.arcsTo(level, to, hit, 10, 0.8f);
        AbilityKit.sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.9f);
        AbilityKit.sound(p, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.6f);
        return true;
    }
}
