package com.newuniverse.nusmp.labyrinth.dnd.economy;

import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.dungeon.sentient.StoneTable;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the Labyrinth pays in things you can hold, not in a number. Two global NeoForge events do it so no monster class needs touching and a
 * monster from ANY mod that spawns in the Spire (a wildcard chamber, a Sealed Archway roster) pays the same: {@link LivingDropsEvent} adds
 * Magic Stone / crystal item entities and a raw monster cut to the drops, and {@link LivingExperienceDropEvent} zeroes the vanilla orbs, so
 * "level up by killing" is replaced by "gather, cook, trade". Both handlers act only when the level is the Labyrinth, the mob is a hostile
 * {@link Enemy} and it is not a boss: bosses keep their own loot tables (relics, the Shadow Key) and are excluded by the interface
 * {@link BudgetedBoss}, the {@code nusmp_peak_boss} tag the Peak Altar puts on its boss, the {@code nusmp:floor_warden} id and the common
 * {@code c:bosses} entity tag, so a boss from another mod that follows the tag convention is excluded without a list. The monster's toughness
 * is read from its BASE max health (attribute modifiers such as the Labyrinth health buffer do not change what a stone is worth, so the buffer
 * cannot inflate the payout). Drops are added, never replaced: vanilla loot stays (rule: add the new, keep the old). The roll itself is the
 * pure {@link EconomyMath}; the raw cut is {@code DungeonFood}'s own toxic item, so the hunger clock has something to eat and a reason to cook.
 */
public final class EconomyDrops {
    private EconomyDrops() {}

    /** The tag the Peak Altar puts on the boss it spawns (same string as {@code PeakAltarBlockEntity.BOSS_TAG}). */
    public static final String PEAK_BOSS_TAG = "nusmp_peak_boss";

    private static final ResourceLocation STONE = tensura("magic_stone"), LOW = tensura("low_quality_magic_crystal"),
            MEDIUM = tensura("medium_quality_magic_crystal"), HIGH = tensura("high_quality_magic_crystal"), WARDEN = ResourceLocation.fromNamespaceAndPath("nusmp", "floor_warden");
    private static final ResourceKey<Enchantment> LOOTING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("looting"));

    private static ResourceLocation tensura(String path) { return ResourceLocation.fromNamespaceAndPath("tensura", path); }

    /** Does this death belong to the economy: a hostile non-boss Mob in the Labyrinth. */
    static boolean applies(LivingEntity v) {
        return v instanceof Mob m && v.level() instanceof ServerLevel sl && Labyrinth.in(sl) && (v instanceof Enemy || com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.isDungeonMonster(v)) && !isBoss(m);
    }

    /** Bosses: the DPS-budget interface, the peak tag, the Floor Warden id and the shared boss entity tag. */
    static boolean isBoss(Mob m) {
        return m instanceof BudgetedBoss || m.getTags().contains(PEAK_BOSS_TAG) || m.getType().is(Tags.EntityTypes.BOSSES)
                || WARDEN.equals(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()));
    }

    public static void onDrops(LivingDropsEvent e) {
        if (!DndConfig.get(DndConfig.DROPS) || !applies(e.getEntity())) return;
        Mob mob = (Mob) e.getEntity();
        ServerLevel level = (ServerLevel) mob.level();
        ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : (mob.getKillCredit() instanceof ServerPlayer lp ? lp : null);
        boolean player = killer != null;
        int looting = player ? looting(level, killer) : 0;
        StoneTable.Grade grade = StoneTable.gradeOf(Wildlife.tierOf(mob), mob.getAttributeBaseValue(Attributes.MAX_HEALTH), false, false);
        Dice dice = level.getRandom()::nextDouble;
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
        EconomyMath.Loot loot = EconomyMath.roll(path, grade, player, looting, DndConfig.get(DndConfig.STONE_CHANCE), DndConfig.get(DndConfig.MEAT_CHANCE), dice);
        if (loot.isEmpty()) return;
        List<ItemStack> out = new ArrayList<>();
        add(out, STONE, loot.stones().stones());
        add(out, LOW, loot.stones().low());
        add(out, MEDIUM, loot.stones().medium());
        add(out, HIGH, loot.stones().high());
        if (!loot.meat().isEmpty()) {
            Item raw = DungeonFood.rawFor(loot.meat().kind());
            if (raw != null) out.add(new ItemStack(raw, loot.meat().count()));
        }
        for (ItemStack s : out) e.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.3, mob.getZ(), s));
    }

    public static void onExperience(LivingExperienceDropEvent e) {
        if (!DndConfig.get(DndConfig.NO_XP) || !applies(e.getEntity())) return;
        e.setDroppedExperience(0);
    }

    /** Count of the item with this id (a missing Tensura crystal falls back to the Magic Stone, a missing stone to nothing), in stacks that fit. */
    private static void add(List<ItemStack> out, ResourceLocation id, int count) {
        if (count <= 0) return;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElseGet(() -> BuiltInRegistries.ITEM.getOptional(STONE).orElse(null));
        if (item == null) return;
        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        while (count > 0) {
            int n = Math.min(count, max);
            out.add(new ItemStack(item, n));
            count -= n;
        }
    }

    /** The Looting level of the killer's main-hand weapon (0 if it has none or the enchantment is missing). */
    private static int looting(ServerLevel level, Player killer) {
        Optional<? extends Holder<Enchantment>> looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(LOOTING);
        return looting.isEmpty() ? 0 : Math.max(0, killer.getMainHandItem().getEnchantments().getLevel(looting.get()));
    }
}
