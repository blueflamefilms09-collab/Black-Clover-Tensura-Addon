package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.MagicRules;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kamehameha (Aspectual magic, designed for this mod from the Dragon Ball technique; the original addon's source was not available): HOLD the key to gather ki in your hands. Every second of the
 * hold (0.7 s once mastered) is one level, up to five, shown on the action bar; RELEASE to fire one piercing beam along your line of sight that strikes every foe on it and stops at the first
 * wall. A higher level is longer, wider, harder and dearer, and a level-2-or-more beam leaves you braced and slowed for a moment. Letting go before level 1 fizzles and costs nothing; if you
 * cannot afford the level you reached, the beam is fired at the highest level you can pay. Numbers: {@link MagicRules.Kamehameha}.
 */
public class KamehamehaMagic extends EliteAspectual {
    static final SkillText TEXT = SkillText.of("kamehameha", "Kamehameha",
            "Aspectual magic. Hold to gather ki (up to five levels), release to fire a piercing beam. A fuller charge is longer, wider and harder; letting go too early costs nothing.");

    private static final String LEVEL = "KameLevel";
    private static final int COLOR = 0xFF7FD6FF;

    public KamehamehaMagic() { super(AspectualType.MISC, TEXT); }

    @Override
    protected List<EliteMode> modes() { return List.of(EliteMode.of("kamehameha.fire", "Kamehameha", MagicRules.Kamehameha.cost(1), 100)); }

    @Override protected int color() { return COLOR; }

    @Override
    protected int cooldownAfter(ManasSkillInstance instance, ServerPlayer player, int mode, int heldTicks) {
        return MagicRules.Kamehameha.cooldownTicks(Math.max(1, MagicRules.Kamehameha.level(heldTicks, masteredBy(instance, player))));
    }

    /** Five levels of 20 ticks and a little over: the key is released by the game when the hold ends. */
    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return MagicRules.Kamehameha.ticksPerLevel(masteredBy(instance, entity)) * MagicRules.Kamehameha.MAX_LEVEL + 20;
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!super.onHeld(instance, entity, heldTicks, mode) || !(entity instanceof ServerPlayer player)) return false;
        boolean mastered = masteredBy(instance, player);
        int level = MagicRules.Kamehameha.level(heldTicks, mastered);
        var tag = instance.getOrCreateTag();
        int shown = tag.getInt(LEVEL);
        if (level == shown) return true;
        tag.putInt(LEVEL, level);
        ServerLevel sl = player.serverLevel();
        if (level >= 1) {
            if (shown == 0) {
                EliteBridge.follow(sl, VfxShape.MANA_CHARGE, player, player.position().add(0, 1, 0), COLOR,
                        MagicRules.Kamehameha.ticksPerLevel(mastered) * MagicRules.Kamehameha.MAX_LEVEL, 1.0f);
            }
            EliteBridge.actionbar(player, SkillMsg.t("kamehameha.level", "Kamehameha: level %s of %s", level, MagicRules.Kamehameha.MAX_LEVEL)
                    .withStyle(level >= MagicRules.Kamehameha.MAX_LEVEL ? ChatFormatting.AQUA : ChatFormatting.WHITE, ChatFormatting.BOLD));
            EliteBridge.sound(sl, player.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 0.6f + 0.15f * level);
            EliteBridge.aura(sl, player, false);
        }
        instance.markDirty();
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int slot, int mode) {
        if (entity instanceof ServerPlayer) instance.getOrCreateTag().putInt(LEVEL, 0);
        super.onRelease(instance, entity, heldTicks, slot, mode);
    }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, int mode, int heldTicks) {
        // super.onRelease already paid the level-1 price; work out the real level and settle the difference
        boolean mastered = masteredBy(instance, player);
        int level = MagicRules.Kamehameha.level(heldTicks, mastered);
        if (!MagicRules.Kamehameha.fires(level)) {
            EliteBridge.actionbar(player, SkillMsg.t("kamehameha.fizzle", "The ki scatters: you let go too early.").withStyle(ChatFormatting.GRAY));
            return false;                                                    // refunded in full: nothing was fired
        }
        double paid = EliteSkillsConfig.cost(MagicRules.Kamehameha.cost(1));
        // pay the rest of the level reached; fall back to the highest level that can be paid
        while (level > 1 && !EliteBridge.hasMagicules(player, EliteSkillsConfig.cost(MagicRules.Kamehameha.cost(level)) - paid)) level--;
        double rest = EliteSkillsConfig.cost(MagicRules.Kamehameha.cost(level)) - paid;
        if (rest > 0 && !EliteBridge.spendMagicules(player, rest)) return false;

        ServerLevel sl = player.serverLevel();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 start = player.getEyePosition().add(look.scale(0.6)).add(0, -0.2, 0);
        double range = MagicRules.Kamehameha.range(level);
        Vec3 end = EliteBridge.lookPoint(player, range + 0.6);
        double dmg = MagicRules.Kamehameha.damage(level, mastered) * EliteSkillsConfig.damageScale();
        int hits = 0;
        for (LivingEntity target : EliteBridge.foesAlong(player, start, end, MagicRules.Kamehameha.width(level))) {
            if (EliteBridge.hit(player, target, dmg, masteryFrac(instance), true) > 0f) hits++;
            Vec3 dir = target.position().subtract(player.position()).multiply(1, 0, 1);
            if (dir.lengthSqr() > 1.0e-4) {
                dir = dir.normalize();
                target.knockback(MagicRules.Kamehameha.knockback(level), -dir.x, -dir.z);
                target.hurtMarked = true;
            }
            EliteBridge.spray(sl, false, target.position().add(0, target.getBbHeight() * 0.6, 0), 6, 0.3, 0.3, 0.3, 0.05);
        }
        EliteBridge.beam(sl, start, end, false);
        EliteBridge.shape(sl, VfxShape.BORN_BEAM, start, end, COLOR, 12 + 2 * level, 0.6f + 0.25f * level);
        EliteBridge.shape(sl, VfxShape.LIGHT_FLARE, start, start.add(look), 0xFFFFFFFF, 10, 0.6f + 0.1f * level);
        if (MagicRules.Kamehameha.finale(level)) EliteBridge.shape(sl, VfxShape.BORN_NOVA, end, end, COLOR, 16, 1.3f);
        EliteBridge.sound(sl, player.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.7f - 0.1f * level);
        int recoil = MagicRules.Kamehameha.recoilTicks(level);
        if (recoil > 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, recoil, 2, true, false));
        EliteBridge.actionbar(player, SkillMsg.t("kamehameha.fired", "Kamehameha! Level %s struck %s.", level, hits).withStyle(ChatFormatting.AQUA));
        grow(instance, player, level);
        return true;
    }
}
