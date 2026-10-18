package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.SummonRules;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the rebuilt skills need a little upkeep outside any button press: lingering zones (blizzard, flame pillars, gravity) must pulse,
 * summons must be steered, expired or cleaned up when their owner dies or leaves, and zones of players who left must be forgotten. This
 * class is that upkeep and nothing else. NO extra wiring is needed: the two listeners the mod already registers hand their events to it
 * ({@link SkillEvolution#onPlayerTick} calls {@link #onPlayerTick}, {@link LiarisFreeseSkill#onKill} calls {@link #onDeath}). If the lead
 * prefers to register these two methods directly on the event bus instead, remove those two calls so nothing runs twice (running twice is
 * harmless but wasteful).
 */
public final class SkillEvents {
    private SkillEvents() {}

    /** Every player tick: pulses the player's zones, and once a second looks after the summons around them. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
        try {
            SkillZones.tick(player);
            if (player.tickCount % 20 == 7) {
                // a Summoner / Lord of Summons owner gets their summons steered every second; everybody else only
                // has expired or ownerless summons swept up, every five seconds (one entity query each, so it stays cheap)
                int cap = SummonerSkill.capFor(player);
                if (cap < SummonRules.HARD_CAP || player.tickCount % 100 == 7) Summons.tick(player, player.getServer(), cap);
            }
            if (player.tickCount % 200 == 11) SkillZones.sweep(player.getServer());
        } catch (RuntimeException | LinkageError upkeep) {
            // upkeep is never allowed to crash the server tick
        }
    }

    /**
     * A player dies: their summons and zones end with them. The check runs one tick later, because a skill such as Unbreakable Will may
     * cancel the death and stand the player back up in this very event: only a player who is really dead then loses their summons.
     */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        var server = player.getServer();
        if (server == null) return;
        server.execute(() -> {
            if (player.isAlive()) return;                                   // the death was refused
            Summons.recall(player);
            SkillZones.clear(player.getUUID());
        });
    }

    /** The server is stopping: forget every zone (summons are saved with their owner and lifetime, and swept on the next start). */
    public static void onServerStopping() {
        SkillZones.clearAll();
        Summons.clearLedger();
    }
}
