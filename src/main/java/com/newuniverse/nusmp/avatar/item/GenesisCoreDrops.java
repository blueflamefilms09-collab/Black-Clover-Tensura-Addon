package com.newuniverse.nusmp.avatar.item;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarPlayerData;
import com.newuniverse.nusmp.avatar.AvatarSystem;
import com.newuniverse.nusmp.quests.QuestHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Set;

/**
 * Why: the Genesis Core must drop exclusively from the dungeon's boss rooms (owner section 1) and nowhere else. Those bosses are the Goliath of the pit,
 * Charybdis of the basin, the Labyrinth's Floor Warden and the Peak boss, plus anything a dungeon director tags as a floor boss. Each player who took part
 * in the fight is guaranteed one core the first time (a pity record kept in their {@link AvatarPlayerData}), and after that gets one with the configured
 * chance ({@code avatar.genesisCoreDropChance}, 15%). The core goes straight into the player's inventory (or at their feet when it is full), so it is never
 * lost to a despawning drop and never given to someone who only stood far away.
 */
public final class GenesisCoreDrops {
    private GenesisCoreDrops() {}

    /** Entity type paths of the dungeon boss rooms (the same ids the quest hooks use). */
    private static final Set<String> BOSS_TYPES = Set.of("goliath", "charybdis_spawn", "floor_warden");
    /** Persistent tags the dungeon directors and the Peak Altar put on their bosses. */
    private static final Set<String> BOSS_TAGS = Set.of("nusmp_floor_boss", "nusmp_juggernaut", "nusmp_charybdis", "nusmp_peak_boss");

    public static boolean isDungeonBoss(LivingEntity e) {
        if (e == null || e instanceof Player) return false;
        try {
            if (e.getTags().contains("nusmp_wanderer")) return false;       // 1.54.0: a wandering Floor Warden of the Mega-Dungeon is not a boss-room boss (Genesis Cores drop from boss rooms only)
            if (BOSS_TYPES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath())) return true;
            for (String t : BOSS_TAGS) if (e.getTags().contains(t)) return true;
        } catch (RuntimeException unreadable) { /* not provably a dungeon boss: not one */ }
        return false;
    }

    public static void onDeath(LivingDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (dead.level().isClientSide || !AvatarConfig.enabled() || !isDungeonBoss(dead)) return;
            for (ServerPlayer p : QuestHooks.participants(dead, e.getSource())) maybeGive(p);
        } catch (Throwable t) {
            // a drop must never disturb a boss death
        }
    }

    private static void maybeGive(ServerPlayer p) {
        if (p.isSpectator()) return;
        AvatarPlayerData d = p.getData(AvatarSystem.PLAYER_DATA);
        boolean first = !d.coreGranted();
        if (!first && p.getRandom().nextDouble() >= AvatarConfig.genesisCoreDropChance()) return;
        if (first) p.setData(AvatarSystem.PLAYER_DATA, d.withCoreGranted());
        ItemStack core = AvatarItems.coreStack();
        if (!p.getInventory().add(core)) p.drop(core, false);
        p.sendSystemMessage(Component.literal("A Genesis Core condenses from the fallen guardian and settles in your hands.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
