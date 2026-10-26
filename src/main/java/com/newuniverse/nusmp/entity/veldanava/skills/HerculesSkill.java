package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.entity.veldanava.VeldanavaBoss;
import com.newuniverse.nusmp.entity.veldanava.VeldanavaItem;
import com.newuniverse.nusmp.entity.veldanava.pocket.PocketLabyrinth;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hercules, Lord of Martial Prowess: the fifth and last of the Veldanava ultimate skills (the numbers are in {@link HerculesRules}, the passive rules in {@link HerculesEvents}). Ten modes: four toggle passives
 * (Adamantine Body, God of Strength, Unstoppable Momentum, Colossal Vigor; under the master toggle since 3.20.0), five actives (Earth-Shattering Tremor, Invictus, Titan's Grip, Devastating Throw, Battle Roar) and the mastery active World-Breaking Strike.
 * The press, cost, cooldown and toggle code is {@link VeldanavaSkillBase}.
 *
 * <p>Worst case per press (rule 11): Tremor one jump (no query) and, at the landing, one box query (at most 24 creatures hurt and lifted, at most 24 effects) and a bowl of at most 240 blocks; Invictus one field and
 * one effect packet now and one more every 80 ticks for ten seconds; Titan's Grip one ray, one move, one effect; Devastating Throw one ray, one field that runs 9 pulses (see {@link HerculesThrow}); Battle Roar one
 * box query (32 enemies, 64 effects) and at most 24 effects cleansed; World-Breaking Strike one ray, one box query (8 creatures hurt, 8 effects each of debt, broken armour), the labyrinths and fields within 24 blocks
 * ended. The passives run once a second per owner: two attribute checks, one heal and at most one effect packet when Momentum reaches a new stage; no box query. One effect packet per press.
 */
public class HerculesSkill extends VeldanavaSkillBase {
    public HerculesSkill() { super(HerculesRules.HERCULES); }

    static final String INVICTUS_KIND = "invictus";

    // ---------------------------------------------------------------- the passives that need a look once a second
    @Override
    protected void secondTick(ManasSkillInstance i, ServerPlayer p, int toggles, boolean mastered) {
        HerculesState.expireLeap(p);
        if (JudgmentRules.on(toggles, 0)) shrugOff(p);
        if (JudgmentRules.on(toggles, 2)) {
            int before = HerculesState.stage(p.getUUID());
            int stage = HerculesState.momentum(p);
            if (stage > before) HerculesFx.momentum(p.serverLevel(), p, stage);
        }
        if (JudgmentRules.on(toggles, 3) && p.getHealth() < p.getMaxHealth() && HerculesRules.calm(p.tickCount, p.getLastHurtMobTimestamp(), p.getLastHurtByMobTimestamp()))
            p.heal(HerculesRules.regen(p.getMaxHealth(), mastered));
    }

    @Override
    protected void toggled(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean on) {
        if (m.index() == 2 && !on) HerculesState.endMomentum(p);
        if (m.index() == 0 && on) shrugOff(p);
    }

    @Override
    protected void forgotten(ServerPlayer p) {
        HerculesState.clear(p);
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), INVICTUS_KIND);
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), HerculesThrow.KIND);
    }

    /** Adamantine Body's second: flame is put out and a Temporal Stutter that was already on is lifted (the blows from fire are cancelled in {@link HerculesEvents}). */
    private static void shrugOff(ServerPlayer p) {
        if (p.isOnFire()) p.clearFire();
        if (p.hasEffect(JudgmentEffects.TEMPORAL_STUTTER)) p.removeEffect(JudgmentEffects.TEMPORAL_STUTTER);
    }

    // ---------------------------------------------------------------- the press
    @Override
    protected boolean activate(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        return switch (m.index()) {
            case 4 -> leap(p);
            case 5 -> invictus(p);
            case 6 -> grip(p);
            case 7 -> hurl(p);
            case 8 -> roar(p);
            default -> strike(p);
        };
    }

    private static Vec3 centre(LivingEntity e) { return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0); }

    private static Vec3 flat(Vec3 look) {
        Vec3 h = new Vec3(look.x, 0.0, look.z);
        return h.lengthSqr() < 1.0E-4 ? Vec3.ZERO : h.normalize();
    }

    /** The foes within {@code r} blocks of {@code at}, nearest first, at most {@code max}. One box query. */
    private static List<LivingEntity> foesAround(ServerPlayer p, ServerLevel l, Vec3 at, double r, int max) {
        double r2 = r * r;
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(r, Math.min(r, 12.0), r),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.position().distanceToSqr(at) <= r2 && Foes.isFoe(p, x)));
        near.sort(Comparator.comparingDouble(x -> x.position().distanceToSqr(at)));
        return near.size() > max ? new ArrayList<>(near.subList(0, max)) : near;
    }

    // ---------------------------------------------------------------- Earth-Shattering Tremor
    /** The take-off: a leap along the look direction; the landing is {@link #land}, run by {@link HerculesEvents} when the owner hits the ground within {@link HerculesRules#LEAP_WINDOW_TICKS} ticks. */
    private static boolean leap(ServerPlayer p) {
        if (!p.onGround() || p.isPassenger() || p.isInWater()) { fail(p, "Stand on firm ground to leap."); return false; }
        Vec3 h = flat(p.getLookAngle());
        p.setDeltaMovement(h.scale(HerculesRules.LEAP_FORWARD).add(0.0, HerculesRules.LEAP_UP, 0.0));
        p.hurtMarked = true;
        HerculesState.leap(p);
        HerculesFx.leap(p.serverLevel(), p, p.position().add(h.scale(16.0)));
        return true;
    }

    /**
     * The landing: every foe within {@link HerculesRules#TREMOR_RADIUS} blocks (at most 24) takes a blow that falls off with the distance, is thrown up and loses its footing (Movement Interference); the ground under
     * the owner is broken in a bowl ({@link HerculesTerrain}).
     */
    static void land(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 at = p.position();
        double radius = HerculesRules.TREMOR_RADIUS;
        double mastery = HerculesDamage.mastery(p);
        for (LivingEntity t : foesAround(p, l, at, radius, HerculesRules.TREMOR_MAX_CREATURES)) {
            double d = Math.sqrt(t.position().distanceToSqr(at));
            HerculesDamage.blow(p, t, HerculesRules.TREMOR_DAMAGE * HerculesRules.tremorFalloff(d, radius), mastery);
            if (!t.isAlive()) continue;
            if (!JudgmentState.onFor(t, HerculesRules.CODE, 0)) {
                Vec3 away = flat(t.position().subtract(at)).scale(0.4);
                t.setDeltaMovement(away.x, HerculesRules.KNOCK_UP * (BalanceLaw.isBoss(t) ? 0.4 : 1.0), away.z);
                t.hurtMarked = true;
            }
            t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, BalanceLaw.controlTicks(t, HerculesRules.TREMOR_INTERFERENCE_TICKS), 0, false, true, true));
        }
        HerculesTerrain.bowl(l, HerculesTerrain.under(at), HerculesRules.TREMOR_BOWL_RADIUS, HerculesRules.TREMOR_BOWL_DEPTH, HerculesRules.TREMOR_MAX_BLOCKS);
        HerculesFx.tremor(l, at, radius);
        if (JudgmentConfig.herculesUpgrades()) HerculesState.aftershock(p, at);
    }

    /** The aftershock of a tremor ({@link HerculesState#tickAftershocks}): the ground heaves again; every foe within {@link HerculesRules#AFTERSHOCK_RADIUS} (at most 24) takes half a tremor and is lifted a little. No blocks are broken. */
    static void aftershock(ServerPlayer p, ServerLevel l, Vec3 at) {
        if (!JudgmentConfig.enabled() || JudgmentState.skill(p) != HerculesRules.CODE) return;
        double mastery = HerculesDamage.mastery(p);
        for (LivingEntity t : foesAround(p, l, at, HerculesRules.AFTERSHOCK_RADIUS, HerculesRules.AFTERSHOCK_MAX)) {
            double d = Math.sqrt(t.position().distanceToSqr(at));
            HerculesDamage.blow(p, t, HerculesRules.aftershockDamage(d), mastery);
            if (t.isAlive() && !JudgmentState.onFor(t, HerculesRules.CODE, 0)) {
                t.setDeltaMovement(t.getDeltaMovement().x, HerculesRules.KNOCK_UP * 0.5 * (BalanceLaw.isBoss(t) ? 0.4 : 1.0), t.getDeltaMovement().z);
                t.hurtMarked = true;
            }
        }
        HerculesFx.tremor(l, at, HerculesRules.AFTERSHOCK_RADIUS);
    }

    // ---------------------------------------------------------------- Invictus
    /** Invictus: for ten seconds the owner's Hit Points cannot fall below one and melee blows grow as the health falls. One field draws the laurel again every 80 ticks and ends it. */
    private static boolean invictus(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), INVICTUS_KIND);
        HerculesState.startInvictus(p);
        JudgmentFields.add(p, l, p, p.position(), 2.0, HerculesRules.INVICTUS_TICKS + HerculesRules.INVICTUS_PULSE_TICKS, new Invictus(), INVICTUS_KIND);
        HerculesFx.invictus(l, p, true);
        p.displayClientMessage(Component.literal("Invictus: you cannot fall for " + HerculesRules.INVICTUS_TICKS / 20 + " seconds.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Invictus as a field that follows its owner: once a second it checks the time, every fourth second it draws the laurel again. */
    static final class Invictus implements JudgmentFields.Behavior {
        private int pulses;

        @Override public int period() { return HerculesRules.INVICTUS_PULSE_TICKS; }

        @Override
        public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
            if (!HerculesState.invictus(owner)) { f.endsAt = 0L; return; }
            if (++pulses % HerculesRules.INVICTUS_FX_EVERY_PULSES == 0) HerculesFx.invictus(level, owner, false);
        }

        @Override
        public void end(ServerLevel level, JudgmentFields.Field f) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(f.owner);
            if (owner == null) return;
            long until = owner.getPersistentData().getLong(HerculesState.INVICTUS_UNTIL);
            boolean ranOut = until > 0L && until <= owner.level().getGameTime();
            HerculesState.endInvictus(owner);
            if (ranOut && owner.isAlive() && JudgmentConfig.herculesUpgrades() && JudgmentState.skill(owner) == HerculesRules.CODE) {
                owner.heal(HerculesRules.rally(owner.getMaxHealth(), JudgmentState.mastered(owner)));
                HerculesFx.invictus(level, owner, false);
            }
        }
    }

    // ---------------------------------------------------------------- Titan's Grip
    /** Titan's Grip: the foe in sight (a flying boss too) is dragged to {@link HerculesRules#GRIP_STOP} blocks in front of the owner and locked. */
    private static boolean grip(ServerPlayer p) {
        LivingEntity t = JudgmentState.first(p, HerculesRules.GRIP_RANGE, HerculesRules.GRIP_WIDTH);
        ServerLevel l = p.serverLevel();
        if (t == null || !Foes.isFoe(p, t)) { fail(p, "No enemy in sight to grip."); return false; }
        if (PocketLabyrinth.inPocket(t) != PocketLabyrinth.inPocket(p)) { fail(p, "It is out of your reach."); return false; }
        Vec3 h = flat(p.getLookAngle());
        Vec3 stop = null;
        for (double k = HerculesRules.GRIP_STOP; k <= HerculesRules.GRIP_STOP + 2.0 && stop == null; k += 1.0) {
            Vec3 c = new Vec3(p.getX() + h.x * k, p.getY(), p.getZ() + h.z * k);
            if (l.noCollision(t, t.getBoundingBox().move(c.subtract(t.position())))) stop = c;
        }
        if (stop == null) { fail(p, "There is no room to pull it to you."); return false; }
        HerculesFx.grip(l, p, t);
        IsrafilState.place(t, l, stop);
        t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, HerculesRules.GRIP_LOCK_TICKS), 0, false, true, true));
        if (JudgmentConfig.herculesUpgrades()) HerculesDamage.blow(p, t, HerculesRules.GRIP_CRUSH_DAMAGE, HerculesDamage.mastery(p));
        return true;
    }

    // ---------------------------------------------------------------- Devastating Throw
    /**
     * Devastating Throw: the foe in sight within {@link HerculesRules#THROW_PICK_RANGE} blocks is hurled along the look direction; with none, a chunk of the ground the owner looks at (within
     * {@link HerculesRules#CHUNK_REACH} blocks) is lifted and hurled. One field of the {@code throw} kind carries it ({@link HerculesThrow}).
     */
    private static boolean hurl(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        LivingEntity t = JudgmentState.first(p, HerculesRules.THROW_PICK_RANGE, 1.2);
        if (t != null && !Foes.isFoe(p, t)) t = null;
        if (t != null && PocketLabyrinth.inPocket(t) != PocketLabyrinth.inPocket(p)) t = null;
        Vec3 look = p.getViewVector(1f);
        Vec3 start;
        if (t != null) {
            start = centre(t);
        } else {
            if (HerculesTerrain.lift(p, l) <= 0) { fail(p, "Nothing within reach to hurl."); return false; }
            start = p.getEyePosition().add(look.scale(2.0));
        }
        Vec3 reach = start.add(look.scale(HerculesRules.throwPath(HerculesRules.THROW_MAX_TICKS)));
        var hit = l.clip(new net.minecraft.world.level.ClipContext(start, reach, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        Vec3 landing = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.getLocation() : reach;
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), HerculesThrow.KIND);
        if (!JudgmentFields.add(p, l, null, start, HerculesRules.THROW_BLAST_RADIUS, HerculesRules.THROW_MAX_TICKS + HerculesRules.THROW_PULSE_TICKS * 2, new HerculesThrow(t == null ? null : t.getUUID(), start, look), HerculesThrow.KIND)) {
            fail(p, "Too many fields stand on this server.");
            return false;
        }
        if (t != null) {
            t.setDeltaMovement(look.scale(HerculesRules.THROW_SPEED));
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, HerculesRules.THROW_MAX_TICKS, 0, false, true, true));
        }
        HerculesFx.thrown(l, start, landing, t == null);
        return true;
    }

    // ---------------------------------------------------------------- Battle Roar
    /** Battle Roar: the owner's debuffs are lifted; every foe within thirty blocks (at most 32) is seized by Fear and Temporal Stutter (both limited per target by the Balance Law); since 3.20.0 the nearest sixteen within twelve blocks also take the thunder of the roar. */
    private static boolean roar(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        JibrilState.cleanse(p, HerculesRules.ROAR_CLEANSE_MAX);
        int n = 0, thunder = 0;
        boolean upgrades = JudgmentConfig.herculesUpgrades();
        double mastery = HerculesDamage.mastery(p);
        for (LivingEntity t : foesAround(p, l, p.position(), HerculesRules.ROAR_RADIUS, HerculesRules.ROAR_MAX)) {
            int ticks = BalanceLaw.controlTicks(t, HerculesRules.ROAR_FEAR_TICKS);
            EnergyBridge.effect(t, "fear", ticks, 0);
            t.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, Math.min(ticks, HerculesRules.ROAR_STUTTER_TICKS), 0, false, true, true));
            n++;
        }

        HerculesFx.roar(l, p);
        p.displayClientMessage(Component.literal(n == 0 ? "The roar rings out; nothing hears it." : "The roar shakes " + n + (n == 1 ? " enemy." : " enemies.")).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    // ---------------------------------------------------------------- World-Breaking Strike
    /**
     * World-Breaking Strike: one punch. The spatial barriers within {@link HerculesRules#STRIKE_SHATTER_RADIUS} blocks of the fist (the pockets of other players' labyrinths, the enemy's fields) end; of the (at most
     * eight) foes within {@link HerculesRules#STRIKE_RADIUS} blocks of it, each has Michael's Castle Guard, Uriel's prison and the guard of the Seraphim shattered, its armour broken for
     * {@link HerculesRules#ARMOR_BREAK_TICKS} ticks, {@link HerculesRules#STRIKE_DEBT} stacks of Temporal Debt laid on it, and takes the blow (the nearest one in full, the others half).
     */
    private static boolean strike(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        LivingEntity aimed = JudgmentState.first(p, HerculesRules.STRIKE_REACH + 1.0, HerculesRules.STRIKE_WIDTH);
        if (aimed != null && !Foes.isFoe(p, aimed)) aimed = null;
        Vec3 point = aimed != null ? centre(aimed) : p.getEyePosition().add(p.getViewVector(1f).scale(HerculesRules.STRIKE_REACH * 0.8));
        List<LivingEntity> foes = foesAround(p, l, point, HerculesRules.STRIKE_RADIUS, HerculesRules.STRIKE_MAX);
        if (aimed != null && !foes.remove(aimed)) foes = new ArrayList<>(foes.subList(0, Math.min(foes.size(), HerculesRules.STRIKE_MAX - 1)));
        if (aimed != null) foes.add(0, aimed);
        int broken = PocketLabyrinth.endNear(l, point, HerculesRules.STRIKE_SHATTER_RADIUS, p.getUUID())
                + JudgmentFields.clearAround(l, point, HerculesRules.STRIKE_SHATTER_RADIUS, p.getUUID());
        if (foes.isEmpty() && broken == 0) { fail(p, "Nothing within reach of the fist."); return false; }
        double mastery = HerculesDamage.mastery(p);
        int rank = 0;
        for (LivingEntity t : foes) {
            if (t instanceof VeldanavaBoss b && b.shatterSpatial()) broken++;
            if (t instanceof ServerPlayer sp && spendCastleGuard(sp)) broken++;
            t.addEffect(new MobEffectInstance(JudgmentEffects.ARMOR_BREAK, HerculesRules.armorBreakTicks(t instanceof net.minecraft.world.entity.player.Player, BalanceLaw.isBoss(t)), 0, false, true, true));
            TemporalDebt.add(t, HerculesRules.STRIKE_DEBT);
            HerculesDamage.blow(p, t, HerculesRules.strikeDamage(rank++), mastery);
        }
        HerculesFx.strike(l, p, point);
        p.displayClientMessage(Component.literal("World-Breaking Strike: " + foes.size() + (foes.size() == 1 ? " enemy" : " enemies") + ", " + broken + " barrier" + (broken == 1 ? "" : "s") + " shattered.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Michael's Castle Guard of a player (one fatal blow a day, {@link VeldanavaItem#TAG_MICHAEL}): spent for today, so the blow that follows is not turned away. True when there was one to spend. */
    static boolean spendCastleGuard(ServerPlayer t) {
        if (!t.getPersistentData().getBoolean(VeldanavaItem.TAG_MICHAEL)) return false;
        long day = t.level().getDayTime() / 24000L;
        if (t.getPersistentData().getLong(VeldanavaItem.TAG_MICHAEL_DAY) == day + 1) return false;
        t.getPersistentData().putLong(VeldanavaItem.TAG_MICHAEL_DAY, day + 1);
        return true;
    }
}
