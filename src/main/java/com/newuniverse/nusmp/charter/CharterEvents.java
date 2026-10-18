package com.newuniverse.nusmp.charter;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The game-bus side of the Charter Grimoires. Call {@link #register} once from the mod constructor with {@code NeoForge.EVENT_BUS}.
 * <ul>
 *   <li><b>The first-kill drop</b>: when a boss of this mod (only the registry namespace {@code nusmp}, see {@link CharterTable#byEntity}) dies, the
 *       world's lock is read; if the grimoire is unclaimed and a player killed it, a configurable roll ({@link DropRoll}) decides. On success the lock
 *       is set in the same call (the first claim wins, all of it on the server thread), the book goes to the killer, everybody is told and every
 *       client is synced. The handler runs after everybody else's (a death another mod cancelled is not a kill), and the book is a separate item
 *       given to the killer, so bosses that clear their drops do not matter.</li>
 *   <li><b>Last attacker</b>: the last player to hurt a boss is remembered for thirty seconds, for kills by the world or by a mob.</li>
 *   <li><b>Mirroring</b>: the once-a-second pass of {@link CharterMirror}. <b>Sync</b>: the lock is sent at login.</li>
 * </ul>
 */
public final class CharterEvents {
    private CharterEvents() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static boolean registered;

    /** The last player to hurt a boss, by the boss's entity UUID, and the game time of the blow. */
    private record Hit(UUID player, long tick) {}

    private static final Map<UUID, Hit> LAST_HIT = new HashMap<>();
    private static final int MAX_TRACKED = 64;

    public static void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(EventPriority.LOWEST, CharterEvents::onDeath);
        bus.addListener(EventPriority.LOWEST, CharterEvents::onDamaged);
        bus.addListener(CharterEvents::onPlayerTick);
        bus.addListener(CharterEvents::onLogin);
        bus.addListener(CharterEvents::onLogout);
        bus.addListener(CharterEvents::onServerStopping);
    }

    // ================================================================ who is a boss
    private static CharterTable.Boss bossOf(LivingEntity e) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key == null ? null : CharterTable.byEntity(key.getNamespace(), key.getPath());
    }

    // ================================================================ the last attacker
    static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity target = e.getEntity();
        if (!(target.level() instanceof ServerLevel level) || bossOf(target) == null) return;
        ServerPlayer who = playerOf(level, e.getSource().getEntity());
        if (who == null) return;
        if (LAST_HIT.size() >= MAX_TRACKED) LAST_HIT.values().removeIf(h -> level.getGameTime() - h.tick() > KillerPick.FALLBACK_TICKS);
        if (LAST_HIT.size() < MAX_TRACKED || LAST_HIT.containsKey(target.getUUID())) LAST_HIT.put(target.getUUID(), new Hit(who.getUUID(), level.getGameTime()));
    }

    /** The player an attacker is: a player, or the owner of a pet or summon (when online). */
    private static ServerPlayer playerOf(ServerLevel level, Entity attacker) {
        if (attacker instanceof ServerPlayer p) return p;
        if (attacker instanceof OwnableEntity o && o.getOwnerUUID() != null && level.getServer() != null)
            return level.getServer().getPlayerList().getPlayer(o.getOwnerUUID());
        return null;
    }

    private static boolean eligible(Player p) { return !p.isCreative() && !p.isSpectator(); }

    // ================================================================ the drop
    static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        CharterTable.Boss boss = bossOf(dead);
        if (boss == null) return;
        MinecraftServer server = level.getServer();
        Hit hit = LAST_HIT.remove(dead.getUUID());
        if (server == null) return;
        ModWorldDropData data = ModWorldDropData.get(server);
        if (data.hasDropped(boss.id())) return;                                      // step 2: claimed already: nothing rolls, nothing is looked up

        ServerPlayer direct = playerOf(level, e.getSource().getEntity());
        KillerPick.Candidate d = direct == null ? null : new KillerPick.Candidate(direct.getUUID(), eligible(direct), 0L);
        KillerPick.Candidate recent = null;
        if (hit != null) {
            ServerPlayer rp = server.getPlayerList().getPlayer(hit.player());
            if (rp != null) recent = new KillerPick.Candidate(rp.getUUID(), eligible(rp), level.getGameTime() - hit.tick());
        } else if (dead.getLastHurtByMob() instanceof Player lp) {
            ServerPlayer rp = server.getPlayerList().getPlayer(lp.getUUID());
            if (rp != null) recent = new KillerPick.Candidate(rp.getUUID(), eligible(rp), dead.tickCount - dead.getLastHurtByMobTimestamp());
        }
        UUID winner = KillerPick.choose(d, recent);
        ServerPlayer killer = winner == null ? null : server.getPlayerList().getPlayer(winner);

        double chance = DropRoll.chance(boss.chance(), CharterConfig.dropChance());
        DropRoll.Result r = DropRoll.decide(CharterConfig.enabled(), false, killer != null, chance, level.random::nextDouble);
        if (r != DropRoll.Result.DROP) return;
        net.minecraft.world.item.Item item = CharterItems.item(boss.id());
        if (item == null) return;                                                    // the book must exist before the lock is set: a lock without a book would be a lost grimoire
        ItemStack stack = new ItemStack(item);
        if (!data.markDropped(boss.id(), killer.getUUID(), killer.getGameProfile().getName())) return;   // first wins: the lock is checked and set in one call
        try {
            server.overworld().getDataStorage().save();                              // written now, not at the next autosave: a crash must not lose the lock while the book is already in an inventory
        } catch (RuntimeException saveFailed) {
            LOG.warn("[nusmp] could not write the Canon lock at once ({}); it is saved with the world", saveFailed.toString());
        }
        award(server, level, data, boss, killer, stack);
    }

    private static void award(MinecraftServer server, ServerLevel level, ModWorldDropData data, CharterTable.Boss boss, ServerPlayer killer, ItemStack stack) {
        if (!killer.addItem(stack)) {                         // a full inventory: at their feet, where it cannot despawn or burn
            ItemEntity drop = new ItemEntity(killer.serverLevel(), killer.getX(), killer.getY() + 0.5, killer.getZ(), stack);
            drop.setPickUpDelay(0);
            drop.setUnlimitedLifetime();
            drop.setInvulnerable(true);
            killer.serverLevel().addFreshEntity(drop);
        }
        LOG.info("[nusmp] {} claimed the Canon Grimoire of {} (day {})", killer.getGameProfile().getName(), boss.id(), data.claim(boss.id()).day());
        server.getPlayerList().broadcastSystemMessage(Component.literal("[Server] " + killer.getGameProfile().getName()
                + " is the first to claim the Canon Grimoire of " + boss.name() + ".").withStyle(ChatFormatting.GOLD), false);
        CharterNet.sendAll(data);
    }

    // ================================================================ players
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) CharterMirror.pass(p);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        CharterNet.sendTo(p, ModWorldDropData.get(p.getServer()));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { CharterMirror.forget(e.getEntity().getUUID()); }

    static void onServerStopping(ServerStoppingEvent e) {
        LAST_HIT.clear();
        CharterMirror.clear();
    }

    // ================================================================ for the command
    /** Gives a grimoire without touching the lock. False if the id is unknown. */
    static boolean give(ServerPlayer p, String bossId) {
        net.minecraft.world.item.Item it = CharterItems.item(bossId);
        if (it == null) return false;
        ItemStack stack = new ItemStack(it);
        if (!p.addItem(stack)) {
            ItemEntity drop = new ItemEntity(p.serverLevel(), p.getX(), p.getY() + 0.5, p.getZ(), stack);
            drop.setPickUpDelay(0);
            p.serverLevel().addFreshEntity(drop);
        }
        return true;
    }
}
