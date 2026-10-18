package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.ExtraRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Iron Resolve (Extra, designed for this mod; the original addon's source was not available): the closer to the edge, the harder to move. PASSIVE: every blow you take while below half
 * health stacks Resolve (five stacks, seven mastered, for eight seconds); each stack turns aside 5% of the damage (45% at most), and from three stacks you can no longer be knocked back.
 * PRESS: Brace for three seconds: half of all damage ignored and no knockback at all. Numbers: {@link ExtraRules.IronResolve}.
 */
public class IronResolveSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("iron_resolve", "Iron Resolve",
            "Extra. Wounded, every blow you take stacks Resolve: less damage and, from three stacks, no knockback. Press to Brace and halve all damage for three seconds.");

    static final String STACKS = "Stacks", LAST_HIT = "LastHit", BRACE_UNTIL = "BraceUntil";

    public IronResolveSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("iron_resolve.brace", "Brace", ExtraRules.IronResolve.BRACE_COST, ExtraRules.IronResolve.BRACE_COOLDOWN));
    }

    @Override protected int color() { return 0xFFB8C0D0; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        instance.getOrCreateTag().putLong(BRACE_UNTIL, player.level().getGameTime() + ExtraRules.IronResolve.BRACE_TICKS);
        instance.markDirty();
        EliteBridge.shape(player.serverLevel(), VfxShape.IRON_FX1, player.position(), player.position().add(0, 1, 0), 0xFFB8C0D0, 30, 0.9f);
        EliteBridge.sound(player.serverLevel(), player.position(), SoundEvents.ANVIL_LAND, 0.7f, 1.4f);
        EliteBridge.actionbar(player, SkillMsg.t("iron_resolve.brace", "You brace.").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        return true;
    }

    static boolean braced(ManasSkillInstance instance, LivingEntity e) { return e.level().getGameTime() < instance.getOrCreateTag().getLong(BRACE_UNTIL); }

    static int stacks(ManasSkillInstance instance, LivingEntity e) {
        var tag = instance.getOrCreateTag();
        return ExtraRules.IronResolve.expired(e.level().getGameTime(), tag.getLong(LAST_HIT)) ? 0 : tag.getInt(STACKS);
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!EliteSkillsConfig.enabled() || amount.get() <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        var tag = instance.getOrCreateTag();
        long now = owner.level().getGameTime();
        int stacks = stacks(instance, owner);
        double share = owner.getHealth() / Math.max(1.0f, owner.getMaxHealth());
        int next = ExtraRules.IronResolve.stackAfterHit(stacks, share, masteredBy(instance, owner));
        if (next != stacks || share <= ExtraRules.IronResolve.WOUNDED_BELOW) {
            tag.putInt(STACKS, next);
            tag.putLong(LAST_HIT, now);
            instance.markDirty();
        }
        double cut = ExtraRules.IronResolve.reduction(next, braced(instance, owner));
        if (cut > 0) amount.set((float) (amount.get() * (1.0 - cut)));
        return true;
    }

    /** Once a second: stacks that have run out are dropped. */
    @Override
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {
        var tag = instance.getOrCreateTag();
        if (tag.getInt(STACKS) > 0 && ExtraRules.IronResolve.expired(player.level().getGameTime(), tag.getLong(LAST_HIT))) {
            tag.putInt(STACKS, 0);
            instance.markDirty();
        }
    }

    /** True if the owner of this instance cannot be knocked back right now (the elite knock-back handler asks). */
    static boolean knockbackImmune(ManasSkillInstance instance, LivingEntity owner) {
        return ExtraRules.IronResolve.knockbackImmune(stacks(instance, owner), braced(instance, owner));
    }
}
