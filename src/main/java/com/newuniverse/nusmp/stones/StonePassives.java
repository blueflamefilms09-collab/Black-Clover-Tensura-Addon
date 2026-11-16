package com.newuniverse.nusmp.stones;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the attribute passives of a stone worn in the slot must be gated (slot, gate, setting), which the relic core's own {@code Passive} cannot do, so the slot applies its own transient modifiers once a
 * second and removes them at once when the stone is taken off, paused or the setting is switched off. Transient: they never reach the save file, so a crash cannot leave a stone's health on a player who no
 * longer wears it. Non-attribute passives (the mental shield, Foresight, the Soul anchor, the debuff negation) are event rules in {@link StoneEvents}. Server side; at most three modifiers per player.
 */
final class StonePassives {
    private StonePassives() {}

    private record Mod(Holder<Attribute> holder, ResourceLocation id, double base, Operation op) {}

    private static final Map<InfinityStone, List<Mod>> MODS = new EnumMap<>(InfinityStone.class);

    private static void add(InfinityStone s, String attribute, String name, double base, Operation op) {
        ResourceLocation attr = ResourceLocation.tryParse(attribute);
        if (attr == null) return;
        BuiltInRegistries.ATTRIBUTE.getHolder(attr).ifPresent(h ->
                MODS.computeIfAbsent(s, k -> new ArrayList<>()).add(new Mod(h, ResourceLocation.fromNamespaceAndPath("anime_multiverse", "stone_" + name), base, op)));
    }

    private static boolean built;

    /** The table is built on first use, when the attribute registry is bound (never in a static initialiser). */
    private static synchronized void build() {
        if (built) return;
        built = true;
        add(InfinityStone.SPACE, "minecraft:generic.movement_speed", "space_speed", 0.30, Operation.ADD_MULTIPLIED_TOTAL);
        add(InfinityStone.SPACE, "minecraft:generic.knockback_resistance", "space_anchor", 0.50, Operation.ADD_VALUE);
        add(InfinityStone.MIND, "tensura:magicule_regeneration_multiplier", "mind_regen", 0.25, Operation.ADD_MULTIPLIED_TOTAL);
        add(InfinityStone.POWER, "minecraft:generic.max_health", "power_health", 0.40, Operation.ADD_MULTIPLIED_TOTAL);
        add(InfinityStone.POWER, "minecraft:generic.armor", "power_armor", 6.0, Operation.ADD_VALUE);
        add(InfinityStone.TIME, "minecraft:generic.attack_speed", "time_haste", 0.15, Operation.ADD_MULTIPLIED_TOTAL);
        add(InfinityStone.REALITY, "minecraft:generic.luck", "reality_luck", 2.0, Operation.ADD_VALUE);
        add(InfinityStone.SOUL, "tensura:max_aura", "soul_aura", 0.25, Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Puts the stone's modifiers on (replacing one whose amount changed with the scale) and takes every other stone's off. Idempotent; a second apply of the same scale changes nothing. */
    static void apply(ServerPlayer p, InfinityStone worn, double scale) {
        build();
        for (Map.Entry<InfinityStone, List<Mod>> en : MODS.entrySet()) {
            boolean on = en.getKey() == worn;
            for (Mod m : en.getValue()) {
                AttributeInstance inst = p.getAttribute(m.holder());
                if (inst == null) continue;
                AttributeModifier cur = inst.getModifier(m.id());
                if (!on) {
                    if (cur != null) inst.removeModifier(m.id());
                    continue;
                }
                double amount = StoneRules.passive(m.base(), scale);
                if (cur != null && Math.abs(cur.amount() - amount) < 1.0e-9) continue;
                if (cur != null) inst.removeModifier(m.id());
                inst.addTransientModifier(new AttributeModifier(m.id(), amount, m.op()));
            }
        }
    }

    /** Takes every stone modifier off. */
    static void clear(ServerPlayer p) {
        build();
        for (List<Mod> mods : MODS.values()) {
            for (Mod m : mods) {
                AttributeInstance inst = p.getAttribute(m.holder());
                if (inst != null && inst.getModifier(m.id()) != null) inst.removeModifier(m.id());
            }
        }
    }

    /** How many modifiers the table holds for a stone (a self-test reads it). */
    static int count(InfinityStone s) {
        build();
        List<Mod> l = MODS.get(s);
        return l == null ? 0 : l.size();
    }
}
