package com.newuniverse.nusmp.beastwill;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the attribute bonuses of a will (3.45.0), applied and removed in ONE place so cleanup can never miss one. Every bonus is a TRANSIENT modifier (never saved: a crash can not leave a permanent one) with
 * a fixed id {@code nusmp:bw/<attribute>/<add|mult>}, one id per attribute and operation, so applying again replaces instead of stacking. The bonuses of the passive and of the running phase are SUMMED per
 * attribute, multiplied by the strength share, and then cut to the hard cap of {@link BeastWillRules#ATTR_CAPS}: however a will file or a stack of scales adds up, the total can not pass the cap
 * (that is the answer to "no runaway multiplicative bonuses"). {@link #clear} removes everything of the system from a player; it is what the form's end, death, respawn, logout and a swap all call.
 */
public final class BeastWillAttributes {
    private BeastWillAttributes() {}

    static ResourceLocation id(String attribute, BeastWillDef.Op op) {
        String path = attribute.substring(attribute.indexOf(':') + 1).replace('.', '_');
        return ResourceLocation.fromNamespaceAndPath("nusmp", "bw/" + path + "/" + (op == BeastWillDef.Op.ADD ? "add" : "mult"));
    }

    private static Holder<Attribute> holder(String attribute) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attribute)).orElse(null);
    }

    /** The summed, scaled and capped bonus per "attribute|op" for a will in a phase (phase 0 = only the passive). Pure arithmetic over the data, so the tests can read it. */
    public static Map<String, Double> totals(BeastWillDef def, int phase, double scale) {
        Map<String, Double> sum = new LinkedHashMap<>();
        for (BeastWillDef.Stat s : def.passive()) add(sum, s, scale);
        BeastWillDef.Phase ph = def.phase(phase);
        if (ph != null) for (BeastWillDef.Stat s : ph.stats()) add(sum, s, scale);
        sum.replaceAll((k, v) -> {
            BeastWillRules.AttrCap cap = BeastWillRules.ATTR_CAPS.get(k.substring(0, k.indexOf('|')));
            if (cap == null) return 0.0;
            return Math.min(v, k.endsWith("|ADD") ? cap.maxAdd() : cap.maxMult());
        });
        return sum;
    }

    private static void add(Map<String, Double> sum, BeastWillDef.Stat s, double scale) {
        sum.merge(s.attribute() + "|" + s.op().name(), s.amount() * BeastWillRules.clamp(scale, 0, 2), Double::sum);
    }

    /** Replaces every bonus of the system on the player with the ones for this will, phase and strength. */
    public static void apply(ServerPlayer p, BeastWillDef def, int phase, double scale) {
        clear(p);
        if (def == null || scale <= 0) return;
        for (Map.Entry<String, Double> e : totals(def, phase, scale).entrySet()) {
            double v = e.getValue();
            if (!(v > 1.0e-6)) continue;
            int bar = e.getKey().indexOf('|');
            String attribute = e.getKey().substring(0, bar);
            BeastWillDef.Op op = BeastWillDef.Op.valueOf(e.getKey().substring(bar + 1));
            Holder<Attribute> h = holder(attribute);
            AttributeInstance inst = h == null ? null : p.getAttribute(h);
            if (inst == null) continue;
            inst.addTransientModifier(new AttributeModifier(id(attribute, op), v, op == BeastWillDef.Op.ADD ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Removes every modifier of the system (all allowed attributes, both operations). Cheap: twenty removals. */
    public static void clear(ServerPlayer p) {
        for (String attribute : BeastWillRules.ATTR_CAPS.keySet()) {
            Holder<Attribute> h = holder(attribute);
            AttributeInstance inst = h == null ? null : p.getAttribute(h);
            if (inst == null) continue;
            inst.removeModifier(id(attribute, BeastWillDef.Op.ADD));
            inst.removeModifier(id(attribute, BeastWillDef.Op.MULT));
        }
    }

    /** True when any modifier of the system is on the player (the self-test and the cleanup tests read it). */
    public static boolean any(ServerPlayer p) {
        for (String attribute : BeastWillRules.ATTR_CAPS.keySet()) {
            Holder<Attribute> h = holder(attribute);
            AttributeInstance inst = h == null ? null : p.getAttribute(h);
            if (inst != null && (inst.hasModifier(id(attribute, BeastWillDef.Op.ADD)) || inst.hasModifier(id(attribute, BeastWillDef.Op.MULT)))) return true;
        }
        return false;
    }
}
