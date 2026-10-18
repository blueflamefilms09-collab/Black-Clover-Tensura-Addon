package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.SkillMath;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the rebuilt skills share three things the old classes each did by hand or not at all: a per-skill icon at
 * {@code textures/skill/<id>.png}, a name and description that go through lang keys with an English fallback, and the tier (Unique,
 * mastered Unique, Ultimate) every rule is keyed on. One base class gives all of them, so no skill can ship with a vanilla placeholder
 * icon or an untranslatable name. The registry ids are NOT touched: this class only changes how a skill presents and measures itself.
 */
public abstract class NUSkill extends Skill {
    private final SkillText text;
    private final ResourceLocation icon;
    /** True for the Ultimate evolutions (Lord of Summons, Hero's Argonaut ...), whose numbers come from the ULTIMATE tier. */
    protected final boolean ultimate;

    protected NUSkill(SkillType type, SkillText text) { this(type, text, false); }

    protected NUSkill(SkillType type, SkillText text, boolean ultimate) {
        super(type);
        this.text = text;
        this.ultimate = ultimate;
        this.icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/" + text.id() + ".png");
    }

    public SkillText text() { return text; }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    @Override
    public MutableComponent getName() { return Component.translatableWithFallback(text.nameKey(), text.name()); }

    @Override
    public MutableComponent getSkillDescription() { return Component.translatableWithFallback(text.descriptionKey(), text.description()); }

    /** The tier the skill's numbers are read from for this wearer. */
    protected Tier tier(ManasSkillInstance instance, LivingEntity entity) {
        return Tier.of(instance.isMastered(entity), ultimate);
    }

    /** Mastery as a 0..1 fraction (BalanceLaw scales damage with it). */
    protected static double masteryFrac(ManasSkillInstance instance) {
        int max = instance.getMaxMastery();
        return max <= 0 ? 0.0 : SkillMath.clamp(instance.getMastery() / max, 0.0, 1.0);
    }

    /** Sets a cooldown given in ticks (ManasCore wants whole seconds). */
    protected static void cooldown(ManasSkillInstance instance, int mode, int ticks) {
        instance.setCoolDown(SkillMath.ticksToSeconds(ticks), mode);
    }

    /** Mastery points for a cast, then marks the instance for syncing. */
    protected static void grow(ManasSkillInstance instance, ServerPlayer player, double amount) {
        if (amount > 0) instance.addMasteryPoint(player, amount);
        instance.markDirty();
    }
}
