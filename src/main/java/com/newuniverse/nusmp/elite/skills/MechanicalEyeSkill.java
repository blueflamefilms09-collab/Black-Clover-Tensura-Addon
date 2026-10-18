package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.IntrinsicRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Mechanical Eye (Intrinsic, designed for this mod; the original addon's source was not available): an eye that measures. Mode 1, Scan Pulse, makes the foes around you (24 blocks, 40 mastered,
 * at most 16) glow for five seconds and reads the nearest one out on the action bar: its health, its armour and the blow it hits with. Mode 2, Weak Point, marks the foe you look at: for ten
 * seconds everything YOU do to it hits 20% harder (the damage handler in {@link EliteSkillEvents} asks {@link #markMultiplier}). It is not Hawk Eye: Hawk Eye lights the dark and glows hostiles
 * while toggled; this one analyses and marks. Numbers: {@link IntrinsicRules.MechanicalEye}.
 */
public class MechanicalEyeSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("mechanical_eye", "Mechanical Eye",
            "Intrinsic. Scan Pulse lights the foes around you and reads the nearest one out (health, armour, damage). Weak Point marks the foe you look at: your hits on it are 20% stronger for ten seconds.");

    static final String MARK = "MarkUuid", MARK_UNTIL = "MarkUntil";

    public MechanicalEyeSkill() { super(SkillType.INTRINSIC, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("mechanical_eye.scan", "Scan Pulse", 120.0, 100),
                EliteMode.of("mechanical_eye.mark", "Weak Point", IntrinsicRules.MechanicalEye.MARK_COST, IntrinsicRules.MechanicalEye.MARK_COOLDOWN));
    }

    @Override protected int color() { return 0xFF7AD8FF; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean mastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        if (mode == 1) {
            LivingEntity t = EliteBridge.lookFoe(player, IntrinsicRules.MechanicalEye.markRange(mastered));
            if (t == null) {
                EliteBridge.fail(player, SkillMsg.t("mechanical_eye.no_target", "No foe in your sights."));
                return false;
            }
            var tag = instance.getOrCreateTag();
            tag.putUUID(MARK, t.getUUID());
            tag.putLong(MARK_UNTIL, level.getGameTime() + IntrinsicRules.MechanicalEye.MARK_TICKS);
            instance.markDirty();
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, IntrinsicRules.MechanicalEye.MARK_TICKS, 0, true, false));
            EliteBridge.shape(level, VfxShape.EYE_FX1, player.getEyePosition(), t.getBoundingBox().getCenter(), 0xFF7AD8FF, 20, 0.8f);
            EliteBridge.sound(level, player.position(), SoundEvents.ENDER_EYE_DEATH, 0.8f, 1.6f);
            EliteBridge.actionbar(player, SkillMsg.t("mechanical_eye.marked", "Weak point found: %s.", t.getDisplayName()).withStyle(ChatFormatting.AQUA));
            return true;
        }
        double range = IntrinsicRules.MechanicalEye.scanRange(mastered);
        Vec3 here = player.position();
        List<LivingEntity> foes = EliteBridge.foes(player, here, range);
        if (foes.isEmpty()) {
            EliteBridge.fail(player, SkillMsg.t("mechanical_eye.nothing", "The scan finds no foes."));
            return false;
        }
        int lit = 0;
        for (LivingEntity e : foes) {
            if (lit++ >= IntrinsicRules.MechanicalEye.MAX_TRACKED) break;
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
        }
        LivingEntity near = foes.get(0);
        var dmg = near.getAttribute(Attributes.ATTACK_DAMAGE);
        EliteBridge.actionbar(player, SkillMsg.t("mechanical_eye.read", "%s: %s of %s health, armour %s, hits for %s. %s foes scanned.", near.getDisplayName(),
                (int) Math.ceil(near.getHealth()), (int) Math.ceil(near.getMaxHealth()), near.getArmorValue(), dmg == null ? 0 : (int) Math.round(dmg.getValue()), foes.size())
                .withStyle(ChatFormatting.AQUA));
        EliteBridge.ring(level, here.add(0, 0.1, 0), Math.min(range, 12.0), 0x7AD8FF);
        EliteBridge.shape(level, VfxShape.EYE_FX1, player.getEyePosition(), here.add(0, 1, 0), 0xFF7AD8FF, 16, 0.6f);
        EliteBridge.sound(level, here, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.8f);
        return true;
    }

    /** The multiplier on a blow by this player at this victim: 1.2 while the victim is the live mark. */
    static double markMultiplier(ManasSkillInstance instance, LivingEntity attacker, LivingEntity victim) {
        var tag = instance.getOrCreateTag();
        if (!tag.hasUUID(MARK)) return 1.0;
        UUID id = tag.getUUID(MARK);
        boolean live = IntrinsicRules.MechanicalEye.markActive(attacker.level().getGameTime(), tag.getLong(MARK_UNTIL));
        return IntrinsicRules.MechanicalEye.markedMultiplier(live && id.equals(victim.getUUID()));
    }
}
