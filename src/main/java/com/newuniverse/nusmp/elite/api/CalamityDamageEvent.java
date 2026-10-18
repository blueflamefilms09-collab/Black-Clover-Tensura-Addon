package com.newuniverse.nusmp.elite.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

import java.util.UUID;

/**
 * Why: the records subsystem (built in parallel by someone else) must know how much damage a player does to a calamity and how much a calamity does to a player, to keep records such as
 * "most damage to a calamity in one fight". The world engine does not import it: it fires this event on the GAME bus ({@code NeoForge.EVENT_BUS}) and the records builder listens.
 * Server thread only. Not cancellable. Fired after the damage was applied (the amounts are what really changed: after the boss's mitigation, the phase guard and the per-second damage budget).
 *
 * <p>Fields: {@link #calamityId()} the id of the calamity's data file; {@link #boss()} the boss entity (its UUID is {@link #bossId()}); {@link #player()} the player on the other side
 * (the attacker when {@link #toBoss()} is true, the victim otherwise); {@link #amount()} health points really lost (&gt; 0); {@link #toBoss()} true = a player hurt the calamity,
 * false = the calamity hurt a player; {@link #phase()} the boss's phase (0-based) when it happened; {@link #fightTicks()} ticks since the boss rose.
 * Damage dealt by a player's tamed or summoned creature is credited to its owner when the owner is a player.
 */
public final class CalamityDamageEvent extends Event {
    private final String calamityId;
    private final LivingEntity boss;
    private final ServerPlayer player;
    private final float amount;
    private final boolean toBoss;
    private final int phase;
    private final long fightTicks;

    public CalamityDamageEvent(String calamityId, LivingEntity boss, ServerPlayer player, float amount, boolean toBoss, int phase, long fightTicks) {
        this.calamityId = calamityId;
        this.boss = boss;
        this.player = player;
        this.amount = amount;
        this.toBoss = toBoss;
        this.phase = phase;
        this.fightTicks = fightTicks;
    }

    public String calamityId() { return calamityId; }

    public LivingEntity boss() { return boss; }

    public UUID bossId() { return boss.getUUID(); }

    public ServerPlayer player() { return player; }

    public float amount() { return amount; }

    public boolean toBoss() { return toBoss; }

    public int phase() { return phase; }

    public long fightTicks() { return fightTicks; }
}
