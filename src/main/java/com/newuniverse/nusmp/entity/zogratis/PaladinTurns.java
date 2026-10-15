package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.arc.PaladinTurn;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the Soul Touch turns the victim's companions against them for twenty seconds. "Companion" is three different things in this
 * mod: a tamed animal (a {@link TamableAnimal} or any other {@link OwnableEntity} with the player as owner), a creature called by a
 * relic (a persistent-data tag, see relics/Fx.summon) and a Djinn. A Djinn is armour-only effect data with no entity at all (the
 * relics/djinn package has no mob), so there is nothing to turn and it is skipped. The turn is a <i>loan</i>, never a conversion: no
 * entity type, health or attribute is changed, only who the creature fights for, so giving it back is exact.
 * <ul>
 *   <li><b>Tag</b>: {@value #TAG} in the creature's persistent data holds Lucius, the real owner and the end tick. It is saved with the
 *       creature, so a reload in the middle of a turn cannot keep a pet hostile for ever: {@code LuciusArcHooks} gives back any tagged
 *       creature that joins a level without a live loan.</li>
 *   <li><b>Owner</b>: a {@link TamableAnimal} refuses to attack its owner (vanilla {@code canAttack}), so for the loan its owner id is
 *       cleared and written back at the end.</li>
 *   <li><b>Target</b>: a replacement for the creature's own target goals without touching its goal lists (they are protected): the
 *       NeoForge {@code LivingChangeTargetEvent} is redirected to the original owner, and a once-a-half-second drive points the creature
 *       at its prey, paths to it and strikes in reach (the same way relics/RelicSummons drive Tensura's brain-driven mobs).</li>
 *   <li><b>Limits</b>: six at once (the pure {@link PaladinTurn}), never a boss, never a member of the Zogratis family, never a player.
 *       Nothing here damages the creature; the owner's own blows on it are cancelled so a panicked player cannot kill their pet, and an
 *       anti-magic blow frees it.</li>
 * </ul>
 */
public final class PaladinTurns {
    private PaladinTurns() {}

    /** The persistent-data compound a turned creature carries. */
    public static final String TAG = "nusmp_paladin_turn";
    private static final String RELIC_OWNER = "nusmp_relic_owner";
    private static final int PURPLE = 0xB07CFF;

    public static boolean isTurned(Entity e) { return e != null && e.getPersistentData().contains(TAG); }

    /** The id of the Lucius a turned creature serves (null if it is not turned). */
    public static UUID luciusOf(Entity e) {
        if (!isTurned(e)) return null;
        CompoundTag t = e.getPersistentData().getCompound(TAG);
        return t.hasUUID("Lucius") ? t.getUUID("Lucius") : null;
    }

    /** The player this creature belongs to: a tamed owner, or the player that called it with a relic. Null if nobody's. */
    public static UUID ownerOf(Entity e) {
        if (isTurned(e)) {
            CompoundTag t = e.getPersistentData().getCompound(TAG);
            if (t.hasUUID("Owner")) return t.getUUID("Owner");
        }
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return o.getOwnerUUID();
        return e.getPersistentData().hasUUID(RELIC_OWNER) ? e.getPersistentData().getUUID(RELIC_OWNER) : null;
    }

    /** Can this entity be turned on behalf of 'playerId'? */
    static boolean eligible(Entity e, UUID playerId) {
        if (!(e instanceof Mob m) || !m.isAlive() || m.isRemoved() || e instanceof Player || isTurned(e) || ZogratisEntities.isFamily(e)) return false;
        if (BossIdentity.isBoss(e) || BalanceLaw.isBoss(m) || m.isSpectator()) return false;
        UUID owner = ownerOf(e);
        return owner != null && owner.equals(playerId);
    }

    /**
     * Turns every companion of 'victim' within 48 blocks (nearest first) that the roster has room for. Returns how many were turned.
     * Server side only.
     */
    static int turnCompanionsOf(LuciusBoss boss, PaladinTurn<UUID> roster, ServerPlayer victim) {
        if (!(boss.level() instanceof ServerLevel sl)) return 0;
        long now = sl.getGameTime();
        List<Mob> pool = new ArrayList<>(sl.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(48), m -> eligible(m, victim.getUUID())));
        pool.sort(Comparator.comparingDouble(m -> m.distanceToSqr(victim)));
        int turned = 0;
        for (Mob m : pool) {
            PaladinTurn.Result r = roster.turn(m.getUUID(), victim.getUUID(), now);
            if (r == PaladinTurn.Result.FULL) break;
            if (r != PaladinTurn.Result.TURNED) continue;
            apply(m, boss.getUUID(), victim.getUUID(), now + PaladinTurn.TURN_TICKS);
            turned++;
        }
        if (turned > 0) {
            victim.displayClientMessage(Component.literal(turned + (turned == 1 ? " companion is" : " companions are")
                    + " turned against you by Lucius for 20 seconds. Anti-magic frees one.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        }
        return turned;
    }

    /** Writes the tag and switches the creature's side. */
    static void apply(Mob m, UUID lucius, UUID owner, long until) {
        CompoundTag t = new CompoundTag();
        t.putUUID("Lucius", lucius);
        t.putUUID("Owner", owner);
        t.putLong("Until", until);
        if (m instanceof TamableAnimal ta && owner.equals(ta.getOwnerUUID())) {
            t.putBoolean("Cleared", true);
            ta.setOwnerUUID(null);
            ta.setOrderedToSit(false);
        }
        m.getPersistentData().put(TAG, t);
        m.setTarget(null);
        m.getNavigation().stop();
        if (m.level() instanceof ServerLevel sl) {
            ZogratisVfx.soulVortex(sl, m.position(), 1.6, 30, PURPLE);
            LuciusFx.sound(sl, m.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.7f);
        }
    }

    /** Gives a creature back to its owner exactly as it was: owner id, no tag, no target, no glow. Safe to call twice. */
    public static void restore(Mob m) {
        if (!isTurned(m)) return;
        CompoundTag t = m.getPersistentData().getCompound(TAG);
        if (t.getBoolean("Cleared") && t.hasUUID("Owner") && m instanceof TamableAnimal ta && ta.getOwnerUUID() == null) ta.setOwnerUUID(t.getUUID("Owner"));
        m.getPersistentData().remove(TAG);
        m.setTarget(null);
        try { m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET); } catch (RuntimeException brainless) { /* goal-driven mobs have no such memory */ }
        m.getNavigation().stop();
        m.removeEffect(MobEffects.GLOWING);
    }

    /** What a turned creature should attack: its original owner if they are near and fair game, else the nearest other player in survival. */
    static LivingEntity prey(Mob m) {
        if (!(m.level() instanceof ServerLevel sl)) return null;
        UUID owner = ownerOf(m);
        if (owner != null && sl.getPlayerByUUID(owner) instanceof ServerPlayer p && fair(p) && p.distanceToSqr(m) < 48 * 48) return p;
        ServerPlayer best = null;
        double bd = 32 * 32;
        for (ServerPlayer p : sl.players()) {
            double d = p.distanceToSqr(m);
            if (fair(p) && d < bd) { bd = d; best = p; }
        }
        return best;
    }

    private static boolean fair(Player p) { return p.isAlive() && !p.isCreative() && !p.isSpectator(); }

    /** Is 'target' something a turned creature may attack: a player in survival, or a player's companion that is not itself turned. */
    static boolean acceptable(LivingEntity target) {
        if (target == null || !target.isAlive() || ZogratisEntities.isFamily(target) || isTurned(target)) return false;
        if (target instanceof Player p) return fair(p);
        return ownerOf(target) != null;
    }

    /** Once every ten ticks: the drive of one turned creature (target, path, a blow in reach). */
    static void drive(Mob m, long now) {
        try {
            LivingEntity prey = prey(m);
            if (prey == null) return;
            if (m.getTarget() != prey) {
                m.setTarget(prey);
                m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, prey);
            }
            m.getNavigation().moveTo(prey, 1.15);
            CompoundTag d = m.getPersistentData();
            if (now - d.getLong("nusmp_paladin_strike") >= 20 && m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(prey.getBoundingBox())) {
                d.putLong("nusmp_paladin_strike", now);
                m.swing(InteractionHand.MAIN_HAND);
                m.doHurtTarget(prey);
            }
            if (now % 20 == 0) m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        } catch (RuntimeException | LinkageError odd) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    /** A turned creature's blow, capped: no more than this share of the victim's maximum health. */
    public static final double HIT_CAP = 0.12;

    public static float cap(float amount, LivingEntity victim) { return (float) Math.min(amount, victim.getMaxHealth() * HIT_CAP); }
}
