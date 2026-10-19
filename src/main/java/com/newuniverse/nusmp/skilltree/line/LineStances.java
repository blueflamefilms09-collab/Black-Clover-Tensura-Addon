package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: a stance is the one kind of node that is neither a one-shot art nor a passive: the player takes it up, it changes how they fight until they drop it or take another, and the owner's rule is
 * "one at a time, upkeep-free". So the state is a single string in the player's own data (the key of the stance that is on), choosing a stance is a cast (cooldown, an optional small price), and its
 * numbers act in two ways: armour, speed and knockback resistance as TRANSIENT attribute modifiers (never saved, so a world saved mid-stance carries nothing to leak out of it), and the damage bonus,
 * the critical chance and the regeneration read by the damage events and the beat. The modifiers are kept in step with the gate by {@link #sync} every half second: a stance of a skill the player no longer owns is dropped on the first beat, so losing a skill cleans up after itself with no special case. One stance's modifiers use fixed ids, which
 * is safe exactly because only one stance can be on.
 */
public final class LineStances {
    private LineStances() {}

    /** The numbers of the stance that is on, already read at its level. */
    public record View(String key, SkillRoute route, SkillNode node, int level, double damagePct, double armor, double speedPct, double knockbackRes, double regen, double critChance) {}

    private static final ResourceLocation ARMOR_ID = id("stance_armor"), SPEED_ID = id("stance_speed"), KB_ID = id("stance_knockback");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", "line/" + path); }

    /** The ids of the modifiers a stance uses (the tests and the cleanup know them). */
    public static java.util.List<ResourceLocation> modifierIds() { return java.util.List.of(ARMOR_ID, SPEED_ID, KB_ID); }

    /** The stance that is on and allowed to act right now (the player owns its unique skill), or null. */
    public static View current(ServerPlayer p) {
        String key = LineState.stance(p);
        if (key.isEmpty()) return null;
        int hash = key.indexOf('#');
        if (hash <= 0) return null;
        SkillRoute route = RouteRegistry.byId(key.substring(0, hash));
        SkillNode node = route == null ? null : route.node(key.substring(hash + 1));
        if (node == null || !"stance".equals(node.kind())) return null;
        UniqueBooks.Owned owned = UniqueBooks.ownedOf(p, route.id());
        if (owned == null) return null;
        int lv = LineGate.activeLevel(node, owned.fraction());
        if (lv < 1) return null;
        return new View(key, route, node, lv, node.value("damage_pct", lv, 0), node.value("armor", lv, 0), node.value("speed_pct", lv, 0), node.value("knockback_res", lv, 0),
                node.value("regen", lv, 0), node.value("crit_chance", lv, 0));
    }

    /**
     * Takes up the stance of this cast, or drops it when it is the one already on. Returns true when the stance is ON afterwards. Only one stance is ever on: taking another replaces the first.
     */
    public static boolean toggle(ActiveCtx c) {
        String key = SkillRoute.key(c.route.id(), c.node.id());
        boolean wasOn = key.equals(LineState.stance(c.p));
        LineState.setStance(c.p, wasOn ? "" : key);
        sync(c.p);
        return !wasOn;
    }

    /** Brings the transient modifiers in step with the stance that is on (and forgets a stance that is no longer allowed). */
    public static void sync(ServerPlayer p) {
        View v = current(p);
        if (v == null && !LineState.stance(p).isEmpty()) LineState.setStance(p, "");
        apply(p, Attributes.ARMOR, ARMOR_ID, v == null ? 0 : v.armor(), AttributeModifier.Operation.ADD_VALUE);
        apply(p, Attributes.MOVEMENT_SPEED, SPEED_ID, v == null ? 0 : v.speedPct(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(p, Attributes.KNOCKBACK_RESISTANCE, KB_ID, v == null ? 0 : v.knockbackRes(), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        if (Math.abs(amount) < 1e-9) {
            if (inst.hasModifier(id)) inst.removeModifier(id);
            return;
        }
        AttributeModifier have = inst.getModifier(id);
        if (have == null || have.amount() != amount || have.operation() != op) inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** Removes every modifier of a stance (the player left, or the engine is shutting down). */
    public static void clear(ServerPlayer p) {
        apply(p, Attributes.ARMOR, ARMOR_ID, 0, AttributeModifier.Operation.ADD_VALUE);
        apply(p, Attributes.MOVEMENT_SPEED, SPEED_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(p, Attributes.KNOCKBACK_RESISTANCE, KB_ID, 0, AttributeModifier.Operation.ADD_VALUE);
    }
}
