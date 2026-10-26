package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Sandalphon, Lord of Judgment: the first of the five Veldanava ultimate skills (the numbers are in {@link JudgmentRules#SANDALPHON}, the passive rules in {@link JudgmentEvents}).
 * Ten modes, cycled with the mode key: four toggle passives (a press flips them), five actives that each cost Magicule, and Final Judgment, the mastery active that is held for three seconds.
 *
 * <p>Cost: the skill charges its own Magicule through {@link TensuraBridge} (Tensura's own charge is 0 here), so a refused or empty cast spends nothing. At most one active every
 * {@link JudgmentRules#ACTIVE_GAP_TICKS} ticks per player. Per press the worst case is one segment query, 8 + 32 + 12 creatures touched and one effect packet; the passive scans run once a
 * second per owner and touch at most 12 + 16 creatures. Everything here runs on the server thread, and every piece of state is the player's own (keyed by the player), so any number of
 * players can hold the skill at once.
 */
public class SandalphonSkill extends VeldanavaSkillBase {
    private static final String TAG_STATE = "final_state", TAG_TARGET = "final_target";
    private static final int FINAL_MODE = 9;

    public SandalphonSkill() { super(JudgmentRules.SANDALPHON); }

    // ---------------------------------------------------------------- the passives that need a look once a second
    @Override
    protected void secondTick(ManasSkillInstance i, ServerPlayer p, int toggles, boolean mastered) {
        if (JudgmentRules.on(toggles, 0)) presence(p);
        if (JudgmentRules.on(toggles, 3)) gaze(p);
    }

    /** Executioner's Authority: the creatures that mean harm, up to twelve, near the owner show through walls for a moment. */
    private static void presence(ServerPlayer p) {
        double r = JudgmentRules.PRESENCE_RANGE;
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isAlive() && x instanceof Enemy && !x.isAlliedTo(p) && !VeldanavaGuard.mindImmune(x) && x.distanceToSqr(p) <= r * r);
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        for (int n = 0; n < Math.min(near.size(), JudgmentRules.MAX_PRESENCE_TARGETS); n++) near.get(n).addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
    }

    /** Judgment Gaze: whoever looks straight at the owner (up to sixteen) is action-locked for a moment and owes two more seconds. */
    private static void gaze(ServerPlayer p) {
        double r = JudgmentRules.GAZE_RANGE;
        Vec3 eye = p.getEyePosition();
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p) && x.distanceToSqr(p) <= r * r);
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : near) {
            if (n >= JudgmentRules.MAX_GAZE_TARGETS) break;
            Vec3 v = t.getViewVector(1f), d = eye.subtract(t.getEyePosition());
            if (!JudgmentRules.looksAt(v.x, v.y, v.z, d.x, d.y, d.z, JudgmentRules.GAZE_COSINE) || !t.hasLineOfSight(p)) continue;
            n++;
            t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, JudgmentRules.GAZE_LOCK_TICKS), 0, false, true, true));
            TemporalDebt.add(t, JudgmentRules.DEBT_PER_GAZE);
        }
    }

    // ---------------------------------------------------------------- the press
    @Override
    protected boolean activate(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        return switch (m.index()) {
            case 4 -> bullet(i, p, mastered);
            case 5 -> condemnation(p);
            case 6 -> edict(p);
            case 7 -> cleave(i, p, mastered);
            default -> verdict(p);
        };
    }

    // ---------------------------------------------------------------- the five actives
    /** Judgment Bullet: instant, pierces blocks and up to eight creatures along sixty-four blocks; each loses its good effects, stutters, cannot heal for five seconds and owes ten seconds. */
    private static boolean bullet(ManasSkillInstance i, ServerPlayer p, boolean mastered) {
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double mastery = mastery(p, i);
        for (LivingEntity t : JudgmentState.along(p, JudgmentRules.BULLET_RANGE, JudgmentRules.BULLET_WIDTH, JudgmentRules.MAX_BULLET_HITS)) {
            JudgmentDamage.hit(p, t, (float) JudgmentRules.BULLET_DAMAGE, mastery);
            if (!t.isAlive()) continue;
            JudgmentDamage.stripBeneficial(t, JudgmentRules.STRIP_MAX);
            t.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, BalanceLaw.controlTicks(t, JudgmentRules.STUTTER_BULLET_TICKS), 0, false, true, true));
            TemporalDebt.add(t, JudgmentRules.DEBT_BULLET);
            JudgmentDamage.markNoHeal(t, JudgmentRules.NO_HEAL_BULLET_TICKS);
        }
        JudgmentFx.bullet(l, eye.add(look.scale(0.6)).add(0.0, -0.25, 0.0), eye.add(look.scale(JudgmentRules.BULLET_RANGE)));
        return true;
    }

    /** Condemnation: the creature in the line of sight gets the owner's Judgment Sigil (the owner's old one goes), owes twenty-five seconds and, at or below a fifth of its health, dies now. */
    private static boolean condemnation(ServerPlayer p) {
        LivingEntity t = JudgmentState.first(p, JudgmentRules.CONDEMN_RANGE, 1.0);
        if (t == null) { fail(p, "Nothing to condemn in your sight."); return false; }
        LivingEntity old = oldMark(p);
        if (old != null && old != t) JudgmentDamage.clearSigil(old);
        JudgmentDamage.mark(p, t);
        TemporalDebt.add(t, JudgmentRules.DEBT_CONDEMN);
        JudgmentFx.sigil(p.serverLevel(), t);
        p.displayClientMessage(Component.literal("Condemned: " + t.getDisplayName().getString()).withStyle(ChatFormatting.GOLD), true);
        if (JudgmentRules.atOrBelow(t.getHealth(), t.getMaxHealth(), JudgmentRules.EXECUTE_BELOW)) JudgmentDamage.execute(p, t);
        return true;
    }

    private static LivingEntity oldMark(ServerPlayer p) {
        if (!p.getPersistentData().hasUUID(JudgmentDamage.OWNER_SIGIL)) return null;
        Entity e = p.serverLevel().getEntity(p.getPersistentData().getUUID(JudgmentDamage.OWNER_SIGIL));
        return e instanceof LivingEntity le ? le : null;
    }

    /** Supreme Edict: every foe within thirty blocks (the thirty-two nearest) loses its good effects, and every domain of another owner there ends. */
    private static boolean edict(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        double r = JudgmentRules.EDICT_RADIUS;
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p) && x.distanceToSqr(p) <= r * r));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= JudgmentRules.MAX_EDICT_TARGETS) break;
            JudgmentDamage.stripBeneficial(t, JudgmentRules.STRIP_MAX);
        }
        JudgmentFields.clearAround(l, p.position(), r, p.getUUID());
        JudgmentFx.edict(l, p.position());
        return true;
    }

    /** Guillotine Cleave: a short sweep in front (reach 5.5, 130 degrees, twelve creatures): true damage, Future Rejection, five seconds of debt. */
    private static boolean cleave(ManasSkillInstance i, ServerPlayer p, boolean mastered) {
        ServerLevel l = p.serverLevel();
        double reach = JudgmentRules.CLEAVE_REACH;
        Vec3 look = p.getViewVector(1f);
        double mastery = mastery(p, i);
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach, 2.5, reach),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p)
                        && JudgmentRules.inCone(look.x, look.z, x.getX() - p.getX(), x.getZ() - p.getZ(), reach + x.getBbWidth() * 0.5, JudgmentRules.CLEAVE_HALF_ANGLE_DEG)));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= JudgmentRules.MAX_CLEAVE_TARGETS) break;
            JudgmentDamage.hit(p, t, (float) JudgmentRules.CLEAVE_DAMAGE, mastery);
            if (!t.isAlive()) continue;
            t.addEffect(new MobEffectInstance(JudgmentEffects.FUTURE_REJECTION, JudgmentRules.REJECTION_TICKS, 0, false, true, true));
            TemporalDebt.add(t, JudgmentRules.DEBT_CLEAVE);
        }
        JudgmentFx.cleave(l, p.position().add(0.0, 1.0, 0.0), p.position().add(0.0, 1.0, 0.0).add(look.scale(reach)));
        return true;
    }

    /** Verdict of Ruin: a field of true damage that silences and hinders everyone in it; it follows the creature in the line of sight, or stands where the owner aims. */
    private static boolean verdict(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        LivingEntity t = JudgmentState.first(p, JudgmentRules.VERDICT_RANGE, 1.0);
        Vec3 center = t != null ? t.position().add(0.0, 0.1, 0.0) : GrimoireBook.aim(p, JudgmentRules.VERDICT_RANGE);
        if (!JudgmentFields.add(p, l, t, center, JudgmentRules.VERDICT_RADIUS, JudgmentRules.VERDICT_TICKS)) {
            fail(p, "Too many judgment fields stand on this server.");
            return false;
        }
        JudgmentFx.field(l, t, center, JudgmentRules.VERDICT_RADIUS, JudgmentRules.VERDICT_TICKS);
        return true;
    }

    // ---------------------------------------------------------------- Final Judgment (held)
    @Override
    protected void startHeld(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        var tag = i.getOrCreateTag();
        tag.putInt(TAG_STATE, 0);
        double cost = JudgmentRules.cost(m, true, JudgmentConfig.costScale());
        if (!p.isCreative() && TensuraBridge.magicule(p) < cost) { fail(p, "Not enough magicules (need " + (long) cost + ")."); return; }
        LivingEntity t = JudgmentState.first(p, JudgmentRules.FINAL_RANGE, 1.2);
        if (t == null) { fail(p, "Nothing in your sight to judge."); return; }
        tag.putInt(TAG_STATE, 1);
        tag.putUUID(TAG_TARGET, t.getUUID());
        i.markDirty();
        JudgmentFx.cross(p.serverLevel(), t);
        lock(t);
    }

    /** Freezes the target in time for a moment; a boss (whose control is limited by the Balance Law) only stutters. */
    private static void lock(LivingEntity t) {
        t.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, JudgmentRules.CHANNEL_LOCK_TICKS, 0, false, true, true));
        if (BalanceLaw.isBoss(t)) return;
        t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, JudgmentRules.CHANNEL_LOCK_TICKS, 0, false, true, true));
        t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, JudgmentRules.CHANNEL_LOCK_TICKS, 0, false, true, true));
    }

    @Override
    protected boolean held(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks, boolean mastered) {
        if (m.index() != FINAL_MODE) return true;
        var tag = i.getOrCreateTag();
        if (tag.getInt(TAG_STATE) != 1) return false;
        Entity found = tag.hasUUID(TAG_TARGET) ? p.serverLevel().getEntity(tag.getUUID(TAG_TARGET)) : null;
        if (!(found instanceof LivingEntity t) || !t.isAlive() || !p.isAlive() || t.distanceTo(p) > JudgmentRules.FINAL_RANGE + 8.0 || heldTicks > JudgmentRules.FINAL_MAX_HELD) {
            endFinal(i, p, "Final Judgment loses its target.");
            return false;
        }
        if (heldTicks % 10 == 0) lock(t);
        if (heldTicks < JudgmentRules.FINAL_CHANNEL_TICKS) return true;

        double cost = JudgmentRules.cost(m, true, JudgmentConfig.costScale());
        if (!p.isCreative() && !TensuraBridge.spendMagicule(p, cost)) {
            endFinal(i, p, "Not enough magicules (need " + (long) cost + ").");
            return false;
        }
        double damage = TemporalDebt.detonate(t, JudgmentRules.FINAL_DEBT_MULTIPLIER, JudgmentRules.FINAL_MIN_DAMAGE, JudgmentConfig.finalCap());
        JudgmentDamage.detonation(p, t, damage);
        JudgmentFx.boom(p.serverLevel(), t.position());
        startCooldown(i, m, true);
        addMasteryPoint(i, p);
        endFinal(i, p, null);
        return false;
    }

    @Override
    protected void released(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks) {
        if (m.index() == FINAL_MODE && i.getOrCreateTag().getInt(TAG_STATE) == 1) endFinal(i, p, "Final Judgment is let go.");
    }

    private static void endFinal(ManasSkillInstance i, ServerPlayer p, String message) {
        i.getOrCreateTag().putInt(TAG_STATE, 0);
        i.markDirty();
        if (message != null) fail(p, message);
    }

    /** Owner UUID helper for the tests and the events: the player whose sigil sits on {@code target}. */
    static UUID sigilBy(LivingEntity target) {
        var t = target.getPersistentData();
        return t.hasUUID(JudgmentDamage.SIGIL_BY) ? t.getUUID(JudgmentDamage.SIGIL_BY) : null;
    }
}
