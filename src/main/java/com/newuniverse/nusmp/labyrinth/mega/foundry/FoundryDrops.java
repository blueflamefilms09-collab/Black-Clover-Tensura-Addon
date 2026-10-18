package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.eco.EcoConfig;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionLoot;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the foundry's drops are the economy's, not a second economy. The standard listeners already pay what every monster of the dungeon pays (the 1.36.0 economy's Magic Stones and
 * meat, the 1.55.0 gear roll) to anything that is an {@code Enemy} or carries the Dungeon Master's tag, and a foundry mob is both, so they work unchanged. Two things are the
 * foundry's own. First, the FACTION extras: the faction drops ask the entity TYPE for its faction, which is useless for one type that is 400 mobs, so this listener asks the mob
 * (its definition names the faction) and adds exactly what {@code FactionDrops} would add: the faction's cut of meat and the threat-scaled stone bonus, by the same {@link FactionLoot}
 * rules and the same switches. Second, the MINION rule: a summoner's minions must not be a farm, so a listener at the highest priority empties their drops and cancels the event
 * before any other listener sees it. Both are server-side, in the Mega-Dungeon only, and never throw into the event bus.
 */
public final class FoundryDrops {
    private FoundryDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** Highest priority: a minion drops nothing and the event is cancelled so the economy and the gear roll never run for it. */
    public static void onMinionDrops(LivingDropsEvent e) {
        if (e.getEntity() instanceof FoundryMobEntity m && m.isMinion()) {
            e.getDrops().clear();
            e.setCanceled(true);
        }
    }

    /** Normal priority: the faction extras of a foundry mob that a player killed. */
    public static void onDrops(LivingDropsEvent e) {
        try {
            if (!(e.getEntity() instanceof FoundryMobEntity m) || !(m.level() instanceof ServerLevel level) || !Labyrinth.inMega(level) || m.isMinion()) return;
            if (!FoundryConfig.get(FoundryConfig.EXTRA_DROPS) || !DndConfig.get(DndConfig.DROPS) || !EcoConfig.get(EcoConfig.FACTION_DROPS)) return;
            var def = m.faction() == null ? null : FactionRegistry.graph().def(m.faction());
            if (def == null) return;
            ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : (m.getKillCredit() instanceof ServerPlayer lp ? lp : null);
            if (killer == null) return;
            Dice dice = level.getRandom()::nextDouble;
            int threat = Math.max(1, MegaDungeon.threatAt(level, m.blockPosition()));
            FactionLoot.Extra extra = FactionLoot.roll(def, "foundry_" + m.foundryId(), threat, 0, true, EcoConfig.get(EcoConfig.MAX_EXTRA_MEAT), EcoConfig.get(EcoConfig.STONE_THREAT_BONUS), dice);
            if (extra.isEmpty()) return;
            List<ItemStack> out = new ArrayList<>();
            if (extra.meat() > 0) {
                Item raw = DungeonFood.rawFor(extra.meatKind());
                if (raw != null) out.add(new ItemStack(raw, extra.meat()));
            }
            add(out, FactionLoot.STONE_PATH, extra.stones().stones());
            add(out, FactionLoot.LOW_PATH, extra.stones().low());
            add(out, FactionLoot.MEDIUM_PATH, extra.stones().medium());
            add(out, FactionLoot.HIGH_PATH, extra.stones().high());
            for (ItemStack s : out) e.getDrops().add(new ItemEntity(level, m.getX(), m.getY() + 0.3, m.getZ(), s));
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] foundry faction drops failed once and are skipped for that death", ex);
        }
    }

    private static void add(List<ItemStack> out, String path, int count) {
        if (count <= 0 || com.newuniverse.nusmp.elite.hub.Currency.stonesSuppressed()) return;      // 1.83.0 Hub economy: with Tensura coins as the currency the stones and crystals are replaced by coins (elite/hub)
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tensura", path);
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElseGet(() -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("tensura", FactionLoot.STONE_PATH)).orElse(null));
        if (item == null) return;
        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        while (count > 0) {
            int n = Math.min(count, max);
            out.add(new ItemStack(item, n));
            count -= n;
        }
    }
}
