package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the abilities that react to the world (a blow, a jump, a target change, a night's sleep) cannot live in an act, so they are routed from here to
 * the card classes with the least possible work done in the handler itself.
 * <p>
 * Wave 28: the game-bus handlers, registered once by {@link Wave28#hooks}. Each leaves at once for anyone the wave does not concern: the wave's flag
 * {@link Wave28Rules#F_ANY} is one cached lookup, and nothing here scans the world on a tick (the Witcher's Medallion looks once a second, and only
 * for its wearer).
 */
final class Wave28Events {
    private Wave28Events() {}

    /** Once a second per player (staggered by entity id): the potions' clocks (they run even if nothing is worn), then the worn relics' upkeep. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 6) {
            Wave28Potions.headPass(p);
            Wave28Potions.pukePass(p);
            return;
        }
        if (phase != 11) return;
        boolean any = RelicFlags.has(p, Wave28Rules.F_ANY);
        if (any && RelicFlags.has(p, Wave28Rules.F_PROF)) Wave28Hands.profPass(p);
        else Wave28Hands.profCleanup(p);
        if (any && RelicFlags.has(p, Wave28Rules.F_CHROMA)) Wave28Neck.chromaPass(p);
        else Wave28Neck.chromaCleanup(p);
        Wave28Neck.fenrirPass(p, any && RelicFlags.has(p, Wave28Rules.F_FENRIR));
        if (!any) return;
        if (RelicFlags.has(p, Wave28Rules.F_SUN)) Wave28Neck.sunPass(p);
        if (RelicFlags.has(p, Wave28Rules.F_WITCHER)) Wave28Neck.witcherPass(p);
        if (RelicFlags.has(p, Wave28Rules.F_PANTS)) Wave28Belt.pass(p);
    }

    /** Before armour: the Hellfire Gauntlet's blows and the pantaloons' check on the attacker; the drawn resistance of the Chromatic Collar on the victim. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        if (src.getEntity() instanceof ServerPlayer a && src.getDirectEntity() == a && RelicFlags.has(a, Wave28Rules.F_ANY)) {
            if (RelicFlags.has(a, Wave28Rules.F_PANTS)) Wave28Belt.check(a);
            if (RelicFlags.has(a, Wave28Rules.F_HELL) && src.is(DamageTypes.PLAYER_ATTACK)) Wave28Hands.blow(a, victim, e);
        }
        if (victim instanceof ServerPlayer v && RelicFlags.has(v, Wave28Rules.F_CHROMA)) {
            float keep = Wave28Neck.chromaKeep(v, src);
            if (keep < 1f) e.setAmount(e.getAmount() * keep);
        }
    }

    /** After armour, before the health changes: a beheaded player's killing blow is turned; a warded creature's killing blow is cancelled. */
    static void onDamagePre(LivingDamageEvent.Pre e) {
        LivingEntity v = e.getEntity();
        float health = v.getHealth(), damage = e.getNewDamage();
        if (damage < health) return;
        if (v instanceof ServerPlayer p && Wave28Potions.saveFromBlow(p, health, damage, e)) return;
        if (Wave28Neck.warded(v, health, damage)) {
            e.setNewDamage(0f);
            Wave28Neck.spendWard(v);
        }
    }

    /** After the blow: the Hellfire Gauntlet's aura burns whoever struck the wearer in melee. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer v) || !RelicFlags.has(v, Wave28Rules.F_HELL)) return;
        DamageSource src = e.getSource();
        if (src.getDirectEntity() instanceof LivingEntity attacker && attacker == src.getEntity() && attacker != v) Wave28Hands.auraAnswer(v, attacker);
    }

    /** A charmed creature does not pick its charmer; a creature of dragon kind does not pick a penitent. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity t = e.getNewAboutToBeSetTarget();
        if (t == null || !(e.getEntity() instanceof Mob m)) return;
        if (Wave28Neck.charmedAgainst(m, t)) {
            e.setCanceled(true);
            return;
        }
        if (t instanceof ServerPlayer p && RelicFlags.has(p, Wave28Rules.F_PENITENT) && Wave28Neck.dragonRecognises(m, p)) e.setCanceled(true);
    }

    /** A jump is a check for the pantaloons. */
    static void onJump(LivingEvent.LivingJumpEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && RelicFlags.has(p, Wave28Rules.F_PANTS)) Wave28Belt.check(p);
    }

    /** A beheaded player cannot eat or drink. */
    static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Wave28Potions.beheaded(p)) return;
        UseAnim a = e.getItem().getUseAnimation();
        if (a == UseAnim.EAT || a == UseAnim.DRINK) {
            e.setCanceled(true);
            Wave28Util.tell(p, "Your head has no body to feed.", net.minecraft.ChatFormatting.DARK_RED);
        }
    }

    /** A night's sleep puts a beheaded head back. */
    static void onWake(PlayerWakeUpEvent e) {
        if (!e.wakeImmediately() && e.getEntity() instanceof ServerPlayer p) Wave28Potions.reattach(p);
    }

    /** A new body after death does not carry the potions' states over. */
    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave28Potions.clear(p);
    }
}
