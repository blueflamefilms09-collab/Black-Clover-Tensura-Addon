package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Why: the "permanent stat boost" of the brief has to be REAL attribute strength, and it has to stay right whatever happens to the player: the same fixed
 * modifier id per stat ({@code nusmp:system/str} ...) means applying is idempotent (a second call finds the modifier already correct and changes nothing, and a
 * changed table value replaces the old one instead of stacking), it can be removed by id alone, and no other system can mistake it for its own. The amounts
 * come from the table (a point's worth and its ceiling are data), and every attribute id is resolved at run time and SKIPPED when it is not registered or the
 * player does not have it, so the Tensura magicule effect simply does nothing in a game without Tensura instead of crashing. The modifiers are PERMANENT
 * (saved with the player, so they survive a restart even before anything re-applies them) and are re-applied on login, respawn (death creates a new body that
 * has none), dimension change and clone, because the saved points in the attachment are the truth and the modifiers are only their effect. Server thread only.
 */
public final class SystemStats {
    private SystemStats() {}

    /** The fixed id of a stat's modifier on every attribute it feeds. */
    public static ResourceLocation modifierId(Stat stat) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "system/" + stat.id());
    }

    /** Makes the player's attributes match their saved points (the modifiers are added, replaced, or removed). Safe to call any number of times. */
    public static void apply(ServerPlayer p) {
        apply(p, SystemQuests.state(p));
    }

    public static void apply(ServerPlayer p, SystemState state) {
        StatTable table = SystemData.stats();
        for (Stat stat : Stat.values()) {
            StatTable.Def def = table.def(stat);
            if (def == null) continue;
            int points = state.points(stat);
            ResourceLocation id = modifierId(stat);
            for (StatTable.Effect e : def.effects()) {
                AttributeInstance inst = instance(p, e.attribute());
                if (inst == null) continue;
                double amount = e.amount(points);
                if (points <= 0 || amount == 0.0) {
                    if (inst.hasModifier(id)) inst.removeModifier(id);
                    continue;
                }
                AttributeModifier.Operation op = operation(e.op());
                AttributeModifier current = inst.getModifier(id);
                if (current != null && current.amount() == amount && current.operation() == op) continue;
                inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Takes every System modifier off the player (an admin reset, or a stat table that is being replaced). The saved points are not touched. */
    public static void remove(ServerPlayer p) {
        for (Stat stat : Stat.values()) {
            StatTable.Def def = SystemData.stats().def(stat);
            if (def == null) continue;
            for (StatTable.Effect e : def.effects()) {
                AttributeInstance inst = instance(p, e.attribute());
                if (inst != null && inst.hasModifier(modifierId(stat))) inst.removeModifier(modifierId(stat));
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** The modifier amount a stat's first effect currently has on the player (0 when it has none, or the attribute is absent): for status output and tests. */
    public static double currentAmount(ServerPlayer p, Stat stat, int effectIndex) {
        StatTable.Def def = SystemData.stats().def(stat);
        if (def == null || effectIndex < 0 || effectIndex >= def.effects().size()) return 0.0;
        AttributeInstance inst = instance(p, def.effects().get(effectIndex).attribute());
        AttributeModifier m = inst == null ? null : inst.getModifier(modifierId(stat));
        return m == null ? 0.0 : m.amount();
    }

    /** The player's instance of the attribute with this registry id, or null when the id is bad, the attribute is not registered, or the player has no such attribute. */
    static AttributeInstance instance(ServerPlayer p, String attributeId) {
        ResourceLocation rl = ResourceLocation.tryParse(attributeId);
        if (rl == null) return null;
        Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h).orElse(null);
        return holder == null ? null : p.getAttribute(holder);
    }

    private static AttributeModifier.Operation operation(StatTable.Op op) {
        return switch (op) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
