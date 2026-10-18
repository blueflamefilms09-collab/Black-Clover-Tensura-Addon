package com.newuniverse.nusmp.skilltree.menu;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: this is the "top-level Unique Skill" a Skill Line is in Tensura's Ability Selection menu. Tensura builds that menu's left list from the ManasCore skills a player has learned, filtered by
 * {@code Skill.SkillType}, so the only way to put a line in that list (next to "Bewilder" or "Fighter") is to be a real Tensura {@link Skill} of type {@code UNIQUE} that the player has learned. The
 * stand-in is exactly that and NOTHING MORE: it carries the line's name, description and icon for Tensura's own list and detail view, and it does nothing else. It cannot be used ({@link #canInteractSkill}
 * and {@link #canActivateSkill} say no), cannot be toggled, ticked, scrolled or put in one of the three skill slots ({@link #canBeSlotted}), so pressing a Tensura skill key can never run a line's art a
 * second time: the arts stay where they have always been (the Arts bar and the line engine's cast service, gated by {@code LineGate}), and the stand-in is only the line's handle in Tensura's list. Its
 * state lives in the player's skill-tree record, never in the instance (no mastery, no cooldown, no tag), so a stand-in that is copied, lost or re-taught cannot change what a line does. The name comes
 * through a lang key with the line's own name as the fallback (the same shape as every rebuilt skill), and the description is the route's own text when the route is known on this side (the owned lines
 * on a client, every line on a server) and a general sentence when it is not (a line the client has only a card for), so the fog of war never leaks a description.
 */
public final class LineStandInSkill extends Skill {
    private final LineIndex.Line line;
    private final ResourceLocation icon;

    public LineStandInSkill(LineIndex.Line line) {
        super(SkillType.UNIQUE);
        this.line = line;
        this.icon = ResourceLocation.fromNamespaceAndPath(LineIndex.NAMESPACE, "textures/skill/line/" + line.bare() + ".png");
    }

    public LineIndex.Line line() { return line; }

    /** The route id of the line this skill stands for ({@code nusmp:fire_dragon_slayer}). */
    public String routeId() { return line.routeId(); }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    @Override public MutableComponent getName() { return Component.translatableWithFallback(MenuText.standInNameKey(line.bare()), line.name()); }

    @Override
    public MutableComponent getSkillDescription() {
        SkillRoute r = RouteRegistry.byId(line.routeId());
        if (r != null && !r.desc().isEmpty()) return Component.literal(r.desc());
        return MenuText.tr(MenuText.STANDIN_DESC);
    }

    // ---------------------------------------------------------------- harmless by construction
    @Override public boolean canInteractSkill(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canActivateSkill(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canScroll(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }

    @Override public boolean canBeSlotted(ManasSkillInstance instance, LivingEntity entity, int slot) { return false; }
}
