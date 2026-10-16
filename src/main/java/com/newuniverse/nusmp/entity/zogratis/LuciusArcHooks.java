package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.boss.arc.CoreGemMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.UUID;

/**
 * Why: the arc needs a handful of facts that only the NeoForge event bus can give, and they are all about <i>other</i> entities, so
 * they cannot live inside {@link LuciusBoss}:
 * <ul>
 *   <li><b>Turned companions</b> ({@link PaladinTurns}): redirect a turned creature's target ({@code LivingChangeTargetEvent}), cancel the
 *       owner's blows on it and free it on an anti-magic blow ({@code LivingIncomingDamageEvent}), and give every tagged creature back
 *       when it joins or leaves a level or the server stops (so a crash or a chunk unload in the middle of a turn can never leave a
 *       pet hostile).</li>
 *   <li><b>The pressure cap</b>: every blow that Lucius, one of his clones or a turned companion deals to a player is held to a share of
 *       that player's health per second, shared by all of them ({@code CloneScheduler.PressureBudget}). This is the "DPS cap" of the
 *       arc: it is applied where the damage arrives, so it covers every spell without touching any of the spell code, and the bullet
 *       hell of act 3 cannot out-damage it.</li>
 *   <li><b>The critical hit</b>: vanilla does not crit against a part (it is not a living entity), so the swing strength of the
 *       attacker is recorded when the attack starts ({@code AttackEntityEvent}) and the rest of vanilla's conditions are read when the
 *       blow arrives.</li>
 * </ul>
 * Anti-magic is recognised by the existing detection ({@link LuciusFx#antiMagicHit}: the demon swords, the Anti-Magic Lord, the grimoire)
 * and by the item tag {@code nusmp:anti_magic_weapons}, which lists the registered 3D weapons (every entry optional).
 * Wire it in NUSMP once: {@code LuciusArcHooks.register()} (idempotent).
 */
public final class LuciusArcHooks {
    private LuciusArcHooks() {}

    public static final TagKey<Item> ANTI_MAGIC_WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "anti_magic_weapons"));
    private static final String SWING = "nusmp_lu_swing", SWING_T = "nusmp_lu_swing_t";
    private static boolean done;

    public static void register() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(LuciusArcHooks::onIncoming);
        NeoForge.EVENT_BUS.addListener(LuciusArcHooks::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(LuciusArcHooks::onJoin);
        NeoForge.EVENT_BUS.addListener(LuciusArcHooks::onLeave);
        NeoForge.EVENT_BUS.addListener(LuciusArcHooks::onAttack);
        NeoForge.EVENT_BUS.addListener(LuciusArcHooks::onStopping);
    }

    // ================================================================ facts about a blow
    /** Is this blow anti-magic: the existing detection, or the player's main hand holds a weapon of the {@code anti_magic_weapons} tag. */
    public static boolean antiMagic(DamageSource source) {
        if (LuciusFx.antiMagicHit(source)) return true;
        return source.getEntity() instanceof Player p && source.getDirectEntity() == p && p.getMainHandItem().is(ANTI_MAGIC_WEAPONS);
    }

    /** Is this blow a critical hit: a crit arrow, or a melee blow that meets vanilla's conditions (swing strength recorded this tick). */
    public static boolean crit(DamageSource source, long now) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof AbstractArrow arrow) return arrow.isCritArrow();
        if (source.getEntity() instanceof Player p && direct == p) {
            CompoundTag d = p.getPersistentData();
            float strength = d.getLong(SWING_T) == now ? d.getFloat(SWING) : 0f;
            return CoreGemMath.meleeCrit(strength, p.fallDistance, p.onGround(), p.onClimbable(), p.isInWater(), p.hasEffect(MobEffects.BLINDNESS),
                    p.isPassenger(), p.isSprinting());
        }
        return false;
    }

    // ================================================================ events
    static void onAttack(AttackEntityEvent e) {
        if (!(e.getTarget() instanceof LuciusCoreGem) || e.getEntity().level().isClientSide) return;
        Player p = e.getEntity();
        p.getPersistentData().putFloat(SWING, p.getAttackStrengthScale(0.5f));
        p.getPersistentData().putLong(SWING_T, p.level().getGameTime());
    }

    /** 1.45.0: the event this class cancelled itself (the pressure cap), so that {@link LuciusBossEventHandler} does not read it as a dodge. */
    static volatile LivingIncomingDamageEvent ownCancel;

    /** 1.45.0: is this attacker Lucius, one of his clones or a creature he turned (while the final arc is on)? */
    static boolean luciusSide(ServerLevel sl, Entity attacker) { return attacker != null && directorOf(sl, attacker) != null; }

    /** The director whose pressure budget covers a blow by this attacker (Lucius, one of his clones, a creature he turned), or null. */
    private static LuciusArcDirector directorOf(ServerLevel sl, Entity attacker) {
        LuciusBoss boss = null;
        if (attacker instanceof LuciusBoss b) boss = b;
        else if (attacker instanceof ZogratisCloneEntity c && c.ownerId() != null && sl.getEntity(c.ownerId()) instanceof LuciusBoss b) boss = b;
        else if (PaladinTurns.isTurned(attacker) && PaladinTurns.luciusOf(attacker) != null && sl.getEntity(PaladinTurns.luciusOf(attacker)) instanceof LuciusBoss b) boss = b;
        return boss != null && LuciusArcDirector.on() ? boss.director() : null;
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel sl)) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        if (PaladinTurns.isTurned(victim)) {
            UUID owner = PaladinTurns.ownerOf(victim);
            if (attacker != null && owner != null && attacker.getUUID().equals(owner)) {
                e.setCanceled(true);                                                    // a panicking owner cannot kill their own companion
                if (attacker instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Your companion is not itself. Strike Lucius, or free it with anti-magic.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
            if (victim instanceof Mob m && antiMagic(src)) {
                UUID lu = PaladinTurns.luciusOf(victim);
                if (lu != null && sl.getEntity(lu) instanceof LuciusBoss b) b.director().free(m);
                else PaladinTurns.restore(m);
                e.setCanceled(true);
                if (attacker instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Anti-magic frees the companion.").withStyle(ChatFormatting.GREEN), true);
                return;
            }
        }
        if (victim instanceof ServerPlayer p && attacker != null) {
            LuciusArcDirector d = directorOf(sl, attacker);
            if (d == null) return;
            float amount = e.getAmount();
            if (PaladinTurns.isTurned(attacker)) amount = PaladinTurns.cap(amount, p);
            float allowed = d.pressure.allow(p.getId(), sl.getGameTime(), amount, p.getMaxHealth());
            if (allowed <= 0f) {
                ownCancel = e;                                                        // 1.45.0: not a dodge of the player's, the dodge counter must not count it
                e.setCanceled(true);
            }
            else if (allowed < e.getAmount()) e.setAmount(allowed);
        }
    }

    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide || !(who instanceof Mob m) || !PaladinTurns.isTurned(m)) return;
        if (PaladinTurns.acceptable(e.getNewAboutToBeSetTarget())) return;
        LivingEntity prey = PaladinTurns.prey(m);
        if (prey != null) e.setNewAboutToBeSetTarget(prey);
        else e.setCanceled(true);
    }

    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m) || !PaladinTurns.isTurned(m)) return;
        PaladinTurns.restore(m);                                                       // it was saved in the middle of a turn: it is its owner's again
    }

    static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m) || !PaladinTurns.isTurned(m)) return;
        PaladinTurns.restore(m);
    }

    static void onStopping(ServerStoppingEvent e) {
        MinecraftServer server = e.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity ent : level.getAllEntities()) if (ent instanceof Mob m && PaladinTurns.isTurned(m)) PaladinTurns.restore(m);
        }
    }
}
