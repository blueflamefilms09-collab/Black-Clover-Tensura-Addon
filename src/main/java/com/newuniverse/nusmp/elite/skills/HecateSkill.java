package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.UniqueRules;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hecate (Unique, designed for this mod from the triple goddess of the moon and the crossroads; the original addon's source was not available): three faces, three modes, and the real moon
 * decides how strong each is. MAIDEN (the waxing moon) fires a Moonbolt of silver light along your sight (36 blocks) that pierces every foe on it and marks them with a glow, and quickens
 * you. MOTHER (the full moon) blesses everyone near you (10 blocks) with healing and cleansing and a spell of regeneration. CRONE (the new moon) lays a hex on the crossroads: foes around you
 * are weakened and slowed, lose track of you (hunters forget their target) and, if the shared control lock is free, are blinded for a moment, while you vanish for four seconds. A face is
 * strongest when the moon is at its own phase (x1.4) and weakest at the opposite one (x0.85); by day the moon's power is only 70%. Numbers: {@link UniqueRules.Hecate}.
 */
public class HecateSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("hecate", "Hecate",
            "Unique. Three faces of the moon: the Maiden's piercing Moonbolt, the Mother's healing blessing, the Crone's hex of the crossroads. Each is strongest at its own moon phase, and weaker by day.");

    public HecateSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("hecate.maiden", "Maiden: Moonbolt", UniqueRules.Hecate.cost(UniqueRules.Hecate.Form.MAIDEN), UniqueRules.Hecate.cooldown(UniqueRules.Hecate.Form.MAIDEN)),
                EliteMode.of("hecate.mother", "Mother: Blessing", UniqueRules.Hecate.cost(UniqueRules.Hecate.Form.MOTHER), UniqueRules.Hecate.cooldown(UniqueRules.Hecate.Form.MOTHER)),
                EliteMode.of("hecate.crone", "Crone: Crossroads Hex", UniqueRules.Hecate.cost(UniqueRules.Hecate.Form.CRONE), UniqueRules.Hecate.cooldown(UniqueRules.Hecate.Form.CRONE)));
    }

    @Override protected int color() { return 0xFFCFE8FF; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        UniqueRules.Hecate.Form form = UniqueRules.Hecate.form(mode);
        ServerLevel level = player.serverLevel();
        double power = UniqueRules.Hecate.moonPower(form, EliteUtil.moonPhase(level), level.isNight());
        boolean mastered = masteredBy(instance, player);
        double mastery = masteryFrac(instance);
        EliteBridge.actionbar(player, SkillMsg.t("hecate.power", "Hecate's moon power: x%s", String.format(java.util.Locale.ROOT, "%.2f", power)).withStyle(ChatFormatting.AQUA));
        return switch (form) {
            case MAIDEN -> moonbolt(player, level, power, mastered, mastery);
            case MOTHER -> blessing(player, level, power, mastered);
            case CRONE -> hex(player, level, power, mastered);
        };
    }

    private boolean moonbolt(ServerPlayer player, ServerLevel level, double power, boolean mastered, double mastery) {
        Vec3 start = player.getEyePosition().add(0, -0.2, 0);
        Vec3 end = EliteBridge.lookPoint(player, UniqueRules.Hecate.boltRange(mastered));
        double dmg = UniqueRules.Hecate.boltDamage(mastered) * power * EliteSkillsConfig.damageScale();
        for (LivingEntity t : EliteBridge.foesAlong(player, start, end, 1.0)) {
            EliteBridge.hit(player, t, dmg, mastery, true);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
            EliteBridge.spray(level, false, t.position().add(0, t.getBbHeight() * 0.6, 0), 6, 0.3, 0.3, 0.3, 0.04);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, true, false));
        EliteBridge.beam(level, start, end, false);
        EliteBridge.shape(level, VfxShape.MIRROR_RAY, start, end, 0xFFCFE8FF, 14, 0.9f);
        EliteBridge.shape(level, VfxShape.LIGHT_FLARE, end, end, 0xFFFFFFFF, 10, 0.8f);
        EliteBridge.sound(level, player.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 1.6f);
        return true;
    }

    private boolean blessing(ServerPlayer player, ServerLevel level, double power, boolean mastered) {
        Vec3 at = player.position();
        double share = Math.min(0.75, UniqueRules.Hecate.healShare(mastered) * power);
        int healed = 0;
        for (LivingEntity ally : EliteBridge.alliesAround(player, at, UniqueRules.Hecate.blessRadius(mastered))) {
            BalanceLaw.heal(ally, (float) (ally.getMaxHealth() * share));
            EliteBridge.cleanse(ally);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (200 * power), 0, true, false));
            EliteBridge.aura(level, ally, false);
            healed++;
        }
        EliteBridge.follow(level, VfxShape.DIVINE_HALO, player, at.add(0, 1, 0), 0xFFFFF2C8, 40, 1.1f);
        EliteBridge.sound(level, at, SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.3f);
        EliteBridge.actionbar(player, SkillMsg.t("hecate.blessed", "The Mother's light touches %s.", healed).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    private boolean hex(ServerPlayer player, ServerLevel level, double power, boolean mastered) {
        Vec3 at = player.position();
        int ticks = (int) (UniqueRules.Hecate.hexTicks(mastered) * power);
        boolean control = BalanceLaw.beginControl(player);
        int hexed = 0;
        for (LivingEntity t : EliteBridge.foes(player, at, UniqueRules.Hecate.hexRadius(mastered))) {
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, true, false));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, true, false));
            if (control) {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(60, ticks / 2), 0, true, false));
                if (t instanceof Mob m) m.setTarget(null);                    // lost at the crossroads: the hunter forgets whom it hunted
            }
            EliteBridge.spray(level, true, t.position().add(0, t.getBbHeight() * 0.6, 0), 6, 0.3, 0.4, 0.3, 0.03);
            hexed++;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, UniqueRules.Hecate.veilTicks(), 0, true, false));
        EliteBridge.shape(level, VfxShape.DARK_BLACK_MOON, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF3A2A5A, 40, 1.2f);
        EliteBridge.burst(level, at.add(0, 1, 0), true, 16, 1.0);
        EliteBridge.sound(level, at, SoundEvents.WITHER_AMBIENT, 0.6f, 1.5f);
        EliteBridge.actionbar(player, SkillMsg.t("hecate.hexed", "The crossroads swallow %s foes.", hexed).withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }
}
