package com.newuniverse.nusmp.elite.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the second half of the report to the records subsystem: a calamity fell. Fired once per calamity on the GAME bus ({@code NeoForge.EVENT_BUS}), server thread, not cancellable, after the
 * boss's death was confirmed (another mod cancelling the death means no event).
 *
 * <p>Fields: {@link #calamityId()} the id of the data file; {@link #boss()} the dead entity; {@link #killer()} the player who dealt the last blow (null when it was not a player: a tamed
 * creature's owner counts as the player, an environmental death leaves it null); {@link #participants()} every player who damaged it, with the health they removed (UUID to damage, the sum is
 * about the boss's max health); {@link #onlinePlayers()} the participants that are online now (to give them something); {@link #fightTicks()} how long the fight lasted (ticks from the rise);
 * {@link #phaseCount()} the number of phases the boss had; {@link #noDamageTaken()} true when no player lost health to the boss during the whole fight (a "flawless" fight);
 * {@link #soloist()} true when exactly one player took part.
 */
public final class CalamityKillEvent extends Event {
    private final String calamityId;
    private final LivingEntity boss;
    private final ServerPlayer killer;
    private final Map<UUID, Float> participants;
    private final List<ServerPlayer> online;
    private final long fightTicks;
    private final int phaseCount;
    private final boolean noDamageTaken;

    public CalamityKillEvent(String calamityId, LivingEntity boss, ServerPlayer killer, Map<UUID, Float> participants, List<ServerPlayer> online, long fightTicks, int phaseCount,
                             boolean noDamageTaken) {
        this.calamityId = calamityId;
        this.boss = boss;
        this.killer = killer;
        this.participants = Map.copyOf(participants);
        this.online = List.copyOf(online);
        this.fightTicks = fightTicks;
        this.phaseCount = phaseCount;
        this.noDamageTaken = noDamageTaken;
    }

    public String calamityId() { return calamityId; }

    public LivingEntity boss() { return boss; }

    public ServerPlayer killer() { return killer; }

    public Map<UUID, Float> participants() { return participants; }

    public List<ServerPlayer> onlinePlayers() { return online; }

    public long fightTicks() { return fightTicks; }

    public int phaseCount() { return phaseCount; }

    public boolean noDamageTaken() { return noDamageTaken; }

    public boolean soloist() { return participants.size() == 1; }
}
