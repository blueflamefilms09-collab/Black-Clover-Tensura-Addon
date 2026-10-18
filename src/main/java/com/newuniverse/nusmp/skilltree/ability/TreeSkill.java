package com.newuniverse.nusmp.skilltree.ability;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Why: this is the ABILITY HOOK of the owner's rule: "a player can only cast Fire Dragon's Roar if its route is unlocked AND its node is active in their capability map". The mod's spells
 * are cast through Tensura's skill system (ManasCore skills, bound to Tensura's own skill keys), so a Fairy Tail ability is a real ManasCore skill, and this base class is the single place
 * where the gate lives: {@link #canInteractSkill} (what the skill menu and the key handler ask first) and {@link #onRelease} (the cast itself) both ask
 * {@link SkillTreeApi#canCast}, so a skill that was learned but whose node was refunded, or that a command granted by hand, still cannot be cast. The node's level reaches the technique as
 * a parameter, so the numbers scale with the SP the player invested. Subclasses only write {@link #cast}; the cooldown, the mastery point and the refusal messages are done here for all of them.
 * A tree skill never touches the Black Clover grimoire: the grimoire pages are other skills in other registries with their own gate.
 */
public abstract class TreeSkill extends Skill {
    private final String routeId, nodeId;

    protected TreeSkill(String routeId, String nodeId) {
        super(SkillType.EXTRA);
        this.routeId = routeId;
        this.nodeId = nodeId;
    }

    /** The route this skill belongs to ({@code nusmp:fire_dragon_slayer}). */
    public final String routeId() { return routeId; }

    /** The node of that route that grants it. */
    public final String nodeId() { return nodeId; }

    @Override
    public boolean canInteractSkill(ManasSkillInstance instance, LivingEntity entity) {
        if (!super.canInteractSkill(instance, entity)) return false;
        return !(entity instanceof Player p) || SkillTreeApi.canCast(p, routeId, nodeId);
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!SkillTreeApi.canCast(player, routeId, nodeId)) {
            fail(player, "This technique is locked. Unlock its route and buy its node in the Skill Tree.");
            return;
        }
        if (instance.onCoolDown(mode)) {
            fail(player, "On cooldown.");
            return;
        }
        int level = Math.max(1, SkillTreeApi.nodeLevel(player, routeId, nodeId));
        if (!cast(player, instance, level, heldTicks, mode)) return;
        instance.setCoolDown(NUSMP.ticksToSeconds(cooldownTicks(level)), mode);
        instance.addMasteryPoint(player, 1);
        instance.markDirty();
    }

    /**
     * The technique. {@code nodeLevel} is the level the player bought (at least 1). Pay the cost (see {@link AbilityKit#pay}), do the effect through {@link AbilityKit}, return true when it was used
     * (the cooldown then starts) or false to refuse (say why with {@link AbilityKit#say}); nothing is spent when you return false before paying.
     */
    protected abstract boolean cast(ServerPlayer player, ManasSkillInstance instance, int nodeLevel, int heldTicks, int mode);

    /** The cooldown in ticks at a node level (at least 20); the dispatcher converts it to seconds. */
    protected abstract int cooldownTicks(int nodeLevel);

    private static void fail(ServerPlayer player, String text) { player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true); }
}
