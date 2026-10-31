package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;

/**
 * Why: the rare drop of a Beast Will Core (3.45.0), kept to the rules of the owner's brief: only a beast of A rank or better (the rank worked out from its Existence Points, see {@link BeastWillRules#rankOf})
 * and only the beasts a will's file names; a wild one that has lived long enough (no spawn-and-kill farm), not tamed, not owned, not a player; killed by a player; one chance per kill and one core at most; the
 * chance is the will's own (a legendary one below one per cent) times the server's scale; and a player waits {@code coreCooldownDays} game days between two cores of the same will and can not get the core of
 * a will they already hold. The core lies on the ground without a despawn timer and glows, so a kill far from home does not lose it. Cost: one entity-type lookup per kill by a player, nothing per tick.
 */
public final class BeastWillDrops {
    private BeastWillDrops() {}

    /** The rank a beast counts as, from its Existence Points (or, for a creature Tensura does not know, from its health). Null below rank C. */
    public static BeastRank rankOf(LivingEntity beast) {
        double ep = TensuraBridge.maxEp(beast);
        return BeastWillRules.rankOf(BeastWillRules.effectiveEp(ep, beast.getMaxHealth(), TensuraBridge.isTensuraMob(beast)), BeastWillConfig.rankEp());
    }

    /** Is this creature a wild, grown, ownerless beast (a candidate for the drop at all)? */
    static boolean wild(LivingEntity v) {
        if (v instanceof Player) return false;
        if (v instanceof TamableAnimal t && (t.isTame() || t.getOwnerUUID() != null)) return false;
        if (TensuraBridge.permanentOwner(v) != null) return false;
        return v.tickCount >= BeastWillConfig.minVictimAge();
    }

    public static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !BeastWillConfig.enabled() || BeastWillConfig.dropScale() <= 0) return;
        ServerPlayer killer = LootVictims.killer(e.getSource(), v);
        if (killer == null || killer == v) return;
        List<BeastWillDef> candidates = BeastWillCatalog.forEntity(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(v.getType())));
        if (candidates.isEmpty() || !wild(v)) return;
        BeastRank rank = rankOf(v);
        if (rank == null || !rank.atLeast(BeastWillConfig.minDropRank())) return;
        BeastWillState s = BeastWillStore.of(killer);
        long today = BeastWillRules.dayOf(killer.level().getGameTime());
        for (BeastWillDef def : candidates) {
            if (!rank.atLeast(def.minRank()) || s.has(def.id())) continue;
            Long last = s.coreDay.get(def.id());
            if (last != null && today - last < def.coreCooldownDays()) continue;
            if (v.getRandom().nextDouble() >= def.dropChance() * BeastWillConfig.dropScale()) continue;
            s.coreDay.put(def.id(), today);
            ItemStack core = BeastWillItems.core(def.id(), beastName(v), rank);
            ItemEntity drop = new ItemEntity(v.level(), v.getX(), v.getY() + 0.5, v.getZ(), core);
            drop.setUnlimitedLifetime();
            drop.setGlowingTag(true);
            drop.setPickUpDelay(20);
            e.getDrops().add(drop);
            BeastWillFeedback.say(killer, "core_dropped", true, BeastWillText.name(def));
            return;                                                                          // one core at most per kill
        }
    }

    private static String beastName(LivingEntity v) {
        String n = v.getType().getDescription().getString();
        return n.length() > 60 ? n.substring(0, 60) : n;
    }

    /** For the commands and the doc: the entity type id the drop looks at. */
    static String idOf(EntityType<?> t) { return String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(t)); }
}
