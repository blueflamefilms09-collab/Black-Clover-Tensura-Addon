package com.newuniverse.nusmp.bossdrops2;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.bossdrops2.OneOfOne.Outcome;
import com.newuniverse.nusmp.bossdrops2.SignaturePlan.Award;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * Why: the two small hooks of the worthwhile boss drops (docs/boss_drops_worthwhile.md), applied from OUTSIDE the boss classes (no boss entity is edited, so it also covers the boss whose class belongs to
 * somebody else, and Goku's normal death path):
 * <ul>
 *   <li>{@code LivingDamageEvent.Post}: a player's hit on a boss is added to the {@link DamageLedger} (bounded, see there). Everything else is dropped by two cheap checks first: a living victim that is
 *       not a player, and a damage source that is a player.</li>
 *   <li>{@code LivingDeathEvent} (lowest priority, like the older boss drops: a death another mod cancelled is no kill): the boss's table ({@link SignatureData}) is read, the fighters chosen
 *       ({@link SignaturePlan#select}), the one-of-one asked of the relic ledger ({@link OneOfOne}), the pieces planned ({@link SignaturePlan#plan}) and handed over to each player's inventory, the rest at
 *       their feet. A boss entity is paid at most once.</li>
 * </ul>
 * Cost: per hit on a boss one table lookup and one ledger add; per kill one plan of at most {@link SignaturePlan#MAX_PIECES} pieces, one relic ledger lookup, at most {@link SignaturePlan#MAX_FIGHTERS}
 * system messages (the vanilla chat), no tick work, no packet of its own. The whole class does nothing while {@code bossDrops.worthwhile} is false.
 */
public final class SignatureEvents {
    private SignatureEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final DamageLedger LEDGER = new DamageLedger();
    private static boolean registered;

    public static void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(SignatureEvents::onDamage);
        bus.addListener(EventPriority.LOWEST, SignatureEvents::onDeath);
        bus.addListener((ServerStoppedEvent e) -> LEDGER.clear());
    }

    /** The table that pays for this entity: its own (by entity type path, of this mod) else the default one when the entity is a boss by tag; null when it is no boss. */
    static SignatureTable.Table tableFor(LivingEntity e) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (key != null && "anime_multiverse".equals(key.getNamespace())) {
            SignatureTable.Table t = SignatureData.table(key.getPath());
            if (t != null) return t;
        }
        return BossIdentity.isBoss(e) ? SignatureData.table(BossSigCatalog.DEFAULT) : null;
    }

    static void onDamage(LivingDamageEvent.Post e) {
        if (!SignatureConfig.worthwhile()) return;
        LivingEntity victim = e.getEntity();
        if (victim instanceof Player || victim.level().isClientSide) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer hitter)) return;
        float dmg = e.getNewDamage();
        if (!(dmg > 0.0f) || tableFor(victim) == null) return;
        LEDGER.add(victim.getUUID(), hitter.getUUID(), dmg);
    }

    static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || dead instanceof Player || !SignatureConfig.worthwhile()) return;
        SignatureTable.Table table = tableFor(dead);
        if (table == null || !LEDGER.claim(dead.getUUID())) return;
        Map<UUID, Double> dealt = LEDGER.take(dead.getUUID());
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ServerPlayer killer = LootVictims.killer(e.getSource(), dead);
        Map<UUID, ServerPlayer> players = new HashMap<>();
        Map<UUID, Double> damage = new HashMap<>();
        for (Map.Entry<UUID, Double> d : dealt.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(d.getKey());
            if (eligible(p)) {
                players.put(p.getUUID(), p);
                damage.put(p.getUUID(), d.getValue());
            }
        }
        if (eligible(killer)) players.put(killer.getUUID(), killer);
        Set<UUID> present = new HashSet<>();
        if (dead instanceof ZogratisBoss z) {
            for (UUID id : z.fighters()) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (eligible(p) && p.level() == level) {
                    players.putIfAbsent(p.getUUID(), p);
                    present.add(p.getUUID());
                }
            }
        }
        List<Fighter> fighters = SignaturePlan.select(damage, present, eligible(killer) ? killer.getUUID() : null, dead.getMaxHealth());
        if (fighters.isEmpty()) return;
        pay(server, level, table, dead.getDisplayName(), fighters, players, level.random::nextDouble);
    }

    private static boolean eligible(ServerPlayer p) { return p != null && !p.isCreative() && !p.isSpectator(); }

    /**
     * Plans and hands over one kill; returns every stack that was given (for the self-test and the log). {@code fighters} comes from {@link SignaturePlan#select}, {@code players} maps each of them to
     * the online player. The relic ledger is asked only when the table has a valid one-of-one.
     */
    static List<ItemStack> pay(MinecraftServer server, ServerLevel level, SignatureTable.Table table, Component bossName, List<Fighter> fighters, Map<UUID, ServerPlayer> players, DoubleSupplier rng) {
        List<ItemStack> given = new ArrayList<>();
        if (fighters.isEmpty()) return given;
        ItemStack[] relic = {ItemStack.EMPTY};
        Outcome outcome = OneOfOne.resolve(table.hasUnique() && relicValid(table.unique().relic()), () -> {
            RelicDef def = Relics.byId(table.unique().relic());
            ItemStack s = RelicCatalog.newStack(def);
            if (s.isEmpty()) return false;
            if (!UniqueLedger.issue(level, def.id(), s)) return false;
            relic[0] = s;
            return true;
        });
        if (outcome == Outcome.ISSUED && relic[0].isEmpty()) outcome = Outcome.EXISTS;           // the relic item is not registered: nothing was made, pay the echo
        List<Award> awards = SignaturePlan.plan(table, fighters, outcome, rng);
        Map<UUID, Award> best = new HashMap<>();
        for (Award a : awards) {
            ServerPlayer p = players.get(a.player());
            ItemStack stack = stackOf(a, table, relic[0]);
            if (p == null || stack.isEmpty()) continue;
            give(p, stack);
            given.add(stack);
            best.merge(a.player(), a, (old, now) -> rank(now) > rank(old) ? now : old);
        }
        for (Fighter f : fighters) {
            ServerPlayer p = players.get(f.id());
            if (p == null) continue;
            Award a = best.get(f.id());
            if (a != null && a.kind() == SignaturePlan.Kind.UNIQUE) p.sendSystemMessage(Component.translatable("bossdrops2.msg.unique", bossName, relic[0].getHoverName()).withStyle(ChatFormatting.GOLD));
            else if (a != null && a.kind() == SignaturePlan.Kind.ECHO) p.sendSystemMessage(Component.translatable("bossdrops2.msg.echo", Component.translatable("item.anime_multiverse." + table.unique().relic())).withStyle(ChatFormatting.LIGHT_PURPLE));
            else if (a != null) p.sendSystemMessage(Component.translatable("bossdrops2.msg.share", bossName).withStyle(ChatFormatting.GRAY));
        }
        if (outcome == Outcome.ISSUED) UniqueLedger.scan(server);
        if (!given.isEmpty()) LOG.info("[anime_multiverse] boss {} paid {} piece(s) to {} fighter(s), one-of-one: {}", table.boss(), given.size(), fighters.size(), outcome);
        return given;
    }

    private static int rank(Award a) {
        return switch (a.kind()) {
            case UNIQUE -> 3;
            case ECHO -> 2;
            case SIGNATURE -> 1;
            default -> 0;
        };
    }

    /** A relic is a valid one-of-one when it is registered, unique in the world and not one half of a pair (a pair is issued by its own grant). */
    static boolean relicValid(String id) {
        RelicDef d = Relics.byId(id);
        return d != null && d.isUnique() && !d.isPairHalf();
    }

    private static ItemStack stackOf(Award a, SignatureTable.Table table, ItemStack relic) {
        switch (a.kind()) {
            case UNIQUE:
                return relic;
            case ECHO:
                return BossSigItems.echo(table.boss(), table.hasUnique() ? table.unique().relic() : "");
            case SIGNATURE:
                return BossSigItems.signature(table.boss(), table.tier(), a.count());
            default:
                ResourceLocation id = ResourceLocation.tryParse(a.item());
                Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null || item == net.minecraft.world.item.Items.AIR) {
                    LOG.warn("[anime_multiverse] boss signature extra {} of {} names an item that does not exist and is skipped", a.item(), table.boss());
                    return ItemStack.EMPTY;
                }
                return new ItemStack(item, Math.min(a.count(), new ItemStack(item).getMaxStackSize()));
        }
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (p.addItem(stack.copy())) return;
        ItemEntity drop = new ItemEntity(p.serverLevel(), p.getX(), p.getY() + 0.5, p.getZ(), stack.copy());
        drop.setPickUpDelay(0);
        p.serverLevel().addFreshEntity(drop);
    }
}
