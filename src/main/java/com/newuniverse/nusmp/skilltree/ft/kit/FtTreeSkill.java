package com.newuniverse.nusmp.skilltree.ft.kit;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the fifteen techniques of the Sky Dragon Slayer and Ice-Make routes would each repeat the same four things (read my numbers from the route file at my node level, pay my share
 * of the magicules, take my cooldown from the table, show my icon), and the day one of those four changes they would drift apart. This base class does them once on top of
 * {@link TreeSkill}, which already owns the one gate of the whole system (the route must be unlocked and the node bought) and the bookkeeping after a cast; a technique here
 * therefore writes only {@code cast}, and the numbers come from {@link RouteRegistry#value} (which answers the fallback when the route, the node or the table is unknown, so it never throws in the middle of a cast) with a per-route fallback (the shipped level-1 values). The
 * cooldown is final here and always the {@code cooldown} table, because the balance law (at least 3 s, at least 45 s for a Secret Art) is checked on that table by {@link FtBudget}, and a
 * technique that computed its own cooldown would escape the check. The icon is a vanilla texture, like the older skills of the mod (a skill icon is just a 16 by 16 picture in
 * Tensura's skill menu), so no new art has to ship. {@link #castDirect} is the single seam the in-game self-tests use to run the technique without the key-release bookkeeping
 * (cooldown, mastery point), because those need a stored skill instance that a stand-in player does not have; the gate itself is tested through the real {@code onRelease}.
 */
public abstract class FtTreeSkill extends TreeSkill {
    /** The shipped level-1 value of a table (what a technique falls back to when the registry has no such table). */
    @FunctionalInterface
    public interface Defaults {
        double of(String nodeId, String key);
    }

    private final ResourceLocation icon;
    private final Defaults defaults;

    /**
     * @param texture the vanilla texture path under {@code textures/}, for example {@code item/goat_horn} or {@code block/blue_ice}
     */
    protected FtTreeSkill(String routeId, String nodeId, String texture, Defaults defaults) {
        super(routeId, nodeId);
        this.icon = ResourceLocation.withDefaultNamespace("textures/" + texture + ".png");
        this.defaults = defaults;
    }

    @Override
    public ResourceLocation getSkillIcon() { return icon; }

    /** The node's named number at a level. */
    protected final double num(String key, int level) { return RouteRegistry.value(routeId(), nodeId(), key, level, defaults.of(nodeId(), key)); }

    /** The node's named number rounded to a whole number (ticks, counts). */
    protected final int whole(String key, int level) { return (int) Math.round(num(key, level)); }

    @Override
    protected final int cooldownTicks(int nodeLevel) { return Math.max(20, whole("cooldown", nodeLevel)); }

    /** Spends this node's {@code cost} share of the maximum magicules; false (and the player told) when they cannot pay. */
    protected final boolean pay(ServerPlayer player, int level) { return AbilityKit.pay(player, num("cost", level)); }

    /** Runs the technique at a level without the gate, the cooldown or the mastery point (self-tests only; real casts go through {@code onRelease}). */
    public final boolean castDirect(ServerPlayer player, ManasSkillInstance instance, int level) { return cast(player, instance, level, 0, 0); }
}
