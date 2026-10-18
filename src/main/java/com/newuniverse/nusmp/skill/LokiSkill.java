package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.GodRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Loki, the Trickster: vanish and blink forward. The blink uses the relics' safe-landing search (a spot with room to stand and ground
 * below, never lava or fire, never inside a wall) and fails without cost if there is no such spot in reach. On arrival you turn
 * invisible and every monster nearby that was hunting you forgets you. Numbers: {@link GodRules.Loki}.
 */
public class LokiSkill extends GodSkill {
    static final SkillText TEXT = SkillText.of("loki", "Loki",
            "Unique. The Trickster. Vanish from sight and blink forward; monsters that were hunting you lose track of you.");

    public LokiSkill() { super(TEXT); }

    @Override protected double cost() { return GodRules.Loki.COST; }
    @Override protected int cooldownTicks() { return GodRules.Loki.COOLDOWN; }
    @Override protected int color() { return 0xFF3FD8A8; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        if (!com.newuniverse.nusmp.relics.Fx.teleportLook(player, GodRules.Loki.blinkRange(tier))) {
            SkillUtil.fail(player, SkillMsg.t("loki.no_room", "No room to blink."));
            return false;
        }
        Vec3 to = player.position();
        if (from.distanceTo(to) < GodRules.Loki.minBlink()) {           // a wall right in front: put the player back
            player.teleportTo(from.x, from.y, from.z);
            SkillUtil.fail(player, SkillMsg.t("loki.no_room", "No room to blink."));
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, GodRules.Loki.invisibleTicks(tier), 0));
        for (LivingEntity foe : SkillTargets.foes(player, from, GodRules.Loki.forgetRadius(tier))) {
            if (foe instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);
        }
        SkillFx.burst(level, from.add(0, 1, 0), true, 12, 0.5);
        SkillFx.burst(level, to.add(0, 1, 0), true, 12, 0.5);
        SkillFx.shape(level, VfxShape.SPATIAL_RIFT, from.add(0, 1, 0), to.add(0, 1, 0), 0xFF6A3FC0, 20, 0.8f);
        SkillFx.sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.3F);
        return true;
    }
}
