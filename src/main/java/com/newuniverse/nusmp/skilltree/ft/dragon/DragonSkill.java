package com.newuniverse.nusmp.skilltree.ft.dragon;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ability.TreeSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: every active node of the two Dragon Slayer routes is a real ManasCore skill (a {@link TreeSkill}), so it shows up in Tensura's skill menu, takes Tensura's skill keys and
 * cooldown display, and is gated by the one hook the engine owns ({@code SkillTreeApi.canCast}: route unlocked, node bought, class active). This base adds what is common to all
 * fifteen techniques and nothing the engine already does: the icon (a 32 px texture of this mod under {@code textures/skill/ft/}), the lookup of a tuning number (the
 * registry's value for the node's current level, i.e. what the datapack says now, falling back to the shipped table of {@link DragonTables} when the route or the key is
 * missing, so a cast can never read a hole), the cost (a share of the maximum magicules through {@link AbilityKit#pay}) and the cooldown (the node's {@code cooldown} table).
 * It also stands guard: a technique that throws is caught, logged, answered with "fizzled" and counted, never allowed to escape into the key handler of a server full of
 * players, and the in-game self-test reads that count so a swallowed error is still a failed test. Server thread only, no state except that bounded failure list.
 */
public abstract class DragonSkill extends TreeSkill {
    private static final Logger LOG = LogUtils.getLogger();
    private static final List<String> FAILURES = new ArrayList<>();

    private final ResourceLocation icon;
    private final DragonTables tables;

    /**
     * @param routeId the route ({@code nusmp:fire_dragon_slayer})
     * @param nodeId  the node of that route that grants this skill
     * @param icon    the file name (without folder or extension) under {@code textures/skill/ft/}
     * @param tables  the shipped numbers of the route
     */
    protected DragonSkill(String routeId, String nodeId, String icon, DragonTables tables) {
        super(routeId, nodeId);
        this.icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/ft/" + icon + ".png");
        this.tables = tables;
    }

    @Override
    public ResourceLocation getSkillIcon() { return icon; }

    /** The number {@code key} at {@code level}: the registry node's value, else the shipped default. */
    protected final double num(String key, int level) { return DragonKit.value(tables, nodeId(), key, level); }

    /** {@link #num} rounded to a whole number (ticks, targets, counts). */
    protected final int inum(String key, int level) { return (int) Math.round(num(key, level)); }

    /** Pays the node's {@code cost_pct} (percent of the maximum magicules); false and nothing spent when the caster cannot. */
    protected final boolean pay(ServerPlayer p, int level) { return AbilityKit.pay(p, num("cost_pct", level) / 100.0); }

    @Override
    protected final int cooldownTicks(int nodeLevel) { return Math.max(20, inum("cooldown", nodeLevel)); }

    @Override
    protected final boolean cast(ServerPlayer player, ManasSkillInstance instance, int nodeLevel, int heldTicks, int mode) {
        try {
            return perform(player, instance, nodeLevel);
        } catch (Throwable t) {
            synchronized (FAILURES) {
                if (FAILURES.size() < 16) FAILURES.add(nodeId() + ": " + t);
            }
            LOG.warn("[nusmp] {} failed: {}", getRegistryName(), t.toString());
            AbilityKit.say(player, "The technique fizzles.");
            return false;
        }
    }

    /**
     * The technique itself. {@code level} is the node level the player bought (at least 1). Check what can be refused first (a target, room to move), then pay
     * ({@link #pay}), then act; return false only before paying. Delayed parts go through {@link DragonTimers}.
     */
    protected abstract boolean perform(ServerPlayer player, ManasSkillInstance instance, int level);

    /** The failures caught since the last call (for the self-test), oldest first; empties the list. */
    public static List<String> takeFailures() {
        synchronized (FAILURES) {
            List<String> out = new ArrayList<>(FAILURES);
            FAILURES.clear();
            return out;
        }
    }

    static void clearFailures() {
        synchronized (FAILURES) { FAILURES.clear(); }
    }
}
