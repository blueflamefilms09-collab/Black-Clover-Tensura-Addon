package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Perk;
import com.newuniverse.nusmp.elite.nations.core.PerkTable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the Nation Perks ("perks (Nation Perks)"): the table loaded from {@code perks.json} (or the built-in one), the numbers derived from it for a nation (treasury cap, member cap, shop discount, guard
 * extras, board slots, contract bonus) and the member attribute buffs applied near the capital. Attribute buffs are TRANSIENT modifiers with a stable id, so they never persist on the player, never stack
 * (the same id is replaced) and are removed the moment the player leaves the capital area or the nation (or the system is switched off). Server side.
 */
public final class NationPerks {
    private NationPerks() {}

    private static volatile PerkTable table = PerkTable.defaults();

    public static PerkTable table() { return table; }

    public static void setTable(PerkTable t) { table = t == null ? PerkTable.defaults() : t; }

    // ---------------------------------------------------------------- numbers of a nation
    public static long treasuryCap(Nation n) {
        long base = NationsConfig.get(NationsConfig.BASE_TREASURY_CAP);
        return Math.max(0L, base + (long) table.sum(Perk.TREASURY_CAP, n.level));
    }

    public static int memberCap(Nation n) {
        int cap = NationsConfig.get(NationsConfig.BASE_MEMBER_CAP) + (int) table.sum(Perk.MEMBER_CAP, n.level);
        return Math.max(1, Math.min(NationsConfig.get(NationsConfig.MAX_MEMBER_CAP), cap));
    }

    /** Percent off shop prices for members of this nation. */
    public static int shopDiscountPercent(Nation n) { return (int) Math.max(0, Math.min(90, table.sum(Perk.SHOP_DISCOUNT, n.level))); }

    public static int guardExtra(Nation n) { return (int) table.sum(Perk.GUARD_COUNT, n.level); }

    public static int boardSlots(Nation n) { return (int) table.sum(Perk.BOARD_SLOTS, n.level); }

    public static double contractBonusPercent(Nation n) { return table.sum(Perk.CONTRACT_BONUS, n.level); }

    // ---------------------------------------------------------------- attributes
    private static ResourceLocation modId(String attributeId) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "nation_perk/" + attributeId.replace(':', '_').replace('.', '_'));
    }

    private static Map<String, Double> attributeTotals(int level) {
        Map<String, Double> out = new HashMap<>();
        for (Perk p : table.ofType(Perk.ATTRIBUTE, level)) out.merge(p.arg(), p.value(), Double::sum);
        return out;
    }

    /** Gives (near = true) or takes away (near = false) the attribute perks of a nation level for one player. Cheap when nothing changes. */
    public static void apply(ServerPlayer p, int level, boolean near) {
        Map<String, Double> totals = near ? attributeTotals(level) : Map.of();
        for (String arg : allAttributeIds()) {
            Holder<Attribute> h = holder(arg);
            if (h == null) continue;
            AttributeInstance inst = p.getAttribute(h);
            if (inst == null) continue;
            ResourceLocation id = modId(arg);
            AttributeModifier existing = inst.getModifier(id);
            Double want = totals.get(arg);
            if (want == null || want == 0.0) {
                if (existing != null) inst.removeModifier(id);
            } else if (existing == null || existing.amount() != want) {
                if (existing != null) inst.removeModifier(id);
                inst.addTransientModifier(new AttributeModifier(id, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    /** Removes every attribute perk from a player (left the nation, system off). */
    public static void clear(ServerPlayer p) { apply(p, 0, false); }

    private static java.util.Set<String> allAttributeIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Perk perk : table.upTo(1000)) if (perk.is(Perk.ATTRIBUTE)) ids.add(perk.arg());
        return ids;
    }

    private static Holder<Attribute> holder(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h).orElse(null);
    }
}
