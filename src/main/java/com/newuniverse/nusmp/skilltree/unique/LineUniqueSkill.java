package com.newuniverse.nusmp.skilltree.unique;

import com.newuniverse.nusmp.skilltree.data.RouteEvolution;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.line.EvolutionConfig;
import com.newuniverse.nusmp.skilltree.line.EvolutionRules;
import com.newuniverse.nusmp.skilltree.line.EvolutionText;
import com.newuniverse.nusmp.skilltree.line.LineText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: this is what a Skill Book teaches since 1.96.0: a real Tensura skill of type UNIQUE, one per book, registered at start-up ({@link UniqueSkills}), so it sits in Tensura's own Ability Selection screen
 * next to Bewilder or Fighter and is slotted, scrolled and switched the way every Unique skill is, with no panel of ours. It does two things. ACTIVE: its modes are the line's arts in route-file order, the first one
 * is the signature art; a press of the skill key casts the art of the current mode through the line engine ({@code UniqueBooks.useArt}: the magicule price, the target check, the balance law, the cooldown), and a
 * later art awakens only when the skill is mastered far enough ({@link MasteryLevels}). PASSIVE: while the skill is switched ON (Tensura's own toggle; it is on when the skill is learned) the line's passives and stat
 * bonuses are applied by the engine's gate, at a level set by the skill's mastery. Mastery is Tensura's own: every art cast and every ten seconds the skill is on adds a point. The skill keeps no state of its own
 * beyond what ManasCore stores in the instance (mastery, cooldowns, the toggle); the numbers come from the route file ({@code data/nusmp/skill_trees}), which exists only on the server, so the client side of this class
 * reads only the shipped table of books ({@link LineIndex.Line}), the same words on both sides.
 */
public final class LineUniqueSkill extends Skill {
    private final LineIndex.Line line;
    private final ResourceLocation icon;

    public LineUniqueSkill(LineIndex.Line line) {
        super(SkillType.UNIQUE);
        this.line = line;
        this.icon = ResourceLocation.fromNamespaceAndPath(LineIndex.NAMESPACE, "textures/skill/line/" + line.bare() + ".png");
    }

    public LineIndex.Line line() { return line; }

    /** The bare id of the line ({@code lady_luck}). */
    public String bare() { return line.bare(); }

    /** The route id of the line ({@code nusmp:lady_luck}): the data the server reads the arts and passives from. */
    public String routeId() { return line.routeId(); }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    @Override public MutableComponent getName() { return Component.translatableWithFallback(SkillTexts.nameKey(line.bare()), line.name()); }

    @Override
    public MutableComponent getSkillDescription() {
        MutableComponent base = Component.translatableWithFallback(SkillTexts.descKey(line.bare()), SkillTexts.description(line));
        RouteEvolution ev = evolution();
        if (ev == null) return base;
        base.append(Component.literal("\n")).append(EvolutionText.tr(EvolutionText.REQUIREMENT, ev.text()).withStyle(ChatFormatting.GOLD));       // 3.26.0 (Evolution skills): what the skill asks and what it becomes
        if (!ev.ultimateName().isEmpty()) base.append(Component.literal("\n")).append(EvolutionText.tr(EvolutionText.EVOLVED_NAME, ev.ultimateName()).withStyle(ChatFormatting.YELLOW));
        return base;
    }

    /** 3.26.0 (Evolution skills): the line's evolution requirement (the server has the whole route, a client the card of it), or null for a line without one or when the feature is off. */
    private RouteEvolution evolution() {
        SkillRoute r = RouteRegistry.byId(line.routeId());
        return r != null && r.hasEvolution() && EvolutionConfig.enabled() ? r.evolution() : null;
    }

    /**
     * The mode name of the Ultimate art while its skill has an evolution: the requirement and its progress ("n / count", mirrored by the server into the instance's own tag, see {@code Evolution#mirror}),
     * then "needs mastery", then the evolved name once it can be cast. Null for every other mode.
     */
    private Component evolutionLabel(ManasSkillInstance instance, int m, String name) {
        RouteEvolution ev = evolution();
        CompoundTag tag = instance.getTag();
        if (ev == null || tag == null || !tag.contains(EvolutionRules.TAG_ULTIMATE) || tag.getInt(EvolutionRules.TAG_ULTIMATE) != m) return null;
        int progress = tag.getInt(EvolutionRules.TAG_PROGRESS);
        String evolved = ev.ultimateName().isEmpty() ? name : ev.ultimateName();
        return switch (EvolutionRules.card(progress, ev.count(), MasteryLevels.mastered(UniqueBooks.fraction(instance)))) {
            case PROGRESS -> EvolutionText.tr(EvolutionText.MODE_PROGRESS, name, EvolutionRules.progressText(progress, ev.count()));
            case NEEDS_MASTERY -> EvolutionText.tr(EvolutionText.MODE_NEEDS_MASTERY, evolved);
            case READY -> {
                int cd = instance.getCoolDown(m);
                yield Component.literal(cd > 0 ? evolved + " (" + cd + "s)" : evolved);
            }
        };
    }

    // ---------------------------------------------------------------- the arts are the modes
    @Override public int getModes(ManasSkillInstance instance) { return SkillTexts.modeNames(line).size(); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int n = SkillTexts.modeNames(line).size();
        return Math.floorMod(mode + (reverse ? -1 : 1), n);
    }

    @Override public String getModeId(ManasSkillInstance instance, int mode) { return "art_" + Math.floorMod(mode, SkillTexts.modeNames(line).size()); }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        var names = SkillTexts.modeNames(line);
        int m = Math.floorMod(mode, names.size());
        String name = names.get(m);
        Component evo = evolutionLabel(instance, m, name);
        if (evo != null) return evo;
        if (!line.arts().isEmpty() && !MasteryLevels.awake(m, names.size(), UniqueBooks.fraction(instance))) {
            return LineText.tr(LineText.MODE_LOCKED, name);
        }
        int cd = instance.getCoolDown(m);
        return Component.literal(cd > 0 ? name + " (" + cd + "s)" : name);
    }

    /** The price is paid by the line engine as a share of the maximum magicules (see {@code AbilityKit.pay}), so Tensura's own cost line stays at zero. */
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }

    // ---------------------------------------------------------------- the toggle is the passives' switch
    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) p.displayClientMessage(LineText.tr(LineText.SKILL_ON, getName()).withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) p.displayClientMessage(LineText.tr(LineText.SKILL_OFF, getName()).withStyle(ChatFormatting.GRAY), true);
    }

    /** A point of mastery every ten seconds the skill is on: a skill of pure passives still grows. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer p) || !instance.isToggled() || !p.isAlive()) return;
        if ((p.tickCount + p.getId()) % UniqueBooks.MASTERY_BEAT != 0) return;
        instance.addMasteryPoint(p, 1);
        instance.markDirty();
    }

    // ---------------------------------------------------------------- the active art
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity instanceof ServerPlayer p) UniqueBooks.useArt(p, instance, this, mode);
    }

    @Override public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) { return true; }

    @Override public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) { }
}
