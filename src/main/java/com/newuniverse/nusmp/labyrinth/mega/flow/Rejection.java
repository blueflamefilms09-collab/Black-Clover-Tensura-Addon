package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: "if they cast opposing magic, the environment violently rejects it, draining their health" has to be something a player FEELS and understands at
 * once, and something that can never become a way to die by accident or to farm. This is the game-side half of that, the one place that applies a
 * rejection; what it costs and when is decided by the pure {@link FlowRules}. The price is taken as true damage by setting the health directly
 * (the safest choice: armour, resistance and Totems cannot dodge it, and the non-lethal floor is exact) with the hurt flinch broadcast so it looks
 * like a blow; only a drain that would really kill (possible only when the owner turns {@code nonLethal} off) goes through {@code hurt}, the normal death
 * path, because setting a player's health to zero by hand leaves a half-dead player. DECISION, on purpose: the drain is ABSOLUTE. The place's rejection is
 * not a spell, so the Rare grade's magic resistance of the gear module (which reduces vanilla {@code magic} and Tensura's spell types by up to 40 percent)
 * must not shrink it; a damage event of the {@code magic} type would be softened by that gear and by the Resistance effect. Setting the health raises no
 * damage event at all, and the lethal case uses {@code generic_kill}, which no armour, resistance, gear or Totem touches (vanilla's tags
 * bypasses_armor, bypasses_resistance and bypasses_invulnerability all list it; checked in the 1.21.1 data), so a rejection costs exactly what
 * {@link FlowRules#rejectionDrain} says. Creative and spectator players, and the dead, are skipped. The feedback is the place speaking: an
 * action-bar line, the rejected magic's Rukh scattering away from the player while the place's own Rukh close in on them, and two sounds, all through
 * {@code RukhFx} (so the particle budget applies) and with no vanilla particle at all.
 */
final class Rejection {
    private Rejection() {}

    /**
     * Rejects one cast of {@code cast} alignment in a place whose flow is {@code flow}. Returns true when the rejection was applied, false when the player is
     * exempt or the cooldown still runs. Server thread; never throws for a living player.
     */
    static boolean apply(ServerPlayer p, Alignment cast, Alignment flow) {
        if (p.isCreative() || p.isSpectator() || !p.isAlive()) return false;
        ServerLevel level = p.serverLevel();
        if (!FlowTracker.shared().tryReject(p.getUUID(), level.getGameTime(), FlowConfig.get(FlowConfig.REJECT_COOLDOWN_TICKS))) return false;
        boolean nonLethal = FlowConfig.get(FlowConfig.NON_LETHAL);
        double drain = FlowRules.rejectionDrain(p.getMaxHealth(), p.getHealth(), FlowConfig.get(FlowConfig.REJECT_PERCENT),
                FlowConfig.get(FlowConfig.REJECT_MIN_HEALTH), nonLethal);
        if (drain > 0.0) {
            if (p.getHealth() - drain > 0.0) {
                p.setHealth((float) (p.getHealth() - drain));                                     // always the case with nonLethal: no damage event, so nothing can soften the price
                level.broadcastEntityEvent(p, (byte) 2);                                         // the hurt flinch and sound, without a damage event
            } else {
                p.hurt(p.damageSources().genericKill(), (float) drain);                           // only with nonLethal off: the normal death path, through a type no armour, resistance or gear softens
            }
        }
        feedback(level, p, cast, flow);
        return true;
    }

    private static void feedback(ServerLevel level, ServerPlayer p, Alignment cast, Alignment flow) {
        Vec3 at = FlowEvents.pointOf(p).add(0.0, p.getBbHeight() * 0.6, 0.0);
        RukhFx.burst(level, at, rukh(cast), 12, 0.6);                                           // the rejected magic's Rukh scatter away
        RukhFx.gather(level, p, rukh(flow), 8);                                                   // the place's own Rukh close in
        level.playSound(null, FlowEvents.posOf(p), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.9f, 0.7f);
        level.playSound(null, FlowEvents.posOf(p), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
        String word = cast == Alignment.DARK ? "darkness" : "light";
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.mfw.reject." + cast.id(), "The Rukh of this place reject your " + word + "."), true);
    }

    /** The particle colour that stands for an alignment (NEUTRAL has no Rukh of its own and reads as light, the default colour). */
    static RukhFx.Alignment rukh(Alignment a) { return a == Alignment.DARK ? RukhFx.Alignment.DARK : RukhFx.Alignment.LIGHT; }
}
