package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.neoforge.common.Tags;

/**
 * Why: the faction rules apply to WILD monsters only. A boss has its own balance (the DPS budget, its phases, its loot table of relics and keys)
 * and a faction quarrel must neither start nor end its fight; a servant (a tamed or owned creature, a Paladin, a shadow soldier, a relic's summon)
 * answers to a player, and a goblin's grudge must not take it over, nor the target guard shield it from the monsters that ought to hunt it. This is
 * the one place that decides which a mob is, from the game objects; the marker strings themselves are in the pure {@link TurfMarks}. The boss test
 * is the economy's own (the {@code BudgetedBoss} interface, the {@code c:bosses} entity tag, the Floor Warden by id) plus the wanderer and old-boss
 * tags, plus the Floor Warden CLASS, which catches the Labyrinth Guardian, a subclass with an id of its own. Both checks are cheap (a class test, a
 * small set lookup, one persistent-data read) and run once per mob per second at most.
 */
final class TurfEligibility {
    private TurfEligibility() {}

    private static final ResourceLocation WARDEN = ResourceLocation.tryParse(TurfMarks.WARDEN_ID);

    static boolean isBoss(Mob m) {
        if (m instanceof BudgetedBoss || m instanceof FloorWardenEntity) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.BOSS_TAGS)) return true;
        if (m.getType().is(Tags.EntityTypes.BOSSES)) return true;
        return WARDEN != null && WARDEN.equals(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()));
    }

    static boolean isServant(Mob m) {
        if (m instanceof TamableAnimal t && t.isTame()) return true;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.SERVANT_TAGS)) return true;
        CompoundTag data = m.getPersistentData();
        if (data.isEmpty()) return false;
        for (String key : TurfMarks.SERVANT_KEYS) if (data.contains(key)) return true;
        return false;
    }

    /** A creature the faction rules apply to: neither a boss nor anyone's servant. */
    static boolean wild(Mob m) {
        return !isBoss(m) && !isServant(m);
    }

    /** Why the rules skip this mob ({@code boss}, {@code servant}), or null when they apply (for the admin command). */
    static String why(Mob m) {
        if (isBoss(m)) return "boss";
        return isServant(m) ? "servant" : null;
    }
}
