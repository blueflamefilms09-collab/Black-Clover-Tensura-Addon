package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: a second {@code LivingDropsEvent} listener that runs after the 1.36.0 economy ({@code EconomyDrops}, which stays untouched and keeps paying
 * every monster of the dimension) and adds only what the Mega-Dungeon makes different: the creature's FACTION cut of meat and a stone bonus that
 * grows with the sector's THREAT (the numbers are {@link FactionLoot}). A second listener rather than a change to the first because the base
 * economy is shared with the Spire and has its own tests, its own config switch and its own balance; the extras are added, never substituted
 * (vanilla loot and the base drops stay). It applies on exactly the base economy's terms: the Mega-Dungeon only, a hostile {@code Enemy} that is not a
 * boss, and (the faction part) a wild creature of a known faction killed by a player, so a monster that died to another monster in a turf war
 * is no farm. Both switches must be on: {@code dnd.drops} (the economy as a whole) and {@code eco.factionDropsEnabled}. The four stone item ids are
 * the base economy's, repeated because {@code EconomyDrops} keeps them private; a test fails if they ever differ.
 */
public final class FactionDrops {
    private FactionDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final ResourceLocation STONE = tensura(FactionLoot.STONE_PATH), LOW = tensura(FactionLoot.LOW_PATH),
            MEDIUM = tensura(FactionLoot.MEDIUM_PATH), HIGH = tensura(FactionLoot.HIGH_PATH);
    private static final ResourceKey<Enchantment> LOOTING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("looting"));

    private static ResourceLocation tensura(String path) { return ResourceLocation.fromNamespaceAndPath("tensura", path); }

    public static void onDrops(LivingDropsEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !Labyrinth.inMega(level) || !(mob instanceof Enemy || MegaDungeon.isDungeonMonster(mob))) return;
            if (!DndConfig.get(DndConfig.DROPS) || !EcoConfig.get(EcoConfig.FACTION_DROPS) || !TurfEligibility.wild(mob)) return;
            String faction = FactionRegistry.factionOf(mob.getType());
            FactionDef def = faction == null ? null : FactionRegistry.graph().def(faction);
            if (def == null) return;
            ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : (mob.getKillCredit() instanceof ServerPlayer lp ? lp : null);
            if (killer == null) return;
            Dice dice = level.getRandom()::nextDouble;
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
            int threat = MegaDungeon.threatAt(level, mob.blockPosition());
            FactionLoot.Extra extra = FactionLoot.roll(def, path, threat, looting(level, killer), true, EcoConfig.get(EcoConfig.MAX_EXTRA_MEAT),
                    EcoConfig.get(EcoConfig.STONE_THREAT_BONUS), dice);
            if (extra.isEmpty()) return;
            List<ItemStack> out = new ArrayList<>();
            if (extra.meat() > 0) {
                Item raw = DungeonFood.rawFor(extra.meatKind());
                if (raw != null) out.add(new ItemStack(raw, extra.meat()));
            }
            add(out, STONE, extra.stones().stones());
            add(out, LOW, extra.stones().low());
            add(out, MEDIUM, extra.stones().medium());
            add(out, HIGH, extra.stones().high());
            for (ItemStack s : out) e.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.3, mob.getZ(), s));
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] Mega-Dungeon faction drops failed once and are skipped for that death", ex);
        }
    }

    /** {@code count} of the item with this id, in stacks that fit; a missing crystal falls back to the Magic Stone and a missing stone to nothing, as in the base economy. */
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
