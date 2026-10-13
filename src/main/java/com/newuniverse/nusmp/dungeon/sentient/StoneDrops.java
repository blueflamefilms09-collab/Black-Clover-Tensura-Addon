package com.newuniverse.nusmp.dungeon.sentient;

import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a creature of the Dungeon leaves when it dies: Magic Stones and crystals ({@link StoneTable}), and now and then a rare synthesis
 * material ({@link MaterialTable}). The stones and crystals are Tensura's items, looked up by id when they are needed; one that is missing
 * is replaced by a Magic Stone, and if there is no Magic Stone either, nothing drops (nothing here ever throws for it).
 */
final class StoneDrops {
    private StoneDrops() {}

    private static final ResourceLocation STONE = tensura("magic_stone"), LOW = tensura("low_quality_magic_crystal"),
            MEDIUM = tensura("medium_quality_magic_crystal"), HIGH = tensura("high_quality_magic_crystal");
    private static final ResourceKey<Enchantment> LOOTING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("looting"));

    private static ResourceLocation tensura(String path) { return ResourceLocation.fromNamespaceAndPath("tensura", path); }

    /**
     * The drops of one death: for a kill by a player the grade's haul and maybe a material (a Juggernaut always leaves its heart); for a
     * death no player caused, one stone in four.
     */
    static List<ItemStack> roll(ServerLevel level, Mob mob, ServerPlayer killer, double scale) {
        List<ItemStack> out = new ArrayList<>();
        RandomSource rnd = level.getRandom();
        Dice dice = rnd::nextDouble;
        boolean juggernaut = mob.getTags().contains(SentientTags.JUGGERNAUT);
        if (killer == null) {
            add(out, STONE, StoneTable.ambient(scale, dice).stones(), STONE);
            return out;
        }
        int looting = looting(level, killer);
        StoneTable.Grade grade = StoneTable.gradeOf(Wildlife.tierOf(mob), mob.getMaxHealth(), mob.getTags().contains(SentientTags.FLOOR_BOSS), juggernaut);
        StoneTable.Haul haul = StoneTable.roll(grade, looting, scale, dice);
        add(out, STONE, haul.stones(), STONE);
        add(out, LOW, haul.low(), STONE);
        add(out, MEDIUM, haul.medium(), STONE);
        add(out, HIGH, haul.high(), STONE);
        if (juggernaut) {
            Item heart = SentientItems.item(MaterialTable.HEART);
            if (heart != null) out.add(new ItemStack(heart));
        } else {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (id.getNamespace().equals("tensura") || id.getNamespace().equals(com.newuniverse.nusmp.NUSMP.MODID)) {
                String material = MaterialTable.roll(id.getPath(), mob.getTags().contains(SentientTags.AQUA), looting, dice);
                Item item = material == null ? null : SentientItems.item(material);
                if (item != null) out.add(new ItemStack(item));
            }
        }
        return out;
    }

    /** Adds {@code count} of the item with this id (or of {@code fallback} if the first is not in this game), in stacks that fit. */
    private static void add(List<ItemStack> out, ResourceLocation id, int count, ResourceLocation fallback) {
        if (count <= 0) return;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) item = BuiltInRegistries.ITEM.getOptional(fallback).orElse(null);
        if (item == null) return;
        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        while (count > 0) {
            int n = Math.min(count, max);
            out.add(new ItemStack(item, n));
            count -= n;
        }
    }

    /** The Looting level of the weapon in the killer's main hand (0 if it has none). */
    private static int looting(ServerLevel level, ServerPlayer killer) {
        Optional<? extends Holder<Enchantment>> looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(LOOTING);
        return looting.isEmpty() ? 0 : Math.max(0, killer.getMainHandItem().getEnchantments().getLevel(looting.get()));
    }
}
