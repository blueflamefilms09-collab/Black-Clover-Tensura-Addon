package com.newuniverse.nusmp.skilltree.line;

import net.minecraft.core.Holder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the {@code attr} kind is the "stat" node of a line: a permanent bonus to one attribute (maximum health, armour, speed, reach, ...) while the line is equipped and the switch is on. The owner's
 * rule is that these are TRANSIENT attribute modifiers: never written to the save (a world saved with a line equipped carries no modifier to leak into a world where the line is not), re-applied on the
 * half-second beat, and removed on the first beat after the line is unequipped, the switch is turned off or the node is lost. So the whole class is one reconcile function: it asks the gate
 * ({@link LinePassives#active}) for the attribute nodes that count right now, works out the modifier each should have (its own id, so two stat nodes on the same attribute stack), applies the missing
 * or changed ones, and removes every modifier it applied before that is no longer wanted. What it applied last time is remembered as a list of {@code attribute|id} words in the player's own data, which
 * is how a modifier of a node that no longer exists (a datapack removed it) is still found and removed. The numbers: {@code amount} at the node's level, and the operation {@code add} (a flat bonus)
 * or {@code mult} (a share of the base value, 0.05 is five percent).
 */
public final class LineAttrs {
    private LineAttrs() {}

    /** The one kind this class runs (the test pins it to the catalogue). */
    static final String KIND = "attr";
    /** The kinds that have an executor here. */
    public static Set<String> kinds() { return Set.of(KIND); }

    /** The modifier id of a stat node: {@code nusmp:line/attr/<route path>/<node>}. */
    public static ResourceLocation modifierId(String routeId, String nodeId) {
        int colon = routeId.indexOf(':');
        String path = colon >= 0 ? routeId.substring(colon + 1) : routeId;
        return ResourceLocation.fromNamespaceAndPath("nusmp", "line/attr/" + path + "/" + nodeId);
    }

    private record Wanted(String attribute, double amount, AttributeModifier.Operation op) {}

    /** Brings the player's transient line modifiers in step with the gate; returns how many are applied. */
    public static int sync(ServerPlayer p) {
        Map<ResourceLocation, Wanted> wanted = new LinkedHashMap<>();
        for (LinePassives.ActiveNode a : LinePassives.active(p)) {
            if (!KIND.equals(a.node().kind())) continue;
            String attr = a.node().param("attribute", "");
            if (LineLookups.attribute(attr) == null) continue;
            double amount = a.node().value("amount", a.level(), 0.0);
            if (amount == 0.0) continue;
            AttributeModifier.Operation op = "mult".equals(a.node().param("op", "add")) ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE;
            wanted.put(modifierId(a.route().id(), a.node().id()), new Wanted(attr, amount, op));
        }
        ListTag before = LineState.root(p).getList(LineState.MODS, Tag.TAG_STRING);
        if (wanted.isEmpty() && before.isEmpty()) return 0;
        for (int i = 0; i < before.size(); i++) {                                       // remove what was applied and is no longer wanted
            String entry = before.getString(i);
            int bar = entry.indexOf('|');
            if (bar <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(bar + 1));
            if (id == null || wanted.containsKey(id)) continue;
            Holder<Attribute> attr = LineLookups.attribute(entry.substring(0, bar));
            AttributeInstance inst = attr == null ? null : p.getAttribute(attr);
            if (inst != null && inst.hasModifier(id)) inst.removeModifier(id);
        }
        ListTag now = new ListTag();
        int applied = 0;
        for (Map.Entry<ResourceLocation, Wanted> e : wanted.entrySet()) {
            Wanted w = e.getValue();
            AttributeInstance inst = p.getAttribute(LineLookups.attribute(w.attribute()));
            if (inst == null) continue;
            AttributeModifier have = inst.getModifier(e.getKey());
            if (have == null || have.amount() != w.amount() || have.operation() != w.op()) inst.addOrUpdateTransientModifier(new AttributeModifier(e.getKey(), w.amount(), w.op()));
            now.add(StringTag.valueOf(w.attribute() + "|" + e.getKey()));
            applied++;
        }
        LineState.root(p).put(LineState.MODS, now);
        return applied;
    }

    /** Removes every line modifier from the player (the player left, or the engine shuts down). */
    public static void clear(ServerPlayer p) {
        ListTag before = LineState.root(p).getList(LineState.MODS, Tag.TAG_STRING);
        for (int i = 0; i < before.size(); i++) {
            String entry = before.getString(i);
            int bar = entry.indexOf('|');
            if (bar <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(bar + 1));
            Holder<Attribute> attr = LineLookups.attribute(entry.substring(0, bar));
            AttributeInstance inst = attr == null || id == null ? null : p.getAttribute(attr);
            if (inst != null && inst.hasModifier(id)) inst.removeModifier(id);
        }
        LineState.root(p).remove(LineState.MODS);
    }

    /** The attribute words and their lists, for the catalogue test. */
    static List<String> attributeWords() { return List.copyOf(LineLookups.attributeWords()); }
}
