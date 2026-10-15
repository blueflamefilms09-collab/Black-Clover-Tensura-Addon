package com.newuniverse.nusmp.labyrinth.dnd.vital;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the "Vital Strike" of brief item 7, the answer to "D&amp;D's big HP pools against the anime's instant lethality": the buffer
 * ({@link HpBuffer}) makes a Labyrinth fight last, and this lets a heavy blow or a high-tier Relic cut through it. It is deliberately a
 * conversion of a hit, not a removal of the buffer: the handler only RAISES the amount of one hit (to {@code VITAL_EXECUTE} of the target's
 * maximum health, at least), so the buffer is still what a normal hit has to chew through and a weak party still has to fight.
 * <p>
 * The hook is NeoForge 21.1's {@link LivingIncomingDamageEvent} (the first of the three damage events: incoming, then the pipeline of
 * armour/resistance/absorption {@code DamageContainer} reductions, then {@code LivingDamageEvent.Post}). At this point
 * {@code getAmount()} is the raw amount before ANY of the target's mitigation and {@code setAmount} feeds the rest of the pipeline, so "the
 * target's own damage reduction still applies after it" holds with no extra code, and a mob with a resistance effect is not made
 * unkillable by a Vital Strike nor made one-shot by one. The old {@code LivingHurtEvent} of 1.20 does not exist in 21.1 and
 * {@code LivingDamageEvent.Post} is too late (the damage is already applied), so Incoming is the single correct place. The handler runs at
 * default priority and only ever raises the number, so it composes with the mod's other Incoming handlers (they may lower it afterwards).
 * <p>
 * Who counts: the attacker is {@code source.getEntity()}, the ROOT cause (the owner of an arrow or a thrown trident, the caster of a spell
 * that credits its caster), so a bow shot is a Vital Strike as well as a sword stroke; the direct entity is not needed. The target must be a
 * buffered non-boss {@link Mob} ({@link VitalMath#eligible}): bosses are excluded because they have their own balance, the Floor Warden its
 * 200 HP and the Zogratis/Zagred bosses a damage-per-second budget ({@code BudgetedBoss}): if the execution could reach them it would
 * skip that budget, which exists precisely to stop a single player hit from deleting a boss. Creative attackers are fine (the cap that
 * matters, the share of max health, applies to them as well).
 * <p>
 * Per-player state is two numbers (meter value + tick written, last strike tick) in a server-side map keyed by UUID, as the brief asks, not a
 * capability or attachment: it is deliberately forgotten on logout and on server stop, a meter that decays 5 % per second is worth nothing
 * to save. The spectacle is the sound {@code TRIDENT_THUNDER} pitched low, a burst of Rukh through {@code RukhFx} (server-side
 * {@code sendParticles} under the hood, so every nearby client sees it; the API clamps the count and a per-tick budget) and an actionbar line.
 */
public final class VitalStrike {
    private VitalStrike() {}

    /** Per-player state: meter value, the tick it was written, the tick of the last Vital Strike. */
    private record State(double meter, long meterTick, long lastVital) {}

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    /** Registers the handlers on the game bus (the lead calls this once from LabyrinthSystem). */
    public static void register(net.neoforged.bus.api.IEventBus gameBus) {
        gameBus.addListener(VitalStrike::onIncoming);
        gameBus.addListener(HpBuffer::onJoin);
        gameBus.addListener(VitalStrike::onLogout);
        gameBus.addListener(VitalStrike::onServerStopping);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { STATES.remove(e.getEntity().getUUID()); }

    public static void onServerStopping(ServerStoppingEvent e) { STATES.clear(); }

    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide() || !(target.level() instanceof ServerLevel level) || !Labyrinth.in(level)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(target instanceof Mob) || !DndConfig.get(DndConfig.VITAL)) return;
        boolean eligible = VitalMath.eligible(HpBuffer.isBoss(target), HpBuffer.isBuffered(target));
        if (!eligible) return;                                               // cheap exit before any relic lookup: most hits in the Labyrinth are on non-targets

        long now = level.getGameTime();
        State st = STATES.get(player.getUUID());
        double meter = st == null ? 0.0 : VitalMath.meterAt(st.meter(), st.meterTick(), now);
        long last = st == null ? Long.MIN_VALUE : st.lastVital();
        boolean onCooldown = VitalMath.cooldownLeft(last, now, DndConfig.get(DndConfig.VITAL_COOLDOWN_TICKS)) > 0L;

        float amount = e.getAmount();
        VitalMath.Outcome o = VitalMath.resolve(amount, target.getMaxHealth(), true, relicHit(player), meter, onCooldown,
                DndConfig.get(DndConfig.VITAL_THRESHOLD), DndConfig.get(DndConfig.VITAL_EXECUTE));
        boolean fired = o.vital();
        if (fired) e.setAmount(o.damage());
        if (fired || o.meter() > 0.0 || st != null) STATES.put(player.getUUID(), new State(o.meter(), now, fired ? now : last));
        if (fired) spectacle(level, player, target);
    }

    /** Is the player holding or wearing a relic whose grade reaches the configured minimum? Hands, armour and Curios (the relic cache, 10 ticks old at most). */
    private static boolean relicHit(ServerPlayer player) {
        int min = DndConfig.get(DndConfig.VITAL_RELIC_GRADE);
        for (Relics.Worn w : Relics.wornList(player)) {
            if (VitalMath.gradeQualifies(w.def().grade().ordinal(), min)) return true;
        }
        return false;
    }

    private static void spectacle(ServerLevel level, ServerPlayer player, LivingEntity target) {
        player.displayClientMessage(Component.translatable("message.nusmp.vital_strike"), true);
        if (!DndConfig.get(DndConfig.VITAL_SHOWY)) return;
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.9f, 0.6f);
        RukhFx.burst(level, at, RukhFx.Alignment.LIGHT, 12);      // a Vital Strike is a flash of gold: the LIGHT kind; RukhFx clamps and budgets it
    }
}
