package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;
import com.newuniverse.nusmp.relics.djinn.dark.DarkRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Where Djinn Seals come from (the acquisition economy; the doc has the reasoning):
 * <ul>
 *   <li><b>The floor bosses.</b> The first time each of the Dungeon's four floor bosses falls in a world (Goliath, the Charybdis Spawn, the Devil
 *       Avatar, the Blood-Red Commander) it pays one random Seal of a Djinn nobody holds and no Seal exists for. Heard through the
 *       {@link DungeonCollapseEvent} every one of them posts. The killer receives it (a Seal in an inventory is safe from the collapse).</li>
 *   <li><b>A vault chest</b>, very rarely ({@link HouseholdRules#VAULT_CHANCE}): {@link #vaultSeal}, for the vault / Treasury code to call.</li>
 *   <li><b>Between players</b>: a Seal is an item; it is dropped, traded and, when an owner dies or releases, comes back into the world.</li>
 *   <li><b>An admin</b>: /multiverse djinn grant.</li>
 *   <li><b>The Dark Rukh Shard</b> (apart from the Seals): the Blood-Red Commander and the Devil Avatar drop one to the killer with a 15% chance on every
 *       kill, Lucius likewise ({@link DarkDjinn#onLuciusDeath}), and a vault chest that holds no Seal has a 2% chance of one.</li>
 * </ul>
 * A Seal that nobody has seen for a week of running time lets its Djinn sleep again, so none is lost for good ({@link #onServerTick}).
 */
public final class DjinnSources {
    private DjinnSources() {}

    // ---------------------------------------------------------------- the floor bosses
    public static void onCollapse(DungeonCollapseEvent e) {
        if (!e.bossDefeated() || e.zone() == null) return;
        if (DarkRules.isShardZone(e.zone().name()))                                                     // the Dark Rukh Shard: a roll on every kill, apart from the Seal
            DarkDjinn.rollBossShard(e.level(), e.cause(), Vec3.atCenterOf(e.centre()).add(0, 1, 0), DarkRules.bossName(e.zone().name()));
        if (!HouseholdRules.isSealBoss(e.zone().name())) return;
        ServerLevel level = e.level();
        MinecraftServer server = level.getServer();
        if (server == null) return;
        DjinnLedger ledger = DjinnLedger.get(server);
        String zone = e.zone().name();
        if (ledger.zoneRewarded(zone)) return;
        ledger.markZone(zone);                                              // the boss has paid, even if every Djinn is taken
        Djinn d = HouseholdRules.pickSleeping(ledger.sleeping(), level.getRandom().nextDouble());
        if (d == null) return;
        long now = server.overworld().getGameTime();
        ledger.sealIssued(d, level.dimension().location().toString(), e.centre(), now);
        ItemStack seal = DjinnItems.sealStack(d);
        ServerPlayer cause = e.cause();
        if (cause != null && cause.isAlive()) {
            cause.getInventory().placeItemBackInInventory(seal);
            cause.sendSystemMessage(Component.literal("A Djinn Seal comes loose from the fallen guardian and settles in your hands: " + d.fullName() + ".").withColor(d.color()));
        } else {
            dropSeal(level, Vec3.atCenterOf(e.centre()).add(0, 1, 0), seal);
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("A Djinn stirs in the Dungeon: the Seal of " + d.displayName() + " has been given up.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    // ---------------------------------------------------------------- vaults
    /**
     * A Seal for a vault or Treasury chest, or EMPTY: a 3% chance, and only if some Djinn still sleeps. Marks the Djinn as having a Seal in the
     * world; the caller must put the stack into the chest (or the loot). Call it once per chest filled.
     */
    public static ItemStack vaultSeal(ServerLevel level, RandomSource random) {
        ItemStack seal = rollVaultSeal(level, random);
        return seal.isEmpty() ? DarkDjinn.vaultShard(random) : seal;                                    // no Seal: a 2% chance of a Dark Rukh Shard in the same slot
    }

    private static ItemStack rollVaultSeal(ServerLevel level, RandomSource random) {
        MinecraftServer server = level.getServer();
        if (server == null || random.nextDouble() >= HouseholdRules.VAULT_CHANCE) return ItemStack.EMPTY;
        DjinnLedger ledger = DjinnLedger.get(server);
        Djinn d = HouseholdRules.pickSleeping(ledger.sleeping(), random.nextDouble());
        if (d == null) return ItemStack.EMPTY;
        ledger.sealIssued(d, level.dimension().location().toString(), null, server.overworld().getGameTime());
        return DjinnItems.sealStack(d);
    }

    // ---------------------------------------------------------------- dropping
    /** A Seal on the ground: it cannot be burned, blown up or despawn, glows so it can be found, and waits a moment before it can be picked up. */
    public static ItemEntity dropSeal(ServerLevel level, Vec3 at, ItemStack seal) {
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, seal);
        item.setUnlimitedLifetime();
        item.setInvulnerable(true);
        item.setGlowingTag(true);
        item.setPickUpDelay(40);
        item.setDeltaMovement(0, 0.25, 0);
        level.addFreshEntity(item);
        return item;
    }

    // ---------------------------------------------------------------- keeping count of the Seals
    /** Every ten seconds: the Seals players carry are seen; every five minutes: the Seals nobody has seen for a week let their Djinn sleep. */
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        long now = server.overworld().getGameTime();
        if (now % 200 == 11) {
            DjinnLedger ledger = DjinnLedger.get(server);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                String dim = p.level().dimension().location().toString();
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    Djinn d = DjinnItems.djinnOf(p.getInventory().getItem(i));
                    if (d != null) ledger.seen(d, now, dim, p.blockPosition());
                }
            }
        }
        if (now % 6000 == 23) DjinnLedger.get(server).sweepLost(now);
    }

    /** A chest or any container opened: a Seal in it is seen. */
    public static void onContainerOpen(PlayerContainerEvent.Open e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        AbstractContainerMenu menu = e.getContainer();
        DjinnLedger ledger = null;
        for (Slot s : menu.slots) {
            Djinn d = DjinnItems.djinnOf(s.getItem());
            if (d == null) continue;
            if (ledger == null) ledger = DjinnLedger.get(p.getServer());
            ledger.seen(d, p.getServer().overworld().getGameTime(), p.level().dimension().location().toString(), p.blockPosition());
        }
    }

    /** For the status command: the sleeping Djinn, by name. */
    static List<Djinn> sleeping(MinecraftServer server) { return DjinnLedger.get(server).sleeping(); }
}
