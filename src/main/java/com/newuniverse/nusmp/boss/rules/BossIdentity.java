package com.newuniverse.nusmp.boss.rules;

import com.newuniverse.nusmp.boss.BossEntities;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Why: the boss rules apply to "every boss of this mod" and the mod has several ways a boss is made: entity classes that carry the DPS
 * budget (the Zogratis family, Zagred), registry-listed floor bosses, tagged mobs driven by a director (a boss that borrows a Tensura body is a
 * vanilla-typed mob with a persistent tag), and the Labyrinth Floor Warden. One place
 * answers "is this a boss" so the heal guard, the skill sweep and the activation veto cannot disagree. Strictly this mod's bosses: a
 * Tensura or vanilla boss is never touched (the same isolation rule as the buff engine); an addon can opt a type in by adding it to the
 * entity type tag {@code nusmp:boss}.
 */
public final class BossIdentity {
    private BossIdentity() {}

    /** Entity types that are bosses by data: add an id to data/nusmp/tags/entity_type/boss.json to opt one in. */
    public static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "boss"));

    /** The persistent entity tags the dungeon directors put on their bosses (floor boss, Juggernaut, Charybdis). */
    private static final String[] TAGS = {"nusmp_floor_boss", "nusmp_juggernaut", "nusmp_charybdis"};

    public static boolean isBoss(Entity e) {
        if (e == null || e.level() == null) return false;
        try {
            if (e instanceof BudgetedBoss || e instanceof FloorWardenEntity) return true;
            if (BossEntities.isModBoss(e) || e.getType().is(BOSS_TAG)) return true;
            for (String t : TAGS) if (e.getTags().contains(t)) return true;
        } catch (RuntimeException unreadable) { /* not provably a boss: not one */ }
        return false;
    }
}
