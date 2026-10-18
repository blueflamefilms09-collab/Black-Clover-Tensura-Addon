package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.ExtraRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Ancient Soul (Extra, designed for this mod; the original addon's source was not available): a soul that has lived before. PASSIVE: every foe you kill leaves a fragment of an old soul in you
 * (a boss leaves five, at most 100 are kept), and part of any magic that hits you (12%, 20% mastered) never lands. PRESS: Echo of Ages spends 20 fragments to restore 30% of your health
 * (45% mastered) and wash away every harmful effect. Fragments are kept in the skill's own data, so they survive death and relogging. Numbers: {@link ExtraRules.AncientSoul}.
 */
public class AncientSoulSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("ancient_soul", "Ancient Soul",
            "Extra. Kills leave fragments of old souls in you; part of any magic that hits you never lands. Press to spend 20 fragments and mend yourself.");

    static final String FRAGMENTS = "Fragments";

    public AncientSoulSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("ancient_soul.echo", "Echo of Ages", ExtraRules.AncientSoul.COST, ExtraRules.AncientSoul.COOLDOWN));
    }

    @Override protected int color() { return 0xFFD8C8FF; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        int have = instance.getOrCreateTag().getInt(FRAGMENTS);
        if (!ExtraRules.AncientSoul.canSpend(have)) {
            EliteBridge.fail(player, SkillMsg.t("ancient_soul.few", "Not enough soul fragments (%s of %s).", have, ExtraRules.AncientSoul.SPEND));
            return false;
        }
        boolean mastered = masteredBy(instance, player);
        instance.getOrCreateTag().putInt(FRAGMENTS, ExtraRules.AncientSoul.spend(have));
        BalanceLaw.heal(player, (float) (player.getMaxHealth() * ExtraRules.AncientSoul.healShare(mastered)));
        EliteBridge.cleanse(player);
        ServerLevel level = player.serverLevel();
        EliteBridge.aura(level, player, false);
        EliteBridge.burst(level, player.position().add(0, 1, 0), false, 14, 0.6);
        EliteBridge.shape(level, VfxShape.SOUL_VORTEX, player.position(), player.position().add(0, 1, 0), 0xFFD8C8FF, 24, 0.9f);
        EliteBridge.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
        EliteBridge.actionbar(player, SkillMsg.t("ancient_soul.echo", "The echo of old lives mends you (%s fragments left).",
                instance.getOrCreateTag().getInt(FRAGMENTS)).withStyle(ChatFormatting.LIGHT_PURPLE));
        instance.markDirty();
        return true;
    }

    /** Passive: part of any magic damage never lands. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!EliteSkillsConfig.enabled() || amount.get() <= 0 || !source.is(DamageTypeTags.WITCH_RESISTANT_TO)) return true;
        amount.set((float) (amount.get() * (1.0 - ExtraRules.AncientSoul.magicReduction(masteredBy(instance, owner)))));
        return true;
    }

    /** A kill by the owner: a fragment (five for a boss). Called by the elite kill handler. */
    static void onKill(ManasSkillInstance instance, ServerPlayer killer, LivingEntity victim) {
        var tag = instance.getOrCreateTag();
        int before = tag.getInt(FRAGMENTS);
        tag.putInt(FRAGMENTS, ExtraRules.AncientSoul.add(before, ExtraRules.AncientSoul.gainForKill(BalanceLaw.isBoss(victim))));
        if (tag.getInt(FRAGMENTS) != before) instance.markDirty();
    }
}
