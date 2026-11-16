package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.SkillMath;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;

/**
 * Why: the rebuilt skills share three things the old classes each did by hand or not at all: a per-skill icon at
 * {@code textures/skill/<id>.png}, a name and description that go through lang keys with an English fallback, and the tier (Unique,
 * mastered Unique, Ultimate) every rule is keyed on. One base class gives all of them, so no skill can ship with a vanilla placeholder
 * icon or an untranslatable name. The registry ids are NOT touched: this class only changes how a skill presents and measures itself.
 * Ultimate classes (docs/ultimate_classes.md): a classified Ultimate names its {@link UltimateClass}, which sets its mastery, its tier once
 * mastered, its price ({@link #pay}: Tensura's getMagiculeCost + EnergyHelper.isOutOfEnergy, scaled with max magicule) and its milestones.
 */
public abstract class NUSkill extends Skill {
    /** Tag keys on the skill instance: the max mastery its mastery was earned against, and the last milestone announced. */
    static final String TAG_MASTERY_MAX = "NuMasteryMax", TAG_MILESTONE = "NuMilestone";
    /** The price of the cast being paid right now, read back by {@link #getMagiculeCost} inside the one isOutOfEnergy call (server thread; cleared in finally, never holds a player). */
    private static final ThreadLocal<Double> PAYING = new ThreadLocal<>();

    private final SkillText text;
    private final ResourceLocation icon;
    /** True for the Ultimate evolutions (Lord of Summons, Hero's Argonaut ...), whose numbers come from the ULTIMATE tier. The old path: kept for anything without a class. */
    protected final boolean ultimate;

    protected NUSkill(SkillType type, SkillText text) { this(type, text, false); }

    protected NUSkill(SkillType type, SkillText text, boolean ultimate) {
        super(type);
        this.text = text;
        this.ultimate = ultimate;
        this.icon = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/skill/" + text.id() + ".png");
    }

    public SkillText text() { return text; }

    /** The Ultimate class of this skill, or null for an unclassified skill (every Unique and Extra, and any Ultimate not yet classified: it keeps the old boolean path). */
    public UltimateClass ultimateClass() { return null; }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    @Override
    public MutableComponent getName() { return Component.translatableWithFallback(text.nameKey(), text.name()); }

    @Override
    public MutableComponent getSkillDescription() { return Component.translatableWithFallback(text.descriptionKey(), text.description()); }

    /** A classified Ultimate takes its class's mastery (SERVER setting ultimateClasses.classMastery); everything else keeps Tensura's. */
    @Override
    public int getMaxMastery() {
        UltimateClass c = ultimateClass();
        return c != null && NUConfig.ucClassMastery() ? NUConfig.ucMaxMastery(c) : super.getMaxMastery();
    }

    /** Tensura's own max mastery for this skill type (what mastery was earned against before the classes). */
    final int tensuraMaxMastery() { return super.getMaxMastery(); }

    /** The tier the skill's numbers are read from for this wearer. A mastered classified Ultimate is ULTIMATE_MASTERED (setting masteredTier). */
    protected Tier tier(ManasSkillInstance instance, LivingEntity entity) {
        boolean mastered = entity != null && instance.isMastered(entity);
        UltimateClass c = ultimateClass();
        return Tier.of(mastered, ultimate || c != null, c != null && NUConfig.ucMasteredTier());
    }

    /** Mastery as a 0..1 fraction (BalanceLaw scales damage with it). */
    protected static double masteryFrac(ManasSkillInstance instance) {
        int max = instance.getMaxMastery();
        return max <= 0 ? 0.0 : SkillMath.clamp(instance.getMastery() / max, 0.0, 1.0);
    }

    /** Sets a cooldown given in ticks (ManasCore wants whole seconds); x0.75 from the 75 % milestone of a classified Ultimate. */
    protected static void cooldown(ManasSkillInstance instance, int mode, int ticks) {
        instance.setCoolDown(SkillMath.ticksToSeconds((int) Math.round(ticks * UltimateClass.milestoneCooldown(milestone(instance)))), mode);
    }

    /** Mastery points for a cast, then marks the instance for syncing. */
    protected static void grow(ManasSkillInstance instance, ServerPlayer player, double amount) {
        if (amount > 0) instance.addMasteryPoint(player, amount);
        instance.markDirty();
    }

    // ------------------------------------------------------------------ milestones (master guide §5.2)
    private static UltimateClass classOf(ManasSkillInstance instance) {
        return instance != null && instance.getSkill() instanceof NUSkill s ? s.ultimateClass() : null;
    }

    /** 0..4 (25 / 50 / 75 / 100 %) for a classified Ultimate with milestones on; 0 for everything else. */
    protected static int milestone(ManasSkillInstance instance) {
        if (classOf(instance) == null || !NUConfig.ucMilestones()) return 0;
        return UltimateClass.milestone(masteryFrac(instance));
    }

    /** Range / radius factor: x1.2 from the 25 % milestone. A skill multiplies its own range by it. */
    protected static double milestoneRange(ManasSkillInstance instance) { return UltimateClass.milestoneRange(milestone(instance)); }

    /** True from the 50 % milestone: the skill may add the second effect of its main mode. */
    protected static boolean milestoneSecondEffect(ManasSkillInstance instance) { return UltimateClass.milestoneSecondEffect(milestone(instance)); }

    /** Tells the player once about each milestone reached (the highest one announced is kept in the instance tag). */
    static void announceMilestone(ManasSkillInstance instance, ServerPlayer player) {
        int m = milestone(instance);
        if (m <= 0) return;
        CompoundTag tag = instance.getOrCreateTag();
        if (tag.getInt(TAG_MILESTONE) >= m) return;
        tag.putInt(TAG_MILESTONE, m);
        instance.markDirty();
        Component name = instance.getSkill() instanceof NUSkill s ? s.getName() : Component.empty();
        Component msg = switch (m) {
            case 1 -> SkillMsg.t("milestone.1", "%s is a quarter mastered: its reach grows.", name);
            case 2 -> SkillMsg.t("milestone.2", "%s is half mastered: its main power gains a second effect.", name);
            case 3 -> SkillMsg.t("milestone.3", "%s is three quarters mastered: it costs less and recharges faster.", name);
            default -> SkillMsg.t("milestone.4", "%s is mastered: its mastered form awakens.", name);
        };
        player.displayClientMessage(msg.copy().withStyle(ChatFormatting.GOLD), false);
    }

    // ------------------------------------------------------------------ cost (master guide P1 #1)
    /**
     * The price of a cast whose old flat price is {@code flat}, for this caster: the class's costPercent / costReference (unclassified skills: plainCostPercent / plainCostReference), never below
     * the flat price, x0.8 from the 75 % milestone. The flat price already carries the server's cost multiplier where the call site applied it (DMUtil.cost), so it is not applied again.
     */
    protected static double price(ManasSkillInstance instance, ServerPlayer player, double flat) {
        if (!(flat > 0)) return 0.0;
        if (NUConfig.ucFlatCosts()) return flat;
        return BalanceLaw.classCost(player, classOf(instance), flat) * UltimateClass.milestoneCost(milestone(instance));
    }

    /** Pays a cast whose old flat price is {@code flat} once, through Tensura (see {@link #charge}); call it after every check, right before the effect. */
    protected static boolean pay(ManasSkillInstance instance, ServerPlayer player, int mode, double flat) {
        return charge(instance, player, mode, price(instance, player, flat));
    }

    /**
     * Pays an already worked out {@link #price} once: {@code getMagiculeCost} answers it and {@code EnergyHelper.isOutOfEnergy(player, instance, mode)} checks and takes it (creative pays nothing,
     * Tensura's own "not enough magicule" message). With the SERVER setting ultimateClasses.flatCosts on: the old direct write. False = not paid, nothing taken.
     */
    protected static boolean charge(ManasSkillInstance instance, ServerPlayer player, int mode, double price) {
        if (!(price > 0)) return true;
        if (NUConfig.ucFlatCosts() || instance == null || !(instance.getSkill() instanceof NUSkill)) return SkillUtil.spendMagicules(player, price);
        PAYING.set(price);
        boolean paid;
        try {
            paid = !EnergyHelper.isOutOfEnergy(player, instance, mode);
        } finally {
            PAYING.remove();
        }
        if (paid) announceMilestone(instance, player);
        return paid;
    }

    /** The cast being paid; 0 otherwise (the costs live in each mode's rules, so Tensura's UI shows none, as before). */
    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        Double p = PAYING.get();
        return p != null ? p : 0.0;
    }

    // ------------------------------------------------------------------ class mastery: keep earned mastery when the max changes
    /** A newly learned classified skill: its mastery is measured against today's max from the start. */
    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        if (entity == null || entity.level().isClientSide() || ultimateClass() == null) return;
        CompoundTag tag = instance.getOrCreateTag();
        if (!tag.contains(TAG_MASTERY_MAX)) {
            tag.putInt(TAG_MASTERY_MAX, getMaxMastery());
            instance.markDirty();
        }
    }

    /**
     * Moves the mastery of a classified skill to the same fraction of today's max when the max changed since it was earned (an old save: Tensura's 10,000; or a config change), so mastered
     * stays mastered. True when it changed anything.
     */
    static boolean restamp(ManasSkillInstance instance) {
        if (!(instance.getSkill() instanceof NUSkill s) || s.ultimateClass() == null) return false;
        int now = s.getMaxMastery();
        CompoundTag t = instance.getTag();
        boolean stamped = t != null && t.contains(TAG_MASTERY_MAX);
        int was = stamped ? t.getInt(TAG_MASTERY_MAX) : s.tensuraMaxMastery();
        if (stamped && was == now) return false;
        double m = instance.getMastery();
        double moved = UltimateClass.rescaleMastery(m, was, now);
        if (moved != m) instance.setMastery(moved);
        instance.getOrCreateTag().putInt(TAG_MASTERY_MAX, now);
        instance.markDirty();
        return true;
    }

    /** Login, once per player: one pass over the player's own learned skills (no tick work, nothing sent unless an instance changed). */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return;
            for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) restamp(i);
        } catch (RuntimeException ignored) { /* a skill storage that cannot be read now is read again at the next login */ }
    }
}
