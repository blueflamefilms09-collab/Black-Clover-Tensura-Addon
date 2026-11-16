package com.newuniverse.nusmp.bossdrops;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the one small hook of the boss drop framework (docs/bosses/boss_drops.md). When a boss of this mod dies, the table of its id ({@link BossDropData}) is rolled once ({@link BossDropTable#roll}) and the
 * result goes to the player of the last blow ({@link LootVictims#killer}: the attacker, else the player the game credits), straight into their inventory, else at their feet, so a boss that clears its own
 * loot list does not matter (the Charter Grimoire works the same way). Only the killer is paid: nobody else, no "top damager" share. Runs once per boss death on the server thread: no per-tick work, no
 * packet, one saved-data lookup for a first-kill row, at most {@link BossDropTable#MAX_PIECES} item pieces. Creative and spectator killers get nothing (no farming by commands).
 */
public final class BossDropEvents {
    private BossDropEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean registered;

    public static void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(EventPriority.LOWEST, BossDropEvents::onDeath);
    }

    /** The boss id of an entity of this mod: the Charter id when it has one, else the registry path; null for anything outside the namespace. */
    static String bossId(LivingEntity e) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (key == null || !"anime_multiverse".equals(key.getNamespace())) return null;
        CharterTable.Boss b = CharterTable.byEntity(key.getNamespace(), key.getPath());
        return b == null ? key.getPath() : b.id();
    }

    static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !BossDropConfig.enabled()) return;
        String id = bossId(dead);
        if (id == null) return;
        BossDropTable.Table table = BossDropData.table(id);
        if (table == null) return;
        ServerPlayer killer = LootVictims.killer(e.getSource(), dead);
        MinecraftServer server = level.getServer();
        if (killer == null || server == null || killer.isCreative() || killer.isSpectator()) return;
        pay(server, level, killer, table, level.random::nextDouble);
    }

    /** Rolls the table for this killer and hands the result over; returns what was paid (for the self-test and the log). */
    static List<ItemStack> pay(MinecraftServer server, ServerLevel level, ServerPlayer killer, BossDropTable.Table table, java.util.function.DoubleSupplier rng) {
        boolean first = hasFirstKillRow(table) && BossDropLedger.get(server).firstKill(killer.getUUID(), table.boss());
        List<BossDropTable.Stack> rolled = BossDropTable.roll(table, first, BossDropConfig.chanceScale(), rng);
        int cap = Math.min(BossDropConfig.stackCap(), table.maxStacks());
        if (rolled.size() > cap) rolled = rolled.subList(0, cap);
        List<ItemStack> given = new ArrayList<>();
        int room = BossDropTable.MAX_PIECES;
        for (BossDropTable.Stack s : rolled) {
            Optional<Item> item = Optional.ofNullable(ResourceLocation.tryParse(s.item())).flatMap(BuiltInRegistries.ITEM::getOptional);
            if (item.isEmpty() || item.get() == net.minecraft.world.item.Items.AIR) {
                LOG.warn("[anime_multiverse] boss drop {} of {} names an item that does not exist and is skipped", s.item(), table.boss());
                continue;
            }
            for (int n : BossDropTable.split(s.count(), new ItemStack(item.get()).getMaxStackSize(), room)) {
                ItemStack stack = new ItemStack(item.get(), n);
                room--;
                if (!killer.addItem(stack.copy())) {
                    ItemEntity drop = new ItemEntity(killer.serverLevel(), killer.getX(), killer.getY() + 0.5, killer.getZ(), stack.copy());
                    drop.setPickUpDelay(0);
                    killer.serverLevel().addFreshEntity(drop);
                }
                given.add(stack);
            }
        }
        if (!given.isEmpty()) LOG.info("[anime_multiverse] {} got {} boss drop piece(s) from {}", killer.getGameProfile().getName(), given.size(), table.boss());
        return given;
    }

    private static boolean hasFirstKillRow(BossDropTable.Table t) {
        for (BossDropTable.Row r : t.rows()) if (r.firstKillOnly()) return true;
        return false;
    }
}
