package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.eco.TurfMarks;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.Tags;

/**
 * Why: {@link LootRules} is a pure function over plain facts, and somebody has to read those facts off the live game; this is that reader, kept out of the rule so the
 * rule stays testable and kept out of the two entry points (the global loot modifier and the Guardian's death listener) so they read a death in exactly the same way.
 * It answers three questions. WHO killed it: the player the damage came from, else the player the game credits (a tamed pet's owner, the last player to hurt it), which is
 * how the dungeon's other drops decide it. WHAT it was: a Guardian (the scoreboard tag the guard module puts on every sector boss), an ordinary monster, or something
 * that never pays. WHERE it stood: the road and floor of the sector, read with the dungeon's own facade {@link MegaDungeon} (never by parsing positions), and for a Guardian
 * the sector it guards by the sector id the guard module stores on it. The monster test deliberately repeats, line for line, the three tests the dungeon's equipment
 * drops use ({@code GearDrops.hostile}, {@code isBoss}, {@code isServant}: they are package-private there, and the rule is "a skill book drops on exactly the kills that
 * pay equipment"): a vanilla-style {@code Enemy} or a Dungeon Master creature (Tensura's monsters are tamable animals and not {@code Enemy}), that is neither a boss
 * nor anyone's servant, so a pet, a shadow soldier, a Paladin or a wandering boss is no farm. Server thread only.
 */
public final class LootVictims {
    private LootVictims() {}

    private static final ResourceLocation WARDEN = ResourceLocation.tryParse(TurfMarks.WARDEN_ID);

    /** Where a sector Guardian stands in the dungeon's order: the road and the last (boss) floor of the sector it guards. */
    public record Site(Polarity polarity, int floor) {}

    // ------------------------------------------------------------------ who
    /** The player that killed (or last hurt) the thing, from a damage source and the entity's own credit; null when no player did. */
    public static ServerPlayer killer(DamageSource source, LivingEntity dead) {
        Entity cause = source == null ? null : source.getEntity();
        if (cause instanceof ServerPlayer sp) return sp;
        return dead != null && dead.getKillCredit() instanceof ServerPlayer lp ? lp : null;
    }

    /** The same from a loot context of an entity kill (the attacker, else the last player to damage it). */
    public static ServerPlayer killer(LootContext context) {
        Entity attacker = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        if (attacker instanceof ServerPlayer sp) return sp;
        Player last = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        return last instanceof ServerPlayer lp ? lp : null;
    }

    // ------------------------------------------------------------------ what
    /** What this dying thing is, for the drop rule. */
    public static LootRules.Kind kindOf(LivingEntity dead) {
        if (dead == null) return LootRules.Kind.OTHER;
        if (dead.getTags().contains(LabyrinthGuardianEntity.SECTOR_TAG)) return LootRules.Kind.GUARDIAN;
        if (!(dead instanceof Mob mob)) return LootRules.Kind.OTHER;
        if (!hostile(mob) || isBoss(mob) || isServant(mob)) return LootRules.Kind.OTHER;
        return LootRules.Kind.MONSTER;
    }

    /** A monster of the dungeon: a vanilla-style {@code Enemy}, or one of the Dungeon Master's creatures (scoreboard tag {@code nusmp_dm}). */
    static boolean hostile(Mob m) { return m instanceof Enemy || MegaDungeon.isDungeonMonster(m); }

    /** Bosses, by the dungeon economy's own tests: the DPS-budget interface, the Floor Warden class, the boss and wanderer tags, the shared {@code c:bosses} tag, the Warden's id. */
    static boolean isBoss(Mob m) {
        if (m instanceof BudgetedBoss || m instanceof FloorWardenEntity) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.BOSS_TAGS)) return true;
        if (m.getType().is(Tags.EntityTypes.BOSSES)) return true;
        return WARDEN != null && WARDEN.equals(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()));
    }

    /** A servant answers to a player (a tamed or owned creature, a Paladin, a shadow soldier, a relic's summon): its death is no loot. */
    static boolean isServant(Mob m) {
        if (m instanceof TamableAnimal t && t.isTame()) return true;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.SERVANT_TAGS)) return true;
        CompoundTag data = m.getPersistentData();
        if (data.isEmpty()) return false;
        for (String key : TurfMarks.SERVANT_KEYS) if (data.contains(key)) return true;
        return false;
    }

    // ------------------------------------------------------------------ where
    /** The road of the sector the thing stands in, or null outside any sector. */
    public static Polarity polarityAt(ServerLevel level, LivingEntity dead) { return MegaDungeon.polarityAt(level, dead.blockPosition()); }

    /** The signed floor the thing stands on, 0 outside any sector. */
    public static int floorAt(ServerLevel level, LivingEntity dead) { return MegaDungeon.floorAt(level, dead.blockPosition()); }

    /**
     * The sector a Guardian guards: the id the guard module stored on it (a Guardian made by hand for a test stores -1, belongs to no sector and pays nothing), else, for
     * one made before that key existed, the sector that holds its body. Null when no sector is known.
     */
    public static Site guardianSite(ServerLevel level, LivingEntity guardian) {
        MegaData data = MegaData.get(level.getServer());
        CompoundTag tag = guardian.getPersistentData();
        Sector sector = tag.contains(LabyrinthGuardianEntity.SECTOR_KEY, Tag.TAG_ANY_NUMERIC)
                ? data.sector(tag.getInt(LabyrinthGuardianEntity.SECTOR_KEY))
                : data.sectorAt(guardian.blockPosition());
        return sector == null ? null : new Site(sector.polarity, sector.lastFloor());
    }
}
