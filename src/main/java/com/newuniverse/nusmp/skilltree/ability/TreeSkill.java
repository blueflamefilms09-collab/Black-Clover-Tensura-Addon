package com.newuniverse.nusmp.skilltree.ability;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the technique classes of the four Heavy Knight books (provoke, shield wall, power strike, ...) are written as small skills with one {@link #cast} method that reads its numbers from the route file at a
 * level, pays its share of the magicules, does one clear thing and returns whether it was used. Since 1.96.0 they are no longer learned one by one through a tree: the book's unique skill ({@code LineUniqueSkill}) is
 * the only skill a player owns, and its modes run these techniques through {@code castDirect}, with the level and the cooldown decided by the unique skill's mastery. So this base is now INERT as a skill of its own:
 * it cannot be used from Tensura's menu or keys (it can no longer be learned by anything, and a player who still holds one from an older world has it forgotten at login), and only keeps the
 * {@link #routeId} / {@link #nodeId} the technique belongs to and the {@link #cast} contract the techniques implement. The registry ids stay registered so an old save that names one still loads.
 */
public abstract class TreeSkill extends Skill {
    private final String routeId, nodeId;

    protected TreeSkill(String routeId, String nodeId) {
        super(SkillType.EXTRA);
        this.routeId = routeId;
        this.nodeId = nodeId;
    }

    /** The route this technique belongs to ({@code nusmp:oath_of_the_heavy_armor}). */
    public final String routeId() { return routeId; }

    /** The node of that route that names it. */
    public final String nodeId() { return nodeId; }

    @Override public boolean canInteractSkill(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canActivateSkill(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return false; }

    @Override public boolean canScroll(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }

    @Override public boolean canBeSlotted(ManasSkillInstance instance, LivingEntity entity, int slot) { return false; }

    /**
     * The technique. {@code nodeLevel} is the level it counts at (at least 1). Pay the cost (see {@link AbilityKit#pay}), do the effect through {@link AbilityKit}, return true when it was used (the cooldown
     * then starts) or false to refuse (say why with {@link AbilityKit#say}); nothing is spent when you return false before paying.
     */
    protected abstract boolean cast(ServerPlayer player, ManasSkillInstance instance, int nodeLevel, int heldTicks, int mode);

    /** The cooldown in ticks at a node level (at least 20). */
    protected abstract int cooldownTicks(int nodeLevel);
}
