package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.ExtraRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Meditation (Extra, designed for this mod; the original addon's source was not available): TOGGLE. While it is on and you keep still and unhurt, Focus builds (one level per six seconds, five
 * at most); each level returns a growing share of your maximum magicules every second, and from Focus 3 also a little health. Moving, or being hurt, empties the Focus and you start
 * again (a hit also holds it down for five seconds). It costs nothing to hold, which is the point: it is how a caster refills between fights. Numbers: {@link ExtraRules.Meditation} and the
 * {@code meditationRegenScale} config dial. The upkeep runs from the elite player tick, because ManasCore only ticks passive skills now and then.
 */
public class MeditationSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("meditation", "Meditation",
            "Extra. Toggle. Keep still and unhurt and Focus builds: magicules (and later health) return every second. Moving or being hurt breaks it.");

    private static final String STILL = "Still", LAST_HURT = "LastHurt", MX = "MX", MY = "MY", MZ = "MZ", HELD = "FocusSeconds";

    public MeditationSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    protected List<EliteMode> modes() { return List.of(EliteMode.of("meditation.focus", "Focus", 0.0, 0)); }

    @Override protected boolean instant(int mode) { return false; }

    @Override protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) { return false; }

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        tag.putInt(STILL, 0);
        tag.putDouble(MX, player.getX());
        tag.putDouble(MY, player.getY());
        tag.putDouble(MZ, player.getZ());
        EliteBridge.actionbar(player, SkillMsg.t("meditation.on", "You settle into stillness.").withStyle(ChatFormatting.AQUA));
        instance.markDirty();
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) EliteBridge.actionbar(player, SkillMsg.t("meditation.off", "You end your meditation.").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (instance.isToggled() && amount.get() > 0) {
            var tag = instance.getOrCreateTag();
            tag.putLong(LAST_HURT, owner.level().getGameTime());
            tag.putInt(STILL, 0);
            instance.markDirty();
        }
        return true;
    }

    @Override
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {
        if (!instance.isToggled() || !EliteSkillsConfig.enabled()) return;
        var tag = instance.getOrCreateTag();
        double dx = player.getX() - tag.getDouble(MX), dy = player.getY() - tag.getDouble(MY), dz = player.getZ() - tag.getDouble(MZ);
        tag.putDouble(MX, player.getX());
        tag.putDouble(MY, player.getY());
        tag.putDouble(MZ, player.getZ());
        long now = player.level().getGameTime();
        boolean steady = ExtraRules.Meditation.still(dx * dx + dy * dy + dz * dz) && !ExtraRules.Meditation.inCombat(now, tag.getLong(LAST_HURT));
        int before = ExtraRules.Meditation.focus(tag.getInt(STILL));
        int still = steady ? tag.getInt(STILL) + 1 : 0;
        tag.putInt(STILL, still);
        int focus = ExtraRules.Meditation.focus(still);
        if (focus != before) {
            EliteBridge.actionbar(player, SkillMsg.t("meditation.focus", "Focus %s of %s", focus, ExtraRules.Meditation.MAX_FOCUS).withStyle(focus > before ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        if (focus > 0) {
            double max = EliteUtil.maxMagicule(player);
            EliteUtil.addMagicule(player, max * ExtraRules.Meditation.regenShare(focus, masteredBy(instance, player)) * EliteSkillsConfig.get(EliteSkillsConfig.MEDITATION_REGEN_SCALE));
            double heal = ExtraRules.Meditation.healPerSecond(focus);
            if (heal > 0) BalanceLaw.heal(player, (float) heal);
            if (focus >= 2) EliteBridge.aura(player.serverLevel(), player, false);
            int held = tag.getInt(HELD) + 1;
            tag.putInt(HELD, held);
            if (focus >= 3 && held % 10 == 0) instance.addMasteryPoint(player);
        }
        instance.markDirty();
    }
}
