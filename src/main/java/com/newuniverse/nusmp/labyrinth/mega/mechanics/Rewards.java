package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: a mechanic that was beaten pays out, and what it pays is data on the mechanic ({@code reward}: loot, xp, heal, mana). The loot is the dungeon's OWN trial table
 * ({@code nusmp:chests/labyrinth_trial}: ingots, arrows, bread, experience bottles, magic stones - never a relic, which only a Crucible may hand out in the Mega-Dungeon), rolled
 * once per player present, popped at the anchor so nothing is lost to a full inventory; experience scales with depth; healing and mana go to the players in the area. Shared by the
 * counters and by the {@code give_loot} primitive.
 */
final class Rewards {
    private Rewards() {}

    static final ResourceLocation TABLE = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/labyrinth_trial");

    static void give(Instance in, ServerPlayer by) {
        List<ServerPlayer> who = in.inside.stream().filter(Safety::affects).toList();
        if (who.isEmpty() && by != null) who = List.of(by);
        switch (in.def.reward()) {
            case "loot" -> {
                for (ServerPlayer p : who) loot(in.level, TABLE, 1, in.anchor, p, in.rng.nextLong());
                if (who.isEmpty()) loot(in.level, TABLE, 1, in.anchor, null, in.rng.nextLong());
            }
            case "xp" -> xp(in.level, in.anchor, (int) (14 + 9 * Scaling.s(in.floor)) * Math.max(1, who.size()));
            case "heal" -> { for (ServerPlayer p : who) p.heal(p.getMaxHealth() * 0.5f); }
            case "mana" -> { for (ServerPlayer p : who) Mana.add(p, 0.3); }
            default -> { }
        }
        Rukh.burst(in, "light", "column", 10);
        in.level.playSound(null, in.anchor, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.2f);
    }

    /** Rolls a loot table and pops the stacks at the position. */
    static void loot(ServerLevel level, ResourceLocation table, int rolls, BlockPos at, ServerPlayer luck, long seed) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, table);
        LootTable t = level.getServer().reloadableRegistries().getLootTable(key);
        for (int i = 0; i < Math.max(1, rolls); i++) {
            LootParams.Builder b = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at));
            if (luck != null) b.withLuck(luck.getLuck()).withOptionalParameter(LootContextParams.THIS_ENTITY, luck);
            for (ItemStack s : t.getRandomItems(b.create(LootContextParamSets.CHEST), seed + i)) {
                if (s != null && !s.isEmpty()) Block.popResource(level, at.above(), s);
            }
        }
    }

    static void xp(ServerLevel level, BlockPos at, int amount) {
        ExperienceOrb.award(level, Vec3.atCenterOf(at.above()), Math.max(1, amount));
    }
}
