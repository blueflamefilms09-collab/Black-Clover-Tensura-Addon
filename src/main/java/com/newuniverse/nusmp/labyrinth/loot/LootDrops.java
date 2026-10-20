package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionIds;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Why: this is the game side of the drop decision, the only part that touches a monster, a level and a loot context. The pure {@link KillLoot} decides WHAT drops from a
 * family, a depth, a luck and the rates; this class finds those inputs and turns the answer into stacks. A kill counts when a PLAYER dealt the last blow (the vanilla
 * {@code LAST_DAMAGE_PLAYER} parameter: monsters killing each other, or a trap, drop nothing extra, so there is no way to farm the table with a mob grinder that no player
 * fights); the victim is not a player, not a tamed pet, not a summoned minion of the Mob Foundry; and the place counts: a dimension of the mod that is not the Hub (the
 * Mega-Dungeon, the old Spire, a rift pocket), or, by the config, one of the mod's own monsters (the Dungeon Master's tag, a Foundry mob, any entity of the {@code nusmp}
 * namespace) anywhere. The family is the Dungeon Master's faction tag first, then the entity-type tag {@code nusmp:mega/<faction>}, then the words of the registry path; the depth is
 * the floor under the monster (or, outside the Mega-Dungeon, a guess from its health); a boss is whatever {@link BalanceLaw#isBoss} says. An item that is already in the loot is
 * never added a second time. Server side; any fault leaves the loot as it was.
 */
public final class LootDrops {
    private LootDrops() {}

    /** Is this a dimension of the mod where the loot modifier works for every monster: a nusmp dimension that is neither the Hub nor the removed Living Dungeon? */
    public static boolean dungeon(Level level) {
        if (level == null) return false;
        ResourceLocation d = level.dimension().location();
        return "nusmp".equals(d.getNamespace()) && !Labyrinth.inSanctuary(level) && !Labyrinth.inRemovedDungeon(level);
    }

    /** Is this one of the mod's own monsters: tagged by the Dungeon Master, a Mob Foundry mob, or of the mod's namespace? */
    public static boolean ours(LivingEntity e) {
        if (MegaDungeon.isDungeonMonster(e) || e instanceof FoundryMobEntity) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && "nusmp".equals(id.getNamespace());
    }

    /** The loot family of a monster. */
    public static String familyOf(LivingEntity e) {
        String dm = null, tag = null;
        if (e instanceof FoundryMobEntity f && f.faction() != null) dm = f.faction();
        for (String f : FactionIds.CONTRACT) {
            if (dm == null && e.getTags().contains(MegaDungeon.DM_TAG + "_" + f)) dm = f;
            if (tag == null && e.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "mega/" + f)))) tag = f;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return LootFamilies.resolve(dm, tag, id == null ? "" : id.getPath());
    }

    /** The depth of a kill: the floor under the monster in the Mega-Dungeon, else a guess from its health (twelve health to a floor). */
    public static int depthOf(ServerLevel level, LivingEntity e) {
        if (MegaDungeon.in(level)) return Math.abs(MegaDungeon.floorAt(level, e.blockPosition()));
        return Math.max(1, (int) (e.getMaxHealth() / 12.0F));
    }

    /** Adds the drops of this kill to the loot; returns how many stacks were added. */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!LootConfig.get(LootConfig.ENABLED)) return 0;
        Entity victim = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(victim instanceof LivingEntity mob) || mob instanceof Player || context.getParamOrNull(LootContextParams.DAMAGE_SOURCE) == null) return 0;
        if (context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) == null) return 0;
        if (mob instanceof TamableAnimal t && t.isTame()) return 0;
        if (mob.getTags().contains(FoundryMobEntity.MINION_TAG)) return 0;
        ServerLevel level = context.getLevel();
        if (level == null) return 0;
        boolean here = dungeon(level);
        if (!here && !(LootConfig.get(LootConfig.OURS_OUTSIDE) && ours(mob))) return 0;
        if (Labyrinth.inSanctuary(level)) return 0;
        Dice dice = context.getRandom()::nextDouble;
        int depth = depthOf(level, mob);
        KillLoot.Input in = new KillLoot.Input(familyOf(mob), depth, BalanceLaw.isBoss(mob), context.getLuck());
        return stacks(loot, KillLoot.roll(dice, in, tables(), LootConfig.rates()), depth);
    }

    private static volatile LootTables tables;

    static LootTables tables() {
        LootTables t = tables;
        if (t == null) tables = t = LootTables.of(LootRoster.get());
        return t;
    }

    /** Turns drops into stacks (skipping items that do not exist and items already in the loot) and appends them. */
    static int stacks(List<ItemStack> loot, List<KillLoot.Drop> drops, int depth) {
        int n = 0;
        for (KillLoot.Drop d : drops) {
            Item item = LootItems.get(d.id());
            if (item == null || has(loot, item)) continue;
            ItemStack stack;
            if (d.kind() == LootTables.Kind.GEAR && item instanceof LootGear g) {
                stack = GearStacks.create(item, GearStacks.roll(g, d.rarity() == null ? LootRarity.COMMON : d.rarity(), d.seed(), depth));
            } else {
                stack = new ItemStack(item, d.count());
            }
            if (stack.isEmpty()) continue;
            loot.add(stack);
            n++;
        }
        return n;
    }

    private static boolean has(List<ItemStack> loot, Item item) {
        for (ItemStack s : loot) if (s.is(item)) return true;
        return false;
    }

    /** The entity type of a registry id; used by the self-test. */
    static EntityType<?> type(String id) { return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null); }
}
