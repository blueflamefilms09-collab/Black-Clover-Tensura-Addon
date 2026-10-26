package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.veldanava.pocket.PocketLabyrinth;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Jibril, Lord of Rigor: the third of the five Veldanava ultimate skills (the numbers are in {@link JibrilRules}, the passive rules in {@link JibrilEvents}). Ten modes: four toggle passives (Divine
 * Rigor, Heavenly Presence, Strict Governance, Wings of Revelation), five actives (Absolute Command, Judgment of Heaven, Heavenly Purge, Holy Shackles, Word of Law) and the held mastery active Kingdom of
 * God. The press, cost, cooldown and toggle code is {@link VeldanavaSkillBase}.
 *
 * <p>Worst case per press (rule 11): Absolute Command 1 line-of-sight query, 1 target; Judgment of Heaven 1 query + 1 box query, 7 hits; Heavenly Purge 2 box queries (clouds: at most 64 removed; allied
 * players: at most 24 cleansed) and a field sweep; Holy Shackles 1 query, 8 effects moved; Word of Law no query (a flag on the owner); Kingdom of God one field and, once a second, one box query with at most 24
 * enemies touched. The passives run once a second per owner: one box query of 30 blocks for Heavenly Presence and Strict Governance together (12 creatures burned, 16 players' cooldowns walked at most 24
 * skills x 16 modes), at most 8 effects cleansed for Divine Rigor, one ability refresh for the wings. One effect packet per press (the Kingdom and the wings: one every 80 ticks while they last).
 */
public class JibrilSkill extends VeldanavaSkillBase {
    public JibrilSkill() { super(JibrilRules.JIBRIL); }

    // ---------------------------------------------------------------- the passives that need a look once a second
    @Override
    protected void secondTick(ManasSkillInstance i, ServerPlayer p, int toggles, boolean mastered) {
        if (JudgmentRules.on(toggles, 0)) rigor(p, mastered); else JibrilState.aura(p, false);
        wings(p, JudgmentRules.on(toggles, 3));
        boolean presence = JudgmentRules.on(toggles, 1), governance = JudgmentRules.on(toggles, 2) && JudgmentConfig.strictGovernance();
        if (presence || governance) scan(i, p, presence, governance, mastery(p, i));
    }

    @Override
    protected void toggled(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean on) {
        if (m.index() == 0) { if (on) rigor(p, JudgmentState.mastered(p)); else JibrilState.aura(p, false); }
        if (m.index() == 3) wings(p, on, true);
    }

    @Override
    protected void forgotten(ServerPlayer p) {
        JibrilState.clear(p);
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), KINGDOM_KIND);
    }

    /** Divine Rigor: every harmful effect the owner already carries is lifted; a mastered owner lets the allies near share it (see {@link JibrilEvents}). */
    private static void rigor(ServerPlayer p, boolean mastered) {
        JibrilState.cleanse(p, JibrilRules.RIGOR_CLEANSE_MAX);
        JibrilState.aura(p, mastered);
    }

    private static void wings(ServerPlayer p, boolean on) { wings(p, on, false); }

    /** Wings of Revelation: the flight is given (and kept given) while the toggle is on, taken back when it is off; the wings are drawn again every 80 ticks while the player is in the air. */
    private static void wings(ServerPlayer p, boolean on, boolean first) {
        JibrilState.applyWings(p, on && JudgmentConfig.wingsFlight());
        if (on && (first || (p.tickCount % JibrilRules.WINGS_FX_EVERY_TICKS == 0 && JibrilState.airborne(p)))) JibrilFx.wings(p.serverLevel(), p, first);
    }

    /**
     * One box query of thirty blocks for the two passives that look at their surroundings: Heavenly Presence burns the (at most twelve) demons, undead and indebted creatures within sixteen blocks, and
     * Strict Governance stretches the cooldowns of the (at most sixteen) enemy players within thirty.
     */
    private static void scan(ManasSkillInstance i, ServerPlayer p, boolean presence, boolean governance, double mastery) {
        double r = JibrilRules.GOVERNANCE_RADIUS;
        double r2 = r * r, pr2 = JibrilRules.PRESENCE_RADIUS * JibrilRules.PRESENCE_RADIUS;
        List<LivingEntity> near = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.distanceToSqr(p) <= r2));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int burned = 0, held = 0;
        for (LivingEntity t : near) {
            if (governance && t instanceof ServerPlayer sp && held < JibrilRules.GOVERNANCE_MAX_PLAYERS && Foes.isFoe(p, sp)) {
                held++;
                JibrilState.stretchCooldowns(sp);
            }
            if (presence && burned < JibrilRules.PRESENCE_MAX_TARGETS && t.distanceToSqr(p) <= pr2 && burns(t) && Foes.isFoe(p, t)) {
                burned++;
                JibrilState.holy(p, t, (float) JibrilRules.presenceDamage(TemporalDebt.get(t)), mastery);
            }
        }
    }

    /** True for what Heavenly Presence burns: the undead, a demon, and anything carrying Temporal Debt. */
    static boolean burns(LivingEntity t) {
        if (t.getType().is(EntityTypeTags.UNDEAD) || TemporalDebt.get(t) > 0) return true;
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t.getType());
        return key != null && JibrilRules.demonic(key.getPath());
    }

    // ---------------------------------------------------------------- the press
    @Override
    protected boolean activate(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        return switch (m.index()) {
            case 4 -> command(p);
            case 5 -> judgment(i, p);
            case 6 -> purge(p);
            case 7 -> shackles(p);
            default -> word(p, mastered);
        };
    }

    /** The creature in the line of sight that is not on the caster's side, or null (the failure is told). */
    private static LivingEntity target(ServerPlayer p, double range, String verb) {
        LivingEntity t = JudgmentState.first(p, range, 1.0);
        if (t == null) { fail(p, "Nothing in your sight to " + verb + "."); return null; }
        if (Foes.isAlly(p, t)) { fail(p, t.getDisplayName().getString() + " stands on your side."); return null; }
        return t;
    }

    /**
     * Absolute Command: the creature stops. No attack, no item, no skill, no spell for the control time the Balance Law allows (5 s at most for a mob, 3 s for a player, 1 s for a boss, shorter when
     * repeated); a player's hotbar is held on the slot it was on.
     */
    private static boolean command(ServerPlayer p) {
        LivingEntity t = target(p, JibrilRules.COMMAND_RANGE, "command");
        if (t == null) return false;
        int ticks = BalanceLaw.controlTicks(t, JibrilRules.COMMAND_TICKS);
        t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, ticks, 0, false, true, true));
        JudgmentEffects.silence(t, ticks);
        if (t instanceof ServerPlayer sp) JibrilState.lockHotbar(sp, ticks);
        JibrilFx.command(p.serverLevel(), t, ticks);
        return true;
    }

    /**
     * Judgment of Heaven: a pillar of light on the target, through every block. Every good effect of the target is stripped, the target takes heavy holy damage, and up to six creatures standing in the
     * pillar take half of it.
     */
    private static boolean judgment(ManasSkillInstance i, ServerPlayer p) {
        LivingEntity t = target(p, JibrilRules.JUDGMENT_RANGE, "judge");
        if (t == null) return false;
        ServerLevel l = p.serverLevel();
        double mastery = mastery(p, i);
        Vec3 ground = t.position();
        boolean upgrades = JudgmentConfig.jibrilUpgrades();
        JibrilState.holy(p, t, (float) (upgrades ? JibrilRules.judgmentDamage(burns(t)) : JibrilRules.JUDGMENT_DAMAGE), mastery);
        if (t.isAlive()) JudgmentDamage.stripBeneficial(t, JudgmentRules.STRIP_MAX);
        List<LivingEntity> splash = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, new AABB(ground, ground).inflate(JibrilRules.JUDGMENT_SPLASH_RADIUS, 3.0, JibrilRules.JUDGMENT_SPLASH_RADIUS),
                x -> x != t && x.isAlive() && !x.isSpectator() && Foes.isFoe(p, x) && x.distanceToSqr(ground) <= JibrilRules.JUDGMENT_SPLASH_RADIUS * JibrilRules.JUDGMENT_SPLASH_RADIUS));
        splash.sort(Comparator.comparingDouble(x -> x.distanceToSqr(ground)));
        int n = 0;
        for (LivingEntity x : splash) {
            if (n++ >= JibrilRules.JUDGMENT_MAX_SPLASH) break;
            JibrilState.holy(p, x, (float) (JibrilRules.JUDGMENT_DAMAGE * JibrilRules.JUDGMENT_SPLASH_SHARE), mastery);
        }
        JibrilFx.judgment(l, ground);
        return true;
    }

    /**
     * Heavenly Purge: within fifty blocks every lingering cloud is gone (at most 64), every field of another owner ends (Verdict of Ruin, Nihility Collapse, a cage), every pocket maze there ends, and the
     * caster and the allied players nearby (at most 24) are cleansed of harmful effects. Blocks are never scanned or changed: "environmental corruption" is what the fields and clouds are.
     */
    static boolean purge(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        double r = JibrilRules.PURGE_RADIUS;
        Vec3 at = p.position();
        AABB box = p.getBoundingBox().inflate(r, Math.min(r, 64.0), r);
        int clouds = 0;
        for (AreaEffectCloud c : l.getEntitiesOfClass(AreaEffectCloud.class, box, x -> x.isAlive() && x.distanceToSqr(p) <= r * r)) {
            if (clouds++ >= JibrilRules.PURGE_MAX_CLOUDS) break;
            c.discard();
        }
        JudgmentFields.clearAround(l, at, r, p.getUUID());
        PocketLabyrinth.endNear(l, at, r, p.getUUID());
        JibrilState.cleanse(p, JibrilRules.PURGE_CLEANSE_MAX);
        p.clearFire();
        int allies = 0;
        for (ServerPlayer o : l.getEntitiesOfClass(ServerPlayer.class, box, x -> x != p && x.isAlive() && x.distanceToSqr(p) <= r * r && Foes.isAlly(p, x))) {
            if (allies++ >= JibrilRules.PURGE_MAX_ALLIES) break;
            JibrilState.cleanse(o, JibrilRules.PURGE_CLEANSE_MAX);
            o.clearFire();
        }
        if (JudgmentConfig.jibrilUpgrades()) burnTheUnholy(p, l, at);
        JibrilFx.purge(l, at, (float) r);
        return true;
    }

    /** The Purge's light burns the unholy near the caster: one box query, the nearest {@link JibrilRules#PURGE_BURN_MAX} foes that {@link #burns}, one holy blow each. */
    private static void burnTheUnholy(ServerPlayer p, ServerLevel l, Vec3 at) {
        double r = JibrilRules.PURGE_BURN_RADIUS;
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r, 4.0, r),
                x -> x != p && x.isAlive() && !x.isSpectator() && Foes.isFoe(p, x) && burns(x) && x.distanceToSqr(at) <= r * r));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(at)));
        double mastery = JudgmentState.mastered(p) ? 1.0 : 0.4;
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= JibrilRules.PURGE_BURN_MAX) break;
            JibrilState.holy(p, t, (float) JibrilRules.PURGE_BURN_DAMAGE, mastery);
        }
    }

    /** Holy Shackles: the creature is bound (Movement Interference for the control time the Balance Law allows) and up to eight of its good effects are taken and given to the caster. */
    private static boolean shackles(ServerPlayer p) {
        LivingEntity t = target(p, JibrilRules.SHACKLE_RANGE, "bind");
        if (t == null) return false;
        t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, BalanceLaw.controlTicks(t, JibrilRules.SHACKLE_TICKS), 0, false, true, true));
        int stolen = JibrilState.steal(t, p, JibrilRules.SHACKLE_STEAL_MAX);
        if (JudgmentConfig.jibrilUpgrades()) JibrilState.holy(p, t, (float) JibrilRules.shackleBite(stolen), JudgmentState.mastered(p) ? 1.0 : 0.4);
        JibrilFx.shackles(p.serverLevel(), t, p);
        return true;
    }

    /** Word of Law: opens the parry window on the caster (twelve ticks, twenty once mastered); the blow that lands in it is negated and comes back at 300% ({@link JibrilEvents}). */
    private static boolean word(ServerPlayer p, boolean mastered) {
        int ticks = JibrilRules.parryTicks(mastered);
        JibrilState.openParry(p, ticks);
        JibrilFx.word(p.serverLevel(), p, ticks);
        return true;
    }

    // ---------------------------------------------------------------- Kingdom of God (held, mastery)
    static final String KINGDOM_KIND = "kingdom";

    @Override
    protected void startHeld(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        double perSecond = JudgmentRules.cost(m, true, JudgmentConfig.costScale());
        if (!p.isCreative() && !TensuraBridge.spendMagicule(p, perSecond)) { fail(p, "Not enough magicules (need " + (long) perSecond + " a second)."); return; }
        ServerLevel l = p.serverLevel();
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), KINGDOM_KIND);
        if (!JudgmentFields.add(p, l, p, p.position(), JibrilRules.KINGDOM_RADIUS, JibrilRules.KINGDOM_MAX_TICKS, new Kingdom(perSecond), KINGDOM_KIND)) {
            if (!p.isCreative()) TensuraBridge.refundMagicule(p, perSecond);
            fail(p, "Too many fields stand on this server.");
            return;
        }
        JibrilState.kingdom(p);
        JibrilFx.kingdom(l, p, true);
        startCooldown(i, m, true);
        addMasteryPoint(i, p);
        i.markDirty();
    }

    @Override
    protected boolean held(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks, boolean mastered) {
        return JudgmentFields.find(p.getUUID(), KINGDOM_KIND) != null;
    }

    @Override
    protected void released(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks) {
        if (JudgmentFields.clearKind(p.getServer(), p.getUUID(), KINGDOM_KIND) > 0) fail(p, "The Kingdom of God is let go.");
    }

    /**
     * The Kingdom of God as a field that follows its owner: once a second it takes the next second's Magicule (and ends when there is none), renews the sanctuary record the events read (allies inside cannot
     * be brought below 1 HP, see {@link JibrilEvents}), strips the good effects of the (at most 24) enemies inside and crushes them with gravity, and draws the dome again every 80 ticks.
     */
    static final class Kingdom implements JudgmentFields.Behavior {
        private final double costPerSecond;
        private int pulses;

        Kingdom(double costPerSecond) { this.costPerSecond = costPerSecond; }

        @Override public int period() { return JibrilRules.KINGDOM_PULSE_TICKS; }

        @Override
        public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
            if (!owner.isCreative() && costPerSecond > 0.0 && !TensuraBridge.spendMagicule(owner, costPerSecond)) {
                fail(owner, "The Kingdom of God falls: no magicules left.");
                f.endsAt = 0L;
                return;
            }
            JibrilState.kingdom(owner);
            if (++pulses % (JibrilRules.KINGDOM_FX_EVERY_TICKS / JibrilRules.KINGDOM_PULSE_TICKS) == 0) JibrilFx.kingdom(level, owner, false);
            boolean wear = JudgmentConfig.jibrilUpgrades();
            double r = f.radius, r2 = r * r;
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(r, Math.min(r, 48.0), r),
                    x -> x != owner && x.isAlive() && !x.isSpectator() && x.distanceToSqr(owner) <= r2);
            int n = 0;
            for (LivingEntity t : near) {
                if (n >= JibrilRules.KINGDOM_MAX_ENEMIES) break;
                if (!Foes.isFoe(owner, t)) continue;
                n++;
                JudgmentDamage.stripBeneficial(t, JibrilRules.KINGDOM_STRIP);
                if (wear) JudgmentDamage.share(owner, t, JibrilRules.KINGDOM_PULSE_SHARE);
                t.addEffect(new MobEffectInstance(JudgmentEffects.GRAVITY_CRUSH, JibrilRules.KINGDOM_CRUSH_TICKS, 0, false, true, true));      // a weight, not a hold: renewed each second, so the Balance Law's control limits are not spent on it
            }
        }

        @Override
        public void end(ServerLevel level, JudgmentFields.Field f) { JibrilState.endKingdom(f.owner); }
    }
}
